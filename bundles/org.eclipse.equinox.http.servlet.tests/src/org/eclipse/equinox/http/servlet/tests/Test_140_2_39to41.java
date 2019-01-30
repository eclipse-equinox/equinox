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

import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.equinox.http.servlet.testbase.BaseTest;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.runtime.dto.DTOConstants;
import org.osgi.service.http.runtime.dto.FailedServletContextDTO;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

public class Test_140_2_39to41 extends BaseTest {

	@Test
	public void test_140_2_39to41() throws Exception {
		BundleContext context = getBundleContext();

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "foo");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/foo");
		registrations.add(context.registerService(ServletContextHelper.class, new ServletContextHelper() {}, properties));

		properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "foo");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/foo/bar");
		properties.put(Constants.SERVICE_RANKING, new Integer(1000));
		registrations.add(context.registerService(ServletContextHelper.class, new ServletContextHelper() {}, properties));

		FailedServletContextDTO failedServletContextDTO = getFailedServletContextDTOByName("foo");

		assertNotNull(failedServletContextDTO);
		assertEquals(
				DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE,
				failedServletContextDTO.failureReason);
	}

}
