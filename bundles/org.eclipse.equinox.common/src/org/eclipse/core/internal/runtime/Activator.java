/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
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
import java.util.*;
import org.eclipse.core.internal.boot.PlatformURLBaseConnection;
import org.eclipse.core.internal.boot.PlatformURLHandler;
import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.core.runtime.ServiceCaller;
import org.eclipse.equinox.log.ExtendedLogReaderService;
import org.eclipse.equinox.log.ExtendedLogService;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
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
	public static final String PLUGIN_ID = "org.eclipse.equinox.common"; //$NON-NLS-1$ 

	/**
	 * Table to keep track of all the URL converter services.
	 */
	private static Map<String, ServiceTracker<Object, URLConverter>> urlTrackers = new HashMap<>();
	private static BundleContext bundleContext;
	private static Activator singleton;
	private ServiceRegistration<URLConverter> platformURLConverterService = null;
	private ServiceRegistration<IAdapterManager> adapterManagerService = null;
	private final ServiceCaller<Location> installLocationTracker = new ServiceCaller<>(getClass(), Location.class, Location.INSTALL_FILTER);
	private final ServiceCaller<Location> instanceLocationTracker = new ServiceCaller<>(getClass(), Location.class, Location.INSTANCE_FILTER);
	private final ServiceCaller<Location> configLocationTracker = new ServiceCaller<>(getClass(), Location.class, Location.CONFIGURATION_FILTER);
	@SuppressWarnings("deprecation")
	private final ServiceCaller<PackageAdmin> bundleTracker = new ServiceCaller<>(getClass(), PackageAdmin.class);
	private final ServiceCaller<DebugOptions> debugTracker = new ServiceCaller<>(getClass(), DebugOptions.class);
	private final ServiceCaller<FrameworkLog> logTracker = new ServiceCaller<>(getClass(), FrameworkLog.class);
	private final ServiceCaller<BundleLocalization> localizationTracker = new ServiceCaller<>(getClass(), BundleLocalization.class);

	private ServiceRegistration<DebugOptionsListener> debugRegistration;

	/*
	 * Returns the singleton for this Activator. Callers should be aware that
	 * this will return null if the bundle is not active.
	 */
	public static Activator getDefault() {
		return singleton;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		bundleContext = context;
		singleton = this;

		RuntimeLog.setLogWriter(getPlatformWriter(context));
		Dictionary<String, Object> urlProperties = new Hashtable<>();
		urlProperties.put("protocol", "platform"); //$NON-NLS-1$ //$NON-NLS-2$
		platformURLConverterService = context.registerService(URLConverter.class, new PlatformURLConverter(), urlProperties);
		adapterManagerService = context.registerService(IAdapterManager.class, AdapterManager.getDefault(), null);
		installPlatformURLSupport();
		Hashtable<String, String> properties = new Hashtable<>(2);
		properties.put(DebugOptions.LISTENER_SYMBOLICNAME, PLUGIN_ID);
		debugRegistration = context.registerService(DebugOptionsListener.class, TracingOptions.DEBUG_OPTIONS_LISTENER, properties);
	}

	private PlatformLogWriter getPlatformWriter(BundleContext context) {
		ServiceReference<ExtendedLogService> logRef = context.getServiceReference(ExtendedLogService.class);
		ServiceReference<ExtendedLogReaderService> readerRef = context.getServiceReference(ExtendedLogReaderService.class);
		ServiceReference<PackageAdmin> packageAdminRef = context.getServiceReference(PackageAdmin.class);
		if (logRef == null || readerRef == null || packageAdminRef == null)
			return null;
		ExtendedLogService logService = context.getService(logRef);
		ExtendedLogReaderService readerService = context.getService(readerRef);
		PackageAdmin packageAdmin = context.getService(packageAdminRef);
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
		return configLocationTracker.current().orElse(null);
	}

	/*
	 * Return the debug options service, if available.
	 */
	public DebugOptions getDebugOptions() {
		return debugTracker.current().orElse(null);
	}

	/*
	 * Return the framework log service, if available.
	 */
	public FrameworkLog getFrameworkLog() {
		return logTracker.current().orElse(null);
	}

	/*
	 * Return the instance location service, if available.
	 */
	public Location getInstanceLocation() {
		return instanceLocationTracker.current().orElse(null);
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
		for (Bundle bundle : bundles) {
			if ((bundle.getState() & (Bundle.INSTALLED | Bundle.UNINSTALLED)) == 0) {
				return bundle;
			}
		}
		return null;
	}

	/*
	 * Return the package admin service, if available.
	 */
	private PackageAdmin getBundleAdmin() {
		return bundleTracker.current().orElse(null);
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
		return installLocationTracker.current().orElse(null);
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
			throw new MissingResourceException(CommonMessages.activator_resourceBundleNotStarted, bundle.getSymbolicName(), ""); //$NON-NLS-1$
		}
		BundleLocalization location = localizationTracker.current().orElse(null);
		ResourceBundle result = null;
		if (location != null)
			result = location.getLocalization(bundle, locale);
		if (result == null)
			throw new MissingResourceException(NLS.bind(CommonMessages.activator_resourceBundleNotFound, locale), bundle.getSymbolicName(), ""); //$NON-NLS-1$
		return result;
	}

	@Override
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
		if (debugRegistration != null) {
			debugRegistration.unregister();
			debugRegistration = null;
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
				for (ServiceTracker<Object, URLConverter> tracker : urlTrackers.values()) {
					tracker.close();
				}
				urlTrackers = new HashMap<>();
			}
		}
	}

	/*
	 * Return the URL Converter for the given URL. Return null if we can't
	 * find one.
	 */
	public static URLConverter getURLConverter(URL url) {
		BundleContext ctx = getContext();
		if (url == null || ctx == null) {
			return null;
		}
		String protocol = url.getProtocol();
		synchronized (urlTrackers) {
			ServiceTracker<Object, URLConverter> tracker = urlTrackers.get(protocol);
			if (tracker == null) {
				// get the right service based on the protocol
				String FILTER_PREFIX = "(&(objectClass=" + URLConverter.class.getName() + ")(protocol="; //$NON-NLS-1$ //$NON-NLS-2$
				String FILTER_POSTFIX = "))"; //$NON-NLS-1$
				Filter filter = null;
				try {
					filter = ctx.createFilter(FILTER_PREFIX + protocol + FILTER_POSTFIX);
				} catch (InvalidSyntaxException e) {
					return null;
				}
				tracker = new ServiceTracker<>(getContext(), filter, null);
				tracker.open();
				// cache it in the registry
				urlTrackers.put(protocol, tracker);
			}
			return tracker.getService();
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

		Hashtable<String, String[]> properties = new Hashtable<>(1);
		properties.put(URLConstants.URL_HANDLER_PROTOCOL, new String[] {PlatformURLHandler.PROTOCOL});
		getContext().registerService(URLStreamHandlerService.class.getName(), new PlatformURLHandler(), properties);
	}

}
