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
 *    SpringSource, a division of VMware - initial API and implementation and/or initial documentation
 *******************************************************************************/

package org.eclipse.equinox.region.internal.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.*;
import org.eclipse.equinox.region.*;
import org.eclipse.equinox.region.RegionDigraph.FilteredRegion;
import org.eclipse.virgo.teststubs.osgi.framework.StubBundle;
import org.eclipse.virgo.teststubs.osgi.framework.StubBundleContext;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.*;

public class StandardRegionDigraphTests {

	private RegionDigraph digraph;

	private Region mockRegion1;

	private Region mockRegion2;

	private Region mockRegion3;

	private RegionFilter regionFilter1;

	private RegionFilter regionFilter2;

	@Before
	public void setUp() throws Exception {
		StubBundle stubSystemBundle = new StubBundle(0L, "osgi.framework", new Version("0"), "loc");
		StubBundleContext stubBundleContext = new StubBundleContext();
		stubBundleContext.addInstalledBundle(stubSystemBundle);
		this.digraph = RegionReflectionUtils.newStandardRegionDigraph(stubBundleContext, new ThreadLocal<Region>());

		this.mockRegion1 = mock(Region.class);
		when(this.mockRegion1.getName()).thenReturn("mockRegion1");
		when(this.mockRegion1.getRegionDigraph()).thenReturn(this.digraph);

		this.mockRegion2 = mock(Region.class);
		when(this.mockRegion2.getName()).thenReturn("mockRegion2");
		when(this.mockRegion2.getRegionDigraph()).thenReturn(this.digraph);

		this.mockRegion3 = mock(Region.class);
		when(this.mockRegion3.getName()).thenReturn("mockRegion3");
		when(this.mockRegion3.getRegionDigraph()).thenReturn(this.digraph);

	}

	private void setDefaultFilters() {
		this.regionFilter1 = digraph.createRegionFilterBuilder().build();
		this.regionFilter2 = digraph.createRegionFilterBuilder().build();
	}

	private void setAllowedFilters(String b1Name, Version b1Version, String b2Name, Version b2Version)
			throws InvalidSyntaxException {
		String filter1 = "(&(" + RegionFilter.VISIBLE_BUNDLE_NAMESPACE + "=" + b1Name + ")("
				+ Constants.BUNDLE_VERSION_ATTRIBUTE + "=" + b1Version + "))";
		regionFilter1 = digraph.createRegionFilterBuilder().allow(RegionFilter.VISIBLE_BUNDLE_NAMESPACE, filter1)
				.build();

		String filter2 = "(&(" + RegionFilter.VISIBLE_BUNDLE_NAMESPACE + "=" + b2Name + ")("
				+ Constants.BUNDLE_VERSION_ATTRIBUTE + "=" + b2Version + "))";
		regionFilter2 = digraph.createRegionFilterBuilder().allow(RegionFilter.VISIBLE_BUNDLE_NAMESPACE, filter2)
				.build();

	}

	@Test
	public void testConnect() throws BundleException {
		setDefaultFilters();

		this.digraph.connect(this.mockRegion1, this.regionFilter1, this.mockRegion2);
	}

	@Test
	public void testConnectWithFilterContents() throws BundleException, InvalidSyntaxException {
		String b1Name = "b1";
		Version b1Version = new Version("0");
		String b2Name = "b2";
		Version b2Version = new Version("0");
		setAllowedFilters(b1Name, b1Version, b2Name, b2Version);
		when(this.mockRegion1.getBundle(b1Name, b1Version)).thenReturn(null);
		when(this.mockRegion1.getBundle(b2Name, b2Version)).thenReturn(null);

		this.digraph.connect(this.mockRegion1, this.regionFilter1, this.mockRegion2);
		this.digraph.connect(this.mockRegion1, this.regionFilter2, this.mockRegion3);
	}

	@Test(expected = BundleException.class)
	public void testConnectLoop() throws BundleException {
		setDefaultFilters();

		this.digraph.connect(this.mockRegion1, this.regionFilter1, this.mockRegion1);
	}

	@Test(expected = BundleException.class)
	public void testDuplicateConnection() throws BundleException {
		setDefaultFilters();

		this.digraph.connect(this.mockRegion1, this.regionFilter1, this.mockRegion2);
		this.digraph.connect(this.mockRegion1, this.regionFilter2, this.mockRegion2);
	}

	@Test
	public void testReplaceConnection() throws BundleException {
		setDefaultFilters();

		RegionFilter existing;

		existing = this.digraph.replaceConnection(this.mockRegion1, this.regionFilter1, this.mockRegion2);
		assertEquals("Wrong existing filter.", null, existing);

		existing = this.digraph.replaceConnection(this.mockRegion1, this.regionFilter2, this.mockRegion2);
		assertEquals("Wrong existing filter.", this.regionFilter1, existing);

		existing = this.digraph.replaceConnection(this.mockRegion1, null, this.mockRegion2);
		assertEquals("Wrong existing filter.", this.regionFilter2, existing);

		existing = this.digraph.replaceConnection(this.mockRegion1, this.regionFilter1, this.mockRegion2);
		assertEquals("Wrong existing filter.", null, existing);
	}

	@Test
	public void testGetEdges() throws BundleException {
		setDefaultFilters();

		this.digraph.connect(this.mockRegion1, this.regionFilter1, this.mockRegion2);
		this.digraph.connect(this.mockRegion1, this.regionFilter2, this.mockRegion3);
		this.digraph.connect(this.mockRegion2, this.regionFilter2, this.mockRegion1);

		Set<FilteredRegion> edges = this.digraph.getEdges(this.mockRegion1);

		assertEquals(2, edges.size());

		for (FilteredRegion edge : edges) {
			if (edge.getRegion().equals(this.mockRegion2)) {
				assertEquals(this.regionFilter1, edge.getFilter());
			} else if (edge.getRegion().equals(this.mockRegion3)) {
				assertEquals(this.regionFilter2, edge.getFilter());
			} else {
				fail("unexpected edge");
			}
		}

		this.digraph.replaceConnection(this.mockRegion1, this.regionFilter2, this.mockRegion2);

		edges = this.digraph.getEdges(this.mockRegion1);
		assertEquals(2, edges.size());

		for (FilteredRegion edge : edges) {
			if (edge.getRegion().equals(this.mockRegion2)) {
				assertEquals(this.regionFilter2, edge.getFilter());
			} else if (edge.getRegion().equals(this.mockRegion3)) {
				assertEquals(this.regionFilter2, edge.getFilter());
			} else {
				fail("unexpected edge");
			}
		}
	}

	@Test
	public void testRemoveRegion() throws BundleException {
		setDefaultFilters();

		this.digraph.connect(this.mockRegion1, this.regionFilter1, this.mockRegion2);
		this.digraph.connect(this.mockRegion2, this.regionFilter2, this.mockRegion1);
		assertNotNull(this.digraph.getRegion("mockRegion1"));
		assertNotNull(this.digraph.getRegion("mockRegion2"));
		this.digraph.removeRegion(this.mockRegion1);
		assertNull(this.digraph.getRegion("mockRegion1"));
		assertNotNull(this.digraph.getRegion("mockRegion2"));
	}

	@Test
	public void testGetRegions() throws BundleException {
		setDefaultFilters();

		this.digraph.connect(this.mockRegion1, this.regionFilter1, this.mockRegion2);
		Set<Region> regions = this.digraph.getRegions();
		assertEquals(2, regions.size());
		assertTrue(regions.contains(this.mockRegion1));
		assertTrue(regions.contains(this.mockRegion2));
	}

	private static final String REGION_A = "A";
	private static final String REGION_B = "B";
	private static final String REGION_C = "C";
	private static final String REGION_D = "D";

	@Test
	public void testCopyRegion() throws BundleException, InvalidSyntaxException {
		RegionDigraph testDigraph = RegionReflectionUtils.newStandardRegionDigraph();
		long bundleId = 1;
		Region a = testDigraph.createRegion(REGION_A);
		a.addBundle(bundleId++);
		a.addBundle(bundleId++);
		Region b = testDigraph.createRegion(REGION_B);
		b.addBundle(bundleId++);
		b.addBundle(bundleId++);
		Region c = testDigraph.createRegion(REGION_C);
		c.addBundle(bundleId++);
		c.addBundle(bundleId++);
		Region d = testDigraph.createRegion(REGION_D);
		d.addBundle(bundleId++);
		d.addBundle(bundleId++);

		testDigraph.connect(a, testDigraph.createRegionFilterBuilder().allow("a", "(a=x)").build(), b);
		testDigraph.connect(b, testDigraph.createRegionFilterBuilder().allow("b", "(b=x)").build(), c);
		testDigraph.connect(c, testDigraph.createRegionFilterBuilder().allow("c", "(c=x)").build(), d);
		testDigraph.connect(d, testDigraph.createRegionFilterBuilder().allow("d", "(d=x)").build(), a);
		RegionDigraph testCopy = testDigraph.copy();
		StandardRegionDigraphPeristenceTests.assertEquals(testDigraph, testCopy);

		a = testCopy.getRegion(REGION_A);
		b = testCopy.getRegion(REGION_B);
		c = testCopy.getRegion(REGION_C);
		d = testCopy.getRegion(REGION_D);

		for (Region region : testCopy) {
			testCopy.removeRegion(region);
		}

		testCopy.connect(a, testCopy.createRegionFilterBuilder().allow("a", "(a=x)").build(), d);
		testCopy.connect(b, testCopy.createRegionFilterBuilder().allow("b", "(b=x)").build(), a);
		testCopy.connect(c, testCopy.createRegionFilterBuilder().allow("c", "(c=x)").build(), b);
		testCopy.connect(d, testCopy.createRegionFilterBuilder().allow("d", "(d=x)").build(), c);

		try {
			StandardRegionDigraphPeristenceTests.assertEquals(testDigraph, testCopy);
			fail("Digraphs must not be equal");
		} catch (AssertionError e) {
			// expected
		}
		testDigraph.replace(testCopy);
		StandardRegionDigraphPeristenceTests.assertEquals(testDigraph, testCopy);

		// test that we can continue to use the copy to replace as long as it is upto
		// date with the last replace
		Region testAdd1 = testCopy.createRegion("testAdd1");
		testCopy.connect(testAdd1, testCopy.createRegionFilterBuilder().allow("testAdd1", "(testAdd=x)").build(), a);
		try {
			StandardRegionDigraphPeristenceTests.assertEquals(testDigraph, testCopy);
			fail("Digraphs must not be equal");
		} catch (AssertionError e) {
			// expected
		}
		testDigraph.replace(testCopy);
		StandardRegionDigraphPeristenceTests.assertEquals(testDigraph, testCopy);

		// test that we fail if the digraph was modified since last copy/replace
		testCopy = testDigraph.copy();
		// add a new bundle to the original
		Region origA = testDigraph.getRegion(REGION_A);
		origA.addBundle(bundleId++);
		try {
			testDigraph.replace(testCopy);
			fail("Digraph changed since copy.");
		} catch (BundleException e) {
			// expected
		}

		// test that we fail if the digraph was modified since last copy/replace
		testCopy = testDigraph.copy();
		// add a new bundle to the original
		origA.removeBundle(bundleId);
		try {
			testDigraph.replace(testCopy);
			fail("Digraph changed since copy.");
		} catch (BundleException e) {
			// expected
		}

		testCopy = testDigraph.copy();
		// add a new region to the original
		Region testAdd2 = testDigraph.createRegion("testAdd2");
		testDigraph.connect(testAdd2, testCopy.createRegionFilterBuilder().allow("testAdd2", "(testAdd=x)").build(),
				origA);
		try {
			testDigraph.replace(testCopy);
			fail("Digraph changed since copy.");
		} catch (BundleException e) {
			// expected
		}

		testCopy = testDigraph.copy();
		// change a connection in the original
		testDigraph.removeRegion(testAdd2);
		testDigraph.connect(testAdd2, testCopy.createRegionFilterBuilder().allow("testAdd2", "(testAdd=y)").build(),
				origA);
		try {
			testDigraph.replace(testCopy);
			fail("Digraph changed since copy.");
		} catch (BundleException e) {
			// expected
		}

		testCopy = testDigraph.copy();
		Region origB = testDigraph.getRegion(REGION_B);
		// add a connection in the original
		testDigraph.connect(testAdd2, testCopy.createRegionFilterBuilder().allow("testAdd2", "(testAdd=y)").build(),
				origB);
		try {
			testDigraph.replace(testCopy);
			fail("Digraph changed since copy.");
		} catch (BundleException e) {
			// expected
		}
	}

	static class TestRegionDigraphVisitor implements RegionDigraphVisitor {
		final Collection<Region> visited = new ArrayList<Region>();
		final String namespace;
		final Map<String, ?> attributes;

		public TestRegionDigraphVisitor(String namespace, Map<String, ?> attributes) {
			super();
			this.namespace = namespace;
			this.attributes = attributes;
		}

		@Override
		public boolean visit(Region region) {
			visited.add(region);
			return true;
		}

		@Override
		public boolean preEdgeTraverse(RegionFilter regionFilter) {
			return regionFilter.isAllowed(namespace, attributes);
		}

		@Override
		public void postEdgeTraverse(RegionFilter regionFilter) {
			// nothing
		}

		Collection<Region> clearVisited() {
			Collection<Region> result = new ArrayList<Region>(visited);
			visited.clear();
			return result;
		}
	}

	@Test
	public void testVisitRegions() throws BundleException, InvalidSyntaxException {
		RegionDigraph testDigraph = RegionReflectionUtils.newStandardRegionDigraph();
		Region a = testDigraph.createRegion(REGION_A);
		Region b = testDigraph.createRegion(REGION_B);
		Region c = testDigraph.createRegion(REGION_C);
		Region d = testDigraph.createRegion(REGION_D);

		testDigraph.connect(a, testDigraph.createRegionFilterBuilder().allow("b", "(b=x)").allow("c", "(c=x)")
				.allow("d", "(d=x)").build(), b);
		testDigraph.connect(b, testDigraph.createRegionFilterBuilder().allow("c", "(c=x)").allow("d", "(d=x)").build(),
				c);
		testDigraph.connect(c, testDigraph.createRegionFilterBuilder().allow("d", "(d=x)").build(), d);

		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put("d", "x");
		TestRegionDigraphVisitor visitor = new TestRegionDigraphVisitor("d", attributes);

		Collection<Region> expected = new ArrayList<Region>(Arrays.asList(a, b, c, d));
		for (Region region : expected.toArray(new Region[0])) {
			testDigraph.visitSubgraph(region, visitor);
			Collection<Region> visited = visitor.clearVisited();
			assertEquals("Wrong number of visited: " + region, expected.size(), visited.size());
			assertTrue("Wrong visited content: " + region, visited.containsAll(expected));
			expected.remove(region);
		}

		attributes.clear();
		attributes.put("c", "x");
		visitor = new TestRegionDigraphVisitor("c", attributes);
		expected = new ArrayList<Region>(Arrays.asList(a, b, c));
		for (Region region : expected.toArray(new Region[0])) {
			testDigraph.visitSubgraph(region, visitor);
			Collection<Region> visited = visitor.clearVisited();
			assertEquals("Wrong number of visited: " + region, expected.size(), visited.size());
			assertTrue("Wrong visited content: " + region, visited.containsAll(expected));
			expected.remove(region);
		}

		attributes.clear();
		attributes.put("b", "x");
		visitor = new TestRegionDigraphVisitor("b", attributes);
		expected = new ArrayList<Region>(Arrays.asList(a, b));
		for (Region region : expected.toArray(new Region[0])) {
			testDigraph.visitSubgraph(region, visitor);
			Collection<Region> visited = visitor.clearVisited();
			assertEquals("Wrong number of visited: " + region, expected.size(), visited.size());
			assertTrue("Wrong visited content: " + region, visited.containsAll(expected));
			expected.remove(region);
		}
	}

	@Test
	public void testGetHooks() throws BundleException {
		setDefaultFilters();

		assertNotNull("Resolver Hook is null", digraph.getResolverHookFactory());
		assertNotNull("Bundle Event Hook is null", digraph.getBundleEventHook());
		assertNotNull("Bundle Find Hook is null", digraph.getBundleFindHook());
		assertNotNull("Servie Event Hook is null", digraph.getServiceEventHook());
		assertNotNull("Service Find Hook is null", digraph.getServiceFindHook());

		RegionDigraph copy = digraph.copy();
		assertNotNull("Resolver Hook is null", copy.getResolverHookFactory());
		assertNotNull("Bundle Event Hook is null", copy.getBundleEventHook());
		assertNotNull("Bundle Find Hook is null", copy.getBundleFindHook());
		assertNotNull("Servie Event Hook is null", copy.getServiceEventHook());
		assertNotNull("Service Find Hook is null", copy.getServiceFindHook());
	}

}
