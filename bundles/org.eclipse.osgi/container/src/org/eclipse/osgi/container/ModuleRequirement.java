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

import java.util.HashMap;
import java.util.Map;
import org.eclipse.osgi.internal.container.Capabilities;
import org.eclipse.osgi.internal.framework.FilterImpl;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.namespace.*;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.resource.Namespace;

/**
 * An implementation of {@link BundleRequirement}.  This requirement implements
 * the matches method according to the OSGi specification which includes
 * implementing the mandatory directive for the osgi.wiring.* namespaces.
 * @since 3.10
 * @noextend This class is not intended to be subclassed by clients.
 */
public class ModuleRequirement implements BundleRequirement {
	private final String namespace;
	private final Map<String, String> directives;
	private final Map<String, Object> attributes;
	private final ModuleRevision revision;

	ModuleRequirement(String namespace, Map<String, String> directives, Map<String, ?> attributes, ModuleRevision revision) {
		this.namespace = namespace;
		this.directives = ModuleRevisionBuilder.unmodifiableMap(directives);
		this.attributes = ModuleRevisionBuilder.unmodifiableMap(attributes);
		this.revision = revision;
	}

	@Override
	public ModuleRevision getRevision() {
		return revision;
	}

	@Override
	public boolean matches(BundleCapability capability) {
		if (!namespace.equals(capability.getNamespace()))
			return false;
		String filterSpec = directives.get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
		FilterImpl f = null;
		if (filterSpec != null) {
			try {
				f = FilterImpl.newInstance(filterSpec);
			} catch (InvalidSyntaxException e) {
				return false;
			}
		}
		boolean matchMandatory = PackageNamespace.PACKAGE_NAMESPACE.equals(namespace) || BundleNamespace.BUNDLE_NAMESPACE.equals(namespace) || HostNamespace.HOST_NAMESPACE.equals(namespace);
		return Capabilities.matches(f, capability, matchMandatory);
	}

	@Override
	public String getNamespace() {
		return namespace;
	}

	@Override
	public Map<String, String> getDirectives() {
		return directives;
	}

	@Override
	public Map<String, Object> getAttributes() {
		return attributes;
	}

	@Override
	public ModuleRevision getResource() {
		return revision;
	}

	@Override
	public String toString() {
		return namespace + ModuleRevision.toString(attributes, false) + ModuleRevision.toString(directives, true);
	}

	private static final String PACKAGENAME_FILTER_COMPONENT = PackageNamespace.PACKAGE_NAMESPACE + "="; //$NON-NLS-1$

	DynamicModuleRequirement getDynamicPackageRequirement(ModuleRevision host, String dynamicPkgName) {
		if (!PackageNamespace.PACKAGE_NAMESPACE.equals(namespace)) {
			return null;
		}
		if (!PackageNamespace.RESOLUTION_DYNAMIC.equals(directives.get(PackageNamespace.REQUIREMENT_RESOLUTION_DIRECTIVE))) {
			// not dynamic
			return null;
		}
		String dynamicFilter = directives.get(PackageNamespace.REQUIREMENT_FILTER_DIRECTIVE);
		// TODO we make some assumptions here on the format of the filter string
		int packageNameBegin = dynamicFilter.indexOf(PACKAGENAME_FILTER_COMPONENT);
		if (packageNameBegin == -1) {
			// not much we can do
			return null;
		}
		packageNameBegin += PACKAGENAME_FILTER_COMPONENT.length();
		int packageNameEnd = dynamicFilter.indexOf(')', packageNameBegin);
		if (packageNameEnd == -1) {
			// not much we can do
			return null;
		}
		String filterPackageName = dynamicFilter.substring(packageNameBegin, packageNameEnd);
		String specificPackageFilter = null;
		if ("*".equals(filterPackageName)) { //$NON-NLS-1$
			// matches all
			specificPackageFilter = dynamicFilter.replace(PACKAGENAME_FILTER_COMPONENT + filterPackageName, PACKAGENAME_FILTER_COMPONENT + dynamicPkgName);
		} else if (filterPackageName.endsWith(".*")) { //$NON-NLS-1$
			if (dynamicPkgName.startsWith(filterPackageName.substring(0, filterPackageName.length() - 1))) {
				specificPackageFilter = dynamicFilter.replace(PACKAGENAME_FILTER_COMPONENT + filterPackageName, PACKAGENAME_FILTER_COMPONENT + dynamicPkgName);
			}
		} else if (dynamicPkgName.equals(filterPackageName)) {
			specificPackageFilter = dynamicFilter;
		}

		if (specificPackageFilter != null) {
			Map<String, String> dynamicDirectives = new HashMap<>(directives);
			dynamicDirectives.put(PackageNamespace.REQUIREMENT_FILTER_DIRECTIVE, specificPackageFilter);
			return new DynamicModuleRequirement(host, dynamicDirectives);
		}
		return null;
	}

	class DynamicModuleRequirement extends ModuleRequirement {

		DynamicModuleRequirement(ModuleRevision host, Map<String, String> directives) {
			super(ModuleRequirement.this.getNamespace(), directives, ModuleRequirement.this.getAttributes(), host);
		}

		ModuleRequirement getOriginal() {
			return ModuleRequirement.this;
		}
	}

}
