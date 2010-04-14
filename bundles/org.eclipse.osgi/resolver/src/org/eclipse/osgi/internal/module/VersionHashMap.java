/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Danail Nachev -  ProSyst - bug 218625
 *******************************************************************************/
package org.eclipse.osgi.internal.module;

import java.util.*;

public class VersionHashMap extends MappedList implements Comparator {
	private ResolverImpl resolver;

	public VersionHashMap(ResolverImpl resolver) {
		this.resolver = resolver;
	}

	// assumes existing array is sorted
	// finds the index where to insert the new value
	protected int insertionIndex(Object[] existing, Object value) {
		int index = existing.length;
		if (compare(existing[existing.length - 1], value) > 0) {
			index = Arrays.binarySearch(existing, value, this);
			if (index < 0)
				index = -index - 1;
		}
		return index;
	}

	public void put(VersionSupplier[] versionSuppliers) {
		for (int i = 0; i < versionSuppliers.length; i++)
			put(versionSuppliers[i].getName(), versionSuppliers[i]);
	}

	public boolean contains(VersionSupplier vs) {
		return contains(vs, false) != null;
	}

	private VersionSupplier contains(VersionSupplier vs, boolean remove) {
		Object existing = internal.get(vs.getName());
		if (existing == null)
			return null;
		if (existing == vs) {
			if (remove)
				internal.remove(vs.getName());
			return vs;
		}
		if (!existing.getClass().isArray())
			return null;
		Object[] existingValues = (Object[]) existing;
		for (int i = 0; i < existingValues.length; i++)
			if (existingValues[i] == vs) {
				if (remove) {
					if (existingValues.length == 2) {
						internal.put(vs.getName(), existingValues[i == 0 ? 1 : 0]);
						return vs;
					}
					Object[] newExisting = new Object[existingValues.length - 1];
					System.arraycopy(existingValues, 0, newExisting, 0, i);
					if (i + 1 < existingValues.length)
						System.arraycopy(existingValues, i + 1, newExisting, i, existingValues.length - i - 1);
					internal.put(vs.getName(), newExisting);
				}
				return vs;
			}
		return null;
	}

	public Object remove(VersionSupplier toBeRemoved) {
		return contains(toBeRemoved, true);
	}

	public void remove(VersionSupplier[] versionSuppliers) {
		for (int i = 0; i < versionSuppliers.length; i++)
			remove(versionSuppliers[i]);
	}

	// Once we have resolved bundles, we need to make sure that version suppliers
	// from the resolved bundles are ahead of those from unresolved bundles
	void reorder() {
		for (Iterator it = internal.values().iterator(); it.hasNext();) {
			Object existing = it.next();
			if (!existing.getClass().isArray())
				continue;
			Arrays.sort((Object[]) existing, this);
		}
	}

	// Compares two VersionSuppliers for descending ordered sorts.
	// The VersionSuppliers are sorted by the following priorities
	// First the resolution status of the supplying bundle.
	// Second is the supplier version.
	// Third is the bundle id of the supplying bundle.
	public int compare(Object o1, Object o2) {
		if (!(o1 instanceof VersionSupplier) || !(o2 instanceof VersionSupplier))
			throw new IllegalArgumentException();
		VersionSupplier vs1 = (VersionSupplier) o1;
		VersionSupplier vs2 = (VersionSupplier) o2;
		// if the selection policy is set then use that
		if (resolver.getSelectionPolicy() != null)
			return resolver.getSelectionPolicy().compare(vs1.getBaseDescription(), vs2.getBaseDescription());
		String systemBundle = resolver.getSystemBundle();
		if (systemBundle.equals(vs1.getBundle().getSymbolicName()) && !systemBundle.equals(vs2.getBundle().getSymbolicName()))
			return -1;
		else if (!systemBundle.equals(vs1.getBundle().getSymbolicName()) && systemBundle.equals(vs2.getBundle().getSymbolicName()))
			return 1;
		if (vs1.getBundle().isResolved() != vs2.getBundle().isResolved())
			return vs1.getBundle().isResolved() ? -1 : 1;
		int versionCompare = -(vs1.getVersion().compareTo(vs2.getVersion()));
		if (versionCompare != 0)
			return versionCompare;
		return vs1.getBundle().getBundleId() <= vs2.getBundle().getBundleId() ? -1 : 1;
	}
}
