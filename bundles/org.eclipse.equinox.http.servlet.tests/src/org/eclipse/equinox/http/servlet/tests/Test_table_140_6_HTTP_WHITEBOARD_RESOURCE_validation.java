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

public class Test_table_140_6_HTTP_WHITEBOARD_RESOURCE_validation extends BaseTest {

	@Test
	public void test_table_140_6_HTTP_WHITEBOARD_RESOURCE_validation() throws Exception {
		BundleContext context = getBundleContext();

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN, 34l);
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX, "/org/eclipse/equinox/http/servlet/tests");
		ServiceRegistration<Object> sr = context.registerService(Object.class, new Object(), properties);
		registrations.add(sr);

		FailedResourceDTO failedResourceDTO = getFailedResourceDTOByServiceId(getServiceId(sr));
		assertNotNull(failedResourceDTO);
		assertEquals(DTOConstants.FAILURE_REASON_VALIDATION_FAILED, failedResourceDTO.failureReason);

		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN, "/*");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX, 45);
		sr.setProperties(properties);

		failedResourceDTO = getFailedResourceDTOByServiceId(getServiceId(sr));
		assertNotNull(failedResourceDTO);
		assertEquals(DTOConstants.FAILURE_REASON_VALIDATION_FAILED, failedResourceDTO.failureReason);

		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN, "/*");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX, new String[] {"/a", "/b"});
		sr.setProperties(properties);

		failedResourceDTO = getFailedResourceDTOByServiceId(getServiceId(sr));
		assertNotNull(failedResourceDTO);
		assertEquals(DTOConstants.FAILURE_REASON_VALIDATION_FAILED, failedResourceDTO.failureReason);

		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN, new String[] {"/a", "/b"});
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX, "/org/eclipse/equinox/http/servlet/tests/index.txt");
		sr.setProperties(properties);

		failedResourceDTO = getFailedResourceDTOByServiceId(getServiceId(sr));
		assertNull(failedResourceDTO);

		ResourceDTO resourceDTO = getResourceDTOByServiceId(
				DEFAULT,
				getServiceId(sr));
		assertNotNull(resourceDTO);
		assertEquals(2, resourceDTO.patterns.length);
	}

}
