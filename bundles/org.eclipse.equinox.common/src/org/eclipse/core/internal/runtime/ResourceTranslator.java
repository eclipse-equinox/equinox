/*******************************************************************************
 * Copyright (c) 2003, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sergey Prigogin (Google) - use parameterized types (bug 442021)
 *******************************************************************************/
package org.eclipse.core.internal.runtime;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.*;

/**
 * This class can only be used if OSGi plugin is available.
 */
public class ResourceTranslator {
	private static final String KEY_PREFIX = "%"; //$NON-NLS-1$
	private static final String KEY_DOUBLE_PREFIX = "%%"; //$NON-NLS-1$	

	public static String getResourceString(Bundle bundle, String value) {
		return getResourceString(bundle, value, null);
	}

	public static String getResourceString(Bundle bundle, String value, ResourceBundle resourceBundle) {
		String s = value.trim();
		if (!s.startsWith(KEY_PREFIX, 0))
			return s;
		if (s.startsWith(KEY_DOUBLE_PREFIX, 0))
			return s.substring(1);

		int ix = s.indexOf(' ');
		String key = ix == -1 ? s : s.substring(0, ix);
		String dflt = ix == -1 ? s : s.substring(ix + 1);

		if (resourceBundle == null && bundle != null) {
			try {
				resourceBundle = getResourceBundle(bundle);
			} catch (MissingResourceException e) {
				// just return the default (dflt)
			}
		}

		if (resourceBundle == null)
			return dflt;

		try {
			return resourceBundle.getString(key.substring(1));
		} catch (MissingResourceException e) {
			//this will avoid requiring a bundle access on the next lookup
			return dflt;
		}
	}

	public static ResourceBundle getResourceBundle(Bundle bundle) throws MissingResourceException {
		return getResourceBundle(bundle, null);
	}

	private static ResourceBundle getResourceBundle(Bundle bundle, String language) throws MissingResourceException {
		if (hasRuntime21(bundle)) {
			Locale locale = (language == null) ? Locale.getDefault() : new Locale(language);
			return ResourceBundle.getBundle("plugin", locale, createTempClassloader(bundle)); //$NON-NLS-1$
		}
		return Activator.getLocalization(bundle, language);
	}

	public static String[] getResourceString(Bundle bundle, String[] nonTranslated, String locale) {
		if (bundle == null)
			return nonTranslated;

		ResourceBundle resourceBundle = null;
		try {
			resourceBundle = getResourceBundle(bundle, locale);
		} catch (MissingResourceException e) {
			// ignore - bug 371103
		}
		String[] translated = new String[nonTranslated.length];
		for (int i = 0; i < nonTranslated.length; i++) {
			translated[i] = getResourceString(bundle, nonTranslated[i], resourceBundle);
		}
		return translated;
	}

	private static boolean hasRuntime21(Bundle b) {
		try {
			ManifestElement[] prereqs = ManifestElement.parseHeader(Constants.REQUIRE_BUNDLE, b.getHeaders("").get(Constants.REQUIRE_BUNDLE)); //$NON-NLS-1$
			if (prereqs == null)
				return false;
			for (ManifestElement prereq : prereqs) {
				if ("2.1".equals(prereq.getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE)) && "org.eclipse.core.runtime".equals(prereq.getValue())) {//$NON-NLS-1$//$NON-NLS-2$
					return true;
				}
			}
		} catch (BundleException e) {
			return false;
		}
		return false;
	}

	private static ClassLoader createTempClassloader(Bundle b) {
		ArrayList<URL> classpath = new ArrayList<>();
		addClasspathEntries(b, classpath);
		addBundleRoot(b, classpath);
		addDevEntries(b, classpath);
		addFragments(b, classpath);
		URL[] urls = new URL[classpath.size()];
		return new URLClassLoader(classpath.toArray(urls));
	}

	private static void addFragments(Bundle host, ArrayList<URL> classpath) {
		Activator activator = Activator.getDefault();
		if (activator == null)
			return;
		Bundle[] fragments = activator.getFragments(host);
		if (fragments == null)
			return;

		for (Bundle fragment : fragments) {
			addClasspathEntries(fragment, classpath);
			addDevEntries(fragment, classpath);
		}
	}

	private static void addClasspathEntries(Bundle b, ArrayList<URL> classpath) {
		ManifestElement[] classpathElements;
		try {
			classpathElements = ManifestElement.parseHeader(Constants.BUNDLE_CLASSPATH, b.getHeaders("").get(Constants.BUNDLE_CLASSPATH)); //$NON-NLS-1$
			if (classpathElements == null)
				return;
			for (ManifestElement classpathElement : classpathElements) {
				URL classpathEntry = b.getEntry(classpathElement.getValue());
				if (classpathEntry != null)
					classpath.add(classpathEntry);
			}
		} catch (BundleException e) {
			//ignore
		}
	}

	private static void addBundleRoot(Bundle b, ArrayList<URL> classpath) {
		classpath.add(b.getEntry("/")); //$NON-NLS-1$
	}

	private static void addDevEntries(Bundle b, ArrayList<URL> classpath) {
		if (!DevClassPathHelper.inDevelopmentMode())
			return;

		String[] binaryPaths = DevClassPathHelper.getDevClassPath(b.getSymbolicName());
		for (String binaryPath : binaryPaths) {
			URL classpathEntry = b.getEntry(binaryPath);
			if (classpathEntry != null)
				classpath.add(classpathEntry);
		}
	}
}
