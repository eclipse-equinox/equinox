/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.resolver;

import java.util.List;
import java.util.Vector;
import org.eclipse.osgi.service.resolver.BundleDescription;

/**
 * This implementation of State does a bookkeeping of all added/removed 
 */
public class UserState extends StateImpl {
	private long referenceTimestamp; // TODO what is this used for?
	private List added = new Vector(); // TODO why not ArrayList
	private List removed = new Vector(); // TODO why not ArrayList
	public long getReferenceTimestamp() {
		return referenceTimestamp;
	}
	public void setReferenceTimestamp(long referenceTimestamp) {
		this.referenceTimestamp = referenceTimestamp;
	}
	public synchronized boolean addBundle(BundleDescription description) {
		if (!super.addBundle(description))
			return false;
		added.add(new Long(description.getBundleId()));
		return true;
	}
	public synchronized BundleDescription removeBundle(long bundleId) {
		BundleDescription description = super.removeBundle(bundleId);
		if (description == null)
			return null;
		removed.add(new Long(description.getBundleId()));
		return description;
	}
	public Long[] getAllAdded() {
		return (Long[]) added.toArray(new Long[added.size()]);
	}
	public Long[] getAllRemoved() {
		return (Long[]) removed.toArray(new Long[removed.size()]);
	}
}