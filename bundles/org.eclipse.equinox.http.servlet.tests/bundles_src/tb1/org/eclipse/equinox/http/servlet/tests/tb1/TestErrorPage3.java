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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.equinox.http.servlet.tests.tb.AbstractTestServlet;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

/*
 * This servlet is registered with the HttpService via the immediate DS
 * component OSGI-INF/testServlet1_component.xml.
 */
public class TestErrorPage3 extends AbstractTestServlet {
	private static final long serialVersionUID = 1L;
	private final Collection<ServiceRegistration<?>> registrations = new ArrayList<>();
	@Override
	public void activate(ComponentContext componentContext) {
		Dictionary<String, String> servletProps = new Hashtable<>();
		servletProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "S3");
		servletProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, regexAlias());
		registrations.add(componentContext.getBundleContext().registerService(Servlet.class, this, servletProps));
		Dictionary<String, Object> errorProps = new Hashtable<>();
		errorProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "E3");
		errorProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE, "400");
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
		throws IOException {

		response.sendError(400);
	}

	Servlet errorServlet = new HttpServlet() {
		private static final long serialVersionUID = 1L;

		@Override
		protected void service(
				HttpServletRequest request, HttpServletResponse response)
			throws IOException {

			if (response.isCommitted()) {
				System.out.println("Problem?");

				return;
			}

			PrintWriter writer = response.getWriter();

			String requestURI = (String)request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
			Integer status = (Integer)request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

			writer.print(status + " ERROR : " + requestURI);
		}

	};

}
