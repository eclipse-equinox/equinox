/*******************************************************************************
 * Copyright (c) 2008, 2012 VMware Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   VMware Inc. - initial contribution
 *******************************************************************************/

package org.eclipse.equinox.region;

import java.io.InputStream;
import java.util.Set;
import org.eclipse.equinox.region.RegionDigraph.FilteredRegion;
import org.osgi.framework.*;

/**
 * A <i>region</i> is a subset of the bundles of an OSGi framework. A regions is "weakly" isolated from other regions
 * except that is has full visibility of certain (subject to a {@link RegionFilter}) bundles, packages, and services
 * from other regions to which it is connected. However a bundle running in a region is not protected from discovering
 * bundles in other regions, e.g. by following wires using Wire Admin or similar services, so this is why regions are
 * only weakly isolated from each other.
 * <p />
 * 
 * <strong>Concurrent Semantics</strong><br />
 * 
 * Implementations must be thread safe.
 * 
 */
public interface Region {

	/**
	 * Returns the name of the region.
	 * 
	 * @return the region name
	 */
	String getName();

	/**
	 * Associates a given bundle, which has therefore already been installed, with this region.
	 * <p>
	 * This method is typically used to associate the system bundle with a region. Note that the system bundle is not
	 * treated specially and in order to be fully visible in a region, it must either be associated with the region or
	 * imported from another region via a connection.
	 * <p>
	 * If the bundle is already associated with this region, takes no action and returns normally.
	 * <p>
	 * If the bundle is already associated with another region, throws BundleException with exception type
	 * INVALID_OPERATION.
	 *
	 * @param bundle the bundle to be associated with this region
	 * @throws BundleException if the bundle cannot be associated with the region
	 */
	void addBundle(Bundle bundle) throws BundleException;

	/**
	 * Associates the given bundle id with this region. If the given bundle id is already associated with this region,
	 * this is not an error and there is no effect.
	 * <p>
	 * If the bundle is already associated with another region, throws BundleException with exception type
	 * INVALID_OPERATION.
	 * <p>
	 * This is useful when manipulating offline resolver states and bundle descriptions which do not correspond to
	 * bundles.
	 * 
	 * @param bundleId the bundle id to be associated with this region
	 * @throws BundleException if the bundle cannot be associated with the region
	 */
	void addBundle(long bundleId) throws BundleException;

	/**
	 * Installs a bundle and associates the bundle with this region. The bundle's location will have the region name
	 * appended to the given location to ensure the location is unique across regions.
	 * 
	 * @param location the bundle location string
	 * @param input a stream of the bundle's contents or <code>null</code>
	 * @return the installed Bundle
	 * @throws BundleException if the install fails
	 * @see BundleContext#installBundle(String, InputStream)
	 */
	Bundle installBundle(String location, InputStream input) throws BundleException;

	/**
	 * Installs a bundle and associates the bundle with this region. The bundle's location will have the region name
	 * appended to the given location to ensure the location is unique across regions.
	 * 
	 * @param location the bundle location string
	 * @return the installed Bundle
	 * @throws BundleException if the install fails
	 * @see BundleContext#installBundle(String)
	 */
	Bundle installBundle(String location) throws BundleException;

	/**
	 * Installs a bundle and associates the bundle with this region. The bundle's location 
	 * will be used as is.  The caller of this method is responsible for ensuring the 
	 * location is unique across regions.
	 * 
	 * @param location the bundle location string
	 * @param input a stream of the bundle's contents or <code>null</code>
	 * @return the installed Bundle
	 * @throws BundleException if the install fails
	 * @see BundleContext#installBundle(String, InputStream)
	 */
	Bundle installBundleAtLocation(String location, InputStream input) throws BundleException;

	/**
	 * 
	 * Gets the bundle ids of the bundles associated with this region.
	 * 
	 * @return a set of bundle ids
	 */
	Set<Long> getBundleIds();

	/**
	 * Returns <code>true</code> if and only if the given bundle belongs to this region.
	 * 
	 * @param bundle a {@link Bundle}
	 * @return <code>true</code> if the given bundle belongs to this region and <code>false</code> otherwise
	 */
	boolean contains(Bundle bundle);

	/**
	 * Returns <code>true</code> if and only if a bundle with the given bundle id belongs to this region.
	 * 
	 * @param bundleId a bundle id
	 * @return <code>true</code> if a bundle with the given bundle id belongs to this region and <code>false</code>
	 *         otherwise
	 */
	boolean contains(long bundleId);

	/**
	 * Get the bundle in this region with the given symbolic name and version.
	 * 
	 * @param symbolicName
	 * @param version
	 * @return the bundle or <code>null</code> if there is no such bundle
	 */
	Bundle getBundle(String symbolicName, Version version);

	/**
	 * Connects this region to the given head region and associates the given {@link RegionFilter} with the connection.
	 * This region may then, subject to the region filter, see bundles, packages, and services visible in the head
	 * region.
	 * <p>
	 * If the filter allows the same bundle symbolic name and version as a bundle already present in this region or a
	 * filter connecting this region to a region other than the tail region, then BundleException with exception type
	 * DUPLICATE_BUNDLE_ERROR is thrown.
	 * <p>
	 * If the given source region is already connected to the given tail region, then BundleException with exception
	 * type UNSUPPORTED_OPERATION is thrown.
	 * 
	 * @param headRegion the region to connect this region to
	 * @param filter a {@link RegionFilter} which controls what is visible across the connection
	 * @throws BundleException if the connection was not created
	 */
	void connectRegion(Region headRegion, RegionFilter filter) throws BundleException;

	/**
	 * Removes the given bundle from this region. If the given bundle does not belong to this region, this is not an
	 * error and there is no effect.
	 * 
	 * @param bundle the bundle to be removed
	 */
	void removeBundle(Bundle bundle);

	/**
	 * Removes the given bundle id from this region. If the given bundle id is not associated with this region, this is
	 * not an error and there is no effect.
	 * 
	 * @param bundleId the bundle id to be removed
	 */
	void removeBundle(long bundleId);

	/**
	 * Gets a {@link Set} containing a snapshot of the {@link FilteredRegion FilteredRegions} attached to this tail
	 * region.
	 * 
	 * @return a {@link Set} of {@link FilteredRegion FilteredRegions} of head regions and region filters
	 */
	Set<FilteredRegion> getEdges();

	/**
	 * Visit the subgraph connected to this region.
	 * 
	 * @param visitor a {@link RegionDigraphVisitor} to be called as the subgraph is navigated
	 */
	void visitSubgraph(RegionDigraphVisitor visitor);

	/**
	 * Gets the {@link RegionDigraph} this region belongs to.  This method must never return null,
	 * even when a region is removed from a digraph.
	 * @return the digraph this region belongs to
	 */
	RegionDigraph getRegionDigraph();

}
