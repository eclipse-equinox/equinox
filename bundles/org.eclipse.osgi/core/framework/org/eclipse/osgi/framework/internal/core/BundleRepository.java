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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.eclipse.osgi.framework.adaptor.Version;

public class BundleRepository {
	/** bundles by install order */
	private ArrayList bundlesByInstallOrder;

	/** bundles keyed by bundle Id */
	private KeyedHashSet bundlesById;

	/** bundles keyed by GlobalName */
	private Hashtable bundlesByGlobalName;

	/** PackageAdmin */
	private PackageAdmin packageAdmin;

	public BundleRepository(int initialCapacity, PackageAdmin packageAdmin) {
		bundlesByInstallOrder = new ArrayList(initialCapacity);
		bundlesById = new KeyedHashSet(initialCapacity,true);
		bundlesByGlobalName = new Hashtable(initialCapacity);
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
		return (Bundle)bundlesById.getByKey(key);
	}

	public Bundle[] getBundles(String globalName) {
		return (Bundle[]) bundlesByGlobalName.get(globalName);
	}

	public Bundle getBundle(String globalName, String version){
		ArrayList list = (ArrayList) bundlesByGlobalName.get(globalName);
		if (list != null) {
			Version ver = new Version(version);
			int size = list.size();
			if (size>0) {
				for(int i=0; i<size; i++) {
					Bundle bundle = (Bundle) list.get(i);
					if (bundle.getVersion().isPerfect(ver)) {
						return bundle;
					}
				}
			}
		}
		return null;
	}

	public void add(Bundle bundle) {
		bundlesByInstallOrder.add(bundle);
		bundlesById.add(bundle);
		String globalName = bundle.getGlobalName();
		if (globalName != null) {
			Bundle[] bundles = (Bundle[]) bundlesByGlobalName.get(globalName);
			if (bundles == null) {
				// making the initial capacity on this 1 since it
				// should be rare that multiple version exist
				bundles = new Bundle[1];
				bundles[0] = bundle;
				bundlesByGlobalName.put(globalName,bundles);
				return;
			}
			ArrayList list = new ArrayList(bundles.length+1);
			// find place to insert the bundle
			Version newVersion = bundle.getVersion();
			boolean added = false;
			for (int i=0; i<bundles.length; i++) {
				Bundle oldBundle = bundles[i];
				Version oldVersion = oldBundle.getVersion();
				if (!added && newVersion.isGreaterOrEqualTo(oldVersion)){
					added = true;
					list.add(bundle);
				}
				list.add(oldBundle);
			}
			if (!added) {
				list.add(bundle);
			}

			list.toArray(bundles);
			bundlesByGlobalName.put(globalName,bundles);
		}
	}

	public boolean remove(Bundle bundle) {
		boolean removed = bundlesById.remove(bundle);
		if (removed) {
			bundlesByInstallOrder.remove(bundle);
			String globalName = bundle.getGlobalName();
			if (globalName != null) {
				ArrayList list = (ArrayList) bundlesByGlobalName.get(globalName);
				if (list != null) {
					list.remove(bundle);
				}
			}
		}
		return removed;
	}

	public void removeAllBundles() {
		bundlesByInstallOrder.clear();
		bundlesById = new KeyedHashSet();
		bundlesByGlobalName.clear();
	}

	public synchronized void markDependancies() {
		KeyedElement[] elements = bundlesById.elements();
		for(int i=0; i<elements.length; i++) {
			if (elements[i] instanceof BundleHost) {
				((BundleHost)elements[i]).getLoaderProxy().markDependencies();
			}
		}
	}

	public synchronized void unMarkDependancies(BundleLoaderProxy user) {
		KeyedElement[] elements = bundlesById.elements();
		for(int i=0; i<elements.length; i++) {
			if (elements[i] instanceof BundleHost) {
				BundleLoaderProxy loaderProxy = ((BundleHost)elements[i]).getLoaderProxy();
				loaderProxy.unMarkUsed(user);
			}
		}

		// look in removal pending
		int size = packageAdmin.removalPending.size();
		for (int i=0; i<size; i++) {
			BundleLoaderProxy loaderProxy = (BundleLoaderProxy) packageAdmin.removalPending.elementAt(i);
			loaderProxy.unMarkUsed(user);
		}
		user.markedUsedDependencies = false;
	}
}
