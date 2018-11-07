/*******************************************************************************
 * Copyright (c) 2011 SAP AG
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

import static org.junit.Assert.*;

import org.junit.Test;

public class RolePrincipalTests {
	
	private static final String ROLE_NAME = "administrator";

	@Test
	public void testHashCode() {
		RolePrincipal role = new RolePrincipal(ROLE_NAME);
		assertEquals("Role hash code not as expected", 73 + ROLE_NAME.hashCode(), role.hashCode());
	}

	@Test
	public void testGetName() {
		RolePrincipal role = new RolePrincipal(ROLE_NAME);
		assertEquals("Role not as expected", ROLE_NAME, role.getName());
	}

	@Test
	public void testEqualsObject() {
		RolePrincipal role = new RolePrincipal(ROLE_NAME);
		RolePrincipal sameRole = new RolePrincipal(ROLE_NAME);
		RolePrincipal emptyRole = new RolePrincipal(null);
		
		assertTrue("Roles should be equal", role.equals(role));
		assertTrue("Roles should be equal", role.equals(sameRole));
		assertFalse("Roles should not be equal", role.equals(emptyRole));
	}

}
