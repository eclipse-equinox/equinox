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
import java.io.PrintWriter;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.equinox.http.servlet.testbase.BaseTest;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.runtime.dto.FailedServletDTO;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

public class Test_140_4_14to15 extends BaseTest {

	@Test
	public void test_140_4_14to15() throws Exception {
		BundleContext context = getBundleContext();

		final AtomicBoolean invoked = new AtomicBoolean(false);

		@SuppressWarnings("serial")
		Servlet servlet = new HttpServlet() {

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response)
					throws IOException {

				invoked.set(true);

				PrintWriter writer = response.getWriter();
				writer.write((request.getContextPath() == null) ? "" : request.getContextPath());
				writer.write(":");
				writer.write((request.getServletPath() == null) ? "" : request.getServletPath());
				writer.write(":");
				writer.write((request.getPathInfo() == null) ? "" : request.getPathInfo());
			}

		};

		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/");
		registrations.add(context.registerService(Servlet.class, servlet, properties));

		FailedServletDTO[] failedServletDTOs = getFailedServletDTOs();

		assertEquals(0, failedServletDTOs.length);

		assertEquals(":/a.html:", requestAdvisor.request("a.html"));
		assertTrue(invoked.get());
		invoked.set(false);
		assertEquals(":/a.xhtml:", requestAdvisor.request("a.xhtml"));
		assertTrue(invoked.get());
		invoked.set(false);
		assertEquals(":/some/path/a.xhtml:", requestAdvisor.request("some/path/a.xhtml"));
		assertTrue(invoked.get());
	}

}
