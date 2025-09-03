/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package org.eclipse.equinox.log;

import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Collections;
import java.util.Enumeration;

/**
 * Stores a set of <code>LogPermission</code> permissions.
 *
 * @ThreadSafe
 * @see java.security.Permission
 * @see java.security.Permissions
 * @see java.security.PermissionCollection
 * @since 3.7
 */
public final class LogPermissionCollection extends PermissionCollection {
	private static final long serialVersionUID = -1955409691185916778L;
	LogPermission logPermission;

	@Override
	public void add(Permission permission) {
		if (!(permission instanceof LogPermission)) {
			throw new IllegalArgumentException("invalid permission: " + permission); //$NON-NLS-1$
		}
		if (isReadOnly()) {
			throw new SecurityException("attempt to add a LogPermission to a readonly LogPermissionCollection"); //$NON-NLS-1$
		}
		if (permission != null) {
			logPermission = (LogPermission) permission;
		}
	}

	@Override
	public Enumeration<Permission> elements() {
		return logPermission != null ? Collections.enumeration(Collections.singleton(logPermission)) : Collections.emptyEnumeration();
	}

	@Override
	public boolean implies(Permission permission) {
		return logPermission != null && logPermission.implies(permission);
	}

}
