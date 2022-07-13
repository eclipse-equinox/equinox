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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import org.osgi.framework.PackagePermission;
import org.osgi.framework.ServicePermission;

public class PermissionTests {

	protected void badServicePermission(String name, String actions) {
		try {
			ServicePermission p = new ServicePermission(name, actions);
			fail(p + " created with invalid actions"); //$NON-NLS-1$
		} catch (IllegalArgumentException e) {
			// expected
		}
	}

	protected void badPackagePermission(String name, String actions) {
		try {
			PackagePermission p = new PackagePermission(name, actions);
			fail(p + " created with invalid actions"); //$NON-NLS-1$
		} catch (IllegalArgumentException e) {
			// expected
		}
	}

	protected void checkEnumeration(Enumeration en, boolean isEmpty) {
		assertEquals(en + " empty state is invalid", !isEmpty, en.hasMoreElements()); //$NON-NLS-1$
		try {
			while (en.hasMoreElements()) {
				en.nextElement();
			}
		} catch (NoSuchElementException e) {
			fail(en + " threw NoSuchElementException"); //$NON-NLS-1$
		}

		try {
			en.nextElement();
			fail(en + " is empty but didn't throw NoSuchElementException"); //$NON-NLS-1$
		} catch (NoSuchElementException e) {
			// expected
		}
	}

	protected void shouldImply(Permission p1, Permission p2) {
		assertTrue(p1 + " does not imply " + p2, p1.implies(p2)); //$NON-NLS-1$
	}

	protected void shouldNotImply(Permission p1, Permission p2) {
		assertFalse(p1 + " does imply " + p2, p1.implies(p2)); //$NON-NLS-1$
	}

	protected void shouldImply(PermissionCollection p1, Permission p2) {
		assertTrue(p1 + " does not imply " + p2, p1.implies(p2)); //$NON-NLS-1$
	}

	protected void shouldNotImply(PermissionCollection p1, Permission p2) {
		assertFalse(p1 + " does imply " + p2, p1.implies(p2)); //$NON-NLS-1$
	}

	protected void shouldEqual(Permission p1, Permission p2) {
		assertTrue(p1 + " does not equal " + p2, p1.equals(p2)); //$NON-NLS-1$
		assertTrue(p2 + " does not equal " + p1, p2.equals(p1)); //$NON-NLS-1$
	}

	protected void shouldNotEqual(Permission p1, Permission p2) {
		assertFalse(p1 + " does equal " + p2, p1.equals(p2)); //$NON-NLS-1$
	}

	protected void shouldAdd(PermissionCollection p1, Permission p2) {
		try {
			p1.add(p2);
		} catch (Exception e) {
			fail(p1 + " will not add " + p2); //$NON-NLS-1$
		}
	}

	protected void shouldNotAdd(PermissionCollection p1, Permission p2) {
		try {
			p1.add(p2);
			fail(p1 + " will add " + p2); //$NON-NLS-1$
		} catch (Exception e) {
			// expected
		}
	}

	protected void testSerialization(Permission p1) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream out = new ObjectOutputStream(baos);

			out.writeObject(p1);
			out.flush();
			out.close();

			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
			ObjectInputStream in = new ObjectInputStream(bais);

			Permission p2 = (Permission) in.readObject();

			shouldEqual(p1, p2);
			shouldImply(p1, p2);
			shouldImply(p2, p1);
		} catch (Exception e) {
			fail(e.toString());
		}
	}

}
