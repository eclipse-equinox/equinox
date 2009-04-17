/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.permadmin;

import org.eclipse.osgi.framework.adaptor.BundleProtectionDomain;
import org.osgi.framework.Bundle;

public class EquinoxProtectionDomain extends BundleProtectionDomain {

	public EquinoxProtectionDomain(BundlePermissions bundlePermissions) {
		super(bundlePermissions);
	}

	public Bundle getBundle() {
		return ((BundlePermissions) getPermissions()).getBundle();
	}

	public void clearPermissionCache() {
		((BundlePermissions) getPermissions()).clearPermissionCache();
	}
}
