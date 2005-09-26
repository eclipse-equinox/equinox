/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.internal.core;

import java.security.Permission;
import java.util.*;

/**
 * A combination of two BundlePermissionCollection classes.
 *
 */
final class BundleCombinedPermissions extends BundlePermissionCollection {
	private static final long serialVersionUID = 4049357526208360496L;
	private BundlePermissionCollection assigned;
	private BundlePermissionCollection implied;
	private ConditionalPermissions conditional;
	private ConditionalPermissionSet restrictedPermissions;
	private boolean isDefault;

	/**
	 * Create a permission combiner class.
	 *
	 * @param implied The permissions a bundle always has.
	 */
	BundleCombinedPermissions(BundlePermissionCollection implied) {
		this.implied = implied;

		setReadOnly(); /* this doesn't really mean anything */
	}

	/**
	 * Assign the administrator defined permissions.
	 *
	 * @param assigned The permissions assigned by the administrator.
	 * @param isDefault If true, the assigned permissions are the default permissions.
	 */
	void setAssignedPermissions(BundlePermissionCollection assigned, boolean isDefault) {
		this.assigned = assigned;
		this.isDefault = isDefault;
	}

	/**
	 * Assign the conditional permissions
	 * 
	 * @param conditional The conditional permissions assigned by the administrator
	 */
	void setConditionalPermissions(ConditionalPermissions conditional) {
		this.conditional = conditional;
	}

	void checkConditionalPermissionInfo(ConditionalPermissionInfoImpl cpi) {
		if (conditional != null) {
			conditional.checkConditionalPermissionInfo(cpi);
		}
	}

	void unresolvePermissions() {
		if (assigned != null)
			assigned.unresolvePermissions();
		if (implied != null)
			implied.unresolvePermissions();
		if (conditional != null)
			conditional.unresolvePermissions();
		if (restrictedPermissions != null)
			restrictedPermissions.unresolvePermissions();
	}

	/**
	 * Adds the argument to the collection.
	 *
	 * @param		permission java.security.Permission
	 *					the permission to add to the collection.
	 * @exception	SecurityException
	 *					if the collection is read only.
	 */
	public void add(Permission permission) {
		throw new SecurityException();
	}

	/**
	 * Answers an enumeration of the permissions
	 * in the receiver.
	 *
	 * @return		Enumeration
	 *					the permissions in the receiver.
	 */
	public Enumeration elements() {
		// TODO return an empty enumeration for now; 
		// It does not seem possible to do this properly with multiple exports and conditional permissions.
		return new Enumeration() {
			public boolean hasMoreElements() {
				return false;
			}
			public Object nextElement() {
				throw new NoSuchElementException();
			}
		};
	}

	/**
	 * Indicates whether the argument permission is implied
	 * by the permissions contained in the receiver.
	 *
	 * @return		boolean
	 *					<code>true</code> if the argument permission
	 *					is implied by the permissions in the receiver,
	 *					and <code>false</code> if it is not.
	 * @param		permission java.security.Permission
	 *					the permission to check
	 */
	public boolean implies(Permission permission) {
		if ((implied != null) && implied.implies(permission))
			return true;

		/* We must be allowed by the restricted permissions to have any hope of
		 * passing the check */
		if ((restrictedPermissions != null) && !restrictedPermissions.implies(permission)) {
			return false;
		}

		/* If we aren't using the default permissions, then the assigned
		 * permission are the exact permissions the bundle has. */
		if (!isDefault && assigned != null)
			return assigned.implies(permission);
		if (conditional != null) {
			boolean conditionalImplies = conditional.implies(permission);
			if (!conditional.isEmpty()) {
				return conditionalImplies;
			}
		}
		/* If there aren't any conditional permissions that apply, we use
		 * the default. */
		return assigned.implies(permission);
	}

	/**
	 * Sets the restricted Permissions of the Bundle. This set of Permissions limit the
	 * Permissions available to the Bundle.
	 * 
	 * @param restrictedPermissions the maximum set of permissions allowed to the Bundle 
	 * irrespective of the actual permissions assigned to it.
	 */
	public void setRestrictedPermissions(ConditionalPermissionSet restrictedPermissions) {
		this.restrictedPermissions = restrictedPermissions;
	}
}
