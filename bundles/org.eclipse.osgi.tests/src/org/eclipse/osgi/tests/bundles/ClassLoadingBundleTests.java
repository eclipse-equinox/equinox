/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
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
import java.lang.reflect.Method;
import java.net.*;
import java.util.*;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.osgi.framework.*;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.*;
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

	public void testLoadTriggerClass() throws Exception {
		Bundle chainTest = installer.installBundle("chain.test"); //$NON-NLS-1$
		Bundle chainTestA = installer.installBundle("chain.test.a"); //$NON-NLS-1$
		installer.installBundle("chain.test.b"); //$NON-NLS-1$
		installer.installBundle("chain.test.c"); //$NON-NLS-1$
		installer.installBundle("chain.test.d"); //$NON-NLS-1$
		assertTrue("Did not resolve chainTest", installer.resolveBundles(new Bundle[] {chainTest})); //$NON-NLS-1$
		chainTest.start(Bundle.START_ACTIVATION_POLICY);
		chainTestA.start(Bundle.START_ACTIVATION_POLICY);
		assertEquals("Wrong state", Bundle.STARTING, chainTest.getState()); //$NON-NLS-1$
		chainTest.loadClass("org.osgi.framework.BundleActivator"); //$NON-NLS-1$
		assertEquals("Wrong state", Bundle.STARTING, chainTest.getState()); //$NON-NLS-1$
		assertEquals("Wrong state", Bundle.STARTING, chainTestA.getState()); //$NON-NLS-1$
		chainTest.loadClass("chain.test.a.AChain"); //$NON-NLS-1$
		assertEquals("Wrong state", Bundle.STARTING, chainTest.getState()); //$NON-NLS-1$
		assertEquals("Wrong state", Bundle.ACTIVE, chainTestA.getState()); //$NON-NLS-1$
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

		Object[] actualEvents = simpleResults.getResults(12);
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

	public void testBug300692_01() throws BundleException {
		Bundle chainTest = installer.installBundle("chain.test"); //$NON-NLS-1$
		Bundle chainTestA = installer.installBundle("chain.test.a"); //$NON-NLS-1$
		Bundle chainTestB = installer.installBundle("chain.test.b"); //$NON-NLS-1$
		Bundle chainTestC = installer.installBundle("chain.test.c"); //$NON-NLS-1$
		Bundle chainTestD = installer.installBundle("chain.test.d"); //$NON-NLS-1$
		syncListenerResults.getResults(0);

		StartLevel sl = installer.getStartLevel();
		int currentSL = sl.getStartLevel();
		int testSL = currentSL + 1;
		sl.setBundleStartLevel(chainTest, testSL);
		sl.setBundleStartLevel(chainTestA, testSL);
		sl.setBundleStartLevel(chainTestB, testSL);
		sl.setBundleStartLevel(chainTestC, testSL);
		sl.setBundleStartLevel(chainTestD, testSL);
		installer.resolveBundles(new Bundle[] {chainTest, chainTestA, chainTestB, chainTestC, chainTestD});

		Object[] expectedEvents = new Object[5];
		expectedEvents[0] = new BundleEvent(BundleEvent.RESOLVED, chainTestD);
		expectedEvents[1] = new BundleEvent(BundleEvent.RESOLVED, chainTestC);
		expectedEvents[2] = new BundleEvent(BundleEvent.RESOLVED, chainTestB);
		expectedEvents[3] = new BundleEvent(BundleEvent.RESOLVED, chainTestA);
		expectedEvents[4] = new BundleEvent(BundleEvent.RESOLVED, chainTest);

		Object[] actualEvents = syncListenerResults.getResults(5);
		compareResults(expectedEvents, actualEvents);
		try {
			System.setProperty("test.bug300692", "true");
			chainTest.start();
			sl.setStartLevel(testSL);
			Object[] expectedFrameworkEvents = new Object[1];
			expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, OSGiTestsActivator.getContext().getBundle(0), null);
			Object[] actualFrameworkEvents = frameworkListenerResults.getResults(1);
			compareResults(expectedFrameworkEvents, actualFrameworkEvents);

			expectedEvents = new Object[14];
			int i = 0;
			expectedEvents[i++] = new BundleEvent(BundleEvent.LAZY_ACTIVATION, chainTestA);
			expectedEvents[i++] = new BundleEvent(BundleEvent.LAZY_ACTIVATION, chainTestB);
			expectedEvents[i++] = new BundleEvent(BundleEvent.LAZY_ACTIVATION, chainTestC);
			expectedEvents[i++] = new BundleEvent(BundleEvent.LAZY_ACTIVATION, chainTestD);
			expectedEvents[i++] = new BundleEvent(BundleEvent.STARTING, chainTest);
			expectedEvents[i++] = new BundleEvent(BundleEvent.STARTING, chainTestB);
			expectedEvents[i++] = new BundleEvent(BundleEvent.STARTED, chainTestB);
			expectedEvents[i++] = new BundleEvent(BundleEvent.STARTING, chainTestC);
			expectedEvents[i++] = new BundleEvent(BundleEvent.STARTED, chainTestC);
			expectedEvents[i++] = new BundleEvent(BundleEvent.STARTING, chainTestA);
			expectedEvents[i++] = new BundleEvent(BundleEvent.STARTED, chainTestA);
			expectedEvents[i++] = new BundleEvent(BundleEvent.STARTING, chainTestD);
			expectedEvents[i++] = new BundleEvent(BundleEvent.STARTED, chainTestD);
			expectedEvents[i++] = new BundleEvent(BundleEvent.STARTED, chainTest);

			actualFrameworkEvents = syncListenerResults.getResults(14);
			compareResults(expectedEvents, actualFrameworkEvents);
		} finally {
			System.getProperties().remove("test.bug300692");
			sl.setStartLevel(currentSL);
			Object[] expectedFrameworkEvents = new Object[1];
			expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, OSGiTestsActivator.getContext().getBundle(0), null);
			Object[] actualFrameworkEvents = frameworkListenerResults.getResults(1);
			compareResults(expectedFrameworkEvents, actualFrameworkEvents);
		}
	}

	public void testBug300692_02() throws BundleException {
		Bundle chainTest = installer.installBundle("chain.test"); //$NON-NLS-1$
		Bundle chainTestA = installer.installBundle("chain.test.a"); //$NON-NLS-1$
		Bundle chainTestB = installer.installBundle("chain.test.b"); //$NON-NLS-1$
		Bundle chainTestC = installer.installBundle("chain.test.c"); //$NON-NLS-1$
		Bundle chainTestD = installer.installBundle("chain.test.d"); //$NON-NLS-1$
		syncListenerResults.getResults(0);

		StartLevel sl = installer.getStartLevel();
		int currentSL = sl.getStartLevel();
		int testSL = currentSL + 1;
		sl.setBundleStartLevel(chainTest, testSL);
		sl.setBundleStartLevel(chainTestA, testSL);
		sl.setBundleStartLevel(chainTestB, testSL);
		sl.setBundleStartLevel(chainTestC, testSL);
		sl.setBundleStartLevel(chainTestD, testSL);
		installer.resolveBundles(new Bundle[] {chainTest, chainTestA, chainTestB, chainTestC, chainTestD});

		Object[] expectedEvents = new Object[5];
		expectedEvents[0] = new BundleEvent(BundleEvent.RESOLVED, chainTestD);
		expectedEvents[1] = new BundleEvent(BundleEvent.RESOLVED, chainTestC);
		expectedEvents[2] = new BundleEvent(BundleEvent.RESOLVED, chainTestB);
		expectedEvents[3] = new BundleEvent(BundleEvent.RESOLVED, chainTestA);
		expectedEvents[4] = new BundleEvent(BundleEvent.RESOLVED, chainTest);

		Object[] actualEvents = syncListenerResults.getResults(5);
		compareResults(expectedEvents, actualEvents);
		try {
			System.setProperty("test.bug300692", "true");
			System.setProperty("test.bug300692.listener", "true");
			chainTest.start();
			sl.setStartLevel(testSL);
			Object[] expectedFrameworkEvents = new Object[1];
			expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, OSGiTestsActivator.getContext().getBundle(0), null);
			Object[] actualFrameworkEvents = frameworkListenerResults.getResults(1);
			compareResults(expectedFrameworkEvents, actualFrameworkEvents);

			expectedEvents = new Object[14];
			int i = 0;

			expectedEvents[i++] = new BundleEvent(BundleEvent.LAZY_ACTIVATION, chainTestA);
			expectedEvents[i++] = new BundleEvent(BundleEvent.LAZY_ACTIVATION, chainTestB);
			expectedEvents[i++] = new BundleEvent(BundleEvent.LAZY_ACTIVATION, chainTestC);
			expectedEvents[i++] = new BundleEvent(BundleEvent.LAZY_ACTIVATION, chainTestD);
			expectedEvents[i++] = new BundleEvent(BundleEvent.STARTING, chainTest);
			expectedEvents[i++] = new BundleEvent(BundleEvent.STARTING, chainTestB);
			expectedEvents[i++] = new BundleEvent(BundleEvent.STARTED, chainTestB);
			expectedEvents[i++] = new BundleEvent(BundleEvent.STARTING, chainTestC);
			expectedEvents[i++] = new BundleEvent(BundleEvent.STARTED, chainTestC);
			expectedEvents[i++] = new BundleEvent(BundleEvent.STARTING, chainTestA);
			expectedEvents[i++] = new BundleEvent(BundleEvent.STARTED, chainTestA);
			expectedEvents[i++] = new BundleEvent(BundleEvent.STARTING, chainTestD);
			expectedEvents[i++] = new BundleEvent(BundleEvent.STARTED, chainTestD);
			expectedEvents[i++] = new BundleEvent(BundleEvent.STARTED, chainTest);

			actualFrameworkEvents = syncListenerResults.getResults(14);
			compareResults(expectedEvents, actualFrameworkEvents);
		} finally {
			System.getProperties().remove("test.bug300692");
			System.getProperties().remove("test.bug300692.listener");
			sl.setStartLevel(currentSL);
			Object[] expectedFrameworkEvents = new Object[1];
			expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, OSGiTestsActivator.getContext().getBundle(0), null);
			Object[] actualFrameworkEvents = frameworkListenerResults.getResults(1);
			compareResults(expectedFrameworkEvents, actualFrameworkEvents);
		}
	}

	public void testBug405918() throws BundleException {
		Bundle chainTest = installer.installBundle("chain.test"); //$NON-NLS-1$
		Bundle chainTestA = installer.installBundle("chain.test.a"); //$NON-NLS-1$
		Bundle chainTestB = installer.installBundle("chain.test.b"); //$NON-NLS-1$
		Bundle chainTestC = installer.installBundle("chain.test.c"); //$NON-NLS-1$
		Bundle chainTestD = installer.installBundle("chain.test.d"); //$NON-NLS-1$
		syncListenerResults.getResults(0);

		StartLevel sl = installer.getStartLevel();
		int currentSL = sl.getStartLevel();
		int testSL = currentSL + 1;
		sl.setBundleStartLevel(chainTest, testSL);
		sl.setBundleStartLevel(chainTestA, testSL);
		sl.setBundleStartLevel(chainTestB, testSL);
		sl.setBundleStartLevel(chainTestC, testSL);
		sl.setBundleStartLevel(chainTestD, testSL);
		installer.resolveBundles(new Bundle[] {chainTest, chainTestA, chainTestB, chainTestC, chainTestD});

		// eager start chainTestD
		chainTestD.start();

		Object[] expectedEvents = new Object[5];
		expectedEvents[0] = new BundleEvent(BundleEvent.RESOLVED, chainTestD);
		expectedEvents[1] = new BundleEvent(BundleEvent.RESOLVED, chainTestC);
		expectedEvents[2] = new BundleEvent(BundleEvent.RESOLVED, chainTestB);
		expectedEvents[3] = new BundleEvent(BundleEvent.RESOLVED, chainTestA);
		expectedEvents[4] = new BundleEvent(BundleEvent.RESOLVED, chainTest);

		Object[] actualEvents = syncListenerResults.getResults(5);
		compareResults(expectedEvents, actualEvents);
		try {
			System.setProperty("test.bug300692", "true");
			System.setProperty("test.bug300692.listener", "true");
			chainTest.start();
			sl.setStartLevel(testSL);
			Object[] expectedFrameworkEvents = new Object[1];
			expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, OSGiTestsActivator.getContext().getBundle(0), null);
			Object[] actualFrameworkEvents = frameworkListenerResults.getResults(1);
			compareResults(expectedFrameworkEvents, actualFrameworkEvents);

			expectedEvents = new Object[14];
			int i = 0;

			expectedEvents[i++] = new BundleEvent(BundleEvent.LAZY_ACTIVATION, chainTestA);
			expectedEvents[i++] = new BundleEvent(BundleEvent.LAZY_ACTIVATION, chainTestB);
			expectedEvents[i++] = new BundleEvent(BundleEvent.LAZY_ACTIVATION, chainTestC);
			expectedEvents[i++] = new BundleEvent(BundleEvent.STARTING, chainTest);
			expectedEvents[i++] = new BundleEvent(BundleEvent.STARTING, chainTestB);
			expectedEvents[i++] = new BundleEvent(BundleEvent.STARTED, chainTestB);
			expectedEvents[i++] = new BundleEvent(BundleEvent.STARTING, chainTestC);
			expectedEvents[i++] = new BundleEvent(BundleEvent.STARTED, chainTestC);
			expectedEvents[i++] = new BundleEvent(BundleEvent.STARTING, chainTestA);
			expectedEvents[i++] = new BundleEvent(BundleEvent.STARTED, chainTestA);
			expectedEvents[i++] = new BundleEvent(BundleEvent.LAZY_ACTIVATION, chainTestD);
			expectedEvents[i++] = new BundleEvent(BundleEvent.STARTING, chainTestD);
			expectedEvents[i++] = new BundleEvent(BundleEvent.STARTED, chainTestD);
			expectedEvents[i++] = new BundleEvent(BundleEvent.STARTED, chainTest);

			actualFrameworkEvents = syncListenerResults.getResults(14);
			compareResults(expectedEvents, actualFrameworkEvents);
		} finally {
			System.getProperties().remove("test.bug300692");
			System.getProperties().remove("test.bug300692.listener");
			sl.setStartLevel(currentSL);
			Object[] expectedFrameworkEvents = new Object[1];
			expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, OSGiTestsActivator.getContext().getBundle(0), null);
			Object[] actualFrameworkEvents = frameworkListenerResults.getResults(1);
			compareResults(expectedFrameworkEvents, actualFrameworkEvents);
		}
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

	public void testFragmentMultiHost() throws Exception {
		Bundle hostA1 = installer.installBundle("fragment.test.attach.host.a"); //$NON-NLS-1$
		Bundle hostA2 = installer.installBundle("fragment.test.attach.host.a.v2"); //$NON-NLS-1$
		Bundle fragA = installer.installBundle("fragment.test.attach.frag.a"); //$NON-NLS-1$
		assertTrue("Host/Frag resolve", installer.resolveBundles(new Bundle[] {hostA1, hostA2, fragA})); //$NON-NLS-1$

		assertEquals("Wrong number of hosts", 2, installer.getPackageAdmin().getHosts(fragA).length); //$NON-NLS-1$
		runTestRunner(hostA1, "fragment.test.attach.host.a.internal.test.TestPackageAccess"); //$NON-NLS-1$
		runTestRunner(hostA2, "fragment.test.attach.host.a.internal.test.TestPackageAccess2"); //$NON-NLS-1$
	}

	private void runTestRunner(Bundle host, String classname) {
		try {
			ITestRunner testRunner = (ITestRunner) host.loadClass(classname).newInstance();
			testRunner.testIt();
		} catch (Exception e) {
			fail("Failed package access test", e); //$NON-NLS-1$
		}

	}

	public void testFragmentExportPackage() throws Exception {
		Bundle hostA = installer.installBundle("fragment.test.attach.host.a"); //$NON-NLS-1$
		assertTrue("Host resolve", installer.resolveBundles(new Bundle[] {hostA})); //$NON-NLS-1$

		// make sure class loader for hostA is initialized
		hostA.loadClass("fragment.test.attach.host.a.internal.test.PackageAccessTest"); //$NON-NLS-1$

		Bundle fragB = installer.installBundle("fragment.test.attach.frag.b"); //$NON-NLS-1$
		Bundle hostARequire = installer.installBundle("fragment.test.attach.host.a.require"); //$NON-NLS-1$
		assertTrue("RequireA/Frag", installer.resolveBundles(new Bundle[] {hostARequire, fragB})); //$NON-NLS-1$
		try {
			hostARequire.loadClass("fragment.test.attach.frag.b.Test"); //$NON-NLS-1$
		} catch (ClassNotFoundException e) {
			fail("Unexpected class loading exception", e); //$NON-NLS-1$
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

	public void testOSGiLazyStartDelay() throws Exception {
		final Bundle osgiD = installer.installBundle("osgi.lazystart.d"); //$NON-NLS-1$
		final Bundle osgiE = installer.installBundle("osgi.lazystart.e"); //$NON-NLS-1$
		assertTrue("osgi lazy start resolve", installer.resolveBundles(new Bundle[] {osgiD, osgiE})); //$NON-NLS-1$

		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					osgiD.loadClass("osgi.lazystart.d.DTest");
				} catch (ClassNotFoundException e) {
					// should fail here
					throw new RuntimeException(e);
				}
			}
		}, "Starting: " + osgiD);
		t.start();

		Thread.sleep(100);
		long startTime = System.currentTimeMillis();
		osgiE.start();
		long endTime = System.currentTimeMillis() - startTime;
		assertTrue("Starting of test bundle was too short: " + endTime, endTime > 3000);
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
			assertEquals("Expected invalid operation", BundleException.START_TRANSIENT_ERROR, e.getType()); //$NON-NLS-1$
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

	public void testStartResolve() throws Exception {
		// install a bundle and set its start-level high, then crank up the framework start-level.  This should result in no events
		Bundle test = installer.installBundle("test"); //$NON-NLS-1$
		StartLevel startLevel = installer.getStartLevel();
		startLevel.setBundleStartLevel(test, startLevel.getStartLevel() + 10);
		try {
			test.start();
		} catch (BundleException e) {
			fail("Unexpected exception", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state", Bundle.INSTALLED, test.getState()); //$NON-NLS-1$
		startLevel.setStartLevel(startLevel.getStartLevel() + 15);
		Object[] expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, OSGiTestsActivator.getContext().getBundle(0), null);
		Object[] actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);
		assertEquals("Wrong state", Bundle.ACTIVE, test.getState()); //$NON-NLS-1$
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

	public void testBug258659_01() throws Exception {
		// install a bundle
		Bundle osgiA = installer.installBundle("osgi.lazystart.a"); //$NON-NLS-1$
		SynchronousBundleListener testLoadClassListener = new SynchronousBundleListener() {
			public void bundleChanged(BundleEvent event) {
				if (event.getType() == BundleEvent.LAZY_ACTIVATION)
					try {
						event.getBundle().loadClass("osgi.lazystart.a.ATest"); //$NON-NLS-1$
					} catch (ClassNotFoundException e) {
						simpleResults.addEvent(e);
					}
			}

		};
		OSGiTestsActivator.getContext().addBundleListener(testLoadClassListener);
		try {
			osgiA.start(Bundle.START_ACTIVATION_POLICY);
			Object[] expectedEvents = new Object[1];
			expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, osgiA);
			Object[] actualEvents = simpleResults.getResults(1);
			compareResults(expectedEvents, actualEvents);
		} finally {
			OSGiTestsActivator.getContext().removeBundleListener(testLoadClassListener);
		}
	}

	public void testBug258659_02() throws Exception {
		// install a bundle
		Bundle osgiA = installer.installBundle("osgi.lazystart.a"); //$NON-NLS-1$
		osgiA.start(Bundle.START_ACTIVATION_POLICY);
		SynchronousBundleListener testLoadClassListener = new SynchronousBundleListener() {
			public void bundleChanged(BundleEvent event) {
				if (event.getType() == BundleEvent.LAZY_ACTIVATION)
					try {
						event.getBundle().loadClass("osgi.lazystart.a.ATest"); //$NON-NLS-1$
					} catch (ClassNotFoundException e) {
						simpleResults.addEvent(e);
					}
			}

		};
		OSGiTestsActivator.getContext().addBundleListener(testLoadClassListener);
		try {
			installer.refreshPackages(new Bundle[] {osgiA});
			Object[] expectedEvents = new Object[1];
			expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, osgiA);
			Object[] actualEvents = simpleResults.getResults(1);
			compareResults(expectedEvents, actualEvents);
		} finally {
			OSGiTestsActivator.getContext().removeBundleListener(testLoadClassListener);
		}
	}

	public void testBug258659_03() throws Exception {
		// install a bundle
		Bundle test = installer.installBundle("test"); //$NON-NLS-1$
		SynchronousBundleListener testLoadClassListener = new SynchronousBundleListener() {
			public void bundleChanged(BundleEvent event) {
				if (event.getType() == BundleEvent.STARTED)
					try {
						event.getBundle().stop();
					} catch (BundleException e) {
						simpleResults.addEvent(e);
					}
			}

		};
		OSGiTestsActivator.getContext().addBundleListener(testLoadClassListener);
		try {
			test.start();
			Object[] expectedEvents = new Object[2];
			expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, test);
			expectedEvents[1] = new BundleEvent(BundleEvent.STOPPED, test);
			Object[] actualEvents = simpleResults.getResults(2);
			compareResults(expectedEvents, actualEvents);
		} finally {
			OSGiTestsActivator.getContext().removeBundleListener(testLoadClassListener);
		}
	}

	public void testBug258659_04() throws Exception {
		// install a bundle
		Bundle test = installer.installBundle("test"); //$NON-NLS-1$
		test.start();
		SynchronousBundleListener testLoadClassListener = new SynchronousBundleListener() {
			public void bundleChanged(BundleEvent event) {
				if (event.getType() == BundleEvent.STARTED)
					try {
						event.getBundle().stop();
					} catch (BundleException e) {
						simpleResults.addEvent(e);
					}
			}

		};
		// clear the results from the initial start
		simpleResults.getResults(0);
		// listen for the events from refreshing
		OSGiTestsActivator.getContext().addBundleListener(testLoadClassListener);
		try {
			installer.refreshPackages(new Bundle[] {test});
			Object[] expectedEvents = new Object[3];
			expectedEvents[0] = new BundleEvent(BundleEvent.STOPPED, test);
			expectedEvents[1] = new BundleEvent(BundleEvent.STARTED, test);
			expectedEvents[2] = new BundleEvent(BundleEvent.STOPPED, test);
			Object[] actualEvents = simpleResults.getResults(3);
			compareResults(expectedEvents, actualEvents);
		} finally {
			OSGiTestsActivator.getContext().removeBundleListener(testLoadClassListener);
		}
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

	public void testGetEntryDir01() throws BundleException {
		Bundle test = installer.installBundle("test"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] {test});
		URL aDir = test.getEntry("a"); //$NON-NLS-1$
		assertNotNull("aDir", aDir); //$NON-NLS-1$
		assertTrue(aDir.toExternalForm(), aDir.getFile().endsWith("/")); //$NON-NLS-1$
		URL bDir = test.getEntry("a/b"); //$NON-NLS-1$
		assertNotNull("bDir", bDir); //$NON-NLS-1$
		assertTrue(bDir.toExternalForm(), bDir.getFile().endsWith("/")); //$NON-NLS-1$

		aDir = test.getEntry("a/"); //$NON-NLS-1$
		assertNotNull("aDir", aDir); //$NON-NLS-1$
		assertTrue(aDir.toExternalForm(), aDir.getFile().endsWith("/")); //$NON-NLS-1$
		bDir = test.getEntry("a/b/"); //$NON-NLS-1$
		assertNotNull("bDir", bDir); //$NON-NLS-1$
		assertTrue(bDir.toExternalForm(), bDir.getFile().endsWith("/")); //$NON-NLS-1$

		URL dResource = test.getEntry("a/b/c/d"); //$NON-NLS-1$
		assertNotNull("dResource", dResource); //$NON-NLS-1$
		assertFalse(dResource.toExternalForm(), dResource.getFile().endsWith("/")); //$NON-NLS-1$

		dResource = test.getEntry("a/b/c/d/"); //$NON-NLS-1$
		if (dResource != null) // note that File bundles will return non-null whilc jar'ed bundles will return null
			assertFalse(dResource.toExternalForm(), dResource.getFile().endsWith("/")); //$NON-NLS-1$

	}

	public void testGetResourceDir01() throws BundleException {
		Bundle test = installer.installBundle("test"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] {test});
		URL aDir = test.getResource("a"); //$NON-NLS-1$
		assertNotNull("aDir", aDir); //$NON-NLS-1$
		assertTrue(aDir.toExternalForm(), aDir.getFile().endsWith("/")); //$NON-NLS-1$
		URL bDir = test.getResource("a/b"); //$NON-NLS-1$
		assertNotNull("bDir", bDir); //$NON-NLS-1$
		assertTrue(bDir.toExternalForm(), bDir.getFile().endsWith("/")); //$NON-NLS-1$

		aDir = test.getResource("a/"); //$NON-NLS-1$
		assertNotNull("aDir", aDir); //$NON-NLS-1$
		assertTrue(aDir.toExternalForm(), aDir.getFile().endsWith("/")); //$NON-NLS-1$
		bDir = test.getResource("a/b/"); //$NON-NLS-1$
		assertNotNull("bDir", bDir); //$NON-NLS-1$
		assertTrue(bDir.toExternalForm(), bDir.getFile().endsWith("/")); //$NON-NLS-1$

		URL dResource = test.getResource("a/b/c/d"); //$NON-NLS-1$
		assertNotNull("dResource", dResource); //$NON-NLS-1$
		assertFalse(dResource.toExternalForm(), dResource.getFile().endsWith("/")); //$NON-NLS-1$

		dResource = test.getResource("a/b/c/d/"); //$NON-NLS-1$
		if (dResource != null) // note that File bundles will return non-null whilc jar'ed bundles will return null
			assertFalse(dResource.toExternalForm(), dResource.getFile().endsWith("/")); //$NON-NLS-1$

	}

	public void testBootGetResources01() throws Exception {
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
		int dotIndex = manifest.getHost().indexOf('.');
		long bundleId = dotIndex >= 0 && dotIndex < manifest.getHost().length() - 1 ? Long.parseLong(manifest.getHost().substring(0, dotIndex)) : Long.parseLong(manifest.getHost());
		assertEquals("host id", test.getBundleId(), bundleId); //$NON-NLS-1$
	}

	public void testBootGetResources02() throws Exception {
		// properly test bug 375783 when used as a parent class loader
		// This will fail on the IBM VM (see bug 409314)
		if (System.getProperty(Constants.FRAMEWORK_BOOTDELEGATION) != null)
			return; // cannot really test this if this property is set
		Bundle test = installer.installBundle("test"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] {test});
		BundleWiring wiring = test.adapt(BundleWiring.class);
		ClassLoader bcl = wiring.getClassLoader();
		ClassLoader cl = new URLClassLoader(new URL[0], bcl);
		Enumeration manifests = cl.getResources("META-INF/MANIFEST.MF"); //$NON-NLS-1$
		assertNotNull("manifests", manifests); //$NON-NLS-1$
		ArrayList manifestURLs = new ArrayList();
		while (manifests.hasMoreElements())
			manifestURLs.add(manifests.nextElement());
		assertEquals("manifest number", 1, manifestURLs.size()); //$NON-NLS-1$
		URL manifest = (URL) manifestURLs.get(0);
		assertEquals("wrong protocol", "bundleresource", manifest.getProtocol());
		int dotIndex = manifest.getHost().indexOf('.');
		long bundleId = dotIndex >= 0 && dotIndex < manifest.getHost().length() - 1 ? Long.parseLong(manifest.getHost().substring(0, dotIndex)) : Long.parseLong(manifest.getHost());
		assertEquals("host id", test.getBundleId(), bundleId); //$NON-NLS-1$
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

	public void testMultipleGetResources01a() throws Exception {
		Bundle test = installer.installBundle("test"); //$NON-NLS-1$
		// test that we can get multiple resources from a bundle
		// and use that bundle's class loader as a parent
		installer.resolveBundles(new Bundle[] {test});
		BundleWiring wiring = test.adapt(BundleWiring.class);
		ClassLoader bcl = wiring.getClassLoader();
		ClassLoader cl = new URLClassLoader(new URL[0], bcl);
		Enumeration resources = cl.getResources("data/resource1"); //$NON-NLS-1$
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

	public void testMultipleGetResources03() throws Exception {
		installer.installBundle("test"); //$NON-NLS-1$
		Bundle test2 = installer.installBundle("test2"); //$NON-NLS-1$
		// test that we can get multiple resources from a bundle
		// test that using a context gives correct results for multiple resources (bug 261853)
		Enumeration resources = test2.getResources("data/"); //$NON-NLS-1$
		assertNotNull("resources", resources); //$NON-NLS-1$
		ArrayList resourceURLs = new ArrayList();
		while (resources.hasMoreElements())
			resourceURLs.add(resources.nextElement());
		assertEquals("resource number", 4, resourceURLs.size()); //$NON-NLS-1$
		assertEquals("root resource", "root classpath", readURL(new URL((URL) resourceURLs.get(0), "resource1"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals("stuff resource", "stuff classpath", readURL(new URL((URL) resourceURLs.get(1), "resource1"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals("root resource", "root classpath test2", readURL(new URL((URL) resourceURLs.get(2), "resource1"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals("stuff resource", "stuff classpath test2", readURL(new URL((URL) resourceURLs.get(3), "resource1"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public void testListResources() throws BundleException {
		installer.installBundle("test"); //$NON-NLS-1$
		Bundle test2 = installer.installBundle("test2"); //$NON-NLS-1$

		assertTrue("Could not resolve test2 bundle", installer.resolveBundles(new Bundle[] {test2}));
		BundleWiring test2Wiring = (BundleWiring) test2.adapt(BundleWiring.class);
		Collection resources = test2Wiring.listResources("/", "*", 0);
		assertTrue("could not find resource", resources.contains("resource2"));
		resources = test2Wiring.listResources("data/", "resource2", 0);
		assertTrue("could not find resource", resources.contains("data/resource2"));
		resources = test2Wiring.listResources("/", "resource*", BundleWiring.LISTRESOURCES_RECURSE);
		assertTrue("could not find resource", resources.contains("data/resource2"));
	}

	public void testURLExternalFormat01() throws Exception {
		Bundle test = installer.installBundle("test"); //$NON-NLS-1$
		// test the external format of bundle entry URLs
		URL entry = test.getEntry("data/resource1"); //$NON-NLS-1$
		assertNotNull("entry", entry); //$NON-NLS-1$
		assertEquals("root resource", "root classpath", readURL(entry)); //$NON-NLS-1$ //$NON-NLS-2$
		URL entryCopy = new URL(entry.toExternalForm());
		assertEquals("external format", entry.toExternalForm(), entryCopy.toExternalForm()); //$NON-NLS-1$
		assertEquals("root resource", "root classpath", readURL(entryCopy)); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testURLExternalFormat02() throws Exception {
		Bundle test = installer.installBundle("test"); //$NON-NLS-1$
		// test the external format of bundle entry URLs
		URL entry = test.getResource("data/resource1"); //$NON-NLS-1$
		assertNotNull("entry", entry); //$NON-NLS-1$
		assertEquals("root resource", "root classpath", readURL(entry)); //$NON-NLS-1$ //$NON-NLS-2$
		URL entryCopy = new URL(entry.toExternalForm());
		assertEquals("external format", entry.toExternalForm(), entryCopy.toExternalForm()); //$NON-NLS-1$
		assertEquals("root resource", "root classpath", readURL(entryCopy)); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testURLExternalFormat03() throws BundleException {
		Bundle test = installer.installBundle("test"); //$NON-NLS-1$
		// test the external format of bundle resource URLs
		URL entry = test.getEntry("data/resource1"); //$NON-NLS-1$
		assertNotNull("entry", entry); //$NON-NLS-1$
		URI uri1 = null;
		URI uri2 = null;
		URI uri3 = null;
		try {
			uri1 = new URI(entry.getProtocol(), null, entry.getHost(), entry.getPort(), entry.getPath(), null, entry.getQuery());
			uri2 = new URI(entry.getProtocol(), entry.getHost(), entry.getPath(), entry.getQuery());
			uri3 = new URI(entry.toExternalForm());
		} catch (URISyntaxException e) {
			fail("Unexpected URI exception", e); //$NON-NLS-1$
		}
		URL url1 = null;
		URL url2 = null;
		URL url3 = null;
		URL url4 = null;
		try {
			url1 = uri1.toURL();
			url2 = uri2.toURL();
			url3 = uri3.toURL();
			url4 = new URL(uri1.toString());
		} catch (MalformedURLException e) {
			fail("Unexpected URL exception", e); //$NON-NLS-1$
		}
		checkURL("root classpath", entry, url1); //$NON-NLS-1$
		checkURL("root classpath", entry, url2); //$NON-NLS-1$
		checkURL("root classpath", entry, url3); //$NON-NLS-1$
		checkURL("root classpath", entry, url4); //$NON-NLS-1$
	}

	public void testURLExternalFormat04() throws BundleException {
		Bundle test = installer.installBundle("test"); //$NON-NLS-1$
		// test the external format of bundle resource URLs
		URL entry = test.getResource("data/resource1"); //$NON-NLS-1$
		assertNotNull("entry", entry); //$NON-NLS-1$
		URI uri1 = null;
		URI uri2 = null;
		URI uri3 = null;
		try {
			uri1 = new URI(entry.getProtocol(), null, entry.getHost(), entry.getPort(), entry.getPath(), null, entry.getQuery());
			uri2 = new URI(entry.getProtocol(), entry.getHost(), entry.getPath(), entry.getQuery());
			uri3 = new URI(entry.toExternalForm());
		} catch (URISyntaxException e) {
			fail("Unexpected URI exception", e); //$NON-NLS-1$
		}
		URL url1 = null;
		URL url2 = null;
		URL url3 = null;
		URL url4 = null;
		try {
			url1 = uri1.toURL();
			url2 = uri2.toURL();
			url3 = uri3.toURL();
			url4 = new URL(uri1.toString());
		} catch (MalformedURLException e) {
			fail("Unexpected URL exception", e); //$NON-NLS-1$
		}
		checkURL("root classpath", entry, url1); //$NON-NLS-1$
		checkURL("root classpath", entry, url2); //$NON-NLS-1$
		checkURL("root classpath", entry, url3); //$NON-NLS-1$
		checkURL("root classpath", entry, url4); //$NON-NLS-1$
	}

	private void checkURL(String content, URL orig, URL copy) {
		assertEquals("external format", orig.toExternalForm(), copy.toExternalForm()); //$NON-NLS-1$
		assertEquals(content, content, readURL(copy));
	}

	public void testURI() throws URISyntaxException {
		new URI("bundleentry", "1", "/test", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public void testMultipleExportFragments01() throws Exception {
		Bundle host = installer.installBundle("host.multiple.exports"); //$NON-NLS-1$
		Bundle frag = installer.installBundle("frag.multiple.exports"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] {host, frag});
		PackageAdmin packageAdmin = installer.getPackageAdmin();
		ExportedPackage[] hostExports = packageAdmin.getExportedPackages(host);
		assertEquals("Number host exports", 4, hostExports == null ? 0 : hostExports.length); //$NON-NLS-1$

		BundleWiring hostWiring = (BundleWiring) host.adapt(BundleWiring.class);
		assertNotNull("No host wiring", hostWiring);

		List packageCapabilities = hostWiring.getCapabilities(PackageNamespace.PACKAGE_NAMESPACE);
		assertEquals("Number host export capabilities", 4, packageCapabilities.size()); //$NON-NLS-1$

		assertEquals("Check export name", "host.multiple.exports", ((BundleCapability) packageCapabilities.get(0)).getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE)); //$NON-NLS-1$//$NON-NLS-2$
		assertEquals("Check include directive", "Public*", (String) ((BundleCapability) packageCapabilities.get(0)).getDirectives().get(PackageNamespace.CAPABILITY_INCLUDE_DIRECTIVE)); //$NON-NLS-1$//$NON-NLS-2$

		assertEquals("Check export name", "host.multiple.exports.onlyone", ((BundleCapability) packageCapabilities.get(1)).getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE)); //$NON-NLS-1$ //$NON-NLS-2$

		assertEquals("Check export name", "host.multiple.exports", ((BundleCapability) packageCapabilities.get(2)).getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE)); //$NON-NLS-1$//$NON-NLS-2$
		assertEquals("Check scope attribute", "private", (String) ((BundleCapability) packageCapabilities.get(2)).getAttributes().get("scope")); //$NON-NLS-1$//$NON-NLS-2$

		assertEquals("Check export name", "host.multiple.exports.onlyone", ((BundleCapability) packageCapabilities.get(3)).getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE)); //$NON-NLS-1$ //$NON-NLS-2$
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

	public void disableTestXFriends() throws Exception {
		// TODO this will fail since we don't have strict mode in the new framework
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
			// would be used to disable strict mode if we could
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

	public void testUninstallInUse01() throws BundleException {
		Bundle exporter1 = installer.installBundle("exporter.importer1"); //$NON-NLS-1$
		BundleRevision iExporter1 = exporter1.adapt(BundleRevision.class);
		Bundle exporter2 = installer.installBundle("exporter.importer2"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] {exporter1, exporter2});
		exporter1.uninstall();
		Bundle importer = installer.installBundle("exporter.importer4"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] {importer});
		BundleWiring importerWiring = importer.adapt(BundleWiring.class);
		assertNotNull("Bundle b has no wiring.", importerWiring);
		List<BundleWire> bImports = importerWiring.getRequiredWires(PackageNamespace.PACKAGE_NAMESPACE);
		assertEquals("Wrong number of imported packages.", 1, bImports.size());
		assertEquals("Wrong exporter.", iExporter1, bImports.get(0).getProvider());
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

	public void testBuddyClassLoadingRegistered1() throws Exception {
		Bundle registeredA = installer.installBundle("buddy.registered.a"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] {registeredA});
		Enumeration testFiles = registeredA.getResources("resources/test.txt"); //$NON-NLS-1$
		assertNotNull("testFiles", testFiles); //$NON-NLS-1$
		ArrayList texts = new ArrayList();
		while (testFiles.hasMoreElements())
			texts.add(readURL((URL) testFiles.nextElement()));
		assertEquals("test.txt number", 1, texts.size()); //$NON-NLS-1$
		assertTrue("buddy.registered.a", texts.contains("buddy.registered.a")); //$NON-NLS-1$ //$NON-NLS-2$

		Bundle registeredATest1 = installer.installBundle("buddy.registered.a.test1"); //$NON-NLS-1$
		Bundle registeredATest2 = installer.installBundle("buddy.registered.a.test2"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] {registeredATest1, registeredATest2});
		testFiles = registeredA.getResources("resources/test.txt"); //$NON-NLS-1$
		assertNotNull("testFiles", testFiles); //$NON-NLS-1$
		texts = new ArrayList();
		while (testFiles.hasMoreElements())
			texts.add(readURL((URL) testFiles.nextElement()));

		// The real test
		assertEquals("test.txt number", 3, texts.size()); //$NON-NLS-1$
		assertTrue("buddy.registered.a", texts.contains("buddy.registered.a")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("buddy.registered.a.test1", texts.contains("buddy.registered.a.test1")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("buddy.registered.a.test2", texts.contains("buddy.registered.a.test2")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testBuddyClassLoadingRegistered2() throws Exception {
		Bundle registeredA = installer.installBundle("buddy.registered.a"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] {registeredA});
		URL testFile = registeredA.getResource("resources/test1.txt"); //$NON-NLS-1$
		assertNull("test1.txt", testFile); //$NON-NLS-1$

		testFile = registeredA.getResource("resources/test2.txt"); //$NON-NLS-1$
		assertNull("test2.txt", testFile); //$NON-NLS-1$

		Bundle registeredATest1 = installer.installBundle("buddy.registered.a.test1"); //$NON-NLS-1$
		Bundle registeredATest2 = installer.installBundle("buddy.registered.a.test2"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] {registeredATest1, registeredATest2});

		testFile = registeredA.getResource("resources/test1.txt"); //$NON-NLS-1$
		assertNotNull("test1.txt", testFile); //$NON-NLS-1$
		assertEquals("buddy.registered.a.test1", "buddy.registered.a.test1", readURL(testFile)); //$NON-NLS-1$ //$NON-NLS-2$

		testFile = registeredA.getResource("resources/test2.txt"); //$NON-NLS-1$
		assertNotNull("test2.txt", testFile); //$NON-NLS-1$
		assertEquals("buddy.registered.a.test2", "buddy.registered.a.test2", readURL(testFile)); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testBuddyClassLoadingRegistered3() throws Exception {
		Bundle registeredA = installer.installBundle("buddy.registered.a"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] {registeredA});
		try {
			registeredA.loadClass("buddy.registered.a.test1.ATest"); //$NON-NLS-1$
			fail("expected ClassNotFoundException"); //$NON-NLS-1$
		} catch (ClassNotFoundException e) {
			// expected
		}
		try {
			registeredA.loadClass("buddy.registered.a.test2.ATest"); //$NON-NLS-1$
			fail("expected ClassNotFoundException"); //$NON-NLS-1$
		} catch (ClassNotFoundException e) {
			// expected
		}
		Bundle registeredATest1 = installer.installBundle("buddy.registered.a.test1"); //$NON-NLS-1$
		Bundle registeredATest2 = installer.installBundle("buddy.registered.a.test2"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] {registeredATest1, registeredATest2});

		try {
			Class testClass = registeredA.loadClass("buddy.registered.a.test1.ATest"); //$NON-NLS-1$
			assertNotNull("testClass", testClass); //$NON-NLS-1$
		} catch (ClassNotFoundException e) {
			fail("Unexpected ClassNotFoundException", e); //$NON-NLS-1$
		}

		try {
			Class testClass = registeredA.loadClass("buddy.registered.a.test2.ATest"); //$NON-NLS-1$
			assertNotNull("testClass", testClass); //$NON-NLS-1$
		} catch (ClassNotFoundException e) {
			fail("Unexpected ClassNotFoundException", e); //$NON-NLS-1$
		}
	}

	public void testBuddyClassLoadingDependent1() throws Exception {
		Bundle dependentA = installer.installBundle("buddy.dependent.a"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] {dependentA});
		Enumeration testFiles = dependentA.getResources("resources/test.txt"); //$NON-NLS-1$
		assertNotNull("testFiles", testFiles); //$NON-NLS-1$
		ArrayList texts = new ArrayList();
		while (testFiles.hasMoreElements())
			texts.add(readURL((URL) testFiles.nextElement()));
		assertEquals("test.txt number", 1, texts.size()); //$NON-NLS-1$
		assertTrue("buddy.dependent.a", texts.contains("buddy.dependent.a")); //$NON-NLS-1$ //$NON-NLS-2$

		Bundle dependentATest1 = installer.installBundle("buddy.dependent.a.test1"); //$NON-NLS-1$
		Bundle dependentATest2 = installer.installBundle("buddy.dependent.a.test2"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] {dependentATest1, dependentATest2});
		testFiles = dependentA.getResources("resources/test.txt"); //$NON-NLS-1$
		assertNotNull("testFiles", testFiles); //$NON-NLS-1$
		texts = new ArrayList();
		while (testFiles.hasMoreElements())
			texts.add(readURL((URL) testFiles.nextElement()));
		assertEquals("test.txt number", 3, texts.size()); //$NON-NLS-1$
		assertTrue("buddy.dependent.a", texts.contains("buddy.dependent.a")); //$NON-NLS-1$//$NON-NLS-2$
		assertTrue("buddy.dependent.a.test1", texts.contains("buddy.dependent.a.test1")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("buddy.dependent.a.test2", texts.contains("buddy.dependent.a.test2")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testBuddyClassLoadingDependent2() throws Exception {
		Bundle dependentA = installer.installBundle("buddy.dependent.a"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] {dependentA});
		URL testFile = dependentA.getResource("resources/test1.txt"); //$NON-NLS-1$
		assertNull("test1.txt", testFile); //$NON-NLS-1$

		testFile = dependentA.getResource("resources/test2.txt"); //$NON-NLS-1$
		assertNull("test2.txt", testFile); //$NON-NLS-1$

		Bundle dependentATest1 = installer.installBundle("buddy.dependent.a.test1"); //$NON-NLS-1$
		Bundle dependentATest2 = installer.installBundle("buddy.dependent.a.test2"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] {dependentATest1, dependentATest2});

		testFile = dependentA.getResource("resources/test1.txt"); //$NON-NLS-1$
		assertNotNull("test1.txt", testFile); //$NON-NLS-1$
		assertEquals("buddy.dependent.a.test1", "buddy.dependent.a.test1", readURL(testFile)); //$NON-NLS-1$ //$NON-NLS-2$

		testFile = dependentA.getResource("resources/test2.txt"); //$NON-NLS-1$
		assertNotNull("test2.txt", testFile); //$NON-NLS-1$
		assertEquals("buddy.dependent.a.test2", "buddy.dependent.a.test2", readURL(testFile)); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testBuddyClassLoadingDependent3() throws Exception {
		Bundle dependentA = installer.installBundle("buddy.dependent.a"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] {dependentA});

		try {
			dependentA.loadClass("buddy.dependent.a.test1.ATest"); //$NON-NLS-1$
			fail("expected ClassNotFoundException"); //$NON-NLS-1$
		} catch (ClassNotFoundException e) {
			// expected
		}

		try {
			dependentA.loadClass("buddy.dependent.a.test2.ATest"); //$NON-NLS-1$
			fail("expected ClassNotFoundException"); //$NON-NLS-1$
		} catch (ClassNotFoundException e) {
			// expected
		}

		Bundle dependentATest1 = installer.installBundle("buddy.dependent.a.test1"); //$NON-NLS-1$
		Bundle dependentATest2 = installer.installBundle("buddy.dependent.a.test2"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] {dependentATest1, dependentATest2});

		try {
			Class testClass = dependentA.loadClass("buddy.dependent.a.test1.ATest"); //$NON-NLS-1$
			assertNotNull("testClass", testClass); //$NON-NLS-1$
		} catch (ClassNotFoundException e) {
			fail("Unexpected ClassNotFoundException", e); //$NON-NLS-1$
		}
		try {
			Class testClass = dependentA.loadClass("buddy.dependent.a.test2.ATest"); //$NON-NLS-1$
			assertNotNull("testClass", testClass); //$NON-NLS-1$
		} catch (ClassNotFoundException e) {
			fail("Unexpected ClassNotFoundException", e); //$NON-NLS-1$
		}
	}

	public void testBuddyClassLoadingInvalid() throws Exception {
		Bundle invalidA = installer.installBundle("buddy.invalid.a"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] {invalidA});
		invalidA.getResource("doesNotExist");
		try {
			invalidA.loadClass("does.not.Exist");
		} catch (ClassNotFoundException e) {
			// expected
		}
	}

	public void testBuddyClassloadingBug438904() throws Exception {
		Bundle host = installer.installBundle("test.bug438904.host");
		Bundle frag = installer.installBundle("test.bug438904.frag");
		Bundle global = installer.installBundle("test.bug438904.global");
		installer.resolveBundles(new Bundle[] {host, frag, global});
		global.loadClass("test.bug438904.host.Test1");
		global.loadClass("test.bug438904.frag.Test2");
	}

	public void testBundleReference01() throws Exception {
		Bundle test = installer.installBundle("test"); //$NON-NLS-1$
		Class clazz = test.loadClass("test1.Activator"); //$NON-NLS-1$
		Bundle bundle = FrameworkUtil.getBundle(clazz);
		assertEquals("Wrong bundle", test, bundle); //$NON-NLS-1$
	}

	public void testBundleReference02() throws Exception {
		Bundle test = installer.installBundle("test"); //$NON-NLS-1$
		Class clazz = test.loadClass("test1.Activator"); //$NON-NLS-1$
		ClassLoader cl = clazz.getClassLoader();
		if (!(cl instanceof BundleReference))
			fail("ClassLoader is not of type BundleReference"); //$NON-NLS-1$
		assertEquals("Wrong bundle", test, ((BundleReference) cl).getBundle()); //$NON-NLS-1$
	}

	public void testResolveURLRelativeBundleResourceWithPort() throws Exception {
		URL directory = new URL("bundleresource://82:1/dictionaries/"); //$NON-NLS-1$
		assertEquals(1, directory.getPort());

		URL resource = new URL(directory, "en_GB.dictionary"); //$NON-NLS-1$
		assertEquals(1, resource.getPort());
	}

	public void testManifestPackageSpec() {
		try {
			Bundle test = installer.installBundle("test.manifestpackage"); //$NON-NLS-1$
			test.start();
		} catch (Exception e) {
			fail("Unexpected exception", e); //$NON-NLS-1$
		}
	}

	public void testArrayTypeLoad() {
		doTestArrayTypeLoad("[B"); //$NON-NLS-1$
		doTestArrayTypeLoad("[C"); //$NON-NLS-1$
		doTestArrayTypeLoad("[D"); //$NON-NLS-1$
		doTestArrayTypeLoad("[F"); //$NON-NLS-1$
		doTestArrayTypeLoad("[I"); //$NON-NLS-1$
		doTestArrayTypeLoad("[J"); //$NON-NLS-1$
		doTestArrayTypeLoad("[S"); //$NON-NLS-1$
		doTestArrayTypeLoad("[Z"); //$NON-NLS-1$
		doTestArrayTypeLoad("[Lorg.eclipse.osgi.tests.bundles.ArrayTest;"); //$NON-NLS-1$
		doTestArrayTypeLoad("[[D"); //$NON-NLS-1$
		doTestArrayTypeLoad("[[Lorg.eclipse.osgi.tests.bundles.ArrayTest;"); //$NON-NLS-1$
	}

	public void testSystemBundleGetResources01() {
		Bundle systemBundle = OSGiTestsActivator.getContext().getBundle(0);
		Enumeration resources = null;
		try {
			resources = systemBundle.getResources("systembundle.properties");
		} catch (IOException e) {
			fail("Failed to get resources", e);
		}
		assertNotNull("Resources is null", resources);
	}

	public void testSystemBundleGetResources02() {
		Bundle systemBundle = OSGiTestsActivator.getContext().getBundle(0);
		Enumeration resources = null;
		try {
			resources = systemBundle.getResources("java/lang/test.resource");
		} catch (IOException e) {
			fail("Failed to get resources", e);
		}
		assertNull("Resources is not null", resources);
	}

	public void testBug299921() {
		ClassLoader cl = this.getClass().getClassLoader();
		Enumeration resources = null;
		try {
			Method findMethod = ClassLoader.class.getDeclaredMethod("findResources", new Class[] {String.class});
			findMethod.setAccessible(true);

			resources = (Enumeration) findMethod.invoke(cl, new Object[] {"test/doesnotexist.txt"});
		} catch (Exception e) {
			fail("Unexpected error calling getResources", e);
		}
		assertNotNull("Should not be null", resources);
		assertFalse("Found resources!", resources.hasMoreElements());
		try {
			resources = cl.getResources("test/doesnotexist.txt");
		} catch (IOException e) {
			fail("Unexpected IOException", e);
		}
		assertNotNull("Should not be null", resources);
		assertFalse("Found resources!", resources.hasMoreElements());
	}

	public void testBug306181() throws BundleException {
		StartLevel sl = installer.getStartLevel();
		int origSL = sl.getStartLevel();
		int origBundleSL = sl.getInitialBundleStartLevel();
		int newSL = origSL + 1;
		sl.setInitialBundleStartLevel(newSL);
		try {
			Bundle a = installer.installBundle("test.bug306181a");
			Bundle b = installer.installBundle("test.bug306181b");

			sl.setBundleStartLevel(a, newSL);
			sl.setBundleStartLevel(b, newSL);
			installer.resolveBundles(new Bundle[] {a, b});
			a.start();
			b.start(Bundle.START_ACTIVATION_POLICY);

			sl.setStartLevel(newSL);
			Object[] expectedFrameworkEvents = new Object[1];
			expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, OSGiTestsActivator.getContext().getBundle(0), null);
			Object[] actualFrameworkEvents = frameworkListenerResults.getResults(1);
			compareResults(expectedFrameworkEvents, actualFrameworkEvents);

			assertEquals("Bundle A is not active", Bundle.ACTIVE, a.getState());
			assertEquals("Bundle B is not active", Bundle.STARTING, b.getState());
			ServiceReference[] regs = a.getRegisteredServices();
			if (regs != null && regs.length > 0) {
				fail(OSGiTestsActivator.getContext().getService(regs[0]).toString());
			}
		} finally {
			sl.setInitialBundleStartLevel(origBundleSL);
			sl.setStartLevel(origSL);
			Object[] expectedFrameworkEvents = new Object[1];
			expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, OSGiTestsActivator.getContext().getBundle(0), null);
			Object[] actualFrameworkEvents = frameworkListenerResults.getResults(1);
			compareResults(expectedFrameworkEvents, actualFrameworkEvents);
		}
	}

	public void testBug348805() {
		final boolean[] endCalled = {false};
		ResolverHookFactory error = new ResolverHookFactory() {
			public ResolverHook begin(Collection triggers) {
				return new ResolverHook() {
					public void filterSingletonCollisions(BundleCapability singleton, Collection collisionCandidates) {
						// Nothing
					}

					public void filterResolvable(Collection candidates) {
						throw new RuntimeException("Error");
					}

					public void filterMatches(BundleRequirement requirement, Collection candidates) {
						// Nothing
					}

					public void end() {
						endCalled[0] = true;
					}
				};
			}
		};
		ServiceRegistration reg = OSGiTestsActivator.getContext().registerService(ResolverHookFactory.class, error, null);
		try {
			Bundle test = installer.installBundle("test"); //$NON-NLS-1$
			try {
				test.start();
				fail("Should not be able to start this bundle");
			} catch (BundleException e) {
				// expected
				assertEquals("Wrong exception type.", BundleException.REJECTED_BY_HOOK, e.getType());
			}
		} catch (BundleException e) {
			fail("Unexpected install fail", e);
		} finally {
			reg.unregister();
		}
		assertTrue("end is not called", endCalled[0]);
	}

	public void testBug348806() {
		ResolverHookFactory error = new ResolverHookFactory() {
			public ResolverHook begin(Collection triggers) {
				return new ResolverHook() {
					public void filterSingletonCollisions(BundleCapability singleton, Collection collisionCandidates) {
						// Nothing
					}

					public void filterResolvable(Collection candidates) {
						// Nothing
					}

					public void filterMatches(BundleRequirement requirement, Collection candidates) {
						// Nothing
					}

					public void end() {
						throw new RuntimeException("Error");
					}
				};
			}
		};
		ServiceRegistration reg = OSGiTestsActivator.getContext().registerService(ResolverHookFactory.class, error, null);
		try {
			Bundle test = installer.installBundle("test"); //$NON-NLS-1$
			try {
				test.start();
				fail("Should not be able to start this bundle");
			} catch (BundleException e) {
				// expected
				assertEquals("Wrong exception type.", BundleException.REJECTED_BY_HOOK, e.getType());
			}
		} catch (BundleException e) {
			fail("Unexpected install fail", e);
		} finally {
			reg.unregister();
		}
	}

	public void testBug370258_beginException() {
		final boolean[] endCalled = {false};
		ResolverHookFactory endHook = new ResolverHookFactory() {
			public ResolverHook begin(Collection triggers) {
				return new ResolverHook() {
					public void filterSingletonCollisions(BundleCapability singleton, Collection collisionCandidates) {
						// Nothing
					}

					public void filterResolvable(Collection candidates) {
						throw new RuntimeException("Error");
					}

					public void filterMatches(BundleRequirement requirement, Collection candidates) {
						// Nothing
					}

					public void end() {
						endCalled[0] = true;
					}
				};
			}
		};
		ResolverHookFactory error = new ResolverHookFactory() {
			public ResolverHook begin(Collection triggers) {
				throw new RuntimeException("Error");
			}
		};

		ServiceRegistration endReg = OSGiTestsActivator.getContext().registerService(ResolverHookFactory.class, endHook, null);
		ServiceRegistration errorReg = OSGiTestsActivator.getContext().registerService(ResolverHookFactory.class, error, null);
		try {
			Bundle test = installer.installBundle("test"); //$NON-NLS-1$
			try {
				test.start();
				fail("Should not be able to start this bundle");
			} catch (BundleException e) {
				// expected
				assertEquals("Wrong exception type.", BundleException.REJECTED_BY_HOOK, e.getType());
			}
		} catch (BundleException e) {
			fail("Unexpected install fail", e);
		} finally {
			errorReg.unregister();
			endReg.unregister();
		}
		assertTrue("end is not called", endCalled[0]);
	}

	public void testBug370258_endException() {
		final boolean[] endCalled = {false};
		ResolverHookFactory endHook = new ResolverHookFactory() {
			public ResolverHook begin(Collection triggers) {
				return new ResolverHook() {
					public void filterSingletonCollisions(BundleCapability singleton, Collection collisionCandidates) {
						// Nothing
					}

					public void filterResolvable(Collection candidates) {
						throw new RuntimeException("Error");
					}

					public void filterMatches(BundleRequirement requirement, Collection candidates) {
						// Nothing
					}

					public void end() {
						endCalled[0] = true;
					}
				};
			}
		};
		ResolverHookFactory error = new ResolverHookFactory() {
			public ResolverHook begin(Collection triggers) {
				return new ResolverHook() {
					public void filterSingletonCollisions(BundleCapability singleton, Collection collisionCandidates) {
						// Nothing
					}

					public void filterResolvable(Collection candidates) {
						// Nothing
					}

					public void filterMatches(BundleRequirement requirement, Collection candidates) {
						// Nothing
					}

					public void end() {
						throw new RuntimeException("Error");
					}
				};
			}
		};

		ServiceRegistration errorReg = OSGiTestsActivator.getContext().registerService(ResolverHookFactory.class, error, null);
		ServiceRegistration endReg = OSGiTestsActivator.getContext().registerService(ResolverHookFactory.class, endHook, null);
		try {
			Bundle test = installer.installBundle("test"); //$NON-NLS-1$
			try {
				test.start();
				fail("Should not be able to start this bundle");
			} catch (BundleException e) {
				// expected
				assertEquals("Wrong exception type.", BundleException.REJECTED_BY_HOOK, e.getType());
			}
		} catch (BundleException e) {
			fail("Unexpected install fail", e);
		} finally {
			errorReg.unregister();
			endReg.unregister();
		}
		assertTrue("end is not called", endCalled[0]);
	}

	public void testLoadClassUnresolved() throws Exception {
		Bundle chainTest = installer.installBundle("chain.test"); //$NON-NLS-1$
		assertFalse("Should not resolve bundle: " + chainTest, installer.resolveBundles(new Bundle[] {chainTest}));
		try {
			fail("Should not be able to load class: " + chainTest.loadClass("chain.test.TestSingleChain")); //$NON-NLS-1$
		} catch (ClassNotFoundException e) {
			// expected
		}

	}

	private void doTestArrayTypeLoad(String name) {
		try {
			Class arrayType = OSGiTestsActivator.getContext().getBundle().loadClass(name);
			assertNotNull("Null class", arrayType); //$NON-NLS-1$
			assertTrue("Class is not an array: " + arrayType, arrayType.isArray()); //$NON-NLS-1$
		} catch (ClassNotFoundException e) {
			fail("Unexpected exception", e); //$NON-NLS-1$
		}
	}

	private String readURL(URL url) {
		StringBuffer sb = new StringBuffer();
		try {
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
		} catch (IOException e) {
			fail("Unexpected exception reading url: " + url.toExternalForm(), e); //$NON-NLS-1$
		}
		return sb.toString();
	}
}
