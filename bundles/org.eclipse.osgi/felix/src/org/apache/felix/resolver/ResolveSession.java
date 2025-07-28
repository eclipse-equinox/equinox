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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import org.apache.felix.resolver.util.CandidateSelector;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.resolver.ResolutionException;
import org.osgi.service.resolver.ResolveContext;

// Note this class is not thread safe.
// Only use in the context of a single thread.
class ResolveSession implements Runnable
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
    private final Logger logger;

    static ResolveSession createSession(ResolveContext resolveContext, Executor executor, Resource dynamicHost,
            Requirement dynamicReq, List<Capability> dynamicCandidates, Logger logger)
    {
        ResolveSession session = new ResolveSession(resolveContext, executor, dynamicHost, dynamicReq,
                dynamicCandidates, logger);
        // call onCancel first
        session.getContext().onCancel(session);
        // now gather the mandatory and optional resources
        session.initMandatoryAndOptionalResources();
        return session;
    }

    private ResolveSession(ResolveContext resolveContext, Executor executor, Resource dynamicHost,
            Requirement dynamicReq, List<Capability> dynamicCandidates, Logger logger)
    {
        m_resolveContext = resolveContext;
        m_executor = executor;
        m_dynamicHost = dynamicHost;
        m_dynamicReq = dynamicReq;
        m_dynamicCandidates = dynamicCandidates;
        this.logger = logger;
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

    public Logger getLogger() {
        return logger;
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
                        throw new IllegalArgumentException("Unknown permutation type: " + type);
                }
            } catch (IndexOutOfBoundsException e) {
                // just a safeguard, this really should never happen
                typeToAddTo.add(permutation);
            }
            logger.logPermutationAdded(type);
        }
    }

    Candidates getNextPermutation() {
        Candidates next = null;
        PermutationType type;
        do {
            if (!m_usesPermutations.isEmpty())
            {
                next = m_usesPermutations.remove(0);
                type = PermutationType.USES;
            }
            else if (!m_importPermutations.isEmpty())
            {
                next = m_importPermutations.remove(0);
                type = PermutationType.IMPORT;
            }
            else if (!m_substPermutations.isEmpty())
            {
                next = m_substPermutations.remove(0);
                type = PermutationType.SUBSTITUTE;
            }
            else {
                return null;
            }
            logger.logProcessPermutation(type);
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