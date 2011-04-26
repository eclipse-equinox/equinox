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

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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

	private static final String REGION_LOCATION_DELIMITER = "@";

	private static final String REFERENCE_SCHEME = "reference:";

	private static final String FILE_SCHEME = "file:";

	// A concurrent data structure ensures the contains method does not need synchronisation.
	private final ConcurrentMap<Long, Boolean> bundleIds = new ConcurrentHashMap<Long, Boolean>();

	// This monitor guards bundleIds. bundleIds is a concurrent data structure and
	// may be read without synchronisation, but updates need synchronising to avoid races.
	// Note that this monitor also locks modifications and read operations on the RegionDigraph
	// As well as bundle id modifications of other Regions in the digraph
	// It should be considered a global lock on the complete digraph.
	private final Object globalUpdateMonitor;
	private final AtomicLong globalTimeStamp;

	private final String regionName;

	private final RegionDigraph regionDigraph;

	private final BundleContext bundleContext;

	private final ThreadLocal<Region> threadLocal;

	BundleIdBasedRegion(String regionName, RegionDigraph regionDigraph, BundleContext bundleContext, ThreadLocal<Region> threadLocal, Object globalUpdateMonitor, AtomicLong globalTimeStamp) {
		if (regionName == null)
			throw new IllegalArgumentException("The region name must not be null");
		if (regionDigraph == null)
			throw new IllegalArgumentException("The region digraph must not be null");
		if (globalUpdateMonitor == null)
			throw new IllegalArgumentException("The global update monitor must not be null");
		this.regionName = regionName;
		this.regionDigraph = regionDigraph;
		this.bundleContext = bundleContext;
		this.threadLocal = threadLocal;
		this.globalUpdateMonitor = globalUpdateMonitor;
		this.globalTimeStamp = globalTimeStamp;
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
			// note that obtaining the Iterator for the digraph requires locking the digraph monitor; but we already have it
			for (Region r : this.regionDigraph) {
				if (!this.equals(r) && r.contains(bundleId)) {
					throw new BundleException("Bundle '" + bundleId + "' is already associated with region '" + r + "'", BundleException.INVALID_OPERATION);
				}
			}
			this.bundleIds.putIfAbsent(bundleId, Boolean.TRUE);
			this.globalTimeStamp.incrementAndGet();
		}

	}

	/**
	 * {@inheritDoc}
	 */
	public Bundle installBundle(String location, InputStream input) throws BundleException {
		if (this.bundleContext == null)
			throw new BundleException("This region is not connected to an OSGi Framework.", BundleException.INVALID_OPERATION);
		setRegionThreadLocal();
		try {
			return this.bundleContext.installBundle(this.regionName + REGION_LOCATION_DELIMITER + location, input);
		} finally {
			removeRegionThreadLocal();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Bundle installBundle(String location) throws BundleException {
		if (this.bundleContext == null)
			throw new BundleException("This region is not connected to an OSGi Framework.", BundleException.INVALID_OPERATION);
		setRegionThreadLocal();
		try {
			return this.bundleContext.installBundle(this.regionName + REGION_LOCATION_DELIMITER + location, openBundleStream(location));
		} finally {
			removeRegionThreadLocal();
		}
	}

	private void setRegionThreadLocal() {
		if (this.threadLocal != null)
			this.threadLocal.set(this);
	}

	private void removeRegionThreadLocal() {
		if (this.threadLocal != null)
			this.threadLocal.remove();
	}

	private InputStream openBundleStream(String location) throws BundleException {
		String absoluteBundleReferenceUriString;
		if (location.startsWith(REFERENCE_SCHEME))
			absoluteBundleReferenceUriString = location;
		else
			absoluteBundleReferenceUriString = REFERENCE_SCHEME + getAbsoluteUriString(location);

		try {
			// Use the reference: scheme to obtain an InputStream for either a file or a directory.
			return new URL(absoluteBundleReferenceUriString).openStream();

		} catch (MalformedURLException e) {
			throw new BundleException("Location '" + location + "' resulted in an invalid bundle URI '" + absoluteBundleReferenceUriString + "'", e);
		} catch (IOException e) {
			throw new BundleException("Location '" + location + "' referred to an invalid bundle at URI '" + absoluteBundleReferenceUriString + "'", e);
		}
	}

	private String getAbsoluteUriString(String location) throws BundleException {
		if (!location.startsWith(FILE_SCHEME)) {
			throw new BundleException("Cannot install from location '" + location + "' which did not start with '" + FILE_SCHEME + "'");
		}

		String filePath = location.substring(FILE_SCHEME.length());

		return FILE_SCHEME + new File(filePath).getAbsolutePath();
	}

	/**
	 * {@inheritDoc}
	 */
	public Bundle getBundle(String symbolicName, Version version) {
		if (bundleContext == null)
			return null; // this region is not connected to an OSGi framework

		// The following iteration is weakly consistent and will never throw ConcurrentModificationException.
		for (long bundleId : this.bundleIds.keySet()) {
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

	// The contains methods must not acquire the updateMonitor in order to prevent
	// deadlock since a thread holding the updateMonitor of one region can call
	// checkBundleNotAssociatedWithAnotherRegion which can call the contains
	// method on another region.
	/**
	 * {@inheritDoc}
	 */
	public boolean contains(long bundleId) {
		return this.bundleIds.containsKey(bundleId);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean contains(Bundle bundle) {
		return this.bundleIds.containsKey(bundle.getBundleId());
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
			this.bundleIds.remove(bundleId);
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
			bundleIds.addAll(this.bundleIds.keySet());
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
