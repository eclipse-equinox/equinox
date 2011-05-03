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
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.hooks.service.FindHook;

/**
 * {@link RegionServiceFindHook} manages the visibility of services across regions according to the
 * {@link RegionDigraph}.
 * <p />
 * 
 * <strong>Concurrent Semantics</strong><br />
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
	public void find(BundleContext context, String name, String filter, boolean allServices, Collection<ServiceReference<?>> references) {
		if (context.getBundle().getBundleId() == 0L) {
			return;
		}

		Region finderRegion = getRegion(context);
		if (finderRegion == null) {
			references.clear();
			return;
		}

		Visitor visitor = new Visitor(references);
		finderRegion.visitSubgraph(visitor);
		Collection<ServiceReference<?>> allowed = visitor.getAllowed();

		references.retainAll(allowed);
	}

	class Visitor extends RegionDigraphVisitorBase<ServiceReference<?>> {

		Visitor(Collection<ServiceReference<?>> candidates) {
			super(candidates);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected boolean contains(Region region, ServiceReference<?> candidate) {
			return region.contains(candidate.getBundle());
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected boolean isAllowed(ServiceReference<?> candidate, RegionFilter filter) {
			return filter.isAllowed(candidate) || filter.isAllowed(candidate.getBundle());
		}

	}

	private Region getRegion(BundleContext context) {
		return this.regionDigraph.getRegion(context.getBundle());
	}

}
