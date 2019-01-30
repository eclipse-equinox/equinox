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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.AsyncContext;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.equinox.http.servlet.testbase.BaseTest;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.runtime.dto.RequestInfoDTO;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

public class Test_table_140_4_HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED extends BaseTest {

	@Test
	public void test_table_140_4_HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED() throws Exception {
		BundleContext context = getBundleContext();
		final AtomicBoolean invoked = new AtomicBoolean(false);

		@SuppressWarnings("serial")
		class AServlet extends HttpServlet {

			final ExecutorService	executor	= Executors.newCachedThreadPool();

			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
				doGetAsync(req.startAsync());
			}

			private void doGetAsync(final AsyncContext asyncContext) {
				executor.submit(new Callable<Void>() {
					@Override
					public Void call() throws Exception {
						try {
							invoked.set(true);

							PrintWriter writer = asyncContext.getResponse().getWriter();

							writer.print("a");
						} finally {
							asyncContext.complete();
						}

						return null;
					}
				});
			}

		}

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED, "true");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/a");
		ServiceRegistration<Servlet> srA = context.registerService(Servlet.class, new AServlet(), properties);
		registrations.add(srA);

		RequestInfoDTO requestInfoDTO = calculateRequestInfoDTO("/a");

		assertNotNull(requestInfoDTO);
		assertNotNull(requestInfoDTO.servletDTO);
		assertTrue(requestInfoDTO.servletDTO.asyncSupported);
		assertTrue(getServiceId(srA) == requestInfoDTO.servletDTO.serviceId);
		assertEquals("a", requestInfoDTO.servletDTO.name);
		assertEquals("a", requestAdvisor.request("a"));
		assertTrue(invoked.get());
		invoked.set(false);

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED, "false");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "b");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/b");
		ServiceRegistration<Servlet> srB = context.registerService(Servlet.class, new AServlet(), properties);
		registrations.add(srB);

		assertEquals("500", requestAdvisor.request("b", null).get("responseCode").get(0));
		assertFalse(invoked.get());
	}

}
