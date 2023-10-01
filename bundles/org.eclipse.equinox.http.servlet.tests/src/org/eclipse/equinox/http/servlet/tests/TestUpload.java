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
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_MULTIPART_ENABLED;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_MULTIPART_MAXFILESIZE;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.eclipse.equinox.http.servlet.testbase.BaseTest;
import org.junit.Test;

public class TestUpload extends BaseTest {

	@Test
	public void testUpload() throws Exception {
		final CountDownLatch receivedLatch = new CountDownLatch(1);
		final Map<String,Long> contents = new HashMap<>();
		setupUploadServlet(receivedLatch, contents);

		postContent(getClass().getResource("resource1.txt"), 201);
		assertTrue(receivedLatch.await(5, TimeUnit.SECONDS));
		assertEquals(1, contents.size());
		assertEquals(26L, (long) contents.get("text.txt"));
	}

	private void setupUploadServlet(final CountDownLatch receivedLatch,
			final Map<String,Long> contents) {
		final Dictionary<String,Object> servletProps = new Hashtable<>();
		servletProps.put(HTTP_WHITEBOARD_SERVLET_PATTERN, "/post");
		servletProps.put(HTTP_WHITEBOARD_SERVLET_MULTIPART_ENABLED,
				Boolean.TRUE);
		servletProps.put(HTTP_WHITEBOARD_SERVLET_MULTIPART_MAXFILESIZE, 1024L);

		@SuppressWarnings("serial")
		final Servlet uploadServlet = new HttpServlet() {
			@Override
			protected void doPost(HttpServletRequest req,
					HttpServletResponse resp)
					throws IOException, ServletException {
				try {
					final Collection<Part> parts = req.getParts();
					for (final Part p : parts) {
						contents.put(p.getName(), p.getSize());
					}
					resp.setStatus(201);
				} finally {
					receivedLatch.countDown();
				}

			}
		};

		long before = this.getHttpRuntimeChangeCount();
		registrations.add(getBundleContext().registerService(
				Servlet.class.getName(), uploadServlet, servletProps));
		this.waitForRegistration(before);
	}

	private void postContent(final URL resource, final int expectedRT) throws IOException {
		Map<String, List<Object>> map = new HashMap<>();

		map.put("method", Arrays.<Object>asList("POST"));
		map.put("text.txt", Arrays.<Object>asList(resource));

		Map<String, List<String>> result = requestAdvisor.upload("post", map);

		assertEquals(expectedRT + "", result.get("responseCode").get(0));
	}

}
