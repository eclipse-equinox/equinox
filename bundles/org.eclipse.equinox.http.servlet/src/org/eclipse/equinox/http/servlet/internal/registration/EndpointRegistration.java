/*******************************************************************************
 * Copyright (c) 2014, 2019 Raymond Augé and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Raymond Augé <raymond.auge@liferay.com> - Bug 436698
 ******************************************************************************/

package org.eclipse.equinox.http.servlet.internal.registration;

import java.io.IOException;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.equinox.http.servlet.internal.context.ContextController;
import org.eclipse.equinox.http.servlet.internal.context.ServiceHolder;
import org.eclipse.equinox.http.servlet.internal.servlet.Match;
import org.osgi.dto.DTO;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.http.context.ServletContextHelper;

/**
 * @author Raymond Augé
 */
public abstract class EndpointRegistration<D extends DTO>
	extends MatchableRegistration<Servlet, D> implements Comparable<EndpointRegistration<?>>{

	protected final ServiceHolder<Servlet> servletHolder;
	private final ServletContextHelper servletContextHelper; //The context used during the registration of the servlet
	private final ContextController contextController;
	private final ClassLoader classLoader;

	public EndpointRegistration(
		ServiceHolder<Servlet> servletHolder, D d, ServletContextHelper servletContextHelper,
		ContextController contextController) {

		super(servletHolder.get(), d);
		this.servletHolder = servletHolder;
		this.servletContextHelper = servletContextHelper;
		this.contextController = contextController;
		if (servletHolder.getLegacyTCCL() != null) {
			// legacy registrations used the current TCCL at registration time
			classLoader = servletHolder.getLegacyTCCL();
		} else {
			classLoader = servletHolder.getBundle().adapt(BundleWiring.class).getClassLoader();
		}
		createContextAttributes();
	}

	@Override
	public void destroy() {
		ClassLoader original = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(classLoader);

			contextController.getEndpointRegistrations().remove(this);
			contextController.getHttpServiceRuntime().getRegisteredObjects().remove(this.getT());
			contextController.ungetServletContextHelper(servletHolder.getBundle());

			super.destroy();
			getT().destroy();
		}
		finally {
			destroyContextAttributes();
			Thread.currentThread().setContextClassLoader(original);
			servletHolder.release();
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof EndpointRegistration)) {
			return false;
		}

		EndpointRegistration<?> endpointRegistration = (EndpointRegistration<?>)obj;

		return getD().equals(endpointRegistration.getD());
	}

	@Override
	public int hashCode() {
		return getD().hashCode();
	}

	//Delegate the init call to the actual servlet
	public void init(ServletConfig servletConfig) throws ServletException {
		ClassLoader original = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(classLoader);

			getT().init(servletConfig);
		}
		finally {
			Thread.currentThread().setContextClassLoader(original);
		}
	}

	public abstract String getName();

	public abstract String[] getPatterns();

	public abstract long getServiceId();

	public ServletContext getServletContext() {
		return getT().getServletConfig().getServletContext();
	}

	public ServletContextHelper getServletContextHelper() {
		return servletContextHelper;
	}

	public abstract ServiceReference<?> getServiceReference();

	@Override
	public String match(
		String name, String servletPath, String pathInfo, String extension,
		Match match) {

		if (match == Match.ERROR) {
			return null;
		}

		if (name != null) {
			if (getName().equals(name)) {
				return name;
			}

			return null;
		}

		String[] patterns = getPatterns();

		if (patterns == null) {
			return null;
		}

		for (String pattern : patterns) {
			if (doMatch(pattern, servletPath, pathInfo, extension, match)) {
				return pattern;
			}
		}

		return null;
	}

	//Delegate the handling of the request to the actual servlet
	public void service(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		ClassLoader original = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(classLoader);
			getT().service(req, resp);
		} finally {
			Thread.currentThread().setContextClassLoader(original);
		}
	}

	private void createContextAttributes() {
		contextController.createContextAttributes();
	}

	private void destroyContextAttributes() {
		contextController.destroyContextAttributes();
	}

	@Override
	public int compareTo(EndpointRegistration<?> o) {
		int result = servletHolder.compareTo(o.servletHolder);
		if (result == 0) {
			result = Long.compare(getD().hashCode(), o.getD().hashCode());
		}
		return result;
	}

	@Override
	public String toString() {
		String toString = _toString;

		if (toString == null) {
			toString = SIMPLE_NAME + '[' + getD().toString() + ']';

			_toString = toString;
		}

		return toString;
	}

	private static final String SIMPLE_NAME =
		EndpointRegistration.class.getSimpleName();

	private String _toString;
}
