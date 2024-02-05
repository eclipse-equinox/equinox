/*******************************************************************************
 * Copyright (c) 2014 Raymond Augé and others.
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

package org.eclipse.equinox.http.servlet.tests.tb1;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;

import org.eclipse.equinox.http.servlet.tests.tb.AbstractTestServlet;
import org.eclipse.equinox.http.servlet.tests.util.BaseFilter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

/*
 * This servlet is registered with the HttpService via the immediate DS
 * component OSGI-INF/testServlet1_component.xml.
 */
public class TestServletContextHelper10 extends AbstractTestServlet {
	private static final long serialVersionUID = 1L;
	private final Collection<ServiceRegistration<?>> registrations = new ArrayList<>();

	@Override
	public void activate(ComponentContext componentContext) {
		Dictionary<String, String> servletProps = new Hashtable<>();
		servletProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, regexAlias());
		servletProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
		registrations.add(componentContext.getBundleContext().registerService(Servlet.class, this, servletProps));

		Dictionary<String, String> filterProps = new Hashtable<>();
		filterProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "F1");
		filterProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, regexAlias());
		filterProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
		registrations.add(componentContext.getBundleContext().registerService(Filter.class, f1, filterProps));

		BundleContext bundleContext = componentContext.getBundleContext();
		Bundle bundle = bundleContext.getBundle();

		ServletContextHelper servletContextHelper = new ServletContextHelper(bundle) {
		};
		Dictionary<String, String> contextProps = new Hashtable<>();
		contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "a");
		contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/a");
		registrations.add(componentContext.getBundleContext().registerService(ServletContextHelper.class,
				servletContextHelper, contextProps));
	}

	@Override
	public void deactivate() {
		for (ServiceRegistration<?> registration : registrations) {
			registration.unregister();
		}
	}

	@Override
	protected void handleDoGet(HttpServletRequest request, PrintWriter writer) {
		writer.print('a');
	}

	private Filter f1 = new BaseFilter('c');
}
