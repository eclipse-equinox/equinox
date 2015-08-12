/*******************************************************************************
 * Copyright (c) 2014, 2015 Raymond Augé and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Raymond Augé <raymond.auge@liferay.com> - Bug 436698
 ******************************************************************************/

package org.eclipse.equinox.http.servlet.internal.context;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.equinox.http.servlet.internal.registration.EndpointRegistration;
import org.eclipse.equinox.http.servlet.internal.registration.FilterRegistration;
import org.eclipse.equinox.http.servlet.internal.servlet.*;

/**
 * @author Raymond Augé
 */
public class DispatchTargets {

	public DispatchTargets(
		ContextController contextController,
		EndpointRegistration<?> endpointRegistration,
		String servletPath, String pathInfo, String pattern) {

		this(
			contextController, endpointRegistration,
			Collections.<FilterRegistration>emptyList(), servletPath, pathInfo,
			pattern);
	}

	public DispatchTargets(
		ContextController contextController,
		EndpointRegistration<?> endpointRegistration,
		List<FilterRegistration> matchingFilterRegistrations,
		String servletPath, String pathInfo, String pattern) {

		this.contextController = contextController;
		this.endpointRegistration = endpointRegistration;
		this.matchingFilterRegistrations = matchingFilterRegistrations;
		this.servletPath = servletPath;
		this.pathInfo = pathInfo;
	}

	public boolean doDispatch(
			HttpServletRequest request, HttpServletResponse response,
			String requestURI, DispatcherType dispatcherType)
		throws ServletException, IOException {

		if (dispatcherType == DispatcherType.INCLUDE) {
			request.setAttribute(RequestDispatcher.INCLUDE_CONTEXT_PATH, contextController.getContextPath());
			request.setAttribute(RequestDispatcher.INCLUDE_PATH_INFO, getPathInfo());
			request.setAttribute(RequestDispatcher.INCLUDE_QUERY_STRING, request.getQueryString());
			request.setAttribute(RequestDispatcher.INCLUDE_REQUEST_URI, requestURI);
			request.setAttribute(RequestDispatcher.INCLUDE_SERVLET_PATH, getServletPath());
		}
		else if (dispatcherType == DispatcherType.FORWARD) {
			response.resetBuffer();

			request.setAttribute(RequestDispatcher.FORWARD_CONTEXT_PATH, request.getContextPath());
			request.setAttribute(RequestDispatcher.FORWARD_PATH_INFO, request.getPathInfo());
			request.setAttribute(RequestDispatcher.FORWARD_QUERY_STRING, request.getQueryString());
			request.setAttribute(RequestDispatcher.FORWARD_REQUEST_URI, request.getRequestURI());
			request.setAttribute(RequestDispatcher.FORWARD_SERVLET_PATH, request.getServletPath());
		}

		HttpServletRequestBuilder httpRuntimeRequest = HttpServletRequestBuilder.findHttpRuntimeRequest(request);
		boolean pushedState = false;

		try {
			if (httpRuntimeRequest == null) {
				httpRuntimeRequest = new HttpServletRequestBuilder(request, this, dispatcherType);
				request = httpRuntimeRequest;
				response = new HttpServletResponseWrapperImpl(response);
			}
			else {
				httpRuntimeRequest.push(this);
				pushedState = true;
			}

			ResponseStateHandler responseStateHandler = new ResponseStateHandler(
				request, response, this, dispatcherType);

			responseStateHandler.processRequest();

			if ((dispatcherType == DispatcherType.FORWARD) &&
				!response.isCommitted()) {

				response.flushBuffer();
				response.getWriter().close();
			}

			return true;
		}
		finally {
			if (pushedState) {
				httpRuntimeRequest.pop();
			}
		}
	}

	public ContextController getContextController() {
		return contextController;
	}

	public List<FilterRegistration> getMatchingFilterRegistrations() {
		return matchingFilterRegistrations;
	}

	public String getPathInfo() {
		return pathInfo;
	}

	public String getServletPath() {
		return servletPath;
	}

	public EndpointRegistration<?> getServletRegistration() {
		return endpointRegistration;
	}

	private final ContextController contextController;
	private final EndpointRegistration<?> endpointRegistration;
	private final List<FilterRegistration> matchingFilterRegistrations;
	private final String pathInfo;
	private final String servletPath;

}