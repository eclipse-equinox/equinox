/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.bundles;

import java.util.Arrays;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.wiring.FrameworkWiring;

public class PlatformAdminBundleTests extends AbstractBundleTests {
	public static Test suite() {
		return new TestSuite(PlatformAdminBundleTests.class);
	}

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

	public void testResolveRefresh() throws BundleException {
		PlatformAdmin pa = installer.getPlatformAdmin();
		State systemState = pa.getState(false);

		Bundle chainTestA = installer.installBundle("chain.test.a");
		Bundle chainTestB = installer.installBundle("chain.test.b");
		Bundle chainTestC = installer.installBundle("chain.test.c");
		Bundle chainTestD = installer.installBundle("chain.test.d");
		assertTrue("Could not resolve bundles.", getContext().getBundle(0).adapt(FrameworkWiring.class).resolveBundles(Arrays.asList(chainTestA, chainTestB, chainTestC, chainTestD)));

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
		installer.refreshPackages(new Bundle[] {chainTestD});

		assertFalse("testADesc is resolved!!", testADesc.isResolved());
		assertFalse("testBDesc is resolved!!", testBDesc.isResolved());
		assertFalse("testCDesc is resolved!!", testCDesc.isResolved());
	}

	public void disableTestUnresolvedLeaves01() throws Exception {
		Bundle chainTestA = installer.installBundle("chain.test.a");
		Bundle chainTestB = installer.installBundle("chain.test.b");
		Bundle chainTestC = installer.installBundle("chain.test.c");
		Bundle[] allBundles = new Bundle[] {chainTestA, chainTestB, chainTestC};

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
		VersionConstraint[] unsatifiedLeaves = pa.getStateHelper().getUnsatisfiedLeaves(new BundleDescription[] {testADesc});
		assertNotNull("Unsatified constraints is null!!", unsatifiedLeaves);
		assertEquals("Wrong number of constraints!!", 2, unsatifiedLeaves.length);
		for (int i = 0; i < unsatifiedLeaves.length; i++) {
			assertTrue("Constraint type is not import package: " + unsatifiedLeaves[i], unsatifiedLeaves[i] instanceof ImportPackageSpecification);
			assertEquals("Package name is wrong: " + i, "chain.test.d", unsatifiedLeaves[i].getName());
			if (unsatifiedLeaves[i].getBundle() != testBDesc && unsatifiedLeaves[i].getBundle() != testCDesc)
				fail("Wrong bundle for the constraint: " + unsatifiedLeaves[i].getBundle());
		}
	}

}
