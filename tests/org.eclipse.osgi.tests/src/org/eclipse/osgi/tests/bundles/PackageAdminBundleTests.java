/*******************************************************************************
 * Copyright (c) 2007, 2022 IBM Corporation and others.
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
package org.eclipse.osgi.tests.bundles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.framework.Version;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;

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

	public class Bug289719Listener implements SynchronousBundleListener {

		ArrayList expectedEvents = new ArrayList();
		ArrayList failures = new ArrayList();
		int i = 0;

		public synchronized void setExpectedEvents(BundleEvent[] events) {
			i = 0;
			failures.clear();
			expectedEvents.clear();
			expectedEvents.addAll(Arrays.asList(events));
		}

		public synchronized void bundleChanged(BundleEvent event) {
			BundleEvent expected = expectedEvents.size() == 0 ? null : (BundleEvent) expectedEvents.remove(0);
			try {
				assertEqualEvent("Compare results: " + i, expected, event);
			} catch (Throwable t) {
				failures.add(t);
			} finally {
				i++;
			}
		}

		public synchronized Throwable[] getFailures() {
			Throwable[] results = (Throwable[]) failures.toArray(new Throwable[failures.size()]);
			setExpectedEvents(new BundleEvent[0]);
			return results;
		}
	}

	@Test
	public void testBundleEvents01() throws Exception {
		Bundle chainTest = installer.installBundle("chain.test"); //$NON-NLS-1$
		Bundle chainTestA = installer.installBundle("chain.test.a"); //$NON-NLS-1$
		Bundle chainTestB = installer.installBundle("chain.test.b"); //$NON-NLS-1$
		Bundle chainTestC = installer.installBundle("chain.test.c"); //$NON-NLS-1$
		Bundle chainTestD = installer.installBundle("chain.test.d"); //$NON-NLS-1$
		Bundle[] resolveBundles = new Bundle[] {chainTestC, chainTestA, chainTestB, chainTest, chainTestD};
		Bundle[] dependencyOrder = new Bundle[] {chainTest, chainTestA, chainTestB, chainTestC, chainTestD};
		TestListener testListener = new TestListener();
		OSGiTestsActivator.getContext().addBundleListener(testListener);
		try {
			installer.resolveBundles(resolveBundles);
			BundleEvent[] events = testListener.getEvents();
			assertEquals("Event count", 10, events.length); //$NON-NLS-1$
			int j = 0;
			for (int i = dependencyOrder.length - 1; i >= 0; i--, j++) {
				assertTrue("Resolved Event Bundle: " + dependencyOrder[i].getSymbolicName(), dependencyOrder[i] == events[j].getBundle()); //$NON-NLS-1$
				assertEquals("Expecting Resolved event", BundleEvent.RESOLVED, events[j].getType()); //$NON-NLS-1$
			}
			j = 5;
			for (int i = dependencyOrder.length - 1; i >= 0; i--, j++) {
				assertTrue("Lazy Starting Bundle: " + dependencyOrder[i].getSymbolicName(), dependencyOrder[i] == events[j].getBundle()); //$NON-NLS-1$
				assertEquals("Expecting Lazy Starting event", BundleEvent.LAZY_ACTIVATION, events[j].getType()); //$NON-NLS-1$
			}
		} finally {
			OSGiTestsActivator.getContext().removeBundleListener(testListener);
		}
	}

	@Test
	public void testBundleEvents02() throws Exception {
		Bundle chainTest = installer.installBundle("chain.test"); //$NON-NLS-1$
		Bundle chainTestA = installer.installBundle("chain.test.a"); //$NON-NLS-1$
		Bundle chainTestB = installer.installBundle("chain.test.b"); //$NON-NLS-1$
		Bundle chainTestC = installer.installBundle("chain.test.c"); //$NON-NLS-1$
		Bundle chainTestD = installer.installBundle("chain.test.d"); //$NON-NLS-1$
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
			assertEquals("Event count", 25, events.length); //$NON-NLS-1$
			int j = 0;
			for (int i = 0; i < dependencyOrder.length; i++, j += 2) {
				assertTrue("Stopping Event Bundle: " + dependencyOrder[i].getSymbolicName(), dependencyOrder[i] == events[j].getBundle()); //$NON-NLS-1$
				assertEquals("Expecting Stopping event", BundleEvent.STOPPING, events[j].getType()); //$NON-NLS-1$
				assertTrue("Stopped Event Bundle: " + dependencyOrder[i].getSymbolicName(), dependencyOrder[i] == events[j + 1].getBundle()); //$NON-NLS-1$
				assertEquals("Expecting Stopping event", BundleEvent.STOPPED, events[j + 1].getType()); //$NON-NLS-1$
			}
			j = 10;
			for (int i = 0; i < dependencyOrder.length; i++, j++) {
				assertTrue("Unresolved Event Bundle: " + dependencyOrder[i].getSymbolicName(), dependencyOrder[i] == events[j].getBundle()); //$NON-NLS-1$
				assertEquals("Expecting Unresolved event", BundleEvent.UNRESOLVED, events[j].getType()); //$NON-NLS-1$
			}
			j = 15;
			for (int i = dependencyOrder.length - 1; i >= 0; i--, j++) {
				assertTrue("Resolved Event Bundle: " + dependencyOrder[i].getSymbolicName(), dependencyOrder[i] == events[j].getBundle()); //$NON-NLS-1$
				assertEquals("Expecting Resolved event", BundleEvent.RESOLVED, events[j].getType()); //$NON-NLS-1$
			}
			j = 20;
			for (int i = dependencyOrder.length - 1; i >= 0; i--, j++) {
				assertTrue("Lazy Starting Event Bundle: " + dependencyOrder[i].getSymbolicName(), dependencyOrder[i] == events[j].getBundle()); //$NON-NLS-1$
				assertEquals("Expecting Lazy Starting event", BundleEvent.LAZY_ACTIVATION, events[j].getType()); //$NON-NLS-1$
			}

		} finally {
			OSGiTestsActivator.getContext().removeBundleListener(testListener);
		}
	}

	@Test
	public void testBug259903() throws Exception {
		Bundle bug259903a = installer.installBundle("test.bug259903.a"); //$NON-NLS-1$
		Bundle bug259903b = installer.installBundle("test.bug259903.b"); //$NON-NLS-1$
		Bundle bug259903c = installer.installBundle("test.bug259903.c"); //$NON-NLS-1$

		installer.resolveBundles(new Bundle[] { bug259903a, bug259903b, bug259903c });
		bug259903c.start();
		bug259903a.uninstall();
		installer.installBundle("test.bug259903.a.update"); //$NON-NLS-1$
		installer.refreshPackages(new Bundle[] { bug259903a });
		Object[] expectedEvents = new Object[] { new BundleEvent(BundleEvent.STOPPED, bug259903c) };
		Object[] actualEvents = simpleResults.getResults(expectedEvents.length);
		compareResults(expectedEvents, actualEvents);
	}

	@Test
	public void testBug287636() throws Exception {
		Bundle bug287636a = installer.installBundle("test.bug287636.a1"); //$NON-NLS-1$
		Bundle bug287636b = installer.installBundle("test.bug287636.b"); //$NON-NLS-1$
		bug287636a.start();
		bug287636b.start();
		assertTrue("Bundles are not resolved", installer.resolveBundles(new Bundle[] { bug287636a, bug287636b })); //$NON-NLS-1$
		ExportedPackage ep = installer.getPackageAdmin().getExportedPackage("test.bug287636.a"); //$NON-NLS-1$
		assertNotNull("Could not find exported package", ep); //$NON-NLS-1$
		assertEquals("Wrong version", new Version(1, 0, 0), ep.getVersion()); //$NON-NLS-1$
		// update bundle to export new 1.1.0 version of the pacakge
		String updateLocation = installer.getBundleLocation("test.bug287636.a2"); //$NON-NLS-1$
		bug287636a.update(new URL(updateLocation).openStream());
		bug287636b.update();
		updateLocation = installer.getBundleLocation("test.bug287636.a1"); //$NON-NLS-1$
		bug287636a.update(new URL(updateLocation).openStream());
		bug287636b.update();
		updateLocation = installer.getBundleLocation("test.bug287636.a2"); //$NON-NLS-1$
		bug287636a.update(new URL(updateLocation).openStream());
		bug287636b.update();
		installer.refreshPackages(null);
		ep = installer.getPackageAdmin().getExportedPackage("test.bug287636.a"); //$NON-NLS-1$

		assertNotNull("Could not find exported package", ep); //$NON-NLS-1$
		assertEquals("Wrong version", new Version(1, 1, 0), ep.getVersion()); //$NON-NLS-1$
		ExportedPackage eps[] = installer.getPackageAdmin().getExportedPackages("test.bug287636.a"); //$NON-NLS-1$
		assertNotNull("Could not find exported package", eps); //$NON-NLS-1$
		assertEquals("Wrong number of exports", 1, eps.length); //$NON-NLS-1$
		assertEquals("Wrong version", new Version(1, 1, 0), eps[0].getVersion()); //$NON-NLS-1$
		eps = installer.getPackageAdmin().getExportedPackages(bug287636a);
		assertNotNull("Could not find exported package", eps); //$NON-NLS-1$
		assertEquals("Wrong number of exports", 1, eps.length); //$NON-NLS-1$
		assertEquals("Wrong version", new Version(1, 1, 0), eps[0].getVersion()); //$NON-NLS-1$
	}

	@Test
	public void testBug289719() throws Exception {
		Bundle bug259903a = installer.installBundle("test.bug259903.a"); //$NON-NLS-1$
		Bundle bug259903b = installer.installBundle("test.bug259903.b"); //$NON-NLS-1$
		Bundle bug259903c = installer.installBundle("test.bug259903.c"); //$NON-NLS-1$
		Bug289719Listener testListener = new Bug289719Listener();

		try {
			installer.resolveBundles(new Bundle[] {bug259903a, bug259903b, bug259903c});
			bug259903a.start();
			bug259903b.start();
			bug259903c.start();
			installer.getStartLevel().setBundleStartLevel(bug259903c, 2);
			installer.getStartLevel().setBundleStartLevel(bug259903b, 3);
			installer.getStartLevel().setBundleStartLevel(bug259903a, 4);
			OSGiTestsActivator.getContext().addBundleListener(testListener);
			BundleEvent[] expectedEvents = new BundleEvent[] {new BundleEvent(BundleEvent.STOPPING, bug259903a), new BundleEvent(BundleEvent.STOPPED, bug259903a), new BundleEvent(BundleEvent.STOPPING, bug259903b), new BundleEvent(BundleEvent.STOPPED, bug259903b), new BundleEvent(BundleEvent.STOPPING, bug259903c), new BundleEvent(BundleEvent.STOPPED, bug259903c), new BundleEvent(BundleEvent.UNRESOLVED, bug259903a), new BundleEvent(BundleEvent.UNRESOLVED, bug259903b), new BundleEvent(BundleEvent.UNRESOLVED, bug259903c), new BundleEvent(BundleEvent.RESOLVED, bug259903c), new BundleEvent(BundleEvent.RESOLVED, bug259903b), new BundleEvent(BundleEvent.RESOLVED, bug259903a), new BundleEvent(BundleEvent.STARTING, bug259903c), new BundleEvent(BundleEvent.STARTED, bug259903c),
					new BundleEvent(BundleEvent.STARTING, bug259903b), new BundleEvent(BundleEvent.STARTED, bug259903b), new BundleEvent(BundleEvent.STARTING, bug259903a), new BundleEvent(BundleEvent.STARTED, bug259903a)};
			testListener.setExpectedEvents(expectedEvents);
			// add a small delay to ensure the async bundle start-level changes above are done (bug 300820)
			Thread.sleep(500);
			installer.refreshPackages(new Bundle[] {bug259903a});
			Throwable[] results = testListener.getFailures();
			assertEquals(getMessage(results), 0, results.length);

			expectedEvents = new BundleEvent[] {new BundleEvent(BundleEvent.STOPPING, bug259903c), new BundleEvent(BundleEvent.STOPPED, bug259903c), new BundleEvent(BundleEvent.STOPPING, bug259903b), new BundleEvent(BundleEvent.STOPPED, bug259903b), new BundleEvent(BundleEvent.STOPPING, bug259903a), new BundleEvent(BundleEvent.STOPPED, bug259903a), new BundleEvent(BundleEvent.UNRESOLVED, bug259903c), new BundleEvent(BundleEvent.UNRESOLVED, bug259903b), new BundleEvent(BundleEvent.UNRESOLVED, bug259903a), new BundleEvent(BundleEvent.RESOLVED, bug259903a), new BundleEvent(BundleEvent.RESOLVED, bug259903b), new BundleEvent(BundleEvent.RESOLVED, bug259903c), new BundleEvent(BundleEvent.STARTING, bug259903a), new BundleEvent(BundleEvent.STARTED, bug259903a),
					new BundleEvent(BundleEvent.STARTING, bug259903b), new BundleEvent(BundleEvent.STARTED, bug259903b), new BundleEvent(BundleEvent.STARTING, bug259903c), new BundleEvent(BundleEvent.STARTED, bug259903c)};
			testListener.setExpectedEvents(expectedEvents);
			installer.getStartLevel().setBundleStartLevel(bug259903c, 4);
			installer.getStartLevel().setBundleStartLevel(bug259903b, 4);
			installer.getStartLevel().setBundleStartLevel(bug259903a, 4);
			// add a small delay to ensure the async bundle start-level changes above are done (bug 300820)
			Thread.sleep(500);
			installer.refreshPackages(new Bundle[] {bug259903a});
			results = testListener.getFailures();
			assertEquals(getMessage(results), 0, results.length);

		} finally {
			OSGiTestsActivator.getContext().removeBundleListener(testListener);
		}
	}

	@Test
	public void testBug415447() {
		PackageAdmin pa = installer.getPackageAdmin();
		Bundle[] systemBundles = pa.getBundles(Constants.SYSTEM_BUNDLE_SYMBOLICNAME, null);
		assertNotNull("No system bundles found.", systemBundles);
		assertEquals("Srong number of system bundles.", 1, systemBundles.length);
		assertEquals("Wrong system bundle found.", OSGiTestsActivator.getContext().getBundle(Constants.SYSTEM_BUNDLE_LOCATION), systemBundles[0]);
	}

	@Test
	public void testUninstallWhileResolving() throws BundleException {
		ServiceRegistration<ResolverHookFactory> resolverHookReg = getContext().registerService(ResolverHookFactory.class, triggers -> new ResolverHook() {

			@Override
			public void filterSingletonCollisions(BundleCapability singleton, Collection<BundleCapability> collisionCandidates) {
				// Nothing
			}

			@Override
			public void filterResolvable(Collection<BundleRevision> candidates) {
				// prevent all resolves
				candidates.clear();
			}

			@Override
			public void filterMatches(BundleRequirement requirement, Collection<BundleCapability> candidates) {
				// nothing
			}

			@Override
			public void end() {
				// nothing
			}
		}, null);
		try {
			Bundle b1 = installer.installBundle("test.uninstall.start1"); //$NON-NLS-1$
			Bundle b2 = installer.installBundle("test.uninstall.start2"); //$NON-NLS-1$
			try {
				b1.start();
			} catch (BundleException e) {
				// expected
			}
			try {
				b2.start();
			} catch (BundleException e) {
				// expected
			}
			resolverHookReg.unregister();
			resolverHookReg = null;
			getContext().getBundle(Constants.SYSTEM_BUNDLE_LOCATION).adapt(FrameworkWiring.class).resolveBundles(Arrays.asList(b1, b2));
		} finally {
			if (resolverHookReg != null) {
				resolverHookReg.unregister();
			}
		}
	}

	private String getMessage(Throwable[] results) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		for (Throwable result : results) {
			result.printStackTrace(pw);
		}
		return sw.toString();
	}
}
