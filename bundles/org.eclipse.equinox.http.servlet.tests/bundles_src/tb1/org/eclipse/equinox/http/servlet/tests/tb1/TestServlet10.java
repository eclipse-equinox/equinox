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

package org.eclipse.equinox.http.servlet.tests.tb1;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.equinox.http.servlet.tests.tb.AbstractTestServlet;

/**
 * @author Raymond Augé
 */
public class TestServlet10 extends AbstractTestServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {

		ServletContext servletContext = getServletContext();

		InputStream in = servletContext.getResourceAsStream(
			"/org/eclipse/equinox/http/servlet/tests/tb1/resource1.txt");
		OutputStream out = response.getOutputStream();

		try {
			  byte[] buffer = new byte[2048];
			  int bytesRead;

			  while ((bytesRead = in.read(buffer)) != -1) {
				 out.write(buffer, 0, bytesRead);
			  }
		}
		finally {
			out.close();
			in.close();
		}
	}

	@Override
	protected void handleDoGet(HttpServletRequest request, PrintWriter writer) throws ServletException, IOException {
		//
	}

}
