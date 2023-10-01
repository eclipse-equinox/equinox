/*******************************************************************************
 * Copyright (c) 2005, 2015 Cognos Incorporated, IBM Corporation and others.
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
 *     Raymond Augé <raymond.auge@liferay.com> - Bug 436698
 *******************************************************************************/

package org.eclipse.equinox.http.servlet.internal;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import org.eclipse.equinox.http.servlet.ExtendedHttpService;
import org.eclipse.equinox.http.servlet.internal.servlet.ProxyServlet;
import org.eclipse.equinox.http.servlet.internal.util.*;
import org.osgi.framework.*;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.http.runtime.HttpServiceRuntimeConstants;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class Activator
	implements BundleActivator, ServiceTrackerCustomizer<HttpServlet, HttpTuple> {

	/**
	 * 
	 */
	private static final Random RANDOM = new Random();
	private static final String DEFAULT_SERVICE_DESCRIPTION = "Equinox Servlet Bridge"; //$NON-NLS-1$
	private static final String DEFAULT_SERVICE_VENDOR = "Eclipse.org"; //$NON-NLS-1$
	private static final String PROP_GLOBAL_WHITEBOARD = "equinox.http.global.whiteboard"; //$NON-NLS-1$
	public static final String UNIQUE_SERVICE_ID = "equinox.http.id"; //$NON-NLS-1$
	private static final String[] HTTP_SERVICES_CLASSES = new String[] {
		HttpService.class.getName(), ExtendedHttpService.class.getName()
	};

	private static volatile BundleContext context;
	private static ConcurrentMap<ProxyServlet, Object> registrations =
		new ConcurrentHashMap<>();

	private ServiceTracker<HttpServlet, HttpTuple> serviceTracker;

	public static void addProxyServlet(ProxyServlet proxyServlet) {
		Object previousRegistration = registrations.putIfAbsent(
			proxyServlet, proxyServlet);
		BundleContext currentContext = context;
		try {
			if (!(previousRegistration instanceof ServiceRegistration) &&
				(currentContext != null)) {
				ServiceRegistration<HttpServlet> serviceRegistration =
					currentContext.registerService(
						HttpServlet.class, proxyServlet,
						new Hashtable<String, Object>());

				registrations.put(proxyServlet, serviceRegistration);
			}
		} catch (IllegalStateException ex) {
			//If the currentContext is no longer valid.
			return;
		}
	}

	public static void unregisterHttpService(ProxyServlet proxyServlet) {
		Object registration = registrations.remove(proxyServlet);

		if (registration instanceof ServiceRegistration) {
			ServiceRegistration<?> serviceRegistration =
				(ServiceRegistration<?>)registration;

			serviceRegistration.unregister();
		}
	}

	public void start(BundleContext bundleContext) throws Exception {
		context = bundleContext;

		processRegistrations();

		serviceTracker = new ServiceTracker<>(
			bundleContext, HttpServlet.class, this);

		serviceTracker.open();
	}

	public void stop(BundleContext bundleContext) throws Exception {
		serviceTracker.close();
		serviceTracker = null;
		context = null;
	}

	public HttpTuple addingService(
		ServiceReference<HttpServlet> serviceReference) {
		BundleContext currentContext = context;
		if (currentContext == null) {
			return null;
		}

		try {
			HttpServlet httpServlet = currentContext.getService(serviceReference);

			if (!(httpServlet instanceof ProxyServlet)) {
				currentContext.ungetService(serviceReference);
				return null;
			}

			ProxyServlet proxyServlet = (ProxyServlet)httpServlet;

			ServletConfig servletConfig = proxyServlet.getServletConfig();
			ServletContext servletContext = servletConfig.getServletContext();

			Dictionary<String, Object> serviceProperties =
				new Hashtable<>(3);

			Enumeration<String> initparameterNames =
				servletConfig.getInitParameterNames();

			while (initparameterNames.hasMoreElements()) {
				String name = initparameterNames.nextElement();

				serviceProperties.put(
					name, servletConfig.getInitParameter(name));
			}

			if (serviceProperties.get(Constants.SERVICE_VENDOR) == null) {
				serviceProperties.put(
					Constants.SERVICE_VENDOR, DEFAULT_SERVICE_VENDOR);
			}

			if (serviceProperties.get(Constants.SERVICE_DESCRIPTION) == null) {
				serviceProperties.put(
					Constants.SERVICE_DESCRIPTION, DEFAULT_SERVICE_DESCRIPTION);
			}

			Object httpServiceEndpointObj = serviceProperties.get(HttpServiceRuntimeConstants.HTTP_SERVICE_ENDPOINT);

			if (httpServiceEndpointObj == null) {
				String[] httpServiceEndpoints = getHttpServiceEndpoints(
					serviceProperties, servletContext, servletConfig.getServletName());

				serviceProperties.put(
					HttpServiceRuntimeConstants.HTTP_SERVICE_ENDPOINT,
					httpServiceEndpoints);
			}
			else {
				List<String> httpServiceEndpoints = new ArrayList<>();

				String contextPath = servletContext.getContextPath();

				for (String httpServiceEndpoint : StringPlus.from(httpServiceEndpointObj)) {
					if (!httpServiceEndpoint.startsWith(Const.HTTP.concat(":")) && !httpServiceEndpoint.startsWith(contextPath)) { //$NON-NLS-1$
						httpServiceEndpoint = contextPath + httpServiceEndpoint;
					}

					httpServiceEndpoints.add(httpServiceEndpoint);
				}

				serviceProperties.put(
					HttpServiceRuntimeConstants.HTTP_SERVICE_ENDPOINT,
					httpServiceEndpoints);
			}

			// need a unique id for our service to match old HttpService HttpContext
			serviceProperties.put(UNIQUE_SERVICE_ID, RANDOM.nextLong());
			// white board support
			// determine if the system bundle context should be used:
			boolean useSystemContext = Boolean.valueOf(currentContext.getProperty(PROP_GLOBAL_WHITEBOARD));
			BundleContext trackingContext = useSystemContext ? currentContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION).getBundleContext() : currentContext;
			HttpServiceRuntimeImpl httpServiceRuntime = new HttpServiceRuntimeImpl(
				trackingContext, currentContext, servletContext, serviceProperties);
			httpServiceRuntime.open();

			proxyServlet.setHttpServiceRuntimeImpl(httpServiceRuntime);

			// imperative API support;
			// the http service must be registered first so we can get its service id
			HttpServiceFactory httpServiceFactory = new HttpServiceFactory(httpServiceRuntime);
			ServiceRegistration<?> hsfRegistration = currentContext.registerService(
				HTTP_SERVICES_CLASSES, httpServiceFactory, serviceProperties);

			serviceProperties.put(HttpServiceRuntimeConstants.HTTP_SERVICE_ID, Collections.singletonList(hsfRegistration.getReference().getProperty(Constants.SERVICE_ID)));

			ServiceRegistration<HttpServiceRuntime> hsrRegistration =
				currentContext.registerService(
					HttpServiceRuntime.class, httpServiceRuntime,
					serviceProperties);

			httpServiceRuntime.setHsrRegistration(hsrRegistration);

			return new HttpTuple(
				proxyServlet, httpServiceFactory, hsfRegistration,
				httpServiceRuntime, hsrRegistration);
		} catch (IllegalStateException ex) {
			//If the currentContext is no longer valid.
			return null;
		}
	}

	public void modifiedService(
		ServiceReference<HttpServlet> serviceReference, HttpTuple httpTuple) {

		removedService(serviceReference, httpTuple);
		addingService(serviceReference);
	}

	public void removedService(
		ServiceReference<HttpServlet> serviceReference, HttpTuple httpTuple) {
		BundleContext currentContext = context;
		if (currentContext != null) {
			try {
				currentContext.ungetService(serviceReference);
				httpTuple.destroy();
			} catch (IllegalStateException ex) {
				//If the currentContext is no longer valid.
				return;
			}
		}
	}

	private String[] getHttpServiceEndpoints(
		Dictionary<String, Object> serviceProperties, ServletContext servletContext, String servletName) {

		List<String> httpServiceEndpoints = new ArrayList<>();

		String contextPath = (String)serviceProperties.get(Const.CONTEXT_PATH);

		if ((contextPath != null)) {
			String httpHost = (String)serviceProperties.get(Const.HTTP_HOST);
			String httpPort = (String)serviceProperties.get(Const.HTTP_PORT);

			if (httpPort != null) {
				if (httpHost == null) {
					String endpoint = assembleEndpoint(Const.HTTP, Const.LOCALHOST, httpPort, contextPath);
					httpServiceEndpoints.add(endpoint);
				}
				else {
					String endpoint = assembleEndpoint(Const.HTTP, httpHost, httpPort, contextPath);
					httpServiceEndpoints.add(endpoint);
				}
			}

			String httpsHost = (String)serviceProperties.get(Const.HTTPS_HOST);
			String httpsPort = (String)serviceProperties.get(Const.HTTPS_PORT);

			if (httpsPort != null) {
				if (httpsHost == null) {
					String endpoint = assembleEndpoint(Const.HTTPS, Const.LOCALHOST, httpsPort, contextPath);
					httpServiceEndpoints.add(endpoint);
				}
				else {
					String endpoint = assembleEndpoint(Const.HTTPS, httpHost, httpsPort, contextPath);
					httpServiceEndpoints.add(endpoint);
				}
			}

			if (!httpServiceEndpoints.isEmpty()) {
				return httpServiceEndpoints.toArray(new String[0]);
			}
		}

		contextPath = servletContext.getContextPath();

		ServletRegistration servletRegistration = null;
		try {
			servletRegistration = servletContext.getServletRegistration(servletName);
		} catch (UnsupportedOperationException e) {
			StringBuilder sb = new StringBuilder();
			sb.append("Could not find the servlet registration for the servlet: "); //$NON-NLS-1$
			sb.append(servletName);
			sb.append(" The Http Service will not be able to locate it's root path."); //$NON-NLS-1$
			sb.append(" This can be overcome by specifying an init-param with name 'osgi.http.endpoint'"); //$NON-NLS-1$
			sb.append(" and value equal to the servlet mapping minus the glob character '*'."); //$NON-NLS-1$
			servletContext.log(sb.toString());
		}

		if (servletRegistration == null) {
			return new String[0];
		}

		Collection<String> mappings = servletRegistration.getMappings();

		for (String mapping : mappings) {
			if (mapping.indexOf('/') == 0) {
				if (mapping.charAt(mapping.length() - 1) == '*') {
					mapping = mapping.substring(0, mapping.length() - 1);

					if ((mapping.length() > 1) &&
						(mapping.charAt(mapping.length() - 1) != '/')) {

						mapping += '/';
					}
				}

				httpServiceEndpoints.add(contextPath + mapping);
			}
		}

		return httpServiceEndpoints.toArray(new String[0]);
	}

	private String assembleEndpoint(String protocol, String host, String port, String contextPath) {
		StringBuilder sb = new StringBuilder();
		sb.append(protocol);
		sb.append(Const.PROTOCOL);
		sb.append(host);
		sb.append(':');
		sb.append(port);
		sb.append(contextPath);
		if (sb.charAt(sb.length() - 1) != '/') {
			sb.append('/');
		}
		return sb.toString();
	}

	private void processRegistrations() {
		BundleContext currentContext = context;
		if (currentContext == null) {
			return;
		}

		for (Entry<ProxyServlet, Object> entry : registrations.entrySet()) {
			ProxyServlet proxyServlet = entry.getKey();
			Object value = entry.getValue();

			try {
				if (!(value instanceof ServiceRegistration)) {
					ServiceRegistration<HttpServlet> serviceRegistration =
						currentContext.registerService(
							HttpServlet.class, proxyServlet,
							new Hashtable<String, Object>());

					entry.setValue(serviceRegistration);
				}
			} catch (IllegalStateException ex) {
				//If the currentContext is no longer valid.
				return;
			}
		}
	}

}
