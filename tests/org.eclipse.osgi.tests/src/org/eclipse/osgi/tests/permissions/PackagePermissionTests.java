/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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
package org.eclipse.osgi.tests.permissions;

import java.security.Permission;
import java.security.PermissionCollection;
import java.util.PropertyPermission;
import org.junit.Test;
import org.osgi.framework.PackagePermission;

public class PackagePermissionTests extends PermissionTests {

	@Test
	public void testPackagePermission() {
		badPackagePermission("a.b.c", "x"); //$NON-NLS-1$ //$NON-NLS-2$
		badPackagePermission("a.b.c", "   get  ,  x   "); //$NON-NLS-1$ //$NON-NLS-2$
		badPackagePermission("a.b.c", ""); //$NON-NLS-1$ //$NON-NLS-2$
		badPackagePermission("a.b.c", "      "); //$NON-NLS-1$ //$NON-NLS-2$
		badPackagePermission("a.b.c", null); //$NON-NLS-1$
		badPackagePermission("a.b.c", ","); //$NON-NLS-1$ //$NON-NLS-2$
		badPackagePermission("a.b.c", ",xxx"); //$NON-NLS-1$ //$NON-NLS-2$
		badPackagePermission("a.b.c", "xxx,"); //$NON-NLS-1$ //$NON-NLS-2$
		badPackagePermission("a.b.c", "import,"); //$NON-NLS-1$ //$NON-NLS-2$
		badPackagePermission("a.b.c", "export,   "); //$NON-NLS-1$ //$NON-NLS-2$
		badPackagePermission("a.b.c", "importme,"); //$NON-NLS-1$ //$NON-NLS-2$
		badPackagePermission("a.b.c", "exportme,"); //$NON-NLS-1$ //$NON-NLS-2$
		badPackagePermission("a.b.c", ",import"); //$NON-NLS-1$ //$NON-NLS-2$
		badPackagePermission("a.b.c", ",export"); //$NON-NLS-1$ //$NON-NLS-2$
		badPackagePermission("a.b.c", "   importme   "); //$NON-NLS-1$ //$NON-NLS-2$
		badPackagePermission("a.b.c", "   exportme     "); //$NON-NLS-1$ //$NON-NLS-2$
		badPackagePermission("a.b.c", "   impor"); //$NON-NLS-1$ //$NON-NLS-2$
		badPackagePermission("a.b.c", "   expor"); //$NON-NLS-1$ //$NON-NLS-2$

		Permission op = new PropertyPermission("java.home", "read"); //$NON-NLS-1$ //$NON-NLS-2$

		PackagePermission p11 = new PackagePermission("com.foo.service1", "    IMPORT,export   "); //$NON-NLS-1$ //$NON-NLS-2$
		PackagePermission p12 = new PackagePermission("com.foo.service1", "EXPORT  ,   import"); //$NON-NLS-1$ //$NON-NLS-2$
		PackagePermission p13 = new PackagePermission("com.foo.service1", "expORT   "); //$NON-NLS-1$ //$NON-NLS-2$
		PackagePermission p14 = new PackagePermission("com.foo.service1", "    Import    "); //$NON-NLS-1$ //$NON-NLS-2$

		shouldImply(p11, p11);
		shouldImply(p11, p12);
		shouldImply(p11, p13);
		shouldImply(p11, p14);

		shouldImply(p12, p11);
		shouldImply(p12, p12);
		shouldImply(p12, p13);
		shouldImply(p12, p14);

		shouldImply(p13, p11);
		shouldImply(p13, p12);
		shouldImply(p13, p13);
		shouldImply(p13, p14);

		shouldImply(p14, p14);

		shouldNotImply(p14, p11);
		shouldNotImply(p14, p12);
		shouldNotImply(p14, p13);

		shouldNotImply(p11, op);

		shouldEqual(p11, p11);
		shouldEqual(p11, p12);
		shouldEqual(p11, p13);
		shouldEqual(p12, p11);
		shouldEqual(p12, p12);
		shouldEqual(p12, p13);
		shouldEqual(p13, p11);
		shouldEqual(p13, p12);
		shouldEqual(p13, p13);

		shouldNotEqual(p11, p14);
		shouldNotEqual(p12, p14);
		shouldNotEqual(p13, p14);
		shouldNotEqual(p14, p11);
		shouldNotEqual(p14, p12);
		shouldNotEqual(p14, p13);

		PermissionCollection pc = p13.newPermissionCollection();

		checkEnumeration(pc.elements(), true);

		shouldNotImply(pc, p11);

		shouldAdd(pc, p14);
		shouldImply(pc, p14);
		shouldNotImply(pc, p11);
		shouldNotImply(pc, p12);
		shouldNotImply(pc, p13);

		shouldAdd(pc, p13);
		shouldImply(pc, p11);
		shouldImply(pc, p12);
		shouldImply(pc, p13);
		shouldImply(pc, p14);

		shouldNotAdd(pc, op);

		pc = p13.newPermissionCollection();

		shouldAdd(pc, p13);
		shouldImply(pc, p11);
		shouldImply(pc, p12);
		shouldImply(pc, p13);
		shouldImply(pc, p14);

		pc = p11.newPermissionCollection();

		shouldAdd(pc, p11);
		shouldImply(pc, p11);
		shouldImply(pc, p12);
		shouldImply(pc, p13);
		shouldImply(pc, p14);

		pc.setReadOnly();

		shouldNotAdd(pc, p12);

		checkEnumeration(pc.elements(), false);

		PackagePermission p21 = new PackagePermission("com.foo.service2", "import"); //$NON-NLS-1$ //$NON-NLS-2$
		PackagePermission p22 = new PackagePermission("com.foo.*", "import"); //$NON-NLS-1$ //$NON-NLS-2$
		PackagePermission p23 = new PackagePermission("com.*", "import"); //$NON-NLS-1$ //$NON-NLS-2$
		PackagePermission p24 = new PackagePermission("*", "import"); //$NON-NLS-1$ //$NON-NLS-2$

		shouldImply(p21, p21);
		shouldImply(p22, p21);
		shouldImply(p23, p21);
		shouldImply(p24, p21);

		shouldImply(p22, p22);
		shouldImply(p23, p22);
		shouldImply(p24, p22);

		shouldImply(p23, p23);
		shouldImply(p24, p23);

		shouldImply(p24, p24);

		shouldNotImply(p21, p22);
		shouldNotImply(p21, p23);
		shouldNotImply(p21, p24);

		shouldNotImply(p22, p23);
		shouldNotImply(p22, p24);

		shouldNotImply(p23, p24);

		pc = p21.newPermissionCollection();

		shouldAdd(pc, p21);
		shouldImply(pc, p21);
		shouldNotImply(pc, p22);
		shouldNotImply(pc, p23);
		shouldNotImply(pc, p24);

		shouldAdd(pc, p22);
		shouldImply(pc, p21);
		shouldImply(pc, p22);
		shouldNotImply(pc, p23);
		shouldNotImply(pc, p24);

		shouldAdd(pc, p23);
		shouldImply(pc, p21);
		shouldImply(pc, p22);
		shouldImply(pc, p23);
		shouldNotImply(pc, p24);

		shouldAdd(pc, p24);
		shouldImply(pc, p21);
		shouldImply(pc, p22);
		shouldImply(pc, p23);
		shouldImply(pc, p24);

		pc = p22.newPermissionCollection();

		shouldAdd(pc, p22);
		shouldImply(pc, p21);
		shouldImply(pc, p22);
		shouldNotImply(pc, p23);
		shouldNotImply(pc, p24);

		pc = p23.newPermissionCollection();

		shouldAdd(pc, p23);
		shouldImply(pc, p21);
		shouldImply(pc, p22);
		shouldImply(pc, p23);
		shouldNotImply(pc, p24);

		pc = p24.newPermissionCollection();

		shouldAdd(pc, p24);
		shouldImply(pc, p21);
		shouldImply(pc, p22);
		shouldImply(pc, p23);
		shouldImply(pc, p24);

		testSerialization(p11);
		testSerialization(p12);
		testSerialization(p13);
		testSerialization(p14);
		testSerialization(p21);
		testSerialization(p22);
		testSerialization(p23);
		testSerialization(p24);
	}

}
