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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.eclipse.equinox.region.*;
import org.eclipse.equinox.region.RegionDigraph.FilteredRegion;
import org.osgi.framework.*;

/**
 * {@link BundleIdBasedRegion} is an implementation of {@link Region} which keeps a track of the bundles in the region
 * by recording their bundle identifiers.
 * <p />
 * 
 * <strong>Concurrent Semantics</strong><br />
 * Thread safe.
 */
final class BundleIdBasedRegion implements Region {

	private static final String REGION_LOCATION_DELIMITER = "#"; //$NON-NLS-1$

	private static final String FILE_SCHEME = "file:"; //$NON-NLS-1$

	// Note that this global digraph monitor locks modifications and read operations on the RegionDigraph
	// This includes modifying and reading the bunlde ids included in this region
	// It should be considered a global lock on the complete digraph.
	private final Object globalUpdateMonitor;
	private final AtomicLong globalTimeStamp;
	private final Map<Long, Region> globalBundleToRegion;

	private final String regionName;

	private final RegionDigraph regionDigraph;

	private final BundleContext bundleContext;

	private final ThreadLocal<Region> threadLocal;

	BundleIdBasedRegion(String regionName, RegionDigraph regionDigraph, BundleContext bundleContext, ThreadLocal<Region> threadLocal, Object globalUpdateMonitor, AtomicLong globalTimeStamp, Map<Long, Region> globalBundleToRegion) {
		if (regionName == null)
			throw new IllegalArgumentException("The region name must not be null"); //$NON-NLS-1$
		if (regionDigraph == null)
			throw new IllegalArgumentException("The region digraph must not be null"); //$NON-NLS-1$
		if (globalUpdateMonitor == null)
			throw new IllegalArgumentException("The global update monitor must not be null"); //$NON-NLS-1$
		if (globalBundleToRegion == null)
			throw new IllegalArgumentException("The global bundle to region must not be null"); //$NON-NLS-1$
		this.regionName = regionName;
		this.regionDigraph = regionDigraph;
		this.bundleContext = bundleContext;
		this.threadLocal = threadLocal;
		this.globalUpdateMonitor = globalUpdateMonitor;
		this.globalTimeStamp = globalTimeStamp;
		this.globalBundleToRegion = globalBundleToRegion;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getName() {
		return this.regionName;
	}

	/**
	 * {@inheritDoc}
	 */
	public void addBundle(Bundle bundle) throws BundleException {
		addBundle(bundle.getBundleId());
	}

	/**
	 * {@inheritDoc}
	 */
	// There is a global lock obtained to ensure consistency across the complete digraph
	public void addBundle(long bundleId) throws BundleException {
		synchronized (this.globalUpdateMonitor) {
			Region r = this.globalBundleToRegion.get(bundleId);
			if (r != null && r != this) {
				throw new BundleException("Bundle '" + bundleId + "' is already associated with region '" + r + "'", BundleException.INVALID_OPERATION); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			this.globalBundleToRegion.put(bundleId, this);
			this.globalTimeStamp.incrementAndGet();
		}

	}

	/**
	 * {@inheritDoc}
	 */
	public Bundle installBundle(String location, InputStream input) throws BundleException {
		if (this.bundleContext == null)
			throw new BundleException("This region is not connected to an OSGi Framework.", BundleException.INVALID_OPERATION); //$NON-NLS-1$
		setRegionThreadLocal();
		try {
			input = checkFileProtocol(location, input);
			return this.bundleContext.installBundle(location + REGION_LOCATION_DELIMITER + this.regionName, input);
		} finally {
			removeRegionThreadLocal();
		}
	}

	private InputStream checkFileProtocol(String location, InputStream input) throws BundleException {
		if (input != null || location.startsWith(FILE_SCHEME))
			return input;
		try {
			return new URL(location).openStream();
		} catch (MalformedURLException e) {
			throw new BundleException("The location resulted in an invalid bundle URI: " + location, e); //$NON-NLS-1$
		} catch (IOException e) {
			throw new BundleException("The location referred to an invalid bundle at URI: " + location, e); //$NON-NLS-1$
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Bundle installBundle(String location) throws BundleException {
		return installBundle(location, null);
	}

	private void setRegionThreadLocal() {
		if (this.threadLocal != null)
			this.threadLocal.set(this);
	}

	private void removeRegionThreadLocal() {
		if (this.threadLocal != null)
			this.threadLocal.remove();
	}

	/**
	 * {@inheritDoc}
	 */
	public Bundle getBundle(String symbolicName, Version version) {
		if (bundleContext == null)
			return null; // this region is not connected to an OSGi framework

		Set<Long> bundleIds = getBundleIds();
		for (long bundleId : bundleIds) {
			Bundle bundle = bundleContext.getBundle(bundleId);
			if (bundle != null && symbolicName.equals(bundle.getSymbolicName()) && version.equals(bundle.getVersion())) {
				return bundle;
			}
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public void connectRegion(Region headRegion, RegionFilter filter) throws BundleException {
		this.regionDigraph.connect(this, filter, headRegion);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean contains(long bundleId) {
		synchronized (globalUpdateMonitor) {
			return globalBundleToRegion.get(bundleId) == this;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean contains(Bundle bundle) {
		return contains(bundle.getBundleId());
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.regionName.hashCode();
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof BundleIdBasedRegion)) {
			return false;
		}
		BundleIdBasedRegion other = (BundleIdBasedRegion) obj;
		return this.regionName.equals(other.regionName);
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeBundle(Bundle bundle) {
		removeBundle(bundle.getBundleId());

	}

	/**
	 * {@inheritDoc}
	 */
	public void removeBundle(long bundleId) {
		synchronized (this.globalUpdateMonitor) {
			this.globalBundleToRegion.remove(bundleId);
			this.globalTimeStamp.incrementAndGet();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return getName();
	}

	public Set<Long> getBundleIds() {
		Set<Long> bundleIds = new HashSet<Long>();
		synchronized (this.globalUpdateMonitor) {
			for (Map.Entry<Long, Region> entry : globalBundleToRegion.entrySet()) {
				if (entry.getValue() == this) {
					bundleIds.add(entry.getKey());
				}
			}
		}
		return bundleIds;
	}

	public Set<FilteredRegion> getEdges() {
		return this.regionDigraph.getEdges(this);
	}

	public void visitSubgraph(RegionDigraphVisitor visitor) {
		this.regionDigraph.visitSubgraph(this, visitor);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public RegionDigraph getRegionDigraph() {
		return this.regionDigraph;
	}

}
