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

import java.util.ArrayList;
import java.util.List;
import org.eclipse.osgi.service.resolver.BundleDescription;

/**
 * This implementation of State does a bookkeeping of all added/removed 
 */
public class UserState extends StateImpl {
	private List added = new ArrayList();
	private List removed = new ArrayList();
	private List updated = new ArrayList();

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

	public boolean updateBundle(BundleDescription newDescription) {
		if (!super.updateBundle(newDescription))
			return false;
		updated.add(new Long(newDescription.getBundleId()));
		return true;
	}

	public Long[] getAllAdded() {
		return (Long[]) added.toArray(new Long[added.size()]);
	}

	public Long[] getAllRemoved() {
		return (Long[]) removed.toArray(new Long[removed.size()]);
	}
	
	public Long[] getAllUpdated() {
		return (Long[]) updated.toArray(new Long[updated.size()]);
	}
	
}