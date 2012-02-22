/*******************************************************************************
 * Copyright (c) 2003, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Danail Nachev -  ProSyst - bug 218625
 *******************************************************************************/
package org.eclipse.osgi.internal.resolver;

import java.lang.reflect.Constructor;
import java.util.*;
import org.eclipse.osgi.framework.internal.core.*;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Namespace;

/**
 * This class builds bundle description objects from manifests
 */
public class StateBuilder {
	private static final String[] DEFINED_EXPORT_PACKAGE_DIRECTIVES = {Constants.USES_DIRECTIVE, Constants.INCLUDE_DIRECTIVE, Constants.EXCLUDE_DIRECTIVE, Constants.FRIENDS_DIRECTIVE, Constants.INTERNAL_DIRECTIVE, Constants.MANDATORY_DIRECTIVE};
	private static final String[] DEFINED_IMPORT_PACKAGE_DIRECTIVES = {Constants.RESOLUTION_DIRECTIVE};
	private static final String[] DEFINED_PACKAGE_MATCHING_ATTRS = {Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE, Constants.BUNDLE_VERSION_ATTRIBUTE, Constants.PACKAGE_SPECIFICATION_VERSION, Constants.VERSION_ATTRIBUTE};
	private static final String[] DEFINED_REQUIRE_BUNDLE_DIRECTIVES = {Constants.RESOLUTION_DIRECTIVE, Constants.VISIBILITY_DIRECTIVE};
	private static final String[] DEFINED_FRAGMENT_HOST_DIRECTIVES = {Constants.EXTENSION_DIRECTIVE};
	static final String[] DEFINED_BSN_DIRECTIVES = {Constants.SINGLETON_DIRECTIVE, Constants.FRAGMENT_ATTACHMENT_DIRECTIVE, Constants.MANDATORY_DIRECTIVE};
	static final String[] DEFINED_BSN_MATCHING_ATTRS = {Constants.BUNDLE_VERSION_ATTRIBUTE, Constants.OPTIONAL_ATTRIBUTE, Constants.REPROVIDE_ATTRIBUTE};
	private static final String[] DEFINED_REQUIRE_CAPABILITY_DIRECTIVES = {Constants.RESOLUTION_DIRECTIVE, Constants.FILTER_DIRECTIVE, Namespace.REQUIREMENT_CARDINALITY_DIRECTIVE};
	private static final String[] DEFINED_REQUIRE_CAPABILITY_ATTRS = {};
	private static final String[] DEFINED_OSGI_VALIDATE_HEADERS = {Constants.IMPORT_PACKAGE, Constants.DYNAMICIMPORT_PACKAGE, Constants.EXPORT_PACKAGE, Constants.FRAGMENT_HOST, Constants.BUNDLE_SYMBOLICNAME, Constants.REQUIRE_BUNDLE};
	static final String GENERIC_REQUIRE = "Eclipse-GenericRequire"; //$NON-NLS-1$
	static final String GENERIC_CAPABILITY = "Eclipse-GenericCapability"; //$NON-NLS-1$

	private static final String ATTR_TYPE_STRING = "string"; //$NON-NLS-1$
	private static final String ATTR_TYPE_VERSION = "version"; //$NON-NLS-1$
	private static final String ATTR_TYPE_URI = "uri"; //$NON-NLS-1$
	private static final String ATTR_TYPE_LONG = "long"; //$NON-NLS-1$
	private static final String ATTR_TYPE_DOUBLE = "double"; //$NON-NLS-1$
	private static final String ATTR_TYPE_SET = "set"; //$NON-NLS-1$
	private static final String ATTR_TYPE_LIST = "List"; //$NON-NLS-1$
	private static final String OPTIONAL_ATTR = "optional"; //$NON-NLS-1$
	private static final String MULTIPLE_ATTR = "multiple"; //$NON-NLS-1$
	private static final String TRUE = "true"; //$NON-NLS-1$

	static BundleDescription createBundleDescription(StateImpl state, Dictionary<String, String> manifest, String location) throws BundleException {
		BundleDescriptionImpl result = new BundleDescriptionImpl();
		String manifestVersionHeader = manifest.get(Constants.BUNDLE_MANIFESTVERSION);
		boolean jreBundle = "true".equals(manifest.get(Constants.Eclipse_JREBUNDLE)); //$NON-NLS-1$
		int manifestVersion = 1;
		if (manifestVersionHeader != null)
			manifestVersion = Integer.parseInt(manifestVersionHeader);
		if (manifestVersion >= 2)
			validateHeaders(manifest, jreBundle);

		// retrieve the symbolic-name and the singleton status
		String symbolicNameHeader = manifest.get(Constants.BUNDLE_SYMBOLICNAME);
		if (symbolicNameHeader != null) {
			ManifestElement[] symbolicNameElements = ManifestElement.parseHeader(Constants.BUNDLE_SYMBOLICNAME, symbolicNameHeader);
			if (symbolicNameElements.length > 0) {
				ManifestElement bsnElement = symbolicNameElements[0];
				result.setSymbolicName(bsnElement.getValue());
				String singleton = bsnElement.getDirective(Constants.SINGLETON_DIRECTIVE);
				if (singleton == null) // TODO this is for backward compatibility; need to check manifest version < 2 to allow this after everyone has converted to new syntax
					singleton = bsnElement.getAttribute(Constants.SINGLETON_DIRECTIVE);
				result.setStateBit(BundleDescriptionImpl.SINGLETON, "true".equals(singleton)); //$NON-NLS-1$
				String fragmentAttachment = bsnElement.getDirective(Constants.FRAGMENT_ATTACHMENT_DIRECTIVE);
				if (fragmentAttachment != null) {
					if (fragmentAttachment.equals(Constants.FRAGMENT_ATTACHMENT_RESOLVETIME)) {
						result.setStateBit(BundleDescriptionImpl.ATTACH_FRAGMENTS, true);
						result.setStateBit(BundleDescriptionImpl.DYNAMIC_FRAGMENTS, false);
					} else if (fragmentAttachment.equals(Constants.FRAGMENT_ATTACHMENT_NEVER)) {
						result.setStateBit(BundleDescriptionImpl.ATTACH_FRAGMENTS, false);
						result.setStateBit(BundleDescriptionImpl.DYNAMIC_FRAGMENTS, false);
					}
				}
				result.setDirective(Constants.MANDATORY_DIRECTIVE, ManifestElement.getArrayFromList(bsnElement.getDirective(Constants.MANDATORY_DIRECTIVE)));
				result.setAttributes(getAttributes(bsnElement, DEFINED_BSN_MATCHING_ATTRS));
				result.setArbitraryDirectives(getDirectives(bsnElement, DEFINED_BSN_DIRECTIVES));
			}
		}
		// retrieve other headers
		String version = manifest.get(Constants.BUNDLE_VERSION);
		try {
			result.setVersion((version != null) ? Version.parseVersion(version) : Version.emptyVersion);
		} catch (IllegalArgumentException ex) {
			if (manifestVersion >= 2) {
				String message = NLS.bind(Msg.MANIFEST_INVALID_HEADER_EXCEPTION, Constants.BUNDLE_VERSION, version);
				throw new BundleException(message + " : " + ex.getMessage(), BundleException.MANIFEST_ERROR, ex); //$NON-NLS-1$
			}
			// prior to R4 the Bundle-Version header was not interpreted by the Framework;
			// must not fail for old R3 style bundles
		}
		result.setLocation(location);
		result.setPlatformFilter(manifest.get(Constants.ECLIPSE_PLATFORMFILTER));
		String[] brees = ManifestElement.getArrayFromList(manifest.get(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT));
		result.setExecutionEnvironments(brees);
		ManifestElement[] host = ManifestElement.parseHeader(Constants.FRAGMENT_HOST, manifest.get(Constants.FRAGMENT_HOST));
		if (host != null)
			result.setHost(createHostSpecification(host[0], state));
		ManifestElement[] exports = ManifestElement.parseHeader(Constants.EXPORT_PACKAGE, manifest.get(Constants.EXPORT_PACKAGE));
		ManifestElement[] provides = ManifestElement.parseHeader(Constants.PROVIDE_PACKAGE, manifest.get(Constants.PROVIDE_PACKAGE));
		boolean strict = state != null && state.inStrictMode();
		List<String> providedExports = new ArrayList<String>(provides == null ? 0 : provides.length);
		result.setExportPackages(createExportPackages(exports, provides, providedExports, strict));
		ManifestElement[] imports = ManifestElement.parseHeader(Constants.IMPORT_PACKAGE, manifest.get(Constants.IMPORT_PACKAGE));
		ManifestElement[] dynamicImports = ManifestElement.parseHeader(Constants.DYNAMICIMPORT_PACKAGE, manifest.get(Constants.DYNAMICIMPORT_PACKAGE));
		result.setImportPackages(createImportPackages(result.getExportPackages(), providedExports, imports, dynamicImports, manifestVersion));
		ManifestElement[] requires = ManifestElement.parseHeader(Constants.REQUIRE_BUNDLE, manifest.get(Constants.REQUIRE_BUNDLE));
		result.setRequiredBundles(createRequiredBundles(requires));
		String[][] genericAliases = getGenericAliases(state);
		ManifestElement[] genericRequires = getGenericRequires(manifest, genericAliases);
		ManifestElement[] osgiRequires = ManifestElement.parseHeader(Constants.REQUIRE_CAPABILITY, manifest.get(Constants.REQUIRE_CAPABILITY));
		result.setGenericRequires(createGenericRequires(genericRequires, osgiRequires, brees));
		ManifestElement[] genericCapabilities = getGenericCapabilities(manifest, genericAliases);
		ManifestElement[] osgiCapabilities = ManifestElement.parseHeader(Constants.PROVIDE_CAPABILITY, manifest.get(Constants.PROVIDE_CAPABILITY));
		result.setGenericCapabilities(createGenericCapabilities(genericCapabilities, osgiCapabilities, result));
		ManifestElement[] nativeCode = ManifestElement.parseHeader(Constants.BUNDLE_NATIVECODE, manifest.get(Constants.BUNDLE_NATIVECODE));
		result.setNativeCodeSpecification(createNativeCode(nativeCode));
		return result;
	}

	private static ManifestElement[] getGenericRequires(Dictionary<String, String> manifest, String[][] genericAliases) throws BundleException {
		ManifestElement[] genericRequires = ManifestElement.parseHeader(GENERIC_REQUIRE, manifest.get(GENERIC_REQUIRE));
		List<ManifestElement> aliasList = null;
		if (genericAliases.length > 0) {
			aliasList = new ArrayList<ManifestElement>(genericRequires == null ? 0 : genericRequires.length);
			for (int i = 0; i < genericAliases.length; i++) {
				ManifestElement[] aliasReqs = ManifestElement.parseHeader(genericAliases[i][1], manifest.get(genericAliases[i][1]));
				if (aliasReqs == null)
					continue;
				for (int j = 0; j < aliasReqs.length; j++) {
					StringBuffer strBuf = new StringBuffer();
					strBuf.append(aliasReqs[j].getValue()).append(':').append(genericAliases[i][2]);
					String filter = aliasReqs[j].getAttribute(Constants.SELECTION_FILTER_ATTRIBUTE);
					if (filter != null)
						strBuf.append("; ").append(Constants.SELECTION_FILTER_ATTRIBUTE).append(filter).append("=\"").append(filter).append("\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					ManifestElement[] withType = ManifestElement.parseHeader(genericAliases[i][1], strBuf.toString());
					aliasList.add(withType[0]);
				}
			}
		}
		if (aliasList == null || aliasList.size() == 0)
			return genericRequires;
		if (genericRequires != null)
			for (int i = 0; i < genericRequires.length; i++)
				aliasList.add(genericRequires[i]);
		return aliasList.toArray(new ManifestElement[aliasList.size()]);
	}

	private static ManifestElement[] getGenericCapabilities(Dictionary<String, String> manifest, String[][] genericAliases) throws BundleException {
		ManifestElement[] genericCapabilities = ManifestElement.parseHeader(GENERIC_CAPABILITY, manifest.get(GENERIC_CAPABILITY));
		List<ManifestElement> aliasList = null;
		if (genericAliases.length > 0) {
			aliasList = new ArrayList<ManifestElement>(genericCapabilities == null ? 0 : genericCapabilities.length);
			for (int i = 0; i < genericAliases.length; i++) {
				ManifestElement[] aliasCapabilities = ManifestElement.parseHeader(genericAliases[i][0], manifest.get(genericAliases[i][0]));
				if (aliasCapabilities == null)
					continue;
				for (int j = 0; j < aliasCapabilities.length; j++) {
					StringBuffer strBuf = new StringBuffer();
					strBuf.append(aliasCapabilities[j].getValue()).append(':').append(genericAliases[i][2]);
					for (Enumeration<String> keys = aliasCapabilities[j].getKeys(); keys != null && keys.hasMoreElements();) {
						String key = keys.nextElement();
						strBuf.append("; ").append(key).append("=\"").append(aliasCapabilities[j].getAttribute(key)).append("\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					}
					ManifestElement[] withTypes = ManifestElement.parseHeader(genericAliases[i][0], strBuf.toString());
					aliasList.add(withTypes[0]);
				}
			}
		}
		if (aliasList == null || aliasList.size() == 0)
			return genericCapabilities;
		if (genericCapabilities != null)
			for (int i = 0; i < genericCapabilities.length; i++)
				aliasList.add(genericCapabilities[i]);
		return aliasList.toArray(new ManifestElement[aliasList.size()]);
	}

	private static String[][] getGenericAliases(StateImpl state) {
		String genericAliasesProp = getPlatformProperty(state, "osgi.genericAliases"); //$NON-NLS-1$
		if (genericAliasesProp == null)
			return new String[0][0];
		String[] aliases = ManifestElement.getArrayFromList(genericAliasesProp, ","); //$NON-NLS-1$
		String[][] result = new String[aliases.length][];
		for (int i = 0; i < aliases.length; i++)
			result[i] = ManifestElement.getArrayFromList(aliases[i], ":"); //$NON-NLS-1$
		return result;
	}

	private static String getPlatformProperty(State state, String key) {
		Dictionary<Object, Object>[] platformProps = state == null ? null : state.getPlatformProperties();
		return platformProps == null || platformProps.length == 0 ? null : (String) platformProps[0].get(key);
	}

	private static void validateHeaders(Dictionary<String, String> manifest, boolean jreBundle) throws BundleException {
		for (int i = 0; i < DEFINED_OSGI_VALIDATE_HEADERS.length; i++) {
			String header = manifest.get(DEFINED_OSGI_VALIDATE_HEADERS[i]);
			if (header != null) {
				ManifestElement[] elements = ManifestElement.parseHeader(DEFINED_OSGI_VALIDATE_HEADERS[i], header);
				checkForDuplicateDirectivesAttributes(DEFINED_OSGI_VALIDATE_HEADERS[i], elements);
				if (DEFINED_OSGI_VALIDATE_HEADERS[i] == Constants.IMPORT_PACKAGE)
					checkImportExportSyntax(DEFINED_OSGI_VALIDATE_HEADERS[i], elements, false, false, jreBundle);
				if (DEFINED_OSGI_VALIDATE_HEADERS[i] == Constants.DYNAMICIMPORT_PACKAGE)
					checkImportExportSyntax(DEFINED_OSGI_VALIDATE_HEADERS[i], elements, false, true, jreBundle);
				if (DEFINED_OSGI_VALIDATE_HEADERS[i] == Constants.EXPORT_PACKAGE)
					checkImportExportSyntax(DEFINED_OSGI_VALIDATE_HEADERS[i], elements, true, false, jreBundle);
				if (DEFINED_OSGI_VALIDATE_HEADERS[i] == Constants.FRAGMENT_HOST)
					checkExtensionBundle(DEFINED_OSGI_VALIDATE_HEADERS[i], elements);
			} else if (DEFINED_OSGI_VALIDATE_HEADERS[i] == Constants.BUNDLE_SYMBOLICNAME) {
				throw new BundleException(NLS.bind(StateMsg.HEADER_REQUIRED, Constants.BUNDLE_SYMBOLICNAME), BundleException.MANIFEST_ERROR);
			}
		}
	}

	private static BundleSpecification[] createRequiredBundles(ManifestElement[] specs) {
		if (specs == null)
			return null;
		BundleSpecification[] result = new BundleSpecification[specs.length];
		for (int i = 0; i < specs.length; i++)
			result[i] = createRequiredBundle(specs[i]);
		return result;
	}

	static BundleSpecification createRequiredBundle(ManifestElement spec) {
		BundleSpecificationImpl result = new BundleSpecificationImpl();
		result.setName(spec.getValue());
		result.setVersionRange(getVersionRange(spec.getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE)));
		result.setExported(Constants.VISIBILITY_REEXPORT.equals(spec.getDirective(Constants.VISIBILITY_DIRECTIVE)) || "true".equals(spec.getAttribute(Constants.REPROVIDE_ATTRIBUTE))); //$NON-NLS-1$
		result.setOptional(Constants.RESOLUTION_OPTIONAL.equals(spec.getDirective(Constants.RESOLUTION_DIRECTIVE)) || "true".equals(spec.getAttribute(Constants.OPTIONAL_ATTRIBUTE))); //$NON-NLS-1$
		result.setAttributes(getAttributes(spec, DEFINED_BSN_MATCHING_ATTRS));
		result.setArbitraryDirectives(getDirectives(spec, DEFINED_REQUIRE_BUNDLE_DIRECTIVES));
		return result;
	}

	private static ImportPackageSpecification[] createImportPackages(ExportPackageDescription[] exported, List<String> providedExports, ManifestElement[] imported, ManifestElement[] dynamicImported, int manifestVersion) {
		List<ImportPackageSpecification> allImports = null;
		if (manifestVersion < 2) {
			// add implicit imports for each exported package if manifest verions is less than 2.
			if (exported.length == 0 && imported == null && dynamicImported == null)
				return null;
			allImports = new ArrayList<ImportPackageSpecification>(exported.length + (imported == null ? 0 : imported.length));
			for (int i = 0; i < exported.length; i++) {
				if (providedExports.contains(exported[i].getName()))
					continue;
				ImportPackageSpecificationImpl result = new ImportPackageSpecificationImpl();
				result.setName(exported[i].getName());
				result.setVersionRange(getVersionRange(exported[i].getVersion().toString()));
				result.setDirective(Constants.RESOLUTION_DIRECTIVE, ImportPackageSpecification.RESOLUTION_STATIC);
				allImports.add(result);
			}
		} else {
			allImports = new ArrayList<ImportPackageSpecification>(imported == null ? 0 : imported.length);
		}

		// add dynamics first so they will get overriden by static imports if
		// the same package is dyanamically imported and statically imported.
		if (dynamicImported != null)
			for (int i = 0; i < dynamicImported.length; i++)
				addImportPackages(dynamicImported[i], allImports, manifestVersion, true);
		if (imported != null)
			for (int i = 0; i < imported.length; i++)
				addImportPackages(imported[i], allImports, manifestVersion, false);
		return allImports.toArray(new ImportPackageSpecification[allImports.size()]);
	}

	public static void addImportPackages(ManifestElement importPackage, List<ImportPackageSpecification> allImports, int manifestVersion, boolean dynamic) {
		String[] importNames = importPackage.getValueComponents();
		for (int i = 0; i < importNames.length; i++) {
			// do not allow for multiple imports of same package of manifest version < 2
			if (manifestVersion < 2) {
				Iterator<ImportPackageSpecification> iter = allImports.iterator();
				while (iter.hasNext())
					if (importNames[i].equals(iter.next().getName()))
						iter.remove();
			}

			ImportPackageSpecificationImpl result = new ImportPackageSpecificationImpl();
			result.setName(importNames[i]);
			// set common attributes for both dynamic and static imports
			String versionString = importPackage.getAttribute(Constants.VERSION_ATTRIBUTE);
			if (versionString == null) // specification-version aliases to version
				versionString = importPackage.getAttribute(Constants.PACKAGE_SPECIFICATION_VERSION);
			result.setVersionRange(getVersionRange(versionString));
			result.setBundleSymbolicName(importPackage.getAttribute(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE));
			result.setBundleVersionRange(getVersionRange(importPackage.getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE)));
			// only set the matching attributes if manifest version >= 2
			if (manifestVersion >= 2)
				result.setAttributes(getAttributes(importPackage, DEFINED_PACKAGE_MATCHING_ATTRS));

			if (dynamic)
				result.setDirective(Constants.RESOLUTION_DIRECTIVE, ImportPackageSpecification.RESOLUTION_DYNAMIC);
			else
				result.setDirective(Constants.RESOLUTION_DIRECTIVE, getResolution(importPackage.getDirective(Constants.RESOLUTION_DIRECTIVE)));
			result.setArbitraryDirectives(getDirectives(importPackage, DEFINED_IMPORT_PACKAGE_DIRECTIVES));

			allImports.add(result);
		}
	}

	private static String getResolution(String resolution) {
		String result = ImportPackageSpecification.RESOLUTION_STATIC;
		if (Constants.RESOLUTION_OPTIONAL.equals(resolution) || ImportPackageSpecification.RESOLUTION_DYNAMIC.equals(resolution))
			result = resolution;
		return result;
	}

	static ExportPackageDescription[] createExportPackages(ManifestElement[] exported, ManifestElement[] provides, List<String> providedExports, boolean strict) {
		int numExports = (exported == null ? 0 : exported.length) + (provides == null ? 0 : provides.length);
		if (numExports == 0)
			return null;
		List<ExportPackageDescription> allExports = new ArrayList<ExportPackageDescription>(numExports);
		if (exported != null)
			for (int i = 0; i < exported.length; i++)
				addExportPackages(exported[i], allExports, strict);
		if (provides != null)
			addProvidePackages(provides, allExports, providedExports);
		return allExports.toArray(new ExportPackageDescription[allExports.size()]);
	}

	static void addExportPackages(ManifestElement exportPackage, List<ExportPackageDescription> allExports, boolean strict) {
		String[] exportNames = exportPackage.getValueComponents();
		for (int i = 0; i < exportNames.length; i++) {
			// if we are in strict mode and the package is marked as internal, skip it.
			if (strict && "true".equals(exportPackage.getDirective(Constants.INTERNAL_DIRECTIVE))) //$NON-NLS-1$
				continue;
			ExportPackageDescriptionImpl result = new ExportPackageDescriptionImpl();
			result.setName(exportNames[i]);
			String versionString = exportPackage.getAttribute(Constants.VERSION_ATTRIBUTE);
			if (versionString == null) // specification-version aliases to version
				versionString = exportPackage.getAttribute(Constants.PACKAGE_SPECIFICATION_VERSION);
			if (versionString != null)
				result.setVersion(Version.parseVersion(versionString));
			result.setDirective(Constants.USES_DIRECTIVE, ManifestElement.getArrayFromList(exportPackage.getDirective(Constants.USES_DIRECTIVE)));
			result.setDirective(Constants.INCLUDE_DIRECTIVE, exportPackage.getDirective(Constants.INCLUDE_DIRECTIVE));
			result.setDirective(Constants.EXCLUDE_DIRECTIVE, exportPackage.getDirective(Constants.EXCLUDE_DIRECTIVE));
			result.setDirective(Constants.FRIENDS_DIRECTIVE, ManifestElement.getArrayFromList(exportPackage.getDirective(Constants.FRIENDS_DIRECTIVE)));
			result.setDirective(Constants.INTERNAL_DIRECTIVE, Boolean.valueOf(exportPackage.getDirective(Constants.INTERNAL_DIRECTIVE)));
			result.setDirective(Constants.MANDATORY_DIRECTIVE, ManifestElement.getArrayFromList(exportPackage.getDirective(Constants.MANDATORY_DIRECTIVE)));
			result.setAttributes(getAttributes(exportPackage, DEFINED_PACKAGE_MATCHING_ATTRS));
			result.setArbitraryDirectives(getDirectives(exportPackage, DEFINED_EXPORT_PACKAGE_DIRECTIVES));
			allExports.add(result);
		}
	}

	private static void addProvidePackages(ManifestElement[] provides, List<ExportPackageDescription> allExports, List<String> providedExports) {
		ExportPackageDescription[] currentExports = allExports.toArray(new ExportPackageDescription[allExports.size()]);
		for (int i = 0; i < provides.length; i++) {
			boolean duplicate = false;
			for (int j = 0; j < currentExports.length; j++)
				if (provides[i].getValue().equals(currentExports[j].getName())) {
					duplicate = true;
					break;
				}
			if (!duplicate) {
				ExportPackageDescriptionImpl result = new ExportPackageDescriptionImpl();
				result.setName(provides[i].getValue());
				allExports.add(result);
			}
			providedExports.add(provides[i].getValue());
		}
	}

	static Map<String, String> getDirectives(ManifestElement element, String[] definedDirectives) {
		Enumeration<String> keys = element.getDirectiveKeys();
		if (keys == null)
			return null;
		Map<String, String> arbitraryDirectives = null;
		keyloop: while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			for (String definedDirective : definedDirectives) {
				if (definedDirective.equals(key))
					continue keyloop;
			}
			if (arbitraryDirectives == null)
				arbitraryDirectives = new HashMap<String, String>();
			arbitraryDirectives.put(key, element.getDirective(key));
		}
		return arbitraryDirectives;
	}

	static Map<String, Object> getAttributes(ManifestElement element, String[] definedAttrs) {
		Enumeration<String> keys = element.getKeys();
		Map<String, Object> arbitraryAttrs = null;
		if (keys == null)
			return null;
		while (keys.hasMoreElements()) {
			boolean definedAttr = false;
			String key = keys.nextElement();
			for (int i = 0; i < definedAttrs.length; i++) {
				if (definedAttrs[i].equals(key)) {
					definedAttr = true;
					break;
				}
			}
			String value = element.getAttribute(key);
			int colonIndex = key.indexOf(':');
			String type = ATTR_TYPE_STRING;
			if (colonIndex > 0) {
				type = key.substring(colonIndex + 1).trim();
				key = key.substring(0, colonIndex).trim();
			}
			if (!definedAttr) {
				if (arbitraryAttrs == null)
					arbitraryAttrs = new HashMap<String, Object>();
				arbitraryAttrs.put(key, convertValue(type, value));
			}
		}
		return arbitraryAttrs;
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
				Class<?> uriClazz = Class.forName("java.net.URI"); //$NON-NLS-1$
				Constructor<?> constructor = uriClazz.getConstructor(new Class[] {String.class});
				return constructor.newInstance(new Object[] {trimmed});
			} catch (ClassNotFoundException e) {
				// oh well cannot support; just use string
				return value;
			} catch (RuntimeException e) { // got some reflection exception
				throw e;
			} catch (Exception e) {
				throw new RuntimeException(e.getMessage(), e);
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

	static HostSpecification createHostSpecification(ManifestElement spec, State state) {
		if (spec == null)
			return null;
		HostSpecificationImpl result = new HostSpecificationImpl();
		result.setName(spec.getValue());
		result.setVersionRange(getVersionRange(spec.getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE)));
		String multiple = spec.getDirective("multiple-hosts"); //$NON-NLS-1$
		if (multiple == null)
			multiple = getPlatformProperty(state, "osgi.support.multipleHosts"); //$NON-NLS-1$
		result.setIsMultiHost("true".equals(multiple)); //$NON-NLS-1$
		result.setAttributes(getAttributes(spec, DEFINED_BSN_MATCHING_ATTRS));
		result.setArbitraryDirectives(getDirectives(spec, DEFINED_FRAGMENT_HOST_DIRECTIVES));
		return result;
	}

	private static GenericSpecification[] createGenericRequires(ManifestElement[] equinoxRequires, ManifestElement[] osgiRequires, String[] brees) throws BundleException {
		List<GenericSpecification> result = createEquinoxRequires(equinoxRequires);
		result = createOSGiRequires(osgiRequires, result);
		result = convertBREEs(brees, result);
		return result == null ? null : result.toArray(new GenericSpecification[result.size()]);
	}

	static List<GenericSpecification> convertBREEs(String[] brees, List<GenericSpecification> result) throws BundleException {
		if (brees == null || brees.length == 0)
			return result;
		if (result == null)
			result = new ArrayList<GenericSpecification>(brees.length);
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
		GenericSpecificationImpl spec = new GenericSpecificationImpl();
		spec.setResolution(GenericSpecificationImpl.RESOLUTION_FROM_BREE);
		spec.setType(StateImpl.OSGI_EE_NAMESPACE);
		try {
			FilterImpl filter = FilterImpl.newInstance(filterSpec);
			spec.setMatchingFilter(filter);
			String name = filter.getPrimaryKeyValue(spec.getType());
			if (name != null)
				spec.setName(name);
		} catch (InvalidSyntaxException e) {
			throw new BundleException("Error converting required execution environment.", e); //$NON-NLS-1$
		}
		result.add(spec);
		return result;
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

	static List<GenericSpecification> createOSGiRequires(ManifestElement[] osgiRequires, List<GenericSpecification> result) throws BundleException {
		if (osgiRequires == null)
			return result;
		if (result == null)
			result = new ArrayList<GenericSpecification>();
		for (ManifestElement element : osgiRequires) {
			String[] namespaces = element.getValueComponents();
			for (String namespace : namespaces) {
				GenericSpecificationImpl spec = new GenericSpecificationImpl();
				spec.setType(namespace);
				String filterSpec = element.getDirective(Constants.FILTER_DIRECTIVE);
				if (filterSpec != null) {
					try {
						FilterImpl filter = FilterImpl.newInstance(filterSpec);
						spec.setMatchingFilter(filter);
						String name = filter.getPrimaryKeyValue(namespace);
						if (name != null)
							spec.setName(name);
					} catch (InvalidSyntaxException e) {
						String message = NLS.bind(Msg.MANIFEST_INVALID_HEADER_EXCEPTION, Constants.REQUIRE_CAPABILITY, element.toString());
						throw new BundleException(message + " : filter", BundleException.MANIFEST_ERROR, e); //$NON-NLS-1$
					}
				}
				String resolutionDirective = element.getDirective(Constants.RESOLUTION_DIRECTIVE);
				int resolution = 0;
				if (Constants.RESOLUTION_OPTIONAL.equals(resolutionDirective))
					resolution |= GenericSpecification.RESOLUTION_OPTIONAL;
				String cardinality = element.getDirective(Namespace.REQUIREMENT_CARDINALITY_DIRECTIVE);
				if (Namespace.CARDINALITY_MULTIPLE.equals(cardinality))
					resolution |= GenericSpecification.RESOLUTION_MULTIPLE;
				spec.setResolution(resolution);
				spec.setAttributes(getAttributes(element, DEFINED_REQUIRE_CAPABILITY_ATTRS));
				spec.setArbitraryDirectives(getDirectives(element, DEFINED_REQUIRE_CAPABILITY_DIRECTIVES));
				result.add(spec);
			}
		}
		return result;
	}

	private static List<GenericSpecification> createEquinoxRequires(ManifestElement[] equinoxRequires) throws BundleException {
		if (equinoxRequires == null)
			return null;
		ArrayList<GenericSpecification> results = new ArrayList<GenericSpecification>(equinoxRequires.length);
		for (int i = 0; i < equinoxRequires.length; i++) {
			String[] genericNames = equinoxRequires[i].getValueComponents();
			for (int j = 0; j < genericNames.length; j++) {
				GenericSpecificationImpl spec = new GenericSpecificationImpl();
				int colonIdx = genericNames[j].indexOf(':');
				if (colonIdx > 0) {
					spec.setName(genericNames[j].substring(0, colonIdx));
					spec.setType(genericNames[j].substring(colonIdx + 1));
				} else
					spec.setName(genericNames[j]);
				try {
					spec.setMatchingFilter(equinoxRequires[i].getAttribute(Constants.SELECTION_FILTER_ATTRIBUTE), true);
				} catch (InvalidSyntaxException e) {
					String message = NLS.bind(Msg.MANIFEST_INVALID_HEADER_EXCEPTION, GENERIC_REQUIRE, equinoxRequires[i].toString());
					throw new BundleException(message + " : " + Constants.SELECTION_FILTER_ATTRIBUTE, BundleException.MANIFEST_ERROR, e); //$NON-NLS-1$
				}
				String optional = equinoxRequires[i].getAttribute(OPTIONAL_ATTR);
				String multiple = equinoxRequires[i].getAttribute(MULTIPLE_ATTR);
				int resolution = 0;
				if (TRUE.equals(optional))
					resolution |= GenericSpecification.RESOLUTION_OPTIONAL;
				if (TRUE.equals(multiple))
					resolution |= GenericSpecification.RESOLUTION_MULTIPLE;
				spec.setResolution(resolution);
				results.add(spec);
			}
		}
		return results;
	}

	private static GenericDescription[] createGenericCapabilities(ManifestElement[] equinoxCapabilities, ManifestElement[] osgiCapabilities, BundleDescription description) throws BundleException {
		List<GenericDescription> result = createEquinoxCapabilities(equinoxCapabilities);
		result = createOSGiCapabilities(osgiCapabilities, result, description);
		return result == null ? null : result.toArray(new GenericDescription[result.size()]);
	}

	static List<GenericDescription> createOSGiCapabilities(ManifestElement[] osgiCapabilities, List<GenericDescription> result, BundleDescription description) throws BundleException {
		if (result == null)
			result = new ArrayList<GenericDescription>(osgiCapabilities == null ? 1 : osgiCapabilities.length + 1);
		// Always have an osgi.identity capability if there is a symbolic name.
		GenericDescription osgiIdentity = createOsgiIdentityCapability(description);
		if (osgiIdentity != null)
			// always add the capability to the front
			result.add(0, osgiIdentity);
		return createOSGiCapabilities(osgiCapabilities, result, (Integer) null);
	}

	static List<GenericDescription> createOSGiCapabilities(ManifestElement[] osgiCapabilities, List<GenericDescription> result, Integer profileIndex) throws BundleException {
		if (osgiCapabilities == null)
			return result;
		if (result == null)
			result = new ArrayList<GenericDescription>(osgiCapabilities.length);

		for (ManifestElement element : osgiCapabilities) {
			String[] namespaces = element.getValueComponents();
			for (String namespace : namespaces) {
				if (IdentityNamespace.IDENTITY_NAMESPACE.equals(namespace))
					throw new BundleException("A bundle is not allowed to define a capability in the " + IdentityNamespace.IDENTITY_NAMESPACE + " name space."); //$NON-NLS-1$ //$NON-NLS-2$

				GenericDescriptionImpl desc = new GenericDescriptionImpl();
				desc.setType(namespace);
				Map<String, Object> mapAttrs = getAttributes(element, new String[0]);
				if (profileIndex != null)
					mapAttrs.put(ExportPackageDescriptionImpl.EQUINOX_EE, profileIndex);
				Dictionary<String, Object> attrs = mapAttrs == null ? new Hashtable<String, Object>() : new Hashtable<String, Object>(mapAttrs);
				desc.setAttributes(attrs);
				Map<String, String> directives = new HashMap<String, String>();
				Enumeration<String> keys = element.getDirectiveKeys();
				if (keys != null)
					for (keys = element.getDirectiveKeys(); keys.hasMoreElements();) {
						String key = keys.nextElement();
						directives.put(key, element.getDirective(key));
					}
				desc.setDirectives(directives);
				result.add(desc);
			}
		}
		return result;
	}

	private static List<GenericDescription> createEquinoxCapabilities(ManifestElement[] equinoxCapabilities) throws BundleException {
		if (equinoxCapabilities == null)
			return null;
		ArrayList<GenericDescription> results = new ArrayList<GenericDescription>(equinoxCapabilities.length);
		for (int i = 0; i < equinoxCapabilities.length; i++) {
			String[] genericNames = equinoxCapabilities[i].getValueComponents();
			for (int j = 0; j < genericNames.length; j++) {
				GenericDescriptionImpl desc = new GenericDescriptionImpl();
				String name = genericNames[j];
				int colonIdx = genericNames[j].indexOf(':');
				if (colonIdx > 0) {
					name = genericNames[j].substring(0, colonIdx);
					desc.setType(genericNames[j].substring(colonIdx + 1));
					if (IdentityNamespace.IDENTITY_NAMESPACE.equals(desc.getType()))
						throw new BundleException("A bundle is not allowed to define a capability in the " + IdentityNamespace.IDENTITY_NAMESPACE + " name space."); //$NON-NLS-1$ //$NON-NLS-2$
				}
				Map<String, Object> mapAttrs = getAttributes(equinoxCapabilities[i], new String[] {Constants.VERSION_ATTRIBUTE});
				Dictionary<String, Object> attrs = mapAttrs == null ? new Hashtable<String, Object>() : new Hashtable<String, Object>(mapAttrs);
				attrs.put(desc.getType(), name);
				String versionString = equinoxCapabilities[i].getAttribute(Constants.VERSION_ATTRIBUTE);
				if (versionString != null)
					attrs.put(Constants.VERSION_ATTRIBUTE, Version.parseVersion(versionString));
				desc.setAttributes(attrs);
				results.add(desc);
			}
		}
		return results;
	}

	private static NativeCodeSpecification createNativeCode(ManifestElement[] nativeCode) throws BundleException {
		if (nativeCode == null)
			return null;
		NativeCodeSpecificationImpl result = new NativeCodeSpecificationImpl();
		result.setName(Constants.BUNDLE_NATIVECODE);
		int length = nativeCode.length;
		if (length > 0 && nativeCode[length - 1].getValue().equals("*")) { //$NON-NLS-1$
			result.setOptional(true);
			length--;
		}
		NativeCodeDescriptionImpl[] suppliers = new NativeCodeDescriptionImpl[length];
		for (int i = 0; i < length; i++) {
			suppliers[i] = createNativeCodeDescription(nativeCode[i]);
		}
		result.setPossibleSuppliers(suppliers);
		return result;
	}

	private static NativeCodeDescriptionImpl createNativeCodeDescription(ManifestElement manifestElement) throws BundleException {
		NativeCodeDescriptionImpl result = new NativeCodeDescriptionImpl();
		result.setName(Constants.BUNDLE_NATIVECODE);
		result.setNativePaths(manifestElement.getValueComponents());
		result.setOSNames(manifestElement.getAttributes(Constants.BUNDLE_NATIVECODE_OSNAME));
		result.setProcessors(manifestElement.getAttributes(Constants.BUNDLE_NATIVECODE_PROCESSOR));
		result.setOSVersions(createVersionRanges(manifestElement.getAttributes(Constants.BUNDLE_NATIVECODE_OSVERSION)));
		result.setLanguages(manifestElement.getAttributes(Constants.BUNDLE_NATIVECODE_LANGUAGE));
		try {
			result.setFilter(manifestElement.getAttribute(Constants.SELECTION_FILTER_ATTRIBUTE));
		} catch (InvalidSyntaxException e) {
			String message = NLS.bind(Msg.MANIFEST_INVALID_HEADER_EXCEPTION, Constants.BUNDLE_NATIVECODE, manifestElement.toString());
			throw new BundleException(message + " : " + Constants.SELECTION_FILTER_ATTRIBUTE, BundleException.MANIFEST_ERROR, e); //$NON-NLS-1$
		}
		return result;
	}

	private static VersionRange[] createVersionRanges(String[] ranges) {
		if (ranges == null)
			return null;
		VersionRange[] result = new VersionRange[ranges.length];
		for (int i = 0; i < result.length; i++)
			result[i] = new VersionRange(ranges[i]);
		return result;
	}

	private static VersionRange getVersionRange(String versionRange) {
		if (versionRange == null)
			return null;
		return new VersionRange(versionRange);
	}

	public static void checkImportExportSyntax(String headerKey, ManifestElement[] elements, boolean export, boolean dynamic, boolean jreBundle) throws BundleException {
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
					throw new BundleException(message + " : " + NLS.bind(StateMsg.HEADER_PACKAGE_DUPLICATES, packageNames[j]), BundleException.MANIFEST_ERROR); //$NON-NLS-1$
				}
				// check for java.*
				if (!jreBundle && packageNames[j].startsWith("java.")) { //$NON-NLS-1$
					String message = NLS.bind(Msg.MANIFEST_INVALID_HEADER_EXCEPTION, headerKey, elements[i].toString());
					throw new BundleException(message + " : " + NLS.bind(StateMsg.HEADER_PACKAGE_JAVA, packageNames[j]), BundleException.MANIFEST_ERROR); //$NON-NLS-1$
				}
				packages.add(packageNames[j]);
			}
			// check for version/specification version mismatch
			String version = elements[i].getAttribute(Constants.VERSION_ATTRIBUTE);
			if (version != null) {
				String specVersion = elements[i].getAttribute(Constants.PACKAGE_SPECIFICATION_VERSION);
				if (specVersion != null && !specVersion.equals(version))
					throw new BundleException(NLS.bind(StateMsg.HEADER_VERSION_ERROR, Constants.VERSION_ATTRIBUTE, Constants.PACKAGE_SPECIFICATION_VERSION), BundleException.MANIFEST_ERROR);
			}
			// check for bundle-symbolic-name and bundle-verion attibures
			// (failure)
			if (export) {
				if (elements[i].getAttribute(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE) != null) {
					String message = NLS.bind(Msg.MANIFEST_INVALID_HEADER_EXCEPTION, headerKey, elements[i].toString());
					throw new BundleException(message + " : " + NLS.bind(StateMsg.HEADER_EXPORT_ATTR_ERROR, Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE, Constants.EXPORT_PACKAGE), BundleException.MANIFEST_ERROR); //$NON-NLS-1$
				}
				if (elements[i].getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE) != null) {
					String message = NLS.bind(Msg.MANIFEST_INVALID_HEADER_EXCEPTION, headerKey, elements[i].toString());
					throw new BundleException(NLS.bind(message + " : " + StateMsg.HEADER_EXPORT_ATTR_ERROR, Constants.BUNDLE_VERSION_ATTRIBUTE, Constants.EXPORT_PACKAGE), BundleException.MANIFEST_ERROR); //$NON-NLS-1$
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
						throw new BundleException(NLS.bind(message + " : " + StateMsg.HEADER_DIRECTIVE_DUPLICATES, key), BundleException.MANIFEST_ERROR); //$NON-NLS-1$
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
						throw new BundleException(message + " : " + NLS.bind(StateMsg.HEADER_ATTRIBUTE_DUPLICATES, key), BundleException.MANIFEST_ERROR); //$NON-NLS-1$
					}
				}
			}
		}
	}

	private static void checkExtensionBundle(String headerKey, ManifestElement[] elements) throws BundleException {
		if (elements.length == 0 || elements[0].getDirective(Constants.EXTENSION_DIRECTIVE) == null)
			return;
		String hostName = elements[0].getValue();
		// XXX: The extension bundle check is done against system.bundle and org.eclipse.osgi
		if (!hostName.equals(Constants.SYSTEM_BUNDLE_SYMBOLICNAME) && !hostName.equals(Constants.getInternalSymbolicName())) {
			String message = NLS.bind(Msg.MANIFEST_INVALID_HEADER_EXCEPTION, headerKey, elements[0].toString());
			throw new BundleException(message + " : " + NLS.bind(StateMsg.HEADER_EXTENSION_ERROR, hostName), BundleException.MANIFEST_ERROR); //$NON-NLS-1$
		}
	}

	static GenericDescription createOsgiIdentityCapability(BundleDescription description) {
		if (description.getSymbolicName() == null)
			return null;
		GenericDescriptionImpl result = new GenericDescriptionImpl();
		result.setType(IdentityNamespace.IDENTITY_NAMESPACE);
		Dictionary<String, Object> attributes = new Hashtable<String, Object>(description.getDeclaredAttributes());
		// remove osgi.wiring.bundle and bundle-version attributes
		attributes.remove(BundleNamespace.BUNDLE_NAMESPACE);
		attributes.remove(Constants.BUNDLE_VERSION_ATTRIBUTE);
		attributes.put(IdentityNamespace.IDENTITY_NAMESPACE, description.getSymbolicName());
		attributes.put(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, description.getHost() == null ? IdentityNamespace.TYPE_BUNDLE : IdentityNamespace.TYPE_FRAGMENT);
		attributes.put(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, description.getVersion());
		result.setAttributes(attributes);
		Map<String, String> directives = new HashMap<String, String>(description.getDeclaredDirectives());
		// remove defaults directive values
		if (!description.isSingleton())
			directives.remove(Constants.SINGLETON_DIRECTIVE);
		if (description.attachFragments() && description.dynamicFragments())
			directives.remove(Constants.FRAGMENT_ATTACHMENT_DIRECTIVE);
		result.setDirectives(directives);
		return result;
	}
}
