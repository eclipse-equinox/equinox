/*******************************************************************************
 * Copyright (c) 2005, 2014 Cognos Incorporated, IBM Corporation and others.
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

import java.lang.reflect.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.eclipse.equinox.http.servlet.internal.context.DispatchTargets;
import org.eclipse.equinox.http.servlet.internal.registration.EndpointRegistration;
import org.eclipse.equinox.http.servlet.internal.util.Const;
import org.eclipse.equinox.http.servlet.internal.util.EventListeners;
import org.osgi.service.http.HttpContext;

public class HttpServletRequestBuilder {

	static interface RequestGetter {
		HttpServletRequest getOriginalRequest();
	}

	private DispatchTargets dispatchTargets;
	private EndpointRegistration<?> servletRegistration;
	private final HttpServletRequest request;
	private HttpServletRequest requestProxy;
	private boolean isRequestDispatcherInclude;

	static final String INCLUDE_REQUEST_URI_ATTRIBUTE = "javax.servlet.include.request_uri"; //$NON-NLS-1$
	static final String INCLUDE_CONTEXT_PATH_ATTRIBUTE = "javax.servlet.include.context_path"; //$NON-NLS-1$
	static final String INCLUDE_SERVLET_PATH_ATTRIBUTE = "javax.servlet.include.servlet_path"; //$NON-NLS-1$
	static final String INCLUDE_PATH_INFO_ATTRIBUTE = "javax.servlet.include.path_info"; //$NON-NLS-1$

	private final ThreadLocal<HttpServletRequest> requestTL = new ThreadLocal<HttpServletRequest>();

	private final static Map<Method, Method> requestToHandlerMethods;

	static {
		requestToHandlerMethods = createContextToHandlerMethods();
	}

	private static Map<Method, Method> createContextToHandlerMethods() {
		Map<Method, Method> methods = new HashMap<Method, Method>();
		Method[] handlerMethods =
			HttpServletRequestBuilder.class.getDeclaredMethods();

		for (int i = 0; i < handlerMethods.length; i++) {
			Method handlerMethod = handlerMethods[i];
			String name = handlerMethod.getName();
			Class<?>[] parameterTypes = handlerMethod.getParameterTypes();

			try {
				Method method = HttpServletRequest.class.getMethod(
					name, parameterTypes);
				methods.put(method, handlerMethod);
			}
			catch (NoSuchMethodException e) {
				// do nothing
			}
		}

		return methods;
	}

	public HttpServletRequestBuilder(HttpServletRequest request, DispatchTargets dispatchTargets) {
		this.request = request;
		this.dispatchTargets = dispatchTargets;
		this.servletRegistration = dispatchTargets.getServletRegistration();

		isRequestDispatcherInclude = request.getAttribute(HttpServletRequestBuilder.INCLUDE_REQUEST_URI_ATTRIBUTE) != null;

		this.requestProxy = (HttpServletRequest)Proxy.newProxyInstance(
			getClass().getClassLoader(),
			new Class[] {HttpServletRequest.class, RequestGetter.class},
			new InvocationHandler() {

				@Override
				public Object invoke(Object proxy, Method method, Object[] args)
					throws Throwable {

					if (method.getName().equals("getOriginalRequest")) {
						return getOriginalRequest();
					}

					return HttpServletRequestBuilder.this.invoke(proxy, method, args);
				}

			}
		);
	}

	public HttpServletRequest build() {
		return requestProxy;
	}

	Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		requestTL.set((HttpServletRequest)proxy);

		try {
			Method m = requestToHandlerMethods.get(method);
			if (m != null) {
				return m.invoke(this, args);
			}
			return method.invoke(request, args);
		}
		finally {
			requestTL.remove();
		}
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
		if (isRequestDispatcherInclude)
			return request.getPathInfo();

		return dispatchTargets.getPathInfo();
	}

	public ServletContext getServletContext() {
		return servletRegistration.getServletContext();
	}

	public String getServletPath() {
		if (isRequestDispatcherInclude)
			return request.getServletPath();

		if (dispatchTargets.getServletPath().equals(Const.SLASH)) {
			return Const.BLANK;
		}
		return dispatchTargets.getServletPath();
	}

	public String getContextPath() {
		return dispatchTargets.getContextController().getFullContextPath();
	}

	public Object getAttribute(String attributeName) {
		String servletPath = dispatchTargets.getServletPath();
		if (isRequestDispatcherInclude) {
			if (attributeName.equals(HttpServletRequestBuilder.INCLUDE_CONTEXT_PATH_ATTRIBUTE)) {
				String contextPath = (String) request.getAttribute(HttpServletRequestBuilder.INCLUDE_CONTEXT_PATH_ATTRIBUTE);
				if (contextPath == null || contextPath.equals(Const.SLASH))
					contextPath = Const.BLANK;

				String includeServletPath = (String) request.getAttribute(HttpServletRequestBuilder.INCLUDE_SERVLET_PATH_ATTRIBUTE);
				if (includeServletPath == null || includeServletPath.equals(Const.SLASH))
					includeServletPath = Const.BLANK;

				return contextPath + includeServletPath;
			} else if (attributeName.equals(HttpServletRequestBuilder.INCLUDE_SERVLET_PATH_ATTRIBUTE)) {
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
		if (!path.startsWith(getContextPath())) {
			path = getContextPath().substring(
				request.getContextPath().length()).concat(path);
		}

		return new RequestDispatcherAdaptor(request.getRequestDispatcher(path));
	}

	public static String getDispatchPathInfo(HttpServletRequest req) {
		if (req.getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE) != null)
			return (String) req.getAttribute(INCLUDE_PATH_INFO_ATTRIBUTE);

		return req.getPathInfo();
	}

	public static String getDispatchServletPath(HttpServletRequest req) {
		if (req.getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE) != null) {
			String servletPath = (String) req.getAttribute(INCLUDE_SERVLET_PATH_ATTRIBUTE);
			return (servletPath == null) ? Const.BLANK : servletPath;
		}
		return req.getServletPath();
	}

	public HttpSession getSession() {
		HttpSession session = request.getSession();
		if (session != null) {
			return new HttpSessionAdaptor(
				session, servletRegistration.getT(),
				dispatchTargets.getContextController().getEventListeners());
		}

		return null;
	}

	public HttpSession getSession(boolean create) {
		HttpSession session = request.getSession(create);
		if (session != null) {
			return new HttpSessionAdaptor(
				session, servletRegistration.getT(),
				dispatchTargets.getContextController().getEventListeners());
		}

		return null;
	}

	public void removeAttribute(String name) {
		request.removeAttribute(name);

		EventListeners eventListeners = dispatchTargets.getContextController().getEventListeners();

		List<ServletRequestAttributeListener> listeners = eventListeners.get(
			ServletRequestAttributeListener.class);

		if (listeners.isEmpty()) {
			return;
		}

		ServletRequestAttributeEvent servletRequestAttributeEvent =
			new ServletRequestAttributeEvent(
				servletRegistration.getServletContext(), requestProxy, name, null);

		for (ServletRequestAttributeListener servletRequestAttributeListener : listeners) {
			servletRequestAttributeListener.attributeRemoved(
				servletRequestAttributeEvent);
		}
	}

	public void setAttribute(String name, Object value) {
		boolean added = (request.getAttribute(name) == null);
		request.setAttribute(name, value);

		EventListeners eventListeners = dispatchTargets.getContextController().getEventListeners();

		List<ServletRequestAttributeListener> listeners = eventListeners.get(
			ServletRequestAttributeListener.class);

		if (listeners.isEmpty()) {
			return;
		}

		ServletRequestAttributeEvent servletRequestAttributeEvent =
			new ServletRequestAttributeEvent(
				servletRegistration.getServletContext(), requestProxy, name, value);

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

	HttpServletRequest getOriginalRequest() {
		return request;
	}

}