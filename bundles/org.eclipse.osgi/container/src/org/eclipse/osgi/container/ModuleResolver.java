/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.container;

import java.security.Permission;
import java.util.*;
import org.apache.felix.resolver.ResolverImpl;
import org.eclipse.osgi.container.ModuleRequirement.DynamicModuleRequirement;
import org.eclipse.osgi.internal.container.Converters;
import org.eclipse.osgi.report.resolution.*;
import org.eclipse.osgi.report.resolution.ResolutionReport.Entry;
import org.eclipse.osgi.report.resolution.ResolutionReport.Entry.Type;
import org.osgi.framework.*;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.namespace.*;
import org.osgi.framework.wiring.*;
import org.osgi.resource.*;
import org.osgi.service.resolver.*;

/**
 * The module resolver handles calls to the {@link Resolver} service for resolving modules
 * in a module {@link ModuleContainer container}.
 */
final class ModuleResolver {
	private static final Collection<String> NON_PAYLOAD_CAPABILITIES = Arrays.asList(IdentityNamespace.IDENTITY_NAMESPACE);
	private static final Collection<String> NON_PAYLOAD_REQUIREMENTS = Arrays.asList(HostNamespace.HOST_NAMESPACE, ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE);

	final ThreadLocal<Boolean> threadResolving = new ThreadLocal<Boolean>() {
		@Override
		protected Boolean initialValue() {
			return Boolean.FALSE;
		}
	};
	final ModuleContainerAdaptor adaptor;
	final Resolver resolver;

	/**
	 * Constructs the module resolver with the specified resolver hook factory
	 * and resolver.
	 * @param adaptor the container adaptor
	 */
	ModuleResolver(ModuleContainerAdaptor adaptor) {
		this.adaptor = adaptor;
		this.resolver = adaptor.getResolver();
	}

	/**
	 * Attempts to resolve all unresolved modules installed in the specified module database.
	 * returns a delta containing the new wirings or modified wirings that should be 
	 * merged into the specified moduleDatabase.
	 * <p>
	 * This method only does read operations on the database no wirings are modified
	 * directly by this method.  The returned wirings need to be merged into 
	 * the database.
	 * @param triggers the triggers that caused the resolver operation to occur
	 * @param triggersMandatory true if the triggers must be resolved by the resolve process
	 * @param unresolved a snapshot of unresolved revisions
	 * @param wiringCopy the wirings snapshot of the currently resolved revisions
	 * @param moduleDatabase the module database.
	 * @return a delta container the new wirings or modified wirings that should be
	 * merged into the moduleDatabase
	 * @throws ResolutionException
	 */
	Map<ModuleRevision, ModuleWiring> resolveDelta(Collection<ModuleRevision> triggers, boolean triggersMandatory, Collection<ModuleRevision> unresolved, Map<ModuleRevision, ModuleWiring> wiringCopy, ModuleDatabase moduleDatabase) throws ResolutionException {
		ResolveProcess resolveProcess = new ResolveProcess(unresolved, triggers, triggersMandatory, wiringCopy, moduleDatabase);
		Map<Resource, List<Wire>> result = resolveProcess.resolve();
		return generateDelta(result, wiringCopy);
	}

	Map<ModuleRevision, ModuleWiring> resolveDynamicDelta(DynamicModuleRequirement dynamicReq, Collection<ModuleRevision> unresolved, Map<ModuleRevision, ModuleWiring> wiringCopy, ModuleDatabase moduleDatabase) throws ResolutionException {
		ResolveProcess resolveProcess = new ResolveProcess(unresolved, dynamicReq, wiringCopy, moduleDatabase);
		Map<Resource, List<Wire>> result = resolveProcess.resolve();
		return generateDelta(result, wiringCopy);
	}

	private static Map<ModuleRevision, ModuleWiring> generateDelta(Map<Resource, List<Wire>> result, Map<ModuleRevision, ModuleWiring> wiringCopy) {
		Map<ModuleRevision, Map<ModuleCapability, List<ModuleWire>>> provided = new HashMap<ModuleRevision, Map<ModuleCapability, List<ModuleWire>>>();
		Map<ModuleRevision, List<ModuleWire>> required = new HashMap<ModuleRevision, List<ModuleWire>>();
		// First populate the list of provided and required wires for revision
		// This is done this way to share the wire object between both the provider and requirer
		for (Map.Entry<Resource, List<Wire>> resultEntry : result.entrySet()) {
			ModuleRevision revision = (ModuleRevision) resultEntry.getKey();
			List<ModuleWire> requiredWires = new ArrayList<ModuleWire>(resultEntry.getValue().size());
			for (Wire wire : resultEntry.getValue()) {
				ModuleWire moduleWire = new ModuleWire((ModuleCapability) wire.getCapability(), (ModuleRevision) wire.getProvider(), (ModuleRequirement) wire.getRequirement(), (ModuleRevision) wire.getRequirer());
				requiredWires.add(moduleWire);
				Map<ModuleCapability, List<ModuleWire>> providedWiresMap = provided.get(moduleWire.getProvider());
				if (providedWiresMap == null) {
					providedWiresMap = new HashMap<ModuleCapability, List<ModuleWire>>();
					provided.put(moduleWire.getProvider(), providedWiresMap);
				}
				List<ModuleWire> providedWires = providedWiresMap.get(moduleWire.getCapability());
				if (providedWires == null) {
					providedWires = new ArrayList<ModuleWire>();
					providedWiresMap.put(moduleWire.getCapability(), providedWires);
				}
				providedWires.add(moduleWire);
			}
			required.put(revision, requiredWires);
		}

		Map<ModuleRevision, ModuleWiring> delta = new HashMap<ModuleRevision, ModuleWiring>();
		// now create the ModuleWiring for the newly resolved revisions
		for (ModuleRevision revision : required.keySet()) {
			ModuleWiring existingWiring = wiringCopy.get(revision);
			if (existingWiring == null) {
				delta.put(revision, createNewWiring(revision, provided, required));
			} else {
				// this is to handle dynamic imports
				delta.put(revision, createWiringDelta(revision, existingWiring, provided.get(revision), required.get(revision)));
			}
		}
		// Also need to create the wiring deltas for already resolved bundles
		// This should only include updating provided wires and
		// for fragments it may include new hosts
		for (ModuleRevision revision : provided.keySet()) {
			ModuleWiring existingWiring = wiringCopy.get(revision);
			if (existingWiring != null && !delta.containsKey(revision)) {
				delta.put(revision, createWiringDelta(revision, existingWiring, provided.get(revision), required.get(revision)));
			}
		}
		return delta;
	}

	private static ModuleWiring createNewWiring(ModuleRevision revision, Map<ModuleRevision, Map<ModuleCapability, List<ModuleWire>>> provided, Map<ModuleRevision, List<ModuleWire>> required) {
		Map<ModuleCapability, List<ModuleWire>> providedWireMap = provided.get(revision);
		if (providedWireMap == null)
			providedWireMap = Collections.emptyMap();
		List<ModuleWire> requiredWires = required.get(revision);
		if (requiredWires == null)
			requiredWires = Collections.emptyList();

		List<ModuleCapability> capabilities = new ArrayList<ModuleCapability>(revision.getModuleCapabilities(null));
		ListIterator<ModuleCapability> iCapabilities = capabilities.listIterator(capabilities.size());
		List<ModuleRequirement> requirements = new ArrayList<ModuleRequirement>(revision.getModuleRequirements(null));
		ListIterator<ModuleRequirement> iRequirements = requirements.listIterator(requirements.size());

		// if revision is a fragment remove payload requirements and capabilities
		if ((BundleRevision.TYPE_FRAGMENT & revision.getTypes()) != 0) {
			removePayloadContent(iCapabilities, iRequirements);
		} else {
			// add fragment capabilities and requirements
			List<ModuleCapability> hostCapabilities = revision.getModuleCapabilities(HostNamespace.HOST_NAMESPACE);
			ModuleCapability hostCapability = hostCapabilities.isEmpty() ? null : hostCapabilities.get(0);
			if (hostCapability != null) {
				addPayloadContent(providedWireMap.get(hostCapability), iCapabilities, iRequirements);
			}
		}

		removeNonEffectiveCapabilities(iCapabilities);
		removeNonEffectiveRequirements(iRequirements, requiredWires);
		Collection<String> substituted = removeSubstitutedCapabilities(iCapabilities, requiredWires);

		List<ModuleWire> providedWires = new ArrayList<ModuleWire>();
		addProvidedWires(providedWireMap, providedWires, capabilities);

		filterCapabilityPermissions(capabilities);
		return new ModuleWiring(revision, capabilities, requirements, providedWires, requiredWires, substituted);
	}

	private static void removePayloadContent(ListIterator<ModuleCapability> iCapabilities, ListIterator<ModuleRequirement> iRequirements) {
		rewind(iCapabilities);
		while (iCapabilities.hasNext()) {
			if (!NON_PAYLOAD_CAPABILITIES.contains(iCapabilities.next().getNamespace())) {
				iCapabilities.remove();
			}
		}

		rewind(iRequirements);
		while (iRequirements.hasNext()) {
			if (!NON_PAYLOAD_REQUIREMENTS.contains(iRequirements.next().getNamespace())) {
				iRequirements.remove();
			}
		}
	}

	private static Collection<String> removeSubstitutedCapabilities(ListIterator<ModuleCapability> iCapabilities, List<ModuleWire> requiredWires) {
		Collection<String> substituted = null;
		for (ModuleWire moduleWire : requiredWires) {
			if (!PackageNamespace.PACKAGE_NAMESPACE.equals(moduleWire.getCapability().getNamespace()))
				continue;
			String packageName = (String) moduleWire.getCapability().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE);
			rewind(iCapabilities);
			while (iCapabilities.hasNext()) {
				ModuleCapability capability = iCapabilities.next();
				if (PackageNamespace.PACKAGE_NAMESPACE.equals(capability.getNamespace())) {
					if (packageName.equals(capability.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE))) {
						// found a package capability with the same name as a package that got imported
						// this indicates a substitution
						iCapabilities.remove();
						if (substituted == null) {
							substituted = new ArrayList<String>();
						}
						substituted.add(packageName);
						if (!substituted.contains(packageName)) {
							substituted.add(packageName);
						}
					}
				}
			}
		}
		return substituted == null ? Collections.<String> emptyList() : substituted;
	}

	private static void removeNonEffectiveRequirements(ListIterator<ModuleRequirement> iRequirements, List<ModuleWire> requiredWires) {
		rewind(iRequirements);
		while (iRequirements.hasNext()) {
			ModuleRequirement requirement = iRequirements.next();
			// check the effective directive;
			Object effective = requirement.getDirectives().get(Namespace.REQUIREMENT_EFFECTIVE_DIRECTIVE);
			if (effective != null && !Namespace.EFFECTIVE_RESOLVE.equals(effective)) {
				iRequirements.remove();
			} else {
				// check the resolution directive
				Object resolution = requirement.getDirectives().get(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE);
				if (Namespace.RESOLUTION_OPTIONAL.equals(resolution)) {
					boolean found = false;
					// need to check the wires to see if the optional requirement is resolved
					wires: for (ModuleWire wire : requiredWires) {
						if (wire.getRequirement().equals(requirement)) {
							found = true;
							break wires;
						}
					}
					if (!found) {
						// optional requirement is not resolved
						iRequirements.remove();
					}
				}
			}
		}
	}

	static void removeNonEffectiveCapabilities(ListIterator<ModuleCapability> iCapabilities) {
		rewind(iCapabilities);
		while (iCapabilities.hasNext()) {
			Object effective = iCapabilities.next().getDirectives().get(Namespace.CAPABILITY_EFFECTIVE_DIRECTIVE);
			if (effective != null && !Namespace.EFFECTIVE_RESOLVE.equals(effective))
				iCapabilities.remove();
		}
	}

	private static void addPayloadContent(List<ModuleWire> hostWires, ListIterator<ModuleCapability> iCapabilities, ListIterator<ModuleRequirement> iRequirements) {
		if (hostWires == null)
			return;
		for (ModuleWire hostWire : hostWires) {
			// add fragment capabilities
			String currentNamespace = null;
			List<ModuleCapability> fragmentCapabilities = hostWire.getRequirer().getModuleCapabilities(null);
			for (ModuleCapability fragmentCapability : fragmentCapabilities) {
				if (NON_PAYLOAD_CAPABILITIES.contains(fragmentCapability.getNamespace())) {
					continue; // don't include, not a payload capability
				}
				if (!fragmentCapability.getNamespace().equals(currentNamespace)) {
					currentNamespace = fragmentCapability.getNamespace();
					fastForward(iCapabilities);
					while (iCapabilities.hasPrevious()) {
						if (iCapabilities.previous().getNamespace().equals(currentNamespace)) {
							iCapabilities.next(); // put position after the last one
							break;
						}
					}
				}
				iCapabilities.add(fragmentCapability);
			}
			// add fragment requirements
			currentNamespace = null;
			List<ModuleRequirement> fragmentRequriements = hostWire.getRequirer().getModuleRequirements(null);
			for (ModuleRequirement fragmentRequirement : fragmentRequriements) {
				if (NON_PAYLOAD_REQUIREMENTS.contains(fragmentRequirement.getNamespace())) {
					continue; // don't inlcude, not a payload requirement
				}
				if (!fragmentRequirement.getNamespace().equals(currentNamespace)) {
					currentNamespace = fragmentRequirement.getNamespace();
					fastForward(iRequirements);
					while (iRequirements.hasPrevious()) {
						if (iRequirements.previous().getNamespace().equals(currentNamespace)) {
							iRequirements.next(); // put position after the last one
							break;
						}
					}
				}
				iRequirements.add(fragmentRequirement);
			}
		}
	}

	private static void addProvidedWires(Map<ModuleCapability, List<ModuleWire>> toAdd, List<ModuleWire> existing, final List<ModuleCapability> orderedCapabilities) {
		if (toAdd == null)
			return;
		int originalSize = existing.size();
		for (ModuleCapability capability : orderedCapabilities) {
			List<ModuleWire> newWires = toAdd.get(capability);
			if (newWires != null) {
				existing.addAll(newWires);
			}
		}
		if (originalSize != 0) {
			Collections.sort(existing, new Comparator<ModuleWire>() {
				@Override
				public int compare(ModuleWire w1, ModuleWire w2) {
					int index1 = orderedCapabilities.indexOf(w1.getCapability());
					int index2 = orderedCapabilities.indexOf(w2.getCapability());
					return index1 - index2;
				}
			});
		}
	}

	private static void addRequiredWires(List<ModuleWire> toAdd, List<ModuleWire> existing, final List<ModuleRequirement> orderedRequirements) {
		if (toAdd == null)
			return;
		int originalSize = existing.size();
		existing.addAll(toAdd);
		if (originalSize != 0) {
			Collections.sort(existing, new Comparator<ModuleWire>() {
				@Override
				public int compare(ModuleWire w1, ModuleWire w2) {
					int index1 = orderedRequirements.indexOf(w1.getRequirement());
					int index2 = orderedRequirements.indexOf(w2.getRequirement());
					return index1 - index2;
				}
			});
		}
	}

	private static void fastForward(ListIterator<?> listIterator) {
		while (listIterator.hasNext())
			listIterator.next();
	}

	static void rewind(ListIterator<?> listIterator) {
		while (listIterator.hasPrevious())
			listIterator.previous();
	}

	@SuppressWarnings("unchecked")
	private static ModuleWiring createWiringDelta(ModuleRevision revision, ModuleWiring existingWiring, Map<ModuleCapability, List<ModuleWire>> providedWireMap, List<ModuleWire> requiredWires) {
		// Create a ModuleWiring that only contains the new ordered list of provided wires
		List<ModuleWire> existingProvidedWires = existingWiring.getProvidedModuleWires(null);
		List<ModuleCapability> existingCapabilities = existingWiring.getModuleCapabilities(null);
		addProvidedWires(providedWireMap, existingProvidedWires, existingCapabilities);

		// Also need to include any new required wires that may have be added for fragment hosts
		// Also will be needed for dynamic imports
		List<ModuleWire> existingRequiredWires = existingWiring.getRequiredModuleWires(null);
		List<ModuleRequirement> existingRequirements = existingWiring.getModuleRequirements(null);
		addRequiredWires(requiredWires, existingRequiredWires, existingRequirements);

		// add newly resolved fragment capabilities and requirements
		if (providedWireMap != null) {
			List<ModuleCapability> hostCapabilities = revision.getModuleCapabilities(HostNamespace.HOST_NAMESPACE);
			ModuleCapability hostCapability = hostCapabilities.isEmpty() ? null : hostCapabilities.get(0);
			List<ModuleWire> newHostWires = hostCapability == null ? null : providedWireMap.get(hostCapability);
			if (newHostWires != null) {
				addPayloadContent(newHostWires, existingCapabilities.listIterator(), existingRequirements.listIterator());
			}
		}

		filterCapabilityPermissions(existingCapabilities);
		return new ModuleWiring(revision, existingCapabilities, existingRequirements, existingProvidedWires, existingRequiredWires, Collections.EMPTY_LIST);
	}

	private static void filterCapabilityPermissions(List<ModuleCapability> capabilities) {
		if (System.getSecurityManager() == null) {
			return;
		}
		for (Iterator<ModuleCapability> iCapabilities = capabilities.iterator(); iCapabilities.hasNext();) {
			ModuleCapability capability = iCapabilities.next();
			Permission permission = getProvidePermission(capability);
			Bundle provider = capability.getRevision().getBundle();
			if (provider != null && !provider.hasPermission(permission)) {
				iCapabilities.remove();
			}
		}
	}

	static boolean isSingleton(ModuleRevision revision) {
		List<Capability> identities = revision.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
		if (identities.isEmpty())
			return false;
		return "true".equals(identities.get(0).getDirectives().get(IdentityNamespace.CAPABILITY_SINGLETON_DIRECTIVE)); //$NON-NLS-1$
	}

	static Version getVersion(Capability c) {
		String versionAttr = null;
		String namespace = c.getNamespace();
		if (IdentityNamespace.IDENTITY_NAMESPACE.equals(namespace)) {
			versionAttr = IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE;
		} else if (PackageNamespace.PACKAGE_NAMESPACE.equals(namespace)) {
			versionAttr = PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE;
		} else if (BundleNamespace.BUNDLE_NAMESPACE.equals(namespace)) {
			versionAttr = BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE;
		} else if (HostNamespace.HOST_NAMESPACE.equals(namespace)) {
			versionAttr = HostNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE;
		} else {
			// Just default to version attribute
			versionAttr = IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE;
		}
		Object version = c.getAttributes().get(versionAttr);
		return version instanceof Version ? (Version) version : Version.emptyVersion;
	}

	static Permission getRequirePermission(BundleCapability candidate) {
		String name = candidate.getNamespace();
		if (PackageNamespace.PACKAGE_NAMESPACE.equals(name)) {
			return new PackagePermission(getPermisionName(candidate), candidate.getRevision().getBundle(), PackagePermission.IMPORT);
		}
		if (HostNamespace.HOST_NAMESPACE.equals(name)) {
			return new BundlePermission(getPermisionName(candidate), BundlePermission.FRAGMENT);
		}
		if (BundleNamespace.BUNDLE_NAMESPACE.equals(name)) {
			return new BundlePermission(getPermisionName(candidate), BundlePermission.REQUIRE);
		}
		return new CapabilityPermission(name, candidate.getAttributes(), candidate.getRevision().getBundle(), CapabilityPermission.REQUIRE);
	}

	static Permission getProvidePermission(BundleCapability candidate) {
		String name = candidate.getNamespace();
		if (PackageNamespace.PACKAGE_NAMESPACE.equals(name)) {
			return new PackagePermission(getPermisionName(candidate), PackagePermission.EXPORTONLY);
		}
		if (HostNamespace.HOST_NAMESPACE.equals(name)) {
			return new BundlePermission(getPermisionName(candidate), BundlePermission.HOST);
		}
		if (BundleNamespace.BUNDLE_NAMESPACE.equals(name)) {
			return new BundlePermission(getPermisionName(candidate), BundlePermission.PROVIDE);
		}
		return new CapabilityPermission(name, CapabilityPermission.PROVIDE);
	}

	private static String getPermisionName(BundleCapability candidate) {
		Object name = candidate.getAttributes().get(candidate.getNamespace());
		if (name instanceof String) {
			return (String) name;
		}
		if (name instanceof Collection) {
			Collection<?> names = (Collection<?>) name;
			return names.isEmpty() ? "unknown" : names.iterator().next().toString(); //$NON-NLS-1$
		}
		return "unknown"; //$NON-NLS-1$
	}

	class ResolveProcess extends ResolveContext implements Comparator<Capability> {
		private final ModuleResolutionReport.Builder reportBuilder = new ModuleResolutionReport.Builder();
		/*
		 * Contains the revisions that were requested to be resolved and is not
		 * modified post instantiation.
		 */
		private final Collection<ModuleRevision> unresolved;
		/*
		 * Contains unresolved revisions that should not be resolved as part of
		 * this process. The reasons they should not be resolved will vary. For 
		 * example, some might have been filtered out by the resolver hook while
		 * others represent singleton collisions. It is assumed that all
		 * unresolved revisions are disabled at the start of the resolve
		 * process (see initialization in constructors). Any not filtered out
		 * by ResolverHook.filterResolvable are then removed but may be added 
		 * back later for other reasons.
		 */
		private final Collection<ModuleRevision> disabled;
		private final Collection<ModuleRevision> triggers;
		private final Collection<ModuleRevision> optionals;
		private final boolean triggersMandatory;
		private final ModuleDatabase moduleDatabase;
		private final Map<ModuleRevision, ModuleWiring> wirings;
		private final DynamicModuleRequirement dynamicReq;
		private volatile ResolverHook hook = null;
		private volatile Map<String, Collection<ModuleRevision>> byName = null;
		/*
		 * Used to generate the UNRESOLVED_PROVIDER resolution report entries.
		 * 
		 * The inner map associates a requirement to the set of all matching
		 * capabilities that were found. The outer map associates the requiring
		 * resource to the inner map so that its contents may easily be looked
		 * up from the set of unresolved resources, if any, after the resolution
		 * has occurred.
		 */
		private final Map<Resource, Map<Requirement, Set<Capability>>> unresolvedProviders = new HashMap<Resource, Map<Requirement, Set<Capability>>>();

		ResolveProcess(Collection<ModuleRevision> unresolved, Collection<ModuleRevision> triggers, boolean triggersMandatory, Map<ModuleRevision, ModuleWiring> wirings, ModuleDatabase moduleDatabase) {
			this.unresolved = unresolved;
			this.disabled = new HashSet<ModuleRevision>(unresolved);
			this.triggers = triggers;
			this.triggersMandatory = triggersMandatory;
			this.optionals = new ArrayList<ModuleRevision>(unresolved);
			if (this.triggersMandatory) {
				this.optionals.removeAll(triggers);
			}
			this.wirings = wirings;
			this.moduleDatabase = moduleDatabase;
			this.dynamicReq = null;
		}

		ResolveProcess(Collection<ModuleRevision> unresolved, DynamicModuleRequirement dynamicReq, Map<ModuleRevision, ModuleWiring> wirings, ModuleDatabase moduleDatabase) {
			this.unresolved = unresolved;
			this.disabled = new HashSet<ModuleRevision>(unresolved);
			ModuleRevision revision = dynamicReq.getRevision();
			this.triggers = new ArrayList<ModuleRevision>(1);
			this.triggers.add(revision);
			this.triggersMandatory = false;
			this.optionals = new ArrayList<ModuleRevision>(unresolved);
			this.wirings = wirings;
			this.moduleDatabase = moduleDatabase;
			this.dynamicReq = dynamicReq;
		}

		@Override
		public List<Capability> findProviders(Requirement requirement) {
			List<ModuleCapability> candidates = moduleDatabase.findCapabilities((ModuleRequirement) requirement);
			// TODO Record missing capability here if empty? Then record other
			// entry types later if an existing capability was filtered?
			List<Capability> result = filterProviders(requirement, candidates);
			if (result.isEmpty())
				reportBuilder.addEntry(requirement.getResource(), Entry.Type.MISSING_CAPABILITY, requirement);
			else
				computeUnresolvedProviders(requirement, result);
			return result;
		}

		private List<Capability> filterProviders(Requirement requirement, List<ModuleCapability> candidates) {
			ListIterator<ModuleCapability> iCandidates = candidates.listIterator();
			filterDisabled(iCandidates);
			removeNonEffectiveCapabilities(iCandidates);
			removeSubstituted(iCandidates);
			filterPermissions((BundleRequirement) requirement, iCandidates);
			hook.filterMatches((BundleRequirement) requirement, Converters.asListBundleCapability(candidates));
			Collections.sort(candidates, this);
			return Converters.asListCapability(candidates);
		}

		private void filterPermissions(BundleRequirement requirement, ListIterator<ModuleCapability> iCandidates) {
			rewind(iCandidates);
			if (System.getSecurityManager() == null || !iCandidates.hasNext()) {
				return;
			}

			if (requirement.getRevision().getBundle() == null) {
				// this container is not modeling a real framework, no permission check is done
				return;
			}

			candidates: while (iCandidates.hasNext()) {
				ModuleCapability candidate = iCandidates.next();
				// TODO this is a hack for when a bundle imports and exports the same package
				if (PackageNamespace.PACKAGE_NAMESPACE.equals(requirement.getNamespace())) {
					if (requirement.getRevision().equals(candidate.getRevision())) {
						continue candidates;
					}
				}
				Permission requirePermission = getRequirePermission(candidate);
				Permission providePermission = getProvidePermission(candidate);
				if (!requirement.getRevision().getBundle().hasPermission(requirePermission) || !candidate.getRevision().getBundle().hasPermission(providePermission)) {
					iCandidates.remove();
				}
			}
		}

		private void filterDisabled(ListIterator<ModuleCapability> iCandidates) {
			rewind(iCandidates);
			while (iCandidates.hasNext()) {
				if (disabled.contains(iCandidates.next().getResource()))
					iCandidates.remove();
			}
		}

		private void removeSubstituted(ListIterator<ModuleCapability> iCapabilities) {
			rewind(iCapabilities);
			while (iCapabilities.hasNext()) {
				ModuleCapability capability = iCapabilities.next();
				ModuleWiring wiring = wirings.get(capability.getRevision());
				if (wiring != null && wiring.isSubtituted(capability)) {
					iCapabilities.remove();
				}
			}
		}

		@Override
		public int insertHostedCapability(List<Capability> capabilities, HostedCapability hostedCapability) {
			int index = Collections.binarySearch(capabilities, hostedCapability, this);
			if (index < 0)
				index = -index - 1;
			capabilities.add(index, hostedCapability);
			return index;
		}

		@Override
		public boolean isEffective(Requirement requirement) {
			String effective = requirement.getDirectives().get(Namespace.REQUIREMENT_EFFECTIVE_DIRECTIVE);
			return effective == null || Namespace.EFFECTIVE_RESOLVE.equals(effective);
		}

		@SuppressWarnings("unchecked")
		@Override
		public Map<Resource, Wiring> getWirings() {
			Map<?, ?> raw = wirings;
			return Collections.unmodifiableMap((Map<Resource, Wiring>) raw);
		}

		@Override
		public Collection<Resource> getMandatoryResources() {
			if (triggersMandatory) {
				return Converters.asCollectionResource(triggers);
			}
			return super.getMandatoryResources();
		}

		@Override
		public Collection<Resource> getOptionalResources() {
			return Converters.asCollectionResource(optionals);
		}

		Map<Resource, List<Wire>> resolve() throws ResolutionException {
			if (threadResolving.get().booleanValue()) {
				throw new IllegalStateException("Detected a recursive resolve operation.");
			}
			threadResolving.set(Boolean.TRUE);
			try {
				try {
					hook = adaptor.getResolverHookFactory().begin(Converters.asListBundleRevision((List<? extends BundleRevision>) triggers));
				} catch (RuntimeException e) {
					if (e.getCause() instanceof BundleException) {
						BundleException be = (BundleException) e.getCause();
						if (be.getType() == BundleException.REJECTED_BY_HOOK) {
							throw new ResolutionException(be);
						}
					}
					throw e;
				}
				Map<Resource, List<Wire>> result = null;
				try {
					filterResolvable();
					selectSingletons();
					Map<Resource, List<Wire>> extensionWirings = resolveFrameworkExtensions();
					if (!extensionWirings.isEmpty()) {
						result = extensionWirings;
					} else {
						// remove disabled from optional and triggers to prevent the resolver from resolving them
						optionals.removeAll(disabled);
						if (triggers.removeAll(disabled) && triggersMandatory) {
							throw new ResolutionException("Could not resolve mandatory modules because another singleton was selected or the module was disabled: " + disabled);
						}
						if (dynamicReq != null) {
							result = resolveDynamic();
						} else {
							result = resolver.resolve(this);
						}
					}
					return result;
				} finally {
					computeUnresolvedProviderResolutionReportEntries(result);
					if (hook instanceof ResolutionReport.Listener)
						((ResolutionReport.Listener) hook).handleResolutionReport(reportBuilder.build());
					hook.end();
				}
			} finally {
				threadResolving.set(Boolean.FALSE);
			}
		}

		/*
		 * Given the results of a resolution, compute which, if any, of the
		 * enabled, resolving resources are still unresolved. For those that are
		 * unresolved, generate resolution report entries for unresolved
		 * providers, if necessary.
		 */
		private void computeUnresolvedProviderResolutionReportEntries(Map<Resource, List<Wire>> resolution) {
			// Create a collection representing the resources asked to be
			// resolved.
			Collection<Resource> shouldHaveResolvedResources = new ArrayList<Resource>(unresolved);
			// Remove disabled resources.
			shouldHaveResolvedResources.removeAll(disabled);
			// Remove resolved resources, if necessary. The resolution will be
			// null if the resolver threw an exception because the triggers
			// were mandatory but didn't resolve.
			if (resolution != null)
				shouldHaveResolvedResources.removeAll(resolution.keySet());
			// What remains are resources that should have resolved but didn't.
			// For each resource, add report entries for any unresolved
			// providers.
			for (Resource shouldHaveResolvedResource : shouldHaveResolvedResources) {
				Map<Requirement, Set<Capability>> requirementToCapabilities = unresolvedProviders.get(shouldHaveResolvedResource);
				if (requirementToCapabilities == null)
					continue;
				// If nothing resolved then there are no resolved resources to
				// filter out.
				if (resolution != null) {
					// Filter out capability providers that resolved.
					for (Iterator<Set<Capability>> values = requirementToCapabilities.values().iterator(); values.hasNext();) {
						Set<Capability> value = values.next();
						for (Iterator<Capability> capabilities = value.iterator(); capabilities.hasNext();)
							if (resolution.containsKey(capabilities.next().getResource()))
								// Remove the resolved capability provider.
								capabilities.remove();
						if (value.isEmpty())
							// Remove the requirement that has no unresolved
							// capability providers.
							values.remove();
					}
				}
				// Add a report entry if there are any remaining requirements
				// pointing to unresolved capability providers.
				if (!requirementToCapabilities.isEmpty())
					reportBuilder.addEntry(shouldHaveResolvedResource, Entry.Type.UNRESOLVED_PROVIDER, requirementToCapabilities);
			}
		}

		/*
		 * Given a requirement and its matching capabilities, map the
		 * requirement's resource to the requirement and matching capabilities.
		 * This data is used to compute report entries for resources that did
		 * not resolve because a provider did not resolve.
		 */
		private void computeUnresolvedProviders(Requirement requirement, Collection<? extends Capability> capabilities) {
			Resource requirer = requirement.getResource();
			Map<Requirement, Set<Capability>> requirementToCapabilities = unresolvedProviders.get(requirer);
			if (requirementToCapabilities == null) {
				requirementToCapabilities = new HashMap<Requirement, Set<Capability>>();
				unresolvedProviders.put(requirer, requirementToCapabilities);
			}
			Set<Capability> value = requirementToCapabilities.get(requirement);
			if (value == null) {
				value = new HashSet<Capability>(capabilities.size());
				requirementToCapabilities.put(requirement, value);
			}
			for (Capability capability : capabilities)
				if (!wirings.containsKey(capability.getResource()))
					value.add(capability);
		}

		private Map<Resource, List<Wire>> resolveFrameworkExtensions() {
			// special handling only if explicitly asked to resolve an extension as mandatory.
			ModuleRevision extension = triggersMandatory && triggers.size() == 1 ? triggers.iterator().next() : null;
			if (extension == null) {
				return Collections.emptyMap();
			}

			// Need the system module wiring to attach to.
			Module systemModule = moduleDatabase.getModule(0);
			ModuleRevision systemRevision = systemModule == null ? null : systemModule.getCurrentRevision();
			ModuleWiring systemWiring = systemRevision == null ? null : wirings.get(systemRevision);
			if (systemWiring == null) {
				return Collections.emptyMap();
			}

			// Check all the non-payload requirements are met and a proper host is found
			Map<Resource, List<Wire>> result = new HashMap<Resource, List<Wire>>(0);
			if (!disabled.contains(extension)) {
				List<Wire> nonPayloadWires = new ArrayList<Wire>(0);
				boolean foundHost = false;
				boolean payLoadRequirements = false;
				for (ModuleRequirement requirement : extension.getModuleRequirements(null)) {
					if (NON_PAYLOAD_REQUIREMENTS.contains(requirement.getNamespace())) {
						List<ModuleCapability> matching = moduleDatabase.findCapabilities(requirement);
						for (ModuleCapability candidate : matching) {
							if (candidate.getRevision().getRevisions().getModule().getId().longValue() == 0) {
								foundHost |= HostNamespace.HOST_NAMESPACE.equals(requirement.getNamespace());
								nonPayloadWires.add(new ModuleWire(candidate, candidate.getRevision(), requirement, extension));
							}
						}
					} else {
						// oh oh! somehow a payload requirement got into the mix
						payLoadRequirements |= true;
					}

				}
				if (foundHost && !payLoadRequirements) {
					result.put(extension, nonPayloadWires);
				}
			}
			return result;
		}

		private Map<Resource, List<Wire>> resolveDynamic() throws ResolutionException {
			if (!(resolver instanceof ResolverImpl)) {
				throw new ResolutionException("Dynamic import resolution not supported by the resolver: " + resolver.getClass());
			}
			List<Capability> dynamicMatches = filterProviders(dynamicReq.getOriginal(), moduleDatabase.findCapabilities(dynamicReq));
			Collection<Resource> ondemandFragments = Converters.asCollectionResource(moduleDatabase.getFragmentRevisions());

			return ((ResolverImpl) resolver).resolve(this, dynamicReq.getRevision(), dynamicReq.getOriginal(), dynamicMatches, ondemandFragments);

		}

		private void filterResolvable() {
			Collection<ModuleRevision> enabledCandidates = new ArrayList<ModuleRevision>(unresolved);
			hook.filterResolvable(Converters.asListBundleRevision((List<? extends BundleRevision>) enabledCandidates));
			disabled.removeAll(enabledCandidates);
			for (ModuleRevision revision : disabled)
				reportBuilder.addEntry(revision, Entry.Type.FILTERED_BY_RESOLVER_HOOK, null);
		}

		private void selectSingletons() {
			Map<String, Collection<ModuleRevision>> selectedSingletons = new HashMap<String, Collection<ModuleRevision>>();
			for (ModuleRevision revision : unresolved) {
				if (!isSingleton(revision) || disabled.contains(revision))
					continue;
				String bsn = revision.getSymbolicName();
				Collection<ModuleRevision> selected = selectedSingletons.get(bsn);
				if (selected != null)
					continue; // already processed the bsn
				selected = new ArrayList<ModuleRevision>(1);
				selectedSingletons.put(bsn, selected);

				Collection<ModuleRevision> sameBSN = getRevisions(bsn);
				if (sameBSN.size() < 2) {
					selected.add(revision);
					continue;
				}
				// prime selected with resolved singleton bundles
				for (ModuleRevision singleton : sameBSN) {
					if (isSingleton(singleton) && wirings.containsKey(singleton))
						selected.add(singleton);
				}
				// get the collision map for the BSN
				Map<ModuleRevision, Collection<ModuleRevision>> collisionMap = getCollisionMap(sameBSN);
				// process the collision map
				for (ModuleRevision singleton : sameBSN) {
					if (selected.contains(singleton))
						continue; // no need to process resolved bundles
					Collection<ModuleRevision> collisions = collisionMap.get(singleton);
					if (collisions == null || disabled.contains(singleton))
						continue; // not a singleton or not resolvable
					Collection<ModuleRevision> pickOneToResolve = new ArrayList<ModuleRevision>();
					for (ModuleRevision collision : collisions) {
						if (selected.contains(collision)) {
							// Must fail since there is already a selected bundle which is a collision of the singleton bundle
							disabled.add(singleton);
							reportBuilder.addEntry(singleton, Type.SINGLETON_SELECTION, collision);
							break;
						}
						if (!pickOneToResolve.contains(collision))
							pickOneToResolve.add(collision);
					}
					if (!disabled.contains(singleton)) {
						// need to make sure the bundle does not collide from the POV of another entry
						for (Map.Entry<ModuleRevision, Collection<ModuleRevision>> collisionEntry : collisionMap.entrySet()) {
							if (collisionEntry.getKey() != singleton && collisionEntry.getValue().contains(singleton)) {
								if (selected.contains(collisionEntry.getKey())) {
									// Must fail since there is already a selected bundle for which the singleton bundle is a collision
									disabled.add(singleton);
									reportBuilder.addEntry(singleton, Type.SINGLETON_SELECTION, collisionEntry.getKey());
									break;
								}
								if (!pickOneToResolve.contains(collisionEntry.getKey()))
									pickOneToResolve.add(collisionEntry.getKey());
							}
						}
					}
					if (!disabled.contains(singleton)) {
						pickOneToResolve.add(singleton);
						selected.add(pickOneToResolve(pickOneToResolve));
					}
				}
			}
		}

		private Collection<ModuleRevision> getRevisions(String name) {
			Map<String, Collection<ModuleRevision>> current = byName;
			if (current == null) {
				// generate the map using unresolved collection and wiring snap shot
				// this is to avoid interacting with the module database
				Set<ModuleRevision> revisions = new HashSet<ModuleRevision>();
				revisions.addAll(unresolved);
				revisions.addAll(wirings.keySet());
				current = new HashMap<String, Collection<ModuleRevision>>();
				for (ModuleRevision revision : revisions) {
					Collection<ModuleRevision> sameName = current.get(revision.getSymbolicName());
					if (sameName == null) {
						sameName = new ArrayList<ModuleRevision>();
						current.put(revision.getSymbolicName(), sameName);
					}
					sameName.add(revision);
				}
				byName = current;
			}
			Collection<ModuleRevision> result = current.get(name);
			if (result == null) {
				return Collections.emptyList();
			}
			return result;
		}

		private ModuleRevision pickOneToResolve(Collection<ModuleRevision> pickOneToResolve) {
			ModuleRevision selectedVersion = null;
			for (ModuleRevision singleton : pickOneToResolve) {
				if (selectedVersion == null)
					selectedVersion = singleton;
				boolean higherVersion = selectedVersion.getVersion().compareTo(singleton.getVersion()) < 0;
				if (higherVersion)
					selectedVersion = singleton;
			}

			for (ModuleRevision singleton : pickOneToResolve) {
				if (singleton != selectedVersion) {
					disabled.add(singleton);
					reportBuilder.addEntry(singleton, Type.SINGLETON_SELECTION, selectedVersion);
				}
			}
			return selectedVersion;
		}

		private Map<ModuleRevision, Collection<ModuleRevision>> getCollisionMap(Collection<ModuleRevision> sameBSN) {
			Map<ModuleRevision, Collection<ModuleRevision>> result = new HashMap<ModuleRevision, Collection<ModuleRevision>>();
			for (ModuleRevision singleton : sameBSN) {
				if (!isSingleton(singleton) || disabled.contains(singleton))
					continue; // ignore non-singleton and non-resolvable
				List<BundleCapability> capabilities = new ArrayList<BundleCapability>(sameBSN.size() - 1);
				for (ModuleRevision collision : sameBSN) {
					if (collision == singleton || !isSingleton(collision) || disabled.contains(collision))
						continue; // Ignore the bundle we are checking and non-singletons and non-resolvable
					capabilities.add(getIdentity(collision));
				}
				hook.filterSingletonCollisions(getIdentity(singleton), capabilities);
				Collection<ModuleRevision> collisionCandidates = new ArrayList<ModuleRevision>(capabilities.size());
				for (BundleCapability identity : capabilities) {
					collisionCandidates.add((ModuleRevision) identity.getRevision());
				}
				result.put(singleton, collisionCandidates);
			}
			return result;
		}

		private BundleCapability getIdentity(ModuleRevision bundle) {
			List<BundleCapability> identities = bundle.getDeclaredCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
			return identities.isEmpty() ? null : identities.get(0);
		}

		@Override
		public int compare(Capability c1, Capability c2) {
			// TODO Ideally this policy should be handled by the ModuleDatabase.
			// To do that the wirings would have to be provided since the wirings may
			// be a subset of the current wirings provided by the ModuleDatabase
			boolean resolved1 = wirings.get(c1.getResource()) != null;
			boolean resolved2 = wirings.get(c2.getResource()) != null;
			if (resolved1 != resolved2)
				return resolved1 ? -1 : 1;

			Version v1 = getVersion(c1);
			Version v2 = getVersion(c2);
			int versionCompare = -(v1.compareTo(v2));
			if (versionCompare != 0)
				return versionCompare;

			ModuleRevision m1 = getModuleRevision(c1);
			ModuleRevision m2 = getModuleRevision(c2);
			Long id1 = m1.getRevisions().getModule().getId();
			Long id2 = m2.getRevisions().getModule().getId();

			if (id1.equals(id2) && !m1.equals(m2)) {
				// sort based on revision ordering
				List<ModuleRevision> revisions = m1.getRevisions().getModuleRevisions();
				int index1 = revisions.indexOf(m1);
				int index2 = revisions.indexOf(m2);
				// we want to sort the indexes from highest to lowest
				return index2 - index1;
			}
			return id1.compareTo(id2);
		}

		ModuleRevision getModuleRevision(Capability c) {
			// We assume all capabilities here either come from us and have ModuleRevision resources or
			// they are HostedCapabilities which have ModuleRevision resources as the host revision
			if (c instanceof HostedCapability) {
				c = ((HostedCapability) c).getDeclaredCapability();
			}
			if (c instanceof ModuleCapability) {
				return ((ModuleCapability) c).getRevision();
			}
			// TODO is there some bug in the resolver?
			return null;
		}
	}
}
