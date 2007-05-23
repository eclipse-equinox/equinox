/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.jsp.jasper.registry;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class Activator implements BundleActivator, ServiceTrackerCustomizer {

	private ServiceTracker packageAdminTracker;
	private static PackageAdmin packageAdmin;
	private BundleContext context;

	public void start(BundleContext context) throws Exception {
		this.context = context;
		packageAdminTracker = new ServiceTracker(context, PackageAdmin.class.getName(), this);
		packageAdminTracker.open();
	}

	public void stop(BundleContext context) throws Exception {
		packageAdminTracker.close();
		packageAdminTracker = null;
		this.context = null;
	}

	public static synchronized Bundle getBundle(String symbolicName) {
		if (packageAdmin == null)
			throw new IllegalStateException("Not started"); //$NON-NLS-1$

		Bundle[] bundles = packageAdmin.getBundles(symbolicName, null);
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

	public Object addingService(ServiceReference reference) {
		synchronized (Activator.class) {
			packageAdmin = (PackageAdmin) context.getService(reference);
		}
		return packageAdmin;
	}

	public void modifiedService(ServiceReference reference, Object service) {
	}

	public void removedService(ServiceReference reference, Object service) {
		synchronized (Activator.class) {
			context.ungetService(reference);
			packageAdmin = null;
		}
	}
}
