/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.bundles;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.Bundle;

public class PlatformAdminBundleTests extends AbstractBundleTests {
	public static Test suite() {
		return new TestSuite(PlatformAdminBundleTests.class);
	}

	public void testDisabledInfo01() throws Exception {
		String testPolicy = "testDisabledInfo01";
		Bundle chainTest = installer.installBundle("chain.test");
		Bundle chainTestA = installer.installBundle("chain.test.a");
		Bundle chainTestB = installer.installBundle("chain.test.b");
		Bundle chainTestC = installer.installBundle("chain.test.c");
		Bundle chainTestD = installer.installBundle("chain.test.d");
		Bundle[] allBundles = new Bundle[] {chainTest, chainTestA, chainTestB, chainTestC, chainTestD};

		PlatformAdmin pa = installer.getPlatformAdmin();
		State systemState = pa.getState(false);
		BundleDescription testDesc = systemState.getBundle(chainTest.getBundleId());
		BundleDescription testADesc = systemState.getBundle(chainTestA.getBundleId());
		BundleDescription testBDesc = systemState.getBundle(chainTestB.getBundleId());
		BundleDescription testCDesc = systemState.getBundle(chainTestC.getBundleId());
		BundleDescription testDDesc = systemState.getBundle(chainTestD.getBundleId());
		assertNotNull("testDesc null!!", testDesc);
		assertNotNull("testADesc null!!", testADesc);
		assertNotNull("testBDesc null!!", testBDesc);
		assertNotNull("testCDesc null!!", testCDesc);
		assertNotNull("testDDesc null!!", testDDesc);

		installer.resolveBundles(allBundles);
		assertTrue("testDesc not resolved!!", testDesc.isResolved());
		assertTrue("testADesc not resolved!!", testADesc.isResolved());
		assertTrue("testBDesc not resolved!!", testBDesc.isResolved());
		assertTrue("testCDesc not resolved!!", testCDesc.isResolved());
		assertTrue("testDDesc not resolved!!", testDDesc.isResolved());

		// ok finally we can start testing!!!
		// disable the toplevel testDesc bundle
		pa.addDisabledInfo(new DisabledInfo(testPolicy, "reason 1", testDesc));
		// force the bundles to re-resolve
		installer.refreshPackages(allBundles);
		// only testDesc should be unresolved
		assertFalse("testDesc is resolved!!", testDesc.isResolved());
		assertTrue("testADesc not resolved!!", testADesc.isResolved());
		assertTrue("testBDesc not resolved!!", testBDesc.isResolved());
		assertTrue("testCDesc not resolved!!", testCDesc.isResolved());
		assertTrue("testDDesc not resolved!!", testDDesc.isResolved());
		// now enable the bundle
		DisabledInfo info = systemState.getDisabledInfo(testDesc, testPolicy);
		assertNotNull("info is null!!", info);
		pa.removeDisabledInfo(info);
		// just do normal resolve
		assertTrue("resolveBundles returned false!", installer.resolveBundles(new Bundle[] {chainTest}));
		// all bundles should be resolved
		assertTrue("testDesc not resolved!!", testDesc.isResolved());
		assertTrue("testADesc not resolved!!", testADesc.isResolved());
		assertTrue("testBDesc not resolved!!", testBDesc.isResolved());
		assertTrue("testCDesc not resolved!!", testCDesc.isResolved());
		assertTrue("testDDesc not resolved!!", testDDesc.isResolved());
	}

	public void testDisabledInfo02() throws Exception {
		String testPolicy1 = "testDisabledInfo01";
		String testPolicy2 = "testDisabledInfo02";
		Bundle chainTest = installer.installBundle("chain.test");
		Bundle chainTestA = installer.installBundle("chain.test.a");
		Bundle chainTestB = installer.installBundle("chain.test.b");
		Bundle chainTestC = installer.installBundle("chain.test.c");
		Bundle chainTestD = installer.installBundle("chain.test.d");
		Bundle[] allBundles = new Bundle[] {chainTest, chainTestA, chainTestB, chainTestC, chainTestD};

		PlatformAdmin pa = installer.getPlatformAdmin();
		State systemState = pa.getState(false);
		BundleDescription testDesc = systemState.getBundle(chainTest.getBundleId());
		BundleDescription testADesc = systemState.getBundle(chainTestA.getBundleId());
		BundleDescription testBDesc = systemState.getBundle(chainTestB.getBundleId());
		BundleDescription testCDesc = systemState.getBundle(chainTestC.getBundleId());
		BundleDescription testDDesc = systemState.getBundle(chainTestD.getBundleId());
		assertNotNull("testDesc null!!", testDesc);
		assertNotNull("testADesc null!!", testADesc);
		assertNotNull("testBDesc null!!", testBDesc);
		assertNotNull("testCDesc null!!", testCDesc);
		assertNotNull("testDDesc null!!", testDDesc);

		installer.resolveBundles(allBundles);
		assertTrue("testDesc not resolved!!", testDesc.isResolved());
		assertTrue("testADesc not resolved!!", testADesc.isResolved());
		assertTrue("testBDesc not resolved!!", testBDesc.isResolved());
		assertTrue("testCDesc not resolved!!", testCDesc.isResolved());
		assertTrue("testDDesc not resolved!!", testDDesc.isResolved());

		// ok finally we can start testing!!!
		// disable the toplevel testDesc bundle with two DisabledInfos
		pa.addDisabledInfo(new DisabledInfo(testPolicy1, "reason 1", testDesc));
		pa.addDisabledInfo(new DisabledInfo(testPolicy2, "reason 2", testDesc));
		// force the bundles to re-resolve
		installer.refreshPackages(allBundles);
		// only testDesc should be unresolved
		assertFalse("testDesc is resolved!!", testDesc.isResolved());
		assertTrue("testADesc not resolved!!", testADesc.isResolved());
		assertTrue("testBDesc not resolved!!", testBDesc.isResolved());
		assertTrue("testCDesc not resolved!!", testCDesc.isResolved());
		assertTrue("testDDesc not resolved!!", testDDesc.isResolved());

		DisabledInfo info1 = systemState.getDisabledInfo(testDesc, testPolicy1);
		DisabledInfo info2 = systemState.getDisabledInfo(testDesc, testPolicy2);
		assertNotNull("info1 is null!!", info1);
		assertNotNull("info2 is null!!", info2);
		// now remove the first policy
		pa.removeDisabledInfo(info1);
		// just do normal resolve
		assertFalse("resolveBundles returned true!", installer.resolveBundles(new Bundle[] {chainTest}));
		// testDesc should still be unresolved
		assertFalse("testDesc is resolved!!", testDesc.isResolved());
		assertTrue("testADesc not resolved!!", testADesc.isResolved());
		assertTrue("testBDesc not resolved!!", testBDesc.isResolved());
		assertTrue("testCDesc not resolved!!", testCDesc.isResolved());
		assertTrue("testDDesc not resolved!!", testDDesc.isResolved());

		// now remove the second policy
		pa.removeDisabledInfo(info2);
		// just do normal resolve
		assertTrue("resolveBundles returned false!", installer.resolveBundles(new Bundle[] {chainTest}));
		// testDesc should still be unresolved
		assertTrue("testDesc not resolved!!", testDesc.isResolved());
		assertTrue("testADesc not resolved!!", testADesc.isResolved());
		assertTrue("testBDesc not resolved!!", testBDesc.isResolved());
		assertTrue("testCDesc not resolved!!", testCDesc.isResolved());
		assertTrue("testDDesc not resolved!!", testDDesc.isResolved());
	}

	public void testDisabledInfo03() throws Exception {
		String testPolicy1 = "testDisabledInfo01";
		String testPolicy2 = "testDisabledInfo02";
		Bundle chainTest = installer.installBundle("chain.test");
		Bundle chainTestA = installer.installBundle("chain.test.a");
		Bundle chainTestB = installer.installBundle("chain.test.b");
		Bundle chainTestC = installer.installBundle("chain.test.c");
		Bundle chainTestD = installer.installBundle("chain.test.d");
		Bundle[] allBundles = new Bundle[] {chainTest, chainTestA, chainTestB, chainTestC, chainTestD};

		PlatformAdmin pa = installer.getPlatformAdmin();
		State systemState = pa.getState(false);
		BundleDescription testDesc = systemState.getBundle(chainTest.getBundleId());
		BundleDescription testADesc = systemState.getBundle(chainTestA.getBundleId());
		BundleDescription testBDesc = systemState.getBundle(chainTestB.getBundleId());
		BundleDescription testCDesc = systemState.getBundle(chainTestC.getBundleId());
		BundleDescription testDDesc = systemState.getBundle(chainTestD.getBundleId());
		assertNotNull("testDesc null!!", testDesc);
		assertNotNull("testADesc null!!", testADesc);
		assertNotNull("testBDesc null!!", testBDesc);
		assertNotNull("testCDesc null!!", testCDesc);
		assertNotNull("testDDesc null!!", testDDesc);

		installer.resolveBundles(allBundles);
		assertTrue("testDesc not resolved!!", testDesc.isResolved());
		assertTrue("testADesc not resolved!!", testADesc.isResolved());
		assertTrue("testBDesc not resolved!!", testBDesc.isResolved());
		assertTrue("testCDesc not resolved!!", testCDesc.isResolved());
		assertTrue("testDDesc not resolved!!", testDDesc.isResolved());

		// ok finally we can start testing!!!
		// disable the toplevel testDesc bundle
		pa.addDisabledInfo(new DisabledInfo(testPolicy1, "reason 1", testDesc));
		pa.addDisabledInfo(new DisabledInfo(testPolicy2, "reason 2", testDesc));
		// force the bundles to re-resolve
		installer.refreshPackages(allBundles);
		// only testDesc should be unresolved
		assertFalse("testDesc is resolved!!", testDesc.isResolved());
		assertTrue("testADesc not resolved!!", testADesc.isResolved());
		assertTrue("testBDesc not resolved!!", testBDesc.isResolved());
		assertTrue("testCDesc not resolved!!", testCDesc.isResolved());
		assertTrue("testDDesc not resolved!!", testDDesc.isResolved());

		ResolverError[] errors = systemState.getResolverErrors(testDesc);
		assertNotNull("resolver errors is null!!", errors);
		assertEquals("unexpected number of errors", 1, errors.length);
		assertEquals("unexpected error type", ResolverError.DISABLED_BUNDLE, errors[0].getType());
	}

	public void testDisabledInfo04() throws Exception {
		String testPolicy1 = "testDisabledInfo01";
		Bundle chainTest = installer.installBundle("chain.test");
		Bundle chainTestA = installer.installBundle("chain.test.a");
		Bundle chainTestB = installer.installBundle("chain.test.b");
		Bundle chainTestC = installer.installBundle("chain.test.c");
		Bundle chainTestD = installer.installBundle("chain.test.d");
		Bundle[] allBundles = new Bundle[] {chainTest, chainTestA, chainTestB, chainTestC, chainTestD};

		PlatformAdmin pa = installer.getPlatformAdmin();
		State systemState = pa.getState(false);
		BundleDescription testDesc = systemState.getBundle(chainTest.getBundleId());
		BundleDescription testADesc = systemState.getBundle(chainTestA.getBundleId());
		BundleDescription testBDesc = systemState.getBundle(chainTestB.getBundleId());
		BundleDescription testCDesc = systemState.getBundle(chainTestC.getBundleId());
		BundleDescription testDDesc = systemState.getBundle(chainTestD.getBundleId());
		assertNotNull("testDesc null!!", testDesc);
		assertNotNull("testADesc null!!", testADesc);
		assertNotNull("testBDesc null!!", testBDesc);
		assertNotNull("testCDesc null!!", testCDesc);
		assertNotNull("testDDesc null!!", testDDesc);

		installer.resolveBundles(allBundles);
		assertTrue("testDesc not resolved!!", testDesc.isResolved());
		assertTrue("testADesc not resolved!!", testADesc.isResolved());
		assertTrue("testBDesc not resolved!!", testBDesc.isResolved());
		assertTrue("testCDesc not resolved!!", testCDesc.isResolved());
		assertTrue("testDDesc not resolved!!", testDDesc.isResolved());

		// ok finally we can start testing!!!
		// disable the bottom level bundle, this should cause all bundles to be unresolved
		pa.addDisabledInfo(new DisabledInfo(testPolicy1, "reason 1", testDDesc));
		// force the bundles to re-resolve
		installer.refreshPackages(allBundles);
		// only testDesc should be unresolved
		assertFalse("testDesc is resolved!!", testDesc.isResolved());
		assertFalse("testADesc is resolved!!", testADesc.isResolved());
		assertFalse("testBDesc is resolved!!", testBDesc.isResolved());
		assertFalse("testCDesc is resolved!!", testCDesc.isResolved());
		assertFalse("testDDesc is resolved!!", testDDesc.isResolved());

		// check that the resolver errors are the correct type
		ResolverError[] errors = systemState.getResolverErrors(testDDesc);
		assertNotNull("resolver errors is null!!", errors);
		assertEquals("unexpected number of errors", 1, errors.length);
		assertEquals("unexpected error type", ResolverError.DISABLED_BUNDLE, errors[0].getType());

		// all other errors should be missing import errors
		errors = systemState.getResolverErrors(testDesc);
		assertNotNull("resolver errors is null!!", errors);
		assertEquals("unexpected number of errors", 1, errors.length);
		assertEquals("unexpected error type", ResolverError.MISSING_IMPORT_PACKAGE, errors[0].getType());

		errors = systemState.getResolverErrors(testADesc);
		assertNotNull("resolver errors is null!!", errors);
		assertEquals("unexpected number of errors", 1, errors.length);
		assertEquals("unexpected error type", ResolverError.MISSING_IMPORT_PACKAGE, errors[0].getType());

		errors = systemState.getResolverErrors(testBDesc);
		assertNotNull("resolver errors is null!!", errors);
		assertEquals("unexpected number of errors", 1, errors.length);
		assertEquals("unexpected error type", ResolverError.MISSING_IMPORT_PACKAGE, errors[0].getType());

		errors = systemState.getResolverErrors(testCDesc);
		assertNotNull("resolver errors is null!!", errors);
		assertEquals("unexpected number of errors", 1, errors.length);
		assertEquals("unexpected error type", ResolverError.MISSING_IMPORT_PACKAGE, errors[0].getType());
	}

	public void testDisabledInfo05() throws Exception {
		String testPolicy1 = "testDisabledInfo01";
		String testPolicy2 = "testDisabledInfo02";
		Bundle chainTest = installer.installBundle("chain.test");
		Bundle chainTestA = installer.installBundle("chain.test.a");
		Bundle chainTestB = installer.installBundle("chain.test.b");
		Bundle chainTestC = installer.installBundle("chain.test.c");
		Bundle chainTestD = installer.installBundle("chain.test.d");
		Bundle[] allBundles = new Bundle[] {chainTest, chainTestA, chainTestB, chainTestC, chainTestD};

		PlatformAdmin pa = installer.getPlatformAdmin();
		State systemState = pa.getState(false);
		BundleDescription testDesc = systemState.getBundle(chainTest.getBundleId());
		BundleDescription testADesc = systemState.getBundle(chainTestA.getBundleId());
		BundleDescription testBDesc = systemState.getBundle(chainTestB.getBundleId());
		BundleDescription testCDesc = systemState.getBundle(chainTestC.getBundleId());
		BundleDescription testDDesc = systemState.getBundle(chainTestD.getBundleId());
		assertNotNull("testDesc null!!", testDesc);
		assertNotNull("testADesc null!!", testADesc);
		assertNotNull("testBDesc null!!", testBDesc);
		assertNotNull("testCDesc null!!", testCDesc);
		assertNotNull("testDDesc null!!", testDDesc);

		installer.resolveBundles(allBundles);
		assertTrue("testDesc not resolved!!", testDesc.isResolved());
		assertTrue("testADesc not resolved!!", testADesc.isResolved());
		assertTrue("testBDesc not resolved!!", testBDesc.isResolved());
		assertTrue("testCDesc not resolved!!", testCDesc.isResolved());
		assertTrue("testDDesc not resolved!!", testDDesc.isResolved());

		// ok finally we can start testing!!!
		// add many disabled infos to test the getDisabledInfo methods
		DisabledInfo info1 = new DisabledInfo(testPolicy1, "reason 1", testDesc);
		DisabledInfo info2 = new DisabledInfo(testPolicy2, "reason 2", testDesc);
		DisabledInfo info3 = new DisabledInfo(testPolicy1, "reason 3", testDDesc);
		DisabledInfo info4 = new DisabledInfo(testPolicy2, "reason 4", testDDesc);
		pa.addDisabledInfo(info1);
		pa.addDisabledInfo(info2);
		pa.addDisabledInfo(info3);
		pa.addDisabledInfo(info4);
		// force the bundles to re-resolve
		installer.refreshPackages(allBundles);

		// check the errors only, resolution was checked in other tests
		ResolverError[] errors = systemState.getResolverErrors(testDesc);
		assertNotNull("resolver errors is null!!", errors);
		assertEquals("unexpected number of errors", 1, errors.length);
		assertEquals("unexpected error type", ResolverError.DISABLED_BUNDLE, errors[0].getType());

		errors = systemState.getResolverErrors(testDDesc);
		assertNotNull("resolver errors is null!!", errors);
		assertEquals("unexpected number of errors", 1, errors.length);
		assertEquals("unexpected error type", ResolverError.DISABLED_BUNDLE, errors[0].getType());

		DisabledInfo getInfo1 = systemState.getDisabledInfo(testDesc, testPolicy1);
		assertEquals("info1 not equal!", info1, getInfo1);
		DisabledInfo getInfo2 = systemState.getDisabledInfo(testDesc, testPolicy2);
		assertEquals("info2 not equal!", info2, getInfo2);
		DisabledInfo getInfo3 = systemState.getDisabledInfo(testDDesc, testPolicy1);
		assertEquals("info3 not equal!", info3, getInfo3);
		DisabledInfo getInfo4 = systemState.getDisabledInfo(testDDesc, testPolicy2);
		assertEquals("info4 not equal!", info4, getInfo4);

		DisabledInfo[] infos1 = systemState.getDisabledInfos(testDesc);
		assertNotNull("infos1 is null!!", infos1);
		assertEquals("unexpected info1 size", 2, infos1.length);
		assertTrue("info1 not found", infos1[0] == info1 || infos1[1] == info1);
		assertTrue("info2 not found", infos1[0] == info2 || infos1[1] == info2);
		DisabledInfo[] infos2 = systemState.getDisabledInfos(testDDesc);
		assertNotNull("infos2 is null!!", infos2);
		assertEquals("unexpected info2 size", 2, infos2.length);
		assertTrue("info3 not found", infos2[0] == info3 || infos2[1] == info3);
		assertTrue("info4 not found", infos2[0] == info4 || infos2[1] == info4);
	}

	public void testDisabledInfo06() throws Exception {
		String testPolicy = "testDisabledInfo01";
		Bundle chainTest = installer.installBundle("chain.test");
		Bundle chainTestA = installer.installBundle("chain.test.a");
		Bundle chainTestB = installer.installBundle("chain.test.b");
		Bundle chainTestC = installer.installBundle("chain.test.c");
		Bundle chainTestD = installer.installBundle("chain.test.d");
		Bundle[] allBundles = new Bundle[] {chainTest, chainTestA, chainTestB, chainTestC, chainTestD};

		PlatformAdmin pa = installer.getPlatformAdmin();
		State systemState = pa.getState(false);
		BundleDescription testDesc = systemState.getBundle(chainTest.getBundleId());
		BundleDescription testADesc = systemState.getBundle(chainTestA.getBundleId());
		BundleDescription testBDesc = systemState.getBundle(chainTestB.getBundleId());
		BundleDescription testCDesc = systemState.getBundle(chainTestC.getBundleId());
		BundleDescription testDDesc = systemState.getBundle(chainTestD.getBundleId());
		assertNotNull("testDesc null!!", testDesc);
		assertNotNull("testADesc null!!", testADesc);
		assertNotNull("testBDesc null!!", testBDesc);
		assertNotNull("testCDesc null!!", testCDesc);
		assertNotNull("testDDesc null!!", testDDesc);

		installer.resolveBundles(allBundles);
		assertTrue("testDesc not resolved!!", testDesc.isResolved());
		assertTrue("testADesc not resolved!!", testADesc.isResolved());
		assertTrue("testBDesc not resolved!!", testBDesc.isResolved());
		assertTrue("testCDesc not resolved!!", testCDesc.isResolved());
		assertTrue("testDDesc not resolved!!", testDDesc.isResolved());

		// ok finally we can start testing!!!
		// disable the toplevel testDesc bundle
		DisabledInfo info = new DisabledInfo(testPolicy, "reason 1", testDesc);
		pa.addDisabledInfo(info);
		// force the bundles to re-resolve
		installer.refreshPackages(allBundles);
		// only testDesc should be unresolved
		assertFalse("testDesc is resolved!!", testDesc.isResolved());
		assertTrue("testADesc not resolved!!", testADesc.isResolved());
		assertTrue("testBDesc not resolved!!", testBDesc.isResolved());
		assertTrue("testCDesc not resolved!!", testCDesc.isResolved());
		assertTrue("testDDesc not resolved!!", testDDesc.isResolved());
		// uninstall the bundle testDesc
		Bundle uninstalledChainTest = installer.uninstallBundle("chain.test");
		assertTrue("Unexpected uninstall result", uninstalledChainTest == chainTest);

		// now try to get the DisabledInfo
		DisabledInfo getInfo = systemState.getDisabledInfo(testDesc, testPolicy);
		assertNull("info is not null!!", getInfo);
		// try to add the info again.  This should fail
		try {
			pa.addDisabledInfo(info);
			pa.removeDisabledInfo(info);
			fail("should not be able to add a DisabledInfo for a bundle not in the state");
		} catch (IllegalArgumentException e) {
			// expected
		}
	}

	public void testUnresolvedLeaves01() throws Exception {
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
