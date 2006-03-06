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
package org.eclipse.core.internal.runtime;

import java.net.URL;
import java.util.*;
import org.eclipse.core.internal.boot.PlatformURLBaseConnection;
import org.eclipse.core.internal.boot.PlatformURLHandler;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.urlconversion.URLConverter;
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
	private static Activator bundle = null;
	private ServiceRegistration platformURLConverterService = null;
	private ServiceTracker installLocationTracker = null;
	private ServiceTracker configLocationTracker = null;
	private ServiceTracker bundleTracker = null;
	private ServiceTracker debugTracker = null;

	/**
	 * The bundle context associated this plug-in
	 */
	private static BundleContext bundleContext;

	public static Activator getDefault() {
		return bundle;
	}

	/**
	 * This method is called upon plug-in activation
	 */
	public void start(BundleContext context) throws Exception {
		bundleContext = context;
		bundle = this;

		Dictionary urlProperties = new Hashtable();
		urlProperties.put("protocol", "platform"); //$NON-NLS-1$ //$NON-NLS-2$
		platformURLConverterService = context.registerService(URLConverter.class.getName(), new PlatformURLConverter(), urlProperties);

		installPlatformURLSupport();
	}

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

	public DebugOptions getDebugOptions() {
		if (debugTracker == null) {
			debugTracker = new ServiceTracker(bundleContext, DebugOptions.class.getName(), null);
			debugTracker.open();
		}
		return (DebugOptions) debugTracker.getService();
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

	private PackageAdmin getBundleAdmin() {
		if (bundleTracker == null) {
			bundleTracker = new ServiceTracker(getContext(), PackageAdmin.class.getName(), null);
			bundleTracker.open();
		}
		return (PackageAdmin) bundleTracker.getService();
	}

	public Bundle[] getFragments(Bundle host) {
		PackageAdmin admin = getBundleAdmin();
		if (admin == null)
			return new Bundle[0];
		return admin.getFragments(host);
	}

	public URL getInstallLocation() {
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
		return ((Location) installLocationTracker.getService()).getURL();
	}

	/**
	 * This method is called when the plug-in is stopped
	 */
	public void stop(BundleContext context) throws Exception {
		CommonOSGiUtils.getDefault().closeServices();
		closeURLTrackerServices();
		if (platformURLConverterService != null) {
			platformURLConverterService.unregister();
			platformURLConverterService = null;
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
		bundleContext = null;
	}

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

		PlatformURLBaseConnection.startup(getInstallLocation());

		Hashtable properties = new Hashtable(1);
		properties.put(URLConstants.URL_HANDLER_PROTOCOL, new String[] {PlatformURLHandler.PROTOCOL});
		getContext().registerService(URLStreamHandlerService.class.getName(), new PlatformURLHandler(), properties);
	}

}
