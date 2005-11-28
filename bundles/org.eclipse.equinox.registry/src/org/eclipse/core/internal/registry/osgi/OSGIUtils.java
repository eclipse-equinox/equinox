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

import java.util.Date;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.resolver.PlatformAdmin;
import org.osgi.framework.*;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

/**
 * The class contains a set of helper methods for the runtime content plugin.
 * The closeServices() method should be called before the plugin is stopped. 
 * 
 * @since org.eclipse.equinox.registry 1.0
 */
public class OSGIUtils {
	private ServiceTracker debugTracker = null;
	private ServiceTracker bundleTracker = null;
	// XXX platfromTracker is misspelt.
	private ServiceTracker platfromTracker = null;
	private ServiceTracker configurationLocationTracker = null;

	// OSGI system properties.  Copied from EclipseStarter
	public static final String PROP_CONFIG_AREA = "osgi.configuration.area"; //$NON-NLS-1$
	public static final String PROP_INSTANCE_AREA = "osgi.instance.area"; //$NON-NLS-1$

	private static final OSGIUtils singleton = new OSGIUtils();

	public static OSGIUtils getDefault() {
		return singleton;
	}

	/**
	 * Private constructor to block instance creation.
	 */
	private OSGIUtils() {
		super();
		initServices();
	}

	/**
	 * Print a debug message to the console. 
	 * Pre-pend the message with the current date and the name of the current thread.
	 */
	// XXX be careful using this method.  In general you should try to log first and then if 
	//  debug is on print to system out.  
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
			message("Registry OSGIUtils called before plugin started"); //$NON-NLS-1$
			return;
		}

		debugTracker = new ServiceTracker(context, DebugOptions.class.getName(), null);
		debugTracker.open();

		bundleTracker = new ServiceTracker(context, PackageAdmin.class.getName(), null);
		bundleTracker.open();

		platfromTracker = new ServiceTracker(context, PlatformAdmin.class.getName(), null);
		platfromTracker.open();

		// locations

		final String FILTER_PREFIX = "(&(objectClass=org.eclipse.osgi.service.datalocation.Location)(type="; //$NON-NLS-1$
		Filter filter = null;
		try {
			filter = context.createFilter(FILTER_PREFIX + PROP_CONFIG_AREA + "))"); //$NON-NLS-1$
		} catch (InvalidSyntaxException e) {
			// ignore this.  It should never happen as we have tested the above format.
		}
		configurationLocationTracker = new ServiceTracker(context, filter, null);
		configurationLocationTracker.open();

	}

	void closeServices() {
		if (debugTracker != null) {
			debugTracker.close();
			debugTracker = null;
		}
		if (bundleTracker != null) {
			bundleTracker.close();
			bundleTracker = null;
		}
		if (configurationLocationTracker != null) {
			configurationLocationTracker.close();
			configurationLocationTracker = null;
		}
		if (platfromTracker != null) {
			platfromTracker.close();
			platfromTracker = null;
		}
	}

	public boolean getBooleanDebugOption(String option, boolean defaultValue) {
		if (debugTracker == null) {
			message("Debug tracker is not set"); //$NON-NLS-1$
			return defaultValue;
		}
		DebugOptions options = (DebugOptions) debugTracker.getService();
		if (options != null) {
			String value = options.getOption(option);
			if (value != null)
				return value.equalsIgnoreCase("true"); //$NON-NLS-1$
		}
		return defaultValue;
	}

	public PackageAdmin getPackageAdmin() {
		if (bundleTracker == null) {
			message("Bundle tracker is not set"); //$NON-NLS-1$
			return null;
		}
		return (PackageAdmin) bundleTracker.getService();
	}

	public Bundle getBundle(String bundleName) {
		PackageAdmin packageAdmin = getPackageAdmin();
		if (packageAdmin == null)
			return null;
		Bundle[] bundles = packageAdmin.getBundles(bundleName, null);
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

	public Bundle[] getFragments(Bundle bundle) {
		PackageAdmin packageAdmin = getPackageAdmin();
		if (packageAdmin == null)
			return null;
		return packageAdmin.getFragments(bundle);
	}

	public boolean isFragment(Bundle bundle) {
		PackageAdmin packageAdmin = getPackageAdmin();
		if (packageAdmin == null)
			return false;
		return (packageAdmin.getBundleType(bundle) & PackageAdmin.BUNDLE_TYPE_FRAGMENT) > 0;
	}

	public Bundle[] getHosts(Bundle bundle) {
		PackageAdmin packageAdmin = getPackageAdmin();
		if (packageAdmin == null)
			return null;
		return packageAdmin.getHosts(bundle);
	}

	public Location getConfigurationLocation() {
		if (configurationLocationTracker != null)
			return (Location) configurationLocationTracker.getService();
		else
			return null;
	}

	public PlatformAdmin getPlatformAdmin() {
		if (platfromTracker != null)
			return (PlatformAdmin) platfromTracker.getService();
		else
			return null;
	}

}
