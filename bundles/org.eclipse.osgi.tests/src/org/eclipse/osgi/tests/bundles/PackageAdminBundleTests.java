/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.bundles;

import java.util.ArrayList;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.osgi.framework.*;

public class PackageAdminBundleTests extends AbstractBundleTests {
	public class TestListener implements SynchronousBundleListener {
		ArrayList events = new ArrayList();

		public synchronized void bundleChanged(BundleEvent event) {
			events.add(event);
		}

		public synchronized BundleEvent[] getEvents() {
			try {
				return (BundleEvent[]) events.toArray(new BundleEvent[0]);
			} finally {
				events.clear();
			}
		}
	}

	public static Test suite() {
		return new TestSuite(PackageAdminBundleTests.class);
	}

	public void testBundleEvents01() throws Exception {
		Bundle chainTest = installer.installBundle("chain.test");
		Bundle chainTestA = installer.installBundle("chain.test.a");
		Bundle chainTestB = installer.installBundle("chain.test.b");
		Bundle chainTestC = installer.installBundle("chain.test.c");
		Bundle chainTestD = installer.installBundle("chain.test.d");
		Bundle[] resolveBundles = new Bundle[] {chainTestC, chainTestA, chainTestB, chainTest, chainTestD};
		Bundle[] dependencyOrder = new Bundle[] {chainTest, chainTestA, chainTestB, chainTestC, chainTestD};
		TestListener testListener = new TestListener();
		OSGiTestsActivator.getContext().addBundleListener(testListener);
		try {
			installer.resolveBundles(resolveBundles);
			BundleEvent[] events = testListener.getEvents();
			assertEquals("Event count", 10, events.length);
			int j = 0;
			for (int i = dependencyOrder.length - 1; i >= 0; i--, j++) {
				assertTrue("Resolved Event Bundle: " + dependencyOrder[i].getSymbolicName(), dependencyOrder[i] == events[j].getBundle());
				assertEquals("Expecting Resolved event", BundleEvent.RESOLVED, events[j].getType());
			}
			j = 5;
			for (int i = dependencyOrder.length - 1; i >= 0; i--, j++) {
				assertTrue("Lazy Starting Bundle: " + dependencyOrder[i].getSymbolicName(), dependencyOrder[i] == events[j].getBundle());
				assertEquals("Expecting Lazy Starting event", BundleEvent.LAZY_ACTIVATION, events[j].getType());
			}
		} finally {
			OSGiTestsActivator.getContext().removeBundleListener(testListener);
		}
	}

	public void testBundleEvents02() throws Exception {
		Bundle chainTest = installer.installBundle("chain.test");
		Bundle chainTestA = installer.installBundle("chain.test.a");
		Bundle chainTestB = installer.installBundle("chain.test.b");
		Bundle chainTestC = installer.installBundle("chain.test.c");
		Bundle chainTestD = installer.installBundle("chain.test.d");
		Bundle[] resolveBundles = new Bundle[] {chainTestC, chainTestA, chainTestB, chainTest, chainTestD};
		Bundle[] dependencyOrder = new Bundle[] {chainTest, chainTestA, chainTestB, chainTestC, chainTestD};
		TestListener testListener = new TestListener();
		OSGiTestsActivator.getContext().addBundleListener(testListener);
		try {
			installer.resolveBundles(resolveBundles);
			BundleEvent[] events = testListener.getEvents();
			// throw away the events.  This was already tested
			installer.refreshPackages(resolveBundles);
			events = testListener.getEvents();
			assertEquals("Event count", 25, events.length);
			int j = 0;
			for (int i = 0; i < dependencyOrder.length; i++, j += 2) {
				assertTrue("Stopping Event Bundle: " + dependencyOrder[i].getSymbolicName(), dependencyOrder[i] == events[j].getBundle());
				assertEquals("Expecting Stopping event", BundleEvent.STOPPING, events[j].getType());
				assertTrue("Stopped Event Bundle: " + dependencyOrder[i].getSymbolicName(), dependencyOrder[i] == events[j + 1].getBundle());
				assertEquals("Expecting Stopping event", BundleEvent.STOPPED, events[j + 1].getType());
			}
			j = 10;
			for (int i = 0; i < dependencyOrder.length; i++, j++) {
				assertTrue("Unresolved Event Bundle: " + dependencyOrder[i].getSymbolicName(), dependencyOrder[i] == events[j].getBundle());
				assertEquals("Expecting Unresolved event", BundleEvent.UNRESOLVED, events[j].getType());
			}
			j = 15;
			for (int i = dependencyOrder.length - 1; i >= 0; i--, j++) {
				assertTrue("Resolved Event Bundle: " + dependencyOrder[i].getSymbolicName(), dependencyOrder[i] == events[j].getBundle());
				assertEquals("Expecting Resolved event", BundleEvent.RESOLVED, events[j].getType());
			}
			j = 20;
			for (int i = dependencyOrder.length - 1; i >= 0; i--, j++) {
				assertTrue("Lazy Starting Event Bundle: " + dependencyOrder[i].getSymbolicName(), dependencyOrder[i] == events[j].getBundle());
				assertEquals("Expecting Lazy Starting event", BundleEvent.LAZY_ACTIVATION, events[j].getType());
			}

		} finally {
			OSGiTestsActivator.getContext().removeBundleListener(testListener);
		}
	}

	public void testBug259903() throws Exception {
		Bundle bug259903a = installer.installBundle("test.bug259903.a");
		Bundle bug259903b = installer.installBundle("test.bug259903.b");
		Bundle bug259903c = installer.installBundle("test.bug259903.c");

		try {
			installer.resolveBundles(new Bundle[] {bug259903a, bug259903b, bug259903c});
			bug259903c.start();
			bug259903a.uninstall();
			installer.installBundle("test.bug259903.a.update");
			installer.refreshPackages(new Bundle[] {bug259903a});
			Object[] expectedEvents = new Object[] {new BundleEvent(BundleEvent.STOPPED, bug259903c)};
			Object[] actualEvents = simpleResults.getResults(expectedEvents.length);
			compareResults(expectedEvents, actualEvents);
		} catch (Exception e) {
			fail("Unexpected exception", e);
		}
	}
}
