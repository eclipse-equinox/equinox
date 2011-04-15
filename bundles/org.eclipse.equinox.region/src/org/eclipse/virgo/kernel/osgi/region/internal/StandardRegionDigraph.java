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

package org.eclipse.virgo.kernel.osgi.region.internal;

import java.util.*;
import org.eclipse.virgo.kernel.osgi.region.*;
import org.eclipse.virgo.util.math.OrderedPair;
import org.osgi.framework.*;

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

    private final Object monitor = new Object();

    private final Set<Region> regions = new HashSet<Region>();

    private final Map<OrderedPair<Region, Region>, RegionFilter> filter = new HashMap<OrderedPair<Region, Region>, RegionFilter>();

    private final BundleContext bundleContext;

    private final ThreadLocal<Region> threadLocal;

    private final SubgraphTraverser subgraphTraverser;

    StandardRegionDigraph() {
        this(null, null);
    }

    public StandardRegionDigraph(BundleContext bundleContext, ThreadLocal<Region> threadLocal) {
        this.subgraphTraverser = new SubgraphTraverser();
        this.bundleContext = bundleContext;
        this.threadLocal = threadLocal;
    }

    /**
     * {@inheritDoc}
     */
    public Region createRegion(String regionName) throws BundleException {
        Region region = new BundleIdBasedRegion(regionName, this, this.bundleContext, this.threadLocal);
        synchronized (this.monitor) {
            if (getRegion(regionName) != null) {
                throw new BundleException("Region '" + regionName + "' already exists", BundleException.UNSUPPORTED_OPERATION);
            }
            this.regions.add(region);
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
        OrderedPair<Region, Region> nodePair = new OrderedPair<Region, Region>(tailRegion, headRegion);
        boolean tailAdded = false;
        boolean headAdded = false;
        synchronized (this.monitor) {
            if (this.filter.containsKey(nodePair)) {
                throw new BundleException("Region '" + tailRegion + "' is already connected to region '" + headRegion,
                    BundleException.UNSUPPORTED_OPERATION);
            } else {
                checkFilterDoesNotAllowExistingBundle(tailRegion, filter);
                tailAdded = this.regions.add(tailRegion);
                headAdded = this.regions.add(headRegion);
                this.filter.put(nodePair, filter);
            }
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
        Set<FilteredRegion> edges = new HashSet<FilteredRegion>();
        synchronized (this.monitor) {
            Set<OrderedPair<Region, Region>> regionPairs = this.filter.keySet();
            for (OrderedPair<Region, Region> regionPair : regionPairs) {
                if (tailRegion.equals(regionPair.getFirst())) {
                    edges.add(new StandardFilteredRegion(regionPair.getSecond(), this.filter.get(regionPair)));
                }
            }
        }
        return edges;
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
            Iterator<OrderedPair<Region, Region>> i = this.filter.keySet().iterator();
            while (i.hasNext()) {
                OrderedPair<Region, Region> regionPair = i.next();
                if (region.equals(regionPair.getFirst()) || region.equals(regionPair.getSecond())) {
                    i.remove();
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
            for (OrderedPair<Region, Region> regionPair : this.filter.keySet()) {
                if (!first) {
                    s.append(", ");
                }
                s.append(regionPair.getFirst() + "->" + regionPair.getSecond());
                first = false;
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
            Collection<ServiceReference<RegionLifecycleListener>> listenerServiceReferences = this.bundleContext.getServiceReferences(
                RegionLifecycleListener.class, null);
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
        Map<Region, Set<FilteredRegion>> result = new HashMap<Region, Set<FilteredRegion>>();
        synchronized (this.monitor) {
            for (Region region : regions) {
                result.put(region, getEdges(region));
            }
        }
        return result;
    }

    public RegionDigraphPersistence getRegionDigraphPersistence() {
        return new StandardRegionDigraphPersistence();
    }

	@Override
	public RegionDigraph copy() throws BundleException {
		StandardRegionDigraph digraphCopy = new StandardRegionDigraph();
		digraphCopy.replace(this);
		return digraphCopy;
	}

	@Override
	public void replace(RegionDigraph digraph) throws BundleException {
        if (!(digraph instanceof StandardRegionDigraph))
            throw new IllegalArgumentException("Only digraphs of type '" + StandardRegionDigraph.class.getName() + "' are allowed: "
                + digraph.getClass().getName());
        Map<Region, Set<FilteredRegion>> filteredRegions = ((StandardRegionDigraph) digraph).getFilteredRegions();
		synchronized (this.monitor) {
			this.regions.clear();
			this.filter.clear();
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

    
}
