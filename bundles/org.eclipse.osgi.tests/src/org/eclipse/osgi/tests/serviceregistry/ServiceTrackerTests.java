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
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class ServiceTrackerTests extends AbstractBundleTests {
	public static Test suite() {
		return new TestSuite(ServiceTrackerTests.class);
	}

	public void testServiceTracker01() {
		final String testMethodName = getName();
		// simple ServiceTracker test
		Runnable runIt = new Runnable() {
			public void run() {
				// nothing
			}
		};
		Hashtable props = new Hashtable();
		props.put(testMethodName, Boolean.TRUE);
		ServiceRegistration reg = OSGiTestsActivator.getContext().registerService(Runnable.class.getName(), runIt, props);
		ServiceTracker testTracker = null;
		try {
			final boolean[] results = new boolean[] {false, false, false};
			ServiceTrackerCustomizer testCustomizer = new ServiceTrackerCustomizer() {
				public Object addingService(ServiceReference reference) {
					results[0] = true;
					return reference;
				}

				public void modifiedService(ServiceReference reference, Object service) {
					results[1] = true;
				}

				public void removedService(ServiceReference reference, Object service) {
					results[2] = true;
				}
			};
			try {
				testTracker = new ServiceTracker(OSGiTestsActivator.getContext(), FrameworkUtil.createFilter("(&(objectclass=java.lang.Runnable)(" + testMethodName.toLowerCase() + "=true))"), testCustomizer); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (InvalidSyntaxException e) {
				fail("filter error", e); //$NON-NLS-1$
			}
			testTracker.open();
			assertTrue("Did not call addingService", results[0]); //$NON-NLS-1$
			assertFalse("Did call modifiedService", results[1]); //$NON-NLS-1$
			assertFalse("Did call removedService", results[2]); //$NON-NLS-1$
			clearResults(results);

			// change props to still match
			props.put("testChangeProp", Boolean.FALSE); //$NON-NLS-1$
			reg.setProperties(props);
			assertFalse("Did call addingService", results[0]); //$NON-NLS-1$
			assertTrue("Did not call modifiedService", results[1]); //$NON-NLS-1$
			assertFalse("Did call removedService", results[2]); //$NON-NLS-1$
			clearResults(results);

			// change props to no longer match
			props.put(testMethodName, Boolean.FALSE);
			reg.setProperties(props);
			assertFalse("Did call addingService", results[0]); //$NON-NLS-1$
			assertFalse("Did call modifiedService", results[1]); //$NON-NLS-1$
			assertTrue("Did not call removedService", results[2]); //$NON-NLS-1$
			clearResults(results);

			// change props to no longer match
			props.put("testChangeProp", Boolean.TRUE); //$NON-NLS-1$
			reg.setProperties(props);
			assertFalse("Did call addingService", results[0]); //$NON-NLS-1$
			assertFalse("Did call modifiedService", results[1]); //$NON-NLS-1$
			assertFalse("Did call removedService", results[2]); //$NON-NLS-1$
			clearResults(results);

			// change props back to match
			props.put(testMethodName, Boolean.TRUE);
			reg.setProperties(props);
			assertTrue("Did not call addingService", results[0]); //$NON-NLS-1$
			assertFalse("Did call modifiedService", results[1]); //$NON-NLS-1$
			assertFalse("Did call removedService", results[2]); //$NON-NLS-1$
			clearResults(results);

		} finally {
			if (reg != null)
				reg.unregister();
			if (testTracker != null)
				testTracker.close();
		}
	}

	public void testServiceTracker02() {
		final String testMethodName = getName();
		// simple ServiceTracker test
		Runnable runIt = new Runnable() {
			public void run() {
				// nothing
			}
		};
		Hashtable props = new Hashtable();
		props.put(testMethodName, Boolean.FALSE);
		ServiceRegistration reg = OSGiTestsActivator.getContext().registerService(Runnable.class.getName(), runIt, props);
		ServiceTracker testTracker = null;
		try {
			final boolean[] results = new boolean[] {false, false, false};
			ServiceTrackerCustomizer testCustomizer = new ServiceTrackerCustomizer() {
				public Object addingService(ServiceReference reference) {
					results[0] = true;
					return reference;
				}

				public void modifiedService(ServiceReference reference, Object service) {
					results[1] = true;
				}

				public void removedService(ServiceReference reference, Object service) {
					results[2] = true;
				}
			};
			try {
				testTracker = new ServiceTracker(OSGiTestsActivator.getContext(), FrameworkUtil.createFilter("(&(objectclass=java.lang.Runnable)(" + testMethodName.toLowerCase() + "=true))"), testCustomizer); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (InvalidSyntaxException e) {
				fail("filter error", e); //$NON-NLS-1$
			}
			testTracker.open();
			assertFalse("Did call addingService", results[0]); //$NON-NLS-1$
			assertFalse("Did call modifiedService", results[1]); //$NON-NLS-1$
			assertFalse("Did call removedService", results[2]); //$NON-NLS-1$
			clearResults(results);

			// change props to match
			props.put(testMethodName, Boolean.TRUE);
			reg.setProperties(props);
			assertTrue("Did not call addingService", results[0]); //$NON-NLS-1$
			assertFalse("Did call modifiedService", results[1]); //$NON-NLS-1$
			assertFalse("Did call removedService", results[2]); //$NON-NLS-1$
			clearResults(results);

			// change props to still match
			props.put("testChangeProp", Boolean.TRUE); //$NON-NLS-1$
			reg.setProperties(props);
			assertFalse("Did call addingService", results[0]); //$NON-NLS-1$
			assertTrue("Did not call modifiedService", results[1]); //$NON-NLS-1$
			assertFalse("Did call removedService", results[2]); //$NON-NLS-1$
			clearResults(results);

			// change props to no longer match
			props.put(testMethodName, Boolean.FALSE);
			reg.setProperties(props);
			assertFalse("Did call addingService", results[0]); //$NON-NLS-1$
			assertFalse("Did call modifiedService", results[1]); //$NON-NLS-1$
			assertTrue("Did not call removedService", results[2]); //$NON-NLS-1$
			clearResults(results);

			// change props to no longer match
			props.put("testChangeProp", Boolean.FALSE); //$NON-NLS-1$
			reg.setProperties(props);
			assertFalse("Did call addingService", results[0]); //$NON-NLS-1$
			assertFalse("Did call modifiedService", results[1]); //$NON-NLS-1$
			assertFalse("Did call removedService", results[2]); //$NON-NLS-1$
			clearResults(results);

		} finally {
			if (reg != null)
				reg.unregister();
			if (testTracker != null)
				testTracker.close();
		}
	}

	public void testServiceTracker03() {
		final String testMethodName = getName();
		// simple ServiceTracker test
		Runnable runIt = new Runnable() {
			public void run() {
				// nothing
			}
		};
		Hashtable props = new Hashtable();
		props.put(testMethodName, Boolean.TRUE);
		ServiceRegistration reg = OSGiTestsActivator.getContext().registerService(Runnable.class.getName(), runIt, props);
		ServiceTracker testTracker = null;
		try {
			final boolean[] results = new boolean[] {false, false, false};
			ServiceTrackerCustomizer testCustomizer = new ServiceTrackerCustomizer() {
				public Object addingService(ServiceReference reference) {
					results[0] = true;
					return reference;
				}

				public void modifiedService(ServiceReference reference, Object service) {
					results[1] = true;
				}

				public void removedService(ServiceReference reference, Object service) {
					results[2] = true;
				}
			};
			try {
				testTracker = new ServiceTracker(OSGiTestsActivator.getContext(), FrameworkUtil.createFilter("(&(objectclass=java.lang.Runnable)(" + testMethodName.toLowerCase() + "=true))"), testCustomizer); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (InvalidSyntaxException e) {
				fail("filter error", e); //$NON-NLS-1$
			}
			testTracker.open();
			assertTrue("Did not call addingService", results[0]); //$NON-NLS-1$
			assertFalse("Did call modifiedService", results[1]); //$NON-NLS-1$
			assertFalse("Did call removedService", results[2]); //$NON-NLS-1$
			clearResults(results);

			// change props to not match
			props.put(testMethodName, Boolean.FALSE);
			reg.setProperties(props);
			assertFalse("Did call addingService", results[0]); //$NON-NLS-1$
			assertFalse("Did call modifiedService", results[1]); //$NON-NLS-1$
			assertTrue("Did not call removedService", results[2]); //$NON-NLS-1$
			clearResults(results);

			// change props to match
			props.put(testMethodName, Boolean.TRUE);
			reg.setProperties(props);
			assertTrue("Did not call addingService", results[0]); //$NON-NLS-1$
			assertFalse("Did call modifiedService", results[1]); //$NON-NLS-1$
			assertFalse("Did call removedService", results[2]); //$NON-NLS-1$
			clearResults(results);

			// change props to still match
			props.put("testChangeProp", Boolean.FALSE); //$NON-NLS-1$
			reg.setProperties(props);
			assertFalse("Did call addingService", results[0]); //$NON-NLS-1$
			assertTrue("Did not call modifiedService", results[1]); //$NON-NLS-1$
			assertFalse("Did call removedService", results[2]); //$NON-NLS-1$
			clearResults(results);
		} finally {
			if (reg != null)
				reg.unregister();
			if (testTracker != null)
				testTracker.close();
		}
	}

	private void clearResults(boolean[] results) {
		for (int i = 0; i < results.length; i++)
			results[i] = false;
	}
}
