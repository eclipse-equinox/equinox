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

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;

import org.eclipse.equinox.http.servlet.testbase.BaseTest;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.http.runtime.dto.RequestInfoDTO;
import org.osgi.service.http.runtime.dto.ServletContextDTO;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

public class Test_140_2_26to27 extends BaseTest {

	@SuppressWarnings("serial")
	@Test
	public void test_140_2_26to27() throws Exception {
		BundleContext context = getBundleContext();

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "foo");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/foo");
		registrations.add(context.registerService(ServletContextHelper.class, new ServletContextHelper() {}, properties));

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "foobar");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/foo");
		properties.put(Constants.SERVICE_RANKING, new Integer(Integer.MAX_VALUE));
		registrations.add(context.registerService(ServletContextHelper.class, new ServletContextHelper() {}, properties));

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(osgi.http.whiteboard.context.name=foo)");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "first");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/bar/someServlet");
		registrations.add(context.registerService(Servlet.class, new HttpServlet() {}, properties));

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(osgi.http.whiteboard.context.name=foobar)");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "second");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/bar/someServlet");
		registrations.add(context.registerService(Servlet.class, new HttpServlet() {}, properties));

		ServletContextDTO servletContextDTO = getServletContextDTOByName("foobar");

		assertNotNull(servletContextDTO);

		HttpServiceRuntime httpServiceRuntime = getHttpServiceRuntime();

		RequestInfoDTO requestInfoDTO = httpServiceRuntime.calculateRequestInfoDTO("/foo/bar/someServlet");

		assertNotNull(requestInfoDTO);
		assertNotNull(requestInfoDTO.servletDTO);
		assertEquals(servletContextDTO.serviceId, requestInfoDTO.servletDTO.servletContextId);
		assertEquals("second", requestInfoDTO.servletDTO.name);
	}

}
