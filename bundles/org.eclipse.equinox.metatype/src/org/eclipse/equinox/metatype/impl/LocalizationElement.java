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
package org.eclipse.equinox.metatype.impl;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import org.osgi.framework.Bundle;

public abstract class LocalizationElement {
	public static final char KEY_SIGN = '%';
	public static final char LOCALE_SEP = '_';

	/*
	 * Internal Method - to get resource bundle.
	 */
	private static ResourceBundle getResourceBundle(String localization, String locale, Bundle bundle) {
		// Determine the base name of the bundle localization property files.
		// If the <MetaData> 'localization' attribute was not specified,
		// use the Bundle-Localization manifest header value instead if it exists.
		String resourceBase = localization != null ? localization : MetaTypeProviderImpl.getBundleLocalization(bundle);

		// There are seven searching candidates possible:
		// baseName + 
		//		"_" + language1 + "_" + country1 + "_" + variation1	+ ".properties"
		// or	"_" + language1 + "_" + country1					+ ".properties"
		// or	"_" + language1										+ ".properties"
		// or	"_" + language2 + "_" + country2 + "_" + variation2	+ ".properties"
		// or	"_" + language2 + "_" + country2					+ ".properties"
		// or	"_" + language2										+ ".properties"
		// or	""													+ ".properties"
		//
		// Where language1[_country1[_variation1]] is the requested locale,
		// and language2[_country2[_variation2]] is the default locale.

		String[] searchCandidates = new String[7];

		// Candidates from passed locale:
		if (locale != null && locale.length() > 0) {
			int idx1_first = locale.indexOf(LOCALE_SEP);
			if (idx1_first == -1) {
				// locale has only language.
				searchCandidates[2] = MetaTypeProviderImpl.RESOURCE_FILE_CONN + locale;
			} else {
				// locale has at least language and country.
				searchCandidates[2] = MetaTypeProviderImpl.RESOURCE_FILE_CONN + locale.substring(0, idx1_first);
				int idx1_second = locale.indexOf(LOCALE_SEP, idx1_first + 1);
				if (idx1_second == -1) {
					// locale just has both language and country.
					searchCandidates[1] = MetaTypeProviderImpl.RESOURCE_FILE_CONN + locale;
				} else {
					// locale has language, country, and variation all.
					searchCandidates[1] = MetaTypeProviderImpl.RESOURCE_FILE_CONN + locale.substring(0, idx1_second);
					searchCandidates[0] = MetaTypeProviderImpl.RESOURCE_FILE_CONN + locale;
				}
			}
		}

		// Candidates from Locale.getDefault():
		String defaultLocale = Locale.getDefault().toString();
		int idx2_first = defaultLocale.indexOf(LOCALE_SEP);
		int idx2_second = defaultLocale.indexOf(LOCALE_SEP, idx2_first + 1);
		if (idx2_second != -1) {
			// default-locale is format of [language]_[country]_variation.
			searchCandidates[3] = MetaTypeProviderImpl.RESOURCE_FILE_CONN + defaultLocale;
			if (searchCandidates[3].equalsIgnoreCase(searchCandidates[0])) {
				searchCandidates[3] = null;
			}
		}
		if ((idx2_first != -1) && (idx2_second != idx2_first + 1)) {
			// default-locale is format of [language]_country[_variation].
			searchCandidates[4] = MetaTypeProviderImpl.RESOURCE_FILE_CONN + ((idx2_second == -1) ? defaultLocale : defaultLocale.substring(0, idx2_second));
			if (searchCandidates[4].equalsIgnoreCase(searchCandidates[1])) {
				searchCandidates[4] = null;
			}
		}
		if ((idx2_first == -1) && (defaultLocale.length() > 0)) {
			// default-locale has only language.
			searchCandidates[5] = MetaTypeProviderImpl.RESOURCE_FILE_CONN + defaultLocale;
		} else if (idx2_first > 0) {
			// default-locale is format of language_[...].
			searchCandidates[5] = MetaTypeProviderImpl.RESOURCE_FILE_CONN + defaultLocale.substring(0, idx2_first);
		}
		if (searchCandidates[5] != null && searchCandidates[5].equalsIgnoreCase(searchCandidates[2])) {
			searchCandidates[5] = null;
		}

		// The final candidate.
		searchCandidates[6] = ""; //$NON-NLS-1$

		URL resourceUrl = null;
		URL[] urls = null;

		for (int idx = 0; (idx < searchCandidates.length) && (resourceUrl == null); idx++) {
			urls = (searchCandidates[idx] == null ? null : FragmentUtils.findEntries(bundle, resourceBase + searchCandidates[idx] + MetaTypeProviderImpl.RESOURCE_FILE_EXT));
			if (urls != null && urls.length > 0)
				resourceUrl = urls[0];
		}

		if (resourceUrl != null) {
			try {
				return new PropertyResourceBundle(resourceUrl.openStream());
			} catch (IOException ioe) {
				// Exception when creating PropertyResourceBundle object.
			}
		}
		return null;
	}

	private final String localization;

	// @GuardedBy("this")
	private Bundle bundle;
	// @GuardedBy("this")
	private String locale;
	// @GuardedBy("this")
	private ResourceBundle resourceBundle;

	public LocalizationElement(String localization) {
		this.localization = localization;
	}

	String getLocalization() {
		return localization;
	}

	/**
	 * Method to get the localized text of inputed String.
	 */
	protected String getLocalized(String key) {
		if (key == null) {
			return null;
		}
		if ((key.length() > 1) && (key.charAt(0) == KEY_SIGN)) {
			ResourceBundle rb = getResourceBundle();
			if (rb != null) {
				try {
					String transfered = rb.getString(key.substring(1));
					if (transfered != null) {
						return transfered;
					}
				} catch (MissingResourceException mre) {
					// Nothing found for this key.
				}
			}
			// If no localization file available or no localized value found
			// for the key, then return the raw data without the key-sign.
			return key.substring(1);
		}
		return key;
	}

	/*
	 * This method must be (and currently is) called after setLocaleAndBundle(String, Bundle).
	 * If the bundle is not set, an NPE will be generated by getResourceBundle(String, String, Bundle).
	 */
	protected synchronized ResourceBundle getResourceBundle() {
		if (resourceBundle == null) {
			resourceBundle = getResourceBundle(localization, locale, bundle);
		}
		return resourceBundle;
	}

	/*
	 * This method must be (and currently is) called before getResourceBundle().
	 * If the bundle is not set, an NPE will be generated by getResourceBundle(String, String, Bundle).
	 */
	protected synchronized void setLocaleAndBundle(String locale, Bundle bundle) {
		this.locale = locale;
		this.bundle = bundle;
	}
}
