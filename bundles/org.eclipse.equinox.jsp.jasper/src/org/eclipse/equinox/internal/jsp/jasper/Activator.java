/*******************************************************************************
 * Copyright (c) 2005, 2017 Cognos Incorporated, IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Cognos Incorporated - initial API and implementation
 *     IBM Corporation - bug fixes and enhancements
 *******************************************************************************/
package org.eclipse.equinox.internal.jsp.jasper;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class Activator implements BundleActivator, ServiceTrackerCustomizer<PackageAdmin, PackageAdmin> {

	private ServiceTracker<?, PackageAdmin> packageAdminTracker;
	private static PackageAdmin packageAdmin;
	private volatile static Bundle thisBundle;
	private BundleContext bundleContext;

	@Override
	public void start(BundleContext context) throws Exception {
		// disable the JSR99 compiler that does not work in OSGi;
		// This will convince jasper to use the JDTCompiler that invokes ecj (see JSP-21
		// on the glassfish bug-tracker)
		System.setProperty("org.apache.jasper.compiler.disablejsr199", Boolean.TRUE.toString()); //$NON-NLS-1$
		this.bundleContext = context;
		thisBundle = context.getBundle();
		packageAdminTracker = new ServiceTracker<>(context, PackageAdmin.class, this);
		packageAdminTracker.open();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		packageAdminTracker.close();
		packageAdminTracker = null;
		thisBundle = null;
		this.bundleContext = null;
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

	public static synchronized Bundle getBundle(Class<?> clazz) {
		return FrameworkUtil.getBundle(clazz);
	}

	public static Bundle[] getFragments(Bundle bundle) {
		if (packageAdmin == null) {
			throw new IllegalStateException("Not started"); //$NON-NLS-1$
		}

		return packageAdmin.getFragments(bundle);
	}

	public static Bundle getJasperBundle() {
		Bundle bundle = getBundle(org.apache.jasper.servlet.JspServlet.class);
		if (bundle != null) {
			return bundle;
		}

		if (thisBundle == null) {
			throw new IllegalStateException("Not started"); //$NON-NLS-1$
		}

		ExportedPackage[] exportedPackages = packageAdmin.getExportedPackages("org.apache.jasper.servlet"); //$NON-NLS-1$
		for (ExportedPackage exportedPackage : exportedPackages) {
			Bundle[] importingBundles = exportedPackage.getImportingBundles();
			for (Bundle importingBundle : importingBundles) {
				if (thisBundle.equals(importingBundle)) {
					return exportedPackage.getExportingBundle();
				}
			}
		}
		return null;
	}
}
