/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.permadmin;

import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import org.osgi.framework.Bundle;

public final class BundlePermissions extends PermissionCollection {
	private static final long serialVersionUID = -5443618108312606612L;

	// Note that this forces the Enumeration inner class to be loaded as soon as possible (see bug 119069)  
	static final Enumeration<Permission> EMPTY_ENUMERATION = new Enumeration<Permission>() {
		public boolean hasMoreElements() {
			return false;
		}

		public Permission nextElement() {
			throw new NoSuchElementException();
		}
	};

	private final Bundle bundle;
	private final SecurityAdmin securityAdmin;
	private final PermissionInfoCollection impliedPermissions;
	private final PermissionInfoCollection restrictedPermissions;

	public BundlePermissions(Bundle bundle, SecurityAdmin securityAdmin, PermissionInfoCollection impliedPermissions, PermissionInfoCollection restrictedPermissions) {
		this.bundle = bundle;
		this.securityAdmin = securityAdmin;
		this.impliedPermissions = impliedPermissions;
		this.restrictedPermissions = restrictedPermissions;
		setReadOnly(); // collections are managed with ConditionalPermissionAdmin
	}

	public void add(Permission permission) {
		throw new SecurityException();
	}

	public Enumeration<Permission> elements() {
		// TODO return an empty enumeration for now; 
		// It does not seem possible to do this properly with multiple exports and conditional permissions.
		// When looking to fix this be sure the Enumeration class is loaded as soon as possible (see bug 119069)
		return EMPTY_ENUMERATION;
	}

	public boolean implies(Permission permission) {
		// first check implied permissions
		if ((impliedPermissions != null) && impliedPermissions.implies(permission))
			return true;
		// We must be allowed by the restricted permissions to have any hope of passing the check
		if ((restrictedPermissions != null) && !restrictedPermissions.implies(permission))
			return false;
		return securityAdmin.checkPermission(permission, this);
	}

	public Bundle getBundle() {
		return bundle;
	}

	void clearPermissionCache() {
		if (impliedPermissions != null)
			impliedPermissions.clearPermissionCache();
		if (restrictedPermissions != null)
			restrictedPermissions.clearPermissionCache();
	}
}
