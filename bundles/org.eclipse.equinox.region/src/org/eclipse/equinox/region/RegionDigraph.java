/*******************************************************************************
 * Copyright (c) 2011, 2012 VMware Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SpringSource, a division of VMware - initial API and implementation and/or initial documentation
 *******************************************************************************/

package org.eclipse.equinox.region;

import java.util.Set;
import org.osgi.framework.*;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;

/**
 * {@link RegionDigraph} is a <a href="http://en.wikipedia.org/wiki/Directed_graph">directed graph</a>, or
 * <i>digraph</i>, of {@link Region Regions}. The regions form the nodes of the graph and the edges connect regions to
 * other regions.
 * <p>
 * Each edge (r, s) of the digraph is directed from region r, known as the <i>tail</i> of the edge, to region s, known
 * as the <i>head</i> of the edge.
 * <p>
 * Each edge is associated with a {@link RegionFilter}, making the digraph a <i>labelled</i> digraph. The region filter
 * for edge (r, s) allows region r to see certain bundles, packages, and services visible in region s.
 * <p>
 * Although the digraph may contain cycles it does not contain any <i>loops</i> which are edges of the form (r, r) for
 * some region r. Loopless digraphs are known as <i>simple</i> digraphs. So the digraph is a simple, labelled digraph.
 * <p>
 * The region digraph extends <code>Iterable<Region></code> and so a foreach statement may be used to iterate over (a
 * snapshot of) the regions in the digraph, e.g.
 * 
 * <pre>
 * for (Region r : regionDigraph) {
 *   ...
 * }
 * </pre>
 * <p>
 * <strong>Concurrent Semantics</strong><br />
 * 
 * Implementations of this interface must be thread safe.
 * 
 */
public interface RegionDigraph extends Iterable<Region> {
	/**
	 * A {@link FilteredRegion} represents the head region and the {@link RegionFilter} used 
	 * in a connection with a tail region.
	 */
	public interface FilteredRegion {
		/**
		 * The head {@link Region} for which the filter is being applied.
		 * @return the head region.
		 */
		Region getRegion();

		/**
		 * The {@link RegionFilter} used to determine capabilities which are visible from the
		 * head region.
		 * @return the region filter.
		 */
		RegionFilter getFilter();
	}

	/**
	 * Create a {@link Region} with the given name. If a region with the given name already exists, then BundleException
	 * with exception type UNSUPPORTED_OPERATION is thrown.  If the region name is not valid then an
	 * IllegalArgumentException is thrown.  A valid region name contains none of the following 
	 * characters:
	 * <ul>
	 *   <li> : (colon)</li>
	 *   <li> = (equals)</li>
	 *   <li> \n (newline)</li>
	 *   <li> * (asterisk)</li>
	 *   <li> ? (question mark)</li>
	 *   <li> , (comma)</li>
	 *   <li> &quot; (double quotes)</li>
	 *   <li> \ (backslash)</li>
	 * </ul>
	 * 
	 * @param regionName the name of the region
	 * @return the {@link Region} created
	 * @throws BundleException if the region was not created
	 * @throws IllegalArgumentException if the region name is not valid
	 */
	Region createRegion(String regionName) throws BundleException;

	/**
	 * Create a {@link RegionFilterBuilder} instance.
	 * 
	 * @return a region filter builder
	 */
	RegionFilterBuilder createRegionFilterBuilder();

	/**
	 * Removes the given {@link Region} from the digraph along with any edges which have the given region as head or
	 * tail. If the given region is not present in the digraph, this is not an error and there is no effect.
	 * 
	 * @param region the {@link Region} to be removed
	 */
	void removeRegion(Region region);

	/**
	 * Gets all the {@link Region Regions} in the digraph.
	 * 
	 * @return a set of {@link Region Regions}
	 */
	Set<Region> getRegions();

	/**
	 * Gets the {@link Region} in the digraph with the given name.
	 * 
	 * @param regionName the name of the region
	 * @return the {@link Region} or <code>null</code> if no such region is present in the digraph
	 */
	Region getRegion(String regionName);

	/**
	 * Gets the {@link Region} in the digraph containing the given bundle.
	 * 
	 * @param bundle the bundle to search for
	 * @return the {@link Region} which contains the given bundle or <code>null</code> if there is no such region
	 */
	Region getRegion(Bundle bundle);

	/**
	 * Gets the {@link Region} in the digraph containing a bundle with the given bundle id.
	 * 
	 * @param bundleId the bundleId of the bundle to search for
	 * @return the {@link Region} which contains a bundle with the given bundle or <code>null</code> if there is no such
	 *         region
	 */
	Region getRegion(long bundleId);

	/**
	 * Connects a given tail region to a given head region via an edge labelled with the given {@link RegionFilter}. The
	 * tail region may then, subject to the region filter, see bundles, packages, and services visible in the head
	 * region.
	 * <p>
	 * The given head and tail regions are added to the digraph if they are not already present.
	 * <p>
	 * If the given tail region is already connected to the given head region, then BundleException with exception type
	 * UNSUPPORTED_OPERATION is thrown.
	 * <p>
	 * If the given head and the given tail are identical, then BundleException with exception type
	 * UNSUPPORTED_OPERATION is thrown.
	 * 
	 * @param tailRegion the region at the tail of the new edge
	 * @param filter a {@link RegionFilter} which labels the new edge
	 * @param headRegion the region at the head of the new edge
	 * @throws BundleException if the edge was not created
	 */
	void connect(Region tailRegion, RegionFilter filter, Region headRegion) throws BundleException;

	/**
	 * Gets a {@link Set} containing a snapshot of the {@link FilteredRegion FilteredRegions} attached to the given tail
	 * region.
	 * 
	 * @param tailRegion the tail region whose edges are gotten
	 * @return a {@link Set} of {@link FilteredRegion FilteredRegions} of head regions and region filters
	 */
	Set<FilteredRegion> getEdges(Region tailRegion);

	/**
	 * Visit the subgraph connected to the given region.
	 * 
	 * @param startingRegion the region at which to start
	 * @param visitor a {@link RegionDigraphVisitor} to be called as the subgraph is navigated
	 */
	void visitSubgraph(Region startingRegion, RegionDigraphVisitor visitor);

	/**
	 * Gets a {@link RegionDigraphPersistence} object which can be used to save and load a {@link RegionDigraph} to and
	 * from persistent storage.
	 * 
	 * @return a {@link RegionDigraphPersistence} object.
	 */
	RegionDigraphPersistence getRegionDigraphPersistence();

	/**
	 * Creates a copy of this {@link RegionDigraph}.  Modifying the returned copy has no effect on this
	 * digraph.
	 * @return a copy of this digraph.
	 * @throws BundleException if the digraph could not be copied
	 */
	RegionDigraph copy() throws BundleException;

	/**
	 * Replaces the content of this digraph with the content of the supplied digraph.
	 * The supplied digraph must have been returned by a call to this digraph 
	 * {@link #copy()} method.  If this digraph has been modified between the 
	 * call to {@link #copy()} and {@link #replace(RegionDigraph)} then an
	 * exception is thrown.
	 * @param digraph the digraph to replace this digraph with.
	 * @throws BundleException if the digraph could not be replaced
	 */
	void replace(RegionDigraph digraph) throws BundleException;

	/**
	 * Gets the resolver hook factory associated with this digraph.
	 * @return the resolver hook factory
	 */
	ResolverHookFactory getResolverHookFactory();

	/**
	 * Gets the bundle event hook associated with this digraph.
	 * @return the bundle event hook
	 */
	org.osgi.framework.hooks.bundle.EventHook getBundleEventHook();

	/**
	 * Gets the bundle find hook associated with this digraph.
	 * @return the bundle find hook
	 */
	org.osgi.framework.hooks.bundle.FindHook getBundleFindHook();

	/**
	 * Gets the service event hook associated with this digraph.
	 * @return the service event hook
	 */
	@SuppressWarnings("deprecation")
	org.osgi.framework.hooks.service.EventHook getServiceEventHook();

	/**
	 * Gets the service find hook associated with this digraph.
	 * @return the service find hook
	 */
	org.osgi.framework.hooks.service.FindHook getServiceFindHook();

	/**
	 * Sets a {@link Region} as default one, where all bundles installed via {@link BundleContext} will be included.
	 * If the default {@link Region} isn't set newly installed bundles are assigned to their installer's region.
	 * 
	 * @param defaultRegion the region where all bundles installed via {@link BundleContext} will be assigned to
	 */
	void setDefaultRegion(Region defaultRegion);

	/**
	 * Gets the default {@link Region}, where all bundles installed via {@link BundleContext} are assigned.
	 * If the default {@link Region} isn't set newly installed bundles are assigned to their installer's region.
	 * 
	 * @param defaultRegion the region where all bundles installed via {@link BundleContext} will be assigned to
	 * @return The default region to assign to or <b>null</b> if it isn't set
	 */
	Region getDefaultRegion();

}
