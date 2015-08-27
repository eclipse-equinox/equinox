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
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.equinox.http.servlet.internal.registration.EndpointRegistration;
import org.eclipse.equinox.http.servlet.internal.registration.FilterRegistration;
import org.eclipse.equinox.http.servlet.internal.servlet.*;
import org.eclipse.equinox.http.servlet.internal.util.Const;
import org.eclipse.equinox.http.servlet.internal.util.Params;

/**
 * @author Raymond Augé
 */
public class DispatchTargets {

	public DispatchTargets(
		ContextController contextController,
		EndpointRegistration<?> endpointRegistration,
		String requestURI, String servletPath, String pathInfo, String queryString) {

		this(
			contextController, endpointRegistration,
			Collections.<FilterRegistration>emptyList(), requestURI, servletPath, pathInfo,
			queryString);
	}

	public DispatchTargets(
		ContextController contextController,
		EndpointRegistration<?> endpointRegistration,
		List<FilterRegistration> matchingFilterRegistrations,
		String requestURI, String servletPath, String pathInfo, String queryString) {

		this.contextController = contextController;
		this.endpointRegistration = endpointRegistration;
		this.matchingFilterRegistrations = matchingFilterRegistrations;
		this.requestURI = requestURI;
		this.servletPath = servletPath;
		this.pathInfo = pathInfo;
		this.parameterMap = queryStringToParameterMap(queryString);
		this.queryString = queryString;

		this.string = getClass().getSimpleName() + '[' + contextController.getFullContextPath() + requestURI + ", " + endpointRegistration.toString() + ']'; //$NON-NLS-1$
	}

	public boolean doDispatch(
			HttpServletRequest request, HttpServletResponse response,
			String path, DispatcherType dispatcherType)
		throws ServletException, IOException {

		if (dispatcherType == DispatcherType.INCLUDE) {
			request.setAttribute(RequestDispatcher.INCLUDE_CONTEXT_PATH, contextController.getContextPath());
			request.setAttribute(RequestDispatcher.INCLUDE_PATH_INFO, getPathInfo());
			request.setAttribute(RequestDispatcher.INCLUDE_QUERY_STRING, getQueryString());
			request.setAttribute(RequestDispatcher.INCLUDE_REQUEST_URI, getRequestURI());
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

		HttpServletRequestBuilderWrapperImpl httpRuntimeRequest = HttpServletRequestBuilderWrapperImpl.findHttpRuntimeRequest(request);
		boolean pushedState = false;

		try {
			if (httpRuntimeRequest == null) {
				httpRuntimeRequest = new HttpServletRequestBuilderWrapperImpl(request, this, dispatcherType);
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

	public Map<String, String[]> getParameterMap() {
		return parameterMap;
	}

	public String getPathInfo() {
		return pathInfo;
	}

	public String getQueryString() {
		return queryString;
	}

	public String getRequestURI() {
		return requestURI;
	}

	public String getServletPath() {
		return servletPath;
	}

	public EndpointRegistration<?> getServletRegistration() {
		return endpointRegistration;
	}

	@Override
	public String toString() {
		return string;
	}

	private static Map<String, String[]> queryStringToParameterMap(String queryString) {
		if ((queryString == null) || (queryString.length() == 0)) {
			return Collections.emptyMap();
		}

		try {
			Map<String, String[]> parameterMap = new LinkedHashMap<String, String[]>();
			String[] parameters = queryString.split(Const.AMP);
			for (String parameter : parameters) {
				int index = parameter.indexOf('=');
				String name = (index > 0) ? URLDecoder.decode(parameter.substring(0, index), Const.UTF8) : parameter;
				String[] values = parameterMap.get(name);
				if (values == null) {
					values = new String[0];
				}
				String value = ((index > 0) && (parameter.length() > index + 1)) ? URLDecoder.decode(parameter.substring(index + 1), Const.UTF8) : null;
				values = Params.append(values, value);
				parameterMap.put(name, values);
			}
			return Collections.unmodifiableMap(parameterMap);
		}
		catch (UnsupportedEncodingException unsupportedEncodingException) {
			throw new RuntimeException(unsupportedEncodingException);
		}
	}

	private final ContextController contextController;
	private final EndpointRegistration<?> endpointRegistration;
	private final List<FilterRegistration> matchingFilterRegistrations;
	private final String pathInfo;
	private final Map<String, String[]> parameterMap;
	private final String queryString;
	private final String requestURI;
	private final String servletPath;
	private final String string;

}