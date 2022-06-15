/*******************************************************************************
 * Copyright (c) 2018 Connexta, LLC and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Connexta, LLC - initial implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.permadmin;

import java.security.Permission;
import java.util.Objects;

class EvaluationCacheKey {

	private final Permission permission;

	private final BundlePermissions bundlePermissions;

	EvaluationCacheKey(BundlePermissions bundlePermissions, Permission permission) {
		this.permission = permission;
		this.bundlePermissions = bundlePermissions;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		EvaluationCacheKey that = (EvaluationCacheKey) o;
		return bundlePermissions == that.bundlePermissions && Objects.equals(permission, that.permission);
	}

	@Override
	public int hashCode() {
		return Objects.hash(bundlePermissions, permission);
	}
}
