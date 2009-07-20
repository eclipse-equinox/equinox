/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.serviceregistry;

import java.util.Hashtable;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.eclipse.osgi.tests.bundles.AbstractBundleTests;
import org.osgi.framework.*;

public class ServiceRegistryTests extends AbstractBundleTests {
	public static Test suite() {
		TestSuite suite = new TestSuite(ServiceRegistryTests.class.getName());
		suite.addTest(new TestSuite(ServiceRegistryTests.class));
		suite.addTest(ServiceExceptionTests.suite());
		suite.addTest(ServiceHookTests.suite());
		suite.addTest(ServiceTrackerTests.suite());
		return suite;
	}

	public void testServiceListener01() {
		final String testMethodName = getName();
		// simple ServiceListener test
		Runnable runIt = new Runnable() {
			public void run() {
				// nothing
			}
		};
		final boolean[] results = new boolean[] {false, false, false, false};
		ServiceListener testListener = new ServiceListener() {
			public void serviceChanged(ServiceEvent event) {
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
			}
		};
		try {
			OSGiTestsActivator.getContext().addServiceListener(testListener, "(&(objectclass=java.lang.Runnable)(" + testMethodName.toLowerCase() + "=true))"); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (InvalidSyntaxException e) {
			fail("filter error", e); //$NON-NLS-1$
		}
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

	public void testServiceListener02() {
		final String testMethodName = getName();
		// simple ServiceListener test
		Runnable runIt = new Runnable() {
			public void run() {
				// nothing
			}
		};
		final boolean[] results = new boolean[] {false, false, false, false};
		ServiceListener testListener = new ServiceListener() {
			public void serviceChanged(ServiceEvent event) {
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
			}
		};
		try {
			OSGiTestsActivator.getContext().addServiceListener(testListener, "(&(objectclass=java.lang.Runnable)(" + testMethodName.toLowerCase() + "=true))"); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (InvalidSyntaxException e) {
			fail("filter error", e); //$NON-NLS-1$
		}
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

	public void testServiceListener03() {
		final String testMethodName = getName();
		// simple ServiceListener test
		Runnable runIt = new Runnable() {
			public void run() {
				// nothing
			}
		};
		final int[] results = new int[] {0, 0, 0, 0};
		ServiceListener testListener = new ServiceListener() {
			public void serviceChanged(ServiceEvent event) {
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
			}
		};
		try {
			OSGiTestsActivator.getContext().addServiceListener(testListener, "(&(objectclass=java.lang.Runnable)(" + testMethodName.toLowerCase() + "=true))"); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (InvalidSyntaxException e) {
			fail("filter error", e); //$NON-NLS-1$
		}
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

	public void testServiceOrdering01() {
		final String testMethodName = getName();
		// test that getServiceReference returns the proper service
		Runnable runIt = new Runnable() {
			public void run() {
				// nothing
			}
		};
		Hashtable props = new Hashtable();
		props.put("name", testMethodName); //$NON-NLS-1$ 
		props.put(Constants.SERVICE_DESCRIPTION, "min value"); //$NON-NLS-1$
		props.put(Constants.SERVICE_RANKING, new Integer(Integer.MIN_VALUE));
		ServiceRegistration reg1 = OSGiTestsActivator.getContext().registerService(Runnable.class.getName(), runIt, props);

		props.put(Constants.SERVICE_DESCRIPTION, "max value first"); //$NON-NLS-1$
		props.put(Constants.SERVICE_RANKING, new Integer(Integer.MAX_VALUE));
		ServiceRegistration reg2 = OSGiTestsActivator.getContext().registerService(Runnable.class.getName(), runIt, props);

		props.put(Constants.SERVICE_DESCRIPTION, "max value second"); //$NON-NLS-1$
		props.put(Constants.SERVICE_RANKING, new Integer(Integer.MAX_VALUE));
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

	public void testDuplicateObjectClass() {
		ServiceRegistration reg = null;
		try {
			reg = OSGiTestsActivator.getContext().registerService(new String[] {Runnable.class.getName(), Object.class.getName(), Object.class.getName()}, new Runnable() {
				public void run() {
					// nothing
				}
			}, null);
		} catch (Throwable t) {
			fail("Failed to register service with duplicate objectClass names", t); //$NON-NLS-1$
		} finally {
			if (reg != null)
				reg.unregister();
		}
	}

	public void testServiceReferenceCompare01() {
		final String testMethodName = getName();
		// test that getServiceReference returns the proper service
		Runnable runIt = new Runnable() {
			public void run() {
				// nothing
			}
		};
		Hashtable props = new Hashtable();
		props.put("name", testMethodName); //$NON-NLS-1$ 
		props.put(Constants.SERVICE_DESCRIPTION, "min value"); //$NON-NLS-1$
		props.put(Constants.SERVICE_RANKING, new Integer(Integer.MIN_VALUE));
		ServiceRegistration reg1 = OSGiTestsActivator.getContext().registerService(Runnable.class.getName(), runIt, props);

		props.put(Constants.SERVICE_DESCRIPTION, "max value first"); //$NON-NLS-1$
		props.put(Constants.SERVICE_RANKING, new Integer(Integer.MAX_VALUE));
		ServiceRegistration reg2 = OSGiTestsActivator.getContext().registerService(Runnable.class.getName(), runIt, props);

		props.put(Constants.SERVICE_DESCRIPTION, "max value second"); //$NON-NLS-1$
		props.put(Constants.SERVICE_RANKING, new Integer(Integer.MAX_VALUE));
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

	private void clearResults(boolean[] results) {
		for (int i = 0; i < results.length; i++)
			results[i] = false;
	}

	private void clearResults(int[] results) {
		for (int i = 0; i < results.length; i++)
			results[i] = 0;
	}
}
