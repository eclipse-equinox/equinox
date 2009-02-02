/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.useradmin.tests;

import java.util.Dictionary;
import junit.framework.TestCase;
import org.eclipse.equinox.compendium.tests.Activator;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.useradmin.*;

public class UserTest extends TestCase {

	private UserAdmin userAdmin;
	private ServiceReference userAdminReference;

	boolean locked = false;
	Object lock = new Object();

	protected void setUp() throws Exception {
		Activator.getBundle(Activator.BUNDLE_USERADMIN).start();
		userAdminReference = Activator.getBundleContext().getServiceReference(UserAdmin.class.getName());
		userAdmin = (UserAdmin) Activator.getBundleContext().getService(userAdminReference);
	}

	protected void tearDown() throws Exception {
		Activator.getBundleContext().ungetService(userAdminReference);
		Activator.getBundle(Activator.BUNDLE_USERADMIN).stop();
	}

	public void testCreate() throws Exception {
		User user = (User) userAdmin.createRole("testUserCreate1", Role.USER); //$NON-NLS-1$
		assertNotNull(user);
		assertEquals("testUserCreate1", user.getName()); //$NON-NLS-1$
		assertTrue(user.getType() == Role.USER);
		user.getProperties().put("test", "valu"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testGetUser() {
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
		userAdmin.removeRole(user.getName());
	}

	public void testRemovedUser() {
		assertNull(userAdmin.getRole("testUserCreate1")); //$NON-NLS-1$
	}

	public void testCreate02() throws Exception {
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

	public void testGetUser02() {
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

		Dictionary creds = ((User) user).getCredentials();
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
		userAdmin.removeRole(user.getName());
	}

	public void testRemovedUser02() {
		assertNull(userAdmin.getRole("testUserCreate2")); //$NON-NLS-1$
	}

	public void testUserCreateAndRemove() throws Exception {
		User user = (User) userAdmin.createRole("testUserCreateAndRemove", Role.USER); //$NON-NLS-1$
		assertNotNull(user);
		assertEquals("testUserCreateAndRemove", user.getName()); //$NON-NLS-1$
		assertTrue(user.getType() == Role.USER);
		assertTrue(userAdmin.removeRole("testUserCreateAndRemove")); //$NON-NLS-1$
		assertNull(userAdmin.getRole("testUserCreateAndRemove")); //$NON-NLS-1$
	}

}
