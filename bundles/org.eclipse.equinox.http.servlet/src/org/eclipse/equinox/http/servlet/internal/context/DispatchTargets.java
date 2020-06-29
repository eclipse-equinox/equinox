/*******************************************************************************
 * Copyright (c) 2014, 2020 Raymond Augé and others.
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
 *     Istvan Sajtos <istvan.sajtos@liferay.com> - Bug 490608
 ******************************************************************************/

package org.eclipse.equinox.http.servlet.internal.context;

import java.io.*;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.*;
import javax.servlet.http.*;
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
		this.servletPath = (servletPath == null) ? Const.BLANK : servletPath;
		this.pathInfo = pathInfo;
		this.queryString = queryString;
	}

	public void addRequestParameters(HttpServletRequest request) {
		currentRequest = request;
	}

	public void doDispatch(
			HttpServletRequest originalRequest, HttpServletResponse response,
			String path, DispatcherType requestedDispatcherType)
		throws ServletException, IOException {

		setDispatcherType(requestedDispatcherType);

		HttpServletRequest request = originalRequest;
		HttpServletRequestWrapperImpl requestWrapper = HttpServletRequestWrapperImpl.findHttpRuntimeRequest(originalRequest);
		HttpServletResponseWrapper responseWrapper = HttpServletResponseWrapperImpl.findHttpRuntimeResponse(response);

		boolean includeWrapperAdded = false;

		try (RequestAttributeSetter setter = new RequestAttributeSetter(originalRequest)) {
			if (dispatcherType == DispatcherType.INCLUDE) {
				setter.setAttribute(RequestDispatcher.INCLUDE_CONTEXT_PATH, contextController.getFullContextPath());
				setter.setAttribute(RequestDispatcher.INCLUDE_PATH_INFO, getPathInfo());
				setter.setAttribute(RequestDispatcher.INCLUDE_QUERY_STRING, getQueryString());
				setter.setAttribute(RequestDispatcher.INCLUDE_REQUEST_URI, getRequestURI());
				setter.setAttribute(RequestDispatcher.INCLUDE_SERVLET_PATH, getServletPath());
			}
			else if (dispatcherType == DispatcherType.FORWARD) {
				response.resetBuffer();

				setter.setAttribute(RequestDispatcher.FORWARD_CONTEXT_PATH, originalRequest.getContextPath());
				setter.setAttribute(RequestDispatcher.FORWARD_PATH_INFO, originalRequest.getPathInfo());
				setter.setAttribute(RequestDispatcher.FORWARD_QUERY_STRING, originalRequest.getQueryString());
				setter.setAttribute(RequestDispatcher.FORWARD_REQUEST_URI, originalRequest.getRequestURI());
				setter.setAttribute(RequestDispatcher.FORWARD_SERVLET_PATH, originalRequest.getServletPath());
			}

			if (requestWrapper == null) {
				requestWrapper = new HttpServletRequestWrapperImpl(originalRequest);
				request = requestWrapper;
			}

			if (responseWrapper == null) {
				responseWrapper = new HttpServletResponseWrapperImpl(response);
				response = responseWrapper;
			}

			requestWrapper.push(this);

			if ((dispatcherType == DispatcherType.INCLUDE) && !(responseWrapper.getResponse() instanceof IncludeDispatchResponseWrapper)) {
				// add the include wrapper to avoid header and status writes
				responseWrapper.setResponse(new IncludeDispatchResponseWrapper((HttpServletResponse)responseWrapper.getResponse()));
				includeWrapperAdded = true;
			}

			ResponseStateHandler responseStateHandler = new ResponseStateHandler(request, response, this);

			responseStateHandler.processRequest();
		}
		finally {
			if ((dispatcherType == DispatcherType.INCLUDE) && (responseWrapper.getResponse() instanceof IncludeDispatchResponseWrapper) && includeWrapperAdded) {
				// remove the include wrapper we added
				responseWrapper.setResponse(((IncludeDispatchResponseWrapper)responseWrapper.getResponse()).getResponse());
			}

			requestWrapper.pop();
		}
	}

	public ContextController getContextController() {
		return contextController;
	}

	public DispatcherType getDispatcherType() {
		return dispatcherType;
	}

	public List<FilterRegistration> getMatchingFilterRegistrations() {
		return matchingFilterRegistrations;
	}

	public Map<String, String[]> getParameterMap() {
		if ((parameterMap == null) && (currentRequest != null)) {
			Map<String, String[]> parameterMapCopy = queryStringToParameterMap(queryString);

			for (Map.Entry<String, String[]> entry : currentRequest.getParameterMap().entrySet()) {
				String[] values = parameterMapCopy.get(entry.getKey());
				if (!Arrays.equals(values, entry.getValue())) {
					values = Params.append(values, entry.getValue());
					parameterMapCopy.put(entry.getKey(), values);
				}
			}

			parameterMap = Collections.unmodifiableMap(parameterMapCopy);
		}
		return parameterMap;
	}

	public String getPathInfo() {
		return pathInfo;
	}

	public String getQueryString() {
		if ((queryString == null) && (currentRequest != null)) {
			queryString = currentRequest.getQueryString();
		}
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

	public Map<String, Object> getSpecialOverides() {
		return specialOverides;
	}

	public void setDispatcherType(DispatcherType dispatcherType) {
		this.dispatcherType = dispatcherType;
	}

	@Override
	public String toString() {
		String value = string;

		if (value == null) {
			value = SIMPLE_NAME + '[' + contextController.getFullContextPath() + requestURI + (queryString != null ? '?' + queryString : "") + ", " + endpointRegistration.toString() + ']'; //$NON-NLS-1$ //$NON-NLS-2$

			string = value;
		}

		return value;
	}

	private static Map<String, String[]> queryStringToParameterMap(String queryString) {
		if ((queryString == null) || (queryString.length() == 0)) {
			return new LinkedHashMap<String, String[]>();
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
				String value = ((index > 0) && (parameter.length() > index + 1)) ? URLDecoder.decode(parameter.substring(index + 1), Const.UTF8) : ""; //$NON-NLS-1$
				values = Params.append(values, value);
				parameterMap.put(name, values);
			}
			return parameterMap;
		}
		catch (UnsupportedEncodingException unsupportedEncodingException) {
			throw new RuntimeException(unsupportedEncodingException);
		}
	}

	private static class RequestAttributeSetter implements Closeable {

		private final ServletRequest servletRequest;
		private final Map<String, Object> oldValues = new HashMap<String, Object>();

		public RequestAttributeSetter(ServletRequest servletRequest) {
			this.servletRequest = servletRequest;
		}

		public void setAttribute(String name, Object value) {
			oldValues.put(name, servletRequest.getAttribute(name));

			servletRequest.setAttribute(name, value);
		}

		@Override
		public void close() {
			for (Map.Entry<String, Object> oldValue : oldValues.entrySet()) {
				if (oldValue.getValue() == null) {
					servletRequest.removeAttribute(oldValue.getKey());
				}
				else {
					servletRequest.setAttribute(oldValue.getKey(), oldValue.getValue());
				}
			}
		}
	}

	private static final String SIMPLE_NAME = DispatchTargets.class.getSimpleName();

	private final ContextController contextController;
	private DispatcherType dispatcherType;
	private final EndpointRegistration<?> endpointRegistration;
	private volatile HttpServletRequest currentRequest;
	private final List<FilterRegistration> matchingFilterRegistrations;
	private final String pathInfo;
	private Map<String, String[]> parameterMap;
	private String queryString;
	private final String requestURI;
	private final String servletPath;
	private final String servletName;
	private final Map<String, Object> specialOverides = new ConcurrentHashMap<String, Object>();
	private String string;

}