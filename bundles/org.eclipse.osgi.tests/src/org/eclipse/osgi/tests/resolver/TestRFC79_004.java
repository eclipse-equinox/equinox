package org.eclipse.osgi.tests.resolver;

import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.service.resolver.StateObjectFactory;
import org.eclipse.osgi.tests.services.resolver.AbstractStateTest;
import org.osgi.framework.BundleException;


public class TestRFC79_004 extends AbstractStateTest {
	public TestRFC79_004(String testName) {
		super(testName);
	}

	BundleDescription bundle_1 = null;
	BundleDescription bundle_2 = null;
	BundleDescription bundle_3 = null;
	BundleDescription bundle_4 = null;

	
	public void testTest_004() {
		State state = buildEmptyState();
		StateObjectFactory sof = platformAdmin.getFactory();

		bundle_1 = create_bundle_1(sof);
		bundle_2 = create_bundle_2(sof);
		bundle_3 = create_bundle_3(sof);
		bundle_4 = create_bundle_4(sof);
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
		ExportPackageDescription[] exports = bundle_1.getResolvedImports();
		assertNotNull("export array is unexpectedly null", exports);
		assertTrue("export array is unexpectedly empty", exports.length > 0);
		for (int i = 0; i < exports.length; i++) {
			String exportPackageName = exports[i].getName();
			assertNotNull("package name is null", exportPackageName);
			if (exportPackageName.equals("org.apache.commons.logging")) {
				assertNotNull("Package [org.apache.commons.logging] is not wired when it should be ", exports[i].getExporter());
				assertEquals("Package [org.apache.commons.logging] is wired incorrectly ", exports[i].getExporter(), bundle_4);
			} else if (exportPackageName.equals("org.apache.commons.io")) {
				assertNotNull("Package [org.apache.commons.io] is not wired when it should be ", exports[i].getExporter());
				assertEquals("Package [org.apache.commons.io] is wired incorrectly ", exports[i].getExporter(), bundle_4);
			}
		} // end for
	} // end method

	public void checkWiringState_2() {
		ExportPackageDescription[] exports = bundle_2.getResolvedImports();
		assertNotNull("export array is unexpectedly null", exports);
		assertTrue("export array is unexpectedly empty", exports.length > 0);
		for (int i = 0; i < exports.length; i++) {
			String exportPackageName = exports[i].getName();
			assertNotNull("package name is null", exportPackageName);
			if (exportPackageName.equals("org.apache.commons.logging")) {
				assertNotNull("Package [org.apache.commons.logging] is not wired when it should be ", exports[i].getExporter());
				assertEquals("Package [org.apache.commons.logging] is wired incorrectly ", exports[i].getExporter(), bundle_3);
			} else if (exportPackageName.equals("org.apache.commons.io")) {
				assertNotNull("Package [org.apache.commons.io] is not wired when it should be ", exports[i].getExporter());
				assertEquals("Package [org.apache.commons.io] is wired incorrectly ", exports[i].getExporter(), bundle_3);
			}
		} // end for
	} // end method

	public void checkWiringState_3() {
	} // end method

	public void checkWiringState_4() {
	} // end method

	

	public void checkWiring_a() {
		checkWiringState_1();
		checkWiringState_2();
		checkWiringState_3();
		checkWiringState_4();
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
	} // end method

	
	public void checkBundlesResolved_a() {
		assertTrue("unexpected bundle resolution state", bundle_1.isResolved());
		assertTrue("unexpected bundle resolution state", bundle_2.isResolved());
		assertTrue("unexpected bundle resolution state", bundle_3.isResolved());
		assertTrue("unexpected bundle resolution state", bundle_4.isResolved());
	} // end method

	
	public BundleDescription create_bundle_1(StateObjectFactory sof) {
		java.util.Dictionary dictionary_1 = new java.util.Properties();
		BundleDescription bundle = null;
		dictionary_1.put("Bundle-ManifestVersion", "2");
		dictionary_1.put("Bundle-SymbolicName", "A");
		dictionary_1.put("Import-Package", "org.apache.commons.logging; org.apache.commons.io; version=2.0.0");
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
		dictionary_2.put("Import-Package", "org.apache.commons.logging; version=2.0.0, org.apache.commons.io; version=\"[2.0.0, 2.0.0]\"");
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
		dictionary_3.put("Bundle-SymbolicName", "ApacheCommons");
		dictionary_3.put("Export-Package", "org.apache.commons.logging; org.apache.commons.io; version=2.0.0; grouping:=commons");
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
		dictionary_4.put("Bundle-SymbolicName", "ApacheCommons'");
		dictionary_4.put("Export-Package", "org.apache.commons.logging; org.apache.commons.io; version=2.1.0; grouping:=commons");
		try {
			bundle = sof.createBundleDescription(dictionary_4, "bundle_4", 4);
		} catch (BundleException be) {
			fail(be.getMessage());
		}
		return bundle;
	} // end of method


} // end of testcase
