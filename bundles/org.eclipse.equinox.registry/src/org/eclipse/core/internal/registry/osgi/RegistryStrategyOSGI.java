/*******************************************************************************
 * Copyright (c) 2005, 2018 IBM Corporation and others.
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
package org.eclipse.core.internal.registry.osgi;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.xml.parsers.SAXParserFactory;
import org.eclipse.core.internal.registry.*;
import org.eclipse.core.internal.runtime.ResourceTranslator;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.spi.*;
import org.eclipse.osgi.service.localization.LocaleProvider;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 * The registry strategy that can be used in OSGi world. It provides the
 * following functionality:
 * <ul>
 * <li>Translation is done with ResourceTranslator</li>
 * <li>Registry is filled with information stored in plugin.xml / fragment.xml
 * of OSGi bundles</li>
 * <li>Uses bunlde-based class loading to create executable extensions</li>
 * <li>Performs registry validation based on the time stamps of the plugin.xml /
 * fragment.xml files</li>
 * <li>XML parser is obtained via an OSGi service</li>
 * </ul>
 *
 * @see RegistryFactory#setDefaultRegistryProvider(IRegistryProvider)
 * @since org.eclipse.equinox.registry 3.2
 */

public class RegistryStrategyOSGI extends RegistryStrategy {

	/**
	 * Registry access key
	 */
	private final Object token;

	/**
	 * Debug extension registry
	 */
	protected boolean DEBUG;

	/**
	 * Debug extension registry events
	 */
	protected boolean DEBUG_REGISTRY_EVENTS;

	/**
	 * Tracker for the XML parser service
	 */
	private ServiceTracker<?, ?> xmlTracker = null;

	/**
	 * Tracker for the LocaleProvider service
	 */
	private ServiceTracker<?, ?> localeTracker = null;

	/**
	 * Value of the query "should we track contributions timestamps" is cached in
	 * this variable
	 */
	private boolean trackTimestamp;

	/**
	 * @param theStorageDir - array of file system directories to store cache files;
	 *                      might be null
	 * @param cacheReadOnly - array of read only attributes. True: cache at this
	 *                      location is read only; false: cache is read/write
	 * @param key           - control key for the registry (should be the same key
	 *                      as used in the RegistryManager#createExtensionRegistry()
	 *                      of this registry
	 */
	public RegistryStrategyOSGI(File[] theStorageDir, boolean[] cacheReadOnly, Object key) {
		super(theStorageDir, cacheReadOnly);
		token = key;

		// Only do timestamp calculations if osgi.checkConfiguration is set to "true"
		// (typically,
		// this implies -dev mode)
		BundleContext context = Activator.getContext();
		if (context != null)
			trackTimestamp = "true".equalsIgnoreCase(context.getProperty(IRegistryConstants.PROP_CHECK_CONFIG)); //$NON-NLS-1$
		else
			trackTimestamp = false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.core.runtime.spi.RegistryStrategy#translate(java.lang.String,
	 * java.util.ResourceBundle)
	 */
	@Override
	public final String translate(String key, ResourceBundle resources) {
		return ResourceTranslator.getResourceString(null, key, resources);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.core.runtime.spi.RegistryStrategy#translate(java.lang.String[],
	 * org.eclipse.core.runtime.IContributor, java.lang.String)
	 */
	@Override
	public String[] translate(String[] nonTranslated, IContributor contributor, String locale) {
		return ResourceTranslator.getResourceString(ContributorFactoryOSGi.resolve(contributor), nonTranslated, locale);
	}

	////////////////////////////////////////////////////////////////////////////////////////
	// Use OSGi bundles for namespace resolution (contributors: plugins and
	//////////////////////////////////////////////////////////////////////////////////////// fragments)

	/**
	 * The default load factor for the bundle cache.
	 */
	private static float DEFAULT_BUNDLECACHE_LOADFACTOR = 0.75f;

	/**
	 * The expected bundle cache size (calculated as a number of bundles divided by
	 * the DEFAULT_BUNDLECACHE_LOADFACTOR). The bundle cache will be resized
	 * automatically is this number is exceeded.
	 */
	private static int DEFAULT_BUNDLECACHE_SIZE = 200;

	/**
	 * For performance, we cache mapping of IDs to Bundles.
	 *
	 * We don't expect mapping to change during the runtime. (Or, in the OSGI terms,
	 * we don't expect bundle IDs to be reused during the Eclipse run.) The Bundle
	 * object is stored as a weak reference to facilitate GC in case the bundle was
	 * uninstalled during the Eclipse run.
	 */
	private final ReferenceMap bundleMap = new ReferenceMap(ReferenceMap.SOFT, DEFAULT_BUNDLECACHE_SIZE,
			DEFAULT_BUNDLECACHE_LOADFACTOR);

	private final ReadWriteLock bundleMapLock = new ReentrantReadWriteLock();

	// String Id to OSGi Bundle conversion
	private Bundle getBundle(String id) {
		if (id == null)
			return null;
		long OSGiId;
		try {
			OSGiId = Long.parseLong(id);
		} catch (NumberFormatException e) {
			return null;
		}
		// We assume here that OSGI Id will fit into "int". As the number of
		// registry elements themselves are expected to fit into "int", this
		// is a valid assumption for the time being.
		Bundle bundle;
		bundleMapLock.readLock().lock();
		try {
			bundle = (Bundle) bundleMap.get((int) OSGiId);
		} finally {
			bundleMapLock.readLock().unlock();
		}
		if (bundle != null)
			return bundle;
		// note: we accept that two concurrent threads end up here for the same id,
		// because they will anyway resolve the same mapping
		bundle = Activator.getContext().getBundle(OSGiId);
		bundleMapLock.writeLock().lock();
		try {
			bundleMap.put((int) OSGiId, bundle);
		} finally {
			bundleMapLock.writeLock().unlock();
		}
		return bundle;
	}

	/////////////////////////////////////////////////////////////////////////////////////
	// Executable extensions: bundle-based class loading

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.core.runtime.spi.RegistryStrategy#createExecutableExtension(org.
	 * eclipse.core.runtime.spi.RegistryContributor, java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public Object createExecutableExtension(RegistryContributor contributor, String className,
			String overridenContributorName) throws CoreException {
		Bundle contributingBundle;
		if (overridenContributorName != null && !overridenContributorName.equals("")) //$NON-NLS-1$
			contributingBundle = OSGIUtils.getDefault().getBundle(overridenContributorName);
		else
			contributingBundle = getBundle(contributor.getId());

		if (contributingBundle == null)
			throwException(NLS.bind(RegistryMessages.plugin_loadClassError, "UNKNOWN BUNDLE", className), //$NON-NLS-1$
					new InvalidRegistryObjectException());

		// load the requested class from this bundle
		Class<?> classInstance = null;
		try {
			classInstance = contributingBundle.loadClass(className);
		} catch (Exception | LinkageError e1) {
			throwException(
					NLS.bind(RegistryMessages.plugin_loadClassError, contributingBundle.getSymbolicName(), className),
					e1);
		}

		// create a new instance
		Object result = null;
		try {
			result = classInstance.getDeclaredConstructor().newInstance();
		} catch (Exception | LinkageError e) {
			throwException(NLS.bind(RegistryMessages.plugin_instantiateClassError, contributingBundle.getSymbolicName(),
					className), e);
		}
		return result;
	}

	private void throwException(String message, Throwable exception) throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, RegistryMessages.OWNER_NAME, IRegistryConstants.PLUGIN_ERROR,
				message, exception));
	}

	/////////////////////////////////////////////////////////////////////////////////////
	// Start / stop extra processing: adding bundle listener; fill registry if not
	///////////////////////////////////////////////////////////////////////////////////// filled
	///////////////////////////////////////////////////////////////////////////////////// from
	///////////////////////////////////////////////////////////////////////////////////// cache

	/**
	 * Listening to the bundle events.
	 */
	private EclipseBundleListener pluginBundleListener = null;

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.core.runtime.spi.RegistryStrategy#onStart(org.eclipse.core.
	 * runtime.IExtensionRegistry, boolean)
	 */
	@Override
	public void onStart(IExtensionRegistry registry, boolean loadedFromCache) {
		super.onStart(registry, loadedFromCache);

		if (!(registry instanceof ExtensionRegistry))
			return;
		// register a listener to catch new bundle installations/resolutions.
		pluginBundleListener = new EclipseBundleListener((ExtensionRegistry) registry, token, this);
		Activator.getContext().addBundleListener(pluginBundleListener);

		// populate the registry with all the currently installed bundles.
		// There is a small window here while processBundles is being
		// called where the pluginBundleListener may receive a BundleEvent
		// to add/remove a bundle from the registry. This is ok since
		// the registry is a synchronized object and will not add the
		// same bundle twice.
		if (!loadedFromCache)
			pluginBundleListener.processBundles(Activator.getContext().getBundles());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.core.runtime.spi.RegistryStrategy#onStop(org.eclipse.core.runtime
	 * .IExtensionRegistry)
	 */
	@Override
	public void onStop(IExtensionRegistry registry) {
		if (pluginBundleListener != null)
			Activator.getContext().removeBundleListener(pluginBundleListener);
		if (xmlTracker != null) {
			xmlTracker.close();
			xmlTracker = null;
		}
		if (localeTracker != null) {
			localeTracker.close();
			localeTracker = null;
		}
		super.onStop(registry);
	}

	//////////////////////////////////////////////////////////////////////////////////////
	// Cache strategy

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.core.runtime.spi.RegistryStrategy#cacheUse()
	 */
	@Override
	public boolean cacheUse() {
		return !"true".equals(RegistryProperties.getProperty(IRegistryConstants.PROP_NO_REGISTRY_CACHE)); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.core.runtime.spi.RegistryStrategy#cacheLazyLoading()
	 */
	@Override
	public boolean cacheLazyLoading() {
		return !("true" //$NON-NLS-1$
				.equalsIgnoreCase(RegistryProperties.getProperty(IRegistryConstants.PROP_NO_LAZY_CACHE_LOADING)));
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.core.runtime.spi.RegistryStrategy#getContributionsTimestamp()
	 */
	@Override
	public long getContributionsTimestamp() {
		if (!checkContributionsTimestamp())
			return 0;
		RegistryTimestamp expectedTimestamp = new RegistryTimestamp();
		BundleContext context = Activator.getContext();
		Bundle[] allBundles = context.getBundles();
		for (Bundle b : allBundles) {
			URL pluginManifest = EclipseBundleListener.getExtensionURL(b, false);
			if (pluginManifest == null)
				continue;
			long timestamp = getExtendedTimestamp(b, pluginManifest);
			expectedTimestamp.add(timestamp);
		}
		return expectedTimestamp.getContentsTimestamp();
	}

	public boolean checkContributionsTimestamp() {
		return trackTimestamp;
	}

	public long getExtendedTimestamp(Bundle bundle, URL pluginManifest) {
		if (pluginManifest == null)
			return 0;
		try {
			return pluginManifest.openConnection().getLastModified() + bundle.getBundleId();
		} catch (IOException e) {
			if (debug()) {
				System.out.println("Unable to obtain timestamp for the bundle " + bundle.getSymbolicName()); //$NON-NLS-1$
				e.printStackTrace();
			}
			return 0;
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.core.runtime.spi.RegistryStrategy#getXMLParser()
	 */
	@Override
	public SAXParserFactory getXMLParser() {
		if (xmlTracker == null) {
			xmlTracker = new ServiceTracker<>(Activator.getContext(), SAXParserFactory.class.getName(), null);
			xmlTracker.open();
		}
		return (SAXParserFactory) xmlTracker.getService();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.core.runtime.spi.RegistryStrategy#getLocale()
	 */
	@Override
	public String getLocale() {
		if (localeTracker == null) {
			localeTracker = new ServiceTracker<>(Activator.getContext(), LocaleProvider.class.getName(), null);
			localeTracker.open();
		}
		LocaleProvider localeProvider = (LocaleProvider) localeTracker.getService();
		if (localeProvider != null) {
			Locale currentLocale = localeProvider.getLocale();
			if (currentLocale != null)
				return currentLocale.toString();
		}
		return super.getLocale();
	}
}
