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
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.*;
import org.osgi.framework.namespace.*;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.resource.Namespace;

/**
 * An implementation of {@link BundleRequirement}.  This requirement implements 
 * the matches method according to the OSGi specification which includes
 * implementing the mandatory directive for the osgi.wiring.* namespaces.
 */
public class ModuleRequirement implements BundleRequirement {
	private final String namespace;
	private final Map<String, String> directives;
	private final Map<String, Object> attributes;
	private final ModuleRevision revision;
	private volatile Filter filter;

	ModuleRequirement(String namespace, Map<String, String> directives, Map<String, Object> attributes, ModuleRevision revision) {
		this.namespace = namespace;
		this.directives = Collections.unmodifiableMap(directives);
		this.attributes = Collections.unmodifiableMap(attributes);
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
		if (filterSpec != null) {
			Filter f = getFilter();
			if (f == null) {
				try {
					f = FrameworkUtil.createFilter(filterSpec);
					setFilter(f);
				} catch (InvalidSyntaxException e) {
					return false;
				}
			}
			if (!f.matches(capability.getAttributes()))
				return false;
		}
		if (PackageNamespace.PACKAGE_NAMESPACE.equals(namespace) || BundleNamespace.BUNDLE_NAMESPACE.equals(namespace) || HostNamespace.HOST_NAMESPACE.equals(namespace)) {
			// check for mandatory directive
			String mandatory = capability.getDirectives().get(AbstractWiringNamespace.CAPABILITY_MANDATORY_DIRECTIVE);
			if (mandatory != null) {
				if (filterSpec == null)
					return false;
				String[] mandatoryAttrs = ManifestElement.getArrayFromList(mandatory, ","); //$NON-NLS-1$
				for (String mandatoryAttr : mandatoryAttrs) {
					// TODO doing the simple thing here.  there are likely corner cases this does not satisfy
					if (filterSpec.indexOf("(" + mandatoryAttr + "=") < 0) //$NON-NLS-1$ //$NON-NLS-2$
						return false;
				}
			}
		}

		return true;
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

	/**
	 * The cached instance of the filter used for matching this requirement
	 * @return the cached instance of the filter.
	 */
	public Filter getFilter() {
		return filter;
	}

	/**
	 * Used to cache an instance of a filter used for matching this requirement
	 * @param filter
	 */
	void setFilter(Filter filter) {
		this.filter = filter;
	}

	public String toString() {
		return namespace + ModuleRevision.toString(attributes, false) + ModuleRevision.toString(directives, true);
	}

	private static final String PACKAGENAME_FILTER_COMPONENT = PackageNamespace.PACKAGE_NAMESPACE + "=";

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
		if ("*".equals(filterPackageName)) {
			// matches all
			specificPackageFilter = dynamicFilter.replace(PACKAGENAME_FILTER_COMPONENT + filterPackageName, PACKAGENAME_FILTER_COMPONENT + dynamicPkgName);
		} else if (filterPackageName.endsWith(".*")) {
			if (dynamicPkgName.startsWith(filterPackageName.substring(0, filterPackageName.length() - 1))) {
				specificPackageFilter = dynamicFilter.replace(PACKAGENAME_FILTER_COMPONENT + filterPackageName, PACKAGENAME_FILTER_COMPONENT + dynamicPkgName);
			}
		} else if (dynamicPkgName.equals(filterPackageName)) {
			specificPackageFilter = dynamicFilter;
		}

		if (specificPackageFilter != null) {
			Map<String, String> dynamicDirectives = new HashMap<String, String>(directives);
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
