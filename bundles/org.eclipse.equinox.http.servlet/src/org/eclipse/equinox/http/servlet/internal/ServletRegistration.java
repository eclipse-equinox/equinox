/*******************************************************************************
 * Copyright (c) 2005 Cognos Incorporated.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Cognos Incorporated - initial API and implementation
 *     IBM Corporation - comments
 *******************************************************************************/

package org.eclipse.equinox.http.servlet.internal;

import java.io.IOException;
import java.util.Set;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;

//This class wraps the servlet object registered in the HttpService.registerServlet call, to manage the context classloader when handleRequests are being asked.
//It is also responsible to ensure that a given servlet has only been registered once. 
public class ServletRegistration extends Registration {

	private Servlet servlet; //The actual servlet object registered against the http service. All requests will eventually be delegated to it.
	private HttpContext httpContext; //The context used during the registration of the servlet
	private Set servlets; //All the servlets registered against the instance of the proxy servlet that "ownes" self.
	private ClassLoader registeredContextClassLoader;

	public ServletRegistration(Servlet servlet, HttpContext context, Bundle bundle, Set servlets) {
		this.servlet = servlet;
		this.servlets = servlets;
		this.httpContext = context;
		registeredContextClassLoader = Thread.currentThread().getContextClassLoader();
	}

	public void destroy() {
		ClassLoader original = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(registeredContextClassLoader);
			super.destroy();
		} finally {
			Thread.currentThread().setContextClassLoader(original);
		}
		servlet.destroy();
	}

	public void close() {
		servlets.remove(servlet);
	}

	//Delegate the init call to the actual servlet
	public void init(ServletConfig servletConfig) throws ServletException {
		ClassLoader original = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(registeredContextClassLoader);
			servlet.init(servletConfig);
		} finally {
			Thread.currentThread().setContextClassLoader(original);
		}
		servlets.add(servlet);
	}

	public void checkServletRegistration() throws ServletException {
		if (servlets.contains(servlet)) {
			throw new ServletException("This servlet has already been registered at a different alias."); //$NON-NLS-1$
		}
	}

	//Delegate the handling of the request to the actual servlet
	public boolean handleRequest(HttpServletRequest req, HttpServletResponse resp, String alias) throws IOException, ServletException {
		HttpServletRequest wrappedRequest = new HttpServletRequestAdaptor(req, alias, servlet);

		if (httpContext.handleSecurity(wrappedRequest, resp)) {
			ClassLoader original = Thread.currentThread().getContextClassLoader();
			try {
				Thread.currentThread().setContextClassLoader(registeredContextClassLoader);
				servlet.service(wrappedRequest, resp);
			} finally {
				Thread.currentThread().setContextClassLoader(original);
			}
		}
		return true;
	}
}
