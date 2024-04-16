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

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.equinox.http.servlet.testbase.BaseTest;
import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.runtime.dto.DTOConstants;
import org.osgi.service.http.runtime.dto.FailedServletDTO;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

public class Test_140_4_17to22 extends BaseTest {

	@Test
	public void test_140_4_17to22() throws Exception {
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
		ServiceRegistration<Servlet> srA = getBundleContext().registerService(Servlet.class, new AServlet("a"),
				properties);
		registrations.add(srA);

		assertEquals("a", requestAdvisor.request("a"));

		properties = new Hashtable<>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "b");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/a");
		ServiceRegistration<Servlet> srB = getBundleContext().registerService(Servlet.class, new AServlet("b"),
				properties);
		registrations.add(srB);

		assertEquals("a", requestAdvisor.request("a"));

		FailedServletDTO failedServletDTO = getFailedServletDTOByName("b");

		assertNotNull(failedServletDTO);
		assertEquals(DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE, failedServletDTO.failureReason);
		assertEquals(getServiceId(srB), failedServletDTO.serviceId);

		properties = new Hashtable<>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "c");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/a");
		properties.put(Constants.SERVICE_RANKING, 1000);
		registrations.add(getBundleContext().registerService(Servlet.class, new AServlet("c"), properties));

		assertEquals("c", requestAdvisor.request("a"));

		failedServletDTO = getFailedServletDTOByName("a");

		assertNotNull(failedServletDTO);
		assertEquals(DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE, failedServletDTO.failureReason);
		assertEquals(getServiceId(srA), failedServletDTO.serviceId);
	}

}
