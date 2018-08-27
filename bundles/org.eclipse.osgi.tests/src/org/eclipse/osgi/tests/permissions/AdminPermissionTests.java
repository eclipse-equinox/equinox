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
import junit.framework.Test;
import junit.framework.TestSuite;
import org.osgi.framework.AdminPermission;

public class AdminPermissionTests extends PermissionTests {

	public static Test suite() {
		return new TestSuite(AdminPermissionTests.class);
	}

	public void testAdminPermission() {
		AdminPermission p1 = new AdminPermission();
		AdminPermission p2 = new AdminPermission("*", "*"); //$NON-NLS-1$ //$NON-NLS-2$
		Permission op = new PropertyPermission("java.home", "read"); //$NON-NLS-1$ //$NON-NLS-2$

		shouldImply(p1, p2);
		shouldImply(p1, p1);
		shouldNotImply(p1, op);

		shouldEqual(p1, p2);
		shouldNotEqual(p1, op);

		PermissionCollection pc = p1.newPermissionCollection();

		checkEnumeration(pc.elements(), true);

		shouldNotImply(pc, p1);

		shouldAdd(pc, p1);
		shouldAdd(pc, p2);
		shouldNotAdd(pc, op);

		pc.setReadOnly();

		shouldNotAdd(pc, new AdminPermission());

		shouldImply(pc, p1);
		shouldImply(pc, p2);
		shouldNotImply(pc, op);

		checkEnumeration(pc.elements(), false);

		testSerialization(p1);
		testSerialization(p2);
	}

}
