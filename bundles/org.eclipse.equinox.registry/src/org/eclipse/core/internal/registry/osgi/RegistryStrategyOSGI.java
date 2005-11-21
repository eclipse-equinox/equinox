/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.registry.osgi;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.ResourceBundle;
import javax.xml.parsers.SAXParserFactory;
import org.eclipse.core.internal.registry.*;
import org.eclipse.core.internal.runtime.ResourceTranslator;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.registry.spi.RegistryStrategy;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 * The registry strategy that can be used in OSGi world. It provides the following functionality:
 * 
 *  - Event scheduling is done using Eclipse job scheduling mechanism
 *  - Translation is done with Equinox ResourceTranslator
 *  - Uses OSGi bundles for namespace resolution (contributors: plugins and fragments)
 *  - Registry is filled with information stored in plugin.xml / fragment.xml of OSGi bundles
 *  - Uses bunlde-based class loading to create executable extensions
 *  - Performs registry validation based on the time stamps of the plugin.xml / fragment.xml files
 *  - XML parser is obtained via an OSGi service
 *  
 * @since org.eclipse.equinox.registry 1.0
 * 
 */

public class RegistryStrategyOSGI extends RegistryStrategy {

	/**
	 * Registry access key
	 */
	private Object token;

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
	private ServiceTracker xmlTracker = null;

	/**
	 * @param theStorageDir - file system directory to store cache files; might be null
	 * @param cacheReadOnly - true: cache is read only; false: cache is read/write
	 * @param key - control key for the registry (should be the same key as used in 
	 * the RegistryManager#createExtensionRegistry() of this registry
	 */
	public RegistryStrategyOSGI(File theStorageDir, boolean cacheReadOnly, Object key) {
		super(theStorageDir, cacheReadOnly);
		token = key;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.registry.spi.RegistryStrategy#isModifiable()
	 */
	public boolean isModifiable() {
		return false; // Clients are not allowed to add information into this registry
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.registry.spi.RegistryStrategy#translate(java.lang.String, java.util.ResourceBundle)
	 */
	public final String translate(String key, ResourceBundle resources) {
		return ResourceTranslator.getResourceString(null, key, resources);
	}

	//////////////////////////////////////////////////////////////////////////////////////////
	// Use Eclipse job scheduling mechanism

	private final static class ExtensionEventDispatcherJob extends Job {
		// an "identy rule" that forces extension events to be queued		
		private final static ISchedulingRule EXTENSION_EVENT_RULE = new ISchedulingRule() {
			public boolean contains(ISchedulingRule rule) {
				return rule == this;
			}

			public boolean isConflicting(ISchedulingRule rule) {
				return rule == this;
			}
		};
		private Map deltas;
		private Object[] listenerInfos;
		private Object registry;

		public ExtensionEventDispatcherJob(Object[] listenerInfos, Map deltas, Object registry) {
			// name not NL'd since it is a system job
			super("Registry event dispatcher"); //$NON-NLS-1$
			setSystem(true);
			this.listenerInfos = listenerInfos;
			this.deltas = deltas;
			this.registry = registry;
			// all extension event dispatching jobs use this rule
			setRule(EXTENSION_EVENT_RULE);
		}

		public IStatus run(IProgressMonitor monitor) {
			return RegistryStrategy.processChangeEvent(listenerInfos, deltas, registry);
		}
	}
	/* (non-Javadoc)

	 * @see org.eclipse.equinox.registry.spi.RegistryStrategy#scheduleChangeEvent(java.lang.Object[], java.util.Map, java.lang.Object)
	 */
	public final void scheduleChangeEvent(Object[] listeners, Map deltas, Object registry) {
		new ExtensionEventDispatcherJob(listeners, deltas, registry).schedule();
	}

	////////////////////////////////////////////////////////////////////////////////////////
	// Use OSGi bundles for namespace resolution (contributors: plugins and fragments)

	static private Bundle getContributingBundle(long contributingBundleId) {
		return Activator.getContext().getBundle(contributingBundleId);
	}

	static private Bundle getNamespaceBundle(long contributingBundleId) {
		Bundle contributingBundle = getContributingBundle(contributingBundleId);
		if (contributingBundle == null) // When restored from disk the underlying bundle may have been uninstalled
			throw new IllegalStateException("Internal error in extension registry. The bundle corresponding to this contribution has been uninstalled."); //$NON-NLS-1$
		if (OSGIUtils.getDefault().isFragment(contributingBundle))
			return OSGIUtils.getDefault().getHosts(contributingBundle)[0];
		return contributingBundle;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.registry.spi.RegistryStrategy#getNamespaceOwnerId(long)
	 */
	public long getNamespaceOwnerId(long contributorId) {
		Bundle namespaceBundle = getNamespaceBundle(contributorId);
		if (namespaceBundle == null)
			return -1;
		return namespaceBundle.getBundleId();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.registry.spi.RegistryStrategy#getNamespace(long)
	 */
	public String getNamespace(long contributorId) {
		if (contributorId == -1)
			return null;
		Bundle namespaceBundle = getNamespaceBundle(contributorId);
		if (namespaceBundle == null)
			return null;
		return namespaceBundle.getSymbolicName();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.registry.spi.RegistryStrategy#getContributorIds(java.lang.String)
	 */
	public long[] getNamespaceContributors(String namespace) {
		Bundle correspondingHost = OSGIUtils.getDefault().getBundle(namespace);
		if (correspondingHost == null)
			return new long[0];
		Bundle[] fragments = OSGIUtils.getDefault().getFragments(correspondingHost);
		if (fragments == null)
			return new long[] {correspondingHost.getBundleId()};
		long[] result = new long[fragments.length + 1];
		for (int i = 0; i < fragments.length; i++) {
			result[i] = fragments[i].getBundleId();
		}
		result[fragments.length] = correspondingHost.getBundleId();
		return result;
	}

	/////////////////////////////////////////////////////////////////////////////////////
	// Executable extensions: bundle-based class loading

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.registry.spi.RegistryStrategy#createExecutableExtension(java.lang.String, long, java.lang.String, java.lang.String, java.lang.Object, java.lang.String, org.eclipse.equinox.registry.IConfigurationElement)
	 */
	public Object createExecutableExtension(String pluginName, long namespaceOwnerId, String namespaceName, String className, Object initData, String propertyName, org.eclipse.equinox.registry.IConfigurationElement confElement) throws CoreException {

		if (pluginName != null && !pluginName.equals("") && !pluginName.equals(namespaceName)) { //$NON-NLS-1$
			Bundle otherBundle = null;
			otherBundle = OSGIUtils.getDefault().getBundle(pluginName);
			return createExecutableExtension(otherBundle, className, initData, propertyName, confElement);
		}

		Bundle contributingBundle = Activator.getContext().getBundle(namespaceOwnerId);
		return createExecutableExtension(contributingBundle, className, initData, propertyName, confElement);
	}

	private Object createExecutableExtension(Bundle bundle, String className, Object initData, String propertyName, org.eclipse.equinox.registry.IConfigurationElement confElement) throws CoreException {
		// load the requested class from this plugin
		Class classInstance = null;
		try {
			classInstance = bundle.loadClass(className);
		} catch (Exception e1) {
			throwException(NLS.bind(RegistryMessages.plugin_loadClassError, bundle.getSymbolicName(), className), e1);
		} catch (LinkageError e) {
			throwException(NLS.bind(RegistryMessages.plugin_loadClassError, bundle.getSymbolicName(), className), e);
		}

		// create a new instance
		Object result = null;
		try {
			result = classInstance.newInstance();
		} catch (Exception e) {
			throwException(NLS.bind(RegistryMessages.plugin_instantiateClassError, bundle.getSymbolicName(), className), e);
		}

		return result;
	}

	private void throwException(String message, Throwable exception) throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, RegistryMessages.OWNER_NAME, IRegistryConstants.PLUGIN_ERROR, message, exception));
	}

	/////////////////////////////////////////////////////////////////////////////////////
	// Start / stop extra processing: adding bundle listener; fill registry if not filled from cache

	/**
	 * Listening to the bunlde events.
	 */
	private EclipseBundleListener pluginBundleListener = null;

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.registry.spi.RegistryStrategy#start(java.lang.Object)
	 */
	public void onStart(Object registry) {
		super.onStart(registry);
		if (!(registry instanceof ExtensionRegistry))
			return;
		ExtensionRegistry theRegistry = (ExtensionRegistry) registry;

		// register a listener to catch new bundle installations/resolutions.
		pluginBundleListener = new EclipseBundleListener(theRegistry, token);
		Activator.getContext().addBundleListener(pluginBundleListener);

		// populate the registry with all the currently installed bundles.
		// There is a small window here while processBundles is being
		// called where the pluginBundleListener may receive a BundleEvent 
		// to add/remove a bundle from the registry.  This is ok since
		// the registry is a synchronized object and will not add the
		// same bundle twice.
		if (!theRegistry.filledFromCache())
			pluginBundleListener.processBundles(Activator.getContext().getBundles());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.registry.spi.RegistryStrategy#stop(java.lang.Object)
	 */
	public void onStop(Object registry) {
		if (pluginBundleListener != null)
			Activator.getContext().removeBundleListener(pluginBundleListener);
		if (xmlTracker != null) {
			xmlTracker.close();
			xmlTracker = null;
		}
		super.onStop(registry);
	}

	//////////////////////////////////////////////////////////////////////////////////////
	// Cache strategy

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.registry.spi.RegistryStrategy#useCache()
	 */
	public boolean cacheUse() {
		return !"true".equals(System.getProperty(IRegistryConstants.PROP_NO_REGISTRY_CACHE)); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.registry.spi.RegistryStrategy#lazyCacheLoading()
	 */
	public boolean cacheLazyLoading() {
		return !("true".equalsIgnoreCase(System.getProperty(IRegistryConstants.PROP_NO_LAZY_CACHE_LOADING))); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.registry.spi.RegistryStrategy#computeTimeStamp()
	 */
	public long cacheComputeTimeStamp() {
		// If the check config prop is false or not set then exit
		if (!"true".equalsIgnoreCase(System.getProperty(IRegistryConstants.PROP_CHECK_CONFIG))) //$NON-NLS-1$  
			return 0;
		BundleContext context = Activator.getContext();
		if (context == null)
			return 0;
		Bundle[] allBundles = context.getBundles();
		long result = 0;
		for (int i = 0; i < allBundles.length; i++) {
			URL pluginManifest = allBundles[i].getEntry("plugin.xml"); //$NON-NLS-1$
			if (pluginManifest == null)
				pluginManifest = allBundles[i].getEntry("fragment.xml"); //$NON-NLS-1$
			if (pluginManifest == null)
				continue;
			try {
				URLConnection connection = pluginManifest.openConnection();
				result ^= connection.getLastModified() + allBundles[i].getBundleId();
			} catch (IOException e) {
				return 0;
			}
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.registry.spi.RegistryStrategy#getXMLParser()
	 */
	public SAXParserFactory getXMLParser() {
		if (xmlTracker == null) {
			xmlTracker = new ServiceTracker(Activator.getContext(), SAXParserFactory.class.getName(), null);
			xmlTracker.open();
		}
		return (SAXParserFactory) xmlTracker.getService();
	}

}
