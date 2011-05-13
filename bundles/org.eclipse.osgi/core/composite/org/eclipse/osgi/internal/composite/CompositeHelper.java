/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.composite;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.jar.*;
import org.eclipse.osgi.internal.baseadaptor.BaseStorageHook;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.*;
import org.osgi.service.permissionadmin.PermissionAdmin;
import org.osgi.service.permissionadmin.PermissionInfo;

public class CompositeHelper {
	private static final PermissionInfo[] COMPOSITE_PERMISSIONS = new PermissionInfo[] {new PermissionInfo(PackagePermission.class.getName(), "*", PackagePermission.EXPORT), new PermissionInfo(ServicePermission.class.getName(), "*", ServicePermission.REGISTER + ',' + ServicePermission.GET)}; //$NON-NLS-1$ //$NON-NLS-2$
	private static final String COMPOSITE_POLICY = "org.eclipse.osgi.composite"; //$NON-NLS-1$
	private static String ELEMENT_SEPARATOR = "; "; //$NON-NLS-1$
	private static final Object EQUALS_QUOTE = "=\""; //$NON-NLS-1$
	private static final String[] INVALID_COMPOSITE_HEADERS = new String[] {Constants.DYNAMICIMPORT_PACKAGE, Constants.FRAGMENT_HOST, Constants.REQUIRE_BUNDLE, Constants.BUNDLE_NATIVECODE, Constants.BUNDLE_CLASSPATH, Constants.BUNDLE_ACTIVATOR, Constants.BUNDLE_LOCALIZATION, Constants.BUNDLE_ACTIVATIONPOLICY};

	private static Manifest getCompositeManifest(Map compositeManifest) {
		Manifest manifest = new Manifest();
		Attributes attributes = manifest.getMainAttributes();
		attributes.putValue("Manifest-Version", "1.0"); //$NON-NLS-1$//$NON-NLS-2$
		// get the common headers Bundle-ManifestVersion, Bundle-SymbolicName and Bundle-Version
		// get the manifest version from the map
		String manifestVersion = (String) compositeManifest.remove(Constants.BUNDLE_MANIFESTVERSION);
		// here we assume the validation got the correct version for us
		attributes.putValue(Constants.BUNDLE_MANIFESTVERSION, manifestVersion);
		// Ignore the Equinox composite bundle header
		compositeManifest.remove(BaseStorageHook.COMPOSITE_HEADER);
		attributes.putValue(BaseStorageHook.COMPOSITE_HEADER, BaseStorageHook.COMPOSITE_BUNDLE);
		for (Iterator entries = compositeManifest.entrySet().iterator(); entries.hasNext();) {
			Map.Entry entry = (Entry) entries.next();
			if (entry.getKey() instanceof String && entry.getValue() instanceof String)
				attributes.putValue((String) entry.getKey(), (String) entry.getValue());
		}
		return manifest;
	}

	private static Manifest getSurrogateManifest(Dictionary compositeManifest, BundleDescription compositeDesc, ExportPackageDescription[] matchingExports) {
		Manifest manifest = new Manifest();
		Attributes attributes = manifest.getMainAttributes();
		attributes.putValue("Manifest-Version", "1.0"); //$NON-NLS-1$//$NON-NLS-2$
		// Ignore the manifest version from the map
		// always use bundle manifest version 2
		attributes.putValue(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		// Ignore the Equinox composite bundle header
		attributes.putValue(BaseStorageHook.COMPOSITE_HEADER, BaseStorageHook.SURROGATE_BUNDLE);

		if (compositeDesc != null && matchingExports != null) {
			// convert the exports from the composite into imports
			addImports(attributes, compositeDesc, matchingExports);

			// convert the matchingExports from the composite into exports
			addExports(attributes, compositeDesc, matchingExports);
		}

		// add the rest
		for (Enumeration keys = compositeManifest.keys(); keys.hasMoreElements();) {
			Object header = keys.nextElement();
			if (Constants.BUNDLE_MANIFESTVERSION.equals(header) || BaseStorageHook.COMPOSITE_HEADER.equals(header) || Constants.IMPORT_PACKAGE.equals(header) || Constants.EXPORT_PACKAGE.equals(header))
				continue;
			if (header instanceof String && compositeManifest.get(header) instanceof String)
				attributes.putValue((String) header, (String) compositeManifest.get(header));
		}
		return manifest;
	}

	static InputStream getCompositeInput(Map frameworkConfig, Map compositeManifest) throws IOException {
		// use an in memory stream to store the content
		ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
		// the composite bundles only consist of a manifest describing the packages they import and export
		// and a framework config properties file
		Manifest manifest = CompositeHelper.getCompositeManifest(compositeManifest);
		JarOutputStream jarOut = new JarOutputStream(bytesOut, manifest);
		try {
			// store the framework config
			Properties fwProps = new Properties();
			if (frameworkConfig != null)
				fwProps.putAll(frameworkConfig);
			JarEntry entry = new JarEntry(CompositeImpl.COMPOSITE_CONFIGURATION);
			jarOut.putNextEntry(entry);
			fwProps.store(jarOut, null);
			jarOut.closeEntry();
			jarOut.flush();
		} finally {
			try {
				jarOut.close();
			} catch (IOException e) {
				// nothing
			}
		}
		return new ByteArrayInputStream(bytesOut.toByteArray());
	}

	static InputStream getSurrogateInput(Dictionary compositeManifest, BundleDescription compositeDesc, ExportPackageDescription[] matchingExports) throws IOException {
		// use an in memory stream to store the content
		ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
		Manifest manifest = CompositeHelper.getSurrogateManifest(compositeManifest, compositeDesc, matchingExports);
		JarOutputStream jarOut = new JarOutputStream(bytesOut, manifest);
		jarOut.flush();
		jarOut.close();
		return new ByteArrayInputStream(bytesOut.toByteArray());
	}

	private static void addImports(Attributes attrigutes, BundleDescription compositeDesc, ExportPackageDescription[] matchingExports) {
		ExportPackageDescription[] exports = compositeDesc.getExportPackages();
		List systemExports = getSystemExports(matchingExports);
		if (exports.length == 0 && systemExports.size() == 0)
			return;
		StringBuffer importStatement = new StringBuffer();
		Collection importedNames = new ArrayList(exports.length);
		int i = 0;
		for (; i < exports.length; i++) {
			if (i != 0)
				importStatement.append(',');
			importedNames.add(exports[i].getName());
			getImportFrom(exports[i], importStatement);
		}
		for (Iterator iSystemExports = systemExports.iterator(); iSystemExports.hasNext();) {
			ExportPackageDescription systemExport = (ExportPackageDescription) iSystemExports.next();
			if (!importedNames.contains(systemExport.getName())) {
				if (i != 0)
					importStatement.append(',');
				i++;
				importStatement.append(systemExport.getName()).append(ELEMENT_SEPARATOR).append(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE).append('=').append(Constants.SYSTEM_BUNDLE_SYMBOLICNAME);
			}
		}
		attrigutes.putValue(Constants.IMPORT_PACKAGE, importStatement.toString());
	}

	private static List getSystemExports(ExportPackageDescription[] matchingExports) {
		ArrayList list = null;
		for (int i = 0; i < matchingExports.length; i++) {
			if (matchingExports[i].getExporter().getBundleId() != 0)
				continue;
			if (list == null)
				list = new ArrayList();
			list.add(matchingExports[i]);
		}
		return list == null ? Collections.EMPTY_LIST : list;
	}

	private static void getImportFrom(ExportPackageDescription export, StringBuffer importStatement) {
		importStatement.append(export.getName()).append(ELEMENT_SEPARATOR);
		Version version = export.getVersion();
		importStatement.append(Constants.VERSION_ATTRIBUTE).append(EQUALS_QUOTE).append('[').append(version).append(',').append(new Version(version.getMajor(), version.getMinor(), version.getMicro() + 1)).append(')').append('\"');
		addMap(importStatement, export.getAttributes(), "="); //$NON-NLS-1$
	}

	private static void addExports(Attributes attributes, BundleDescription compositeDesc, ExportPackageDescription[] matchingExports) {
		if (matchingExports.length == 0)
			return;
		StringBuffer exportStatement = new StringBuffer();
		for (int i = 0; i < matchingExports.length; i++) {
			if (matchingExports[i].getExporter() == compositeDesc) {
				// the matching export from outside is the composite bundle itself
				// this must be one of our own substitutable exports, it must be ignored (bug 345640)
				continue;
			}
			if (exportStatement.length() > 0)
				exportStatement.append(',');
			getExportFrom(matchingExports[i], exportStatement);
		}
		if (exportStatement.length() > 0)
			attributes.putValue(Constants.EXPORT_PACKAGE, exportStatement.toString());
	}

	private static void getExportFrom(ExportPackageDescription export, StringBuffer exportStatement) {
		exportStatement.append(export.getName()).append(ELEMENT_SEPARATOR);
		exportStatement.append(Constants.VERSION_ATTRIBUTE).append(EQUALS_QUOTE).append(export.getVersion()).append('\"');
		addMap(exportStatement, export.getDirectives(), ":="); //$NON-NLS-1$
		addMap(exportStatement, export.getAttributes(), "="); //$NON-NLS-1$
	}

	private static void addMap(StringBuffer manifest, Map values, String assignment) {
		if (values == null)
			return; // nothing to add
		for (Iterator iEntries = values.entrySet().iterator(); iEntries.hasNext();) {
			manifest.append(ELEMENT_SEPARATOR);
			Map.Entry entry = (Entry) iEntries.next();
			manifest.append(entry.getKey()).append(assignment).append('\"');
			Object value = entry.getValue();
			if (value instanceof String[]) {
				String[] strings = (String[]) value;
				for (int i = 0; i < strings.length; i++) {
					if (i != 0)
						manifest.append(',');
					manifest.append(strings[i]);
				}
			} else {
				manifest.append(value);
			}
			manifest.append('\"');
		}
	}

	static void setCompositePermissions(String bundleLocation, BundleContext systemContext) {
		ServiceReference ref = systemContext.getServiceReference(PermissionAdmin.class.getName());
		PermissionAdmin permAdmin = (PermissionAdmin) (ref == null ? null : systemContext.getService(ref));
		if (permAdmin == null)
			throw new RuntimeException("No Permission Admin service is available"); //$NON-NLS-1$
		try {
			permAdmin.setPermissions(bundleLocation, COMPOSITE_PERMISSIONS);
		} finally {
			systemContext.ungetService(ref);
		}
	}

	static void setDisabled(boolean disable, Bundle bundle, BundleContext systemContext) {
		ServiceReference ref = systemContext.getServiceReference(PlatformAdmin.class.getName());
		PlatformAdmin pa = (PlatformAdmin) (ref == null ? null : systemContext.getService(ref));
		if (pa == null)
			throw new RuntimeException("No Platform Admin service is available."); //$NON-NLS-1$
		try {
			State state = pa.getState(false);
			BundleDescription desc = state.getBundle(bundle.getBundleId());
			setDisabled(disable, desc);
		} finally {
			systemContext.ungetService(ref);
		}
	}

	static void setDisabled(boolean disable, BundleDescription bundle) {
		State state = bundle.getContainingState();
		if (disable) {
			state.addDisabledInfo(new DisabledInfo(COMPOSITE_POLICY, "Composite companion bundle is not resolved.", bundle)); //$NON-NLS-1$
		} else {
			DisabledInfo toRemove = state.getDisabledInfo(bundle, COMPOSITE_POLICY);
			if (toRemove != null)
				state.removeDisabledInfo(toRemove);
		}
	}

	static void validateCompositeManifest(Map compositeManifest) throws BundleException {
		if (compositeManifest == null)
			throw new BundleException("The composite manifest cannot be null.", BundleException.MANIFEST_ERROR); //$NON-NLS-1$
		// check for symbolic name
		if (compositeManifest.get(Constants.BUNDLE_SYMBOLICNAME) == null)
			throw new BundleException("The composite manifest must contain a Bundle-SymbolicName header.", BundleException.MANIFEST_ERROR); //$NON-NLS-1$
		// check for invalid manifests headers
		for (int i = 0; i < INVALID_COMPOSITE_HEADERS.length; i++)
			if (compositeManifest.get(INVALID_COMPOSITE_HEADERS[i]) != null)
				throw new BundleException("The composite manifest must not contain the header " + INVALID_COMPOSITE_HEADERS[i], BundleException.MANIFEST_ERROR); //$NON-NLS-1$
		// validate manifest version
		String manifestVersion = (String) compositeManifest.get(Constants.BUNDLE_MANIFESTVERSION);
		if (manifestVersion == null) {
			compositeManifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		} else {
			try {
				Integer parsed = Integer.valueOf(manifestVersion);
				if (parsed.intValue() > 2 || parsed.intValue() < 2)
					throw new BundleException("Invalid Bundle-ManifestVersion: " + manifestVersion); //$NON-NLS-1$
			} catch (NumberFormatException e) {
				throw new BundleException("Invalid Bundle-ManifestVersion: " + manifestVersion); //$NON-NLS-1$
			}
		}
	}
}
