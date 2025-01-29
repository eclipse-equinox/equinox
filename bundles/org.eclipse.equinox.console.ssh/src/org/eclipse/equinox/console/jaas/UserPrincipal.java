/*******************************************************************************
 * Copyright (c) 2011, 2017 SAP AG
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

import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

/**
 * This class represents a user with password and roles
 */
public class UserPrincipal implements Principal {
	private final String username;
	private char[] password;
	private final Set<RolePrincipal> rolePrincipals = new HashSet<>();

	public UserPrincipal(String username, String password) {
		this.username = username;
		this.password = new char[password.length()];
		System.arraycopy(password.toCharArray(), 0, this.password, 0, this.password.length);
	}

	@Override
	public String getName() {
		return username;
	}

	public boolean authenticate(char[] password) {
		if (password == null) {
			return false;
		}

		if (this.password == null) {
			return false;
		}

		if (this.password.length != password.length) {
			return false;
		}

		for (int i = 0; i < this.password.length; i++) {
			if (this.password[i] != password[i]) {
				return false;
			}
		}

		return true;
	}

	public Set<RolePrincipal> getRoles() {
		return rolePrincipals;
	}

	public synchronized void addRole(RolePrincipal rolePrincipal) {
		rolePrincipals.add(rolePrincipal);
	}

	@Override
	public boolean equals(Object userPrincipal) {
		if (userPrincipal == null) {
			return false;
		}

		if (this == userPrincipal) {
			return true;
		}

		if (!(userPrincipal instanceof UserPrincipal)) {
			return false;
		}

		UserPrincipal otherUser = (UserPrincipal) userPrincipal;
		if (username != null) {
			if (!username.equals(otherUser.username)) {
				return false;
			}
		} else {
			if (otherUser.username != null) {
				return false;
			}
		}

		if (password != null) {
			if (otherUser.password == null) {
				return false;
			}

			if (password.length != otherUser.password.length) {
				return false;
			}

			for (int i = 0; i < password.length; i++) {
				if (password[i] != otherUser.password[i]) {
					return false;
				}
			}
		} else {
			if (otherUser.username != null) {
				return false;
			}
		}

		if (rolePrincipals != null) {
			if (!(rolePrincipals.equals(otherUser.rolePrincipals))) {
				return false;
			}
		} else {
			if (otherUser.rolePrincipals != null) {
				return false;
			}
		}

		return true;
	}

	public void destroy() {
		for (int i = 0; i < password.length; i++) {
			password[i] = ' ';
		}

		password = null;
	}

	@Override
	public int hashCode() {
		int result = 1;
		result = 73 * result + (username == null ? 0 : username.hashCode());
		result = 73 * result + (password == null ? 0 : new String(password).hashCode());
		result = 73 * result + (rolePrincipals == null ? 0 : rolePrincipals.hashCode());
		return result;
	}
}
