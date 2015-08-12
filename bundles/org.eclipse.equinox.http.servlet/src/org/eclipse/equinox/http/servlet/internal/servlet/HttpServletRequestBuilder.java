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

import java.util.List;
import java.util.Stack;
import javax.servlet.*;
import javax.servlet.http.*;
import org.eclipse.equinox.http.servlet.internal.context.ContextController;
import org.eclipse.equinox.http.servlet.internal.context.DispatchTargets;
import org.eclipse.equinox.http.servlet.internal.util.Const;
import org.eclipse.equinox.http.servlet.internal.util.EventListeners;
import org.osgi.service.http.HttpContext;

public class HttpServletRequestBuilder extends HttpServletRequestWrapper {

	private Stack<DispatchTargets> dispatchTargets = new Stack<DispatchTargets>();
	private final HttpServletRequest request;
	private final DispatcherType dispatcherType;

	static final String INCLUDE_REQUEST_URI_ATTRIBUTE = "javax.servlet.include.request_uri"; //$NON-NLS-1$
	static final String INCLUDE_CONTEXT_PATH_ATTRIBUTE = "javax.servlet.include.context_path"; //$NON-NLS-1$
	static final String INCLUDE_SERVLET_PATH_ATTRIBUTE = "javax.servlet.include.servlet_path"; //$NON-NLS-1$
	static final String INCLUDE_PATH_INFO_ATTRIBUTE = "javax.servlet.include.path_info"; //$NON-NLS-1$

	public static HttpServletRequestBuilder findHttpRuntimeRequest(
		HttpServletRequest request) {

		while (request instanceof HttpServletRequestWrapper) {
			if (request instanceof HttpServletRequestBuilder) {
				return (HttpServletRequestBuilder)request;
			}

			request = (HttpServletRequest)((HttpServletRequestWrapper)request).getRequest();
		}

		return null;
	}

	public HttpServletRequestBuilder(HttpServletRequest request, DispatchTargets dispatchTargets, DispatcherType dispatcherType) {
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
			if (attributeName.equals(HttpServletRequestBuilder.INCLUDE_CONTEXT_PATH_ATTRIBUTE)) {
				String contextPath = (String) request.getAttribute(HttpServletRequestBuilder.INCLUDE_CONTEXT_PATH_ATTRIBUTE);
				if (contextPath == null || contextPath.equals(Const.SLASH))
					contextPath = Const.BLANK;

				String includeServletPath = (String) request.getAttribute(HttpServletRequestBuilder.INCLUDE_SERVLET_PATH_ATTRIBUTE);
				if (includeServletPath == null || includeServletPath.equals(Const.SLASH))
					includeServletPath = Const.BLANK;

				return contextPath + includeServletPath;
			} else if (attributeName.equals(HttpServletRequestBuilder.INCLUDE_SERVLET_PATH_ATTRIBUTE)) {
				String attributeServletPath = (String) request.getAttribute(HttpServletRequestBuilder.INCLUDE_SERVLET_PATH_ATTRIBUTE);
				if (attributeServletPath != null) {
					return attributeServletPath;
				}
				if (servletPath.equals(Const.SLASH)) {
					return Const.BLANK;
				}
				return servletPath;
			} else if (attributeName.equals(HttpServletRequestBuilder.INCLUDE_PATH_INFO_ATTRIBUTE)) {
				String pathInfoAttribute = (String) request.getAttribute(HttpServletRequestBuilder.INCLUDE_PATH_INFO_ATTRIBUTE);
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
			return (String) req.getAttribute(INCLUDE_PATH_INFO_ATTRIBUTE);

		return req.getPathInfo();
	}

	public static String getDispatchServletPath(HttpServletRequest req) {
		if (req.getDispatcherType() == DispatcherType.INCLUDE) {
			String servletPath = (String) req.getAttribute(INCLUDE_SERVLET_PATH_ATTRIBUTE);
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
	}

	public synchronized void push(DispatchTargets dispatchTargets) {
		this.dispatchTargets.push(dispatchTargets);
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