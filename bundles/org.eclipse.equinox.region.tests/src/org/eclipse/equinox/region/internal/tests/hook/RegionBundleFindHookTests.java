/*******************************************************************************
 * Copyright (c) 2011, 2013 VMware Inc.
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

package org.eclipse.equinox.region.internal.tests.hook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.*;
import org.eclipse.equinox.region.*;
import org.eclipse.equinox.region.internal.tests.RegionReflectionUtils;
import org.eclipse.virgo.teststubs.osgi.framework.StubBundle;
import org.eclipse.virgo.teststubs.osgi.framework.StubBundleContext;
import org.junit.*;
import org.osgi.framework.*;
import org.osgi.framework.hooks.bundle.FindHook;

public class RegionBundleFindHookTests {

	private static final String BUNDLE_X = "X";

	private static final Version BUNDLE_VERSION = new Version("0");

	private long bundleId;

	private static final String REGION_A = "RegionA";

	private static final String BUNDLE_A = "BundleA";

	private static final String REGION_B = "RegionB";

	private static final String BUNDLE_B = "BundleB";

	private static final String REGION_C = "RegionC";

	private static final String BUNDLE_C = "BundleC";

	private static final String REGION_D = "RegionD";

	private static final String BUNDLE_D = "BundleD";

	private RegionDigraph digraph;

	private FindHook bundleFindHook;

	private Map<String, Region> regions;

	private Map<String, Bundle> bundles;

	private Collection<Bundle> candidates;

	private ThreadLocal<Region> threadLocal;

	@Before
	public void setUp() throws Exception {
		this.bundleId = 1L;
		this.regions = new HashMap<>();
		this.bundles = new HashMap<>();

		StubBundle stubSystemBundle = new StubBundle(0L, "osgi.framework", new Version("0"), "loc");
		StubBundleContext stubBundleContext = new StubBundleContext();
		stubBundleContext.addInstalledBundle(stubSystemBundle);
		this.threadLocal = new ThreadLocal<>();
		this.digraph = RegionReflectionUtils.newStandardRegionDigraph(stubBundleContext, this.threadLocal);
		this.bundleFindHook = RegionReflectionUtils.newRegionBundleFindHook(this.digraph, stubSystemBundle.getBundleId());
		this.candidates = new HashSet<>();

		// Create regions A, B, C, D containing bundles A, B, C, D, respectively.
		createRegion(REGION_A, BUNDLE_A);
		createRegion(REGION_B, BUNDLE_B);
		createRegion(REGION_C, BUNDLE_C);
		createRegion(REGION_D, BUNDLE_D);

		createBundle(BUNDLE_X);

	}

	@After
	public void tearDown() throws Exception {
		// nothing
	}

	@Test
	public void testFindInSameRegion() {
		this.candidates.add(bundle(BUNDLE_A));
		this.bundleFindHook.find(bundleContext(BUNDLE_A), this.candidates);
		assertTrue(this.candidates.contains(bundle(BUNDLE_A)));
	}

	@Test
	public void testFindInDisconnectedRegion() {
		this.candidates.add(bundle(BUNDLE_B));
		this.bundleFindHook.find(bundleContext(BUNDLE_A), this.candidates);
		assertFalse(this.candidates.contains(bundle(BUNDLE_B)));
	}

	@Test
	public void testFindConnectedRegionAllowed() throws BundleException, InvalidSyntaxException {
		doTestFindConnectedRegionAllowed(false);
	}

	@Test
	public void testFindConnectedRegionAllowedWithNegate() throws BundleException, InvalidSyntaxException {
		doTestFindConnectedRegionAllowed(true);
	}

	private void doTestFindConnectedRegionAllowed(boolean negate) throws BundleException, InvalidSyntaxException {
		RegionFilter filter = createFilter(negate, BUNDLE_B);
		region(REGION_A).connectRegion(region(REGION_B), filter);

		this.candidates.add(bundle(BUNDLE_B));
		this.bundleFindHook.find(bundleContext(BUNDLE_A), this.candidates);
		assertTrue(this.candidates.contains(bundle(BUNDLE_B)));
	}

	@Test
	public void testFindConnectedRegionFiltering() throws BundleException, InvalidSyntaxException {
		doTestFindConnectedRegionFiltering(false);
	}

	@Test
	public void testFindConnectedRegionFilteringWithNegate() throws BundleException, InvalidSyntaxException {
		doTestFindConnectedRegionFiltering(true);
	}

	private void doTestFindConnectedRegionFiltering(boolean negate) throws BundleException, InvalidSyntaxException {
		region(REGION_A).connectRegion(region(REGION_B), createFilter(negate, BUNDLE_B));
		Bundle x = createBundle(BUNDLE_X);
		region(REGION_B).addBundle(x);

		this.candidates.add(bundle(BUNDLE_B));
		this.candidates.add(x);
		this.bundleFindHook.find(bundleContext(BUNDLE_A), this.candidates);
		assertTrue(this.candidates.contains(bundle(BUNDLE_B)));
		assertFalse(this.candidates.contains(x));
	}

	@Test
	public void testFindTransitive() throws BundleException, InvalidSyntaxException {
		doTestFindTransitive(false);
	}

	@Test
	public void testFindTransitiveWithNegate() throws BundleException, InvalidSyntaxException {
		doTestFindTransitive(true);
	}

	private void doTestFindTransitive(boolean negate) throws BundleException, InvalidSyntaxException {
		region(REGION_A).connectRegion(region(REGION_B), createFilter(negate, BUNDLE_C));
		region(REGION_B).connectRegion(region(REGION_C), createFilter(negate, BUNDLE_C));
		region(REGION_C).addBundle(bundle(BUNDLE_X));

		this.candidates.add(bundle(BUNDLE_B));
		this.candidates.add(bundle(BUNDLE_C));
		this.candidates.add(bundle(BUNDLE_X));
		this.bundleFindHook.find(bundleContext(BUNDLE_A), this.candidates);
		assertTrue(this.candidates.contains(bundle(BUNDLE_C)));
		assertFalse(this.candidates.contains(bundle(BUNDLE_B)));
		assertFalse(this.candidates.contains(bundle(BUNDLE_X)));
	}

	@Test
	public void testFindInCyclicGraph() throws BundleException, InvalidSyntaxException {
		doTestFindInCyclicGraph(false);
	}

	@Test
	public void testFindInCyclicGraphWithNegate() throws BundleException, InvalidSyntaxException {

		doTestFindInCyclicGraph(true);
	}

	private void doTestFindInCyclicGraph(boolean negate) throws BundleException, InvalidSyntaxException {
		region(REGION_D).addBundle(bundle(BUNDLE_X));

		region(REGION_A).connectRegion(region(REGION_B), createFilter(negate, BUNDLE_D, BUNDLE_X));
		region(REGION_B).connectRegion(region(REGION_A), createFilter(negate));

		region(REGION_B).connectRegion(region(REGION_D), createFilter(negate, BUNDLE_D));
		region(REGION_D).connectRegion(region(REGION_B), createFilter(negate));

		region(REGION_B).connectRegion(region(REGION_C), createFilter(negate, BUNDLE_X));
		region(REGION_C).connectRegion(region(REGION_B), createFilter(negate));

		region(REGION_C).connectRegion(region(REGION_D), createFilter(negate, BUNDLE_X));
		region(REGION_D).connectRegion(region(REGION_C), createFilter(negate));

		region(REGION_A).connectRegion(region(REGION_C), createFilter(negate));
		region(REGION_C).connectRegion(region(REGION_A), createFilter(negate));

		region(REGION_D).connectRegion(region(REGION_A), createFilter(negate));
		region(REGION_A).connectRegion(region(REGION_D), createFilter(negate));

		// Find from region A.
		this.candidates.add(bundle(BUNDLE_B));
		this.candidates.add(bundle(BUNDLE_C));
		this.candidates.add(bundle(BUNDLE_D));
		this.candidates.add(bundle(BUNDLE_X));

		this.bundleFindHook.find(bundleContext(BUNDLE_A), this.candidates);
		assertEquals(2, this.candidates.size());
		assertTrue(this.candidates.contains(bundle(BUNDLE_D)));
		assertTrue(this.candidates.contains(bundle(BUNDLE_X)));

		// Find from region B
		this.candidates.add(bundle(BUNDLE_B));
		this.candidates.add(bundle(BUNDLE_C));
		this.candidates.add(bundle(BUNDLE_D));
		this.candidates.add(bundle(BUNDLE_X));

		this.bundleFindHook.find(bundleContext(BUNDLE_B), this.candidates);
		assertEquals(3, this.candidates.size());
		assertTrue(this.candidates.contains(bundle(BUNDLE_B)));
		assertTrue(this.candidates.contains(bundle(BUNDLE_D)));
		assertTrue(this.candidates.contains(bundle(BUNDLE_X)));
	}

	@Test
	public void testFindFromSystemBundle() {
		this.candidates.add(bundle(BUNDLE_A));

		Bundle stubBundle = new StubBundle(0L, "sys", BUNDLE_VERSION, "");
		this.bundleFindHook.find(stubBundle.getBundleContext(), this.candidates);
		assertEquals(1, this.candidates.size());
		assertTrue(this.candidates.contains(bundle(BUNDLE_A)));
	}

	@Test
	public void testFindFromBundleInNoRegion() {
		this.candidates.add(bundle(BUNDLE_A));

		Bundle stranger = createBundle("stranger");
		this.bundleFindHook.find(stranger.getBundleContext(), this.candidates);
		assertEquals(0, this.candidates.size());
	}

	private Region createRegion(String regionName, String... bundleSymbolicNames) throws BundleException {
		Region region = this.digraph.createRegion(regionName);
		for (String bundleSymbolicName : bundleSymbolicNames) {
			Bundle stubBundle = createBundle(bundleSymbolicName);
			region.addBundle(stubBundle);
		}
		this.regions.put(regionName, region);
		return region;
	}

	private Region region(String regionName) {
		return this.regions.get(regionName);
	}

	private RegionFilter createFilter(boolean negate, String... bundleSymbolicNames) throws InvalidSyntaxException {
		Collection<String> filters = new ArrayList<>(bundleSymbolicNames.length);
		for (String bundleSymbolicName : bundleSymbolicNames) {
			filters.add('(' + RegionFilter.VISIBLE_BUNDLE_NAMESPACE + '=' + bundleSymbolicName + ')');
		}
		RegionFilterBuilder builder = digraph.createRegionFilterBuilder();
		for (String filter : filters) {
			builder.allow(RegionFilter.VISIBLE_BUNDLE_NAMESPACE, filter);
		}

		if (negate) {
			String negateFilter = "(!(|" + "(" + RegionFilter.VISIBLE_ALL_NAMESPACE_ATTRIBUTE + "=" + RegionFilter.VISIBLE_BUNDLE_NAMESPACE + ")" + "(" + RegionFilter.VISIBLE_ALL_NAMESPACE_ATTRIBUTE + "=" + RegionFilter.VISIBLE_BUNDLE_LIFECYCLE_NAMESPACE + ")" + "))";
			builder.allow(RegionFilter.VISIBLE_ALL_NAMESPACE, negateFilter);
		}
		return builder.build();
	}

	private Bundle createBundle(String bundleSymbolicName) {
		Bundle stubBundle = new StubBundle(this.bundleId++, bundleSymbolicName, BUNDLE_VERSION, "loc:" + bundleSymbolicName);
		this.bundles.put(bundleSymbolicName, stubBundle);
		return stubBundle;
	}

	private BundleContext bundleContext(String bundleSymbolicName) {
		return bundle(bundleSymbolicName).getBundleContext();
	}

	private Bundle bundle(String bundleSymbolicName) {
		Bundle bundleA = this.bundles.get(bundleSymbolicName);
		return bundleA;
	}

}
