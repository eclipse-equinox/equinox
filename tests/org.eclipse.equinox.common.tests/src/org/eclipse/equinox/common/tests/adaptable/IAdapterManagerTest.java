/*******************************************************************************
 *  Copyright (c) 2004, 2018 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.common.tests.adaptable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;

import org.eclipse.core.internal.runtime.AdapterManager;
import org.eclipse.core.internal.runtime.IAdapterFactoryExt;
import org.eclipse.core.runtime.AssertionFailedException;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.core.tests.harness.BundleTestingHelper;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;

/**
 * Tests API on the IAdapterManager class.
 */
public class IAdapterManagerTest {
	// following classes are for testComputeClassOrder and other tests for
	// non-trivial class hierarchies
	interface C {
	}

	interface D {
	}

	interface M {
	}

	interface N {
	}

	interface O {
	}

	interface A extends M, N {
	}

	interface B extends O {
	}

	class Y implements C, D {
	}

	class X extends Y implements A, B {
	}

	private static final String NON_EXISTING = "com.does.not.Exist";
	private static final String TEST_ADAPTER = "org.eclipse.equinox.common.tests.adaptable.TestAdapter";
	private static final String TEST_ADAPTER_CL = "testAdapter.testUnknown";
	private IAdapterManager manager;

	@Before
	public void setUp() throws Exception {
		manager = AdapterManager.getDefault();
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

	@Test
	public void testGetAdapterNullArgs() {
		TestAdaptable adaptable = new TestAdaptable();
		assertThrows(RuntimeException.class, () -> manager.getAdapter(adaptable, (Class<?>) null));
		assertThrows(RuntimeException.class, () -> manager.getAdapter(null, NON_EXISTING));
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

	/**
	 * Test adapting to classes not reachable by the default bundle class loader
	 * (bug 200068). NOTE: This test uses .class file compiled with 1.4 JRE. As a
	 * result, the test can not be run on pre-1.4 JRE.
	 */
	@Test
	public void testAdapterClassLoader() throws MalformedURLException, BundleException, IOException {
		TestAdaptable adaptable = new TestAdaptable();
		assertTrue(manager.hasAdapter(adaptable, TEST_ADAPTER_CL));
		assertNull(manager.loadAdapter(adaptable, TEST_ADAPTER_CL));
		Bundle bundle = null;
		try {
			BundleContext bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();
			bundle = BundleTestingHelper.installBundle("0.1", bundleContext,
					"Plugin_Testing/adapters/testAdapter_1.0.0");
			BundleTestingHelper.refreshPackages(bundleContext, new Bundle[] { bundle });

			assertTrue(manager.hasAdapter(adaptable, TEST_ADAPTER_CL));
			Object result = manager.loadAdapter(adaptable, TEST_ADAPTER_CL);
			assertNotNull(result);
			assertTrue(TEST_ADAPTER_CL.equals(result.getClass().getName()));
		} finally {
			if (bundle != null) {
				bundle.uninstall();
			}
		}
	}

	/**
	 * Tests for {@link IAdapterManager#computeClassOrder(Class)}.
	 */
	@Test
	public void testComputeClassOrder() {
		Class<?>[] expected = new Class[] { X.class, Y.class, Object.class, A.class, B.class, M.class, N.class, O.class,
				C.class, D.class };
		Class<?>[] actual = manager.computeClassOrder(X.class);
		assertEquals("1.0", expected.length, actual.length);
		for (int i = 0; i < actual.length; i++) {
			assertEquals("1.1." + i, expected[i], actual[i]);
		}
	}

	@Test
	public void testFactoryViolatingContract() {
		class Private {
		}

		IAdapterFactory fac = new IAdapterFactory() {
			@SuppressWarnings("unchecked")
			@Override
			public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
				if (adapterType == Private.class) {
					// Cast below violates the contract of the factory
					return (T) Boolean.FALSE;
				}
				return null;
			}

			@Override
			public Class<?>[] getAdapterList() {
				return new Class[] { Private.class };
			}
		};
		try {
			manager.registerAdapters(fac, Private.class);
			AssertionFailedException e = assertThrows(
					AssertionFailedException.class,
					() -> manager.getAdapter(new Private(), Private.class));
			assertTrue(e.getMessage().contains(fac.getClass().getName()));
			assertTrue(e.getMessage().contains(Boolean.class.getName()));
			assertTrue(e.getMessage().contains(Private.class.getName()));
		} finally {
			manager.unregisterAdapters(fac, Private.class);
		}
	}

	@Test
	public void testContinueAfterNullAdapterFactory() {
		class PrivateAdapter {
		}
		class PrivateAdaptable {
		}
		IAdapterFactory nullAdapterFactory = new IAdapterFactory() {
			@Override
			public Class<?>[] getAdapterList() {
				return new Class<?>[] { PrivateAdapter.class };
			}

			@Override
			public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
				return null;
			}
		};
		IAdapterFactory adapterFactory = new IAdapterFactory() {
			@Override
			public Class<?>[] getAdapterList() {
				return new Class<?>[] { PrivateAdapter.class };
			}

			@Override
			public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
				return (T) new PrivateAdapter();
			}
		};
		try {
			manager.registerAdapters(nullAdapterFactory, PrivateAdaptable.class);
			manager.registerAdapters(adapterFactory, PrivateAdaptable.class);
			assertNotNull(manager.getAdapter(new PrivateAdaptable(), PrivateAdapter.class));
		} catch (Exception ex) {
			manager.unregisterAdapters(nullAdapterFactory);
			manager.unregisterAdapters(adapterFactory);
		}
	}

	@Test
	public void testMultipleAdapterFactoriesFromExtensionPoint() {
		assertNotNull(manager.getAdapter(new TestAdaptable(), TestAdapter2.class));
	}

	@Test
	public void testNoAdapterForType() {
		manager.queryAdapter(new Object() {
		}, String.class.getName());
		// verifies it doesn't fail with NPE or other exception
	}

	@Test
	public void testGetAdapterForSpecializedNamedSubtype() {
		IAdapterFactory factory = new IAdapterFactory() {
			@Override
			public Class<?>[] getAdapterList() {
				return new Class<?>[] { C.class };
			}

			@Override
			public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
				return (T) new Y();
			}
		};
		manager.registerAdapters(factory, Object.class);
		try {
			assertNotNull(manager.getAdapter(new Object(), C.class.getName()));
		} finally {
			manager.unregisterAdapters(factory);
		}
	}

	public static class GenericToStringAdapterFactory implements IAdapterFactory, IAdapterFactoryExt {

		public boolean loaded = true;
		private Class<?> adaptableType;

		public GenericToStringAdapterFactory(Class<?> adaptableType) {
			this.adaptableType = adaptableType;
		}

		@Override
		public IAdapterFactory loadFactory(boolean force) {
			if (loaded || force) {
				loaded = true;
				return this;
			}
			return null;
		}

		@Override
		public String[] getAdapterNames() {
			return new String[] { String.class.getName() };
		}

		@Override
		public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
			assertTrue(adaptableType.isInstance(adaptableObject));
			if (!loaded) {
				return null;
			}
			if (adapterType == String.class) {
				return adapterType.cast(adaptableObject.toString() + " adapted by " + this.getClass().getSimpleName());
			}
			return null;
		}

		@Override
		public Class<?>[] getAdapterList() {
			return new Class<?>[] { String.class };
		}

	}

	public static class XToStringAdapterFactory extends GenericToStringAdapterFactory {

		public XToStringAdapterFactory() {
			super(X.class);
		}
	}

	public static class YToStringAdapterFactory extends GenericToStringAdapterFactory {

		public YToStringAdapterFactory() {
			super(Y.class);
		}
	}

	/**
	 * Tests that the correct adapter is returned for class X extends Y
	 */
	@Test
	public void testGetAdapterXY() {
		X xAdaptable = new X();
		Y yAdaptable = new Y();

		XToStringAdapterFactory xFactory = new XToStringAdapterFactory();
		YToStringAdapterFactory yFactory = new YToStringAdapterFactory();
		manager.registerAdapters(xFactory, X.class);
		manager.registerAdapters(yFactory, Y.class);
		try {
			assertTrue(((String) manager.loadAdapter(xAdaptable, "java.lang.String"))
					.endsWith(" adapted by " + XToStringAdapterFactory.class.getSimpleName()));
			assertTrue(((String) manager.loadAdapter(yAdaptable, "java.lang.String"))
					.endsWith(" adapted by " + YToStringAdapterFactory.class.getSimpleName()));
			assertTrue(((String) manager.getAdapter(xAdaptable, "java.lang.String"))
					.endsWith(" adapted by " + XToStringAdapterFactory.class.getSimpleName()));
			assertTrue(((String) manager.getAdapter(yAdaptable, "java.lang.String"))
					.endsWith(" adapted by " + YToStringAdapterFactory.class.getSimpleName()));
			assertTrue(manager.getAdapter(xAdaptable, String.class)
					.endsWith(" adapted by " + XToStringAdapterFactory.class.getSimpleName()));
			assertTrue(manager.getAdapter(yAdaptable, String.class)
					.endsWith(" adapted by " + YToStringAdapterFactory.class.getSimpleName()));
		} finally {
			manager.unregisterAdapters(xFactory, TestAdaptable.class);
			manager.unregisterAdapters(yFactory, TestAdaptable.class);
		}
	}

	/**
	 * Tests that the correct adapter is returned for class X extends Y when X
	 * adapter is not loaded
	 */
	@Test
	public void testGetAdapterXYNotLoaded() {
		X xAdaptable = new X();
		Y yAdaptable = new Y();

		XToStringAdapterFactory xFactory = new XToStringAdapterFactory();
		xFactory.loaded = false;
		YToStringAdapterFactory yFactory = new YToStringAdapterFactory();
		manager.registerAdapters(xFactory, X.class);
		manager.registerAdapters(yFactory, Y.class);
		try {
			/*
			 * Here, before the XToStringAdapterFactory is fully loaded, the adapter
			 * registered to the super type (aka YToStringAdapterFactory) will be used to
			 * adapt the X instance to string
			 */
			assertTrue(((String) manager.getAdapter(xAdaptable, "java.lang.String"))
					.endsWith(" adapted by " + YToStringAdapterFactory.class.getSimpleName()));
			assertTrue(((String) manager.getAdapter(yAdaptable, "java.lang.String"))
					.endsWith(" adapted by " + YToStringAdapterFactory.class.getSimpleName()));
			assertTrue(manager.getAdapter(xAdaptable, String.class)
					.endsWith(" adapted by " + YToStringAdapterFactory.class.getSimpleName()));
			assertTrue(manager.getAdapter(yAdaptable, String.class)
					.endsWith(" adapted by " + YToStringAdapterFactory.class.getSimpleName()));

			// now load the X adapter factory:
			assertTrue(((String) manager.loadAdapter(xAdaptable, "java.lang.String"))
					.endsWith(" adapted by " + XToStringAdapterFactory.class.getSimpleName()));

			// and repeat making sure the X adapter factory is now used
			assertTrue(((String) manager.getAdapter(xAdaptable, "java.lang.String"))
					.endsWith(" adapted by " + XToStringAdapterFactory.class.getSimpleName()));
			assertTrue(((String) manager.getAdapter(yAdaptable, "java.lang.String"))
					.endsWith(" adapted by " + YToStringAdapterFactory.class.getSimpleName()));
			assertTrue(manager.getAdapter(xAdaptable, String.class)
					.endsWith(" adapted by " + XToStringAdapterFactory.class.getSimpleName()));
			assertTrue(manager.getAdapter(yAdaptable, String.class)
					.endsWith(" adapted by " + YToStringAdapterFactory.class.getSimpleName()));

		} finally {
			manager.unregisterAdapters(xFactory, TestAdaptable.class);
			manager.unregisterAdapters(yFactory, TestAdaptable.class);
		}
	}

	/**
	 * Tests that the correct adapter is returned for class X extends Y when X
	 * adapter is not loaded, but is loaded immediately
	 */
	@Test
	public void testGetAdapterXYNotLoadedForceLoad() {
		X xAdaptable = new X();
		Y yAdaptable = new Y();

		XToStringAdapterFactory xFactory = new XToStringAdapterFactory();
		xFactory.loaded = false;
		YToStringAdapterFactory yFactory = new YToStringAdapterFactory();
		manager.registerAdapters(xFactory, X.class);
		manager.registerAdapters(yFactory, Y.class);
		try {
			assertTrue(((String) manager.loadAdapter(xAdaptable, "java.lang.String"))
					.endsWith(" adapted by " + XToStringAdapterFactory.class.getSimpleName()));
			assertTrue(((String) manager.loadAdapter(yAdaptable, "java.lang.String"))
					.endsWith(" adapted by " + YToStringAdapterFactory.class.getSimpleName()));
			assertTrue(((String) manager.getAdapter(xAdaptable, "java.lang.String"))
					.endsWith(" adapted by " + XToStringAdapterFactory.class.getSimpleName()));
			assertTrue(((String) manager.getAdapter(yAdaptable, "java.lang.String"))
					.endsWith(" adapted by " + YToStringAdapterFactory.class.getSimpleName()));
			assertTrue(manager.getAdapter(xAdaptable, String.class)
					.endsWith(" adapted by " + XToStringAdapterFactory.class.getSimpleName()));
			assertTrue(manager.getAdapter(yAdaptable, String.class)
					.endsWith(" adapted by " + YToStringAdapterFactory.class.getSimpleName()));

		} finally {
			manager.unregisterAdapters(xFactory, TestAdaptable.class);
			manager.unregisterAdapters(yFactory, TestAdaptable.class);
		}
	}
}
