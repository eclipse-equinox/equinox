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
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;

import org.eclipse.equinox.http.servlet.testbase.BaseTest;
import org.eclipse.equinox.http.servlet.tests.util.MockSCL;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.runtime.dto.ServletContextDTO;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

public class Test_140_9_ServletContextDTO_custom_listener extends BaseTest {

	@Test
	public void test_140_9_ServletContextDTO_custom_listener() {
		BundleContext context = getBundleContext();
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "a");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/a");
		registrations.add(context.registerService(ServletContextHelper.class, new ServletContextHelper() {}, properties));

		ServletContextDTO servletContextDTO = getServletContextDTOByName("a");
		assertNotNull(servletContextDTO);

		AtomicReference<ServletContext> sc1 = new AtomicReference<>();

		properties = new Hashtable<>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(osgi.http.whiteboard.context.name=a)");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER, "true");
		ServiceRegistration<?> sr = context.registerService(ServletContextListener.class, new MockSCL(sc1), properties);
		registrations.add(sr);

		servletContextDTO = getServletContextDTOByName("a");
		assertEquals(1, servletContextDTO.listenerDTOs.length);
		assertEquals(getServiceId(sr), servletContextDTO.listenerDTOs[0].serviceId);
	}

}
