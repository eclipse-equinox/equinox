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

package org.eclipse.equinox.http.servlet.tests.tb;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

/*
 * The parent class for the various test servlets. This class is responsible
 * for registering the servlet with the HttpService, and handles the HTTP GET
 * requests by providing a template method that is implemented by subclasses.
 */
@SuppressWarnings("deprecation")
public abstract class AbstractTestResource {
	protected static final String STATUS_OK = "OK"; //$NON-NLS-1$
	protected static final String STATUS_ERROR = "ERROR"; //$NON-NLS-1$

	private HttpService service;

	public void activate(ComponentContext componentContext) throws ServletException, NamespaceException {
		HttpService service = getHttpService();
		String alias = getAlias();
		service.registerResources(alias, getName() , null);
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

	protected String getAlias() {
		return createDefaultAlias();
	}

	protected HttpService getHttpService() {
		return service;
	}

	protected String getName() {
		Class<?> clazz = getClass();
		Package javaPackage = clazz.getPackage();
		return "/" + javaPackage.getName().replaceAll("\\.", "/");
	}

	private String getSimpleClassName() {
		Class<?> clazz = getClass();
		return clazz.getSimpleName();
	}

	protected void handleDoGet(HttpServletRequest request, PrintWriter writer) throws ServletException, IOException {
		writer.print(AbstractTestResource.STATUS_OK);
	}

	public final void setHttpService(HttpService service) {
		this.service = service;
	}
}
