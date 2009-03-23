package org.eclipse.equinox.log;

import java.security.Permission;
import java.security.PermissionCollection;

public class LogPermission extends Permission {
	private static final long serialVersionUID = -441193976837153362L;
	private static final String ALL = "*"; //$NON-NLS-1$
	public static final String LOG = "log"; //$NON-NLS-1$

	public LogPermission(String name, String actions) {
		super(name);
		if (!name.equals(ALL))
			throw new IllegalArgumentException("name must be *"); //$NON-NLS-1$

		actions = actions.trim();
		if (!actions.equalsIgnoreCase(LOG) && !actions.equals(ALL))
			throw new IllegalArgumentException("actions must be * or log"); //$NON-NLS-1$
	}

	public boolean equals(Object obj) {
		return obj instanceof LogPermission;
	}

	public String getActions() {
		return LOG;
	}

	public int hashCode() {
		return LogPermission.class.hashCode();
	}

	public boolean implies(Permission permission) {
		return permission instanceof LogPermission;
	}

	public PermissionCollection newPermissionCollection() {
		return new LogPermissionCollection();
	}
}
