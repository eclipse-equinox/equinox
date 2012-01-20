/*******************************************************************************
 * Copyright (c) 2005, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.runtime;

import java.net.URL;
import java.util.*;
import org.eclipse.core.internal.boot.PlatformURLBaseConnection;
import org.eclipse.core.internal.boot.PlatformURLHandler;
import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.equinox.log.ExtendedLogReaderService;
import org.eclipse.equinox.log.ExtendedLogService;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.localization.BundleLocalization;
import org.eclipse.osgi.service.urlconversion.URLConverter;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.url.URLConstants;
import org.osgi.service.url.URLStreamHandlerService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * The Common runtime plugin class.
 * 
 * This class can only be used if OSGi plugin is available.
 */
public class Activator implements BundleActivator {

	/**
	 * Table to keep track of all the URL converter services.
	 */
	private static Map urlTrackers = new HashMap();
	private static BundleContext bundleContext;
	private static Activator singleton;
	private ServiceRegistration platformURLConverterService = null;
	private ServiceRegistration adapterManagerService = null;
	private ServiceTracker installLocationTracker = null;
	private ServiceTracker instanceLocationTracker = null;
	private ServiceTracker configLocationTracker = null;
	private ServiceTracker bundleTracker = null;
	private ServiceTracker debugTracker = null;
	private ServiceTracker logTracker = null;
	private ServiceTracker localizationTracker = null;

	/*
	 * Returns the singleton for this Activator. Callers should be aware that
	 * this will return null if the bundle is not active.
	 */
	public static Activator getDefault() {
		return singleton;
	}

	/* (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		bundleContext = context;
		singleton = this;
		RuntimeLog.setLogWriter(getPlatformWriter(context));
		Dictionary urlProperties = new Hashtable();
		urlProperties.put("protocol", "platform"); //$NON-NLS-1$ //$NON-NLS-2$
		platformURLConverterService = context.registerService(URLConverter.class.getName(), new PlatformURLConverter(), urlProperties);
		adapterManagerService = context.registerService(IAdapterManager.class.getName(), AdapterManager.getDefault(), null);
		installPlatformURLSupport();
	}

	private PlatformLogWriter getPlatformWriter(BundleContext context) {
		ServiceReference logRef = context.getServiceReference(ExtendedLogService.class.getName());
		ServiceReference readerRef = context.getServiceReference(ExtendedLogReaderService.class.getName());
		ServiceReference packageAdminRef = context.getServiceReference(PackageAdmin.class.getName());
		if (logRef == null || readerRef == null || packageAdminRef == null)
			return null;
		ExtendedLogService logService = (ExtendedLogService) context.getService(logRef);
		ExtendedLogReaderService readerService = (ExtendedLogReaderService) context.getService(readerRef);
		PackageAdmin packageAdmin = (PackageAdmin) context.getService(packageAdminRef);
		if (logService == null || readerService == null || packageAdmin == null)
			return null;
		PlatformLogWriter writer = new PlatformLogWriter(logService, packageAdmin, context.getBundle());
		readerService.addLogListener(writer, writer);
		return writer;
	}

	/*
	 * Return the configuration location service, if available.
	 */
	public Location getConfigurationLocation() {
		if (configLocationTracker == null) {
			Filter filter = null;
			try {
				filter = bundleContext.createFilter(Location.CONFIGURATION_FILTER);
			} catch (InvalidSyntaxException e) {
				// should not happen
			}
			configLocationTracker = new ServiceTracker(bundleContext, filter, null);
			configLocationTracker.open();
		}
		return (Location) configLocationTracker.getService();
	}

	/*
	 * Return the debug options service, if available.
	 */
	public DebugOptions getDebugOptions() {
		if (debugTracker == null) {
			debugTracker = new ServiceTracker(bundleContext, DebugOptions.class.getName(), null);
			debugTracker.open();
		}
		return (DebugOptions) debugTracker.getService();
	}

	/*
	 * Return the framework log service, if available.
	 */
	public FrameworkLog getFrameworkLog() {
		if (logTracker == null) {
			logTracker = new ServiceTracker(bundleContext, FrameworkLog.class.getName(), null);
			logTracker.open();
		}
		return (FrameworkLog) logTracker.getService();
	}

	/*
	 * Return the instance location service, if available.
	 */
	public Location getInstanceLocation() {
		if (instanceLocationTracker == null) {
			Filter filter = null;
			try {
				filter = bundleContext.createFilter(Location.INSTANCE_FILTER);
			} catch (InvalidSyntaxException e) {
				// ignore this.  It should never happen as we have tested the above format.
			}
			instanceLocationTracker = new ServiceTracker(bundleContext, filter, null);
			instanceLocationTracker.open();
		}
		return (Location) instanceLocationTracker.getService();
	}

	/**
	 * Return the resolved bundle with the specified symbolic name.
	 * 
	 * @see PackageAdmin#getBundles(String, String)
	 */
	public Bundle getBundle(String symbolicName) {
		PackageAdmin admin = getBundleAdmin();
		if (admin == null)
			return null;
		Bundle[] bundles = admin.getBundles(symbolicName, null);
		if (bundles == null)
			return null;
		//Return the first bundle that is not installed or uninstalled
		for (int i = 0; i < bundles.length; i++) {
			if ((bundles[i].getState() & (Bundle.INSTALLED | Bundle.UNINSTALLED)) == 0) {
				return bundles[i];
			}
		}
		return null;
	}

	/*
	 * Return the package admin service, if available.
	 */
	private PackageAdmin getBundleAdmin() {
		if (bundleTracker == null) {
			bundleTracker = new ServiceTracker(getContext(), PackageAdmin.class.getName(), null);
			bundleTracker.open();
		}
		return (PackageAdmin) bundleTracker.getService();
	}

	/*
	 * Return an array of fragments for the given bundle host.
	 */
	public Bundle[] getFragments(Bundle host) {
		PackageAdmin admin = getBundleAdmin();
		if (admin == null)
			return new Bundle[0];
		return admin.getFragments(host);
	}

	/*
	 * Return the install location service if available.
	 */
	public Location getInstallLocation() {
		if (installLocationTracker == null) {
			Filter filter = null;
			try {
				filter = bundleContext.createFilter(Location.INSTALL_FILTER);
			} catch (InvalidSyntaxException e) {
				// should not happen
			}
			installLocationTracker = new ServiceTracker(bundleContext, filter, null);
			installLocationTracker.open();
		}
		return (Location) installLocationTracker.getService();
	}

	/**
	 * Returns the bundle id of the bundle that contains the provided object, or
	 * <code>null</code> if the bundle could not be determined.
	 */
	public String getBundleId(Object object) {
		if (object == null)
			return null;
		PackageAdmin packageAdmin = getBundleAdmin();
		if (packageAdmin == null)
			return null;
		Bundle source = packageAdmin.getBundle(object.getClass());
		if (source != null && source.getSymbolicName() != null)
			return source.getSymbolicName();
		return null;
	}

	/**
	 * Returns the resource bundle responsible for location of the given bundle
	 * in the given locale. Does not return null.
	 * @throws MissingResourceException If the corresponding resource could not be found
	 */
	public ResourceBundle getLocalization(Bundle bundle, String locale) throws MissingResourceException {
		if (localizationTracker == null) {
			BundleContext context = Activator.getContext();
			if (context == null) {
				throw new MissingResourceException(CommonMessages.activator_resourceBundleNotStarted, bundle.getSymbolicName(), ""); //$NON-NLS-1$
			}
			localizationTracker = new ServiceTracker(context, BundleLocalization.class.getName(), null);
			localizationTracker.open();
		}
		BundleLocalization location = (BundleLocalization) localizationTracker.getService();
		ResourceBundle result = null;
		if (location != null)
			result = location.getLocalization(bundle, locale);
		if (result == null)
			throw new MissingResourceException(NLS.bind(CommonMessages.activator_resourceBundleNotFound, locale), bundle.getSymbolicName(), ""); //$NON-NLS-1$
		return result;
	}

	/* (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		closeURLTrackerServices();
		if (platformURLConverterService != null) {
			platformURLConverterService.unregister();
			platformURLConverterService = null;
		}
		if (adapterManagerService != null) {
			adapterManagerService.unregister();
			adapterManagerService = null;
		}
		if (installLocationTracker != null) {
			installLocationTracker.close();
			installLocationTracker = null;
		}
		if (configLocationTracker != null) {
			configLocationTracker.close();
			configLocationTracker = null;
		}
		if (bundleTracker != null) {
			bundleTracker.close();
			bundleTracker = null;
		}
		if (debugTracker != null) {
			debugTracker.close();
			debugTracker = null;
		}
		if (logTracker != null) {
			logTracker.close();
			logTracker = null;
		}
		if (instanceLocationTracker != null) {
			instanceLocationTracker.close();
			instanceLocationTracker = null;
		}
		if (localizationTracker != null) {
			localizationTracker.close();
			localizationTracker = null;
		}
		RuntimeLog.setLogWriter(null);
		bundleContext = null;
		singleton = null;
	}

	/*
	 * Return this bundle's context.
	 */
	static BundleContext getContext() {
		return bundleContext;
	}

	/*
	 * Let go of all the services that we acquired and kept track of.
	 */
	private static void closeURLTrackerServices() {
		synchronized (urlTrackers) {
			if (!urlTrackers.isEmpty()) {
				for (Iterator iter = urlTrackers.keySet().iterator(); iter.hasNext();) {
					String key = (String) iter.next();
					ServiceTracker tracker = (ServiceTracker) urlTrackers.get(key);
					tracker.close();
				}
				urlTrackers = new HashMap();
			}
		}
	}

	/*
	 * Return the URL Converter for the given URL. Return null if we can't
	 * find one.
	 */
	public static URLConverter getURLConverter(URL url) {
		String protocol = url.getProtocol();
		synchronized (urlTrackers) {
			ServiceTracker tracker = (ServiceTracker) urlTrackers.get(protocol);
			if (tracker == null) {
				// get the right service based on the protocol
				String FILTER_PREFIX = "(&(objectClass=" + URLConverter.class.getName() + ")(protocol="; //$NON-NLS-1$ //$NON-NLS-2$
				String FILTER_POSTFIX = "))"; //$NON-NLS-1$
				Filter filter = null;
				try {
					filter = getContext().createFilter(FILTER_PREFIX + protocol + FILTER_POSTFIX);
				} catch (InvalidSyntaxException e) {
					return null;
				}
				tracker = new ServiceTracker(getContext(), filter, null);
				tracker.open();
				// cache it in the registry
				urlTrackers.put(protocol, tracker);
			}
			return (URLConverter) tracker.getService();
		}
	}

	/**
	 * Register the platform URL support as a service to the URLHandler service
	 */
	private void installPlatformURLSupport() {
		PlatformURLPluginConnection.startup();
		PlatformURLFragmentConnection.startup();
		PlatformURLMetaConnection.startup();
		PlatformURLConfigConnection.startup();

		Location service = getInstallLocation();
		if (service != null)
			PlatformURLBaseConnection.startup(service.getURL());

		Hashtable properties = new Hashtable(1);
		properties.put(URLConstants.URL_HANDLER_PROTOCOL, new String[] {PlatformURLHandler.PROTOCOL});
		getContext().registerService(URLStreamHandlerService.class.getName(), new PlatformURLHandler(), properties);
	}

}
