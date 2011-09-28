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

package org.eclipse.equinox.internal.region.hook;

import java.util.*;
import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraph;
import org.osgi.framework.*;
import org.osgi.framework.hooks.bundle.EventHook;
import org.osgi.framework.hooks.bundle.FindHook;

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

	private final FindHook bundleFindHook;

	private final ThreadLocal<Region> threadLocal;

	public RegionBundleEventHook(RegionDigraph regionDigraph, FindHook bundleFindBook, ThreadLocal<Region> threadLocal) {
		this.regionDigraph = regionDigraph;
		this.bundleFindHook = bundleFindBook;
		this.threadLocal = threadLocal;
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
		Iterator<BundleContext> i = contexts.iterator();
		while (i.hasNext()) {
			if (!find(i.next(), eventBundle)) {
				i.remove();
			}
		}
		if (event.getType() == BundleEvent.UNINSTALLED) {
			bundleUninstalled(eventBundle);
		}
	}

	private boolean find(BundleContext finderBundleContext, Bundle candidateBundle) {
		Collection<Bundle> candidates = new ArrayList<Bundle>(1);
		candidates.add(candidateBundle);
		this.bundleFindHook.find(finderBundleContext, candidates);
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
