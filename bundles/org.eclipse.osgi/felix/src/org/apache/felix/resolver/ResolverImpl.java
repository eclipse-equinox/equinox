/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.resolver;

import java.security.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.felix.resolver.reason.ReasonException;
import org.apache.felix.resolver.util.ArrayMap;
import org.apache.felix.resolver.util.CandidateSelector;
import org.apache.felix.resolver.util.OpenHashMap;
import org.osgi.framework.namespace.*;
import org.osgi.resource.*;
import org.osgi.service.resolver.*;

public class ResolverImpl implements Resolver
{
    private final AccessControlContext m_acc =
        System.getSecurityManager() != null ?
            AccessController.getContext() :
            null;

    private final Logger m_logger;

    private final int m_parallelism;

    private final Executor m_executor;

    enum PermutationType {
        USES,
        IMPORT,
        SUBSTITUTE
    }

    // Note this class is not thread safe.
    // Only use in the context of a single thread.
    static class ResolveSession implements Runnable
    {
        // Holds the resolve context for this session
        private final ResolveContext m_resolveContext;
        private final Collection<Resource> m_mandatoryResources;
        private final Collection<Resource> m_optionalResources;
        private final Resource m_dynamicHost;
        private final Requirement m_dynamicReq;
        private final List<Capability> m_dynamicCandidates;
        // keeps track of valid related resources that we have seen.
        // a null value or TRUE indicate it is valid
        private Map<Resource, Boolean> m_validRelatedResources = new HashMap<Resource, Boolean>(0);
        // keeps track of related resources for each resource
        private Map<Resource, Collection<Resource>> m_relatedResources = new HashMap<Resource, Collection<Resource>>(0);
        // Holds candidate permutations based on permutating "uses" chains.
        // These permutations are given higher priority.
        private final List<Candidates> m_usesPermutations = new LinkedList<Candidates>();
        private int m_usesIndex = 0;
        // Holds candidate permutations based on permutating requirement candidates.
        // These permutations represent backtracking on previous decisions.
        private final List<Candidates> m_importPermutations = new LinkedList<Candidates>();
        private int m_importIndex = 0;
        // Holds candidate permutations based on substituted packages
        private final List<Candidates> m_substPermutations = new LinkedList<Candidates>();
        private int m_substituteIndex = 0;
        // Holds candidate permutations based on removing candidates that satisfy
        // multiple cardinality requirements.
        // This permutation represents a permutation that is consistent because we have
        // removed the offending capabilities
        private Candidates m_multipleCardCandidates = null;
        // The delta is used to detect that we have already processed this particular permutation
        private final Set<Object> m_processedDeltas = new HashSet<Object>();
        private final Executor m_executor;
        private final Set<Requirement> m_mutated = new HashSet<Requirement>();
        private final Set<Requirement> m_sub_mutated = new HashSet<Requirement>();
        private final ConcurrentMap<String, List<String>> m_usesCache = new ConcurrentHashMap<String, List<String>>();
        private ResolutionError m_currentError;
        volatile private CancellationException m_isCancelled = null;

        static ResolveSession createSession(ResolveContext resolveContext, Executor executor, Resource dynamicHost, Requirement dynamicReq, List<Capability> dynamicCandidates)
        {
            ResolveSession session = new ResolveSession(resolveContext, executor, dynamicHost, dynamicReq, dynamicCandidates);
            // call onCancel first
            session.getContext().onCancel(session);
            // now gather the mandatory and optional resources
            session.initMandatoryAndOptionalResources();
            return session;
        }

        private ResolveSession(ResolveContext resolveContext, Executor executor, Resource dynamicHost, Requirement dynamicReq, List<Capability> dynamicCandidates)
        {
            m_resolveContext = resolveContext;
            m_executor = executor;
            m_dynamicHost = dynamicHost;
            m_dynamicReq = dynamicReq;
            m_dynamicCandidates = dynamicCandidates;
            if (m_dynamicHost != null) {
                m_mandatoryResources = Collections.singletonList(dynamicHost);
                m_optionalResources = Collections.emptyList();
            } else {
                // Do not call resolve context yet, onCancel must be called first
                m_mandatoryResources = new ArrayList<Resource>();
                m_optionalResources = new ArrayList<Resource>();
            }
        }

        private void initMandatoryAndOptionalResources() {
            if (!isDynamic()) {
                m_mandatoryResources.addAll(getContext().getMandatoryResources());
                m_optionalResources.addAll(getContext().getOptionalResources());
            }
        }
        Candidates getMultipleCardCandidates()
        {
            return m_multipleCardCandidates;
        }

        ResolveContext getContext()
        {
            return m_resolveContext;
        }

        ConcurrentMap<String, List<String>> getUsesCache() {
            return m_usesCache;
        }

        void permutateIfNeeded(PermutationType type, Requirement req, Candidates permutation) {
            List<Capability> candidates = permutation.getCandidates(req);
            if ((candidates != null) && (candidates.size() > 1))
            {
                if ((type == PermutationType.SUBSTITUTE)) {
                    if (!m_sub_mutated.add(req)) {
                        return;
                    }
                } else if (!m_mutated.add(req)) {
                    return;
                }
                // If we haven't already permutated the existing
                // import, do so now.
                addPermutation(type, permutation.permutate(req));
            }
        }

        private void clearMutateIndexes() {
            m_usesIndex = 0;
            m_importIndex = 0;
            m_substituteIndex = 0;
            m_mutated.clear();
            // NOTE: m_sub_mutated is never cleared.
            // It is unclear if even more permutations based on a substitutions will ever help.
            // Being safe and reducing extra permutations until we get a scenario that proves
            // more permutations would really help.
        }

        void addPermutation(PermutationType type, Candidates permutation) {
            if (permutation != null)
            {
                List<Candidates> typeToAddTo = null;
                try {
                    switch (type) {
                        case USES :
                            typeToAddTo = m_usesPermutations;
                            m_usesPermutations.add(m_usesIndex++, permutation);
                            break;
                        case IMPORT :
                            typeToAddTo = m_importPermutations;
                            m_importPermutations.add(m_importIndex++, permutation);
                            break;
                        case SUBSTITUTE :
                            typeToAddTo = m_substPermutations;
                            m_substPermutations.add(m_substituteIndex++, permutation);
                            break;
                        default :
                            throw new IllegalArgumentException("Unknown permitation type: " + type);
                    }
                } catch (IndexOutOfBoundsException e) {
                    // just a safeguard, this really should never happen
                    typeToAddTo.add(permutation);
                }
            }
        }

        Candidates getNextPermutation() {
            Candidates next = null;
            do {
                if (!m_usesPermutations.isEmpty())
                {
                    next = m_usesPermutations.remove(0);
                }
                else if (!m_importPermutations.isEmpty())
                {
                    next = m_importPermutations.remove(0);
                }
                else if (!m_substPermutations.isEmpty())
                {
                    next = m_substPermutations.remove(0);
                }
                else {
                    return null;
                }
            }
            while(!m_processedDeltas.add(next.getDelta()));
            // Null out each time a new permutation is attempted.
            // We only use this to store a valid permutation which is a
            // delta of the current permutation.
            m_multipleCardCandidates = null;
            // clear mutateIndexes also so we insert new permutations
            // based of this permutation as a higher priority
            clearMutateIndexes();
            return next;
        }

        void clearPermutations() {
            m_usesPermutations.clear();
            m_importPermutations.clear();
            m_substPermutations.clear();
            m_multipleCardCandidates = null;
            m_processedDeltas.clear();
            m_currentError = null;
        }

        boolean checkMultiple(
                UsedBlames usedBlames,
                Blame usedBlame,
                Candidates permutation)
        {
            // Check the root requirement to see if it is a multiple cardinality
            // requirement.
            CandidateSelector candidates = null;
            Requirement req = usedBlame.m_reqs.get(0);
            if (Util.isMultiple(req))
            {
                // Create a copy of the current permutation so we can remove the
                // candidates causing the blame.
                if (m_multipleCardCandidates == null)
                {
                    m_multipleCardCandidates = permutation.copy();
                }
                // Get the current candidate list and remove all the offending root
                // cause candidates from a copy of the current permutation.
                candidates = m_multipleCardCandidates.clearMultipleCardinalityCandidates(req, usedBlames.getRootCauses(req));
            }
            // We only are successful if there is at least one candidate left
            // for the requirement
            return (candidates != null) && !candidates.isEmpty();
        }

        long getPermutationCount() {
            return m_usesPermutations.size() + m_importPermutations.size() + m_substPermutations.size(); 
        }

        Executor getExecutor() {
            return m_executor;
        }

        ResolutionError getCurrentError() {
            return m_currentError;
        }

        void setCurrentError(ResolutionError error) {
            this.m_currentError = error;
        }

        boolean isDynamic() {
            return m_dynamicHost != null;
        }

        Collection<Resource> getMandatoryResources() {
            return m_mandatoryResources;
        }

        Collection<Resource> getOptionalResources() {
            return m_optionalResources;
        }

        Resource getDynamicHost() {
            return m_dynamicHost;
        }

        Requirement getDynamicRequirement() {
            return m_dynamicReq;
        }

        List<Capability> getDynamicCandidates() {
            return m_dynamicCandidates;
        }

        public boolean isValidRelatedResource(Resource resource) {
            Boolean valid = m_validRelatedResources.get(resource);
            if (valid == null)
            {
                // Mark this resource as a valid related resource
                m_validRelatedResources.put(resource, Boolean.TRUE);
                valid = Boolean.TRUE;
            }
            return valid;
        }

        public boolean invalidateRelatedResource(Resource faultyResource) {
            Boolean valid = m_validRelatedResources.get(faultyResource);
            if (valid != null && valid)
            {
                // This was related resource.
                // Invalidate it and try again.
                m_validRelatedResources.put(faultyResource, Boolean.FALSE);
                return true;
            }
            return false;
        }

        public Collection<Resource> getRelatedResources(Resource resource) {
            Collection<Resource> related =  m_relatedResources.get(resource);
            return related == null ? Collections.<Resource> emptyList() : related;
        }

        public void setRelatedResources(Resource resource, Collection<Resource> related) {
            m_relatedResources.put(resource, related);
        }

        @Override
        public void run() {
            m_isCancelled = new CancellationException();
        }

        boolean isCancelled() {
            return m_isCancelled != null;
        }

        void checkForCancel() throws ResolutionException {
            if (isCancelled()) {
                throw new ResolutionException("Resolver operation has been cancelled.", m_isCancelled, null);
            }
        }
    }

    public ResolverImpl(Logger logger)
    {
        this(logger, Runtime.getRuntime().availableProcessors());
    }

    public ResolverImpl(Logger logger, int parallelism)
    {
        this.m_logger = logger;
        this.m_parallelism = parallelism;
        this.m_executor = null;
    }

    public ResolverImpl(Logger logger, Executor executor)
    {
        this.m_logger = logger;
        this.m_parallelism = -1;
        this.m_executor = executor;
    }

    public Map<Resource, List<Wire>> resolve(ResolveContext rc) throws ResolutionException
    {
        if (m_executor != null)
        {
            return resolve(rc, m_executor);
        }
        else if (m_parallelism > 1)
        {
            final ExecutorService executor =
                System.getSecurityManager() != null ?
                    AccessController.doPrivileged(
                        new PrivilegedAction<ExecutorService>()
                        {
                            public ExecutorService run()
                            {
                                return Executors.newFixedThreadPool(m_parallelism);
                            }
                        }, m_acc)
                :
                    Executors.newFixedThreadPool(m_parallelism);
            try
            {
                return resolve(rc, executor);
            }
            finally
            {
                if (System.getSecurityManager() != null)
                {
                    AccessController.doPrivileged(new PrivilegedAction<Void>(){
                        public Void run() {
                            executor.shutdownNow();
                            return null;
                        }
                    }, m_acc);
                }
                else
                {
                    executor.shutdownNow();
                }
            }
        }
        else
        {
            return resolve(rc, new DumbExecutor());
        }
    }

    public Map<Resource, List<Wire>> resolve(ResolveContext rc, Executor executor) throws ResolutionException
    {
        ResolveSession session = ResolveSession.createSession(rc, executor, null, null, null);
        return doResolve(session);
    }

    private Map<Resource, List<Wire>> doResolve(ResolveSession session) throws ResolutionException {
        Map<Resource, List<Wire>> wireMap = new HashMap<Resource, List<Wire>>();
        boolean retry;
        do
        {
            retry = false;
            try
            {
                getInitialCandidates(session);
                if (session.getCurrentError() != null) {
                    throw session.getCurrentError().toException();
                }

                Map<Resource, ResolutionError> faultyResources = new HashMap<Resource, ResolutionError>();
                Candidates allCandidates = findValidCandidates(session, faultyResources);
                session.checkForCancel();

                // If there is a resolve exception, then determine if an
                // optionally resolved resource is to blame (typically a fragment).
                // If so, then remove the optionally resolved resolved and try
                // again; otherwise, m_currentError the resolve exception.
                if (session.getCurrentError() != null)
                {
                    Set<Resource> resourceKeys = faultyResources.keySet();
                    retry = (session.getOptionalResources().removeAll(resourceKeys));
                    for (Resource faultyResource : resourceKeys)
                    {
                        if (session.invalidateRelatedResource(faultyResource))
                        {
                            retry = true;
                        }
                    }
                    // log all the resolution exceptions for the uses constraint violations
                    for (Map.Entry<Resource, ResolutionError> usesError : faultyResources.entrySet())
                    {
                        m_logger.logUsesConstraintViolation(usesError.getKey(), usesError.getValue());
                    }
                    if (!retry)
                    {
                        throw session.getCurrentError().toException();
                    }
                }
                // If there is no exception to m_currentError, then this was a clean
                // resolve, so populate the wire map.
                else
                {
                    if (session.getMultipleCardCandidates() != null)
                    {
                        // Candidates for multiple cardinality requirements were
                        // removed in order to provide a consistent class space.
                        // Use the consistent permutation
                        allCandidates = session.getMultipleCardCandidates();
                    }
                    if (session.isDynamic() )
                    {
                        wireMap = populateDynamicWireMap(session,
                            wireMap, allCandidates);
                    }
                    else
                    {
                        for (Resource resource : allCandidates.getRootHosts().keySet())
                        {
                            if (allCandidates.isPopulated(resource))
                            {
                                wireMap =
                                    populateWireMap(
                                        session, allCandidates.getWrappedHost(resource),
                                        wireMap, allCandidates);
                            }
                        }
                    }
                }
            }
            finally
            {
                // Always clear the state.
                session.clearPermutations();
            }
        }
        while (retry);

        return wireMap;
    }

    private void getInitialCandidates(ResolveSession session) {
        // Create object to hold all candidates.
        Candidates initialCandidates;
        if (session.isDynamic()) {
            // Create all candidates pre-populated with the single candidate set
            // for the resolving dynamic import of the host.
            initialCandidates = new Candidates(session);
            ResolutionError prepareError = initialCandidates.populateDynamic();
            if (prepareError != null) {
                session.setCurrentError(prepareError);
                return;
            }
        } else {
            List<Resource> toPopulate = new ArrayList<Resource>();

            // Populate mandatory resources; since these are mandatory
            // resources, failure throws a resolve exception.
            for (Resource resource : session.getMandatoryResources())
            {
                if (Util.isFragment(resource) || (session.getContext().getWirings().get(resource) == null))
                {
                    toPopulate.add(resource);
                }
            }
            // Populate optional resources; since these are optional
            // resources, failure does not throw a resolve exception.
            for (Resource resource : session.getOptionalResources())
            {
                if (Util.isFragment(resource) || (session.getContext().getWirings().get(resource) == null))
                {
                    toPopulate.add(resource);
                }
            }

            initialCandidates = new Candidates(session);
            initialCandidates.populate(toPopulate);
        }

        // Merge any fragments into hosts.
        ResolutionError prepareError = initialCandidates.prepare();
        if (prepareError != null)
        {
            session.setCurrentError(prepareError);
        }
        else
        {
            // Record the initial candidate permutation.
            session.addPermutation(PermutationType.USES, initialCandidates);
        }
    }

    private Candidates findValidCandidates(ResolveSession session, Map<Resource, ResolutionError> faultyResources) {
        Candidates allCandidates = null;
        boolean foundFaultyResources = false;
        do
        {
            allCandidates = session.getNextPermutation();
            if (allCandidates == null)
            {
                break;
            }

//allCandidates.dump();

            Map<Resource, ResolutionError> currentFaultyResources = new HashMap<Resource, ResolutionError>();

            session.setCurrentError(
                    checkConsistency(
                            session,
                            allCandidates,
                            currentFaultyResources
                    )
            );

            if (!currentFaultyResources.isEmpty())
            {
                if (!foundFaultyResources)
                {
                    foundFaultyResources = true;
                    faultyResources.putAll(currentFaultyResources);
                }
                else if (faultyResources.size() > currentFaultyResources.size())
                {
                    // save the optimal faultyResources which has less
                    faultyResources.clear();
                    faultyResources.putAll(currentFaultyResources);
                }
            }
        }
        while (!session.isCancelled() && session.getCurrentError() != null);

        return allCandidates;
    }

    private ResolutionError checkConsistency(
        ResolveSession session,
        Candidates allCandidates,
        Map<Resource, ResolutionError> currentFaultyResources)
    {
        ResolutionError rethrow = allCandidates.checkSubstitutes();
        if (rethrow != null)
        {
            return rethrow;
        }
        Map<Resource, Resource> allhosts = allCandidates.getRootHosts();
        // Calculate package spaces
        Map<Resource, Packages> resourcePkgMap =
            calculatePackageSpaces(session, allCandidates, allhosts.values());
        ResolutionError error = null;
        // Check package consistency
        Map<Resource, Object> resultCache =
                new OpenHashMap<Resource, Object>(resourcePkgMap.size());
        for (Entry<Resource, Resource> entry : allhosts.entrySet())
        {
            rethrow = checkPackageSpaceConsistency(
                    session, entry.getValue(),
                    allCandidates, session.isDynamic(), resourcePkgMap, resultCache);
            if (session.isCancelled()) {
                return null;
            }
            if (rethrow != null)
            {
                Resource faultyResource = entry.getKey();
                // check that the faulty requirement is not from a fragment
                for (Requirement faultyReq : rethrow.getUnresolvedRequirements())
                {
                    if (faultyReq instanceof WrappedRequirement)
                    {
                        faultyResource =
                                ((WrappedRequirement) faultyReq)
                                        .getDeclaredRequirement().getResource();
                        break;
                    }
                }
                currentFaultyResources.put(faultyResource, rethrow);
                error = rethrow;
            }
        }
        return error;
    }

    public Map<Resource,List<Wire>> resolveDynamic(ResolveContext context,
            Wiring hostWiring, Requirement dynamicRequirement)
            throws ResolutionException
    {
        Resource host = hostWiring.getResource();
        List<Capability> matches = context.findProviders(dynamicRequirement);
        // We can only create a dynamic import if the following
        // conditions are met:
        // 1. The package in question is not already imported.
        // 2. The package in question is not accessible via require-bundle.
        // 3. The package in question is not exported by the resource.
        if (!matches.isEmpty())
        {
            // Make sure all matching candidates are packages.
            for (Capability cap : matches)
            {
                if (!cap.getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE))
                {
                    throw new IllegalArgumentException(
                        "Matching candidate does not provide a package name.");
                }
            }
            ResolveSession session = ResolveSession.createSession(context,  new DumbExecutor(), host, dynamicRequirement, matches);
            return doResolve(session);
        }

        throw new Candidates.MissingRequirementError(dynamicRequirement).toException();
    }

    private static List<WireCandidate> getWireCandidates(ResolveSession session, Candidates allCandidates, Resource resource)
    {
        // Create a list for requirement and proposed candidate
        // capability or actual capability if resource is resolved or not.
        List<WireCandidate> wireCandidates = new ArrayList<WireCandidate>(256);
        Wiring wiring = session.getContext().getWirings().get(resource);
        if (wiring != null)
        {
            // Use wires to get actual requirements and satisfying capabilities.
            for (Wire wire : wiring.getRequiredResourceWires(null))
            {
                // Wrap the requirement as a hosted requirement if it comes
                // from a fragment, since we will need to know the host. We
                // also need to wrap if the requirement is a dynamic import,
                // since that requirement will be shared with any other
                // matching dynamic imports.
                Requirement r = wire.getRequirement();
                if (!r.getResource().equals(wire.getRequirer())
                    || Util.isDynamic(r))
                {
                    r = new WrappedRequirement(wire.getRequirer(), r);
                }
                // Wrap the capability as a hosted capability if it comes
                // from a fragment, since we will need to know the host.
                Capability c = wire.getCapability();
                if (!c.getResource().equals(wire.getProvider()))
                {
                    c = new WrappedCapability(wire.getProvider(), c);
                }
                wireCandidates.add(new WireCandidate(r, c));
            }

            // Since the resource is resolved, it could be dynamically importing,
            // so check to see if there are candidates for any of its dynamic
            // imports.
            //
            // NOTE: If the resource is dynamically importing, the fact that
            // the dynamic import is added here last to the
            // list is used later when checking to see if the package being
            // dynamically imported shadows an existing provider.
            Requirement dynamicReq = session.getDynamicRequirement();
            if (dynamicReq != null && resource.equals(session.getDynamicHost()))
            {
                // Grab first (i.e., highest priority) candidate.
                Capability cap = allCandidates.getFirstCandidate(dynamicReq);
                wireCandidates.add(new WireCandidate(dynamicReq, cap));
            }
        }
        else
        {
            for (Requirement req : resource.getRequirements(null))
            {
                if (!Util.isDynamic(req))
                {
                    // Get the candidates for the current requirement.
                    List<Capability> candCaps = allCandidates.getCandidates(req);
                    // Optional requirements may not have any candidates.
                    if (candCaps == null)
                    {
                        continue;
                    }

                    // For multiple cardinality requirements, we need to grab
                    // all candidates.
                    if (Util.isMultiple(req))
                    {
                        // Use the same requirement, but list each capability separately
                        for (Capability cap : candCaps)
                        {
                            wireCandidates.add(new WireCandidate(req, cap));
                        }
                    }
                    // Grab first (i.e., highest priority) candidate
                    else
                    {
                        Capability cap = candCaps.get(0);
                        wireCandidates.add(new WireCandidate(req, cap));
                    }
                }
            }
        }
        return wireCandidates;
    }

    private static Packages getPackages(
            ResolveSession session,
            Candidates allCandidates,
            Map<Resource, List<WireCandidate>> allWireCandidates,
            Map<Resource, Packages> allPackages,
            Resource resource,
            Packages resourcePkgs)
    {
        // First, all all exported packages
        // This has been done previously

        // Second, add all imported packages to the target resource's package space.
        for (WireCandidate wire : allWireCandidates.get(resource))
        {
            // If this resource is dynamically importing, then the last requirement
            // is the dynamic import being resolved, since it is added last to the
            // parallel lists above. For the dynamically imported package, make
            // sure that the resource doesn't already have a provider for that
            // package, which would be illegal and shouldn't be allowed.
            if (Util.isDynamic(wire.requirement))
            {
                String pkgName = (String) wire.capability.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE);
                if (resourcePkgs.m_exportedPkgs.containsKey(pkgName)
                    || resourcePkgs.m_importedPkgs.containsKey(pkgName)
                    || resourcePkgs.m_requiredPkgs.containsKey(pkgName))
                {
                    throw new IllegalArgumentException(
                        "Resource "
                        + resource
                        + " cannot dynamically import package '"
                        + pkgName
                        + "' since it already has access to it.");
                }
            }

            mergeCandidatePackages(
                session,
                allPackages,
                allCandidates,
                resourcePkgs,
                wire.requirement,
                wire.capability,
                new HashSet<Capability>(),
                new HashSet<Resource>());
        }

        return resourcePkgs;
    }

    private void computeUses(
            ResolveSession session,
            Map<Resource, List<WireCandidate>> allWireCandidates,
            Map<Resource, Packages> resourcePkgMap,
            Resource resource)
    {
        List<WireCandidate> wireCandidates = allWireCandidates.get(resource);
        Packages resourcePkgs = resourcePkgMap.get(resource);
        // Fourth, if the target resource is unresolved or is dynamically importing,
        // then add all the uses constraints implied by its imported and required
        // packages to its package space.
        // NOTE: We do not need to do this for resolved resources because their
        // package space is consistent by definition and these uses constraints
        // are only needed to verify the consistency of a resolving resource. The
        // only exception is if a resolved resource is dynamically importing, then
        // we need to calculate its uses constraints again to make sure the new
        // import is consistent with the existing package space.
        Wiring wiring = session.getContext().getWirings().get(resource);
        Set<Capability> usesCycleMap = new HashSet<Capability>();

        int size = wireCandidates.size();
        boolean isDynamicImporting = size > 0
                && Util.isDynamic(wireCandidates.get(size - 1).requirement);

        if ((wiring == null) || isDynamicImporting)
        {
            // Merge uses constraints from required capabilities.
            for (WireCandidate w : wireCandidates)
            {
                Requirement req = w.requirement;
                Capability cap = w.capability;
                // Ignore bundle/package requirements, since they are
                // considered below.
                if (!req.getNamespace().equals(BundleNamespace.BUNDLE_NAMESPACE)
                    && !req.getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE))
                {
                    List<Requirement> blameReqs =
                            Collections.singletonList(req);

                    mergeUses(
                        session,
                        resource,
                        resourcePkgs,
                        cap,
                        blameReqs,
                        cap,
                        resourcePkgMap,
                        usesCycleMap);
                }
            }
            // Merge uses constraints from imported packages.
            for (List<Blame> blames : resourcePkgs.m_importedPkgs.values())
            {
                for (Blame blame : blames)
                {
                    List<Requirement> blameReqs =
                        Collections.singletonList(blame.m_reqs.get(0));

                    mergeUses(
                        session,
                        resource,
                        resourcePkgs,
                        blame.m_cap,
                        blameReqs,
                        null,
                        resourcePkgMap,
                        usesCycleMap);
                }
            }
            // Merge uses constraints from required bundles.
            for (List<Blame> blames : resourcePkgs.m_requiredPkgs.values())
            {
                for (Blame blame : blames)
                {
                    List<Requirement> blameReqs =
                        Collections.singletonList(blame.m_reqs.get(0));

                    mergeUses(
                        session,
                        resource,
                        resourcePkgs,
                        blame.m_cap,
                        blameReqs,
                        null,
                        resourcePkgMap,
                        usesCycleMap);
                }
            }
        }
    }

    private static void mergeCandidatePackages(
            ResolveSession session,
            Map<Resource, Packages> resourcePkgMap,
            Candidates allCandidates,
            Packages packages,
            Requirement currentReq,
            Capability candCap,
            Set<Capability> capabilityCycles,
            Set<Resource> visitedRequiredBundles)
    {
        if (!capabilityCycles.add(candCap))
        {
            return;
        }

        if (candCap.getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE))
        {
            mergeCandidatePackage(
                packages.m_importedPkgs,
                currentReq, candCap);
        }
        else if (candCap.getNamespace().equals(BundleNamespace.BUNDLE_NAMESPACE))
        {
            // Get the candidate's package space to determine which packages
            // will be visible to the current resource.
            if (visitedRequiredBundles.add(candCap.getResource()))
            {
                // We have to merge all exported packages from the candidate,
                // since the current resource requires it.
                for (Blame blame : resourcePkgMap.get(candCap.getResource()).m_exportedPkgs.values())
                {
                    mergeCandidatePackage(
                        packages.m_requiredPkgs,
                        currentReq,
                        blame.m_cap);
                }
                // now merge in substitutes
                for (Blame blame : resourcePkgMap.get(
                    candCap.getResource()).m_substitePkgs.values())
                {
                    mergeCandidatePackage(packages.m_requiredPkgs, currentReq,
                        blame.m_cap);
                }
            }

            // If the candidate requires any other bundles with reexport visibility,
            // then we also need to merge their packages too.
            Wiring candWiring = session.getContext().getWirings().get(candCap.getResource());
            if (candWiring != null)
            {
                for (Wire w : candWiring.getRequiredResourceWires(null))
                {
                    if (w.getRequirement().getNamespace()
                        .equals(BundleNamespace.BUNDLE_NAMESPACE))
                    {
                        if (Util.isReexport(w.getRequirement()))
                        {
                            mergeCandidatePackages(
                                session,
                                resourcePkgMap,
                                allCandidates,
                                packages,
                                currentReq,
                                w.getCapability(),
                                capabilityCycles,
                                visitedRequiredBundles);
                        }
                    }
                }
            }
            else
            {
                for (Requirement req : candCap.getResource().getRequirements(null))
                {
                    if (req.getNamespace().equals(BundleNamespace.BUNDLE_NAMESPACE))
                    {
                        if (Util.isReexport(req))
                        {
                            Capability cap = allCandidates.getFirstCandidate(req);
                            if (cap != null)
                            {
                                mergeCandidatePackages(
                                        session,
                                        resourcePkgMap,
                                        allCandidates,
                                        packages,
                                        currentReq,
                                        cap,
                                        capabilityCycles,
                                        visitedRequiredBundles);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void mergeCandidatePackage(
        OpenHashMap<String, List<Blame>> packages,
        Requirement currentReq, Capability candCap)
    {
        if (candCap.getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE))
        {
            // Merge the candidate capability into the resource's package space
            // for imported or required packages, appropriately.

            String pkgName = (String) candCap.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE);

            List<Requirement> blameReqs = Collections.singletonList(currentReq);

            List<Blame> blames = packages.getOrCompute(pkgName);
            blames.add(new Blame(candCap, blameReqs));

//dumpResourcePkgs(current, currentPkgs);
        }
    }

    private void mergeUses(
        ResolveSession session, Resource current, Packages currentPkgs,
        Capability mergeCap, List<Requirement> blameReqs, Capability matchingCap,
        Map<Resource, Packages> resourcePkgMap,
        Set<Capability> cycleMap)
    {
        // If there are no uses, then just return.
        // If the candidate resource is the same as the current resource,
        // then we don't need to verify and merge the uses constraints
        // since this will happen as we build up the package space.
        if (current.equals(mergeCap.getResource()))
        {
            return;
        }

        // Check for cycles.
        if (!cycleMap.add(mergeCap))
        {
            return;
        }

        for (Capability candSourceCap : getPackageSources(mergeCap, resourcePkgMap))
        {
            List<String> uses;
// TODO: RFC-112 - Need impl-specific type
//            if (candSourceCap instanceof FelixCapability)
//            {
//                uses = ((FelixCapability) candSourceCap).getUses();
//            }
//            else
            {
                String s = candSourceCap.getDirectives().get(Namespace.CAPABILITY_USES_DIRECTIVE);
                if (s != null && s.length() > 0)
                {
                    // Parse these uses directive.
                    uses = session.getUsesCache().get(s);
                    if (uses == null)
                    {
                        uses = parseUses(s);
                        session.getUsesCache().put(s, uses);
                    }
                }
                else
                {
                    continue;
                }
            }
            Packages candSourcePkgs = resourcePkgMap.get(candSourceCap.getResource());
            for (String usedPkgName : uses)
            {
                List<Blame> candSourceBlames;
                // Check to see if the used package is exported.
                Blame candExportedBlame = candSourcePkgs.m_exportedPkgs.get(usedPkgName);
                if (candExportedBlame != null)
                {
                    candSourceBlames = Collections.singletonList(candExportedBlame);
                }
                else
                {
                    // If the used package is not exported, check to see if it
                    // is required.
                    candSourceBlames = candSourcePkgs.m_requiredPkgs.get(usedPkgName);
                    // Lastly, if the used package is not required, check to see if it
                    // is imported.
                    if (candSourceBlames == null)
                    {
                        candSourceBlames = candSourcePkgs.m_importedPkgs.get(usedPkgName);
                    }
                }

                // If the used package cannot be found, then just ignore it
                // since it has no impact.
                if (candSourceBlames == null)
                {
                    continue;
                }

                ArrayMap<Set<Capability>, UsedBlames> usedPkgBlames = currentPkgs.m_usedPkgs.getOrCompute(usedPkgName);
                List<Blame> newBlames = new ArrayList<Blame>();
                for (Blame blame : candSourceBlames)
                {
                    List<Requirement> newBlameReqs;
                    if (blame.m_reqs != null)
                    {
                        newBlameReqs = new ArrayList<Requirement>(blameReqs.size() + 1);
                        newBlameReqs.addAll(blameReqs);
                        // Only add the last requirement in blame chain because
                        // that is the requirement wired to the blamed capability
                        newBlameReqs.add(blame.m_reqs.get(blame.m_reqs.size() - 1));
                    }
                    else
                    {
                        newBlameReqs = blameReqs;
                    }
                    newBlames.add(new Blame(blame.m_cap, newBlameReqs));
                }
                addUsedBlames(usedPkgBlames, newBlames, matchingCap, resourcePkgMap);
                for (Blame newBlame : newBlames)
                {
                    mergeUses(session, current, currentPkgs, newBlame.m_cap, newBlame.m_reqs, matchingCap,
                        resourcePkgMap, cycleMap);
                }
            }
        }
    }

    private Map<Resource, Packages> calculatePackageSpaces(
            final ResolveSession session,
            final Candidates allCandidates,
            Collection<Resource> hosts)
    {
        final EnhancedExecutor executor = new EnhancedExecutor(session.getExecutor());

        // Parallel compute wire candidates
        final Map<Resource, List<WireCandidate>> allWireCandidates = new ConcurrentHashMap<Resource, List<WireCandidate>>();
        {
            final ConcurrentMap<Resource, Runnable> tasks = new ConcurrentHashMap<Resource, Runnable>(allCandidates.getNbResources());
            class Computer implements Runnable
            {
                final Resource resource;
                public Computer(Resource resource)
                {
                    this.resource = resource;
                }
                public void run()
                {
                    List<WireCandidate> wireCandidates = getWireCandidates(session, allCandidates, resource);
                    allWireCandidates.put(resource, wireCandidates);
                    for (WireCandidate w : wireCandidates)
                    {
                        Resource u = w.capability.getResource();
                        if (!tasks.containsKey(u))
                        {
                            Computer c = new Computer(u);
                            if (tasks.putIfAbsent(u, c) == null)
                            {
                                executor.execute(c);
                            }
                        }
                    }
                }
            }
            for (Resource resource : hosts)
            {
                executor.execute(new Computer(resource));
            }
            executor.await();
        }

        // Parallel get all exported packages
        final OpenHashMap<Resource, Packages> allPackages = new OpenHashMap<Resource, Packages>(allCandidates.getNbResources());
        for (final Resource resource : allWireCandidates.keySet())
        {
            final Packages packages = new Packages(resource);
            allPackages.put(resource, packages);
            executor.execute(new Runnable()
            {
                public void run()
                {
                    calculateExportedPackages(session, allCandidates, resource,
                        packages.m_exportedPkgs, packages.m_substitePkgs);
                }
            });
        }
        executor.await();

        // Parallel compute package lists
        for (final Resource resource : allWireCandidates.keySet())
        {
            executor.execute(new Runnable()
            {
                public void run()
                {
                    getPackages(session, allCandidates, allWireCandidates, allPackages, resource, allPackages.get(resource));
                }
            });
        }
        executor.await();

        // Compute package sources
        // First, sequentially compute packages for resources
        // that have required packages, so that all recursive
        // calls can be done without threading problems
        for (Map.Entry<Resource, Packages> entry : allPackages.fast())
        {
            final Resource resource = entry.getKey();
            final Packages packages = entry.getValue();
            if (!packages.m_requiredPkgs.isEmpty())
            {
                getPackageSourcesInternal(session, allPackages, resource, packages);
            }
        }
        // Next, for all remaining resources, we can compute them
        // in parallel, as they won't refer to other resource packages
        for (Map.Entry<Resource, Packages> entry : allPackages.fast())
        {
            final Resource resource = entry.getKey();
            final Packages packages = entry.getValue();
            if (packages.m_sources.isEmpty())
            {
                executor.execute(new Runnable()
                {
                    public void run()
                    {
                        getPackageSourcesInternal(session, allPackages, resource, packages);
                    }
                });
            }
        }
        executor.await();

        // Parallel compute uses
        for (final Resource resource : allWireCandidates.keySet())
        {
            executor.execute(new Runnable()
            {
                public void run()
                {
                    computeUses(session, allWireCandidates, allPackages, resource);
                }
            });
        }
        executor.await();

        return allPackages;
    }

    private static List<String> parseUses(String s) {
        int nb = 1;
        int l = s.length();
        for (int i = 0; i < l; i++) {
            if (s.charAt(i) == ',') {
                nb++;
            }
        }
        List<String> uses = new ArrayList<String>(nb);
        int start = 0;
        while (true) {
            while (start < l) {
                char c = s.charAt(start);
                if (c != ' ' && c != ',') {
                    break;
                }
                start++;
            }
            int end = start + 1;
            while (end < l) {
                char c = s.charAt(end);
                if (c == ' ' || c == ',') {
                    break;
                }
                end++;
            }
            if (start < l) {
                uses.add(s.substring(start, end));
                start = end + 1;
            } else {
                break;
            }
        }
        return uses;
    }

    private void addUsedBlames(
        ArrayMap<Set<Capability>, UsedBlames> usedBlames, Collection<Blame> blames, Capability matchingCap, Map<Resource, Packages> resourcePkgMap)
    {
        Set<Capability> usedCaps;
        if (blames.size() == 1)
        {
            usedCaps = getPackageSources(blames.iterator().next().m_cap, resourcePkgMap);
        }
        else
        {
            usedCaps = new HashSet<Capability>();
            for (Blame blame : blames)
            {
                usedCaps.addAll(getPackageSources(blame.m_cap, resourcePkgMap));
            }
        }
        if (usedCaps.isEmpty())
        {
            // This most likely is an issue with the resolve context.
            // To avoid total failure we do not add blames if there is
            // no source capabilities
            m_logger.log(Logger.LOG_INFO,
                "Package sources are empty for used capability: " + blames);
            return;
        }
        // Find UsedBlame that uses the same capability as the new blame.
        UsedBlames addToBlame = usedBlames.getOrCompute(usedCaps);
        // Add the new Blames and record the matching capability cause
        // in case the root requirement has multiple cardinality.
        for (Blame blame : blames)
        {
            addToBlame.addBlame(blame, matchingCap);
        }
    }

    private ResolutionError checkPackageSpaceConsistency(
        ResolveSession session,
        Resource resource,
        Candidates allCandidates,
        boolean dynamic,
        Map<Resource, Packages> resourcePkgMap,
        Map<Resource, Object> resultCache)
    {
        if (!dynamic && session.getContext().getWirings().containsKey(resource))
        {
            return null;
        }
        Object cache = resultCache.get(resource);
        if (cache != null)
        {
            return cache instanceof ResolutionError ? (ResolutionError) cache : null;
        }

        Packages pkgs = resourcePkgMap.get(resource);

        ResolutionError rethrow = null;

        // Check for conflicting imports from fragments.
        // TODO: Is this only needed for imports or are generic and bundle requirements also needed?
        //       I think this is only a special case for fragment imports because they can overlap
        //       host imports, which is not allowed in normal metadata.
        for (Entry<String, List<Blame>> entry : pkgs.m_importedPkgs.fast())
        {
            String pkgName = entry.getKey();
            List<Blame> blames = entry.getValue();
            if (blames.size() > 1)
            {
                Blame sourceBlame = null;
                for (Blame blame : blames)
                {
                    if (sourceBlame == null)
                    {
                        sourceBlame = blame;
                    }
                    else if (!sourceBlame.m_cap.getResource().equals(blame.m_cap.getResource()))
                    {
                        // Try to permutate the conflicting requirement.
                        session.addPermutation(PermutationType.IMPORT, allCandidates.permutate(blame.m_reqs.get(0)));
                        // Try to permutate the source requirement.
                        session.addPermutation(PermutationType.IMPORT, allCandidates.permutate(sourceBlame.m_reqs.get(0)));
                        // Report conflict.
                        rethrow = new UseConstraintError(
                                session.getContext(), allCandidates,
                                resource, pkgName,
                                sourceBlame, blame);
                        if (m_logger.isDebugEnabled())
                        {
                            m_logger.debug(
                                    "Candidate permutation failed due to a conflict with a "
                                            + "fragment import; will try another if possible."
                                            + " (" + rethrow.getMessage() + ")");
                        }
                        return rethrow;
                    }
                }
            }
        }
        // IMPLEMENTATION NOTE:
        // Below we track the mutated reqs that have been permuted
        // in a single candidates permutation.  This permutation may contain a
        // delta of several reqs which conflict with a directly imported/required candidates.
        // When several reqs are permuted at the same time this reduces the number of solutions tried.
        // See the method Candidates::canRemoveCandidate for a case where substitutions must be checked
        // because of this code that may permute multiple reqs in on candidates permutation.
        AtomicReference<Candidates> permRef1 = new AtomicReference<Candidates>();
        AtomicReference<Candidates> permRef2 = new AtomicReference<Candidates>();
        Set<Requirement> mutated = null;

        // Check if there are any uses conflicts with exported packages.
        for (Entry<String, Blame> entry : pkgs.m_exportedPkgs.fast())
        {
            String pkgName = entry.getKey();
            Blame exportBlame = entry.getValue();
            ArrayMap<Set<Capability>, UsedBlames> pkgBlames = pkgs.m_usedPkgs.get(pkgName);
            if (pkgBlames == null)
            {
                continue;
            }
            for (UsedBlames usedBlames : pkgBlames.values())
            {
                if (!isCompatible(exportBlame, usedBlames.m_caps, resourcePkgMap))
                {
                    mutated = (mutated != null)
                            ? mutated
                            : new HashSet<Requirement>();
                    rethrow = permuteUsedBlames(session, rethrow, allCandidates, resource,
                            pkgName, null, usedBlames, permRef1, permRef2, mutated);
                }
            }

            if (rethrow != null)
            {
                if (!mutated.isEmpty())
                {
                    session.addPermutation(PermutationType.USES, permRef1.get());
                    session.addPermutation(PermutationType.USES, permRef2.get());
                }
                if (m_logger.isDebugEnabled())
                {
                    m_logger.debug("Candidate permutation failed due to a conflict between "
                            + "an export and import; will try another if possible."
                            + " (" + rethrow.getMessage() + ")");
                }
                return rethrow;
            }
        }

        // Check if there are any uses conflicts with imported and required packages.
        // We combine the imported and required packages here into one map.
        // Imported packages are added after required packages because they shadow or override
        // the packages from required bundles.
        OpenHashMap<String, List<Blame>> allImportRequirePkgs;
        if (pkgs.m_requiredPkgs.isEmpty())
        {
            allImportRequirePkgs = pkgs.m_importedPkgs;
        }
        else
        {
            allImportRequirePkgs = new OpenHashMap<String, List<Blame>>(pkgs.m_requiredPkgs.size() + pkgs.m_importedPkgs.size());
            allImportRequirePkgs.putAll(pkgs.m_requiredPkgs);
            allImportRequirePkgs.putAll(pkgs.m_importedPkgs);
        }

        for (Entry<String, List<Blame>> entry : allImportRequirePkgs.fast())
        {
            String pkgName = entry.getKey();
            ArrayMap<Set<Capability>, UsedBlames> pkgBlames = pkgs.m_usedPkgs.get(pkgName);
            if (pkgBlames == null)
            {
                continue;
            }
            List<Blame> requirementBlames = entry.getValue();

            for (UsedBlames usedBlames : pkgBlames.values())
            {
                if (!isCompatible(requirementBlames, usedBlames.m_caps, resourcePkgMap))
                {
                    mutated = (mutated != null)
                            ? mutated
                            : new HashSet<Requirement>();// Split packages, need to think how to get a good message for split packages (sigh)
                    // For now we just use the first requirement that brings in the package that conflicts
                    Blame requirementBlame = requirementBlames.get(0);
                    rethrow = permuteUsedBlames(session, rethrow, allCandidates, resource, pkgName, requirementBlame, usedBlames, permRef1, permRef2, mutated);
                }

                // If there was a uses conflict, then we should add a uses
                // permutation if we were able to permutate any candidates.
                // Additionally, we should try to push an import permutation
                // for the original import to force a backtracking on the
                // original candidate decision if no viable candidate is found
                // for the conflicting uses constraint.
                if (rethrow != null)
                {
                    // Add uses permutation if we m_mutated any candidates.
                    if (!mutated.isEmpty())
                    {
                        session.addPermutation(PermutationType.USES, permRef1.get());
                        session.addPermutation(PermutationType.USES, permRef2.get());
                    }

                    // Try to permutate the candidate for the original
                    // import requirement; only permutate it if we haven't
                    // done so already.
                    for (Blame requirementBlame : requirementBlames)
                    {
                        Requirement req = requirementBlame.m_reqs.get(0);
                        if (!mutated.contains(req))
                        {
                            // Since there may be lots of uses constraint violations
                            // with existing import decisions, we may end up trying
                            // to permutate the same import a lot of times, so we should
                            // try to check if that the case and only permutate it once.
                            session.permutateIfNeeded(PermutationType.IMPORT, req, allCandidates);
                        }
                    }

                    if (m_logger.isDebugEnabled())
                    {
                        m_logger.debug("Candidate permutation failed due to a conflict between "
                                        + "imports; will try another if possible."
                                        + " (" + rethrow.getMessage() + ")"
                        );
                    }
                    return rethrow;
                }
            }
        }

        resultCache.put(resource, Boolean.TRUE);

        // Now check the consistency of all resources on which the
        // current resource depends. Keep track of the current number
        // of permutations so we know if the lower level check was
        // able to create a permutation or not in the case of failure.
        long permCount = session.getPermutationCount();
        for (Requirement req : resource.getRequirements(null))
        {
            Capability cap = allCandidates.getFirstCandidate(req);
            if (cap != null)
            {
                if (!resource.equals(cap.getResource()))
                {
                    rethrow = checkPackageSpaceConsistency(
                            session, cap.getResource(),
                            allCandidates, false, resourcePkgMap, resultCache);
                    if (session.isCancelled()) {
                        return null;
                    }
                    if (rethrow != null)
                    {
                        // If the lower level check didn't create any permutations,
                        // then we should create an import permutation for the
                        // requirement with the dependency on the failing resource
                        // to backtrack on our current candidate selection.
                        if (permCount == session.getPermutationCount())
                        {
                            session.addPermutation(PermutationType.IMPORT, allCandidates.permutate(req));
                        }
                        return rethrow;
                    }
                }
            }
        }
        return null;
    }
    
    private ResolutionError permuteUsedBlames(ResolveSession session,
          ResolutionError rethrow, Candidates allCandidates, Resource resource,
          String pkgName, Blame requirementBlame, UsedBlames usedBlames,
          AtomicReference<Candidates> permRef1, AtomicReference<Candidates> permRef2,
          Set<Requirement> mutated)
    {
        for (Blame usedBlame : usedBlames.m_blames)
        {
            if (session.checkMultiple(usedBlames, usedBlame, allCandidates))
            {
                // Continue to the next usedBlame, if possible we
                // removed the conflicting candidates.
                continue;
            }

            if (rethrow == null)
            {
                if (requirementBlame == null)
                {
                    rethrow = new UseConstraintError(session.getContext(), allCandidates,
                            resource, pkgName, usedBlame);
                }
                else
                {
                    rethrow = new UseConstraintError(session.getContext(), allCandidates,
                            resource, pkgName, requirementBlame, usedBlame);
                }
            }

            // Create a candidate permutation that eliminates all candidates
            // that conflict with existing selected candidates going from direct requirement -> root
            Candidates perm1 = permRef1.get();
            if (perm1 == null)
            {
                perm1 = allCandidates.copy();
                permRef1.set(perm1);
            }
            for (int reqIdx = usedBlame.m_reqs.size() - 1; reqIdx >= 0; reqIdx--)
            {
                Requirement req = usedBlame.m_reqs.get(reqIdx);
                if (permuteUsedBlameRequirement(req, mutated, perm1))
                {
                    break;
                }
            }
            // Create a candidate permutation that eliminates all candidates
            // that conflict with existing selected candidates going from root -> direct requirement
            Candidates perm2 = permRef2.get();
            if (perm2 == null)
            {
                perm2 = allCandidates.copy();
                permRef2.set(perm2);
            }
            for (int reqIdx = 0; reqIdx < usedBlame.m_reqs.size(); reqIdx++)
            {
                Requirement req = usedBlame.m_reqs.get(reqIdx);
                if (permuteUsedBlameRequirement(req, mutated, perm2))
                {
                    break;
                }
            }
        }
        return rethrow;
    }

    private boolean permuteUsedBlameRequirement(Requirement req, Set<Requirement> mutated, Candidates permutation)
    {
        // Sanity check for multiple.
        if (Util.isMultiple(req))
        {
            return false;
        }
        // If we've already permutated this requirement in another
        // uses constraint, don't permutate it again just continue
        // with the next uses constraint.
        if (mutated.contains(req))
        {
            return true;
        }

        // See if we can permutate the candidates for blamed
        // requirement; there may be no candidates if the resource
        // associated with the requirement is already resolved.
        if (permutation.canRemoveCandidate(req))
        {
            permutation.removeFirstCandidate(req);
            mutated.add(req);
            return true;
        }
        return false;
    }
    
    private static OpenHashMap<String, Blame> calculateExportedPackages(
            ResolveSession session,
            Candidates allCandidates,
            Resource resource,
        OpenHashMap<String, Blame> exports, OpenHashMap<String, Blame> substitutes)
    {
        // Get all exported packages.
        Wiring wiring = session.getContext().getWirings().get(resource);
        List<Capability> caps = (wiring != null)
            ? wiring.getResourceCapabilities(null)
            : resource.getCapabilities(null);
        for (Capability cap : caps)
        {
            if (cap.getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE))
            {
                if (!cap.getResource().equals(resource))
                {
                    cap = new WrappedCapability(resource, cap);
                }
                exports.put(
                    (String) cap.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE),
                    new Blame(cap, null));
            }
        }
        // Remove substitutable exports that were imported.
        // For resolved resources Wiring.getCapabilities()
        // already excludes imported substitutable exports, but
        // for resolving resources we must look in the candidate
        // map to determine which exports are substitutable.
        if (wiring != null)
        {
            for (Wire wire : session.getContext().getSubstitutionWires(wiring))
            {
                Capability cap = wire.getCapability();
                if (!cap.getResource().equals(wire.getProvider()))
                {
                    cap = new WrappedCapability(wire.getProvider(), cap);
                }
                substitutes.put(
                    // Using a null on requirement instead of the wire requirement here.
                    // It is unclear if we want to treat the substitution requirement as a permutation req here.
                    (String) cap.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE),
                    new Blame(cap, null));
            }
        }
        else
        {
            if (!exports.isEmpty())
            {
                for (Requirement req : resource.getRequirements(null))
                {
                    if (req.getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE))
                    {
                        Capability cand = allCandidates.getFirstCandidate(req);
                        if (cand != null)
                        {
                            String pkgName = (String) cand.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE);
                            Blame blame = exports.remove(pkgName);
                            if (blame != null)
                            {
                                // Using a null on requirement instead of the wire requirement here.
                                // It is unclear if we want to treat the substitution requirement as a permutation req here.
                                substitutes.put(pkgName, new Blame(cand, null));
                            }
                        }
                    }
                }
            }
        }
        return exports;
    }

    private static boolean isCompatible(
        Blame currentBlame, Set<Capability> candSources,
        Map<Resource, Packages> resourcePkgMap)
    {
        if (candSources.contains(currentBlame.m_cap))
        {
            return true;
        }
        Set<Capability> currentSources = getPackageSources(currentBlame.m_cap, resourcePkgMap);
        return currentSources.containsAll(candSources)
                || candSources.containsAll(currentSources);
    }

    private static boolean isCompatible(
        List<Blame> currentBlames, Set<Capability> candSources,
        Map<Resource, Packages> resourcePkgMap)
    {
        int size = currentBlames.size();
        switch (size)
        {
        case 0:
            return true;
        case 1:
            return isCompatible(currentBlames.get(0), candSources, resourcePkgMap);
        default:
            Set<Capability> currentSources = new HashSet<Capability>(currentBlames.size());
            for (Blame currentBlame : currentBlames)
            {
                Set<Capability> blameSources = getPackageSources(currentBlame.m_cap, resourcePkgMap);
                currentSources.addAll(blameSources);
            }
            return currentSources.containsAll(candSources)
                || candSources.containsAll(currentSources);
        }
    }

    private static Set<Capability> getPackageSources(
            Capability cap, Map<Resource, Packages> resourcePkgMap)
    {
        Resource resource = cap.getResource();
        if(resource == null)
        {
            return new HashSet<Capability>();
        }

        OpenHashMap<Capability, Set<Capability>> sources = resourcePkgMap.get(resource).m_sources;
        if(sources == null)
        {
            return new HashSet<Capability>();
        }

        Set<Capability> packageSources = sources.get(cap);
        if(packageSources == null)
        {
            return new HashSet<Capability>();
        }

        return packageSources;
    }

    private static void getPackageSourcesInternal(
        ResolveSession session, Map<Resource, Packages> resourcePkgMap,
        Resource resource, Packages packages)
    {
        Wiring wiring = session.getContext().getWirings().get(resource);
        List<Capability> caps = (wiring != null)
                ? wiring.getResourceCapabilities(null)
                : resource.getCapabilities(null);
        @SuppressWarnings("serial")
        OpenHashMap<String, Set<Capability>> pkgs = new OpenHashMap<String, Set<Capability>>(caps.size()) {
            public Set<Capability> compute(String pkgName) {
                return new HashSet<Capability>();
            }
        };
        Map<Capability, Set<Capability>> sources = packages.m_sources;
        for (Capability sourceCap : caps)
        {
            if (sourceCap.getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE))
            {
                String pkgName = (String) sourceCap.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE);
                Set<Capability> pkgCaps = pkgs.getOrCompute(pkgName);
                // Since capabilities may come from fragments, we need to check
                // for that case and wrap them.
                if (!resource.equals(sourceCap.getResource()))
                {
                    sourceCap = new WrappedCapability(resource, sourceCap);
                }
                sources.put(sourceCap, pkgCaps);
                pkgCaps.add(sourceCap);
            }
            else
            {
                // Otherwise, need to return generic capabilities that have
                // uses constraints so they are included for consistency
                // checking.
                String uses = sourceCap.getDirectives().get(Namespace.CAPABILITY_USES_DIRECTIVE);
                if ((uses != null) && uses.length() > 0)
                {
                    sources.put(sourceCap, Collections.singleton(sourceCap));
                }
                else
                {
                    sources.put(sourceCap, Collections.<Capability>emptySet());
                }
            }
        }
        for (Map.Entry<String, Set<Capability>> pkg : pkgs.fast())
        {
            String pkgName = pkg.getKey();
            List<Blame> required = packages.m_requiredPkgs.get(pkgName);
            if (required != null)
            {
                Set<Capability> srcs = pkg.getValue();
                for (Blame blame : required)
                {
                    Capability bcap = blame.m_cap;
                    if (srcs.add(bcap))
                    {
                        Resource capResource = bcap.getResource();
                        Packages capPackages = resourcePkgMap.get(capResource);
                        Set<Capability> additional = capPackages.m_sources.get(bcap);
                        if (additional == null)
                        {
                            getPackageSourcesInternal(session, resourcePkgMap, capResource, capPackages);
                            additional = capPackages.m_sources.get(bcap);
                        }
                        srcs.addAll(additional);
                    }
                }
            }
        }
    }

    private static Resource getDeclaredResource(Resource resource)
    {
        if (resource instanceof WrappedResource)
        {
            return ((WrappedResource) resource).getDeclaredResource();
        }
        return resource;
    }

    private static Capability getDeclaredCapability(Capability c)
    {
        if (c instanceof HostedCapability)
        {
            return ((HostedCapability) c).getDeclaredCapability();
        }
        return c;
    }

    private static Requirement getDeclaredRequirement(Requirement r)
    {
        if (r instanceof WrappedRequirement)
        {
            return ((WrappedRequirement) r).getDeclaredRequirement();
        }
        return r;
    }

    private static Map<Resource, List<Wire>> populateWireMap(
        ResolveSession session, Resource resource,
        Map<Resource, List<Wire>> wireMap, Candidates allCandidates)
    {
        Resource unwrappedResource = getDeclaredResource(resource);
        if (!session.getContext().getWirings().containsKey(unwrappedResource)
            && !wireMap.containsKey(unwrappedResource))
        {
            wireMap.put(unwrappedResource, Collections.<Wire>emptyList());

            List<Wire> packageWires = new ArrayList<Wire>();
            List<Wire> bundleWires = new ArrayList<Wire>();
            List<Wire> capabilityWires = new ArrayList<Wire>();

            for (Requirement req : resource.getRequirements(null))
            {
                List<Capability> cands = allCandidates.getCandidates(req);
                if ((cands != null) && (cands.size() > 0))
                {
                    for (Capability cand : cands)
                    {
                        // Do not create wires for the osgi.wiring.* namespaces
                        // if the provider and requirer are the same resource;
                        // allow such wires for non-OSGi wiring namespaces.
                        if (!cand.getNamespace().startsWith("osgi.wiring.")
                            || !resource.equals(cand.getResource()))
                        {
                            // Populate wires for the candidate
                            populateWireMap(session, cand.getResource(),
                                    wireMap, allCandidates);

                            Resource provider;
                            if (req.getNamespace().equals(IdentityNamespace.IDENTITY_NAMESPACE)) {
                                provider = getDeclaredCapability(cand).getResource();
                            } else {
                                provider = getDeclaredResource(cand.getResource());
                            }
                            Wire wire = new WireImpl(
                                unwrappedResource,
                                getDeclaredRequirement(req),
                                provider,
                                getDeclaredCapability(cand));
                            if (req.getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE))
                            {
                                packageWires.add(wire);
                            }
                            else if (req.getNamespace().equals(BundleNamespace.BUNDLE_NAMESPACE))
                            {
                                bundleWires.add(wire);
                            }
                            else
                            {
                                capabilityWires.add(wire);
                            }
                        }
                        if (!Util.isMultiple(req))
                        {
                            // If not multiple just create a wire for the first candidate.
                            break;
                        }
                    }
                }
            }

            // Combine package wires with require wires last.
            packageWires.addAll(bundleWires);
            packageWires.addAll(capabilityWires);
            wireMap.put(unwrappedResource, packageWires);

            // Add host wire for any fragments.
            if (resource instanceof WrappedResource)
            {
                List<Resource> fragments = ((WrappedResource) resource).getFragments();
                for (Resource fragment : fragments)
                {
                    // Get wire list for the fragment from the wire map.
                    // If there isn't one, then create one. Note that we won't
                    // add the wire list to the wire map until the end, so
                    // we can determine below if this is the first time we've
                    // seen the fragment while populating wires to avoid
                    // creating duplicate non-payload wires if the fragment
                    // is attached to more than one host.
                    List<Wire> fragmentWires = wireMap.get(fragment);
                    fragmentWires = (fragmentWires == null)
                        ? new ArrayList<Wire>() : fragmentWires;

                    // Loop through all of the fragment's requirements and create
                    // any necessary wires for non-payload requirements.
                    for (Requirement req : fragment.getRequirements(null))
                    {
                        // Only look at non-payload requirements.
                        if (!isPayload(req))
                        {
                            // If this is the host requirement, then always create
                            // a wire for it to the current resource.
                            if (req.getNamespace().equals(HostNamespace.HOST_NAMESPACE))
                            {
                                fragmentWires.add(
                                    new WireImpl(
                                        getDeclaredResource(fragment),
                                        req,
                                        unwrappedResource,
                                        unwrappedResource.getCapabilities(
                                            HostNamespace.HOST_NAMESPACE).get(0)));
                            }
                            // Otherwise, if the fragment isn't already resolved and
                            // this is the first time we are seeing it, then create
                            // a wire for the non-payload requirement.
                            else if (!session.getContext().getWirings().containsKey(fragment)
                                && !wireMap.containsKey(fragment))
                            {
                                Wire wire = createWire(req, allCandidates);
                                if (wire != null)
                                {
                                    fragmentWires.add(wire);
                                }
                            }
                        }
                    }

                    // Finally, add the fragment's wire list to the wire map.
                    wireMap.put(fragment, fragmentWires);
                }
            }
            // now make sure any related resources are populated
            for (Resource related : session.getRelatedResources(unwrappedResource)) {
                if (allCandidates.isPopulated(related)) {
                    populateWireMap(session, related, wireMap, allCandidates);
                }
            }
        }

        return wireMap;
    }

    private static Wire createWire(Requirement requirement, Candidates allCandidates)
    {
        Capability cand = allCandidates.getFirstCandidate(requirement);
        if (cand == null) {
            return null;
        }
        return new WireImpl(
            getDeclaredResource(requirement.getResource()),
            getDeclaredRequirement(requirement),
            getDeclaredResource(cand.getResource()),
            getDeclaredCapability(cand));
    }

    private static boolean isPayload(Requirement fragmentReq)
    {
        // this is where we would add other non-payload namespaces
        if (ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE
            .equals(fragmentReq.getNamespace()))
        {
            return false;
        }
        if (HostNamespace.HOST_NAMESPACE.equals(fragmentReq.getNamespace()))
        {
            return false;
        }
        return true;
    }

    private static Map<Resource, List<Wire>> populateDynamicWireMap(
        ResolveSession session, Map<Resource,
        List<Wire>> wireMap, Candidates allCandidates)
    {
        wireMap.put(session.getDynamicHost(), Collections.<Wire>emptyList());

        List<Wire> packageWires = new ArrayList<Wire>();

        // Get the candidates for the current dynamic requirement.
        // Record the dynamic candidate.
        Capability dynCand = allCandidates.getFirstCandidate(session.getDynamicRequirement());

        if (!session.getContext().getWirings().containsKey(dynCand.getResource()))
        {
            populateWireMap(session, dynCand.getResource(),
                wireMap, allCandidates);
        }

        packageWires.add(
            new WireImpl(
                session.getDynamicHost(),
                session.getDynamicRequirement(),
                getDeclaredResource(dynCand.getResource()),
                getDeclaredCapability(dynCand)));

        wireMap.put(session.getDynamicHost(), packageWires);

        return wireMap;
    }

    @SuppressWarnings("unused")
    private static void dumpResourcePkgMap(
        ResolveContext rc, Map<Resource, Packages> resourcePkgMap)
    {
        System.out.println("+++RESOURCE PKG MAP+++");
        for (Entry<Resource, Packages> entry : resourcePkgMap.entrySet())
        {
            dumpResourcePkgs(rc, entry.getKey(), entry.getValue());
        }
    }

    private static void dumpResourcePkgs(
        ResolveContext rc, Resource resource, Packages packages)
    {
        Wiring wiring = rc.getWirings().get(resource);
        System.out.println(resource
            + " (" + ((wiring != null) ? "RESOLVED)" : "UNRESOLVED)"));
        System.out.println("  EXPORTED");
        for (Entry<String, Blame> entry : packages.m_exportedPkgs.entrySet())
        {
            System.out.println("    " + entry.getKey() + " - " + entry.getValue());
        }
        System.out.println("  IMPORTED");
        for (Entry<String, List<Blame>> entry : packages.m_importedPkgs.entrySet())
        {
            System.out.println("    " + entry.getKey() + " - " + entry.getValue());
        }
        System.out.println("  REQUIRED");
        for (Entry<String, List<Blame>> entry : packages.m_requiredPkgs.entrySet())
        {
            System.out.println("    " + entry.getKey() + " - " + entry.getValue());
        }
        System.out.println("  USED");
        for (Entry<String, ArrayMap<Set<Capability>, UsedBlames>> entry : packages.m_usedPkgs.entrySet())
        {
            System.out.println("    " + entry.getKey() + " - " + entry.getValue().values());
        }
    }

    private static final class WireCandidate
    {
        public final Requirement requirement;
        public final Capability capability;

        public WireCandidate(Requirement requirement, Capability capability)
        {
            this.requirement = requirement;
            this.capability = capability;
        }
    }

    public static class Packages
    {
        public final OpenHashMap<String, Blame> m_exportedPkgs;
        public final OpenHashMap<String, Blame> m_substitePkgs;
        public final OpenHashMap<String, List<Blame>> m_importedPkgs;
        public final OpenHashMap<String, List<Blame>> m_requiredPkgs;
        public final OpenHashMap<String, ArrayMap<Set<Capability>, UsedBlames>> m_usedPkgs;
        public final OpenHashMap<Capability, Set<Capability>> m_sources;

        @SuppressWarnings("serial")
        public Packages(Resource resource)
        {
            int nbCaps = resource.getCapabilities(null).size();
            int nbReqs = resource.getRequirements(null).size();

            m_exportedPkgs = new OpenHashMap<String, Blame>(nbCaps);
            m_substitePkgs = new OpenHashMap<String, Blame>(nbCaps);
            m_importedPkgs = new OpenHashMap<String, List<Blame>>(nbReqs) {
                public List<Blame> compute(String s) {
                    return new ArrayList<Blame>();
                }
            };
            m_requiredPkgs = new OpenHashMap<String, List<Blame>>(nbReqs) {
                public List<Blame> compute(String s) {
                    return new ArrayList<Blame>();
                }
            };
            m_usedPkgs = new OpenHashMap<String, ArrayMap<Set<Capability>, UsedBlames>>(128) {
                @Override
                protected ArrayMap<Set<Capability>, UsedBlames> compute(String s) {
                    return new ArrayMap<Set<Capability>, UsedBlames>() {
                        @Override
                        protected UsedBlames compute(Set<Capability> key) {
                            return new UsedBlames(key);
                        }
                    };
                }
            };
            m_sources = new OpenHashMap<Capability, Set<Capability>>(nbCaps);
        }
    }

    private static class Blame
    {
        public final Capability m_cap;
        public final List<Requirement> m_reqs;

        public Blame(Capability cap, List<Requirement> reqs)
        {
            m_cap = cap;
            m_reqs = reqs;
        }

        @Override
        public String toString()
        {
            return m_cap.getResource()
                + "." + m_cap.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE)
                + (((m_reqs == null) || m_reqs.isEmpty())
                ? " NO BLAME"
                : " BLAMED ON " + m_reqs);
        }

        @Override
        public boolean equals(Object o)
        {
            return (o instanceof Blame) && m_reqs.equals(((Blame) o).m_reqs)
                && m_cap.equals(((Blame) o).m_cap);
        }
    }

    /*
     * UsedBlames hold a list of Blame that have a common used capability.
     * The UsedBlames stores sets of capabilities (root causes) that match a
     * root requirement with multiple cardinality.  These causes are the
     * capabilities that pulled in the common used capability.
     * It is assumed that multiple cardinality requirements can only be
     * root requirements of a Blame.
     *
     * This is only true because capabilities can only use a package
     * capability.  They cannot use any other kind of capability so we
     * do not have to worry about transitivity of the uses directive
     * from other capability types.
     */
    private static class UsedBlames
    {
        public final Set<Capability> m_caps;
        public final List<Blame> m_blames = new ArrayList<ResolverImpl.Blame>();
        private Map<Requirement, Set<Capability>> m_rootCauses;

        public UsedBlames(Set<Capability> caps)
        {
            m_caps = caps;
        }

        public void addBlame(Blame blame, Capability matchingRootCause)
        {
            if (!m_caps.contains(blame.m_cap))
            {
                throw new IllegalArgumentException(
                    "Attempt to add a blame with a different used capability: "
                    + blame.m_cap);
            }
            m_blames.add(blame);
            if (matchingRootCause != null)
            {
                Requirement req = blame.m_reqs.get(0);
                // Assumption made that the root requirement of the chain is the only
                // possible multiple cardinality requirement and that the matching root cause
                // capability is passed down from the beginning of the chain creation.
                if (Util.isMultiple(req))
                {
                    // The root requirement is multiple. Need to store the root cause
                    // so that we can find it later in case the used capability which the cause
                    // capability pulled in is a conflict.
                    if (m_rootCauses == null)
                    {
                        m_rootCauses = new HashMap<Requirement, Set<Capability>>();
                    }
                    Set<Capability> rootCauses = m_rootCauses.get(req);
                    if (rootCauses == null)
                    {
                        rootCauses = new HashSet<Capability>();
                        m_rootCauses.put(req, rootCauses);
                    }
                    rootCauses.add(matchingRootCause);
                }
            }
        }

        public Set<Capability> getRootCauses(Requirement req)
        {
            if (m_rootCauses == null)
            {
                return Collections.emptySet();
            }
            Set<Capability> result = m_rootCauses.get(req);
            return result == null ? Collections.<Capability>emptySet() : result;
        }

        @Override
        public String toString()
        {
            return m_blames.toString();
        }
    }

    private static final class UseConstraintError extends ResolutionError {

        private final ResolveContext m_context;
        private final Candidates m_allCandidates;
        private final Resource m_resource;
        private final String m_pkgName;
        private final Blame m_blame1;
        private final Blame m_blame2;

        public UseConstraintError(ResolveContext context, Candidates allCandidates, Resource resource, String pkgName, Blame blame) {
            this(context, allCandidates, resource, pkgName, blame, null);
        }

        public UseConstraintError(ResolveContext context, Candidates allCandidates, Resource resource, String pkgName, Blame blame1, Blame blame2) {
            this.m_context = context;
            this.m_allCandidates = allCandidates;
            this.m_resource = resource;
            this.m_pkgName = pkgName;
            if (blame1 == null)
            {
                throw new NullPointerException("First blame cannot be null.");
            }
            this.m_blame1 = blame1;
            this.m_blame2 = blame2;
        }

        public String getMessage() {
            if (m_blame2 == null)
            {
                return "Uses constraint violation. Unable to resolve resource "
                        + Util.getSymbolicName(m_resource)
                        + " [" + m_resource
                        + "] because it exports package '"
                        + m_pkgName
                        + "' and is also exposed to it from resource "
                        + Util.getSymbolicName(m_blame1.m_cap.getResource())
                        + " [" + m_blame1.m_cap.getResource()
                        + "] via the following dependency chain:\n\n"
                        + toStringBlame(m_blame1);
            }
            else
            {
                return  "Uses constraint violation. Unable to resolve resource "
                        + Util.getSymbolicName(m_resource)
                        + " [" + m_resource
                        + "] because it is exposed to package '"
                        + m_pkgName
                        + "' from resources "
                        + Util.getSymbolicName(m_blame1.m_cap.getResource())
                        + " [" + m_blame1.m_cap.getResource()
                        + "] and "
                        + Util.getSymbolicName(m_blame2.m_cap.getResource())
                        + " [" + m_blame2.m_cap.getResource()
                        + "] via two dependency chains.\n\nChain 1:\n"
                        + toStringBlame(m_blame1)
                        + "\n\nChain 2:\n"
                        + toStringBlame(m_blame2);
            }
        }

        public Collection<Requirement> getUnresolvedRequirements() {
            if (m_blame2 == null)
            {
                // This is an export conflict so there is only the first blame;
                // use its requirement.
                return Collections.singleton(m_blame1.m_reqs.get(0));
            }
            else
            {
                return Collections.singleton(m_blame2.m_reqs.get(0));
            }
        }

        private String toStringBlame(Blame blame)
        {
            StringBuilder sb = new StringBuilder();
            if ((blame.m_reqs != null) && !blame.m_reqs.isEmpty())
            {
                for (int i = 0; i < blame.m_reqs.size(); i++)
                {
                    Requirement req = blame.m_reqs.get(i);
                    sb.append("  ");
                    sb.append(Util.getSymbolicName(req.getResource()));
                    sb.append(" [");
                    sb.append(req.getResource().toString());
                    sb.append("]\n");
                    if (req.getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE))
                    {
                        sb.append("    import: ");
                    }
                    else
                    {
                        sb.append("    require: ");
                    }
                    sb.append(req.getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE));
                    sb.append("\n     |");
                    if (req.getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE))
                    {
                        sb.append("\n    export: ");
                    }
                    else
                    {
                        sb.append("\n    provide: ");
                    }
                    if ((i + 1) < blame.m_reqs.size())
                    {
                        Capability cap = getSatisfyingCapability(blame.m_reqs.get(i));
                        if (cap.getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE))
                        {
                            sb.append(PackageNamespace.PACKAGE_NAMESPACE);
                            sb.append("=");
                            sb.append(cap.getAttributes()
                                    .get(PackageNamespace.PACKAGE_NAMESPACE));
                            Capability usedCap =
                                    getSatisfyingCapability(blame.m_reqs.get(i + 1));
                            sb.append("; uses:=");
                            sb.append(usedCap.getAttributes()
                                    .get(PackageNamespace.PACKAGE_NAMESPACE));
                        }
                        else
                        {
                            sb.append(cap);
                        }
                        sb.append("\n");
                    }
                    else
                    {
                        Capability export = getSatisfyingCapability(blame.m_reqs.get(i));
                        sb.append(export.getNamespace());
                        sb.append(": ");
                        Object namespaceVal = export.getAttributes().get(export.getNamespace());
                        if (namespaceVal != null)
                        {
                            sb.append(namespaceVal.toString());
                        }
                        else
                        {
                            for (Entry<String, Object> attrEntry : export.getAttributes().entrySet())
                            {
                                sb.append(attrEntry.getKey()).append('=')
                                        .append(attrEntry.getValue()).append(';');
                            }
                        }
                        if (export.getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE)
                                && !export.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE)
                                .equals(blame.m_cap.getAttributes().get(
                                        PackageNamespace.PACKAGE_NAMESPACE)))
                        {
                            sb.append("; uses:=");
                            sb.append(blame.m_cap.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
                            sb.append("\n    export: ");
                            sb.append(PackageNamespace.PACKAGE_NAMESPACE);
                            sb.append("=");
                            sb.append(blame.m_cap.getAttributes()
                                    .get(PackageNamespace.PACKAGE_NAMESPACE));
                        }
                        sb.append("\n  ");
                        sb.append(Util.getSymbolicName(blame.m_cap.getResource()));
                        sb.append(" [");
                        sb.append(blame.m_cap.getResource().toString());
                        sb.append("]");
                    }
                }
            }
            else
            {
                sb.append(blame.m_cap.getResource().toString());
            }
            return sb.toString();
        }

        private Capability getSatisfyingCapability(Requirement req)
        {
            // If the requiring revision is not resolved, then check in the
            // candidate map for its matching candidate.
            Capability cap = m_allCandidates.getFirstCandidate(req);
            // Otherwise, if the requiring revision is resolved then check
            // in its wires for the capability satisfying the requirement.
            if (cap == null && m_context.getWirings().containsKey(req.getResource()))
            {
                List<Wire> wires =
                        m_context.getWirings().get(req.getResource()).getRequiredResourceWires(null);
                req = getDeclaredRequirement(req);
                for (Wire w : wires)
                {
                    if (w.getRequirement().equals(req))
                    {
                        // TODO: RESOLVER - This is not 100% correct, since requirements for
                        //       dynamic imports with wildcards will reside on many wires and
                        //       this code only finds the first one, not necessarily the correct
                        //       one. This is only used for the diagnostic message, but it still
                        //       could confuse the user.
                        cap = w.getCapability();
                        break;
                    }
                }
            }

            return cap;
        }

        @Override
        public ResolutionException toException()
        {
            return new ReasonException(ReasonException.Reason.UseConstraint, getMessage(), null, getUnresolvedRequirements());
        }
    }

    private static class EnhancedExecutor
    {
        private final Executor executor;
        private final Queue<Future<Void>> awaiting = new ConcurrentLinkedQueue<Future<Void>>();
        private final AtomicReference<Throwable> throwable = new AtomicReference<Throwable>();

        public EnhancedExecutor(Executor executor)
        {
            this.executor = executor;
        }

        public void execute(final Runnable runnable)
        {
            FutureTask<Void> task = new FutureTask<Void>(new Runnable()
            {
                public void run()
                {
                    try
                    {
                        runnable.run();
                    }
                    catch (Throwable t)
                    {
                        throwable.compareAndSet(null, t);
                    }
                }
            }, (Void) null);
            // must have a happens-first to add the task to awaiting
            awaiting.add(task);
            try
            {
                executor.execute(task);
            }
            catch (Throwable t)
            {
                // if the task did not get added successfully to the executor we must cancel
                // the task so we don't await on it
                task.cancel(false);
                throwable.compareAndSet(null, t);
            }
        }

        public void await()
        {
            Future<Void> awaitTask;
            while (throwable.get() == null && (awaitTask = awaiting.poll()) != null)
            {
                if (!awaitTask.isDone() && !awaitTask.isCancelled())
                {
                    try
                    {
                        awaitTask.get();
                    }
                    catch (CancellationException e)
                    {
                        // ignore; will have throwable set
                    }
                    catch (InterruptedException e)
                    {
                        throw new IllegalStateException(e);
                    }
                    catch (ExecutionException e)
                    {
                        throw new RuntimeException(e.getCause());
                    }
                }
            }
            Throwable t = throwable.get();
            if (t != null)
            {
                if (t instanceof Runnable)
                {
                    throw (RuntimeException) t;
                }
                else if (t instanceof Error)
                {
                    throw (Error) t;
                }
                throw new RuntimeException(t);
            }
        }
    }

    static class DumbExecutor implements Executor
    {
        public void execute(Runnable command)
        {
            command.run();
        }
    }

}
