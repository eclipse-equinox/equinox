/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
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
import org.eclipse.osgi.container.namespaces.*;
import org.eclipse.osgi.internal.framework.EquinoxContainer;
import org.eclipse.osgi.internal.framework.FilterImpl;
import org.eclipse.osgi.internal.messages.Msg;
import org.eclipse.osgi.internal.util.Tokenizer;
import org.eclipse.osgi.storage.NativeCodeFinder;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
import org.osgi.framework.namespace.*;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Namespace;

/**
 * A factory for creating {@link ModuleRevisionBuilder}s based on OSGi bundle manifests.
 * @since 3.10
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class OSGiManifestBuilderFactory {
	private static final String ATTR_TYPE_STRING = "string"; //$NON-NLS-1$
	private static final String ATTR_TYPE_VERSION = "version"; //$NON-NLS-1$
	private static final String ATTR_TYPE_URI = "uri"; //$NON-NLS-1$
	private static final String ATTR_TYPE_LONG = "long"; //$NON-NLS-1$
	private static final String ATTR_TYPE_DOUBLE = "double"; //$NON-NLS-1$
	private static final String ATTR_TYPE_SET = "set"; //$NON-NLS-1$
	private static final String ATTR_TYPE_LIST = "List"; //$NON-NLS-1$
	private static final String ATTR_OLD_REPRIVIDE = "reprovide"; //$NON-NLS-1$
	private static final String HEADER_OLD_PROVIDE_PACKAGE = "Provide-Package"; //$NON-NLS-1$
	private static final String[] DEFINED_OSGI_VALIDATE_HEADERS = {Constants.IMPORT_PACKAGE, Constants.DYNAMICIMPORT_PACKAGE, Constants.EXPORT_PACKAGE, Constants.FRAGMENT_HOST, Constants.BUNDLE_SYMBOLICNAME, Constants.REQUIRE_BUNDLE};
	private static final Collection<String> SYSTEM_CAPABILITIES = Collections.unmodifiableCollection(Arrays.asList(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE, NativeNamespace.NATIVE_NAMESPACE));
	private static final Collection<String> PROHIBITED_CAPABILITIES = Collections.unmodifiableCollection(Arrays.asList(IdentityNamespace.IDENTITY_NAMESPACE));

	/**
	 * Creates a builder for the specified bundle manifest
	 * @param manifest the bundle manifest
	 * @return a builder for the specified bundle manifest
	 * @throws BundleException if the bundle manifest is invalid
	 */
	public static ModuleRevisionBuilder createBuilder(Map<String, String> manifest) throws BundleException {
		return createBuilder(manifest, null, null, null);
	}

	/**
	 * Creates a builder for the specified bundle manifest.  An alias can be supplied
	 * for the symbolic name.  Also extra package exports and extra provided capabilities
	 * may be specified outside of the supplied manifest.  This is useful for creating
	 * a builder for the system module which takes into account the configuration
	 * properties {@link Constants#FRAMEWORK_SYSTEMPACKAGES_EXTRA} and
	 * {@link Constants#FRAMEWORK_SYSTEMCAPABILITIES_EXTRA}.
	 * @param manifest the bundle manifest
	 * @param symbolicNameAlias the symbolic name alias.  A <code>null</code> value is allowed.
	 * @param extraExports the extra package exports.  A <code>null</code> value is allowed.
	 * @param extraCapabilities the extra provided capabilities.   A <code>null</code> value is allowed.
	 * @return a builder for the specified bundle manifest
	 * @throws BundleException if the bundle manifest is invalid
	 */
	public static ModuleRevisionBuilder createBuilder(Map<String, String> manifest, String symbolicNameAlias, String extraExports, String extraCapabilities) throws BundleException {
		ModuleRevisionBuilder builder = new ModuleRevisionBuilder();

		int manifestVersion = getManifestVersion(manifest);
		if (manifestVersion >= 2) {
			validateHeaders(manifest);
		}

		Object symbolicName = getSymbolicNameAndVersion(builder, manifest, symbolicNameAlias, manifestVersion);

		Collection<Map<String, Object>> exportedPackages = new ArrayList<Map<String, Object>>();
		getPackageExports(builder, ManifestElement.parseHeader(Constants.EXPORT_PACKAGE, manifest.get(Constants.EXPORT_PACKAGE)), symbolicName, exportedPackages);
		getPackageExports(builder, ManifestElement.parseHeader(HEADER_OLD_PROVIDE_PACKAGE, manifest.get(HEADER_OLD_PROVIDE_PACKAGE)), symbolicName, exportedPackages);
		if (extraExports != null && !extraExports.isEmpty()) {
			getPackageExports(builder, ManifestElement.parseHeader(Constants.EXPORT_PACKAGE, extraExports), symbolicName, exportedPackages);
		}
		getPackageImports(builder, manifest, exportedPackages, manifestVersion);

		getRequireBundle(builder, ManifestElement.parseHeader(Constants.REQUIRE_BUNDLE, manifest.get(Constants.REQUIRE_BUNDLE)));

		getProvideCapabilities(builder, ManifestElement.parseHeader(Constants.PROVIDE_CAPABILITY, manifest.get(Constants.PROVIDE_CAPABILITY)), extraCapabilities == null);
		if (extraCapabilities != null && !extraCapabilities.isEmpty()) {
			getProvideCapabilities(builder, ManifestElement.parseHeader(Constants.PROVIDE_CAPABILITY, extraCapabilities), false);
		}
		getRequireCapabilities(builder, ManifestElement.parseHeader(Constants.REQUIRE_CAPABILITY, manifest.get(Constants.REQUIRE_CAPABILITY)));

		addRequireEclipsePlatform(builder, manifest);

		getEquinoxDataCapability(builder, manifest);

		getFragmentHost(builder, ManifestElement.parseHeader(Constants.FRAGMENT_HOST, manifest.get(Constants.FRAGMENT_HOST)));

		convertBREEs(builder, manifest);

		getNativeCode(builder, manifest);
		return builder;
	}

	private static void validateHeaders(Map<String, String> manifest) throws BundleException {
		for (int i = 0; i < DEFINED_OSGI_VALIDATE_HEADERS.length; i++) {
			String header = manifest.get(DEFINED_OSGI_VALIDATE_HEADERS[i]);
			if (header != null) {
				ManifestElement[] elements = ManifestElement.parseHeader(DEFINED_OSGI_VALIDATE_HEADERS[i], header);
				checkForDuplicateDirectivesAttributes(DEFINED_OSGI_VALIDATE_HEADERS[i], elements);
				if (DEFINED_OSGI_VALIDATE_HEADERS[i] == Constants.IMPORT_PACKAGE)
					checkImportExportSyntax(DEFINED_OSGI_VALIDATE_HEADERS[i], elements, false, false);
				if (DEFINED_OSGI_VALIDATE_HEADERS[i] == Constants.DYNAMICIMPORT_PACKAGE)
					checkImportExportSyntax(DEFINED_OSGI_VALIDATE_HEADERS[i], elements, false, true);
				if (DEFINED_OSGI_VALIDATE_HEADERS[i] == Constants.EXPORT_PACKAGE)
					checkImportExportSyntax(DEFINED_OSGI_VALIDATE_HEADERS[i], elements, true, false);
				if (DEFINED_OSGI_VALIDATE_HEADERS[i] == Constants.FRAGMENT_HOST)
					checkExtensionBundle(DEFINED_OSGI_VALIDATE_HEADERS[i], elements, manifest);
			} else if (DEFINED_OSGI_VALIDATE_HEADERS[i] == Constants.BUNDLE_SYMBOLICNAME) {
				throw new BundleException(Constants.BUNDLE_SYMBOLICNAME + " header is required.", BundleException.MANIFEST_ERROR); //$NON-NLS-1$
			}
		}
	}

	private static void checkImportExportSyntax(String headerKey, ManifestElement[] elements, boolean export, boolean dynamic) throws BundleException {
		if (elements == null)
			return;
		int length = elements.length;
		Set<String> packages = new HashSet<String>(length);
		for (int i = 0; i < length; i++) {
			// check for duplicate imports
			String[] packageNames = elements[i].getValueComponents();
			for (int j = 0; j < packageNames.length; j++) {
				if (!export && !dynamic && packages.contains(packageNames[j])) {
					String message = NLS.bind(Msg.MANIFEST_INVALID_HEADER_EXCEPTION, headerKey, elements[i].toString());
					throw new BundleException(message + " : " + NLS.bind(Msg.HEADER_PACKAGE_DUPLICATES, packageNames[j]), BundleException.MANIFEST_ERROR); //$NON-NLS-1$
				}
				// check for java.*
				if (packageNames[j].startsWith("java.")) { //$NON-NLS-1$
					String message = NLS.bind(Msg.MANIFEST_INVALID_HEADER_EXCEPTION, headerKey, elements[i].toString());
					throw new BundleException(message + " : " + NLS.bind(Msg.HEADER_PACKAGE_JAVA, packageNames[j]), BundleException.MANIFEST_ERROR); //$NON-NLS-1$
				}
				packages.add(packageNames[j]);
			}
			// check for version/specification version mismatch
			String version = elements[i].getAttribute(Constants.VERSION_ATTRIBUTE);
			if (version != null) {
				String specVersion = elements[i].getAttribute(Constants.PACKAGE_SPECIFICATION_VERSION);
				if (specVersion != null && !specVersion.equals(version))
					throw new BundleException(NLS.bind(Msg.HEADER_VERSION_ERROR, Constants.VERSION_ATTRIBUTE, Constants.PACKAGE_SPECIFICATION_VERSION), BundleException.MANIFEST_ERROR);
			}
			// check for bundle-symbolic-name and bundle-verion attibures
			// (failure)
			if (export) {
				if (elements[i].getAttribute(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE) != null) {
					String message = NLS.bind(Msg.MANIFEST_INVALID_HEADER_EXCEPTION, headerKey, elements[i].toString());
					throw new BundleException(message + " : " + NLS.bind(Msg.HEADER_EXPORT_ATTR_ERROR, Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE, Constants.EXPORT_PACKAGE), BundleException.MANIFEST_ERROR); //$NON-NLS-1$
				}
				if (elements[i].getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE) != null) {
					String message = NLS.bind(Msg.MANIFEST_INVALID_HEADER_EXCEPTION, headerKey, elements[i].toString());
					throw new BundleException(NLS.bind(message + " : " + Msg.HEADER_EXPORT_ATTR_ERROR, Constants.BUNDLE_VERSION_ATTRIBUTE, Constants.EXPORT_PACKAGE), BundleException.MANIFEST_ERROR); //$NON-NLS-1$
				}
			}
		}
	}

	private static void checkForDuplicateDirectivesAttributes(String headerKey, ManifestElement[] elements) throws BundleException {
		// check for duplicate directives
		for (int i = 0; i < elements.length; i++) {
			Enumeration<String> directiveKeys = elements[i].getDirectiveKeys();
			if (directiveKeys != null) {
				while (directiveKeys.hasMoreElements()) {
					String key = directiveKeys.nextElement();
					String[] directives = elements[i].getDirectives(key);
					if (directives.length > 1) {
						String message = NLS.bind(Msg.MANIFEST_INVALID_HEADER_EXCEPTION, headerKey, elements[i].toString());
						throw new BundleException(NLS.bind(message + " : " + Msg.HEADER_DIRECTIVE_DUPLICATES, key), BundleException.MANIFEST_ERROR); //$NON-NLS-1$
					}
				}
			}
			Enumeration<String> attrKeys = elements[i].getKeys();
			if (attrKeys != null) {
				while (attrKeys.hasMoreElements()) {
					String key = attrKeys.nextElement();
					String[] attrs = elements[i].getAttributes(key);
					if (attrs.length > 1) {
						String message = NLS.bind(Msg.MANIFEST_INVALID_HEADER_EXCEPTION, headerKey, elements[i].toString());
						throw new BundleException(message + " : " + NLS.bind(Msg.HEADER_ATTRIBUTE_DUPLICATES, key), BundleException.MANIFEST_ERROR); //$NON-NLS-1$
					}
				}
			}
		}
	}

	private static void checkExtensionBundle(String headerKey, ManifestElement[] elements, Map<String, String> manifest) throws BundleException {
		if (elements.length == 0)
			return;
		String hostName = elements[0].getValue();
		// XXX: The extension bundle check is done against system.bundle and org.eclipse.osgi
		if (!hostName.equals(Constants.SYSTEM_BUNDLE_SYMBOLICNAME) && !hostName.equals(EquinoxContainer.NAME)) {
			if (elements[0].getDirective(Constants.EXTENSION_DIRECTIVE) != null) {
				String message = NLS.bind(Msg.MANIFEST_INVALID_HEADER_EXCEPTION, headerKey, elements[0].toString());
				throw new BundleException(message + " : " + NLS.bind(Msg.HEADER_EXTENSION_ERROR, hostName), BundleException.MANIFEST_ERROR); //$NON-NLS-1$
			}
		} else {
			if (manifest.get(Constants.IMPORT_PACKAGE) != null)
				throw new BundleException(Msg.OSGiManifestBuilderFactory_ExtensionImportError, BundleException.MANIFEST_ERROR);
			if (manifest.get(Constants.REQUIRE_BUNDLE) != null)
				throw new BundleException(Msg.OSGiManifestBuilderFactory_ExtensionReqBundleError, BundleException.MANIFEST_ERROR);
			if (manifest.get(Constants.REQUIRE_CAPABILITY) != null) {
				ManifestElement[] manifestElements = ManifestElement.parseHeader(Constants.REQUIRE_CAPABILITY, manifest.get(Constants.REQUIRE_CAPABILITY));
				if (manifestElements != null) {
					for (ManifestElement manifestElement : manifestElements) {
						for (String value : manifestElement.getValueComponents()) {
							if (!ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE.equals(value)) {
								throw new BundleException(NLS.bind(Msg.OSGiManifestBuilderFactory_ExtensionReqCapError, value), BundleException.MANIFEST_ERROR);
							}
						}
					}
				}

			}
			if (manifest.get(Constants.BUNDLE_NATIVECODE) != null)
				throw new BundleException(Msg.OSGiManifestBuilderFactory_ExtensionNativeError, BundleException.MANIFEST_ERROR);

		}
	}

	private static int getManifestVersion(Map<String, String> manifest) {
		String manifestVersionHeader = manifest.get(Constants.BUNDLE_MANIFESTVERSION);
		return manifestVersionHeader == null ? 1 : Integer.parseInt(manifestVersionHeader);
	}

	private static Object getSymbolicNameAndVersion(ModuleRevisionBuilder builder, Map<String, String> manifest, String symbolicNameAlias, int manifestVersion) throws BundleException {
		boolean isFragment = manifest.get(Constants.FRAGMENT_HOST) != null;
		builder.setTypes(isFragment ? BundleRevision.TYPE_FRAGMENT : 0);
		String version = manifest.get(Constants.BUNDLE_VERSION);
		try {
			builder.setVersion((version != null) ? Version.parseVersion(version) : Version.emptyVersion);
		} catch (IllegalArgumentException ex) {
			if (manifestVersion >= 2) {
				String message = NLS.bind(Msg.OSGiManifestBuilderFactory_InvalidManifestError, Constants.BUNDLE_VERSION, version);
				throw new BundleException(message, BundleException.MANIFEST_ERROR, ex);
			}
			// prior to R4 the Bundle-Version header was not interpreted by the Framework;
			// must not fail for old R3 style bundles
		}

		Object symbolicName = null;
		String symbolicNameHeader = manifest.get(Constants.BUNDLE_SYMBOLICNAME);
		if (symbolicNameHeader != null) {
			ManifestElement[] symbolicNameElements = ManifestElement.parseHeader(Constants.BUNDLE_SYMBOLICNAME, symbolicNameHeader);
			if (symbolicNameElements.length > 0) {
				ManifestElement bsnElement = symbolicNameElements[0];
				builder.setSymbolicName(bsnElement.getValue());
				if (symbolicNameAlias != null) {
					List<String> result = new ArrayList<String>();
					result.add(builder.getSymbolicName());
					result.add(symbolicNameAlias);
					symbolicName = result;
				} else {
					symbolicName = builder.getSymbolicName();
				}
				Map<String, String> directives = getDirectives(bsnElement);
				directives.remove(BundleNamespace.CAPABILITY_USES_DIRECTIVE);
				directives.remove(BundleNamespace.CAPABILITY_EFFECTIVE_DIRECTIVE);
				Map<String, Object> attributes = getAttributes(bsnElement);
				if (!directives.containsKey(IdentityNamespace.CAPABILITY_SINGLETON_DIRECTIVE)) {
					// previous versions of equinox treated the singleton attribute as a directive
					Object singletonAttr = attributes.get(IdentityNamespace.CAPABILITY_SINGLETON_DIRECTIVE);
					if ("true".equals(singletonAttr)) { //$NON-NLS-1$
						directives.put(IdentityNamespace.CAPABILITY_SINGLETON_DIRECTIVE, (String) singletonAttr);
					}
				}
				if (!isFragment) {
					// create the bundle namespace
					Map<String, Object> bundleAttributes = new HashMap<String, Object>(attributes);
					bundleAttributes.put(BundleNamespace.BUNDLE_NAMESPACE, symbolicName);
					bundleAttributes.put(BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE, builder.getVersion());
					builder.addCapability(BundleNamespace.BUNDLE_NAMESPACE, directives, bundleAttributes);

					// create the host namespace
					// only if the directive is not never
					if (!HostNamespace.FRAGMENT_ATTACHMENT_NEVER.equals(directives.get(HostNamespace.CAPABILITY_FRAGMENT_ATTACHMENT_DIRECTIVE))) {
						Map<String, Object> hostAttributes = new HashMap<String, Object>(attributes);
						hostAttributes.put(HostNamespace.HOST_NAMESPACE, symbolicName);
						hostAttributes.put(HostNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE, builder.getVersion());
						builder.addCapability(HostNamespace.HOST_NAMESPACE, directives, hostAttributes);
					}
				}
				// every bundle that has a symbolic name gets an identity;
				// never use the symbolic name alias for the identity namespace
				Map<String, Object> identityAttributes = new HashMap<String, Object>(attributes);
				identityAttributes.put(IdentityNamespace.IDENTITY_NAMESPACE, builder.getSymbolicName());
				identityAttributes.put(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, builder.getVersion());
				identityAttributes.put(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, isFragment ? IdentityNamespace.TYPE_FRAGMENT : IdentityNamespace.TYPE_BUNDLE);
				builder.addCapability(IdentityNamespace.IDENTITY_NAMESPACE, directives, identityAttributes);
			}
		}

		return symbolicName == null ? symbolicNameAlias : symbolicName;
	}

	private static void getPackageExports(ModuleRevisionBuilder builder, ManifestElement[] exportElements, Object symbolicName, Collection<Map<String, Object>> exportedPackages) {
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
			Version version = versionAttr == null ? (specVersionAttr == null ? Version.emptyVersion : Version.parseVersion(specVersionAttr)) : Version.parseVersion(versionAttr);
			attributes.put(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE, version);
			if (symbolicName != null) {
				attributes.put(PackageNamespace.CAPABILITY_BUNDLE_SYMBOLICNAME_ATTRIBUTE, symbolicName);
			}
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
			// the attribute "optional" used to be used in old versions of equinox to specify optional imports
			// preserving behavior for compatibility
			Object optionalAttr = attributes.remove(Namespace.RESOLUTION_OPTIONAL);
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

				// check the old optional attribute
				if ("true".equals(optionalAttr) && packageDirectives.get(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE) == null) { //$NON-NLS-1$
					packageDirectives.put(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE, Namespace.RESOLUTION_OPTIONAL);
				}
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

	private static Map<String, String> getDirectives(ManifestElement element) {
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

	private static void getRequireBundle(ModuleRevisionBuilder builder, ManifestElement[] requireBundles) {
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
			// These two attrs are used as directives in previous versions of equinox
			// Preserving behavior for compatibility reasons.
			Object optionalAttr = attributes.remove(Namespace.RESOLUTION_OPTIONAL);
			Object reprovideAttr = attributes.remove(ATTR_OLD_REPRIVIDE);
			for (String bundleName : bundleNames) {
				if (bundleName.equals(builder.getSymbolicName())) {
					// ignore requirements to ourself
					continue;
				}
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
				// check the old compatibility attributes
				if ("true".equals(optionalAttr) && bundleDirectives.get(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE) == null) { //$NON-NLS-1$
					bundleDirectives.put(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE, Namespace.RESOLUTION_OPTIONAL);
				}
				if ("true".equals(reprovideAttr) && bundleDirectives.get(BundleNamespace.REQUIREMENT_VISIBILITY_DIRECTIVE) == null) { //$NON-NLS-1$
					bundleDirectives.put(BundleNamespace.REQUIREMENT_VISIBILITY_DIRECTIVE, BundleNamespace.VISIBILITY_REEXPORT);
				}
				builder.addRequirement(BundleNamespace.BUNDLE_NAMESPACE, bundleDirectives, new HashMap<String, Object>(0));
			}
		}
	}

	private static void getFragmentHost(ModuleRevisionBuilder builder, ManifestElement[] fragmentHosts) {
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
		// Add a fragment capability to advertise what host this resource is providing a fragment for
		directives = Collections.singletonMap(EquinoxModuleDataNamespace.CAPABILITY_EFFECTIVE_DIRECTIVE, EquinoxModuleDataNamespace.EFFECTIVE_INFORMATION);
		builder.addCapability(EquinoxFragmentNamespace.FRAGMENT_NAMESPACE, directives, Collections.<String, Object> singletonMap(EquinoxFragmentNamespace.FRAGMENT_NAMESPACE, hostName));
	}

	private static void getProvideCapabilities(ModuleRevisionBuilder builder, ManifestElement[] provideElements, boolean checkSystemCapabilities) throws BundleException {
		if (provideElements == null)
			return;
		for (ManifestElement provideElement : provideElements) {
			String[] namespaces = provideElement.getValueComponents();
			Map<String, Object> attributes = getAttributes(provideElement);
			Map<String, String> directives = getDirectives(provideElement);
			for (String namespace : namespaces) {
				if (PROHIBITED_CAPABILITIES.contains(namespace) || (checkSystemCapabilities && SYSTEM_CAPABILITIES.contains(namespace))) {
					throw new BundleException("A bundle is not allowed to define a capability in the " + namespace + " name space.", BundleException.MANIFEST_ERROR); //$NON-NLS-1$ //$NON-NLS-2$
				}

				builder.addCapability(namespace, directives, attributes);
			}
		}
	}

	private static void getRequireCapabilities(ModuleRevisionBuilder builder, ManifestElement[] requireElements) {
		if (requireElements == null)
			return;
		for (ManifestElement requireElement : requireElements) {
			String[] namespaces = requireElement.getValueComponents();
			Map<String, Object> attributes = getAttributes(requireElement);
			Map<String, String> directives = getDirectives(requireElement);
			for (String namespace : namespaces) {
				builder.addRequirement(namespace, directives, attributes);
			}
		}
	}

	private static void addRequireEclipsePlatform(ModuleRevisionBuilder builder, Map<String, String> manifest) {
		String platformFilter = manifest.get(EclipsePlatformNamespace.ECLIPSE_PLATFORM_FILTER_HEADER);
		if (platformFilter == null) {
			return;
		}
		// only support one
		HashMap<String, String> directives = new HashMap<String, String>();
		directives.put(EclipsePlatformNamespace.REQUIREMENT_FILTER_DIRECTIVE, platformFilter);
		builder.addRequirement(EclipsePlatformNamespace.ECLIPSE_PLATFORM_NAMESPACE, directives, Collections.<String, Object> emptyMap());
	}

	private static void getEquinoxDataCapability(ModuleRevisionBuilder builder, Map<String, String> manifest) throws BundleException {
		Map<String, Object> attributes = new HashMap<String, Object>();

		// Get the activation policy attributes
		ManifestElement[] policyElements = ManifestElement.parseHeader(Constants.BUNDLE_ACTIVATIONPOLICY, manifest.get(Constants.BUNDLE_ACTIVATIONPOLICY));
		if (policyElements != null) {
			ManifestElement policy = policyElements[0];
			String policyName = policy.getValue();
			if (EquinoxModuleDataNamespace.CAPABILITY_ACTIVATION_POLICY_LAZY.equals(policyName)) {
				attributes.put(EquinoxModuleDataNamespace.CAPABILITY_ACTIVATION_POLICY, policyName);
				String includeSpec = policy.getDirective(Constants.INCLUDE_DIRECTIVE);
				if (includeSpec != null) {
					attributes.put(EquinoxModuleDataNamespace.CAPABILITY_LAZY_INCLUDE_ATTRIBUTE, convertValueWithNoWhitespace("List<String>", includeSpec)); //$NON-NLS-1$
				}
				String excludeSpec = policy.getDirective(Constants.EXCLUDE_DIRECTIVE);
				if (excludeSpec != null) {
					attributes.put(EquinoxModuleDataNamespace.CAPABILITY_LAZY_EXCLUDE_ATTRIBUTE, convertValueWithNoWhitespace("List<String>", excludeSpec)); //$NON-NLS-1$
				}
			}
		} else {
			policyElements = ManifestElement.parseHeader(EquinoxModuleDataNamespace.LAZYSTART_HEADER, manifest.get(EquinoxModuleDataNamespace.LAZYSTART_HEADER));
			if (policyElements == null) {
				policyElements = ManifestElement.parseHeader(EquinoxModuleDataNamespace.AUTOSTART_HEADER, manifest.get(EquinoxModuleDataNamespace.AUTOSTART_HEADER));
			}
			if (policyElements != null) {
				ManifestElement policy = policyElements[0];
				String excludeSpec = policy.getAttribute(EquinoxModuleDataNamespace.LAZYSTART_EXCEPTIONS_ATTRIBUTE);
				if ("true".equals(policy.getValue())) { //$NON-NLS-1$
					attributes.put(EquinoxModuleDataNamespace.CAPABILITY_ACTIVATION_POLICY, EquinoxModuleDataNamespace.CAPABILITY_ACTIVATION_POLICY_LAZY);
					if (excludeSpec != null) {
						attributes.put(EquinoxModuleDataNamespace.CAPABILITY_LAZY_EXCLUDE_ATTRIBUTE, convertValueWithNoWhitespace("List<String>", excludeSpec)); //$NON-NLS-1$
					}
				} else {
					// NOTICE - the exclude list gets converted to an include list when the header is not true
					if (excludeSpec != null) {
						attributes.put(EquinoxModuleDataNamespace.CAPABILITY_ACTIVATION_POLICY, EquinoxModuleDataNamespace.CAPABILITY_ACTIVATION_POLICY_LAZY);
						attributes.put(EquinoxModuleDataNamespace.CAPABILITY_LAZY_INCLUDE_ATTRIBUTE, convertValueWithNoWhitespace("List<String>", excludeSpec)); //$NON-NLS-1$
					}
				}
			}
		}

		// Get the activator
		String activator = manifest.get(Constants.BUNDLE_ACTIVATOR);
		if (activator == null && manifest.get(Constants.FRAGMENT_HOST) != null) {
			// we look for the extension activator for fragments
			// probably should do this only for framework extensions, but there is no harm to check for others
			// it is only acted upon for framework extension fragments
			activator = manifest.get(Constants.EXTENSION_BUNDLE_ACTIVATOR);
		}
		if (activator != null) {
			attributes.put(EquinoxModuleDataNamespace.CAPABILITY_ACTIVATOR, activator);
		}

		// Get the class path
		ManifestElement[] classpathElements = ManifestElement.parseHeader(Constants.BUNDLE_CLASSPATH, manifest.get(Constants.BUNDLE_CLASSPATH));
		if (classpathElements != null) {
			List<String> classpath = new ArrayList<String>();
			for (ManifestElement element : classpathElements) {
				String[] components = element.getValueComponents();
				for (String component : components) {
					classpath.add(component);
				}
			}
			attributes.put(EquinoxModuleDataNamespace.CAPABILITY_CLASSPATH, classpath);
		}

		// Get the buddy policy list
		ManifestElement[] buddyPolicies = ManifestElement.parseHeader(EquinoxModuleDataNamespace.BUDDY_POLICY_HEADER, manifest.get(EquinoxModuleDataNamespace.BUDDY_POLICY_HEADER));
		if (buddyPolicies != null) {
			List<String> policies = new ArrayList<String>();
			for (ManifestElement element : buddyPolicies) {
				for (String component : element.getValueComponents()) {
					policies.add(component);
				}
			}
			attributes.put(EquinoxModuleDataNamespace.CAPABILITY_BUDDY_POLICY, policies);
		}

		// Get the registered buddy list
		ManifestElement[] registeredBuddies = ManifestElement.parseHeader(EquinoxModuleDataNamespace.REGISTERED_BUDDY_HEADER, manifest.get(EquinoxModuleDataNamespace.REGISTERED_BUDDY_HEADER));
		if (registeredBuddies != null) {
			List<String> buddies = new ArrayList<String>();
			for (ManifestElement element : registeredBuddies) {
				for (String component : element.getValueComponents()) {
					buddies.add(component);
				}
			}
			attributes.put(EquinoxModuleDataNamespace.CAPABILITY_BUDDY_REGISTERED, buddies);
		}

		// only create the capability if the attributes is not empty
		if (!attributes.isEmpty()) {
			Map<String, String> directives = Collections.singletonMap(EquinoxModuleDataNamespace.CAPABILITY_EFFECTIVE_DIRECTIVE, EquinoxModuleDataNamespace.EFFECTIVE_INFORMATION);
			builder.addCapability(EquinoxModuleDataNamespace.MODULE_DATA_NAMESPACE, directives, attributes);
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

	private static Object convertValueWithNoWhitespace(String type, String value) {
		value = value.replaceAll("\\s", ""); //$NON-NLS-1$//$NON-NLS-2$
		return convertValue(type, value);
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

	static String escapeFilterInput(final String value) {
		boolean escaped = false;
		int inlen = value.length();
		int outlen = inlen << 1; /* inlen * 2 */

		char[] output = new char[outlen];
		value.getChars(0, inlen, output, inlen);

		int cursor = 0;
		for (int i = inlen; i < outlen; i++) {
			char c = output[i];
			switch (c) {
				case '*' :
				case '\\' :
				case '(' :
				case ')' :
					output[cursor] = '\\';
					cursor++;
					escaped = true;
					break;
			}

			output[cursor] = c;
			cursor++;
		}

		return escaped ? new String(output, 0, cursor) : value;
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
				throw new BundleException("Error converting required execution environment.", BundleException.MANIFEST_ERROR, e1); //$NON-NLS-1$
			}
		}
		return filterSpec;
	}

	private static String[] getOSGiEENameVersion(String bree) {
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

		return new String[] {escapeFilterInput(eeName), v1};
	}

	static class NativeClause implements Comparable<NativeClause> {
		private final int manifestIndex;
		final List<String> nativePaths;
		final String filter;
		private final Version highestFloor;
		private final boolean hasLanguage;

		NativeClause(int manifestIndex, ManifestElement clause) throws BundleException {
			this.manifestIndex = manifestIndex;
			this.nativePaths = new ArrayList<String>(Arrays.asList(clause.getValueComponents()));
			StringBuilder sb = new StringBuilder();
			sb.append("(&"); //$NON-NLS-1$
			addToNativeCodeFilter(sb, clause, Constants.BUNDLE_NATIVECODE_OSNAME);
			addToNativeCodeFilter(sb, clause, Constants.BUNDLE_NATIVECODE_PROCESSOR);
			this.highestFloor = (Version) addToNativeCodeFilter(sb, clause, Constants.BUNDLE_NATIVECODE_OSVERSION);
			this.hasLanguage = ((Boolean) addToNativeCodeFilter(sb, clause, Constants.BUNDLE_NATIVECODE_LANGUAGE)).booleanValue();
			String selectionFilter = clause.getAttribute(Constants.SELECTION_FILTER_ATTRIBUTE);
			if (selectionFilter != null) {
				// do a sanity check to make sure the filter is valid
				try {
					FrameworkUtil.createFilter(selectionFilter);
				} catch (InvalidSyntaxException e) {
					throw new BundleException("Bad native code selection-filter.", BundleException.MANIFEST_ERROR, e); //$NON-NLS-1$
				}
				sb.append(selectionFilter);
			}
			sb.append(')');
			String filterResult = sb.toString();
			if (filterResult.equals("(&)")) { //$NON-NLS-1$
				// no matching attributes found just match all osnames
				filterResult = "(" + NativeNamespace.CAPABILITY_OSNAME_ATTRIBUTE + "=*)"; //$NON-NLS-1$ //$NON-NLS-2$
			}
			this.filter = filterResult;
		}

		private static Object addToNativeCodeFilter(StringBuilder filter, ManifestElement nativeCode, String attribute) {
			Boolean hasLanguage = Boolean.FALSE;
			Version highestFloor = null;
			String[] attrValues = nativeCode.getAttributes(attribute);
			if (attrValues != null) {

				String filterAttribute = attribute;
				if (Constants.BUNDLE_NATIVECODE_OSNAME.equals(attribute)) {
					filterAttribute = NativeNamespace.CAPABILITY_OSNAME_ATTRIBUTE;
				} else if (Constants.BUNDLE_NATIVECODE_PROCESSOR.equals(attribute)) {
					filterAttribute = NativeNamespace.CAPABILITY_PROCESSOR_ATTRIBUTE;
				} else if (Constants.BUNDLE_NATIVECODE_LANGUAGE.equals(attribute)) {
					filterAttribute = NativeNamespace.CAPABILITY_LANGUAGE_ATTRIBUTE;
					hasLanguage = attrValues.length > 0;
				} else if (Constants.BUNDLE_NATIVECODE_OSVERSION.equals(attribute)) {
					filterAttribute = NativeNamespace.CAPABILITY_OSVERSION_ATTRIBUTE;
				}

				if (attrValues.length > 1) {
					filter.append("(|"); //$NON-NLS-1$
				}
				for (String attrAlias : attrValues) {
					if (NativeNamespace.CAPABILITY_OSVERSION_ATTRIBUTE.equals(filterAttribute)) {
						VersionRange range = new VersionRange(attrAlias);
						if (highestFloor == null || highestFloor.compareTo(range.getLeft()) < 0) {
							highestFloor = range.getLeft();
						}
						filter.append(range.toFilterString(filterAttribute));
					} else {
						filter.append('(').append(filterAttribute).append("~=").append(escapeFilterInput(attrAlias)).append(')'); //$NON-NLS-1$
					}
				}
				if (attrValues.length > 1) {
					filter.append(')');
				}
			}
			return Constants.BUNDLE_NATIVECODE_LANGUAGE.equals(attribute) ? hasLanguage : highestFloor;
		}

		@Override
		public int compareTo(NativeClause other) {
			if (this.highestFloor != null) {
				if (other == null) {
					return -1;
				}
				if (other.highestFloor == null) {
					return -1;
				}
				int compareVersions = this.highestFloor.compareTo(other.highestFloor);
				if (compareVersions != 0) {
					return -(compareVersions);
				}
			} else if (other.highestFloor != null) {
				return 1;
			}
			if (this.hasLanguage) {
				return other.hasLanguage ? this.manifestIndex - other.manifestIndex : 1;
			}
			return other.hasLanguage ? -1 : this.manifestIndex - other.manifestIndex;
		}
	}

	private static void getNativeCode(ModuleRevisionBuilder builder, Map<String, String> manifest) throws BundleException {
		ManifestElement[] elements = ManifestElement.parseHeader(Constants.BUNDLE_NATIVECODE, manifest.get(Constants.BUNDLE_NATIVECODE));
		if (elements == null) {
			return;
		}

		boolean optional = false;

		List<NativeClause> nativeClauses = new ArrayList<NativeClause>();
		for (int i = 0; i < elements.length; i++) {
			if (i == elements.length - 1) {
				optional = elements[i].getValue().equals("*"); //$NON-NLS-1$
			}
			if (!optional) {
				nativeClauses.add(new NativeClause(i, elements[i]));
			}
		}
		Collections.sort(nativeClauses);

		Map<String, Object> attributes = new HashMap<String, Object>(2);
		int numNativePaths = nativeClauses.size();
		StringBuilder allNativeFilters = new StringBuilder();
		allNativeFilters.append("(|"); //$NON-NLS-1$
		for (int i = 0; i < numNativePaths; i++) {
			NativeClause nativeClause = nativeClauses.get(i);
			if (numNativePaths == 1) {
				attributes.put(NativeCodeFinder.REQUIREMENT_NATIVE_PATHS_ATTRIBUTE, nativeClause.nativePaths);
			} else {
				attributes.put(NativeCodeFinder.REQUIREMENT_NATIVE_PATHS_ATTRIBUTE + '.' + i, nativeClause.nativePaths);
			}
			allNativeFilters.append(nativeClauses.get(i).filter);
		}
		allNativeFilters.append(')');

		Map<String, String> directives = new HashMap<String, String>(2);
		directives.put(NativeNamespace.REQUIREMENT_FILTER_DIRECTIVE, allNativeFilters.toString());
		if (optional) {
			directives.put(NativeNamespace.REQUIREMENT_RESOLUTION_DIRECTIVE, NativeNamespace.RESOLUTION_OPTIONAL);
		}

		builder.addRequirement(NativeNamespace.NATIVE_NAMESPACE, directives, attributes);
	}
}
