package org.eclipse.osgi.tests.resolver;

import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.service.resolver.StateObjectFactory;
import org.eclipse.osgi.tests.services.resolver.AbstractStateTest;
import org.osgi.framework.BundleException;


public class TestRFC79_006 extends AbstractStateTest {
	public TestRFC79_006(String testName) {
		super(testName);
	}

	BundleDescription bundle_1 = null;
	BundleDescription bundle_2 = null;
	BundleDescription bundle_3 = null;
	BundleDescription bundle_4 = null;
	BundleDescription bundle_5 = null;
	BundleDescription bundle_6 = null;

	
	public void testTest_006() {
		State state = buildEmptyState();
		StateObjectFactory sof = platformAdmin.getFactory();

		bundle_1 = create_bundle_1(sof);
		bundle_2 = create_bundle_2(sof);
		bundle_3 = create_bundle_3(sof);
		bundle_4 = create_bundle_4(sof);
		bundle_5 = create_bundle_5(sof);
		bundle_6 = create_bundle_6(sof);
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
			ExportPackageDescription exp = exports[i];
			String exportPackageName = exp.getName();
			assertNotNull("package name is null", exportPackageName);
			if (exportPackageName.equals("org.xml.sax")) {
				assertNotNull("Package [org.xml.sax] is not wired when it should be ", exp.getExporter());
				assertEquals("Package [org.xml.sax] is wired incorrectly ", exp.getExporter(), bundle_3);
			} else if (exportPackageName.equals("org.w3c.dom")) {
				assertNotNull("Package [org.w3c.dom] is not wired when it should be ", exp.getExporter());
				assertEquals("Package [org.w3c.dom] is wired incorrectly ", exp.getExporter(), bundle_4);
			} else if (exportPackageName.equals("javax.xml.parsers")) {
				assertNotNull("Package [javax] is not wired when it should be ", exp.getExporter());
				assertEquals("Package [javax] is wired incorrectly ", exp.getExporter(), bundle_6);
			}
		} // end for
	} // end method

	public void checkWiringState_2() {
	} // end method

	public void checkWiringState_3() {
	} // end method

	public void checkWiringState_4() {
	} // end method

	public void checkWiringState_5() {
	} // end method

	public void checkWiringState_6() {
		ExportPackageDescription[] exports = bundle_6.getResolvedImports();
		assertNotNull("export array is unexpectedly null", exports);
		assertTrue("export array is unexpectedly empty", exports.length > 0);
		for (int i = 0; i < exports.length; i++) {
			ExportPackageDescription exp = exports[i];
			String exportPackageName = exp.getName();
			assertNotNull("package name is null", exportPackageName);
			if (exportPackageName.equals("org.w3c.dom")) {
				assertNotNull("Package [org.w3c.dom] is not wired when it should be ", exp.getExporter());
				assertEquals("Package [org.w3c.dom] is wired incorrectly ", exp.getExporter(), bundle_4);
			} else if (exportPackageName.equals("org.xml.sax")) {
				assertNotNull("Package [org.xml.sax] is not wired when it should be ", exp.getExporter());
				assertEquals("Package [org.xml.sax] is wired incorrectly ", exp.getExporter(), bundle_3);
			}
		} // end for
	} // end method

	public void checkWiring_a() {
		checkWiringState_1();
		checkWiringState_2();
		checkWiringState_3();
		checkWiringState_4();
		checkWiringState_5();
		checkWiringState_6();
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
	} // end method

	
	public void checkBundlesResolved_a() {
		assertTrue("unexpected bundle resolution state", bundle_1.isResolved());
		assertTrue("unexpected bundle resolution state", !bundle_2.isResolved());
		assertTrue("unexpected bundle resolution state", bundle_3.isResolved());
		assertTrue("unexpected bundle resolution state", bundle_4.isResolved());
		assertTrue("unexpected bundle resolution state", bundle_5.isResolved());
		assertTrue("unexpected bundle resolution state", bundle_6.isResolved());
	} // end method

	
	public BundleDescription create_bundle_1(StateObjectFactory sof) {
		java.util.Dictionary dictionary_1 = new java.util.Properties();
		BundleDescription bundle = null;
		dictionary_1.put("Bundle-ManifestVersion", "2");
		dictionary_1.put("Bundle-SymbolicName", "Client A");
		dictionary_1.put("Import-Package", "org.xml.sax; version=\"[1.3.0, 1.3.0]\", org.w3c.dom; version=\"[2.1.0, 2.1.0]\", javax.xml.parsers; version=\"[1.1.0, 1.1.0]\"");
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
		dictionary_2.put("Bundle-SymbolicName", "Client B");
		dictionary_2.put("Import-Package", "org.xml.sax; version=\"[1.3.0, 1.3.0]\", org.w3c.dom; version=\"[2.2.0, 2.2.0]\", javax.xml.parsers; version=\"[1.1.0, 1.1.0]\"");
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
		dictionary_3.put("Bundle-SymbolicName", "SAX");
		dictionary_3.put("Export-Package", "org.xml.sax; version=1.3.0");
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
		dictionary_4.put("Bundle-SymbolicName", "DOM A");
		dictionary_4.put("Export-Package", "org.w3c.dom; version=2.1.0");
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
		dictionary_5.put("Bundle-SymbolicName", "DOM B");
		dictionary_5.put("Export-Package", "org.w3c.dom; version=2.2.0");
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
		dictionary_6.put("Bundle-SymbolicName", "JAXP");
		dictionary_6.put("Export-Package", "javax.xml.parsers; version=1.1.0; grouping:=jaxp");
		dictionary_6.put("Import-Package", "org.w3c.dom; version=\"[2.1.0, 2.1.0]\"; grouping:=jaxp, org.xml.sax; version=\"[1.3.0, 1.3.0]\"; grouping:=jaxp");
		try {
			bundle = sof.createBundleDescription(dictionary_6, "bundle_6", 6);
		} catch (BundleException be) {
			fail(be.getMessage());
		}
		return bundle;
	} // end of method
	
} // end of testcase
