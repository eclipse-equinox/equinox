/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
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
import org.eclipse.equinox.http.servlet.internal.util.Const;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.http.runtime.dto.FilterDTO;

//This class wraps the filter object registered in the HttpService.registerFilter call, to manage the context classloader when handleRequests are being asked.
public class FilterRegistration
	extends MatchableRegistration<Filter, FilterDTO>
	implements Comparable<FilterRegistration> {

	private final ServiceHolder<Filter> filterHolder;
	private final ClassLoader classLoader;
	private final int priority;
	private final ContextController contextController;
	private final boolean initDestoyWithContextController;

	public FilterRegistration(
		ServiceHolder<Filter> filterHolder, FilterDTO filterDTO, int priority,
		ContextController contextController, boolean legacyRegistration) {

		super(filterHolder.get(), filterDTO);
		this.filterHolder = filterHolder;
		this.priority = priority;
		this.contextController = contextController;
		if (legacyRegistration) {
			// legacy filter registrations used the current TCCL at registration time
			classLoader = Thread.currentThread().getContextClassLoader();
		} else {
			classLoader = filterHolder.getBundle().adapt(BundleWiring.class).getClassLoader();
		}
		String legacyContextFilter = (String) filterHolder.getServiceReference().getProperty(Const.EQUINOX_LEGACY_CONTEXT_SELECT);
		if (legacyContextFilter != null) {
			// This is a legacy Filter registration.  
			// This filter tells us the real context controller,
			// backed by an HttpContext that should be used to init/destroy this Filter
			org.osgi.framework.Filter f = null;
			try {
				 f = FrameworkUtil.createFilter(legacyContextFilter);
			}
			catch (InvalidSyntaxException e) {
				// nothing
			}
			initDestoyWithContextController = f == null || contextController.matches(f);
		} else {
			initDestoyWithContextController = true;
		}
	}

	public int compareTo(FilterRegistration otherFilterRegistration) {
		int priorityDifference = priority - otherFilterRegistration.priority;
		if (priorityDifference != 0)
			return -priorityDifference;

		return (Math.abs(getD().serviceId) >
			Math.abs(otherFilterRegistration.getD().serviceId)) ? 1 : -1;
	}

	public void destroy() {
		if (!initDestoyWithContextController) {
			return;
		}
		ClassLoader original = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(classLoader);
			contextController.getHttpServiceRuntime().getRegisteredObjects().remove(this.getT());
			contextController.getFilterRegistrations().remove(this);
			contextController.ungetServletContextHelper(filterHolder.getBundle());
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
			getT().doFilter(request, response, chain);
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

	//Delegate the init call to the actual filter
	public void init(FilterConfig filterConfig) throws ServletException {
		if (!initDestoyWithContextController) {
			return;
		}
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
			contextController);
	}

	private void destroyContextAttributes() {
		contextController.getProxyContext().destroyContextAttributes(
			contextController);
	}

}
