/*******************************************************************************
 * Copyright (c) 2004, 2017 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.osgi.storage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.container.ModuleWire;
import org.eclipse.osgi.container.ModuleWiring;
import org.eclipse.osgi.framework.util.CaseInsensitiveDictionaryMap;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.osgi.framework.Constants;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.wiring.BundleRevision;

/**
 * This class is used to localize manifest headers for a revision.
 */
public class ManifestLocalization {
	final String defaultRoot;
	private final Generation generation;
	private final Dictionary<String, String> rawHeaders;
	private volatile Dictionary<String, String> defaultLocaleHeaders = null;
	private final Hashtable<String, BundleResourceBundle> cache = new Hashtable<>(5);

	public ManifestLocalization(Generation generation, Dictionary<String, String> rawHeaders, String defaultRoot) {
		this.generation = generation;
		this.rawHeaders = rawHeaders;
		this.defaultRoot = defaultRoot;
	}

	public void clearCache() {
		synchronized (cache) {
			cache.clear();
			defaultLocaleHeaders = null;
		}
	}

	Dictionary<String, String> getHeaders(String localeString) {
		if (localeString == null)
			localeString = Locale.getDefault().toString();
		if (localeString.length() == 0)
			return rawHeaders;
		boolean isDefaultLocale = localeString.equals(Locale.getDefault().toString());
		Dictionary<String, String> currentDefault = defaultLocaleHeaders;
		if (isDefaultLocale && currentDefault != null) {
			return currentDefault;
		}
		if (generation.getRevision().getRevisions().getModule().getState().equals(Module.State.UNINSTALLED)) {
			// defaultLocaleHeaders should have been initialized on uninstall
			if (currentDefault != null)
				return currentDefault;
			return rawHeaders;
		}
		ResourceBundle localeProperties = getResourceBundle(localeString, isDefaultLocale);
		CaseInsensitiveDictionaryMap<String, String> localeHeaders = new CaseInsensitiveDictionaryMap<>(this.rawHeaders);
		for (Entry<String, String> entry : localeHeaders.entrySet()) {
			String value = entry.getValue();
			if (value.startsWith("%") && (value.length() > 1)) { //$NON-NLS-1$
				String propertiesKey = value.substring(1);
				try {
					value = localeProperties == null ? propertiesKey : (String) localeProperties.getObject(propertiesKey);
				} catch (MissingResourceException ex) {
					value = propertiesKey;
				}
				entry.setValue(value);
			}
		}
		Dictionary<String, String> result = localeHeaders.asUnmodifiableDictionary();
		if (isDefaultLocale) {
			defaultLocaleHeaders = result;
		}
		return result;
	}

	private String[] buildNLVariants(String nl) {
		List<String> result = new ArrayList<>();
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

		BundleResourceBundle result = cache.get(localeString);
		if (result != null)
			return result.isEmpty() ? null : result;

		// Collect all the necessary inputstreams to create the resource bundle without
		// holding any locks.  Finding resources and inputstreams from the wirings requires a
		// read lock on the module database.  We must not hold the cache lock while doing this;
		// otherwise out of order locks will be possible when the resolver needs to clear the cache
		String[] nlVarients = buildNLVariants(localeString);
		InputStream[] nlStreams = new InputStream[nlVarients.length];
		for (int i = nlVarients.length - 1; i >= 0; i--) {

			URL url = findResource(localizationHeader + (nlVarients[i].equals("") ? nlVarients[i] : '_' + nlVarients[i]) + ".properties"); //$NON-NLS-1$ //$NON-NLS-2$
			if (url != null) {
				try {
					nlStreams[i] = url.openStream();
				} catch (IOException e) {
					// ignore
				}
			}
		}

		synchronized (cache) {
			BundleResourceBundle parent = null;
			for (int i = nlVarients.length - 1; i >= 0; i--) {
				BundleResourceBundle varientBundle = null;
				InputStream varientStream = nlStreams[i];
				if (varientStream == null) {
					varientBundle = cache.get(nlVarients[i]);
				} else {
					try {
						varientBundle = new LocalizationResourceBundle(varientStream);
					} catch (IOException e) {
						// ignore and continue
					} finally {
						if (varientStream != null) {
							try {
								varientStream.close();
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
		ModuleWiring searchWiring = generation.getRevision().getWiring();
		if (searchWiring != null) {
			if ((generation.getRevision().getTypes() & BundleRevision.TYPE_FRAGMENT) != 0) {
				List<ModuleWire> hostWires = searchWiring.getRequiredModuleWires(HostNamespace.HOST_NAMESPACE);
				searchWiring = null;
				Long lowestHost = Long.MAX_VALUE;
				if (hostWires != null) {
					// search for the host with the highest ID
					for (ModuleWire hostWire : hostWires) {
						Long hostID = hostWire.getProvider().getRevisions().getModule().getId();
						if (hostID.compareTo(lowestHost) <= 0) {
							lowestHost = hostID;
							searchWiring = hostWire.getProviderWiring();
						}
					}
				}
			}
		}
		if (searchWiring != null) {
			int lastSlash = resource.lastIndexOf('/');
			String path = lastSlash > 0 ? resource.substring(0, lastSlash) : "/"; //$NON-NLS-1$
			String fileName = lastSlash != -1 ? resource.substring(lastSlash + 1) : resource;
			List<URL> result = searchWiring.findEntries(path, fileName, 0);
			return (result == null || result.isEmpty()) ? null : result.get(0);
		}
		// search the raw bundle file for the generation
		return generation.getEntry(resource);
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

		@Override
		public void setParent(ResourceBundle parent) {
			super.setParent(parent);
		}

		@Override
		public boolean isEmpty() {
			return false;
		}

		@Override
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

		@Override
		public Enumeration<String> getKeys() {
			return Collections.emptyEnumeration();
		}

		@Override
		protected Object handleGetObject(String arg0) throws MissingResourceException {
			return null;
		}

		@Override
		public void setParent(ResourceBundle parent) {
			super.setParent(parent);
		}

		@Override
		public boolean isEmpty() {
			if (parent == null)
				return true;
			return ((BundleResourceBundle) parent).isEmpty();
		}

		@Override
		public boolean isStemEmpty() {
			if (defaultRoot.equals(localeString))
				return false;
			if (parent == null)
				return true;
			return ((BundleResourceBundle) parent).isStemEmpty();
		}
	}
}
