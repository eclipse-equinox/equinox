/*******************************************************************************
 * Copyright (c) 2011, 2018 SAP AG and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Lazar Kirchev, SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.console.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Test;

public class SecureUserStoreTests {

	private static final String USER_STORE_FILE_NAME_PROPERTY = "org.eclipse.equinox.console.jaas.file";
	private static final String USER_STORE_FILE_NAME = SecureUserStoreTests.class.getName() + "_store";
	private static final String USERNAME1 = "username1";
	private static final String USERNAME2 = "username2";
	private static final String PASSWORD1 = "password1";
	private static final String PASSWORD2 = "password2";
	private static final String ROLES1 = "role1,role2";
	private static final String ROLES2 = "role3,role4";
	private static final String ROLES_TO_REMOVE = "role2";
	private static final String REMAINING_ROLES = "role1";

	@Test
	public void testStore() throws Exception {
		cleanUp();

		System.setProperty(USER_STORE_FILE_NAME_PROPERTY, USER_STORE_FILE_NAME);

		SecureUserStore.initStorage();
		assertTrue("Secure store file does not exist", new File(USER_STORE_FILE_NAME).exists());

		SecureUserStore.putUser(USERNAME1, PASSWORD1, null);
		SecureUserStore.putUser(USERNAME2, PASSWORD2, ROLES2);

		String[] usernames = SecureUserStore.getUserNames();
		boolean arePresent = (usernames[0].equals(USERNAME1) || usernames[0].equals(USERNAME2))
				&& (usernames[1].equals(USERNAME1) || usernames[1].equals(USERNAME2))
				&& (!usernames[0].equals(usernames[1]));
		assertTrue("Usernames not correctly saved", arePresent);

		String pass1 = SecureUserStore.getPassword(USERNAME1);
		String pass2 = SecureUserStore.getPassword(USERNAME2);
		assertTrue("Passwords not correctly saved", pass1.equals(PASSWORD1) && pass2.equals(PASSWORD2));

		boolean existsUser1 = SecureUserStore.existsUser(USERNAME1);
		boolean existsUser2 = SecureUserStore.existsUser(USERNAME2);
		assertTrue("Users should exist", existsUser1 && existsUser2);

		String roles = SecureUserStore.getRoles(USERNAME2);
		assertEquals("Roles for the second user are not as expected", ROLES2, roles);

		SecureUserStore.addRoles(USERNAME1, ROLES1);
		roles = SecureUserStore.getRoles(USERNAME1);
		boolean areRolesEqual = compareRoles(ROLES1, roles);
		assertTrue("Roles for the first user are not as expected", areRolesEqual);

		SecureUserStore.removeRoles(USERNAME1, ROLES_TO_REMOVE);
		roles = SecureUserStore.getRoles(USERNAME1);
		areRolesEqual = compareRoles(REMAINING_ROLES, roles);
		assertTrue("Roles for the first user are not as expected", areRolesEqual);

		SecureUserStore.resetPassword(USERNAME1);
		String pass = SecureUserStore.getPassword(USERNAME1);
		assertNull("Password should be null", pass);

		SecureUserStore.setPassword(USERNAME1, PASSWORD1);
		pass = SecureUserStore.getPassword(USERNAME1);
		assertEquals("Password should be null", PASSWORD1, pass);

		SecureUserStore.deleteUser(USERNAME2);
		assertFalse("User2 should not exist", SecureUserStore.existsUser(USERNAME2));
	}

	@After
	public void cleanUp() {
		System.setProperty(USER_STORE_FILE_NAME_PROPERTY, "");
		File file = new File(USER_STORE_FILE_NAME);
		if (file.exists()) {
			file.delete();
		}
	}

	private boolean compareRoles(String expectedRoles, String actualRoles) {
		Set<String> expectedRolesSet = new HashSet<>();
		for (String role : expectedRoles.split(",")) {
			expectedRolesSet.add(role);
		}

		Set<String> actualRolesSet = new HashSet<>();
		for (String role : actualRoles.split(",")) {
			actualRolesSet.add(role);
		}

		return expectedRolesSet.equals(actualRolesSet);
	}
}
