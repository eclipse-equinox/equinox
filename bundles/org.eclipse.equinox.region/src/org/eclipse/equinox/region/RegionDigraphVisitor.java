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

package org.eclipse.equinox.region;

/**
 * {@link RegionDigraphVisitor} is used to traverse a subgraph of a {@link RegionDigraph}.
 * <p />
 * 
 * <strong>Concurrent Semantics</strong><br />
 * 
 * Implementations of this interface must be thread safe.
 */
public interface RegionDigraphVisitor {

	/**
	 * Visits the given region and determines whether or not to continue traversing.
	 * 
	 * @param region the region to visit
	 * @return <code>true</code> if the traversal is to continue and <code>false</code> otherwise
	 */
	boolean visit(Region region);

	/**
	 * Prepares to traverse an edge with the given {@link RegionFilter} and determines whether or not to traverse the
	 * edge.
	 * 
	 * @param regionFilter the {@link RegionFilter} of the edge to be traversed
	 * @return <code>true</code> if the edge is to be traversed and <code>false</code> otherwise
	 */
	boolean preEdgeTraverse(RegionFilter regionFilter);

	/**
	 * This is called after traversing an edge with the given {@link RegionFilter}.
	 * 
	 * @param regionFilter the {@link RegionFilter} of the edge that has just been traversed
	 */
	void postEdgeTraverse(RegionFilter regionFilter);

}