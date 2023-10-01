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
import static org.junit.Assert.assertNull;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.Servlet;

import org.eclipse.equinox.http.servlet.testbase.BaseTest;
import org.eclipse.equinox.http.servlet.tests.util.MockServlet;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.runtime.dto.DTOConstants;
import org.osgi.service.http.runtime.dto.FailedResourceDTO;
import org.osgi.service.http.runtime.dto.ResourceDTO;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

public class Test_140_6_1 extends BaseTest {

	@Test
	public void test_140_6_1() throws Exception {
		BundleContext context = getBundleContext();

		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/*");
		registrations.add(context.registerService(Servlet.class, new MockServlet().content("b"), properties));

		properties = new Hashtable<>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN, "/*");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX, "/org/eclipse/equinox/http/servlet/tests");
		ServiceRegistration<Object> sr = context.registerService(Object.class, new Object(), properties);
		registrations.add(sr);

		FailedResourceDTO failedResourceDTO = getFailedResourceDTOByServiceId(getServiceId(sr));
		assertNotNull(failedResourceDTO);
		assertEquals(DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE, failedResourceDTO.failureReason);

		ResourceDTO resourceDTO = getResourceDTOByServiceId(
				DEFAULT,
				getServiceId(sr));
		assertNull(resourceDTO);
		assertEquals("b", requestAdvisor.request("index.txt"));

		properties.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
		sr.setProperties(properties);

		resourceDTO = getResourceDTOByServiceId(
				DEFAULT,
				getServiceId(sr));
		assertNotNull(resourceDTO);
		assertEquals("a\n", requestAdvisor.request("index.txt"));

		failedResourceDTO = getFailedResourceDTOByServiceId(getServiceId(sr));
		assertNull(failedResourceDTO);
	}

}
