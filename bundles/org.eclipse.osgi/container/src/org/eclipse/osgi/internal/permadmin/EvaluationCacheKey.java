/*******************************************************************************
 * Copyright (c) 2018 Connexta, LLC and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Connexta, LLC - initial implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.permadmin;

import java.security.Permission;
import java.util.Objects;

class EvaluationCacheKey {

    private final Permission permission;

    private final Long bundleId;

    EvaluationCacheKey(BundlePermissions bundlePermissions, Permission permission) {
        this.permission = permission;
        this.bundleId = bundlePermissions.getBundle().getBundleId();
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
        return Objects.equals(bundleId, that.bundleId) && Objects.equals(
                permission,
                that.permission);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bundleId, permission);
    }
}
