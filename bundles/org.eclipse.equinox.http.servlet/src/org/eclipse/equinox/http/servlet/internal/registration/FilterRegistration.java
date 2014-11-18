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
package org.eclipse.equinox.http.servlet.internal.registration;

import java.io.IOException;
import java.util.Arrays;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.equinox.http.servlet.internal.context.ContextController;
import org.eclipse.equinox.http.servlet.internal.context.ContextController.ServiceHolder;
import org.eclipse.equinox.http.servlet.internal.servlet.FilterChainImpl;
import org.eclipse.equinox.http.servlet.internal.servlet.Match;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.runtime.dto.FilterDTO;

//This class wraps the filter object registered in the HttpService.registerFilter call, to manage the context classloader when handleRequests are being asked.
public class FilterRegistration
	extends MatchableRegistration<Filter, FilterDTO>
	implements Comparable<FilterRegistration> {

	private final ServiceHolder<Filter> filterHolder;
	private final ServletContextHelper servletContextHelper; //The context used during the registration of the filter
	private final ClassLoader classLoader;
	private final int priority;
	private final ContextController contextController;

	public FilterRegistration(
		ServiceHolder<Filter> filterHolder, FilterDTO filterDTO, int priority,
		ServletContextHelper servletContextHelper,
		ContextController contextController) {

		super(filterHolder.get(), filterDTO);
		this.filterHolder = filterHolder;
		this.priority = priority;
		this.servletContextHelper = servletContextHelper;
		this.contextController = contextController;
		classLoader = contextController.getClassLoader();
	}

	public int compareTo(FilterRegistration otherFilterRegistration) {
		int priorityDifference = priority - otherFilterRegistration.priority;
		if (priorityDifference != 0)
			return -priorityDifference;

		return (Math.abs(getD().serviceId) >
			Math.abs(otherFilterRegistration.getD().serviceId)) ? 1 : -1;
	}

	public void destroy() {
		ClassLoader original = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(classLoader);

			contextController.getFilterRegistrations().remove(this);
			super.destroy();
			getT().destroy();
		}
		finally {
			destroyContextAttributes();
			Thread.currentThread().setContextClassLoader(original);
			filterHolder.release();
		}
	}

	public boolean appliesTo(FilterChainImpl filterChainImpl) {
		return (Arrays.binarySearch(
			getD().dispatcher, filterChainImpl.getDispatcherType().name()) >= 0);
	}

	//Delegate the handling of the request to the actual filter
	public void doFilter(
			HttpServletRequest request, HttpServletResponse response,
			FilterChain chain)
		throws IOException, ServletException {

		ClassLoader original = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(classLoader);

			if (servletContextHelper.handleSecurity(request, response)) {
				getT().doFilter(request, response, chain);
			}
		}
		finally {
			Thread.currentThread().setContextClassLoader(original);
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof FilterRegistration)) {
			return false;
		}

		FilterRegistration filterRegistration = (FilterRegistration)obj;

		return getT().equals(filterRegistration.getT());
	}

	@Override
	public int hashCode() {
		return Long.valueOf(getD().serviceId).hashCode();
	}

	public ServletContextHelper getServletContextHelper() {
		return servletContextHelper;
	}

	//Delegate the init call to the actual filter
	public void init(FilterConfig filterConfig) throws ServletException {
		boolean initialized = false;
		ClassLoader original = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(classLoader);

			createContextAttributes();
			getT().init(filterConfig);
			initialized = true;
		}
		finally {
			if (!initialized) {
				destroyContextAttributes();
			}
			Thread.currentThread().setContextClassLoader(original);
		}
	}

	@Override
	public String match(
		String name, String servletPath, String pathInfo, String extension,
		Match match) {

		if (name != null) {
			if (getD().servletNames == null) {
				return null;
			}

			for (String servletName : getD().servletNames) {
				if (servletName.equals(name)) {
					return name;
				}
			}

			return null;
		}

		for (String pattern : getD().patterns) {
			if (doMatch(pattern, servletPath, pathInfo, extension, match)) {
				return pattern;
			}
		}

		return null;
	}

	private void createContextAttributes() {
		contextController.getProxyContext().createContextAttributes(
			servletContextHelper);
	}

	private void destroyContextAttributes() {
		contextController.getProxyContext().destroyContextAttributes(
			servletContextHelper);
	}

}
