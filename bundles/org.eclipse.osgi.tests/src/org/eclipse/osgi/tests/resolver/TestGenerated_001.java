/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.resolver;

import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.service.resolver.StateObjectFactory;
import org.eclipse.osgi.tests.services.resolver.AbstractStateTest;
import org.osgi.framework.BundleException;



public class TestGenerated_001 extends AbstractStateTest {
	public TestGenerated_001(String testName) {
		super(testName);
	}

	BundleDescription bundle_1 = null;
	BundleDescription bundle_2 = null;
	BundleDescription bundle_3 = null;
	BundleDescription bundle_4 = null;
	BundleDescription bundle_5 = null;
	BundleDescription bundle_6 = null;
	BundleDescription bundle_7 = null;
	BundleDescription bundle_8 = null;
	BundleDescription bundle_9 = null;
	BundleDescription bundle_10 = null;

	public void testTest_001() {
		State state = buildEmptyState();
		StateObjectFactory sof = platformAdmin.getFactory();

		bundle_1 = create_bundle_1(sof);
		bundle_2 = create_bundle_2(sof);
		bundle_3 = create_bundle_3(sof);
		bundle_4 = create_bundle_4(sof);
		bundle_5 = create_bundle_5(sof);
		bundle_6 = create_bundle_6(sof);
		bundle_7 = create_bundle_7(sof);
		bundle_8 = create_bundle_8(sof);
		bundle_9 = create_bundle_9(sof);
		bundle_10 = create_bundle_10(sof);
		//***************************************************
		// stage a
		// expect to pass =true
		//***************************************************
		addBundlesToState_a(state);
		//***************************************************
		try {
			state.resolve();
		} catch (Throwable t) {
			fail("unexpected exception class=" + t.getClass().getName()
					+ " message=" + t.getMessage());
			return;
		}
		checkBundlesResolved_a();
		checkWiring_a();
	} // end of method

	public void checkWiringState_1() {
	} // end method

	public void checkWiringState_2() {
	} // end method

	public void checkWiringState_3() {
		ExportPackageDescription[] exports = null;
		exports = null;
		exports = bundle_3.getResolvedImports();
		assertNotNull("export array is unexpectedly null", exports);
		assertTrue("export array is unexpectedly empty", exports.length > 0);
		for (int i = 0; i < exports.length; i++) {
			ExportPackageDescription exp = exports[i];
			String exportPackageName = exp.getName();
			assertNotNull("package name is null", exportPackageName);
			if (exportPackageName.equals("a2")) {
				assertNotNull("Package [a2] is not wired when it should  be ", exp.getExporter());
				assertEquals("Package [a2] is  wired incorrectly ", exp.getExporter(), bundle_8);
			} else if (exportPackageName.equals("a1")) {
				assertNotNull("Package [a1] is not wired when it should  be ", exp.getExporter());
				assertEquals("Package [a1] is  wired incorrectly ", exp.getExporter(), bundle_3);
			} else if (exportPackageName.equals("a3")) {
				assertNotNull("Package [a3] is not wired when it should  be ", exp.getExporter());
				assertEquals("Package [a3] is  wired incorrectly ", exp.getExporter(), bundle_6);
			}
		} // end for
	} // end method

	public void checkWiringState_4() {
	} // end method

	public void checkWiringState_5() {
	} // end method

	public void checkWiringState_6() {
		ExportPackageDescription[] exports = null;
		exports = null;
		exports = bundle_6.getResolvedImports();
		assertNotNull("export array is unexpectedly null", exports);
		assertTrue("export array is unexpectedly empty", exports.length > 0);
		for (int i = 0; i < exports.length; i++) {
			ExportPackageDescription exp = exports[i];
			String exportPackageName = exp.getName();
			assertNotNull("package name is null", exportPackageName);
			if (exportPackageName.equals("a2")) {
				assertNotNull("Package [a2] is not wired when it should  be ", exp.getExporter());
				assertEquals("Package [a2] is  wired incorrectly ", exp.getExporter(), bundle_8);
			} else if (exportPackageName.equals("a1")) {
				assertNotNull("Package [a1] is not wired when it should  be ", exp.getExporter());
				assertEquals("Package [a1] is  wired incorrectly ", exp.getExporter(), bundle_3);
			} else if (exportPackageName.equals("a3")) {
				assertNotNull("Package [a3] is not wired when it should  be ", exp.getExporter());
				assertEquals("Package [a3] is  wired incorrectly ", exp.getExporter(), bundle_6);
			}
		} // end for
	} // end method

	public void checkWiringState_7() {
		ExportPackageDescription[] exports = null;
		exports = null;
		exports = bundle_7.getResolvedImports();
		assertNotNull("export array is unexpectedly null", exports);
		assertTrue("export array is unexpectedly empty", exports.length > 0);
		for (int i = 0; i < exports.length; i++) {
			ExportPackageDescription exp = exports[i];
			String exportPackageName = exp.getName();
			assertNotNull("package name is null", exportPackageName);
			if (exportPackageName.equals("a2")) {
				assertNotNull("Package [a2] is not wired when it should  be ", exp.getExporter());
				assertEquals("Package [a2] is  wired incorrectly ", exp.getExporter(), bundle_8);
			} else if (exportPackageName.equals("a1")) {
				assertNotNull("Package [a1] is not wired when it should  be ", exp.getExporter());
				assertEquals("Package [a1] is  wired incorrectly ", exp.getExporter(), bundle_3);
			} else if (exportPackageName.equals("a3")) {
				assertNotNull("Package [a3] is not wired when it should  be ", exp.getExporter());
				assertEquals("Package [a3] is  wired incorrectly ", exp.getExporter(), bundle_6);
			}
		} // end for
	} // end method

	public void checkWiringState_8() {
		ExportPackageDescription[] exports = null;
		exports = null;
		exports = bundle_8.getResolvedImports();
		assertNotNull("export array is unexpectedly null", exports);
		assertTrue("export array is unexpectedly empty", exports.length > 0);
		for (int i = 0; i < exports.length; i++) {
			ExportPackageDescription exp = exports[i];
			String exportPackageName = exp.getName();
			assertNotNull("package name is null", exportPackageName);
			if (exportPackageName.equals("a2")) {
				assertNotNull("Package [a2] is not wired when it should  be ", exp.getExporter());
				assertEquals("Package [a2] is  wired incorrectly ", exp.getExporter(), bundle_8);
			} else if (exportPackageName.equals("a1")) {
				assertNotNull("Package [a1] is not wired when it should  be ", exp.getExporter());
				assertEquals("Package [a1] is  wired incorrectly ", exp.getExporter(), bundle_3);
			} else if (exportPackageName.equals("a3")) {
				assertNotNull("Package [a3] is not wired when it should  be ", exp.getExporter());
				assertEquals("Package [a3] is  wired incorrectly ", exp.getExporter(), bundle_6);
			}
		} // end for
	} // end method

	public void checkWiringState_9() {
	} // end method

	public void checkWiringState_10() {
	} // end method

	public void checkWiring_a() {
		checkWiringState_1();
		checkWiringState_2();
		checkWiringState_3();
		checkWiringState_4();
		checkWiringState_5();
		checkWiringState_6();
		checkWiringState_7();
		checkWiringState_8();
		checkWiringState_9();
		checkWiringState_10();
	} // end method

	public void addBundlesToState_a(State state) {
		boolean added = false;
		added = state.addBundle(bundle_1);
		assertTrue("failed to add bundle ", added);
		added = state.addBundle(bundle_2);
		assertTrue("failed to add bundle ", added);
		added = state.addBundle(bundle_3);
		assertTrue("failed to add bundle ", added);
		added = state.addBundle(bundle_4);
		assertTrue("failed to add bundle ", added);
		added = state.addBundle(bundle_5);
		assertTrue("failed to add bundle ", added);
		added = state.addBundle(bundle_6);
		assertTrue("failed to add bundle ", added);
		added = state.addBundle(bundle_7);
		assertTrue("failed to add bundle ", added);
		added = state.addBundle(bundle_8);
		assertTrue("failed to add bundle ", added);
		added = state.addBundle(bundle_9);
		assertTrue("failed to add bundle ", added);
		added = state.addBundle(bundle_10);
		assertTrue("failed to add bundle ", added);
	} // end method

	public void checkBundlesResolved_a() {
		assertTrue("unexpected bundle resolution state", !bundle_1.isResolved());
		assertTrue("unexpected bundle resolution state", !bundle_2.isResolved());
		assertTrue("unexpected bundle resolution state", bundle_3.isResolved());
		assertTrue("unexpected bundle resolution state", !bundle_4.isResolved());
		assertTrue("unexpected bundle resolution state", !bundle_5.isResolved());
		assertTrue("unexpected bundle resolution state", bundle_6.isResolved());
		assertTrue("unexpected bundle resolution state", bundle_7.isResolved());
		assertTrue("unexpected bundle resolution state", bundle_8.isResolved());
		assertTrue("unexpected bundle resolution state", !bundle_9.isResolved());
		assertTrue("unexpected bundle resolution state", !bundle_10.isResolved());
	} // end method

	public BundleDescription create_bundle_1(StateObjectFactory sof) {
		java.util.Dictionary dictionary_1 = new java.util.Properties();
		BundleDescription bundle = null;
		dictionary_1.put("Bundle-ManifestVersion", "2");
		dictionary_1.put("Bundle-SymbolicName", "A");
		dictionary_1.put("Import-Package", "a1; version=\"[2, 3]\", a2; version=8, a3; version=\"[5, 13]\"");
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
		dictionary_2.put("Import-Package", "a1; version=\"[3, 3.9]\", a2; version=\"[8, 8.9]\", a3; version=0");
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
		dictionary_3.put("Export-Package", "a1; version=7");
		dictionary_3.put("Import-Package", "a1; version=3, a2; version=8, a3; version=0");
		try {
			bundle = sof.createBundleDescription(dictionary_3, "bundle_3", 3);
		} catch (BundleException be) {
			fail(be.getMessage());
		}
		return bundle;
	} // end of method

	public BundleDescription create_bundle_4(StateObjectFactory sof) {
		java.util.Dictionary dictionary_4 = new java.util.Properties();
		BundleDescription bundle = null;
		dictionary_4.put("Bundle-ManifestVersion", "2");
		dictionary_4.put("Bundle-SymbolicName", "D");
		dictionary_4.put("Import-Package", "a1; version=\"[3, 3.9]\", a2; version=8, a3; version=5");
		try {
			bundle = sof.createBundleDescription(dictionary_4, "bundle_4", 4);
		} catch (BundleException be) {
			fail(be.getMessage());
		}
		return bundle;
	} // end of method

	public BundleDescription create_bundle_5(StateObjectFactory sof) {
		java.util.Dictionary dictionary_5 = new java.util.Properties();
		BundleDescription bundle = null;
		dictionary_5.put("Bundle-ManifestVersion", "2");
		dictionary_5.put("Bundle-SymbolicName", "E");
		dictionary_5.put("Import-Package", "a1; version=7, a2; version=8, a3; version=5");
		try {
			bundle = sof.createBundleDescription(dictionary_5, "bundle_5", 5);
		} catch (BundleException be) {
			fail(be.getMessage());
		}
		return bundle;
	} // end of method

	public BundleDescription create_bundle_6(StateObjectFactory sof) {
		java.util.Dictionary dictionary_6 = new java.util.Properties();
		BundleDescription bundle = null;
		dictionary_6.put("Bundle-ManifestVersion", "2");
		dictionary_6.put("Bundle-SymbolicName", "F");
		dictionary_6.put("Export-Package", "a3; version=0");
		dictionary_6.put("Import-Package", "a1; version=7, a2; version=\"[7, 8]\", a3; version=\"[0, 0]\"");
		try {
			bundle = sof.createBundleDescription(dictionary_6, "bundle_6", 6);
		} catch (BundleException be) {
			fail(be.getMessage());
		}
		return bundle;
	} // end of method

	public BundleDescription create_bundle_7(StateObjectFactory sof) {
		java.util.Dictionary dictionary_7 = new java.util.Properties();
		BundleDescription bundle = null;
		dictionary_7.put("Bundle-ManifestVersion", "2");
		dictionary_7.put("Bundle-SymbolicName", "G");
		dictionary_7.put("Export-Package", "a1; version=3");
		dictionary_7.put("Import-Package", "a1; version=\"[7, 7.9]\", a2; version=8, a3; version=0");
		try {
			bundle = sof.createBundleDescription(dictionary_7, "bundle_7", 7);
		} catch (BundleException be) {
			fail(be.getMessage());
		}
		return bundle;
	} // end of method

	public BundleDescription create_bundle_8(StateObjectFactory sof) {
		java.util.Dictionary dictionary_8 = new java.util.Properties();
		BundleDescription bundle = null;
		dictionary_8.put("Bundle-ManifestVersion", "2");
		dictionary_8.put("Bundle-SymbolicName", "H");
		dictionary_8.put("Export-Package", "a2; version=8");
		dictionary_8.put("Import-Package", "a1; version=3, a2; version=8, a3; version=0");
		try {
			bundle = sof.createBundleDescription(dictionary_8, "bundle_8", 8);
		} catch (BundleException be) {
			fail(be.getMessage());
		}
		return bundle;
	} // end of method

	public BundleDescription create_bundle_9(StateObjectFactory sof) {
		java.util.Dictionary dictionary_9 = new java.util.Properties();
		BundleDescription bundle = null;
		dictionary_9.put("Bundle-ManifestVersion", "2");
		dictionary_9.put("Bundle-SymbolicName", "I");
		dictionary_9.put("Export-Package", "a3; version=5");
		dictionary_9.put("Import-Package", "a1; version=\"[3, 3.9]\", a2; version=\"[8, 16]\", a3; version=5");
		try {
			bundle = sof.createBundleDescription(dictionary_9, "bundle_9", 9);
		} catch (BundleException be) {
			fail(be.getMessage());
		}
		return bundle;
	} // end of method

	public BundleDescription create_bundle_10(StateObjectFactory sof) {
		java.util.Dictionary dictionary_10 = new java.util.Properties();
		BundleDescription bundle = null;
		dictionary_10.put("Bundle-ManifestVersion", "2");
		dictionary_10.put("Bundle-SymbolicName", "J");
		dictionary_10.put("Import-Package", "a1; version=7, a2; version=\"[7, 8]\", a3; version=5");
		try {
			bundle = sof.createBundleDescription(dictionary_10, "bundle_10", 10);
		} catch (BundleException be) {
			fail(be.getMessage());
		}
		return bundle;
	} // end of method
} // end of testcase
