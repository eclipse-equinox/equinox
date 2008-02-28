/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.security.credentials;

import java.security.Principal;
import org.eclipse.equinox.security.auth.credentials.IPublicCredential;

public class EquinoxPublicCredential implements IPublicCredential {

	final private String name;
	final private Principal primaryRole;
	final private Principal[] roles;
	final private String loginModuleID;

	public EquinoxPublicCredential(String name, Principal primaryRole, String loginModuleID) {
		this.name = name;
		this.primaryRole = primaryRole;
		this.roles = null;
		this.loginModuleID = loginModuleID;
	}

	public EquinoxPublicCredential(String name, Principal[] roles, String loginModuleID) {
		this.name = name;
		this.primaryRole = null;
		this.roles = roles;
		this.loginModuleID = loginModuleID;
	}

	public String getName() {
		return name;
	}

	public Principal getPrimaryRole() {
		if (primaryRole != null)
			return primaryRole;
		if (roles != null && roles.length >= 1)
			return roles[0];
		return null;
	}

	public Principal[] getRoles() {
		return roles;
	}

	public String getProviderID() {
		return loginModuleID;
	}

}
