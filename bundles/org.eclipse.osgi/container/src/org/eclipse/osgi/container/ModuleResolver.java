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

import java.util.*;
import org.apache.felix.resolver.ResolverImpl;
import org.eclipse.osgi.container.ModuleRequirement.DynamicModuleRequirement;
import org.eclipse.osgi.internal.container.Converters;
import org.osgi.framework.Version;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.namespace.*;
import org.osgi.framework.wiring.*;
import org.osgi.resource.*;
import org.osgi.service.resolver.*;

/**
 * The module resolver handles calls to the {@link Resolver} service for resolving modules
 * in a module {@link ModuleContainer container}.
 */
class ModuleResolver {
	final ModuleContainerAdaptor adaptor;

	/**
	 * Constructs the module resolver with the specified resolver hook factory
	 * and resolver.
	 * @param adaptor the container adaptor
	 */
	ModuleResolver(ModuleContainerAdaptor adaptor) {
		this.adaptor = adaptor;
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
	 * @param moduleDataBase the module database.
	 * @return a delta container the new wirings or modified wirings that should be
	 * merged into the moduleDatabase
	 * @throws ResolutionException
	 */
	Map<ModuleRevision, ModuleWiring> resolveDelta(Collection<ModuleRevision> triggers, boolean triggersMandatory, Collection<ModuleRevision> unresolved, Map<ModuleRevision, ModuleWiring> wiringCopy, ModuleDataBase moduleDataBase) throws ResolutionException {
		ResolveProcess resolveProcess = new ResolveProcess(unresolved, triggers, triggersMandatory, wiringCopy, moduleDataBase);
		Map<Resource, List<Wire>> result = resolveProcess.resolve();
		return generateDelta(result, wiringCopy);
	}

	Map<ModuleRevision, ModuleWiring> resolveDynamicDelta(DynamicModuleRequirement dynamicReq, Collection<ModuleRevision> unresolved, Map<ModuleRevision, ModuleWiring> wiringCopy, ModuleDataBase moduleDataBase) throws ResolutionException {
		ResolveProcess resolveProcess = new ResolveProcess(unresolved, dynamicReq, wiringCopy, moduleDataBase);
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

		// add fragment capabilities and requirements
		List<ModuleCapability> hostCapabilities = revision.getModuleCapabilities(HostNamespace.HOST_NAMESPACE);
		ModuleCapability hostCapability = hostCapabilities.isEmpty() ? null : hostCapabilities.get(0);
		if (hostCapability != null) {
			addFragmentContent(providedWireMap.get(hostCapability), iCapabilities, iRequirements);
		}

		removeNonEffectiveCapabilities(iCapabilities);
		removeNonEffectiveRequirements(iRequirements, requiredWires);
		Collection<String> substituted = removeSubstitutedCapabilities(iCapabilities, requiredWires);

		List<ModuleWire> providedWires = new ArrayList<ModuleWire>();
		addProvidedWires(providedWireMap, providedWires, capabilities);

		return new ModuleWiring(revision, capabilities, requirements, providedWires, requiredWires, substituted);
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
		requirements: while (iRequirements.hasNext()) {
			ModuleRequirement requirement = iRequirements.next();
			// check the effective directive;
			Object effective = requirement.getAttributes().get(Namespace.REQUIREMENT_EFFECTIVE_DIRECTIVE);
			if (effective != null && !Namespace.EFFECTIVE_RESOLVE.equals(effective)) {
				iRequirements.remove();
				break requirements;
			}
			// check the resolution directive
			Object resolution = requirement.getAttributes().get(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE);
			if (resolution != null && !Namespace.RESOLUTION_MANDATORY.equals(resolution)) {
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
					break requirements;
				}
			}
		}
	}

	static void removeNonEffectiveCapabilities(ListIterator<ModuleCapability> iCapabilities) {
		rewind(iCapabilities);
		while (iCapabilities.hasNext()) {
			Object effective = iCapabilities.next().getAttributes().get(Namespace.CAPABILITY_EFFECTIVE_DIRECTIVE);
			if (effective != null && !Namespace.EFFECTIVE_RESOLVE.equals(effective))
				iCapabilities.remove();
		}
	}

	private static void addFragmentContent(List<ModuleWire> hostWires, ListIterator<ModuleCapability> iCapabilities, ListIterator<ModuleRequirement> iRequirements) {
		if (hostWires == null)
			return;
		for (ModuleWire hostWire : hostWires) {
			// add fragment capabilities
			String currentNamespace = null;
			List<ModuleCapability> fragmentCapabilities = hostWire.getRequirer().getModuleCapabilities(null);
			for (ModuleCapability fragmentCapability : fragmentCapabilities) {
				if (IdentityNamespace.IDENTITY_NAMESPACE.equals(fragmentCapability.getNamespace()))
					continue; // identity is not a payload
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
				String fragNamespace = fragmentRequirement.getNamespace();
				if (HostNamespace.HOST_NAMESPACE.equals(fragNamespace) || ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE.equals(fragNamespace))
					continue; // host and osgi.ee is not a payload
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
		List<ModuleWire> existingProvided = existingWiring.getProvidedModuleWires(null);
		addProvidedWires(providedWireMap, existingProvided, existingWiring.getModuleCapabilities(null));

		// Also need to include any new required wires that may have be added for fragment hosts
		// Also will be needed for dynamic imports
		List<ModuleWire> existingRequired = existingWiring.getRequiredModuleWires(null);
		addRequiredWires(requiredWires, existingRequired, existingWiring.getModuleRequirements(null));
		return new ModuleWiring(revision, Collections.EMPTY_LIST, Collections.EMPTY_LIST, existingProvided, existingRequired, Collections.EMPTY_LIST);
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

	class ResolveProcess extends ResolveContext implements Comparator<Capability> {
		private final Collection<ModuleRevision> unresolved;
		private final Collection<ModuleRevision> disabled;
		private final Collection<ModuleRevision> triggers;
		private final Collection<ModuleRevision> optionals;
		private final boolean triggersMandatory;
		private final ModuleDataBase moduleDataBase;
		private final Map<ModuleRevision, ModuleWiring> wirings;
		private final DynamicModuleRequirement dynamicReq;
		private volatile ResolverHook hook = null;
		private volatile Map<String, Collection<ModuleRevision>> byName = null;

		ResolveProcess(Collection<ModuleRevision> unresolved, Collection<ModuleRevision> triggers, boolean triggersMandatory, Map<ModuleRevision, ModuleWiring> wirings, ModuleDataBase moduleDataBase) {
			this.unresolved = unresolved;
			this.disabled = new HashSet<ModuleRevision>(unresolved);
			this.triggers = triggers;
			this.triggersMandatory = triggersMandatory;
			this.optionals = new ArrayList<ModuleRevision>(unresolved);
			if (this.triggersMandatory) {
				this.optionals.removeAll(triggers);
			}
			this.wirings = wirings;
			this.moduleDataBase = moduleDataBase;
			this.dynamicReq = null;
		}

		ResolveProcess(Collection<ModuleRevision> unresolved, DynamicModuleRequirement dynamicReq, Map<ModuleRevision, ModuleWiring> wirings, ModuleDataBase moduleDataBase) {
			this.unresolved = unresolved;
			this.disabled = new HashSet<ModuleRevision>(unresolved);
			ModuleRevision revision = dynamicReq.getRevision();
			this.triggers = new ArrayList<ModuleRevision>(1);
			this.triggers.add(revision);
			this.triggersMandatory = false;
			this.optionals = new ArrayList<ModuleRevision>(unresolved);
			this.wirings = wirings;
			this.moduleDataBase = moduleDataBase;
			this.dynamicReq = dynamicReq;
		}

		@Override
		public List<Capability> findProviders(Requirement requirement) {
			List<ModuleCapability> candidates = moduleDataBase.findCapabilities((ModuleRequirement) requirement);
			return filterProviders(requirement, candidates);
		}

		private List<Capability> filterProviders(Requirement requirement, List<ModuleCapability> candidates) {
			ListIterator<ModuleCapability> iCandidates = candidates.listIterator();
			filterDisabled(iCandidates);
			removeNonEffectiveCapabilities(iCandidates);
			removeSubstituted(iCandidates);
			hook.filterMatches((BundleRequirement) requirement, Converters.asListBundleCapability(candidates));
			Collections.sort(candidates, this);
			return Converters.asListCapability(candidates);
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
			hook = adaptor.getResolverHookFactory().begin(Converters.asListBundleRevision((List<? extends BundleRevision>) triggers));
			try {
				filterResolvable();
				selectSingletons();
				// remove disabled from optional and triggers to prevent the resolver from resolving them
				optionals.removeAll(disabled);
				if (triggers.removeAll(disabled) && triggersMandatory) {
					throw new ResolutionException("Could not resolve mandatory modules because another singleton was selected or the module was disabled: " + disabled);
				}
				if (dynamicReq != null) {
					return resolveDynamic();
				}
				return adaptor.getResolver().resolve(this);
			} finally {
				hook.end();
			}
		}

		private Map<Resource, List<Wire>> resolveDynamic() throws ResolutionException {
			Resolver resolver = adaptor.getResolver();
			if (!(resolver instanceof ResolverImpl)) {
				throw new ResolutionException("Dynamic import resolution not supported by the resolver: " + resolver.getClass());
			}
			List<Capability> dynamicMatches = filterProviders(dynamicReq.getOriginal(), moduleDataBase.findCapabilities(dynamicReq));
			Collection<Resource> ondemandFragments = Converters.asCollectionResource(moduleDataBase.getFragmentRevisions());

			return ((ResolverImpl) resolver).resolve(this, dynamicReq.getRevision(), dynamicReq.getOriginal(), dynamicMatches, ondemandFragments);

		}

		private void filterResolvable() {
			Collection<ModuleRevision> enabledCandidates = new ArrayList<ModuleRevision>(unresolved);
			hook.filterResolvable(Converters.asListBundleRevision((List<? extends BundleRevision>) enabledCandidates));
			disabled.removeAll(enabledCandidates);
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

				// TODO out of band call that obtains the read lock
				// Should generate our own copy Map<String, Collection<ModuleRevision>> 
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
							// TODO add resolver diagnostics here
							//state.addResolverError(singleton.getBundleDescription(), ResolverError.SINGLETON_SELECTION, collision.getBundleDescription().toString(), null);
							break;
						}
						if (!pickOneToResolve.contains(collision))
							pickOneToResolve.add(collision);
					}
					// need to make sure the bundle does not collide from the POV of another entry
					for (Map.Entry<ModuleRevision, Collection<ModuleRevision>> collisionEntry : collisionMap.entrySet()) {
						if (collisionEntry.getKey() != singleton && collisionEntry.getValue().contains(singleton)) {
							if (selected.contains(collisionEntry.getKey())) {
								// Must fail since there is already a selected bundle for which the singleton bundle is a collision
								disabled.add(singleton);
								// TODO add resolver diagnostics here
								// state.addResolverError(singleton.getBundleDescription(), ResolverError.SINGLETON_SELECTION, collisionEntry.getKey().getBundleDescription().toString(), null);
								break;
							}
							if (!pickOneToResolve.contains(collisionEntry.getKey()))
								pickOneToResolve.add(collisionEntry.getKey());
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
					// TODO add resolver diagnostic here.
					// state.addResolverError(singleton.getBundleDescription(), ResolverError.SINGLETON_SELECTION, selectedVersion.getBundleDescription().toString(), null);
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
			// TODO Ideally this policy should be handled by the ModuleDataBase.
			// To do that the wirings would have to be provided since the wirings may
			// be a subset of the current wirings provided by the ModuleDataBase
			boolean resolved1 = wirings.get(c1.getResource()) != null;
			boolean resolved2 = wirings.get(c2.getResource()) != null;
			if (resolved1 != resolved2)
				return resolved1 ? -1 : 1;

			Version v1 = getVersion(c1);
			Version v2 = getVersion(c2);
			int versionCompare = -(v1.compareTo(v2));
			if (versionCompare != 0)
				return versionCompare;

			// We assume all resources here come from us and are ModuleRevision objects
			ModuleRevision m1 = (ModuleRevision) c1.getResource();
			ModuleRevision m2 = (ModuleRevision) c2.getResource();
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
	}
}
