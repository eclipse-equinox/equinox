/*******************************************************************************
 * Copyright (c) 2015 Raymond Augé and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Raymond Augé <raymond.auge@liferay.com> - Bug 464377
 ******************************************************************************/

package org.eclipse.equinox.http.servlet.tests.util;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class BufferedServlet extends HttpServlet {

	static final char[] value = String.format("%01023d", 1).toCharArray();

	@Override
	protected void service(
			HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {

		response.setContentType( "text/html" );
		response.setBufferSize(value.length);

		PrintWriter writer = response.getWriter();

		for (int i = 0; i < 10; i++) {
			writer.print(value);

			response.flushBuffer();
			response.setStatus(HttpServletResponse.SC_OK);
		}

		writer.print(value);
	}

}