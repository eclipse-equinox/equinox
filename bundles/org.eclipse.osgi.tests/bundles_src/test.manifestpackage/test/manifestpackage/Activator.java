/*******************************************************************************
 * Copyright (c) 2009, 2016 IBM Corporation and others.
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
package test.manifestpackage;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import test.manifestpackage.a.A;
import test.manifestpackage.b.B;
import test.manifestpackage.c.C;
import test.manifestpackage.d.D;
import test.manifestpackage.e.E;
import test.manifestpackage.f.F;

public class Activator implements BundleActivator {

	public void start(BundleContext context) throws Exception {
		Package thisPkg = getClass().getPackage();
		assertNotNull("thisPkg", thisPkg);
		checkPackage(thisPkg, "main", "1.0", "equinox.main", "MAIN", "10.0", "equinox.main");

		Package aPkg = A.class.getPackage();
		assertNotNull("aPkg", aPkg);
		checkPackage(aPkg, "a", "1.1", "equinox.a", "A", "11.0", "equinox.a");

		Package bPkg = B.class.getPackage();
		assertNotNull("bPkg", bPkg);
		checkPackage(bPkg, "b", "1.2", "equinox.b", "B", "12.0", "equinox.b");

		Package cPkg = C.class.getPackage();
		assertNotNull("cPkg", cPkg);
		checkPackage(cPkg, "c", "1.3", "equinox.c", "C", "13.0", "equinox.c");

		Package dPkg = D.class.getPackage();
		assertNotNull("dPkg", dPkg);
		checkPackage(dPkg, "d", "1.0", "equinox.main", "D", "10.0", "equinox.main");

		Package ePkg = E.class.getPackage();
		assertNotNull("ePkg", ePkg);
		checkPackage(ePkg, "main", "1.5", "equinox.main", "MAIN", "15.0", "equinox.main");

		Package fPkg = F.class.getPackage();
		assertNotNull("fPkg", fPkg);
		checkPackage(fPkg, "main", "1.0", "equinox.f", "MAIN", "10.0", "equinox.f");
	}

	private void checkPackage(Package pkg, String specTitle, String specVersion, String specVendor, String implTitle,
			String implVersion, String implVendor) {
		assertEquals(specTitle, pkg.getSpecificationTitle());
		assertEquals(specVersion, pkg.getSpecificationVersion());
		assertEquals(specVendor, pkg.getSpecificationVendor());
		assertEquals(implTitle, pkg.getImplementationTitle());
		assertEquals(implVersion, pkg.getImplementationVersion());
		assertEquals(implVendor, pkg.getImplementationVendor());
	}

	private void assertEquals(String expected, String actual) {
		if (!expected.equals(actual))
			throw new RuntimeException("Expected: \"" + expected + "\" but got: \"" + actual + "\"");
	}

	private void assertNotNull(String msg, Package pkg) {
		if (pkg == null)
			throw new RuntimeException(msg);
	}

	public void stop(BundleContext context) throws Exception {
		// nothing
	}
}
