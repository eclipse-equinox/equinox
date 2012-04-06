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
import org.eclipse.osgi.container.wiring.ModuleWiring;
import org.osgi.framework.Version;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.namespace.*;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.*;
import org.osgi.service.resolver.*;

class ResolveProcess extends ResolveContext implements Comparator<Capability> {
	private final Collection<ModuleRevision> unresolved;
	private final Collection<ModuleRevision> disabled;
	private final Collection<ModuleRevision> triggers;
	private final ModuleDataBase moduleDataBase;
	private final Map<ModuleRevision, ModuleWiring> wirings;
	private final Resolver resolver;
	private final ResolverHookFactory resolverHookFactory;

	public ResolveProcess(Collection<ModuleRevision> unresolved, Collection<ModuleRevision> triggers, Map<ModuleRevision, ModuleWiring> wirings, ModuleDataBase moduleDataBase, Resolver resolver, ResolverHookFactory resolverHookFactory) {
		this.unresolved = unresolved;
		this.disabled = new HashSet<ModuleRevision>(unresolved);
		this.triggers = triggers;
		this.wirings = wirings;
		this.moduleDataBase = moduleDataBase;
		this.resolver = resolver;
		this.resolverHookFactory = resolverHookFactory;
	}

	@Override
	public List<Capability> findProviders(Requirement requirement) {
		List<Capability> candidates = moduleDataBase.findCapabilities((ModuleRequirement) requirement);
		Collections.sort(candidates, this);
		return candidates;
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
	public Collection<Resource> getOptionalResources() {
		@SuppressWarnings("unchecked")
		Collection<Resource> results = (Collection<Resource>) ((Collection<? extends Resource>) unresolved);
		return results;
	}

	Map<Resource, List<Wire>> resolve() throws ResolutionException {
		ResolverHook hook = resolverHookFactory.begin(Converters.asListBundleRevision((List<? extends BundleRevision>) triggers));
		try {
			filterResolvable(hook);
			selectSingletons(hook);
			// remove disabled from unresolved to prevent the resolver from resolving them
			unresolved.removeAll(disabled);
			return resolver.resolve(this);
		} finally {
			hook.end();
		}
	}

	private void filterResolvable(ResolverHook hook) {
		Collection<ModuleRevision> enabledCandidates = new ArrayList<ModuleRevision>(unresolved);
		hook.filterResolvable(Converters.asListBundleRevision((List<? extends BundleRevision>) enabledCandidates));
		disabled.removeAll(enabledCandidates);
	}

	private boolean isSingleton(ModuleRevision revision) {
		List<Capability> identities = revision.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
		if (identities.isEmpty())
			return false;
		return "true".equals(identities.get(0).getDirectives().get(IdentityNamespace.CAPABILITY_SINGLETON_DIRECTIVE)); //$NON-NLS-1$
	}

	private void selectSingletons(ResolverHook hook) {
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

			Collection<ModuleRevision> sameBSN = moduleDataBase.getRevisions(bsn, null);
			if (sameBSN.size() < 2) {
				selected.add(revision);
				continue;
			}
			// prime selected with resolved singleton bundles
			for (ModuleRevision singleton : sameBSN) {
				if (isSingleton(singleton) && singleton.getWiring() != null)
					selected.add(singleton);
			}
			// get the collision map for the BSN
			Map<ModuleRevision, Collection<ModuleRevision>> collisionMap = getCollisionMap(sameBSN, hook);
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

	private Map<ModuleRevision, Collection<ModuleRevision>> getCollisionMap(Collection<ModuleRevision> sameBSN, ResolverHook hook) {
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
		Long id1 = ((ModuleRevision) c1.getResource()).getRevisions().getId();
		Long id2 = ((ModuleRevision) c2.getResource()).getRevisions().getId();

		return id1 <= id2 ? -1 : 1;
	}

	private static Version getVersion(Capability c) {
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
}
