/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.http.servlet.tests;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.equinox.http.servlet.testbase.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

public class Bug564747_Test extends BaseTest {

	@Test
	public void test() throws Exception {
		Servlet servlet = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

				PrintWriter writer = resp.getWriter();
				writer.write(req.getQueryString());
				writer.write("|");
				writer.write(String.valueOf(req.getParameter("p")));
				writer.write("|");
				writer.write(Arrays.toString(req.getParameterValues("p")));
				writer.write("|");
				writer.write(String.valueOf(req.getParameter("q")));
				writer.write("|");
				writer.write(Arrays.toString(req.getParameterValues("q")));
				writer.write("|");
				writer.write(String.valueOf(req.getParameter("r")));
				writer.write("|");
				writer.write(Arrays.toString(req.getParameterValues("r")));
				writer.write("|");
				writer.write(String.valueOf(req.getParameter("s")));
				writer.write("|");
				writer.write(Arrays.toString(req.getParameterValues("s")));
			}
		};

		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "S13");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/Servlet13/*");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet, props));

		String result = requestAdvisor.request("Servlet13/a?p=&q=&q=2&r=3");

		Assert.assertEquals("Wrong result: " + result, "p=&q=&q=2&r=3||[]||[, 2]|3|[3]|null|null", result);
	}

}
