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

import java.net.URL;
import java.util.*;
import org.eclipse.osgi.internal.container.Converters;
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
public class ModuleWiring implements BundleWiring {
	private static final RuntimePermission GET_CLASSLOADER_PERM = new RuntimePermission("getClassLoader"); //$NON-NLS-1$
	private final ModuleRevision revision;
	private final List<ModuleCapability> capabilities;
	private final List<ModuleRequirement> requirements;
	private final Collection<String> substitutedPkgNames;
	private final Object monitor = new Object();
	private ModuleClassLoader loader = null;
	private volatile List<ModuleWire> providedWires;
	private volatile List<ModuleWire> requiredWires;
	private volatile boolean isValid = true;

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
		return ((BundleRevision.TYPE_FRAGMENT & revision.getTypes()) != 0) && !getRequiredWires(HostNamespace.HOST_NAMESPACE).isEmpty();
	}

	public List<ModuleCapability> getModuleCapabilities(String namespace) {
		if (!isValid)
			return null;
		if (namespace == null)
			return new ArrayList<ModuleCapability>(capabilities);
		List<ModuleCapability> result = new ArrayList<ModuleCapability>();
		for (ModuleCapability capability : capabilities) {
			if (namespace.equals(capability.getNamespace())) {
				result.add(capability);
			}
		}
		return result;
	}

	public List<ModuleRequirement> getModuleRequirements(String namespace) {
		if (!isValid)
			return null;
		if (namespace == null)
			return new ArrayList<ModuleRequirement>(requirements);
		List<ModuleRequirement> result = new ArrayList<ModuleRequirement>();
		for (ModuleRequirement requirement : requirements) {
			if (namespace.equals(requirement.getNamespace())) {
				result.add(requirement);
			}
		}
		return result;
	}

	@Override
	public List<BundleCapability> getCapabilities(String namespace) {
		return Converters.asListBundleCapability(getModuleCapabilities(namespace));

	}

	@Override
	public List<BundleRequirement> getRequirements(String namespace) {
		return Converters.asListBundleRequirement(getModuleRequirements(namespace));
	}

	public List<ModuleWire> getProvidedModuleWires(String namespace) {
		return getWires(namespace, providedWires);
	}

	public List<ModuleWire> getRequiredModuleWires(String namespace) {
		return getWires(namespace, requiredWires);
	}

	@Override
	public List<BundleWire> getProvidedWires(String namespace) {
		return Converters.asListBundleWire(getWires(namespace, providedWires));
	}

	@Override
	public List<BundleWire> getRequiredWires(String namespace) {
		return Converters.asListBundleWire(getWires(namespace, requiredWires));
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
		return (ClassLoader) getModuleClassLoader();
	}

	public ModuleClassLoader getModuleClassLoader() {
		SecurityManager sm = System.getSecurityManager();
		if (sm != null) {
			sm.checkPermission(GET_CLASSLOADER_PERM);
		}
		synchronized (monitor) {
			if (!isValid) {
				return null;
			}
			if (loader == null) {
				loader = revision.getRevisions().getContainer().adaptor.createClassLoader(this);
			}
			return loader;
		}

	}

	@Override
	public List<URL> findEntries(String path, String filePattern, int options) {
		if (!hasResourcePermission())
			return Collections.emptyList();
		ModuleClassLoader current = getModuleClassLoader();
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
		ModuleClassLoader current = getModuleClassLoader();
		if (current == null) {
			// must not be valid
			return null;
		}
		return current.listResources(path, filePattern, options);
	}

	@Override
	public List<Capability> getResourceCapabilities(String namespace) {
		return Converters.asListCapability(getCapabilities(namespace));
	}

	@Override
	public List<Requirement> getResourceRequirements(String namespace) {
		return Converters.asListRequirement(getRequirements(namespace));
	}

	@Override
	public List<Wire> getProvidedResourceWires(String namespace) {
		return Converters.asListWire(getWires(namespace, providedWires));
	}

	@Override
	public List<Wire> getRequiredResourceWires(String namespace) {
		return Converters.asListWire(getWires(namespace, requiredWires));
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

	void invalidate() {
		synchronized (monitor) {
			this.isValid = false;
			if (loader != null) {
				loader.close();
				loader = null;
			}
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

	Collection<String> getSubstitutedNames() {
		return substitutedPkgNames;
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
}
