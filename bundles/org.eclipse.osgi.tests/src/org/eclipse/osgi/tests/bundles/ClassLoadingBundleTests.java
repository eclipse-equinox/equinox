/*******************************************************************************
 * Copyright (c) 2006, 2022 IBM Corporation and others.
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import junit.framework.AssertionFailedError;
import org.eclipse.osgi.internal.loader.BundleLoader;
import org.eclipse.osgi.internal.loader.ModuleClassLoader;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleReference;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;

public class ClassLoadingBundleTests extends AbstractBundleTests {

	@Test
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

	@Test
	public void testLoadTriggerClass() throws Exception {
		Bundle chainTest = installer.installBundle("chain.test"); //$NON-NLS-1$
		Bundle chainTestA = installer.installBundle("chain.test.a"); //$NON-NLS-1$
		installer.installBundle("chain.test.b"); //$NON-NLS-1$
		installer.installBundle("chain.test.c"); //$NON-NLS-1$
		installer.installBundle("chain.test.d"); //$NON-NLS-1$
		assertTrue("Did not resolve chainTest", installer.resolveBundles(new Bundle[] { chainTest })); //$NON-NLS-1$
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

	@Test
	public void testChainDepedencies() throws Exception {
		Bundle chainTest = installer.installBundle("chain.test"); //$NON-NLS-1$
		Bundle chainTestA = installer.installBundle("chain.test.a"); //$NON-NLS-1$
		Bundle chainTestB = installer.installBundle("chain.test.b"); //$NON-NLS-1$
		installer.installBundle("chain.test.c"); //$NON-NLS-1$
		installer.installBundle("chain.test.d"); //$NON-NLS-1$
		((ITestRunner) chainTest.loadClass("chain.test.TestSingleChain").getDeclaredConstructor().newInstance()) //$NON-NLS-1$
				.testIt();

		Object[] expectedEvents = new Object[6];
		expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, chainTestB);
		expectedEvents[1] = new BundleEvent(BundleEvent.STARTED, chainTestA);
		expectedEvents[2] = new BundleEvent(BundleEvent.STOPPED, chainTestA);
		expectedEvents[3] = new BundleEvent(BundleEvent.STOPPED, chainTestB);
		expectedEvents[4] = new BundleEvent(BundleEvent.STARTED, chainTestB);
		expectedEvents[5] = new BundleEvent(BundleEvent.STARTED, chainTestA);

		installer.refreshPackages(new Bundle[] { chainTestB });

		((ITestRunner) chainTest.loadClass("chain.test.TestSingleChain").getDeclaredConstructor().newInstance()) //$NON-NLS-1$
				.testIt();

		Object[] actualEvents = simpleResults.getResults(6);
		compareResults(expectedEvents, actualEvents);
	}

	@Test
	public void testMultiChainDepedencies01() throws Exception {
		Bundle chainTest = installer.installBundle("chain.test"); //$NON-NLS-1$
		Bundle chainTestA = installer.installBundle("chain.test.a"); //$NON-NLS-1$
		Bundle chainTestB = installer.installBundle("chain.test.b"); //$NON-NLS-1$
		Bundle chainTestC = installer.installBundle("chain.test.c"); //$NON-NLS-1$
		Bundle chainTestD = installer.installBundle("chain.test.d"); //$NON-NLS-1$
		chainTest.loadClass("chain.test.TestMultiChain").getDeclaredConstructor().newInstance(); //$NON-NLS-1$

		Object[] expectedEvents = new Object[8];
		expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, chainTestD);
		expectedEvents[1] = new BundleEvent(BundleEvent.STARTED, chainTestB);
		expectedEvents[2] = new BundleEvent(BundleEvent.STARTED, chainTestC);
		expectedEvents[3] = new BundleEvent(BundleEvent.STARTED, chainTestA);
		expectedEvents[4] = new BundleEvent(BundleEvent.STOPPED, chainTestA);
		expectedEvents[5] = new BundleEvent(BundleEvent.STOPPED, chainTestB);
		expectedEvents[6] = new BundleEvent(BundleEvent.STOPPED, chainTestC);
		expectedEvents[7] = new BundleEvent(BundleEvent.STOPPED, chainTestD);

		installer.refreshPackages(new Bundle[] { chainTestC, chainTestD });

		Object[] actualEvents = simpleResults.getResults(8);
		compareResults(expectedEvents, actualEvents);

		chainTest.loadClass("chain.test.TestMultiChain").getDeclaredConstructor().newInstance(); //$NON-NLS-1$
		expectedEvents = new Object[4];
		expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, chainTestD);
		expectedEvents[1] = new BundleEvent(BundleEvent.STARTED, chainTestB);
		expectedEvents[2] = new BundleEvent(BundleEvent.STARTED, chainTestC);
		expectedEvents[3] = new BundleEvent(BundleEvent.STARTED, chainTestA);

		actualEvents = simpleResults.getResults(4);
		compareResults(expectedEvents, actualEvents);
	}

	@Test
	public void testMultiChainDepedencies02() throws Exception {
		Bundle chainTestA = installer.installBundle("chain.test.a"); //$NON-NLS-1$
		Bundle chainTestB = installer.installBundle("chain.test.b"); //$NON-NLS-1$
		Bundle chainTestC = installer.installBundle("chain.test.c"); //$NON-NLS-1$
		Bundle chainTestD = installer.installBundle("chain.test.d"); //$NON-NLS-1$
		syncListenerResults.getResults(0);
		installer.resolveBundles(new Bundle[] { chainTestA, chainTestB, chainTestC, chainTestD });

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

		installer.refreshPackages(new Bundle[] { chainTestC, chainTestD });

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

	@Test
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
		installer.resolveBundles(new Bundle[] { chainTest, chainTestA, chainTestB, chainTestC, chainTestD });

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
			expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED,
					OSGiTestsActivator.getContext().getBundle(0), null);
			Object[] actualFrameworkEvents = frameworkListenerResults.getResults(1);
			compareResults(expectedFrameworkEvents, actualFrameworkEvents);

			expectedEvents = new Object[14];
			int i = 0;
			expectedEvents[i++] = new BundleEvent(BundleEvent.LAZY_ACTIVATION, chainTestA);
			expectedEvents[i++] = new BundleEvent(BundleEvent.LAZY_ACTIVATION, chainTestB);
			expectedEvents[i++] = new BundleEvent(BundleEvent.LAZY_ACTIVATION, chainTestC);
			expectedEvents[i++] = new BundleEvent(BundleEvent.LAZY_ACTIVATION, chainTestD);
			expectedEvents[i++] = new BundleEvent(BundleEvent.STARTING, chainTest);
			expectedEvents[i++] = new BundleEvent(BundleEvent.STARTING, chainTestD);
			expectedEvents[i++] = new BundleEvent(BundleEvent.STARTED, chainTestD);
			expectedEvents[i++] = new BundleEvent(BundleEvent.STARTING, chainTestB);
			expectedEvents[i++] = new BundleEvent(BundleEvent.STARTED, chainTestB);
			expectedEvents[i++] = new BundleEvent(BundleEvent.STARTING, chainTestC);
			expectedEvents[i++] = new BundleEvent(BundleEvent.STARTED, chainTestC);
			expectedEvents[i++] = new BundleEvent(BundleEvent.STARTING, chainTestA);
			expectedEvents[i++] = new BundleEvent(BundleEvent.STARTED, chainTestA);
			expectedEvents[i++] = new BundleEvent(BundleEvent.STARTED, chainTest);

			actualFrameworkEvents = syncListenerResults.getResults(14);
			compareResults(expectedEvents, actualFrameworkEvents);
		} finally {
			System.getProperties().remove("test.bug300692");
			sl.setStartLevel(currentSL);
			Object[] expectedFrameworkEvents = new Object[1];
			expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED,
					OSGiTestsActivator.getContext().getBundle(0), null);
			Object[] actualFrameworkEvents = frameworkListenerResults.getResults(1);
			compareResults(expectedFrameworkEvents, actualFrameworkEvents);
		}
	}

	@Test
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
		installer.resolveBundles(new Bundle[] { chainTest, chainTestA, chainTestB, chainTestC, chainTestD });

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
			expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED,
					OSGiTestsActivator.getContext().getBundle(0), null);
			Object[] actualFrameworkEvents = frameworkListenerResults.getResults(1);
			compareResults(expectedFrameworkEvents, actualFrameworkEvents);

			expectedEvents = new Object[14];
			int i = 0;

			expectedEvents[i++] = new BundleEvent(BundleEvent.LAZY_ACTIVATION, chainTestA);
			expectedEvents[i++] = new BundleEvent(BundleEvent.LAZY_ACTIVATION, chainTestB);
			expectedEvents[i++] = new BundleEvent(BundleEvent.LAZY_ACTIVATION, chainTestC);
			expectedEvents[i++] = new BundleEvent(BundleEvent.LAZY_ACTIVATION, chainTestD);
			expectedEvents[i++] = new BundleEvent(BundleEvent.STARTING, chainTest);
			expectedEvents[i++] = new BundleEvent(BundleEvent.STARTING, chainTestD);
			expectedEvents[i++] = new BundleEvent(BundleEvent.STARTED, chainTestD);
			expectedEvents[i++] = new BundleEvent(BundleEvent.STARTING, chainTestB);
			expectedEvents[i++] = new BundleEvent(BundleEvent.STARTED, chainTestB);
			expectedEvents[i++] = new BundleEvent(BundleEvent.STARTING, chainTestC);
			expectedEvents[i++] = new BundleEvent(BundleEvent.STARTED, chainTestC);
			expectedEvents[i++] = new BundleEvent(BundleEvent.STARTING, chainTestA);
			expectedEvents[i++] = new BundleEvent(BundleEvent.STARTED, chainTestA);
			expectedEvents[i++] = new BundleEvent(BundleEvent.STARTED, chainTest);

			actualFrameworkEvents = syncListenerResults.getResults(14);
			compareResults(expectedEvents, actualFrameworkEvents);
		} finally {
			System.getProperties().remove("test.bug300692");
			System.getProperties().remove("test.bug300692.listener");
			sl.setStartLevel(currentSL);
			Object[] expectedFrameworkEvents = new Object[1];
			expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED,
					OSGiTestsActivator.getContext().getBundle(0), null);
			Object[] actualFrameworkEvents = frameworkListenerResults.getResults(1);
			compareResults(expectedFrameworkEvents, actualFrameworkEvents);
		}
	}

	@Test
	public void testBug408629() throws BundleException {
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
		installer.resolveBundles(new Bundle[] { chainTest, chainTestA, chainTestB, chainTestC, chainTestD });

		// eager start chainTestD
		chainTestD.start();

		Object[] expectedEvents1 = new Object[5];
		expectedEvents1[0] = new BundleEvent(BundleEvent.RESOLVED, chainTestD);
		expectedEvents1[1] = new BundleEvent(BundleEvent.RESOLVED, chainTestC);
		expectedEvents1[2] = new BundleEvent(BundleEvent.RESOLVED, chainTestB);
		expectedEvents1[3] = new BundleEvent(BundleEvent.RESOLVED, chainTestA);
		expectedEvents1[4] = new BundleEvent(BundleEvent.RESOLVED, chainTest);

		Object[] actualEvents = syncListenerResults.getResults(5);
		compareResults(expectedEvents1, actualEvents);
		try {
			System.setProperty("test.bug300692", "true");
			System.setProperty("test.bug300692.listener", "true");
			chainTest.start();
			sl.setStartLevel(testSL);
			Object[] expectedFrameworkEvents = new Object[1];
			expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED,
					OSGiTestsActivator.getContext().getBundle(0), null);
			Object[] actualFrameworkEvents = frameworkListenerResults.getResults(1);
			compareResults(expectedFrameworkEvents, actualFrameworkEvents);

			expectedEvents1 = new Object[14];
			Object[] expectedEvents2 = new Object[14];
			int i1 = 0;
			int i2 = 0;

			expectedEvents1[i1++] = new BundleEvent(BundleEvent.LAZY_ACTIVATION, chainTestA);
			expectedEvents2[i2++] = new BundleEvent(BundleEvent.LAZY_ACTIVATION, chainTestA);
			expectedEvents1[i1++] = new BundleEvent(BundleEvent.LAZY_ACTIVATION, chainTestB);
			expectedEvents2[i2++] = new BundleEvent(BundleEvent.LAZY_ACTIVATION, chainTestB);
			expectedEvents1[i1++] = new BundleEvent(BundleEvent.LAZY_ACTIVATION, chainTestC);
			expectedEvents2[i2++] = new BundleEvent(BundleEvent.LAZY_ACTIVATION, chainTestC);
			expectedEvents1[i1++] = new BundleEvent(BundleEvent.STARTING, chainTest);
			expectedEvents2[i2++] = new BundleEvent(BundleEvent.STARTING, chainTest);
			expectedEvents1[i1++] = new BundleEvent(BundleEvent.LAZY_ACTIVATION, chainTestD);
			expectedEvents1[i1++] = new BundleEvent(BundleEvent.STARTING, chainTestB);
			expectedEvents2[i2++] = new BundleEvent(BundleEvent.STARTING, chainTestB);
			expectedEvents1[i1++] = new BundleEvent(BundleEvent.STARTED, chainTestB);
			expectedEvents2[i2++] = new BundleEvent(BundleEvent.STARTED, chainTestB);
			expectedEvents2[i2++] = new BundleEvent(BundleEvent.LAZY_ACTIVATION, chainTestD);
			expectedEvents1[i1++] = new BundleEvent(BundleEvent.STARTING, chainTestD);
			expectedEvents2[i2++] = new BundleEvent(BundleEvent.STARTING, chainTestD);
			expectedEvents1[i1++] = new BundleEvent(BundleEvent.STARTED, chainTestD);
			expectedEvents2[i2++] = new BundleEvent(BundleEvent.STARTED, chainTestD);
			expectedEvents1[i1++] = new BundleEvent(BundleEvent.STARTING, chainTestC);
			expectedEvents2[i2++] = new BundleEvent(BundleEvent.STARTING, chainTestC);
			expectedEvents1[i1++] = new BundleEvent(BundleEvent.STARTED, chainTestC);
			expectedEvents2[i2++] = new BundleEvent(BundleEvent.STARTED, chainTestC);
			expectedEvents1[i1++] = new BundleEvent(BundleEvent.STARTING, chainTestA);
			expectedEvents2[i2++] = new BundleEvent(BundleEvent.STARTING, chainTestA);
			expectedEvents1[i1++] = new BundleEvent(BundleEvent.STARTED, chainTestA);
			expectedEvents2[i2++] = new BundleEvent(BundleEvent.STARTED, chainTestA);
			expectedEvents1[i1++] = new BundleEvent(BundleEvent.STARTED, chainTest);
			expectedEvents2[i2++] = new BundleEvent(BundleEvent.STARTED, chainTest);

			actualFrameworkEvents = syncListenerResults.getResults(14);
			try {
				compareResults(expectedEvents1, actualFrameworkEvents);
			} catch (AssertionFailedError e) {
				// try the second alternative
				compareResults(expectedEvents2, actualFrameworkEvents);
			}
		} finally {
			System.getProperties().remove("test.bug300692");
			System.getProperties().remove("test.bug300692.listener");
			sl.setStartLevel(currentSL);
			Object[] expectedFrameworkEvents = new Object[1];
			expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED,
					OSGiTestsActivator.getContext().getBundle(0), null);
			Object[] actualFrameworkEvents = frameworkListenerResults.getResults(1);
			compareResults(expectedFrameworkEvents, actualFrameworkEvents);
		}
	}

	@Test
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

	@Test
	public void testFragmentPackageAccess() throws Exception {
		Bundle hostA = installer.installBundle("fragment.test.attach.host.a"); //$NON-NLS-1$
		Bundle fragA = installer.installBundle("fragment.test.attach.frag.a"); //$NON-NLS-1$
		assertTrue("Host/Frag resolve", installer.resolveBundles(new Bundle[] { hostA, fragA })); //$NON-NLS-1$

		ITestRunner testRunner = (ITestRunner) hostA
				.loadClass("fragment.test.attach.host.a.internal.test.TestPackageAccess").getDeclaredConstructor() //$NON-NLS-1$
				.newInstance();
		testRunner.testIt();
	}

	@Test
	public void testFragmentMultiHost() throws Exception {
		Bundle hostA1 = installer.installBundle("fragment.test.attach.host.a"); //$NON-NLS-1$
		Bundle hostA2 = installer.installBundle("fragment.test.attach.host.a.v2"); //$NON-NLS-1$
		Bundle fragA = installer.installBundle("fragment.test.attach.frag.a"); //$NON-NLS-1$
		assertTrue("Host/Frag resolve", installer.resolveBundles(new Bundle[] { hostA1, hostA2, fragA })); //$NON-NLS-1$

		assertEquals("Wrong number of hosts", 2, installer.getPackageAdmin().getHosts(fragA).length); //$NON-NLS-1$
		runTestRunner(hostA1, "fragment.test.attach.host.a.internal.test.TestPackageAccess"); //$NON-NLS-1$
		runTestRunner(hostA2, "fragment.test.attach.host.a.internal.test.TestPackageAccess2"); //$NON-NLS-1$
	}

	private void runTestRunner(Bundle host, String classname) throws Exception {
		ITestRunner testRunner = (ITestRunner) host.loadClass(classname).getDeclaredConstructor().newInstance();
		testRunner.testIt();

	}

	@Test
	public void testFragmentExportPackage() throws Exception {
		Bundle hostA = installer.installBundle("fragment.test.attach.host.a"); //$NON-NLS-1$
		assertTrue("Host resolve", installer.resolveBundles(new Bundle[] { hostA })); //$NON-NLS-1$

		// make sure class loader for hostA is initialized
		hostA.loadClass("fragment.test.attach.host.a.internal.test.PackageAccessTest"); //$NON-NLS-1$

		Bundle fragB = installer.installBundle("fragment.test.attach.frag.b"); //$NON-NLS-1$
		Bundle hostARequire = installer.installBundle("fragment.test.attach.host.a.require"); //$NON-NLS-1$
		assertTrue("RequireA/Frag", installer.resolveBundles(new Bundle[] { hostARequire, fragB })); //$NON-NLS-1$

		hostARequire.loadClass("fragment.test.attach.frag.b.Test"); //$NON-NLS-1$
	}

	@Test
	public void testLegacyLazyStart() throws Exception {
		Bundle legacy = installer.installBundle("legacy.lazystart"); //$NON-NLS-1$
		Bundle legacyA = installer.installBundle("legacy.lazystart.a"); //$NON-NLS-1$
		Bundle legacyB = installer.installBundle("legacy.lazystart.b"); //$NON-NLS-1$
		Bundle legacyC = installer.installBundle("legacy.lazystart.c"); //$NON-NLS-1$
		assertTrue("legacy lazy start resolve", //$NON-NLS-1$
				installer.resolveBundles(new Bundle[] { legacy, legacyA, legacyB, legacyC }));

		((ITestRunner) legacy.loadClass("legacy.lazystart.SimpleLegacy").getDeclaredConstructor() //$NON-NLS-1$
				.newInstance()).testIt();
		Object[] expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, legacyA);
		Object[] actualEvents = simpleResults.getResults(1);
		compareResults(expectedEvents, actualEvents);

		((ITestRunner) legacy.loadClass("legacy.lazystart.TrueExceptionLegacy1").getDeclaredConstructor() //$NON-NLS-1$
				.newInstance()).testIt();
		assertTrue("exceptions no event", simpleResults.getResults(0).length == 0); //$NON-NLS-1$
		((ITestRunner) legacy.loadClass("legacy.lazystart.TrueExceptionLegacy2").getDeclaredConstructor().newInstance()) //$NON-NLS-1$
				.testIt();
		expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, legacyB);
		actualEvents = simpleResults.getResults(1);
		compareResults(expectedEvents, actualEvents);

		((ITestRunner) legacy.loadClass("legacy.lazystart.FalseExceptionLegacy1").getDeclaredConstructor() //$NON-NLS-1$
				.newInstance()).testIt();
		assertTrue("exceptions no event", simpleResults.getResults(0).length == 0); //$NON-NLS-1$
		((ITestRunner) legacy.loadClass("legacy.lazystart.FalseExceptionLegacy2").getDeclaredConstructor() //$NON-NLS-1$
				.newInstance()).testIt();
		expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, legacyC);
		actualEvents = simpleResults.getResults(1);
		compareResults(expectedEvents, actualEvents);
	}

	@Test
	public void testLegacyLoadActivation() throws Exception {
		// test that calling loadClass from a non-lazy start bundle does not activate
		// the bundle
		Bundle test = installer.installBundle("test"); //$NON-NLS-1$
		assertThrows(ClassNotFoundException.class, () -> test.loadClass("does.not.exist.Test")); //$NON-NLS-1$
		Object[] expectedEvents = new Object[0];
		Object[] actualEvents = simpleResults.getResults(0);
		compareResults(expectedEvents, actualEvents);

		// test that calling loadClass from a lazy start bundle does not activates a
		// bundle
		// This is not disabled by default (bug 503742)
		Bundle legacyA = installer.installBundle("legacy.lazystart.a"); //$NON-NLS-1$
		assertThrows(ClassNotFoundException.class, () -> legacyA.loadClass("does.not.exist.Test")); //$NON-NLS-1$
		expectedEvents = new Object[0];
		actualEvents = simpleResults.getResults(0);
		compareResults(expectedEvents, actualEvents);
		assertEquals("Wrong state for lazy bundle.", Bundle.STARTING, legacyA.getState());
	}

	@Test
	public void testOSGiLazyStart() throws Exception {
		Bundle osgi = installer.installBundle("osgi.lazystart"); //$NON-NLS-1$
		Bundle osgiA = installer.installBundle("osgi.lazystart.a"); //$NON-NLS-1$
		Bundle osgiB = installer.installBundle("osgi.lazystart.b"); //$NON-NLS-1$
		Bundle osgiC = installer.installBundle("osgi.lazystart.c"); //$NON-NLS-1$
		assertTrue("osgi lazy start resolve", installer.resolveBundles(new Bundle[] { osgi, osgiA, osgiB, osgiC })); //$NON-NLS-1$

		((ITestRunner) osgi.loadClass("osgi.lazystart.LazySimple").getDeclaredConstructor().newInstance()).testIt(); //$NON-NLS-1$
		Object[] expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, osgiA);
		Object[] actualEvents = simpleResults.getResults(1);
		compareResults(expectedEvents, actualEvents);

		((ITestRunner) osgi.loadClass("osgi.lazystart.LazyExclude1").getDeclaredConstructor().newInstance()).testIt(); //$NON-NLS-1$
		assertTrue("exceptions no event", simpleResults.getResults(0).length == 0); //$NON-NLS-1$
		((ITestRunner) osgi.loadClass("osgi.lazystart.LazyExclude2").getDeclaredConstructor().newInstance()).testIt(); //$NON-NLS-1$
		expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, osgiB);
		actualEvents = simpleResults.getResults(1);
		compareResults(expectedEvents, actualEvents);

		((ITestRunner) osgi.loadClass("osgi.lazystart.LazyInclude1").getDeclaredConstructor().newInstance()).testIt(); //$NON-NLS-1$
		assertTrue("exceptions no event", simpleResults.getResults(0).length == 0); //$NON-NLS-1$
		((ITestRunner) osgi.loadClass("osgi.lazystart.LazyInclude2").getDeclaredConstructor().newInstance()).testIt(); //$NON-NLS-1$
		expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, osgiC);
		actualEvents = simpleResults.getResults(1);
		compareResults(expectedEvents, actualEvents);
	}

	@Test
	public void testOSGiLazyStartDelay() throws Exception {
		final Bundle osgiD = installer.installBundle("osgi.lazystart.d"); //$NON-NLS-1$
		final Bundle osgiE = installer.installBundle("osgi.lazystart.e"); //$NON-NLS-1$
		assertTrue("osgi lazy start resolve", installer.resolveBundles(new Bundle[] { osgiD, osgiE })); //$NON-NLS-1$

		Thread t = new Thread(() -> {
			try {
				osgiD.loadClass("osgi.lazystart.d.DTest");
			} catch (ClassNotFoundException e) {
				// should fail here
				throw new RuntimeException(e);
			}
		}, "Starting: " + osgiD);
		t.start();

		Thread.sleep(100);
		long startTime = System.currentTimeMillis();
		osgiE.start();
		long endTime = System.currentTimeMillis() - startTime;
		assertTrue("Starting of test bundle was too short: " + endTime, endTime > 3000);
	}

	@Test
	public void testStartTransientByLoadClass() throws Exception {
		// install a bundle and set its start-level high, then crank up the framework
		// start-level. This should result in no events
		Bundle osgiA = installer.installBundle("osgi.lazystart.a"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] { osgiA });
		StartLevel startLevel = installer.getStartLevel();
		startLevel.setBundleStartLevel(osgiA, startLevel.getStartLevel() + 10);

		// test transient start by loadClass
		startLevel.setStartLevel(startLevel.getStartLevel() + 15);
		Object[] expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED,
				OSGiTestsActivator.getContext().getBundle(0), null);
		Object[] actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);

		startLevel.setStartLevel(startLevel.getStartLevel() - 15);
		expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED,
				OSGiTestsActivator.getContext().getBundle(0), null);
		actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);

		Object[] expectedEvents = new Object[0];
		Object[] actualEvents = simpleResults.getResults(0);
		compareResults(expectedEvents, actualEvents);

		// now load a class from it before the start-level is met. This should result in
		// no events
		osgiA.loadClass("osgi.lazystart.a.ATest"); //$NON-NLS-1$
		expectedEvents = new Object[0];
		actualEvents = simpleResults.getResults(0);
		compareResults(expectedEvents, actualEvents);

		startLevel.setStartLevel(startLevel.getStartLevel() + 15);
		expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED,
				OSGiTestsActivator.getContext().getBundle(0), null);
		actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);

		expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, osgiA);
		actualEvents = simpleResults.getResults(1);
		compareResults(expectedEvents, actualEvents);

		startLevel.setStartLevel(startLevel.getStartLevel() - 15);
		expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED,
				OSGiTestsActivator.getContext().getBundle(0), null);
		actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);

		expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STOPPED, osgiA);
		actualEvents = simpleResults.getResults(1);
		compareResults(expectedEvents, actualEvents);

		// now load a class while start-level is met.
		startLevel.setStartLevel(startLevel.getStartLevel() + 15);
		expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED,
				OSGiTestsActivator.getContext().getBundle(0), null);
		actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);

		osgiA.loadClass("osgi.lazystart.a.ATest"); //$NON-NLS-1$
		expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, osgiA);
		actualEvents = simpleResults.getResults(1);
		compareResults(expectedEvents, actualEvents);

		startLevel.setStartLevel(startLevel.getStartLevel() - 15);
		expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED,
				OSGiTestsActivator.getContext().getBundle(0), null);
		actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);

		expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STOPPED, osgiA);
		actualEvents = simpleResults.getResults(1);
		compareResults(expectedEvents, actualEvents);
	}

	@Test
	public void testStartTransient() throws Exception {
		// install a bundle and set its start-level high, then crank up the framework
		// start-level. This should result in no events
		Bundle osgiA = installer.installBundle("osgi.lazystart.a"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] { osgiA });
		StartLevel startLevel = installer.getStartLevel();
		startLevel.setBundleStartLevel(osgiA, startLevel.getStartLevel() + 10);

		// test transient start Bundle.start(START_TRANSIENT)
		startLevel.setStartLevel(startLevel.getStartLevel() + 15);
		Object[] expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED,
				OSGiTestsActivator.getContext().getBundle(0), null);
		Object[] actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);

		startLevel.setStartLevel(startLevel.getStartLevel() - 15);
		expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED,
				OSGiTestsActivator.getContext().getBundle(0), null);
		actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);

		Object[] expectedEvents = new Object[0];
		Object[] actualEvents = simpleResults.getResults(0);
		compareResults(expectedEvents, actualEvents);

		// now call start(START_TRANSIENT) before the start-level is met. This should
		// result in no events
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
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED,
				OSGiTestsActivator.getContext().getBundle(0), null);
		actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);

		startLevel.setStartLevel(startLevel.getStartLevel() - 15);
		expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED,
				OSGiTestsActivator.getContext().getBundle(0), null);
		actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);

		expectedEvents = new Object[0];
		actualEvents = simpleResults.getResults(0);
		compareResults(expectedEvents, actualEvents);

		// now call start(START_TRANSIENT) while start-level is met.
		startLevel.setStartLevel(startLevel.getStartLevel() + 15);
		expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED,
				OSGiTestsActivator.getContext().getBundle(0), null);
		actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);

		osgiA.start(Bundle.START_TRANSIENT);

		expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, osgiA);
		actualEvents = simpleResults.getResults(1);
		compareResults(expectedEvents, actualEvents);

		startLevel.setStartLevel(startLevel.getStartLevel() - 15);
		expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED,
				OSGiTestsActivator.getContext().getBundle(0), null);
		actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);

		expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STOPPED, osgiA);
		actualEvents = simpleResults.getResults(1);
		compareResults(expectedEvents, actualEvents);
	}

	@Test
	public void testStartResolve() throws Exception {
		// install a bundle and set its start-level high, then crank up the framework
		// start-level. This should result in no events
		Bundle test = installer.installBundle("test"); //$NON-NLS-1$
		StartLevel startLevel = installer.getStartLevel();
		startLevel.setBundleStartLevel(test, startLevel.getStartLevel() + 10);
		test.start();

		assertEquals("Wrong state", Bundle.INSTALLED, test.getState()); //$NON-NLS-1$
		startLevel.setStartLevel(startLevel.getStartLevel() + 15);
		Object[] expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED,
				OSGiTestsActivator.getContext().getBundle(0), null);
		Object[] actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);
		assertEquals("Wrong state", Bundle.ACTIVE, test.getState()); //$NON-NLS-1$
	}

	@Test
	public void testStopTransient() throws Exception {
		Bundle osgiA = installer.installBundle("osgi.lazystart.a"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] { osgiA });
		StartLevel startLevel = installer.getStartLevel();
		startLevel.setBundleStartLevel(osgiA, startLevel.getStartLevel() + 10);
		// persistently start the bundle
		osgiA.start();

		// test that the bundle is started when start-level is met
		startLevel.setStartLevel(startLevel.getStartLevel() + 15);
		Object[] expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED,
				OSGiTestsActivator.getContext().getBundle(0), null);
		Object[] actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);

		Object[] expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, osgiA);
		Object[] actualEvents = simpleResults.getResults(1);
		compareResults(expectedEvents, actualEvents);

		startLevel.setStartLevel(startLevel.getStartLevel() - 15);
		expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED,
				OSGiTestsActivator.getContext().getBundle(0), null);
		actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);

		expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STOPPED, osgiA);
		actualEvents = simpleResults.getResults(1);
		compareResults(expectedEvents, actualEvents);

		// now call stop(STOP_TRANSIENT) while the start-level is met.
		startLevel.setStartLevel(startLevel.getStartLevel() + 15);
		expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED,
				OSGiTestsActivator.getContext().getBundle(0), null);
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
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED,
				OSGiTestsActivator.getContext().getBundle(0), null);
		actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);

		// now make sure the bundle still restarts when start-level is met
		startLevel.setStartLevel(startLevel.getStartLevel() + 15);
		expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED,
				OSGiTestsActivator.getContext().getBundle(0), null);
		actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);

		expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, osgiA);
		actualEvents = simpleResults.getResults(1);
		compareResults(expectedEvents, actualEvents);

		startLevel.setStartLevel(startLevel.getStartLevel() - 15);
		expectedFrameworkEvents = new Object[1];
		expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED,
				OSGiTestsActivator.getContext().getBundle(0), null);
		actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);

		expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STOPPED, osgiA);
		actualEvents = simpleResults.getResults(1);
		compareResults(expectedEvents, actualEvents);
	}

	@Test
	public void testBug258659_01() throws Exception {
		// install a bundle
		Bundle osgiA = installer.installBundle("osgi.lazystart.a"); //$NON-NLS-1$
		SynchronousBundleListener testLoadClassListener = event -> {
			if (event.getType() == BundleEvent.LAZY_ACTIVATION)
				try {
					event.getBundle().loadClass("osgi.lazystart.a.ATest"); //$NON-NLS-1$
				} catch (ClassNotFoundException e) {
					simpleResults.addEvent(e);
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

	@Test
	public void testBug258659_02() throws Exception {
		// install a bundle
		Bundle osgiA = installer.installBundle("osgi.lazystart.a"); //$NON-NLS-1$
		osgiA.start(Bundle.START_ACTIVATION_POLICY);
		SynchronousBundleListener testLoadClassListener = event -> {
			if (event.getType() == BundleEvent.LAZY_ACTIVATION)
				try {
					event.getBundle().loadClass("osgi.lazystart.a.ATest"); //$NON-NLS-1$
				} catch (ClassNotFoundException e) {
					simpleResults.addEvent(e);
				}
		};
		OSGiTestsActivator.getContext().addBundleListener(testLoadClassListener);
		try {
			installer.refreshPackages(new Bundle[] { osgiA });
			Object[] expectedEvents = new Object[1];
			expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, osgiA);
			Object[] actualEvents = simpleResults.getResults(1);
			compareResults(expectedEvents, actualEvents);
		} finally {
			OSGiTestsActivator.getContext().removeBundleListener(testLoadClassListener);
		}
	}

	@Test
	public void testBug258659_03() throws Exception {
		// install a bundle
		Bundle test = installer.installBundle("test"); //$NON-NLS-1$
		SynchronousBundleListener testLoadClassListener = event -> {
			if (event.getType() == BundleEvent.STARTED)
				try {
					event.getBundle().stop();
				} catch (BundleException e) {
					simpleResults.addEvent(e);
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

	@Test
	public void testBug258659_04() throws Exception {
		// install a bundle
		Bundle test = installer.installBundle("test"); //$NON-NLS-1$
		test.start();
		SynchronousBundleListener testLoadClassListener = event -> {
			if (event.getType() == BundleEvent.STARTED)
				try {
					event.getBundle().stop();
				} catch (BundleException e) {
					simpleResults.addEvent(e);
				}
		};
		// clear the results from the initial start
		simpleResults.getResults(0);
		// listen for the events from refreshing
		OSGiTestsActivator.getContext().addBundleListener(testLoadClassListener);
		try {
			installer.refreshPackages(new Bundle[] { test });
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

	@Test
	public void testBug213791() throws Exception {
		// install a bundle and call start(START_ACTIVATION_POLICY) twice
		Bundle osgiA = installer.installBundle("osgi.lazystart.a"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] { osgiA });
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

	@Test
	public void testThreadLock() throws Exception {
		Bundle threadLockTest = installer.installBundle("thread.locktest"); //$NON-NLS-1$
		threadLockTest.loadClass("thread.locktest.ATest"); //$NON-NLS-1$

		Object[] expectedEvents = new Object[2];
		expectedEvents[0] = Long.valueOf(5000);
		expectedEvents[1] = new BundleEvent(BundleEvent.STARTED, threadLockTest);
		Object[] actualEvents = simpleResults.getResults(2);
		compareResults(expectedEvents, actualEvents);

	}

	@Test
	public void testURLsBug164077() throws Exception {
		Bundle test = installer.installBundle("test"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] { test });
		URL[] urls = new URL[2];
		urls[0] = test.getResource("a/b/c/d"); //$NON-NLS-1$
		urls[1] = test.getEntry("a/b/c/d"); //$NON-NLS-1$
		assertNotNull("resource", urls[0]); //$NON-NLS-1$
		assertNotNull("entry", urls[1]); //$NON-NLS-1$
		for (URL url : urls) {
			URL testURL = new URL(url, "g"); //$NON-NLS-1$
			assertEquals("g", "/a/b/c/g", testURL.getPath()); //$NON-NLS-1$ //$NON-NLS-2$
			testURL = new URL(url, "./g"); //$NON-NLS-1$
			assertEquals("./g", "/a/b/c/g", testURL.getPath()); //$NON-NLS-1$ //$NON-NLS-2$
			testURL = new URL(url, "g/"); //$NON-NLS-1$
			assertEquals("g/", "/a/b/c/g/", testURL.getPath()); //$NON-NLS-1$ //$NON-NLS-2$
			testURL = new URL(url, "/g"); //$NON-NLS-1$
			assertEquals("/g", "/g", testURL.getPath()); //$NON-NLS-1$ //$NON-NLS-2$
			testURL = new URL(url, "?y"); //$NON-NLS-1$
			assertEquals("?y", "/a/b/c/?y", testURL.getPath()); //$NON-NLS-1$ //$NON-NLS-2$
			testURL = new URL(url, "g?y"); //$NON-NLS-1$
			assertEquals("g?y", "/a/b/c/g?y", testURL.getPath()); //$NON-NLS-1$ //$NON-NLS-2$
			testURL = new URL(url, "g#s"); //$NON-NLS-1$
			assertEquals("g#s", "/a/b/c/g#s", testURL.getPath() + "#s"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			testURL = new URL(url, "g?y#s"); //$NON-NLS-1$
			assertEquals("g?y#s", "/a/b/c/g?y#s", testURL.getPath() + "#s"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			testURL = new URL(url, ";x"); //$NON-NLS-1$
			assertEquals(";x", "/a/b/c/;x", testURL.getPath()); //$NON-NLS-1$ //$NON-NLS-2$
			testURL = new URL(url, "g;x"); //$NON-NLS-1$
			assertEquals("g;x", "/a/b/c/g;x", testURL.getPath()); //$NON-NLS-1$ //$NON-NLS-2$
			testURL = new URL(url, "g;x?y#s"); //$NON-NLS-1$
			assertEquals("g;x?y#s", "/a/b/c/g;x?y#s", testURL.getPath() + "#s"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			testURL = new URL(url, "."); //$NON-NLS-1$
			assertEquals(".", "/a/b/c/", testURL.getPath()); //$NON-NLS-1$ //$NON-NLS-2$
			testURL = new URL(url, "./"); //$NON-NLS-1$
			assertEquals("./", "/a/b/c/", testURL.getPath()); //$NON-NLS-1$ //$NON-NLS-2$
			testURL = new URL(url, ".."); //$NON-NLS-1$
			assertEquals("..", "/a/b/", testURL.getPath()); //$NON-NLS-1$ //$NON-NLS-2$
			testURL = new URL(url, "../"); //$NON-NLS-1$
			assertEquals("../", "/a/b/", testURL.getPath()); //$NON-NLS-1$ //$NON-NLS-2$
			testURL = new URL(url, "../g"); //$NON-NLS-1$
			assertEquals("../g", "/a/b/g", testURL.getPath()); //$NON-NLS-1$ //$NON-NLS-2$
			testURL = new URL(url, "../.."); //$NON-NLS-1$
			assertEquals("../..", "/a/", testURL.getPath()); //$NON-NLS-1$ //$NON-NLS-2$
			testURL = new URL(url, "../../"); //$NON-NLS-1$
			assertEquals("../../", "/a/", testURL.getPath()); //$NON-NLS-1$ //$NON-NLS-2$
			testURL = new URL(url, "../../g"); //$NON-NLS-1$
			assertEquals("../../g", "/a/g", testURL.getPath()); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	@Test
	public void testEntryURLEqualsHashCode() throws Exception {
		Bundle test = installer.installBundle("test"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] { test });
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

	@Test
	public void testResourceURLEqualsHashCode() throws Exception {
		Bundle test = installer.installBundle("test"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] { test });
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

	@Test
	public void testGetEntryDir01() throws BundleException {
		Bundle test = installer.installBundle("test"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] { test });
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
		if (dResource != null) // note that File bundles will return non-null whilc jar'ed bundles will return
								// null
			assertFalse(dResource.toExternalForm(), dResource.getFile().endsWith("/")); //$NON-NLS-1$

	}

	@Test
	public void testGetResourceDir01() throws BundleException {
		Bundle test = installer.installBundle("test"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] { test });
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
		if (dResource != null) // note that File bundles will return non-null whilc jar'ed bundles will return
								// null
			assertFalse(dResource.toExternalForm(), dResource.getFile().endsWith("/")); //$NON-NLS-1$

	}

	@Test
	public void testBootGetResources01() throws Exception {
		if (System.getProperty(Constants.FRAMEWORK_BOOTDELEGATION) != null)
			return; // cannot really test this if this property is set
		// make sure there is only one manifest found
		Bundle test = installer.installBundle("test"); //$NON-NLS-1$
		Enumeration<URL> manifests = test.getResources("META-INF/MANIFEST.MF"); //$NON-NLS-1$
		assertNotNull("manifests", manifests); //$NON-NLS-1$
		List<URL> manifestURLs = Collections.list(manifests);
		assertEquals("manifest number", 1, manifestURLs.size()); //$NON-NLS-1$
		URL manifest = manifestURLs.get(0);
		int dotIndex = manifest.getHost().indexOf('.');
		long bundleId = dotIndex >= 0 && dotIndex < manifest.getHost().length() - 1
				? Long.parseLong(manifest.getHost().substring(0, dotIndex))
				: Long.parseLong(manifest.getHost());
		assertEquals("host id", test.getBundleId(), bundleId); //$NON-NLS-1$
	}

	@Test
	public void testBootGetResources02() throws Exception {
		// properly test bug 375783 when used as a parent class loader
		// This will fail on the IBM VM (see bug 409314)
		if (System.getProperty(Constants.FRAMEWORK_BOOTDELEGATION) != null)
			return; // cannot really test this if this property is set
		Bundle test = installer.installBundle("test"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] { test });
		BundleWiring wiring = test.adapt(BundleWiring.class);
		ClassLoader bcl = wiring.getClassLoader();
		URLClassLoader cl = new URLClassLoader(new URL[0], bcl);
		Enumeration<URL> manifests = cl.getResources("META-INF/MANIFEST.MF"); //$NON-NLS-1$
		assertNotNull("manifests", manifests); //$NON-NLS-1$
		List<URL> manifestURLs = Collections.list(manifests);
		assertEquals("manifest number", 1, manifestURLs.size()); //$NON-NLS-1$
		URL manifest = manifestURLs.get(0);
		assertEquals("wrong protocol", "bundleresource", manifest.getProtocol());
		int dotIndex = manifest.getHost().indexOf('.');
		long bundleId = dotIndex >= 0 && dotIndex < manifest.getHost().length() - 1
				? Long.parseLong(manifest.getHost().substring(0, dotIndex))
				: Long.parseLong(manifest.getHost());
		assertEquals("host id", test.getBundleId(), bundleId); //$NON-NLS-1$
		cl.close();
	}

	@Test
	public void testMultipleGetResources01() throws Exception {
		Bundle test = installer.installBundle("test"); //$NON-NLS-1$
		// test that we can get multiple resources from a bundle
		Enumeration<URL> resources = test.getResources("data/resource1"); //$NON-NLS-1$
		assertNotNull("resources", resources); //$NON-NLS-1$
		List<URL> resourceURLs = Collections.list(resources);
		assertEquals("resource number", 2, resourceURLs.size()); //$NON-NLS-1$
		assertEquals("root resource", "root classpath", readURL(resourceURLs.get(0))); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("stuff resource", "stuff classpath", readURL(resourceURLs.get(1))); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testMultipleGetResources01a() throws Exception {
		Bundle test = installer.installBundle("test"); //$NON-NLS-1$
		// test that we can get multiple resources from a bundle
		// and use that bundle's class loader as a parent
		installer.resolveBundles(new Bundle[] { test });
		BundleWiring wiring = test.adapt(BundleWiring.class);
		ClassLoader bcl = wiring.getClassLoader();
		URLClassLoader cl = new URLClassLoader(new URL[0], bcl);
		Enumeration<URL> resources = cl.getResources("data/resource1"); //$NON-NLS-1$
		assertNotNull("resources", resources); //$NON-NLS-1$
		List<URL> resourceURLs = Collections.list(resources);
		assertEquals("resource number", 2, resourceURLs.size()); //$NON-NLS-1$
		assertEquals("root resource", "root classpath", readURL(resourceURLs.get(0))); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("stuff resource", "stuff classpath", readURL(resourceURLs.get(1))); //$NON-NLS-1$ //$NON-NLS-2$
		cl.close();
	}

	@Test
	public void testMultipleGetResources02() throws Exception {
		installer.installBundle("test"); //$NON-NLS-1$
		Bundle test2 = installer.installBundle("test2"); //$NON-NLS-1$
		// test that we can get multiple resources from a bundle
		Enumeration<URL> resources = test2.getResources("data/resource1"); //$NON-NLS-1$
		assertNotNull("resources", resources); //$NON-NLS-1$
		List<URL> resourceURLs = Collections.list(resources);
		assertEquals("resource number", 4, resourceURLs.size()); //$NON-NLS-1$
		assertEquals("root resource", "root classpath", readURL(resourceURLs.get(0))); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("stuff resource", "stuff classpath", readURL(resourceURLs.get(1))); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("root resource", "root classpath test2", readURL(resourceURLs.get(2))); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("stuff resource", "stuff classpath test2", readURL(resourceURLs.get(3))); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testMultipleGetResources03() throws Exception {
		installer.installBundle("test"); //$NON-NLS-1$
		Bundle test2 = installer.installBundle("test2"); //$NON-NLS-1$
		// test that we can get multiple resources from a bundle
		// test that using a context gives correct results for multiple resources (bug
		// 261853)
		Enumeration<URL> resources = test2.getResources("data/"); //$NON-NLS-1$
		assertNotNull("resources", resources); //$NON-NLS-1$
		List<URL> resourceURLs = Collections.list(resources);
		assertEquals("resource number", 4, resourceURLs.size()); //$NON-NLS-1$
		assertEquals("root resource", "root classpath", readURL(new URL(resourceURLs.get(0), "resource1"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals("stuff resource", "stuff classpath", readURL(new URL(resourceURLs.get(1), "resource1"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals("root resource", "root classpath test2", readURL(new URL(resourceURLs.get(2), "resource1"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals("stuff resource", "stuff classpath test2", readURL(new URL(resourceURLs.get(3), "resource1"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	@Test
	public void testListResources() throws BundleException {
		installer.installBundle("test"); //$NON-NLS-1$
		Bundle test2 = installer.installBundle("test2"); //$NON-NLS-1$

		assertTrue("Could not resolve test2 bundle", installer.resolveBundles(new Bundle[] { test2 }));
		BundleWiring test2Wiring = test2.adapt(BundleWiring.class);
		Collection<String> resources = test2Wiring.listResources("/", "*", 0);
		assertTrue("could not find resource", resources.contains("resource2"));
		resources = test2Wiring.listResources("data/", "resource2", 0);
		assertTrue("could not find resource", resources.contains("data/resource2"));
		resources = test2Wiring.listResources("/", "resource*", BundleWiring.LISTRESOURCES_RECURSE);
		assertTrue("could not find resource", resources.contains("data/resource2"));
	}

	@Test
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

	@Test
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

	@Test
	public void testURLExternalFormat03() throws Exception {
		Bundle test = installer.installBundle("test"); //$NON-NLS-1$
		// test the external format of bundle resource URLs
		URL entry = test.getEntry("data/resource1"); //$NON-NLS-1$
		assertNotNull("entry", entry); //$NON-NLS-1$
		URI uri1 = new URI(entry.getProtocol(), null, entry.getHost(), entry.getPort(), entry.getPath(), null,
				entry.getQuery());
		URI uri2 = new URI(entry.getProtocol(), entry.getHost(), entry.getPath(), entry.getQuery());
		URI uri3 = new URI(entry.toExternalForm());

		URL url1 = uri1.toURL();
		URL url2 = uri2.toURL();
		URL url3 = uri3.toURL();
		URL url4 = new URL(uri1.toString());
		checkURL("root classpath", entry, url1); //$NON-NLS-1$
		checkURL("root classpath", entry, url2); //$NON-NLS-1$
		checkURL("root classpath", entry, url3); //$NON-NLS-1$
		checkURL("root classpath", entry, url4); //$NON-NLS-1$
	}

	@Test
	public void testURLExternalFormat04() throws Exception {
		Bundle test = installer.installBundle("test"); //$NON-NLS-1$
		// test the external format of bundle resource URLs
		URL entry = test.getResource("data/resource1"); //$NON-NLS-1$
		assertNotNull("entry", entry); //$NON-NLS-1$
		URI uri1 = new URI(entry.getProtocol(), null, entry.getHost(), entry.getPort(), entry.getPath(), null,
				entry.getQuery());
		URI uri2 = new URI(entry.getProtocol(), entry.getHost(), entry.getPath(), entry.getQuery());
		URI uri3 = new URI(entry.toExternalForm());
		URL url1 = uri1.toURL();
		URL url2 = uri2.toURL();
		URL url3 = uri3.toURL();
		URL url4 = new URL(uri1.toString());
		checkURL("root classpath", entry, url1); //$NON-NLS-1$
		checkURL("root classpath", entry, url2); //$NON-NLS-1$
		checkURL("root classpath", entry, url3); //$NON-NLS-1$
		checkURL("root classpath", entry, url4); //$NON-NLS-1$
	}

	private void checkURL(String content, URL orig, URL copy) throws Exception {
		assertEquals("external format", orig.toExternalForm(), copy.toExternalForm()); //$NON-NLS-1$
		assertEquals(content, content, readURL(copy));
	}

	@Test
	public void testURI() throws URISyntaxException {
		new URI("bundleentry", "1", "/test", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	@Test
	public void testMultipleExportFragments01() throws Exception {
		Bundle host = installer.installBundle("host.multiple.exports"); //$NON-NLS-1$
		Bundle frag = installer.installBundle("frag.multiple.exports"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] { host, frag });
		PackageAdmin packageAdmin = installer.getPackageAdmin();
		ExportedPackage[] hostExports = packageAdmin.getExportedPackages(host);
		assertEquals("Number host exports", 4, hostExports == null ? 0 : hostExports.length); //$NON-NLS-1$

		BundleWiring hostWiring = host.adapt(BundleWiring.class);
		assertNotNull("No host wiring", hostWiring);

		List<BundleCapability> packageCapabilities = hostWiring.getCapabilities(PackageNamespace.PACKAGE_NAMESPACE);
		assertEquals("Number host export capabilities", 4, packageCapabilities.size()); //$NON-NLS-1$

		assertEquals("Check export name", "host.multiple.exports", //$NON-NLS-1$//$NON-NLS-2$
				packageCapabilities.get(0).getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
		assertEquals("Check include directive", "Public*", //$NON-NLS-1$//$NON-NLS-2$
				packageCapabilities.get(0).getDirectives().get(PackageNamespace.CAPABILITY_INCLUDE_DIRECTIVE));

		assertEquals("Check export name", "host.multiple.exports.onlyone", //$NON-NLS-1$ //$NON-NLS-2$
				packageCapabilities.get(1).getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));

		assertEquals("Check export name", "host.multiple.exports", //$NON-NLS-1$//$NON-NLS-2$
				packageCapabilities.get(2).getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
		assertEquals("Check scope attribute", "private", //$NON-NLS-1$//$NON-NLS-2$
				packageCapabilities.get(2).getAttributes().get("scope"));

		assertEquals("Check export name", "host.multiple.exports.onlyone", //$NON-NLS-1$ //$NON-NLS-2$
				packageCapabilities.get(3).getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
	}

	@Test
	public void testMultipleExportFragments02() throws Exception {
		Bundle host = installer.installBundle("host.multiple.exports"); //$NON-NLS-1$
		Bundle frag = installer.installBundle("frag.multiple.exports"); //$NON-NLS-1$
		Bundle client1 = installer.installBundle("client1.multiple.exports"); //$NON-NLS-1$

		installer.resolveBundles(new Bundle[] { host, frag, client1 });
		client1.start();
		client1.stop();
		Object[] expectedEvents = new Object[4];
		expectedEvents[0] = "host.multiple.exports.PublicClass1"; //$NON-NLS-1$
		expectedEvents[1] = "host.multiple.exports.PublicClass2"; //$NON-NLS-1$
		expectedEvents[2] = "success"; //$NON-NLS-1$
		expectedEvents[3] = "success"; //$NON-NLS-1$
		Object[] actualEvents = simpleResults.getResults(4);
		assertArrayEquals(expectedEvents, actualEvents);
	}

	@Test
	public void testMultipleExportFragments03() throws Exception {
		Bundle host = installer.installBundle("host.multiple.exports"); //$NON-NLS-1$
		Bundle frag = installer.installBundle("frag.multiple.exports"); //$NON-NLS-1$
		Bundle client2 = installer.installBundle("client2.multiple.exports"); //$NON-NLS-1$

		installer.resolveBundles(new Bundle[] { host, frag, client2 });
		client2.start();
		client2.stop();
		Object[] expectedEvents = new Object[4];
		expectedEvents[0] = "host.multiple.exports.PublicClass1"; //$NON-NLS-1$
		expectedEvents[1] = "host.multiple.exports.PublicClass2"; //$NON-NLS-1$
		expectedEvents[2] = "host.multiple.exports.PrivateClass1"; //$NON-NLS-1$
		expectedEvents[3] = "host.multiple.exports.PrivateClass2"; //$NON-NLS-1$
		Object[] actualEvents = simpleResults.getResults(4);
		assertArrayEquals(expectedEvents, actualEvents);
	}

	@Test
	@Ignore
	public void testXFriends() throws Exception {
		// TODO this will fail since we don't have strict mode in the new framework
		try {
			Bundle test1 = installer.installBundle("xfriends.test1"); //$NON-NLS-1$
			Bundle test2 = installer.installBundle("xfriends.test2"); //$NON-NLS-1$
			Bundle test3 = installer.installBundle("xfriends.test3"); //$NON-NLS-1$
			installer.resolveBundles(new Bundle[] { test1, test2, test3 });
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

	@Test
	public void testImporterExporter01() throws BundleException {
		Bundle importerExporter1 = installer.installBundle("exporter.importer1"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] { importerExporter1 });
		PackageAdmin pa = installer.getPackageAdmin();
		ExportedPackage[] origExportedPackages = pa.getExportedPackages("exporter.importer.test"); //$NON-NLS-1$
		assertNotNull("No exporter.importer.test found", origExportedPackages); //$NON-NLS-1$
		assertEquals("Wrong number of exports", 1, origExportedPackages.length); //$NON-NLS-1$
		Bundle exporter = origExportedPackages[0].getExportingBundle();
		assertEquals("Wrong exporter", importerExporter1, exporter); //$NON-NLS-1$
		// TODO need to get clarification from OSGi on what is returned by
		// getImportingBundles when there is no importers
		Bundle[] origImporters = origExportedPackages[0].getImportingBundles();
		assertTrue("Should have no importers", origImporters == null || origImporters.length == 0); //$NON-NLS-1$

		// install another importer/exporter. This bundle should wire to the original
		// exporter
		Bundle importerExporter2 = installer.installBundle("exporter.importer2"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] { importerExporter2 });

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

	@Test
	public void testImporterExporter02() throws BundleException {
		Bundle importerExporter3 = installer.installBundle("exporter.importer3"); //$NON-NLS-1$
		Bundle importerExporter4 = installer.installBundle("exporter.importer4"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] { importerExporter3, importerExporter4 });

		importerExporter3.start();
		importerExporter3.stop();
		importerExporter3.update();
		importerExporter3.start();
	}

	@Test
	public void testUninstallInUse01() throws BundleException {
		if (getContext().getServiceReference("org.eclipse.equinox.region.RegionDigraph") != null) {
			System.out.println("Cannot test uninstall in use with RegionDigraph service");
			return;
		}
		Bundle exporter1 = installer.installBundle("exporter.importer1"); //$NON-NLS-1$
		BundleRevision iExporter1 = exporter1.adapt(BundleRevision.class);
		Bundle exporter2 = installer.installBundle("exporter.importer2"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] { exporter1, exporter2 });
		exporter1.uninstall();
		Bundle importer = installer.installBundle("exporter.importer4"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] { importer });
		BundleWiring importerWiring = importer.adapt(BundleWiring.class);
		assertNotNull("Bundle b has no wiring.", importerWiring);
		List<BundleWire> bImports = importerWiring.getRequiredWires(PackageNamespace.PACKAGE_NAMESPACE);
		assertEquals("Wrong number of imported packages.", 1, bImports.size());
		assertEquals("Wrong exporter.", iExporter1, bImports.get(0).getProvider());
	}

	@Test
	public void testBug207847() throws BundleException {
		Bundle test = installer.installBundle("test"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] { test });
		test.start();

		Bundle frag1 = installer.installBundle("test.fragment1"); //$NON-NLS-1$
		Bundle frag2 = installer.installBundle("test.fragment2"); //$NON-NLS-1$
		Bundle frag3 = installer.installBundle("test.fragment3"); //$NON-NLS-1$
		Bundle frag4 = installer.installBundle("test.fragment4"); //$NON-NLS-1$
		Bundle frag5 = installer.installBundle("test.fragment5"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] { frag1, frag2, frag3, frag4, frag5 });

		assertTrue("host is not resolved", (test.getState() & Bundle.ACTIVE) != 0); //$NON-NLS-1$
		assertTrue("frag1 is not resolved", (frag1.getState() & Bundle.RESOLVED) != 0); //$NON-NLS-1$
		assertTrue("frag2 is not resolved", (frag2.getState() & Bundle.RESOLVED) != 0); //$NON-NLS-1$
		assertTrue("frag3 is not resolved", (frag3.getState() & Bundle.RESOLVED) != 0); //$NON-NLS-1$
		assertTrue("frag4 is not resolved", (frag4.getState() & Bundle.RESOLVED) != 0); //$NON-NLS-1$
		assertTrue("frag5 is not resolved", (frag5.getState() & Bundle.RESOLVED) != 0); //$NON-NLS-1$
	}

	@Test
	public void testBug235958() throws BundleException {
		Bundle testX = installer.installBundle("test.bug235958.x"); //$NON-NLS-1$
		Bundle testY = installer.installBundle("test.bug235958.y"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] { testX, testY });
		testX.start();
	}

	@Test
	public void testBuddyClassLoadingRegistered1() throws Exception {
		Bundle registeredA = installer.installBundle("buddy.registered.a"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] { registeredA });
		Enumeration<URL> testFiles = registeredA.getResources("resources/test.txt"); //$NON-NLS-1$
		assertNotNull("testFiles", testFiles); //$NON-NLS-1$
		ArrayList<String> texts = new ArrayList<>();
		while (testFiles.hasMoreElements())
			texts.add(readURL(testFiles.nextElement()));
		assertEquals("test.txt number", 1, texts.size()); //$NON-NLS-1$
		assertTrue("buddy.registered.a", texts.contains("buddy.registered.a")); //$NON-NLS-1$ //$NON-NLS-2$

		Bundle registeredATest1 = installer.installBundle("buddy.registered.a.test1"); //$NON-NLS-1$
		Bundle registeredATest2 = installer.installBundle("buddy.registered.a.test2"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] { registeredATest1, registeredATest2 });
		testFiles = registeredA.getResources("resources/test.txt"); //$NON-NLS-1$
		assertNotNull("testFiles", testFiles); //$NON-NLS-1$
		texts = new ArrayList<>();
		while (testFiles.hasMoreElements())
			texts.add(readURL(testFiles.nextElement()));

		// The real test
		assertEquals("test.txt number", 3, texts.size()); //$NON-NLS-1$
		assertTrue("buddy.registered.a", texts.contains("buddy.registered.a")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("buddy.registered.a.test1", texts.contains("buddy.registered.a.test1")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("buddy.registered.a.test2", texts.contains("buddy.registered.a.test2")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testBuddyClassLoadingRegistered2() throws Exception {
		Bundle registeredA = installer.installBundle("buddy.registered.a"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] { registeredA });
		URL testFile = registeredA.getResource("resources/test1.txt"); //$NON-NLS-1$
		assertNull("test1.txt", testFile); //$NON-NLS-1$

		testFile = registeredA.getResource("resources/test2.txt"); //$NON-NLS-1$
		assertNull("test2.txt", testFile); //$NON-NLS-1$

		Bundle registeredATest1 = installer.installBundle("buddy.registered.a.test1"); //$NON-NLS-1$
		Bundle registeredATest2 = installer.installBundle("buddy.registered.a.test2"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] { registeredATest1, registeredATest2 });

		testFile = registeredA.getResource("resources/test1.txt"); //$NON-NLS-1$
		assertNotNull("test1.txt", testFile); //$NON-NLS-1$
		assertEquals("buddy.registered.a.test1", "buddy.registered.a.test1", readURL(testFile)); //$NON-NLS-1$ //$NON-NLS-2$

		testFile = registeredA.getResource("resources/test2.txt"); //$NON-NLS-1$
		assertNotNull("test2.txt", testFile); //$NON-NLS-1$
		assertEquals("buddy.registered.a.test2", "buddy.registered.a.test2", readURL(testFile)); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testBuddyClassLoadingRegistered3() throws Exception {
		Bundle registeredA = installer.installBundle("buddy.registered.a"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] { registeredA });

		assertThrows(ClassNotFoundException.class, () -> registeredA.loadClass("buddy.registered.a.test1.ATest"));
		assertThrows(ClassNotFoundException.class, () -> registeredA.loadClass("buddy.registered.a.test2.ATest"));

		Bundle registeredATest1 = installer.installBundle("buddy.registered.a.test1"); //$NON-NLS-1$
		Bundle registeredATest2 = installer.installBundle("buddy.registered.a.test2"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] { registeredATest1, registeredATest2 });

		assertNotNull("Class buddy.registered.a.test1.ATest", registeredA.loadClass("buddy.registered.a.test1.ATest"));
		assertNotNull("Class buddy.registered.a.test2.ATest", registeredA.loadClass("buddy.registered.a.test2.ATest"));
	}

	@Test
	public void testBuddyClassLoadingRegisteredListResources() throws Exception {
		Bundle registeredA = installer.installBundle("buddy.registered.a");
		installer.resolveBundles(new Bundle[] { registeredA });

		Bundle registeredATest1 = installer.installBundle("buddy.registered.a.test1");
		Bundle registeredATest2 = installer.installBundle("buddy.registered.a.test2");
		installer.resolveBundles(new Bundle[] { registeredATest1, registeredATest2 });

		Collection<String> result = registeredA.adapt(BundleWiring.class).listResources("resources", "*",
				BundleWiring.LISTRESOURCES_RECURSE);
		assertEquals("Wrong number of resources listed: " + result, 3, result.size());
		assertTrue("resources/test.txt", result.contains("resources/test.txt"));
		assertTrue("resources/test1.txt", result.contains("resources/test1.txt"));
		assertTrue("resources/test2.txt", result.contains("resources/test2.txt"));

		result = registeredA.adapt(BundleWiring.class).listResources("resources", "*",
				BundleWiring.LISTRESOURCES_LOCAL | BundleWiring.LISTRESOURCES_RECURSE);
		assertEquals("Wrong number of resources listed: " + result, 1, result.size());
		assertTrue("resources/test.txt", result.contains("resources/test.txt"));

		result = registeredA.adapt(BundleWiring.class).listResources("buddy/registered/a", "*.class",
				BundleWiring.LISTRESOURCES_RECURSE);
		assertEquals("Wrong number of resources listed: " + result, 2, result.size());
		assertTrue("buddy/registered/a/test1/ATest.class", result.contains("buddy/registered/a/test1/ATest.class"));
		assertTrue("buddy/registered/a/test2/ATest.class", result.contains("buddy/registered/a/test2/ATest.class"));

		result = registeredA.adapt(BundleWiring.class).listResources("", this.getClass().getSimpleName() + ".class",
				BundleWiring.LISTRESOURCES_RECURSE);
		assertEquals("Wrong number of resources listed: " + result, 1, result.size());
	}

	@Test
	public void testBuddyClassLoadingDependentListResources() throws Exception {
		Bundle registeredA = installer.installBundle("buddy.dependent.a");
		installer.resolveBundles(new Bundle[] { registeredA });

		Bundle registeredATest1 = installer.installBundle("buddy.dependent.a.test1");
		Bundle registeredATest2 = installer.installBundle("buddy.dependent.a.test2");
		installer.resolveBundles(new Bundle[] { registeredATest1, registeredATest2 });

		Collection<String> result = registeredA.adapt(BundleWiring.class).listResources("resources", "*",
				BundleWiring.LISTRESOURCES_RECURSE);
		assertEquals("Wrong number of resources listed: " + result, 3, result.size());
		assertTrue("resources/test.txt", result.contains("resources/test.txt"));
		assertTrue("resources/test1.txt", result.contains("resources/test1.txt"));
		assertTrue("resources/test2.txt", result.contains("resources/test2.txt"));

		result = registeredA.adapt(BundleWiring.class).listResources("resources", "*",
				BundleWiring.LISTRESOURCES_LOCAL | BundleWiring.LISTRESOURCES_RECURSE);
		assertEquals("Wrong number of resources listed: " + result, 1, result.size());
		assertTrue("resources/test.txt", result.contains("resources/test.txt"));

		result = registeredA.adapt(BundleWiring.class).listResources("buddy/dependent/a", "*.class",
				BundleWiring.LISTRESOURCES_RECURSE);
		assertEquals("Wrong number of resources listed: " + result, 2, result.size());
		assertTrue("buddy/dependent/a/test1/ATest.class", result.contains("buddy/dependent/a/test1/ATest.class"));
		assertTrue("buddy/dependent/a/test2/ATest.class", result.contains("buddy/dependent/a/test2/ATest.class"));

		result = registeredA.adapt(BundleWiring.class).listResources("", this.getClass().getSimpleName() + ".class",
				BundleWiring.LISTRESOURCES_RECURSE);
		assertEquals("Wrong number of resources listed: " + result, 1, result.size());
	}

	@Test
	public void testBuddyClassLoadingDependent1() throws Exception {
		Bundle dependentA = installer.installBundle("buddy.dependent.a"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] { dependentA });
		Enumeration<URL> testFiles = dependentA.getResources("resources/test.txt"); //$NON-NLS-1$
		assertNotNull("testFiles", testFiles); //$NON-NLS-1$
		ArrayList<String> texts = new ArrayList<>();
		while (testFiles.hasMoreElements())
			texts.add(readURL(testFiles.nextElement()));
		assertEquals("test.txt number", 1, texts.size()); //$NON-NLS-1$
		assertTrue("buddy.dependent.a", texts.contains("buddy.dependent.a")); //$NON-NLS-1$ //$NON-NLS-2$

		Bundle dependentATest1 = installer.installBundle("buddy.dependent.a.test1"); //$NON-NLS-1$
		Bundle dependentATest2 = installer.installBundle("buddy.dependent.a.test2"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] { dependentATest1, dependentATest2 });
		testFiles = dependentA.getResources("resources/test.txt"); //$NON-NLS-1$
		assertNotNull("testFiles", testFiles); //$NON-NLS-1$
		texts = new ArrayList<>();
		while (testFiles.hasMoreElements())
			texts.add(readURL(testFiles.nextElement()));
		assertEquals("test.txt number", 3, texts.size()); //$NON-NLS-1$
		assertTrue("buddy.dependent.a", texts.contains("buddy.dependent.a")); //$NON-NLS-1$//$NON-NLS-2$
		assertTrue("buddy.dependent.a.test1", texts.contains("buddy.dependent.a.test1")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("buddy.dependent.a.test2", texts.contains("buddy.dependent.a.test2")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testBuddyClassLoadingDependent2() throws Exception {
		Bundle dependentA = installer.installBundle("buddy.dependent.a"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] { dependentA });
		URL testFile = dependentA.getResource("resources/test1.txt"); //$NON-NLS-1$
		assertNull("test1.txt", testFile); //$NON-NLS-1$

		testFile = dependentA.getResource("resources/test2.txt"); //$NON-NLS-1$
		assertNull("test2.txt", testFile); //$NON-NLS-1$

		Bundle dependentATest1 = installer.installBundle("buddy.dependent.a.test1"); //$NON-NLS-1$
		Bundle dependentATest2 = installer.installBundle("buddy.dependent.a.test2"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] { dependentATest1, dependentATest2 });

		testFile = dependentA.getResource("resources/test1.txt"); //$NON-NLS-1$
		assertNotNull("test1.txt", testFile); //$NON-NLS-1$
		assertEquals("buddy.dependent.a.test1", "buddy.dependent.a.test1", readURL(testFile)); //$NON-NLS-1$ //$NON-NLS-2$

		testFile = dependentA.getResource("resources/test2.txt"); //$NON-NLS-1$
		assertNotNull("test2.txt", testFile); //$NON-NLS-1$
		assertEquals("buddy.dependent.a.test2", "buddy.dependent.a.test2", readURL(testFile)); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testBuddyClassLoadingDependent3() throws Exception {
		Bundle dependentA = installer.installBundle("buddy.dependent.a"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] { dependentA });

		assertThrows(ClassNotFoundException.class, () -> dependentA.loadClass("buddy.dependent.a.test1.ATest"));
		assertThrows(ClassNotFoundException.class, () -> dependentA.loadClass("buddy.dependent.a.test2.ATest"));

		Bundle dependentATest1 = installer.installBundle("buddy.dependent.a.test1"); //$NON-NLS-1$
		Bundle dependentATest2 = installer.installBundle("buddy.dependent.a.test2"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] { dependentATest1, dependentATest2 });

		assertNotNull("Class buddy.dependent.a.test1.ATest", dependentA.loadClass("buddy.dependent.a.test1.ATest"));
		assertNotNull("Class buddy.dependent.a.test2.ATest", dependentA.loadClass("buddy.dependent.a.test2.ATest"));
	}

	@Test
	public void testBuddyClassLoadingInvalid() throws Exception {
		Bundle invalidA = installer.installBundle("buddy.invalid.a"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] { invalidA });
		invalidA.getResource("doesNotExist");
		assertThrows(ClassNotFoundException.class, () -> invalidA.loadClass("does.not.Exist")); //$NON-NLS-1$

	}

	@Test
	public void testBuddyClassloadingBug438904() throws Exception {
		Bundle host = installer.installBundle("test.bug438904.host");
		Bundle frag = installer.installBundle("test.bug438904.frag");
		Bundle global = installer.installBundle("test.bug438904.global");
		installer.resolveBundles(new Bundle[] { host, frag, global });
		global.loadClass("test.bug438904.host.Test1");
		global.loadClass("test.bug438904.frag.Test2");
	}

	@Test
	public void testBuddyClassLoadingGlobalNotFound() throws Exception {
		Bundle global = installer.installBundle("test.bug438904.global");

		assertThrows(ClassNotFoundException.class, () -> global.loadClass("does.not.exist.Test"));
		assertNull("Expected null resource found.", global.getResource("does/not/exist/Test.txt"));
	}

	@Test
	public void testBuddyClassLoadingGlobalFound() throws Exception {
		Bundle global = installer.installBundle("test.bug438904.global");

		global.loadClass("org.osgi.framework.Bundle");
	}

	@Test
	public void testUnitTestForcompoundEnumerations() {
		Enumeration<Object> result = BundleLoader.compoundEnumerations(null, Collections.emptyEnumeration());
		assertNotNull("Null result.", result);
		assertFalse("Found elements.", result.hasMoreElements());

		result = BundleLoader.compoundEnumerations(Collections.emptyEnumeration(), null);
		assertNotNull("Null result.", result);
		assertFalse("Found elements.", result.hasMoreElements());

		result = BundleLoader.compoundEnumerations(null, null);
		assertNotNull("Null result.", result);
		assertFalse("Found elements.", result.hasMoreElements());

		result = BundleLoader.compoundEnumerations(Collections.emptyEnumeration(), Collections.emptyEnumeration());
		assertNotNull("Null result.", result);
		assertFalse("Found elements.", result.hasMoreElements());
	}

	@Test
	public void testBundleClassLoaderEmptyGetResources() throws Exception {
		final ClassLoader bundleClassLoader = getClass().getClassLoader();
		// Using a resource we know does not exist
		final String resource = "META-INF/services/test.does.note.ExistService";
		doTestEmptyGetResources(bundleClassLoader, resource);
	}

	private void doTestEmptyGetResources(ClassLoader testClassLoader, String resource) throws Exception {
		URL systemURL = ClassLoader.getSystemClassLoader().getResource(resource);
		assertNull("Found a parent resource: " + resource, systemURL);
		// Should return null resource
		URL testurl = testClassLoader.getResource(resource);
		assertNull("Found a resource: " + resource, testurl);

		Enumeration<URL> testResources = testClassLoader.getResources(resource);
		assertNotNull("null resources from testClassLoader: " + resource, testResources);
		assertFalse("Resources has elements.", testResources.hasMoreElements());
	}

	@Test
	public void testBundleReference01() throws Exception {
		Bundle test = installer.installBundle("test"); //$NON-NLS-1$
		Class<?> clazz = test.loadClass("test1.Activator"); //$NON-NLS-1$
		Bundle bundle = FrameworkUtil.getBundle(clazz);
		assertEquals("Wrong bundle", test, bundle); //$NON-NLS-1$
	}

	@Test
	public void testBundleReference02() throws Exception {
		Bundle test = installer.installBundle("test"); //$NON-NLS-1$
		Class<?> clazz = test.loadClass("test1.Activator"); //$NON-NLS-1$
		ClassLoader cl = clazz.getClassLoader();
		assertTrue("ClassLoader is not of type BundleReference", cl instanceof BundleReference);
		assertEquals("Wrong bundle", test, ((BundleReference) cl).getBundle()); //$NON-NLS-1$
	}

	@Test
	public void testResolveURLRelativeBundleResourceWithPort() throws Exception {
		URL directory = new URL("bundleresource://82:1/dictionaries/"); //$NON-NLS-1$
		assertEquals(1, directory.getPort());

		URL resource = new URL(directory, "en_GB.dictionary"); //$NON-NLS-1$
		assertEquals(1, resource.getPort());
	}

	@Test
	public void testManifestPackageSpec() throws BundleException {
		Bundle test = installer.installBundle("test.manifestpackage"); //$NON-NLS-1$
		test.start();
	}

	@Test
	public void testArrayTypeLoad() throws ClassNotFoundException {
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

	@Test
	public void testSystemBundleGetResources01() throws IOException {
		Bundle systemBundle = OSGiTestsActivator.getContext().getBundle(0);
		assertNotNull("Resources is null", systemBundle.getResources("systembundle.properties"));
	}

	@Test
	public void testSystemBundleGetResources02() throws IOException {
		Bundle systemBundle = OSGiTestsActivator.getContext().getBundle(0);
		assertNull("Resources is not null", systemBundle.getResources("java/lang/test.resource"));
	}

	@Test
	public void testBug299921() throws Exception {
		ClassLoader cl = this.getClass().getClassLoader();
		Method findMethod = BundleInstallUpdateTests.findDeclaredMethod(cl.getClass(), "findResources", String.class);
		findMethod.setAccessible(true);

		Enumeration resources = (Enumeration) findMethod.invoke(cl, "test/doesnotexist.txt");
		assertNotNull("Should not be null", resources);
		assertFalse("Found resources!", resources.hasMoreElements());
		resources = cl.getResources("test/doesnotexist.txt");
		assertNotNull("Should not be null", resources);
		assertFalse("Found resources!", resources.hasMoreElements());
	}

	@Test
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
			installer.resolveBundles(new Bundle[] { a, b });
			a.start();
			b.start(Bundle.START_ACTIVATION_POLICY);

			sl.setStartLevel(newSL);
			Object[] expectedFrameworkEvents = new Object[1];
			expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED,
					OSGiTestsActivator.getContext().getBundle(0), null);
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
			expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED,
					OSGiTestsActivator.getContext().getBundle(0), null);
			Object[] actualFrameworkEvents = frameworkListenerResults.getResults(1);
			compareResults(expectedFrameworkEvents, actualFrameworkEvents);
		}
	}

	@Test
	public void testBug348805() throws BundleException {
		final boolean[] endCalled = { false };
		ResolverHookFactory error = triggers -> new ResolverHook() {
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
		ServiceRegistration<ResolverHookFactory> reg = OSGiTestsActivator.getContext()
				.registerService(ResolverHookFactory.class, error, null);
		try {
			Bundle test = installer.installBundle("test"); //$NON-NLS-1$
			BundleException e = assertThrows(BundleException.class, test::start);
			assertEquals("Wrong exception type.", BundleException.REJECTED_BY_HOOK, e.getType());
		} finally {
			reg.unregister();
		}
		assertTrue("end is not called", endCalled[0]);
	}

	@Test
	public void testBug348806() throws BundleException {
		ResolverHookFactory error = triggers -> new ResolverHook() {
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
		ServiceRegistration<ResolverHookFactory> reg = OSGiTestsActivator.getContext()
				.registerService(ResolverHookFactory.class, error, null);
		try {
			Bundle test = installer.installBundle("test"); //$NON-NLS-1$
			BundleException e = assertThrows(BundleException.class, test::start);
			assertEquals("Wrong exception type.", BundleException.REJECTED_BY_HOOK, e.getType());
		} finally {
			reg.unregister();
		}
	}

	@Test
	public void testBug370258_beginException() throws BundleException {
		final boolean[] endCalled = { false };
		ResolverHookFactory endHook = triggers -> new ResolverHook() {
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
		ResolverHookFactory error = triggers -> {
			throw new RuntimeException("Error");
		};

		ServiceRegistration<ResolverHookFactory> endReg = OSGiTestsActivator.getContext()
				.registerService(ResolverHookFactory.class, endHook, null);
		ServiceRegistration<ResolverHookFactory> errorReg = OSGiTestsActivator.getContext()
				.registerService(ResolverHookFactory.class, error, null);
		try {
			Bundle test = installer.installBundle("test"); //$NON-NLS-1$
			BundleException e = assertThrows(BundleException.class, test::start);
			assertEquals("Wrong exception type.", BundleException.REJECTED_BY_HOOK, e.getType());
		} finally {
			errorReg.unregister();
			endReg.unregister();
		}
		assertTrue("end is not called", endCalled[0]);
	}

	@Test
	public void testBug370258_endException() throws BundleException {
		final boolean[] endCalled = { false };
		ResolverHookFactory endHook = triggers -> new ResolverHook() {
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
		ResolverHookFactory error = triggers -> new ResolverHook() {
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

		ServiceRegistration<ResolverHookFactory> errorReg = OSGiTestsActivator.getContext()
				.registerService(ResolverHookFactory.class, error, null);
		ServiceRegistration<ResolverHookFactory> endReg = OSGiTestsActivator.getContext()
				.registerService(ResolverHookFactory.class, endHook, null);
		try {
			Bundle test = installer.installBundle("test"); //$NON-NLS-1$
			BundleException e = assertThrows(BundleException.class, test::start);
			assertEquals("Wrong exception type.", BundleException.REJECTED_BY_HOOK, e.getType());
		} finally {
			errorReg.unregister();
			endReg.unregister();
		}
		assertTrue("end is not called", endCalled[0]);
	}

	@Test
	public void testLoadClassUnresolved() throws Exception {
		Bundle chainTest = installer.installBundle("chain.test"); //$NON-NLS-1$
		assertFalse("Should not resolve bundle: " + chainTest, installer.resolveBundles(new Bundle[] { chainTest }));
		assertThrows("Should not be able to load class: chain.test.TestSingleChain", ClassNotFoundException.class,
				() -> chainTest.loadClass("chain.test.TestSingleChain"));

	}

	private void doTestArrayTypeLoad(String name) throws ClassNotFoundException {
		Class<?> arrayType = OSGiTestsActivator.getBundle().loadClass(name);
		assertNotNull("Null class", arrayType); //$NON-NLS-1$
		assertTrue("Class is not an array: " + arrayType, arrayType.isArray()); //$NON-NLS-1$
	}

	private String readURL(URL url) throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
			return reader.lines().collect(Collectors.joining("\n"));
		}
	}

	@Test
	public void testDefaultLocalUninstall() throws Exception {
		Bundle test = installer.installBundle("security.a"); //$NON-NLS-1$
		test.uninstall();
		Dictionary<String, String> headers = test.getHeaders();
		String bundleName = headers.get(Constants.BUNDLE_NAME);
		assertEquals("Wrong bundle name header.", "default", bundleName);
	}

	@Test
	public void testBug490902() throws BundleException, InterruptedException, ClassNotFoundException,
			InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException {
		final Bundle a1 = installer.installBundle("test.bug490902.a");
		final Bundle b1 = installer.installBundle("test.bug490902.b");
		installer.resolveBundles(new Bundle[] { a1, b1 });

		final CountDownLatch startingB = new CountDownLatch(1);
		final CountDownLatch endedSecondThread = new CountDownLatch(1);
		SynchronousBundleListener delayB1 = event -> {
			if (event.getBundle() == b1 && BundleEvent.STARTING == event.getType()) {
				try {
					startingB.countDown();
					System.out.println(getName() + ": Delaying now ...");
					Thread.sleep(15000);
					System.out.println(getName() + ": Done delaying.");
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		};
		getContext().addBundleListener(delayB1);
		try {
			new Thread(() -> {
				try {
					System.out.println(getName() + ": Initial load test.");
					a1.loadClass("test.bug490902.a.TestLoadA1").getDeclaredConstructor().newInstance();
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}, "Initial load test thread.").start();

			startingB.await();
			Thread secondThread = new Thread(() -> {
				try {
					System.out.println(getName() + ": Second load test.");
					a1.loadClass("test.bug490902.a.TestLoadA1").getDeclaredConstructor().newInstance();
				} catch (Throwable e) {
					e.printStackTrace();
				} finally {
					endedSecondThread.countDown();
				}
			}, "Second load test thread.");
			secondThread.start();
			// hack to make sure secondThread is in the middle of Class.forName
			Thread.sleep(10000);

			System.out.println(getName() + ": About to interrupt:" + secondThread.getName());
			secondThread.interrupt();
			endedSecondThread.await();
			a1.loadClass("test.bug490902.a.TestLoadA1").getDeclaredConstructor().newInstance();
		} finally {
			getContext().removeBundleListener(delayB1);
		}
	}

	@Test
	public void testRecursiveWeavingHookFactory() {
		final ThreadLocal<Boolean> testThread = new ThreadLocal<>() {
			@Override
			protected Boolean initialValue() {
				return Boolean.FALSE;
			}
		};

		testThread.set(Boolean.TRUE);
		final Set<String> weavingHookClasses = new HashSet<>();
		final List<WovenClass> called = new ArrayList<>();
		final AtomicBoolean loadNewClassInWeave = new AtomicBoolean(false);

		ServiceFactory<WeavingHook> topFactory = new ServiceFactory<>() {
			@Override
			public WeavingHook getService(Bundle bundle, ServiceRegistration<WeavingHook> registration) {
				if (!testThread.get()) {
					return null;
				}
				WeavingHook hook = wovenClass -> {
					if (loadNewClassInWeave.get()) {
						// Force a load of inner class (must not be a lambda)
						Runnable run = new Runnable() {
							@Override
							public void run() {
								// nothing
							}
						};
						run.run();
						weavingHookClasses.add(run.getClass().getName());
					}
					called.add(wovenClass);
				};
				weavingHookClasses.add(hook.getClass().getName());
				return hook;
			}

			@Override
			public void ungetService(Bundle bundle, ServiceRegistration<WeavingHook> registration,
					WeavingHook service) {
				// nothing
			}
		};
		ServiceRegistration<WeavingHook> reg = getContext().registerService(WeavingHook.class, topFactory, null);

		Runnable run = null;
		try {
			// force call to factory without protection of the framework recursion checks
			topFactory.getService(null, null);

			// set flag to load inner class while weaving
			loadNewClassInWeave.set(true);

			// Force a load of inner class (must not be a lambda)
			run = new Runnable() {
				@Override
				public void run() {
					// nothing
				}
			};
			run.run();
		} finally {
			reg.unregister();
		}

		assertEquals("Unexpected number of woven classes.", 2, called.size());
		for (WovenClass wovenClass : called) {
			if (weavingHookClasses.contains(wovenClass.getClassName())) {
				assertNull("Did not expect to find class: " + wovenClass.getDefinedClass(),
						wovenClass.getDefinedClass());
			} else {
				assertEquals("Expected the inner runnable class.", run.getClass(), wovenClass.getDefinedClass());
			}
		}
	}

	@Test
	public void testLoaderUninstalledBundle() throws BundleException, IOException {
		String testResourcePath = "testResource";
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); // $NON-NLS-1$
		Map<String, String> testHeaders = new HashMap<>();
		testHeaders.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		testHeaders.put(Constants.BUNDLE_SYMBOLICNAME, getName());
		config.mkdirs();
		File testBundleFile = SystemBundleTests.createBundle(config, getName(), testHeaders,
				Collections.singletonMap(testResourcePath, "testValue"));
		Bundle test = getContext().installBundle(getName(), new FileInputStream(testBundleFile));
		test.start();
		BundleWiring wiring = test.adapt(BundleWiring.class);
		assertNotNull("No wiring found.", wiring);
		ModuleClassLoader bundleClassLoader = (ModuleClassLoader) wiring.getClassLoader();
		URL testResource = bundleClassLoader.findLocalResource(testResourcePath);
		assertNotNull("No test resource found.", testResource);

		test.update(new FileInputStream(testBundleFile));
		testResource = bundleClassLoader.findLocalResource(testResourcePath);
		assertNull("Found resource.", testResource);

		Object[] expectedFrameworkEvents = new Object[] { new FrameworkEvent(FrameworkEvent.INFO, test, null) };
		Object[] actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);

		wiring = test.adapt(BundleWiring.class);
		assertNotNull("No wiring found.", wiring);
		bundleClassLoader = (ModuleClassLoader) wiring.getClassLoader();
		testResource = bundleClassLoader.findLocalResource(testResourcePath);
		assertNotNull("No test resource found.", testResource);

		test.uninstall();

		testResource = bundleClassLoader.findLocalResource(testResourcePath);
		assertNull("Found resource.", testResource);

		actualFrameworkEvents = frameworkListenerResults.getResults(1);
		compareResults(expectedFrameworkEvents, actualFrameworkEvents);
	}

	@Test
	public void testStaleLoaderNPE() throws BundleException, IOException, InterruptedException {
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); // $NON-NLS-1$
		config.mkdirs();

		Map<String, String> exporterHeaders = new HashMap<>();
		exporterHeaders.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		exporterHeaders.put(Constants.BUNDLE_SYMBOLICNAME, "exporter");
		exporterHeaders.put(Constants.EXPORT_PACKAGE, "export1, export2, export3, export4");
		Map<String, String> exporterContent = new HashMap<>();
		exporterContent.put("export1/", null);
		exporterContent.put("export1/SomeClass.class", "SomeClass.class");
		exporterContent.put("export2/", null);
		exporterContent.put("export2/SomeClass.class", "SomeClass.class");
		exporterContent.put("export3/", null);
		exporterContent.put("export3/resource.txt", "resource.txt");
		exporterContent.put("export4/", null);
		exporterContent.put("export4/resource.txt", "resource.txt");
		File exporterBundleFile = SystemBundleTests.createBundle(config, getName() + "-exporter", exporterHeaders,
				exporterContent);

		Map<String, String> importerHeaders = new HashMap<>();
		importerHeaders.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		importerHeaders.put(Constants.BUNDLE_SYMBOLICNAME, "importer");
		importerHeaders.put(Constants.IMPORT_PACKAGE, "export1, export2, export3, export4");
		Map<String, String> importerContent = new HashMap<>();
		importerContent.put("importer/", null);
		importerContent.put("importer/resource.txt", "resource.txt");
		importerContent.put("importer/SomeClass.class", "SomeClass.class");
		File importerBundleFile = SystemBundleTests.createBundle(config, getName() + "-importer", importerHeaders,
				importerContent);

		Map<String, String> requirerHeaders = new HashMap<>();
		requirerHeaders.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		requirerHeaders.put(Constants.BUNDLE_SYMBOLICNAME, "requirer");
		requirerHeaders.put(Constants.REQUIRE_BUNDLE, "exporter");
		Map<String, String> requirerContent = new HashMap<>();
		requirerContent.put("requirer/", null);
		requirerContent.put("requirer/resource.txt", "resource.txt");
		requirerContent.put("requirer/SomeClass.class", "SomeClass.class");
		File requirerBundleFile = SystemBundleTests.createBundle(config, getName() + "-requirer", requirerHeaders,
				requirerContent);

		Bundle exporter = getContext().installBundle(getName() + "-exporter", new FileInputStream(exporterBundleFile));
		exporter.start();
		Bundle importer = getContext().installBundle(getName() + "-importer", new FileInputStream(importerBundleFile));
		importer.start();
		final Bundle requirer = getContext().installBundle(getName() + "-requirer",
				new FileInputStream(requirerBundleFile));
		requirer.start();

		BundleWiring importerWiring = importer.adapt(BundleWiring.class);
		BundleWiring requirerWiring = requirer.adapt(BundleWiring.class);

		// get class loaders so we can use them after invalidating the wires
		ClassLoader importerCL = importerWiring.getClassLoader();
		ClassLoader requirerCL = requirerWiring.getClassLoader();

		assertThrows(LinkageError.class, () -> importerCL.loadClass("export1.SomeClass"));

		URL export3Resource = importerCL.getResource("export3/resource.txt");
		assertNotNull("Missing resource.", export3Resource);

		assertThrows(LinkageError.class, () -> requirerCL.loadClass("export1.SomeClass"));

		export3Resource = requirerCL.getResource("export3/resource.txt");
		assertNotNull("Missing resource.", export3Resource);

		// invalid wires by refreshing the exporter
		refreshBundles(Collections.singleton(exporter));

		// add a framework event listener to find error message about invalud class
		// loaders
		final BlockingQueue<FrameworkEvent> events = new LinkedBlockingQueue<>();
		getContext().addFrameworkListener(event -> {
			if (event.getBundle() == requirer) {
				events.add(event);
			}
		});

		assertThrows(LinkageError.class, () -> importerCL.loadClass("export2.SomeClass"));
		// NOTE the import sources are calculated at loader creating time which happened
		// before the refresh

		URL export4Resource = importerCL.getResource("export4/resource.txt");
		// Note that import sources are calculated at loader creating time
		// which happened before the refresh
		assertNotNull("Missing resource.", export4Resource);

		assertThrows(ClassNotFoundException.class, () -> requirerCL.loadClass("export2.SomeClass"));
		// NOTE the require sources are calculated lazily but now the wire is invalid
		// after the refresh

		export4Resource = requirerCL.getResource("export4/resource.txt");
		assertNull("Found resource from invalid wire.", export4Resource);

		// find the expected event
		FrameworkEvent event = events.poll(5, TimeUnit.SECONDS);
		assertNotNull("No FrameworkEvent found.", event);
		assertEquals("Wrong bundle for event.", requirer, event.getBundle());
		assertEquals("Wrong event type.", FrameworkEvent.ERROR, event.getType());
		assertTrue("Wrong exception: " + event.getThrowable(), event.getThrowable() instanceof RuntimeException);
		assertTrue("Wrong message: " + event.getThrowable().getMessage(),
				event.getThrowable().getMessage().startsWith("Invalid class loader"));

		// make sure there are no others
		assertNull("Found more events.", events.poll(1, TimeUnit.SECONDS));
	}

	@Test
	public void testBug565522FragmentClasspath() throws Exception {
		ByteArrayOutputStream libResourceJarBytes1 = new ByteArrayOutputStream();
		try (JarOutputStream libResourceJar = new JarOutputStream(libResourceJarBytes1)) {
			libResourceJar.putNextEntry(new JarEntry("META-INF/"));
			libResourceJar.closeEntry();
			libResourceJar.putNextEntry(new JarEntry("META-INF/services/"));
			libResourceJar.closeEntry();
			libResourceJar.putNextEntry(new JarEntry("META-INF/services/some.bundle.Factory"));
			libResourceJar.write("testFactory1".getBytes());
			libResourceJar.closeEntry();
		}
		ByteArrayOutputStream libResourceJarBytes2 = new ByteArrayOutputStream();
		try (JarOutputStream libResourceJar = new JarOutputStream(libResourceJarBytes2)) {
			libResourceJar.putNextEntry(new JarEntry("META-INF/"));
			libResourceJar.closeEntry();
			libResourceJar.putNextEntry(new JarEntry("META-INF/services/"));
			libResourceJar.closeEntry();
			libResourceJar.putNextEntry(new JarEntry("META-INF/services/some.bundle.Factory"));
			libResourceJar.write("testFactory2".getBytes());
			libResourceJar.closeEntry();
		}

		File outputDir = OSGiTestsActivator.getContext().getDataFile(getName()); // $NON-NLS-1$
		outputDir.mkdirs();

		Map<String, String> hostHeaders = new HashMap<>();
		hostHeaders.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		hostHeaders.put(Constants.BUNDLE_SYMBOLICNAME, "host");
		hostHeaders.put(Constants.BUNDLE_CLASSPATH, "., lib/resource.jar");
		Map<String, byte[]> hostEntries = new HashMap<>();
		hostEntries.put("lib/", null);
		hostEntries.put("lib/resource.jar", libResourceJarBytes1.toByteArray());
		File hostFile = SystemBundleTests.createBundleWithBytes(outputDir, "host", hostHeaders, hostEntries);

		Map<String, String> fragHeaders = new HashMap<>();
		fragHeaders.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		fragHeaders.put(Constants.BUNDLE_SYMBOLICNAME, "fragment");
		fragHeaders.put(Constants.BUNDLE_CLASSPATH, "., lib/resource.jar");
		fragHeaders.put(Constants.FRAGMENT_HOST, "host");
		Map<String, byte[]> fragEntries = new HashMap<>();
		fragEntries.put("lib/", null);
		fragEntries.put("lib/resource.jar", libResourceJarBytes2.toByteArray());
		File fragFile = SystemBundleTests.createBundleWithBytes(outputDir, "frag", fragHeaders, fragEntries);

		Bundle host = null, frag = null;
		try {
			host = getContext().installBundle(hostFile.toURI().toASCIIString());
			frag = getContext().installBundle(fragFile.toURI().toASCIIString());
			host.start();
			Enumeration<URL> eResources = host.getResources("META-INF/services/some.bundle.Factory");
			assertNotNull("No resources found.", eResources);
			List<URL> resources = Collections.list(eResources);
			assertEquals("Wrong number of resources.", 2, resources.size());
			assertEquals("Wrong content for resource 1", "testFactory1", readURL(resources.get(0)));
			assertEquals("Wrong content for resource 2", "testFactory2", readURL(resources.get(1)));

			// round trip the URLs
			URL copyURL1 = new URL(resources.get(0).toExternalForm());
			URL copyURL2 = new URL(resources.get(1).toExternalForm());
			assertEquals("Wrong content for url copy 1", "testFactory1", readURL(copyURL1));
			assertEquals("Wrong content for url copy 2", "testFactory2", readURL(copyURL2));
		} finally {
			if (host != null) {
				host.uninstall();
			}
			if (frag != null) {
				frag.uninstall();
			}
		}
	}

	void refreshBundles(Collection<Bundle> bundles) throws InterruptedException {
		final CountDownLatch refreshSignal = new CountDownLatch(1);
		getContext().getBundle(Constants.SYSTEM_BUNDLE_LOCATION).adapt(FrameworkWiring.class).refreshBundles(bundles,
				event -> {
					if (event.getType() == FrameworkEvent.PACKAGES_REFRESHED) {
						refreshSignal.countDown();
					}
				});
		refreshSignal.await(30, TimeUnit.SECONDS);
	}
}
