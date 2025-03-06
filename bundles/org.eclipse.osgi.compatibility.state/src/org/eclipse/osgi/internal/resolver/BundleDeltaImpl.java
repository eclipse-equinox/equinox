/*******************************************************************************
 * Copyright (c) 2003, 2010 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Rob Harrop - SpringSource Inc. (bug 247522)
 *******************************************************************************/
package org.eclipse.osgi.internal.resolver;

import org.eclipse.osgi.service.resolver.BundleDelta;
import org.eclipse.osgi.service.resolver.BundleDescription;

final class BundleDeltaImpl implements BundleDelta {

	private volatile BundleDescription bundleDescription;
	private volatile int type;

	public BundleDeltaImpl(BundleDescription bundleDescription) {
		this(bundleDescription, 0);
	}

	public BundleDeltaImpl(BundleDescription bundleDescription, int type) {
		this.bundleDescription = bundleDescription;
		this.type = type;
	}

	@Override
	public BundleDescription getBundle() {
		return bundleDescription;
	}

	@Override
	public int getType() {
		return type;
	}

	protected void setBundle(BundleDescription bundleDescription) {
		this.bundleDescription = bundleDescription;
	}

	protected void setType(int type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return bundleDescription.getSymbolicName() + '_' + bundleDescription.getVersion() + " (" + toTypeString(type) + ")"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static String toTypeString(int type) {
		StringBuilder typeStr = new StringBuilder();
		if ((type & BundleDelta.ADDED) != 0) {
			typeStr.append("ADDED,"); //$NON-NLS-1$
		}
		if ((type & BundleDelta.REMOVED) != 0) {
			typeStr.append("REMOVED,"); //$NON-NLS-1$
		}
		if ((type & BundleDelta.RESOLVED) != 0) {
			typeStr.append("RESOLVED,"); //$NON-NLS-1$
		}
		if ((type & BundleDelta.UNRESOLVED) != 0) {
			typeStr.append("UNRESOLVED,"); //$NON-NLS-1$
		}
		if ((type & BundleDelta.LINKAGE_CHANGED) != 0) {
			typeStr.append("LINKAGE_CHANGED,"); //$NON-NLS-1$
		}
		if ((type & BundleDelta.UPDATED) != 0) {
			typeStr.append("UPDATED,"); //$NON-NLS-1$
		}
		if ((type & BundleDelta.REMOVAL_PENDING) != 0) {
			typeStr.append("REMOVAL_PENDING,"); //$NON-NLS-1$
		}
		if ((type & BundleDelta.REMOVAL_COMPLETE) != 0) {
			typeStr.append("REMOVAL_COMPLETE,"); //$NON-NLS-1$
		}
		if (typeStr.length() > 0) {
			typeStr.deleteCharAt(typeStr.length() - 1);
		}
		return typeStr.toString();
	}

	@Override
	public int compareTo(BundleDelta obj) {
		long idcomp = getBundle().getBundleId() - obj.getBundle().getBundleId();
		return (idcomp < 0L) ? -1 : ((idcomp > 0L) ? 1 : 0);
	}
}
