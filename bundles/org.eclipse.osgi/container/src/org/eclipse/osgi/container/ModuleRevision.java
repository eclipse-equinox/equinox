/*******************************************************************************
 * Copyright (c) 2012, 2016 IBM Corporation and others.
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

import java.util.*;
import java.util.Map.Entry;
import org.eclipse.osgi.container.ModuleRevisionBuilder.GenericInfo;
import org.eclipse.osgi.container.namespaces.EquinoxModuleDataNamespace;
import org.eclipse.osgi.internal.container.InternalUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.wiring.*;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

/**
 * An implementation of {@link BundleRevision}.
 * @since 3.10
 */
public final class ModuleRevision implements BundleRevision {
	private final String symbolicName;
	private final Version version;
	private final int types;
	private final List<ModuleCapability> capabilities;
	private final List<ModuleRequirement> requirements;
	private final ModuleRevisions revisions;
	private final Object revisionInfo;
	private volatile Boolean lazyActivationPolicy = null;

	ModuleRevision(String symbolicName, Version version, int types, List<GenericInfo> capabilityInfos, List<GenericInfo> requirementInfos, ModuleRevisions revisions, Object revisionInfo) {
		this.symbolicName = symbolicName;
		this.version = version;
		this.types = types;
		this.capabilities = createCapabilities(capabilityInfos);
		this.requirements = createRequirements(requirementInfos);
		this.revisions = revisions;
		this.revisionInfo = revisionInfo;
	}

	private List<ModuleCapability> createCapabilities(List<GenericInfo> capabilityInfos) {
		if (capabilityInfos == null || capabilityInfos.isEmpty())
			return Collections.emptyList();
		List<ModuleCapability> result = new ArrayList<>(capabilityInfos.size());
		for (GenericInfo info : capabilityInfos) {
			result.add(new ModuleCapability(info.namespace, info.directives, info.attributes, this));
		}
		return result;
	}

	private List<ModuleRequirement> createRequirements(List<GenericInfo> requirementInfos) {
		if (requirementInfos == null || requirementInfos.isEmpty())
			return Collections.emptyList();
		List<ModuleRequirement> result = new ArrayList<>(requirementInfos.size());
		for (GenericInfo info : requirementInfos) {
			result.add(new ModuleRequirement(info.namespace, info.directives, info.attributes, this));
		}
		return result;
	}

	@Override
	public Bundle getBundle() {
		return revisions.getBundle();
	}

	@Override
	public String getSymbolicName() {
		return symbolicName;
	}

	@Override
	public Version getVersion() {
		return version;
	}

	@Override
	public List<BundleCapability> getDeclaredCapabilities(String namespace) {
		return InternalUtils.asListBundleCapability(getModuleCapabilities(namespace));
	}

	@Override
	public List<BundleRequirement> getDeclaredRequirements(String namespace) {
		return InternalUtils.asListBundleRequirement(getModuleRequirements(namespace));
	}

	/**
	 * Returns the capabilities declared by this revision
	 * @param namespace The namespace of the declared capabilities to return or 
	 * {@code null} to return the declared capabilities from all namespaces. 
	 * @return An unmodifiable list containing the declared capabilities.
	 */
	public List<ModuleCapability> getModuleCapabilities(String namespace) {
		if (namespace == null)
			return Collections.unmodifiableList(capabilities);
		List<ModuleCapability> result = new ArrayList<>();
		for (ModuleCapability capability : capabilities) {
			if (namespace.equals(capability.getNamespace())) {
				result.add(capability);
			}
		}
		return Collections.unmodifiableList(result);
	}

	/**
	 * Returns the requirements declared by this revision
	 * @param namespace The namespace of the declared requirements to return or 
	 * {@code null} to return the declared requirements from all namespaces. 
	 * @return An unmodifiable list containing the declared requirements.
	 */
	public List<ModuleRequirement> getModuleRequirements(String namespace) {
		if (namespace == null)
			return Collections.unmodifiableList(requirements);
		List<ModuleRequirement> result = new ArrayList<>();
		for (ModuleRequirement requirement : requirements) {
			if (namespace.equals(requirement.getNamespace())) {
				result.add(requirement);
			}
		}
		return Collections.unmodifiableList(result);
	}

	@Override
	public int getTypes() {
		return types;
	}

	@Override
	public ModuleWiring getWiring() {
		return revisions.getContainer().getWiring(this);
	}

	@Override
	public List<Capability> getCapabilities(String namespace) {
		return InternalUtils.asListCapability(getDeclaredCapabilities(namespace));
	}

	@Override
	public List<Requirement> getRequirements(String namespace) {
		return InternalUtils.asListRequirement(getDeclaredRequirements(namespace));
	}

	/**
	 * Returns the {@link ModuleRevisions revisions} for this revision.
	 * @return the {@link ModuleRevisions revisions} for this revision.
	 */
	public ModuleRevisions getRevisions() {
		return revisions;
	}

	/**
	 * Returns the revision info for this revision.  The revision info is
	 * assigned when a revision is created to install a module or update module
	 * @return the revision info for this revision, may be {@code null}.
	 */
	public Object getRevisionInfo() {
		return revisionInfo;
	}

	/**
	 * A convenience method to quickly determine if this revision
	 * has declared the lazy activation policy.
	 * @return true if the lazy activation policy has been declared by this module; otherwise false is returned.
	 */
	public boolean hasLazyActivatePolicy() {
		Boolean currentPolicy = lazyActivationPolicy;
		if (currentPolicy != null) {
			return currentPolicy.booleanValue();
		}
		boolean lazyPolicy = false;
		List<Capability> data = getCapabilities(EquinoxModuleDataNamespace.MODULE_DATA_NAMESPACE);
		if (!data.isEmpty()) {
			Capability moduleData = data.get(0);
			lazyPolicy = EquinoxModuleDataNamespace.CAPABILITY_ACTIVATION_POLICY_LAZY.equals(moduleData.getAttributes().get(EquinoxModuleDataNamespace.CAPABILITY_ACTIVATION_POLICY));
		}
		lazyActivationPolicy = Boolean.valueOf(lazyPolicy);
		return lazyPolicy;
	}

	boolean isCurrent() {
		return !revisions.isUninstalled() && this.equals(revisions.getCurrentRevision());
	}

	@Override
	public String toString() {
		List<ModuleCapability> identities = getModuleCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
		if (identities.isEmpty())
			return super.toString();
		return identities.get(0).toString();
	}

	static <V> String toString(Map<String, V> map, boolean directives) {
		return toString(map, directives, false);
	}

	static <V> String toString(Map<String, V> map, boolean directives, boolean stringsOnly) {
		if (map.size() == 0)
			return ""; //$NON-NLS-1$
		String assignment = directives ? ":=" : "="; //$NON-NLS-1$ //$NON-NLS-2$
		Set<Entry<String, V>> set = map.entrySet();
		StringBuffer sb = new StringBuffer();
		for (Entry<String, V> entry : set) {
			sb.append("; "); //$NON-NLS-1$
			String key = entry.getKey();
			Object value = entry.getValue();
			if (value instanceof List) {
				@SuppressWarnings("unchecked")
				List<Object> list = (List<Object>) value;
				if (list.size() == 0)
					continue;
				Object component = list.get(0);
				String className = component.getClass().getName();
				String type = className.substring(className.lastIndexOf('.') + 1);
				sb.append(key).append(':').append("List<").append(type).append(">").append(assignment).append('"'); //$NON-NLS-1$ //$NON-NLS-2$
				for (Object object : list)
					sb.append(object).append(',');
				sb.setLength(sb.length() - 1);
				sb.append('"');
			} else {
				String type = ""; //$NON-NLS-1$
				if (!(value instanceof String) && !stringsOnly) {
					String className = value.getClass().getName();
					type = ":" + className.substring(className.lastIndexOf('.') + 1); //$NON-NLS-1$
				}
				sb.append(key).append(type).append(assignment).append('"').append(value).append('"');
			}
		}
		return sb.toString();
	}
}
