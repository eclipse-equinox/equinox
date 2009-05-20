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

import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.equinox.http.registry.HttpContextExtensionService;
import org.osgi.framework.*;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

public class HttpServiceTracker extends ServiceTracker {

	private BundleContext context;
	private PackageAdmin packageAdmin;
	private IExtensionRegistry registry;

	private ServiceRegistration registration;
	Map httpRegistryManagers = new HashMap();

	public HttpServiceTracker(BundleContext context, PackageAdmin packageAdmin, IExtensionRegistry registry) {
		super(context, HttpService.class.getName(), null);
		this.context = context;
		this.packageAdmin = packageAdmin;
		this.registry = registry;
	}

	public void open() {
		super.open();
		registration = context.registerService(HttpContextExtensionService.class.getName(), new HttpContextExtensionServiceFactory(), null);
	}

	public void close() {
		registration.unregister();
		registration = null;
		super.close();
	}

	public synchronized Object addingService(ServiceReference reference) {
		HttpService httpService = (HttpService) super.addingService(reference);
		if (httpService == null)
			return null;

		HttpRegistryManager httpRegistryManager = new HttpRegistryManager(reference, httpService, packageAdmin, registry);
		httpRegistryManager.start();
		httpRegistryManagers.put(reference, httpRegistryManager);

		return httpService;
	}

	public void modifiedService(ServiceReference reference, Object service) {
		// ignored
	}

	public synchronized void removedService(ServiceReference reference, Object service) {
		HttpRegistryManager httpRegistryManager = (HttpRegistryManager) httpRegistryManagers.remove(reference);
		if (httpRegistryManager != null) {
			httpRegistryManager.stop();
		}
		super.removedService(reference, service);
	}

	public class HttpContextExtensionServiceFactory implements ServiceFactory {

		public Object getService(Bundle bundle, ServiceRegistration registration) {
			return new HttpContextExtensionServiceImpl(bundle);
		}

		public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
			// do nothing
		}
	}

	public class HttpContextExtensionServiceImpl implements HttpContextExtensionService {

		private Bundle bundle;

		public HttpContextExtensionServiceImpl(Bundle bundle) {
			this.bundle = bundle;
		}

		public HttpContext getHttpContext(ServiceReference httpServiceReference, String httpContextId) {
			synchronized (HttpServiceTracker.this) {
				HttpRegistryManager httpRegistryManager = (HttpRegistryManager) httpRegistryManagers.get(httpServiceReference);
				if (httpRegistryManager == null)
					return null;

				return httpRegistryManager.getHttpContext(httpContextId, bundle);
			}
		}
	}
}
