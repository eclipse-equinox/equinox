/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.core.runtime.internal.adaptor;

import java.io.*;
import java.net.URL;
import java.security.*;
import java.util.Dictionary;
import java.util.Enumeration;
import org.eclipse.core.runtime.adaptor.LocationManager;
import org.eclipse.osgi.baseadaptor.*;
import org.eclipse.osgi.baseadaptor.hooks.StorageHook;
import org.eclipse.osgi.framework.adaptor.FrameworkAdaptor;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.framework.internal.core.FrameworkProperties;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.framework.util.Headers;
import org.eclipse.osgi.framework.util.KeyedElement;
import org.eclipse.osgi.internal.baseadaptor.AdaptorUtil;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.pluginconversion.PluginConversionException;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;

public final class EclipseStorageHook implements StorageHook, HookConfigurator {
	// System property used to check timestamps of the bundles in the configuration
	private static final String PROP_CHECK_CONFIG = "osgi.checkConfiguration"; //$NON-NLS-1$
	private static final String PROP_COMPATIBILITY_LAZYSTART = "osgi.compatibility.eagerStart.LazyActivation"; //$NON-NLS-1$
	private static final boolean COMPATIBILITY_LAZYSTART = Boolean.valueOf(FrameworkProperties.getProperty(PROP_COMPATIBILITY_LAZYSTART, "true")).booleanValue(); //$NON-NLS-1$
	private static final int STORAGE_VERION = 4;

	public static final String KEY = EclipseStorageHook.class.getName();
	public static final int HASHCODE = KEY.hashCode();

	private static final byte FLAG_LAZY_START = 0x01;
	private static final byte FLAG_HAS_PACKAGE_INFO = 0x02;
	// Note that the 0x04 was used in previous versions, if a new flag is needed then do not reuse this one
	//private static final byte FLAG_ACTIVATE_ON_CLASSLOAD = 0x04;
	// Flag to indicate that an include directive is present on the lazy activation policy
	private static final byte FLAG_HAS_LAZY_INCLUDE = 0x08;

	/** data to detect modification made in the manifest */
	private long manifestTimeStamp = 0;
	private byte manifestType = PluginConverterImpl.MANIFEST_TYPE_UNKNOWN;

	private BaseData bundledata;

	/** the Plugin-Class header */
	private String pluginClass = null;
	/**  Eclipse-LazyStart header */
	private String[] lazyStartExcludes;
	private String[] lazyStartIncludes;
	private int bundleManfestVersion;
	/** shortcut to know if a bundle has a buddy */
	private String buddyList;
	/** shortcut to know if a bundle is a registrant to a registered policy */
	private String registeredBuddyList;
	/** DS Service Component header */
	private String serviceComponent;
	private byte flags = 0;

	public int getStorageVersion() {
		return STORAGE_VERION;
	}

	/**
	 * @throws BundleException  
	 */
	public StorageHook create(BaseData data) throws BundleException {
		EclipseStorageHook storageHook = new EclipseStorageHook();
		storageHook.bundledata = data;
		return storageHook;
	}

	@SuppressWarnings("deprecation")
	public void initialize(Dictionary<String, String> manifest) throws BundleException {
		String activationPolicy = manifest.get(Constants.BUNDLE_ACTIVATIONPOLICY);
		if (activationPolicy != null) {
			parseActivationPolicy(this, activationPolicy);
		} else {
			String lazyStart = manifest.get(Constants.ECLIPSE_LAZYSTART);
			if (lazyStart == null)
				lazyStart = manifest.get(Constants.ECLIPSE_AUTOSTART);
			parseLazyStart(this, lazyStart);
		}
		try {
			String versionString = manifest.get(Constants.BUNDLE_MANIFESTVERSION);
			bundleManfestVersion = versionString == null ? 0 : Integer.parseInt(versionString);
		} catch (NumberFormatException nfe) {
			bundleManfestVersion = 0;
		}
		pluginClass = manifest.get(Constants.PLUGIN_CLASS);
		buddyList = manifest.get(Constants.BUDDY_LOADER);
		registeredBuddyList = manifest.get(Constants.REGISTERED_POLICY);
		if (hasPackageInfo(bundledata.getEntry(Constants.OSGI_BUNDLE_MANIFEST)))
			flags |= FLAG_HAS_PACKAGE_INFO;
		String genFrom = manifest.get(PluginConverterImpl.GENERATED_FROM);
		if (genFrom != null) {
			ManifestElement generatedFrom = ManifestElement.parseHeader(PluginConverterImpl.GENERATED_FROM, genFrom)[0];
			if (generatedFrom != null) {
				manifestTimeStamp = Long.parseLong(generatedFrom.getValue());
				manifestType = Byte.parseByte(generatedFrom.getAttribute(PluginConverterImpl.MANIFEST_TYPE_ATTRIBUTE));
			}
		}
		if (isAutoStartable()) {
			bundledata.setStatus(bundledata.getStatus() | Constants.BUNDLE_LAZY_START);
			if (COMPATIBILITY_LAZYSTART)
				bundledata.setStatus(bundledata.getStatus() | Constants.BUNDLE_STARTED | Constants.BUNDLE_ACTIVATION_POLICY);
		}
		serviceComponent = manifest.get(CachedManifest.SERVICE_COMPONENT);
	}

	public StorageHook load(BaseData target, DataInputStream in) throws IOException {
		EclipseStorageHook storageHook = new EclipseStorageHook();
		storageHook.bundledata = target;
		storageHook.flags = in.readByte();
		int pkgCount = in.readInt();
		String[] packageList = pkgCount > 0 ? new String[pkgCount] : null;
		for (int i = 0; i < pkgCount; i++)
			packageList[i] = in.readUTF();
		storageHook.lazyStartExcludes = packageList;
		if ((storageHook.flags & FLAG_HAS_LAZY_INCLUDE) != 0) {
			pkgCount = in.readInt();
			packageList = pkgCount > 0 ? new String[pkgCount] : null;
			for (int i = 0; i < pkgCount; i++)
				packageList[i] = in.readUTF();
			storageHook.lazyStartIncludes = packageList;
		}
		storageHook.buddyList = AdaptorUtil.readString(in, false);
		storageHook.registeredBuddyList = AdaptorUtil.readString(in, false);
		storageHook.pluginClass = AdaptorUtil.readString(in, false);
		storageHook.manifestTimeStamp = in.readLong();
		storageHook.manifestType = in.readByte();
		storageHook.bundleManfestVersion = in.readInt();
		if (storageHook.isAutoStartable()) {
			if ((target.getStatus() & Constants.BUNDLE_LAZY_START) == 0)
				target.setStatus(target.getStatus() | Constants.BUNDLE_LAZY_START);
			// if the compatibility flag is set then we must make sure the persistent start bit is set and the activation policy bit;
			// if the persistent start bit was already set then we should not set the activation policy bit because this is an "eager" started bundle.
			if (COMPATIBILITY_LAZYSTART && (target.getStatus() & Constants.BUNDLE_STARTED) == 0)
				target.setStatus(target.getStatus() | Constants.BUNDLE_STARTED | Constants.BUNDLE_ACTIVATION_POLICY);
		}
		storageHook.serviceComponent = AdaptorUtil.readString(in, false);
		return storageHook;
	}

	public void save(DataOutputStream out) throws IOException {
		if (bundledata == null)
			throw new IllegalStateException();
		// when this is stored back we always use the has include/exclude flag
		out.writeByte(flags);
		String[] excludes = getLazyStartExcludes();
		if (excludes == null)
			out.writeInt(0);
		else {
			out.writeInt(excludes.length);
			for (int i = 0; i < excludes.length; i++)
				out.writeUTF(excludes[i]);
		}
		if ((flags & FLAG_HAS_LAZY_INCLUDE) != 0) {
			String[] includes = getLazyStartIncludes();
			if (includes == null)
				out.writeInt(0);
			else {
				out.writeInt(includes.length);
				for (int i = 0; i < includes.length; i++)
					out.writeUTF(includes[i]);
			}
		}
		AdaptorUtil.writeStringOrNull(out, getBuddyList());
		AdaptorUtil.writeStringOrNull(out, getRegisteredBuddyList());
		AdaptorUtil.writeStringOrNull(out, getPluginClass());
		out.writeLong(getManifestTimeStamp());
		out.writeByte(getManifestType());
		out.writeInt(getBundleManifestVersion());
		AdaptorUtil.writeStringOrNull(out, serviceComponent);
	}

	public int getKeyHashCode() {
		return HASHCODE;
	}

	public boolean compare(KeyedElement other) {
		return other.getKey() == KEY;
	}

	public Object getKey() {
		return KEY;
	}

	public boolean isLazyStart() {
		return (flags & FLAG_LAZY_START) == FLAG_LAZY_START;
	}

	public String[] getLazyStartExcludes() {
		return lazyStartExcludes;
	}

	public String[] getLazyStartIncludes() {
		return lazyStartIncludes;
	}

	public String getBuddyList() {
		return buddyList;
	}

	public boolean hasPackageInfo() {
		return (flags & FLAG_HAS_PACKAGE_INFO) == FLAG_HAS_PACKAGE_INFO;
	}

	public String getPluginClass() {
		return pluginClass;
	}

	public String getRegisteredBuddyList() {
		return registeredBuddyList;
	}

	public long getManifestTimeStamp() {
		return manifestTimeStamp;
	}

	public byte getManifestType() {
		return manifestType;
	}

	public int getBundleManifestVersion() {
		return bundleManfestVersion;
	}

	public String getServiceComponent() {
		return serviceComponent;
	}

	/**
	 * Checks whether this bundle is auto started for all resource/class loads or only for a
	 * subset of resource/classloads 
	 * @return true if the bundle is auto started; false otherwise
	 */
	public boolean isAutoStartable() {
		return isLazyStart() || (lazyStartExcludes != null && lazyStartExcludes.length > 0);
	}

	private void parseLazyStart(EclipseStorageHook storageHook, String headerValue) {
		storageHook.lazyStartExcludes = null;
		ManifestElement[] allElements = null;
		try {
			allElements = ManifestElement.parseHeader(Constants.ECLIPSE_LAZYSTART, headerValue);
		} catch (BundleException e) {
			// just use the default settings (no auto activation)
			String message = NLS.bind(EclipseAdaptorMsg.ECLIPSE_CLASSLOADER_CANNOT_GET_HEADERS, storageHook.bundledata.getLocation());
			bundledata.getAdaptor().getFrameworkLog().log(new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, FrameworkLogEntry.ERROR, 0, message, 0, e, null));
		}
		//Eclipse-AutoStart not found... 
		if (allElements == null)
			return;
		// the single value for this element should be true|false
		if ("true".equalsIgnoreCase(allElements[0].getValue())) //$NON-NLS-1$
			storageHook.flags |= FLAG_LAZY_START;
		// look for any exceptions (the attribute) to the autoActivate setting
		String[] exceptions = ManifestElement.getArrayFromList(allElements[0].getAttribute(Constants.ECLIPSE_LAZYSTART_EXCEPTIONS));
		storageHook.lazyStartExcludes = exceptions;
	}

	private void parseActivationPolicy(EclipseStorageHook storageHook, String headerValue) {
		storageHook.lazyStartExcludes = null;
		ManifestElement[] allElements = null;
		try {
			allElements = ManifestElement.parseHeader(Constants.BUNDLE_ACTIVATIONPOLICY, headerValue);
		} catch (BundleException e) {
			// just use the default settings (no auto activation)
			String message = NLS.bind(EclipseAdaptorMsg.ECLIPSE_CLASSLOADER_CANNOT_GET_HEADERS, storageHook.bundledata.getLocation());
			bundledata.getAdaptor().getFrameworkLog().log(new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, FrameworkLogEntry.ERROR, 0, message, 0, e, null));
		}
		//Bundle-ActivationPolicy not found. 
		if (allElements == null)
			return;
		// the single value for this type is lazy
		if (!Constants.ACTIVATION_LAZY.equalsIgnoreCase(allElements[0].getValue()))
			return;
		storageHook.flags |= FLAG_LAZY_START;
		// look for any include or exclude attrs
		storageHook.lazyStartExcludes = ManifestElement.getArrayFromList(allElements[0].getDirective(Constants.EXCLUDE_DIRECTIVE));
		storageHook.lazyStartIncludes = ManifestElement.getArrayFromList(allElements[0].getDirective(Constants.INCLUDE_DIRECTIVE));
		if (storageHook.lazyStartIncludes != null)
			storageHook.flags |= FLAG_HAS_LAZY_INCLUDE;
	}

	// Used to check the bundle manifest file for any package information.
	// This is used when '.' is on the Bundle-ClassPath to prevent reading
	// the bundle manifest for pacakge information when loading classes.
	private static boolean hasPackageInfo(URL url) {
		if (url == null)
			return false;
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(url.openStream()));
			String line;
			while ((line = br.readLine()) != null) {
				if (line.length() < 20)
					continue;
				switch (line.charAt(0)) {
					case 'S' :
						if (line.charAt(1) == 'p')
							if (line.startsWith("Specification-Title: ") || line.startsWith("Specification-Version: ") || line.startsWith("Specification-Vendor: ")) //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$
								return true;
						break;
					case 'I' :
						if (line.startsWith("Implementation-Title: ") || line.startsWith("Implementation-Version: ") || line.startsWith("Implementation-Vendor: ")) //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$ 
							return true;
						break;
				}
			}
		} catch (IOException ioe) {
			// do nothing
		} finally {
			if (br != null)
				try {
					br.close();
				} catch (IOException e) {
					// do nothing
				}
		}
		return false;
	}

	public void addHooks(HookRegistry hookRegistry) {
		hookRegistry.addStorageHook(this);
	}

	private void checkTimeStamp() throws IllegalArgumentException {
		if (!checkManifestTimeStamp())
			throw new IllegalArgumentException();
	}

	private boolean checkManifestTimeStamp() {
		if (!"true".equalsIgnoreCase(FrameworkProperties.getProperty(EclipseStorageHook.PROP_CHECK_CONFIG))) //$NON-NLS-1$
			return true;
		if (PluginConverterImpl.getTimeStamp(bundledata.getBundleFile().getBaseFile(), getManifestType()) == getManifestTimeStamp()) {
			if ((getManifestType() & (PluginConverterImpl.MANIFEST_TYPE_JAR | PluginConverterImpl.MANIFEST_TYPE_BUNDLE)) != 0)
				return true;
			String cacheLocation = FrameworkProperties.getProperty(LocationManager.PROP_MANIFEST_CACHE);
			Location parentConfiguration = LocationManager.getConfigurationLocation().getParentLocation();
			if (parentConfiguration != null) {
				try {
					return checkManifestAndParent(cacheLocation, bundledata.getSymbolicName(), bundledata.getVersion().toString(), getManifestType()) != null;
				} catch (BundleException e) {
					return false;
				}
			}
			File cacheFile = new File(cacheLocation, bundledata.getSymbolicName() + '_' + bundledata.getVersion() + ".MF"); //$NON-NLS-1$
			if (cacheFile.isFile())
				return true;
		}
		return false;
	}

	private Headers<String, String> checkManifestAndParent(String cacheLocation, String symbolicName, String version, byte inputType) throws BundleException {
		Headers<String, String> result = basicCheckManifest(cacheLocation, symbolicName, version, inputType);
		if (result != null)
			return result;
		Location parentConfiguration = null;
		if ((parentConfiguration = LocationManager.getConfigurationLocation().getParentLocation()) != null)
			result = basicCheckManifest(new File(parentConfiguration.getURL().getFile(), FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME + '/' + LocationManager.MANIFESTS_DIR).toString(), symbolicName, version, inputType);
		return result;
	}

	private Headers<String, String> basicCheckManifest(String cacheLocation, String symbolicName, String version, byte inputType) throws BundleException {
		File currentFile = new File(cacheLocation, symbolicName + '_' + version + ".MF"); //$NON-NLS-1$
		if (PluginConverterImpl.upToDate(currentFile, bundledata.getBundleFile().getBaseFile(), inputType)) {
			try {
				return Headers.parseManifest(new FileInputStream(currentFile));
			} catch (FileNotFoundException e) {
				// do nothing.
			}
		}
		return null;
	}

	Dictionary<String, String> createCachedManifest(boolean firstTime) throws BundleException {
		return firstTime ? getGeneratedManifest() : new CachedManifest(this);
	}

	public Dictionary<String, String> getGeneratedManifest() throws BundleException {
		if (System.getSecurityManager() == null)
			return getGeneratedManifest0();
		try {
			return AccessController.doPrivileged(new PrivilegedExceptionAction<Dictionary<String, String>>() {
				public Dictionary<String, String> run() throws BundleException {
					return getGeneratedManifest0();
				}
			});
		} catch (PrivilegedActionException e) {
			throw (BundleException) e.getException();
		}
	}

	final Dictionary<String, String> getGeneratedManifest0() throws BundleException {
		Dictionary<String, String> builtIn = AdaptorUtil.loadManifestFrom(bundledata);
		if (builtIn != null) {
			// the bundle has a built-in manifest - we may not have to generate one
			if (!isComplete(builtIn)) {
				Dictionary<String, String> generatedManifest = generateManifest(builtIn);
				if (generatedManifest != null)
					return generatedManifest;
			}
			// the manifest is complete or we could not complete it - take it as it is
			manifestType = PluginConverterImpl.MANIFEST_TYPE_BUNDLE;
			File baseFile = bundledata.getBundleFile().getBaseFile();
			if (baseFile != null && bundledata.getBundleFile().getBaseFile().isFile()) {
				manifestTimeStamp = bundledata.getBundleFile().getBaseFile().lastModified();
				manifestType |= PluginConverterImpl.MANIFEST_TYPE_JAR;
			} else
				manifestTimeStamp = bundledata.getBundleFile().getEntry(Constants.OSGI_BUNDLE_MANIFEST).getTime();
			return builtIn;
		}
		Dictionary<String, String> result = generateManifest(null);
		if (result == null)
			throw new BundleException(NLS.bind(EclipseAdaptorMsg.ECLIPSE_DATA_MANIFEST_NOT_FOUND, bundledata.getLocation()));
		return result;
	}

	private Dictionary<String, String> generateManifest(Dictionary<String, String> builtIn) throws BundleException {
		String cacheLocation = FrameworkProperties.getProperty(LocationManager.PROP_MANIFEST_CACHE);
		if (bundledata.getSymbolicName() != null) {
			Headers<String, String> existingHeaders = checkManifestAndParent(cacheLocation, bundledata.getSymbolicName(), bundledata.getVersion().toString(), manifestType);
			if (existingHeaders != null)
				return existingHeaders;
		}

		PluginConverterImpl converter = PluginConverterImpl.getDefault();
		if (converter == null)
			converter = new PluginConverterImpl(bundledata.getAdaptor(), bundledata.getAdaptor().getContext());

		Dictionary<String, String> generatedManifest;
		try {
			generatedManifest = converter.convertManifest(bundledata.getBundleFile().getBaseFile(), true, null, true, null);
		} catch (PluginConversionException pce) {
			String message = NLS.bind(EclipseAdaptorMsg.ECLIPSE_CONVERTER_ERROR_CONVERTING, bundledata.getBundleFile().getBaseFile());
			throw new BundleException(message, BundleException.MANIFEST_ERROR, pce);
		}

		//Now we know the symbolicId and the version of the bundle, we check to see if don't have a manifest for it already
		Version version = Version.parseVersion(generatedManifest.get(Constants.BUNDLE_VERSION));
		String symbolicName = ManifestElement.parseHeader(org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME, generatedManifest.get(org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME))[0].getValue();
		ManifestElement generatedFrom = ManifestElement.parseHeader(PluginConverterImpl.GENERATED_FROM, generatedManifest.get(PluginConverterImpl.GENERATED_FROM))[0];
		Headers<String, String> existingHeaders = checkManifestAndParent(cacheLocation, symbolicName, version.toString(), Byte.parseByte(generatedFrom.getAttribute(PluginConverterImpl.MANIFEST_TYPE_ATTRIBUTE)));
		//We don't have a manifest.
		manifestTimeStamp = Long.parseLong(generatedFrom.getValue());
		manifestType = Byte.parseByte(generatedFrom.getAttribute(PluginConverterImpl.MANIFEST_TYPE_ATTRIBUTE));
		if (bundledata.getAdaptor().isReadOnly() || existingHeaders != null)
			return existingHeaders;

		//merge the original manifest with the generated one
		if (builtIn != null) {
			Enumeration<String> keysEnum = builtIn.keys();
			while (keysEnum.hasMoreElements()) {
				String key = keysEnum.nextElement();
				generatedManifest.put(key, builtIn.get(key));
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

	private boolean isComplete(Dictionary<String, String> manifest) {
		// a manifest is complete if it has a Bundle-SymbolicName entry...
		if (manifest.get(org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME) != null)
			return true;
		// ...or it does not have a plugin/fragment manifest where to get the other entries from  
		return bundledata.getEntry(PluginConverterImpl.PLUGIN_MANIFEST) == null && bundledata.getEntry(PluginConverterImpl.FRAGMENT_MANIFEST) == null;
	}

	public BaseData getBaseData() {
		return bundledata;
	}

	public void copy(StorageHook storageHook) {
		// copy nothing all must be re-read from a manifest
	}

	public void validate() throws IllegalArgumentException {
		checkTimeStamp();
	}

	public FrameworkAdaptor getAdaptor() {
		if (bundledata != null)
			return bundledata.getAdaptor();
		return null;
	}

	public Dictionary<String, String> getManifest(boolean firstLoad) throws BundleException {
		return createCachedManifest(firstLoad);
	}

	public boolean forgetStatusChange(int status) {
		return false;
	}

	public boolean forgetStartLevelChange(int startlevel) {
		return false;
	}
}
