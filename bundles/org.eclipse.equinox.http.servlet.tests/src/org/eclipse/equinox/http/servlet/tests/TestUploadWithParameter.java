/*******************************************************************************
 * Copyright (c) 2020 Dirk Fauth and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Dirk Fauth <dirk.fauth@googlemail.com> - Bug 567831
 ******************************************************************************/

package org.eclipse.equinox.http.servlet.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_MULTIPART_ENABLED;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_MULTIPART_MAXFILESIZE;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.eclipse.equinox.http.servlet.testbase.BaseTest;
import org.junit.Test;

public class TestUploadWithParameter extends BaseTest {

	@Test
	public void testUploadWithParameter() throws Exception {
		final CountDownLatch receivedLatch = new CountDownLatch(1);
		final HashMap<String, Object> contents = new HashMap<>();
		final HashMap<String, String> contentsByKey = new HashMap<>();
		setupUploadWithParameterServlet(receivedLatch, contents, contentsByKey);

		postContentWithParameter(getClass().getResource("resource1.txt"), 201);
		assertTrue(receivedLatch.await(5, TimeUnit.SECONDS));
		assertEquals(2, contents.size());
		assertEquals("Test", contents.get("single"));
		assertNotNull(contents.get("multi"));
		assertTrue(contents.get("multi") instanceof List);

		@SuppressWarnings("unchecked")
		List<String> multi = (List<String>) contents.get("multi");
		assertEquals(3, multi.size());
		assertTrue(multi.contains("One"));
		assertTrue(multi.contains("Two"));
		assertTrue(multi.contains("Three"));

		assertEquals(2, contentsByKey.size());
		assertEquals("Test", contentsByKey.get("single"));
		assertEquals("One", contentsByKey.get("multi"));
	}

	private void setupUploadWithParameterServlet(CountDownLatch receivedLatch, Map<String, Object> contents,
			Map<String, String> contentsByKey) {
		final Dictionary<String, Object> servletProps = new Hashtable<>();
		servletProps.put(HTTP_WHITEBOARD_SERVLET_PATTERN, "/post");
		servletProps.put(HTTP_WHITEBOARD_SERVLET_MULTIPART_ENABLED, Boolean.TRUE);
		servletProps.put(HTTP_WHITEBOARD_SERVLET_MULTIPART_MAXFILESIZE, 1024L);

		@SuppressWarnings("serial")
		final Servlet uploadServlet = new HttpServlet() {
			@Override
			protected void doPost(HttpServletRequest req, HttpServletResponse resp)
					throws IOException, ServletException {

				try {
					// check if the multi values are sent as post parameter in the multipart request
					ArrayList<String> collected = new ArrayList<>();
					for (Part supportPart : req.getParts()) {
						if (supportPart.getName().equals("multi")) {
							try (BufferedReader reader = new BufferedReader(
									new InputStreamReader(supportPart.getInputStream()))) {
								List<String> collect = reader.lines().collect(Collectors.toList());
								if (collect != null && !collect.isEmpty()) {
									collected.addAll(collect);
								}
							}
						} else if (supportPart.getName().equals("single")) {
							try (BufferedReader reader = new BufferedReader(
									new InputStreamReader(supportPart.getInputStream()))) {
								contents.put("single", reader.readLine());
							}
						}
					}
					if (!collected.isEmpty()) {
						contents.put("multi", collected);
					}

					try (BufferedReader reader = new BufferedReader(
							new InputStreamReader(req.getPart("single").getInputStream()))) {
						contentsByKey.put("single", reader.readLine());
					}
					try (BufferedReader reader = new BufferedReader(
							new InputStreamReader(req.getPart("multi").getInputStream()))) {
						contentsByKey.put("multi", reader.readLine());
					}

					resp.setStatus(201);
				} finally {
					receivedLatch.countDown();
				}

			}
		};

		long before = this.getHttpRuntimeChangeCount();
		registrations.add(getBundleContext().registerService(Servlet.class.getName(), uploadServlet, servletProps));
		this.waitForRegistration(before);
	}

	private void postContentWithParameter(final URL resource, final int expectedRT) throws IOException {
		Map<String, List<Object>> header = new LinkedHashMap<>();
		header.put("method", Arrays.<Object>asList("POST"));
		header.put("text.txt", Arrays.<Object>asList(resource));

		Map<String, Object> formFields = new LinkedHashMap<>();
		formFields.put("single", "Test");
		formFields.put("multi", Arrays.asList("One", "Two", "Three"));

		Map<String, List<String>> result = requestAdvisor.upload("post", header, formFields);

		assertEquals(expectedRT + "", result.get("responseCode").get(0));
	}

}
