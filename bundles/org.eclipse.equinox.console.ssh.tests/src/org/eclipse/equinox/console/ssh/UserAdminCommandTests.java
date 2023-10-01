/*******************************************************************************
 * Copyright (c) 2011, 2021 SAP AG and others
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

package org.eclipse.equinox.console.ssh;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.apache.felix.service.command.CommandSession;
import org.eclipse.equinox.console.storage.DigestUtil;
import org.eclipse.equinox.console.storage.SecureUserStore;
import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;


public class UserAdminCommandTests {

	private static final String USER_STORE_FILE_NAME_PROPERTY = "org.eclipse.equinox.console.jaas.file";
	private static final String USER_STORE_FILE_NAME = UserAdminCommandTests.class.getName() + "_store";
	private static final String USERNAME_OPTION = "-username";
	private static final String PASSWORD_OPTION = "-password";
	private static final String ROLES_OPTION = "-roles";
	private static final String USERNAME1 = "username1";
	private static final String USERNAME2 = "username2";
	private static final String PASSWORD1 = "password1";
	private static final String PASSWORD2 = "password2";
	private static final String ROLES1 = "role1,role2";
	private static final String ROLES2 = "role3,role4";
	private static final String ROLES_TO_REMOVE = "role2";
	private static final String REMAINING_ROLES = "role1";

	@Test
	public void testCommand() throws Exception {
		cleanUp();

		System.setProperty(USER_STORE_FILE_NAME_PROPERTY, USER_STORE_FILE_NAME);
		SecureUserStore.initStorage();

		CommandSession session = Mockito.mock(CommandSession.class);
		Mockito.when(session.put(any(String.class), any())).thenReturn(new Object());

		UserAdminCommand command = new UserAdminCommand();
		command.addUser(new String[] {USERNAME_OPTION, USERNAME1, PASSWORD_OPTION, PASSWORD1});
		command.addUser(new String[] {USERNAME_OPTION, USERNAME2, PASSWORD_OPTION, PASSWORD2, ROLES_OPTION, ROLES2});

		String[] usernames = SecureUserStore.getUserNames();
		boolean arePresent = (usernames[0].equals(USERNAME1) || usernames[0].equals(USERNAME2)) && (usernames[1].equals(USERNAME1) || usernames[1].equals(USERNAME2)) && (!usernames[0].equals(usernames[1]));
		assertTrue("Usernames not correctly saved", arePresent);

		String pass1 = SecureUserStore.getPassword(USERNAME1);
		String pass2 = SecureUserStore.getPassword(USERNAME2);
		assertTrue("Passwords not correctly saved", pass1.equals(DigestUtil.encrypt(PASSWORD1)) && pass2.equals(DigestUtil.encrypt(PASSWORD2)));

		String roles = SecureUserStore.getRoles(USERNAME2);
		assertEquals("Roles for the second user are not as expected", ROLES2, roles);

		command.addRoles(new String[] {USERNAME_OPTION, USERNAME1, ROLES_OPTION, ROLES1});
		roles = SecureUserStore.getRoles(USERNAME1);
		boolean areRolesEqual = compareRoles(ROLES1, roles);
		assertTrue("Roles for the first user are not as expected", areRolesEqual);

		command.removeRoles(new String[] {USERNAME_OPTION, USERNAME1, ROLES_OPTION, ROLES_TO_REMOVE});
		roles = SecureUserStore.getRoles(USERNAME1);
		areRolesEqual = compareRoles(REMAINING_ROLES, roles);
		assertTrue("Roles for the first user are not as expected", areRolesEqual);

		command.resetPassword(USERNAME1);
		String pass = SecureUserStore.getPassword(USERNAME1);
		assertNull("Password should be null", pass);

		command.setPassword(new String[] {USERNAME_OPTION, USERNAME1, PASSWORD_OPTION, PASSWORD1});
		pass = SecureUserStore.getPassword(USERNAME1);
		assertEquals("Password should be null", DigestUtil.encrypt(PASSWORD1), pass);

		command.deleteUser(USERNAME2);
		assertFalse("User2 should not exist", SecureUserStore.existsUser(USERNAME2));
	}

	@After
	public void cleanUp() {
		System.setProperty(USER_STORE_FILE_NAME_PROPERTY, "");
		File file = new File(USER_STORE_FILE_NAME);
		if(file.exists()) {
			file.delete();
		}
	}

	private boolean compareRoles(String expectedRoles, String actualRoles) {
		Set<String> expectedRolesSet = new HashSet<>();
		for(String role : expectedRoles.split(",")) {
			expectedRolesSet.add(role);
		}

		Set<String> actualRolesSet = new HashSet<>();
		for(String role : actualRoles.split(",")) {
			actualRolesSet.add(role);
		}

		return expectedRolesSet.equals(actualRolesSet);
	}

}
