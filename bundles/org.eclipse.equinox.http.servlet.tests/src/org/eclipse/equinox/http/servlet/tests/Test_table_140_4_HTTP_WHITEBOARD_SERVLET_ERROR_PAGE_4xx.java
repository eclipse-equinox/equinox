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
import static org.junit.Assert.assertNull;
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
import org.osgi.framework.Constants;
import org.osgi.service.http.runtime.dto.DTOConstants;
import org.osgi.service.http.runtime.dto.ErrorPageDTO;
import org.osgi.service.http.runtime.dto.FailedErrorPageDTO;
import org.osgi.service.http.runtime.dto.ServletDTO;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

public class Test_table_140_4_HTTP_WHITEBOARD_SERVLET_ERROR_PAGE_4xx extends BaseTest {

	@Test
	public void test_table_140_4_HTTP_WHITEBOARD_SERVLET_ERROR_PAGE_4xx() throws Exception {
		BundleContext context = getBundleContext();

		final AtomicBoolean invoked = new AtomicBoolean(false);

		@SuppressWarnings("serial")
		class AServlet extends HttpServlet {

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
				response.sendError(HttpServletResponse.SC_FORBIDDEN, "a");
			}

		}

		@SuppressWarnings("serial")
		class BServlet extends HttpServlet {

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
				invoked.set(true);
				String message = (String) request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
				response.getWriter().write((message == null) ? "" : message);
			}

		}

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/a");
		registrations.add(context.registerService(Servlet.class, new AServlet(), properties));

		// Register the 4xx (b)
		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "b");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE, "4xx");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/b");
		registrations.add(context.registerService(Servlet.class, new BServlet(), properties));

		ServletDTO servletDTO = getServletDTOByName(DEFAULT, "b");
		assertNotNull(servletDTO);
		ErrorPageDTO errorPageDTO = getErrorPageDTOByName(DEFAULT, "b");
		assertNotNull(errorPageDTO);
		assertTrue(Arrays.binarySearch(errorPageDTO.errorCodes, HttpServletResponse.SC_FORBIDDEN) >= 0);

		Map<String, List<String>> response = requestAdvisor.request("a", null);
		assertEquals("a", response.get("responseBody").get(0));
		assertTrue(invoked.get());
		assertEquals(HttpServletResponse.SC_FORBIDDEN + "", response.get("responseCode").get(0));

		// register a 4xx which will be shadowed (c)
		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "c");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE, "4xx");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/c");
		registrations.add(context.registerService(Servlet.class, new BServlet(), properties));

		FailedErrorPageDTO failedErrorPageDTO = getFailedErrorPageDTOByName("c");
		assertNotNull(failedErrorPageDTO);
		assertEquals(DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE, failedErrorPageDTO.failureReason);

		// register a specific 404 which shouldn't shadow 4xx (b)
		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "d");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE, "404");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/d");
		properties.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
		registrations.add(context.registerService(Servlet.class, new BServlet(), properties));

		failedErrorPageDTO = getFailedErrorPageDTOByName("b");
		assertNull(failedErrorPageDTO);
		failedErrorPageDTO = getFailedErrorPageDTOByName("d");
		assertNull(failedErrorPageDTO);
		errorPageDTO = getErrorPageDTOByName(DEFAULT, "d");
		assertNotNull(errorPageDTO);
		assertEquals(404, errorPageDTO.errorCodes[0]);
	}

}
