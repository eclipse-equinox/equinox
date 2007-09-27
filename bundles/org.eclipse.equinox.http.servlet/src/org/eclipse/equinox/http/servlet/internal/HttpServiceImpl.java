/*******************************************************************************
 * Copyright (c) 2005-2007 Cognos Incorporated, IBM Corporation and others
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

import java.util.*;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import org.osgi.framework.Bundle;
import org.osgi.service.http.*;

public class HttpServiceImpl implements HttpService {

	private Bundle bundle; //The bundle associated with this instance of http service

	private ProxyServlet proxy; //The proxy that does the dispatching of the incoming requests

	Set aliases = new HashSet(); //Aliases registered against this particular instance of the service

	public HttpServiceImpl(Bundle bundle, ProxyServlet proxy) {
		this.bundle = bundle;
		this.proxy = proxy;
	}

	//Clean up method
	public synchronized void unregisterAliases() {
		for (Iterator it = aliases.iterator(); it.hasNext();) {
			String alias = (String) it.next();
			proxy.unregister(alias, false);
		}
		aliases.clear();
	}

	/**
	 * @see HttpService#registerServlet(String, Servlet, Dictionary, HttpContext)
	 */
	public synchronized void registerServlet(String alias, Servlet servlet, Dictionary initparams, HttpContext context) throws ServletException, NamespaceException {
		if (context == null) {
			context = createDefaultHttpContext();
		}
		proxy.registerServlet(alias, servlet, initparams, context, bundle);
		aliases.add(alias);
	}

	/**
	 * @see HttpService#registerResources(String, String, HttpContext)
	 */
	public synchronized void registerResources(String alias, String name, HttpContext context) throws NamespaceException {
		if (context == null) {
			context = createDefaultHttpContext();
		}
		proxy.registerResources(alias, name, context);
		aliases.add(alias);
	}

	/**
	 * @see HttpService#unregister(String)
	 */
	public synchronized void unregister(String alias) {
		if (aliases.remove(alias)) {
			proxy.unregister(alias, true);
		} else {
			// TODO perhaps this is too strong a reaction ?
			throw new IllegalArgumentException("Alias not found."); //$NON-NLS-1$
		}
	}

	/**
	 * @see HttpService#createDefaultHttpContext()
	 */
	public HttpContext createDefaultHttpContext() {
		return new DefaultHttpContext(bundle);
	}
}
