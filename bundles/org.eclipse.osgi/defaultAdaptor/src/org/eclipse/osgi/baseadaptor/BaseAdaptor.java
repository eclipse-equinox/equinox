/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.baseadaptor;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;
import org.eclipse.core.runtime.adaptor.LocationManager;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleFile;
import org.eclipse.osgi.baseadaptor.hooks.*;
import org.eclipse.osgi.framework.adaptor.*;
import org.eclipse.osgi.framework.debug.Debug;
import org.eclipse.osgi.framework.internal.core.*;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.internal.baseadaptor.*;
import org.eclipse.osgi.service.resolver.PlatformAdmin;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;

/**
 * A Framework adaptor implementation that allows additional functionality to be
 * hooked in.  Hooks are configured using {@link HookConfigurator}
 * objects.   A framework extension may add hook configurators which can be used
 * to add hooks to the {@link HookRegistry}.
 * @see HookConfigurator
 * @see HookRegistry
 * @see AdaptorHook
 * @since 3.2
 */
public class BaseAdaptor implements FrameworkAdaptor{
	// System property used to set the parent classloader type (boot is the default)
	private static final String PROP_PARENT_CLASSLOADER = "osgi.parentClassloader"; //$NON-NLS-1$
	// A parent classloader type that specifies the application classloader
	private static final String PARENT_CLASSLOADER_APP = "app"; //$NON-NLS-1$
	// A parent classloader type that specifies the extension classlaoder
	private static final String PARENT_CLASSLOADER_EXT = "ext"; //$NON-NLS-1$
	// A parent classloader type that specifies the boot classlaoder
	private static final String PARENT_CLASSLOADER_BOOT = "boot"; //$NON-NLS-1$
	// A parent classloader type that specifies the framework classlaoder
	private static final String PARENT_CLASSLOADER_FWK = "fwk"; //$NON-NLS-1$
	// The BundleClassLoader parent to use when creating BundleClassLoaders.
	private static ClassLoader bundleClassLoaderParent;
	static {
		// check property for specified parent
		String type = FrameworkProperties.getProperty(BaseAdaptor.PROP_PARENT_CLASSLOADER, BaseAdaptor.PARENT_CLASSLOADER_BOOT);
		if (BaseAdaptor.PARENT_CLASSLOADER_FWK.equalsIgnoreCase(type))
			bundleClassLoaderParent = FrameworkAdaptor.class.getClassLoader();
		else if (BaseAdaptor.PARENT_CLASSLOADER_APP.equalsIgnoreCase(type))
			bundleClassLoaderParent = ClassLoader.getSystemClassLoader();
		else if (BaseAdaptor.PARENT_CLASSLOADER_EXT.equalsIgnoreCase(type)) {
			ClassLoader appCL = ClassLoader.getSystemClassLoader();
			if (appCL != null)
				bundleClassLoaderParent = appCL.getParent();
		}
		// default to boot classloader
		if (bundleClassLoaderParent == null)
			bundleClassLoaderParent = new ParentClassLoader();
	}

	// Empty parent classloader.  This is used by default as the BundleClassLoader parent.
	private static class ParentClassLoader extends ClassLoader {
		protected ParentClassLoader() {
			super(null);
		}
	}

	private EventPublisher eventPublisher;
	private ServiceRegistry serviceRegistry;
	private boolean stopping;
	private HookRegistry hookRegistry;
	private FrameworkLog log;
	private BundleContext context;
	private BaseStorage storage;
	private BundleWatcher bundleWatcher;

	/**
	 * Constructs a BaseAdaptor.
	 * @param args arguments passed to the adaptor by the framework.
	 */
	public BaseAdaptor(String[] args) {
		if (LocationManager.getConfigurationLocation() == null)
			LocationManager.initializeLocations();
		hookRegistry = new HookRegistry(this);
		FrameworkLogEntry[] errors = hookRegistry.initialize();
		if (errors.length > 0)
			for (int i = 0; i < errors.length; i++)
				getFrameworkLog().log(errors[i]);
		storage = getStorage();
		// TODO consider passing args to BaseAdaptorHooks
	}

	/**
	 * This method will call all configured adaptor hooks {@link AdaptorHook#initialize(BaseAdaptor)} method.
	 * @see FrameworkAdaptor#initialize(EventPublisher)
	 */
	public void initialize(EventPublisher publisher) {
		this.eventPublisher = publisher;
		serviceRegistry = new ServiceRegistryImpl();
		((ServiceRegistryImpl) serviceRegistry).initialize();
		// set the adaptor for the adaptor hooks
		AdaptorHook[] adaptorHooks = getHookRegistry().getAdaptorHooks();
		for (int i = 0; i < adaptorHooks.length; i++)
			adaptorHooks[i].initialize(this);
	}

	/**
	 * @see FrameworkAdaptor#initializeStorage()
	 */
	public void initializeStorage() throws IOException {
		storage.initialize(this);
	}

	/**
	 * @see FrameworkAdaptor#compactStorage()
	 */
	public void compactStorage() throws IOException {
		storage.compact();
	}

	/**
	 * This method will call all the configured adaptor hook {@link AdaptorHook#addProperties(Properties)} methods.
	 * @see FrameworkAdaptor#getProperties()
	 */
	public Properties getProperties() {
		Properties props = new Properties();
		String resource = FrameworkProperties.getProperty(Constants.OSGI_PROPERTIES, Constants.DEFAULT_OSGI_PROPERTIES);
		try {
			InputStream in = null;
			File file = new File(resource);
			if (file.exists())
				in = new FileInputStream(file);
			if (in == null)
				in = getClass().getResourceAsStream(resource);
			if (in != null) {
				try {
					props.load(new BufferedInputStream(in));
				} finally {
					try {
						in.close();
					} catch (IOException ee) {
						// nothing to do
					}
				}
			} else {
				if (Debug.DEBUG && Debug.DEBUG_GENERAL)
					Debug.println("Skipping osgi.properties: " + resource); //$NON-NLS-1$
			}
		} catch (IOException e) {
			if (Debug.DEBUG && Debug.DEBUG_GENERAL)
				Debug.println("Unable to load osgi.properties: " + e.getMessage()); //$NON-NLS-1$
		}
		// add the storage properties
		storage.addProperties(props);
		// add the properties from each adaptor hook
		AdaptorHook[] adaptorHooks = getHookRegistry().getAdaptorHooks();
		for (int i = 0; i < adaptorHooks.length; i++)
			adaptorHooks[i].addProperties(props);
		return props;
	}

	/**
	 * @see FrameworkAdaptor#getInstalledBundles()
	 */
	public BundleData[] getInstalledBundles() {
		return storage.getInstalledBundles();
	}

	/**
	 * This method will call each configured adaptor hook {@link AdaptorHook#mapLocationToURLConnection(String)} method
	 * until one returns a non-null value.  If none of the adaptor hooks return a non-null value then the 
	 * string is used to construct a new URL object to open a new url connection.
	 * 
	 * @see FrameworkAdaptor#mapLocationToURLConnection(String)
	 */
	public URLConnection mapLocationToURLConnection(String location) throws BundleException {
		try {
			URLConnection result = null;
			// try the adaptor hooks first;
			AdaptorHook[] adaptorHooks = getHookRegistry().getAdaptorHooks();
			for (int i = 0; i < adaptorHooks.length; i++) {
				result = adaptorHooks[i].mapLocationToURLConnection(location);
				if (result != null)
					return result;
			}
			// just do the default
			return (new URL(location).openConnection());
		} catch (IOException e) {
			throw new BundleException(NLS.bind(AdaptorMsg.ADAPTOR_URL_CREATE_EXCEPTION, location), e);
		}
	}

	/**
	 * @see FrameworkAdaptor#installBundle(String, URLConnection)
	 */
	public BundleOperation installBundle(String location, URLConnection source) {
		return storage.installBundle(location, source);
	}

	/**
	 * @see FrameworkAdaptor#updateBundle(BundleData, URLConnection)
	 */
	public BundleOperation updateBundle(BundleData bundledata, URLConnection source) {
		return storage.updateBundle((BaseData) bundledata, source);
	}

	/**
	 * @see FrameworkAdaptor#uninstallBundle(BundleData)
	 */
	public BundleOperation uninstallBundle(BundleData bundledata) {
		return storage.uninstallBundle((BaseData) bundledata);
	}

	/**
	 * @see FrameworkAdaptor#getTotalFreeSpace()
	 */
	public long getTotalFreeSpace() throws IOException {
		return storage.getFreeSpace();
	}

	/**
	 * @see FrameworkAdaptor#getPermissionStorage()
	 */
	public PermissionStorage getPermissionStorage() throws IOException {
		return storage.getPermissionStorage();
	}

	/**
	 * @see FrameworkAdaptor#getServiceRegistry()
	 */
	public ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	/**
	 * This method calls all the configured adaptor hook {@link AdaptorHook#frameworkStart(BundleContext)} methods.
	 * @see FrameworkAdaptor#frameworkStart(BundleContext)
	 */
	public void frameworkStart(BundleContext fwContext) throws BundleException {
		this.context = fwContext;
		stopping = false;
		BundleResourceHandler.setContext(fwContext);
		// always start the storage first
		storage.frameworkStart(fwContext);
		AdaptorHook[] adaptorHooks = getHookRegistry().getAdaptorHooks();
		for (int i = 0; i < adaptorHooks.length; i++)
			adaptorHooks[i].frameworkStart(fwContext);
	}

	/**
	 * This method calls all the configured adaptor hook {@link AdaptorHook#frameworkStop(BundleContext)} methods.
	 * @see FrameworkAdaptor#frameworkStop(BundleContext)
	 */
	public void frameworkStop(BundleContext fwContext) throws BundleException {
		// first inform all configured adaptor hooks
		AdaptorHook[] adaptorHooks = getHookRegistry().getAdaptorHooks();
		for (int i = 0; i < adaptorHooks.length; i++)
			adaptorHooks[i].frameworkStop(fwContext);
		// stop the storage last
		storage.frameworkStop(fwContext);
		fwContext = null;
	}

	/**
	 * This method calls all the configured adaptor hook {@link AdaptorHook#frameworkStopping(BundleContext)} methods.
	 * @see FrameworkAdaptor#frameworkStopping(BundleContext)
	 */
	public void frameworkStopping(BundleContext fwContext) {
		stopping = true;
		// always tell storage of stopping first
		storage.frameworkStopping(fwContext);
		// inform all configured adaptor hooks last
		AdaptorHook[] adaptorHooks = getHookRegistry().getAdaptorHooks();
		for (int i = 0; i < adaptorHooks.length; i++)
			adaptorHooks[i].frameworkStopping(fwContext);
	}

	/**
	 * @see FrameworkAdaptor#getInitialBundleStartLevel()
	 */
	public int getInitialBundleStartLevel() {
		return storage.getInitialBundleStartLevel();
	}

	/**
	 * @see FrameworkAdaptor#setInitialBundleStartLevel(int)
	 */
	public void setInitialBundleStartLevel(int value) {
		storage.setInitialBundleStartLevel(value);
	}

	/**
	 * This method calls all configured adaptor hook  {@link AdaptorHook#createFrameworkLog()} methods 
	 * until the first one returns a non-null value.  If none of the adaptor hooks return a non-null
	 * value then a framework log implementation which does nothing is returned.
	 * @see FrameworkAdaptor#getFrameworkLog()
	 */
	public FrameworkLog getFrameworkLog() {
		if (log != null)
			return log;
		AdaptorHook[] adaptorHooks = getHookRegistry().getAdaptorHooks();
		for (int i = 0; i < adaptorHooks.length; i++) {
			log = adaptorHooks[i].createFrameworkLog();
			if (log != null)
				return log;
		}
		log = new FrameworkLog() {
			public void log(FrameworkEvent frameworkEvent) {
				log(new FrameworkLogEntry(frameworkEvent.getBundle().getSymbolicName() == null ? frameworkEvent.getBundle().getLocation() : frameworkEvent.getBundle().getSymbolicName(), FrameworkLogEntry.ERROR, 0, "FrameworkEvent.ERROR", 0, frameworkEvent.getThrowable(), null)); //$NON-NLS-1$
			}

			public void log(FrameworkLogEntry logEntry) {
				System.err.print(logEntry.getEntry() + " "); //$NON-NLS-1$
				System.err.println(logEntry.getMessage());
				if (logEntry.getThrowable() != null)
					logEntry.getThrowable().printStackTrace(System.err);
			}

			public void setWriter(Writer newWriter, boolean append) {
				// do nothing
			}

			public void setFile(File newFile, boolean append) throws IOException {
				// do nothing
			}

			public File getFile() {
				// do nothing
				return null;
			}

			public void setConsoleLog(boolean consoleLog) {
				// do nothing
			}

			public void close() {
				// do nothing
			}
		};
		return log;
	}

	/**
	 * @see FrameworkAdaptor#createSystemBundleData()
	 */
	public BundleData createSystemBundleData() throws BundleException {
		return new SystemBundleData(this);
	}

	/**
	 * @see FrameworkAdaptor#getBundleWatcher()
	 */
	public BundleWatcher getBundleWatcher() {
		if (bundleWatcher != null)
			return bundleWatcher;
		final BundleWatcher[] watchers = hookRegistry.getWatchers();
		if (watchers.length == 0)
			return null;
		bundleWatcher = new BundleWatcher() {
			public void watchBundle(Bundle bundle, int type) {
				for (int i = 0; i < watchers.length; i++)
					watchers[i].watchBundle(bundle, type);
			}	
		};
		return bundleWatcher;
	}

	/**
	 * @see FrameworkAdaptor#getPlatformAdmin()
	 */
	public PlatformAdmin getPlatformAdmin() {
		return storage.getStateManager();
	}

	/**
	 * @see FrameworkAdaptor#getState()
	 */
	public State getState() {
		return storage.getStateManager().getSystemState();
	}

	/**
	 * This method calls all the configured classloading hooks {@link ClassLoadingHook#getBundleClassLoaderParent()} methods 
	 * until one returns a non-null value.
	 * @see FrameworkAdaptor#getBundleClassLoaderParent()
	 */
	public ClassLoader getBundleClassLoaderParent() {
		// ask the configured adaptor hooks first
		ClassLoader result = null;
		ClassLoadingHook[] cpManagerHooks = getHookRegistry().getClassLoadingHooks();
		for (int i = 0; i < cpManagerHooks.length; i++) {
			result = cpManagerHooks[i].getBundleClassLoaderParent();
			if (result != null)
				return result;
		}
		// none of the configured adaptor hooks gave use a parent loader; use the default
		return bundleClassLoaderParent;
	}

	/**
	 * This method calls all the configured adaptor hooks  {@link AdaptorHook#handleRuntimeError(Throwable)} methods.
	 * @see FrameworkAdaptor#handleRuntimeError(Throwable)
	 */
	public void handleRuntimeError(Throwable error) {
		AdaptorHook[] adaptorHooks = getHookRegistry().getAdaptorHooks();
		for (int i = 0; i < adaptorHooks.length; i++)
			adaptorHooks[i].handleRuntimeError(error);
	}

	/**
	 * This method calls all the configured adaptor hooks {@link AdaptorHook#matchDNChain(String, String[])} methods 
	 * until one returns a true value.
	 * @see FrameworkAdaptor#matchDNChain(String, String[])
	 */
	public boolean matchDNChain(String pattern, String[] dnChain) {
		AdaptorHook[] adaptorHooks = getHookRegistry().getAdaptorHooks();
		for (int i = 0; i < adaptorHooks.length; i++)
			if (adaptorHooks[i].matchDNChain(pattern, dnChain))
				return true;
		return false;
	}

	/**
	 * Returns true if the {@link #frameworkStopping(BundleContext)} method has been called
	 * @return true if the framework is stopping
	 */
	public boolean isStopping() {
		return stopping;
	}

	/**
	 * Returns the event publisher for this BaseAdaptor
	 * @return the event publisher for this BaseAdaptor
	 */
	public EventPublisher getEventPublisher() {
		return eventPublisher;
	}

	/**
	 * Returns the <code>HookRegistry</code> object for this adaptor.
	 * @return the <code>HookRegistry</code> object for this adaptor.
	 */
	public HookRegistry getHookRegistry() {
		return hookRegistry;
	}

	/**
	 * Returns the system bundle's context
	 * @return the system bundle's context
	 */
	public BundleContext getContext() {
		return context;
	}

	/**
	 * Creates a bundle file object for the given content and base data. 
	 * This method must delegate to each configured bundle file factory 
	 * {@link BundleFileFactoryHook#createBundleFile(Object, BaseData, boolean)} method until one 
	 * factory returns a non-null value.  If no bundle file factory returns a non-null value 
	 * then the the default behavior will be performed. <p>
	 * If the specified content is <code>null</code> then the base content of the specified 
	 * bundledata must be found before calling any bundle file factories.
	 * @param content The object which contains the content of a bundle file. A value of 
	 * <code>null</code> indicates that the storage must find the base content for the 
	 * specified BaseData.
	 * @param data The BaseData associated with the content
	 * @return a BundleFile object.
	 * @throws IOException if an error occured while creating the BundleFile
	 */
	public BundleFile createBundleFile(Object content, BaseData data) throws IOException {
		return storage.createBundleFile(content, data);
	}

	/**
	 * Returns true if the persistent storage is read-only
	 * @return true if the persistent storage is read-only
	 */
	public boolean isReadOnly() {
		return storage.isReadOnly();
	}

	/*
	 * This is an experimental method to allow adaptors to replace the storage implementation by 
	 * extending BaseAdaptor and overriding this method.  This method is experimental.
	 * @return a base storage object.
	 */
	protected BaseStorage getStorage() {
		if (storage == null)
			storage = BaseStorage.getInstance();
		return storage;
	}
}
