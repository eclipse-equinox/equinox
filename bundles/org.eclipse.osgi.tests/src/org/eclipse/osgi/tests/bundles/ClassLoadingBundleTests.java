/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.bundles;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.osgi.framework.*;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;

public class ClassLoadingBundleTests extends AbstractBundleTests {
	public static Test suite() {
		return new TestSuite(ClassLoadingBundleTests.class);
	}

	public void testSimple() throws Exception {
		Bundle test = installer.installBundle("test"); //$NON-NLS-1$
		test.start();
		installer.shutdown();

		Object[] expectedEvents = new Object[6];
		expectedEvents[0] = new BundleEvent(BundleEvent.INSTALLED, test);
		expectedEvents[1] = new BundleEvent(BundleEvent.RESOLVED, test);
		expectedEvents[2] = new BundleEvent(BundleEvent.STARTED, test);
		expectedEvents[3] = new BundleEvent(BundleEvent.STOPPED, test);
		expectedEvents[4] = new BundleEvent(BundleEvent.UNRESOLVED, test);
		expectedEvents[5] = new BundleEvent(BundleEvent.UNINSTALLED, test);

		Object[] actualEvents = listenerResults.getResults(expectedEvents.length);
		compareResults(expectedEvents, actualEvents);
	}

	public void testChainDepedencies() throws Exception {
		Bundle chainTest = installer.installBundle("chain.test"); //$NON-NLS-1$
		Bundle chainTestA = installer.installBundle("chain.test.a"); //$NON-NLS-1$
		Bundle chainTestB = installer.installBundle("chain.test.b"); //$NON-NLS-1$
		installer.installBundle("chain.test.c"); //$NON-NLS-1$
		installer.installBundle("chain.test.d"); //$NON-NLS-1$
		((ITestRunner) chainTest.loadClass("chain.test.TestSingleChain").newInstance()).testIt(); //$NON-NLS-1$

		Object[] expectedEvents = new Object[6];
		expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, chainTestB);
		expectedEvents[1] = new BundleEvent(BundleEvent.STARTED, chainTestA);
		expectedEvents[2] = new BundleEvent(BundleEvent.STOPPED, chainTestA);
		expectedEvents[3] = new BundleEvent(BundleEvent.STOPPED, chainTestB);
		expectedEvents[4] = new BundleEvent(BundleEvent.STARTED, chainTestB);
		expectedEvents[5] = new BundleEvent(BundleEvent.STARTED, chainTestA);

		installer.refreshPackages(new Bundle[] {chainTestB});

		((ITestRunner) chainTest.loadClass("chain.test.TestSingleChain").newInstance()).testIt(); //$NON-NLS-1$

		Object[] actualEvents = simpleResults.getResults(6);
		compareResults(expectedEvents, actualEvents);
	}

	public void testMultiChainDepedencies01() throws Exception {
		Bundle chainTest = installer.installBundle("chain.test"); //$NON-NLS-1$
		Bundle chainTestA = installer.installBundle("chain.test.a"); //$NON-NLS-1$
		Bundle chainTestB = installer.installBundle("chain.test.b"); //$NON-NLS-1$
		Bundle chainTestC = installer.installBundle("chain.test.c"); //$NON-NLS-1$
		Bundle chainTestD = installer.installBundle("chain.test.d"); //$NON-NLS-1$
		chainTest.loadClass("chain.test.TestMultiChain").newInstance(); //$NON-NLS-1$

		Object[] expectedEvents = new Object[12];
		expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, chainTestD);
		expectedEvents[1] = new BundleEvent(BundleEvent.STARTED, chainTestB);
		expectedEvents[2] = new BundleEvent(BundleEvent.STARTED, chainTestC);
		expectedEvents[3] = new BundleEvent(BundleEvent.STARTED, chainTestA);
		expectedEvents[4] = new BundleEvent(BundleEvent.STOPPED, chainTestA);
		expectedEvents[5] = new BundleEvent(BundleEvent.STOPPED, chainTestB);
		expectedEvents[6] = new BundleEvent(BundleEvent.STOPPED, chainTestC);
		expectedEvents[7] = new BundleEvent(BundleEvent.STOPPED, chainTestD);
		expectedEvents[8] = new BundleEvent(BundleEvent.STARTED, chainTestD);
		expectedEvents[9] = new BundleEvent(BundleEvent.STARTED, chainTestC);
		expectedEvents[10] = new BundleEvent(BundleEvent.STARTED, chainTestB);
		expectedEvents[11] = new BundleEvent(BundleEvent.STARTED, chainTestA);

		installer.refreshPackages(new Bundle[] {chainTestC, chainTestD});

		Object[] actualEvents = simpleResults.getResults(8);
		compareResults(expectedEvents, actualEvents);
	}

	public void testMultiChainDepedencies02() throws Exception {
		Bundle chainTestA = installer.installBundle("chain.test.a"); //$NON-NLS-1$
		Bundle chainTestB = installer.installBundle("chain.test.b"); //$NON-NLS-1$
		Bundle chainTestC = installer.installBundle("chain.test.c"); //$NON-NLS-1$
		Bundle chainTestD = installer.installBundle("chain.test.d"); //$NON-NLS-1$
		syncListenerResults.getResults(0);
		installer.resolveBundles(new Bundle[] {chainTestA, chainTestB, chainTestC, chainTestD});

		Object[] expectedEvents = new Object[8];
		expectedEvents[0] = new BundleEvent(BundleEvent.RESOLVED, chainTestD);
		expectedEvents[1] = new BundleEvent(BundleEvent.RESOLVED, chainTestC);
		expectedEvents[2] = new BundleEvent(BundleEvent.RESOLVED, chainTestB);
		expectedEvents[3] = new BundleEvent(BundleEvent.RESOLVED, chainTestA);
		expectedEvents[4] = new BundleEvent(BundleEvent.LAZY_ACTIVATION, chainTestD);
		expectedEvents[5] = new BundleEvent(BundleEvent.LAZY_ACTIVATION, chainTestC);
		expectedEvents[6] = new BundleEvent(BundleEvent.LAZY_ACTIVATION, chainTestB);
		expectedEvents[7] = new BundleEvent(BundleEvent.LAZY_ACTIVATION, chainTestA);

		Object[] actualEvents = syncListenerResults.getResults(8);
		compareResults(expectedEvents, actualEvents);

		installer.refreshPackages(new Bundle[] {chainTestC, chainTestD});

		expectedEvents = new Object[20];
		expectedEvents[0] = new BundleEvent(BundleEvent.STOPPING, chainTestA);
		expectedEvents[1] = new BundleEvent(BundleEvent.STOPPED, chainTestA);
		expectedEvents[2] = new BundleEvent(BundleEvent.STOPPING, chainTestB);
		expectedEvents[3] = new BundleEvent(BundleEvent.STOPPED, chainTestB);
		expectedEvents[4] = new BundleEvent(BundleEvent.STOPPING, chainTestC);
		expectedEvents[5] = new BundleEvent(BundleEvent.STOPPED, chainTestC);
		expectedEvents[6] = new BundleEvent(BundleEvent.STOPPING, chainTestD);
		expectedEvents[7] = new BundleEvent(BundleEvent.STOPPED, chainTestD);
		expectedEvents[8] = new BundleEvent(BundleEvent.UNRESOLVED, chainTestA);
		expectedEvents[9] = new BundleEvent(BundleEvent.UNRESOLVED, chainTestB);
		expectedEvents[10] = new BundleEvent(BundleEvent.UNRESOLVED, chainTestC);
		expectedEvents[11] = new BundleEvent(BundleEvent.UNRESOLVED, chainTestD);
		expectedEvents[12] = new BundleEvent(BundleEvent.RESOLVED, chainTestD);
		expectedEvents[13] = new BundleEvent(BundleEvent.RESOLVED, chainTestC);
		expectedEvents[14] = new BundleEvent(BundleEvent.RESOLVED, chainTestB);
		expectedEvents[15] = new BundleEvent(BundleEvent.RESOLVED, chainTestA);
		expectedEvents[16] = new BundleEvent(BundleEvent.LAZY_ACTIVATION, chainTestD);
		expectedEvents[17] = new BundleEvent(BundleEvent.LAZY_ACTIVATION, chainTestC);
		expectedEvents[18] = new BundleEvent(BundleEvent.LAZY_ACTIVATION, chainTestB);
		expectedEvents[19] = new BundleEvent(BundleEvent.LAZY_ACTIVATION, chainTestA);

		actualEvents = syncListenerResults.getResults(12);
		compareResults(expectedEvents, actualEvents);
	}

	public void testClassCircularityError() throws Exception {
		Bundle circularityTest = installer.installBundle("circularity.test"); //$NON-NLS-1$
		Bundle circularityTestA = installer.installBundle("circularity.test.a"); //$NON-NLS-1$
		circularityTest.loadClass("circularity.test.TestCircularity"); //$NON-NLS-1$

		Object[] expectedEvents = new Object[2];
		expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, circularityTest);
		expectedEvents[1] = new BundleEvent(BundleEvent.STARTED, circularityTestA);
		Object[] actualEvents = simpleResults.getResults(2);
		compareResults(expectedEvents, actualEvents);
	}

	public void testFragmentPackageAccess() throws Exception {
		Bundle hostA = installer.installBundle("fragment.test.attach.host.a"); //$NON-NLS-1$
		Bundle fragA = installer.installBundle("fragment.test.attach.frag.a"); //$NON-NLS-1$
		assertTrue("Host/Frag resolve", installer.resolveBundles(new Bundle[] {hostA, fragA})); //$NON-NLS-1$

		ITestRunner testRunner = (ITestRunner) hostA.loadClass("fragment.test.attach.host.a.internal.test.TestPackageAccess").newInstance(); //$NON-NLS-1$
		try {
			testRunner.testIt();
		} catch (Exception e) {
			fail("Failed package access test: " + e.getMessage()); //$NON-NLS-1$
		}
	}

	public void testLegacyLazyStart() throws Exception {
		Bundle legacy = installer.installBundle("legacy.lazystart"); //$NON-NLS-1$
		Bundle legacyA = installer.installBundle("legacy.lazystart.a"); //$NON-NLS-1$
		Bundle legacyB = installer.installBundle("legacy.lazystart.b"); //$NON-NLS-1$
		Bundle legacyC = installer.installBundle("legacy.lazystart.c"); //$NON-NLS-1$
		assertTrue("legacy lazy start resolve", installer.resolveBundles(new Bundle[] {legacy, legacyA, legacyB, legacyC})); //$NON-NLS-1$

		((ITestRunner) legacy.loadClass("legacy.lazystart.SimpleLegacy").newInstance()).testIt(); //$NON-NLS-1$
		Object[] expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, legacyA);
		Object[] actualEvents = simpleResults.getResults(1);
		compareResults(expectedEvents, actualEvents);

		((ITestRunner) legacy.loadClass("legacy.lazystart.TrueExceptionLegacy1").newInstance()).testIt(); //$NON-NLS-1$
		assertTrue("exceptions no event", simpleResults.getResults(0).length == 0); //$NON-NLS-1$
		((ITestRunner) legacy.loadClass("legacy.lazystart.TrueExceptionLegacy2").newInstance()).testIt(); //$NON-NLS-1$
		expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, legacyB);
		actualEvents = simpleResults.getResults(1);
		compareResults(expectedEvents, actualEvents);

		((ITestRunner) legacy.loadClass("legacy.lazystart.FalseExceptionLegacy1").newInstance()).testIt(); //$NON-NLS-1$
		assertTrue("exceptions no event", simpleResults.getResults(0).length == 0); //$NON-NLS-1$
		((ITestRunner) legacy.loadClass("legacy.lazystart.FalseExceptionLegacy2").newInstance()).testIt(); //$NON-NLS-1$
		expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, legacyC);
		actualEvents = simpleResults.getResults(1);
		compareResults(expectedEvents, actualEvents);
	}

	public void testLegacyLoadActivation() throws Exception {
		// test that calling loadClass from a non-lazy start bundle does not activate the bundle
		Bundle test = installer.installBundle("test"); //$NON-NLS-1$
		try {
			test.loadClass("does.not.exist.Test"); //$NON-NLS-1$
		} catch (ClassNotFoundException e) {
			// expected
		}
		Object[] expectedEvents = new Object[0];
		Object[] actualEvents = simpleResults.getResults(0);
		compareResults(expectedEvents, actualEvents);

		// test that calling loadClass from a lazy start bundle activates a bundle
		Bundle legacyA = installer.installBundle("legacy.lazystart.a"); //$NON-NLS-1$
		try {
			legacyA.loadClass("does.not.exist.Test"); //$NON-NLS-1$
		} catch (ClassNotFoundException e) {
			// expected
		}
		expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, legacyA);
		actualEvents = simpleResults.getResults(1);
		compareResults(expectedEvents, actualEvents);
	}

	public void testOSGiLazyStart() throws Exception {
		Bundle osgi = installer.installBundle("osgi.lazystart"); //$NON-NLS-1$
		Bundle osgiA = installer.installBundle("osgi.lazystart.a"); //$NON-NLS-1$
		Bundle osgiB = installer.installBundle("osgi.lazystart.b"); //$NON-NLS-1$
		Bundle osgiC = installer.installBundle("osgi.lazystart.c"); //$NON-NLS-1$
		assertTrue("osgi lazy start resolve", installer.resolveBundles(new Bundle[] {osgi, osgiA, osgiB, osgiC})); //$NON-NLS-1$

		((ITestRunner) osgi.loadClass("osgi.lazystart.LazySimple").newInstance()).testIt(); //$NON-NLS-1$
		Object[] expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, osgiA);
		Object[] actualEvents = simpleResults.getResults(1);
		compareResults(expectedEvents, actualEvents);

		((ITestRunner) osgi.loadClass("osgi.lazystart.LazyExclude1").newInstance()).testIt(); //$NON-NLS-1$
		assertTrue("exceptions no event", simpleResults.getResults(0).length == 0); //$NON-NLS-1$
		((ITestRunner) osgi.loadClass("osgi.lazystart.LazyExclude2").newInstance()).testIt(); //$NON-NLS-1$
		expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, osgiB);
		actualEvents = simpleResults.getResults(1);
		compareResults(expectedEvents, actualEvents);

		((ITestRunner) osgi.loadClass("osgi.lazystart.LazyInclude1").newInstance()).testIt(); //$NON-NLS-1$
		assertTrue("exceptions no event", simpleResults.getResults(0).length == 0); //$NON-NLS-1$
		((ITestRunner) osgi.loadClass("osgi.lazystart.LazyInclude2").newInstance()).testIt(); //$NON-NLS-1$
		expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, osgiC);
		actualEvents = simpleResults.getResults(1);
		compareResults(expectedEvents, actualEvents);
	}

	public void testStartTransientByLoadClass() throws Exception {
		// install a bundle and set its start-level high, then crank up the framework start-level.  This should result in no events
		Bundle osgiA = installer.installBundle("osgi.lazystart.a"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] {osgiA});
		StartLevel startLevel = installer.getStartLevel();
		startLevel.setBundleStartLevel(osgiA, startLevel.getStartLevel() + 10);

		// test transient start by loadClass
		startLevel.setStartLevel(startLevel.getStartLevel() + 15);
		Object[] expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, OSGiTestsActivator.getContext().getBundle(0), null);
		Object[] actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);

		startLevel.setStartLevel(startLevel.getStartLevel() - 15);
		expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, OSGiTestsActivator.getContext().getBundle(0), null);
		actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);

		Object[] expectedEvents = new Object[0];
		Object[] actualEvents = simpleResults.getResults(0);
		compareResults(expectedEvents, actualEvents);

		// now load a class from it before the start-level is met.  This should result in no events
		osgiA.loadClass("osgi.lazystart.a.ATest"); //$NON-NLS-1$
		expectedEvents = new Object[0];
		actualEvents = simpleResults.getResults(0);
		compareResults(expectedEvents, actualEvents);

		startLevel.setStartLevel(startLevel.getStartLevel() + 15);
		expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, OSGiTestsActivator.getContext().getBundle(0), null);
		actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);

		startLevel.setStartLevel(startLevel.getStartLevel() - 15);
		expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, OSGiTestsActivator.getContext().getBundle(0), null);
		actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);

		expectedEvents = new Object[0];
		actualEvents = simpleResults.getResults(0);
		compareResults(expectedEvents, actualEvents);

		// now load a class while start-level is met.
		startLevel.setStartLevel(startLevel.getStartLevel() + 15);
		expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, OSGiTestsActivator.getContext().getBundle(0), null);
		actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);

		osgiA.loadClass("osgi.lazystart.a.ATest"); //$NON-NLS-1$
		expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, osgiA);
		actualEvents = simpleResults.getResults(1);
		compareResults(expectedEvents, actualEvents);

		startLevel.setStartLevel(startLevel.getStartLevel() - 15);
		expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, OSGiTestsActivator.getContext().getBundle(0), null);
		actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);

		expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STOPPED, osgiA);
		actualEvents = simpleResults.getResults(1);
		compareResults(expectedEvents, actualEvents);
	}

	public void testStartTransient() throws Exception {
		// install a bundle and set its start-level high, then crank up the framework start-level.  This should result in no events
		Bundle osgiA = installer.installBundle("osgi.lazystart.a"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] {osgiA});
		StartLevel startLevel = installer.getStartLevel();
		startLevel.setBundleStartLevel(osgiA, startLevel.getStartLevel() + 10);

		// test transient start Bundle.start(START_TRANSIENT)
		startLevel.setStartLevel(startLevel.getStartLevel() + 15);
		Object[] expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, OSGiTestsActivator.getContext().getBundle(0), null);
		Object[] actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);

		startLevel.setStartLevel(startLevel.getStartLevel() - 15);
		expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, OSGiTestsActivator.getContext().getBundle(0), null);
		actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);

		Object[] expectedEvents = new Object[0];
		Object[] actualEvents = simpleResults.getResults(0);
		compareResults(expectedEvents, actualEvents);

		// now call start(START_TRANSIENT) before the start-level is met.  This should result in no events
		try {
			osgiA.start(Bundle.START_TRANSIENT);
			assertFalse("Bundle is started!!", osgiA.getState() == Bundle.ACTIVE); //$NON-NLS-1$
		} catch (BundleException e) {
			// expected
		}
		expectedEvents = new Object[0];
		actualEvents = simpleResults.getResults(0);
		compareResults(expectedEvents, actualEvents);

		startLevel.setStartLevel(startLevel.getStartLevel() + 15);
		expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, OSGiTestsActivator.getContext().getBundle(0), null);
		actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);

		startLevel.setStartLevel(startLevel.getStartLevel() - 15);
		expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, OSGiTestsActivator.getContext().getBundle(0), null);
		actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);

		expectedEvents = new Object[0];
		actualEvents = simpleResults.getResults(0);
		compareResults(expectedEvents, actualEvents);

		// now call start(START_TRANSIENT) while start-level is met.
		startLevel.setStartLevel(startLevel.getStartLevel() + 15);
		expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, OSGiTestsActivator.getContext().getBundle(0), null);
		actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);

		osgiA.start(Bundle.START_TRANSIENT);

		expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, osgiA);
		actualEvents = simpleResults.getResults(1);
		compareResults(expectedEvents, actualEvents);

		startLevel.setStartLevel(startLevel.getStartLevel() - 15);
		expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, OSGiTestsActivator.getContext().getBundle(0), null);
		actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);

		expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STOPPED, osgiA);
		actualEvents = simpleResults.getResults(1);
		compareResults(expectedEvents, actualEvents);
	}

	public void testStopTransient() throws Exception {
		Bundle osgiA = installer.installBundle("osgi.lazystart.a"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] {osgiA});
		StartLevel startLevel = installer.getStartLevel();
		startLevel.setBundleStartLevel(osgiA, startLevel.getStartLevel() + 10);
		// persistently start the bundle
		osgiA.start();

		// test that the bundle is started when start-level is met
		startLevel.setStartLevel(startLevel.getStartLevel() + 15);
		Object[] expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, OSGiTestsActivator.getContext().getBundle(0), null);
		Object[] actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);

		Object[] expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, osgiA);
		Object[] actualEvents = simpleResults.getResults(1);
		compareResults(expectedEvents, actualEvents);

		startLevel.setStartLevel(startLevel.getStartLevel() - 15);
		expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, OSGiTestsActivator.getContext().getBundle(0), null);
		actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);

		expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STOPPED, osgiA);
		actualEvents = simpleResults.getResults(1);
		compareResults(expectedEvents, actualEvents);

		// now call stop(STOP_TRANSIENT) while the start-level is met.
		startLevel.setStartLevel(startLevel.getStartLevel() + 15);
		expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, OSGiTestsActivator.getContext().getBundle(0), null);
		actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);

		expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, osgiA);
		actualEvents = simpleResults.getResults(1);
		compareResults(expectedEvents, actualEvents);

		osgiA.stop(Bundle.STOP_TRANSIENT);

		expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STOPPED, osgiA);
		actualEvents = simpleResults.getResults(1);
		compareResults(expectedEvents, actualEvents);

		startLevel.setStartLevel(startLevel.getStartLevel() - 15);
		expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, OSGiTestsActivator.getContext().getBundle(0), null);
		actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);

		// now make sure the bundle still restarts when start-level is met
		startLevel.setStartLevel(startLevel.getStartLevel() + 15);
		expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, OSGiTestsActivator.getContext().getBundle(0), null);
		actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);

		expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, osgiA);
		actualEvents = simpleResults.getResults(1);
		compareResults(expectedEvents, actualEvents);

		startLevel.setStartLevel(startLevel.getStartLevel() - 15);
		expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, OSGiTestsActivator.getContext().getBundle(0), null);
		actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);

		expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STOPPED, osgiA);
		actualEvents = simpleResults.getResults(1);
		compareResults(expectedEvents, actualEvents);
	}

	public void testBug213791() throws Exception {
		// install a bundle and call start(START_ACTIVATION_POLICY) twice
		Bundle osgiA = installer.installBundle("osgi.lazystart.a"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] {osgiA});
		if (osgiA.getState() == Bundle.STARTING)
			osgiA.stop();
		osgiA.start(Bundle.START_ACTIVATION_POLICY);
		Object[] expectedEvents = new Object[0];
		Object[] actualEvents = simpleResults.getResults(0);
		compareResults(expectedEvents, actualEvents);

		osgiA.start(Bundle.START_ACTIVATION_POLICY);
		expectedEvents = new Object[0];
		actualEvents = simpleResults.getResults(0);
		compareResults(expectedEvents, actualEvents);

		osgiA.loadClass("osgi.lazystart.a.ATest"); //$NON-NLS-1$
		expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, osgiA);
		actualEvents = simpleResults.getResults(1);
		compareResults(expectedEvents, actualEvents);

	}

	public void testThreadLock() throws Exception {
		Bundle threadLockTest = installer.installBundle("thread.locktest"); //$NON-NLS-1$
		threadLockTest.loadClass("thread.locktest.ATest"); //$NON-NLS-1$

		Object[] expectedEvents = new Object[2];
		expectedEvents[0] = new Long(5000);
		expectedEvents[1] = new BundleEvent(BundleEvent.STARTED, threadLockTest);
		Object[] actualEvents = simpleResults.getResults(2);
		compareResults(expectedEvents, actualEvents);

	}

	public void testURLsBug164077() throws Exception {
		Bundle test = installer.installBundle("test"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] {test});
		URL[] urls = new URL[2];
		urls[0] = test.getResource("a/b/c/d"); //$NON-NLS-1$
		urls[1] = test.getEntry("a/b/c/d"); //$NON-NLS-1$
		assertNotNull("resource", urls[0]); //$NON-NLS-1$
		assertNotNull("entry", urls[1]); //$NON-NLS-1$
		for (int i = 0; i < urls.length; i++) {
			URL testURL = new URL(urls[i], "g"); //$NON-NLS-1$
			assertEquals("g", "/a/b/c/g", testURL.getPath()); //$NON-NLS-1$ //$NON-NLS-2$
			testURL = new URL(urls[i], "./g"); //$NON-NLS-1$
			assertEquals("./g", "/a/b/c/g", testURL.getPath()); //$NON-NLS-1$ //$NON-NLS-2$
			testURL = new URL(urls[i], "g/"); //$NON-NLS-1$
			assertEquals("g/", "/a/b/c/g/", testURL.getPath()); //$NON-NLS-1$ //$NON-NLS-2$
			testURL = new URL(urls[i], "/g"); //$NON-NLS-1$
			assertEquals("/g", "/g", testURL.getPath()); //$NON-NLS-1$ //$NON-NLS-2$
			testURL = new URL(urls[i], "?y"); //$NON-NLS-1$
			assertEquals("?y", "/a/b/c/?y", testURL.getPath()); //$NON-NLS-1$ //$NON-NLS-2$
			testURL = new URL(urls[i], "g?y"); //$NON-NLS-1$
			assertEquals("g?y", "/a/b/c/g?y", testURL.getPath()); //$NON-NLS-1$ //$NON-NLS-2$
			testURL = new URL(urls[i], "g#s"); //$NON-NLS-1$
			assertEquals("g#s", "/a/b/c/g#s", testURL.getPath() + "#s"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			testURL = new URL(urls[i], "g?y#s"); //$NON-NLS-1$
			assertEquals("g?y#s", "/a/b/c/g?y#s", testURL.getPath() + "#s"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			testURL = new URL(urls[i], ";x"); //$NON-NLS-1$
			assertEquals(";x", "/a/b/c/;x", testURL.getPath()); //$NON-NLS-1$ //$NON-NLS-2$
			testURL = new URL(urls[i], "g;x"); //$NON-NLS-1$
			assertEquals("g;x", "/a/b/c/g;x", testURL.getPath()); //$NON-NLS-1$ //$NON-NLS-2$
			testURL = new URL(urls[i], "g;x?y#s"); //$NON-NLS-1$
			assertEquals("g;x?y#s", "/a/b/c/g;x?y#s", testURL.getPath() + "#s"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			testURL = new URL(urls[i], "."); //$NON-NLS-1$
			assertEquals(".", "/a/b/c/", testURL.getPath()); //$NON-NLS-1$ //$NON-NLS-2$
			testURL = new URL(urls[i], "./"); //$NON-NLS-1$
			assertEquals("./", "/a/b/c/", testURL.getPath()); //$NON-NLS-1$ //$NON-NLS-2$
			testURL = new URL(urls[i], ".."); //$NON-NLS-1$
			assertEquals("..", "/a/b/", testURL.getPath()); //$NON-NLS-1$ //$NON-NLS-2$
			testURL = new URL(urls[i], "../"); //$NON-NLS-1$
			assertEquals("../", "/a/b/", testURL.getPath()); //$NON-NLS-1$ //$NON-NLS-2$
			testURL = new URL(urls[i], "../g"); //$NON-NLS-1$
			assertEquals("../g", "/a/b/g", testURL.getPath()); //$NON-NLS-1$ //$NON-NLS-2$
			testURL = new URL(urls[i], "../.."); //$NON-NLS-1$
			assertEquals("../..", "/a/", testURL.getPath()); //$NON-NLS-1$ //$NON-NLS-2$
			testURL = new URL(urls[i], "../../"); //$NON-NLS-1$
			assertEquals("../../", "/a/", testURL.getPath()); //$NON-NLS-1$ //$NON-NLS-2$
			testURL = new URL(urls[i], "../../g"); //$NON-NLS-1$
			assertEquals("../../g", "/a/g", testURL.getPath()); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	public void testEntryURLEqualsHashCode() throws Exception {
		Bundle test = installer.installBundle("test"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] {test});
		URL testEntry1 = test.getEntry("a/b/c/d"); //$NON-NLS-1$
		URL testEntry2 = test.getEntry("a/b/c/d"); //$NON-NLS-1$
		assertEquals("url equals 1.0", testEntry1, testEntry2); //$NON-NLS-1$
		assertEquals("hashcode equals 1.1", testEntry1.hashCode(), testEntry2.hashCode()); //$NON-NLS-1$

		URL testEntry3 = new URL(testEntry1, "./d"); //$NON-NLS-1$
		assertEquals("url equals 2.0", testEntry1, testEntry3); //$NON-NLS-1$
		assertEquals("hashcode equals 2.1", testEntry1.hashCode(), testEntry3.hashCode()); //$NON-NLS-1$

		URL testEntry4 = new URL(testEntry3.toString());
		assertEquals("url equals 3.0", testEntry4, testEntry3); //$NON-NLS-1$
		assertEquals("hashcode equals 3.1", testEntry4.hashCode(), testEntry3.hashCode()); //$NON-NLS-1$
	}

	public void testResourceURLEqualsHashCode() throws Exception {
		Bundle test = installer.installBundle("test"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] {test});
		URL testResource1 = test.getResource("a/b/c/d"); //$NON-NLS-1$
		URL testResource2 = test.getResource("a/b/c/d"); //$NON-NLS-1$
		assertEquals("url equals 1.0", testResource1, testResource2); //$NON-NLS-1$
		assertEquals("hashcode equals 1.1", testResource1.hashCode(), testResource2.hashCode()); //$NON-NLS-1$

		URL testResource3 = new URL(testResource1, "./d"); //$NON-NLS-1$
		assertEquals("url equals 2.0", testResource1, testResource3); //$NON-NLS-1$
		assertEquals("hashcode equals 2.1", testResource1.hashCode(), testResource3.hashCode()); //$NON-NLS-1$

		URL testResource4 = new URL(testResource3.toString());
		assertEquals("url equals 3.0", testResource4, testResource3); //$NON-NLS-1$
		assertEquals("hashcode equals 3.1", testResource4.hashCode(), testResource3.hashCode()); //$NON-NLS-1$
	}

	public void testBootGetResources() throws Exception {
		if (System.getProperty(Constants.FRAMEWORK_BOOTDELEGATION) != null)
			return; // cannot really test this if this property is set
		// make sure there is only one manifest found
		Bundle test = installer.installBundle("test"); //$NON-NLS-1$
		Enumeration manifests = test.getResources("META-INF/MANIFEST.MF"); //$NON-NLS-1$
		assertNotNull("manifests", manifests); //$NON-NLS-1$
		ArrayList manifestURLs = new ArrayList();
		while (manifests.hasMoreElements())
			manifestURLs.add(manifests.nextElement());
		assertEquals("manifest number", 1, manifestURLs.size()); //$NON-NLS-1$
		URL manifest = (URL) manifestURLs.get(0);
		assertEquals("host id", test.getBundleId(), Long.parseLong(manifest.getHost())); //$NON-NLS-1$
	}

	public void testMultipleGetResources01() throws Exception {
		Bundle test = installer.installBundle("test"); //$NON-NLS-1$
		// test that we can get multiple resources from a bundle
		Enumeration resources = test.getResources("data/resource1"); //$NON-NLS-1$
		assertNotNull("resources", resources); //$NON-NLS-1$
		ArrayList resourceURLs = new ArrayList();
		while (resources.hasMoreElements())
			resourceURLs.add(resources.nextElement());
		assertEquals("resource number", 2, resourceURLs.size()); //$NON-NLS-1$
		assertEquals("root resource", "root classpath", readURL((URL) resourceURLs.get(0))); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("stuff resource", "stuff classpath", readURL((URL) resourceURLs.get(1))); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testMultipleGetResources02() throws Exception {
		installer.installBundle("test"); //$NON-NLS-1$
		Bundle test2 = installer.installBundle("test2"); //$NON-NLS-1$
		// test that we can get multiple resources from a bundle
		Enumeration resources = test2.getResources("data/resource1"); //$NON-NLS-1$
		assertNotNull("resources", resources); //$NON-NLS-1$
		ArrayList resourceURLs = new ArrayList();
		while (resources.hasMoreElements())
			resourceURLs.add(resources.nextElement());
		assertEquals("resource number", 4, resourceURLs.size()); //$NON-NLS-1$
		assertEquals("root resource", "root classpath", readURL((URL) resourceURLs.get(0))); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("stuff resource", "stuff classpath", readURL((URL) resourceURLs.get(1))); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("root resource", "root classpath test2", readURL((URL) resourceURLs.get(2))); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("stuff resource", "stuff classpath test2", readURL((URL) resourceURLs.get(3))); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testMultipleExportFragments01() throws Exception {
		Bundle host = installer.installBundle("host.multiple.exports"); //$NON-NLS-1$
		Bundle frag = installer.installBundle("frag.multiple.exports"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] {host, frag});
		PackageAdmin packageAdmin = installer.getPackageAdmin();
		ExportedPackage[] hostExports = packageAdmin.getExportedPackages(host);
		assertEquals("Number host exports", 3, hostExports == null ? 0 : hostExports.length); //$NON-NLS-1$

		PlatformAdmin platformAdmin = installer.getPlatformAdmin();
		State systemState = platformAdmin.getState(false);
		BundleDescription hostDesc = systemState.getBundle(host.getBundleId());
		ExportPackageDescription[] hostExportDescs = hostDesc.getSelectedExports();
		assertEquals("Number host export descriptions", 3, hostExportDescs.length); //$NON-NLS-1$

		assertEquals("Check export name", "host.multiple.exports", hostExportDescs[0].getName()); //$NON-NLS-1$//$NON-NLS-2$
		assertEquals("Check include directive", "Public*", (String) hostExportDescs[0].getDirective("include")); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$

		assertEquals("Check export name", "host.multiple.exports.onlyone", hostExportDescs[1].getName()); //$NON-NLS-1$ //$NON-NLS-2$

		assertEquals("Check export name", "host.multiple.exports", hostExportDescs[2].getName()); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("Check include directive", "private", (String) hostExportDescs[2].getAttributes().get("scope")); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
	}

	public void testMultipleExportFragments02() throws Exception {
		Bundle host = installer.installBundle("host.multiple.exports"); //$NON-NLS-1$
		Bundle frag = installer.installBundle("frag.multiple.exports"); //$NON-NLS-1$
		Bundle client1 = installer.installBundle("client1.multiple.exports"); //$NON-NLS-1$

		installer.resolveBundles(new Bundle[] {host, frag, client1});
		client1.start();
		client1.stop();
		Object[] expectedEvents = new Object[4];
		expectedEvents[0] = "host.multiple.exports.PublicClass1"; //$NON-NLS-1$
		expectedEvents[1] = "host.multiple.exports.PublicClass2"; //$NON-NLS-1$
		expectedEvents[2] = "success"; //$NON-NLS-1$
		expectedEvents[3] = "success"; //$NON-NLS-1$
		Object[] actualEvents = simpleResults.getResults(4);
		compareResults(expectedEvents, actualEvents);
	}

	public void testMultipleExportFragments03() throws Exception {
		Bundle host = installer.installBundle("host.multiple.exports"); //$NON-NLS-1$
		Bundle frag = installer.installBundle("frag.multiple.exports"); //$NON-NLS-1$
		Bundle client2 = installer.installBundle("client2.multiple.exports"); //$NON-NLS-1$

		installer.resolveBundles(new Bundle[] {host, frag, client2});
		client2.start();
		client2.stop();
		Object[] expectedEvents = new Object[4];
		expectedEvents[0] = "host.multiple.exports.PublicClass1"; //$NON-NLS-1$
		expectedEvents[1] = "host.multiple.exports.PublicClass2"; //$NON-NLS-1$
		expectedEvents[2] = "host.multiple.exports.PrivateClass1"; //$NON-NLS-1$
		expectedEvents[3] = "host.multiple.exports.PrivateClass2"; //$NON-NLS-1$
		Object[] actualEvents = simpleResults.getResults(4);
		compareResults(expectedEvents, actualEvents);
	}

	public void testXFriends() throws Exception {
		System.setProperty("osgi.resolverMode", "strict"); //$NON-NLS-1$ //$NON-NLS-2$
		setPlatformProperties();
		try {
			Bundle test1 = installer.installBundle("xfriends.test1"); //$NON-NLS-1$
			Bundle test2 = installer.installBundle("xfriends.test2"); //$NON-NLS-1$
			Bundle test3 = installer.installBundle("xfriends.test3"); //$NON-NLS-1$
			installer.resolveBundles(new Bundle[] {test1, test2, test3});
			test2.start();
			test2.stop();
			test3.start();
			test3.stop();
			Object[] expectedEvents = new Object[4];
			expectedEvents[0] = "xfriends.test1.onlyforfriends.TestFriends"; //$NON-NLS-1$
			expectedEvents[1] = "xfriends.test1.external.TestFriends"; //$NON-NLS-1$
			expectedEvents[2] = "success"; //$NON-NLS-1$
			expectedEvents[3] = "xfriends.test1.external.TestFriends"; //$NON-NLS-1$
			Object[] actualEvents = simpleResults.getResults(4);
			compareResults(expectedEvents, actualEvents);
		} finally {
			System.getProperties().remove("osgi.resolverMode"); //$NON-NLS-1$
			setPlatformProperties();
		}
	}

	public void testImporterExporter01() throws BundleException {
		Bundle importerExporter1 = installer.installBundle("exporter.importer1"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] {importerExporter1});
		PackageAdmin pa = installer.getPackageAdmin();
		ExportedPackage[] origExportedPackages = pa.getExportedPackages("exporter.importer.test"); //$NON-NLS-1$
		assertNotNull("No exporter.importer.test found", origExportedPackages); //$NON-NLS-1$
		assertEquals("Wrong number of exports", 1, origExportedPackages.length); //$NON-NLS-1$
		Bundle exporter = origExportedPackages[0].getExportingBundle();
		assertEquals("Wrong exporter", importerExporter1, exporter); //$NON-NLS-1$
		// TODO need to get clarification from OSGi on what is returned by getImportingBundles when there is no importers
		Bundle[] origImporters = origExportedPackages[0].getImportingBundles();
		assertTrue("Should have no importers", origImporters == null || origImporters.length == 0); //$NON-NLS-1$

		// install another importer/exporter.  This bundle should wire to the original exporter
		Bundle importerExporter2 = installer.installBundle("exporter.importer2"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] {importerExporter2});

		origImporters = origExportedPackages[0].getImportingBundles();
		assertNotNull("No importers found", origImporters); //$NON-NLS-1$
		assertEquals("Wrong number of importers", 1, origImporters.length); //$NON-NLS-1$
		assertEquals("Wrong importer", importerExporter2, origImporters[0]); //$NON-NLS-1$

		ExportedPackage[] newExportedPackages = pa.getExportedPackages("exporter.importer.test"); //$NON-NLS-1$
		assertNotNull("No exporter.importer.test found", newExportedPackages); //$NON-NLS-1$
		assertEquals("Wrong number of exports", 1, newExportedPackages.length); //$NON-NLS-1$
		exporter = newExportedPackages[0].getExportingBundle();
		assertEquals("Wrong exporter", importerExporter1, exporter); //$NON-NLS-1$
		Bundle[] newImporters = newExportedPackages[0].getImportingBundles();
		assertNotNull("No importers found", newImporters); //$NON-NLS-1$
		assertEquals("Wrong number of importers", 1, newImporters.length); //$NON-NLS-1$
		assertEquals("Wrong importer", importerExporter2, newImporters[0]); //$NON-NLS-1$
	}

	public void testImporterExporter02() throws BundleException {
		Bundle importerExporter3 = installer.installBundle("exporter.importer3"); //$NON-NLS-1$
		Bundle importerExporter4 = installer.installBundle("exporter.importer4"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] {importerExporter3, importerExporter4});

		importerExporter3.start();
		importerExporter3.stop();
		importerExporter3.update();
		try {
			importerExporter3.start();
		} catch (Throwable t) {
			fail("Unexpected exception", t); //$NON-NLS-1$
		}
	}

	public void testBug207847() throws BundleException {
		Bundle test = installer.installBundle("test"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] {test});
		test.start();

		Bundle frag1 = installer.installBundle("test.fragment1"); //$NON-NLS-1$
		Bundle frag2 = installer.installBundle("test.fragment2"); //$NON-NLS-1$
		Bundle frag3 = installer.installBundle("test.fragment3"); //$NON-NLS-1$
		Bundle frag4 = installer.installBundle("test.fragment4"); //$NON-NLS-1$
		Bundle frag5 = installer.installBundle("test.fragment5"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] {frag1, frag2, frag3, frag4, frag5});

		assertTrue("host is not resolved", (test.getState() & Bundle.ACTIVE) != 0); //$NON-NLS-1$
		assertTrue("frag1 is not resolved", (frag1.getState() & Bundle.RESOLVED) != 0); //$NON-NLS-1$
		assertTrue("frag2 is not resolved", (frag2.getState() & Bundle.RESOLVED) != 0); //$NON-NLS-1$
		assertTrue("frag3 is not resolved", (frag3.getState() & Bundle.RESOLVED) != 0); //$NON-NLS-1$
		assertTrue("frag4 is not resolved", (frag4.getState() & Bundle.RESOLVED) != 0); //$NON-NLS-1$
		assertTrue("frag5 is not resolved", (frag5.getState() & Bundle.RESOLVED) != 0); //$NON-NLS-1$
	}

	public void testBug235958() throws BundleException {
		Bundle testX = installer.installBundle("test.bug235958.x"); //$NON-NLS-1$
		Bundle testY = installer.installBundle("test.bug235958.y"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] {testX, testY});
		try {
			testX.start();
		} catch (Exception e) {
			fail("Unexpected Exception", e); //$NON-NLS-1$
		}
	}

	// TODO temporarily disable til we can debug the build test machine on Win XP
	//	public void testBuddyClassLoadingRegistered1() throws Exception{
	//		Bundle registeredA = installer.installBundle("buddy.registered.a");
	//		installer.resolveBundles(new Bundle[] {registeredA});
	//		Enumeration testFiles = registeredA.getResources("resources/test.txt");
	//		assertNotNull("testFiles", testFiles);
	//		ArrayList testURLs = new ArrayList();
	//		while(testFiles.hasMoreElements())
	//			testURLs.add(testFiles.nextElement());
	//		assertEquals("test.txt number", 1, testURLs.size());
	//		assertEquals("buddy.registered.a", "buddy.registered.a", readURL((URL) testURLs.get(0)));
	//
	//		Bundle registeredATest1 = installer.installBundle("buddy.registered.a.test1");
	//		Bundle registeredATest2 = installer.installBundle("buddy.registered.a.test2");
	//		installer.resolveBundles(new Bundle[] {registeredATest1, registeredATest2});
	//		testFiles = registeredA.getResources("resources/test.txt");
	//		assertNotNull("testFiles", testFiles);
	//		testURLs = new ArrayList();
	//		while(testFiles.hasMoreElements())
	//			testURLs.add(testFiles.nextElement());
	//
	//		// TODO some debug code to figure out why this is failing on the test machine
	//		if (registeredATest1.getState() != Bundle.RESOLVED) {
	//			System.out.println("Bundle is not resolved!! " + registeredATest1.getSymbolicName());
	//			State state = Platform.getPlatformAdmin().getState(false);
	//			BundleDescription aDesc = state.getBundle(registeredATest1.getBundleId());
	//			ResolverError[] errors = state.getResolverErrors(aDesc);
	//			for (int i = 0; i < errors.length; i++)
	//				System.out.println(errors[i]);
	//		}
	//		if (registeredATest2.getState() != Bundle.RESOLVED) {
	//			System.out.println("Bundle is not resolved!! " + registeredATest2.getSymbolicName());
	//			State state = Platform.getPlatformAdmin().getState(false);
	//			BundleDescription bDesc = state.getBundle(registeredATest2.getBundleId());
	//			ResolverError[] errors = state.getResolverErrors(bDesc);
	//			for (int i = 0; i < errors.length; i++)
	//				System.out.println(errors[i]);
	//		}
	//
	//		// The real test
	//		assertEquals("test.txt number", 3, testURLs.size());
	//		assertEquals("buddy.registered.a", "buddy.registered.a", readURL((URL) testURLs.get(0)));
	//		assertEquals("buddy.registered.a.test1", "buddy.registered.a.test1", readURL((URL) testURLs.get(1)));
	//		assertEquals("buddy.registered.a.test2", "buddy.registered.a.test2", readURL((URL) testURLs.get(2)));
	//	}
	//
	//	public void testBuddyClassLoadingDependent1() throws Exception{
	//		Bundle dependentA = installer.installBundle("buddy.dependent.a");
	//		installer.resolveBundles(new Bundle[] {dependentA});
	//		Enumeration testFiles = dependentA.getResources("resources/test.txt");
	//		assertNotNull("testFiles", testFiles);
	//		ArrayList testURLs = new ArrayList();
	//		while(testFiles.hasMoreElements())
	//			testURLs.add(testFiles.nextElement());
	//		assertEquals("test.txt number", 1, testURLs.size());
	//		assertEquals("buddy.dependent.a", "buddy.dependent.a", readURL((URL) testURLs.get(0)));
	//
	//		Bundle dependentATest1 = installer.installBundle("buddy.dependent.a.test1");
	//		Bundle dependentATest2 = installer.installBundle("buddy.dependent.a.test2");
	//		installer.resolveBundles(new Bundle[] {dependentATest1, dependentATest2});
	//		testFiles = dependentA.getResources("resources/test.txt");
	//		assertNotNull("testFiles", testFiles);
	//		testURLs = new ArrayList();
	//		while(testFiles.hasMoreElements())
	//			testURLs.add(testFiles.nextElement());
	//		assertEquals("test.txt number", 3, testURLs.size());
	//		assertEquals("buddy.dependent.a", "buddy.dependent.a", readURL((URL) testURLs.get(0)));
	//		assertEquals("buddy.dependent.a.test1", "buddy.dependent.a.test1", readURL((URL) testURLs.get(1)));
	//		assertEquals("buddy.dependent.a.test2", "buddy.dependent.a.test2", readURL((URL) testURLs.get(2)));
	//	}

	private String readURL(URL url) throws IOException {
		StringBuffer sb = new StringBuffer();
		BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
		try {
			for (String line = reader.readLine(); line != null;) {
				sb.append(line);
				line = reader.readLine();
				if (line != null)
					sb.append('\n');
			}
		} finally {
			reader.close();
		}
		return sb.toString();
	}
}
