package org.eclipse.equinox.log;

import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Enumeration;
import java.util.NoSuchElementException;

public final class LogPermissionCollection extends PermissionCollection {
	private static final long serialVersionUID = -1955409691185916778L;
	LogPermission logPermission;

	public void add(Permission permission) {
		if (!(permission instanceof LogPermission))
			throw new IllegalArgumentException("invalid permission: " + permission); //$NON-NLS-1$
		if (isReadOnly())
			throw new SecurityException("attempt to add a LogPermission to a readonly LogPermissionCollection"); //$NON-NLS-1$
		if (permission == null)
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
