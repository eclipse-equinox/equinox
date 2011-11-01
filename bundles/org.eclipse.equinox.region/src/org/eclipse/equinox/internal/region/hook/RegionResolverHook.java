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
import java.util.Iterator;
import org.eclipse.equinox.internal.region.EquinoxStateHelper;
import org.eclipse.equinox.region.*;
import org.osgi.framework.Bundle;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.wiring.*;

/**
 * {@link RegionResolverHook} manages the visibility of bundles across regions according to the {@link RegionDigraph}.
 * <p />
 * 
 * <strong>Concurrent Semantics</strong><br />
 * Thread safe.
 */
final class RegionResolverHook implements ResolverHook {

	private static final Boolean DEBUG = false;

	private final RegionDigraph regionDigraph;

	RegionResolverHook(RegionDigraph regionDigraph) {
		this.regionDigraph = regionDigraph;
	}

	@Override
	public void filterMatches(BundleRequirement requirement, Collection<BundleCapability> candidates) {
		filterCandidates(requirement.getRevision(), candidates, false);
	}

	private void filterCandidates(BundleRevision requirer, Collection<BundleCapability> candidates, boolean singleton) {
		try {
			if (DEBUG) {
				debugEntry(requirer, candidates, singleton);
			}

			if (getBundleId(requirer) == 0L) {
				return;
			}

			Region requirerRegion = getRegion(requirer);
			if (requirerRegion == null) {
				// for singleton check; keep all collisions
				if (!singleton) {
					candidates.clear();
				}
				return;
			}

			Visitor visitor = new Visitor(candidates);
			requirerRegion.visitSubgraph(visitor);
			Collection<BundleCapability> allowed = visitor.getAllowed();

			candidates.retainAll(allowed);
		} finally {
			if (DEBUG) {
				debugExit(requirer, candidates);
			}
		}
	}

	class Visitor extends RegionDigraphVisitorBase<BundleCapability> {

		Visitor(Collection<BundleCapability> candidates) {
			super(candidates);
		}

		@Override
		protected boolean contains(Region region, BundleCapability candidate) {
			return region.equals(getRegion(candidate.getRevision()));
		}

		@Override
		protected boolean isAllowed(BundleCapability candidate, RegionFilter filter) {
			return filter.isAllowed(candidate) || filter.isAllowed(candidate.getRevision());
		}

	}

	Region getRegion(BundleRevision bundleRevision) {
		Bundle bundle = bundleRevision.getBundle();
		if (bundle != null) {
			return getRegion(bundle);
		}
		Long bundleId = getBundleId(bundleRevision);
		return getRegion(bundleId);
	}

	private Region getRegion(Long bundleId) {
		return this.regionDigraph.getRegion(bundleId);
	}

	private Long getBundleId(BundleRevision bundleRevision) {
		return EquinoxStateHelper.getBundleId(bundleRevision);
	}

	private Region getRegion(Bundle bundle) {
		return this.regionDigraph.getRegion(bundle);
	}

	@Override
	public void end() {
		// do nothing
	}

	@Override
	public void filterResolvable(Collection<BundleRevision> candidates) {
		// do nothing
		// may want to consider only allowing candidates contained in the region of the trigger revisions?
	}

	@Override
	public void filterSingletonCollisions(BundleCapability singleton, Collection<BundleCapability> collisionCandidates) {
		filterCandidates(singleton.getRevision(), collisionCandidates, true);
	}

	private void debugEntry(BundleRevision requirer, Collection<BundleCapability> candidates, boolean singleton) {
		System.out.println((singleton ? "Singleton" : "Requirer: ") + requirer.getSymbolicName() + "_" + requirer.getVersion() + "[" + getBundleId(requirer) + "]"); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		System.out.println("  Candidates: "); //$NON-NLS-1$
		Iterator<BundleCapability> i = candidates.iterator();
		while (i.hasNext()) {
			BundleCapability c = i.next();
			String namespace = c.getNamespace();
			if (BundleRevision.PACKAGE_NAMESPACE.equals(namespace)) {
				BundleRevision providerRevision = c.getRevision();
				String pkg = (String) c.getAttributes().get(BundleRevision.PACKAGE_NAMESPACE);
				System.out.println("    Package " + pkg + " from provider " + providerRevision.getSymbolicName() + "_" + providerRevision.getVersion() + "[" + getBundleId(providerRevision) + "]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
				if (pkg.equals("slow")) { //$NON-NLS-1$
					System.out.println(">>> put breakpoint here <<<"); //$NON-NLS-1$
				}
			} else {
				BundleRevision providerRevision = c.getRevision();
				System.out.println("    Bundle from provider " + providerRevision.getSymbolicName() + "_" + providerRevision.getVersion() + "[" + getBundleId(providerRevision) + "]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			}
		}
	}

	private void debugExit(BundleRevision requirer, Collection<BundleCapability> candidates) {
		System.out.println("  Filtered candidates: "); //$NON-NLS-1$
		Iterator<BundleCapability> i = candidates.iterator();
		while (i.hasNext()) {
			BundleCapability c = i.next();
			String namespace = c.getNamespace();
			if (BundleRevision.PACKAGE_NAMESPACE.equals(namespace)) {
				BundleRevision providerRevision = c.getRevision();
				String pkg = (String) c.getAttributes().get(BundleRevision.PACKAGE_NAMESPACE);
				System.out.println("    Package " + pkg + " from provider " + providerRevision.getSymbolicName() + "_" + providerRevision.getVersion() + "[" + getBundleId(providerRevision) + "]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
				if (pkg.equals("slow")) { //$NON-NLS-1$
					System.out.println(">>> put breakpoint here <<<"); //$NON-NLS-1$
				}
			} else {
				BundleRevision providerRevision = c.getRevision();
				System.out.println("    Bundle from provider " + providerRevision.getSymbolicName() + "_" + providerRevision.getVersion() + "[" + getBundleId(providerRevision) + "]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			}
		}
	}
}
