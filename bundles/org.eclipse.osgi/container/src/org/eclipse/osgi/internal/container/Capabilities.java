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
package org.eclipse.osgi.internal.container;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.osgi.container.*;
import org.eclipse.osgi.internal.framework.FilterImpl;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.namespace.*;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;

public class Capabilities {
	public static final Pattern MANDATORY_ATTR = Pattern.compile("\\(([^(=<>]+)\\s*[=<>]\\s*[^)]+\\)");

	static class NamespaceSet {
		private final String name;
		private final Map<String, Set<ModuleCapability>> indexes = new HashMap<String, Set<ModuleCapability>>();
		private final Set<ModuleCapability> all = new HashSet<ModuleCapability>();
		private final Set<ModuleCapability> nonStringIndexes = new HashSet<ModuleCapability>(0);
		private final boolean matchMandatory;

		NamespaceSet(String name) {
			this.name = name;
			this.matchMandatory = PackageNamespace.PACKAGE_NAMESPACE.equals(name) || BundleNamespace.BUNDLE_NAMESPACE.equals(name) || HostNamespace.HOST_NAMESPACE.equals(name);
		}

		void addCapability(ModuleCapability capability) {
			if (!name.equals(capability.getNamespace())) {
				throw new IllegalArgumentException("Invalid namespace: " + capability.getNamespace() + ": expecting: " + name);
			}
			all.add(capability);
			// by convention we index by the namespace attribute
			Object index = capability.getAttributes().get(name);
			if (index == null) {
				return;
			}
			Collection<?> indexCollection = null;
			if (index instanceof Collection) {
				indexCollection = (Collection<?>) index;
			} else if (index.getClass().isArray()) {
				indexCollection = Arrays.asList((Object[]) index);
			}
			if (indexCollection == null) {
				addIndex(index, capability);
			} else {
				for (Object indexKey : indexCollection) {
					addIndex(indexKey, capability);
				}
			}
		}

		private void addIndex(Object indexKey, ModuleCapability capability) {
			if (!(indexKey instanceof String)) {
				nonStringIndexes.add(capability);
			} else {
				Set<ModuleCapability> capabilities = indexes.get(indexKey);
				if (capabilities == null) {
					capabilities = new HashSet<ModuleCapability>(1);
					indexes.put((String) indexKey, capabilities);
				}
				capabilities.add(capability);
			}
		}

		void removeCapability(ModuleCapability capability) {
			if (!name.equals(capability.getNamespace())) {
				throw new IllegalArgumentException("Invalid namespace: " + capability.getNamespace() + ": expecting: " + name);
			}
			all.remove(capability);
			// by convention we index by the namespace attribute
			Object index = capability.getAttributes().get(name);
			if (index == null) {
				return;
			}
			Collection<?> indexCollection = null;
			if (index instanceof Collection) {
				indexCollection = (Collection<?>) index;
			} else if (index.getClass().isArray()) {
				indexCollection = Arrays.asList((Object[]) index);
			}
			if (indexCollection == null) {
				removeIndex(index, capability);
			} else {
				for (Object indexKey : indexCollection) {
					removeIndex(indexKey, capability);
				}
			}
		}

		private void removeIndex(Object indexKey, ModuleCapability capability) {
			if (!(indexKey instanceof String)) {
				nonStringIndexes.remove(capability);
			} else {
				Set<ModuleCapability> capabilities = indexes.get(indexKey);
				if (capabilities != null) {
					capabilities.remove(capability);
				}
			}
		}

		List<ModuleCapability> findCapabilities(ModuleRequirement requirement) {
			if (!name.equals(requirement.getNamespace())) {
				throw new IllegalArgumentException("Invalid namespace: " + requirement.getNamespace() + ": expecting: " + name);
			}
			FilterImpl f = null;
			String filterSpec = requirement.getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
			if (filterSpec != null) {
				try {
					f = FilterImpl.newInstance(filterSpec);
				} catch (InvalidSyntaxException e) {
					return Collections.emptyList();
				}
			}

			List<ModuleCapability> result;
			if (filterSpec == null) {
				result = match(null, all);
			} else {
				String indexKey = f.getPrimaryKeyValue(name);
				if (indexKey == null) {
					result = match(f, all);
				} else {
					Set<ModuleCapability> indexed = indexes.get(indexKey);
					if (indexed == null) {
						result = new ArrayList<ModuleCapability>(0);
					} else {
						result = match(f, indexed);
					}
					if (!nonStringIndexes.isEmpty()) {
						List<ModuleCapability> nonStringResult = match(f, nonStringIndexes);
						for (ModuleCapability capability : nonStringResult) {
							if (!result.contains(capability)) {
								result.add(capability);
							}
						}
					}
				}
			}
			return result;
		}

		private List<ModuleCapability> match(FilterImpl f, Set<ModuleCapability> candidates) {
			List<ModuleCapability> result = new ArrayList<ModuleCapability>(1);
			for (ModuleCapability candidate : candidates) {
				if (matches(f, candidate, matchMandatory)) {
					result.add(candidate);
				}
			}
			return result;
		}
	}

	public static boolean matches(FilterImpl f, Capability candidate, boolean matchMandatory) {
		if (f != null && !f.matches(candidate.getAttributes())) {
			return false;
		}
		if (matchMandatory) {
			// check for mandatory directive
			String mandatory = candidate.getDirectives().get(AbstractWiringNamespace.CAPABILITY_MANDATORY_DIRECTIVE);
			if (mandatory == null) {
				return true;
			}
			if (f == null) {
				return false;
			}
			Matcher matcher = MANDATORY_ATTR.matcher(f.toString());
			String[] mandatoryAttrs = ManifestElement.getArrayFromList(mandatory, ","); //$NON-NLS-1$
			boolean allPresent = true;
			for (String mandatoryAttr : mandatoryAttrs) {
				matcher.reset();
				boolean found = false;
				while (matcher.find()) {
					int numGroups = matcher.groupCount();
					for (int i = 1; i <= numGroups; i++) {
						if (mandatoryAttr.equals(matcher.group(i))) {
							found = true;
						}
					}
				}
				allPresent &= found;
			}
			return allPresent;
		}
		return true;
	}

	Map<String, NamespaceSet> namespaceSets = new HashMap<String, NamespaceSet>();

	/**
	 * Adds the {@link ModuleRevision#getModuleCapabilities(String) capabilities}
	 * provided by the specified revision to this database.  These capabilities must 
	 * become available for lookup with the {@link #findCapabilities(ModuleRequirement)}
	 * method.
	 * @param revision the revision which has capabilities to add
	 */
	public void addCapabilities(ModuleRevision revision) {
		for (ModuleCapability capability : revision.getModuleCapabilities(null)) {
			NamespaceSet namespaceSet = namespaceSets.get(capability.getNamespace());
			if (namespaceSet == null) {
				namespaceSet = new NamespaceSet(capability.getNamespace());
				namespaceSets.put(capability.getNamespace(), namespaceSet);
			}
			namespaceSet.addCapability(capability);
		}
	}

	/**
	 * Removes the {@link ModuleRevision#getModuleCapabilities(String) capabilities}
	 * provided by the specified revision from this database.  These capabilities
	 * must no longer be available for lookup with the 
	 * {@link #findCapabilities(ModuleRequirement)} method.
	 * @param revision
	 */
	public void removeCapabilities(ModuleRevision revision) {
		for (ModuleCapability capability : revision.getModuleCapabilities(null)) {
			NamespaceSet namespaceSet = namespaceSets.get(capability.getNamespace());
			if (namespaceSet != null) {
				namespaceSet.removeCapability(capability);
			}
		}
	}

	/**
	 * Returns a mutable snapshot of capabilities that are candidates for 
	 * satisfying the specified requirement.
	 * @param requirement the requirement
	 * @return the candidates for the requirement
	 */
	public List<ModuleCapability> findCapabilities(ModuleRequirement requirement) {
		NamespaceSet namespaceSet = namespaceSets.get(requirement.getNamespace());
		if (namespaceSet == null) {
			return Collections.emptyList();
		}
		return namespaceSet.findCapabilities(requirement);
	}
}
