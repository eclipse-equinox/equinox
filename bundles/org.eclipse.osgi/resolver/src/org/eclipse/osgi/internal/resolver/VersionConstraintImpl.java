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

import org.eclipse.osgi.service.resolver.*;

public abstract class VersionConstraintImpl implements VersionConstraint {
	private String name;
	private VersionRange versionRange;
	private byte matchingRule = -1;
	private BundleDescription bundle;
	private BundleDescription supplier;
	private Version actualVersion;

	public String getName() {
		return name;
	}

	public Version getActualVersion() {
		return actualVersion;
	}

	public VersionRange getVersionRange() {
		return versionRange;
	}

	public BundleDescription getBundle() {
		return bundle;
	}

	public BundleDescription getSupplier() {
		return supplier;
	}

	public boolean isResolved() {
		return supplier != null;
	}

	public void setActualVersion(Version actualVersion) {
		this.actualVersion = actualVersion;
	}

	public void setSupplier(BundleDescription supplier) {
		this.supplier = supplier;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setBundle(BundleDescription bundle) {
		this.bundle = bundle;
	}

	public void setVersionRange(VersionRange versionRange) {
		this.versionRange = versionRange;
	}

	public void unresolve() {
		actualVersion = null;
		supplier = null;
	}

	public boolean isSatisfiedBy(Version provided) {
		return versionRange == null ? true : versionRange.isIncluded(provided);
	}

	public String toString() {
		return "name: " + name + " - version: " + versionRange; //$NON-NLS-1$ //$NON-NLS-2$
	}
}