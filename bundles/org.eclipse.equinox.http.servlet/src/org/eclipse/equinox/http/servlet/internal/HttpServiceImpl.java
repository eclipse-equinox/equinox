/*******************************************************************************
 * Copyright (c) 2005, 2019 Cognos Incorporated, IBM Corporation and others.
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

import java.security.*;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import org.eclipse.equinox.http.servlet.ExtendedHttpService;
import org.eclipse.equinox.http.servlet.internal.context.WrappedHttpContext;
import org.eclipse.equinox.http.servlet.internal.util.Const;
import org.eclipse.equinox.http.servlet.internal.util.Throw;
import org.osgi.framework.*;
import org.osgi.service.http.*;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

public class HttpServiceImpl implements HttpService, ExtendedHttpService {

	private static AtomicLong legacyIdGenerator = new AtomicLong(0);

	final Bundle bundle; //The bundle associated with this instance of http service
	final HttpServiceRuntimeImpl httpServiceRuntime;
	private volatile boolean shutdown = false; // We prevent use of this instance if HttpServiceFactory.ungetService has called unregisterAliases.

	private final ConcurrentMap<HttpContext, ServiceReference<DefaultServletContextHelper>> contextMap = new ConcurrentHashMap<HttpContext, ServiceReference<DefaultServletContextHelper>>();
	private final ServiceReference<DefaultServletContextHelper> defaultHttpContextReference;

	public HttpServiceImpl(
		Bundle bundle, HttpServiceRuntimeImpl httpServiceRuntime) {

		this.bundle = bundle;
		this.httpServiceRuntime = httpServiceRuntime;
		defaultHttpContextReference = this.bundle.getBundleContext().getServiceReference(DefaultServletContextHelper.class);
	}

	/**
	 * @see HttpService#createDefaultHttpContext()
	 */
	public synchronized HttpContext createDefaultHttpContext() {
		checkShutdown();

		DefaultServletContextHelper defaultServletContextHelper = bundle.getBundleContext().getService(defaultHttpContextReference);

		contextMap.putIfAbsent(defaultServletContextHelper, defaultHttpContextReference);

		return defaultServletContextHelper;
	}

	/**
	 * @throws ServletException
	 * @see ExtendedHttpService#registerFilter(String, Filter, Dictionary, HttpContext)
	 */
	public synchronized void registerFilter(
			final String alias, final Filter filter,
			final Dictionary<String, String> initparams,
			HttpContext httpContext)
		throws ServletException {

		checkShutdown();
		httpContext = httpContext == null ? createDefaultHttpContext() : registerContext(httpContext);
		final ServiceReference<DefaultServletContextHelper> serviceReference = contextMap.get(httpContext);
		try {
			AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
				@Override
				public Void run() throws ServletException {
					httpServiceRuntime.registerHttpServiceFilter(bundle, alias, filter, initparams, serviceReference);
					return null;
				}
			});
		}
		catch (PrivilegedActionException e) {
			Throw.unchecked(e.getException());
		}

	}

	/**
	 * @throws NamespaceException
	 * @see HttpService#registerResources(String, String, HttpContext)
	 */
	public synchronized void registerResources(
			final String alias, final String name, HttpContext httpContext)
		throws NamespaceException {

		checkShutdown();
		httpContext = httpContext == null ? createDefaultHttpContext() : registerContext(httpContext);
		final ServiceReference<DefaultServletContextHelper> serviceReference = contextMap.get(httpContext);
		try {
			AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
				@Override
				public Void run() throws NamespaceException {
					httpServiceRuntime.registerHttpServiceResources(bundle, alias, name, serviceReference);
					return null;
				}
			});
		} catch (PrivilegedActionException e) {
			Throw.unchecked(e.getException());
		}

	}

	/**
	 * @throws ServletException
	 * @throws NamespaceException
	 * @see HttpService#registerServlet(String, Servlet, Dictionary, HttpContext)
	 */
	public synchronized void registerServlet(
			final String alias, final Servlet servlet,
			final Dictionary initparams, HttpContext httpContext)
		throws ServletException, NamespaceException {

		checkShutdown();
		httpContext = httpContext == null ? createDefaultHttpContext() : registerContext(httpContext);
		final ServiceReference<DefaultServletContextHelper> serviceReference = contextMap.get(httpContext);
		try {
			AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
				@Override
				public Void run() throws NamespaceException, ServletException {
					httpServiceRuntime.registerHttpServiceServlet(bundle, alias, servlet, initparams, serviceReference);
					return null;
				}
			});
		} catch (PrivilegedActionException e) {
			Throw.unchecked(e.getException());
		}
	}

	/**
	 * @see HttpService#unregister(String)
	 */
	public synchronized void unregister(String alias) {
		checkShutdown();

		httpServiceRuntime.unregisterHttpServiceAlias(bundle, alias);
	}

	/**
	 * @see ExtendedHttpService#unregisterFilter(Filter)
	 */
	public synchronized void unregisterFilter(Filter filter) {
		checkShutdown();

		httpServiceRuntime.unregisterHttpServiceFilter(bundle, filter);
	}

	//Clean up method
	synchronized void shutdown() {
		httpServiceRuntime.unregisterHttpServiceObjects(bundle);

		shutdown = true;
	}

	private void checkShutdown() {
		if (shutdown) {
			throw new IllegalStateException(
				"Service instance is already shutdown"); //$NON-NLS-1$
		}
	}

	private long generateLegacyId() {
		return legacyIdGenerator.getAndIncrement();
	}

	private HttpContext registerContext(HttpContext httpContext) {
		ServiceReference<? extends HttpContext> serviceReference = contextMap.get(httpContext);

		if (serviceReference == null) {
			Dictionary<String, Object> props = new Hashtable<String, Object>();
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, httpContext.getClass().getName().replaceAll("[^a-zA-Z_0-9\\-]", "_") + "-" + generateLegacyId()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/"); //$NON-NLS-1$
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET, httpServiceRuntime.getTargetFilter());
			props.put(Const.EQUINOX_LEGACY_CONTEXT_HELPER, Boolean.TRUE);
			props.put(Const.EQUINOX_LEGACY_HTTP_CONTEXT_INITIATING_ID, bundle.getBundleId());

			@SuppressWarnings("unchecked")
			ServiceRegistration<DefaultServletContextHelper> registration = (ServiceRegistration<DefaultServletContextHelper>)bundle.getBundleContext().registerService(ServletContextHelper.class.getName(), new WrappedHttpContext(httpContext, bundle), props);

			contextMap.put(httpContext, registration.getReference());
		}

		return httpContext;
	}

}
