/*******************************************************************************
 * Copyright (c) 2015 Raymond Augé and others.
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
 ******************************************************************************/

package org.eclipse.equinox.http.servlet.internal.registration;

import java.lang.reflect.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.eclipse.equinox.http.servlet.internal.context.ContextController;
import org.eclipse.equinox.http.servlet.internal.context.ServiceHolder;
import org.eclipse.equinox.http.servlet.internal.servlet.HttpSessionAdaptor;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.http.runtime.dto.ListenerDTO;

/**
 * @author Raymond Augé
 */
public class ListenerRegistration extends Registration<EventListener, ListenerDTO> {

	private final ServiceHolder<EventListener> listenerHolder;
	private final List<Class<? extends EventListener>> classes;
	private final EventListener proxy;
	private final ServletContext servletContext;
	private final ContextController contextController;
	private final ClassLoader classLoader;

	public ListenerRegistration(
		ServiceHolder<EventListener> listenerHolder, List<Class<? extends EventListener>> classes,
		ListenerDTO listenerDTO, ServletContext servletContext,
		ContextController contextController) {

		super(listenerHolder.get(), listenerDTO);
		this.listenerHolder = listenerHolder;
		this.classes = classes;
		this.servletContext = servletContext;
		this.contextController = contextController;

		classLoader = listenerHolder.getBundle().adapt(BundleWiring.class).getClassLoader();

		createContextAttributes();

		proxy = (EventListener)Proxy.newProxyInstance(
			getClass().getClassLoader(), classes.toArray(new Class[0]),
			new EventListenerInvocationHandler());
	}

	@Override
	public synchronized void destroy() {
		ClassLoader original = Thread.currentThread().getContextClassLoader();

		try {
			Thread.currentThread().setContextClassLoader(classLoader);

			contextController.getListenerRegistrations().remove(this);
			contextController.getEventListeners().remove(classes, this);
			contextController.ungetServletContextHelper(listenerHolder.getBundle());

			super.destroy();

			if (classes.contains(HttpSessionBindingListener.class) ||
				classes.contains(HttpSessionAttributeListener.class) ||
				classes.contains(HttpSessionListener.class)) {

				Map<String, HttpSessionAdaptor> activeSessions =
					contextController.getActiveSessions();

				for (HttpSessionAdaptor adaptor : activeSessions.values()) {
					adaptor.invokeSessionListeners(classes, super.getT());
				}
			}

			if (classes.contains(ServletContextListener.class)) {
				ServletContextListener servletContextListener =
					(ServletContextListener)super.getT();

				servletContextListener.contextDestroyed(
					new ServletContextEvent(servletContext));
			}
		}
		finally {
			destroyContextAttributes();
			Thread.currentThread().setContextClassLoader(original);
			listenerHolder.release();
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ListenerRegistration)) {
			return false;
		}

		ListenerRegistration listenerRegistration = (ListenerRegistration)obj;

		return listenerRegistration.getT().equals(super.getT());
	}

	@Override
	public int hashCode() {
		return Long.valueOf(getD().serviceId).hashCode();
	}

	public ServletContext getServletContext() {
		return servletContext;
	}

	@Override
	public EventListener getT() {
		return proxy;
	}

	private void createContextAttributes() {
		contextController.createContextAttributes();
	}

	private void destroyContextAttributes() {
		contextController.destroyContextAttributes();
	}

	ClassLoader getClassLoader() {
		return classLoader;
	}

	EventListener getDelegate() {
		return super.getT();
	}

	private class EventListenerInvocationHandler implements InvocationHandler {

		public EventListenerInvocationHandler() {
		}

		@Override
		public Object invoke(Object theProxy, Method method, Object[] args)
			throws Throwable {

			Thread thread = Thread.currentThread();
			ClassLoader original = thread.getContextClassLoader();

			try {
				thread.setContextClassLoader(getClassLoader());
				try {
					return method.invoke(getDelegate(), args);
				} catch (InvocationTargetException e) {
					throw e.getCause();
				}
			}
			finally {
				thread.setContextClassLoader(original);
			}
		}
	}

}
