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
import javax.servlet.ServletContext;
import javax.servlet.http.*;
import org.eclipse.equinox.http.servlet.internal.context.ContextController;

// This class adapts HttpSessions in order to return the right ServletContext and attributes
public class HttpSessionAdaptor implements HttpSession {

	private final ContextController controller;
	private final HttpSession session;
	private final ServletContext servletContext;
	private final String attributePrefix;

	public HttpSessionAdaptor(
		HttpSession session, ServletContext servletContext, ContextController controller) {

		this.session = session;
		this.servletContext = servletContext;
		this.controller = controller;
		this.attributePrefix = "equinox.http." + controller.getContextName(); //$NON-NLS-1$
	}

	public ServletContext getServletContext() {
		return servletContext;
	}

	public Object getAttribute(String arg0) {
		return session.getAttribute(attributePrefix + arg0);
	}

	public Enumeration<String> getAttributeNames() {
		return Collections.enumeration(getAttributeNames0());
	}

	private Collection<String> getAttributeNames0() {
		Collection<String> result = new ArrayList<String>();
		Enumeration<String> containerSessionAttributes = session.getAttributeNames();
		while(containerSessionAttributes.hasMoreElements()) {
			String attribute = containerSessionAttributes.nextElement();
			if (attribute.startsWith(attributePrefix)) {
				result.add(attribute.substring(attributePrefix.length()));
			}
		}
		return result;
	}

	/**@deprecated*/
	public Object getValue(String arg0) {
		return getAttribute(arg0);
	}

	/**@deprecated*/
	public String[] getValueNames() {
		Collection<String> result = getAttributeNames0();
		return result.toArray(new String[result.size()]);
	}

	public void invalidate() {
		for (String attribute : getAttributeNames0()) {
			removeAttribute(attribute);
		}
	}

	/**@deprecated*/
	public void putValue(String arg0, Object arg1) {
		setAttribute(arg0, arg1);
	}

	public void removeAttribute(String arg0) {
		session.removeAttribute(attributePrefix + arg0);

		List<HttpSessionAttributeListener> listeners = controller.getEventListeners().get(
			HttpSessionAttributeListener.class);

		if (listeners.isEmpty()) {
			return;
		}

		HttpSessionBindingEvent httpSessionBindingEvent =
			new HttpSessionBindingEvent(this, arg0);

		for (HttpSessionAttributeListener httpSessionAttributeListener : listeners) {
			httpSessionAttributeListener.attributeRemoved(
				httpSessionBindingEvent);
		}
	}

	/**@deprecated*/
	public void removeValue(String arg0) {
		removeAttribute(arg0);
	}

	public void setAttribute(String arg0, Object arg1) {
		boolean added = (session.getAttribute(attributePrefix + arg0) == null);
		session.setAttribute(attributePrefix + arg0, arg1);

		List<HttpSessionAttributeListener> listeners = controller.getEventListeners().get(
			HttpSessionAttributeListener.class);

		if (listeners.isEmpty()) {
			return;
		}

		HttpSessionBindingEvent httpSessionBindingEvent =
			new HttpSessionBindingEvent(this, arg0, arg1);

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
		// Not sure this can be done per context helper
		session.setMaxInactiveInterval(arg0);
	}

	public long getCreationTime() {
		// Not sure this can be done per context helper
		return session.getCreationTime();
	}

	public String getId() {
		// Not sure this can be done per context helper
		return session.getId();
	}

	public long getLastAccessedTime() {
		// Not sure this can be done per context helper
		return session.getLastAccessedTime();
	}

	public int getMaxInactiveInterval() {
		// Not sure this can be done per context helper
		return session.getMaxInactiveInterval();
	}

	/**@deprecated*/
	public javax.servlet.http.HttpSessionContext getSessionContext() {
		// Not sure this can be done per context helper and I think null is returned anyway
		return session.getSessionContext();
	}

	public boolean isNew() {
		// Not sure this can be done per context helper
		return session.isNew();
	}
}
