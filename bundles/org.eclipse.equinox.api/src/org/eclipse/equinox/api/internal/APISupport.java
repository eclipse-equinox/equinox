/*******************************************************************************
 * Copyright (c) 2024 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *     Methods migrated from org.eclipse.core.internal.runtime.Activator contributed by:
 *      IBM Corporation - initial API and implementation
 *      Sergey Prigogin (Google) - use parameterized types (bug 442021)
 *      Christoph Laeubrich - Bug 567344 - Support registration of IAdapterFactory as OSGi Service
 *******************************************************************************/
package org.eclipse.equinox.api.internal;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.eclipse.osgi.service.urlconversion.URLConverter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

/**
 * API Bundle activator that is used to provide API service support and backward
 * compatibility
 */
public class APISupport implements BundleActivator {

	static final String PI_RUNTIME = "org.eclipse.core.runtime"; //$NON-NLS-1$
	public static final String PI_COMMON = "org.eclipse.equinox.common"; //$NON-NLS-1$
	public static final int PLUGIN_ERROR = 2;

	private static volatile ServiceTracker<IAdapterManager, IAdapterManager> adapterMangerTracker;
	private static volatile BundleContext bundleContext;
	private static final Map<String, ServiceTracker<Object, URLConverter>> urlTrackers = new HashMap<>();
	static volatile Bundle equinoxCommonBundle;
	private static ServiceTracker<PackageAdmin, PackageAdmin> adminTracker;

	@Override
	public void start(BundleContext context) throws Exception {
		context.registerService(DebugOptionsListener.class, TracingOptions.DEBUG_OPTIONS_LISTENER,
				FrameworkUtil.asDictionary(Map.of(DebugOptions.LISTENER_SYMBOLICNAME, PI_COMMON)));
		APISupport.bundleContext = context;
		for (Bundle bundle : context.getBundles()) {
			if (PI_COMMON.equals(bundle.getSymbolicName())) {
				// this emulates the previous behavior that when loading any class now in the
				// API bundle has activated equinox.common bundle
				bundle.start();
				equinoxCommonBundle = bundle;
			}
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		bundleContext = null;
		equinoxCommonBundle = null;
	}

	/**
	 * Returns a property either from the bundle or the system properties
	 * 
	 * @param property the property to fetch
	 * @return the value of the property
	 */
	public static String getProperty(String property) {
		BundleContext ctx = bundleContext;
		if (ctx != null) {
			return ctx.getProperty(property);
		}
		return System.getProperty(property);
	}

	/**
	 * @return the IAdapterManager service
	 */
	public static IAdapterManager getAdapterManager() {
		BundleContext context = Objects.requireNonNull(bundleContext, "bundle not started!");
		synchronized (APISupport.class) {
			if (adapterMangerTracker == null) {
				adapterMangerTracker = new ServiceTracker<IAdapterManager, IAdapterManager>(context,
						IAdapterManager.class, null);
				adapterMangerTracker.open();
			}
		}
		return Objects.requireNonNull(adapterMangerTracker.getService(),
				"No IAdapterManager service found in the OSGi registry!");
	}

	/**
	 * Log the given status to the runtime log
	 * 
	 * @param status the status to log
	 */
	public static void log(IStatus status) {
		if (equinoxCommonBundle != null) {
			try {
				Class<?> clazz = equinoxCommonBundle.loadClass("org.eclipse.core.internal.runtime.RuntimeLog");
				Method method = clazz.getMethod("log", IStatus.class);
				method.invoke(null, status);
				return;
			} catch (Exception e) {
			}
		}
		System.out.println(status);
	}

	/*
	 * Return the URL Converter for the given URL. Return null if we can't find one.
	 */
	public static URLConverter getURLConverter(URL url) {
		BundleContext ctx = bundleContext;
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
				tracker = new ServiceTracker<>(ctx, filter, null);
				tracker.open();
				// cache it in the registry
				urlTrackers.put(protocol, tracker);
			}
			return tracker.getService();
		}
	}

	public static Bundle[] getFragments(Bundle host) {
		BundleContext ctx = bundleContext;
		if (ctx == null) {
			return new Bundle[0];
		}
		synchronized (APISupport.class) {
			if (adminTracker == null) {
				adminTracker = new ServiceTracker<>(ctx, PackageAdmin.class, null);
				adminTracker.open();
			}
		}
		PackageAdmin service = adminTracker.getService();
		if (service == null) {
			return new Bundle[0];
		}
		return service.getFragments(host);
	}

	/**
	 * Returns the bundle id of the bundle that contains the provided object, or
	 * <code>null</code> if the bundle could not be determined.
	 */
	public static String getBundleId(Object object) {
		if (object == null)
			return null;
		Bundle source = FrameworkUtil.getBundle(object.getClass());
		if (source != null && source.getSymbolicName() != null)
			return source.getSymbolicName();
		return null;
	}

}
