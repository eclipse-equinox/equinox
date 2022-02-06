/*******************************************************************************
 * Copyright (c) 2008, 2022 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.serviceregistry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.eclipse.osgi.tests.bundles.AbstractBundleTests;
import org.eclipse.osgi.tests.util.MapDictionary;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

public class ServiceRegistryTests extends AbstractBundleTests {

	@Test
	public void testServiceListener01() throws InvalidSyntaxException {
		final String testMethodName = getName();
		// simple ServiceListener test
		Runnable runIt = () -> {
			// nothing
		};
		final boolean[] results = new boolean[] {false, false, false, false};
		ServiceListener testListener = event -> {
			switch (event.getType()) {
				case ServiceEvent.REGISTERED :
					results[0] = true;
					break;
				case ServiceEvent.MODIFIED :
					results[1] = true;
					break;
				case ServiceEvent.MODIFIED_ENDMATCH :
					results[2] = true;
					break;
				case ServiceEvent.UNREGISTERING :
					results[3] = true;
					break;
			}
		};
		OSGiTestsActivator.getContext().addServiceListener(testListener, "(&(objectclass=java.lang.Runnable)(" + testMethodName.toLowerCase() + "=true))"); //$NON-NLS-1$ //$NON-NLS-2$

		ServiceRegistration reg = null;
		try {
			// register service which matches
			Hashtable props = new Hashtable();
			props.put(testMethodName, Boolean.TRUE);
			reg = OSGiTestsActivator.getContext().registerService(Runnable.class.getName(), runIt, props);
			assertTrue("Did not get ServiceEvent.REGISTERED", results[0]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.MODIFIED", results[1]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.MODIFIED_ENDMATCH", results[2]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.UNREGISTERING", results[3]); //$NON-NLS-1$
			clearResults(results);

			// change props to still match
			props.put("testChangeProp", Boolean.FALSE); //$NON-NLS-1$
			reg.setProperties(props);
			assertFalse("Did get ServiceEvent.REGISTERED", results[0]); //$NON-NLS-1$
			assertTrue("Did not get ServiceEvent.MODIFIED", results[1]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.MODIFIED_ENDMATCH", results[2]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.UNREGISTERING", results[3]); //$NON-NLS-1$
			clearResults(results);

			// change props to no longer match
			props.put(testMethodName, Boolean.FALSE);
			reg.setProperties(props);
			assertFalse("Did get ServiceEvent.REGISTERED", results[0]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.MODIFIED", results[1]); //$NON-NLS-1$
			assertTrue("Did not get ServiceEvent.MODIFIED_ENDMATCH", results[2]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.UNREGISTERING", results[3]); //$NON-NLS-1$
			clearResults(results);

			// change props to no longer match
			props.put("testChangeProp", Boolean.TRUE); //$NON-NLS-1$
			reg.setProperties(props);
			assertFalse("Did get ServiceEvent.REGISTERED", results[0]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.MODIFIED", results[1]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.MODIFIED_ENDMATCH", results[2]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.UNREGISTERING", results[3]); //$NON-NLS-1$
			clearResults(results);

			// change props back to match
			props.put(testMethodName, Boolean.TRUE);
			reg.setProperties(props);
			assertFalse("Did get ServiceEvent.REGISTERED", results[0]); //$NON-NLS-1$
			assertTrue("Did not get ServiceEvent.MODIFIED", results[1]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.MODIFIED_ENDMATCH", results[2]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.UNREGISTERING", results[3]); //$NON-NLS-1$
			clearResults(results);

			// unregister
			reg.unregister();
			reg = null;
			assertFalse("Did get ServiceEvent.REGISTERED", results[0]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.MODIFIED", results[1]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.MODIFIED_ENDMATCH", results[2]); //$NON-NLS-1$
			assertTrue("Did not get ServiceEvent.UNREGISTERING", results[3]); //$NON-NLS-1$
			clearResults(results);
		} finally {
			OSGiTestsActivator.getContext().removeServiceListener(testListener);
			if (reg != null)
				reg.unregister();
		}
	}

	@Test
	public void testServiceListener02() throws InvalidSyntaxException {
		final String testMethodName = getName();
		// simple ServiceListener test
		Runnable runIt = () -> {
			// nothing
		};
		final boolean[] results = new boolean[] {false, false, false, false};
		ServiceListener testListener = event -> {
			switch (event.getType()) {
				case ServiceEvent.REGISTERED :
					results[0] = true;
					break;
				case ServiceEvent.MODIFIED :
					results[1] = true;
					break;
				case ServiceEvent.MODIFIED_ENDMATCH :
					results[2] = true;
					break;
				case ServiceEvent.UNREGISTERING :
					results[3] = true;
					break;
			}
		};
		OSGiTestsActivator.getContext().addServiceListener(testListener, "(&(objectclass=java.lang.Runnable)(" + testMethodName.toLowerCase() + "=true))"); //$NON-NLS-1$ //$NON-NLS-2$

		ServiceRegistration reg = null;
		try {
			// register service which does not match
			Hashtable props = new Hashtable();
			props.put(testMethodName, Boolean.FALSE);
			reg = OSGiTestsActivator.getContext().registerService(Runnable.class.getName(), runIt, props);
			assertFalse("Did get ServiceEvent.REGISTERED", results[0]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.MODIFIED", results[1]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.MODIFIED_ENDMATCH", results[2]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.UNREGISTERING", results[3]); //$NON-NLS-1$
			clearResults(results);

			// change props to still not match
			props.put("testChangeProp", Boolean.FALSE); //$NON-NLS-1$
			reg.setProperties(props);
			assertFalse("Did get ServiceEvent.REGISTERED", results[0]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.MODIFIED", results[1]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.MODIFIED_ENDMATCH", results[2]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.UNREGISTERING", results[3]); //$NON-NLS-1$
			clearResults(results);

			// change props to match
			props.put(testMethodName, Boolean.TRUE);
			reg.setProperties(props);
			assertFalse("Did get ServiceEvent.REGISTERED", results[0]); //$NON-NLS-1$
			assertTrue("Did not get ServiceEvent.MODIFIED", results[1]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.MODIFIED_ENDMATCH", results[2]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.UNREGISTERING", results[3]); //$NON-NLS-1$
			clearResults(results);

			// change props to still match
			props.put("testChangeProp", Boolean.TRUE); //$NON-NLS-1$
			reg.setProperties(props);
			assertFalse("Did get ServiceEvent.REGISTERED", results[0]); //$NON-NLS-1$
			assertTrue("Did not get ServiceEvent.MODIFIED", results[1]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.MODIFIED_ENDMATCH", results[2]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.UNREGISTERING", results[3]); //$NON-NLS-1$
			clearResults(results);

			// change props to no longer match
			props.put(testMethodName, Boolean.FALSE);
			reg.setProperties(props);
			assertFalse("Did get ServiceEvent.REGISTERED", results[0]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.MODIFIED", results[1]); //$NON-NLS-1$
			assertTrue("Did not get ServiceEvent.MODIFIED_ENDMATCH", results[2]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.UNREGISTERING", results[3]); //$NON-NLS-1$
			clearResults(results);

			// unregister
			reg.unregister();
			reg = null;
			assertFalse("Did get ServiceEvent.REGISTERED", results[0]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.MODIFIED", results[1]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.MODIFIED_ENDMATCH", results[2]); //$NON-NLS-1$
			assertFalse("Did get ServiceEvent.UNREGISTERING", results[3]); //$NON-NLS-1$
			clearResults(results);
		} finally {
			OSGiTestsActivator.getContext().removeServiceListener(testListener);
			if (reg != null)
				reg.unregister();
		}
	}

	@Test
	public void testServiceListener03() throws InvalidSyntaxException {
		final String testMethodName = getName();
		// simple ServiceListener test
		Runnable runIt = () -> {
			// nothing
		};
		final int[] results = new int[] {0, 0, 0, 0};
		ServiceListener testListener = event -> {
			switch (event.getType()) {
				case ServiceEvent.REGISTERED :
					results[0]++;
					break;
				case ServiceEvent.MODIFIED :
					results[1]++;
					break;
				case ServiceEvent.MODIFIED_ENDMATCH :
					results[2]++;
					break;
				case ServiceEvent.UNREGISTERING :
					results[3]++;
					break;
			}
		};
		OSGiTestsActivator.getContext().addServiceListener(testListener, "(&(objectclass=java.lang.Runnable)(" + testMethodName.toLowerCase() + "=true))"); //$NON-NLS-1$ //$NON-NLS-2$
		ServiceRegistration reg1 = null;
		ServiceRegistration reg2 = null;
		try {
			// register service which does not match
			Hashtable props = new Hashtable();
			props.put(testMethodName, Boolean.FALSE);
			reg1 = OSGiTestsActivator.getContext().registerService(Runnable.class.getName(), runIt, props);
			reg2 = OSGiTestsActivator.getContext().registerService(Runnable.class.getName(), runIt, props);
			assertEquals("Did get ServiceEvent.REGISTERED", 0, results[0]); //$NON-NLS-1$
			assertEquals("Did get ServiceEvent.MODIFIED", 0, results[1]); //$NON-NLS-1$
			assertEquals("Did get ServiceEvent.MODIFIED_ENDMATCH", 0, results[2]); //$NON-NLS-1$
			assertEquals("Did get ServiceEvent.UNREGISTERING", 0, results[3]); //$NON-NLS-1$
			clearResults(results);

			// change props to still not match
			props.put("testChangeProp", Boolean.FALSE); //$NON-NLS-1$
			reg1.setProperties(props);
			assertEquals("Did get ServiceEvent.REGISTERED", 0, results[0]); //$NON-NLS-1$
			assertEquals("Did get ServiceEvent.MODIFIED", 0, results[1]); //$NON-NLS-1$
			assertEquals("Did get ServiceEvent.MODIFIED_ENDMATCH", 0, results[2]); //$NON-NLS-1$
			assertEquals("Did get ServiceEvent.UNREGISTERING", 0, results[3]); //$NON-NLS-1$
			clearResults(results);
			reg2.setProperties(props);
			assertEquals("Did get ServiceEvent.REGISTERED", 0, results[0]); //$NON-NLS-1$
			assertEquals("Did get ServiceEvent.MODIFIED", 0, results[1]); //$NON-NLS-1$
			assertEquals("Did get ServiceEvent.MODIFIED_ENDMATCH", 0, results[2]); //$NON-NLS-1$
			assertEquals("Did get ServiceEvent.UNREGISTERING", 0, results[3]); //$NON-NLS-1$
			clearResults(results);

			// change props to match
			props.put(testMethodName, Boolean.TRUE);
			reg1.setProperties(props);
			assertEquals("Did get ServiceEvent.REGISTERED", 0, results[0]); //$NON-NLS-1$
			assertEquals("Did get ServiceEvent.MODIFIED", 1, results[1]); //$NON-NLS-1$
			assertEquals("Did get ServiceEvent.MODIFIED_ENDMATCH", 0, results[2]); //$NON-NLS-1$
			assertEquals("Did get ServiceEvent.UNREGISTERING", 0, results[3]); //$NON-NLS-1$
			clearResults(results);
			reg2.setProperties(props);
			assertEquals("Did get ServiceEvent.REGISTERED", 0, results[0]); //$NON-NLS-1$
			assertEquals("Did get ServiceEvent.MODIFIED", 1, results[1]); //$NON-NLS-1$
			assertEquals("Did get ServiceEvent.MODIFIED_ENDMATCH", 0, results[2]); //$NON-NLS-1$
			assertEquals("Did get ServiceEvent.UNREGISTERING", 0, results[3]); //$NON-NLS-1$
			clearResults(results);

			// change props to still match
			props.put("testChangeProp", Boolean.TRUE); //$NON-NLS-1$
			reg1.setProperties(props);
			assertEquals("Did get ServiceEvent.REGISTERED", 0, results[0]); //$NON-NLS-1$
			assertEquals("Did get ServiceEvent.MODIFIED", 1, results[1]); //$NON-NLS-1$
			assertEquals("Did get ServiceEvent.MODIFIED_ENDMATCH", 0, results[2]); //$NON-NLS-1$
			assertEquals("Did get ServiceEvent.UNREGISTERING", 0, results[3]); //$NON-NLS-1$
			clearResults(results);
			reg2.setProperties(props);
			assertEquals("Did get ServiceEvent.REGISTERED", 0, results[0]); //$NON-NLS-1$
			assertEquals("Did get ServiceEvent.MODIFIED", 1, results[1]); //$NON-NLS-1$
			assertEquals("Did get ServiceEvent.MODIFIED_ENDMATCH", 0, results[2]); //$NON-NLS-1$
			assertEquals("Did get ServiceEvent.UNREGISTERING", 0, results[3]); //$NON-NLS-1$
			clearResults(results);

			// change props to no longer match
			props.put(testMethodName, Boolean.FALSE);
			reg1.setProperties(props);
			assertEquals("Did get ServiceEvent.REGISTERED", 0, results[0]); //$NON-NLS-1$
			assertEquals("Did get ServiceEvent.MODIFIED", 0, results[1]); //$NON-NLS-1$
			assertEquals("Did get ServiceEvent.MODIFIED_ENDMATCH", 1, results[2]); //$NON-NLS-1$
			assertEquals("Did get ServiceEvent.UNREGISTERING", 0, results[3]); //$NON-NLS-1$
			clearResults(results);
			reg2.setProperties(props);
			assertEquals("Did get ServiceEvent.REGISTERED", 0, results[0]); //$NON-NLS-1$
			assertEquals("Did get ServiceEvent.MODIFIED", 0, results[1]); //$NON-NLS-1$
			assertEquals("Did get ServiceEvent.MODIFIED_ENDMATCH", 1, results[2]); //$NON-NLS-1$
			assertEquals("Did get ServiceEvent.UNREGISTERING", 0, results[3]); //$NON-NLS-1$
			clearResults(results);

			// unregister
			reg1.unregister();
			reg1 = null;
			assertEquals("Did get ServiceEvent.REGISTERED", 0, results[0]); //$NON-NLS-1$
			assertEquals("Did get ServiceEvent.MODIFIED", 0, results[1]); //$NON-NLS-1$
			assertEquals("Did get ServiceEvent.MODIFIED_ENDMATCH", 0, results[2]); //$NON-NLS-1$
			assertEquals("Did get ServiceEvent.UNREGISTERING", 0, results[3]); //$NON-NLS-1$
			clearResults(results);
			reg2.unregister();
			reg2 = null;
			assertEquals("Did get ServiceEvent.REGISTERED", 0, results[0]); //$NON-NLS-1$
			assertEquals("Did get ServiceEvent.MODIFIED", 0, results[1]); //$NON-NLS-1$
			assertEquals("Did get ServiceEvent.MODIFIED_ENDMATCH", 0, results[2]); //$NON-NLS-1$
			assertEquals("Did get ServiceEvent.UNREGISTERING", 0, results[3]); //$NON-NLS-1$
			clearResults(results);
		} finally {
			OSGiTestsActivator.getContext().removeServiceListener(testListener);
			if (reg1 != null)
				reg1.unregister();
			if (reg2 != null)
				reg2.unregister();
		}
	}

	@Test
	public void testServiceOrdering01() {
		final String testMethodName = getName();
		// test that getServiceReference returns the proper service
		Runnable runIt = () -> {
			// nothing
		};
		Hashtable props = new Hashtable();
		props.put("name", testMethodName); //$NON-NLS-1$
		props.put(Constants.SERVICE_DESCRIPTION, "min value"); //$NON-NLS-1$
		props.put(Constants.SERVICE_RANKING, Integer.valueOf(Integer.MIN_VALUE));
		ServiceRegistration reg1 = OSGiTestsActivator.getContext().registerService(Runnable.class.getName(), runIt, props);

		props.put(Constants.SERVICE_DESCRIPTION, "max value first"); //$NON-NLS-1$
		props.put(Constants.SERVICE_RANKING, Integer.valueOf(Integer.MAX_VALUE));
		ServiceRegistration reg2 = OSGiTestsActivator.getContext().registerService(Runnable.class.getName(), runIt, props);

		props.put(Constants.SERVICE_DESCRIPTION, "max value second"); //$NON-NLS-1$
		props.put(Constants.SERVICE_RANKING, Integer.valueOf(Integer.MAX_VALUE));
		ServiceRegistration reg3 = OSGiTestsActivator.getContext().registerService(Runnable.class.getName(), runIt, props);

		try {
			ServiceReference ref = null;
			ref = OSGiTestsActivator.getContext().getServiceReference(Runnable.class.getName());
			assertNotNull("service ref is null", ref); //$NON-NLS-1$
			assertEquals("Wrong references", reg2.getReference(), ref); //$NON-NLS-1$
		} finally {
			if (reg1 != null)
				reg1.unregister();
			if (reg2 != null)
				reg2.unregister();
			if (reg3 != null)
				reg3.unregister();
		}
	}

	@Test
	public void testDuplicateObjectClass() {
		ServiceRegistration reg = null;
		try {
			reg = OSGiTestsActivator.getContext().registerService(new String[] {Runnable.class.getName(), Object.class.getName(), Object.class.getName()}, (Runnable) () -> {
				// nothing
			}, null);
		} finally {
			if (reg != null)
				reg.unregister();
		}
	}

	@Test
	public void testServiceReferenceCompare01() {
		final String testMethodName = getName();
		// test that getServiceReference returns the proper service
		Runnable runIt = () -> {
			// nothing
		};
		Hashtable props = new Hashtable();
		props.put("name", testMethodName); //$NON-NLS-1$
		props.put(Constants.SERVICE_DESCRIPTION, "min value"); //$NON-NLS-1$
		props.put(Constants.SERVICE_RANKING, Integer.valueOf(Integer.MIN_VALUE));
		ServiceRegistration reg1 = OSGiTestsActivator.getContext().registerService(Runnable.class.getName(), runIt, props);

		props.put(Constants.SERVICE_DESCRIPTION, "max value first"); //$NON-NLS-1$
		props.put(Constants.SERVICE_RANKING, Integer.valueOf(Integer.MAX_VALUE));
		ServiceRegistration reg2 = OSGiTestsActivator.getContext().registerService(Runnable.class.getName(), runIt, props);

		props.put(Constants.SERVICE_DESCRIPTION, "max value second"); //$NON-NLS-1$
		props.put(Constants.SERVICE_RANKING, Integer.valueOf(Integer.MAX_VALUE));
		ServiceRegistration reg3 = OSGiTestsActivator.getContext().registerService(Runnable.class.getName(), runIt, props);

		try {
			ServiceReference ref = OSGiTestsActivator.getContext().getServiceReference(Runnable.class.getName());
			ServiceReference ref1 = reg1.getReference();
			ServiceReference ref2 = reg2.getReference();
			ServiceReference ref3 = reg3.getReference();

			assertNotNull("service ref is null", ref); //$NON-NLS-1$
			assertEquals("Wrong reference", ref2, ref); //$NON-NLS-1$

			assertEquals("Wrong references", 0, ref2.compareTo(ref)); //$NON-NLS-1$
			assertEquals("Wrong references", 0, ref.compareTo(ref2)); //$NON-NLS-1$

			assertTrue("Wrong compareTo value: " + ref1.compareTo(ref1), ref1.compareTo(ref1) == 0); //$NON-NLS-1$
			assertTrue("Wrong compareTo value: " + ref1.compareTo(ref2), ref1.compareTo(ref2) < 0); //$NON-NLS-1$
			assertTrue("Wrong compareTo value: " + ref1.compareTo(ref3), ref1.compareTo(ref3) < 0); //$NON-NLS-1$

			assertTrue("Wrong compareTo value: " + ref2.compareTo(ref1), ref2.compareTo(ref1) > 0); //$NON-NLS-1$
			assertTrue("Wrong compareTo value: " + ref2.compareTo(ref2), ref2.compareTo(ref2) == 0); //$NON-NLS-1$
			assertTrue("Wrong compareTo value: " + ref2.compareTo(ref3), ref2.compareTo(ref3) > 0); //$NON-NLS-1$

			assertTrue("Wrong compareTo value: " + ref3.compareTo(ref1), ref3.compareTo(ref1) > 0); //$NON-NLS-1$
			assertTrue("Wrong compareTo value: " + ref3.compareTo(ref2), ref3.compareTo(ref2) < 0); //$NON-NLS-1$
			assertTrue("Wrong compareTo value: " + ref3.compareTo(ref3), ref3.compareTo(ref3) == 0); //$NON-NLS-1$
		} finally {
			if (reg1 != null)
				reg1.unregister();
			if (reg2 != null)
				reg2.unregister();
			if (reg3 != null)
				reg3.unregister();
		}
	}

	@Test
	public void testModifiedRanking() {
		Runnable runIt = () -> {
			// nothing
		};
		Hashtable props = new Hashtable();
		props.put(getName(), Boolean.TRUE);
		props.put(Constants.SERVICE_RANKING, Integer.valueOf(15));
		ServiceRegistration reg1 = getContext().registerService(Runnable.class.getName(), runIt, props);
		props.put(Constants.SERVICE_RANKING, Integer.valueOf(10));
		ServiceRegistration reg2 = getContext().registerService(Runnable.class.getName(), runIt, props);
		try {
			assertEquals("wrong service reference", reg1.getReference(), getContext().getServiceReference("java.lang.Runnable")); //$NON-NLS-1$//$NON-NLS-2$

			props.put(Constants.SERVICE_RANKING, Integer.valueOf(20));
			reg2.setProperties(props);
			assertEquals("wrong service reference", reg2.getReference(), getContext().getServiceReference("java.lang.Runnable")); //$NON-NLS-1$//$NON-NLS-2$
		} finally {
			if (reg1 != null)
				reg1.unregister();
			if (reg2 != null)
				reg2.unregister();
		}
	}

	@Test
	public void testInvalidRanking() throws InterruptedException {
		final CountDownLatch warning = new CountDownLatch(1);
		FrameworkListener warningListener = event -> {
			if (FrameworkEvent.WARNING == event.getType() && OSGiTestsActivator.getBundle().equals(event.getBundle())) {
				warning.countDown();
			}
		};
		Runnable runIt = () -> {
			// nothing
		};
		Hashtable props = new Hashtable();
		props.put(getName(), Boolean.TRUE);
		props.put(Constants.SERVICE_RANKING, "15");
		ServiceRegistration reg1 = null;
		try {
			OSGiTestsActivator.getContext().addFrameworkListener(warningListener);
			reg1 = getContext().registerService(Runnable.class.getName(), runIt, props);
		} finally {
			if (reg1 != null) {
				reg1.unregister();
			}
			OSGiTestsActivator.getContext().removeFrameworkListener(warningListener);
		}
		assertTrue("Timeout waiting for the warning.", warning.await(5, TimeUnit.SECONDS));
	}

	@Test
	public void testNullValue() throws InvalidSyntaxException {
		ServiceRegistration reg = null;
		try {
			Dictionary<String, Object> nullProps = new MapDictionary<>();
			nullProps.put("test.null", null);
			nullProps.put("test.non.null", "v1");
			reg = OSGiTestsActivator.getContext().registerService(Object.class, new Object(), nullProps);
			assertFalse(OSGiTestsActivator.getContext().createFilter("(test.null=*)").match(reg.getReference()));
			assertFalse(OSGiTestsActivator.getContext().createFilter("(test.null=*)").match(reg.getReference().getProperties()));
			assertTrue(OSGiTestsActivator.getContext().createFilter("(&(!(test.null=*))(test.non.null=v1))").match(reg.getReference()));
			assertTrue(OSGiTestsActivator.getContext().createFilter("(&(!(test.null=*))(test.non.null=v1))").match(reg.getReference().getProperties()));
		} finally {
			if (reg != null)
				reg.unregister();
		}
	}

	@Test
	public void testNullKey() throws InvalidSyntaxException {
		ServiceRegistration reg = null;
		try {
			Dictionary<String, Object> nullProps = new MapDictionary<>();
			nullProps.put(null, "null.v1");
			nullProps.put("test.non.null", "v1");
			reg = OSGiTestsActivator.getContext().registerService(Object.class, new Object(), nullProps);
			assertTrue(OSGiTestsActivator.getContext().createFilter("(test.non.null=v1)").match(reg.getReference()));
			assertTrue(OSGiTestsActivator.getContext().createFilter("(test.non.null=v1)").match(reg.getReference().getProperties()));
		} finally {
			if (reg != null)
				reg.unregister();
		}
	}

	@Test
	public void testWrongServiceFactoryObject() throws InterruptedException {
		AtomicReference<String> errorMsg = new AtomicReference<>();
		CountDownLatch gotEvent = new CountDownLatch(1);
		FrameworkListener fwkListener = (e) -> {
			if (e.getType() == FrameworkEvent.ERROR && e.getThrowable() != null) {
				errorMsg.set(e.getThrowable().getMessage());
				gotEvent.countDown();
			}
		};
		ServiceRegistration<Runnable> reg = OSGiTestsActivator.getContext().registerService(Runnable.class,
				new ServiceFactory() {

					@Override
					public Object getService(Bundle bundle, ServiceRegistration registration) {
						return "Wrong object!!";
					}

					@Override
					public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
					}

				}, null);
		OSGiTestsActivator.getContext().addFrameworkListener(fwkListener);
		try {
			ServiceReference<Runnable> ref = reg.getReference();
			Runnable service = OSGiTestsActivator.getContext().getService(ref);
			assertNull(service);
			gotEvent.await(30, TimeUnit.SECONDS);
			assertNotNull(errorMsg.get());
			assertTrue("Wrong error message: " + errorMsg.get(), errorMsg.get().contains(String.class.getName()));
		} finally {
			OSGiTestsActivator.getContext().removeFrameworkListener(fwkListener);
			reg.unregister();
		}
	}

	private void clearResults(boolean[] results) {
		for (int i = 0; i < results.length; i++)
			results[i] = false;
	}

	private void clearResults(int[] results) {
		for (int i = 0; i < results.length; i++)
			results[i] = 0;
	}
}
