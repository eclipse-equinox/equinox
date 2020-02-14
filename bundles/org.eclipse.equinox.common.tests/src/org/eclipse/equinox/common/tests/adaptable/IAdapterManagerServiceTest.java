/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *     Alexander Kurtakov <akurtako@redhat.com> - bug 458490
 *******************************************************************************/
package org.eclipse.equinox.common.tests.adaptable;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.IAdapterManager;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Tests API on the IAdapterManager class accessed via an OSGi service.
 *
 * This class is a copy of IAdapterManagerTest modified to use an OSGi service
 * instead of the Platform API.
 */
public class IAdapterManagerServiceTest {
	private static final String NON_EXISTING = "com.does.not.Exist";
	private static final String TEST_ADAPTER = "org.eclipse.equinox.common.tests.adaptable.TestAdapter";

	private static IAdapterManager manager;

	/*
	 * Return the framework log service, if available.
	 */
	@BeforeClass
	public static void getAdapterManager() {
		BundleContext context = FrameworkUtil.getBundle(IAdapterManagerServiceTest.class).getBundleContext();
		ServiceTracker<IAdapterManager, IAdapterManager> adapterManagerTracker = new ServiceTracker<>(context,
				IAdapterManager.class, null);
		adapterManagerTracker.open();
		manager = adapterManagerTracker.getService();
		adapterManagerTracker.close();
	}

	/**
	 * Tests API method IAdapterManager.hasAdapter.
	 */
	@Test
	public void testHasAdapter() {
		TestAdaptable adaptable = new TestAdaptable();
		// request non-existing adaptable
		assertFalse("1.0", manager.hasAdapter("", NON_EXISTING));

		// request adapter that is in XML but has no registered factory
		assertTrue("1.1", manager.hasAdapter(adaptable, TEST_ADAPTER));

		// request adapter that is not in XML
		assertFalse("1.2", manager.hasAdapter(adaptable, "java.lang.String"));

		// register an adapter factory that maps adaptables to strings
		IAdapterFactory fac = new IAdapterFactory() {
			@Override
			public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
				if (adapterType == String.class) {
					return adapterType.cast(adaptableObject.toString());
				}
				return null;
			}

			@Override
			public Class<?>[] getAdapterList() {
				return new Class[] { String.class };
			}
		};
		manager.registerAdapters(fac, TestAdaptable.class);
		try {
			// request adapter for factory that we've just added
			assertTrue("1.3", manager.hasAdapter(adaptable, "java.lang.String"));
		} finally {
			manager.unregisterAdapters(fac, TestAdaptable.class);
		}

		// request adapter that was unloaded
		assertFalse("1.4", manager.hasAdapter(adaptable, "java.lang.String"));
	}

	/**
	 * Tests API method IAdapterManager.getAdapter.
	 */
	@Test
	public void testGetAdapter() {
		TestAdaptable adaptable = new TestAdaptable();
		// request non-existing adaptable
		assertNull("1.0", manager.getAdapter("", NON_EXISTING));

		// request adapter that is in XML but has no registered factory
		Object result = manager.getAdapter(adaptable, TEST_ADAPTER);
		assertTrue("1.1", result instanceof TestAdapter);

		// request adapter that is not in XML
		assertNull("1.2", manager.getAdapter(adaptable, "java.lang.String"));

		// register an adapter factory that maps adaptables to strings
		IAdapterFactory fac = new IAdapterFactory() {
			@Override
			public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
				if (adapterType == String.class) {
					return adapterType.cast(adaptableObject.toString());
				}
				return null;
			}

			@Override
			public Class<?>[] getAdapterList() {
				return new Class[] { String.class };
			}
		};
		manager.registerAdapters(fac, TestAdaptable.class);
		try {
			// request adapter for factory that we've just added
			result = manager.getAdapter(adaptable, "java.lang.String");
			assertTrue("1.3", result instanceof String);
		} finally {
			manager.unregisterAdapters(fac, TestAdaptable.class);
		}
		// request adapter that was unloaded
		assertNull("1.4", manager.getAdapter(adaptable, "java.lang.String"));
	}

	/**
	 * Tests API method IAdapterManager.loadAdapter.
	 */
	@Test
	public void testLoadAdapter() {
		TestAdaptable adaptable = new TestAdaptable();
		// request non-existing adaptable
		assertNull("1.0", manager.loadAdapter("", NON_EXISTING));

		// request adapter that is in XML but has no registered factory
		Object result = manager.loadAdapter(adaptable, TEST_ADAPTER);
		assertTrue("1.1", result instanceof TestAdapter);

		// request adapter that is not in XML
		assertNull("1.2", manager.loadAdapter(adaptable, "java.lang.String"));

		// register an adapter factory that maps adaptables to strings
		IAdapterFactory fac = new IAdapterFactory() {
			@Override
			public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
				if (adapterType == String.class) {
					return adapterType.cast(adaptableObject.toString());
				}
				return null;
			}

			@Override
			public Class<?>[] getAdapterList() {
				return new Class[] { String.class };
			}
		};
		manager.registerAdapters(fac, TestAdaptable.class);
		try {
			// request adapter for factory that we've just added
			result = manager.loadAdapter(adaptable, "java.lang.String");
			assertTrue("1.3", result instanceof String);
		} finally {
			manager.unregisterAdapters(fac, TestAdaptable.class);
		}
		// request adapter that was unloaded
		assertNull("1.4", manager.loadAdapter(adaptable, "java.lang.String"));
	}
}
