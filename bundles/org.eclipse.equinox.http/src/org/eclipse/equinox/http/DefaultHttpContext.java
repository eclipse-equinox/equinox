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
import java.net.URL;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;

public class DefaultHttpContext implements HttpContext {

	protected Bundle bundle;
	protected HttpSecurityTracker securityTracker;

	public DefaultHttpContext(Bundle bundle, HttpSecurityTracker securityTracker) {
		this.bundle = bundle;
		this.securityTracker = securityTracker;
	}

	/* Implementation defined behavior */
	public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
		return securityTracker.handleSecurity(request, response);
	}

	/* Behavior defined by OSGi spec */
	public URL getResource(String name) {
		return (new SecureAction()).getBundleResource(bundle, name);
	}

	/* Behavior defined by OSGi spec */
	public String getMimeType(String name) {
		return null;
	}

}
