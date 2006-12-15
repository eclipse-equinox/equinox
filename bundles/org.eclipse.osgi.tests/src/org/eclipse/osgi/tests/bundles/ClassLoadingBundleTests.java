/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.bundles;

import java.net.URL;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.osgi.framework.*;
import org.osgi.service.startlevel.StartLevel;

public class ClassLoadingBundleTests extends AbstractBundleTests {
	public static Test suite() {
		return new TestSuite(ClassLoadingBundleTests.class);
	}

	public void testSimple() throws Exception {
		Bundle test = installer.installBundle("test");
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
		Bundle chainTest = installer.installBundle("chain.test");
		Bundle chainTestA = installer.installBundle("chain.test.a");
		Bundle chainTestB = installer.installBundle("chain.test.b");
		installer.installBundle("chain.test.c");
		installer.installBundle("chain.test.d");
		((ITestRunner) chainTest.loadClass("chain.test.TestSingleChain").newInstance()).testIt();


		Object[] expectedEvents = new Object[6];
		expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, chainTestB);
		expectedEvents[1] = new BundleEvent(BundleEvent.STARTED, chainTestA);
		expectedEvents[2] = new BundleEvent(BundleEvent.STOPPED, chainTestA);
		expectedEvents[3] = new BundleEvent(BundleEvent.STOPPED, chainTestB);
		expectedEvents[4] = new BundleEvent(BundleEvent.STARTED, chainTestB);
		expectedEvents[5] = new BundleEvent(BundleEvent.STARTED, chainTestA);

		installer.refreshPackages(new Bundle[] {chainTestB});

		((ITestRunner) chainTest.loadClass("chain.test.TestSingleChain").newInstance()).testIt();

		Object[] actualEvents = simpleResults.getResults(6);
		compareResults(expectedEvents, actualEvents);
	}

	public void testMultiChainDepedencies() throws Exception {
		Bundle chainTest = installer.installBundle("chain.test");
		Bundle chainTestA = installer.installBundle("chain.test.a");
		Bundle chainTestB = installer.installBundle("chain.test.b");
		Bundle chainTestC = installer.installBundle("chain.test.c");
		Bundle chainTestD = installer.installBundle("chain.test.d");
		chainTest.loadClass("chain.test.TestMultiChain").newInstance();


		Object[] expectedEvents = new Object[8];
		expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, chainTestD);
		expectedEvents[1] = new BundleEvent(BundleEvent.STARTED, chainTestB);
		expectedEvents[2] = new BundleEvent(BundleEvent.STARTED, chainTestC);
		expectedEvents[3] = new BundleEvent(BundleEvent.STARTED, chainTestA);
		expectedEvents[4] = new BundleEvent(BundleEvent.STOPPED, chainTestA);
		expectedEvents[5] = new BundleEvent(BundleEvent.STOPPED, chainTestB);
		expectedEvents[6] = new BundleEvent(BundleEvent.STOPPED, chainTestC);
		expectedEvents[7] = new BundleEvent(BundleEvent.STOPPED, chainTestD);

		installer.refreshPackages(new Bundle[] {chainTestC, chainTestD});

		Object[] actualEvents = simpleResults.getResults(8);
		compareResults(expectedEvents, actualEvents);
	}

	public void testClassCircularityError() throws Exception {
		Bundle circularityTest = installer.installBundle("circularity.test");
		Bundle circularityTestA = installer.installBundle("circularity.test.a");
		circularityTest.loadClass("circularity.test.TestCircularity");

		Object[] expectedEvents = new Object[2];
		expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, circularityTest);
		expectedEvents[1] = new BundleEvent(BundleEvent.STARTED, circularityTestA);
		Object[] actualEvents = simpleResults.getResults(2);
		compareResults(expectedEvents, actualEvents);
	}

	public void testFragmentPackageAccess() throws Exception {
		Bundle hostA = installer.installBundle("fragment.test.attach.host.a");
		Bundle fragA = installer.installBundle("fragment.test.attach.frag.a");
		assertTrue("Host/Frag resolve", installer.resolveBundles(new Bundle[] {hostA, fragA}));

		ITestRunner testRunner = (ITestRunner) hostA.loadClass("fragment.test.attach.host.a.internal.test.TestPackageAccess").newInstance();
		try {
			testRunner.testIt();
		} catch (Exception e) {
			fail("Failed package access test: " + e.getMessage());
		}
	}

	public void testLegacyLazyStart() throws Exception {
		Bundle legacy = installer.installBundle("legacy.lazystart");
		Bundle legacyA = installer.installBundle("legacy.lazystart.a");
		Bundle legacyB = installer.installBundle("legacy.lazystart.b");
		Bundle legacyC = installer.installBundle("legacy.lazystart.c");
		assertTrue("legacy lazy start resolve", installer.resolveBundles(new Bundle[] {legacy, legacyA, legacyB, legacyC}));

		((ITestRunner) legacy.loadClass("legacy.lazystart.SimpleLegacy").newInstance()).testIt();
		Object[] expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, legacyA);
		Object[] actualEvents = simpleResults.getResults(1);
		compareResults(expectedEvents, actualEvents);

		((ITestRunner) legacy.loadClass("legacy.lazystart.TrueExceptionLegacy1").newInstance()).testIt();
		assertTrue("exceptions no event", simpleResults.getResults(0).length == 0);
		((ITestRunner) legacy.loadClass("legacy.lazystart.TrueExceptionLegacy2").newInstance()).testIt();
		expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, legacyB);
		actualEvents = simpleResults.getResults(1);
		compareResults(expectedEvents, actualEvents);

		((ITestRunner) legacy.loadClass("legacy.lazystart.FalseExceptionLegacy1").newInstance()).testIt();
		assertTrue("exceptions no event", simpleResults.getResults(0).length == 0);
		((ITestRunner) legacy.loadClass("legacy.lazystart.FalseExceptionLegacy2").newInstance()).testIt();
		expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, legacyC);
		actualEvents = simpleResults.getResults(1);
		compareResults(expectedEvents, actualEvents);
	}

	public void testLegacyLoadActivation() throws Exception {
		// test that calling loadClass from a non-lazy start bundle does not activate the bundle
		Bundle test = installer.installBundle("test");
		try {
			test.loadClass("does.not.exist.Test");
		} catch (ClassNotFoundException e) {
			// expected
		}
		Object[] expectedEvents = new Object[0];
		Object[] actualEvents = simpleResults.getResults(0);
		compareResults(expectedEvents, actualEvents);

		// test that calling loadClass from a lazy start bundle activates a bundle
		Bundle legacyA = installer.installBundle("legacy.lazystart.a");
		try {
			legacyA.loadClass("does.not.exist.Test");
		} catch (ClassNotFoundException e) {
			// expected
		}
		expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, legacyA);
		actualEvents = simpleResults.getResults(1);
		compareResults(expectedEvents, actualEvents);
	}

	public void testOSGiLazyStart() throws Exception {
		Bundle osgi = installer.installBundle("osgi.lazystart");
		Bundle osgiA = installer.installBundle("osgi.lazystart.a");
		Bundle osgiB = installer.installBundle("osgi.lazystart.b");
		Bundle osgiC = installer.installBundle("osgi.lazystart.c");
		assertTrue("osgi lazy start resolve", installer.resolveBundles(new Bundle[] {osgi, osgiA, osgiB, osgiC}));

		((ITestRunner) osgi.loadClass("osgi.lazystart.LazySimple").newInstance()).testIt();
		Object[] expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, osgiA);
		Object[] actualEvents = simpleResults.getResults(1);
		compareResults(expectedEvents, actualEvents);

		((ITestRunner) osgi.loadClass("osgi.lazystart.LazyExclude1").newInstance()).testIt();
		assertTrue("exceptions no event", simpleResults.getResults(0).length == 0);
		((ITestRunner) osgi.loadClass("osgi.lazystart.LazyExclude2").newInstance()).testIt();
		expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, osgiB);
		actualEvents = simpleResults.getResults(1);
		compareResults(expectedEvents, actualEvents);

		((ITestRunner) osgi.loadClass("osgi.lazystart.LazyInclude1").newInstance()).testIt();
		assertTrue("exceptions no event", simpleResults.getResults(0).length == 0);
		((ITestRunner) osgi.loadClass("osgi.lazystart.LazyInclude2").newInstance()).testIt();
		expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, osgiC);
		actualEvents = simpleResults.getResults(1);
		compareResults(expectedEvents, actualEvents);
	}

	public void testStartTransientByLoadClass() throws Exception {
		// install a bundle and set its start-level high, then crank up the framework start-level.  This should result in no events
		Bundle osgiA = installer.installBundle("osgi.lazystart.a");
		installer.resolveBundles(new Bundle[] {osgiA});
		StartLevel startLevel = installer.getStartLevel();
		startLevel.setBundleStartLevel(osgiA, startLevel.getStartLevel() + 10);

		// test transient start by loadClass
		startLevel.setStartLevel(startLevel.getStartLevel() + 15);
		Object[] expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, OSGiTestsActivator.getContext().getBundle(), null);
		Object[] actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);
	
		startLevel.setStartLevel(startLevel.getStartLevel() - 15);
		expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, OSGiTestsActivator.getContext().getBundle(), null);
		actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);

		Object[] expectedEvents = new Object[0];
		Object[] actualEvents = simpleResults.getResults(0);
		compareResults(expectedEvents, actualEvents);

		// now load a class from it before the start-level is met.  This should result in no events
		osgiA.loadClass("osgi.lazystart.a.ATest");
		expectedEvents = new Object[0];
		actualEvents = simpleResults.getResults(0);
		compareResults(expectedEvents, actualEvents);

		startLevel.setStartLevel(startLevel.getStartLevel() + 15);
		expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, OSGiTestsActivator.getContext().getBundle(), null);
		actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);

		startLevel.setStartLevel(startLevel.getStartLevel() - 15);
		expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, OSGiTestsActivator.getContext().getBundle(), null);
		actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);

		expectedEvents = new Object[0];
		actualEvents = simpleResults.getResults(0);
		compareResults(expectedEvents, actualEvents);

		// now load a class while start-level is met.
		startLevel.setStartLevel(startLevel.getStartLevel() + 15);
		expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, OSGiTestsActivator.getContext().getBundle(), null);
		actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);

		osgiA.loadClass("osgi.lazystart.a.ATest");
		expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, osgiA);
		actualEvents = simpleResults.getResults(1);
		compareResults(expectedEvents, actualEvents);

		startLevel.setStartLevel(startLevel.getStartLevel() - 15);
		expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, OSGiTestsActivator.getContext().getBundle(), null);
		actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);

		expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STOPPED, osgiA);
		actualEvents = simpleResults.getResults(1);
		compareResults(expectedEvents, actualEvents);
	}

	public void testStartTransient() throws Exception {
		// install a bundle and set its start-level high, then crank up the framework start-level.  This should result in no events
		Bundle osgiA = installer.installBundle("osgi.lazystart.a");
		installer.resolveBundles(new Bundle[] {osgiA});
		StartLevel startLevel = installer.getStartLevel();
		startLevel.setBundleStartLevel(osgiA, startLevel.getStartLevel() + 10);

		// test transient start Bundle.start(START_TRANSIENT)
		startLevel.setStartLevel(startLevel.getStartLevel() + 15);
		Object[] expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, OSGiTestsActivator.getContext().getBundle(), null);
		Object[] actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);
	
		startLevel.setStartLevel(startLevel.getStartLevel() - 15);
		expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, OSGiTestsActivator.getContext().getBundle(), null);
		actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);

		Object[] expectedEvents = new Object[0];
		Object[] actualEvents = simpleResults.getResults(0);
		compareResults(expectedEvents, actualEvents);

		// now call start(START_TRANSIENT) before the start-level is met.  This should result in no events
		try {
			osgiA.start(Bundle.START_TRANSIENT);
			assertFalse("Bundle is started!!", osgiA.getState() == Bundle.ACTIVE);
		} catch (BundleException e) {
			// expected
		}
		expectedEvents = new Object[0];
		actualEvents = simpleResults.getResults(0);
		compareResults(expectedEvents, actualEvents);

		startLevel.setStartLevel(startLevel.getStartLevel() + 15);
		expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, OSGiTestsActivator.getContext().getBundle(), null);
		actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);

		startLevel.setStartLevel(startLevel.getStartLevel() - 15);
		expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, OSGiTestsActivator.getContext().getBundle(), null);
		actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);

		expectedEvents = new Object[0];
		actualEvents = simpleResults.getResults(0);
		compareResults(expectedEvents, actualEvents);

		// now call start(START_TRANSIENT) while start-level is met.
		startLevel.setStartLevel(startLevel.getStartLevel() + 15);
		expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, OSGiTestsActivator.getContext().getBundle(), null);
		actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);

		osgiA.start(Bundle.START_TRANSIENT);

		expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, osgiA);
		actualEvents = simpleResults.getResults(1);
		compareResults(expectedEvents, actualEvents);

		startLevel.setStartLevel(startLevel.getStartLevel() - 15);
		expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, OSGiTestsActivator.getContext().getBundle(), null);
		actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);

		expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STOPPED, osgiA);
		actualEvents = simpleResults.getResults(1);
		compareResults(expectedEvents, actualEvents);
	}

	public void testStopTransient() throws Exception {
		Bundle osgiA = installer.installBundle("osgi.lazystart.a");
		installer.resolveBundles(new Bundle[] {osgiA});
		StartLevel startLevel = installer.getStartLevel();
		startLevel.setBundleStartLevel(osgiA, startLevel.getStartLevel() + 10);
		// persistently start the bundle
		osgiA.start();

		// test that the bundle is started when start-level is met
		startLevel.setStartLevel(startLevel.getStartLevel() + 15);
		Object[] expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, OSGiTestsActivator.getContext().getBundle(), null);
		Object[] actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);

		Object[] expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, osgiA);
		Object[] actualEvents = simpleResults.getResults(1);
		compareResults(expectedEvents, actualEvents);

		startLevel.setStartLevel(startLevel.getStartLevel() - 15);
		expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, OSGiTestsActivator.getContext().getBundle(), null);
		actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);

		expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STOPPED, osgiA);
		actualEvents = simpleResults.getResults(1);
		compareResults(expectedEvents, actualEvents);

		// now call stop(STOP_TRANSIENT) while the start-level is met.
		startLevel.setStartLevel(startLevel.getStartLevel() + 15);
		expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, OSGiTestsActivator.getContext().getBundle(), null);
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
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, OSGiTestsActivator.getContext().getBundle(), null);
		actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);

		// now make sure the bundle still restarts when start-level is met
		startLevel.setStartLevel(startLevel.getStartLevel() + 15);
		expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, OSGiTestsActivator.getContext().getBundle(), null);
		actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);

		expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, osgiA);
		actualEvents = simpleResults.getResults(1);
		compareResults(expectedEvents, actualEvents);

		startLevel.setStartLevel(startLevel.getStartLevel() - 15);
		expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, OSGiTestsActivator.getContext().getBundle(), null);
		actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);

		expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STOPPED, osgiA);
		actualEvents = simpleResults.getResults(1);
		compareResults(expectedEvents, actualEvents);
	}

	public void testThreadLock() throws Exception {
		Bundle threadLockTest = installer.installBundle("thread.locktest");
		threadLockTest.loadClass("thread.locktest.ATest");

		Object[] expectedEvents = new Object[2];
		expectedEvents[0] = new Long(5000);
		expectedEvents[1] = new BundleEvent(BundleEvent.STARTED, threadLockTest);
		Object[] actualEvents = simpleResults.getResults(2);
		compareResults(expectedEvents, actualEvents);
		
	}

	public void testURLsBug164077() throws Exception {
		Bundle test = installer.installBundle("test");
		installer.resolveBundles(new Bundle[] {test});
		URL[] urls = new URL[2];
		urls[0] = test.getResource("a/b/c/d");
		urls[1] = test.getEntry("a/b/c/d");
		assertNotNull("resource", urls[0]);
		assertNotNull("entry", urls[1]);
		for (int i = 0; i < urls.length; i++) {
			URL testURL = new URL(urls[i], "g");
			assertEquals("g", "/a/b/c/g", testURL.getPath());
			testURL = new URL(urls[i], "./g");
			assertEquals("./g", "/a/b/c/g", testURL.getPath());
			testURL = new URL(urls[i], "g/");
			assertEquals("g/", "/a/b/c/g/", testURL.getPath());
			testURL = new URL(urls[i], "/g");
			assertEquals("/g", "/g", testURL.getPath());
			testURL = new URL(urls[i], "?y");
			assertEquals("?y", "/a/b/c/?y", testURL.getPath());
			testURL = new URL(urls[i], "g?y");
			assertEquals("g?y", "/a/b/c/g?y", testURL.getPath());
			testURL = new URL(urls[i], "g#s");
			assertEquals("g#s", "/a/b/c/g#s", testURL.getPath() + "#s");
			testURL = new URL(urls[i], "g?y#s");
			assertEquals("g?y#s", "/a/b/c/g?y#s", testURL.getPath() + "#s");
			testURL = new URL(urls[i], ";x");
			assertEquals(";x", "/a/b/c/;x", testURL.getPath());
			testURL = new URL(urls[i], "g;x");
			assertEquals("g;x", "/a/b/c/g;x", testURL.getPath());
			testURL = new URL(urls[i], "g;x?y#s");
			assertEquals("g;x?y#s", "/a/b/c/g;x?y#s", testURL.getPath() + "#s");
			testURL = new URL(urls[i], ".");
			assertEquals(".", "/a/b/c/", testURL.getPath());
			testURL = new URL(urls[i], "./");
			assertEquals("./", "/a/b/c/", testURL.getPath());
			testURL = new URL(urls[i], "..");
			assertEquals("..", "/a/b/", testURL.getPath());
			testURL = new URL(urls[i], "../");
			assertEquals("../", "/a/b/", testURL.getPath());
			testURL = new URL(urls[i], "../g");
			assertEquals("../g", "/a/b/g", testURL.getPath());
			testURL = new URL(urls[i], "../..");
			assertEquals("../..", "/a/", testURL.getPath());
			testURL = new URL(urls[i], "../../");
			assertEquals("../../", "/a/", testURL.getPath());
			testURL = new URL(urls[i], "../../g");
			assertEquals("../../g", "/a/g", testURL.getPath());
		}
	}
}
