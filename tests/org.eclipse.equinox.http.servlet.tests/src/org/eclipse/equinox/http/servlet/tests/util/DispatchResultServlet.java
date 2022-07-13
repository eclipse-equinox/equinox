/*******************************************************************************
 * Copyright (c) 2016, 2019 Raymond Augé and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Raymond Augé <raymond.auge@liferay.com> - initial implementation
 *******************************************************************************/
package org.eclipse.equinox.http.servlet.tests.util;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DispatchResultServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void service(
		HttpServletRequest request, HttpServletResponse response)
		throws IOException {

		StringWriter writer = new StringWriter();

		writer.write(request.getContextPath());
		writer.write("|");
		writer.write(request.getPathInfo());
		writer.write("|");
		writer.write(request.getQueryString());
		writer.write("|");
		writer.write(request.getRequestURI());
		writer.write("|");
		writer.write(request.getServletPath());
		writer.write("|");

		if (request.getDispatcherType() == DispatcherType.INCLUDE) {
			writer.write(String.valueOf(request.getAttribute(RequestDispatcher.INCLUDE_CONTEXT_PATH)));
			writer.write("|");
			writer.write(String.valueOf(request.getAttribute(RequestDispatcher.INCLUDE_PATH_INFO)));
			writer.write("|");
			writer.write(String.valueOf(request.getAttribute(RequestDispatcher.INCLUDE_QUERY_STRING)));
			writer.write("|");
			writer.write(String.valueOf(request.getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI)));
			writer.write("|");
			writer.write(String.valueOf(request.getAttribute(RequestDispatcher.INCLUDE_SERVLET_PATH)));
		}
		else if (request.getDispatcherType() == DispatcherType.FORWARD) {
			writer.write(String.valueOf(request.getAttribute(RequestDispatcher.FORWARD_CONTEXT_PATH)));
			writer.write("|");
			writer.write(String.valueOf(request.getAttribute(RequestDispatcher.FORWARD_PATH_INFO)));
			writer.write("|");
			writer.write(String.valueOf(request.getAttribute(RequestDispatcher.FORWARD_QUERY_STRING)));
			writer.write("|");
			writer.write(String.valueOf(request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI)));
			writer.write("|");
			writer.write(String.valueOf(request.getAttribute(RequestDispatcher.FORWARD_SERVLET_PATH)));
		}

		try {
			response.getWriter().write(writer.toString());
		}
		catch (IllegalStateException ise) {
			response.getOutputStream().write(writer.toString().getBytes(StandardCharsets.UTF_8));
		}

	}

}
