/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others
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
package org.eclipse.equinox.cm.test;

import org.osgi.framework.*;
import org.osgi.service.packageadmin.PackageAdmin;

public class Activator implements BundleActivator {

	private static PackageAdmin packageAdmin;
	private static BundleContext bundleContext;
	private ServiceReference<PackageAdmin> packageAdminRef;

	public void start(BundleContext context) throws Exception {
		packageAdminRef = context.getServiceReference(PackageAdmin.class);
		setPackageAdmin(context.getService(packageAdminRef));
		setBundleContext(context);
	}

	public void stop(BundleContext context) throws Exception {
		setBundleContext(null);
		setPackageAdmin(null);
		context.ungetService(packageAdminRef);
	}

	private static synchronized void setBundleContext(BundleContext context) {
		bundleContext = context;
	}

	private static synchronized void setPackageAdmin(PackageAdmin service) {
		packageAdmin = service;
	}

	static synchronized BundleContext getBundleContext() {
		return bundleContext;
	}

	static synchronized Bundle getBundle(String symbolicName) {
		if (packageAdmin == null)
			return null;

		Bundle[] bundles = packageAdmin.getBundles(symbolicName, null);
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
}
