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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.equinox.http.servlet.testbase.BaseTest;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.runtime.dto.ErrorPageDTO;
import org.osgi.service.http.runtime.dto.ServletDTO;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

public class Test_table_140_4_HTTP_WHITEBOARD_SERVLET_ERROR_PAGE_exception extends BaseTest {

	@Test
	public void test_table_140_4_HTTP_WHITEBOARD_SERVLET_ERROR_PAGE_exception() throws Exception {
		BundleContext context = getBundleContext();

		final AtomicBoolean invoked = new AtomicBoolean(false);

		@SuppressWarnings("serial")
		class AException extends ServletException {
		}

		@SuppressWarnings("serial")
		class AServlet extends HttpServlet {

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException {
				throw new AException();
			}

		}

		@SuppressWarnings("serial")
		class BServlet extends HttpServlet {

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
				invoked.set(true);
				String exception = (String) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION_TYPE);
				response.getWriter().write((exception == null) ? "" : exception);
			}

		}

		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/a");
		registrations.add(context.registerService(Servlet.class, new AServlet(), properties));

		properties = new Hashtable<>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "b");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE, ServletException.class.getName());
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/error");
		registrations.add(context.registerService(Servlet.class, new BServlet(), properties));

		ServletDTO servletDTO = getServletDTOByName(DEFAULT, "b");
		assertNotNull(servletDTO);
		ErrorPageDTO errorPageDTO = getErrorPageDTOByName(DEFAULT, "b");
		assertNotNull(errorPageDTO);
		assertTrue(Arrays.binarySearch(errorPageDTO.exceptions, ServletException.class.getName()) >= 0);

		Map<String, List<String>> response = requestAdvisor.request("a", null);
		assertEquals(AException.class.getName(), response.get("responseBody").get(0));
		assertTrue(invoked.get());
		assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR + "", response.get("responseCode").get(0));
	}

}
