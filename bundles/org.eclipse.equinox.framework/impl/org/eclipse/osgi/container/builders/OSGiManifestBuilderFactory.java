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
package org.eclipse.osgi.container.builders;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import org.eclipse.osgi.container.ModuleRevisionBuilder;
import org.eclipse.osgi.framework.internal.core.*;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
import org.osgi.framework.namespace.*;
import org.osgi.framework.wiring.BundleRevision;

public class OSGiManifestBuilderFactory {
	private static final String ATTR_TYPE_STRING = "string"; //$NON-NLS-1$
	private static final String ATTR_TYPE_VERSION = "version"; //$NON-NLS-1$
	private static final String ATTR_TYPE_URI = "uri"; //$NON-NLS-1$
	private static final String ATTR_TYPE_LONG = "long"; //$NON-NLS-1$
	private static final String ATTR_TYPE_DOUBLE = "double"; //$NON-NLS-1$
	private static final String ATTR_TYPE_SET = "set"; //$NON-NLS-1$
	private static final String ATTR_TYPE_LIST = "List"; //$NON-NLS-1$

	public static ModuleRevisionBuilder createBuilder(Map<String, String> manifest) throws BundleException {
		ModuleRevisionBuilder builder = new ModuleRevisionBuilder();

		int manifestVersion = getManifestVersion(manifest);

		getSymbolicNameAndVersion(builder, manifest, manifestVersion);

		Collection<Map<String, Object>> exportedPackages = new ArrayList<Map<String, Object>>();
		getPackageExports(builder, manifest, exportedPackages);
		getPackageImports(builder, manifest, exportedPackages, manifestVersion);

		getRequireBundle(builder, manifest);

		getProvideCapabilities(builder, manifest);
		getRequireCapabilities(builder, manifest);

		getFragmentHost(builder, manifest);

		convertBREEs(builder, manifest);
		return builder;
	}

	private static int getManifestVersion(Map<String, String> manifest) {
		String manifestVersionHeader = manifest.get(Constants.BUNDLE_MANIFESTVERSION);
		return manifestVersionHeader == null ? 1 : Integer.parseInt(manifestVersionHeader);
	}

	private static void getSymbolicNameAndVersion(ModuleRevisionBuilder builder, Map<String, String> manifest, int manifestVersion) throws BundleException {
		boolean isFragment = manifest.get(Constants.FRAGMENT_HOST) != null;
		builder.setTypes(isFragment ? BundleRevision.TYPE_FRAGMENT : 0);
		String version = manifest.get(Constants.BUNDLE_VERSION);
		try {
			builder.setVersion((version != null) ? Version.parseVersion(version) : Version.emptyVersion);
		} catch (IllegalArgumentException ex) {
			if (manifestVersion >= 2) {
				String message = NLS.bind("Invalid Manifest header \"{0}\": {1}", Constants.BUNDLE_VERSION, version);
				throw new BundleException(message, BundleException.MANIFEST_ERROR, ex);
			}
			// prior to R4 the Bundle-Version header was not interpreted by the Framework;
			// must not fail for old R3 style bundles
		}

		String symbolicNameHeader = manifest.get(Constants.BUNDLE_SYMBOLICNAME);
		if (symbolicNameHeader != null) {
			ManifestElement[] symbolicNameElements = ManifestElement.parseHeader(Constants.BUNDLE_SYMBOLICNAME, symbolicNameHeader);
			if (symbolicNameElements.length > 0) {
				ManifestElement bsnElement = symbolicNameElements[0];
				builder.setSymbolicName(bsnElement.getValue());
				Map<String, String> directives = getDirectives(bsnElement);
				directives.remove(BundleNamespace.CAPABILITY_USES_DIRECTIVE);
				directives.remove(BundleNamespace.CAPABILITY_EFFECTIVE_DIRECTIVE);
				Map<String, Object> attributes = getAttributes(bsnElement);
				if (!isFragment) {
					// create the bundle namespace
					Map<String, Object> bundleAttributes = new HashMap<String, Object>(attributes);
					bundleAttributes.put(BundleNamespace.BUNDLE_NAMESPACE, builder.getSymbolicName());
					bundleAttributes.put(BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE, builder.getVersion());
					builder.addCapability(BundleNamespace.BUNDLE_NAMESPACE, directives, bundleAttributes);

					// create the host namespace
					Map<String, Object> hostAttributes = new HashMap<String, Object>(attributes);
					hostAttributes.put(HostNamespace.HOST_NAMESPACE, builder.getSymbolicName());
					hostAttributes.put(HostNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE, builder.getVersion());
					builder.addCapability(HostNamespace.HOST_NAMESPACE, directives, hostAttributes);
				}
				// every bundle that has a symbolic name gets an identity
				Map<String, Object> identityAttributes = new HashMap<String, Object>(attributes);
				identityAttributes.put(IdentityNamespace.IDENTITY_NAMESPACE, builder.getSymbolicName());
				identityAttributes.put(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, builder.getVersion());
				identityAttributes.put(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, isFragment ? IdentityNamespace.TYPE_FRAGMENT : IdentityNamespace.TYPE_BUNDLE);
				builder.addCapability(IdentityNamespace.IDENTITY_NAMESPACE, directives, identityAttributes);
			}
		}
	}

	private static void getPackageExports(ModuleRevisionBuilder builder, Map<String, String> manifest, Collection<Map<String, Object>> exportedPackages) throws BundleException {
		ManifestElement[] exportElements = ManifestElement.parseHeader(Constants.EXPORT_PACKAGE, manifest.get(Constants.EXPORT_PACKAGE));
		if (exportElements == null)
			return;
		for (ManifestElement exportElement : exportElements) {
			String[] packageNames = exportElement.getValueComponents();
			Map<String, Object> attributes = getAttributes(exportElement);
			Map<String, String> directives = getDirectives(exportElement);
			directives.remove(PackageNamespace.CAPABILITY_EFFECTIVE_DIRECTIVE);
			String versionAttr = (String) attributes.remove(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE);
			@SuppressWarnings("deprecation")
			String specVersionAttr = (String) attributes.remove(Constants.PACKAGE_SPECIFICATION_VERSION);
			Version version = versionAttr == null ? (specVersionAttr == null ? Version.parseVersion(specVersionAttr) : Version.emptyVersion) : Version.parseVersion(versionAttr);
			attributes.put(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE, version);
			if (builder.getSymbolicName() != null)
				attributes.put(PackageNamespace.CAPABILITY_BUNDLE_SYMBOLICNAME_ATTRIBUTE, builder.getSymbolicName());
			attributes.put(PackageNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE, builder.getVersion());
			for (String packageName : packageNames) {
				Map<String, Object> packageAttrs = new HashMap<String, Object>(attributes);
				packageAttrs.put(PackageNamespace.PACKAGE_NAMESPACE, packageName);
				builder.addCapability(PackageNamespace.PACKAGE_NAMESPACE, directives, packageAttrs);
				exportedPackages.add(packageAttrs);
			}
		}
	}

	private static void getPackageImports(ModuleRevisionBuilder builder, Map<String, String> manifest, Collection<Map<String, Object>> exportedPackages, int manifestVersion) throws BundleException {
		Collection<String> importPackageNames = new ArrayList<String>();
		ManifestElement[] importElements = ManifestElement.parseHeader(Constants.IMPORT_PACKAGE, manifest.get(Constants.IMPORT_PACKAGE));
		ManifestElement[] dynamicImportElements = ManifestElement.parseHeader(Constants.DYNAMICIMPORT_PACKAGE, manifest.get(Constants.DYNAMICIMPORT_PACKAGE));
		addPackageImports(builder, importElements, importPackageNames, false);
		addPackageImports(builder, dynamicImportElements, importPackageNames, true);
		if (manifestVersion < 2)
			addImplicitImports(builder, exportedPackages, importPackageNames);
	}

	private static void addPackageImports(ModuleRevisionBuilder builder, ManifestElement[] importElements, Collection<String> importPackageNames, boolean dynamic) {
		if (importElements == null)
			return;
		for (ManifestElement importElement : importElements) {
			String[] packageNames = importElement.getValueComponents();
			Map<String, Object> attributes = getAttributes(importElement);
			Map<String, String> directives = getDirectives(importElement);
			directives.remove(PackageNamespace.REQUIREMENT_EFFECTIVE_DIRECTIVE);
			directives.remove(PackageNamespace.REQUIREMENT_CARDINALITY_DIRECTIVE);
			if (dynamic) {
				directives.put(PackageNamespace.REQUIREMENT_RESOLUTION_DIRECTIVE, PackageNamespace.RESOLUTION_DYNAMIC);
			}
			String versionRangeAttr = (String) attributes.remove(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE);
			@SuppressWarnings("deprecation")
			String specVersionRangeAttr = (String) attributes.remove(Constants.PACKAGE_SPECIFICATION_VERSION);
			VersionRange versionRange = versionRangeAttr == null ? (specVersionRangeAttr == null ? null : new VersionRange(specVersionRangeAttr)) : new VersionRange(versionRangeAttr);
			String bundleVersionRangeAttr = (String) attributes.remove(PackageNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE);
			VersionRange bundleVersionRange = bundleVersionRangeAttr == null ? null : new VersionRange(bundleVersionRangeAttr);
			for (String packageName : packageNames) {
				if (dynamic && importPackageNames.contains(packageName))
					continue; // already importing this package, don't add a dynamic import for it
				importPackageNames.add(packageName);

				// fill in the filter directive based on the attributes
				Map<String, String> packageDirectives = new HashMap<String, String>(directives);
				StringBuilder filter = new StringBuilder();
				filter.append('(').append(PackageNamespace.PACKAGE_NAMESPACE).append('=').append(packageName).append(')');
				int size = filter.length();
				for (Map.Entry<String, Object> attribute : attributes.entrySet())
					filter.append('(').append(attribute.getKey()).append('=').append(attribute.getValue()).append(')');
				if (versionRange != null)
					filter.append(versionRange.toFilterString(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE));
				if (bundleVersionRange != null)
					filter.append(bundleVersionRange.toFilterString(PackageNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE));
				if (size != filter.length())
					// need to add (&...)
					filter.insert(0, "(&").append(')'); //$NON-NLS-1$
				packageDirectives.put(PackageNamespace.REQUIREMENT_FILTER_DIRECTIVE, filter.toString());

				// fill in cardinality for dynamic wild cards
				if (dynamic && packageName.indexOf('*') >= 0)
					packageDirectives.put(PackageNamespace.REQUIREMENT_CARDINALITY_DIRECTIVE, PackageNamespace.CARDINALITY_MULTIPLE);

				builder.addRequirement(PackageNamespace.PACKAGE_NAMESPACE, packageDirectives, new HashMap<String, Object>(0));
			}
		}
	}

	private static void addImplicitImports(ModuleRevisionBuilder builder, Collection<Map<String, Object>> exportedPackages, Collection<String> importPackageNames) {
		for (Map<String, Object> exportAttributes : exportedPackages) {
			String packageName = (String) exportAttributes.get(PackageNamespace.PACKAGE_NAMESPACE);
			if (importPackageNames.contains(packageName))
				continue;
			importPackageNames.add(packageName);
			Version packageVersion = (Version) exportAttributes.get(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE);
			StringBuilder filter = new StringBuilder();
			filter.append("(&(").append(PackageNamespace.PACKAGE_NAMESPACE).append('=').append(packageName).append(')'); //$NON-NLS-1$
			filter.append('(').append(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE).append(">=").append(packageVersion).append("))"); //$NON-NLS-1$//$NON-NLS-2$
			Map<String, String> directives = new HashMap<String, String>(1);
			directives.put(PackageNamespace.REQUIREMENT_FILTER_DIRECTIVE, filter.toString());
			builder.addRequirement(PackageNamespace.PACKAGE_NAMESPACE, directives, new HashMap<String, Object>(0));
		}
	}

	static Map<String, String> getDirectives(ManifestElement element) {
		Map<String, String> directives = new HashMap<String, String>();
		Enumeration<String> keys = element.getDirectiveKeys();
		if (keys == null)
			return directives;
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			directives.put(key, element.getDirective(key));
		}
		return directives;
	}

	private static void getRequireBundle(ModuleRevisionBuilder builder, Map<String, String> manifest) throws BundleException {
		ManifestElement[] requireBundles = ManifestElement.parseHeader(Constants.REQUIRE_BUNDLE, manifest.get(Constants.REQUIRE_BUNDLE));
		if (requireBundles == null)
			return;
		for (ManifestElement requireElement : requireBundles) {
			String[] bundleNames = requireElement.getValueComponents();
			Map<String, Object> attributes = getAttributes(requireElement);
			Map<String, String> directives = getDirectives(requireElement);
			directives.remove(BundleNamespace.REQUIREMENT_CARDINALITY_DIRECTIVE);
			directives.remove(BundleNamespace.REQUIREMENT_EFFECTIVE_DIRECTIVE);
			String versionRangeAttr = (String) attributes.remove(BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE);
			VersionRange versionRange = versionRangeAttr == null ? null : new VersionRange(versionRangeAttr);
			for (String bundleName : bundleNames) {
				// fill in the filter directive based on the attributes
				Map<String, String> bundleDirectives = new HashMap<String, String>(directives);
				StringBuilder filter = new StringBuilder();
				filter.append('(').append(BundleNamespace.BUNDLE_NAMESPACE).append('=').append(bundleName).append(')');
				int size = filter.length();
				for (Map.Entry<String, Object> attribute : attributes.entrySet())
					filter.append('(').append(attribute.getKey()).append('=').append(attribute.getValue()).append(')');
				if (versionRange != null)
					filter.append(versionRange.toFilterString(BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE));
				if (size != filter.length())
					// need to add (&...)
					filter.insert(0, "(&").append(')'); //$NON-NLS-1$
				bundleDirectives.put(BundleNamespace.REQUIREMENT_FILTER_DIRECTIVE, filter.toString());
				builder.addRequirement(BundleNamespace.BUNDLE_NAMESPACE, bundleDirectives, new HashMap<String, Object>(0));
			}
		}
	}

	private static void getFragmentHost(ModuleRevisionBuilder builder, Map<String, String> manifest) throws BundleException {
		ManifestElement[] fragmentHosts = ManifestElement.parseHeader(Constants.FRAGMENT_HOST, manifest.get(Constants.FRAGMENT_HOST));
		if (fragmentHosts == null || fragmentHosts.length == 0)
			return;

		ManifestElement fragmentHost = fragmentHosts[0];
		String hostName = fragmentHost.getValue();
		Map<String, Object> attributes = getAttributes(fragmentHost);
		Map<String, String> directives = getDirectives(fragmentHost);
		directives.remove(HostNamespace.REQUIREMENT_CARDINALITY_DIRECTIVE);
		directives.remove(HostNamespace.REQUIREMENT_EFFECTIVE_DIRECTIVE);

		String versionRangeAttr = (String) attributes.remove(HostNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE);
		VersionRange versionRange = versionRangeAttr == null ? null : new VersionRange(versionRangeAttr);

		// fill in the filter directive based on the attributes
		StringBuilder filter = new StringBuilder();
		filter.append('(').append(HostNamespace.HOST_NAMESPACE).append('=').append(hostName).append(')');
		int size = filter.length();
		for (Map.Entry<String, Object> attribute : attributes.entrySet())
			filter.append('(').append(attribute.getKey()).append('=').append(attribute.getValue()).append(')');
		if (versionRange != null)
			filter.append(versionRange.toFilterString(HostNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE));
		if (size != filter.length())
			// need to add (&...)
			filter.insert(0, "(&").append(')'); //$NON-NLS-1$
		directives.put(BundleNamespace.REQUIREMENT_FILTER_DIRECTIVE, filter.toString());
		builder.addRequirement(HostNamespace.HOST_NAMESPACE, directives, new HashMap<String, Object>(0));
	}

	private static void getProvideCapabilities(ModuleRevisionBuilder builder, Map<String, String> manifest) throws BundleException {
		ManifestElement[] provideElements = ManifestElement.parseHeader(Constants.PROVIDE_CAPABILITY, manifest.get(Constants.PROVIDE_CAPABILITY));
		if (provideElements == null)
			return;
		for (ManifestElement provideElement : provideElements) {
			String[] namespaces = provideElement.getValueComponents();
			Map<String, Object> attributes = getAttributes(provideElement);
			Map<String, String> directives = getDirectives(provideElement);
			for (String namespace : namespaces) {
				builder.addCapability(namespace, directives, attributes);
			}
		}
	}

	private static void getRequireCapabilities(ModuleRevisionBuilder builder, Map<String, String> manifest) throws BundleException {
		ManifestElement[] requireElements = ManifestElement.parseHeader(Constants.REQUIRE_CAPABILITY, manifest.get(Constants.REQUIRE_CAPABILITY));
		if (requireElements == null)
			return;
		for (ManifestElement requireElement : requireElements) {
			String[] namespaces = requireElement.getValueComponents();
			Map<String, Object> attributes = getAttributes(requireElement);
			Map<String, String> directives = getDirectives(requireElement);
			for (String namespace : namespaces) {
				if (IdentityNamespace.IDENTITY_NAMESPACE.equals(namespace))
					throw new BundleException("A bundle is not allowed to define a capability in the " + IdentityNamespace.IDENTITY_NAMESPACE + " name space."); //$NON-NLS-1$ //$NON-NLS-2$
				builder.addRequirement(namespace, directives, attributes);
			}
		}
	}

	private static Map<String, Object> getAttributes(ManifestElement element) {
		Enumeration<String> keys = element.getKeys();
		Map<String, Object> attributes = new HashMap<String, Object>();
		if (keys == null)
			return attributes;
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			String value = element.getAttribute(key);
			int colonIndex = key.indexOf(':');
			String type = ATTR_TYPE_STRING;
			if (colonIndex > 0) {
				type = key.substring(colonIndex + 1).trim();
				key = key.substring(0, colonIndex).trim();
			}
			attributes.put(key, convertValue(type, value));
		}
		return attributes;
	}

	private static Object convertValue(String type, String value) {

		if (ATTR_TYPE_STRING.equalsIgnoreCase(type))
			return value;

		String trimmed = value.trim();
		if (ATTR_TYPE_DOUBLE.equalsIgnoreCase(type))
			return new Double(trimmed);
		else if (ATTR_TYPE_LONG.equalsIgnoreCase(type))
			return new Long(trimmed);
		else if (ATTR_TYPE_URI.equalsIgnoreCase(type))
			try {
				return new URI(trimmed);
			} catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}
		else if (ATTR_TYPE_VERSION.equalsIgnoreCase(type))
			return new Version(trimmed);
		else if (ATTR_TYPE_SET.equalsIgnoreCase(type))
			return ManifestElement.getArrayFromList(trimmed, ","); //$NON-NLS-1$

		// assume list type, anything else will throw an exception
		Tokenizer listTokenizer = new Tokenizer(type);
		String listType = listTokenizer.getToken("<"); //$NON-NLS-1$
		if (!ATTR_TYPE_LIST.equalsIgnoreCase(listType))
			throw new RuntimeException("Unsupported type: " + type); //$NON-NLS-1$
		char c = listTokenizer.getChar();
		String componentType = ATTR_TYPE_STRING;
		if (c == '<') {
			componentType = listTokenizer.getToken(">"); //$NON-NLS-1$
			if (listTokenizer.getChar() != '>')
				throw new RuntimeException("Invalid type, missing ending '>' : " + type); //$NON-NLS-1$
		}
		List<String> tokens = new Tokenizer(value).getEscapedTokens(","); //$NON-NLS-1$
		List<Object> components = new ArrayList<Object>();
		for (String component : tokens) {
			components.add(convertValue(componentType, component));
		}
		return components;
	}

	private static void convertBREEs(ModuleRevisionBuilder builder, Map<String, String> manifest) throws BundleException {
		@SuppressWarnings("deprecation")
		String[] brees = ManifestElement.getArrayFromList(manifest.get(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT));
		if (brees == null || brees.length == 0)
			return;
		List<String> breeFilters = new ArrayList<String>();
		for (String bree : brees)
			breeFilters.add(createOSGiEERequirementFilter(bree));
		String filterSpec;
		if (breeFilters.size() == 1) {
			filterSpec = breeFilters.get(0);
		} else {
			StringBuffer filterBuf = new StringBuffer("(|"); //$NON-NLS-1$
			for (String breeFilter : breeFilters) {
				filterBuf.append(breeFilter);
			}
			filterSpec = filterBuf.append(")").toString(); //$NON-NLS-1$
		}

		Map<String, String> directives = new HashMap<String, String>(1);
		directives.put(ExecutionEnvironmentNamespace.REQUIREMENT_FILTER_DIRECTIVE, filterSpec);
		builder.addRequirement(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE, directives, new HashMap<String, Object>(0));
	}

	private static String createOSGiEERequirementFilter(String bree) throws BundleException {
		String[] nameVersion = getOSGiEENameVersion(bree);
		String eeName = nameVersion[0];
		String v = nameVersion[1];
		String filterSpec;
		if (v == null)
			filterSpec = "(osgi.ee=" + eeName + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		else
			filterSpec = "(&(osgi.ee=" + eeName + ")(version=" + v + "))"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		try {
			// do a sanity check
			FilterImpl.newInstance(filterSpec);
		} catch (InvalidSyntaxException e) {
			filterSpec = "(osgi.ee=" + bree + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			try {
				// do another sanity check
				FilterImpl.newInstance(filterSpec);
			} catch (InvalidSyntaxException e1) {
				throw new BundleException("Error converting required execution environment.", e1); //$NON-NLS-1$
			}
		}
		return filterSpec;
	}

	static String[] getOSGiEENameVersion(String bree) {
		String ee1 = null;
		String ee2 = null;
		String v1 = null;
		String v2 = null;
		int separator = bree.indexOf('/');
		if (separator <= 0 || separator == bree.length() - 1) {
			ee1 = bree;
		} else {
			ee1 = bree.substring(0, separator);
			ee2 = bree.substring(separator + 1);
		}
		int v1idx = ee1.indexOf('-');
		if (v1idx > 0 && v1idx < ee1.length() - 1) {
			// check for > 0 to avoid EEs starting with -
			// check for < len - 1 to avoid ending with -
			try {
				v1 = ee1.substring(v1idx + 1);
				// sanity check version format
				Version.parseVersion(v1);
				ee1 = ee1.substring(0, v1idx);
			} catch (IllegalArgumentException e) {
				v1 = null;
			}
		}

		int v2idx = ee2 == null ? -1 : ee2.indexOf('-');
		if (v2idx > 0 && v2idx < ee2.length() - 1) {
			// check for > 0 to avoid EEs starting with -
			// check for < len - 1 to avoid ending with -
			try {
				v2 = ee2.substring(v2idx + 1);
				Version.parseVersion(v2);
				ee2 = ee2.substring(0, v2idx);
			} catch (IllegalArgumentException e) {
				v2 = null;
			}
		}

		if (v1 == null)
			v1 = v2;
		if (v1 != null && v2 != null && !v1.equals(v2)) {
			ee1 = bree;
			ee2 = null;
			v1 = null;
			v2 = null;
		}
		if ("J2SE".equals(ee1)) //$NON-NLS-1$
			ee1 = "JavaSE"; //$NON-NLS-1$
		if ("J2SE".equals(ee2)) //$NON-NLS-1$
			ee2 = "JavaSE"; //$NON-NLS-1$

		String eeName = ee1 + (ee2 == null ? "" : '/' + ee2); //$NON-NLS-1$

		return new String[] {eeName, v1};
	}
}
