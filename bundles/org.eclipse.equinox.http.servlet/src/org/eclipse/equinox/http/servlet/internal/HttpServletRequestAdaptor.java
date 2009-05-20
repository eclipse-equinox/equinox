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

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.http.*;
import org.osgi.service.http.HttpContext;

public class HttpServletRequestAdaptor extends HttpServletRequestWrapper {

	private String alias;
	private Servlet servlet;
	private boolean isRequestDispatcherInclude;

	static final String INCLUDE_REQUEST_URI_ATTRIBUTE = "javax.servlet.include.request_uri"; //$NON-NLS-1$
	static final String INCLUDE_CONTEXT_PATH_ATTRIBUTE = "javax.servlet.include.context_path"; //$NON-NLS-1$
	static final String INCLUDE_SERVLET_PATH_ATTRIBUTE = "javax.servlet.include.servlet_path"; //$NON-NLS-1$
	static final String INCLUDE_PATH_INFO_ATTRIBUTE = "javax.servlet.include.path_info"; //$NON-NLS-1$

	public HttpServletRequestAdaptor(HttpServletRequest req, String alias, Servlet servlet) {
		super(req);
		this.alias = alias;
		this.servlet = servlet;
		isRequestDispatcherInclude = req.getAttribute(HttpServletRequestAdaptor.INCLUDE_REQUEST_URI_ATTRIBUTE) != null;
	}
	
	public String getAuthType() {
		String authType = (String) super.getAttribute(HttpContext.AUTHENTICATION_TYPE);
		if (authType != null)
			return authType;
		
		return super.getAuthType();
	}

	public String getRemoteUser() {
		String remoteUser = (String) super.getAttribute(HttpContext.REMOTE_USER);
		if (remoteUser != null)
			return remoteUser;
		
		return super.getRemoteUser();
	}

	public String getPathInfo() {
		if (isRequestDispatcherInclude)
			return super.getPathInfo();

		if (alias.equals("/")) { //$NON-NLS-1$
			return super.getPathInfo();
		}
		String pathInfo = super.getPathInfo().substring(alias.length());
		if (pathInfo.length() == 0)
			return null;
		
		return pathInfo;
	}

	public String getServletPath() {
		if (isRequestDispatcherInclude)
			return super.getServletPath();

		if (alias.equals("/")) { //$NON-NLS-1$
			return ""; //$NON-NLS-1$
		}
		return alias;
	}

	public String getContextPath() {
		if (isRequestDispatcherInclude)
			return super.getContextPath();

		return super.getContextPath() + super.getServletPath();
	}

	public Object getAttribute(String attributeName) {
		if (isRequestDispatcherInclude) {
			if (attributeName.equals(HttpServletRequestAdaptor.INCLUDE_CONTEXT_PATH_ATTRIBUTE)) {
				String contextPath = (String) super.getAttribute(HttpServletRequestAdaptor.INCLUDE_CONTEXT_PATH_ATTRIBUTE);
				if (contextPath == null || contextPath.equals("/")) //$NON-NLS-1$
					contextPath = ""; //$NON-NLS-1$

				String servletPath = (String) super.getAttribute(HttpServletRequestAdaptor.INCLUDE_SERVLET_PATH_ATTRIBUTE);
				if (servletPath == null || servletPath.equals("/")) //$NON-NLS-1$
					servletPath = ""; //$NON-NLS-1$

				return contextPath + servletPath;
			} else if (attributeName.equals(HttpServletRequestAdaptor.INCLUDE_SERVLET_PATH_ATTRIBUTE)) {
				if (alias.equals("/")) { //$NON-NLS-1$
					return ""; //$NON-NLS-1$
				}
				return alias;
			} else if (attributeName.equals(HttpServletRequestAdaptor.INCLUDE_PATH_INFO_ATTRIBUTE)) {
				String pathInfo = (String) super.getAttribute(HttpServletRequestAdaptor.INCLUDE_PATH_INFO_ATTRIBUTE);
				if (alias.equals("/")) { //$NON-NLS-1$
					return pathInfo;
				}
				
				pathInfo = pathInfo.substring(alias.length());
				if (pathInfo.length() == 0)
					return null;
				
				return pathInfo;
			}
		}

		return super.getAttribute(attributeName);
	}

	public RequestDispatcher getRequestDispatcher(String arg0) {
		return new RequestDispatcherAdaptor(super.getRequestDispatcher(super.getServletPath() + arg0));
	}

	public static String getDispatchPathInfo(HttpServletRequest req) {
		if (req.getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE) != null)
			return (String) req.getAttribute(INCLUDE_PATH_INFO_ATTRIBUTE);

		return req.getPathInfo();
	}

	public static String getDispatchServletPath(HttpServletRequest req) {
		if (req.getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE) != null) {
			String servletPath = (String) req.getAttribute(INCLUDE_SERVLET_PATH_ATTRIBUTE);
			return (servletPath == null) ? "" : servletPath; //$NON-NLS-1$
		}
		return req.getServletPath();
	}

	public HttpSession getSession() {
		HttpSession session = super.getSession();
		if (session != null)
			return new HttpSessionAdaptor(session, servlet);

		return null;
	}

	public HttpSession getSession(boolean create) {
		HttpSession session = super.getSession(create);
		if (session != null)
			return new HttpSessionAdaptor(session, servlet);

		return null;
	}

}
