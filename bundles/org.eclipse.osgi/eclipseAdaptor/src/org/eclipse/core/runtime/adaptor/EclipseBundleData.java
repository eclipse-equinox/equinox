/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime.adaptor;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.framework.internal.defaultadaptor.*;
import org.eclipse.osgi.framework.util.Headers;
import org.eclipse.osgi.service.resolver.Version;
import org.osgi.framework.BundleException;

public class EclipseBundleData extends DefaultBundleData {

	private URL base;
	private static String[] libraryVariants = null;

	/** data to detect modification made in the manifest */
	protected final byte PLUGIN = 1;
	protected final byte BUNDLE = 0;

	private byte manifestType = BUNDLE;
	private long manifestTimeStamp = 0;
	
		// URL protocol designations
	public static final String PROTOCOL = "platform"; //$NON-NLS-1$
	public static final String FILE = "file"; //$NON-NLS-1$

	private static final String PLUGIN_MANIFEST = "plugin.xml"; //$NON-NLS-1$
	private static final String FRAGMENT_MANIFEST = "fragment.xml"; //$NON-NLS-1$
	private static final String NO_TIMESTAMP_CHECKING = "osgi.noManifestTimeChecking"; //$NON-NLS-1$
	protected String isLegacy = null;
	protected String pluginClass = null;

	private static String[] buildLibraryVariants() {
		ArrayList result = new ArrayList();
		EnvironmentInfo info = EnvironmentInfo.getDefault();
		result.add("ws/" + info.getWS() + "/"); //$NON-NLS-1$ //$NON-NLS-2$
		result.add("os/" + info.getOS() + "/" + info.getOSArch() + "/"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		result.add("os/" + info.getOS() + "/"); //$NON-NLS-1$ //$NON-NLS-2$
		String nl = info.getNL();
		nl = nl.replace('_', '/');
		while (nl.length() > 0) {
			result.add("nl/" + nl + "/"); //$NON-NLS-1$ //$NON-NLS-2$
			int i = nl.lastIndexOf('/');
			nl = (i < 0) ? "" : nl.substring(0, i); //$NON-NLS-1$
		}
		result.add(""); //$NON-NLS-1$
		return (String[]) result.toArray(new String[result.size()]);
	}

	public EclipseBundleData(DefaultAdaptor adaptor, long id) throws IOException {
		super(adaptor, id);
	}

	public void initializeNewBundle(String location, String fileName, boolean reference, File file) throws IOException {
		this.location = location;
		this.fileName = fileName;
		this.reference = reference;
		this.file = file;

		setStartLevel(adaptor.getInitialBundleStartLevel());
		bundleFile = BundleFile.createBundleFile(file, this);
		loadFromManifest();
		initializeBase(location);
	}

	public void initializeExistingBundle(String directory) throws IOException {
		
		file = reference ? new File(fileName) : new File(dirGeneration, fileName);
		bundleFile = BundleFile.createBundleFile(file,this);
		if (! checkManifestTimeStamp(bundleFile))
			throw new IOException();
		initializeBase(location);
	}

	private boolean checkManifestTimeStamp(BundleFile bundlefile) {
		if ("true".equalsIgnoreCase(NO_TIMESTAMP_CHECKING)) //$NON-NLS-1$
			return true;
		// always try retrieving a bundle manifest
		BundleEntry bundleManifestEntry = bundlefile.getEntry(Constants.OSGI_BUNDLE_MANIFEST);
		// we thought it was a bundle but it does not have a bundle manifest, or the other way around
		// we need to update this guy
		if (bundleManifestEntry != null ^ getManifestType() == BUNDLE)
			return false;
		if (getManifestType() == BUNDLE)
			return bundleManifestEntry.getTime() == getManifestTimeStamp();
		// we don't have a bundle manifest, so let's check the plugin/fragment manifest
		BundleEntry pluginManifestEntry = bundlefile.getEntry(PLUGIN_MANIFEST);
		if (pluginManifestEntry == null)
			pluginManifestEntry = bundlefile.getEntry(FRAGMENT_MANIFEST);
		// if we have one, check timestamp - if we don't this guy is not anything we know 
		return pluginManifestEntry != null && pluginManifestEntry.getTime() == getManifestTimeStamp();
	}

	private void initializeBase(String location) throws IOException {
		if (!location.endsWith("/"))
			location += "/";
		try {
			base = new URL(location);
			if (base.getProtocol().equals("reference"))
				base = new URL(base.getFile());
		}
		catch (MalformedURLException e) {
			base = null;
		}

	}

	/**
	 * Returns the absolute path name of a native library. The VM invokes this
	 * method to locate the native libraries that belong to classes loaded with
	 * this class loader. If this method returns <code>null</code>, the VM
	 * searches the library along the path specified as the <code>java.library.path</code>
	 * property.
	 * 
	 * @param libname
	 *                   the library name
	 * @return the absolute path of the native library
	 */
	public String findLibrary(String libName) {
		// first do the standard OSGi lookup using the native clauses
		// in the manifest. If that fails, do the legacy Eclipse lookup.
		String result = super.findLibrary(libName);
		if (result != null)
			return result;
		if (libraryVariants == null)
			libraryVariants = buildLibraryVariants();
		if (libName.length() == 0)
			return null;
		if (libName.charAt(0) == '/' || libName.charAt(0) == '\\')
			libName = libName.substring(1);
		libName = System.mapLibraryName(libName);

		//		if (DEBUG && DEBUG_SHOW_ACTIONS && debugNative(libName))
		//			debug("findLibrary(" + libName + ")"); //$NON-NLS-1$ //$NON-NLS-2$

		return searchVariants(libraryVariants, libName);

	}

	private String searchVariants(String[] variants, String path) {
		for (int i = 0; i < variants.length; i++) {
			BundleEntry libEntry = bundleFile.getEntry(variants[i] + path);
			if (libEntry == null) {
				//					if (DEBUG && DEBUG_SHOW_FAILURE)
				//						debug("not found " + variants[i] + path);
				// //$NON-NLS-1$
			} else {
				//					if (DEBUG && DEBUG_SHOW_SUCCESS)
				//						debug("found " + path + " as " +
				// variants[i] + path); //$NON-NLS-1$ //$NON-NLS-2$
				File libFile = bundleFile.getFile(variants[i] + path);
				return libFile.getAbsolutePath();
			}
		}
		return null;
	}

	private URL[] getSearchURLs(URL target) {
		return new URL[] { target };
	}

	public synchronized Dictionary getManifest() {
		try {
			return getManifest(false);
		} catch (BundleException e) {
			// TODO: handle exception
			return null;
		}
	}
	public synchronized Dictionary getManifest(boolean first) throws BundleException {
		if (manifest == null)
			manifest = first ? loadManifest() : new CachedManifest(this);
		if (manifest.get(org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME) == null) {
			Dictionary generatedManifest = generateManifest();
			if (generatedManifest != null)
				manifest = generatedManifest;
		}
		
		return manifest;
		
	}

	public synchronized Dictionary loadManifest() throws BundleException {		
		URL url = getEntry(Constants.OSGI_BUNDLE_MANIFEST);
		if (url != null) {
			try {
				manifestTimeStamp = url.openConnection().getLastModified();
				manifestType = BUNDLE;
			} catch (IOException e) {
				//ignore the exception since this is for timeStamp
			}
			return loadManifestFrom(url);
		}
		Dictionary result = generateManifest();		
		if (result == null)	//TODO: need to NLS this
			throw new BundleException("Manifest not found: " + getLocation());
		return result;
	}

	private File findPluginManifest(File baseLocation) {
		File pluginManifestLocation = new File(baseLocation, PLUGIN_MANIFEST); //$NON-NLS-1$
		if (pluginManifestLocation.isFile())
			return pluginManifestLocation;
		pluginManifestLocation = new File(baseLocation, FRAGMENT_MANIFEST); //$NON-NLS-1$
		if (pluginManifestLocation.isFile())
			return pluginManifestLocation;
		return null;
	}
	
	private Dictionary generateManifest() throws BundleException {
		PluginConverterImpl converter = PluginConverterImpl.getDefault();
		File location = findPluginManifest(file);
		if (location == null)
			return null;

		setManifestTimeStamp(location.lastModified());
		setManifestType(PLUGIN);
		location = converter.convertManifest(location, true);
		if (location == null)
			return null;	
		try {	
			return loadManifestFrom(location.toURL());
		} catch (MalformedURLException mfue) {
			return null;
		}
	}
	private Dictionary loadManifestFrom(URL manifestURL) throws BundleException {
		try {
			return Headers.parseManifest(manifestURL.openStream());
		} catch (IOException e) {
			throw new BundleException("Error reading manifest: " + getLocation(), e);
		}		
	}
	public void save() {
		// do nothing.  This method is here to override one in the superclass.
	}

	protected void loadMetaData() {
		// do nothing.  This method is here to override one in the superclass.
	}

	protected void loadFromMetaData() {
		// do nothing.  This method is here to override one in the superclass.
	}
	protected void loadFromManifest() throws IOException{
		try {
			getManifest(true);
		} catch (BundleException e) {
			throw new IOException("Unable to properly read manifest for: " + location);
		}
		super.loadFromManifest();
		pluginClass = (String)manifest.get("Plugin-Class");
		isLegacy = (String)manifest.get("Legacy");
	}
	public String isLegacy() {
		return isLegacy;
	}
	public void setLegacy(String value) {
		isLegacy = value;
	}

	public String getPluginClass() {
		return pluginClass;
	}
	public void setPluginClass(String value) {
		pluginClass = value;
	}
	public void setLocation(String value) {
		this.location = value;
	}
	public void setName(String value) {
		this.fileName = value;
	}
	public void setUniqueId(String value){
		this.uniqueId = value;
	}
	public void setVersion(Version value){
		this.version = value;
	}
	public void setActivator(String value){
		this.activator = value;
	}
	public void setClassPath(String value){
		this.classpath = value;
	}
	public void setFragment(boolean value){
		this.isFragment = value;
	}
	public void setExecutionEnvironment(String value) {
		this.executionEnvironment = value;
	}
	public void setDynamicImports(String value) {
		this.dynamicImports = value;
	}
	public long getManifestTimeStamp() {
		return manifestTimeStamp;
	}
	public byte getManifestType() {
		return manifestType;
	}
	public void setManifestTimeStamp(long stamp) {
		manifestTimeStamp = stamp;
	}
	public void setManifestType(byte type) {
		manifestType = type;	
	}
}
