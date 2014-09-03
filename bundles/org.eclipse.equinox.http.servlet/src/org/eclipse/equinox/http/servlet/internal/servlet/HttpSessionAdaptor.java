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

import java.util.Enumeration;
import java.util.List;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.http.*;
import org.eclipse.equinox.http.servlet.internal.util.EventListeners;

// This class adapts HttpSessions in order to return the right ServletContext
public class HttpSessionAdaptor implements HttpSession {

	private EventListeners eventListeners;
	private HttpSession session;
	private Servlet servlet;

	public HttpSessionAdaptor(
		HttpSession session, Servlet servlet, EventListeners eventListeners) {

		this.session = session;
		this.servlet = servlet;
		this.eventListeners = eventListeners;
	}

	public ServletContext getServletContext() {
		return servlet.getServletConfig().getServletContext();
	}

	public Object getAttribute(String arg0) {
		return session.getAttribute(arg0);
	}

	public Enumeration<String> getAttributeNames() {
		return session.getAttributeNames();
	}

	public long getCreationTime() {
		return session.getCreationTime();
	}

	public String getId() {
		return session.getId();
	}

	public long getLastAccessedTime() {
		return session.getLastAccessedTime();
	}

	public int getMaxInactiveInterval() {
		return session.getMaxInactiveInterval();
	}

	/**@deprecated*/
	public javax.servlet.http.HttpSessionContext getSessionContext() {
		return session.getSessionContext();
	}

	/**@deprecated*/
	public Object getValue(String arg0) {
		return session.getValue(arg0);
	}

	/**@deprecated*/
	public String[] getValueNames() {
		return session.getValueNames();
	}

	public void invalidate() {
		session.invalidate();
	}

	public boolean isNew() {
		return session.isNew();
	}

	/**@deprecated*/
	public void putValue(String arg0, Object arg1) {
		session.putValue(arg0, arg1);
	}

	public void removeAttribute(String arg0) {
		session.removeAttribute(arg0);

		List<HttpSessionAttributeListener> listeners = eventListeners.get(
			HttpSessionAttributeListener.class);

		if (listeners.isEmpty()) {
			return;
		}

		HttpSessionBindingEvent httpSessionBindingEvent =
			new HttpSessionBindingEvent(session, arg0);

		for (HttpSessionAttributeListener httpSessionAttributeListener : listeners) {
			httpSessionAttributeListener.attributeRemoved(
				httpSessionBindingEvent);
		}
	}

	/**@deprecated*/
	public void removeValue(String arg0) {
		session.removeValue(arg0);

		List<HttpSessionAttributeListener> listeners = eventListeners.get(
			HttpSessionAttributeListener.class);

		if (listeners.isEmpty()) {
			return;
		}

		HttpSessionBindingEvent httpSessionBindingEvent =
			new HttpSessionBindingEvent(session, arg0);

		for (HttpSessionAttributeListener httpSessionAttributeListener : listeners) {
			httpSessionAttributeListener.attributeRemoved(
				httpSessionBindingEvent);
		}
	}

	public void setAttribute(String arg0, Object arg1) {
		boolean added = (session.getAttribute(arg0) == null);
		session.setAttribute(arg0, arg1);

		List<HttpSessionAttributeListener> listeners = eventListeners.get(
			HttpSessionAttributeListener.class);

		if (listeners.isEmpty()) {
			return;
		}

		HttpSessionBindingEvent httpSessionBindingEvent =
			new HttpSessionBindingEvent(session, arg0, arg1);

		for (HttpSessionAttributeListener httpSessionAttributeListener : listeners) {
			if (added) {
				httpSessionAttributeListener.attributeAdded(
					httpSessionBindingEvent);
			}
			else {
				httpSessionAttributeListener.attributeReplaced(
					httpSessionBindingEvent);
			}
		}
	}

	public void setMaxInactiveInterval(int arg0) {
		session.setMaxInactiveInterval(arg0);
	}

}
