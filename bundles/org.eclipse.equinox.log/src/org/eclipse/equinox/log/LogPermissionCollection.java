/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.equinox.log;

import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 * Stores a set of <code>LogPermission</code> permissions.
 *
 * @ThreadSafe
 * @see java.security.Permission
 * @see java.security.Permissions
 * @see java.security.PermissionCollection
 */
public final class LogPermissionCollection extends PermissionCollection {
	private static final long serialVersionUID = -1955409691185916778L;
	LogPermission logPermission;

	public void add(Permission permission) {
		if (!(permission instanceof LogPermission))
			throw new IllegalArgumentException("invalid permission: " + permission); //$NON-NLS-1$
		if (isReadOnly())
			throw new SecurityException("attempt to add a LogPermission to a readonly LogPermissionCollection"); //$NON-NLS-1$
		if (permission != null)
			logPermission = (LogPermission) permission;
	}

	public Enumeration elements() {
		return new Enumeration() {
			private boolean hasMore = (logPermission != null);

			public boolean hasMoreElements() {
				return hasMore;
			}

			public Object nextElement() {
				if (hasMore) {
					hasMore = false;
					return logPermission;
				}
				throw new NoSuchElementException();
			}
		};
	}

	public boolean implies(Permission permission) {
		return logPermission != null && logPermission.implies(permission);
	}

}
