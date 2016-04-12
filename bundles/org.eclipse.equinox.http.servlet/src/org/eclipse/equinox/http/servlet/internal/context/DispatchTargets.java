/*******************************************************************************
 * Copyright (c) 2014, 2016 Raymond Augé and others.
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
import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
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
		EndpointRegistration<?> endpointRegistration, String servletName,
		String requestURI, String servletPath, String pathInfo, String queryString) {

		this(
			contextController, endpointRegistration,
			Collections.<FilterRegistration>emptyList(), servletName, requestURI,
			servletPath, pathInfo, queryString);
	}

	public DispatchTargets(
		ContextController contextController,
		EndpointRegistration<?> endpointRegistration,
		List<FilterRegistration> matchingFilterRegistrations, String servletName,
		String requestURI, String servletPath, String pathInfo, String queryString) {

		this.contextController = contextController;
		this.endpointRegistration = endpointRegistration;
		this.matchingFilterRegistrations = matchingFilterRegistrations;
		this.servletName = servletName;
		this.requestURI = requestURI;
		this.servletPath = servletPath;
		this.pathInfo = pathInfo;
		this.parameterMap = queryStringToParameterMap(queryString);
		this.queryString = queryString;

		this.string = SIMPLE_NAME + '[' + contextController.getFullContextPath() + requestURI + (queryString != null ? '?' + queryString : "") + ", " + endpointRegistration.toString() + ']'; //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void doDispatch(
			HttpServletRequest request, HttpServletResponse response,
			String path, DispatcherType dispatcherType)
		throws ServletException, IOException {

		HttpServletRequestWrapperImpl requestWrapper = HttpServletRequestWrapperImpl.findHttpRuntimeRequest(request);
		HttpServletResponse responseWrapper = HttpServletResponseWrapperImpl.findHttpRuntimeResponse(response);

		try {
			if (requestWrapper == null) {
				requestWrapper = new HttpServletRequestWrapperImpl(request);
				request = requestWrapper;
			}

			if (responseWrapper == null) {
				responseWrapper = new HttpServletResponseWrapperImpl(response);
				response = responseWrapper;
			}

			requestWrapper.push(this, dispatcherType);

			ResponseStateHandler responseStateHandler = new ResponseStateHandler(
				request, response, this, dispatcherType);

			responseStateHandler.processRequest();
		}
		finally {
			requestWrapper.pop();
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
		if (requestURI == null) {
			return null;
		}
		return getContextController().getFullContextPath() + requestURI;
	}

	public String getServletName() {
		return servletName;
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

	private static final String SIMPLE_NAME = DispatchTargets.class.getSimpleName();

	private final ContextController contextController;
	private final EndpointRegistration<?> endpointRegistration;
	private final List<FilterRegistration> matchingFilterRegistrations;
	private final String pathInfo;
	private final Map<String, String[]> parameterMap;
	private final String queryString;
	private final String requestURI;
	private final String servletPath;
	private final String servletName;
	private final String string;

}