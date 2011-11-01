/*******************************************************************************
 * Copyright (c) 2011 VMware Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   VMware Inc. - initial contribution
 *******************************************************************************/

package org.eclipse.equinox.internal.region;

import java.util.*;
import java.util.Map.Entry;
import org.eclipse.equinox.region.Region;
import org.osgi.framework.BundleException;

final class StandardBundleIdToRegionMapping implements BundleIdToRegionMapping {

	private final Object monitor = new Object();

	/*
	 * bundleToRegion maps a given bundle id to the region for which it belongs.
	 * this is a global map for all regions in the digraph
	 */
	private final Map<Long, Region> bundleToRegion = new HashMap<Long, Region>();

	/**
	 * {@inheritDoc} 
	 **/
	@Override
	public void associateBundleWithRegion(long bundleId, Region region) throws BundleException {
		synchronized (this.monitor) {
			Region r = this.bundleToRegion.get(bundleId);
			if (r != null && r != region) {
				throw new BundleException("Bundle '" + bundleId + "' is already associated with region '" + r + "'", BundleException.INVALID_OPERATION); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			this.bundleToRegion.put(bundleId, region);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void dissociateBundleFromRegion(long bundleId, Region region) {
		synchronized (this.monitor) {
			this.bundleToRegion.remove(bundleId);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isBundleAssociatedWithRegion(long bundleId, Region region) {
		synchronized (this.monitor) {
			return this.bundleToRegion.get(bundleId) == region;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<Long> getBundleIds(Region region) {
		Set<Long> bundleIds = new HashSet<Long>();
		synchronized (this.monitor) {
			for (Map.Entry<Long, Region> entry : this.bundleToRegion.entrySet()) {
				if (entry.getValue() == region) {
					bundleIds.add(entry.getKey());
				}
			}
		}
		return Collections.unmodifiableSet(bundleIds);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clear() {
		synchronized (this.monitor) {
			this.bundleToRegion.clear();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Region getRegion(long bundleId) {
		synchronized (this.monitor) {
			return this.bundleToRegion.get(bundleId);
		}
	}

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public void dissociateRegion(Region region) {
		synchronized (this.monitor) {
			Iterator<Entry<Long, Region>> iterator = this.bundleToRegion.entrySet().iterator();
			while (iterator.hasNext()) {
				Entry<Long, Region> entry = iterator.next();
				if (entry.getValue() == region) {
					iterator.remove();
				}
			}
		}
	}
}
