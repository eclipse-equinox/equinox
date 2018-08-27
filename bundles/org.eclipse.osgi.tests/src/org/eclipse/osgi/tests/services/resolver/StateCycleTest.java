/*******************************************************************************
 * Copyright (c) 2004, 2007 IBM Corporation and others.
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
package org.eclipse.osgi.tests.services.resolver;

import java.util.Hashtable;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public class StateCycleTest extends AbstractStateTest {

	public static Test suite() {
		return new TestSuite(StateCycleTest.class);
	}

	public StateCycleTest(String testName) {
		super(testName);
	}

	public void testCycle1() throws BundleException {
		State state1 = buildEmptyState();
		String A_MANIFEST = "Bundle-SymbolicName: org.eclipse.a\nBundle-Version: 1.0\n";
		state1.addBundle(state1.getFactory().createBundleDescription(parseManifest(A_MANIFEST), "org.eclipse.a", 1));
		String B_MANIFEST = "Bundle-SymbolicName: org.eclipse.b\nBundle-Version: 1.0\nRequire-Bundle: org.eclipse.a,org.eclipse.c\n";
		state1.addBundle(state1.getFactory().createBundleDescription(parseManifest(B_MANIFEST), "org.eclipse.b", 2));
		String C_MANIFEST = "Bundle-SymbolicName: org.eclipse.c\nBundle-Version: 1.0\nRequire-Bundle: org.eclipse.a,org.eclipse.b\n";
		state1.addBundle(state1.getFactory().createBundleDescription(parseManifest(C_MANIFEST), "org.eclipse.c", 3));
		String D_MANIFEST = "Bundle-SymbolicName: org.eclipse.d\nBundle-Version: 1.0\nRequire-Bundle: org.eclipse.a,org.eclipse.c\n";
		state1.addBundle(state1.getFactory().createBundleDescription(parseManifest(D_MANIFEST), "org.eclipse.d", 4));
		String E_MANIFEST = "Bundle-SymbolicName: org.eclipse.e\nBundle-Version: 1.0\nRequire-Bundle: org.eclipse.a,org.eclipse.b; optional=true\n";
		state1.addBundle(state1.getFactory().createBundleDescription(parseManifest(E_MANIFEST), "org.eclipse.e", 5));
		String F_MANIFEST = "Bundle-SymbolicName: org.eclipse.f\nBundle-Version: 1.0\nRequire-Bundle: org.eclipse.f\n";
		state1.addBundle(state1.getFactory().createBundleDescription(parseManifest(F_MANIFEST), "org.eclipse.f", 6));
		State state = state1;
		state.resolve();
		BundleDescription bundleA = state.getBundleByLocation("org.eclipse.a");
		BundleDescription bundleB = state.getBundleByLocation("org.eclipse.b");
		BundleDescription bundleC = state.getBundleByLocation("org.eclipse.c");
		BundleDescription bundleD = state.getBundleByLocation("org.eclipse.d");
		BundleDescription bundleE = state.getBundleByLocation("org.eclipse.e");
		BundleDescription bundleF = state.getBundleByLocation("org.eclipse.f");
		BundleDescription[] allBundles = state.getBundles();
		assertContains("0.5", allBundles, bundleA);
		assertContains("0.6", allBundles, bundleB);
		assertContains("0.7", allBundles, bundleC);
		assertContains("0.8", allBundles, bundleD);
		assertContains("0.9", allBundles, bundleE);
		assertContains("0.10", allBundles, bundleF);

		// cycles must resolve now
		assertTrue("1.0", bundleA.isResolved());
		assertTrue("2.0", bundleB.isResolved());
		assertTrue("3.0", bundleC.isResolved());
		assertTrue("4.0", bundleD.isResolved());
		assertTrue("5.0", bundleE.isResolved());
		assertTrue("6.0", bundleF.isResolved());
	}

	public void testCycle2() throws BundleException {
		State state1 = buildEmptyState();
		String A_MANIFEST = "Bundle-SymbolicName: org.eclipse.a\nBundle-Version: 1.0\n";
		state1.addBundle(state1.getFactory().createBundleDescription(parseManifest(A_MANIFEST), "org.eclipse.a", 1));
		String B_MANIFEST = "Bundle-SymbolicName: org.eclipse.b\nBundle-Version: 1.0\nRequire-Bundle: org.eclipse.a,org.eclipse.c\n";
		state1.addBundle(state1.getFactory().createBundleDescription(parseManifest(B_MANIFEST), "org.eclipse.b", 2));
		String C_MANIFEST = "Bundle-SymbolicName: org.eclipse.c\nBundle-Version: 1.0\nRequire-Bundle: org.eclipse.a,org.eclipse.d\n";
		state1.addBundle(state1.getFactory().createBundleDescription(parseManifest(C_MANIFEST), "org.eclipse.c", 3));
		String D_MANIFEST = "Bundle-SymbolicName: org.eclipse.d\nBundle-Version: 1.0\nRequire-Bundle: org.eclipse.a,org.eclipse.b\n";
		state1.addBundle(state1.getFactory().createBundleDescription(parseManifest(D_MANIFEST), "org.eclipse.d", 4));
		String E_MANIFEST = "Bundle-SymbolicName: org.eclipse.e\nBundle-Version: 1.0\nRequire-Bundle: org.eclipse.a,org.eclipse.b; optional=true\n";
		state1.addBundle(state1.getFactory().createBundleDescription(parseManifest(E_MANIFEST), "org.eclipse.e", 5));
		String F_MANIFEST = "Bundle-SymbolicName: org.eclipse.f\nBundle-Version: 1.0\nRequire-Bundle: org.eclipse.f\n";
		state1.addBundle(state1.getFactory().createBundleDescription(parseManifest(F_MANIFEST), "org.eclipse.f", 6));
		State state = state1;
		state.resolve();
		BundleDescription bundleA = state.getBundleByLocation("org.eclipse.a");
		BundleDescription bundleB = state.getBundleByLocation("org.eclipse.b");
		BundleDescription bundleC = state.getBundleByLocation("org.eclipse.c");
		BundleDescription bundleD = state.getBundleByLocation("org.eclipse.d");
		BundleDescription bundleE = state.getBundleByLocation("org.eclipse.e");
		BundleDescription bundleF = state.getBundleByLocation("org.eclipse.f");
		BundleDescription[] allBundles = state.getBundles();
		assertContains("0.5", allBundles, bundleA);
		assertContains("0.6", allBundles, bundleB);
		assertContains("0.7", allBundles, bundleC);
		assertContains("0.8", allBundles, bundleD);
		assertContains("0.9", allBundles, bundleE);
		assertContains("0.10", allBundles, bundleF);

		// cycles must resolve now
		assertTrue("1.0", bundleA.isResolved());
		assertTrue("2.0", bundleB.isResolved());
		assertTrue("3.0", bundleC.isResolved());
		assertTrue("4.0", bundleD.isResolved());
		assertTrue("5.0", bundleE.isResolved());
		assertTrue("6.0", bundleF.isResolved());

	}

	public void testCycle3() throws BundleException {
		State state = buildEmptyState();
		String A_MANIFEST = "Bundle-SymbolicName: org.eclipse.a\nBundle-Version: 1.0\n";
		state.addBundle(state.getFactory().createBundleDescription(parseManifest(A_MANIFEST), "org.eclipse.a", 1));
		String B_MANIFEST = "Bundle-SymbolicName: org.eclipse.b\nBundle-Version: 1.0\nRequire-Bundle: org.eclipse.c\n";
		state.addBundle(state.getFactory().createBundleDescription(parseManifest(B_MANIFEST), "org.eclipse.b", 2));
		String C_MANIFEST = "Bundle-SymbolicName: org.eclipse.c\nBundle-Version: 1.0\nRequire-Bundle: org.eclipse.d\n";
		state.addBundle(state.getFactory().createBundleDescription(parseManifest(C_MANIFEST), "org.eclipse.c", 3));
		String D_MANIFEST = "Bundle-SymbolicName: org.eclipse.d\nBundle-Version: 1.0\nRequire-Bundle: org.eclipse.b\n";
		state.addBundle(state.getFactory().createBundleDescription(parseManifest(D_MANIFEST), "org.eclipse.d", 4));

		state.resolve();
		BundleDescription bundleC = state.getBundleByLocation("org.eclipse.c");
		BundleDescription bundleB = state.getBundleByLocation("org.eclipse.b");
		BundleDescription bundleA = state.getBundleByLocation("org.eclipse.a");
		BundleDescription bundleD = state.getBundleByLocation("org.eclipse.d");
		BundleDescription[] allBundles = state.getBundles();
		assertContains("0.5", allBundles, bundleC);
		assertContains("0.6", allBundles, bundleB);
		assertContains("0.7", allBundles, bundleA);
		assertContains("0.8", allBundles, bundleD);

		// cycles must resolve now
		assertTrue("0.9", bundleA.isResolved());
		assertTrue("1.0", bundleC.isResolved());
		assertTrue("2.0", bundleB.isResolved());
		assertTrue("3.0", bundleD.isResolved());
	}

	public void testCycle4() throws BundleException {
		State state = buildEmptyState();
		String A_MANIFEST = "Bundle-SymbolicName: org.eclipse.a\nBundle-Version: 1.0\n";
		state.addBundle(state.getFactory().createBundleDescription(parseManifest(A_MANIFEST), "org.eclipse.a", 1));
		String B_MANIFEST = "Bundle-SymbolicName: org.eclipse.b\nBundle-Version: 1.0\nRequire-Bundle: org.eclipse.c\n";
		state.addBundle(state.getFactory().createBundleDescription(parseManifest(B_MANIFEST), "org.eclipse.b", 2));
		String C_MANIFEST = "Bundle-SymbolicName: org.eclipse.c\nBundle-Version: 1.0\nRequire-Bundle: org.eclipse.d\n";
		state.addBundle(state.getFactory().createBundleDescription(parseManifest(C_MANIFEST), "org.eclipse.c", 3));
		String D_MANIFEST = "Bundle-SymbolicName: org.eclipse.d\nBundle-Version: 1.0\nRequire-Bundle: org.eclipse.b; optional=true\n";
		state.addBundle(state.getFactory().createBundleDescription(parseManifest(D_MANIFEST), "org.eclipse.d", 4));

		state.resolve();
		BundleDescription bundleC = state.getBundleByLocation("org.eclipse.c");
		BundleDescription bundleB = state.getBundleByLocation("org.eclipse.b");
		BundleDescription bundleA = state.getBundleByLocation("org.eclipse.a");
		BundleDescription bundleD = state.getBundleByLocation("org.eclipse.d");
		BundleDescription[] allBundles = state.getBundles();
		assertContains("0.5", allBundles, bundleC);
		assertContains("0.6", allBundles, bundleB);
		assertContains("0.7", allBundles, bundleA);
		assertContains("0.8", allBundles, bundleD);

		// cycles must resolve now
		assertTrue("0.9", bundleA.isResolved());
		assertTrue("1.0", bundleC.isResolved());
		assertTrue("2.0", bundleB.isResolved());
		assertTrue("3.0", bundleD.isResolved());
	}

	public void test185285() throws BundleException {
		// if two versions of the same bundle export and import two different packages at the same version
		// then we should resolve both sets of imports to the first bundle installed.
		State state = buildEmptyState();
		Hashtable manifest = new Hashtable();
		long bundleID = 0;
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "X");
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0");
		manifest.put(Constants.EXPORT_PACKAGE, "foo; version=\"1.0.0\", bar; version=\"1.0.0\"");
		manifest.put(Constants.IMPORT_PACKAGE, "foo, bar");
		BundleDescription a_100 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "X");
		manifest.put(Constants.BUNDLE_VERSION, "1.0.1");
		manifest.put(Constants.EXPORT_PACKAGE, "foo; version=\"1.0.0\", bar; version=\"1.0.0\"");
		manifest.put(Constants.IMPORT_PACKAGE, "foo, bar");
		BundleDescription a_101 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		state.addBundle(a_100);
		state.addBundle(a_101);

		state.resolve();
		assertTrue("1.0", a_100.isResolved());
		assertTrue("1.1", a_101.isResolved());
		ExportPackageDescription[] selectedExportsA100 = a_100.getSelectedExports();
		ExportPackageDescription[] selectedExportsA101 = a_101.getSelectedExports();
		assertTrue("2.0", selectedExportsA100.length == 2);
		assertTrue("2.1", selectedExportsA101.length == 0);
		ExportPackageDescription[] resolvedImportsA100 = a_100.getResolvedImports();
		ExportPackageDescription[] resolvedImportsA101 = a_100.getResolvedImports();
		assertTrue("3.0", resolvedImportsA100.length == 2);
		assertTrue("3.1", resolvedImportsA101.length == 2);
		for (int i = 0; i < resolvedImportsA100.length; i++) {
			assertTrue("3.2.1." + i, selectedExportsA100[i] == resolvedImportsA100[i]);
			assertTrue("3.2.1." + i, selectedExportsA100[i] == resolvedImportsA101[i]);
		}
	}
}
