/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others
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
package org.eclipse.equinox.useradmin.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Dictionary;
import org.eclipse.equinox.compendium.tests.Activator;
import org.junit.*;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.useradmin.*;

public class UserTest {
	private UserAdmin userAdmin;
	private ServiceReference<UserAdmin> userAdminReference;

	@Test
	public void testUser1() {
		try {
			createUser1();
			User user = userAdmin.getUser("test", "valu"); //$NON-NLS-1$ //$NON-NLS-2$
			assertNotNull(user);
			assertEquals("testUserCreate1", user.getName()); //$NON-NLS-1$
			assertTrue(user.getType() == Role.USER);
			try {
				Role[] roles = userAdmin.getRoles("(test=valu)"); //$NON-NLS-1$
				assertNotNull(roles);
				assertEquals("number of roles", 1, roles.length); //$NON-NLS-1$
			} catch (InvalidSyntaxException e) {
				fail(e.getMessage());
			}
			removeUser1();
		} finally {
			removeUser1Silently();
		}
	}

	@Test
	public void testUser2() {
		try {
			createUser2();
			Role user = userAdmin.getRole("testUserCreate2"); //$NON-NLS-1$
			assertNotNull(user);
			assertEquals("testUserCreate2", user.getName()); //$NON-NLS-1$
			assertTrue(user.getType() == Role.USER);

			Object test1 = user.getProperties().get("test1"); //$NON-NLS-1$
			assertEquals("test1", "valu", test1); //$NON-NLS-1$ //$NON-NLS-2$
			Object test2 = user.getProperties().get("test2"); //$NON-NLS-1$
			assertEquals("test2", "xxxyyyzzz", test2); //$NON-NLS-1$ //$NON-NLS-2$
			Object test3 = user.getProperties().get("test3"); //$NON-NLS-1$
			assertTrue("test3 not byte[]", test3 instanceof byte[]); //$NON-NLS-1$
			byte[] bytes = (byte[]) test3;
			assertEquals("wrong size", 3, bytes.length); //$NON-NLS-1$
			assertEquals("1", 1, bytes[0]); //$NON-NLS-1$
			assertEquals("2", 2, bytes[1]); //$NON-NLS-1$
			assertEquals("3", 3, bytes[2]); //$NON-NLS-1$

			Dictionary<String, Object> creds = (((User) user).getCredentials());
			test1 = creds.get("test1"); //$NON-NLS-1$
			assertEquals("test1", "valu", test1); //$NON-NLS-1$ //$NON-NLS-2$
			test2 = creds.get("test2"); //$NON-NLS-1$
			assertEquals("test2", "xxxyyyzzz", test2); //$NON-NLS-1$ //$NON-NLS-2$
			test3 = creds.get("test3"); //$NON-NLS-1$
			assertTrue("test3 not byte[]", test3 instanceof byte[]); //$NON-NLS-1$
			bytes = (byte[]) test3;
			assertEquals("wrong size", 3, bytes.length); //$NON-NLS-1$
			assertEquals("1", 1, bytes[0]); //$NON-NLS-1$
			assertEquals("2", 2, bytes[1]); //$NON-NLS-1$
			assertEquals("3", 3, bytes[2]); //$NON-NLS-1$

			try {
				Role[] roles = userAdmin.getRoles("(test3=1)"); //$NON-NLS-1$
				assertNotNull(roles);
				assertEquals("number of roles", 1, roles.length); //$NON-NLS-1$
			} catch (InvalidSyntaxException e) {
				fail(e.getMessage());
			}
			removeUser2();
		} finally {
			removeUser2Silently();
		}
	}

	@Test
	public void testUserCreateAndRemove() throws Exception {
		User user = (User) userAdmin.createRole("testUserCreateAndRemove", Role.USER); //$NON-NLS-1$
		assertNotNull(user);
		assertEquals("testUserCreateAndRemove", user.getName()); //$NON-NLS-1$
		assertTrue(user.getType() == Role.USER);
		assertTrue(userAdmin.removeRole("testUserCreateAndRemove")); //$NON-NLS-1$
		assertNull(userAdmin.getRole("testUserCreateAndRemove")); //$NON-NLS-1$
	}

	@Before
	public void setUp() throws Exception {
		Activator.getBundle(Activator.BUNDLE_USERADMIN).start();
		userAdminReference = Activator.getBundleContext().getServiceReference(UserAdmin.class);
		userAdmin = Activator.getBundleContext().getService(userAdminReference);
	}

	@After
	public void tearDown() throws Exception {
		Activator.getBundleContext().ungetService(userAdminReference);
		Activator.getBundle(Activator.BUNDLE_USERADMIN).stop();
	}

	@SuppressWarnings("unchecked")
	private void createUser1() {
		User user = (User) userAdmin.createRole("testUserCreate1", Role.USER); //$NON-NLS-1$
		assertNotNull(user);
		assertEquals("testUserCreate1", user.getName()); //$NON-NLS-1$
		assertTrue(user.getType() == Role.USER);
		user.getProperties().put("test", "valu"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private void removeUser1() {
		assertTrue(userAdmin.removeRole("testUserCreate1")); //$NON-NLS-1$
	}

	private void removeUser1Silently() {
		try {
			removeUser1();
		} catch (Exception e) {
			// Ignore.
		} catch (AssertionError error) {
			// Ignore.
		}
	}

	@SuppressWarnings("unchecked")
	private void createUser2() {
		User user = (User) userAdmin.createRole("testUserCreate2", Role.USER); //$NON-NLS-1$
		assertNotNull(user);
		assertEquals("testUserCreate2", user.getName()); //$NON-NLS-1$
		assertTrue(user.getType() == Role.USER);
		user.getProperties().put("test1", "valu"); //$NON-NLS-1$ //$NON-NLS-2$
		user.getProperties().put("test2", "xxxyyyzzz"); //$NON-NLS-1$ //$NON-NLS-2$
		user.getProperties().put("test3", new byte[] {1, 2, 3}); //$NON-NLS-1$

		user.getCredentials().put("test1", "valu"); //$NON-NLS-1$ //$NON-NLS-2$
		user.getCredentials().put("test2", "xxxyyyzzz"); //$NON-NLS-1$ //$NON-NLS-2$
		user.getCredentials().put("test3", new byte[] {1, 2, 3}); //$NON-NLS-1$
	}

	private void removeUser2() {
		assertTrue(userAdmin.removeRole("testUserCreate2")); //$NON-NLS-1$
	}

	private void removeUser2Silently() {
		try {
			removeUser2();
		} catch (Exception e) {
			// Ignore.
		} catch (AssertionError error) {
			// Ignore.
		}
	}
}
