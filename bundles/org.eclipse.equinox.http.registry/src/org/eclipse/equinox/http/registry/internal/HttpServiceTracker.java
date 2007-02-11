/*******************************************************************************
 * Copyright (c) 2005 Cognos Incorporated.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Cognos Incorporated - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.http.registry.internal;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.equinox.http.registry.NamedHttpContextService;
import org.osgi.framework.*;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

public class HttpServiceTracker extends ServiceTracker {

	private BundleContext context;
	private PackageAdmin packageAdmin;
	private IExtensionRegistry registry;

	Map httpContextManagers = new HashMap();
	private Map servletManagers = new HashMap();
	private Map resourceManagers = new HashMap();
	private ServiceRegistration registration;

	public HttpServiceTracker(BundleContext context, PackageAdmin packageAdmin, IExtensionRegistry registry) {
		super(context, HttpService.class.getName(), null);
		this.context = context;
		this.packageAdmin = packageAdmin;
		this.registry = registry;
	}

	public void open() {
		super.open();
		registration = context.registerService(NamedHttpContextService.class.getName(), new NamedHttpContextServiceImpl(), null);
	}

	public void close() {
		registration.unregister();
		registration = null;
		super.close();
	}

	public synchronized Object addingService(ServiceReference reference) {
		HttpService httpService = (HttpService) context.getService(reference);

		HttpContextManager httpContextManager = new HttpContextManager(httpService, packageAdmin, registry);
		httpContextManager.start();
		httpContextManagers.put(reference, httpContextManager);

		ServletManager servletManager = new ServletManager(httpService, httpContextManager, registry);
		servletManager.start();
		servletManagers.put(reference, servletManager);

		ResourceManager resourceManager = new ResourceManager(httpService, httpContextManager, registry);
		resourceManager.start();
		resourceManagers.put(reference, resourceManager);

		return httpService;
	}

	public void modifiedService(ServiceReference reference, Object service) {
		// ignored
	}

	public synchronized void removedService(ServiceReference reference, Object service) {
		HttpContextManager httpContextManager = (HttpContextManager) httpContextManagers.remove(reference);
		if (httpContextManager != null) {
			httpContextManager.stop();
		}

		ServletManager servletManager = (ServletManager) servletManagers.remove(reference);
		if (servletManager != null) {
			servletManager.stop();
		}

		ResourceManager resourceManager = (ResourceManager) resourceManagers.remove(reference);
		if (resourceManager != null) {
			resourceManager.stop();
		}
		super.removedService(reference, service);
	}

	public class NamedHttpContextServiceImpl implements NamedHttpContextService {

		public HttpContext getNamedHttpContext(ServiceReference httpServiceReference, String httpContextName) {
			synchronized (HttpServiceTracker.this) {
				HttpContextManager httpContextManager = (HttpContextManager) httpContextManagers.get(httpServiceReference);
				if (httpContextManager == null)
					return null;

				return httpContextManager.getHttpContext(httpContextName);
			}
		}
	}
}
