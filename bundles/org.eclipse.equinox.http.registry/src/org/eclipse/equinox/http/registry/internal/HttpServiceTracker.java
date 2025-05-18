/*******************************************************************************
 * Copyright (c) 2005, 2007 Cognos Incorporated, IBM Corporation and others.
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

public class HttpServiceTracker extends ServiceTracker<HttpService, HttpService> {

	private final PackageAdmin packageAdmin;
	private final IExtensionRegistry registry;

	private ServiceRegistration<?> registration;
	Map<ServiceReference<HttpService>, HttpRegistryManager> httpRegistryManagers = new HashMap<>();

	public HttpServiceTracker(BundleContext context, PackageAdmin packageAdmin, IExtensionRegistry registry) {
		super(context, HttpService.class, null);
		this.packageAdmin = packageAdmin;
		this.registry = registry;
	}

	@Override
	public void open() {
		super.open();
		registration = context.registerService(HttpContextExtensionService.class.getName(),
				new HttpContextExtensionServiceFactory(), null);
	}

	@Override
	public void close() {
		registration.unregister();
		registration = null;
		super.close();
	}

	@Override
	public synchronized HttpService addingService(ServiceReference<HttpService> reference) {
		HttpService httpService = super.addingService(reference);
		if (httpService == null) {
			return null;
		}

		HttpRegistryManager httpRegistryManager = new HttpRegistryManager(reference, httpService, packageAdmin,
				registry);
		httpRegistryManager.start();
		httpRegistryManagers.put(reference, httpRegistryManager);

		return httpService;
	}

	@Override
	public void modifiedService(ServiceReference<HttpService> reference, HttpService service) {
		// ignored
	}

	@Override
	public synchronized void removedService(ServiceReference<HttpService> reference, HttpService service) {
		HttpRegistryManager httpRegistryManager = httpRegistryManagers.remove(reference);
		if (httpRegistryManager != null) {
			httpRegistryManager.stop();
		}
		super.removedService(reference, service);
	}

	public class HttpContextExtensionServiceFactory implements ServiceFactory<HttpContextExtensionService> {

		@Override
		public HttpContextExtensionService getService(Bundle bundle,
				ServiceRegistration<HttpContextExtensionService> r) {
			return new HttpContextExtensionServiceImpl(bundle);
		}

		@Override
		public void ungetService(Bundle bundle, ServiceRegistration<HttpContextExtensionService> r,
				HttpContextExtensionService service) {
			// do nothing
		}
	}

	public class HttpContextExtensionServiceImpl implements HttpContextExtensionService {

		private final Bundle bundle;

		public HttpContextExtensionServiceImpl(Bundle bundle) {
			this.bundle = bundle;
		}

		@Override
		public HttpContext getHttpContext(ServiceReference<HttpService> httpServiceReference, String httpContextId) {
			synchronized (HttpServiceTracker.this) {
				HttpRegistryManager httpRegistryManager = httpRegistryManagers.get(httpServiceReference);
				if (httpRegistryManager == null) {
					return null;
				}

				return httpRegistryManager.getHttpContext(httpContextId, bundle);
			}
		}
	}
}
