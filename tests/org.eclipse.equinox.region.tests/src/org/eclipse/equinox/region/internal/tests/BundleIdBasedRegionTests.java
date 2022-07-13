/*******************************************************************************
 * Copyright (c) 2011, 2021 VMware Inc. and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   VMware Inc. - initial contribution
 *******************************************************************************/

package org.eclipse.equinox.region.internal.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Iterator;
import org.eclipse.equinox.region.*;
import org.eclipse.equinox.region.RegionDigraph.FilteredRegion;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.*;

public class BundleIdBasedRegionTests {

	private static final String OTHER_REGION_NAME = "other";

	private static final String BUNDLE_SYMBOLIC_NAME = "b";

	private static final String BUNDLE_SYMBOLIC_NAME_2 = "c";

	private static final Version BUNDLE_VERSION = new Version("1");

	private static final long BUNDLE_ID = 1L;

	private static final long BUNDLE_ID_2 = 2L;

	private static final String REGION_NAME = "reg";

	private static final long TEST_BUNDLE_ID = 99L;

	private Bundle mockBundle;

	private RegionDigraph mockGraph;

	private Iterator<Region> regionIterator;

	private BundleContext mockBundleContext;

	Region mockRegion;

	Region mockRegion2;

	RegionFilter mockRegionFilter;

	private ThreadLocal<Region> threadLocal;

	private Object bundleIdToRegionMapping;

	@Before
	public void setUp() throws Exception {
		this.threadLocal = new ThreadLocal<>();
		this.mockBundle = mock(Bundle.class);
		when(this.mockBundle.getSymbolicName()).thenReturn(BUNDLE_SYMBOLIC_NAME);
		when(this.mockBundle.getVersion()).thenReturn(BUNDLE_VERSION);
		when(this.mockBundle.getBundleId()).thenReturn(BUNDLE_ID);

		this.mockBundleContext = mock(BundleContext.class);
		when(this.mockBundleContext.getBundle(BUNDLE_ID)).thenReturn(this.mockBundle);

		this.mockRegion = mock(Region.class);
		this.mockRegion2 = mock(Region.class);

		this.mockRegionFilter = mock(RegionFilter.class);

		this.regionIterator = new Iterator<Region>() {

			@Override
			public boolean hasNext() {
				return false;
			}

			@Override
			public Region next() {
				return null;
			}

			@Override
			public void remove() {
				// nothing
			}
		};
		this.mockGraph = mock(RegionDigraph.class);
		this.mockGraph.connect(isA(Region.class), eq(this.mockRegionFilter), eq(this.mockRegion));
		this.bundleIdToRegionMapping = RegionReflectionUtils.newStandardBundleIdToRegionMapping();
	}

	@Test
	public void testGetName() {
		defaultSetUp();

		Region r = createDefaultBundleIdBasedRegion();
		assertEquals(REGION_NAME, r.getName());
	}

	private Region createDefaultBundleIdBasedRegion() {
		return createBundleIdBasedRegion(REGION_NAME);
	}

	private Region createBundleIdBasedRegion(String regionName) {
		return RegionReflectionUtils.newBundleIdBasedRegion(regionName, this.mockGraph, this.bundleIdToRegionMapping,
				this.mockBundleContext, this.threadLocal);
	}

	private void defaultSetUp() {
		when(this.mockGraph.iterator()).thenReturn(this.regionIterator);
		when(this.mockGraph.getEdges(isA(Region.class))).thenReturn(new HashSet<>());
	}

	@Test
	public void testAddBundle() throws BundleException {
		when(this.mockGraph.iterator()).thenReturn(this.regionIterator);

		HashSet<FilteredRegion> edges = new HashSet<>();
		edges.add(new FilteredRegion() {

			@Override
			public Region getRegion() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public RegionFilter getFilter() {
				return mockRegionFilter;
			}
		});
		when(this.mockGraph.getEdges(isA(Region.class))).thenReturn(edges);

		Region r = createDefaultBundleIdBasedRegion();
		r.addBundle(this.mockBundle);
	}

	@Test
	public void testAddExistingBundle() throws BundleException {
		defaultSetUp();

		Region r = createDefaultBundleIdBasedRegion();
		r.addBundle(this.mockBundle);
		r.addBundle(this.mockBundle);
	}

	// This restriction was removed, so no exception should be thrown.
	public void testAddConflictingBundle() throws BundleException {
		defaultSetUp();

		Bundle mockBundle2 = mock(Bundle.class);
		when(mockBundle2.getSymbolicName()).thenReturn(BUNDLE_SYMBOLIC_NAME);
		when(mockBundle2.getVersion()).thenReturn(BUNDLE_VERSION);
		when(mockBundle2.getBundleId()).thenReturn(BUNDLE_ID_2);

		Region r = createDefaultBundleIdBasedRegion();
		r.addBundle(this.mockBundle);
		r.addBundle(mockBundle2);
	}

	@Test(expected = BundleException.class)
	public void testAddBundlePresentInAnotherRegion1() throws BundleException {
		Region r = regionForBundlePersentInAnotherRegionTest();
		r.addBundle(this.mockBundle);
	}

	@Test(expected = BundleException.class)
	public void testAddBundlePresentInAnotherRegion2() throws BundleException {
		Region r = regionForBundlePersentInAnotherRegionTest();
		r.addBundle(this.mockBundle.getBundleId());
	}

	private Region regionForBundlePersentInAnotherRegionTest() {
		this.regionIterator = new Iterator<Region>() {

			private int next = 2;

			@Override
			public boolean hasNext() {
				return this.next > 0;
			}

			@Override
			public Region next() {
				switch (next--) {
				case 2:
					return mockRegion;
				default:
					return mockRegion2;
				}
			}

			@Override
			public void remove() {
				// nothing
			}
		};
		when(this.mockGraph.iterator()).thenReturn(this.regionIterator);
		when(this.mockGraph.getEdges(isA(Region.class))).thenReturn(new HashSet<>());
		when(this.mockRegion.contains(eq(BUNDLE_ID))).thenReturn(true);
		when(this.mockRegion2.contains(eq(BUNDLE_ID))).thenReturn(false);
		RegionReflectionUtils.associateBundleWithRegion(this.bundleIdToRegionMapping, BUNDLE_ID, mockRegion);

		Region r = createDefaultBundleIdBasedRegion();
		return r;
	}

	@Test
	public void testContains() throws BundleException {
		defaultSetUp();

		Region r = createDefaultBundleIdBasedRegion();
		r.addBundle(this.mockBundle);
		assertTrue(r.contains(this.mockBundle));
	}

	@Test
	public void testDoesNotContain() {
		defaultSetUp();

		Region r = createDefaultBundleIdBasedRegion();
		assertFalse(r.contains(this.mockBundle));
	}

	@Test
	public void testGetBundle() throws BundleException {
		defaultSetUp();

		Region r = createDefaultBundleIdBasedRegion();
		r.addBundle(this.mockBundle);
		assertEquals(this.mockBundle, r.getBundle(BUNDLE_SYMBOLIC_NAME, BUNDLE_VERSION));
	}

	@Test
	public void testGetBundleNotFound() throws BundleException {
		defaultSetUp();

		Region r = createDefaultBundleIdBasedRegion();
		r.addBundle(this.mockBundle);
		assertNull(r.getBundle(BUNDLE_SYMBOLIC_NAME_2, BUNDLE_VERSION));
	}

	@Test
	public void testConnectRegion() throws BundleException {
		defaultSetUp();

		Region r = createDefaultBundleIdBasedRegion();
		r.connectRegion(this.mockRegion, this.mockRegionFilter);
	}

	@Test
	public void testEquals() {
		defaultSetUp();

		Region r = createDefaultBundleIdBasedRegion();
		Region s = createDefaultBundleIdBasedRegion();
		assertEquals(r, r);
		assertEquals(r, s);
		assertEquals(r.hashCode(), s.hashCode());
	}

	@Test
	public void testNotEqual() {
		defaultSetUp();

		Region r = createDefaultBundleIdBasedRegion();
		Region s = createBundleIdBasedRegion(OTHER_REGION_NAME);
		assertFalse(r.equals(s));
		assertNotNull(r);
	}

	@Test
	public void testAddRemoveBundleId() throws BundleException {
		defaultSetUp();
		Region r = createDefaultBundleIdBasedRegion();
		r.addBundle(TEST_BUNDLE_ID);
		assertTrue(r.contains(TEST_BUNDLE_ID));
		r.removeBundle(TEST_BUNDLE_ID);
		assertFalse(r.contains(TEST_BUNDLE_ID));

	}

}
