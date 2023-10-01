/*******************************************************************************
 * Copyright (c) 2007, 2020 IBM Corporation and others.
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
 *     Christoph Laeubrich - Bug 567344
 *******************************************************************************/
package org.eclipse.equinox.common.tests.adaptable;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.IAdapterManager;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
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
	private static final String TEST_ADAPTER_OSGI = TestAdapter2.class.getName();

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

	@Test
	public void testAdaptersOSGiLazy() {
		AtomicBoolean created = new AtomicBoolean();
		AtomicBoolean unregistered = new AtomicBoolean();
		TestAdaptable2 adaptable = new TestAdaptable2();
		assertFalse("already present", manager.hasAdapter(adaptable, TEST_ADAPTER_OSGI));
		BundleContext context = FrameworkUtil.getBundle(IAdapterManagerServiceTest.class).getBundleContext();
		Hashtable<String, Object> properties = new Hashtable<>();
		properties.put(IAdapterFactory.SERVICE_PROPERTY_ADAPTABLE_CLASS, TestAdaptable2.class.getName());
		ServiceRegistration<?> registration = context.registerService(IAdapterFactory.class.getName(),
				new ServiceFactory<IAdapterFactory>() {

					@Override
					public IAdapterFactory getService(Bundle bundle, ServiceRegistration<IAdapterFactory> r) {
						created.set(true);
						return new TestAdapterFactory2(TestAdapter2::new);
					}

					@Override
					public void ungetService(Bundle bundle, ServiceRegistration<IAdapterFactory> r,
							IAdapterFactory service) {
						unregistered.set(true);
					}
				}, properties);
		assertFalse(created.get());
		Object result = manager.getAdapter(adaptable, TEST_ADAPTER_OSGI);
		assertTrue("result is not of desired type", result instanceof TestAdapter2);
		assertTrue(created.get());
		registration.unregister();
		assertFalse("manager is still present", manager.hasAdapter(adaptable, TEST_ADAPTER_OSGI));
		assertTrue(unregistered.get());
	}

	@Test
	public void testAdaptersOSGiLazyExt() {
		AtomicBoolean created = new AtomicBoolean();
		AtomicBoolean unregistered = new AtomicBoolean();
		TestAdaptable2 adaptable = new TestAdaptable2();
		assertFalse("already present", manager.hasAdapter(adaptable, TEST_ADAPTER_OSGI));
		BundleContext context = FrameworkUtil.getBundle(IAdapterManagerServiceTest.class).getBundleContext();
		Hashtable<String, Object> properties = new Hashtable<>();
		properties.put(IAdapterFactory.SERVICE_PROPERTY_ADAPTABLE_CLASS, TestAdaptable2.class.getName());
		properties.put(IAdapterFactory.SERVICE_PROPERTY_ADAPTER_NAMES, TestAdapter2.class.getName());
		ServiceRegistration<?> registration = context.registerService(IAdapterFactory.class.getName(),
				new ServiceFactory<IAdapterFactory>() {

					@Override
					public IAdapterFactory getService(Bundle bundle, ServiceRegistration<IAdapterFactory> r) {
						created.set(true);
						return new TestAdapterFactory2(TestAdapter2::new);
					}

					@Override
					public void ungetService(Bundle bundle, ServiceRegistration<IAdapterFactory> r,
							IAdapterFactory service) {
						unregistered.set(true);
					}
				}, properties);
		assertTrue("manager is not present", manager.hasAdapter(adaptable, TEST_ADAPTER_OSGI));
		Object result = manager.getAdapter(adaptable, TEST_ADAPTER_OSGI);
		assertNull("result should be null", result);
		assertFalse(created.get());
		result = manager.loadAdapter(adaptable, TEST_ADAPTER_OSGI);
		assertTrue("result is not of desired type", result instanceof TestAdapter2);
		assertTrue(created.get());
		registration.unregister();
		assertFalse("manager is still present", manager.hasAdapter(adaptable, TEST_ADAPTER_OSGI));
		assertTrue(unregistered.get());
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
