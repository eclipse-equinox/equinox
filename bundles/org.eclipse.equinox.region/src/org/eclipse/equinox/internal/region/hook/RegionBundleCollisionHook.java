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

import java.util.Arrays;
import java.util.Collection;
import org.eclipse.equinox.region.*;
import org.osgi.framework.Bundle;
import org.osgi.framework.hooks.bundle.CollisionHook;

/**
 * {@link RegionBundleCollisionHook} manages the collision policy of duplicate bundles.
 * 
 * <strong>Concurrent Semantics</strong><br />
 * Thread safe.
 */
public final class RegionBundleCollisionHook implements CollisionHook {

	private final RegionDigraph regionDigraph;

	private final ThreadLocal<Region> threadLocal;

	public RegionBundleCollisionHook(RegionDigraph regionDigraph, ThreadLocal<Region> threadLocal) {
		this.regionDigraph = regionDigraph;
		this.threadLocal = threadLocal;
	}

	private Region getInstallRegion(Bundle originBundle) {
		/*
		 * BundleIdBasedRegion sets thread local to install bundles into arbitrary regions. If this is not set, the
		 * bundle inherits the region of the origin bundle.
		 */
		Region installRegion = this.threadLocal.get();
		if (installRegion != null) {
			return installRegion;
		}
		return this.regionDigraph.getRegion(originBundle);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void filterCollisions(int operationType, Bundle target, Collection<Bundle> collisionCandidates) {
		Region targetRegion = getInstallRegion(target);
		if (targetRegion == null) {
			// don't know if this is a collision or not just return all
			return;
		}

		// First check the collision candidates from the perspective of the
		// installing/updating targetRegion
		VisitorFromTarget fromTarget = new VisitorFromTarget(collisionCandidates);
		targetRegion.visitSubgraph(fromTarget);
		Collection<Bundle> collisions = fromTarget.getAllowed();

		if (collisions.isEmpty()) {
			// must do a sanity check to make sure the newly installed/updated bundle 
			// does not collide from the perspective of the collision candidate regions
			for (Bundle collisionCandidate : collisionCandidates) {
				Region candidateRegion = regionDigraph.getRegion(collisionCandidate);
				// we know the collision candidates all have the BSN/Version that collide.
				// we use the collision candidate and pretend it is part of the target region
				// to see if we can see it from the candidateRegion
				VisitorFromCandidate fromCandidate = new VisitorFromCandidate(Arrays.asList(collisionCandidate), targetRegion);
				candidateRegion.visitSubgraph(fromCandidate);
				collisions = fromCandidate.getAllowed();
				if (!collisions.isEmpty()) {
					// we can break at the first one since it only takes one to fail the install/update
					break;
				}
			}
		}

		collisionCandidates.retainAll(collisions);
	}

	class VisitorFromTarget extends RegionDigraphVisitorBase<Bundle> {

		VisitorFromTarget(Collection<Bundle> candidates) {
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

	class VisitorFromCandidate extends RegionDigraphVisitorBase<Bundle> {
		private final Region targetRegion;

		VisitorFromCandidate(Collection<Bundle> candidates, Region targetRegion) {
			super(candidates);
			this.targetRegion = targetRegion;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected boolean contains(Region region, Bundle candidate) {
			return region.equals(targetRegion);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected boolean isAllowed(Bundle candidate, RegionFilter filter) {
			return filter.isAllowed(candidate);
		}

	}
}
