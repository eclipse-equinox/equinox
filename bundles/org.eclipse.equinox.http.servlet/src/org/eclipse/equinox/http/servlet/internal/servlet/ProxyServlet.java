/*******************************************************************************
 * Copyright (c) 2005, 2016 Cognos Incorporated, IBM Corporation and others.
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
 *     Raymond Augé - bug fixes and enhancements
 *     Arnaud Mergey <a_mergey@yahoo.fr> - Bug 497510
 *******************************************************************************/
package org.eclipse.equinox.http.servlet.internal.servlet;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.servlet.*;
import javax.servlet.http.*;
import org.eclipse.equinox.http.servlet.internal.Activator;
import org.eclipse.equinox.http.servlet.internal.HttpServiceRuntimeImpl;
import org.eclipse.equinox.http.servlet.internal.context.DispatchTargets;
import org.eclipse.equinox.http.servlet.internal.registration.PreprocessorRegistration;
import org.eclipse.equinox.http.servlet.internal.util.Const;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.whiteboard.Preprocessor;

/**
 * The ProxyServlet is the private side of a Servlet that when registered (and init() called) in a servlet container
 * will in-turn register and provide an OSGi Http Service implementation.
 * This class is not meant for extending or even using directly and is purely meant for registering
 * in a servlet container.
 */
public class ProxyServlet extends HttpServlet {

	private static final long serialVersionUID = 4117456123807468871L;
	private HttpServiceRuntimeImpl httpServiceRuntimeImpl;

	public void init(ServletConfig config) throws ServletException {
		super.init(config);

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

	public void sessionDestroyed(String sessionId) {
		httpServiceRuntimeImpl.sessionDestroyed(sessionId);
	}

	public void sessionIdChanged(String oldSessionId) {
		httpServiceRuntimeImpl.fireSessionIdChanged(oldSessionId);
	}

	/**
	 * get the value of path info, not decoded by the server
	 */
	private String getNotDecodedAlias(HttpServletRequest request) {
		String pathInfo = HttpServletRequestWrapperImpl.getDispatchPathInfo(request);
		if(pathInfo == null) {
			return null;
		}
		String requestUri = HttpServletRequestWrapperImpl.getDispatchRequestURI(request);
		String contextPath = request.getContextPath();
		String servletPath = request.getServletPath();
		if (request.getDispatcherType() == DispatcherType.INCLUDE) {
			contextPath = (String)request.getAttribute(RequestDispatcher.INCLUDE_CONTEXT_PATH);
			servletPath = (String)request.getAttribute(RequestDispatcher.INCLUDE_SERVLET_PATH);
		}
		return requestUri.substring(contextPath.length() + servletPath.length());
	}

	/**
	 * @see HttpServlet#service(ServletRequest, ServletResponse)
	 */
	protected void service(
			HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {

		checkRuntime();

		String alias = getNotDecodedAlias(request);

		if (alias == null) {
			alias = Const.SLASH;
		}

		preprocess(request, response, alias, request.getDispatcherType());
	}

	public void preprocess(
			HttpServletRequest request,
			HttpServletResponse response, String alias, DispatcherType dispatcherType)
		throws ServletException, IOException {

		Map<ServiceReference<Preprocessor>, PreprocessorRegistration> registrations = httpServiceRuntimeImpl.getPreprocessorRegistrations();

		if (registrations.isEmpty()) {
			dispatch(request, response, alias, dispatcherType);
		}
		else {
			List<PreprocessorRegistration> preprocessors = new CopyOnWriteArrayList<>();

			for (Entry<ServiceReference<Preprocessor>, PreprocessorRegistration> entry : registrations.entrySet()) {
				PreprocessorRegistration registration = entry.getValue();
				preprocessors.add(registration);
				registration.addReference();
			}

			try {
				FilterChain chain = new PreprocessorChainImpl(preprocessors, alias, dispatcherType, this);

				chain.doFilter(request, response);
			}
			finally {
				for (PreprocessorRegistration registration : preprocessors) {
					registration.removeReference();
				}
			}
		}
	}

	public void dispatch(
			HttpServletRequest request,
			HttpServletResponse response, String alias, DispatcherType dispatcherType)
		throws ServletException, IOException {

		DispatchTargets dispatchTargets = httpServiceRuntimeImpl.getDispatchTargets(alias, null);

		if (dispatchTargets != null) {
			dispatchTargets.doDispatch(request, response, alias, dispatcherType);

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
