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
	private Dictionary defaultLocaleHeaders = null;

	public ManifestLocalization(AbstractBundle bundle, Dictionary rawHeaders) {
		this.bundle = bundle;
		this.rawHeaders = rawHeaders;
	}

	protected Dictionary getHeaders(String localeString) {
		boolean defaultLocale = false;
		if (localeString == "") { //$NON-NLS-1$
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
			return rawHeaders;
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
					// Do nothing; just use the raw value
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
		String propertiesLocation = (String) rawHeaders.get(Constants.BUNDLE_LOCALIZATION);
		if (propertiesLocation == null) {
			propertiesLocation = Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME;
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
			return null;
		} finally {
			if (resourceStream != null) {
				try {
					resourceStream.close();
				} catch (IOException e3) {
					//Ignore exception
				}
			}
		}
		return resourceBundle;
	}

	/*
	 * This method searchs for properties file the same way the ResourceBundle
	 * algorithm.
	 */
	private URL findProperties(String localeString, String path) {
		String[] nlVariants = buildNLVariants(localeString);
		if (bundle.isResolved()) {
			AbstractBundle bundleHost;
			if (bundle.isFragment()) {
				//if the bundle is a fragment, look in the host first
				bundleHost = (AbstractBundle) bundle.getHost();
			} else {
				//if the bundle is not a fragment, look in the bundle itself,
				//then the attached fragments
				bundleHost = bundle;
			}
			URL result;
			for (int i = 0; i < nlVariants.length; i++) {
				String filePath = path.concat('_' + nlVariants[i] + ".properties"); //$NON-NLS-1$
				result = findInResolved(filePath, bundleHost);
				if (result != null)
					return result;
			}
			//If we get to this point, we haven't found it yet.
			// Look for the base filename
			String filename = path + ".properties"; //$NON-NLS-1$
			return findInResolved(filename, bundleHost);
		} else {
			//only look in the bundle if the bundle is not resolved
			for (int i = 0; i < nlVariants.length; i++) {
				String filePath = path.concat('_' + nlVariants[i] + ".properties"); //$NON-NLS-1$
				return findInBundle(filePath, bundle);
			}
			//If we get to this point, we haven't found it yet.
			// Look for the base filename
			String filename = path + ".properties"; //$NON-NLS-1$
			return findInBundle(filename, bundle);
		}
	}

	private URL findInResolved(String filePath, AbstractBundle bundleHost) {

		URL result = findInBundle(filePath, bundleHost);
		if (result != null)
			return result;
		return findInFragments(filePath, bundleHost);
	}

	private URL findInBundle(String filePath, AbstractBundle searchBundle) {
		return searchBundle.getEntry(filePath);
	}

	private URL findInFragments(String filePath, AbstractBundle searchBundle) {
		org.osgi.framework.Bundle[] fragments = searchBundle.getFragments();
		URL fileURL = null;
		for (int i = 0; fragments != null && i < fragments.length && fileURL == null; i++) {
			fileURL = fragments[i].getEntry(filePath);
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
					value = ""; //$NON-NLS-1$
				}
			}
			dictionary.put(key, value);
		}
		return (dictionary);
	}
}