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

import java.util.*;
import org.eclipse.equinox.internal.region.hook.*;
import org.eclipse.equinox.region.*;
import org.osgi.framework.*;
import org.osgi.framework.hooks.bundle.EventHook;
import org.osgi.framework.hooks.bundle.FindHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;

/**
 * {@link StandardRegionDigraph} is the default implementation of {@link RegionDigraph}.
 * <p />
 * 
 * <strong>Concurrent Semantics</strong><br />
 * 
 * Thread safe.
 * 
 */
public final class StandardRegionDigraph implements RegionDigraph {

	private static final Set<FilteredRegion> EMPTY_EDGE_SET = Collections.unmodifiableSet(new HashSet<FilteredRegion>());

	private final Object monitor = new Object();
	// This monitor guards the check for bundle region association across a complete digraph.
	// See BundleIdBaseRegion for more information
	private final Object regionAssociationMonitor = new Object();

	private final Set<Region> regions = new HashSet<Region>();

	/* edges maps a given region to an immutable set of edges with their tail at the given region. To update
	 * the edges for a region, the corresponding immutable set is replaced atomically. */
	private final Map<Region, Set<FilteredRegion>> edges = new HashMap<Region, Set<FilteredRegion>>();

	private final BundleContext bundleContext;

	private final ThreadLocal<Region> threadLocal;

	private final SubgraphTraverser subgraphTraverser;

	private final org.osgi.framework.hooks.bundle.EventHook bundleEventHook;
	private final org.osgi.framework.hooks.bundle.FindHook bundleFindHook;
	@SuppressWarnings("deprecation")
	private final org.osgi.framework.hooks.service.EventHook serviceEventHook;
	private final org.osgi.framework.hooks.service.FindHook serviceFindHook;
	private final ResolverHookFactory resolverHookFactory;

	StandardRegionDigraph() {
		this(null, null);
	}

	public StandardRegionDigraph(BundleContext bundleContext, ThreadLocal<Region> threadLocal) {
		this.subgraphTraverser = new SubgraphTraverser();
		this.bundleContext = bundleContext;
		this.threadLocal = threadLocal;

		// Note we are safely escaping this only because we know the hook impls
		// do not escape the digraph to other threads on construction.
		this.resolverHookFactory = new RegionResolverHookFactory(this);
		this.bundleFindHook = new RegionBundleFindHook(this, bundleContext == null ? 0 : bundleContext.getBundle().getBundleId());
		this.bundleEventHook = new RegionBundleEventHook(this, this.bundleFindHook, this.threadLocal);

		this.serviceFindHook = new RegionServiceFindHook(this);
		this.serviceEventHook = new RegionServiceEventHook(serviceFindHook);
	}

	/**
	 * {@inheritDoc}
	 */
	public Region createRegion(String regionName) throws BundleException {
		Region region = new BundleIdBasedRegion(regionName, this, this.bundleContext, this.threadLocal, this.regionAssociationMonitor);
		synchronized (this.monitor) {
			if (getRegion(regionName) != null) {
				throw new BundleException("Region '" + regionName + "' already exists", BundleException.UNSUPPORTED_OPERATION);
			}
			this.regions.add(region);
			this.edges.put(region, EMPTY_EDGE_SET);
		}
		notifyAdded(region);
		return region;
	}

	/**
	 * {@inheritDoc}
	 */
	public void connect(Region tailRegion, RegionFilter filter, Region headRegion) throws BundleException {
		if (tailRegion == null)
			throw new IllegalArgumentException("The tailRegion must not be null.");
		if (filter == null)
			throw new IllegalArgumentException("The filter must not be null.");
		if (headRegion == null)
			throw new IllegalArgumentException("The headRegion must not be null.");
		if (headRegion.equals(tailRegion)) {
			throw new BundleException("Cannot connect region '" + headRegion + "' to itself", BundleException.UNSUPPORTED_OPERATION);
		}
		boolean tailAdded = false;
		boolean headAdded = false;
		synchronized (this.monitor) {
			Set<FilteredRegion> edges = this.edges.get(tailRegion);
			if (edges == null) {
				edges = new HashSet<FilteredRegion>();
			} else {
				edges = new HashSet<FilteredRegion>(edges);
				for (FilteredRegion edge : edges) {
					if (headRegion.equals(edge.getRegion())) {
						throw new BundleException("Region '" + tailRegion + "' is already connected to region '" + headRegion, BundleException.UNSUPPORTED_OPERATION);
					}
				}
			}

			checkFilterDoesNotAllowExistingBundle(tailRegion, filter);
			tailAdded = this.regions.add(tailRegion);
			headAdded = this.regions.add(headRegion);
			edges.add(new StandardFilteredRegion(headRegion, filter));
			this.edges.put(tailRegion, Collections.unmodifiableSet(edges));
		}
		if (tailAdded) {
			notifyAdded(tailRegion);
		}
		if (headAdded) {
			notifyAdded(headRegion);
		}
	}

	private void checkFilterDoesNotAllowExistingBundle(Region tailRegion, RegionFilter filter) throws BundleException {
		// TODO: enumerate the bundles in the region and check the filter does not allow any of them
	}

	/**
	 * {@inheritDoc}
	 */
	public Iterator<Region> iterator() {
		synchronized (this.monitor) {
			Set<Region> snapshot = new HashSet<Region>(this.regions.size());
			snapshot.addAll(this.regions);
			return snapshot.iterator();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Set<FilteredRegion> getEdges(Region tailRegion) {
		synchronized (this.monitor) {
			// Cope with the case where tailRegion is not in the digraph
			Set<FilteredRegion> edgeSet = this.edges.get(tailRegion);
			return edgeSet == null ? EMPTY_EDGE_SET : edgeSet;
		}
	}

	private static class StandardFilteredRegion implements FilteredRegion {

		private Region region;

		private RegionFilter regionFilter;

		private StandardFilteredRegion(Region region, RegionFilter regionFilter) {
			this.region = region;
			this.regionFilter = regionFilter;
		}

		public Region getRegion() {
			return this.region;
		}

		public RegionFilter getFilter() {
			return this.regionFilter;
		}

	}

	/**
	 * {@inheritDoc}
	 */
	public Region getRegion(String regionName) {
		synchronized (this.monitor) {
			for (Region region : this) {
				if (regionName.equals(region.getName())) {
					return region;
				}
			}
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Region getRegion(Bundle bundle) {
		synchronized (this.monitor) {
			for (Region region : this) {
				if (region.contains(bundle)) {
					return region;
				}
			}
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Region getRegion(long bundleId) {
		synchronized (this.monitor) {
			for (Region region : this) {
				if (region.contains(bundleId)) {
					return region;
				}
			}
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeRegion(Region region) {
		if (region == null)
			throw new IllegalArgumentException("The region cannot be null.");
		notifyRemoving(region);
		synchronized (this.monitor) {
			this.regions.remove(region);
			this.edges.remove(region);
			for (Region r : this.edges.keySet()) {
				Set<FilteredRegion> edgeSet = this.edges.get(r);
				for (FilteredRegion edge : edgeSet) {
					if (region.equals(edge.getRegion())) {
						Set<FilteredRegion> mutableEdgeSet = new HashSet<FilteredRegion>(edgeSet);
						mutableEdgeSet.remove(edge);
						this.edges.put(r, Collections.unmodifiableSet(mutableEdgeSet));
						break;
					}
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public String toString() {
		synchronized (this.monitor) {
			StringBuffer s = new StringBuffer();
			boolean first = true;
			s.append("RegionDigraph{");
			for (Region r : this) {
				if (!first) {
					s.append(", ");
				}
				s.append(r);
				first = false;
			}
			s.append("}");

			s.append("[");
			first = true;
			for (Region r : this) {
				Set<FilteredRegion> edgeSet = this.edges.get(r);
				if (edgeSet != null) {
					for (FilteredRegion filteredRegion : edgeSet) {
						if (!first) {
							s.append(", ");
						}
						s.append(r + "->" + filteredRegion.getRegion());
						first = false;
					}
				}
			}
			s.append("]");
			return s.toString();
		}
	}

	public Set<Region> getRegions() {
		Set<Region> result = new HashSet<Region>();
		synchronized (this.monitor) {
			result.addAll(this.regions);
		}
		return result;
	}

	public RegionFilterBuilder createRegionFilterBuilder() {
		return new StandardRegionFilterBuilder();
	}

	private void notifyAdded(Region region) {
		Set<RegionLifecycleListener> listeners = getListeners();
		for (RegionLifecycleListener listener : listeners) {
			listener.regionAdded(region);
		}
	}

	private void notifyRemoving(Region region) {
		Set<RegionLifecycleListener> listeners = getListeners();
		for (RegionLifecycleListener listener : listeners) {
			listener.regionRemoving(region);
		}
	}

	private Set<RegionLifecycleListener> getListeners() {
		Set<RegionLifecycleListener> listeners = new HashSet<RegionLifecycleListener>();
		if (this.bundleContext == null)
			return listeners;
		try {
			Collection<ServiceReference<RegionLifecycleListener>> listenerServiceReferences = this.bundleContext.getServiceReferences(RegionLifecycleListener.class, null);
			for (ServiceReference<RegionLifecycleListener> listenerServiceReference : listenerServiceReferences) {
				RegionLifecycleListener regionLifecycleListener = this.bundleContext.getService(listenerServiceReference);
				if (regionLifecycleListener != null) {
					listeners.add(regionLifecycleListener);
				}
			}
		} catch (InvalidSyntaxException e) {
			e.printStackTrace();
		}
		return listeners;
	}

	/**
	 * {@inheritDoc}
	 */
	public void visitSubgraph(Region startingRegion, RegionDigraphVisitor visitor) {
		this.subgraphTraverser.visitSubgraph(startingRegion, visitor);
	}

	/**
	 * Returns a snapshot of filtered regions
	 * 
	 * @return a snapshot of filtered regions
	 */
	Map<Region, Set<FilteredRegion>> getFilteredRegions() {
		synchronized (this.monitor) {
			return new HashMap<Region, Set<FilteredRegion>>(this.edges);
		}
	}

	/** 
	 * {@inheritDoc}
	 */
	public RegionDigraphPersistence getRegionDigraphPersistence() {
		return new StandardRegionDigraphPersistence();
	}

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public RegionDigraph copy() throws BundleException {
		StandardRegionDigraph digraphCopy = new StandardRegionDigraph();
		digraphCopy.replace(this);
		return digraphCopy;
	}

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public void replace(RegionDigraph digraph) throws BundleException {
		if (!(digraph instanceof StandardRegionDigraph))
			throw new IllegalArgumentException("Only digraphs of type '" + StandardRegionDigraph.class.getName() + "' are allowed: " + digraph.getClass().getName());
		Map<Region, Set<FilteredRegion>> filteredRegions = ((StandardRegionDigraph) digraph).getFilteredRegions();
		synchronized (this.monitor) {
			this.regions.clear();
			this.edges.clear();
			for (Region original : filteredRegions.keySet()) {
				Region copy = this.createRegion(original.getName());
				for (Long id : original.getBundleIds()) {
					copy.addBundle(id);
				}
			}
			for (Map.Entry<Region, Set<FilteredRegion>> connection : filteredRegions.entrySet()) {
				Region tailRegion = this.getRegion(connection.getKey().getName());
				for (FilteredRegion headFilter : connection.getValue()) {
					Region headRegion = this.getRegion(headFilter.getRegion().getName());
					this.connect(tailRegion, headFilter.getFilter(), headRegion);
				}
			}
		}
	}

	@Override
	public ResolverHookFactory getResolverHookFactory() {
		return resolverHookFactory;
	}

	@Override
	public EventHook getBundleEventHook() {
		return bundleEventHook;
	}

	@Override
	public FindHook getBundleFindHook() {
		return bundleFindHook;
	}

	@SuppressWarnings("deprecation")
	@Override
	public org.osgi.framework.hooks.service.EventHook getServiceEventHook() {
		return serviceEventHook;
	}

	@Override
	public org.osgi.framework.hooks.service.FindHook getServiceFindHook() {
		return serviceFindHook;
	}

}
