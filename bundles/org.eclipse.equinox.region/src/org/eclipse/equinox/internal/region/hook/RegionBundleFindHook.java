/*******************************************************************************
 * Copyright (c) 2013, 2015 VMware Inc. and others
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

import java.util.Collection;
import java.util.HashMap;
import org.eclipse.equinox.region.*;
import org.osgi.framework.*;
import org.osgi.framework.hooks.bundle.FindHook;

/**
 * {@link RegionBundleFindHook} manages the visibility of bundles across regions
 * according to the {@link RegionDigraph}.
 * <p>
 * <strong>Concurrent Semantics</strong>
 * </p>
 * Thread safe.
 */
public final class RegionBundleFindHook implements FindHook {

	private final RegionDigraph regionDigraph;

	private final long hookImplID;

	public RegionBundleFindHook(RegionDigraph regionDigraph, long hookImplID) {
		this.regionDigraph = regionDigraph;
		this.hookImplID = hookImplID;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void find(BundleContext context, Collection<Bundle> bundles) {
		Bundle finderBundle = getBundle(context);
		if (finderBundle == null) {
			// invalid finder bundle; clear out result
			bundles.clear();
			return;
		}
		long bundleID = finderBundle.getBundleId();
		if (bundleID == 0 || bundleID == hookImplID) {
			// The system bundle and the hook impl bundle can see all bundles
			return;
		}

		Region finderRegion = this.regionDigraph.getRegion(finderBundle);
		RegionBundleFindHook.find(finderRegion, bundles);
	}

	static void find(Region finderRegion, Collection<Bundle> bundles) {
		if (finderRegion == null) {
			bundles.clear();
			return;
		}

		Visitor visitor = new Visitor(bundles);
		finderRegion.visitSubgraph(visitor);
		Collection<Bundle> allowed = visitor.getAllowed();

		bundles.retainAll(allowed);
	}

	static class Visitor extends RegionDigraphVisitorBase<Bundle> {

		Visitor(Collection<Bundle> candidates) {
			super(candidates);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected boolean contains(Region region, Bundle candidate) {
			return region.contains(candidate);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected boolean isAllowed(Bundle candidate, RegionFilter filter) {
			return filter.isAllowed(candidate) || isLifecycleAllowed(filter, candidate);
		}

		private boolean isLifecycleAllowed(RegionFilter filter, Bundle bundle) {
			HashMap<String, Object> attrs = new HashMap<>(4);
			String bsn = bundle.getSymbolicName();
			if (bsn != null) {
				attrs.put(RegionFilter.VISIBLE_BUNDLE_NAMESPACE, bsn);
				attrs.put(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE, bsn);
			}
			attrs.put(org.osgi.framework.Constants.BUNDLE_VERSION_ATTRIBUTE, bundle.getVersion());
			return filter.isAllowed(RegionFilter.VISIBLE_BUNDLE_LIFECYCLE_NAMESPACE, attrs);
		}
	}

	static Bundle getBundle(BundleContext context) {
		try {
			return context.getBundle();
		} catch (IllegalStateException e) {
			// happens if the context is invalid
			return null;
		}
	}
}
