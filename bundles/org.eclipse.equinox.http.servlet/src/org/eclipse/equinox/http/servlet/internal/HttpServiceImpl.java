/*******************************************************************************
 * Copyright (c) 2005, 2014 Cognos Incorporated, IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cognos Incorporated - initial API and implementation
 *     IBM Corporation - bug fixes and enhancements
 *     Raymond Augé <raymond.auge@liferay.com> - Bug 436698
 *******************************************************************************/

package org.eclipse.equinox.http.servlet.internal;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import javax.servlet.*;
import org.eclipse.equinox.http.servlet.ExtendedHttpService;
import org.eclipse.equinox.http.servlet.internal.context.*;
import org.eclipse.equinox.http.servlet.internal.context.ContextController.ServiceHolder;
import org.eclipse.equinox.http.servlet.internal.error.*;
import org.eclipse.equinox.http.servlet.internal.registration.*;
import org.eclipse.equinox.http.servlet.internal.registration.FilterRegistration;
import org.eclipse.equinox.http.servlet.internal.registration.Registration;
import org.eclipse.equinox.http.servlet.internal.registration.ServletRegistration;
import org.eclipse.equinox.http.servlet.internal.util.StringPlus;
import org.osgi.framework.Bundle;
import org.osgi.service.http.*;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.runtime.dto.ErrorPageDTO;

public class HttpServiceImpl implements HttpService, ExtendedHttpService {

	private final Bundle bundle; //The bundle associated with this instance of http service
	private final HttpServiceRuntimeImpl httpServiceRuntime;
	private final ConcurrentMap<ContextController, Map<Object, Registration<?, ?>>> registrations;

	private final ConcurrentMap<ServletContextHelper, ContextController> servletContextHelperRegistrations = new ConcurrentHashMap<ServletContextHelper, ContextController>();
	private final ConcurrentMap<HttpContext, ServletContextHelper> contextMap = new ConcurrentHashMap<HttpContext, ServletContextHelper>();
	private DefaultServletContextHelper defaultServletContextHelper;
	private final AtomicLong legacyServiceIdGenerator;
	private boolean shutdown = false; // We prevent use of this instance if HttpServiceFactory.ungetService has called unregisterAliases.

	public HttpServiceImpl(
		Bundle bundle, HttpServiceRuntimeImpl httpServiceRuntime,
		ConcurrentMap<ContextController, Map<Object, Registration<?, ?>>> registrations) {

		this.bundle = bundle;
		this.httpServiceRuntime = httpServiceRuntime;
		this.registrations = registrations;
		this.legacyServiceIdGenerator =
			httpServiceRuntime.getLegacyServiceIdGenerator();
		this.defaultServletContextHelper = new DefaultServletContextHelper(
			this.bundle);
	}

	/**
	 * @see HttpService#createDefaultHttpContext()
	 */
	public synchronized HttpContext createDefaultHttpContext() {
		checkShutdown();

		return defaultServletContextHelper;
	}

	/**
	 * @see HttpService#registerFilter(String, Filter, Dictionary, HttpContext)
	 */
	public synchronized void registerFilter(
			String alias, Filter filter, Dictionary<String, String> initparams,
			HttpContext httpContext)
		throws ServletException {

		checkShutdown();

		if (filter == null) {
			throw new IllegalArgumentException("Filter cannot be null");
		}

		ContextController contextController = getContextController(
			null, httpContext);

		Map<Object, Registration<?, ?>> contextRegistrations =
			getContextRegistrations(contextController);

		FilterRegistration filterRegistration =
			contextController.addFilterRegistration(
				alias, new ServiceHolder<Filter>(filter), initparams,
				legacyServiceIdGenerator.decrementAndGet());

		contextRegistrations.put(filter, filterRegistration);
	}

	/**
	 * @see ExtendedHttpService#registerFilter(Filter, String, String[], String[], String[], boolean, int, Map, String)
	 */
	public synchronized void registerFilter(
			Filter filter, String name, String[] patterns, String[] servletNames,
			String[] dispatcher, boolean asyncSupported, int filterPriority,
			Map<String, String> initparams, String contextSelector)
		throws ServletException {

		checkShutdown();

		ContextController contextController = getContextController(
			contextSelector, null);

		Map<Object, Registration<?, ?>> contextRegistrations =
			getContextRegistrations(contextController);

		FilterRegistration filterRegistration =
			contextController.addFilterRegistration(
				new ServiceHolder<Filter>(filter), asyncSupported, dispatcher, filterPriority, initparams,
				name, patterns, legacyServiceIdGenerator.decrementAndGet(),
				servletNames);

		contextRegistrations.put(filter, filterRegistration);
	}

	public synchronized void registerListener(
			EventListener eventListener, String contextSelector)
		throws ServletException {

		checkShutdown();

		ContextController contextController = getContextController(
			contextSelector, null);

		Map<Object, Registration<?, ?>> contextRegistrations =
			getContextRegistrations(contextController);

		ListenerRegistration listenerRegistration =
			contextController.addListenerRegistration(
				eventListener, legacyServiceIdGenerator.decrementAndGet());

		contextRegistrations.put(eventListener, listenerRegistration);
	}

	/**
	 * @see HttpService#registerResources(String, String, HttpContext)
	 */
	public synchronized void registerResources(
			String alias, String name, HttpContext httpContext)
		throws NamespaceException {

		checkShutdown();

		ContextController contextController = getContextController(
			null, httpContext);

		checkResourcePatterns(new String[] {alias}, contextController);

		Map<Object, Registration<?, ?>> contextRegistrations =
			getContextRegistrations(contextController);

		ResourceRegistration resourceRegistration =
			contextController.addResourceRegistration(
				alias, name, legacyServiceIdGenerator.decrementAndGet());

		contextRegistrations.put(alias, resourceRegistration);
	}

	/**
	 * @see ExtendedHttpService#registerResources(String[], String, String)
	 */
	public synchronized void registerResources(
			String[] patterns, String prefix, String contextSelector)
		throws NamespaceException {

		checkShutdown();

		ContextController contextController = getContextController(
			contextSelector, null);

		checkResourcePatterns(patterns, contextController);

		Map<Object, Registration<?, ?>> contextRegistrations =
			getContextRegistrations(contextController);

		ResourceRegistration resourceRegistration =
			contextController.addResourceRegistration(
				patterns, prefix, legacyServiceIdGenerator.decrementAndGet(),
				false);

		for (String pattern : patterns) {
			contextRegistrations.put(pattern, resourceRegistration);
		}
	}

	/**
	 * @see HttpService#registerServlet(String, Servlet, Dictionary, HttpContext)
	 */
	public synchronized void registerServlet(
			String alias, Servlet servlet, Dictionary initparams,
			HttpContext httpContext)
		throws ServletException, NamespaceException {

		checkShutdown();

		if (alias == null) {
			throw new IllegalArgumentException("Alias cannot be null");
		}

		ContextController contextController = getContextController(
			null, httpContext);

		checkServletPatterns(new String[] {alias}, null, contextController);

		if (httpServiceRuntime.getRegisteredServlets().contains(servlet)) {
			throw new ServletAlreadyRegisteredException(servlet);
		}

		Map<Object, Registration<?, ?>> contextRegistrations =
			getContextRegistrations(contextController);

		ServletRegistration servletRegistration =
			contextController.addServletRegistration(
				alias, new ServiceHolder<Servlet>(servlet), initparams,
				legacyServiceIdGenerator.decrementAndGet());

		contextRegistrations.put(alias, servletRegistration);
	}

	/**
	 * @see ExtendedHttpService#registerServlet(Servlet, String, String[], String[], boolean, Map, Servlet)
	 */
	public synchronized void registerServlet(
			Servlet servlet, String name, String[] patterns,
			String[] errorPages, boolean asyncSupported,
			Map<String, String> initparams, String contextSelector)
		throws ServletException, NamespaceException {

		checkShutdown();

		ContextController contextController = getContextController(
			contextSelector, null);

		checkServletPatterns(patterns, errorPages, contextController);

		if (httpServiceRuntime.getRegisteredServlets().contains(servlet)) {
			throw new ServletAlreadyRegisteredException(servlet);
		}

		Map<Object, Registration<?, ?>> contextRegistrations =
			getContextRegistrations(contextController);

		ServletRegistration servletRegistration =
			contextController.addServletRegistration(
				new ServiceHolder<Servlet>(servlet), asyncSupported, errorPages, initparams, patterns,
				legacyServiceIdGenerator.decrementAndGet(), name, false);

		if (patterns != null) {
			for (String pattern : patterns) {
				contextRegistrations.put(pattern, servletRegistration);
			}
		}

		if (errorPages != null) {
			for (String errorPage : errorPages) {
				contextRegistrations.put(errorPage, servletRegistration);
			}
		}
	}

	public synchronized void registerServletContextHelper(
			ServletContextHelper servletContextHelper, Bundle bundle,
			String[] contextNames, String contextPath,
			Map<String, String> initparams)
		throws ServletException {

		checkShutdown();

		if (servletContextHelper == null) {
			throw new NullServletContextHelperException();
		}

		if (servletContextHelperRegistrations.containsKey(servletContextHelper)) {
			throw new RegisteredServletContextHelperException();
		}

		Map<String, Object> properties = new HashMap<String, Object>();

		if (initparams != null) {
			properties.putAll(initparams);
		}

		ContextController contextController =
			httpServiceRuntime.addServletContextHelper(
				bundle, servletContextHelper, StringPlus.from(contextNames),
				contextPath, legacyServiceIdGenerator.decrementAndGet(),
				properties);

		servletContextHelperRegistrations.put(
			servletContextHelper, contextController);
	}

	/**
	 * @see HttpService#unregister(String)
	 */
	public synchronized void unregister(String alias) {
		checkShutdown();

		boolean foundEndpoint = false;

		for (HttpContext httpContext : contextMap.keySet()) {
			ContextController contextController = getContextController(
				null, httpContext);

			Map<Object, Registration<?, ?>> contextRegistrations =
				getContextRegistrations(contextController);

			EndpointRegistration<?> endpointRegistration =
				(EndpointRegistration<?>)contextRegistrations.remove(alias);

			if (endpointRegistration != null) {
				foundEndpoint = true;
				for (String pattern : endpointRegistration.getPatterns()) {
					contextRegistrations.remove(pattern);
				}

				endpointRegistration.destroy();
			}
		}

		if (!foundEndpoint) {
			throw new IllegalArgumentException("Alias not found: " + alias);
		}
	}

	@Override
	public synchronized void unregister(String pattern, String contextSelector) {
		checkShutdown();

		ContextController contextController = getContextController(
			contextSelector, null);

		Map<Object, Registration<?, ?>> contextRegistrations =
			getContextRegistrations(contextController);

		EndpointRegistration<?> endpointRegistration =
			(EndpointRegistration<?>)contextRegistrations.remove(pattern);

		if (endpointRegistration != null) {
			for (String curPattern : endpointRegistration.getPatterns()) {
				contextRegistrations.remove(curPattern);
			}

			endpointRegistration.destroy();
		}

		if (endpointRegistration == null) {
			throw new IllegalArgumentException("Pattern not found: " + pattern);
		}
	}

	/**
	 * @see ExtendedHttpService#unregisterFilter(Filter)
	 * @deprecated
	 */
	@Deprecated
	public synchronized void unregisterFilter(Filter filter) {
		checkShutdown();

		FilterRegistration filterRegistration = null;

		for (HttpContext httpContext : contextMap.keySet()) {
			ContextController contextController = getContextController(
				null, httpContext);

			Map<Object, Registration<?, ?>> contextRegistrations =
				getContextRegistrations(contextController);

			filterRegistration =
				(FilterRegistration)contextRegistrations.remove(filter);

			if (filterRegistration != null) {
				filterRegistration.destroy();
			}
		}

		if (filterRegistration == null) {
			throw new IllegalArgumentException("Filter not found: " + filter);
		}
	}

	@Override
	public synchronized void unregisterFilter(
		Filter filter, String contextSelector) {

		ContextController contextController = getContextController(
			contextSelector, null);

		Map<Object, Registration<?, ?>> contextRegistrations =
			getContextRegistrations(contextController);

		FilterRegistration filterRegistration =
			(FilterRegistration)contextRegistrations.remove(filter);

		if (filterRegistration != null) {
			filterRegistration.destroy();
		}
		else {
			throw new IllegalArgumentException("Filter not found: " + filter);
		}
	}

	public synchronized void unregisterListener(
		EventListener eventListener, String contextSelector) {

		checkShutdown();

		ContextController contextController = getContextController(
			contextSelector, null);

		Map<Object, Registration<?, ?>> contextRegistrations =
			getContextRegistrations(contextController);

		ListenerRegistration listenerRegistration =
			(ListenerRegistration)contextRegistrations.remove(eventListener);

		if (listenerRegistration != null) {
			listenerRegistration.destroy();
		}
		else {
			throw new IllegalArgumentException(
				"EventListener not found: " + eventListener);
		}
	}

	public synchronized void unregisterServlet(
		Servlet servlet, String contextSelector) {

		ContextController contextController = getContextController(
			contextSelector, null);

		Map<Object, Registration<?, ?>> contextRegistrations =
			getContextRegistrations(contextController);

		ServletRegistration servletRegistration = null;

		for (Registration<?, ?> curRegistration : contextRegistrations.values()) {
			if (curRegistration.getT().equals(servlet)) {
				servletRegistration = (ServletRegistration)curRegistration;

				break;
			}
		}

		if (servletRegistration != null) {
			String[] patterns = servletRegistration.getPatterns();

			if (patterns != null) {
				for (String pattern : servletRegistration.getPatterns()) {
					contextRegistrations.remove(pattern);
				}
			}

			ErrorPageDTO errorPageDTO = servletRegistration.getErrorPageDTO();

			if (errorPageDTO != null) {
				for (long errorCode : errorPageDTO.errorCodes) {
					contextRegistrations.remove(String.valueOf(errorCode));
				}

				for (String exception : errorPageDTO.exceptions) {
					contextRegistrations.remove(exception);
				}
			}

			servletRegistration.destroy();
		}
		else {
			throw new IllegalArgumentException("Servlet not found: " + servlet);
		}
	}

	public synchronized void unregisterServletContextHelper(
		ServletContextHelper servletContextHelper) {

		checkShutdown();

		ContextController contextController = servletContextHelperRegistrations.remove(
			servletContextHelper);

		if (contextController != null) {
			httpServiceRuntime.removeContextController(contextController);
		}
		else {
			throw new IllegalArgumentException(
				"ServletContextHelper not found: " + servletContextHelper);
		}
	}

	//Clean up method
	synchronized void shutdown() {
		for (ContextController contextController :
				servletContextHelperRegistrations.values()) {

			contextController.destroy();
		}

		servletContextHelperRegistrations.clear();

		shutdown = true;
	}

	private void checkResourcePatterns(
			String[] patterns, ContextController contextController)
		throws NamespaceException {

		if ((patterns == null) || (patterns.length == 0)) {
			throw new IllegalArgumentException("Patterns must contain a value.");
		}

		for (EndpointRegistration<?> endpointRegistration :
				contextController.getEndpointRegistrations()) {

			String[] registeredPatterns = endpointRegistration.getPatterns();

			if (registeredPatterns == null) {
				continue;
			}

			for (String pattern : patterns) {
				int pos = Arrays.binarySearch(registeredPatterns, pattern);

				if (pos > -1) {
					throw new PatternInUseException(pattern);
				}
			}
		}
	}

	private void checkServletPatterns(
			String[] patterns, String[] errorPages,
			ContextController contextController)
		throws NamespaceException {

		if (((patterns == null) || (patterns.length == 0)) &&
			((errorPages == null) || errorPages.length == 0)) {

			throw new IllegalArgumentException(
				"Patterns or servletNames must contain a value.");
		}

		for (EndpointRegistration<?> endpointRegistration :
				contextController.getEndpointRegistrations()) {

			String[] registeredPatterns = endpointRegistration.getPatterns();

			if ((patterns != null) && (registeredPatterns != null)) {
				for (String pattern : patterns) {
					int pos = Arrays.binarySearch(registeredPatterns, pattern);

					if (pos > -1) {
						throw new PatternInUseException(pattern);
					}
				}
			}

			if (errorPages != null) {
				if (!(endpointRegistration instanceof ServletRegistration)) {
					continue;
				}

				ServletRegistration servletRegistration = (ServletRegistration)endpointRegistration;

				ErrorPageDTO errorPageDTO = servletRegistration.getErrorPageDTO();

				if (errorPageDTO == null) {
					continue;
				}

				String[] exceptions = errorPageDTO.exceptions;

				for (String errorPage : errorPages) {
					int pos = Arrays.binarySearch(exceptions, errorPage);

					if (pos > -1) {
						throw new NamespaceException(
							"Error page already registered: " + errorPage);
					}
				}

				long[] errorCodes = errorPageDTO.errorCodes;

				for (String errorPage : errorPages) {
					try {
						long longValue = Long.parseLong(errorPage);

						int pos = Arrays.binarySearch(errorCodes, longValue);

						if (pos > -1) {
							throw new NamespaceException(
								"Error page already registered: " + errorPage);
						}
					}
					catch (NumberFormatException nfe) {
					}
				}
			}
		}
	}

	private void checkShutdown() {
		if (shutdown) {
			throw new IllegalStateException(
				"Service instance is already shutdown"); //$NON-NLS-1$
		}
	}

	private synchronized ContextController getContextController(
		String contextSelector, HttpContext httpContext) {

		String calculatedContextSelector = calculateContextSelector(
			contextSelector, httpContext);

		org.osgi.framework.Filter targetFilter =
			httpServiceRuntime.getContextSelectorFilter(
				bundle, calculatedContextSelector);

		ContextController contextController = httpServiceRuntime.getContextController(
			targetFilter);

		if (contextController != null) {
			return contextController;
		}

		if (contextSelector != null) {
			throw new IllegalArgumentException(
				"No valid ServletContextHelper for filter '" +
					targetFilter.toString() + '\'');
		}

		ServletContextHelper servletContextHelper = defaultServletContextHelper;

		if (httpContext != null) {
			servletContextHelper = contextMap.get(httpContext);
		}

		contextController = httpServiceRuntime.addServletContextHelper(
			bundle, servletContextHelper,
			Collections.singletonList(calculatedContextSelector),
			null, legacyServiceIdGenerator.decrementAndGet(),
			httpServiceRuntime.getAttributes());

		servletContextHelperRegistrations.put(
			servletContextHelper, contextController);

		return contextController;
	}

	private Map<Object, Registration<?, ?>> getContextRegistrations(
		ContextController contextController) {

		Map<Object, Registration<?, ?>> map = registrations.get(
			contextController);

		if (map == null) {
			map = new ConcurrentHashMap<Object, Registration<?,?>>();

			Map<Object, Registration<?, ?>> existing = registrations.putIfAbsent(
				contextController, map);

			if (existing != null) {
				map = existing;
			}
		}

		return map;
	}

	private String calculateContextSelector(
		String contextSelector, HttpContext httpContext) {

		if (contextSelector != null) {
			return contextSelector;
		}

		ServletContextHelper servletContextHelper = null;

		if (httpContext == null) {
			httpContext = defaultServletContextHelper;
		}

		servletContextHelper = contextMap.get(httpContext);

		if (servletContextHelper == null) {
			if (httpContext instanceof ServletContextHelper) {
				servletContextHelper = (ServletContextHelper)httpContext;
			}
			else {
				servletContextHelper = new ServletContextHelperWrapper(
					httpContext, bundle);
			}

			contextMap.putIfAbsent(httpContext, servletContextHelper);
		}

		return String.valueOf(bundle.getBundleId()) + '#' + httpContext.hashCode();
	}

}
