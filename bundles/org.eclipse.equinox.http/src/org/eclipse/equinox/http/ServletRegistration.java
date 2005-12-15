/*******************************************************************************
 * Copyright (c) 1999, 2005 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.http;

import java.io.IOException;
import java.security.AccessControlContext;
import java.security.AccessController;
import javax.servlet.*;
import org.eclipse.equinox.http.servlet.*;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;

public class ServletRegistration implements Registration {
	protected Bundle bundle;
	protected HttpContext httpContext;
	protected String alias;
	protected Servlet servlet;
	protected ServletContextImpl servletContext;
	protected AccessControlContext accessControlContext;

	protected ServletRegistration(Bundle bundle, String alias, Servlet servlet, HttpContext httpContext, ServletContextImpl servletContext) {
		this.bundle = bundle;
		this.alias = alias;
		this.servlet = servlet;
		this.httpContext = httpContext;
		this.servletContext = servletContext;
		// The constructor is called on the calling bundle's thread, therefore this
		// should capture the AccessControlContext for the calling bundle.
		accessControlContext = AccessController.getContext();
	}

	public Bundle getBundle() {
		return (bundle);
	}

	public HttpContext getHttpContext() {
		return (httpContext);
	}

	public void destroy() {
		if (servlet != null) {
			servlet.destroy();
		}

		this.alias = null;
		this.servlet = null;
	}

	public java.lang.String getAlias() {
		return (alias);
	}

	/**
	 * This is to provide the request dispatcher direct access to the servlet for a
	 * RequestDispatcher.include call
	 * @return javax.servlet.Servlet
	 */
	public Servlet getServlet() {
		return (servlet);
	}

	public void service(HttpServletRequestImpl request, HttpServletResponseImpl response) throws ServletException, IOException {
		/* set additional data for the servlet request */
		request.init(alias, servletContext);

		service((ServletRequest) request, (ServletResponse) response);
	}

	public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
		if (servlet instanceof SingleThreadModel) {
			synchronized (this) {
				servlet.service(request, response);
			}
		} else {
			servlet.service(request, response);
		}
	}

}
