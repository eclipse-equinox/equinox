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

package org.eclipse.equinox.http.servlet.internal;

import java.util.*;
import javax.servlet.ServletConfig;
import org.osgi.framework.*;
import org.osgi.service.http.HttpService;

public class Activator implements BundleActivator {

	private static final String DEFAULT_SERVICE_DESCRIPTION = "Equinox Servlet Bridge"; //$NON-NLS-1$
	private static final String DEFAULT_SERVICE_VENDOR = "Eclipse.org"; //$NON-NLS-1$

	private static BundleContext context;
	private static Map serviceRegistrations = new HashMap();

	public void start(BundleContext context) throws Exception {
		startHttpServiceProxy(context);
	}

	public void stop(BundleContext context) throws Exception {
		stopHttpServiceProxy(context);
	}

	private static synchronized void startHttpServiceProxy(BundleContext bundleContext) {
		context = bundleContext;
		Object[] proxyServlets = serviceRegistrations.keySet().toArray();
		for (int i = 0; i < proxyServlets.length; ++i) {
			ServiceRegistration registration = registerHttpService((ProxyServlet) proxyServlets[i]);
			serviceRegistrations.put(proxyServlets[i], registration);
		}
	}

	private static synchronized void stopHttpServiceProxy(BundleContext bundleContext) {
		Object[] proxyServlets = serviceRegistrations.keySet().toArray();
		for (int i = 0; i < proxyServlets.length; ++i) {
			ServiceRegistration registration = (ServiceRegistration) serviceRegistrations.put(proxyServlets[i], null);
			registration.unregister();
		}
		context = null;
	}

	static synchronized void addProxyServlet(ProxyServlet proxyServlet) {
		ServiceRegistration registration = null;
		if (context != null)
			registration = registerHttpService(proxyServlet);

		serviceRegistrations.put(proxyServlet, registration);
	}

	private static ServiceRegistration registerHttpService(ProxyServlet proxyServlet) {
		HttpServiceFactory factory = new HttpServiceFactory(proxyServlet);
		Dictionary serviceProperties = new Hashtable(2);
		ServletConfig config = proxyServlet.getServletConfig();
		Enumeration initparameterNames = config.getInitParameterNames();
		while (initparameterNames.hasMoreElements()) {
			String name = (String) initparameterNames.nextElement();
			serviceProperties.put(name, config.getInitParameter(name));
		}

		if (serviceProperties.get(Constants.SERVICE_VENDOR) == null)
			serviceProperties.put(Constants.SERVICE_VENDOR, DEFAULT_SERVICE_VENDOR);

		if (serviceProperties.get(Constants.SERVICE_DESCRIPTION) == null)
			serviceProperties.put(Constants.SERVICE_DESCRIPTION, DEFAULT_SERVICE_DESCRIPTION);

		return context.registerService(HttpService.class.getName(), factory, serviceProperties);
	}

	static synchronized void removeProxyServlet(ProxyServlet proxyServlet) {
		ServiceRegistration registration = (ServiceRegistration) serviceRegistrations.remove(proxyServlet);
		if (registration != null)
			registration.unregister();
	}
}
