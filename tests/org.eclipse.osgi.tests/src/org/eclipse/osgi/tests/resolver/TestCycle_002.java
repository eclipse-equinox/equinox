/*******************************************************************************
 * Copyright (c) 2004, 2013 IBM Corporation and others.
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
package org.eclipse.osgi.tests.resolver;

import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.tests.services.resolver.AbstractStateTest;
import org.osgi.framework.BundleException;

public class TestCycle_002 extends AbstractStateTest {
	public TestCycle_002(String testName) {
		super(testName);
	}

	BundleDescription bundle_1 = null;
	BundleDescription bundle_2 = null;
	BundleDescription bundle_3 = null;

	public void testTest_002() {
		State state = buildEmptyState();
		StateObjectFactory sof = StateObjectFactory.defaultFactory;

		bundle_1 = create_bundle_1(sof);
		bundle_2 = create_bundle_2(sof);
		bundle_3 = create_bundle_3(sof);
		//***************************************************
		// stage a
		// expect to pass =true
		//***************************************************
		addBundlesToState_a(state);
		//***************************************************
		try {
			state.resolve();
		} catch (Throwable t) {
			fail("unexpected exception class=" + t.getClass().getName() + " message=" + t.getMessage());
			return;
		}
		checkBundlesResolved_a();
		checkWiring_a();
	} // end of method

	public void checkWiringState_1() {
		BundleDescription[] requires = bundle_1.getResolvedRequires();
		assertNotNull("requires array is unexpectedly null", requires);
		assertTrue("requires array is unexpectedly empty", requires.length > 0);
		for (BundleDescription require : requires) {
			String requiresName = require.getName();
			assertNotNull("package name is null", requiresName);
			if (requiresName.equals("B")) {
				assertNotNull("Require [B] is not wired when it should be ", require);
				assertEquals("Require [B] is wired incorrectly ", require, bundle_2);
			} else if (requiresName.equals("C")) {
				assertNotNull("Require [C] is not wired when it should be ", require);
				assertEquals("Require [C] is wired incorrectly ", require, bundle_3);
			}
		} // end for
	} // end method

	public void checkWiringState_2() {
		BundleDescription[] requires = bundle_2.getResolvedRequires();
		assertNotNull("requires array is unexpectedly null", requires);
		assertTrue("requires array is unexpectedly empty", requires.length > 0);
		for (BundleDescription require : requires) {
			String requiresName = require.getName();
			assertNotNull("package name is null", requiresName);
			if (requiresName.equals("A")) {
				assertNotNull("Require [A] is not wired when it should be ", require);
				assertEquals("Require [A] is wired incorrectly ", require, bundle_1);
			} else if (requiresName.equals("C")) {
				assertNotNull("Require [C] is not wired when it should be ", require);
				assertEquals("Require [C] is wired incorrectly ", require, bundle_3);
			}
		} // end for
	} // end method

	public void checkWiringState_3() {
		BundleDescription[] requires = bundle_3.getResolvedRequires();
		assertNotNull("requires array is unexpectedly null", requires);
		assertTrue("requires array is unexpectedly empty", requires.length > 0);
		for (BundleDescription require : requires) {
			String requiresName = require.getName();
			assertNotNull("package name is null", requiresName);
			if (requiresName.equals("A")) {
				assertNotNull("Require [A] is not wired when it should be ", require);
				assertEquals("Require [A] is wired incorrectly ", require, bundle_1);
			} else if (requiresName.equals("B")) {
				assertNotNull("Require [B] is not wired when it should be ", require);
				assertEquals("Require [B] is wired incorrectly ", require, bundle_2);
			}
		} // end for
	} // end method

	public void checkWiring_a() {
		checkWiringState_1();
		checkWiringState_2();
		checkWiringState_3();
	} // end method

	public void addBundlesToState_a(State state) {
		boolean added = false;
		added = state.addBundle(bundle_1);
		assertTrue("failed to add bundle ", added);
		added = state.addBundle(bundle_2);
		assertTrue("failed to add bundle ", added);
		added = state.addBundle(bundle_3);
		assertTrue("failed to add bundle ", added);
	} // end method

	public void checkBundlesResolved_a() {
		assertTrue("unexpected bundle resolution state", bundle_1.isResolved());
		assertTrue("unexpected bundle resolution state", bundle_2.isResolved());
		assertTrue("unexpected bundle resolution state", bundle_3.isResolved());
	} // end method

	public BundleDescription create_bundle_1(StateObjectFactory sof) {
		java.util.Dictionary dictionary_1 = new java.util.Properties();
		BundleDescription bundle = null;
		dictionary_1.put("Bundle-ManifestVersion", "2");
		dictionary_1.put("Bundle-SymbolicName", "A");
		dictionary_1.put("Require-Bundle", "B, C");
		try {
			bundle = sof.createBundleDescription(dictionary_1, "bundle_1", 1);
		} catch (BundleException be) {
			fail(be.getMessage());
		}
		return bundle;
	} // end of method

	public BundleDescription create_bundle_2(StateObjectFactory sof) {
		java.util.Dictionary dictionary_2 = new java.util.Properties();
		BundleDescription bundle = null;
		dictionary_2.put("Bundle-ManifestVersion", "2");
		dictionary_2.put("Bundle-SymbolicName", "B");
		dictionary_2.put("Require-Bundle", "A, C");
		try {
			bundle = sof.createBundleDescription(dictionary_2, "bundle_2", 2);
		} catch (BundleException be) {
			fail(be.getMessage());
		}
		return bundle;
	} // end of method

	public BundleDescription create_bundle_3(StateObjectFactory sof) {
		java.util.Dictionary dictionary_3 = new java.util.Properties();
		BundleDescription bundle = null;
		dictionary_3.put("Bundle-ManifestVersion", "2");
		dictionary_3.put("Bundle-SymbolicName", "C");
		dictionary_3.put("Require-Bundle", "A, B");
		try {
			bundle = sof.createBundleDescription(dictionary_3, "bundle_3", 3);
		} catch (BundleException be) {
			fail(be.getMessage());
		}
		return bundle;
	} // end of method

} // end of testcase
