/*******************************************************************************
 * Copyright (c) 2012, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.container;

import static org.eclipse.osgi.internal.container.InternalUtils.asCopy;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.osgi.container.ModuleRevisionBuilder.GenericInfo;
import org.eclipse.osgi.internal.container.AtomicLazyInitializer;
import org.eclipse.osgi.internal.container.NamespaceList;
import org.osgi.framework.AdminPermission;
import org.osgi.framework.Bundle;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Wire;

/**
 * An implementation of {@link BundleWiring}.
 * @since 3.10
 */
public final class ModuleWiring implements BundleWiring {
	class LoaderInitializer implements Callable<ModuleLoader> {
		@Override
		public ModuleLoader call() throws Exception {
			if (!isValid) {
				return null;
			}
			return getRevision().getRevisions().getContainer().adaptor.createModuleLoader(ModuleWiring.this);
		}
	}

	private static final RuntimePermission GET_CLASSLOADER_PERM = new RuntimePermission("getClassLoader"); //$NON-NLS-1$
	private static final String DYNAMICALLY_ADDED_IMPORT_DIRECTIVE = "x.dynamically.added"; //$NON-NLS-1$
	private final ModuleRevision revision;
	private volatile NamespaceList<ModuleCapability> capabilities;
	private volatile NamespaceList<ModuleRequirement> requirements;
	private final Collection<String> substitutedPkgNames;
	private final AtomicLazyInitializer<ModuleLoader> loader = new AtomicLazyInitializer<>();
	private final LoaderInitializer loaderInitializer = new LoaderInitializer();
	private volatile NamespaceList<ModuleWire> providedWires;
	private volatile NamespaceList<ModuleWire> requiredWires;
	volatile boolean isValid = true;
	private final AtomicReference<Set<String>> dynamicMissRef = new AtomicReference<>();

	ModuleWiring(ModuleRevision revision, NamespaceList<ModuleCapability> capabilities,
			NamespaceList<ModuleRequirement> requirements, NamespaceList<ModuleWire> providedWires,
			NamespaceList<ModuleWire> requiredWires, Collection<String> substitutedPkgNames) {
		super();
		this.revision = revision;
		this.capabilities = capabilities;
		this.requirements = requirements;
		this.providedWires = providedWires;
		this.requiredWires = requiredWires;
		this.substitutedPkgNames = substitutedPkgNames.isEmpty() ? Collections.emptyList() : substitutedPkgNames;
	}

	@Override
	public Bundle getBundle() {
		return revision.getBundle();
	}

	@Override
	public boolean isCurrent() {
		return isValid && revision.isCurrent();
	}

	@Override
	public boolean isInUse() {
		return isCurrent() || !providedWires.isEmpty() || isFragmentInUse();
	}

	private boolean isFragmentInUse() {
		// A fragment is considered in use if it has any required host wires
		if ((BundleRevision.TYPE_FRAGMENT & revision.getTypes()) != 0) {
			List<ModuleWire> hostWires = getRequiredModuleWires(HostNamespace.HOST_NAMESPACE);
			// hostWires may be null if the fragment wiring is no longer valid
			return hostWires == null ? false : !hostWires.isEmpty();
		}
		return false;
	}

	/**
	 * Returns the same result as {@link #getCapabilities(String)} except uses type
	 * ModuleCapability and the returned list is unmodifiable.
	 * 
	 * @param namespace the namespace
	 * @return the capabilities
	 * @see #getCapabilities(String)
	 */
	public List<ModuleCapability> getModuleCapabilities(String namespace) {
		if (!isValid) {
			return null;
		}
		return capabilities.getList(namespace);
	}

	/**
	 * Returns the same result as {@link #getRequirements(String)} except uses type
	 * ModuleRequirement and the returned list is unmodifiable.
	 * 
	 * @param namespace the namespace
	 * @return the requirements
	 * @see #getRequirements(String)
	 */
	public List<ModuleRequirement> getModuleRequirements(String namespace) {
		if (!isValid) {
			return null;
		}
		return requirements.getList(namespace);
	}

	List<ModuleRequirement> getPersistentRequirements() {
		if (!isValid) {
			return null;
		}
		List<ModuleRequirement> persistentRequriements = new ArrayList<>(requirements.getList(null));
		for (Iterator<ModuleRequirement> iRequirements = persistentRequriements.iterator(); iRequirements.hasNext();) {
			ModuleRequirement requirement = iRequirements.next();
			if (PackageNamespace.PACKAGE_NAMESPACE.equals(requirement.getNamespace())) {
				if ("true".equals(requirement.getDirectives().get(DYNAMICALLY_ADDED_IMPORT_DIRECTIVE))) { //$NON-NLS-1$
					iRequirements.remove();
				}
			}
		}
		return persistentRequriements;
	}

	@Override
	public List<BundleCapability> getCapabilities(String namespace) {
		return asCopy(getModuleCapabilities(namespace));

	}

	@Override
	public List<BundleRequirement> getRequirements(String namespace) {
		return asCopy(getModuleRequirements(namespace));
	}

	/**
	 * Returns the same result as {@link #getProvidedWires(String)} except uses type
	 * ModuleWire and the returned list is unmodifiable.
	 * 
	 * @param namespace the namespace
	 * @return the wires
	 * @see #getProvidedWires(String)
	 */
	public List<ModuleWire> getProvidedModuleWires(String namespace) {
		return getWires(namespace, providedWires);
	}

	List<ModuleWire> getPersistentProvidedWires() {
		return getPersistentWires(providedWires);
	}

	/**
	 * Returns the same result as {@link #getRequiredWires(String)} except uses type
	 * ModuleWire and the returned list is unmodifiable.
	 * 
	 * @param namespace the namespace
	 * @return the wires
	 * @see #getRequiredWires(String)
	 */
	public List<ModuleWire> getRequiredModuleWires(String namespace) {
		return getWires(namespace, requiredWires);
	}

	List<ModuleWire> getPersistentRequiredWires() {
		return getPersistentWires(requiredWires);
	}

	private List<ModuleWire> getPersistentWires(NamespaceList<ModuleWire> allWires) {
		if (!isValid) {
			return null;
		}
		List<ModuleWire> persistentWires = new ArrayList<>(allWires.getList(null));
		for (Iterator<ModuleWire> iWires = persistentWires.iterator(); iWires.hasNext();) {
			ModuleWire wire = iWires.next();
			if (PackageNamespace.PACKAGE_NAMESPACE.equals(wire.getRequirement().getNamespace())) {
				if ("true".equals(wire.getRequirement().getDirectives().get(DYNAMICALLY_ADDED_IMPORT_DIRECTIVE))) { //$NON-NLS-1$
					iWires.remove();
				}
			}
		}
		return persistentWires;
	}

	@Override
	public List<BundleWire> getProvidedWires(String namespace) {
		return asCopy(getWires(namespace, providedWires));
	}

	@Override
	public List<BundleWire> getRequiredWires(String namespace) {
		return asCopy(getWires(namespace, requiredWires));
	}

	private List<ModuleWire> getWires(String namespace, NamespaceList<ModuleWire> wires) {
		if (!isValid) {
			return null;
		}
		return wires.getList(namespace);
	}

	@Override
	public ModuleRevision getRevision() {
		return revision;
	}

	@Override
	public ClassLoader getClassLoader() {
		SecurityManager sm = System.getSecurityManager();
		if (sm != null) {
			sm.checkPermission(GET_CLASSLOADER_PERM);
		}

		if (!isValid) {
			return null;
		}
		ModuleLoader current = getModuleLoader();
		if (current == null) {
			// must not be valid
			return null;
		}
		return current.getClassLoader();
	}

	/**
	 * Returns the module loader for this wiring.  If the module
	 * loader does not exist yet then one will be created
	 * @return the module loader for this wiring.
	 */
	public ModuleLoader getModuleLoader() {
		return loader.getInitialized(loaderInitializer);

	}

	void loadFragments(Collection<ModuleRevision> fragments) {
		ModuleLoader current = loader.get();
		if (current != null) {
			current.loadFragments(fragments);
		}
	}

	@Override
	public List<URL> findEntries(String path, String filePattern, int options) {
		if (!hasResourcePermission())
			return Collections.emptyList();
		if (!isValid) {
			return null;
		}
		ModuleLoader current = getModuleLoader();
		if (current == null) {
			// must not be valid
			return null;
		}
		return current.findEntries(path, filePattern, options);
	}

	@Override
	public Collection<String> listResources(String path, String filePattern, int options) {
		if (!hasResourcePermission())
			return Collections.emptyList();
		if (!isValid) {
			return null;
		}
		ModuleLoader current = getModuleLoader();
		if (current == null) {
			// must not be valid
			return null;
		}
		return current.listResources(path, filePattern, options);
	}

	@Override
	public List<Capability> getResourceCapabilities(String namespace) {
		return asCopy(getModuleCapabilities(namespace));
	}

	@Override
	public List<Requirement> getResourceRequirements(String namespace) {
		return asCopy(getModuleRequirements(namespace));
	}

	@Override
	public List<Wire> getProvidedResourceWires(String namespace) {
		return asCopy(getWires(namespace, providedWires));
	}

	@Override
	public List<Wire> getRequiredResourceWires(String namespace) {
		return asCopy(getWires(namespace, requiredWires));
	}

	@Override
	public ModuleRevision getResource() {
		return revision;
	}

	void setProvidedWires(NamespaceList<ModuleWire> providedWires) {
		this.providedWires = providedWires;
	}

	void setRequiredWires(NamespaceList<ModuleWire> requiredWires) {
		this.requiredWires = requiredWires;
	}

	void setCapabilities(NamespaceList<ModuleCapability> capabilities) {
		this.capabilities = capabilities;
	}

	void setRequirements(NamespaceList<ModuleRequirement> requirements) {
		this.requirements = requirements;
	}

	void unload() {
		// When unloading a wiring we need to release the loader.
		// This is so that the loaders are not pinned when stopping the framework.
		// Then the framework can be relaunched, at which point new loaders will
		// get created.
		invalidate0(true);
	}

	void invalidate() {
		invalidate0(false);
	}

	private void invalidate0(boolean releaseLoader) {
		// set the isValid to false first
		isValid = false;
		ModuleLoader current = releaseLoader ? loader.getAndClear() : loader.get();
		revision.getRevisions().getContainer().getAdaptor().invalidateWiring(this, current);
	}

	void validate() {
		this.isValid = true;
	}

	boolean isSubtituted(ModuleCapability capability) {
		if (!PackageNamespace.PACKAGE_NAMESPACE.equals(capability.getNamespace())) {
			return false;
		}
		return substitutedPkgNames.contains(capability.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
	}

	/**
	 * Returns true if the specified package name has been substituted in this wiring
	 * @param packageName the package name to check
	 * @return true if the specified package name has been substituted in this wiring
	 */
	public boolean isSubstitutedPackage(String packageName) {
		return substitutedPkgNames.contains(packageName);
	}

	/**
	 * Returns an unmodifiable collection of package names for
	 * package capabilities that have been substituted.
	 * @return the substituted package names
	 */
	public Collection<String> getSubstitutedNames() {
		return Collections.unmodifiableCollection(substitutedPkgNames);
	}

	private boolean hasResourcePermission() {
		SecurityManager sm = System.getSecurityManager();
		if (sm != null) {
			try {
				sm.checkPermission(new AdminPermission(getBundle(), AdminPermission.RESOURCE));
			} catch (SecurityException e) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Adds the {@link ModuleRevisionBuilder#getRequirements() requirements} from
	 * the specified builder to this wiring.  The new requirements must be in the
	 * {@link PackageNamespace}.  These requirements are transient
	 * and will not exist when loading up persistent wirings.
	 * @param builder the builder that defines the new dynamic imports.
	 */
	public void addDynamicImports(ModuleRevisionBuilder builder) {
		NamespaceList.Builder<GenericInfo> newImports = builder.getRequirementsBuilder();
		NamespaceList.Builder<ModuleRequirement> newRequirements = newImports.transformIntoCopy(info -> {
			if (!PackageNamespace.PACKAGE_NAMESPACE.equals(info.getNamespace())) {
				throw new IllegalArgumentException("Invalid namespace for package imports: " + info.getNamespace()); //$NON-NLS-1$
			}
			Map<String, Object> attributes = new HashMap<>(info.getAttributes());
			Map<String, String> directives = new HashMap<>(info.getDirectives());
			directives.put(DYNAMICALLY_ADDED_IMPORT_DIRECTIVE, "true"); //$NON-NLS-1$
			directives.put(PackageNamespace.REQUIREMENT_RESOLUTION_DIRECTIVE, PackageNamespace.RESOLUTION_DYNAMIC);
			return new ModuleRequirement(info.getNamespace(), directives, attributes, revision);
		}, NamespaceList.REQUIREMENT);

		ModuleDatabase moduleDatabase = revision.getRevisions().getContainer().moduleDatabase;
		moduleDatabase.writeLock();
		try {
			NamespaceList.Builder<ModuleRequirement> requirmentsBuilder = requirements.createBuilder();
			requirmentsBuilder.addAll(newRequirements);
			requirements = requirmentsBuilder.build();
		} finally {
			moduleDatabase.writeUnlock();
		}
	}

	void addDynamicPackageMiss(String packageName) {
		Set<String> misses = dynamicMissRef.get();
		if (misses == null) {
			dynamicMissRef.compareAndSet(null, Collections.synchronizedSet(new HashSet<String>()));
			misses = dynamicMissRef.get();
		}

		misses.add(packageName);
	}

	boolean isDynamicPackageMiss(String packageName) {
		Set<String> misses = dynamicMissRef.get();
		return misses != null && misses.contains(packageName);
	}

	void removeDynamicPackageMisses(Collection<String> packageNames) {
		Set<String> misses = dynamicMissRef.get();
		if (misses != null) {
			misses.removeAll(packageNames);
		}
	}

	@Override
	public String toString() {
		return revision.toString();
	}

	List<Wire> getSubstitutionWires() {
		if (substitutedPkgNames.isEmpty()) {
			return Collections.emptyList();
		}
		// Could cache this, but seems unnecessary since it will only be used by the resolver
		List<Wire> substitutionWires = new ArrayList<>(substitutedPkgNames.size());
		List<ModuleWire> current = requiredWires.getList(PackageNamespace.PACKAGE_NAMESPACE);
		for (ModuleWire wire : current) {
			Capability cap = wire.getCapability();
			if (substitutedPkgNames.contains(cap.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE))) {
				substitutionWires.add(wire);
			}
		}
		return substitutionWires;
	}

	NamespaceList<ModuleCapability> getCapabilities() {
		return capabilities;
	}

	NamespaceList<ModuleWire> getProvidedWires() {
		return providedWires;
	}

	NamespaceList<ModuleRequirement> getRequirements() {
		return requirements;
	}

	NamespaceList<ModuleWire> getRequiredWires() {
		return requiredWires;
	}
}
