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
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;

/**
 * A builder for creating module {@link ModuleRevision} objects.  A builder can only be used by 
 * the module {@link ModuleContainer container} to build revisions when 
 * {@link ModuleContainer#install(BundleContext, String, ModuleRevisionBuilder) 
 * installing} or {@link ModuleContainer#update(Module, ModuleRevisionBuilder) updating} a module.
 * <p>
 * The builder provides the instructions to the container for creating a {@link ModuleRevision}.
 */
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

	/**
	 * Constructs a new module builder
	 */
	public ModuleRevisionBuilder() {
		// nothing
	}

	/**
	 * Sets the symbolic name for the builder
	 * @param symbolicName the symbolic name
	 */
	public void setSymbolicName(String symbolicName) {
		this.symbolicName = symbolicName;
	}

	/**
	 * Sets the module version for the builder.
	 * @param version the version
	 */
	public void setVersion(Version version) {
		this.version = version;
	}

	/**
	 * Sets the module types for the builder.
	 * @param types the module types
	 */
	public void setTypes(int types) {
		this.types = types;
	}

	/**
	 * Adds a capability to this builder using the specified namespace, directives and attributes
	 * @param namespace the namespace of the capability
	 * @param directives the directives of the capability
	 * @param attributes the attributes of the capability
	 */
	public void addCapability(String namespace, Map<String, String> directives, Map<String, Object> attributes) {
		capabilityInfos = addGenericInfo(capabilityInfos, namespace, directives, attributes);
	}

	/**
	 * Adds a requirement to this builder using the specified namespace, directives and attributes
	 * @param namespace the namespace of the requirement
	 * @param directives the directives of the requirement
	 * @param attributes the attributes of the requirement
	 */
	public void addRequirement(String namespace, Map<String, String> directives, Map<String, Object> attributes) {
		requirementInfos = addGenericInfo(requirementInfos, namespace, directives, attributes);
	}

	/**
	 * Returns the symbolic name for this builder.
	 * @return the symbolic name for this builder.
	 */
	public String getSymbolicName() {
		return symbolicName;
	}

	/**
	 * Returns the module version for this builder.
	 * @return the module version for this builder.
	 */
	public Version getVersion() {
		return version;
	}

	/**
	 * Returns the module type for this builder.
	 * @return the module type for this builder.
	 */
	public int getTypes() {
		return types;
	}

	/**
	 * Used by the container to build a new module for installation.
	 * This builder is used to build the {@link Module#getCurrentRevision() current}
	 * revision for the new module. 
	 * @param id the module id being installed.
	 * @param location the location of the module being installed
	 * @param container the container the module is being installed into
	 * @return the new module.
	 */
	Module buildModule(Long id, String location, ModuleContainer container) {
		Module module = container.moduleDataBase.createModule(location, id);
		addRevision(module.getRevisions());
		return module;
	}

	/**
	 * Used by the container to build a new revision to update a module.
	 * @param revisions the module revisions the update is for
	 * @return the new revision for update.
	 */
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
