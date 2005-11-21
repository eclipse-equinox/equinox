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
package org.eclipse.core.internal.runtime;

import java.util.Date;
import java.util.ResourceBundle;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.localization.BundleLocalization;
import org.osgi.framework.*;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

/**
 * The class contains a set of helper methods for the runtime content plugin.
 * The following utility methods are supplied:
 * - provides framework log
 * - provides some bundle discovery funtionality
 * - provides some location services
 * 
 * The closeServices() method should be called before the plugin is stopped.
 *  
 * This class can only be used if OSGi plugin is available.
 * 
 * @since org.eclipse.equinox.common 1.0
 */
public class CommonOSGiUtils {
	private ServiceTracker logTracker = null;
	private ServiceTracker bundleTracker = null;
	private ServiceTracker instanceLocationTracker = null;
	private ServiceTracker localizationTracker = null;

	// OSGI system properties.  Copied from EclipseStarter
	public static final String PROP_INSTANCE_AREA = "osgi.instance.area"; //$NON-NLS-1$

	private static final CommonOSGiUtils singleton = new CommonOSGiUtils();

	public static CommonOSGiUtils getDefault() {
		return singleton;
	}

	/**
	 * Private constructor to block instance creation.
	 */
	private CommonOSGiUtils() {
		super();
		initServices();
	}

	/**
	 * Print a debug message to the console. 
	 * Pre-pend the message with the current date and the name of the current thread.
	 */
	public static void message(String message) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(new Date(System.currentTimeMillis()));
		buffer.append(" - ["); //$NON-NLS-1$
		buffer.append(Thread.currentThread().getName());
		buffer.append("] "); //$NON-NLS-1$
		buffer.append(message);
		System.out.println(buffer.toString());
	}

	private void initServices() {
		BundleContext context = Activator.getContext();
		if (context == null) {
			message("CommonOSGiUtils called before plugin started"); //$NON-NLS-1$
			return;
		}

		logTracker = new ServiceTracker(context, FrameworkLog.class.getName(), null);
		logTracker.open();

		bundleTracker = new ServiceTracker(context, PackageAdmin.class.getName(), null);
		bundleTracker.open();

		// locations

		final String FILTER_PREFIX = "(&(objectClass=org.eclipse.osgi.service.datalocation.Location)(type="; //$NON-NLS-1$
		Filter filter = null;
		try {
			filter = context.createFilter(FILTER_PREFIX + PROP_INSTANCE_AREA + "))"); //$NON-NLS-1$
		} catch (InvalidSyntaxException e) {
			// ignore this.  It should never happen as we have tested the above format.
		}
		instanceLocationTracker = new ServiceTracker(context, filter, null);
		instanceLocationTracker.open();
	}

	void closeServices() {
		if (localizationTracker != null) {
			localizationTracker.close();
			localizationTracker = null;
		}
		if (logTracker != null) {
			logTracker.close();
			logTracker = null;
		}
		if (bundleTracker != null) {
			bundleTracker.close();
			bundleTracker = null;
		}
		if (instanceLocationTracker != null) {
			instanceLocationTracker.close();
			instanceLocationTracker = null;
		}
	}

	public FrameworkLog getFrameworkLog() {
		if (logTracker != null)
			return (FrameworkLog) logTracker.getService();
		message("Log tracker is not set"); //$NON-NLS-1$
		return null;
	}

	public Bundle[] getFragments(Bundle bundle) {
		if (bundleTracker == null) {
			message("Bundle tracker is not set"); //$NON-NLS-1$
			return null;
		}
		PackageAdmin packageAdmin = (PackageAdmin) bundleTracker.getService();
		if (packageAdmin == null)
			return null;
		return packageAdmin.getFragments(bundle);
	}

	public Location getInstanceLocation() {
		if (instanceLocationTracker != null)
			return (Location) instanceLocationTracker.getService();
		else
			return null;
	}

	public ResourceBundle getLocalization(Bundle bundle, String locale) {
		if (localizationTracker == null) {
			BundleContext context = Activator.getContext();
			if (context == null) {
				message("ResourceTranslator called before plugin is started"); //$NON-NLS-1$
				return null;
			}
			localizationTracker = new ServiceTracker(context, BundleLocalization.class.getName(), null);
			localizationTracker.open();
		}
		BundleLocalization location = (BundleLocalization) localizationTracker.getService();
		if (location != null)
			return location.getLocalization(bundle, locale);

		return null;
	}

	/**
	 * Returns the bundle id of the bundle that contains the provided object, or
	 * <code>null</code> if the bundle could not be determined.
	 */
	public String getBundleId(Object object) {
		if (object == null)
			return null;
		if (bundleTracker == null) {
			message("Bundle tracker is not set"); //$NON-NLS-1$
			return null;
		}
		PackageAdmin packageAdmin = (PackageAdmin) bundleTracker.getService();
		if (packageAdmin == null)
			return null;

		Bundle source = packageAdmin.getBundle(object.getClass());
		if (source != null && source.getSymbolicName() != null)
			return source.getSymbolicName();
		return null;
	}

}
