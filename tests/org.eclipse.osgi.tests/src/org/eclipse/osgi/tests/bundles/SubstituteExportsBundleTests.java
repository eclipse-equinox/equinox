/*******************************************************************************
 * Copyright (c) 2008, 2022 IBM Corporation and others.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;

public class SubstituteExportsBundleTests extends AbstractBundleTests {

	@Test
	public void testSubstituteExports01x() throws Exception {
		Bundle a = installer.installBundle("substitutes.a"); //$NON-NLS-1$
		Bundle b = installer.installBundle("substitutes.b"); //$NON-NLS-1$
		Bundle c = installer.installBundle("substitutes.c"); //$NON-NLS-1$
		Bundle d = installer.installBundle("substitutes.d"); //$NON-NLS-1$

		String className = "substitutes.x.Ax"; //$NON-NLS-1$
		Class<?> ax = a.loadClass(className);
		assertEquals("class from b is wrong", ax, b.loadClass(className)); //$NON-NLS-1$
		assertEquals("class from c is wrong", ax, c.loadClass(className)); //$NON-NLS-1$
		assertEquals("class from d is wrong", ax, d.loadClass(className)); //$NON-NLS-1$
	}

	@Test
	public void testSubstituteExports01y() throws Exception {
		Bundle a = installer.installBundle("substitutes.a"); //$NON-NLS-1$
		Bundle b = installer.installBundle("substitutes.b"); //$NON-NLS-1$
		Bundle c = installer.installBundle("substitutes.c"); //$NON-NLS-1$
		Bundle d = installer.installBundle("substitutes.d"); //$NON-NLS-1$

		String className = "substitutes.y.Ay"; //$NON-NLS-1$
		Class<?> ax = a.loadClass(className);
		assertEquals("class from b is wrong", ax, b.loadClass(className)); //$NON-NLS-1$
		assertEquals("class from c is wrong", ax, c.loadClass(className)); //$NON-NLS-1$
		assertEquals("class from d is wrong", ax, d.loadClass(className)); //$NON-NLS-1$
	}

	@Test
	public void testSubstituteExports02() throws Exception {
		Bundle a = installer.installBundle("substitutes.a"); //$NON-NLS-1$
		Bundle b = installer.installBundle("substitutes.b"); //$NON-NLS-1$
		Bundle e = installer.installBundle("substitutes.e"); //$NON-NLS-1$
		Bundle f = installer.installBundle("substitutes.f"); //$NON-NLS-1$

		String className = "substitutes.x.Ax"; //$NON-NLS-1$
		Class<?> ax = a.loadClass(className);
		assertEquals("class from b is wrong", ax, b.loadClass(className)); //$NON-NLS-1$
		assertEquals("class from c is wrong", ax, e.loadClass(className)); //$NON-NLS-1$
		assertEquals("class from d is wrong", ax, f.loadClass(className)); //$NON-NLS-1$
	}

	@Test
	public void testSubstituteExports03() throws Exception {
		Bundle a = installer.installBundle("substitutes.a"); //$NON-NLS-1$
		Bundle b = installer.installBundle("substitutes.b"); //$NON-NLS-1$
		Bundle e = installer.installBundle("substitutes.e"); //$NON-NLS-1$
		Bundle f = installer.installBundle("substitutes.f"); //$NON-NLS-1$
		Bundle g = installer.installBundle("substitutes.g"); //$NON-NLS-1$
		Bundle h = installer.installBundle("substitutes.h"); //$NON-NLS-1$

		String className = "substitutes.x.Ax"; //$NON-NLS-1$
		Class<?> ax = a.loadClass(className);
		assertEquals("class from b is wrong", ax, b.loadClass(className)); //$NON-NLS-1$
		assertEquals("class from c is wrong", ax, e.loadClass(className)); //$NON-NLS-1$
		assertEquals("class from d is wrong", ax, f.loadClass(className)); //$NON-NLS-1$
		assertEquals("class from d is wrong", ax, g.loadClass(className)); //$NON-NLS-1$
		assertEquals("class from d is wrong", ax, h.loadClass(className)); //$NON-NLS-1$
	}

	@Test
	public void testSubstituteExports04() throws Exception {
		Bundle a = installer.installBundle("substitutes.a"); //$NON-NLS-1$
		installer.installBundle("substitutes.a.frag"); //$NON-NLS-1$
		Bundle b = installer.installBundle("substitutes.b"); //$NON-NLS-1$
		installer.installBundle("substitutes.b.frag"); //$NON-NLS-1$
		Bundle c = installer.installBundle("substitutes.c"); //$NON-NLS-1$
		Bundle d = installer.installBundle("substitutes.d"); //$NON-NLS-1$

		String className = "substitutes.x.Ax"; //$NON-NLS-1$
		Class<?> ax = a.loadClass(className);
		assertEquals("class from b is wrong", ax, b.loadClass(className)); //$NON-NLS-1$
		assertEquals("class from c is wrong", ax, c.loadClass(className)); //$NON-NLS-1$
		assertEquals("class from d is wrong", ax, d.loadClass(className)); //$NON-NLS-1$

		String className2 = "substitutes.q.AFq"; //$NON-NLS-1$
		Class<?> aq = a.loadClass(className2);
		assertEquals("class from b is wrong", aq, b.loadClass(className2)); //$NON-NLS-1$
		assertEquals("class from c is wrong", aq, c.loadClass(className2)); //$NON-NLS-1$
		assertEquals("class from d is wrong", aq, d.loadClass(className2)); //$NON-NLS-1$
	}

	@Test
	public void testSubstituteExports05() throws Exception {
		Bundle a = installer.installBundle("substitutes.a"); //$NON-NLS-1$
		installer.installBundle("substitutes.a.frag"); //$NON-NLS-1$
		Bundle b = installer.installBundle("substitutes.b"); //$NON-NLS-1$
		installer.installBundle("substitutes.b.frag"); //$NON-NLS-1$
		Bundle e = installer.installBundle("substitutes.e"); //$NON-NLS-1$
		Bundle f = installer.installBundle("substitutes.f"); //$NON-NLS-1$
		Bundle g = installer.installBundle("substitutes.g"); //$NON-NLS-1$
		Bundle h = installer.installBundle("substitutes.h"); //$NON-NLS-1$

		String className = "substitutes.x.Ax"; //$NON-NLS-1$
		Class<?> ax = a.loadClass(className);
		assertEquals("class from b is wrong", ax, b.loadClass(className)); //$NON-NLS-1$
		assertEquals("class from c is wrong", ax, e.loadClass(className)); //$NON-NLS-1$
		assertEquals("class from d is wrong", ax, f.loadClass(className)); //$NON-NLS-1$
		assertEquals("class from d is wrong", ax, g.loadClass(className)); //$NON-NLS-1$
		assertEquals("class from d is wrong", ax, h.loadClass(className)); //$NON-NLS-1$

		String className2 = "substitutes.q.AFq"; //$NON-NLS-1$
		Class<?> aq = a.loadClass(className2);
		assertEquals("class from b is wrong", aq, b.loadClass(className2)); //$NON-NLS-1$
		assertEquals("class from c is wrong", aq, e.loadClass(className2)); //$NON-NLS-1$
		assertEquals("class from d is wrong", aq, f.loadClass(className2)); //$NON-NLS-1$
		assertEquals("class from d is wrong", aq, g.loadClass(className2)); //$NON-NLS-1$
		assertEquals("class from d is wrong", aq, h.loadClass(className2)); //$NON-NLS-1$
	}

	@Test
	public void testSubstituteExports06() throws Exception {
		Bundle iBundle = installer.installBundle("substitutes.i"); //$NON-NLS-1$
		Bundle jBundle = installer.installBundle("substitutes.j"); //$NON-NLS-1$
		Bundle kBundle = installer.installBundle("substitutes.k"); //$NON-NLS-1$
		Bundle lBundle = installer.installBundle("substitutes.l"); //$NON-NLS-1$
		Bundle mBundle = installer.installBundle("substitutes.m"); //$NON-NLS-1$
		Bundle nBundle = installer.installBundle("substitutes.n"); //$NON-NLS-1$
		Bundle pBundle = installer.installBundle("substitutes.p"); //$NON-NLS-1$
		Bundle qBundle = installer.installBundle("substitutes.q"); //$NON-NLS-1$
		Bundle[] allBundles = new Bundle[] { iBundle, jBundle, kBundle, lBundle, mBundle, nBundle, pBundle, qBundle };

		String classNameIx = "substitutes.x.Ix"; //$NON-NLS-1$
		String classNameKx = "substitutes.x.Kx"; //$NON-NLS-1$
		String classNameMx = "substitutes.x.Mx"; //$NON-NLS-1$
		String classNameIy = "substitutes.y.Iy"; //$NON-NLS-1$
		String classNameKy = "substitutes.y.Ky"; //$NON-NLS-1$
		String classNameMy = "substitutes.y.My"; //$NON-NLS-1$

		Class<?> iX = iBundle.loadClass(classNameIx);
		assertEquals("jBundle has different copy of iX", iX, jBundle.loadClass(classNameIx)); //$NON-NLS-1$
		assertEquals("mBundle has different copy of iX", iX, mBundle.loadClass(classNameIx)); //$NON-NLS-1$
		assertEquals("nBundle has different copy of iX", iX, nBundle.loadClass(classNameIx)); //$NON-NLS-1$
		assertEquals("pBundle has different copy of iX", iX, pBundle.loadClass(classNameIx)); //$NON-NLS-1$
		assertEquals("qBundle has different copy of iX", iX, qBundle.loadClass(classNameIx)); //$NON-NLS-1$

		Class<?> iY = iBundle.loadClass(classNameIy);
		assertEquals("jBundle has different copy of iY", iY, jBundle.loadClass(classNameIy)); //$NON-NLS-1$
		assertEquals("mBundle has different copy of iY", iY, mBundle.loadClass(classNameIy)); //$NON-NLS-1$
		assertEquals("nBundle has different copy of iY", iY, nBundle.loadClass(classNameIy)); //$NON-NLS-1$
		assertEquals("pBundle has different copy of iY", iY, pBundle.loadClass(classNameIy)); //$NON-NLS-1$
		assertEquals("qBundle has different copy of iY", iY, qBundle.loadClass(classNameIy)); //$NON-NLS-1$

		Class<?> kX = kBundle.loadClass(classNameKx);
		assertEquals("lBundle has different copy of Kx", kX, lBundle.loadClass(classNameKx)); //$NON-NLS-1$
		assertEquals("mBundle has different copy of Kx", kX, mBundle.loadClass(classNameKx)); //$NON-NLS-1$
		assertEquals("nBundle has different copy of Kx", kX, nBundle.loadClass(classNameKx)); //$NON-NLS-1$
		assertEquals("pBundle has different copy of Kx", kX, pBundle.loadClass(classNameKx)); //$NON-NLS-1$
		assertEquals("qBundle has different copy of Kx", kX, qBundle.loadClass(classNameKx)); //$NON-NLS-1$

		Class<?> kY = kBundle.loadClass(classNameKy);
		assertEquals("lBundle has different copy of Ky", kY, lBundle.loadClass(classNameKy)); //$NON-NLS-1$
		assertEquals("mBundle has different copy of Ky", kY, mBundle.loadClass(classNameKy)); //$NON-NLS-1$
		assertEquals("nBundle has different copy of Ky", kY, nBundle.loadClass(classNameKy)); //$NON-NLS-1$
		assertEquals("pBundle has different copy of Ky", kY, pBundle.loadClass(classNameKy)); //$NON-NLS-1$
		assertEquals("qBundle has different copy of Ky", kY, qBundle.loadClass(classNameKy)); //$NON-NLS-1$

		Class<?> mX = mBundle.loadClass(classNameMx);
		assertEquals("nBundle has different copy of mX", mX, nBundle.loadClass(classNameMx)); //$NON-NLS-1$
		assertEquals("pBundle has different copy of mX", mX, pBundle.loadClass(classNameMx)); //$NON-NLS-1$
		assertEquals("qBundle has different copy of mX", mX, qBundle.loadClass(classNameMx)); //$NON-NLS-1$

		Class<?> mY = mBundle.loadClass(classNameMy);
		assertEquals("nBundle has different copy of mY", mY, nBundle.loadClass(classNameMy)); //$NON-NLS-1$
		assertEquals("pBundle has different copy of mY", mY, pBundle.loadClass(classNameMy)); //$NON-NLS-1$
		assertEquals("qBundle has different copy of mY", mY, qBundle.loadClass(classNameMy)); //$NON-NLS-1$

		String[] unexpectedClasseNames = new String[] { "substitutes.x.Jx", "substitutes.x.Lx", "substitutes.x.Nx", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				"substitutes.y.Jy", "substitutes.y.Ly", "substitutes.y.Ny" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		for (String unexpectedClasseName : unexpectedClasseNames) {
			for (Bundle bundle : allBundles) {
				assertThrows("Found class " + unexpectedClasseName + " in bundle " + bundle,
						ClassNotFoundException.class, () -> bundle.loadClass(unexpectedClasseName));
			}
		}
	}

	@Test
	public void testSubstituteExports07() throws Exception {
		// same as previous split test but bundles are installed in opposite order to force the opposite classes to load
		Bundle jBundle = installer.installBundle("substitutes.j"); //$NON-NLS-1$
		Bundle iBundle = installer.installBundle("substitutes.i"); //$NON-NLS-1$
		Bundle lBundle = installer.installBundle("substitutes.l"); //$NON-NLS-1$
		Bundle kBundle = installer.installBundle("substitutes.k"); //$NON-NLS-1$
		Bundle nBundle = installer.installBundle("substitutes.n"); //$NON-NLS-1$
		Bundle mBundle = installer.installBundle("substitutes.m"); //$NON-NLS-1$
		Bundle qBundle = installer.installBundle("substitutes.q"); //$NON-NLS-1$
		Bundle pBundle = installer.installBundle("substitutes.p"); //$NON-NLS-1$
		Bundle[] allBundles = new Bundle[] { iBundle, jBundle, kBundle, lBundle, mBundle, nBundle, pBundle, qBundle };

		String classNameJx = "substitutes.x.Jx"; //$NON-NLS-1$
		String classNameLx = "substitutes.x.Lx"; //$NON-NLS-1$
		String classNameNx = "substitutes.x.Nx"; //$NON-NLS-1$
		String classNameJy = "substitutes.y.Jy"; //$NON-NLS-1$
		String classNameLy = "substitutes.y.Ly"; //$NON-NLS-1$
		String classNameNy = "substitutes.y.Ny"; //$NON-NLS-1$

		Class<?> jX = jBundle.loadClass(classNameJx);
		assertEquals("iBundle has different copy of Jx", jX, iBundle.loadClass(classNameJx)); //$NON-NLS-1$
		assertEquals("mBundle has different copy of Jx", jX, mBundle.loadClass(classNameJx)); //$NON-NLS-1$
		assertEquals("nBundle has different copy of Jx", jX, nBundle.loadClass(classNameJx)); //$NON-NLS-1$
		assertEquals("pBundle has different copy of Jx", jX, pBundle.loadClass(classNameJx)); //$NON-NLS-1$
		assertEquals("qBundle has different copy of Jx", jX, qBundle.loadClass(classNameJx)); //$NON-NLS-1$

		Class<?> jY = jBundle.loadClass(classNameJy);
		assertEquals("jBundle has different copy of Jy", jY, iBundle.loadClass(classNameJy)); //$NON-NLS-1$
		assertEquals("mBundle has different copy of Jy", jY, mBundle.loadClass(classNameJy)); //$NON-NLS-1$
		assertEquals("nBundle has different copy of Jy", jY, nBundle.loadClass(classNameJy)); //$NON-NLS-1$
		assertEquals("pBundle has different copy of Jy", jY, pBundle.loadClass(classNameJy)); //$NON-NLS-1$
		assertEquals("qBundle has different copy of Jy", jY, qBundle.loadClass(classNameJy)); //$NON-NLS-1$

		Class<?> lX = lBundle.loadClass(classNameLx);
		assertEquals("lBundle has different copy of Lx", lX, kBundle.loadClass(classNameLx)); //$NON-NLS-1$
		assertEquals("mBundle has different copy of Lx", lX, mBundle.loadClass(classNameLx)); //$NON-NLS-1$
		assertEquals("nBundle has different copy of Lx", lX, nBundle.loadClass(classNameLx)); //$NON-NLS-1$
		assertEquals("pBundle has different copy of Lx", lX, pBundle.loadClass(classNameLx)); //$NON-NLS-1$
		assertEquals("qBundle has different copy of Lx", lX, qBundle.loadClass(classNameLx)); //$NON-NLS-1$

		Class<?> lY = lBundle.loadClass(classNameLy);
		assertEquals("lBundle has different copy of Ly", lY, kBundle.loadClass(classNameLy)); //$NON-NLS-1$
		assertEquals("mBundle has different copy of Ly", lY, mBundle.loadClass(classNameLy)); //$NON-NLS-1$
		assertEquals("nBundle has different copy of Ly", lY, nBundle.loadClass(classNameLy)); //$NON-NLS-1$
		assertEquals("pBundle has different copy of Ly", lY, pBundle.loadClass(classNameLy)); //$NON-NLS-1$
		assertEquals("qBundle has different copy of Ly", lY, qBundle.loadClass(classNameLy)); //$NON-NLS-1$

		Class<?> nX = nBundle.loadClass(classNameNx);
		assertEquals("nBundle has different copy of Nx", nX, mBundle.loadClass(classNameNx)); //$NON-NLS-1$
		assertEquals("pBundle has different copy of Nx", nX, pBundle.loadClass(classNameNx)); //$NON-NLS-1$
		assertEquals("qBundle has different copy of Nx", nX, qBundle.loadClass(classNameNx)); //$NON-NLS-1$

		Class<?> nY = nBundle.loadClass(classNameNy);
		assertEquals("nBundle has different copy of Ny", nY, mBundle.loadClass(classNameNy)); //$NON-NLS-1$
		assertEquals("pBundle has different copy of Ny", nY, pBundle.loadClass(classNameNy)); //$NON-NLS-1$
		assertEquals("qBundle has different copy of Ny", nY, qBundle.loadClass(classNameNy)); //$NON-NLS-1$

		String[] unexpectedClasseNames = new String[] {"substitutes.x.Ix", "substitutes.x.Kx", "substitutes.x.Mx", "substitutes.y.Iy", "substitutes.y.Ky", "substitutes.y.My"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		for (String unexpectedClasseName : unexpectedClasseNames) {
			for (Bundle bundle : allBundles) {
				assertThrows("Found class " + unexpectedClasseName + " in bundle " + bundle,
						ClassNotFoundException.class, () -> bundle.loadClass(unexpectedClasseName));
			}
		}
	}

	@Test
	public void testSubstituteExports08() throws BundleException {
		Bundle a = installer.installBundle("substitutes.a"); //$NON-NLS-1$
		Bundle b = installer.installBundle("substitutes.b"); //$NON-NLS-1$
		Bundle c = installer.installBundle("substitutes.c"); //$NON-NLS-1$
		Bundle d = installer.installBundle("substitutes.d"); //$NON-NLS-1$
		Bundle[] allBundles = new Bundle[] { a, b, c, d };
		doRefreshTest(allBundles, a);
	}

	@Test
	public void testSubstituteExports09() throws BundleException {
		Bundle a = installer.installBundle("substitutes.a"); //$NON-NLS-1$
		Bundle b = installer.installBundle("substitutes.b"); //$NON-NLS-1$
		Bundle e = installer.installBundle("substitutes.e"); //$NON-NLS-1$
		Bundle f = installer.installBundle("substitutes.f"); //$NON-NLS-1$
		Bundle[] allBundles = new Bundle[] { a, b, e, f };
		doRefreshTest(allBundles, a);
	}

	@Test
	public void testSubstituteExports10() throws BundleException {
		Bundle a = installer.installBundle("substitutes.a"); //$NON-NLS-1$
		Bundle b = installer.installBundle("substitutes.b"); //$NON-NLS-1$
		Bundle e = installer.installBundle("substitutes.e"); //$NON-NLS-1$
		Bundle f = installer.installBundle("substitutes.f"); //$NON-NLS-1$
		Bundle g = installer.installBundle("substitutes.g"); //$NON-NLS-1$
		Bundle h = installer.installBundle("substitutes.h"); //$NON-NLS-1$
		Bundle[] allBundles = new Bundle[] { a, b, e, f, g, h };
		doRefreshTest(allBundles, a);
	}

	@Test
	public void testSubstituteExports11() throws BundleException {
		Bundle iBundle = installer.installBundle("substitutes.i"); //$NON-NLS-1$
		Bundle jBundle = installer.installBundle("substitutes.j"); //$NON-NLS-1$
		installer.installBundle("substitutes.k"); //$NON-NLS-1$
		installer.installBundle("substitutes.l"); //$NON-NLS-1$
		Bundle mBundle = installer.installBundle("substitutes.m"); //$NON-NLS-1$
		Bundle nBundle = installer.installBundle("substitutes.n"); //$NON-NLS-1$
		Bundle pBundle = installer.installBundle("substitutes.p"); //$NON-NLS-1$
		Bundle qBundle = installer.installBundle("substitutes.q"); //$NON-NLS-1$
		Bundle[] allBundles = new Bundle[] {iBundle, jBundle, mBundle, nBundle, pBundle, qBundle}; // k and l do not depend on i
		doRefreshTest(allBundles, iBundle);
	}

	@Test
	public void testSubstituteExports12() throws BundleException {
		Bundle jBundle = installer.installBundle("substitutes.j"); //$NON-NLS-1$
		Bundle iBundle = installer.installBundle("substitutes.i"); //$NON-NLS-1$
		installer.installBundle("substitutes.l"); //$NON-NLS-1$
		installer.installBundle("substitutes.k"); //$NON-NLS-1$
		Bundle nBundle = installer.installBundle("substitutes.n"); //$NON-NLS-1$
		Bundle mBundle = installer.installBundle("substitutes.m"); //$NON-NLS-1$
		Bundle qBundle = installer.installBundle("substitutes.q"); //$NON-NLS-1$
		Bundle pBundle = installer.installBundle("substitutes.p"); //$NON-NLS-1$
		Bundle[] allBundles = new Bundle[] {iBundle, jBundle, mBundle, nBundle, pBundle, qBundle}; // k and l do not depend on j
		doRefreshTest(allBundles, jBundle);
	}

	private void doRefreshTest(Bundle[] allBundles, Bundle toRefresh) {
		installer.resolveBundles(allBundles);
		Bundle[] refreshed = installer.refreshPackages(new Bundle[] { toRefresh });
		for (Bundle allBundle : allBundles) {
			boolean found = false;
			for (int j = 0; j < refreshed.length && !found; j++) {
				found = allBundle == refreshed[j];
			}
			assertTrue("bundle did not get refreshed: " + allBundle, found);
		}
		assertEquals("Wrong number of bundles refreshed", allBundles.length, refreshed.length); //$NON-NLS-1$
	}

	@Test
	public void testSubstituteExports13() throws BundleException {
		Bundle a = installer.installBundle("substitutes.a"); //$NON-NLS-1$
		Bundle b = installer.installBundle("substitutes.b"); //$NON-NLS-1$
		Bundle c = installer.installBundle("substitutes.c"); //$NON-NLS-1$
		Bundle d = installer.installBundle("substitutes.d"); //$NON-NLS-1$
		Bundle[] allBundles = new Bundle[] { a, b, c, d };
		assertTrue("Bundles are not resolved", installer.resolveBundles(allBundles)); //$NON-NLS-1$

		PackageAdmin pa = installer.getPackageAdmin();

		ExportedPackage[] xPackages = pa.getExportedPackages("substitutes.x"); //$NON-NLS-1$
		assertNotNull("xPackages is null", xPackages); //$NON-NLS-1$
		assertEquals("xPackages wrong number", 1, xPackages.length); //$NON-NLS-1$
		assertEquals("Wrong exporter", a, xPackages[0].getExportingBundle()); //$NON-NLS-1$
		Bundle[] xImporters = xPackages[0].getImportingBundles();
		assertNotNull("xImporters is null", xImporters); //$NON-NLS-1$
		assertEquals("Wrong number of xImporters", 3, xImporters.length); //$NON-NLS-1$

		ExportedPackage[] yPackages = pa.getExportedPackages("substitutes.y"); //$NON-NLS-1$
		assertNotNull("yPackages is null", yPackages); //$NON-NLS-1$
		assertEquals("yPackages wrong number", 1, yPackages.length); //$NON-NLS-1$
		assertEquals("Wrong exporter", a, yPackages[0].getExportingBundle()); //$NON-NLS-1$
		Bundle[] yImporters = yPackages[0].getImportingBundles();
		assertNotNull("yImporters is null", yImporters); //$NON-NLS-1$
		assertEquals("Wrong number of yImporters", 3, yImporters.length); //$NON-NLS-1$

		Bundle[] expectedImporters = new Bundle[] { b, c, d };
		for (Bundle expectedImporter : expectedImporters) {
			contains("xPackages importers does not contain", xImporters, expectedImporter); //$NON-NLS-1$
			contains("yPackages importers does not contain", yImporters, expectedImporter); //$NON-NLS-1$
		}
	}

	@Test
	public void testSubstituteExports14() throws BundleException {
		Bundle iBundle = installer.installBundle("substitutes.i"); //$NON-NLS-1$
		Bundle jBundle = installer.installBundle("substitutes.j"); //$NON-NLS-1$
		Bundle kBundle = installer.installBundle("substitutes.k"); //$NON-NLS-1$
		Bundle lBundle = installer.installBundle("substitutes.l"); //$NON-NLS-1$
		Bundle mBundle = installer.installBundle("substitutes.m"); //$NON-NLS-1$
		Bundle nBundle = installer.installBundle("substitutes.n"); //$NON-NLS-1$
		Bundle pBundle = installer.installBundle("substitutes.p"); //$NON-NLS-1$
		Bundle qBundle = installer.installBundle("substitutes.q"); //$NON-NLS-1$
		Bundle[] allBundles = new Bundle[] {iBundle, jBundle, kBundle, lBundle, mBundle, nBundle, pBundle, qBundle}; // k and l do not depend on i
		assertTrue("Bundles are not resolved", installer.resolveBundles(allBundles)); //$NON-NLS-1$

		PackageAdmin pa = installer.getPackageAdmin();

		ExportedPackage[] xPackages = pa.getExportedPackages("substitutes.x"); //$NON-NLS-1$
		assertNotNull("xPackages is null", xPackages); //$NON-NLS-1$
		assertEquals("xPackages wrong number", 3, xPackages.length); //$NON-NLS-1$

		ExportedPackage[] yPackages = pa.getExportedPackages("substitutes.y"); //$NON-NLS-1$
		assertNotNull("yPackages is null", yPackages); //$NON-NLS-1$
		assertEquals("yPackages wrong number", 3, yPackages.length); //$NON-NLS-1$

		Bundle[] expectedExporters = new Bundle[] { iBundle, kBundle, mBundle };
		for (Bundle expectedExporter : expectedExporters) {
			boolean found = false;
			for (int j = 0; j < xPackages.length && !found; j++) {
				found = expectedExporter == xPackages[j].getExportingBundle();
				if (found) {
					Bundle[] importingBundles = xPackages[j].getImportingBundles();
					Bundle[] expectedImporters = null;
					String message = null;
					if (expectedExporter == iBundle) {
						expectedImporters = new Bundle[] { jBundle, mBundle, nBundle, pBundle, qBundle };
						message = "iBundle "; //$NON-NLS-1$
					} else if (expectedExporter == kBundle) {
						expectedImporters = new Bundle[] { lBundle, mBundle, nBundle, pBundle, qBundle };
						message = "kBundle "; //$NON-NLS-1$
					} else if (expectedExporter == mBundle) {
						expectedImporters = new Bundle[] { nBundle, pBundle, qBundle };
						message = "mBundle "; //$NON-NLS-1$
					}
					assertEquals(message, expectedImporters.length, importingBundles.length);
					for (Bundle expectedImporter : expectedImporters) {
						contains(message, importingBundles, expectedImporter);
					}
				}
			}
		}

	}

	private void contains(String message, Bundle[] bundles, Bundle b) {
		boolean found = false;
		for (int i = 0; i < bundles.length && !found; i++)
			found = bundles[i] == b;
		assertTrue(message + b, found);
	}
}
