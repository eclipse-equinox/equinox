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

import java.util.*;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.Bundle;

public class StateDeltaImpl implements StateDelta {
	private State state;
	private Map changes = new HashMap();

	public StateDeltaImpl(State state) {
		this.state = state;
	}

	public BundleDelta[] getChanges() {
		return (BundleDelta[]) changes.values().toArray(new BundleDelta[changes.size()]);
	}

	public BundleDelta[] getChanges(int mask, boolean exact) {
		List result = new ArrayList();
		for (Iterator changesIter = changes.values().iterator(); changesIter.hasNext();) {
			BundleDelta change = (BundleDelta) changesIter.next();
			if (change.getType() == mask || (!exact && (change.getType() & mask) == mask))
				result.add(change);
		}
		return (BundleDelta[]) result.toArray(new BundleDelta[result.size()]);
	}

	public State getState() {
		return state;
	}

	void recordBundleAdded(BundleDescriptionImpl added) {
		Object key = added.getKey();
		BundleDeltaImpl change = (BundleDeltaImpl) changes.get(key);
		if (change == null)
			changes.put(key, new BundleDeltaImpl(added, BundleDelta.ADDED));
		else
			change.setType(change.getType() | BundleDelta.ADDED);
	}

	void recordBundleUpdated(BundleDescriptionImpl updated) {
		Object key = updated.getKey();
		BundleDeltaImpl change = (BundleDeltaImpl) changes.get(key);
		if (change == null)
			changes.put(key, new BundleDeltaImpl(updated, BundleDelta.UPDATED));
		else
			change.setType(change.getType() | BundleDelta.UPDATED);
	}

	void recordBundleRemoved(BundleDescriptionImpl removed) {
		Object key = removed.getKey();
		BundleDeltaImpl change = (BundleDeltaImpl) changes.get(key);
		if (change == null)
			changes.put(key, new BundleDeltaImpl(removed, BundleDelta.REMOVED));
		else
			change.setType(change.getType() | BundleDelta.REMOVED);
	}

	void recordConstraintResolved(BundleDescriptionImpl changedLinkage, boolean optional) {
		Object key = changedLinkage.getKey();
		BundleDeltaImpl change = (BundleDeltaImpl) changes.get(key);
		int newType = optional ? BundleDelta.OPTIONAL_LINKAGE_CHANGED : BundleDelta.LINKAGE_CHANGED;
		// LINKAGE_CHANGED overrides OPTIONAL_LINKAGE_CHANGED, but nothing else
		if (change == null || (newType == BundleDelta.LINKAGE_CHANGED && change.getType() == BundleDelta.OPTIONAL_LINKAGE_CHANGED))
			changes.put(key, new BundleDeltaImpl(changedLinkage, newType));
	}

	void recordBundleResolved(BundleDescriptionImpl resolved, int status) {
		Object key = resolved.getKey();
		BundleDeltaImpl change = (BundleDeltaImpl) changes.get(key);
		int newType = status == Bundle.RESOLVED ? BundleDelta.RESOLVED : BundleDelta.UNRESOLVED;
		if (change == null) {
			change = new BundleDeltaImpl(resolved, newType);
			changes.put(key, change);
			return;
		}
		int currentType = change.getType();
		if ((newType == BundleDelta.RESOLVED && currentType == BundleDelta.UNRESOLVED) || (newType == BundleDelta.UNRESOLVED && currentType == BundleDelta.RESOLVED)) {
			changes.remove(key);
			return;
		}
		// new type will have only one of RESOLVED|UNRESOLVED bits set
		newType = newType | (currentType & ~(BundleDelta.RESOLVED | BundleDelta.UNRESOLVED));
		change.setType(newType);
	}
}