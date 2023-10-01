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

package org.eclipse.equinox.region.internal.tests.hook;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.*;
import org.eclipse.equinox.region.*;
import org.eclipse.equinox.region.internal.tests.RegionReflectionUtils;
import org.eclipse.virgo.teststubs.osgi.framework.StubBundle;
import org.eclipse.virgo.teststubs.osgi.framework.StubBundleContext;
import org.junit.*;
import org.osgi.framework.*;
import org.osgi.framework.hooks.bundle.EventHook;

public class RegionBundleEventHookTests {

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

	private EventHook bundleEventHook;

	private Map<String, Region> regions;

	private Map<String, Bundle> bundles;

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
		this.bundleEventHook = RegionReflectionUtils.newRegionBundleEventHook(digraph, threadLocal,
				stubSystemBundle.getBundleId());

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
	public void testEventInSameRegion() {
		Collection<BundleContext> contexts = bundleContexts(BUNDLE_A);
		this.bundleEventHook.event(bundleEvent(BUNDLE_A), contexts);
		assertTrue(contexts.contains(bundleContext(BUNDLE_A)));
	}

	@Test
	public void testEventInDisconnectedRegion() {
		Collection<BundleContext> contexts = bundleContexts(BUNDLE_A);
		this.bundleEventHook.event(bundleEvent(BUNDLE_B), contexts);
		assertFalse(contexts.contains(bundleContext(BUNDLE_A)));
	}

	@Test
	public void testEventConnectedRegionAllowed() throws BundleException, InvalidSyntaxException {
		doTestEventConnectedRegionAllowed(false);
	}

	@Test
	public void testEventConnectedRegionAllowedWithNegate() throws BundleException, InvalidSyntaxException {
		doTestEventConnectedRegionAllowed(true);
	}

	private void doTestEventConnectedRegionAllowed(boolean negate) throws BundleException, InvalidSyntaxException {
		RegionFilter filter = createFilter(negate, BUNDLE_B);
		region(REGION_A).connectRegion(region(REGION_B), filter);

		Collection<BundleContext> contexts = bundleContexts(BUNDLE_A);
		this.bundleEventHook.event(bundleEvent(BUNDLE_B), contexts);
		assertTrue(contexts.contains(bundleContext(BUNDLE_A)));
	}

	@Test
	public void testEventConnectedRegionFiltering() throws BundleException, InvalidSyntaxException {
		doTestEventConnectedRegionFiltering(false);
	}

	@Test
	public void testEventConnectedRegionFilteringWithNegate() throws BundleException, InvalidSyntaxException {
		doTestEventConnectedRegionFiltering(true);
	}

	private void doTestEventConnectedRegionFiltering(boolean negate) throws BundleException, InvalidSyntaxException {
		region(REGION_A).connectRegion(region(REGION_B), createFilter(negate, BUNDLE_B));
		Bundle x = createBundle(BUNDLE_X);
		region(REGION_B).addBundle(x);

		Collection<BundleContext> contexts = bundleContexts(BUNDLE_A, BUNDLE_B, BUNDLE_X);
		this.bundleEventHook.event(bundleEvent(BUNDLE_X), contexts);
		assertTrue(contexts.contains(bundleContext(BUNDLE_B)));
		assertTrue(contexts.contains(bundleContext(BUNDLE_X)));
		assertFalse(contexts.contains(bundleContext(BUNDLE_A)));
	}

	@Test
	public void testEventTransitive() throws BundleException, InvalidSyntaxException {
		doTestEventTransitive(false);
	}

	@Test
	public void testEventTransitiveWithNegate() throws BundleException, InvalidSyntaxException {
		doTestEventTransitive(true);
	}

	private void doTestEventTransitive(boolean negate) throws BundleException, InvalidSyntaxException {
		region(REGION_A).connectRegion(region(REGION_B), createFilter(negate, BUNDLE_C));
		region(REGION_B).connectRegion(region(REGION_C), createFilter(negate, BUNDLE_C));
		region(REGION_C).addBundle(bundle(BUNDLE_X));

		Collection<BundleContext> contexts = bundleContexts(BUNDLE_A, BUNDLE_B, BUNDLE_X);
		this.bundleEventHook.event(bundleEvent(BUNDLE_C), contexts);
		assertTrue(contexts.contains(bundleContext(BUNDLE_B)));
		assertTrue(contexts.contains(bundleContext(BUNDLE_X)));
		assertTrue(contexts.contains(bundleContext(BUNDLE_A)));

		contexts = bundleContexts(BUNDLE_A, BUNDLE_B, BUNDLE_X);
		this.bundleEventHook.event(bundleEvent(BUNDLE_X), contexts);
		assertFalse(contexts.contains(bundleContext(BUNDLE_B)));
		assertTrue(contexts.contains(bundleContext(BUNDLE_X)));
		assertFalse(contexts.contains(bundleContext(BUNDLE_A)));
	}

	@Test
	public void testEventInCyclicGraph() throws BundleException, InvalidSyntaxException {
		doTestEventInCyclicGraph(false);
	}

	@Test
	public void testEventInCyclicGraphWithNegate() throws BundleException, InvalidSyntaxException {

		doTestEventInCyclicGraph(true);
	}

	private void doTestEventInCyclicGraph(boolean negate) throws BundleException, InvalidSyntaxException {
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

		Collection<BundleContext> contexts = bundleContexts(BUNDLE_A, BUNDLE_B);
		this.bundleEventHook.event(bundleEvent(BUNDLE_B), contexts);
		assertFalse(contexts.contains(bundleContext(BUNDLE_A)));
		assertTrue(contexts.contains(bundleContext(BUNDLE_B)));

		contexts = bundleContexts(BUNDLE_A, BUNDLE_B);
		this.bundleEventHook.event(bundleEvent(BUNDLE_C), contexts);
		assertFalse(contexts.contains(bundleContext(BUNDLE_A)));
		assertFalse(contexts.contains(bundleContext(BUNDLE_B)));

		contexts = bundleContexts(BUNDLE_A, BUNDLE_B);
		this.bundleEventHook.event(bundleEvent(BUNDLE_D), contexts);
		assertTrue(contexts.contains(bundleContext(BUNDLE_A)));
		assertTrue(contexts.contains(bundleContext(BUNDLE_B)));

		contexts = bundleContexts(BUNDLE_A, BUNDLE_B);
		this.bundleEventHook.event(bundleEvent(BUNDLE_X), contexts);
		assertTrue(contexts.contains(bundleContext(BUNDLE_A)));
		assertTrue(contexts.contains(bundleContext(BUNDLE_B)));
	}

	@Test
	public void testEventFromSystemBundle() {
		Bundle systemBundle = new StubBundle(0L, "sys", BUNDLE_VERSION, "");
		Collection<BundleContext> contexts = new ArrayList<>(Arrays.asList(systemBundle.getBundleContext()));
		this.bundleEventHook.event(bundleEvent(BUNDLE_A), contexts);
		assertTrue(contexts.contains(systemBundle.getBundleContext()));
	}

	@Test
	public void testEventFromBundleInNoRegion() {
		Bundle stranger = createBundle("stranger");
		Collection<BundleContext> contexts = new ArrayList<>(Arrays.asList(stranger.getBundleContext()));
		this.bundleEventHook.event(bundleEvent(BUNDLE_A), contexts);
		assertTrue(contexts.isEmpty());
	}

	@Test
	public void testDefaultRegion() {
		this.digraph.setDefaultRegion(null);
		Bundle x = createBundle("installed.X");
		this.bundleEventHook.event(new BundleEvent(BundleEvent.INSTALLED, x, bundle(BUNDLE_A)),
				Collections.<BundleContext>emptyList());
		assertTrue(this.digraph.getRegion(x).equals(region(REGION_A)));

		this.digraph.setDefaultRegion(region(REGION_B));
		Bundle y = createBundle("installed.Y");
		this.bundleEventHook.event(new BundleEvent(BundleEvent.INSTALLED, y, bundle(BUNDLE_A)),
				Collections.<BundleContext>emptyList());
		assertTrue(this.digraph.getRegion(y).equals(region(REGION_B)));
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
			String negateFilter = "(!(|" + "(" + RegionFilter.VISIBLE_ALL_NAMESPACE_ATTRIBUTE + "="
					+ RegionFilter.VISIBLE_BUNDLE_NAMESPACE + ")" + "(" + RegionFilter.VISIBLE_ALL_NAMESPACE_ATTRIBUTE
					+ "=" + RegionFilter.VISIBLE_BUNDLE_LIFECYCLE_NAMESPACE + ")" + "))";
			builder.allow(RegionFilter.VISIBLE_ALL_NAMESPACE, negateFilter);
		}
		return builder.build();
	}

	private Bundle createBundle(String bundleSymbolicName) {
		Bundle stubBundle = new StubBundle(this.bundleId++, bundleSymbolicName, BUNDLE_VERSION,
				"loc:" + bundleSymbolicName);
		this.bundles.put(bundleSymbolicName, stubBundle);
		return stubBundle;
	}

	private Collection<BundleContext> bundleContexts(String... bundleSymbolicNames) {
		Collection<BundleContext> contexts = new ArrayList<>();
		for (String symbolicName : bundleSymbolicNames) {
			contexts.add(bundleContext(symbolicName));
		}
		return contexts;
	}

	private BundleContext bundleContext(String bundleSymbolicName) {
		return bundle(bundleSymbolicName).getBundleContext();
	}

	private BundleEvent bundleEvent(String budnleSymbolicName) {
		return new BundleEvent(BundleEvent.STARTED, bundle(budnleSymbolicName));
	}

	private Bundle bundle(String bundleSymbolicName) {
		Bundle bundleA = this.bundles.get(bundleSymbolicName);
		return bundleA;
	}
}
