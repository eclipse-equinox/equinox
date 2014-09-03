/*******************************************************************************
 * Copyright (c) 2005, 2014 Cognos Incorporated, IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cognos Incorporated - initial API and implementation
 *     IBM Corporation - bug fixes and enhancements
 *     Raymond Augé <raymond.auge@liferay.com> - Bug 436698
 *******************************************************************************/

package org.eclipse.equinox.http.servlet.internal.context;

import java.io.IOException;
import java.net.URL;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.context.ServletContextHelperContext;

public class DefaultServletContextHelper extends ServletContextHelper
	implements HttpContext {

	public DefaultServletContextHelper(Bundle bundle) {
		super(bundle);

		this.defaultContext = new ServletContextHelperContext() {

			@Override
			public ServletContextHelper getParentContext(
				ServletContextHelper context) {

				return null;
			}

		};
	}

	@Override
	public boolean handleSecurity(
			HttpServletRequest request, HttpServletResponse response)
		throws IOException {

		return handleSecurity(defaultContext, request, response);
	}

	@Override
	public String getMimeType(String name) {
		return getMimeType(defaultContext, name);
	}

	@Override
	public URL getResource(String name) {
		return getResource(defaultContext, name);
	}

	private final ServletContextHelperContext defaultContext;

}