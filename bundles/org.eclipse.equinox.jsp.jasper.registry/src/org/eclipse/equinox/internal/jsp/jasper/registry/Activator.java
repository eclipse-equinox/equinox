/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others
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
package org.eclipse.equinox.internal.jsp.jasper.registry;

import org.osgi.framework.*;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class Activator implements BundleActivator, ServiceTrackerCustomizer<PackageAdmin, PackageAdmin> {

	private ServiceTracker<?, PackageAdmin> packageAdminTracker;
	private static PackageAdmin packageAdmin;
	private BundleContext bundleContext;

	@Override
	public void start(BundleContext context) throws Exception {
		this.bundleContext = context;
		packageAdminTracker = new ServiceTracker<>(context, PackageAdmin.class, this);
		packageAdminTracker.open();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		packageAdminTracker.close();
		packageAdminTracker = null;
		this.bundleContext = null;
	}

	public static synchronized Bundle getBundle(String symbolicName) {
		if (packageAdmin == null)
			throw new IllegalStateException("Not started"); //$NON-NLS-1$

		Bundle[] bundles = packageAdmin.getBundles(symbolicName, null);
		if (bundles == null)
			return null;
		// Return the first bundle that is not installed or uninstalled
		for (Bundle bundle : bundles) {
			if ((bundle.getState() & (Bundle.INSTALLED | Bundle.UNINSTALLED)) == 0) {
				return bundle;
			}
		}
		return null;
	}

	@Override
	public PackageAdmin addingService(ServiceReference<PackageAdmin> reference) {
		synchronized (Activator.class) {
			packageAdmin = bundleContext.getService(reference);
		}
		return packageAdmin;
	}

	@Override
	public void modifiedService(ServiceReference<PackageAdmin> reference, PackageAdmin service) {
		// do nothing
	}

	@Override
	public void removedService(ServiceReference<PackageAdmin> reference, PackageAdmin service) {
		synchronized (Activator.class) {
			bundleContext.ungetService(reference);
			packageAdmin = null;
		}
	}
}
