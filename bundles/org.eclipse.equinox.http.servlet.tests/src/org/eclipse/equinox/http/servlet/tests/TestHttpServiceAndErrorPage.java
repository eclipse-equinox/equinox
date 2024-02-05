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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.Servlet;

import org.eclipse.equinox.http.servlet.testbase.BaseTest;
import org.eclipse.equinox.http.servlet.tests.util.MockServlet;
import org.junit.Test;
import org.osgi.service.http.runtime.dto.ServletContextDTO;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

public class TestHttpServiceAndErrorPage extends BaseTest {

	/**
	 * Registration of error page with http service (allowed) and error page and
	 * pattern with http service (not allowed)
	 */
	@Test
	public void testHttpServiceAndErrorPage() throws Exception {
		registerDummyServletInHttpService();
		try {
			final String name1 = "testname1";
			final String name2 = "testname2";
			Dictionary<String, Object> properties = new Hashtable<>();
			properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE, name1);
			properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
					"(" + HttpWhiteboardConstants.HTTP_SERVICE_CONTEXT_PROPERTY + "=*)");
			long before = getHttpRuntimeChangeCount();
			registrations.add(getBundleContext().registerService(Servlet.class, new MockServlet(), properties));
			before = waitForRegistration(before);

			properties = new Hashtable<>();
			properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE, name2);
			properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/" + name2);
			properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
					"(" + HttpWhiteboardConstants.HTTP_SERVICE_CONTEXT_PROPERTY + "=*)");
			registrations.add(getBundleContext().registerService(Servlet.class, new MockServlet(), properties));
			before = waitForRegistration(before);

			assertNull(getFailedErrorPageDTOByException(name1));
			assertNotNull("" + getHttpServiceRuntime().getRuntimeDTO(), getFailedErrorPageDTOByException(name2));

			final ServletContextDTO scDTO = getServletContextDTOForDummyServlet();
			assertNotNull(getErrorPageDTOByException(scDTO.name, name1));
			assertNull(getErrorPageDTOByException(scDTO.name, name2));
		} finally {
			unregisterDummyServletFromHttpService();
		}
	}

}
