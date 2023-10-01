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

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.equinox.http.servlet.testbase.BaseTest;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.runtime.dto.RequestInfoDTO;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

public class Test_140_4_26to31 extends BaseTest {

	@Test
	public void test_140_4_26to31() throws Exception {
		BundleContext context = getBundleContext();

		@SuppressWarnings("serial")
		class AServlet extends HttpServlet {

			public AServlet(String content) {
				this.content = content;
			}

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {

				response.getWriter().write(content);
			}

			private final String content;

		}

		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/a");
		// properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
		// "(osgi.http.whiteboard.context.name=org_eclipse_equinox_http_servlet_internal_HttpServiceImpl_DefaultHttpContext-0)");
		properties.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
		ServiceRegistration<Servlet> srA = context.registerService(Servlet.class, new AServlet("a"), properties);
		registrations.add(srA);

		RequestInfoDTO requestInfoDTO = calculateRequestInfoDTO("/a");

		assertNotNull(requestInfoDTO);
		assertNotNull(requestInfoDTO.servletDTO);
		assertEquals("a", requestInfoDTO.servletDTO.name);
		assertEquals(getServiceId(srA), requestInfoDTO.servletDTO.serviceId);
		assertEquals("a", requestAdvisor.request("a"));

		HttpService httpService = getHttpService();

		if (httpService == null) {
			return;
		}

		httpService.registerServlet("/a", new AServlet("b"), null, null);

		try {
			requestInfoDTO = calculateRequestInfoDTO("/a");

			assertNotNull(requestInfoDTO);
			assertNotNull(requestInfoDTO.servletDTO);
			assertFalse(getServiceId(srA) == requestInfoDTO.servletDTO.serviceId);
			assertEquals("b", requestAdvisor.request("a"));
		} finally {
			httpService.unregister("/a");
		}
	}

}
