/*******************************************************************************
 * Copyright (c) 2011 VMware Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SpringSource, a division of VMware - initial API and implementation and/or initial documentation
 *******************************************************************************/

package org.eclipse.equinox.internal.region;

import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraphVisitor;
import org.eclipse.equinox.region.RegionDigraph.FilteredRegion;

import java.util.HashSet;
import java.util.Set;

/**
 * {@link SubgraphTraverser} is a utility for traversing a subgraph of a {@link RegionDigraph} calling a
 * {@link RegionDigraphVisitor} on the way.
 * <p />
 * 
 * <strong>Concurrent Semantics</strong><br />
 * Thread safe.
 */
final class SubgraphTraverser {

	void visitSubgraph(Region startingRegion, RegionDigraphVisitor visitor) {
		visitRemainingSubgraph(startingRegion, visitor, new HashSet<Region>());
	}

	private void visitRemainingSubgraph(Region r, RegionDigraphVisitor visitor, Set<Region> path) {
		if (!path.contains(r)) {
			if (visitor.visit(r)) {
				traverseEdges(r, visitor, path);
			}
		}
	}

	private void traverseEdges(Region r, RegionDigraphVisitor visitor, Set<Region> path) {
		for (FilteredRegion fr : r.getEdges()) {
			if (visitor.preEdgeTraverse(fr.getFilter())) {
				try {
					visitRemainingSubgraph(fr.getRegion(), visitor, extendPath(r, path));
				} finally {
					visitor.postEdgeTraverse(fr.getFilter());
				}
			}
		}
	}

	private Set<Region> extendPath(Region r, Set<Region> path) {
		Set<Region> newPath = new HashSet<Region>(path);
		newPath.add(r);
		return newPath;
	}

}