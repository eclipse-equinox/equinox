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

package org.eclipse.equinox.internal.region;

import java.util.Set;
import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraph;
import org.osgi.framework.BundleException;

/**
 * This internal interface is used to track which bundles belong to which regions.
 * 
 * <strong>Concurrent Semantics</strong><br />
 * Implementations must be thread safe. 
 */
public interface BundleIdToRegionMapping {

	/**
	 * Associates the given bundle id with the given region. If the bundle
	 * id is already associated with a different region, throws {@link BundleException}.
	 * If the bundle id is already associated with the given region, there is no
	 * effect on the association and no exception is thrown.
	 * <p>
	 * If the given region does not belong to a {@link RegionDigraph} an
	 * {@link IllegalStateException} is thrown.
	 * 
	 * @param bundleId the bundle id to be associated
	 * @param region the {@link Region} with which the bundle id is to be associated
	 * @throws BundleException if the bundle id is already associated with a different region
	 */
	void associateBundleWithRegion(long bundleId, Region region) throws BundleException;

	/**
	 * Dissociates the given bundle id from the given region. If the given region
	 * does not belong to a {@link RegionDigraph} an {@link IllegalStateException} is thrown.
	 * 
	 * @param bundleId the bundle id to be dissociated
	 * @param region the {@link Region} from which the bundle id is to be dissociated
	 */
	void dissociateBundleFromRegion(long bundleId, Region region);

	/**
	 * Dissociates any bundle ids which may be associated with the given region.
	 * @param region the {@link Region} to be dissociated
	 */
	void dissociateRegion(Region region);

	/**
	 * Returns the {@link Region} associated with the given bundle id or <code>null</code>
	 * if the given bundle id is not associated with a region associated with a {@link RegionDigraph}.
	 * 
	 * @param bundleId the bundle id whose region is required
	 * @return the {@link Region} associated with the given bundle id or or <code>null</code>
	 * if the given bundle id is not associated with a region
	 */
	Region getRegion(long bundleId);

	/**
	 * Checks the association of the given bundle id with the given region and returns
	 * <code>true</code> if and only if the given bundle id is associated with
	 * the given region
	 * 
	 * @param bundleId the bundle id to be checked
	 * @param region the {@link Region} to be checked
	 * @return <code>true</code> if and only if the given bundle id is associated with
	 * the given region
	 */
	boolean isBundleAssociatedWithRegion(long bundleId, Region region);

	/**
	 * Returns a set of bundle ids associated with the given region. Never
	 * returns <code>null</code>.
	 * 
	 * @param region the {@link Region} whose bundle ids are required
	 * @return the {@link Set} of bundle ids associated with the given region
	 */
	Set<Long> getBundleIds(Region region);

	/**
	 * Dissociates all bundle ids and regions.
	 */
	void clear();

}
