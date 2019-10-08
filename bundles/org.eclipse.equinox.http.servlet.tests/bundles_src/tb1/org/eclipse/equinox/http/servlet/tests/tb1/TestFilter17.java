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
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

/*
 * This servlet is registered with the HttpService via the immediate DS
 * component OSGI-INF/testServlet1_component.xml.
 */
public class TestFilter17 extends AbstractTestServlet {
	private static final long serialVersionUID = 1L;
	private final Collection<ServiceRegistration<?>> registrations = new ArrayList<>();

	@Override
	public void activate(ComponentContext componentContext) {

		Dictionary<String, String> servletProps = new Hashtable<>();
		servletProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "TestFilter17");
		servletProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/TestFilter17/foo/bar/baz");
		registrations.add(componentContext.getBundleContext().registerService(Servlet.class, this, servletProps));

		// Should order like: ebcdadcbe

		// b
		Dictionary<String, Object> filterProps = new Hashtable<>();
		filterProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "F1");
		filterProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/TestFilter17/foo/bar/*");
		registrations.add(componentContext.getBundleContext().registerService(Filter.class, f1, filterProps));

		// c
		filterProps = new Hashtable<>();
		filterProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "F2");
		filterProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/TestFilter17/*");
		registrations.add(componentContext.getBundleContext().registerService(Filter.class, f2, filterProps));

		// d
		filterProps = new Hashtable<>();
		filterProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "F3");
		filterProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/TestFilter17/foo/*");
		registrations.add(componentContext.getBundleContext().registerService(Filter.class, f3, filterProps));

		// e
		filterProps = new Hashtable<>();
		filterProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "F4");
		filterProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_SERVLET, "TestFilter17");
		filterProps.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
		registrations.add(componentContext.getBundleContext().registerService(Filter.class, f4, filterProps));
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

	Filter f1 = new BaseFilter('b');
	Filter f2 = new BaseFilter('c');
	Filter f3 = new BaseFilter('d');
	Filter f4 = new BaseFilter('e');
}
