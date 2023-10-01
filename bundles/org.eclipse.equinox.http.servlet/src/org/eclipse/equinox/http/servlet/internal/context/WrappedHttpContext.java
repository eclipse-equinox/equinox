/*******************************************************************************
 * Copyright (c) Jan. 27, 2019 Liferay, Inc.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Liferay, Inc. - initial API and implementation and/or initial
 *                    documentation
 ******************************************************************************/

package org.eclipse.equinox.http.servlet.internal.context;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.equinox.http.servlet.internal.DefaultServletContextHelper;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;

public class WrappedHttpContext extends DefaultServletContextHelper {

	private final HttpContext httpContext;
	private final Bundle bundle;

	public WrappedHttpContext(HttpContext httpContext, Bundle bundle) {
		super(bundle);
		this.httpContext = httpContext;
		this.bundle = bundle;
	}

	@Override
	public boolean handleSecurity(
		HttpServletRequest request, HttpServletResponse response)
		throws IOException {
		return httpContext.handleSecurity(request, response);
	}

	@Override
	public URL getResource(String name) {
		return httpContext.getResource(name);
	}

	@Override
	public String getMimeType(String name) {
		return httpContext.getMimeType(name);
	}

	@Override
	public Set<String> getResourcePaths(String path) {
		if ((path == null) || (bundle == null)) {
			return null;
		}

		final Enumeration<URL> enumeration = bundle.findEntries(
			path, null, false);

		if (enumeration == null) {
			return null;
		}

		final Set<String> result = new HashSet<>();

		while (enumeration.hasMoreElements()) {
			result.add(enumeration.nextElement().getPath());
		}

		return result;
	}
}
