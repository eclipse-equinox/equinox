/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Raymond Augé <raymond.auge@liferay.com> - Bug 436698
 *******************************************************************************/
package org.eclipse.equinox.http.servlet.tests.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

public class TestServletPrototype extends HttpServlet {

	private static final long serialVersionUID = 1L;

	final BundleContext context;
	final AtomicReference<String> lastGetName = new AtomicReference<>();
	final AtomicReference<String> lastUngetName = new AtomicReference<>();
	final ConcurrentMap<String, Factory> factories = new ConcurrentHashMap<>();

	public TestServletPrototype(BundleContext context) {
		this.context = context;
	}

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
		PrintWriter writer = response.getWriter();

		try {
			handleDoGet(request, response, writer);
		} finally {
			// writer.close();
		}
	}

	class Factory implements PrototypeServiceFactory<Servlet> {
		final AtomicReference<ServiceRegistration<Servlet>> registration = new AtomicReference<>();

		@Override
		public Servlet getService(Bundle bundle, ServiceRegistration<Servlet> registration) {
			String name = (String) registration.getReference()
					.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME);
			lastGetName.set(name);
			return new TestWBServlet();
		}

		@Override
		public void ungetService(Bundle bundle, ServiceRegistration<Servlet> registration, Servlet service) {
			String name = (String) registration.getReference()
					.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME);
			lastUngetName.set(name);
		}

		void setRegistrationProperties(Dictionary<String, Object> serviceProps) {
			ServiceRegistration<Servlet> serviceRegistration = this.registration.get();
			if (serviceRegistration == null) {
				this.registration.set(context.registerService(Servlet.class, this, serviceProps));
			} else {
				for (String key : serviceRegistration.getReference().getPropertyKeys()) {
					if (serviceProps.get(key) == null) {
						serviceProps.put(key, serviceRegistration.getReference().getProperty(key));
					}
				}
				// Update the registration props
				serviceRegistration.setProperties(serviceProps);
			}
		}

		public void unregister() {
			this.registration.getAndSet(null).unregister();
		}

	}

	protected void handleDoGet(HttpServletRequest request, HttpServletResponse response, PrintWriter writer)
			throws IOException {
		String pathInfo = request.getPathInfo();
		if ("/lastGet".equals(pathInfo)) {
			writer.print(lastGetName.getAndSet(null));
		} else if ("/lastUnget".equals(pathInfo)) {
			writer.print(lastUngetName.getAndSet(null));
		} else if ("/configure".equals(pathInfo)) {
			String prototypeName = request.getParameter("test.prototype.name");
			factories.putIfAbsent(prototypeName, new Factory());
			Factory factory = factories.get(prototypeName);
			configure(factory, request);
			writer.print(prototypeName);
		} else if ("/unregister".equals(pathInfo)) {
			String prototypeName = request.getParameter("test.prototype.name");
			Factory factory = factories.remove(prototypeName);
			factory.unregister();
			writer.print(prototypeName);
		} else if ("/error".equals(pathInfo)) {
			String errorCode = request.getParameter("test.error.code");
			if (errorCode != null) {
				response.sendError(Integer.parseInt(errorCode));
			}
		}
	}

	private static void configure(Factory factory, HttpServletRequest request) {
		// copy existing properties
		Hashtable<String, Object> serviceProps = new Hashtable<>();

		// Update ranking
		String serviceRanking = request.getParameter(Constants.SERVICE_RANKING);
		if (serviceRanking != null) {
			serviceProps.put(Constants.SERVICE_RANKING, Integer.valueOf(serviceRanking));
		}
		// Update context selection
		String contextSelect = request.getParameter(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT);
		if (contextSelect != null) {
			serviceProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, contextSelect);
		}
		// Update servlet pattern
		String pattern = request.getParameter(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN);
		if (pattern != null) {
			serviceProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, pattern);
		}
		// update any init params
		for (Enumeration<String> eParamNames = request.getParameterNames(); eParamNames.hasMoreElements();) {
			String name = eParamNames.nextElement();
			if (name.startsWith("servlet.init.")) {
				serviceProps.put(name, request.getParameter(name));
			}
		}

		factory.setRegistrationProperties(serviceProps);
	}
}
