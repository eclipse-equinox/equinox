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

import java.util.*;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.BundleException;

/**
 * This implementation of State does a bookkeeping of all added/removed 
 */
public class UserState extends StateImpl {
	// TODO this is not an accurate way to record updates
	private final Set<String> updated = Collections.synchronizedSet(new HashSet<String>());

	@Override
	public boolean removeBundle(BundleDescription description) {
		if (description.getLocation() != null)
			updated.remove(description.getLocation());
		if (!super.removeBundle(description))
			return false;
		return true;
	}

	@Override
	public boolean updateBundle(BundleDescription newDescription) {
		if (!super.updateBundle(newDescription))
			return false;
		updated.add(newDescription.getLocation());
		return true;
	}

	/**
	 * @throws BundleException  
	 */
	public StateDelta compare(State baseState) throws BundleException {
		BundleDescription[] currentBundles = this.getBundles();
		StateDeltaImpl delta = new StateDeltaImpl(this);
		// process additions and updates
		for (BundleDescription current : currentBundles) {
			BundleDescription existing = baseState.getBundleByLocation(current.getLocation());
			if (existing == null) {
				delta.recordBundleAdded((BundleDescriptionImpl) current);
			} else if (updated.contains(current.getLocation())) {
				delta.recordBundleUpdated((BundleDescriptionImpl) current);
			}
		}
		// process removals
		BundleDescription[] existingBundles = baseState.getBundles();
		for (BundleDescription existing : existingBundles) {
			BundleDescription local = getBundleByLocation(existing.getLocation());
			if (local == null) {
				delta.recordBundleRemoved((BundleDescriptionImpl) existing);
			}
		}
		return delta;
	}
}
