/*******************************************************************************
 * Copyright (c) 2005, 2007 Cognos Incorporated, IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Cognos Incorporated - initial API and implementation
 *     IBM Corporation - bug fixes and enhancements
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
	private ProxyContext proxyContext;

	public ServletRegistration(Servlet servlet, ProxyContext proxyContext, HttpContext context, Bundle bundle, Set servlets) {
		this.servlet = servlet;
		this.servlets = servlets;
		this.httpContext = context;
		this.proxyContext = proxyContext;
		registeredContextClassLoader = Thread.currentThread().getContextClassLoader();
	}

	public void destroy() {
		ClassLoader original = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(registeredContextClassLoader);
			super.destroy();
			servlet.destroy();
		} finally {
			Thread.currentThread().setContextClassLoader(original);
		}		
	}

	public void close() {		
		servlets.remove(servlet);
		proxyContext.destroyContextAttributes(httpContext);
	}

	//Delegate the init call to the actual servlet
	public void init(ServletConfig servletConfig) throws ServletException {
		boolean initialized = false;
		proxyContext.createContextAttributes(httpContext);
		try {
			ClassLoader original = Thread.currentThread().getContextClassLoader();
			try {
				Thread.currentThread().setContextClassLoader(registeredContextClassLoader);
				servlet.init(servletConfig);
			} finally {
				Thread.currentThread().setContextClassLoader(original);
			}
			servlets.add(servlet);
			initialized = true;
		} finally {
			if (! initialized)
				proxyContext.destroyContextAttributes(httpContext);
		}
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
