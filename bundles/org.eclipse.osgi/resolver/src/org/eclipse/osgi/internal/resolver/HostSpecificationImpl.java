/*******************************************************************************
 * Copyright (c) 2003, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Rob Harrop - SpringSource Inc. (bug 247522)
 *******************************************************************************/

package org.eclipse.osgi.internal.resolver;

import org.eclipse.osgi.service.resolver.*;

public class HostSpecificationImpl extends VersionConstraintImpl implements HostSpecification {

	private BundleDescription[] hosts;
	private boolean multihost = false;

	public boolean isSatisfiedBy(BaseDescription supplier) {
		if (!(supplier instanceof BundleDescription))
			return false;
		BundleDescription candidate = (BundleDescription) supplier;
		if (candidate.getHost() != null)
			return false;
		if (getName() != null && getName().equals(candidate.getSymbolicName()) && (getVersionRange() == null || getVersionRange().isIncluded(candidate.getVersion())))
			return true;
		return false;
	}

	public BundleDescription[] getHosts() {
		synchronized (this.monitor) {
			return hosts == null ? BundleDescriptionImpl.EMPTY_BUNDLEDESCS : hosts;
		}
	}

	public boolean isResolved() {
		synchronized (this.monitor) {
			return hosts != null && hosts.length > 0;
		}
	}

	/*
	 * The resolve algorithm will call this method to set the hosts.
	 */
	void setHosts(BundleDescription[] hosts) {
		synchronized (this.monitor) {
			this.hosts = hosts;
		}
	}

	public String toString() {
		return "Fragment-Host: " + getName() + "; bundle-version=\"" + getVersionRange() + "\""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public BaseDescription getSupplier() {
		synchronized (this.monitor) {
			if (hosts == null || hosts.length == 0)
				return null;
			return hosts[0];
		}
	}

	public boolean isMultiHost() {
		synchronized (this.monitor) {
			return multihost;
		}
	}

	void setIsMultiHost(boolean multihost) {
		synchronized (this.monitor) {
			this.multihost = multihost;
		}
	}
}
