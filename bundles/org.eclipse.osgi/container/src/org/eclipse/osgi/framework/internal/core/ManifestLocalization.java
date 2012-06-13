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
package org.eclipse.osgi.framework.internal.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import org.eclipse.osgi.framework.util.Headers;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

/**
 * This class is used by the Bundle Class to localize manifest headers.
 */
public class ManifestLocalization {
	final static String DEFAULT_ROOT = FrameworkProperties.getProperty("equinox.root.locale", "en"); //$NON-NLS-1$ //$NON-NLS-2$
	private final AbstractBundle bundle;
	private final Dictionary<String, String> rawHeaders;
	private Dictionary<String, String> defaultLocaleHeaders = null;
	private final Hashtable<String, BundleResourceBundle> cache = new Hashtable<String, BundleResourceBundle>(5);

	public ManifestLocalization(AbstractBundle bundle, Dictionary<String, String> rawHeaders) {
		this.bundle = bundle;
		this.rawHeaders = rawHeaders;
	}

	Dictionary<String, String> getHeaders(String localeString) {
		if (localeString.length() == 0)
			return rawHeaders;
		boolean isDefaultLocale = localeString.equals(Locale.getDefault().toString());
		Dictionary<String, String> currentDefault = defaultLocaleHeaders;
		if (isDefaultLocale && currentDefault != null) {
			return currentDefault;
		}
		try {
			bundle.checkValid();
		} catch (IllegalStateException ex) {
			// defaultLocaleHeaders should have been initialized on uninstall
			if (currentDefault != null)
				return currentDefault;
			return rawHeaders;
		}
		ResourceBundle localeProperties = getResourceBundle(localeString, isDefaultLocale);
		Enumeration<String> e = this.rawHeaders.keys();
		Headers<String, String> localeHeaders = new Headers<String, String>(this.rawHeaders.size());
		while (e.hasMoreElements()) {
			String key = e.nextElement();
			String value = this.rawHeaders.get(key);
			if (value.startsWith("%") && (value.length() > 1)) { //$NON-NLS-1$
				String propertiesKey = value.substring(1);
				try {
					value = localeProperties == null ? propertiesKey : (String) localeProperties.getObject(propertiesKey);
				} catch (MissingResourceException ex) {
					value = propertiesKey;
				}
			}
			localeHeaders.set(key, value);
		}
		localeHeaders.setReadOnly();
		if (isDefaultLocale) {
			defaultLocaleHeaders = localeHeaders;
		}
		return (localeHeaders);
	}

	private String[] buildNLVariants(String nl) {
		List<String> result = new ArrayList<String>();
		while (nl.length() > 0) {
			result.add(nl);
			int i = nl.lastIndexOf('_');
			nl = (i < 0) ? "" : nl.substring(0, i); //$NON-NLS-1$
		}
		result.add(""); //$NON-NLS-1$
		return result.toArray(new String[result.size()]);
	}

	/*
	 * This method find the appropriate Manifest Localization file inside the
	 * bundle. If not found, return null.
	 */
	ResourceBundle getResourceBundle(String localeString, boolean isDefaultLocale) {
		BundleResourceBundle resourceBundle = lookupResourceBundle(localeString);
		if (isDefaultLocale)
			return (ResourceBundle) resourceBundle;
		// need to determine if this is resource bundle is an empty stem
		// if it is then the default locale should be used
		if (resourceBundle == null || resourceBundle.isStemEmpty())
			return (ResourceBundle) lookupResourceBundle(Locale.getDefault().toString());
		return (ResourceBundle) resourceBundle;
	}

	private BundleResourceBundle lookupResourceBundle(String localeString) {
		// get the localization header as late as possible to avoid accessing the raw headers
		// getting the first value from the raw headers forces the manifest to be parsed (bug 332039)
		String localizationHeader = rawHeaders.get(Constants.BUNDLE_LOCALIZATION);
		if (localizationHeader == null)
			localizationHeader = Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME;
		synchronized (cache) {
			BundleResourceBundle result = cache.get(localeString);
			if (result != null)
				return result.isEmpty() ? null : result;
			String[] nlVarients = buildNLVariants(localeString);
			BundleResourceBundle parent = null;
			for (int i = nlVarients.length - 1; i >= 0; i--) {
				BundleResourceBundle varientBundle = null;
				URL varientURL = findResource(localizationHeader + (nlVarients[i].equals("") ? nlVarients[i] : '_' + nlVarients[i]) + ".properties"); //$NON-NLS-1$ //$NON-NLS-2$
				if (varientURL == null) {
					varientBundle = cache.get(nlVarients[i]);
				} else {
					InputStream resourceStream = null;
					try {
						resourceStream = varientURL.openStream();
						varientBundle = new LocalizationResourceBundle(resourceStream);
					} catch (IOException e) {
						// ignore and continue
					} finally {
						if (resourceStream != null) {
							try {
								resourceStream.close();
							} catch (IOException e3) {
								//Ignore exception
							}
						}
					}
				}

				if (varientBundle == null) {
					varientBundle = new EmptyResouceBundle(nlVarients[i]);
				}
				if (parent != null)
					varientBundle.setParent((ResourceBundle) parent);
				cache.put(nlVarients[i], varientBundle);
				parent = varientBundle;
			}
			result = cache.get(localeString);
			return result.isEmpty() ? null : result;
		}
	}

	private URL findResource(String resource) {
		AbstractBundle searchBundle = bundle;
		if (bundle.isResolved()) {
			if (bundle.isFragment() && bundle.getHosts() != null) {
				//if the bundle is a fragment, look in the host first
				searchBundle = bundle.getHosts()[0];
				if (searchBundle.getState() == Bundle.UNINSTALLED)
					searchBundle = bundle;
			}
			return findInResolved(resource, searchBundle);
		}
		return searchBundle.getEntry0(resource);
	}

	private static URL findInResolved(String filePath, AbstractBundle bundleHost) {
		URL result = bundleHost.getEntry0(filePath);
		if (result != null)
			return result;
		return findInFragments(filePath, bundleHost);
	}

	private static URL findInFragments(String filePath, AbstractBundle searchBundle) {
		BundleFragment[] fragments = searchBundle.getFragments();
		URL fileURL = null;
		for (int i = 0; fragments != null && i < fragments.length && fileURL == null; i++) {
			if (fragments[i].getState() != Bundle.UNINSTALLED)
				fileURL = fragments[i].getEntry0(filePath);
		}
		return fileURL;
	}

	private interface BundleResourceBundle {
		void setParent(ResourceBundle parent);

		boolean isEmpty();

		boolean isStemEmpty();
	}

	private class LocalizationResourceBundle extends PropertyResourceBundle implements BundleResourceBundle {
		public LocalizationResourceBundle(InputStream in) throws IOException {
			super(in);
		}

		public void setParent(ResourceBundle parent) {
			super.setParent(parent);
		}

		public boolean isEmpty() {
			return false;
		}

		public boolean isStemEmpty() {
			return parent == null;
		}
	}

	class EmptyResouceBundle extends ResourceBundle implements BundleResourceBundle {
		private final String localeString;

		public EmptyResouceBundle(String locale) {
			super();
			this.localeString = locale;
		}

		@SuppressWarnings("unchecked")
		public Enumeration<String> getKeys() {
			return Collections.enumeration(Collections.EMPTY_LIST);
		}

		protected Object handleGetObject(String arg0) throws MissingResourceException {
			return null;
		}

		public void setParent(ResourceBundle parent) {
			super.setParent(parent);
		}

		public boolean isEmpty() {
			if (parent == null)
				return true;
			return ((BundleResourceBundle) parent).isEmpty();
		}

		public boolean isStemEmpty() {
			if (DEFAULT_ROOT.equals(localeString))
				return false;
			if (parent == null)
				return true;
			return ((BundleResourceBundle) parent).isStemEmpty();
		}
	}
}
