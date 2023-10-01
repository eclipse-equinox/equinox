/*******************************************************************************
 * Copyright (c) 2011, 2017 SAP AG and others.
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

/**
 * This class represents a user role
 *
 */
public class RolePrincipal implements Principal {
	private String roleName;
	
	public RolePrincipal(String roleName) {
		this.roleName = roleName;
	}

	@Override
	public String getName() {
		return roleName;
	}
	
	@Override
	public boolean equals(Object role) {
		
		if (role == null) {
			return false;
		}
		
		if (this == role) {
			return true;
		}
		
		if (!(role instanceof RolePrincipal)) {
			return false;
		}
		
		RolePrincipal otherRole = (RolePrincipal) role;
		if (roleName != null) {
			if (roleName.equals(otherRole.roleName)) {
				return true;
			} else {
				return false;
			}
		} else {
			if (otherRole.roleName == null) {
				return true;
			} else {
				return false;
			}
		}
	}

	@Override
	public int hashCode() {
		int result = 1;
		result = 73 * result + (roleName == null ? 0 : roleName.hashCode());
		return result;
	}
}
