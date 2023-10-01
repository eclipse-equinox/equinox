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

package org.eclipse.equinox.console.jaas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;

public class UserPrincipalTests {

	private static final String USERNAME = "username";
	private static final String PASSWORD = "password";
	private static final String OTHERUSER = "otheruser";
	private static final String OTHERPASSWORD = "otherpass";
	private static final String WRONG_PASS = "wrong_pass";
	private static final String ROLE = "administrator";
	private static final String OTHERROLE = "otherrole";
	
	@Test
	public void testHashCode() {
		UserPrincipal user = new UserPrincipal(USERNAME, PASSWORD);
		RolePrincipal role = new RolePrincipal(ROLE); 
		user.addRole(role);
		
		int expectedHash = 73 + USERNAME.hashCode();
		expectedHash = 73*expectedHash + PASSWORD.hashCode();
		expectedHash = 73*expectedHash + role.hashCode();
		assertEquals("User hash code not as expected", expectedHash, user.hashCode());
	}

	@Test
	public void testGetName() {
		UserPrincipal user = new UserPrincipal(USERNAME, PASSWORD);
		assertEquals("Username not as expected", USERNAME, user.getName());
	}

	@Test
	public void testAuthenticate() {
		UserPrincipal user = new UserPrincipal(USERNAME, PASSWORD);
		assertTrue("User should be successfully authenticated", user.authenticate(PASSWORD.toCharArray()));
		assertFalse("User should not be authenticated", user.authenticate(WRONG_PASS.toCharArray()));
	}

	@Test
	public void testGetRoles() {
		UserPrincipal user = new UserPrincipal(USERNAME, PASSWORD);
		RolePrincipal role = new RolePrincipal(ROLE); 
		user.addRole(role);
		Set<RolePrincipal> roles = user.getRoles();
		assertEquals("There should be one role", 1, roles.size());
		assertTrue("User roles should contain the role administrator", roles.contains(role));
	}

	@Test
	public void testEqualsObject() {
		UserPrincipal user = new UserPrincipal(USERNAME, PASSWORD);
		RolePrincipal role = new RolePrincipal(ROLE); 
		user.addRole(role);
		
		UserPrincipal sameUser = new UserPrincipal(USERNAME, PASSWORD);
		RolePrincipal sameRole = new RolePrincipal(ROLE);
		sameUser.addRole(sameRole);
		
		UserPrincipal otherUser = new UserPrincipal(OTHERUSER, OTHERPASSWORD);
		RolePrincipal otherRole = new RolePrincipal(OTHERROLE);
		otherUser.addRole(otherRole);
		
		UserPrincipal userOtherRole	 = new UserPrincipal(USERNAME, PASSWORD);
		RolePrincipal otherRolePrincipal = new RolePrincipal(OTHERROLE);
		userOtherRole.addRole(otherRolePrincipal);
		
		assertTrue("User should be equal to itself", user.equals(user));
		assertTrue("Users should be equal", user.equals(sameUser));
		assertFalse("Users should not be equal", user.equals(otherUser));
		assertFalse("Users should not be equal", user.equals(userOtherRole));
	}

	@Test
	public void testDestroy() {
		UserPrincipal user = new UserPrincipal(USERNAME, PASSWORD);
		UserPrincipal same = new UserPrincipal(USERNAME, PASSWORD);
		
		user.destroy();
		assertFalse("Users should not be equal", user.equals(same));
	}

}
