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

/**
 * Indicates a bundle's authority to log on behalf of other bundles.
 *
 * This permission has only a single action: LOG.
 *
 * @ThreadSafe
 * @since 3.7
 */
public class LogPermission extends Permission {
	private static final long serialVersionUID = -441193976837153362L;
	private static final String ALL = "*"; //$NON-NLS-1$

	/**
	 * The action string <code>log</code>.
	 */
	public static final String LOG = "log"; //$NON-NLS-1$

	/**
	 * Create a new LogPermission.
	 *
	 * @param name Name must be &quot;*&quot;.
	 * @param actions <code>log</code> or &quot;*&quot;.
	 */
	public LogPermission(String name, String actions) {
		super(name);
		if (!name.equals(ALL))
			throw new IllegalArgumentException("name must be *"); //$NON-NLS-1$

		actions = actions.trim();
		if (!actions.equalsIgnoreCase(LOG) && !actions.equals(ALL))
			throw new IllegalArgumentException("actions must be * or log"); //$NON-NLS-1$
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof LogPermission;
	}

	@Override
	public String getActions() {
		return LOG;
	}

	@Override
	public int hashCode() {
		return LogPermission.class.hashCode();
	}

	@Override
	public boolean implies(Permission permission) {
		return permission instanceof LogPermission;
	}

	@Override
	public PermissionCollection newPermissionCollection() {
		return new LogPermissionCollection();
	}
}
