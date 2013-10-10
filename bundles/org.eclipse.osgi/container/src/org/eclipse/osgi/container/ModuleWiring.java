/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.container;

import java.net.URL;
import java.util.*;
import org.eclipse.osgi.container.ModuleRevisionBuilder.GenericInfo;
import org.eclipse.osgi.internal.container.InternalUtils;
import org.osgi.framework.AdminPermission;
import org.osgi.framework.Bundle;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.*;
import org.osgi.resource.*;

/**
 * An implementation of {@link BundleWiring}.
 * @since 3.10
 */
public final class ModuleWiring implements BundleWiring {
	private static final RuntimePermission GET_CLASSLOADER_PERM = new RuntimePermission("getClassLoader"); //$NON-NLS-1$
	private static final String DYNAMICALLY_ADDED_IMPORT_DIRECTIVE = "x.dynamically.added"; //$NON-NLS-1$
	private final ModuleRevision revision;
	private volatile List<ModuleCapability> capabilities;
	private volatile List<ModuleRequirement> requirements;
	private final Collection<String> substitutedPkgNames;
	private final Object monitor = new Object();
	private ModuleLoader loader = null;
	private volatile List<ModuleWire> providedWires;
	private volatile List<ModuleWire> requiredWires;
	private volatile boolean isValid = true;
	private Set<String> dynamicMisses;

	ModuleWiring(ModuleRevision revision, List<ModuleCapability> capabilities, List<ModuleRequirement> requirements, List<ModuleWire> providedWires, List<ModuleWire> requiredWires, Collection<String> substitutedPkgNames) {
		super();
		this.revision = revision;
		this.capabilities = capabilities;
		this.requirements = requirements;
		this.providedWires = providedWires;
		this.requiredWires = requiredWires;
		this.substitutedPkgNames = substitutedPkgNames;
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
	 * Returns the same result as {@link #getCapabilities(String)} except
	 * uses type ModuleCapability.
	 * @param namespace the namespace
	 * @return the capabilities
	 * @see #getCapabilities(String)
	 */
	public List<ModuleCapability> getModuleCapabilities(String namespace) {
		return getModuleCapabilities(namespace, capabilities);
	}

	private List<ModuleCapability> getModuleCapabilities(String namespace, List<ModuleCapability> allCapabilities) {
		if (!isValid)
			return null;
		if (namespace == null)
			return new ArrayList<ModuleCapability>(allCapabilities);
		List<ModuleCapability> result = new ArrayList<ModuleCapability>();
		for (ModuleCapability capability : allCapabilities) {
			if (namespace.equals(capability.getNamespace())) {
				result.add(capability);
			}
		}
		return result;
	}

	/**
	 * Returns the same result as {@link #getRequirements(String)} except
	 * uses type ModuleRequirement.
	 * @param namespace the namespace
	 * @return the requirements
	 * @see #getRequirements(String)
	 */
	public List<ModuleRequirement> getModuleRequirements(String namespace) {
		return getModuleRequirements(namespace, requirements);
	}

	List<ModuleRequirement> getPersistentRequirements() {
		List<ModuleRequirement> persistentRequriements = getModuleRequirements(null);
		if (persistentRequriements == null) {
			return null;
		}
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

	private List<ModuleRequirement> getModuleRequirements(String namespace, List<ModuleRequirement> allRequirements) {
		if (!isValid)
			return null;
		if (namespace == null)
			return new ArrayList<ModuleRequirement>(allRequirements);
		List<ModuleRequirement> result = new ArrayList<ModuleRequirement>();
		for (ModuleRequirement requirement : allRequirements) {
			if (namespace.equals(requirement.getNamespace())) {
				result.add(requirement);
			}
		}
		return result;
	}

	@Override
	public List<BundleCapability> getCapabilities(String namespace) {
		return InternalUtils.asListBundleCapability(getModuleCapabilities(namespace));

	}

	@Override
	public List<BundleRequirement> getRequirements(String namespace) {
		return InternalUtils.asListBundleRequirement(getModuleRequirements(namespace));
	}

	/**
	 * Returns the same result as {@link #getProvidedWires(String)} except
	 * uses type ModuleWire.
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
	 * Returns the same result as {@link #getRequiredWires(String)} except
	 * uses type ModuleWire.
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

	private List<ModuleWire> getPersistentWires(List<ModuleWire> allWires) {
		List<ModuleWire> persistentWires = getWires(null, allWires);
		if (persistentWires == null) {
			return null;
		}
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
		return InternalUtils.asListBundleWire(getWires(namespace, providedWires));
	}

	@Override
	public List<BundleWire> getRequiredWires(String namespace) {
		return InternalUtils.asListBundleWire(getWires(namespace, requiredWires));
	}

	private List<ModuleWire> getWires(String namespace, List<ModuleWire> allWires) {
		if (!isValid)
			return null;
		if (namespace == null)
			return new ArrayList<ModuleWire>(allWires);
		List<ModuleWire> result = new ArrayList<ModuleWire>();
		for (ModuleWire moduleWire : allWires) {
			if (namespace.equals(moduleWire.getCapability().getNamespace())) {
				result.add(moduleWire);
			}
		}
		return result;
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
		synchronized (monitor) {
			if (loader == null) {
				if (!isValid) {
					return null;
				}
				loader = revision.getRevisions().getContainer().adaptor.createModuleLoader(this);
			}
			return loader;
		}

	}

	void loadFragments(Collection<ModuleRevision> fragments) {
		synchronized (monitor) {
			if (loader != null) {
				loader.loadFragments(fragments);
			}
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
		return InternalUtils.asListCapability(getCapabilities(namespace));
	}

	@Override
	public List<Requirement> getResourceRequirements(String namespace) {
		return InternalUtils.asListRequirement(getRequirements(namespace));
	}

	@Override
	public List<Wire> getProvidedResourceWires(String namespace) {
		return InternalUtils.asListWire(getWires(namespace, providedWires));
	}

	@Override
	public List<Wire> getRequiredResourceWires(String namespace) {
		return InternalUtils.asListWire(getWires(namespace, requiredWires));
	}

	@Override
	public ModuleRevision getResource() {
		return revision;
	}

	void setProvidedWires(List<ModuleWire> providedWires) {
		this.providedWires = providedWires;
	}

	void setRequiredWires(List<ModuleWire> requiredWires) {
		this.requiredWires = requiredWires;
	}

	void setCapabilities(List<ModuleCapability> capabilities) {
		this.capabilities = capabilities;
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
		ModuleLoader current;
		synchronized (monitor) {
			this.isValid = false;
			current = loader;
			if (releaseLoader) {
				loader = null;
			}
		}
		revision.getRevisions().getContainer().getAdaptor().invalidateWiring(this, current);
	}

	void validate() {
		synchronized (monitor) {
			this.isValid = true;
		}
	}

	boolean isSubtituted(ModuleCapability capability) {
		if (!PackageNamespace.PACKAGE_NAMESPACE.equals(capability.getNamespace())) {
			return false;
		}
		return substitutedPkgNames.contains(capability.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
	}

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

	public void addDynamicImports(ModuleRevisionBuilder builder) {
		List<GenericInfo> newImports = builder.getRequirements();
		List<ModuleRequirement> newRequirements = new ArrayList<ModuleRequirement>();
		for (GenericInfo info : newImports) {
			if (!PackageNamespace.PACKAGE_NAMESPACE.equals(info.getNamespace())) {
				throw new IllegalArgumentException("Invalid namespace for package imports: " + info.getNamespace()); //$NON-NLS-1$
			}
			Map<String, Object> attributes = new HashMap<String, Object>(info.getAttributes());
			Map<String, String> directives = new HashMap<String, String>(info.getDirectives());
			directives.put(DYNAMICALLY_ADDED_IMPORT_DIRECTIVE, "true"); //$NON-NLS-1$
			directives.put(PackageNamespace.REQUIREMENT_RESOLUTION_DIRECTIVE, PackageNamespace.RESOLUTION_DYNAMIC);
			newRequirements.add(new ModuleRequirement(info.getNamespace(), directives, attributes, revision));
		}
		ModuleDatabase moduleDatabase = revision.getRevisions().getContainer().moduleDatabase;
		moduleDatabase.writeLock();
		try {
			List<ModuleRequirement> updatedRequirements = new ArrayList<ModuleRequirement>(requirements);
			updatedRequirements.addAll(newRequirements);
			requirements = updatedRequirements;
		} finally {
			moduleDatabase.writeUnlock();
		}
	}

	void addDynamicPackageMiss(String packageName) {
		synchronized (monitor) {
			if (dynamicMisses == null) {
				dynamicMisses = new HashSet<String>(5);
			}
			dynamicMisses.add(packageName);
		}
	}

	boolean isDynamicPackageMiss(String packageName) {
		synchronized (monitor) {
			return dynamicMisses != null && dynamicMisses.contains(packageName);
		}
	}

	void removeDynamicPackageMisses(Collection<String> packageNames) {
		synchronized (monitor) {
			if (dynamicMisses == null) {
				return;
			}
			dynamicMisses.removeAll(packageNames);
		}
	}
}
