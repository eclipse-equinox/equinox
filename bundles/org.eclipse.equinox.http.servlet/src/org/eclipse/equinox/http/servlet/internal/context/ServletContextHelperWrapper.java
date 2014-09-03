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

package org.eclipse.equinox.http.servlet.internal.context;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.context.ServletContextHelperContext;

/**
 * @author Raymond Augé
 */
public class ServletContextHelperWrapper
	extends ServletContextHelper implements HttpContext {

	public ServletContextHelperWrapper(
		HttpContext httpContext, Bundle bundle) {

		super(bundle);

		_httpContext = httpContext;
		_bundle = bundle;
	}

	@Override
	public String getMimeType(String name) {
		return _httpContext.getMimeType(name);
	}

	@Override
	public String getMimeType(ServletContextHelperContext context, String name) {
		return _httpContext.getMimeType(name);
	}

	@Override
	public String getRealPath(ServletContextHelperContext context, String path) {
		return null;
	}

	@Override
	public URL getResource(String name) {
		return _httpContext.getResource(name);
	}

	@Override
	public URL getResource(ServletContextHelperContext context, String name) {
		return _httpContext.getResource(name);
	}

	@Override
	public Set<String> getResourcePaths(
		ServletContextHelperContext context ,String path) {

		if ((path == null) || (_bundle == null)) {
			return null;
		}

		final Enumeration<URL> enumeration = _bundle.findEntries(
			path, null, false);

		if (enumeration == null) {
			return null;
		}

		final Set<String> result = new HashSet<String>();

		while (enumeration.hasMoreElements()) {
			result.add(enumeration.nextElement().getPath());
		}

		return result;
	}

	@Override
	public boolean handleSecurity(
			HttpServletRequest request, HttpServletResponse response)
		throws IOException {

		return _httpContext.handleSecurity(request, response);
	}

	@Override
	public boolean handleSecurity(
			ServletContextHelperContext context, HttpServletRequest request,
			HttpServletResponse response)
		throws IOException {

		return _httpContext.handleSecurity(request, response);
	}

	private final Bundle _bundle;
	private final HttpContext _httpContext;

}