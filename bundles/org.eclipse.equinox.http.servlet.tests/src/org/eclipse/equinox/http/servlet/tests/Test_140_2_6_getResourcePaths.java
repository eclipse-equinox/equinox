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
import static org.junit.Assert.assertTrue;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;

import org.eclipse.equinox.http.servlet.testbase.BaseTest;
import org.eclipse.equinox.http.servlet.tests.util.MockSCL;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

public class Test_140_2_6_getResourcePaths extends BaseTest {

	@Test
	public void test_140_2_6_getResourcePaths() {
		BundleContext context = getBundleContext();

		final AtomicBoolean invoked = new AtomicBoolean(false);

		ServletContextHelper servletContextHelper = new ServletContextHelper() {

			@Override
			public Set<String> getResourcePaths(String path) {
				invoked.set(true);

				return null;
			}

		};

		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "context1");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/context1");
		registrations.add(context.registerService(ServletContextHelper.class, servletContextHelper, properties));

		AtomicReference<ServletContext> sc1 = new AtomicReference<>();

		properties = new Hashtable<>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER, "true");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(osgi.http.whiteboard.context.name=context1)");
		registrations.add(context.registerService(ServletContextListener.class, new MockSCL(sc1), properties));

		ServletContext servletContext = sc1.get();

		assertNotNull(servletContext);

		servletContext.getResourcePaths("/META-INF/");

		assertTrue(invoked.get());
	}

}
