/*******************************************************************************
 * Copyright (c) 2011, 2015 VMware Inc.
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
import java.util.Iterator;
import org.eclipse.equinox.internal.region.EquinoxStateHelper;
import org.eclipse.equinox.region.*;
import org.osgi.framework.Bundle;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.wiring.*;

/**
 * {@link RegionResolverHook} manages the visibility of bundles across regions
 * according to the {@link RegionDigraph}.
 * <p />
 * 
 * <strong>Concurrent Semantics</strong><br />
 * Thread safe.
 */
public final class RegionResolverHook implements ResolverHook {

	private static final Boolean DEBUG = false;

	private final RegionDigraph regionDigraph;

	public RegionResolverHook(RegionDigraph regionDigraph) {
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
		// filter any revisions that have no region
		for (Iterator<BundleRevision> iCandidates = candidates.iterator(); iCandidates.hasNext();) {
			if (getRegion(iCandidates.next()) == null) {
				iCandidates.remove();
			}
		}
	}

	@Override
	public void filterSingletonCollisions(BundleCapability singleton,
			Collection<BundleCapability> collisionCandidates) {
		filterCandidates(singleton.getRevision(), collisionCandidates, true);
	}

	private void debugEntry(BundleRevision requirer, Collection<BundleCapability> candidates, boolean singleton) {
		System.out.println((singleton ? "Singleton" : "Requirer: ") + requirer.getSymbolicName() + "_" //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
				+ requirer.getVersion() + "[" + getBundleId(requirer) + "]"); //$NON-NLS-1$ //$NON-NLS-2$
		System.out.println("  Candidates: "); //$NON-NLS-1$
		for (BundleCapability c : candidates) {
			String namespace = c.getNamespace();
			if (BundleRevision.PACKAGE_NAMESPACE.equals(namespace)) {
				BundleRevision providerRevision = c.getRevision();
				String pkg = (String) c.getAttributes().get(BundleRevision.PACKAGE_NAMESPACE);
				System.out.println("    Package " + pkg + " from provider " + providerRevision.getSymbolicName() + "_" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						+ providerRevision.getVersion() + "[" + getBundleId(providerRevision) + "]"); //$NON-NLS-1$ //$NON-NLS-2$
				if (pkg.equals("slow")) { //$NON-NLS-1$
					System.out.println(">>> put breakpoint here <<<"); //$NON-NLS-1$
				}
			} else {
				BundleRevision providerRevision = c.getRevision();
				System.out.println("    Bundle from provider " + providerRevision.getSymbolicName() + "_" //$NON-NLS-1$ //$NON-NLS-2$
						+ providerRevision.getVersion() + "[" + getBundleId(providerRevision) + "]"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	private void debugExit(BundleRevision requirer, Collection<BundleCapability> candidates) {
		System.out.println("  Filtered candidates: "); //$NON-NLS-1$
		for (BundleCapability c : candidates) {
			String namespace = c.getNamespace();
			if (BundleRevision.PACKAGE_NAMESPACE.equals(namespace)) {
				BundleRevision providerRevision = c.getRevision();
				String pkg = (String) c.getAttributes().get(BundleRevision.PACKAGE_NAMESPACE);
				System.out.println("    Package " + pkg + " from provider " + providerRevision.getSymbolicName() + "_" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						+ providerRevision.getVersion() + "[" + getBundleId(providerRevision) + "]"); //$NON-NLS-1$ //$NON-NLS-2$
				if (pkg.equals("slow")) { //$NON-NLS-1$
					System.out.println(">>> put breakpoint here <<<"); //$NON-NLS-1$
				}
			} else {
				BundleRevision providerRevision = c.getRevision();
				System.out.println("    Bundle from provider " + providerRevision.getSymbolicName() + "_" //$NON-NLS-1$ //$NON-NLS-2$
						+ providerRevision.getVersion() + "[" + getBundleId(providerRevision) + "]"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}
}
