/*******************************************************************************
 * Copyright (c) 2003, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Rob Harrop - SpringSource Inc. (bug 247521) 
 *******************************************************************************/

package org.eclipse.osgi.framework.internal.core;

import java.util.*;
import org.eclipse.osgi.framework.util.KeyedHashSet;
import org.osgi.framework.Version;

/**
 * The BundleRepository holds all installed Bundle object for the
 * Framework.  The BundleRepository is also used to mark and unmark
 * bundle dependancies.
 *
 * <p> 
 * This class is internally synchronized and supports client locking. Clients
 * wishing to perform threadsafe composite operations on instances of this
 * class can synchronize on the instance itself when doing these operations.
 */
public final class BundleRepository {
	/** bundles by install order */
	private List<AbstractBundle> bundlesByInstallOrder;

	/** bundles keyed by bundle Id */
	private KeyedHashSet bundlesById;

	/** bundles keyed by SymbolicName */
	private Map<String, AbstractBundle[]> bundlesBySymbolicName;

	public BundleRepository(int initialCapacity) {
		synchronized (this) {
			bundlesByInstallOrder = new ArrayList<AbstractBundle>(initialCapacity);
			bundlesById = new KeyedHashSet(initialCapacity, true);
			bundlesBySymbolicName = new HashMap<String, AbstractBundle[]>(initialCapacity);
		}
	}

	/**
	 * Gets a list of bundles ordered by install order.
	 * @return List of bundles by install order.
	 */
	public synchronized List<AbstractBundle> getBundles() {
		return bundlesByInstallOrder;
	}

	/**
	 * Gets a bundle by its bundle Id.
	 * @param bundleId
	 * @return a bundle with the specified id or null if one does not exist
	 */
	public synchronized AbstractBundle getBundle(long bundleId) {
		Long key = new Long(bundleId);
		return (AbstractBundle) bundlesById.getByKey(key);
	}

	public synchronized AbstractBundle[] getBundles(String symbolicName) {
		if (Constants.SYSTEM_BUNDLE_SYMBOLICNAME.equals(symbolicName))
			symbolicName = Constants.getInternalSymbolicName();
		return bundlesBySymbolicName.get(symbolicName);
	}

	@SuppressWarnings("unchecked")
	public synchronized List<AbstractBundle> getBundles(String symbolicName, Version version) {
		AbstractBundle[] bundles = getBundles(symbolicName);
		List<AbstractBundle> result = null;
		if (bundles != null) {
			if (bundles.length > 0) {
				for (int i = 0; i < bundles.length; i++) {
					if (bundles[i].getVersion().equals(version)) {
						if (result == null)
							result = new ArrayList<AbstractBundle>();
						result.add(bundles[i]);
					}
				}
			}
		}
		return result == null ? Collections.EMPTY_LIST : result;
	}

	public synchronized void add(AbstractBundle bundle) {
		bundlesByInstallOrder.add(bundle);
		bundlesById.add(bundle);
		addSymbolicName(bundle);
	}

	private void addSymbolicName(AbstractBundle bundle) {
		String symbolicName = bundle.getSymbolicName();
		if (symbolicName == null)
			return;
		AbstractBundle[] bundles = bundlesBySymbolicName.get(symbolicName);
		if (bundles == null) {
			// making the initial capacity on this 1 since it
			// should be rare that multiple version exist
			bundles = new AbstractBundle[1];
			bundles[0] = bundle;
			bundlesBySymbolicName.put(symbolicName, bundles);
			return;
		}

		List<AbstractBundle> list = new ArrayList<AbstractBundle>(bundles.length + 1);
		// find place to insert the bundle
		Version newVersion = bundle.getVersion();
		boolean added = false;
		for (int i = 0; i < bundles.length; i++) {
			AbstractBundle oldBundle = bundles[i];
			Version oldVersion = oldBundle.getVersion();
			if (!added && newVersion.compareTo(oldVersion) >= 0) {
				added = true;
				list.add(bundle);
			}
			list.add(oldBundle);
		}
		if (!added) {
			list.add(bundle);
		}

		bundles = new AbstractBundle[list.size()];
		list.toArray(bundles);
		bundlesBySymbolicName.put(symbolicName, bundles);
	}

	public synchronized boolean remove(AbstractBundle bundle) {
		// remove by bundle ID
		boolean found = bundlesById.remove(bundle);
		if (!found)
			return false;

		// remove by install order
		bundlesByInstallOrder.remove(bundle);
		// remove by symbolic name
		String symbolicName = bundle.getSymbolicName();
		if (symbolicName == null)
			return true;
		removeSymbolicName(symbolicName, bundle);
		return true;
	}

	private void removeSymbolicName(String symbolicName, AbstractBundle bundle) {
		AbstractBundle[] bundles = bundlesBySymbolicName.get(symbolicName);
		if (bundles == null)
			return;

		// found some bundles with the global name.
		// remove all references to the specified bundle.
		int numRemoved = 0;
		for (int i = 0; i < bundles.length; i++) {
			if (bundle == bundles[i]) {
				numRemoved++;
				bundles[i] = null;
			}
		}
		if (numRemoved > 0) {
			if (bundles.length - numRemoved <= 0) {
				// no bundles left in the array remove the array from the hash
				bundlesBySymbolicName.remove(symbolicName);
			} else {
				// create a new array with the null entries removed.
				AbstractBundle[] newBundles = new AbstractBundle[bundles.length - numRemoved];
				int indexCnt = 0;
				for (int i = 0; i < bundles.length; i++) {
					if (bundles[i] != null) {
						newBundles[indexCnt] = bundles[i];
						indexCnt++;
					}
				}
				bundlesBySymbolicName.put(symbolicName, newBundles);
			}
		}
	}

	public synchronized void update(String oldSymbolicName, AbstractBundle bundle) {
		if (oldSymbolicName != null) {
			if (!oldSymbolicName.equals(bundle.getSymbolicName())) {
				removeSymbolicName(oldSymbolicName, bundle);
				addSymbolicName(bundle);
			}
		} else {
			addSymbolicName(bundle);
		}
	}

	public synchronized void removeAllBundles() {
		bundlesByInstallOrder.clear();
		bundlesById = new KeyedHashSet();
		bundlesBySymbolicName.clear();
	}
}
