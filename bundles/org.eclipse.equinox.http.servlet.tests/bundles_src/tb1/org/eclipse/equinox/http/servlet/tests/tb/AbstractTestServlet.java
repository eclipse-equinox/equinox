/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Raymond Augé <raymond.auge@liferay.com> - Bug 436698
 *******************************************************************************/
package org.eclipse.equinox.http.servlet.tests.tb;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.ComponentContext;
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
	private Map<String, Object> properties;

	public void activate(ComponentContext componentContext) throws ServletException, NamespaceException {
		HttpService service = getHttpService();
		String alias = getAlias();
		service.registerServlet(alias, this, null, null);
	}

	protected final String createDefaultAlias() {
		return '/' + getSimpleClassName();
	}

	protected final String extensionAlias() {
		return "*." + getSimpleClassName();
	}

	protected final String regexAlias() {
		return createDefaultAlias() + "/*";
	}

	public void deactivate() {
		HttpService service = getHttpService();
		String alias = getAlias();
		service.unregister(alias);
	}

	@Override
	protected final void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try (PrintWriter writer = response.getWriter()) {
			handleDoGet(request, writer);
		}
	}

	protected String getAlias() {
		return createDefaultAlias();
	}

	protected HttpService getHttpService() {
		return service;
	}

	protected Map<String, Object> getProperties() {
		return properties;
	}

	protected String getSimpleClassName() {
		Class<?> clazz = getClass();
		return clazz.getSimpleName();
	}

	@SuppressWarnings("unused")
	protected void handleDoGet(HttpServletRequest request, PrintWriter writer) throws ServletException, IOException {
		writer.print(AbstractTestServlet.STATUS_OK);
	}

	public final void setHttpService(HttpService service, Map<String, Object> properties) {
		this.service = service;
		this.properties = properties;
	}
}
