/*******************************************************************************
 * Copyright (c) 2004, 2011 IBM Corporation and others.
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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import org.eclipse.core.runtime.internal.adaptor.*;
import org.eclipse.osgi.framework.adaptor.FrameworkAdaptor;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.framework.internal.core.FrameworkProperties;
import org.eclipse.osgi.internal.baseadaptor.AdaptorUtil;
import org.eclipse.osgi.service.datalocation.Location;

/**
 * This class is used to manage the various Locations for Eclipse.
 * <p>
 * Clients may not extend this class.
 * </p>
 * @since 3.1
 * @noextend This class is not intended to be subclassed by clients.
 */
public class LocationManager {
	private static Location installLocation = null;
	private static Location configurationLocation = null;
	private static Location userLocation = null;
	private static Location instanceLocation = null;
	private static Location eclipseHomeLocation = null;

	public static final String READ_ONLY_AREA_SUFFIX = ".readOnly"; //$NON-NLS-1$
	public static final String PROP_INSTALL_AREA = "osgi.install.area"; //$NON-NLS-1$
	public static final String PROP_CONFIG_AREA = "osgi.configuration.area"; //$NON-NLS-1$
	public static final String PROP_CONFIG_AREA_DEFAULT = "osgi.configuration.area.default"; //$NON-NLS-1$
	public static final String PROP_SHARED_CONFIG_AREA = "osgi.sharedConfiguration.area"; //$NON-NLS-1$
	public static final String PROP_INSTANCE_AREA = "osgi.instance.area"; //$NON-NLS-1$
	public static final String PROP_INSTANCE_AREA_DEFAULT = "osgi.instance.area.default"; //$NON-NLS-1$
	public static final String PROP_USER_AREA = "osgi.user.area"; //$NON-NLS-1$
	public static final String PROP_USER_AREA_DEFAULT = "osgi.user.area.default"; //$NON-NLS-1$
	public static final String PROP_MANIFEST_CACHE = "osgi.manifest.cache"; //$NON-NLS-1$
	public static final String PROP_USER_HOME = "user.home"; //$NON-NLS-1$
	public static final String PROP_USER_DIR = "user.dir"; //$NON-NLS-1$
	public static final String PROP_HOME_LOCATION_AREA = "eclipse.home.location"; //$NON-NLS-1$
	static final String PROP_LAUNCHER = "eclipse.launcher"; //$NON-NLS-1$

	// configuration area file/dir names
	public static final String BUNDLES_DIR = "bundles"; //$NON-NLS-1$
	public static final String STATE_FILE = ".state"; //$NON-NLS-1$
	public static final String LAZY_FILE = ".lazy"; //$NON-NLS-1$
	public static final String BUNDLE_DATA_FILE = ".bundledata"; //$NON-NLS-1$
	public static final String MANIFESTS_DIR = "manifests"; //$NON-NLS-1$
	public static final String CONFIG_FILE = "config.ini"; //$NON-NLS-1$
	public static final String ECLIPSE_PROPERTIES = "eclipse.properties"; //$NON-NLS-1$

	// Constants for configuration location discovery
	private static final String ECLIPSE = "eclipse"; //$NON-NLS-1$
	private static final String PRODUCT_SITE_MARKER = ".eclipseproduct"; //$NON-NLS-1$
	private static final String PRODUCT_SITE_ID = "id"; //$NON-NLS-1$
	private static final String PRODUCT_SITE_VERSION = "version"; //$NON-NLS-1$

	private static final String CONFIG_DIR = "configuration"; //$NON-NLS-1$

	// Data mode constants for user, configuration and data locations.
	private static final String NONE = "@none"; //$NON-NLS-1$
	private static final String NO_DEFAULT = "@noDefault"; //$NON-NLS-1$
	private static final String USER_HOME = "@user.home"; //$NON-NLS-1$
	private static final String USER_DIR = "@user.dir"; //$NON-NLS-1$
	// Placeholder for hashcode of installation directory
	private static final String INSTALL_HASH_PLACEHOLDER = "@install.hash"; //$NON-NLS-1$

	private static final String INSTANCE_DATA_AREA_PREFIX = ".metadata/.plugins/"; //$NON-NLS-1$

	/**
	 * Builds a URL with the given specification
	 * @param spec the URL specification
	 * @param trailingSlash flag to indicate a trailing slash on the spec
	 * @return a URL
	 */
	public static URL buildURL(String spec, boolean trailingSlash) {
		return LocationHelper.buildURL(spec, trailingSlash);
	}

	private static void mungeConfigurationLocation() {
		// if the config property was set, munge it for backwards compatibility.
		String location = FrameworkProperties.getProperty(PROP_CONFIG_AREA);
		if (location != null) {
			if (location.endsWith(".cfg")) { //$NON-NLS-1$
				int index = location.lastIndexOf('/');
				if (index < 0)
					index = location.lastIndexOf('\\');
				location = location.substring(0, index + 1);
				FrameworkProperties.setProperty(PROP_CONFIG_AREA, location);
			}
		}
	}

	/**
	 * Initializes the Location objects for the LocationManager.
	 */
	public static void initializeLocations() {
		// set the osgi storage area if it exists
		String osgiStorage = FrameworkProperties.getProperty(Constants.FRAMEWORK_STORAGE);
		if (osgiStorage != null)
			FrameworkProperties.setProperty(PROP_CONFIG_AREA, osgiStorage);
		// do install location initialization first since others may depend on it
		// assumes that the property is already set
		installLocation = buildLocation(PROP_INSTALL_AREA, null, "", true, false, null); //$NON-NLS-1$

		// TODO not sure what the data area prefix should be here for the user area
		Location temp = buildLocation(PROP_USER_AREA_DEFAULT, null, "", false, false, null); //$NON-NLS-1$
		URL defaultLocation = temp == null ? null : temp.getURL();
		if (defaultLocation == null)
			defaultLocation = buildURL(new File(FrameworkProperties.getProperty(PROP_USER_HOME), "user").getAbsolutePath(), true); //$NON-NLS-1$
		userLocation = buildLocation(PROP_USER_AREA, defaultLocation, "", false, false, null); //$NON-NLS-1$

		temp = buildLocation(PROP_INSTANCE_AREA_DEFAULT, null, "", false, false, INSTANCE_DATA_AREA_PREFIX); //$NON-NLS-1$
		defaultLocation = temp == null ? null : temp.getURL();
		if (defaultLocation == null)
			defaultLocation = buildURL(new File(FrameworkProperties.getProperty(PROP_USER_DIR), "workspace").getAbsolutePath(), true); //$NON-NLS-1$
		instanceLocation = buildLocation(PROP_INSTANCE_AREA, defaultLocation, "", false, false, INSTANCE_DATA_AREA_PREFIX); //$NON-NLS-1$

		mungeConfigurationLocation();
		// compute a default but it is very unlikely to be used since main will have computed everything
		temp = buildLocation(PROP_CONFIG_AREA_DEFAULT, null, "", false, false, null); //$NON-NLS-1$
		defaultLocation = temp == null ? null : temp.getURL();
		if (defaultLocation == null && FrameworkProperties.getProperty(PROP_CONFIG_AREA) == null)
			// only compute the default if the configuration area property is not set
			defaultLocation = buildURL(computeDefaultConfigurationLocation(), true);
		configurationLocation = buildLocation(PROP_CONFIG_AREA, defaultLocation, "", false, false, null); //$NON-NLS-1$
		// get the parent location based on the system property. This will have been set on the 
		// way in either by the caller/user or by main.  There will be no parent location if we are not 
		// cascaded.
		URL parentLocation = computeSharedConfigurationLocation();
		if (parentLocation != null && !parentLocation.equals(configurationLocation.getURL())) {
			Location parent = new BasicLocation(null, parentLocation, true, null);
			((BasicLocation) configurationLocation).setParent(parent);
		}
		initializeDerivedConfigurationLocations();

		if (FrameworkProperties.getProperty(PROP_HOME_LOCATION_AREA) == null) {
			String eclipseLauncher = FrameworkProperties.getProperty(PROP_LAUNCHER);
			String eclipseHomeLocationPath = getEclipseHomeLocation(eclipseLauncher);
			if (eclipseHomeLocationPath != null)
				FrameworkProperties.setProperty(PROP_HOME_LOCATION_AREA, eclipseHomeLocationPath);
		}
		// if eclipse.home.location is not set then default to osgi.install.area
		if (FrameworkProperties.getProperty(PROP_HOME_LOCATION_AREA) == null && FrameworkProperties.getProperty(PROP_INSTALL_AREA) != null)
			FrameworkProperties.setProperty(PROP_HOME_LOCATION_AREA, FrameworkProperties.getProperty(PROP_INSTALL_AREA));
		eclipseHomeLocation = buildLocation(PROP_HOME_LOCATION_AREA, null, "", true, true, null); //$NON-NLS-1$
	}

	private static String getEclipseHomeLocation(String launcher) {
		if (launcher == null)
			return null;
		File launcherFile = new File(launcher);
		if (launcherFile.getParent() == null)
			return null;
		File launcherDir = new File(launcherFile.getParent());
		// check for mac os; the os check is copied from EclipseEnvironmentInfo.
		String macosx = org.eclipse.osgi.service.environment.Constants.OS_MACOSX;
		if (macosx.equals(EclipseEnvironmentInfo.getDefault().getOS()))
			launcherDir = getMacOSEclipsoeHomeLocation(launcherDir);
		return (launcherDir.exists() && launcherDir.isDirectory()) ? launcherDir.getAbsolutePath() : null;
	}

	private static File getMacOSEclipsoeHomeLocation(File launcherDir) {
		// TODO for now we go up three directories from the launcher dir as long as the parent dir is named MacOS; is this always the case?
		// TODO not sure if case is important
		if (!launcherDir.getName().equalsIgnoreCase("macos")) //$NON-NLS-1$
			return launcherDir; // don't do the up three stuff if not in macos directory
		String launcherParent = launcherDir.getParent();
		if (launcherParent != null)
			launcherParent = new File(launcherParent).getParent();
		if (launcherParent != null)
			launcherParent = new File(launcherParent).getParent();
		return launcherParent == null ? null : new File(launcherParent);
	}

	@SuppressWarnings("deprecation")
	private static Location buildLocation(String property, URL defaultLocation, String userDefaultAppendage, boolean readOnlyDefault, boolean computeReadOnly, String dataAreaPrefix) {
		String location = FrameworkProperties.clearProperty(property);
		// the user/product may specify a non-default readOnly setting   
		String userReadOnlySetting = FrameworkProperties.getProperty(property + READ_ONLY_AREA_SUFFIX);
		boolean readOnly = (userReadOnlySetting == null ? readOnlyDefault : Boolean.valueOf(userReadOnlySetting).booleanValue());
		// if the instance location is not set, predict where the workspace will be and 
		// put the instance area inside the workspace meta area.
		if (location == null)
			return new BasicLocation(property, defaultLocation, userReadOnlySetting != null || !computeReadOnly ? readOnly : !canWrite(defaultLocation), dataAreaPrefix);
		String trimmedLocation = location.trim();
		if (trimmedLocation.equalsIgnoreCase(NONE))
			return null;
		if (trimmedLocation.equalsIgnoreCase(NO_DEFAULT))
			return new BasicLocation(property, null, readOnly, dataAreaPrefix);
		if (trimmedLocation.startsWith(USER_HOME)) {
			String base = substituteVar(location, USER_HOME, PROP_USER_HOME);
			location = new File(base, userDefaultAppendage).getAbsolutePath();
		} else if (trimmedLocation.startsWith(USER_DIR)) {
			String base = substituteVar(location, USER_DIR, PROP_USER_DIR);
			location = new File(base, userDefaultAppendage).getAbsolutePath();
		}
		int idx = location.indexOf(INSTALL_HASH_PLACEHOLDER);
		if (idx == 0) {
			throw new RuntimeException("The location cannot start with '" + INSTALL_HASH_PLACEHOLDER + "': " + location); //$NON-NLS-1$ //$NON-NLS-2$
		} else if (idx > 0) {
			location = location.substring(0, idx) + getInstallDirHash() + location.substring(idx + INSTALL_HASH_PLACEHOLDER.length());
		}
		URL url = buildURL(location, true);
		BasicLocation result = null;
		if (url != null) {
			result = new BasicLocation(property, null, userReadOnlySetting != null || !computeReadOnly ? readOnly : !canWrite(url), dataAreaPrefix);
			result.setURL(url, false);
		}
		return result;
	}

	private static String substituteVar(String source, String var, String prop) {
		String value = FrameworkProperties.getProperty(prop, ""); //$NON-NLS-1$
		return value + source.substring(var.length());
	}

	private static void initializeDerivedConfigurationLocations() {
		if (FrameworkProperties.getProperty(PROP_MANIFEST_CACHE) == null)
			FrameworkProperties.setProperty(PROP_MANIFEST_CACHE, getConfigurationFile(MANIFESTS_DIR).getAbsolutePath());
	}

	private static URL computeInstallConfigurationLocation() {
		String property = FrameworkProperties.getProperty(PROP_INSTALL_AREA);
		if (property != null)
			return LocationHelper.buildURL(property, true);
		return null;
	}

	private static URL computeSharedConfigurationLocation() {
		String property = FrameworkProperties.getProperty(PROP_SHARED_CONFIG_AREA);
		if (property == null)
			return null;
		try {
			URL sharedConfigurationURL = LocationHelper.buildURL(property, true);
			if (sharedConfigurationURL == null)
				return null;
			if (sharedConfigurationURL.getPath().startsWith("/")) //$NON-NLS-1$
				// absolute
				return sharedConfigurationURL;
			URL installURL = installLocation.getURL();
			if (!sharedConfigurationURL.getProtocol().equals(installURL.getProtocol()))
				// different protocol
				return sharedConfigurationURL;
			sharedConfigurationURL = new URL(installURL, sharedConfigurationURL.getPath());
			FrameworkProperties.setProperty(PROP_SHARED_CONFIG_AREA, sharedConfigurationURL.toExternalForm());
		} catch (MalformedURLException e) {
			// do nothing here since it is basically impossible to get a bogus url 
		}
		return null;
	}

	private static String computeDefaultConfigurationLocation() {
		// 1) We store the config state relative to the 'eclipse' directory if possible
		// 2) If this directory is read-only 
		//    we store the state in <user.home>/.eclipse/<application-id>_<version> where <user.home> 
		//    is unique for each local user, and <application-id> is the one 
		//    defined in .eclipseproduct marker file. If .eclipseproduct does not
		//    exist, use "eclipse" as the application-id.

		URL installURL = computeInstallConfigurationLocation();
		if (installURL != null && "file".equals(installURL.getProtocol())) { //$NON-NLS-1$
			File installDir = new File(installURL.getFile());
			File defaultConfigDir = new File(installDir, CONFIG_DIR);
			if (!defaultConfigDir.exists())
				defaultConfigDir.mkdirs();
			if (defaultConfigDir.exists() && AdaptorUtil.canWrite(defaultConfigDir))
				return defaultConfigDir.getAbsolutePath();
		}
		// We can't write in the eclipse install dir so try for some place in the user's home dir
		return computeDefaultUserAreaLocation(CONFIG_DIR);
	}

	private static boolean canWrite(URL location) {
		if (location != null && "file".equals(location.getProtocol())) { //$NON-NLS-1$
			File locationDir = new File(location.getFile());
			if (!locationDir.exists())
				locationDir.mkdirs();
			if (locationDir.exists() && AdaptorUtil.canWrite(locationDir))
				return true;
		}
		return false;
	}

	private static String computeDefaultUserAreaLocation(String pathAppendage) {
		//    we store the state in <user.home>/.eclipse/<application-id>_<version> where <user.home> 
		//    is unique for each local user, and <application-id> is the one 
		//    defined in .eclipseproduct marker file. If .eclipseproduct does not
		//    exist, use "eclipse" as the application-id.
		String installProperty = FrameworkProperties.getProperty(PROP_INSTALL_AREA);
		URL installURL = buildURL(installProperty, true);
		if (installURL == null)
			return null;
		File installDir = new File(installURL.getFile());
		String installDirHash = getInstallDirHash();

		String appName = "." + ECLIPSE; //$NON-NLS-1$
		File eclipseProduct = new File(installDir, PRODUCT_SITE_MARKER);
		if (eclipseProduct.exists()) {
			Properties props = new Properties();
			try {
				props.load(new FileInputStream(eclipseProduct));
				String appId = props.getProperty(PRODUCT_SITE_ID);
				if (appId == null || appId.trim().length() == 0)
					appId = ECLIPSE;
				String appVersion = props.getProperty(PRODUCT_SITE_VERSION);
				if (appVersion == null || appVersion.trim().length() == 0)
					appVersion = ""; //$NON-NLS-1$
				appName += File.separator + appId + "_" + appVersion + "_" + installDirHash; //$NON-NLS-1$ //$NON-NLS-2$
			} catch (IOException e) {
				// Do nothing if we get an exception.  We will default to a standard location 
				// in the user's home dir.
				// add the hash to help prevent collisions
				appName += File.separator + installDirHash;
			}
		} else {
			// add the hash to help prevent collisions
			appName += File.separator + installDirHash;
		}
		String userHome = FrameworkProperties.getProperty(PROP_USER_HOME);
		return new File(userHome, appName + "/" + pathAppendage).getAbsolutePath(); //$NON-NLS-1$
	}

	/**
	 * Return hash code identifying an absolute installation path
	 * @return hash code as String
	 */
	private static String getInstallDirHash() {
		// compute an install dir hash to prevent configuration area collisions with other eclipse installs
		String installProperty = FrameworkProperties.getProperty(PROP_INSTALL_AREA);
		URL installURL = buildURL(installProperty, true);
		if (installURL == null)
			return ""; //$NON-NLS-1$
		File installDir = new File(installURL.getFile());
		int hashCode;
		try {
			hashCode = installDir.getCanonicalPath().hashCode();
		} catch (IOException ioe) {
			// fall back to absolute path
			hashCode = installDir.getAbsolutePath().hashCode();
		}
		if (hashCode < 0)
			hashCode = -(hashCode);
		String installDirHash = String.valueOf(hashCode);
		return installDirHash;
	}

	/**
	 * Returns the user Location object
	 * @return the user Location object
	 */
	public static Location getUserLocation() {
		return userLocation;
	}

	/**
	 * Returns the configuration Location object
	 * @return the configuration Location object
	 */
	public static Location getConfigurationLocation() {
		return configurationLocation;
	}

	/**
	 * Returns the install Location object
	 * @return the install Location object
	 */
	public static Location getInstallLocation() {
		return installLocation;
	}

	/**
	 * Returns the instance Location object
	 * @return the instance Location object
	 */
	public static Location getInstanceLocation() {
		return instanceLocation;
	}

	public static Location getEclipseHomeLocation() {
		return eclipseHomeLocation;
	}

	/**
	 * Returns the File object under the configuration location used for the OSGi configuration
	 * @return the OSGi configuration directory
	 */
	public static File getOSGiConfigurationDir() {
		// TODO assumes the URL is a file: url
		return new File(configurationLocation.getURL().getFile(), FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME);
	}

	/**
	 * Returns a file from the configuration area that can be used by the framework
	 * @param filename the filename
	 * @return a file from the configuration area
	 */
	public static File getConfigurationFile(String filename) {
		File dir = getOSGiConfigurationDir();
		if (!dir.exists())
			dir.mkdirs();
		return new File(dir, filename);
	}
}
