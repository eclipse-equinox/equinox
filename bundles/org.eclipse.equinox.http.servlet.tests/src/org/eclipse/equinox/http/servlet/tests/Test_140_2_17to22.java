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

import static org.junit.Assert.assertNull;

import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;

import org.eclipse.equinox.http.servlet.testbase.BaseTest;
import org.eclipse.equinox.http.servlet.tests.util.MockSCL;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.service.FindHook;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

public class Test_140_2_17to22 extends BaseTest {

	@Test
	public void test_140_2_17to22() throws Exception {
		final BundleContext context = getBundleContext();

		FindHook findHook = new FindHook() {

			@Override
			public void find(
					BundleContext bundleContext, String name, String filter,
					boolean allServices, Collection<ServiceReference<?>> references) {

				if (bundleContext != context) {
					return;
				}

				// don't show default ServletContextHelper
				for (Iterator<ServiceReference<?>> iterator = references.iterator(); iterator.hasNext();) {
					ServiceReference<?> sr = iterator.next();

					if (DEFAULT.equals(sr.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME))) {
						iterator.remove();
					}
				}
			}

		};

		registrations.add(context.registerService(FindHook.class, findHook, null));

		AtomicReference<ServletContext> sc1 = new AtomicReference<ServletContext>();

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER, "true");
		ServiceRegistration<ServletContextListener> serviceRegistration = context.registerService(ServletContextListener.class, new MockSCL(sc1), properties);
		registrations.add(serviceRegistration);

		assertNull(sc1.get());
	}

}
