/*******************************************************************************
 * Copyright (c) 2005, 2007 Cognos Incorporated, IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Cognos Incorporated - initial API and implementation
 *     IBM Corporation - bug fixes and enhancements
 *******************************************************************************/

package org.eclipse.equinox.http.registry.internal;

import org.eclipse.core.runtime.IExtensionRegistry;
import org.osgi.framework.*;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class Activator implements BundleActivator, ServiceTrackerCustomizer {

	private ServiceTracker httpServiceTracker;
	private ServiceTracker packageAdminTracker;
	private ServiceTracker registryTracker;

	private PackageAdmin packageAdmin;
	private IExtensionRegistry registry;
	private BundleContext context;

	public void start(BundleContext context) throws Exception {
		this.context = context;
		packageAdminTracker = new ServiceTracker(context, PackageAdmin.class.getName(), this);
		packageAdminTracker.open();

		registryTracker = new ServiceTracker(context, IExtensionRegistry.class.getName(), this);
		registryTracker.open();
	}

	public void stop(BundleContext context) throws Exception {
		packageAdminTracker.close();
		packageAdminTracker = null;
		registryTracker.close();
		registryTracker = null;
		this.context = null;
	}

	public Object addingService(ServiceReference reference) {
		Object service = context.getService(reference);

		if (service instanceof PackageAdmin && packageAdmin == null)
			packageAdmin = (PackageAdmin) service;

		if (service instanceof IExtensionRegistry && registry == null)
			registry = (IExtensionRegistry) service;

		if (packageAdmin != null && registry != null) {
			httpServiceTracker = new HttpServiceTracker(context, packageAdmin, registry);
			httpServiceTracker.open();
		}

		return service;
	}

	public void modifiedService(ServiceReference reference, Object service) {
		// ignore
	}

	public void removedService(ServiceReference reference, Object service) {
		if (service == packageAdmin)
			packageAdmin = null;

		if (service == registry)
			registry = null;

		if (packageAdmin == null || registry == null) {
			if (httpServiceTracker != null) {
				httpServiceTracker.close();
				httpServiceTracker = null;
			}
		}
		context.ungetService(reference);
	}

}
