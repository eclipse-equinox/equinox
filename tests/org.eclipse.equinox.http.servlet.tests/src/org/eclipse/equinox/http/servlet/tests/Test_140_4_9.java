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
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.equinox.http.servlet.testbase.BaseTest;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

public class Test_140_4_9 extends BaseTest {

	@Test
	public void test_140_4_9() throws Exception {
		BundleContext context = getBundleContext();

		final AtomicBoolean invoked = new AtomicBoolean(false);

		@SuppressWarnings("serial")
		Servlet servlet = new HttpServlet() {

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response)
					throws IOException {

				invoked.set(true);

				response.getWriter().write("a");
			}

		};

		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/*");
		registrations.add(context.registerService(Servlet.class, servlet, properties));

		assertEquals("a", requestAdvisor.request("a"));
		assertTrue(invoked.get());
		invoked.set(false);
		assertEquals("a", requestAdvisor.request("b.html"));
		assertTrue(invoked.get());
		invoked.set(false);
		assertEquals("a", requestAdvisor.request("some/path/b.html"));
		assertTrue(invoked.get());
		assertEquals("a", requestAdvisor.request(""));
	}

}
