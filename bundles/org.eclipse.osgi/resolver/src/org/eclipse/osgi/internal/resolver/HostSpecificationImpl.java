/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.internal.resolver;

import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.HostSpecification;

public class HostSpecificationImpl extends VersionConstraintImpl implements HostSpecification {
	private boolean reloadHost;

	public boolean reloadHost() {
		return reloadHost;
	}

	public void setReloadHost(boolean reloadHost) {
		this.reloadHost = reloadHost;
	}

	public boolean isOptional() {
		// a fragment cannot exist without its master
		return false;
	}

	public BundleDescription[] getSuppliers() {
		BundleDescription supplier = getSupplier();
		return (supplier == null) ? new BundleDescription[0] : new BundleDescription[] {supplier};
	}
}