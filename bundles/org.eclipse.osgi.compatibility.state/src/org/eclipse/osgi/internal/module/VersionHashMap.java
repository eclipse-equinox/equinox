/*******************************************************************************
 * Copyright (c) 2004, 2011 IBM Corporation and others.
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
 *     Danail Nachev -  ProSyst - bug 218625
 *******************************************************************************/
package org.eclipse.osgi.internal.module;

import java.util.*;

public class VersionHashMap<V extends VersionSupplier> extends MappedList<String, V> implements Comparator<V> {
	private final ResolverImpl resolver;
	private final boolean preferSystemPackages;

	public VersionHashMap(ResolverImpl resolver) {
		this.resolver = resolver;
		Dictionary<?, ?>[] allProperties = resolver.getState().getPlatformProperties();
		Object preferSystem = allProperties.length == 0 ? "true" : allProperties[0].get("osgi.resolver.preferSystemPackages"); //$NON-NLS-1$//$NON-NLS-2$
		if (preferSystem == null)
			preferSystem = "true"; //$NON-NLS-1$
		preferSystemPackages = Boolean.valueOf(preferSystem.toString()).booleanValue();
	}

	// assumes existing array is sorted
	// finds the index where to insert the new value
	@Override
	protected int insertionIndex(List<V> existing, V value) {
		int index = existing.size();
		if (compare(existing.get(existing.size() - 1), value) > 0) {
			index = Collections.binarySearch(existing, value, this);

			if (index < 0)
				index = -index - 1;
		}
		return index;
	}

	public void put(V[] versionSuppliers) {
		for (V versionSupplier : versionSuppliers) {
			put(versionSupplier.getName(), versionSupplier);
		}
	}

	public boolean contains(V vs) {
		return contains(vs, false) != null;
	}

	private V contains(V vs, boolean remove) {
		List<V> existing = internal.get(vs.getName());
		if (existing == null)
			return null;
		int index = existing.indexOf(vs);
		if (index >= 0) {
			if (remove) {
				existing.remove(index);
				if (existing.size() == 0)
					internal.remove(vs.getName());
			}
			return vs;
		}
		return null;
	}

	public V remove(V toBeRemoved) {
		return contains(toBeRemoved, true);
	}

	public void remove(V[] versionSuppliers) {
		for (V versionSupplier : versionSuppliers) {
			remove(versionSupplier);
		}
	}

	// Once we have resolved bundles, we need to make sure that version suppliers
	// from the resolved bundles are ahead of those from unresolved bundles
	void reorder() {
		for (List<V> existing : internal.values()) {
			if (existing.size() > 1)
				Collections.sort(existing, this);
		}
	}

	// Compares two VersionSuppliers for descending ordered sorts.
	// The VersionSuppliers are sorted by the following priorities
	// First the resolution status of the supplying bundle.
	// Second is the supplier version.
	// Third is the bundle id of the supplying bundle.
	@Override
	public int compare(V vs1, V vs2) {
		// if the selection policy is set then use that
		if (resolver.getSelectionPolicy() != null)
			return resolver.getSelectionPolicy().compare(vs1.getBaseDescription(), vs2.getBaseDescription());
		if (preferSystemPackages) {
			String systemBundle = resolver.getSystemBundle();
			if (systemBundle.equals(vs1.getBundleDescription().getSymbolicName()) && !systemBundle.equals(vs2.getBundleDescription().getSymbolicName()))
				return -1;
			else if (!systemBundle.equals(vs1.getBundleDescription().getSymbolicName()) && systemBundle.equals(vs2.getBundleDescription().getSymbolicName()))
				return 1;
		}
		if (vs1.getBundleDescription().isResolved() != vs2.getBundleDescription().isResolved())
			return vs1.getBundleDescription().isResolved() ? -1 : 1;
		int versionCompare = -(vs1.getVersion().compareTo(vs2.getVersion()));
		if (versionCompare != 0)
			return versionCompare;
		return Long.compare(vs1.getBundleDescription().getBundleId(), vs2.getBundleDescription().getBundleId());
	}
}
