/*******************************************************************************
 * Copyright (c) 2005, 2019 Cognos Incorporated, IBM Corporation and others.
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
 *     Raymond Augé <raymond.auge@liferay.com> - Bug 436698
 *     Arnaud Mergey <a_mergey@yahoo.fr> - Bug 497510
 *******************************************************************************/
package org.eclipse.equinox.http.servlet.internal.servlet;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.servlet.*;
import javax.servlet.http.*;
import org.eclipse.equinox.http.servlet.internal.context.ContextController;
import org.eclipse.equinox.http.servlet.internal.context.DispatchTargets;
import org.eclipse.equinox.http.servlet.internal.registration.EndpointRegistration;
import org.eclipse.equinox.http.servlet.internal.registration.ServletRegistration;
import org.eclipse.equinox.http.servlet.internal.util.Const;
import org.eclipse.equinox.http.servlet.internal.util.EventListeners;
import org.osgi.service.http.HttpContext;

public class HttpServletRequestWrapperImpl extends HttpServletRequestWrapper {

	private final Deque<DispatchTargets> dispatchTargets = new LinkedList<DispatchTargets>();
	private final HttpServletRequest request;
	private Map<String, Part> parts;
	private final Lock lock = new ReentrantLock();

	private static final Set<String> dispatcherAttributes =	new HashSet<String>();

	static {
		dispatcherAttributes.add(RequestDispatcher.ERROR_EXCEPTION);
		dispatcherAttributes.add(RequestDispatcher.ERROR_EXCEPTION_TYPE);
		dispatcherAttributes.add(RequestDispatcher.ERROR_MESSAGE);
		dispatcherAttributes.add(RequestDispatcher.ERROR_REQUEST_URI);
		dispatcherAttributes.add(RequestDispatcher.ERROR_SERVLET_NAME);
		dispatcherAttributes.add(RequestDispatcher.ERROR_STATUS_CODE);
		dispatcherAttributes.add(RequestDispatcher.FORWARD_CONTEXT_PATH);
		dispatcherAttributes.add(RequestDispatcher.FORWARD_PATH_INFO);
		dispatcherAttributes.add(RequestDispatcher.FORWARD_QUERY_STRING);
		dispatcherAttributes.add(RequestDispatcher.FORWARD_REQUEST_URI);
		dispatcherAttributes.add(RequestDispatcher.FORWARD_SERVLET_PATH);
		dispatcherAttributes.add(RequestDispatcher.INCLUDE_CONTEXT_PATH);
		dispatcherAttributes.add(RequestDispatcher.INCLUDE_PATH_INFO);
		dispatcherAttributes.add(RequestDispatcher.INCLUDE_QUERY_STRING);
		dispatcherAttributes.add(RequestDispatcher.INCLUDE_REQUEST_URI);
		dispatcherAttributes.add(RequestDispatcher.INCLUDE_SERVLET_PATH);
	}

	private static final Object NULL_PLACEHOLDER = new Object();

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

	public HttpServletRequestWrapperImpl(HttpServletRequest request) {
		super(request);
		this.request = request;
	}

	@Override
	public String changeSessionId() {
		HttpSessionAdaptor httpSessionAdaptor = (HttpSessionAdaptor)getSession(false);

		if (httpSessionAdaptor == null) {
			throw new IllegalStateException("No session"); //$NON-NLS-1$
		}

		DispatchTargets currentDispatchTarget = dispatchTargets.peek();

		String oldSessionId = httpSessionAdaptor.getId();
		String newSessionId = super.changeSessionId();

		currentDispatchTarget.getContextController().removeActiveSession(oldSessionId);
		currentDispatchTarget.getContextController().addSessionAdaptor(newSessionId, httpSessionAdaptor);

		return newSessionId;
	}

	public String getAuthType() {
		String authType = (String) this.getAttribute(HttpContext.AUTHENTICATION_TYPE);
		if (authType != null)
			return authType;

		return request.getAuthType();
	}

	public String getRemoteUser() {
		String remoteUser = (String) this.getAttribute(HttpContext.REMOTE_USER);
		if (remoteUser != null)
			return remoteUser;

		return request.getRemoteUser();
	}

	public String getPathInfo() {
		DispatchTargets currentDispatchTargets = dispatchTargets.peek();

		if ((currentDispatchTargets.getServletName() != null) ||
			(currentDispatchTargets.getDispatcherType() == DispatcherType.INCLUDE)) {
			return this.dispatchTargets.getLast().getPathInfo();
		}
		return currentDispatchTargets.getPathInfo();
	}

	public DispatcherType getDispatcherType() {
		return dispatchTargets.peek().getDispatcherType();
	}

	public String getParameter(String name) {
		String[] values = getParameterValues(name);
		if ((values == null) || (values.length == 0)) {
			return null;
		}
		return values[0];
	}

	public Map<String, String[]> getParameterMap() {
		return dispatchTargets.peek().getParameterMap();
	}

	public Enumeration<String> getParameterNames() {
		return Collections.enumeration(getParameterMap().keySet());
	}

	public String[] getParameterValues(String name) {
		return getParameterMap().get(name);
	}

	@Override
	public String getQueryString() {
		DispatchTargets currentDispatchTargets = dispatchTargets.peek();

		if ((currentDispatchTargets.getServletName() != null) ||
			(currentDispatchTargets.getDispatcherType() == DispatcherType.INCLUDE)) {
			return request.getQueryString();
		}
		return currentDispatchTargets.getQueryString();
	}

	@Override
	public String getRequestURI() {
		DispatchTargets currentDispatchTargets = dispatchTargets.peek();

		if ((currentDispatchTargets.getServletName() != null) ||
			(currentDispatchTargets.getDispatcherType() == DispatcherType.INCLUDE)) {
			return request.getRequestURI();
		}
		return currentDispatchTargets.getRequestURI();
	}

	public ServletContext getServletContext() {
		return dispatchTargets.peek().getServletRegistration().getServletContext();
	}

	public String getServletPath() {
		DispatchTargets currentDispatchTargets = dispatchTargets.peek();

		if ((currentDispatchTargets.getServletName() != null) ||
			(currentDispatchTargets.getDispatcherType() == DispatcherType.INCLUDE)) {
			return this.dispatchTargets.getLast().getServletPath();
		}
		if (currentDispatchTargets.getServletPath().equals(Const.SLASH)) {
			return Const.BLANK;
		}
		return currentDispatchTargets.getServletPath();
	}

	public String getContextPath() {
		return dispatchTargets.peek().getContextController().getFullContextPath();
	}

	public Object getAttribute(String attributeName) {
		DispatchTargets current = dispatchTargets.peek();
		DispatcherType dispatcherType = current.getDispatcherType();

		if ((dispatcherType == DispatcherType.ASYNC) ||
			(dispatcherType == DispatcherType.REQUEST) ||
			!attributeName.startsWith("javax.servlet.")) { //$NON-NLS-1$

			return request.getAttribute(attributeName);
		}

		boolean hasServletName = (current.getServletName() != null);
		Map<String, Object> specialOverides = current.getSpecialOverides();

		if (dispatcherType == DispatcherType.ERROR) {
			if (dispatcherAttributes.contains(attributeName) &&
				!attributeName.startsWith("javax.servlet.error.")) { //$NON-NLS-1$

				return null;
			}
		}
		else if (dispatcherType == DispatcherType.INCLUDE) {
			if (hasServletName && attributeName.startsWith("javax.servlet.include")) { //$NON-NLS-1$
				return null;
			}

			if (dispatcherAttributes.contains(attributeName)) {
				Object specialOveride = specialOverides.get(attributeName);

				if (NULL_PLACEHOLDER.equals(specialOveride)) {
					return null;
				}
				else if (specialOveride != null) {
					return specialOveride;
				}

				Object attributeValue = super.getAttribute(attributeName);

				if (attributeValue != null) {
					return attributeValue;
				}
			}

			if (attributeName.equals(RequestDispatcher.INCLUDE_CONTEXT_PATH)) {
				return current.getContextController().getFullContextPath();
			}
			else if (attributeName.equals(RequestDispatcher.INCLUDE_PATH_INFO)) {
				return current.getPathInfo();
			}
			else if (attributeName.equals(RequestDispatcher.INCLUDE_QUERY_STRING)) {
				return current.getQueryString();
			}
			else if (attributeName.equals(RequestDispatcher.INCLUDE_REQUEST_URI)) {
				return current.getRequestURI();
			}
			else if (attributeName.equals(RequestDispatcher.INCLUDE_SERVLET_PATH)) {
				return current.getServletPath();
			}

			if (dispatcherAttributes.contains(attributeName)) {
				return null;
			}
		}
		else if (dispatcherType == DispatcherType.FORWARD) {
			if (hasServletName && attributeName.startsWith("javax.servlet.forward")) { //$NON-NLS-1$
				return null;
			}

			if (dispatcherAttributes.contains(attributeName)) {
				Object specialOveride = specialOverides.get(attributeName);

				if (NULL_PLACEHOLDER.equals(specialOveride)) {
					return null;
				}
				else if (specialOveride != null) {
					return specialOveride;
				}
			}

			DispatchTargets original = dispatchTargets.getLast();

			if (attributeName.equals(RequestDispatcher.FORWARD_CONTEXT_PATH)) {
				return original.getContextController().getFullContextPath();
			}
			else if (attributeName.equals(RequestDispatcher.FORWARD_PATH_INFO)) {
				return original.getPathInfo();
			}
			else if (attributeName.equals(RequestDispatcher.FORWARD_QUERY_STRING)) {
				return original.getQueryString();
			}
			else if (attributeName.equals(RequestDispatcher.FORWARD_REQUEST_URI)) {
				return original.getRequestURI();
			}
			else if (attributeName.equals(RequestDispatcher.FORWARD_SERVLET_PATH)) {
				return original.getServletPath();
			}

			if (dispatcherAttributes.contains(attributeName)) {
				return null;
			}
		}

		return request.getAttribute(attributeName);
	}

	public RequestDispatcher getRequestDispatcher(String path) {
		DispatchTargets currentDispatchTarget = dispatchTargets.peek();

		ContextController contextController =
			currentDispatchTarget.getContextController();

		// support relative paths
		if (!path.startsWith(Const.SLASH)) {
			path = currentDispatchTarget.getServletPath() + Const.SLASH + path;
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

	public static String getDispatchRequestURI(HttpServletRequest req) {
		if (req.getDispatcherType() == DispatcherType.INCLUDE)
			return (String) req.getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI);

		return req.getRequestURI();
	}

	public HttpSession getSession() {
		return getSession(true);
	}

	public HttpSession getSession(boolean create) {
		HttpSession session = request.getSession(create);
		if (session != null) {
			DispatchTargets currentDispatchTarget = dispatchTargets.peek();

			return currentDispatchTarget.getContextController().getSessionAdaptor(
				session, currentDispatchTarget.getServletRegistration().getT().getServletConfig().getServletContext());
		}

		return null;
	}

	public synchronized void pop() {
		if (dispatchTargets.size() > 1) {
			this.dispatchTargets.pop();
		}
	}

	public synchronized void push(DispatchTargets toPush) {
		toPush.addRequestParameters(request);
		this.dispatchTargets.push(toPush);
	}

	public void removeAttribute(String name) {
		if (dispatcherAttributes.contains(name)) {
			DispatchTargets current = dispatchTargets.peek();

			current.getSpecialOverides().remove(name);
		}

		request.removeAttribute(name);

		DispatchTargets currentDispatchTarget = dispatchTargets.peek();

		EventListeners eventListeners = currentDispatchTarget.getContextController().getEventListeners();

		List<ServletRequestAttributeListener> listeners = eventListeners.get(
			ServletRequestAttributeListener.class);

		if (listeners.isEmpty()) {
			return;
		}

		ServletRequestAttributeEvent servletRequestAttributeEvent =
			new ServletRequestAttributeEvent(
				currentDispatchTarget.getServletRegistration().getServletContext(), this, name, null);

		for (ServletRequestAttributeListener servletRequestAttributeListener : listeners) {
			servletRequestAttributeListener.attributeRemoved(
				servletRequestAttributeEvent);
		}
	}

	public void setAttribute(String name, Object value) {
		boolean added = (request.getAttribute(name) == null);

		if (dispatcherAttributes.contains(name)) {
			DispatchTargets current = dispatchTargets.peek();

			if (value == null) {
				current.getSpecialOverides().put(name, NULL_PLACEHOLDER);
			}
			else {
				current.getSpecialOverides().put(name, value);
			}
		}

		request.setAttribute(name, value);

		DispatchTargets currentDispatchTarget = dispatchTargets.peek();

		EventListeners eventListeners = currentDispatchTarget.getContextController().getEventListeners();

		List<ServletRequestAttributeListener> listeners = eventListeners.get(
			ServletRequestAttributeListener.class);

		if (listeners.isEmpty()) {
			return;
		}

		ServletRequestAttributeEvent servletRequestAttributeEvent =
			new ServletRequestAttributeEvent(
				currentDispatchTarget.getServletRegistration().getServletContext(), this, name, value);

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

	@Override
	public Part getPart(String name) throws IOException, ServletException {
		return getParts0().get(name);
	}

	@Override
	public Collection<Part> getParts() throws IOException, ServletException {
		return new ArrayList<Part>(getParts0().values());
	}

	public AsyncContext startAsync() throws IllegalStateException {
		EndpointRegistration<?> endpointRegistration = dispatchTargets.peek().getServletRegistration();

		if (endpointRegistration instanceof ServletRegistration) {
			ServletRegistration servletRegistration = (ServletRegistration)endpointRegistration;

			if (servletRegistration.getD().asyncSupported) {
				return request.startAsync();
			}
		}

		throw new IllegalStateException("Async not supported by " + endpointRegistration); //$NON-NLS-1$
	}

	private Map<String, Part> getParts0() throws IOException, ServletException {
		org.eclipse.equinox.http.servlet.internal.registration.ServletRegistration servletRegistration = getServletRegistration();

		if (servletRegistration == null) {
			throw new ServletException("Not a servlet request!"); //$NON-NLS-1$
		}

		lock.lock();

		try {
			if (parts != null) {
				return parts;
			}

			return parts = servletRegistration.parseRequest(this);
		}
		finally {
			lock.unlock();
		}
	}

	private org.eclipse.equinox.http.servlet.internal.registration.ServletRegistration getServletRegistration() {
		EndpointRegistration<?> servletRegistration = dispatchTargets.peek().getServletRegistration();

		if (servletRegistration instanceof org.eclipse.equinox.http.servlet.internal.registration.ServletRegistration) {
			return (org.eclipse.equinox.http.servlet.internal.registration.ServletRegistration)servletRegistration;
		}

		return null;
	}

}
