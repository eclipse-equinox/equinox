/*******************************************************************************
 * Copyright (c) 2005, 2016 Cognos Incorporated, IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cognos Incorporated - initial API and implementation
 *     IBM Corporation - bug fixes and enhancements
 *     Raymond Augé - bug fixes and enhancements
 *******************************************************************************/
package org.eclipse.equinox.http.servlet.internal.servlet;

import java.io.IOException;
import javax.servlet.*;
import javax.servlet.http.*;
import org.eclipse.equinox.http.servlet.internal.Activator;
import org.eclipse.equinox.http.servlet.internal.HttpServiceRuntimeImpl;
import org.eclipse.equinox.http.servlet.internal.context.DispatchTargets;
import org.eclipse.equinox.http.servlet.internal.registration.EndpointRegistration;
import org.eclipse.equinox.http.servlet.internal.registration.ServletRegistration;
import org.eclipse.equinox.http.servlet.internal.util.Const;

/**
 * The ProxyServlet is the private side of a Servlet that when registered (and init() called) in a servlet container
 * will in-turn register and provide an OSGi Http Service implementation.
 * This class is not meant for extending or even using directly and is purely meant for registering
 * in a servlet container.
 */
public class ProxyServlet extends HttpServlet {

	private static final long serialVersionUID = 4117456123807468871L;
	protected static final String MULTIPART_SERVLET_NAME_KEY = "multipart.servlet.name"; //$NON-NLS-1$
	private HttpServiceRuntimeImpl httpServiceRuntimeImpl;
	private String multipartServletName;

	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		multipartServletName = getInitParameter(MULTIPART_SERVLET_NAME_KEY);

		Activator.addProxyServlet(this);
	}

	public void destroy() {
		Activator.unregisterHttpService(this);

		super.destroy();
	}

	public void setHttpServiceRuntimeImpl(
		HttpServiceRuntimeImpl httpServiceRuntimeImpl) {

		this.httpServiceRuntimeImpl = httpServiceRuntimeImpl;
	}

	public void sessionIdChanged(String oldSessionId) {
		httpServiceRuntimeImpl.fireSessionIdChanged(oldSessionId);
	}

	/**
	 * @see HttpServlet#service(ServletRequest, ServletResponse)
	 */
	protected void service(
			HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {

		checkRuntime();

		String alias = HttpServletRequestWrapperImpl.getDispatchPathInfo(request);

		if (alias == null) {
			alias = Const.SLASH;
		}

		DispatchTargets dispatchTargets = httpServiceRuntimeImpl.getDispatchTargets(alias, null);

		if ((dispatchTargets != null) && (multipartServletName != null)) {
			EndpointRegistration<?> endpointRegistration = dispatchTargets.getServletRegistration();

			if (endpointRegistration instanceof ServletRegistration) {
				ServletRegistration servletRegistration = (ServletRegistration)endpointRegistration;

				if (servletRegistration.getD().multipartSupported) {
					RequestDispatcher multipartDispatcher = getServletContext().getNamedDispatcher(multipartServletName);

					if (multipartDispatcher != null) {
						request.setAttribute(DispatchTargets.class.getName(), dispatchTargets);

						multipartDispatcher.forward(request, response);

						return;
					}
				}
			}
		}

		if (dispatchTargets != null) {
			dispatchTargets.doDispatch(
				request, response, alias, request.getDispatcherType());

			return;
		}

		response.sendError(
			HttpServletResponse.SC_NOT_FOUND, "ProxyServlet: " + alias); //$NON-NLS-1$
	}

	private void checkRuntime() {
		if (httpServiceRuntimeImpl == null) {
			throw new IllegalStateException(
				"Proxy servlet not properly initialized. httpServiceRuntimeImpl is null"); //$NON-NLS-1$
		}
	}

}
