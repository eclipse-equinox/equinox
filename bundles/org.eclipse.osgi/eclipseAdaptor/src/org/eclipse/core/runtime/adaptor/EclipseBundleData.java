/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime.adaptor;

import java.io.*;
import java.net.URL;
import java.util.*;
import org.eclipse.osgi.framework.adaptor.FrameworkAdaptor;
import org.eclipse.osgi.framework.adaptor.core.*;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.framework.util.Headers;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.pluginconversion.PluginConversionException;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;

/**
 * Internal class.
 */
//Maybe for consistency should it be overriden to do nothing. See also EclipseAdaptor.saveMetadataFor(BundleData)
public class EclipseBundleData extends AbstractBundleData {
	static final byte MANIFEST_TYPE_UNKNOWN = 0x00;
	static final byte MANIFEST_TYPE_BUNDLE = 0x01;
	static final byte MANIFEST_TYPE_PLUGIN = 0x02;
	static final byte MANIFEST_TYPE_FRAGMENT = 0x04;
	static final byte MANIFEST_TYPE_JAR = 0x08;

	private static String[] libraryVariants = null;

	/** data to detect modification made in the manifest */
	private long manifestTimeStamp = 0;
	private byte manifestType = MANIFEST_TYPE_UNKNOWN;

	// URL protocol designations
	public static final String PROTOCOL = "platform"; //$NON-NLS-1$
	public static final String FILE = "file"; //$NON-NLS-1$

	private static final String PROP_CHECK_CONFIG = "osgi.checkConfiguration"; //$NON-NLS-1$
	// the Plugin-Class header
	protected String pluginClass = null;
	// the Eclipse-AutoStart header	
	private boolean autoStart;
	private String[] autoStartExceptions;

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

	public EclipseBundleData(AbstractFrameworkAdaptor adaptor, long id) {
		super(adaptor, id);
	}

	public void initializeExistingBundle() throws IOException {
		File delete = new File(getBundleStoreDir(), ".delete"); //$NON-NLS-1$

		/* and the directory is not marked for delete */
		if (delete.exists())
			throw new IOException();

		createBaseBundleFile();
		if (!checkManifestTimeStamp())
			throw new IOException();
	}

	private boolean checkManifestTimeStamp() {
		if (!"true".equalsIgnoreCase(System.getProperty(PROP_CHECK_CONFIG))) //$NON-NLS-1$
			return true;

		if (PluginConverterImpl.getTimeStamp(getBaseFile(), getManifestType()) == getManifestTimeStamp()) {
			if ((getManifestType() & (MANIFEST_TYPE_JAR | MANIFEST_TYPE_BUNDLE)) != 0)
				return true;
			String cacheLocation = System.getProperty(LocationManager.PROP_MANIFEST_CACHE);
			Location parentConfiguration = LocationManager.getConfigurationLocation().getParentLocation();
			if (parentConfiguration != null) {
				try {
					return checkManifestAndParent(cacheLocation, getSymbolicName(), getVersion().toString(), getManifestType()) != null;
				} catch (BundleException e) {
					return false;
				}
			}
			File cacheFile = new File(cacheLocation, getSymbolicName() + '_' + getVersion() + ".MF"); //$NON-NLS-1$
			if (cacheFile.isFile())
				return true;
		}
		return false;
	}

	/**
	 * Returns the absolute path name of a native library. The VM invokes this
	 * method to locate the native libraries that belong to classes loaded with
	 * this class loader. If this method returns <code>null</code>, the VM
	 * searches the library along the path specified as the <code>java.library.path</code>
	 * property.
	 * 
	 * @param libName
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

	//TODO Unused method
	private URL[] getSearchURLs(URL target) {
		return new URL[] {target};
	}

	public synchronized Dictionary getManifest() throws BundleException {
		return getManifest(false);
	}

	public synchronized Dictionary getManifest(boolean first) throws BundleException {
		if (manifest == null)
			manifest = first ? loadManifest() : new CachedManifest(this);
		return manifest;
	}

	private boolean isComplete(Dictionary manifest) {
		// a manifest is complete if it has a Bundle-SymbolicName entry...
		if (manifest.get(org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME) != null)
			return true;
		// ...or it does not have a plugin/fragment manifest where to get the other entries from  
		return getEntry(PluginConverterImpl.PLUGIN_MANIFEST) == null && getEntry(PluginConverterImpl.FRAGMENT_MANIFEST) == null;
	}

	public synchronized Dictionary loadManifest() throws BundleException {
		URL url = getEntry(Constants.OSGI_BUNDLE_MANIFEST);
		if (url != null) {
			// the bundle has a built-in manifest - we may not have to generate one
			Dictionary builtIn = loadManifestFrom(url);
			// if the manifest is not complete, add entries derived from plug-in/fragment manifest
			if (!isComplete(builtIn)) {
				Dictionary generatedManifest = generateManifest(builtIn);
				if (generatedManifest != null)
					return generatedManifest;
			}
			// the manifest is complete or we could not complete it - take it as it is
			manifestType = MANIFEST_TYPE_BUNDLE;
			if (getBaseFile().isFile()) {
				manifestTimeStamp = getBaseFile().lastModified();
				manifestType |= MANIFEST_TYPE_JAR;
			} else
				manifestTimeStamp = getBaseBundleFile().getEntry(Constants.OSGI_BUNDLE_MANIFEST).getTime();
			return builtIn;
		}
		Dictionary result = generateManifest(null);
		if (result == null)
			throw new BundleException(EclipseAdaptorMsg.formatter.getString("ECLIPSE_DATA_MANIFEST_NOT_FOUND", getLocation())); //$NON-NLS-1$
		return result;
	}

	private Headers basicCheckManifest(String cacheLocation, String symbolicName, String version, byte inputType) throws BundleException {
		File currentFile = new File(cacheLocation, symbolicName + '_' + version + ".MF"); //$NON-NLS-1$
		if (PluginConverterImpl.upToDate(currentFile, getBaseFile(), inputType)) {
			try {
				return Headers.parseManifest(new FileInputStream(currentFile));
			} catch (FileNotFoundException e) {
				// do nothing.
			}
		}
		return null;
	}

	private Headers checkManifestAndParent(String cacheLocation, String symbolicName, String version, byte inputType) throws BundleException {
		Headers result = basicCheckManifest(cacheLocation, symbolicName, version, inputType);
		if (result != null)
			return result;

		Location parentConfiguration = null;
		if ((parentConfiguration = LocationManager.getConfigurationLocation().getParentLocation()) != null) {
			result = basicCheckManifest(new File(parentConfiguration.getURL().getFile(), FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME + '/' + LocationManager.MANIFESTS_DIR).toString(), symbolicName, version, inputType);
		}
		return result;
	}

	private Dictionary generateManifest(Dictionary originalManifest) throws BundleException {
		String cacheLocation = System.getProperty(LocationManager.PROP_MANIFEST_CACHE);
		if (getSymbolicName() != null) {
			Headers existingHeaders = checkManifestAndParent(cacheLocation, getSymbolicName(), getVersion().toString(), manifestType);
			if (existingHeaders != null)
				return existingHeaders;
		}

		PluginConverterImpl converter = PluginConverterImpl.getDefault();

		Dictionary generatedManifest;
		try {
			generatedManifest = converter.convertManifest(getBaseFile(), true, null, true);
		} catch (PluginConversionException pce) {
			String message = EclipseAdaptorMsg.formatter.getString("ECLIPSE_CONVERTER_ERROR_CONVERTING", getBaseFile()); //$NON-NLS-1$
			throw new BundleException(message, pce); //$NON-NLS-1$
		}

		//Now we know the symbolicId and the version of the bundle, we check to see if don't have a manifest for it already
		Version version = Version.parseVersion((String) generatedManifest.get(Constants.BUNDLE_VERSION));
		String symbolicName = ManifestElement.parseHeader(org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME, (String) generatedManifest.get(org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME))[0].getValue();
		ManifestElement generatedFrom = ManifestElement.parseHeader(PluginConverterImpl.GENERATED_FROM, (String) generatedManifest.get(PluginConverterImpl.GENERATED_FROM))[0];
		Headers existingHeaders = checkManifestAndParent(cacheLocation, symbolicName, version.toString(), Byte.parseByte(generatedFrom.getAttribute(PluginConverterImpl.MANIFEST_TYPE_ATTRIBUTE)));
		//We don't have a manifest.
		setManifestTimeStamp(Long.parseLong(generatedFrom.getValue()));
		setManifestType(Byte.parseByte(generatedFrom.getAttribute(PluginConverterImpl.MANIFEST_TYPE_ATTRIBUTE)));
		if (existingHeaders != null)
			return existingHeaders;

		//merge the original manifest with the generated one
		if (originalManifest != null) {
			Enumeration keysEnum = originalManifest.keys();
			while (keysEnum.hasMoreElements()) {
				Object key = keysEnum.nextElement();
				generatedManifest.put(key, originalManifest.get(key));
			}
		}

		//write the generated manifest
		File bundleManifestLocation = new File(cacheLocation, symbolicName + '_' + version.toString() + ".MF"); //$NON-NLS-1$
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
			throw new BundleException(EclipseAdaptorMsg.formatter.getString("ECLIPSE_DATA_ERROR_READING_MANIFEST", getLocation()), e); //$NON-NLS-1$
		}
	}

	protected void loadFromManifest() throws IOException, BundleException {
		getManifest(true);
		super.loadFromManifest();
		// manifest cannot ever be a cached one otherwise the lines below are bogus
		if (manifest instanceof CachedManifest)
			throw new IllegalStateException();
		pluginClass = (String) manifest.get(EclipseAdaptor.PLUGIN_CLASS);
		parseAutoStart((String) manifest.get(EclipseAdaptor.ECLIPSE_AUTOSTART));
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

	public byte getManifestType() {
		return manifestType;
	}

	public void setManifestType(byte manifestType) {
		this.manifestType = manifestType;
	}

	public void setAutoStart(boolean value) {
		autoStart = value;
	}

	public boolean isAutoStart() {
		return autoStart;
	}

	public int getPersistentStatus() {
		// omit the active state if necessary  
		return isAutoStartable() ? (~Constants.BUNDLE_STARTED) & getStatus() : getStatus();
	}

	public void setAutoStartExceptions(String[] autoStartExceptions) {
		this.autoStartExceptions = autoStartExceptions;
	}

	public String[] getAutoStartExceptions() {
		return autoStartExceptions;
	}

	private void parseAutoStart(String headerValue) {
		autoStart = false;
		autoStartExceptions = null;
		ManifestElement[] allElements = null;
		try {
			allElements = ManifestElement.parseHeader(EclipseAdaptor.ECLIPSE_AUTOSTART, headerValue);
		} catch (BundleException e) {
			// just use the default settings (no auto activation)
			String message = EclipseAdaptorMsg.formatter.getString("ECLIPSE_CLASSLOADER_CANNOT_GET_HEADERS", getLocation()); //$NON-NLS-1$
			EclipseAdaptor.getDefault().getFrameworkLog().log(new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, message, 0, e, null));
		}
		//Eclipse-AutoStart not found... 
		if (allElements == null)
			return;
		// the single value for this element should be true|false
		autoStart = "true".equalsIgnoreCase(allElements[0].getValue()); //$NON-NLS-1$
		// look for any exceptions (the attribute) to the autoActivate setting
		String exceptionsValue = allElements[0].getAttribute(EclipseAdaptor.ECLIPSE_AUTOSTART_EXCEPTIONS);
		if (exceptionsValue == null)
			return;
		StringTokenizer tokenizer = new StringTokenizer(exceptionsValue, ","); //$NON-NLS-1$
		int numberOfTokens = tokenizer.countTokens();
		autoStartExceptions = new String[numberOfTokens];
		for (int i = 0; i < numberOfTokens; i++)
			autoStartExceptions[i] = tokenizer.nextToken().trim();
	}

	public boolean isAutoStartable() {
		return autoStart || (autoStartExceptions != null && autoStartExceptions.length > 0);
	}

	/**
	 * Save the bundle data in the data file.
	 *
	 * @throws IOException if a write error occurs.
	 */
	public synchronized void save() throws IOException {
		((EclipseAdaptor) adaptor).saveMetaDataFor(this);
	}
}
