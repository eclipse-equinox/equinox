/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.http.jetty.internal;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;
import javax.servlet.*;
import org.mortbay.jetty.servlet.ServletHandler;

public class Servlet25Handler extends ServletHandler {
	private static final long serialVersionUID = 1512994864814170424L;
	ServletContext servletContext;

	public Servlet25Handler() {
		super();
		servletContext = new Servlet25Context(super.getServletContext());
	}

	public void destroy() {
		servletContext = null;
		super.destroy();
	}

	public ServletContext getServletContext() {
		return servletContext;
	}

	public class Servlet25Context implements ServletContext {

		private ServletContext delegate;

		public Servlet25Context(ServletContext servletContext) {
			this.delegate = servletContext;
		}

		public Object getAttribute(String name) {
			return delegate.getAttribute(name);
		}

		public Enumeration getAttributeNames() {
			return delegate.getAttributeNames();
		}

		public ServletContext getContext(String uripath) {
			return delegate.getContext(uripath);
		}

		public String getInitParameter(String name) {
			return delegate.getInitParameter(name);
		}

		public Enumeration getInitParameterNames() {
			return delegate.getInitParameterNames();
		}

		public int getMajorVersion() {
			return delegate.getMajorVersion();
		}

		public String getMimeType(String file) {
			return delegate.getMimeType(file);
		}

		public int getMinorVersion() {
			return delegate.getMinorVersion();
		}

		public RequestDispatcher getNamedDispatcher(String name) {
			return delegate.getNamedDispatcher(name);
		}

		public String getRealPath(String path) {
			return delegate.getRealPath(path);
		}

		public RequestDispatcher getRequestDispatcher(String path) {
			return delegate.getRequestDispatcher(path);
		}

		public URL getResource(String path) throws MalformedURLException {
			return delegate.getResource(path);
		}

		public InputStream getResourceAsStream(String path) {
			return delegate.getResourceAsStream(path);
		}

		public Set getResourcePaths(String path) {
			return delegate.getResourcePaths(path);
		}

		public String getServerInfo() {
			return delegate.getServerInfo();
		}

		/**
		 * @deprecated
		 */
		public Servlet getServlet(String name) throws ServletException {
			return delegate.getServlet(name);
		}

		public String getServletContextName() {
			return delegate.getServletContextName();
		}

		/**
		 * @deprecated
		 */
		public Enumeration getServletNames() {
			return delegate.getServletNames();
		}

		/**
		 * @deprecated
		 */
		public Enumeration getServlets() {
			return delegate.getServlets();
		}

		/**
		 * @deprecated
		 */
		public void log(Exception exception, String msg) {
			delegate.log(exception, msg);
		}

		public void log(String message, Throwable throwable) {
			delegate.log(message, throwable);
		}

		public void log(String msg) {
			delegate.log(msg);
		}

		public void removeAttribute(String name) {
			delegate.removeAttribute(name);
		}

		public void setAttribute(String name, Object object) {
			delegate.setAttribute(name, object);
		}

		public String getContextPath() {
			String contextPath = getHttpContext().getContextPath();
			if (contextPath.endsWith("/")) //$NON-NLS-1$
				return contextPath.substring(0, contextPath.length() - 1);

			return contextPath;
		}
	}

}