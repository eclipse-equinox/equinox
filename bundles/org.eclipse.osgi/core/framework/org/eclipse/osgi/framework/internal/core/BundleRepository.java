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

package org.eclipse.osgi.framework.internal.core;

import java.util.*;

import org.eclipse.osgi.service.resolver.Version;

//TODO Does this class need to be synchronized?
public class BundleRepository {
	/** bundles by install order */
	private ArrayList bundlesByInstallOrder;	//TODO It does not seems to me that the users of this really care for the ordering. If this is confirmed this can be remove 
	//and getBundles could either return an array (an array is preferable as one if the method calling method finally returns an array and the others do not care)

	/** bundles keyed by bundle Id */
	private KeyedHashSet bundlesById;

	/** bundles keyed by SymbolicName */
	private Hashtable bundlesBySymbolicName;	//TODO Does this need to be synchronized

	/** PackageAdmin */
	private PackageAdmin packageAdmin;

	public BundleRepository(int initialCapacity, PackageAdmin packageAdmin) {
		bundlesByInstallOrder = new ArrayList(initialCapacity);
		bundlesById = new KeyedHashSet(initialCapacity, true);
		bundlesBySymbolicName = new Hashtable(initialCapacity);
		this.packageAdmin = packageAdmin;
	}

	/**
	 * Gets a list of bundles ordered by install order.
	 * @return List of bundles by install order.
	 */
	public List getBundles() {
		return bundlesByInstallOrder;
	}

	/**
	 * Gets a bundle by its bundle Id.
	 * @param bundleId
	 * @return
	 */
	public Bundle getBundle(long bundleId) {
		Long key = new Long(bundleId);
		return (Bundle) bundlesById.getByKey(key);
	}

	public Bundle[] getBundles(String symbolicName) {
		return (Bundle[]) bundlesBySymbolicName.get(symbolicName);
	}

	public Bundle getBundle(String symbolicName, String version) {
		Bundle[] bundles = (Bundle[]) bundlesBySymbolicName.get(symbolicName);
		if (bundles != null) {
			Version ver = new Version(version);
			if (bundles.length > 0) {
				for (int i = 0; i < bundles.length; i++) {
					if (bundles[i].getVersion().matchQualifier(ver)) {
						return bundles[i];
					}
				}
			}
		}
		return null;
	}

	public void add(Bundle bundle) {
		bundlesByInstallOrder.add(bundle);
		bundlesById.add(bundle);
		String symbolicName = bundle.getSymbolicName();
		if (symbolicName != null) {
			Bundle[] bundles = (Bundle[]) bundlesBySymbolicName.get(symbolicName);
			if (bundles == null) {
				// making the initial capacity on this 1 since it
				// should be rare that multiple version exist
				bundles = new Bundle[1];
				bundles[0] = bundle;
				bundlesBySymbolicName.put(symbolicName, bundles);
				return;
			}

			ArrayList list = new ArrayList(bundles.length + 1);
			// find place to insert the bundle
			Version newVersion = bundle.getVersion();
			boolean added = false;
			for (int i = 0; i < bundles.length; i++) {
				Bundle oldBundle = bundles[i];
				Version oldVersion = oldBundle.getVersion();
				if (!added && newVersion.matchGreaterOrEqualTo(oldVersion)) {
					added = true;
					list.add(bundle);
				}
				list.add(oldBundle);
			}
			if (!added) {
				list.add(bundle);
			}

			bundles = new Bundle[list.size()];
			list.toArray(bundles);
			bundlesBySymbolicName.put(symbolicName, bundles);
		}
	}

	public boolean remove(Bundle bundle) {
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

		Bundle[] bundles = (Bundle[]) bundlesBySymbolicName.get(symbolicName);
		if (bundles == null)
			return true;

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
				Bundle[] newBundles = new Bundle[bundles.length - numRemoved];
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
				
		return true;
	}

	public void removeAllBundles() {
		bundlesByInstallOrder.clear();
		bundlesById = new KeyedHashSet();
		bundlesBySymbolicName.clear();
	}

	public synchronized void markDependancies() {
		KeyedElement[] elements = bundlesById.elements();
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] instanceof BundleHost) {
				((BundleHost) elements[i]).getLoaderProxy().markDependencies();
			}
		}
	}

	public synchronized void unMarkDependancies(BundleLoaderProxy user) {
		KeyedElement[] elements = bundlesById.elements();
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] instanceof BundleHost) {
				BundleLoaderProxy loaderProxy = ((BundleHost) elements[i]).getLoaderProxy();
				loaderProxy.unMarkUsed(user);
			}
		}

		// look in removal pending
		int size = packageAdmin.removalPending.size();
		for (int i = 0; i < size; i++) {
			BundleLoaderProxy loaderProxy = (BundleLoaderProxy) packageAdmin.removalPending.elementAt(i);
			loaderProxy.unMarkUsed(user);
		}
		user.markedUsedDependencies = false;
	}
}
