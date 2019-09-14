/*******************************************************************************
 * Copyright (c) 2011, 2015 VMware Inc. and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SpringSource, a division of VMware - initial API and implementation and/or initial documentation
 *******************************************************************************/

package org.eclipse.equinox.internal.region;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;
import org.eclipse.equinox.internal.region.hook.*;
import org.eclipse.equinox.region.*;
import org.osgi.framework.*;
import org.osgi.framework.hooks.bundle.*;
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
public final class StandardRegionDigraph implements BundleIdToRegionMapping, RegionDigraph {

	private static final Set<FilteredRegion> EMPTY_EDGE_SET = Collections.unmodifiableSet(new HashSet<FilteredRegion>());

	// This monitor guards the modifications and read operations on the digraph as well as 
	// bundle id modifications of all regions in this digraph
	private final Object monitor = new Object();

	private final Map<String, Region> regions = new HashMap<String, Region>();

	// Alien calls may be made to the following object while this.monitor is locked
	// as this.monitor is higher in the lock hierarchy than this object's own monitor.
	private final BundleIdToRegionMapping bundleIdToRegionMapping;

	/* edges maps a given region to an immutable set of edges with their tail at the given region. To update
	 * the edges for a region, the corresponding immutable set is replaced atomically. */
	private final Map<Region, Set<FilteredRegion>> edges = new HashMap<Region, Set<FilteredRegion>>();

	private final BundleContext bundleContext;

	private final ThreadLocal<Region> threadLocal;

	private final SubgraphTraverser subgraphTraverser;

	private final org.osgi.framework.hooks.bundle.CollisionHook bundleCollisionHook;
	private final org.osgi.framework.hooks.bundle.EventHook bundleEventHook;
	private final org.osgi.framework.hooks.bundle.FindHook bundleFindHook;
	@SuppressWarnings("deprecation")
	private final org.osgi.framework.hooks.service.EventHook serviceEventHook;
	private final org.osgi.framework.hooks.service.FindHook serviceFindHook;
	private final ResolverHookFactory resolverHookFactory;
	private final StandardRegionDigraph origin;
	// Guarded by the origin monitor
	private long originUpdateCount;
	private final AtomicLong updateCount = new AtomicLong();

	private volatile Region defaultRegion;

	public StandardRegionDigraph(StandardRegionDigraph origin) throws BundleException {
		this(null, null, origin);

	}

	public StandardRegionDigraph(BundleContext bundleContext, ThreadLocal<Region> threadLocal) throws BundleException {
		this(bundleContext, threadLocal, null);
	}

	private StandardRegionDigraph(BundleContext bundleContext, ThreadLocal<Region> threadLocal, StandardRegionDigraph origin) throws BundleException {
		this.subgraphTraverser = new SubgraphTraverser();
		this.bundleIdToRegionMapping = new StandardBundleIdToRegionMapping();
		this.bundleContext = bundleContext;
		this.threadLocal = threadLocal;

		// Note we are safely escaping this only because we know the hook impls
		// do not escape the digraph to other threads on construction.
		this.resolverHookFactory = new RegionResolverHookFactory(this);

		this.bundleFindHook = new RegionBundleFindHook(this, bundleContext == null ? 0 : bundleContext.getBundle().getBundleId());
		this.bundleEventHook = new RegionBundleEventHook(this, this.threadLocal, bundleContext == null ? 0 : bundleContext.getBundle().getBundleId());
		this.bundleCollisionHook = new RegionBundleCollisionHook(this, this.threadLocal);

		this.serviceFindHook = new RegionServiceFindHook(this);
		this.serviceEventHook = new RegionServiceEventHook(this);
		this.origin = origin;
		if (origin != null) {
			synchronized (origin.monitor) {
				this.originUpdateCount = origin.updateCount.get();
				this.replace(origin, false);
			}
		} else {
			this.originUpdateCount = -1;
		}
		this.defaultRegion = null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Region createRegion(String regionName) throws BundleException {
		return createRegion(regionName, true);
	}

	private Region createRegion(String regionName, boolean notify) throws BundleException {
		Region region = new BundleIdBasedRegion(regionName, this, this, this.bundleContext, this.threadLocal);
		synchronized (this.monitor) {
			if (getRegion(regionName) != null) {
				throw new BundleException("Region '" + regionName + "' already exists", BundleException.UNSUPPORTED_OPERATION); //$NON-NLS-1$ //$NON-NLS-2$
			}
			this.regions.put(region.getName(), region);
			this.edges.put(region, EMPTY_EDGE_SET);
			incrementUpdateCount();
		}
		if (notify) {
			notifyAdded(region);
		}
		return region;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void connect(Region tailRegion, RegionFilter filter, Region headRegion) throws BundleException {
		createConnection(tailRegion, filter, headRegion, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public RegionFilter replaceConnection(Region tailRegion, RegionFilter filter, Region headRegion) throws BundleException {
		return createConnection(tailRegion, filter, headRegion, true);
	}

	private RegionFilter createConnection(Region tailRegion, RegionFilter filter, Region headRegion, boolean replace) throws BundleException {
		if (tailRegion == null)
			throw new IllegalArgumentException("The tailRegion must not be null."); //$NON-NLS-1$
		if (!replace && filter == null)
			throw new IllegalArgumentException("The filter must not be null."); //$NON-NLS-1$
		if (headRegion == null)
			throw new IllegalArgumentException("The headRegion must not be null."); //$NON-NLS-1$
		if (headRegion.equals(tailRegion)) {
			throw new BundleException("Cannot connect region '" + headRegion + "' to itself", BundleException.UNSUPPORTED_OPERATION); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (tailRegion.getRegionDigraph() != this)
			throw new IllegalArgumentException("The tailRegion does not belong to this digraph."); //$NON-NLS-1$
		if (headRegion.getRegionDigraph() != this)
			throw new IllegalArgumentException("The headRegion does not belong to this digraph."); //$NON-NLS-1$

		FilteredRegion existing = null;
		boolean tailAdded = false;
		boolean headAdded = false;
		synchronized (this.monitor) {
			Set<FilteredRegion> connections = this.edges.get(tailRegion);
			if (connections == null) {
				connections = new HashSet<FilteredRegion>();
			} else {
				connections = new HashSet<FilteredRegion>(connections);
				for (FilteredRegion edge : connections) {
					if (headRegion.equals(edge.getRegion())) {
						if (replace) {
							existing = edge;
						} else {
							throw new BundleException("Region '" + tailRegion + "' is already connected to region '" + headRegion, BundleException.UNSUPPORTED_OPERATION); //$NON-NLS-1$ //$NON-NLS-2$
						}
					}
				}
			}

			checkFilterDoesNotAllowExistingBundle(tailRegion, filter);

			tailAdded = this.regions.put(tailRegion.getName(), tailRegion) == null;
			headAdded = this.regions.put(headRegion.getName(), headRegion) == null;

			if (existing != null) {
				connections.remove(existing);
			}

			if (filter != null) {
				connections.add(new StandardFilteredRegion(headRegion, filter));
			}

			this.edges.put(tailRegion, Collections.unmodifiableSet(connections));
			incrementUpdateCount();
		}
		if (tailAdded) {
			notifyAdded(tailRegion);
		}
		if (headAdded) {
			notifyAdded(headRegion);
		}
		return existing == null ? null : existing.getFilter();
	}

	private void checkFilterDoesNotAllowExistingBundle(Region tailRegion, RegionFilter filter) {
		// TODO: enumerate the bundles in the region and check the filter does not allow any of them
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Iterator<Region> iterator() {
		synchronized (this.monitor) {
			Set<Region> snapshot = new HashSet<Region>(this.regions.size());
			snapshot.addAll(this.regions.values());
			return snapshot.iterator();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<FilteredRegion> getEdges(Region tailRegion) {
		synchronized (this.monitor) {
			// Cope with the case where tailRegion is not in the digraph
			Set<FilteredRegion> edgeSet = this.edges.get(tailRegion);
			return edgeSet == null ? EMPTY_EDGE_SET : edgeSet;
		}
	}

	static class StandardFilteredRegion implements FilteredRegion {

		private Region region;

		private RegionFilter regionFilter;

		StandardFilteredRegion(Region region, RegionFilter regionFilter) {
			this.region = region;
			this.regionFilter = regionFilter;
		}

		@Override
		public Region getRegion() {
			return this.region;
		}

		@Override
		public RegionFilter getFilter() {
			return this.regionFilter;
		}

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Region getRegion(String regionName) {
		synchronized (this.monitor) {
			return regions.get(regionName);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Region getRegion(Bundle bundle) {
		return getRegion(bundle.getBundleId());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Region getRegion(long bundleId) {
		synchronized (this.monitor) {
			return this.bundleIdToRegionMapping.getRegion(bundleId);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removeRegion(Region region) {
		if (region == null)
			throw new IllegalArgumentException("The region cannot be null."); //$NON-NLS-1$
		notifyRemoving(region);
		synchronized (this.monitor) {
			if (this.defaultRegion != null && this.defaultRegion.equals(region)) {
				this.defaultRegion = null;
			}
			this.regions.remove(region.getName());
			this.edges.remove(region);
			for (Entry<Region, Set<FilteredRegion>> entry : edges.entrySet()) {
				Region r = entry.getKey();
				Set<FilteredRegion> edgeSet = entry.getValue();
				for (FilteredRegion edge : edgeSet) {
					if (region.equals(edge.getRegion())) {
						Set<FilteredRegion> mutableEdgeSet = new HashSet<FilteredRegion>(edgeSet);
						mutableEdgeSet.remove(edge);
						this.edges.put(r, Collections.unmodifiableSet(mutableEdgeSet));
						break;
					}
				}
			}
			this.bundleIdToRegionMapping.dissociateRegion(region);
			incrementUpdateCount();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		synchronized (this.monitor) {
			StringBuilder s = new StringBuilder();
			boolean first = true;
			s.append("RegionDigraph{"); //$NON-NLS-1$
			for (Region r : this) {
				if (!first) {
					s.append(", "); //$NON-NLS-1$
				}
				s.append(r);
				first = false;
			}
			s.append("}"); //$NON-NLS-1$

			s.append("["); //$NON-NLS-1$
			first = true;
			for (Region r : this) {
				Set<FilteredRegion> edgeSet = this.edges.get(r);
				if (edgeSet != null) {
					for (FilteredRegion filteredRegion : edgeSet) {
						if (!first) {
							s.append(", "); //$NON-NLS-1$
						}
						s.append(r + "->" + filteredRegion.getRegion()); //$NON-NLS-1$
						first = false;
					}
				}
			}
			s.append("]"); //$NON-NLS-1$
			return s.toString();
		}
	}

	@Override
	public Set<Region> getRegions() {
		Set<Region> result = new HashSet<Region>();
		synchronized (this.monitor) {
			result.addAll(this.regions.values());
		}
		return result;
	}

	@Override
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
		if (this.bundleContext == null)
			return Collections.emptySet();
		Set<RegionLifecycleListener> listeners = new HashSet<RegionLifecycleListener>();
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
	@Override
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
	@Override
	public RegionDigraphPersistence getRegionDigraphPersistence() {
		return new StandardRegionDigraphPersistence();
	}

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public RegionDigraph copy() throws BundleException {
		return new StandardRegionDigraph(this);
	}

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public void replace(RegionDigraph digraph) throws BundleException {
		replace(digraph, true);
	}

	private void replace(RegionDigraph digraph, boolean check) throws BundleException {
		if (!(digraph instanceof StandardRegionDigraph))
			throw new IllegalArgumentException("Only digraphs of type '" + StandardRegionDigraph.class.getName() + "' are allowed: " + digraph.getClass().getName()); //$NON-NLS-1$ //$NON-NLS-2$
		StandardRegionDigraph replacement = (StandardRegionDigraph) digraph;
		if (check && replacement.origin != this)
			throw new IllegalArgumentException("The replacement digraph is not a copy of this digraph."); //$NON-NLS-1$

		// notify removing first, and outside the monitor lock
		final Set<Region> removed = getRegions();
		removed.removeAll(replacement.getRegions());
		for (Region region : removed) {
			notifyRemoving(region);
		}

		Map<Region, Set<FilteredRegion>> filteredRegions = replacement.getFilteredRegions();
		final Set<Region> added = new HashSet<Region>();
		synchronized (this.monitor) {
			if (check && this.updateCount.get() != replacement.originUpdateCount) {
				throw new BundleException("The origin update count has changed since the replacement copy was created.", BundleException.INVALID_OPERATION); //$NON-NLS-1$
			}
			Map<String, Region> nameToRegion = new HashMap<String, Region>(regions);
			this.regions.clear();
			this.edges.clear();
			this.bundleIdToRegionMapping.clear();
			for (Region original : filteredRegions.keySet()) {
				Region copy = nameToRegion.get(original.getName());
				if (copy != null) {
					// reuse the previous region object
					regions.put(copy.getName(), copy);
					edges.put(copy, EMPTY_EDGE_SET);
				} else {
					// create a new one
					copy = this.createRegion(original.getName(), false);
					// collect added for notifying later ouside the lock
					added.add(copy);
				}
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
			incrementUpdateCount();
			if (check) {
				replacement.originUpdateCount = this.updateCount.get();
			}
		}
		// Now notify of additions outside the lock
		for (Region region : added) {
			notifyAdded(region);
		}
	}

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public ResolverHookFactory getResolverHookFactory() {
		return resolverHookFactory;
	}

	CollisionHook getBundleCollisionHook() {
		return bundleCollisionHook;
	}

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public EventHook getBundleEventHook() {
		return bundleEventHook;
	}

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public FindHook getBundleFindHook() {
		return bundleFindHook;
	}

	/** 
	 * {@inheritDoc}
	 */
	@SuppressWarnings("deprecation")
	@Override
	public org.osgi.framework.hooks.service.EventHook getServiceEventHook() {
		return serviceEventHook;
	}

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public org.osgi.framework.hooks.service.FindHook getServiceFindHook() {
		return serviceFindHook;
	}

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public void setDefaultRegion(Region defaultRegion) {
		synchronized (this.monitor) {
			if (defaultRegion != null) {
				checkRegionExists(defaultRegion);
			}
			this.defaultRegion = defaultRegion;
		}
	}

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public Region getDefaultRegion() {
		return this.defaultRegion;
	}

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public void associateBundleWithRegion(long bundleId, Region region) throws BundleException {
		synchronized (this.monitor) {
			checkRegionExists(region);
			this.bundleIdToRegionMapping.associateBundleWithRegion(bundleId, region);
			incrementUpdateCount();
		}
	}

	private void checkRegionExists(Region region) {
		if (this.regions.get(region.getName()) == null) {
			throw new IllegalStateException("Operation not allowed on region " + region.getName() + " which is not part of a digraph"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private void incrementUpdateCount() {
		synchronized (this.monitor) {
			this.updateCount.incrementAndGet();
		}

	}

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public void dissociateBundleFromRegion(long bundleId, Region region) {
		synchronized (this.monitor) {
			checkRegionExists(region);
			this.bundleIdToRegionMapping.dissociateBundleFromRegion(bundleId, region);
			incrementUpdateCount();
		}
	}

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public boolean isBundleAssociatedWithRegion(long bundleId, Region region) {
		synchronized (this.monitor) {
			return this.bundleIdToRegionMapping.isBundleAssociatedWithRegion(bundleId, region);
		}
	}

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public Set<Long> getBundleIds(Region region) {
		synchronized (this.monitor) {
			return this.bundleIdToRegionMapping.getBundleIds(region);
		}
	}

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public void clear() {
		synchronized (this.monitor) {
			this.bundleIdToRegionMapping.clear();
			incrementUpdateCount();
		}
	}

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public void dissociateRegion(Region region) {
		synchronized (this.monitor) {
			this.bundleIdToRegionMapping.dissociateRegion(region);
			incrementUpdateCount();
		}
	}

}
