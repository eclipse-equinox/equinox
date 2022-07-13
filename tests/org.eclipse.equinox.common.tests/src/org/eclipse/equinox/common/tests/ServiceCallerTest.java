/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Alex Blewitt - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.common.tests;

import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertNotEquals;
import static org.osgi.framework.FrameworkUtil.asDictionary;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.runtime.ServiceCaller;
import org.eclipse.core.tests.harness.CoreTest;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

public class ServiceCallerTest extends CoreTest {
	static class ServiceExampleFactory implements ServiceFactory<IServiceExample> {
		final Map<Bundle, AtomicInteger> createCount = new ConcurrentHashMap<>();
		volatile ServiceExample lastCreated;

		@Override
		public IServiceExample getService(Bundle bundle, ServiceRegistration<IServiceExample> registration) {
			createCount.computeIfAbsent(bundle, (s) -> new AtomicInteger()).incrementAndGet();
			return lastCreated = new ServiceExample();
		}

		@Override
		public void ungetService(Bundle bundle, ServiceRegistration<IServiceExample> registration,
				IServiceExample service) {
			// do nothing
		}

		int getCreateCount(Bundle b) {
			AtomicInteger result = createCount.get(b);
			return result == null ? 0 : result.get();
		}
	}
	/**
	 * Need a zero argument constructor to satisfy the test harness. This
	 * constructor should not do any real work nor should it be called by user code.
	 */
	public ServiceCallerTest() {
		super(null);
	}

	public ServiceCallerTest(String name) {
		super(name);
	}

	public void testCallOnce() {
		Bundle bundle = FrameworkUtil.getBundle(ServiceCallerTest.class);
		assertNotNull("Test only works under an OSGi runtime", bundle);
		BundleContext context = bundle.getBundleContext();
		ServiceExampleFactory factory = new ServiceExampleFactory();
		ServiceRegistration<IServiceExample> reg = null;
		try {
			Dictionary<String, String> props = new Hashtable<>();
			props.put("test", "value");

			reg = context.registerService(IServiceExample.class, factory, props);
			ServiceCaller.callOnce(IServiceExample.class, IServiceExample.class, IServiceExample::call);
			ServiceExample lastCreated1 = factory.lastCreated;
			assertTrue("Service called successfully", lastCreated1.called);

			Bundle[] users = reg.getReference().getUsingBundles();
			assertNull("Didn't expect users.", users);

			ServiceCaller.callOnce(IServiceExample.class, IServiceExample.class, "(test=value)", IServiceExample::call);
			ServiceExample lastCreated2 = factory.lastCreated;
			assertTrue("Service called successfully", lastCreated2.called);

			assertNotEquals("Should have new service each call", lastCreated1, lastCreated2);

			boolean result = ServiceCaller.callOnce(IServiceExample.class, IServiceExample.class, "(test!=value)",
					IServiceExample::call);
			assertFalse("Should not have found service with filter", result);

			assertEquals("Unexpected createCount", 2, factory.getCreateCount(bundle));
		} finally {
			reg.unregister();
		}
	}

	public void testCall() throws IOException, BundleException, ClassNotFoundException {
		Bundle bundle = FrameworkUtil.getBundle(ServiceCallerTest.class);
		assertNotNull("Test only works under an OSGi runtime", bundle);
		BundleContext context = bundle.getBundleContext();

		Bundle testBundle = StatusTest.installTestClassBundle(context);
		testBundle.start();

		ServiceCaller<IServiceExample> thisClassCaller;
		ServiceCaller<IServiceExample> thisClassCallerFilter;
		ServiceCaller<IServiceExample> otherClassCaller;

		thisClassCaller = new ServiceCaller<>(getClass(), IServiceExample.class);
		thisClassCallerFilter = new ServiceCaller<>(getClass(), IServiceExample.class, "(test=value2)");
		Class<?> testClass = testBundle.loadClass(TestClass.class.getName());
		otherClassCaller = new ServiceCaller<>(testClass, IServiceExample.class);

		assertFalse("Should not be called.", thisClassCaller.call(IServiceExample::call));
		assertFalse("Should not be called.", thisClassCallerFilter.call(IServiceExample::call));
		assertFalse("Should not be called.", otherClassCaller.call(IServiceExample::call));

		assertFalse("Should not be present.", thisClassCaller.current().isPresent());
		assertFalse("Should not be present.", thisClassCallerFilter.current().isPresent());
		assertFalse("Should not be present.", otherClassCaller.current().isPresent());

		ServiceExampleFactory factory = new ServiceExampleFactory();
		Dictionary<String, String> props = new Hashtable<>();
		props.put("test", "value1");
		ServiceRegistration<IServiceExample> reg = context.registerService(IServiceExample.class, factory, props);
		try {
			assertTrue("Call returned false.", thisClassCaller.call(IServiceExample::call));
			assertTrue("Service called successfully", factory.lastCreated.called);
			assertTrue("Should be present.", thisClassCaller.current().isPresent());
			assertEquals("Unexpected current.", thisClassCaller.current().get(), factory.lastCreated);

			assertFalse("Should not be called.", thisClassCallerFilter.call(IServiceExample::call));

			factory.lastCreated.called = false;
			assertTrue("Call returned false.", otherClassCaller.call(IServiceExample::call));
			assertTrue("Service called successfully", factory.lastCreated.called);

			assertTrue("Call returned false.", thisClassCaller.call(IServiceExample::call));
			assertTrue("Call returned false.", otherClassCaller.call(IServiceExample::call));

			assertEquals("Wrong createCount", 1, factory.getCreateCount(bundle));
			assertEquals("Wrong createCount", 1, factory.getCreateCount(testBundle));

			Bundle[] users = reg.getReference().getUsingBundles();
			assertNotNull("Expected some users", users);

			Collection<Bundle> userCollection = Arrays.asList(users);
			assertTrue("Missing bundle.", userCollection.contains(bundle));
			assertTrue("Missing bundle.", userCollection.contains(testBundle));

			reg.unregister();
			assertFalse("Should not be called.", thisClassCaller.call(IServiceExample::call));
			assertFalse("Should not be called.", otherClassCaller.call(IServiceExample::call));
			assertFalse("Should not be present.", thisClassCaller.current().isPresent());

			props.put("test", "value2");
			reg = context.registerService(IServiceExample.class, factory, props);

			assertTrue("Should be present.", thisClassCaller.current().isPresent());
			assertEquals("Unexpected current.", thisClassCaller.current().get(), factory.lastCreated);
			assertTrue("Call returned false.", thisClassCaller.call(IServiceExample::call));
			assertTrue("Call returned false.", thisClassCallerFilter.call(IServiceExample::call));
			assertTrue("Call returned false.", otherClassCaller.call(IServiceExample::call));
			assertTrue("Call returned false.", thisClassCaller.call(IServiceExample::call));
			assertTrue("Call returned false.", thisClassCallerFilter.call(IServiceExample::call));
			assertTrue("Call returned false.", otherClassCaller.call(IServiceExample::call));
			assertEquals("Wrong createCount", 2, factory.getCreateCount(bundle));
			assertEquals("Wrong createCount", 2, factory.getCreateCount(testBundle));

			testBundle.stop();
			assertFalse("Should Not be called.", otherClassCaller.call(IServiceExample::call));

			testBundle.start();
			assertTrue("Call returned false.", otherClassCaller.call(IServiceExample::call));
			assertEquals("Wrong createCount", 3, factory.getCreateCount(testBundle));

			thisClassCaller.unget();
			thisClassCallerFilter.unget();
			assertTrue("Call returned false.", thisClassCaller.call(IServiceExample::call));
			assertTrue("Call returned false.", thisClassCallerFilter.call(IServiceExample::call));
			assertEquals("Wrong createCount", 3, factory.getCreateCount(bundle));

			props.put("test", "value3");
			reg.setProperties(props);
			assertTrue("Call returned false.", thisClassCaller.call(IServiceExample::call));
			assertFalse("Should not be called.", thisClassCallerFilter.call(IServiceExample::call));
			assertFalse("Should not be present.", thisClassCallerFilter.current().isPresent());
		} finally {
			testBundle.uninstall();
			thisClassCaller.unget();
			otherClassCaller.unget();
			try {
				reg.unregister();
			} catch (IllegalStateException e) {
				// ignore
			}
		}
	}

	public void testInvalidFilter() {
		try {
			new ServiceCaller<>(getClass(), IServiceExample.class, "invalid filter");
			fail("Expected an exception on invalid filter.");
		} catch (IllegalArgumentException e) {
			assertTrue("Unexpected cause.", e.getCause() instanceof InvalidSyntaxException);
		}
		try {
			ServiceCaller.callOnce(getClass(), IServiceExample.class, "invalid filter", (example) -> {
			});
			fail("Expected an exception on invalid filter.");
		} catch (IllegalArgumentException e) {
			assertTrue("Unexpected cause.", e.getCause() instanceof InvalidSyntaxException);
		}
	}

	public void testRank() {
		Bundle bundle = FrameworkUtil.getBundle(ServiceCallerTest.class);
		assertNotNull("Test only works under an OSGi runtime", bundle);
		BundleContext context = bundle.getBundleContext();
		ServiceExample s1 = new ServiceExample();
		ServiceExample s2 = new ServiceExample();
		ServiceExample s3 = new ServiceExample();
		ServiceRegistration<IServiceExample> reg1 = null;
		ServiceRegistration<IServiceExample> reg2 = null;
		ServiceRegistration<IServiceExample> reg3 = null;
		try {
			reg1 = context.registerService(IServiceExample.class, s1,
					asDictionary(singletonMap(Constants.SERVICE_RANKING, 1)));
			reg2 = context.registerService(IServiceExample.class, s2,
					asDictionary(singletonMap(Constants.SERVICE_RANKING, 2)));
			reg3 = context.registerService(IServiceExample.class, s3,
					asDictionary(singletonMap(Constants.SERVICE_RANKING, 3)));
			ServiceCaller<IServiceExample> caller = new ServiceCaller<>(getClass(), IServiceExample.class);

			assertHighestRankedCalled(caller, s3, s2, s1, reg3, reg2, reg1);

			reg1.setProperties(asDictionary(singletonMap(Constants.SERVICE_RANKING, 10)));
			assertHighestRankedCalled(caller, s1, s2, s3, reg1, reg2, reg3);

			reg2.setProperties(asDictionary(singletonMap(Constants.SERVICE_RANKING, 10)));
			reg1.setProperties(asDictionary(singletonMap(Constants.SERVICE_RANKING, 11)));
			assertHighestRankedCalled(caller, s1, s2, s3, reg1, reg2, reg3);

			reg1.setProperties(asDictionary(singletonMap(Constants.SERVICE_RANKING, 1)));
			assertHighestRankedCalled(caller, s2, s1, s3, reg2, reg1, reg3);
		} finally {
			if (reg1 != null) {
				reg1.unregister();
			}
			if (reg2 != null) {
				reg2.unregister();
			}
			if (reg3 != null) {
				reg3.unregister();
			}
		}
	}

	private void assertHighestRankedCalled(ServiceCaller<IServiceExample> caller, ServiceExample highest,
			ServiceExample lower1, ServiceExample lower2, ServiceRegistration<IServiceExample> highestReg,
			ServiceRegistration<IServiceExample> lower1Reg, ServiceRegistration<IServiceExample> lower2Reg) {
		assertTrue("Did not call service.", caller.call(IServiceExample::call));
		assertTrue("Highest Ranked not called.", highest.called);
		assertFalse("Lower1 called.", lower1.called);
		assertFalse("Lower2 called.", lower2.called);
		assertNotNull("No users of highest.", highestReg.getReference().getUsingBundles());
		assertNull("Users of lower ranked.", lower1Reg.getReference().getUsingBundles());
		assertNull("Users of lower ranked.", lower2Reg.getReference().getUsingBundles());
		highest.called = false;
		lower1.called = false;
		lower2.called = false;
	}

	interface IServiceExample {
		void call();
	}

	static class ServiceExample implements IServiceExample {
		boolean called = false;

		@Override
		public void call() {
			called = true;
		}

	}
}
