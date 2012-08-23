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

import java.security.AllPermission;
import java.util.*;
import org.eclipse.osgi.internal.framework.FilterImpl;
import org.osgi.framework.*;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.resource.Namespace;

/**
 * A builder for creating module {@link ModuleRevision} objects.  A builder can only be used by 
 * the module {@link ModuleContainer container} to build revisions when 
 * {@link ModuleContainer#install(Module, String, ModuleRevisionBuilder, Object) 
 * installing} or {@link ModuleContainer#update(Module, ModuleRevisionBuilder, Object) updating} a module.
 * <p>
 * The builder provides the instructions to the container for creating a {@link ModuleRevision}.
 * They are not thread-safe; in the absence of external synchronization, they do not support concurrent access by multiple threads.
 * @since 3.10
 */
public final class ModuleRevisionBuilder {
	/**
	 * Provides information about a capability or requirement
	 */
	public static class GenericInfo {
		final String namespace;
		final Map<String, String> directives;
		final Map<String, Object> attributes;

		GenericInfo(String namespace, Map<String, String> directives, Map<String, Object> attributes) {
			this.namespace = namespace;
			this.directives = directives;
			this.attributes = attributes;
		}

		/**
		 * Returns the namespace of this generic info
		 * @return the namespace
		 */
		public String getNamespace() {
			return namespace;
		}

		/**
		 * Returns the directives of this generic info
		 * @return the directives
		 */
		public Map<String, String> getDirectives() {
			return directives;
		}

		/**
		 * Returns the attributes of this generic info
		 * @return the attributes
		 */
		public Map<String, Object> getAttributes() {
			return attributes;
		}
	}

	private String symbolicName = null;
	private Version version = Version.emptyVersion;
	private int types = 0;
	private final List<GenericInfo> capabilityInfos = new ArrayList<GenericInfo>();
	private final List<GenericInfo> requirementInfos = new ArrayList<GenericInfo>();

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
		addGenericInfo(capabilityInfos, namespace, directives, attributes);
	}

	/**
	 * Returns a snapshot of the capabilities for this builder
	 * @return the capabilities
	 */
	public List<GenericInfo> getCapabilities() {
		return new ArrayList<GenericInfo>(capabilityInfos);
	}

	/**
	 * Adds a requirement to this builder using the specified namespace, directives and attributes
	 * @param namespace the namespace of the requirement
	 * @param directives the directives of the requirement
	 * @param attributes the attributes of the requirement
	 */
	public void addRequirement(String namespace, Map<String, String> directives, Map<String, Object> attributes) {
		addGenericInfo(requirementInfos, namespace, directives, attributes);
	}

	/**
	 * Returns a snapshot of the requirements for this builder
	 * @return the requirements
	 */
	public List<GenericInfo> getRequirements() {
		return new ArrayList<GenericInfo>(requirementInfos);
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
	 * Used by the container to build a new revision for a module.
	 * This builder is used to build a new {@link Module#getCurrentRevision() current}
	 * revision for the specified module.
	 * @param module the module to build a new revision for
	 * @param revisionInfo the revision info for the new revision, may be {@code null}
	 * @return the new new {@link Module#getCurrentRevision() current} revision.
	 */
	ModuleRevision addRevision(Module module, Object revisionInfo) {
		Collection<?> systemNames = Collections.emptyList();
		Module systemModule = module.getContainer().getModule(0);
		if (systemModule != null) {
			ModuleRevision systemRevision = systemModule.getCurrentRevision();
			List<ModuleCapability> hostCapabilities = systemRevision.getModuleCapabilities(HostNamespace.HOST_NAMESPACE);
			for (ModuleCapability hostCapability : hostCapabilities) {
				Object hostNames = hostCapability.getAttributes().get(HostNamespace.HOST_NAMESPACE);
				if (hostNames instanceof Collection) {
					systemNames = (Collection<?>) hostNames;
				} else if (hostNames instanceof String) {
					systemNames = Arrays.asList(hostNames);
				}
			}
		}
		ModuleRevisions revisions = module.getRevisions();
		ModuleRevision revision = new ModuleRevision(symbolicName, version, types, capabilityInfos, requirementInfos, revisions, revisionInfo);
		revisions.addRevision(revision);
		module.getContainer().getAdaptor().associateRevision(revision, revisionInfo);

		try {
			List<ModuleRequirement> hostRequirements = revision.getModuleRequirements(HostNamespace.HOST_NAMESPACE);
			for (ModuleRequirement hostRequirement : hostRequirements) {
				FilterImpl f = null;
				String filterSpec = hostRequirement.getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
				if (filterSpec != null) {
					try {
						f = FilterImpl.newInstance(filterSpec);
						String hostName = f.getPrimaryKeyValue(HostNamespace.HOST_NAMESPACE);
						if (hostName != null) {
							if (systemNames.contains(hostName)) {
								Bundle b = module.getBundle();
								if (b != null && !b.hasPermission(new AllPermission())) {
									SecurityException se = new SecurityException("Must have AllPermission granted to install an extension bundle"); //$NON-NLS-1$
									// TODO this is such a hack: making the cause a bundle exception so we can throw the right one later
									BundleException be = new BundleException(se.getMessage(), BundleException.SECURITY_ERROR, se);
									se.initCause(be);
									throw se;
								}
								module.getContainer().checkAdminPermission(module.getBundle(), AdminPermission.EXTENSIONLIFECYCLE);
							}
						}
					} catch (InvalidSyntaxException e) {
						continue;
					}
				}
			}
			module.getContainer().checkAdminPermission(module.getBundle(), AdminPermission.LIFECYCLE);
		} catch (SecurityException e) {
			revisions.removeRevision(revision);
			throw e;
		}
		return revision;
	}

	private static void addGenericInfo(List<GenericInfo> infos, String namespace, Map<String, String> directives, Map<String, Object> attributes) {
		if (infos == null) {
			infos = new ArrayList<GenericInfo>();
		}
		infos.add(new GenericInfo(namespace, copyUnmodifiableMap(directives), copyUnmodifiableMap(attributes)));
	}

	private static <K, V> Map<K, V> copyUnmodifiableMap(Map<K, V> map) {
		return Collections.unmodifiableMap(new HashMap<K, V>(map));
	}
}
