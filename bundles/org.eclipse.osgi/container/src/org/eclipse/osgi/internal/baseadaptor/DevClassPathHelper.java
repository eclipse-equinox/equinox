/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.baseadaptor;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Properties;
import org.eclipse.osgi.framework.internal.core.FrameworkProperties;
import org.eclipse.osgi.util.ManifestElement;

/**
 * This class provides helper methods to support developement classpaths.
 * @since 3.1
 */
public final class DevClassPathHelper {
	static final private String FILE_PROTOCOL = "file"; //$NON-NLS-1$
	static final private boolean inDevelopmentMode;
	static final private File devLocation;
	static private String[] devDefaultClasspath;
	static private Dictionary<String, String> devProperties = null;
	// timestamp for the dev.properties file
	static private long lastModified = 0;

	static {
		String osgiDev = FrameworkProperties.getProperty("osgi.dev"); //$NON-NLS-1$
		File f = null;
		boolean devMode = false;
		if (osgiDev != null) {
			try {
				devMode = true;
				URL location = new URL(osgiDev);

				if (FILE_PROTOCOL.equals(location.getProtocol())) {
					f = new File(location.getFile());
					lastModified = f.lastModified();
				}

				// Check the osgi.dev property to see if dev classpath entries have been defined.
				try {
					load(location.openStream());
					devMode = true;
				} catch (IOException e) {
					// TODO consider logging
				}

			} catch (MalformedURLException e) {
				devDefaultClasspath = getArrayFromList(osgiDev);
			}
		}
		inDevelopmentMode = devMode;
		devLocation = f;
	}

	/*
	 * Updates the dev classpath if the file containing the entries have changed
	 */
	private static void updateDevProperties() {
		if (devLocation == null)
			return;
		if (devLocation.lastModified() == lastModified)
			return;

		try {
			load(new FileInputStream(devLocation));
		} catch (FileNotFoundException e) {
			return;
		}
		lastModified = devLocation.lastModified();
	}

	private static String[] getDevClassPath(String id, Dictionary<String, String> properties, String[] defaultClasspath) {
		String[] result = null;
		if (id != null && properties != null) {
			String entry = properties.get(id);
			if (entry != null)
				result = getArrayFromList(entry);
		}
		if (result == null)
			result = defaultClasspath;
		return result;
	}

	/**
	 * Returns a list of classpath elements for the specified bundle symbolic name.
	 * @param id a bundle symbolic name to get the development classpath for
	 * @param properties a Dictionary of properties to use or <code>null</code> if
	 * the default develoment classpath properties should be used
	 * @return a list of development classpath elements
	 */
	public static String[] getDevClassPath(String id, Dictionary<String, String> properties) {
		if (properties == null) {
			synchronized (DevClassPathHelper.class) {
				updateDevProperties();
				return getDevClassPath(id, devProperties, devDefaultClasspath);
			}
		}
		return getDevClassPath(id, properties, getArrayFromList(properties.get("*"))); //$NON-NLS-1$
	}

	/**
	 * Returns a list of classpath elements for the specified bundle symbolic name.
	 * @param id a bundle symbolic name to get the development classpath for
	 * @return a list of development classpath elements
	 */
	public static String[] getDevClassPath(String id) {
		return getDevClassPath(id, null);
	}

	/**
	 * Returns the result of converting a list of comma-separated tokens into an array
	 * 
	 * @return the array of string tokens
	 * @param prop the initial comma-separated string
	 */
	public static String[] getArrayFromList(String prop) {
		return ManifestElement.getArrayFromList(prop, ","); //$NON-NLS-1$
	}

	/**
	 * Indicates the development mode.
	 * @return true if in development mode; false otherwise
	 */
	public static boolean inDevelopmentMode() {
		return inDevelopmentMode;
	}

	/*
	 * Load the given input stream into a dictionary
	 */
	private static void load(InputStream input) {
		Properties props = new Properties();
		try {
			props.load(input);
		} catch (IOException e) {
			// TODO consider logging here
		} finally {
			if (input != null)
				try {
					input.close();
				} catch (IOException e) {
					// tried our best
				}
		}
		@SuppressWarnings({"unchecked", "rawtypes"})
		Dictionary<String, String> result = (Dictionary) props;
		devProperties = result;
		if (devProperties != null)
			devDefaultClasspath = getArrayFromList(devProperties.get("*")); //$NON-NLS-1$
	}
}
