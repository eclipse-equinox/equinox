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

package org.eclipse.equinox.region.management;

import org.eclipse.equinox.region.RegionDigraph;

import javax.management.MXBean;

/**
 * {@link ManageableRegionDigraph} is a JMX representation of the {@link RegionDigraph}.
 * <p />
 * 
 * <strong>Concurrent Semantics</strong><br />
 * Thread safe.
 */
@MXBean
public interface ManageableRegionDigraph {

	/**
	 * Gets the {@link ManageableRegion}s in the digraph.
	 * 
	 * @return an array of {@link ManageableRegion}s
	 */
	ManageableRegion[] getRegions();

	/**
	 * Gets the {@link ManageableRegion} with the given name.
	 * 
	 * @param regionName the region name
	 * @return a {@link ManageableRegion} or <code>null</code> if there is no region with the given name
	 */
	ManageableRegion getRegion(String regionName);

}
