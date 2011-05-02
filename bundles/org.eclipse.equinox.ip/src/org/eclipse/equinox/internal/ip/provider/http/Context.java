/*******************************************************************************
 * Copyright (c) 1997, 2008 by ProSyst Software GmbH
 * http://www.prosyst.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.ip.provider.http;

import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.http.HttpContext;

/**
 * @author Pavlin Dobrev
 * @version 1.0
 */

final class Context implements HttpContext {

	private Context() {
		// prevent instantiation
	}

	private static Context singleton;

	public static final Context getInstance() {
		if (singleton == null) {
			singleton = new Context();
		}
		return singleton;
	}

	/**
	 * @see org.osgi.service.http.HttpContext#getResource(java.lang.String)
	 */
	public URL getResource(String name) {
		return this.getClass().getResource(name);
	}

	/**
	 * @see org.osgi.service.http.HttpContext#getMimeType(java.lang.String)
	 */
	public String getMimeType(String name) {
		return null;
	}

	/**
	 * @see org.osgi.service.http.HttpContext#handleSecurity(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse)
	 */
	public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) {
		return true;
	}

}
