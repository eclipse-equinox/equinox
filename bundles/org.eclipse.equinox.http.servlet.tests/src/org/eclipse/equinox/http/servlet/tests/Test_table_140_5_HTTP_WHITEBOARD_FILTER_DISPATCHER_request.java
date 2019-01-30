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

import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.Filter;
import javax.servlet.Servlet;

import org.eclipse.equinox.http.servlet.testbase.BaseTest;
import org.eclipse.equinox.http.servlet.tests.util.MockFilter;
import org.eclipse.equinox.http.servlet.tests.util.MockServlet;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.runtime.dto.FilterDTO;
import org.osgi.service.http.runtime.dto.RequestInfoDTO;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

public class Test_table_140_5_HTTP_WHITEBOARD_FILTER_DISPATCHER_request extends BaseTest {

	@Test
	public void test_table_140_5_HTTP_WHITEBOARD_FILTER_DISPATCHER_request() throws Exception {
		BundleContext context = getBundleContext();

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/a");
		ServiceRegistration<?> srA = context.registerService(Filter.class, new MockFilter().around("b"), properties);
		registrations.add(srA);

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/a");
		ServiceRegistration<?> srB = context.registerService(Servlet.class, new MockServlet().content("a"), properties);
		registrations.add(srB);

		RequestInfoDTO requestInfoDTO = calculateRequestInfoDTO("/a");
		assertNotNull(requestInfoDTO);
		assertEquals(1, requestInfoDTO.filterDTOs.length);
		FilterDTO filterDTO = requestInfoDTO.filterDTOs[0];
		assertEquals(getServiceId(srA), filterDTO.serviceId);
		assertEquals(1, filterDTO.dispatcher.length);
		assertEquals("REQUEST", filterDTO.dispatcher[0]);

		assertEquals("bab", requestAdvisor.request("a"));

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_DISPATCHER, "REQUEST");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/a");
		srA.setProperties(properties);

		assertEquals("bab", requestAdvisor.request("a"));
	}

}
