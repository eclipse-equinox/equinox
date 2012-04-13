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
import org.osgi.framework.Version;

public class ModuleRevisionBuilder {
	/**
	 * Provides information about a capability or requirement
	 */
	static class GenericInfo {
		GenericInfo(String namespace, Map<String, String> directives, Map<String, Object> attributes) {
			this.namespace = namespace;
			this.directives = directives;
			this.attributes = attributes;
		}

		final String namespace;
		final Map<String, String> directives;
		final Map<String, Object> attributes;
	}

	private String symbolicName = null;
	private Version version = Version.emptyVersion;
	private int types = 0;
	private List<GenericInfo> capabilityInfos = null;
	private List<GenericInfo> requirementInfos = null;

	public ModuleRevisionBuilder() {
		// nothing
	}

	public void setSymbolicName(String symbolicName) {
		this.symbolicName = symbolicName;
	}

	public void setVersion(Version version) {
		this.version = version;
	}

	public void setTypes(int types) {
		this.types = types;
	}

	public void addCapability(String namespace, Map<String, String> directives, Map<String, Object> attributes) {
		capabilityInfos = addGenericInfo(capabilityInfos, namespace, directives, attributes);
	}

	public void addRequirement(String namespace, Map<String, String> directives, Map<String, Object> attributes) {
		requirementInfos = addGenericInfo(requirementInfos, namespace, directives, attributes);
	}

	public String getSymbolicName() {
		return symbolicName;
	}

	public Version getVersion() {
		return version;
	}

	ModuleRevision buildRevision(Long id, String location, Module module, ModuleContainer container) {
		ModuleRevisions revisions = new ModuleRevisions(id, location, module, container);
		module.setRevisions(revisions);
		return addRevision(revisions);
	}

	ModuleRevision addRevision(ModuleRevisions revisions) {
		ModuleRevision revision = new ModuleRevision(symbolicName, version, types, capabilityInfos, requirementInfos, revisions);
		revisions.addRevision(revision);
		return revision;
	}

	private static List<GenericInfo> addGenericInfo(List<GenericInfo> infos, String namespace, Map<String, String> directives, Map<String, Object> attributes) {
		if (infos == null) {
			infos = new ArrayList<GenericInfo>();
		}
		infos.add(new GenericInfo(namespace, directives, attributes));
		return infos;
	}
}
