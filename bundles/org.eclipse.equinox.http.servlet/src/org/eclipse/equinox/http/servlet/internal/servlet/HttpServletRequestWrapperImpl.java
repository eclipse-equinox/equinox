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

	public class State {

		public State(
			DispatchTargets dispatchTargets,
			DispatcherType dispatcherType, Map<String, Object> previousAttributes,
			Map<String, String[]> previousParams, String previousQueryString) {

			this.dispatchTargets = dispatchTargets;
			this.dispatcherType = dispatcherType;

			Map<String, Object> attributesCopy = new HashMap<String, Object>();

			attributesCopy.putAll(previousAttributes);

			this.attributes = attributesCopy;

			Map<String, String[]> parameterMapCopy = new HashMap<String, String[]>();

			// add the dispatchers query string parameters first
			for (Map.Entry<String, String[]> entry : dispatchTargets.getParameterMap().entrySet()) {
				String[] values = parameterMapCopy.get(entry.getKey());
				values = Params.append(values, entry.getValue());
				parameterMapCopy.put(entry.getKey(), values);
			}

			// add the previous dispatcher's parameters next
			if (previousParams != null) {
				for (Map.Entry<String, String[]> entry : previousParams.entrySet()) {
					String[] values = parameterMapCopy.get(entry.getKey());
					values = Params.append(values, entry.getValue());
					parameterMapCopy.put(entry.getKey(), values);
				}
			}

			this.parameterMap = parameterMapCopy;

			String queryStringCopy = previousQueryString;

			if ((dispatchTargets.getQueryString() != null) && (dispatchTargets.getQueryString().length() > 0) &&
				(queryStringCopy != null) && (queryStringCopy.length() > 0)) {

				queryStringCopy = dispatchTargets.getQueryString() + Const.AMP + queryStringCopy;
			}

			this.queryString = queryStringCopy;
			this.previousQueryString = previousQueryString;

			this.string = getClass().getSimpleName() + '[' + dispatcherType + ", " + dispatchTargets + ", " + queryString + ']'; //$NON-NLS-1$ //$NON-NLS-2$
		}

		public Map<String, Object> getAttributes() {
			return attributes;
		}

		public DispatcherType getDispatcherType() {
			return dispatcherType;
		}

		public DispatchTargets getDispatchTargets() {
			return dispatchTargets;
		}

		public Map<String, Object> getOverloadedAttributes() {
			return overloadedAttributes;
		}

		public Map<String, String[]> getParameterMap() {
			return parameterMap;
		}

		public String getPreviousQueryString() {
			return previousQueryString;
		}

		public String getQueryString() {
			return queryString;
		}

		@Override
		public String toString() {
			return string;
		}

		private final Map<String, Object> attributes;
		private final DispatchTargets dispatchTargets;
		private final DispatcherType dispatcherType;
		private final Map<String, Object> overloadedAttributes = new HashMap<String, Object>();
		private final Map<String, String[]> parameterMap;
		private final String previousQueryString;
		private final String queryString;
		private final String string;

	}

	private static final String[] dispatcherAttributes = new String[] {
		RequestDispatcher.FORWARD_CONTEXT_PATH,
		RequestDispatcher.FORWARD_PATH_INFO,
		RequestDispatcher.FORWARD_QUERY_STRING,
		RequestDispatcher.FORWARD_REQUEST_URI,
		RequestDispatcher.FORWARD_SERVLET_PATH,
		RequestDispatcher.INCLUDE_CONTEXT_PATH,
		RequestDispatcher.INCLUDE_PATH_INFO,
		RequestDispatcher.INCLUDE_QUERY_STRING,
		RequestDispatcher.INCLUDE_REQUEST_URI,
		RequestDispatcher.INCLUDE_SERVLET_PATH
	};

	private final ThreadLocal<Deque<State>> state = new ThreadLocal<Deque<State>>() {
		@Override
		protected Deque<State> initialValue() {
			return new ArrayDeque<State>();
		}
	};

	private final HttpServletRequest request;

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

		Map<String, Object> attributes = new HashMap<String, Object>();

		for (Enumeration<String> names = request.getAttributeNames(); names.hasMoreElements();) {
			String name = names.nextElement();
			attributes.put(name, request.getAttribute(name));
		}

		this.getState().push(new State(dispatchTargets, dispatcherType, attributes, request.getParameterMap(), request.getQueryString()));
	}

	public void destroy() {
		state.remove();
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
		State current = currentState();
		State original = originalState();
		if (current.getDispatchTargets().getServletName() != null)
			return original.getDispatchTargets().getRequestURI();
		if (current.getDispatcherType() == DispatcherType.INCLUDE)
			return original.getDispatchTargets().getPathInfo();
		return current.getDispatchTargets().getPathInfo();
	}

	public DispatcherType getDispatcherType() {
		return currentState().getDispatcherType();
	}

	public String getParameter(String name) {
		String[] values = getParameterValues(name);
		if ((values == null) || (values.length == 0)) {
			return null;
		}
		return values[0];
	}

	public Map<String, String[]> getParameterMap() {
		return currentState().getParameterMap();
	}

	public Enumeration<String> getParameterNames() {
		return Collections.enumeration(getParameterMap().keySet());
	}

	public String[] getParameterValues(String name) {
		return getParameterMap().get(name);
	}

	@Override
	public String getQueryString() {
		return currentState().getQueryString();
	}

	public ServletContext getServletContext() {
		return currentState().getDispatchTargets().getServletRegistration().getServletContext();
	}

	public String getServletPath() {
		if (currentState().getDispatchTargets().getServletName() != null)
			return Const.BLANK;
		if (currentState().getDispatcherType() == DispatcherType.INCLUDE)
			return originalState().getDispatchTargets().getServletPath();
		return currentState().getDispatchTargets().getServletPath();
	}

	public String getContextPath() {
		if (currentState().getDispatcherType() == DispatcherType.INCLUDE)
			return originalState().getDispatchTargets().getContextController().getFullContextPath();
		return currentState().getDispatchTargets().getContextController().getFullContextPath();
	}

	@Override
	public String getRequestURI() {
		State current = currentState();
		DispatchTargets currentDispatchTargets = current.getDispatchTargets();
		if ((currentDispatchTargets.getServletName() != null) ||
			(current.getDispatcherType() == DispatcherType.INCLUDE)) {
			return originalState().getDispatchTargets().getRequestURI();
		}
		return currentDispatchTargets.getRequestURI();
	}

	public Object getAttribute(String attributeName) {
		State current = currentState();
		DispatchTargets currentDispatchTargets = current.getDispatchTargets();

		if (Arrays.binarySearch(dispatcherAttributes, attributeName) > -1) {
			if (currentDispatchTargets.getServletName() != null) {
				return null;
			}

			if (current.getOverloadedAttributes().containsKey(attributeName)) {
				return current.getOverloadedAttributes().get(attributeName);
			}
		}

		if (current.getDispatcherType() == DispatcherType.INCLUDE) {
			if (attributeName.equals(RequestDispatcher.INCLUDE_CONTEXT_PATH)) {
				return currentDispatchTargets.getContextController().getFullContextPath();
			} else if (attributeName.equals(RequestDispatcher.INCLUDE_PATH_INFO)) {
				return currentDispatchTargets.getPathInfo();
			} else if (attributeName.equals(RequestDispatcher.INCLUDE_QUERY_STRING)) {
				return currentDispatchTargets.getQueryString();
			} else if (attributeName.equals(RequestDispatcher.INCLUDE_REQUEST_URI)) {
				return currentDispatchTargets.getRequestURI();
			} else if (attributeName.equals(RequestDispatcher.INCLUDE_SERVLET_PATH)) {
				return currentDispatchTargets.getServletPath();
			}
		}
		if (current.getDispatcherType() == DispatcherType.FORWARD) {
			State original = originalState();
			DispatchTargets originalDispatchTargets = original.getDispatchTargets();

			if (attributeName.equals(RequestDispatcher.FORWARD_CONTEXT_PATH)) {
				return originalDispatchTargets.getContextController().getFullContextPath();
			} else if (attributeName.equals(RequestDispatcher.FORWARD_PATH_INFO)) {
				return originalDispatchTargets.getPathInfo();
			} else if (attributeName.equals(RequestDispatcher.FORWARD_QUERY_STRING)) {
				return original.getQueryString();
			} else if (attributeName.equals(RequestDispatcher.FORWARD_REQUEST_URI)) {
				return originalDispatchTargets.getRequestURI();
			} else if (attributeName.equals(RequestDispatcher.FORWARD_SERVLET_PATH)) {
				return originalDispatchTargets.getServletPath();
			}
		}

		return current.getAttributes().get(attributeName);
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		State current = currentState();

		Set<String> names = new HashSet<String>();
		names.addAll(current.getAttributes().keySet());
		names.addAll(current.getOverloadedAttributes().keySet());

		return Collections.enumeration(names);
	}

	public RequestDispatcher getRequestDispatcher(String path) {
		State current = currentState();
		ContextController contextController = current.getDispatchTargets().getContextController();

		// support relative paths
		if (!path.startsWith(Const.SLASH)) {
			path = current.getDispatchTargets().getServletPath() + Const.SLASH + path;
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

	public HttpSession getSession() {
		HttpSession session = request.getSession();
		if (session != null) {
			DispatchTargets dispatchTargets = currentState().getDispatchTargets();
			return dispatchTargets.getContextController().getSessionAdaptor(
				session, dispatchTargets.getServletRegistration().getT().getServletConfig().getServletContext());
		}

		return null;
	}

	public HttpSession getSession(boolean create) {
		HttpSession session = request.getSession(create);
		if (session != null) {
			DispatchTargets dispatchTargets = currentState().getDispatchTargets();
			return dispatchTargets.getContextController().getSessionAdaptor(
				session, dispatchTargets.getServletRegistration().getT().getServletConfig().getServletContext());
		}

		return null;
	}

	public synchronized void pop() {
		getState().pop();
	}

	public synchronized void push(DispatchTargets dispatchTargets, DispatcherType dispatcherType) {
		State previous = getState().peek();
		getState().push(new State(dispatchTargets, dispatcherType, previous.getAttributes(), previous.getParameterMap(), previous.getQueryString()));
	}

	public void removeAttribute(String name) {
		State current = getState().peek();

		if ((Arrays.binarySearch(dispatcherAttributes, name) > -1) &&
			current.getOverloadedAttributes().containsKey(name)) {
			current.getOverloadedAttributes().remove(name);
		}
		else {
			current.getAttributes().remove(name);
		}

		EventListeners eventListeners = current.getDispatchTargets().getContextController().getEventListeners();

		List<ServletRequestAttributeListener> listeners = eventListeners.get(
			ServletRequestAttributeListener.class);

		if (listeners.isEmpty()) {
			return;
		}

		ServletRequestAttributeEvent servletRequestAttributeEvent =
			new ServletRequestAttributeEvent(
				current.getDispatchTargets().getServletRegistration().getServletContext(), this, name, null);

		for (ServletRequestAttributeListener servletRequestAttributeListener : listeners) {
			servletRequestAttributeListener.attributeRemoved(
				servletRequestAttributeEvent);
		}
	}

	public void setAttribute(String name, Object value) {
		State current = getState().peek();

		boolean added = !current.getAttributes().containsKey(name);

		if (Arrays.binarySearch(dispatcherAttributes, name) > -1) {
			added = !current.getOverloadedAttributes().containsKey(name);

			current.getOverloadedAttributes().put(name, value);
		}
		else {
			current.getAttributes().put(name, value);
		}

		EventListeners eventListeners = current.getDispatchTargets().getContextController().getEventListeners();

		List<ServletRequestAttributeListener> listeners = eventListeners.get(
			ServletRequestAttributeListener.class);

		if (listeners.isEmpty()) {
			return;
		}

		ServletRequestAttributeEvent servletRequestAttributeEvent =
			new ServletRequestAttributeEvent(
				current.getDispatchTargets().getServletRegistration().getServletContext(), this, name, value);

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

	private State currentState() {
		return getState().peek();
	}

	private State originalState() {
		return getState().peekLast();
	}

	private Deque<State> getState() {
		return state.get();
	}

}