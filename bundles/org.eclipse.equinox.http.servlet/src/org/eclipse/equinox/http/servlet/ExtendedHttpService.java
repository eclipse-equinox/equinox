/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Raymond Augé <raymond.auge@liferay.com> - Bug 436698
 *******************************************************************************/
package org.eclipse.equinox.http.servlet;

import java.util.*;
import javax.servlet.*;
import org.osgi.annotation.versioning.ProviderType;
import org.osgi.framework.Bundle;
import org.osgi.service.http.*;
import org.osgi.service.http.context.ServletContextHelper;

/**
 * @since 1.1
 */
@ProviderType
public interface ExtendedHttpService extends HttpService {

	/**
	 * @param alias name in the URI namespace at which the filter is registered
	 * @param filter the filter object to register
	 * @param initparams initialization arguments for the filter or
	 *        <code>null</code> if there are none. This argument is used by the
	 *        filter's <code>FilterConfig</code> object.
	 * @param context the <code>HttpContext</code> object for the registered
	 *        filter, or <code>null</code> if a default <code>HttpContext</code> is
	 *        to be created and used.
	 * @throws javax.servlet.ServletException if the filter's <code>init</code>
	 *            method throws an exception, or the given filter object has
	 *            already been registered at a different alias.
	 * @throws java.lang.IllegalArgumentException if any of the arguments are
	 *            invalid
	 */
	public void registerFilter(String alias, Filter filter, Dictionary<String, String> initparams, HttpContext context) throws ServletException, NamespaceException;

	/**
	 * @param filter the filter object to register
	 * @param name the name to use for the filter registration (required)
	 * @param patterns an array of URI patterns to which the filter will apply (optional) one of this or servletNames are required
	 * @param servletNames an array of servlet names to which a filter will apply (optional)
	 * @param dispatcher the array of dispatcher names to which the filter applies (default is REQUEST when empty or null)
	 * @param asyncSupported whether the filter applies to async servlets
	 * @param filterPriority the priority determines the order filters will take when other criteria still result in conflict
	 * @param initparams initialization arguments for the filter or
	 *        <code>null</code> if there are none. This argument is used by the
	 *        filter's <code>FilterConfig</code> object.
	 * @param contextSelector the name of a context under which to register the filter (if the strings starts with '(' it will be parsed as a OSGi filter string)
	 * @throws javax.servlet.ServletException if the filter's <code>init</code>
	 *         method throws an exception, or the given filter object has
	 *         already been registered.
	 * @throws java.lang.IllegalArgumentException if any of the arguments are
	 *         invalid
	 *
	 * @since 1.2
	 */
	public void registerFilter(
			Filter filter, String name, String[] patterns,
			String[] servletNames, String[] dispatcher,
			boolean asyncSupported, int filterPriority,
			Map<String, String> initparams, String contextSelector)
		throws ServletException;

	/**
	 * Register a listener implementing any of the following types:
	 * <ul>
	 * <li>javax.servlet.ServletContextListener</li>
	 * <li>javax.servlet.ServletContextAttributeListener</li>
	 * <li>javax.servlet.ServletRequestListener</li>
	 * <li>javax.servlet.ServletRequestAttributeListener</li>
	 * <li>javax.servlet.http.HttpSessionListener</li>
	 * <li>javax.servlet.http.HttpSessionAttributeListener</li>
	 * </ul>
	 *
	 * @param eventListener the listener object to register
	 * @param contextSelector the name of a context under which to register the filter (if the strings starts with '(' it will be parsed as a OSGi filter string)
	 * @throws ServletException if the listener is already registered
	 * @throws java.lang.IllegalArgumentException if any of the arguments are
	 *         invalid
	 *
	 * @since 1.2
	 */
	public void registerListener(
			EventListener eventListener, String contextSelector)
		throws ServletException;

	/**
	 * @param patterns an array of URI patterns to which the filter will apply (optional) one of this or servletNames are required
	 * @param prefix the base name of the resources that will be registered
	 * @param contextSelector the name of a context under which to register the filter (if the strings starts with '(' it will be parsed as a OSGi filter string)
	 * @throws NamespaceException if the registration fails because the alias is
	 *         already in use.
	 * @throws java.lang.IllegalArgumentException if any of the parameters are
	 *         invalid
	 *
	 * @since 1.2
	 */
	public void registerResources(
			String[] patterns, String prefix, String contextSelector)
		throws NamespaceException;

	/**
	 * @param servlet the servlet object to register
	 * @param name the name to use for the filter registration (required)
	 * @param patterns an array of URI patterns to which the filter will apply (optional) one of this or servletNames are required
	 * @param errorPages an array of exception names and/or HTTP error codes for which the servlet will server error messages
	 * @param asyncSupported whether the servlet supports async request handling
	 * @param initparams initialization arguments for the servlet or
	 *        <code>null</code> if there are none. This argument is used by the
	 *        servlets's <code>ServletConfig</code> object.
	 * @param contextSelector the name of a context under which to register the servlet (if the strings starts with '(' it will be parsed as a OSGi filter string)
	 * @throws javax.servlet.ServletException if the servlet's <code>init</code>
	 *         method throws an exception, or the given servlet object has
	 *         already been registered.
	 * @throws java.lang.IllegalArgumentException if any of the arguments are
	 *         invalid
	 *
	 * @since 1.2
	 */
	public void registerServlet(
			Servlet servlet, String name, String[] patterns,
			String[] errorPages, boolean asyncSupported,
			Map<String, String> initparams, String contextSelector)
		throws ServletException, NamespaceException;

	/**
	 * @param servletContextHelper the servletContextHelper object to register
	 * @param bundle the bundle to which the servletContextHelper is attached
	 * @param contextNames the contextNames to use for the servletContextHelper
	 *        registration, the first one in the list is considered the official
	 *        name and the others are aliases (at least 1 required)
	 * @param contextPath optional property for defining an additional context
	 *        path for the context.
	 * @param initparams initialization arguments for the servlet context helper
	 *        or <code>null</code> if there are none. These values are
	 *        accessible via the ServletContext.
	 * @throws ServletException if the servletContextHelper is already registered
	 * @throws java.lang.IllegalArgumentException if any of the arguments are
	 *         invalid
	 *
	 * @since 1.2
	 */
	public void registerServletContextHelper(
			ServletContextHelper servletContextHelper, Bundle bundle,
			String[] contextNames, String contextPath,
			Map<String, String> initparams)
		throws ServletException;

	/**
	 * Unregisters a previous registration done by {@code registerServlet} or
	 * {@code registerResources} methods with reference to a
	 * ServletContextHelper.
	 *
	 * <p>
	 * After this call, the registered pattern in the URI name-space of the
	 * ServletContextHelper will no longer be available. If the registration was
	 * for a servlet, the Http Service must call the {@code destroy} method of
	 * the servlet before returning.
	 * <p>
	 * If the bundle which performed the registration is stopped or otherwise
	 * "unget"s the Http Service without calling {@link #unregister(String, String)}
	 * then Http Service must automatically unregister the registration.
	 * However, if the registration was for a servlet, the {@code destroy}
	 * method of the servlet will not be called in this case since the bundle
	 * may be stopped. {@link #unregister(String, String)} must be explicitly
	 * called to cause the {@code destroy} method of the servlet to be called.
	 * This can be done in the {@code BundleActivator.stop} method of the bundle
	 * registering the servlet.
	 *
	 * @param pattern name in the URI name-space of the registration to unregister
	 * @param contextSelector the name of a context under which the filter was registered (if the strings starts with '(' it will be parsed as a OSGi filter string)
	 * @throws java.lang.IllegalArgumentException if there is no registration
	 *         for the pattern or the calling bundle was not the bundle which
	 *         registered the alias.
	 *
	 * @since 1.2
	 */
	public void unregister(String pattern, String contextSelector);

	/**
	 * Unregisters a previous filter registration done by the
	 * <code>registerFilter</code> methods.
	 *
	 * <p>
	 * After this call, the registered filter will no
	 * longer be available. The Http Service must call the <code>destroy</code>
	 * method of the filter before returning.
	 * <p>
	 * If the bundle which performed the registration is stopped or otherwise
	 * "unget"s the Http Service without calling {@link #unregisterFilter} then the Http
	 * Service must automatically unregister the filter registration. However, the
	 * <code>destroy</code> method of the filter will not be called in this case since
	 * the bundle may be stopped.
	 * {@link #unregisterFilter} must be explicitly called to cause the
	 * <code>destroy</code> method of the filter to be called. This can be done
	 * in the <code>BundleActivator.stop</code> method of the
	 * bundle registering the filter.
	 *
	 * @param filter the filter object to unregister
	 * @throws java.lang.IllegalArgumentException if there is no registration
	 *            for the filter or the calling bundle was not the bundle which
	 *            registered the filter.
	 * @deprecated
	 */
	@Deprecated
	public void unregisterFilter(Filter filter);

	/**
	 * Unregisters a previous filter registration done by the
	 * <code>registerFilter</code> methods.
	 *
	 * <p>
	 * After this call, the registered filter will no
	 * longer be available. The Http Service must call the <code>destroy</code>
	 * method of the filter before returning.
	 * <p>
	 * If the bundle which performed the registration is stopped or otherwise
	 * "unget"s the Http Service without calling {@link #unregisterFilter} then the Http
	 * Service must automatically unregister the filter registration. However, the
	 * <code>destroy</code> method of the filter will not be called in this case since
	 * the bundle may be stopped.
	 * {@link #unregisterFilter} must be explicitly called to cause the
	 * <code>destroy</code> method of the filter to be called. This can be done
	 * in the <code>BundleActivator.stop</code> method of the
	 * bundle registering the filter.
	 *
	 * @param filter the filter object to unregister
	 * @param contextSelector the name of a context under which the filter was registered (if the strings starts with '(' it will be parsed as a OSGi filter string)
	 * @throws java.lang.IllegalArgumentException if there is no registration
	 *            for the filter or the calling bundle was not the bundle which
	 *            registered the filter.
	 * @since 1.2
	 */
	public void unregisterFilter(Filter filter, String contextSelector);

	/**
	 * Unregisters a previous listener registration done by the
	 * <code>registerListener</code> methods.
	 *
	 * <p>
	 * After this call, the registered listener will no
	 * longer be available.
	 * <p>
	 *
	 * @param eventListener the listener object to unregister
	 * @param contextSelector the name of a context under which the listener was registered (if the strings starts with '(' it will be parsed as a OSGi filter string)
	 * @throws java.lang.IllegalArgumentException if there is no registration
	 *            for the listener or the calling bundle was not the bundle which
	 *            registered the listener.
	 *
	 * @since 1.2
	 */
	public void unregisterListener(
		EventListener eventListener, String contextSelector);

	/**
	 * Unregisters a previous servlet registration done by the
	 * <code>registerServlet</code> methods.
	 *
	 * <p>
	 * After this call, the registered servlet will no
	 * longer be available.
	 * <p>
	 *
	 * @param servlet the servlet object to unregister
	 * @param contextSelector the name of a context under which the servlet was registered (if the strings starts with '(' it will be parsed as a OSGi filter string)
	 * @throws java.lang.IllegalArgumentException if there is no registration
	 *            for the servlet or the calling bundle was not the bundle which
	 *            registered the servlet.
	 *
	 * @since 1.2
	 */
	public void unregisterServlet(Servlet servlet, String contextSelector);

	/**
	 * Unregisters a previous servletContextHelper registration done by the
	 * <code>registerServletContextHelper</code> methods.
	 *
	 * <p>
	 * After this call, the registered servletContextHelper will no
	 * longer be available.
	 * <p>
	 * If the bundle which performed the registration is stopped or otherwise
	 * "unget"s the Http Service without calling {@link #unregisterServlet} then
	 * the Http Service must automatically unregister the servletContextHelper
	 * registration. {@link #unregisterServletContextHelper} must be explicitly
	 * called for the servletContextHelper to unregister and destroy all
	 * associated resources. This can be done in the <code>BundleActivator.stop</code>
	 * method of the bundle registering the servletContextHelper.
	 *
	 * @param servletContextHelper the servletContextHelper object to unregister
	 * @throws java.lang.IllegalArgumentException if there is no registration
	 *         for the servletContextHelper or the calling bundle was not the
	 *         bundle which registered the servletContextHelper.
	 *
	 * @since 1.2
	 */
	public void unregisterServletContextHelper(
		ServletContextHelper servletContextHelper);

}
