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
import java.net.URL;
import java.util.*;
import java.util.ArrayList;
import java.util.Dictionary;

import org.eclipse.osgi.framework.adaptor.core.*;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.framework.internal.defaultadaptor.*;
import org.eclipse.osgi.framework.util.Headers;
import org.osgi.framework.BundleException;

public class EclipseBundleData extends DefaultBundleData {

	private static String[] libraryVariants = null;

	/** data to detect modification made in the manifest */
	private long manifestTimeStamp = 0;
	
		// URL protocol designations
	public static final String PROTOCOL = "platform"; //$NON-NLS-1$
	public static final String FILE = "file"; //$NON-NLS-1$

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

	public void initializeExistingBundle() throws IOException {
		File delete = new File(getBundleStoreDir(), ".delete");

		/* and the directory is not marked for delete */
		if (delete.exists()) 
			throw new IOException();

		setGenerationDir(new File(getBundleStoreDir(), String.valueOf(getGeneration())));
		setBaseFile(isReference() ? new File(getFileName()) : new File(createGenerationDir(), getFileName()));
		createBaseBundleFile();
		if (! checkManifestTimeStamp(baseBundleFile))
			throw new IOException();
	}

	private boolean checkManifestTimeStamp(BundleFile bundlefile) {
		if ("true".equalsIgnoreCase(NO_TIMESTAMP_CHECKING)) //$NON-NLS-1$
			return true;

		BundleEntry bundleManifestEntry = bundlefile.getEntry("."); //$NON-NLS-1$
		return bundleManifestEntry != null && bundleManifestEntry.getTime() == getManifestTimeStamp();
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
			BundleEntry libEntry = baseBundleFile.getEntry(variants[i] + path);
			if (libEntry == null) {
				//					if (DEBUG && DEBUG_SHOW_FAILURE)
				//						debug("not found " + variants[i] + path);
				// //$NON-NLS-1$
			} else {
				//					if (DEBUG && DEBUG_SHOW_SUCCESS)
				//						debug("found " + path + " as " +
				// variants[i] + path); //$NON-NLS-1$ //$NON-NLS-2$
				File libFile = baseBundleFile.getFile(variants[i] + path);
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
			Dictionary generatedManifest = generateManifest(manifest);
			if (generatedManifest != null)
				manifest = generatedManifest;
		}		
		return manifest;	
	}

	public synchronized Dictionary loadManifest() throws BundleException {		
		URL url = getEntry(Constants.OSGI_BUNDLE_MANIFEST);
		if (url != null) {
			manifestTimeStamp = getBaseBundleFile().getEntry("").getTime();
			return loadManifestFrom(url);
		}
		Dictionary result = generateManifest(null);		
		if (result == null)	//TODO: need to NLS this
			throw new BundleException("Manifest not found: " + getLocation());
		return result;
	}

	private Dictionary generateManifest(Dictionary originalManifest) throws BundleException {
		PluginConverterImpl converter = PluginConverterImpl.getDefault();

		setManifestTimeStamp(getBaseFile().lastModified());
		Dictionary generatedManifest = converter.convertManifest(getBaseFile(), true);
		if (generatedManifest == null)
			return null;

		//merge the original manifest with the generated one
		if (originalManifest != null) {
			Enumeration  enum = originalManifest.keys();
			while (enum.hasMoreElements()) {
				 Object key =  enum.nextElement();
				 generatedManifest.put(key, originalManifest.get(key));
			}
		}
		
		//write the generated manifest
		String cacheLocation = (String) System.getProperties().get("osgi.manifest.cache");
		File bundleManifestLocation = new File(cacheLocation, (String) generatedManifest.get(org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME) + '_' + generatedManifest.get(Constants.BUNDLE_VERSION) + ".MF");
		try {
			converter.writeManifest(bundleManifestLocation, generatedManifest, true);
		} catch (Exception e) {
			//TODO Need to log
		}
		return generatedManifest;

	}
	private Dictionary loadManifestFrom(URL manifestURL) throws BundleException {
		try {
			return Headers.parseManifest(manifestURL.openStream());
		} catch (IOException e) {
			throw new BundleException("Error reading manifest: " + getLocation(), e);
		}		
	}

	protected void loadFromManifest() throws IOException{
		try {
			getManifest(true);
		} catch (BundleException e) {
			throw new IOException("Unable to properly read manifest for: " + getLocation());
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
	public long getManifestTimeStamp() {
		return manifestTimeStamp;
	}
	public void setManifestTimeStamp(long stamp) {
		manifestTimeStamp = stamp;
	}
}
