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

import org.eclipse.equinox.http.servlet.testbase.BaseTest;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.runtime.dto.DTOConstants;
import org.osgi.service.http.runtime.dto.FailedResourceDTO;
import org.osgi.service.http.runtime.dto.ResourceDTO;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

public class Test_140_6_20to21_commonProperties extends BaseTest {

	@Test
	public void test_140_6_20to21_commonProperties() throws Exception {
		BundleContext context = getBundleContext();

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN, "/other.txt");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX, "/org/eclipse/equinox/http/servlet/tests/index.txt");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(osgi.http.whiteboard.context.name=foo)");
		ServiceRegistration<Object> sr = context.registerService(Object.class, new Object(), properties);
		registrations.add(sr);

		FailedResourceDTO failedResourceDTO = getFailedResourceDTOByServiceId(getServiceId(sr));
		assertNotNull(failedResourceDTO);
		assertEquals(DTOConstants.FAILURE_REASON_NO_SERVLET_CONTEXT_MATCHING, failedResourceDTO.failureReason);

		ResourceDTO resourceDTO = getResourceDTOByServiceId(
				DEFAULT,
				getServiceId(sr));
		assertNull(resourceDTO);
		assertEquals("404", requestAdvisor.request("other.txt", null).get("responseCode").get(0));

		properties.remove(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT);
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET, "(some=foo)");
		sr.setProperties(properties);

		failedResourceDTO = getFailedResourceDTOByServiceId(getServiceId(sr));
		assertNull(failedResourceDTO);

		resourceDTO = getResourceDTOByServiceId(
				DEFAULT,
				getServiceId(sr));
		assertNull(resourceDTO);
		assertEquals("404", requestAdvisor.request("other.txt", null).get("responseCode").get(0));
	}

}
