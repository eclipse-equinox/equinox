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
package org.eclipse.core.runtime.adaptor;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import org.eclipse.osgi.service.datalocation.Location;

public class LocationManager {
	private static Location configurationLocation = null;
	private static Location userLocation = null;
	private static Location instanceLocation = null;

	public static final String PROP_INSTALL_LOCATION = "osgi.installLocation"; //$NON-NLS-1$
	public static final String PROP_CONFIG_AREA = "osgi.configuration.area"; //$NON-NLS-1$
	public static final String PROP_INSTANCE_AREA = "osgi.instance.area"; //$NON-NLS-1$
	public static final String PROP_USER_AREA = "osgi.user.area"; //$NON-NLS-1$
	public static final String PROP_MANIFEST_CACHE = "osgi.manifest.cache"; //$NON-NLS-1$
	
	// Constants for configuration location discovery
	private static final String ECLIPSE = "eclipse"; //$NON-NLS-1$
	private static final String PRODUCT_SITE_MARKER = ".eclipseproduct"; //$NON-NLS-1$
	private static final String PRODUCT_SITE_ID = "id"; //$NON-NLS-1$
	private static final String PRODUCT_SITE_VERSION = "version"; //$NON-NLS-1$

	// Data mode constants for user, configuration and data locations.
	private static final String NONE = "<none>"; //$NON-NLS-1$
	private static final String NO_DEFAULT = "<noDefault>"; //$NON-NLS-1$
	private static final String USER_HOME = "<user.home>"; //$NON-NLS-1$
	private static final String USER_DIR = "<user.dir>"; //$NON-NLS-1$

	private static URL buildURL(String spec) {
		try {
			return new URL(spec);
		} catch (MalformedURLException e) {
			if (spec.startsWith("file:"))
				return null;
			return buildURL("file:" + spec);
		}
	}
	
	private static void mungeConfigurationLocation() {
		// if the config property was set, munge it for backwards compatibility.
		String location = System.getProperty(PROP_CONFIG_AREA);
		if (location != null) {
			location = buildURL(location).getFile();
			location = location.replace('\\', '/');
			int index = location.lastIndexOf('/');
			if (location.endsWith(".cfg") || location.endsWith("/")) 
				location = location.substring(0, index);
			System.getProperties().put(PROP_CONFIG_AREA, location);
		} 
	}

	public static void initializeLocations() {
		URL defaultLocation =  buildURL(System.getProperty("user.home"));
		userLocation = buildLocation(PROP_USER_AREA, defaultLocation, "user");

		defaultLocation = buildURL(new File(System.getProperty("user.dir"), "workspace").getAbsolutePath());  //$NON-NLS-1$ //$NON-NLS-2$
		instanceLocation = buildLocation(PROP_INSTANCE_AREA, defaultLocation, "workspace");
		
		mungeConfigurationLocation();
		defaultLocation = buildURL(computeDefaultConfigurationLocation());
		configurationLocation = buildLocation(PROP_CONFIG_AREA, defaultLocation, ".config");
		initializeDerivedConfigurationLocations(configurationLocation.getURL());
	}

	private static Location buildLocation(String property, URL defaultLocation, String userDefaultAppendage) {
		BasicLocation result = null;
		String location = System.getProperty(property);
		System.getProperties().remove(property);
		// if the instance location is not set, predict where the workspace will be and 
		// put the instance area inside the workspace meta area.
		if (location == null) 
			result = new BasicLocation(property, defaultLocation, false);
		else if (location.equalsIgnoreCase(NONE))
			return null;
		else if (location.equalsIgnoreCase(NO_DEFAULT))
			result = new BasicLocation(property, null, false);
		else {
			if (location.equalsIgnoreCase(USER_HOME)) 
				location = computeDefaultUserAreaLocation(userDefaultAppendage);
			if (location.equalsIgnoreCase(USER_DIR)) 
				location = new File(System.getProperty("user.dir"), userDefaultAppendage).getAbsolutePath();
			URL url = buildURL(location);
			if (url != null) {
				result = new BasicLocation(property, null, false);
				result.setURL(url);
			}
		}
		return result;
	}

	private static void initializeDerivedConfigurationLocations(URL base) {
		// TODO assumes the base URL is a file:
		String location = base.getFile();
		System.getProperties().put("org.eclipse.osgi.framework.defaultadaptor.bundledir", location + "/bundles");	
		if (System.getProperty(PROP_MANIFEST_CACHE) == null)
			System.getProperties().put(PROP_MANIFEST_CACHE, location + "/manifests");
	}
	
	private static String computeDefaultConfigurationLocation() {
		// 1) We store the config state relative to the 'eclipse' directory if possible
		// 2) If this directory is read-only 
		//    we store the state in <user.home>/.eclipse/<application-id>_<version> where <user.home> 
		//    is unique for each local user, and <application-id> is the one 
		//    defined in .eclipseproduct marker file. If .eclipseproduct does not
		//    exist, use "eclipse" as the application-id.
		
		String installProperty = System.getProperty(PROP_INSTALL_LOCATION);
		URL installURL = null;
		try {
			installURL = new URL(installProperty);
		} catch (MalformedURLException e) {
			// do nothgin here since it is basically impossible to get a bogus url 
		}
		File installDir = new File(installURL.getFile());
		if ("file".equals(installURL.getProtocol()) && installDir.canWrite()) { //$NON-NLS-1$
//			if (debug)
//				debug("Using the installation directory."); //$NON-NLS-1$
			return new File(installDir, ".config").getAbsolutePath();
		}

		// We can't write in the eclipse install dir so try for some place in the user's home dir
//		if (debug)
//			debug("Using the user.home location."); //$NON-NLS-1$
		return computeDefaultUserAreaLocation(".config");
	}

	private static String computeDefaultUserAreaLocation(String pathAppendage) {
		//    we store the state in <user.home>/.eclipse/<application-id>_<version> where <user.home> 
		//    is unique for each local user, and <application-id> is the one 
		//    defined in .eclipseproduct marker file. If .eclipseproduct does not
		//    exist, use "eclipse" as the application-id.
		String installProperty = System.getProperty(PROP_INSTALL_LOCATION);
		URL installURL = buildURL(installProperty);
		File installDir = new File(installURL.getFile());
		String appName = "." + ECLIPSE; //$NON-NLS-1$
		File eclipseProduct = new File(installDir, PRODUCT_SITE_MARKER );
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
				appName += File.separator + appId + "_" + appVersion; //$NON-NLS-1$
			} catch (IOException e) {
				// Do nothing if we get an exception.  We will default to a standard location 
				// in the user's home dir.
			}
		}
		String userHome = System.getProperty("user.home"); //$NON-NLS-1$
		return new File(userHome, appName + "/" + pathAppendage).getAbsolutePath();
	}
	
	public static Location getUserLocation() {
		return userLocation;
	}
	public static Location getConfigurationLocation() {
		return configurationLocation;
	}
	public static Location getInstanceLocation() {
		return instanceLocation;
	}
}
