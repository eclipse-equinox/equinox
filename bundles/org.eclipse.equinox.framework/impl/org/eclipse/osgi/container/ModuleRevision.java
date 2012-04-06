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
import org.eclipse.osgi.container.ModuleRevisionBuilder.GenericInfo;
import org.eclipse.osgi.container.wiring.ModuleWiring;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.*;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

public class ModuleRevision implements BundleRevision {
	private final String symbolicName;
	private final Version version;
	private final int types;
	private final List<ModuleCapability> capabilities;
	private final List<ModuleRequirement> requirements;
	private final ModuleRevisions revisions;

	ModuleRevision(String symbolicName, Version version, int types, List<GenericInfo> capabilityInfos, List<GenericInfo> requirementInfos, ModuleRevisions revisions) {
		this.symbolicName = symbolicName;
		this.version = version;
		this.types = types;
		this.capabilities = createCapabilities(capabilityInfos);
		this.requirements = createRequirements(requirementInfos);
		this.revisions = revisions;
	}

	private List<ModuleCapability> createCapabilities(List<GenericInfo> capabilityInfos) {
		if (capabilityInfos == null || capabilityInfos.isEmpty())
			return Collections.emptyList();
		List<ModuleCapability> result = new ArrayList<ModuleCapability>(capabilityInfos.size());
		for (GenericInfo info : capabilityInfos) {
			result.add(new ModuleCapability(info.namespace, info.directives, info.attributes, this));
		}
		return result;
	}

	private List<ModuleRequirement> createRequirements(List<GenericInfo> requirementInfos) {
		if (requirementInfos == null || requirementInfos.isEmpty())
			return Collections.emptyList();
		List<ModuleRequirement> result = new ArrayList<ModuleRequirement>(requirementInfos.size());
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
		if (namespace == null)
			return Converters.asListBundleCapability(Collections.unmodifiableList(capabilities));
		List<BundleCapability> result = new ArrayList<BundleCapability>();
		for (ModuleCapability capability : capabilities) {
			if (namespace.equals(capability.getNamespace())) {
				result.add(capability);
			}
		}
		return result;
	}

	@Override
	public List<BundleRequirement> getDeclaredRequirements(String namespace) {
		if (namespace == null)
			return Converters.asListBundleRequirement(Collections.unmodifiableList(requirements));
		List<BundleRequirement> result = new ArrayList<BundleRequirement>();
		for (ModuleRequirement requirement : requirements) {
			if (namespace.equals(requirement.getNamespace())) {
				result.add(requirement);
			}
		}
		return result;
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
		return Converters.asListCapability(getDeclaredCapabilities(namespace));
	}

	@Override
	public List<Requirement> getRequirements(String namespace) {
		return Converters.asListRequirement(getDeclaredRequirements(namespace));
	}

	public ModuleRevisions getRevisions() {
		return revisions;
	}

	public boolean isCurrent() {
		return !revisions.isUninstalled() && revisions.getRevisions().indexOf(this) == 0;
	}
}
