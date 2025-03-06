/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others
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
package org.eclipse.equinox.compendium.tests;

import org.osgi.framework.*;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

/**
 * The activator class controls the plug-in life cycle
 */
@SuppressWarnings("deprecation") // PackageAdmin
public class Activator implements BundleActivator {
	public static String BUNDLE_COORDINATOR = "org.eclipse.equinox.coordinator"; //$NON-NLS-1$
	public static String BUNDLE_EVENT = "org.eclipse.equinox.event"; //$NON-NLS-1$
	public static String BUNDLE_METATYPE = "org.eclipse.equinox.metatype"; //$NON-NLS-1$
	public static String BUNDLE_USERADMIN = "org.eclipse.equinox.useradmin"; //$NON-NLS-1$

	private static Activator plugin;
	private static BundleContext context;

	private ServiceTracker packageAdminTracker;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.Plugins#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext bc) throws Exception {
		plugin = this;
		packageAdminTracker = new ServiceTracker(bc, PackageAdmin.class.getName(), null);
		packageAdminTracker.open();
		Activator.context = bc;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.Plugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext bc) throws Exception {
		if (packageAdminTracker != null) {
			packageAdminTracker.close();
			packageAdminTracker = null;
		}
		plugin = null;
	}

	private static Activator getDefault() {
		return plugin;
	}

	public static synchronized BundleContext getBundleContext() {
		return context;
	}

	public static synchronized Bundle getBundle(String symbolicName) {
		PackageAdmin packageAdmin = Activator.getDefault().getPackageAdmin();
		if (packageAdmin == null) {
			return null;
		}

		Bundle[] bundles = packageAdmin.getBundles(symbolicName, null);
		if (bundles == null) {
			return null;
		}
		for (Bundle bundle : bundles) {
			if ((bundle.getState() & (Bundle.INSTALLED | Bundle.UNINSTALLED)) == 0) {
				return bundle;
			}
		}
		return null;
	}

	private PackageAdmin getPackageAdmin() {
		if (packageAdminTracker == null) {
			return null;
		}
		return (PackageAdmin) packageAdminTracker.getService();
	}

}
