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

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.equinox.http.servlet.testbase.BaseTest;
import org.eclipse.equinox.http.servlet.tests.util.MockFilter;
import org.eclipse.equinox.http.servlet.tests.util.MockServlet;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.runtime.dto.FilterDTO;
import org.osgi.service.http.runtime.dto.RequestInfoDTO;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

public class Test_table_140_5_HTTP_WHITEBOARD_FILTER_DISPATCHER_error extends BaseTest {

	@Test
	public void test_table_140_5_HTTP_WHITEBOARD_FILTER_DISPATCHER_error() throws Exception {
		BundleContext context = getBundleContext();

		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_DISPATCHER, "ERROR");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_SERVLET, "a");
		ServiceRegistration<?> srA = context.registerService(Filter.class, new MockFilter().around("b"), properties);
		registrations.add(srA);

		FilterDTO filterDTO = getFilterDTOByName(DEFAULT, "a");
		assertNotNull(filterDTO);
		assertEquals(1, filterDTO.dispatcher.length);
		assertEquals("ERROR", filterDTO.dispatcher[0]);

		properties = new Hashtable<>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE, "4xx");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/a");
		ServiceRegistration<?> srB = context.registerService(Servlet.class, new MockServlet().content("a"), properties);
		registrations.add(srB);

		RequestInfoDTO requestInfoDTO = calculateRequestInfoDTO("/a");
		assertNotNull(requestInfoDTO);
		assertEquals(0, requestInfoDTO.filterDTOs.length);
		assertEquals("a", requestAdvisor.request("a"));

		@SuppressWarnings("serial")
		MockServlet mockServlet = new MockServlet() {

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
			}

		};

		properties = new Hashtable<>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "b");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/b");
		ServiceRegistration<?> srC = context.registerService(Servlet.class, mockServlet, properties);
		registrations.add(srC);

		Map<String, List<String>> response = requestAdvisor.request("b", null);
		assertEquals("bab", response.get("responseBody").get(0));
	}

}
