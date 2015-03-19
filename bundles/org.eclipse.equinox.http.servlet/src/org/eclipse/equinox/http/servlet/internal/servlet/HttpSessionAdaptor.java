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

import java.io.Serializable;
import java.util.*;
import javax.servlet.ServletContext;
import javax.servlet.http.*;
import org.eclipse.equinox.http.servlet.internal.context.ContextController;

// This class adapts HttpSessions in order to return the right ServletContext and attributes
public class HttpSessionAdaptor implements HttpSession {
	static class ParentSessionListener implements HttpSessionBindingListener, Serializable {
		private static final long serialVersionUID = 4626167646903550760L;
		private static final String PARENT_SESSION_LISTENER_KEY = "org.eclipse.equinox.http.parent.session.listener"; //$NON-NLS-1$
		final Set<HttpSessionAdaptor> innerSessions = new HashSet<HttpSessionAdaptor>();
		@Override
		public void valueBound(HttpSessionBindingEvent event) {
			// do nothing
		}

		@Override
		public void valueUnbound(HttpSessionBindingEvent event) {
			// Here we assume the unbound event is signifying the session is being invalidated.
			// Must invalidate the inner sessions
			Set<HttpSessionAdaptor> innerSessionsToInvalidate;
			synchronized (innerSessions) {
				// copy the sessions to invalidate and clear the set
				innerSessionsToInvalidate = new HashSet<HttpSessionAdaptor>(innerSessions);
				innerSessions.clear();
			}
			for (HttpSessionAdaptor innerSession : innerSessionsToInvalidate) {
				innerSession.invalidate();
			}
		}

		static void addHttpSessionAdaptor(HttpSessionAdaptor innerSession) {
			ParentSessionListener parentListener;
			// need to have a global lock here because we must ensure that this is added only once
			synchronized (ParentSessionListener.class) {
				parentListener = (ParentSessionListener) innerSession.session.getAttribute(PARENT_SESSION_LISTENER_KEY);
				if (parentListener == null) {
					parentListener = new ParentSessionListener();
					innerSession.session.setAttribute(PARENT_SESSION_LISTENER_KEY, parentListener);
				}
			}
			synchronized (parentListener.innerSessions) {
				parentListener.innerSessions.add(innerSession);
			}
		}

		static void removeHttpSessionAdaptor(HttpSessionAdaptor innerSession) {
			ParentSessionListener parentListener;
			// need to have a global lock here because we must ensure that this is added only once
			synchronized (ParentSessionListener.class) {
				parentListener = (ParentSessionListener) innerSession.session.getAttribute(PARENT_SESSION_LISTENER_KEY);
			}
			if (parentListener != null) {
				synchronized (parentListener.innerSessions) {
					parentListener.innerSessions.remove(innerSession);
				}
			}
		}
	}

	class HttpSessionAttributeWrapper implements HttpSessionBindingListener, HttpSessionActivationListener {
		final String name;
		final Object value;

		public HttpSessionAttributeWrapper(String name, Object value) {
			this.name = name;
			this.value = value;
		}
		@Override
		public void valueBound(HttpSessionBindingEvent event) {
			if (value instanceof HttpSessionBindingListener) {
				((HttpSessionBindingListener) value).valueBound(new HttpSessionBindingEvent(HttpSessionAdaptor.this, name, value));
			}
		}

		@Override
		public void valueUnbound(HttpSessionBindingEvent event) {
			if (value instanceof HttpSessionBindingListener) {
				((HttpSessionBindingListener) value).valueUnbound(new HttpSessionBindingEvent(HttpSessionAdaptor.this, name, value));
			}
		}

		@Override
		public void sessionWillPassivate(HttpSessionEvent se) {
			if (value instanceof HttpSessionActivationListener) {
				((HttpSessionActivationListener) value).sessionWillPassivate(new HttpSessionEvent(HttpSessionAdaptor.this));
			}
		}

		@Override
		public void sessionDidActivate(HttpSessionEvent se) {
			if (value instanceof HttpSessionActivationListener) {
				((HttpSessionActivationListener) value).sessionDidActivate(new HttpSessionEvent(HttpSessionAdaptor.this));
			}
		}
	}

	private final ContextController controller;
	private final HttpSession session;
	private final ServletContext servletContext;
	private final String attributePrefix;

	static public HttpSessionAdaptor createHttpSessionAdaptor(
		HttpSession session, ServletContext servletContext, ContextController controller) {
		HttpSessionAdaptor sessionAdaptor = new HttpSessionAdaptor(session, servletContext, controller);
		ParentSessionListener.addHttpSessionAdaptor(sessionAdaptor);
		return sessionAdaptor;
	}

	private HttpSessionAdaptor(
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
		Object result = session.getAttribute(attributePrefix + arg0);
		if (result instanceof HttpSessionAttributeWrapper) {
			result = ((HttpSessionAttributeWrapper) result).value;
		}
		return result;
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
		for(HttpSessionListener listener : controller.getEventListeners().get(HttpSessionListener.class)) {
			listener.sessionDestroyed(new HttpSessionEvent(this));
		}
		for (String attribute : getAttributeNames0()) {
			removeAttribute(attribute);
		}
		ParentSessionListener.removeHttpSessionAdaptor(this);
		controller.removeActiveSession(session);
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

	public void setAttribute(String name, Object value) {
		boolean added = (session.getAttribute(attributePrefix + name) == null);
		if (value instanceof HttpSessionBindingListener || value instanceof HttpSessionActivationListener) {
			value = new HttpSessionAttributeWrapper(name, value);
		}
		session.setAttribute(attributePrefix + name, value);

		List<HttpSessionAttributeListener> listeners = controller.getEventListeners().get(
			HttpSessionAttributeListener.class);

		if (listeners.isEmpty()) {
			return;
		}

		HttpSessionBindingEvent httpSessionBindingEvent =
			new HttpSessionBindingEvent(this, name, value);

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
