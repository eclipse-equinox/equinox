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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;

import org.eclipse.equinox.http.servlet.testbase.BaseTest;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.runtime.dto.RequestInfoDTO;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

public class Test_table_140_4_HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED_validate extends BaseTest {

	@SuppressWarnings("serial")
	@Test
	public void test_table_140_4_HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED_validate() throws Exception {
		BundleContext context = getBundleContext();

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED, "blah");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/a");
		registrations.add(context.registerService(Servlet.class, new HttpServlet() {}, properties));

		RequestInfoDTO requestInfoDTO = calculateRequestInfoDTO("/a");

		assertNotNull(requestInfoDTO);
		assertNotNull(requestInfoDTO.servletDTO);
		assertFalse(requestInfoDTO.servletDTO.asyncSupported);

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED, "true");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/b");
		registrations.add(context.registerService(Servlet.class, new HttpServlet() {}, properties));

		requestInfoDTO = calculateRequestInfoDTO("/b");

		assertNotNull(requestInfoDTO);
		assertNotNull(requestInfoDTO.servletDTO);
		assertTrue(requestInfoDTO.servletDTO.asyncSupported);

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED, "false");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/c");
		registrations.add(context.registerService(Servlet.class, new HttpServlet() {}, properties));

		requestInfoDTO = calculateRequestInfoDTO("/c");

		assertNotNull(requestInfoDTO);
		assertNotNull(requestInfoDTO.servletDTO);
		assertFalse(requestInfoDTO.servletDTO.asyncSupported);

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED, 234l);
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/d");
		registrations.add(context.registerService(Servlet.class, new HttpServlet() {}, properties));

		requestInfoDTO = calculateRequestInfoDTO("/d");

		assertNotNull(requestInfoDTO);
		assertNotNull(requestInfoDTO.servletDTO);
		assertFalse(requestInfoDTO.servletDTO.asyncSupported);

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/e");
		registrations.add(context.registerService(Servlet.class, new HttpServlet() {}, properties));

		requestInfoDTO = calculateRequestInfoDTO("/e");

		assertNotNull(requestInfoDTO);
		assertNotNull(requestInfoDTO.servletDTO);
		assertFalse(requestInfoDTO.servletDTO.asyncSupported);
	}

}
