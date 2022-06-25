/*******************************************************************************
 * Copyright (c) 2011, 2015 VMware Inc. and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   VMware Inc. - initial contribution
 *******************************************************************************/

package org.eclipse.equinox.internal.region.hook;

import java.util.*;
import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraph;
import org.osgi.framework.*;
import org.osgi.framework.hooks.bundle.EventHook;

/**
 * {@link RegionBundleEventHook} manages the visibility of bundle events across regions according to the
 * {@link RegionDigraph}.
 * <p>
 * The current implementation delegates to {@link RegionBundleFindHook}. This is likely to perform adequately because of
 * the low frequency of bundle events and the typically small number of bundle listeners.
 * <p />
 * 
 * <strong>Concurrent Semantics</strong><br />
 * Thread safe.
 */
public final class RegionBundleEventHook implements EventHook {

	private final RegionDigraph regionDigraph;

	private final ThreadLocal<Region> threadLocal;

	private final long hookImplID;

	public RegionBundleEventHook(RegionDigraph regionDigraph, ThreadLocal<Region> threadLocal, long hookImplID) {
		this.regionDigraph = regionDigraph;
		this.threadLocal = threadLocal;
		this.hookImplID = hookImplID;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void event(BundleEvent event, Collection<BundleContext> contexts) {
		Bundle eventBundle = event.getBundle();
		if (event.getType() == BundleEvent.INSTALLED) {
			bundleInstalled(eventBundle, event.getOrigin());
		}
		Map<Region, Boolean> regionAccess = new HashMap<>();
		Iterator<BundleContext> i = contexts.iterator();
		while (i.hasNext()) {
			Bundle bundle = RegionBundleFindHook.getBundle(i.next());
			if (bundle == null) {
				// no bundle for context remove access from it
				i.remove();
				continue;
			}

			long bundleID = bundle.getBundleId();
			if (bundleID == 0 || bundleID == hookImplID) {
				// The system bundle and the hook impl bundle can see all bundles
				continue;
			}
			Region region = regionDigraph.getRegion(bundle);
			if (region == null) {
				// no region for context remove access from it
				i.remove();
			} else {
				Boolean accessible = regionAccess.get(region);
				if (accessible == null) {
					// we have not checked this region's access do it now
					accessible = isAccessible(region, eventBundle);
					regionAccess.put(region, accessible);
				}
				if (!accessible) {
					i.remove();
				}
			}
		}
		if (event.getType() == BundleEvent.UNINSTALLED) {
			bundleUninstalled(eventBundle);
		}
	}

	private boolean isAccessible(Region region, Bundle candidateBundle) {
		Collection<Bundle> candidates = new ArrayList<>(1);
		candidates.add(candidateBundle);
		RegionBundleFindHook.find(region, candidates);
		return !candidates.isEmpty();
	}

	private void bundleInstalled(Bundle eventBundle, Bundle originBundle) {
		/*
		 * BundleIdBasedRegion sets thread local to install bundles into arbitrary regions. If this is not set, the
		 * bundle inherits the region of the origin bundle.
		 */
		Region installRegion = this.threadLocal.get();
		if (installRegion != null) {
			addBundleToRegion(eventBundle, installRegion);
		} else {
			Region defaultAssignRegion = this.regionDigraph.getDefaultRegion();
			if (defaultAssignRegion != null) {
				addBundleToRegion(eventBundle, defaultAssignRegion);
			} else {
				Region originRegion = this.regionDigraph.getRegion(originBundle);
				if (originRegion != null) {
					addBundleToRegion(eventBundle, originRegion);
				}
			}
		}
	}

	private void addBundleToRegion(Bundle eventBundle, Region region) {
		try {
			region.addBundle(eventBundle);
		} catch (BundleException e) {
			e.printStackTrace();
			throw new RuntimeException("Bundle could not be added to region", e); //$NON-NLS-1$
		}
	}

	private void bundleUninstalled(Bundle eventBundle) {
		Region region = this.regionDigraph.getRegion(eventBundle);
		if (region != null) {
			region.removeBundle(eventBundle);
		}
	}

}
