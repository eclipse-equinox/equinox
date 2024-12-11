/*******************************************************************************
 * Copyright (c) 2015, 2016 VMware Inc.
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
import org.eclipse.equinox.region.*;
import org.osgi.framework.*;
import org.osgi.framework.hooks.service.FindHook;

/**
 * {@link RegionServiceFindHook} manages the visibility of services across
 * regions according to the {@link RegionDigraph}.
 * <p>
 * <strong>Concurrent Semantics</strong>
 * </p>
 * Thread safe.
 */
public final class RegionServiceFindHook implements FindHook {

	private final RegionDigraph regionDigraph;

	public RegionServiceFindHook(RegionDigraph regionDigraph) {
		this.regionDigraph = regionDigraph;
	}

	/**
	 * {@inheritDoc}
	 */
	public void find(BundleContext context, String name, String filter, boolean allServices,
			Collection<ServiceReference<?>> references) {
		Bundle finderBundle = RegionBundleFindHook.getBundle(context);
		if (finderBundle == null) {
			// invalid finder bundle; clear out result
			references.clear();
			return;
		}
		if (finderBundle.getBundleId() == 0L) {
			return;
		}

		Region finderRegion = this.regionDigraph.getRegion(finderBundle);
		RegionServiceFindHook.find(finderRegion, references);
	}

	static void find(Region finderRegion, Collection<ServiceReference<?>> references) {
		if (finderRegion == null) {
			references.clear();
			return;
		}

		Visitor visitor = new Visitor(references);
		finderRegion.visitSubgraph(visitor);
		Collection<ServiceReference<?>> allowed = visitor.getAllowed();

		references.retainAll(allowed);
	}

	static class Visitor extends RegionDigraphVisitorBase<ServiceReference<?>> {

		Visitor(Collection<ServiceReference<?>> candidates) {
			super(candidates);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected boolean contains(Region region, ServiceReference<?> candidate) {
			Bundle b = candidate.getBundle();
			return b == null ? false : region.contains(b);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected boolean isAllowed(ServiceReference<?> candidate, RegionFilter filter) {
			return filter.isAllowed(candidate) || filter.isAllowed(candidate.getBundle());
		}

	}
}
