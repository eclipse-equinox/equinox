/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.adaptor.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.Dictionary;
import java.util.Enumeration;
import org.eclipse.osgi.framework.adaptor.*;
import org.eclipse.osgi.framework.debug.Debug;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.framework.internal.protocol.bundleentry.Handler;
import org.eclipse.osgi.framework.util.Headers;
import org.eclipse.osgi.service.resolver.Version;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.BundleException;

//TODO After quickly looking through the code, I don't see why this is not a subclass of AbstractBundleData
public class SystemBundleData implements BundleData {

	public static final String OSGI_FRAMEWORK = "osgi.framework";

	Headers manifest;
	Version version;
	File osgiBase;
	BundleFile bundleFile;

	public SystemBundleData(FrameworkAdaptor adaptor) throws BundleException {
		osgiBase = getOsgiBase();
		this.manifest = createManifest(adaptor);
		String sVersion = (String) manifest.get(Constants.BUNDLE_VERSION);
		if (sVersion != null) {
			version = new Version(sVersion);
		}
		if (osgiBase != null)
			try {
				bundleFile = BundleFile.createBundleFile(osgiBase,this);
			} catch (IOException e) {
				// should not happen
			}
	}

	public BundleClassLoader createClassLoader(ClassLoaderDelegate delegate, ProtectionDomain domain, String[] bundleclasspath) {
		return null;
	}

	public URL getEntry(String path) {
		if (bundleFile == null)
			return null;
		
		BundleEntry entry = bundleFile.getEntry(path);
		if (entry == null) {
			return null;
		}
		try {
			return new URL(null, AbstractBundleData.getBundleEntryURL(getBundleID(),path), new Handler(entry));
		} catch (MalformedURLException e) {
			return null;
		}
	}

	public Enumeration getEntryPaths(String path) {
		if (bundleFile == null)
			return null;
		return bundleFile.getEntryPaths(path);
	}

	public String findLibrary(String libname) {
		return null;
	}

	public void installNativeCode(String[] nativepaths) {
	}

	public File getDataFile(String path) {
		return null;
	}

	public Dictionary getManifest() {
		return manifest;
	}

	public long getBundleID() {
		return 0;
	}

	public String getLocation() {
		return Constants.SYSTEM_BUNDLE_LOCATION;
	}

	public void close() {
	}

	public void open() {
	}

	public boolean isFragment() {
		return false;
	}

	public void setBundle(org.osgi.framework.Bundle bundle) {
		// do nothing.
	}

	// The system bundle does not have any meta data capabilities so the following methods just
	// do nothing or return dummy values.
	public int getStartLevel() {
		return 0;
	}

	public int getStatus() {
		return 0;
	}

	public void setStartLevel(int value) {
	}

	public void setStatus(int value) {
	}

	public void save() throws IOException {
	}

	public String getSymbolicName() {
		//TODO may want to cache
		return parseSymbolicName(manifest);
	}
	/* 
	 * Convenience method that retrieves the simbolic name string from the header.
	 * Note: clients may want to cache the returned value.
	 */
	public static String parseSymbolicName(Dictionary manifest) {
		String symbolicNameEntry = (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME);
		if (symbolicNameEntry == null)
			return null;		
		try {
			return ManifestElement.parseHeader(Constants.BUNDLE_SYMBOLICNAME, symbolicNameEntry)[0].getValue();
		} catch (BundleException e) {
			// here is not the place to validate a manifest			
		}
		return null;		
	}

	public Version getVersion() {
		return version;
	}
	public String getClassPath() {
		return (String) manifest.get(Constants.BUNDLE_CLASSPATH);
	}
	public String getActivator() {
		return (String) manifest.get(Constants.BUNDLE_ACTIVATOR);
	}
	public String getDynamicImports() {
		return (String) manifest.get(Constants.DYNAMICIMPORT_PACKAGE);
	}
	public String getExecutionEnvironment() {
		return (String) manifest.get(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
	}

	protected File getOsgiBase(){
		String fwLocation = System.getProperty(OSGI_FRAMEWORK);
		File result = null;
		if (fwLocation != null){
			try {
				URL baseURL = new URL(fwLocation);
				result = new File(baseURL.getPath());
			} catch (MalformedURLException e) {
				// do nothing, result will be null
			}
		}
		if (result == null) {
			fwLocation = System.getProperty("user.dir");
			if (fwLocation != null) {
				result = new File(fwLocation);
			}
		}
		return result;
	}

	protected Headers createManifest(FrameworkAdaptor adaptor) throws BundleException{
		InputStream in = null;

		if (osgiBase != null && osgiBase.exists()) {
			try {
				in = new FileInputStream(new File(osgiBase,Constants.OSGI_BUNDLE_MANIFEST));
			} catch (FileNotFoundException e) {
				// do nothing here.  in == null
			}
		}

		//TODO Do we still support this case? I thought we would only every ship with manifest.mf
		// If we cannot find the Manifest file then use the old SYSTEMBUNDLE.MF file.
		if (in == null) {
			in = getClass().getResourceAsStream(Constants.OSGI_SYSTEMBUNDLE_MANIFEST);
		}
		if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
			if (in == null) {
				Debug.println("Unable to find system bundle manifest " + Constants.OSGI_SYSTEMBUNDLE_MANIFEST);
			}
		}

		Headers manifest = Headers.parseManifest(in);
		String systemExportProp = System.getProperty(Constants.OSGI_SYSTEMPACKAGES);
		if (systemExportProp != null) {
			String value = (String) manifest.get(Constants.EXPORT_PACKAGE);
			if (value == null) {
				value = systemExportProp;
			} else {
				value += "," + systemExportProp;
			}
			manifest.set(Constants.EXPORT_PACKAGE, null);
			manifest.set(Constants.EXPORT_PACKAGE, value);
		}
		// now get any extra packages and services that the adaptor wants
		// to export and merge this into the system bundle's manifest
		String exportPackages = adaptor.getExportPackages();
		String exportServices = adaptor.getExportServices();
		String providePackages =adaptor.getProvidePackages();
		if (exportPackages != null) {
			String value = (String) manifest.get(Constants.EXPORT_PACKAGE);
			if (value == null) {
				value = exportPackages;
			} else {
				value += "," + exportPackages;
			}
			manifest.set(Constants.EXPORT_PACKAGE, null);
			manifest.set(Constants.EXPORT_PACKAGE, value);
		}
		if (exportServices != null) {
			String value = (String) manifest.get(Constants.EXPORT_SERVICE);
			if (value == null) {
				value = exportServices;
			} else {
				value += "," + exportServices;
			}
			manifest.set(Constants.EXPORT_SERVICE, null);
			manifest.set(Constants.EXPORT_SERVICE, value);
		}
		if (providePackages != null) {
			String value = (String) manifest.get(Constants.PROVIDE_PACKAGE);
			if (value == null) {
				value = providePackages;
			} else {
				value += "," + providePackages;
			}
			manifest.set(Constants.PROVIDE_PACKAGE, null);
			manifest.set(Constants.PROVIDE_PACKAGE, value);
		}
		return manifest;
	}
}
