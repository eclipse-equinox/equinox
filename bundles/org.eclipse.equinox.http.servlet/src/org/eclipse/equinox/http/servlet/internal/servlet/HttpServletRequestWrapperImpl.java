/*******************************************************************************
 * Copyright (c) 2005, 2015 Cognos Incorporated, IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cognos Incorporated - initial API and implementation
 *     IBM Corporation - bug fixes and enhancements
 *     Raymond Augé <raymond.auge@liferay.com> - Bug 436698
 *******************************************************************************/
package org.eclipse.equinox.http.servlet.internal.servlet;

import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.eclipse.equinox.http.servlet.internal.context.ContextController;
import org.eclipse.equinox.http.servlet.internal.context.DispatchTargets;
import org.eclipse.equinox.http.servlet.internal.util.*;
import org.osgi.service.http.HttpContext;

public class HttpServletRequestWrapperImpl extends HttpServletRequestWrapper {

	private Stack<DispatchTargets> dispatchTargets = new Stack<DispatchTargets>();
	private final HttpServletRequest request;
	private final DispatcherType dispatcherType;
	private Map<String, String[]> parameterMap;

	public static HttpServletRequestWrapperImpl findHttpRuntimeRequest(
		HttpServletRequest request) {

		while (request instanceof HttpServletRequestWrapper) {
			if (request instanceof HttpServletRequestWrapperImpl) {
				return (HttpServletRequestWrapperImpl)request;
			}

			request = (HttpServletRequest)((HttpServletRequestWrapper)request).getRequest();
		}

		return null;
	}

	public HttpServletRequestWrapperImpl(HttpServletRequest request, DispatchTargets dispatchTargets, DispatcherType dispatcherType) {
		super(request);
		this.request = request;
		this.dispatchTargets.push(dispatchTargets);
		this.dispatcherType = dispatcherType;
	}

	public String getAuthType() {
		String authType = (String)request.getAttribute(HttpContext.AUTHENTICATION_TYPE);
		if (authType != null)
			return authType;

		return request.getAuthType();
	}

	public String getRemoteUser() {
		String remoteUser = (String) request.getAttribute(HttpContext.REMOTE_USER);
		if (remoteUser != null)
			return remoteUser;

		return request.getRemoteUser();
	}

	public String getPathInfo() {
		if (dispatcherType == DispatcherType.INCLUDE)
			return request.getPathInfo();

		return dispatchTargets.peek().getPathInfo();
	}

	public DispatcherType getDispatcherType() {
		return dispatcherType;
	}

	public String getParameter(String name) {
		String[] values = getParameterValues(name);
		if ((values == null) || (values.length == 0)) {
			return null;
		}
		return values[0];
	}

	public Map<String, String[]> getParameterMap() {
		if (this.parameterMap != null) {
			return this.parameterMap;
		}
		Map<String, String[]> copy = new HashMap<String, String[]>(this.dispatchTargets.peek().getParameterMap());
		for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
			String[] values = copy.get(entry.getKey());
			values = Params.append(values, entry.getValue());
			copy.put(entry.getKey(), values);
		}
		this.parameterMap = Collections.unmodifiableMap(copy);
		return this.parameterMap;
	}

	public Enumeration<String> getParameterNames() {
		return Collections.enumeration(getParameterMap().keySet());
	}

	public String[] getParameterValues(String name) {
		return getParameterMap().get(name);
	}

	@Override
	public String getQueryString() {
		String queryStringA = this.dispatchTargets.peek().getQueryString();
		String queryStringB = request.getQueryString();
		if ((queryStringA != null) && (queryStringA.length() > 0) &&
			(queryStringB != null) && (queryStringB.length() > 0)) {

			return queryStringA + Const.AMP + queryStringB;
		}
		return queryStringB;
	}

	public ServletContext getServletContext() {
		return dispatchTargets.peek().getServletRegistration().getServletContext();
	}

	public String getServletPath() {
		if (dispatcherType == DispatcherType.INCLUDE)
			return request.getServletPath();

		if (dispatchTargets.peek().getServletPath().equals(Const.SLASH)) {
			return Const.BLANK;
		}
		return dispatchTargets.peek().getServletPath();
	}

	public String getContextPath() {
		return dispatchTargets.peek().getContextController().getFullContextPath();
	}

	public Object getAttribute(String attributeName) {
		String servletPath = dispatchTargets.peek().getServletPath();
		if (dispatcherType == DispatcherType.INCLUDE) {
			if (attributeName.equals(RequestDispatcher.INCLUDE_CONTEXT_PATH)) {
				String contextPath = (String) request.getAttribute(RequestDispatcher.INCLUDE_CONTEXT_PATH);
				if (contextPath == null || contextPath.equals(Const.SLASH))
					contextPath = Const.BLANK;

				String includeServletPath = (String) request.getAttribute(RequestDispatcher.INCLUDE_SERVLET_PATH);
				if (includeServletPath == null || includeServletPath.equals(Const.SLASH))
					includeServletPath = Const.BLANK;

				return contextPath + includeServletPath;
			} else if (attributeName.equals(RequestDispatcher.INCLUDE_SERVLET_PATH)) {
				String attributeServletPath = (String) request.getAttribute(RequestDispatcher.INCLUDE_SERVLET_PATH);
				if (attributeServletPath != null) {
					return attributeServletPath;
				}
				if (servletPath.equals(Const.SLASH)) {
					return Const.BLANK;
				}
				return servletPath;
			} else if (attributeName.equals(RequestDispatcher.INCLUDE_PATH_INFO)) {
				String pathInfoAttribute = (String) request.getAttribute(RequestDispatcher.INCLUDE_PATH_INFO);
				if (servletPath.equals(Const.SLASH)) {
					return pathInfoAttribute;
				}

				if ((pathInfoAttribute == null) || (pathInfoAttribute.length() == 0)) {
					return null;
				}

				if (pathInfoAttribute.startsWith(servletPath)) {
					pathInfoAttribute = pathInfoAttribute.substring(servletPath.length());
				}

				if (pathInfoAttribute.length() == 0)
					return null;

				return pathInfoAttribute;
			}
		}

		return request.getAttribute(attributeName);
	}

	public RequestDispatcher getRequestDispatcher(String path) {
		ContextController contextController =
			this.dispatchTargets.peek().getContextController();

		// support relative paths
		if (!path.startsWith(Const.SLASH)) {
			path = this.dispatchTargets.peek().getServletPath() + Const.SLASH + path;
		}
		// if the path starts with the full context path strip it
		else if (path.startsWith(contextController.getFullContextPath())) {
			path = path.substring(contextController.getFullContextPath().length());
		}

		DispatchTargets requestedDispatchTargets = contextController.getDispatchTargets(path, null);

		if (requestedDispatchTargets == null) {
			return null;
		}

		return new RequestDispatcherAdaptor(requestedDispatchTargets, path);
	}

	public static String getDispatchPathInfo(HttpServletRequest req) {
		if (req.getDispatcherType() == DispatcherType.INCLUDE)
			return (String) req.getAttribute(RequestDispatcher.INCLUDE_PATH_INFO);

		return req.getPathInfo();
	}

	public static String getDispatchServletPath(HttpServletRequest req) {
		if (req.getDispatcherType() == DispatcherType.INCLUDE) {
			String servletPath = (String) req.getAttribute(RequestDispatcher.INCLUDE_SERVLET_PATH);
			return (servletPath == null) ? Const.BLANK : servletPath;
		}
		return req.getServletPath();
	}

	public HttpSession getSession() {
		HttpSession session = request.getSession();
		if (session != null) {
			return dispatchTargets.peek().getContextController().getSessionAdaptor(
				session, dispatchTargets.peek().getServletRegistration().getT().getServletConfig().getServletContext());
		}

		return null;
	}

	public HttpSession getSession(boolean create) {
		HttpSession session = request.getSession(create);
		if (session != null) {
			return dispatchTargets.peek().getContextController().getSessionAdaptor(
				session, dispatchTargets.peek().getServletRegistration().getT().getServletConfig().getServletContext());
		}

		return null;
	}

	public synchronized void pop() {
		this.dispatchTargets.pop();
		this.parameterMap = null;
		getParameterMap();
	}

	public synchronized void push(DispatchTargets toPush) {
		this.dispatchTargets.push(toPush);
		this.parameterMap = null;
		getParameterMap();
	}

	public void removeAttribute(String name) {
		request.removeAttribute(name);

		EventListeners eventListeners = dispatchTargets.peek().getContextController().getEventListeners();

		List<ServletRequestAttributeListener> listeners = eventListeners.get(
			ServletRequestAttributeListener.class);

		if (listeners.isEmpty()) {
			return;
		}

		ServletRequestAttributeEvent servletRequestAttributeEvent =
			new ServletRequestAttributeEvent(
				dispatchTargets.peek().getServletRegistration().getServletContext(), this, name, null);

		for (ServletRequestAttributeListener servletRequestAttributeListener : listeners) {
			servletRequestAttributeListener.attributeRemoved(
				servletRequestAttributeEvent);
		}
	}

	public void setAttribute(String name, Object value) {
		boolean added = (request.getAttribute(name) == null);
		request.setAttribute(name, value);

		EventListeners eventListeners = dispatchTargets.peek().getContextController().getEventListeners();

		List<ServletRequestAttributeListener> listeners = eventListeners.get(
			ServletRequestAttributeListener.class);

		if (listeners.isEmpty()) {
			return;
		}

		ServletRequestAttributeEvent servletRequestAttributeEvent =
			new ServletRequestAttributeEvent(
				dispatchTargets.peek().getServletRegistration().getServletContext(), this, name, value);

		for (ServletRequestAttributeListener servletRequestAttributeListener : listeners) {
			if (added) {
				servletRequestAttributeListener.attributeAdded(
					servletRequestAttributeEvent);
			}
			else {
				servletRequestAttributeListener.attributeReplaced(
					servletRequestAttributeEvent);
			}
		}
	}

}