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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.http.HttpContext;
import org.osgi.util.tracker.ServiceTracker;

public class HttpSecurityTracker extends ServiceTracker {

	protected HttpSecurityTracker(BundleContext context) throws InvalidSyntaxException {
		super(context, context.createFilter("(&(objectClass=org.osgi.service.http.HttpContext)(org.eclipse.equinox.http.default.handleSecurity=*))"), null); //$NON-NLS-1$

		open();
	}

	protected boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
		HttpContext service = (HttpContext) getService();

		if (service != null) {
			return (service.handleSecurity(request, response));
		}

		return (defaultHandleSecurity(request, response));
	}

	protected boolean defaultHandleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
		return (true);
	}
}
