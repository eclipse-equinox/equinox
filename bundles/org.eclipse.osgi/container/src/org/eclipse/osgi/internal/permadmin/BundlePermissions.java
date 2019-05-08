/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
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
package org.eclipse.osgi.internal.permadmin;

import java.security.*;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import org.osgi.framework.Bundle;
import org.osgi.framework.PackagePermission;

public final class BundlePermissions extends PermissionCollection {
	private static final long serialVersionUID = -5443618108312606612L;

	// Note that this forces the Enumeration inner class to be loaded as soon as possible (see bug 119069)  
	static final Enumeration<Permission> EMPTY_ENUMERATION = new Enumeration<Permission>() {
		@Override
		public boolean hasMoreElements() {
			return false;
		}

		@Override
		public Permission nextElement() {
			throw new NoSuchElementException();
		}
	};

	private final Bundle bundle;
	private final SecurityAdmin securityAdmin;
	private final PermissionInfoCollection impliedPermissions;
	private final PermissionInfoCollection restrictedPermissions;
	private final Permissions wovenPermissions;

	public BundlePermissions(Bundle bundle, SecurityAdmin securityAdmin, PermissionInfoCollection impliedPermissions, PermissionInfoCollection restrictedPermissions) {
		this.bundle = bundle;
		this.securityAdmin = securityAdmin;
		this.impliedPermissions = impliedPermissions;
		this.restrictedPermissions = restrictedPermissions;
		this.wovenPermissions = new Permissions();
		setReadOnly(); // collections are managed with ConditionalPermissionAdmin
	}

	@Override
	public void add(Permission permission) {
		throw new SecurityException();
	}

	/**
	 * Add a package permission to this woven bundle.
	 * <p/>
	 * Bundles may require additional permissions in order to execute byte code
	 * woven by weaving hooks.
	 * 
	 * @param permission The package permission to add to this woven bundle.
	 * @throws SecurityException If the <code>permission</code>
	 *         does not have an action of {@link PackagePermission#IMPORT}.
	 */
	public void addWovenPermission(PackagePermission permission) {
		if (!permission.getActions().equals(PackagePermission.IMPORT))
			throw new SecurityException();
		wovenPermissions.add(permission);
	}

	@Override
	public Enumeration<Permission> elements() {
		// TODO return an empty enumeration for now; 
		// It does not seem possible to do this properly with multiple exports and conditional permissions.
		// When looking to fix this be sure the Enumeration class is loaded as soon as possible (see bug 119069)
		return EMPTY_ENUMERATION;
	}

	@Override
	public boolean implies(Permission permission) {
		// first check implied permissions
		if ((impliedPermissions != null) && impliedPermissions.implies(permission))
			return true;

		// Now check implied permissions added by weaving hooks.
		if (wovenPermissions.implies(permission))
			return true;

		// We must be allowed by the restricted permissions to have any hope of passing the check
		if ((restrictedPermissions != null) && !restrictedPermissions.implies(permission))
			return false;

		return securityAdmin.checkPermission(permission, this);
	}

	public Bundle getBundle() {
		return bundle;
	}

	public void clearPermissionCache() {
		if (impliedPermissions != null)
			impliedPermissions.clearPermissionCache();
		if (restrictedPermissions != null)
			restrictedPermissions.clearPermissionCache();
	}
}
