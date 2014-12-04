/*******************************************************************************
 * Copyright (c) 2014 Raymond Augé and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Raymond Augé <raymond.auge@liferay.com> - Bug 436698
 ******************************************************************************/

package org.eclipse.equinox.http.servlet.tests.tb1;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.Filter;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.equinox.http.servlet.tests.tb.AbstractTestServlet;
import org.eclipse.equinox.http.servlet.tests.util.BaseFilter;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.NamespaceException;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

/*
 * This servlet is registered with the HttpService via the immediate DS
 * component OSGI-INF/testServlet1_component.xml.
 */
public class TestErrorPage2 extends AbstractTestServlet {
	private static final long serialVersionUID = 1L;
	private final Collection<ServiceRegistration<?>> registrations = new ArrayList<ServiceRegistration<?>>();
	@Override
	public void activate(ComponentContext componentContext) throws ServletException, NamespaceException {
		Dictionary<String, String> servletProps = new Hashtable<String, String>();
		servletProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "S1");
		servletProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, regexAlias());
		registrations.add(componentContext.getBundleContext().registerService(Servlet.class, this, servletProps));
		Dictionary<String, Object> errorProps = new Hashtable<String, Object>();
		errorProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "E1");
		errorProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE, new String[] {MyException.class.getName()});
		registrations.add(componentContext.getBundleContext().registerService(Servlet.class, errorServlet, errorProps));
	}

	@Override
	public void deactivate() {
		for (ServiceRegistration<?> registration : registrations) {
			registration.unregister();
		}
	}

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response)
		throws ServletException ,IOException {

		throw new MyException();
	}

	Filter f1 = new BaseFilter('b');
	Servlet errorServlet = new HttpServlet() {

		@Override
		protected void service(
				HttpServletRequest request, HttpServletResponse response)
			throws ServletException ,IOException {

			if (response.isCommitted()) {
				System.out.println("Problem?");

				return;
			}

			PrintWriter writer = response.getWriter();

			String requestURI = (String)request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
			String exception = (String)request.getAttribute(RequestDispatcher.ERROR_EXCEPTION_TYPE);

			writer.print(exception + " ERROR : " + requestURI);
		}

	};

	public class MyException extends ServletException {
	}

}
