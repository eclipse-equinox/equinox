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
package org.eclipse.core.internal.registry.osgi;

import org.eclipse.core.internal.registry.RegistryMessages;
import org.eclipse.core.internal.runtime.RuntimeLog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.osgi.framework.*;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

/**
 * The class contains a set of helper methods for the runtime content plugin.
 * The closeServices() method should be called before the plugin is stopped. 
 * 
 * @since org.eclipse.equinox.registry 3.2
 */
public class OSGIUtils {
	private ServiceTracker debugTracker = null;
	private ServiceTracker bundleTracker = null;
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

	private void initServices() {
		BundleContext context = Activator.getContext();
		if (context == null) {
			RuntimeLog.log(new Status(IStatus.ERROR, RegistryMessages.OWNER_NAME, 0, RegistryMessages.bundle_not_activated, null));
			return;
		}

		debugTracker = new ServiceTracker(context, DebugOptions.class.getName(), null);
		debugTracker.open();

		bundleTracker = new ServiceTracker(context, PackageAdmin.class.getName(), null);
		bundleTracker.open();

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
	}

	public boolean getBooleanDebugOption(String option, boolean defaultValue) {
		if (debugTracker == null) {
			RuntimeLog.log(new Status(IStatus.ERROR, RegistryMessages.OWNER_NAME, 0, RegistryMessages.bundle_not_activated, null));
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
			RuntimeLog.log(new Status(IStatus.ERROR, RegistryMessages.OWNER_NAME, 0, RegistryMessages.bundle_not_activated, null));
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
		if (configurationLocationTracker == null)
			return null;
		return (Location) configurationLocationTracker.getService();
	}
}
