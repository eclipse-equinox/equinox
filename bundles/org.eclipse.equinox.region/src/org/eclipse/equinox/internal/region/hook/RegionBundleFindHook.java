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

import java.util.Collection;
import org.eclipse.equinox.region.*;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.hooks.bundle.FindHook;

/**
 * {@link RegionBundleFindHook} manages the visibility of bundles across regions according to the {@link RegionDigraph}.
 * <p />
 * 
 * <strong>Concurrent Semantics</strong><br />
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
		long bundleID = context.getBundle().getBundleId();

		if (bundleID == 0 || bundleID == hookImplID) {
			// The system bundle and the hook impl bundle can see all bundles
			return;
		}

		Region finderRegion = getRegion(context);
		if (finderRegion == null) {
			bundles.clear();
			return;
		}

		Visitor visitor = new Visitor(bundles);
		finderRegion.visitSubgraph(visitor);
		Collection<Bundle> allowed = visitor.getAllowed();

		bundles.retainAll(allowed);
	}

	class Visitor extends RegionDigraphVisitorBase<Bundle> {

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
			return filter.isAllowed(candidate);
		}

	}

	private Region getRegion(BundleContext context) {
		return this.regionDigraph.getRegion(context.getBundle());
	}
}
