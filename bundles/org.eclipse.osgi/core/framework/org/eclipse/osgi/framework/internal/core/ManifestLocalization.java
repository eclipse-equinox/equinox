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
package org.eclipse.osgi.framework.internal.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import org.eclipse.osgi.framework.util.Headers;
import org.osgi.framework.Constants;

/**
 * This class is used by the Bundle Class to localize manifest headers.
 */
public class ManifestLocalization {
	private AbstractBundle bundle = null;
	private Dictionary rawHeaders = null;
	private Headers defaultLocaleHeaders = null;

	public ManifestLocalization(AbstractBundle bundle, Dictionary rawHeaders) {
		this.bundle = bundle;
		this.rawHeaders = rawHeaders;
	}

	protected Dictionary getHeaders(String localeString) {
		boolean defaultLocale = false;
		if (localeString == "") {
			return (rawHeaders);
		}
		if (localeString.equals(Locale.getDefault().toString())) {
			if (defaultLocaleHeaders != null) {
				return (defaultLocaleHeaders);
			} else {
				defaultLocale = true;
			}
		}
		try {
			bundle.checkValid();
		} catch (IllegalStateException ex) {
			return (rawHeaders);
		}
		ResourceBundle localeProperties = null;
		localeProperties = getResourceBundle(localeString);
		if (localeProperties == null) {
			//			TODO Need to strip off leading percents. Can do it when
			//Eclipse no uses Bundle.getHeaders("") instead of
			// Bundle.getHeaders()
			//	Dictionary localeHeaders = stripPercents(new Hashtable(
			//			this.rawHeaders.size()));
			//if (defaultLocale) {
			//	defaultLocaleHeaders = localeHeaders;
			//}
			//return (localeHeaders);
			return (rawHeaders);
		}
		Enumeration e = this.rawHeaders.keys();
		Headers localeHeaders = new Headers(this.rawHeaders.size());
		while (e.hasMoreElements()) {
			String key = (String) e.nextElement();
			String value = (String) this.rawHeaders.get(key);
			if (value.startsWith("%") && (value.length() > 1)) { //$NON-NLS-1$
				String propertiesKey = value.substring(1);
				try {
					String transValue = (String) localeProperties.getObject(propertiesKey);
					value = transValue;
				} catch (MissingResourceException ex) {
					//strip the leading percent and return the raw value
					value = propertiesKey;
				}
			}
			localeHeaders.set(key, value);
		}
		if (defaultLocale) {
			defaultLocaleHeaders = localeHeaders;
		}
		return (localeHeaders);
	}

	private String[] buildNLVariants(String nl) {
		ArrayList result = new ArrayList();
		int lastSeparator;
		while ((lastSeparator = nl.lastIndexOf('_')) != -1) {
			result.add(nl);
			if (lastSeparator != -1) {
				nl = nl.substring(0, lastSeparator);
			}
		}
		result.add(nl);
		return (String[]) result.toArray(new String[result.size()]);
	}

	/*
	 * This method find the appropiate Manifest Localization file inside the
	 * bundle. If not found, return null.
	 */
	protected ResourceBundle getResourceBundle(String localeString) {
		URL resourceURL = null;
		String propertiesLocation = (String) rawHeaders.get(Constants.BUNDLE_MANIFEST_LOCALIZATION);
		if (propertiesLocation == null) {
			propertiesLocation = Constants.BUNDLE_DEFAULT_MANIFEST_LOCALIZATION;
		}
		resourceURL = findProperties(localeString, propertiesLocation);
		if (resourceURL == null) {
			return null;
		}
		ResourceBundle resourceBundle = null;
		InputStream resourceStream = null;
		try {
			resourceStream = resourceURL.openStream();
			resourceBundle = new PropertyResourceBundle(resourceStream);
		} catch (IOException e2) {
			if (resourceStream != null) {
				try {
					resourceStream.close();
				} catch (IOException e3) {
					//Ignore exception
				}
			}
			return null;
		}
		return resourceBundle;
	}

	/*
	 * This method searchs for properties file the same way the ResourceBundle
	 * algorithm
	 */
	private URL findProperties(String localeString, String path) {
		String[] nlVariants = buildNLVariants(localeString);
		URL result = null;
		for (int i = 0; i < nlVariants.length; i++) {
			String filePath = path.concat("_" + nlVariants[i] + ".properties");
			result = findInBundle(filePath);
			if (result != null)
				return result;
			result = findInFragments(filePath);
			if (result != null)
				return result;
		}
		// If we get to this point, we haven't found it yet.
		// Look in the plugin and fragment root directories
		String filename = path + ".properties";
		result = findInBundle(filename);
		if (result != null)
			return result;
		return findInFragments(filename);
	}

	private URL findInBundle(String filePath) {
		return this.bundle.getEntry(filePath);
	}

	private URL findInFragments(String filePath) {
		org.osgi.framework.Bundle[] fragments = this.bundle.getFragments();
		URL fileURL = null;
		int i = 0;
		while (fragments != null && i < fragments.length && fileURL == null) {
			fileURL = fragments[i].getEntry(filePath);
			i++;
		}
		return fileURL;
	}

	private Dictionary stripPercents(Dictionary dictionary) {
		//strip out the '%'s and return the raw headers without '%'s
		Enumeration e = rawHeaders.keys();
		while (e.hasMoreElements()) {
			String key = (String) e.nextElement();
			String value = (String) rawHeaders.get(key);
			if (value.startsWith("%")) //$NON-NLS-1$
			{
				if (value.length() > 1) {
					value = value.substring(1);
				} else {
					value = "";
				}
			}
			dictionary.put(key, value);
		}
		return (dictionary);
	}
}