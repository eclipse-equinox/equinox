/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.http.servlet.tests.tb;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

/*
 * The parent class for the various test servlets. This class is responsible 
 * for registering the servlet with the HttpService, and handles the HTTP GET
 * requests by providing a template method that is implemented by subclasses.
 */
public abstract class AbstractTestServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	protected static final String STATUS_OK = "OK"; //$NON-NLS-1$	
	protected static final String STATUS_ERROR = "ERROR"; //$NON-NLS-1$
	
	private HttpService service;
	
	public final void activate() throws ServletException, NamespaceException {
		HttpService service = getHttpService();
		String alias = getAlias();
		service.registerServlet(alias, this, null, null);
	}

	protected final String createDefaultAlias() {
		return '/' + getSimpleClassName();
	}
	
	public final void deactivate() {
		HttpService service = getHttpService();
		String alias = getAlias();
		service.unregister(alias);
	}

	protected final void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		PrintWriter writer = response.getWriter();
		try {
			handleDoGet(request, writer);
		} finally {
			writer.close();
		}
	}
	
	protected String getAlias() {
		return createDefaultAlias();
	}

	private HttpService getHttpService() {
		return service;
	}
	
	private String getSimpleClassName() {
		Class clazz = getClass();
		return clazz.getSimpleName();
	}

	protected void handleDoGet(HttpServletRequest request, PrintWriter writer) throws ServletException, IOException {
		writer.print(AbstractTestServlet.STATUS_OK);
	}
	
	public final void setHttpService(HttpService service) {
		this.service = service;
	}
}
