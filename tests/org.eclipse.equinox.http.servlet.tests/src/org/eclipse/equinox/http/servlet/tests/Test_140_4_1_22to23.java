/*******************************************************************************
 * Copyright (c) Jan. 26, 2019 Liferay, Inc.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Liferay, Inc. - tests
 ******************************************************************************/

package org.eclipse.equinox.http.servlet.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.equinox.http.servlet.testbase.BaseTest;
import org.junit.Test;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

public class Test_140_4_1_22to23 extends BaseTest {

	@Test
	public void test_140_4_1_22to23() throws Exception {
		final AtomicBoolean invoked = new AtomicBoolean(false);

		@SuppressWarnings("serial")
		class AServlet extends HttpServlet {

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
				response.sendError(HttpServletResponse.SC_BAD_GATEWAY, "a");
			}

		}

		@SuppressWarnings("serial")
		class BServlet extends HttpServlet {

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException {
				invoked.set(true);
				throw new ServletException();
			}

		}

		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/a");
		registrations.add(getBundleContext().registerService(Servlet.class, new AServlet(), properties));

		properties = new Hashtable<>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "b");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE, HttpServletResponse.SC_BAD_GATEWAY + "");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/b");
		registrations.add(getBundleContext().registerService(Servlet.class, new BServlet(), properties));

		Map<String, List<String>> response = requestAdvisor.request("a", null);
		assertTrue(!"a".equals(response.get("responseBody").get(0)));
		assertTrue(invoked.get());
		assertEquals(HttpServletResponse.SC_BAD_GATEWAY + "", response.get("responseCode").get(0));
	}

}
