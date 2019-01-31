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
import org.osgi.service.http.runtime.dto.DTOConstants;
import org.osgi.service.http.runtime.dto.FailedFilterDTO;
import org.osgi.service.http.runtime.dto.RequestInfoDTO;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

public class Test_table_140_5_HTTP_WHITEBOARD_FILTER_PATTERN extends BaseTest {

	@Test
	public void test_table_140_5_HTTP_WHITEBOARD_FILTER_PATTERN() throws Exception {
		BundleContext context = getBundleContext();

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, new String[] {"", "/"});
		registrations.add(context.registerService(Servlet.class, new MockServlet().content("a"), properties));

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/**");
		ServiceRegistration<?> sr = context.registerService(Filter.class, new MockFilter().around("b"), properties);
		registrations.add(sr);

		FailedFilterDTO failedFilterDTO = getFailedFilterDTOByName("a");
		assertNotNull(failedFilterDTO);
		assertEquals(DTOConstants.FAILURE_REASON_VALIDATION_FAILED, failedFilterDTO.failureReason);

		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/*");
		sr.setProperties(properties);

		failedFilterDTO = getFailedFilterDTOByName("a");
		assertNull(failedFilterDTO);

		RequestInfoDTO requestInfoDTO = calculateRequestInfoDTO("/a");
		assertNotNull(requestInfoDTO);
		assertEquals(1, requestInfoDTO.filterDTOs.length);
		assertEquals("bab", requestAdvisor.request("a"));
		assertEquals("bab", requestAdvisor.request("a.html"));
		assertEquals("bab", requestAdvisor.request("some/path/b.html"));
		assertEquals("bab", requestAdvisor.request(""));

		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "");
		sr.setProperties(properties);

		requestInfoDTO = calculateRequestInfoDTO("/");
		assertNotNull(requestInfoDTO);
		assertEquals(1, requestInfoDTO.filterDTOs.length);
		assertEquals("a", requestAdvisor.request("a"));
		assertEquals("a", requestAdvisor.request("a.html"));
		assertEquals("a", requestAdvisor.request("some/path/b.html"));
		assertEquals("bab", requestAdvisor.request(""));

		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "*.html");
		sr.setProperties(properties);

		requestInfoDTO = calculateRequestInfoDTO("/a.html");
		assertNotNull(requestInfoDTO);
		assertEquals(1, requestInfoDTO.filterDTOs.length);
		assertEquals("a", requestAdvisor.request("a"));
		assertEquals("bab", requestAdvisor.request("a.html"));
		assertEquals("bab", requestAdvisor.request("some/path/b.html"));
		assertEquals("a", requestAdvisor.request(""));
	}

}
