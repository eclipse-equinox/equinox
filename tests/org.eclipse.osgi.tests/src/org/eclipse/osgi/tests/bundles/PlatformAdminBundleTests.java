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

import static org.hamcrest.CoreMatchers.either;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.ImportPackageSpecification;
import org.eclipse.osgi.service.resolver.PlatformAdmin;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.service.resolver.VersionConstraint;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.hamcrest.MatcherAssert;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.wiring.FrameworkWiring;

public class PlatformAdminBundleTests extends AbstractBundleTests {

	@Test
	public void testInstallUninstallBundle() throws BundleException {
		PlatformAdmin pa = installer.getPlatformAdmin();
		// get system state first to ensure it does not have the test bundle
		State systemState = pa.getState(false);

		BundleDescription testDesc = systemState.getBundleByLocation(installer.getBundleLocation("test"));
		assertNull("Should not find bundle.", testDesc);

		Bundle test = installer.installBundle("test");
		testDesc = systemState.getBundleByLocation(installer.getBundleLocation("test"));
		assertNotNull("Should find bundle.", testDesc);

		test.uninstall();
		testDesc = systemState.getBundleByLocation(installer.getBundleLocation("test"));
		assertNull("Should not find bundle.", testDesc);
	}

	@Test
	public void testResolveRefresh() throws Exception {
		PlatformAdmin pa = installer.getPlatformAdmin();
		State systemState = pa.getState(false);

		Bundle chainTestA = installer.installBundle("chain.test.a");
		Bundle chainTestB = installer.installBundle("chain.test.b");
		Bundle chainTestC = installer.installBundle("chain.test.c");
		Bundle chainTestD = installer.installBundle("chain.test.d");
		assertTrue("Could not resolve bundles.", getContext().getBundle(0).adapt(FrameworkWiring.class)
				.resolveBundles(Arrays.asList(chainTestA, chainTestB, chainTestC, chainTestD)));

		BundleDescription testADesc = systemState.getBundle(chainTestA.getBundleId());
		BundleDescription testBDesc = systemState.getBundle(chainTestB.getBundleId());
		BundleDescription testCDesc = systemState.getBundle(chainTestC.getBundleId());
		BundleDescription testDDesc = systemState.getBundle(chainTestD.getBundleId());
		assertNotNull("testADesc null!!", testADesc);
		assertNotNull("testBDesc null!!", testBDesc);
		assertNotNull("testCDesc null!!", testCDesc);
		assertNotNull("testDDesc null!!", testDDesc);

		assertTrue("testADesc is not resolved!!", testADesc.isResolved());
		assertTrue("testBDesc is not resolved!!", testBDesc.isResolved());
		assertTrue("testCDesc is not resolved!!", testCDesc.isResolved());
		assertTrue("testCDesc is not resolved!!", testDDesc.isResolved());

		chainTestD.uninstall();
		installer.refreshPackages(new Bundle[] { chainTestD });

		if (testADesc.isResolved()) {
			// This is a hack to wait some time to allow package admin event to be fired
			// to all listeners.
			Thread.sleep(1000);
		}

		assertFalse("testADesc is resolved!!", testADesc.isResolved());
		assertFalse("testBDesc is resolved!!", testBDesc.isResolved());
		assertFalse("testCDesc is resolved!!", testCDesc.isResolved());
	}

	@Test
	@Ignore
	public void testUnresolvedLeaves01() throws Exception {
		Bundle chainTestA = installer.installBundle("chain.test.a");
		Bundle chainTestB = installer.installBundle("chain.test.b");
		Bundle chainTestC = installer.installBundle("chain.test.c");
		Bundle[] allBundles = new Bundle[] { chainTestA, chainTestB, chainTestC };

		PlatformAdmin pa = installer.getPlatformAdmin();
		State systemState = pa.getState(false);
		BundleDescription testADesc = systemState.getBundle(chainTestA.getBundleId());
		BundleDescription testBDesc = systemState.getBundle(chainTestB.getBundleId());
		BundleDescription testCDesc = systemState.getBundle(chainTestC.getBundleId());
		assertNotNull("testADesc null!!", testADesc);
		assertNotNull("testBDesc null!!", testBDesc);
		assertNotNull("testCDesc null!!", testCDesc);

		installer.resolveBundles(allBundles);
		assertFalse("testADesc is resolved!!", testADesc.isResolved());
		assertFalse("testBDesc is resolved!!", testBDesc.isResolved());
		assertFalse("testCDesc is resolved!!", testCDesc.isResolved());

		// ok finally we can start testing!!
		VersionConstraint[] unsatifiedLeaves = pa.getStateHelper()
				.getUnsatisfiedLeaves(new BundleDescription[] { testADesc });
		assertNotNull("Unsatified constraints is null!!", unsatifiedLeaves);
		assertEquals("Wrong number of constraints!!", 2, unsatifiedLeaves.length);
		for (int i = 0; i < unsatifiedLeaves.length; i++) {
			assertTrue("Constraint type is not import package: " + unsatifiedLeaves[i],
					unsatifiedLeaves[i] instanceof ImportPackageSpecification);
			assertEquals("Package name is wrong: " + i, "chain.test.d", unsatifiedLeaves[i].getName());

			MatcherAssert.assertThat("Wrong bundle for the constraint: " + unsatifiedLeaves[i].getBundle(),
					unsatifiedLeaves[i].getBundle(), either(is(testBDesc)).or(is(testCDesc)));
		}
	}

	@Test
	public void testR3Bundle() throws BundleException, InvalidSyntaxException {
		PlatformAdmin pa = installer.getPlatformAdmin();
		State systemState = pa.getState(false);
		BundleInstaller r3Installer = new BundleInstaller("test_files/platformAdmin", OSGiTestsActivator.getContext());
		try {
			Bundle b = r3Installer.installBundle("b1");
			BundleDescription bDesc = systemState.getBundle(b.getBundleId());
			assertNotNull("No bundle description.", bDesc);
			ExportPackageDescription[] exports = bDesc.getExportPackages();
			ImportPackageSpecification[] imports = bDesc.getImportPackages();
			assertEquals("Wrong number of exports", 1, exports.length);
			assertEquals("Wrong number of imports.", 2, imports.length);
		} finally {
			r3Installer.shutdown();
		}
	}

	@Test
	public void testNativeCodeFilterWithSpecialChars() throws BundleException, InterruptedException {
		final AtomicReference<FrameworkEvent> error = new AtomicReference<>();
		final CountDownLatch errorCnt = new CountDownLatch(1);
		FrameworkListener errorListener = event -> {
			if (event.getType() == FrameworkEvent.ERROR) {
				error.set(event);
				errorCnt.countDown();
			}
		};
		getContext().addFrameworkListener(errorListener);
		try {
			PlatformAdmin pa = installer.getPlatformAdmin();
			State systemState = pa.getState(false);
			// just making sure the system state is fully created first
			assertNotNull(systemState.getBundle(0));
			Bundle nativeTestF = installer.installBundle("nativetest.f");
			nativeTestF.start();
			// expecting no errors
			errorCnt.await(5, TimeUnit.SECONDS);
			assertNull("Found an error: " + error.get(), error.get());
		} finally {
			getContext().removeFrameworkListener(errorListener);
		}
	}

	@Test
	public void testTimestamp() throws BundleException {
		PlatformAdmin pa = installer.getPlatformAdmin();
		// get system state first to ensure it does not have the test bundle
		State systemState = pa.getState(false);

		long initialTimeStamp = systemState.getTimeStamp();
		Bundle test = installer.installBundle("test");

		assertTrue("Timestamp has not changed.", initialTimeStamp != systemState.getTimeStamp());

		initialTimeStamp = systemState.getTimeStamp();
		test.adapt(BundleStartLevel.class).setStartLevel(1000);

		assertTrue("Timestamp has not changed.", initialTimeStamp == systemState.getTimeStamp());

		initialTimeStamp = systemState.getTimeStamp();
		test.uninstall();
		assertTrue("Timestamp has not changed.", initialTimeStamp != systemState.getTimeStamp());
	}
}
