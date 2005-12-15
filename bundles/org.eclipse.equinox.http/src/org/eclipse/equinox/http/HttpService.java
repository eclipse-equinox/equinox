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

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.NamespaceException;

public class HttpService implements org.osgi.service.http.HttpService {
	private HttpListener listener;
	private Bundle bundle;

	HttpService(HttpListener listener, Bundle bundle) {
		this.listener = listener;
		this.bundle = bundle;
	}

	void destroy() {
		listener.destroyBundle(bundle);
		listener = null;
		bundle = null;
	}

	public void registerResources(String alias, String name, HttpContext httpContext) throws NamespaceException {
		HttpListener listener = this.listener;

		if (listener != null) {
			if (httpContext == null) {
				httpContext = createDefaultHttpContext();
			}

			listener.registerResources(bundle, alias, name, httpContext);
		}
	}

	public void registerServlet(String alias, Servlet servlet, java.util.Dictionary initparams, HttpContext httpContext) throws ServletException, NamespaceException, IllegalArgumentException {
		HttpListener listener = this.listener;

		if (listener != null) {
			if (httpContext == null) {
				httpContext = createDefaultHttpContext();
			}

			listener.registerServlet(bundle, alias, servlet, initparams, httpContext);
		}
	}

	public void unregister(String alias) throws IllegalArgumentException {
		HttpListener listener = this.listener;

		if (listener != null) {
			listener.unregister(bundle, alias);
		}
	}

	public HttpContext createDefaultHttpContext() {
		HttpListener listener = this.listener;

		if (listener != null) {
			return (listener.createDefaultHttpContext(bundle));
		}

		return (null);
	}
}
