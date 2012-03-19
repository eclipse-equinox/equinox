/*******************************************************************************
 * Copyright (c) 2011, 2012 VMware Inc.
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
import java.util.Set;
import java.util.regex.Pattern;
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
	private static final Pattern invalidName = Pattern.compile("[:=\\n*?,\"\\\\]"); //$NON-NLS-1$
	private static final String REGION_LOCATION_DELIMITER = "#"; //$NON-NLS-1$

	private static final String FILE_SCHEME = "file:"; //$NON-NLS-1$

	private final String regionName;

	private final RegionDigraph regionDigraph;

	private final BundleIdToRegionMapping bundleIdToRegionMapping;

	private final BundleContext bundleContext;

	private final ThreadLocal<Region> threadLocal;

	BundleIdBasedRegion(String regionName, RegionDigraph regionDigraph, BundleIdToRegionMapping bundleIdToRegionMapping, BundleContext bundleContext, ThreadLocal<Region> threadLocal) {
		BundleIdBasedRegion.validateName(regionName);
		if (regionDigraph == null)
			throw new IllegalArgumentException("The region digraph must not be null"); //$NON-NLS-1$
		if (bundleIdToRegionMapping == null)
			throw new IllegalArgumentException("The bundle id to region mapping must not be null"); //$NON-NLS-1$
		this.regionName = regionName;
		this.regionDigraph = regionDigraph;
		this.bundleIdToRegionMapping = bundleIdToRegionMapping;
		this.bundleContext = bundleContext;
		this.threadLocal = threadLocal;
	}

	private static void validateName(String name) {
		if (name == null)
			throw new IllegalArgumentException("The region name must not be null"); //$NON-NLS-1$
		if (invalidName.matcher(name).find())
			throw new IllegalArgumentException("The region name has invalid characters: " + name); //$NON-NLS-1$
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return this.regionName;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addBundle(Bundle bundle) throws BundleException {
		addBundle(bundle.getBundleId());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addBundle(long bundleId) throws BundleException {
		this.bundleIdToRegionMapping.associateBundleWithRegion(bundleId, this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Bundle installBundleAtLocation(String location, InputStream input) throws BundleException {
		return installBundle0(location, input, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Bundle installBundle(String location, InputStream input) throws BundleException {
		return installBundle0(location, input, true);
	}

	private Bundle installBundle0(String location, InputStream input, boolean appendRegionName) throws BundleException {
		if (this.bundleContext == null)
			throw new BundleException("This region is not connected to an OSGi Framework.", BundleException.INVALID_OPERATION); //$NON-NLS-1$
		setRegionThreadLocal();
		try {
			input = checkFileProtocol(location, input);
			if (appendRegionName) {
				location = location + REGION_LOCATION_DELIMITER + this.regionName;
			}
			return this.bundleContext.installBundle(location, input);
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
	@Override
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
	@Override
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
	@Override
	public void connectRegion(Region headRegion, RegionFilter filter) throws BundleException {
		this.regionDigraph.connect(this, filter, headRegion);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean contains(long bundleId) {
		return this.bundleIdToRegionMapping.isBundleAssociatedWithRegion(bundleId, this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean contains(Bundle bundle) {
		return contains(bundle.getBundleId());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.regionName.hashCode();
		return result;
	}

	@Override
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
	@Override
	public void removeBundle(Bundle bundle) {
		removeBundle(bundle.getBundleId());

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removeBundle(long bundleId) {
		this.bundleIdToRegionMapping.dissociateBundleFromRegion(bundleId, this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return getName();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<Long> getBundleIds() {
		return this.bundleIdToRegionMapping.getBundleIds(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<FilteredRegion> getEdges() {
		return this.regionDigraph.getEdges(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
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
