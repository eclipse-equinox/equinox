/*******************************************************************************
 * Copyright (c) 2011 VMware Inc.
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

package org.eclipse.equinox.internal.region.management;

import java.util.*;
import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraph;
import org.eclipse.equinox.region.RegionDigraph.FilteredRegion;
import org.eclipse.equinox.region.management.ManageableRegion;
import org.eclipse.equinox.region.management.ManageableRegionDigraph;

/**
 * {@link StandardManageableRegion} is the default implementation of
 * {@link ManageableRegion}.
 * <p>
 * <strong>Concurrent Semantics</strong>
 * </p>
 * Thread safe.
 */
public class StandardManageableRegion implements ManageableRegion {

	private final Region region;

	private final ManageableRegionDigraph manageableRegionDigraph;

	private final RegionDigraph regionDigraph;

	public StandardManageableRegion(Region region, ManageableRegionDigraph manageableRegionDigraph,
			RegionDigraph regionDigraph) {
		this.region = region;
		this.manageableRegionDigraph = manageableRegionDigraph;
		this.regionDigraph = regionDigraph;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return region.getName();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ManageableRegion[] getDependencies() {
		Set<FilteredRegion> edges = this.regionDigraph.getEdges(this.region);
		List<ManageableRegion> dependencies = new ArrayList<>();
		for (FilteredRegion edge : edges) {
			ManageableRegion manageableRegion = this.manageableRegionDigraph.getRegion(edge.getRegion().getName());
			if (manageableRegion != null) {
				dependencies.add(manageableRegion);
			}
		}
		return dependencies.toArray(new ManageableRegion[dependencies.size()]);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long[] getBundleIds() {
		Set<Long> bundleIds = this.region.getBundleIds();
		long[] result = new long[bundleIds.size()];
		int i = 0;
		for (Long bundleId : bundleIds) {
			result[i++] = bundleId;
		}
		return result;
	}

}
