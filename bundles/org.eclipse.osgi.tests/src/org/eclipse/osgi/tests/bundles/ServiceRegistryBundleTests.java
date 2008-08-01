/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.bundles;

import java.util.Hashtable;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class ServiceRegistryBundleTests extends AbstractBundleTests {
	public static Test suite() {
		return new TestSuite(ServiceRegistryBundleTests.class);
	}

	public void testServiceTracker01() {
		// simple ServiceTracker test
		Runnable runIt = new Runnable() {
			public void run() {
				// nothing
			}
		};
		Hashtable props = new Hashtable();
		props.put("testServiceTracker01", Boolean.TRUE); //$NON-NLS-1$
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
				testTracker = new ServiceTracker(OSGiTestsActivator.getContext(), FrameworkUtil.createFilter("(&(objectClass=java.lang.Runnable)(testServiceTracker01=true))"), testCustomizer); //$NON-NLS-1$
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
			props.put("testServiceTracker01", Boolean.FALSE); //$NON-NLS-1$
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
			props.put("testServiceTracker01", Boolean.TRUE); //$NON-NLS-1$
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
		// simple ServiceTracker test
		Runnable runIt = new Runnable() {
			public void run() {
				// nothing
			}
		};
		Hashtable props = new Hashtable();
		props.put("testServiceTracker02", Boolean.FALSE); //$NON-NLS-1$
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
				testTracker = new ServiceTracker(OSGiTestsActivator.getContext(), FrameworkUtil.createFilter("(&(objectClass=java.lang.Runnable)(testServiceTracker02=true))"), testCustomizer); //$NON-NLS-1$
			} catch (InvalidSyntaxException e) {
				fail("filter error", e); //$NON-NLS-1$
			}
			testTracker.open();
			assertFalse("Did call addingService", results[0]); //$NON-NLS-1$
			assertFalse("Did call modifiedService", results[1]); //$NON-NLS-1$
			assertFalse("Did call removedService", results[2]); //$NON-NLS-1$
			clearResults(results);

			// change props to match
			props.put("testServiceTracker02", Boolean.TRUE); //$NON-NLS-1$
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
			props.put("testServiceTracker02", Boolean.FALSE); //$NON-NLS-1$
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

	public void testServiceListener01() {
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
			OSGiTestsActivator.getContext().addServiceListener(testListener, "(&(objectClass=java.lang.Runnable)(testServiceListener01=true))"); //$NON-NLS-1$
		} catch (InvalidSyntaxException e) {
			fail("filter error", e); //$NON-NLS-1$
		}
		ServiceRegistration reg = null;
		try {
			// register service which matches
			Hashtable props = new Hashtable();
			props.put("testServiceListener01", Boolean.TRUE); //$NON-NLS-1$
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
			props.put("testServiceListener01", Boolean.FALSE); //$NON-NLS-1$
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
			props.put("testServiceListener01", Boolean.TRUE); //$NON-NLS-1$
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
			OSGiTestsActivator.getContext().addServiceListener(testListener, "(&(objectClass=java.lang.Runnable)(testServiceListener02=true))"); //$NON-NLS-1$
		} catch (InvalidSyntaxException e) {
			fail("filter error", e); //$NON-NLS-1$
		}
		ServiceRegistration reg = null;
		try {
			// register service which does not match
			Hashtable props = new Hashtable();
			props.put("testServiceListener02", Boolean.FALSE); //$NON-NLS-1$
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
			props.put("testServiceListener02", Boolean.TRUE); //$NON-NLS-1$
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
			props.put("testServiceListener02", Boolean.FALSE); //$NON-NLS-1$
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

	private void clearResults(boolean[] results) {
		for (int i = 0; i < results.length; i++)
			results[i] = false;
	}
}
