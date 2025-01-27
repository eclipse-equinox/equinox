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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Hashtable;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.eclipse.osgi.tests.bundles.AbstractBundleTests;
import org.junit.Test;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class ServiceTrackerTests extends AbstractBundleTests {

	@Test
	public void testServiceTracker01() throws InvalidSyntaxException {
		final String testMethodName = getName();
		// simple ServiceTracker test
		Runnable runIt = () -> {
			// nothing
		};
		Hashtable props = new Hashtable();
		props.put(testMethodName, Boolean.TRUE);
		ServiceRegistration reg = OSGiTestsActivator.getContext().registerService(Runnable.class.getName(), runIt,
				props);
		ServiceTracker testTracker = null;
		try {
			final boolean[] results = new boolean[] { false, false, false };
			ServiceTrackerCustomizer testCustomizer = new ServiceTrackerCustomizer() {
				@Override
				public Object addingService(ServiceReference reference) {
					results[0] = true;
					return reference;
				}

				@Override
				public void modifiedService(ServiceReference reference, Object service) {
					results[1] = true;
				}

				@Override
				public void removedService(ServiceReference reference, Object service) {
					results[2] = true;
				}
			};
			Filter filter = FrameworkUtil
					.createFilter("(&(objectclass=java.lang.Runnable)(" + testMethodName.toLowerCase() + "=true))");
			testTracker = new ServiceTracker(OSGiTestsActivator.getContext(), filter, testCustomizer);
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

	@Test
	public void testServiceTracker02() throws InvalidSyntaxException {
		final String testMethodName = getName();
		// simple ServiceTracker test
		Runnable runIt = () -> {
			// nothing
		};
		Hashtable props = new Hashtable();
		props.put(testMethodName, Boolean.FALSE);
		ServiceRegistration reg = OSGiTestsActivator.getContext().registerService(Runnable.class.getName(), runIt,
				props);
		ServiceTracker testTracker = null;
		try {
			final boolean[] results = new boolean[] { false, false, false };
			ServiceTrackerCustomizer testCustomizer = new ServiceTrackerCustomizer() {
				@Override
				public Object addingService(ServiceReference reference) {
					results[0] = true;
					return reference;
				}

				@Override
				public void modifiedService(ServiceReference reference, Object service) {
					results[1] = true;
				}

				@Override
				public void removedService(ServiceReference reference, Object service) {
					results[2] = true;
				}
			};
			Filter filter = FrameworkUtil
					.createFilter("(&(objectclass=java.lang.Runnable)(" + testMethodName.toLowerCase() + "=true))");
			testTracker = new ServiceTracker(OSGiTestsActivator.getContext(), filter, testCustomizer);
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

	@Test
	public void testServiceTracker03() throws InvalidSyntaxException {
		final String testMethodName = getName();
		// simple ServiceTracker test
		Runnable runIt = () -> {
			// nothing
		};
		Hashtable props = new Hashtable();
		props.put(testMethodName, Boolean.TRUE);
		ServiceRegistration reg = OSGiTestsActivator.getContext().registerService(Runnable.class.getName(), runIt,
				props);
		ServiceTracker testTracker = null;
		try {
			final boolean[] results = new boolean[] { false, false, false };
			ServiceTrackerCustomizer testCustomizer = new ServiceTrackerCustomizer() {
				@Override
				public Object addingService(ServiceReference reference) {
					results[0] = true;
					return reference;
				}

				@Override
				public void modifiedService(ServiceReference reference, Object service) {
					results[1] = true;
				}

				@Override
				public void removedService(ServiceReference reference, Object service) {
					results[2] = true;
				}
			};
			Filter filter = FrameworkUtil
					.createFilter("(&(objectclass=java.lang.Runnable)(" + testMethodName.toLowerCase() + "=true))");
			testTracker = new ServiceTracker(OSGiTestsActivator.getContext(), filter, testCustomizer);
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
