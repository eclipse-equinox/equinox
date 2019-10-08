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
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.eclipse.equinox.http.servlet.testbase.BaseTest;
import org.eclipse.equinox.http.servlet.tests.util.MockPreprocessor;
import org.eclipse.equinox.http.servlet.tests.util.MockServlet;
import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.runtime.dto.PreprocessorDTO;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.service.http.whiteboard.Preprocessor;

public class PreprocessorTestCase extends BaseTest {

	@Test
	public void testPreprocessorInitParameters() {
		Dictionary<String,Object> properties = new Hashtable<>();
		properties
				.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_PREPROCESSOR_INIT_PARAM_PREFIX
						+ "param1", "value1");
		properties
				.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_PREPROCESSOR_INIT_PARAM_PREFIX
						+ "param2", "value2");
		properties
				.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_PREPROCESSOR_INIT_PARAM_PREFIX
						+ "param3", 345l);

		long before = this.getHttpRuntimeChangeCount();
		final ServiceRegistration<Preprocessor> reg = getBundleContext()
				.registerService(Preprocessor.class, new MockPreprocessor(),
						properties);
		registrations.add(reg);
		this.waitForRegistration(before);

		final PreprocessorDTO[] dtos = this.getHttpServiceRuntime()
				.getRuntimeDTO().preprocessorDTOs;
		assertEquals(1, dtos.length);

		assertTrue(dtos[0].initParams.containsKey("param1"));
		assertTrue(dtos[0].initParams.containsKey("param2"));
		assertFalse(dtos[0].initParams.containsKey("param3"));
		assertEquals(getServiceId(reg), dtos[0].serviceId);
	}

	@Test
	public void testPreprocessorRanking() throws Exception {
		// register preprocessor with ranking -5
		Dictionary<String,Object> properties = new Hashtable<>();
		properties.put(Constants.SERVICE_RANKING, -5);

		long before = this.getHttpRuntimeChangeCount();
		registrations
				.add(getBundleContext().registerService(Preprocessor.class.getName(),
						new MockPreprocessor().around("d"), properties));
		before = this.waitForRegistration(before);

		// register preprocessor with ranking 8
		properties = new Hashtable<>();
		properties.put(Constants.SERVICE_RANKING, 8);

		registrations
				.add(getBundleContext().registerService(Preprocessor.class.getName(),
						new MockPreprocessor().around("a"), properties));
		before = this.waitForRegistration(before);

		// register preprocessor with invalid ranking
		properties = new Hashtable<>();
		properties.put(Constants.SERVICE_RANKING, 3L); // this is invalid ->
														// ranking = 0

		registrations
				.add(getBundleContext().registerService(Preprocessor.class.getName(),
						new MockPreprocessor().around("b"), properties));
		before = this.waitForRegistration(before);

		// register preprocessor with no ranking
		properties = new Hashtable<>();

		registrations
				.add(getBundleContext().registerService(Preprocessor.class.getName(),
						new MockPreprocessor().around("c"), properties));
		before = this.waitForRegistration(before);

		// check that we have four preprocessors
		final PreprocessorDTO[] dtos = this.getHttpServiceRuntime()
				.getRuntimeDTO().preprocessorDTOs;
		assertEquals(4, dtos.length);

		// register endpoint
		properties = new Hashtable<>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN,
				"/available");
		registrations.add(getBundleContext().registerService(
				Servlet.class, new MockServlet().content("hello"), properties));

		assertEquals("abcdhellodcba", requestAdvisor.request("available"));
	}

	/**
	 * Test a request with a servlet registered at that url and check if the
	 * preprocessor is invoked. Do the same with a non existing url.
	 */
	@Test
	public void testPreprocessorInvocation() throws Exception {
		// register preprocessor
		final List<String> filterActions = new ArrayList<>();
		long before = this.getHttpRuntimeChangeCount();
		registrations.add(getBundleContext().registerService(
				Preprocessor.class.getName(), new MockPreprocessor() {

					@Override
					public void doFilter(ServletRequest request,
							ServletResponse response, FilterChain chain)
							throws IOException, ServletException {
						filterActions.add("a");
						super.doFilter(request, new HttpServletResponseWrapper(
								(HttpServletResponse) response) {

							private boolean hasStatus = false;

							private void addStatus(final int sc) {
								if (!hasStatus) {
									hasStatus = true;
									filterActions.add(String.valueOf(sc));
								}
							}

							@Override
							public void setStatus(int sc) {
								addStatus(sc);
								super.setStatus(sc);
							}

							@Override
							public void sendError(int sc, String msg)
									throws IOException {
								addStatus(sc);
								super.sendError(sc, msg);
							}

							@Override
							public void sendError(int sc) throws IOException {
								addStatus(sc);
								super.sendError(sc);
							}

						}, chain);
						filterActions.add("b");
					}

				}, null));
		before = this.waitForRegistration(before);

		// register endpoint
		Dictionary<String,Object> properties = new Hashtable<>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN,
				"/available");
		registrations.add(getBundleContext().registerService(
				Servlet.class, new MockServlet().content("hello"), properties));

		assertEquals("hello", requestAdvisor.request("available"));
		assertEquals(2, filterActions.size());
		assertEquals("a", filterActions.get(0));
		assertEquals("b", filterActions.get(1));

		// request a non existing pattern - this will somehow set the status
		// code to 404
		filterActions.clear();
		requestAdvisor.request("foo", null);
		assertEquals(3, filterActions.size());
		assertEquals("a", filterActions.get(0));
		assertEquals("404", filterActions.get(1));
		assertEquals("b", filterActions.get(2));
	}

}
