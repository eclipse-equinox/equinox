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

import org.eclipse.osgi.service.resolver.BundleDelta;
import org.eclipse.osgi.service.resolver.BundleDescription;

public class BundleDeltaImpl implements BundleDelta {

	private BundleDescription bundleDescription;
	private int type;

	public BundleDeltaImpl(BundleDescription bundleDescription) {
		this(bundleDescription, 0);
	}

	public BundleDeltaImpl(BundleDescription bundleDescription, int type) {
		this.bundleDescription = bundleDescription;
		this.type = type;
	}

	public BundleDescription getBundle() {
		return bundleDescription;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public String toString() {
		return bundleDescription.getSymbolicName() + '_' + bundleDescription.getVersion() + " (" + toTypeString(type) + ")";
	}

	private static String toTypeString(int type) {
		StringBuffer typeStr = new StringBuffer();
		if ((type & BundleDelta.ADDED) != 0)
			typeStr.append("ADDED,");
		if ((type & BundleDelta.REMOVED) != 0)
			typeStr.append("REMOVED,");
		if ((type & BundleDelta.RESOLVED) != 0)
			typeStr.append("RESOLVED,");
		if ((type & BundleDelta.UNRESOLVED) != 0)
			typeStr.append("UNRESOLVED,");
		if ((type & BundleDelta.LINKAGE_CHANGED) != 0)
			typeStr.append("LINKAGE_CHANGED,");
		if ((type & BundleDelta.UPDATED) != 0)
			typeStr.append("UPDATED,");
		if (typeStr.length() > 0)
			typeStr.deleteCharAt(typeStr.length() - 1);
		return typeStr.toString();
	}
}