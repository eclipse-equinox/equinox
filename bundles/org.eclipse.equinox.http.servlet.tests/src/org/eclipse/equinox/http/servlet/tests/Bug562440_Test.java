/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.http.servlet.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.equinox.http.servlet.testbase.BaseTest;
import org.junit.Test;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

public class Bug562440_Test extends BaseTest {

	@Test
	public void test_ServletGetStatus() throws Exception {
		final AtomicReference<String> status = new AtomicReference<>();
		final AtomicReference<Boolean> error = new AtomicReference<>(false);
		Servlet servlet = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
				if (error.get()) {
					resp.sendError(404);
				}
				status.set(Integer.toString(resp.getStatus()));
			}
		};

		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "S1");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/S1/*");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet, props));

		requestAdvisor.request("S1/a");

		assertEquals("200", status.get());

		error.set(true);
		try {
			requestAdvisor.request("S1/a");
		} catch (IOException e) {
			assertEquals("404", status.get());
		}
	}

	@Test
	public void test_ServletFlushBuffer() throws Exception {
		final AtomicReference<Boolean> flushBuffer = new AtomicReference<>(false);
		Servlet servlet = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

				resp.sendError(404, "NOT FOUND");
				if (flushBuffer.get()) {
					resp.flushBuffer();
				} else {
					resp.getOutputStream().flush();
				}
			}
		};

		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "S1");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/S1/*");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet, props));

		Map<String, List<String>> response = requestAdvisor.request("S1/a", null);
		assertEquals("404", response.get("responseCode").get(0));

		flushBuffer.set(true);
		response = requestAdvisor.request("S1/a", null);
		assertEquals("404", response.get("responseCode").get(0));
		assertTrue(response.get("responseBody").get(0).contains("NOT FOUND"));
	}
}
