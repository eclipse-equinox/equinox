/*******************************************************************************
 * Copyright (c) 2014 Raymond Augé and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Raymond Augé <raymond.auge@liferay.com> - Bug 436698
 ******************************************************************************/

package org.eclipse.equinox.http.servlet.tests.util;

import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;

public class BaseHttpContext implements HttpContext {
	private final boolean handleSecurity;
	private final String resourceRoot;
	private final Bundle bundle;

	public BaseHttpContext(boolean handleSecurity, String resourceRoot, Bundle bundle) {
		this.handleSecurity = handleSecurity;
		this.resourceRoot = resourceRoot;
		this.bundle = bundle;
	}

	@Override
	public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) {
		return handleSecurity;
	}

	@Override
	public URL getResource(String name) {
		return bundle.getEntry(resourceRoot + '/' + name);
	}

	@Override
	public String getMimeType(String name) {
		return null;
	}

}
