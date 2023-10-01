/*******************************************************************************
 * Copyright (c) 2012, 2015 VMware Inc.
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
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.wiring.*;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

public class RegionResolverHookTests {

	private static final String PACKAGE_A = "package.a";

	private static final String PACKAGE_B = "package.b";

	private static final String PACKAGE_C = "package.c";

	private static final String PACKAGE_D = "package.d";

	private static final String PACKAGE_X = "package.x";

	private static final String PACKAGE_DUP = "duplicate";

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

	private ResolverHook resolverHook;

	private Map<String, Region> regions;

	private Map<String, Bundle> bundles;

	private Collection<BundleCapability> candidates;

	private ThreadLocal<Region> threadLocal;

	@Before
	public void setUp() throws Exception {
		this.bundleId = 1L;
		this.regions = new HashMap<>();
		this.bundles = new HashMap<>();
		this.threadLocal = new ThreadLocal<>();
		StubBundle stubSystemBundle = new StubBundle(0L, "osgi.framework", new Version("0"), "loc");
		StubBundleContext stubBundleContext = new StubBundleContext();
		stubBundleContext.addInstalledBundle(stubSystemBundle);
		this.digraph = RegionReflectionUtils.newStandardRegionDigraph(stubBundleContext, this.threadLocal);
		this.resolverHook = RegionReflectionUtils.newRegionResolverHook(this.digraph);
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
	public void testResolveInSameRegion() {
		this.candidates.add(packageCapability(BUNDLE_A, PACKAGE_A));
		this.resolverHook.filterMatches(bundleRequirement(BUNDLE_A), this.candidates);
		assertTrue(this.candidates.contains(packageCapability(BUNDLE_A, PACKAGE_A)));
	}

	@Test
	public void testNoRegionResolvable() {
		Collection<BundleRevision> resolvable = new ArrayList<>(
				Collections.singleton(new StubBundleRevision(bundle(BUNDLE_X))));
		this.resolverHook.filterResolvable(resolvable);
		assertTrue("Resolvable is not empty" + resolvable, resolvable.isEmpty());
	}

	@Test
	public void testResolveInDisconnectedRegion() {
		this.candidates.add(packageCapability(BUNDLE_B, PACKAGE_B));
		this.resolverHook.filterMatches(bundleRequirement(BUNDLE_A), this.candidates);
		assertFalse(this.candidates.contains(packageCapability(BUNDLE_B, PACKAGE_B)));
	}

	@Test
	public void testResolveConnectedRegionAllowed() throws BundleException, InvalidSyntaxException {
		RegionFilter filter = createFilter(PACKAGE_B);
		region(REGION_A).connectRegion(region(REGION_B), filter);

		this.candidates.add(packageCapability(BUNDLE_B, PACKAGE_B));
		this.resolverHook.filterMatches(bundleRequirement(BUNDLE_A), this.candidates);
		assertTrue(this.candidates.contains(packageCapability(BUNDLE_B, PACKAGE_B)));
	}

	@Test
	public void testResolveBundleCapabilityConnectedRegionAllowed() throws BundleException, InvalidSyntaxException {
		RegionFilter filter = createBundleFilter(BUNDLE_B, BUNDLE_VERSION);
		region(REGION_A).connectRegion(region(REGION_B), filter);

		this.candidates.add(bundleCapability(BUNDLE_B));
		this.resolverHook.filterMatches(bundleRequirement(BUNDLE_A), this.candidates);
		assertTrue(this.candidates.contains(bundleCapability(BUNDLE_B)));
	}

	@Test
	public void testResolveConnectedRegionFiltering() throws BundleException, InvalidSyntaxException {
		region(REGION_A).connectRegion(region(REGION_B), createFilter(PACKAGE_B));
		Bundle x = createBundle(BUNDLE_X);
		region(REGION_B).addBundle(x);

		this.candidates.add(packageCapability(BUNDLE_B, PACKAGE_B));
		this.candidates.add(packageCapability(BUNDLE_X, PACKAGE_X));
		this.resolverHook.filterMatches(bundleRequirement(BUNDLE_A), this.candidates);
		assertTrue(this.candidates.contains(packageCapability(BUNDLE_B, PACKAGE_B)));
		assertFalse(this.candidates.contains(packageCapability(BUNDLE_X, PACKAGE_X)));
	}

	@Test
	public void testResolveBundleConnectedRegionFiltering() throws BundleException, InvalidSyntaxException {
		RegionFilter filter = createBundleFilter(BUNDLE_B, BUNDLE_VERSION);
		region(REGION_A).connectRegion(region(REGION_B), filter);
		Bundle x = createBundle(BUNDLE_X);
		region(REGION_B).addBundle(x);

		this.candidates.add(bundleCapability(BUNDLE_B));
		this.candidates.add(bundleCapability(BUNDLE_X));
		this.resolverHook.filterMatches(bundleRequirement(BUNDLE_A), this.candidates);
		assertTrue(this.candidates.contains(bundleCapability(BUNDLE_B)));
		assertFalse(this.candidates.contains(bundleCapability(BUNDLE_X)));
	}

	@Test
	public void testResolveSingletonInSameRegions() {
		List<BundleCapability> collisionCandidates = new ArrayList<>();
		collisionCandidates.add(bundleCapability(BUNDLE_B));
		collisionCandidates.add(bundleCapability(BUNDLE_C));
		collisionCandidates.add(bundleCapability(BUNDLE_D));
		this.resolverHook.filterSingletonCollisions(bundleCapability(BUNDLE_A), collisionCandidates);
		assertEquals("Wrong number of collitions", 0, collisionCandidates.size());
	}

	@Test
	public void testResolveSingletonInDifferentRegions() throws BundleException {
		region(REGION_A).addBundle(bundle(BUNDLE_X));
		BundleCapability collision = bundleCapability(BUNDLE_X);
		List<BundleCapability> collisionCandidates = new ArrayList<>();
		collisionCandidates.add(collision);
		collisionCandidates.add(bundleCapability(BUNDLE_B));
		collisionCandidates.add(bundleCapability(BUNDLE_C));
		collisionCandidates.add(bundleCapability(BUNDLE_D));
		this.resolverHook.filterSingletonCollisions(bundleCapability(BUNDLE_A), collisionCandidates);
		assertEquals("Wrong number of collitions", 1, collisionCandidates.size());
		collisionCandidates.contains(collision);
	}

	@Test
	public void testResolveSingletonConnectedRegions() throws BundleException, InvalidSyntaxException {
		RegionFilter filter = createBundleFilter(BUNDLE_B, BUNDLE_VERSION);
		region(REGION_A).connectRegion(region(REGION_B), filter);
		region(REGION_A).addBundle(bundle(BUNDLE_X));
		BundleCapability collisionX = bundleCapability(BUNDLE_X);
		BundleCapability collisionB = bundleCapability(BUNDLE_B);
		List<BundleCapability> collisionCandidates = new ArrayList<>();
		collisionCandidates.add(collisionX);
		collisionCandidates.add(collisionB);
		collisionCandidates.add(bundleCapability(BUNDLE_C));
		collisionCandidates.add(bundleCapability(BUNDLE_D));
		this.resolverHook.filterSingletonCollisions(bundleCapability(BUNDLE_A), collisionCandidates);
		assertEquals("Wrong number of collitions", 2, collisionCandidates.size());
		collisionCandidates.contains(collisionX);
		collisionCandidates.contains(collisionB);
	}

	@Test
	public void testResolveTransitive() throws BundleException, InvalidSyntaxException {
		region(REGION_A).connectRegion(region(REGION_B), createFilter(PACKAGE_C));
		region(REGION_B).connectRegion(region(REGION_C), createFilter(PACKAGE_C));
		region(REGION_C).addBundle(bundle(BUNDLE_X));

		this.candidates.add(packageCapability(BUNDLE_B, PACKAGE_B));
		this.candidates.add(packageCapability(BUNDLE_C, PACKAGE_C));
		this.candidates.add(packageCapability(BUNDLE_X, PACKAGE_X));
		this.resolverHook.filterMatches(bundleRequirement(BUNDLE_A), this.candidates);
		assertTrue(this.candidates.contains(packageCapability(BUNDLE_C, PACKAGE_C)));
		assertFalse(this.candidates.contains(packageCapability(BUNDLE_B, PACKAGE_B)));
		assertFalse(this.candidates.contains(packageCapability(BUNDLE_X, PACKAGE_X)));
	}

	@Test
	public void testResolveTransitiveDups() throws BundleException, InvalidSyntaxException {
		region(REGION_A).connectRegion(region(REGION_B), createFilter(PACKAGE_B));
		region(REGION_A).connectRegion(region(REGION_C), createFilter(PACKAGE_DUP));
		region(REGION_A).connectRegion(region(REGION_D), createFilter(PACKAGE_DUP));
		region(REGION_B).connectRegion(region(REGION_C), createFilter(PACKAGE_DUP));
		region(REGION_C).connectRegion(region(REGION_D), createFilter(PACKAGE_DUP));
		region(REGION_D).connectRegion(region(REGION_A), createFilter(PACKAGE_DUP));

		this.candidates.add(packageCapability(BUNDLE_A, PACKAGE_DUP));
		this.candidates.add(packageCapability(BUNDLE_B, PACKAGE_DUP));
		this.candidates.add(packageCapability(BUNDLE_C, PACKAGE_DUP));
		this.candidates.add(packageCapability(BUNDLE_D, PACKAGE_DUP));

		Collection<BundleCapability> testCandidates = new ArrayList<>(this.candidates);
		this.resolverHook.filterMatches(bundleRequirement(BUNDLE_A), testCandidates);
		assertTrue(testCandidates.contains(packageCapability(BUNDLE_A, PACKAGE_DUP)));
		assertFalse(testCandidates.contains(packageCapability(BUNDLE_B, PACKAGE_DUP)));
		assertTrue(testCandidates.contains(packageCapability(BUNDLE_C, PACKAGE_DUP)));
		assertTrue(testCandidates.contains(packageCapability(BUNDLE_D, PACKAGE_DUP)));

		testCandidates = new ArrayList<>(this.candidates);
		this.resolverHook.filterMatches(bundleRequirement(BUNDLE_B), testCandidates);
		assertTrue(testCandidates.contains(packageCapability(BUNDLE_A, PACKAGE_DUP)));
		assertTrue(testCandidates.contains(packageCapability(BUNDLE_B, PACKAGE_DUP)));
		assertTrue(testCandidates.contains(packageCapability(BUNDLE_C, PACKAGE_DUP)));
		assertTrue(testCandidates.contains(packageCapability(BUNDLE_D, PACKAGE_DUP)));
	}

	@Test
	public void testResolveInCyclicGraph() throws BundleException, InvalidSyntaxException {
		region(REGION_D).addBundle(bundle(BUNDLE_X));

		region(REGION_A).connectRegion(region(REGION_B), createFilter(PACKAGE_D, PACKAGE_X));
		region(REGION_B).connectRegion(region(REGION_A), createFilter());

		region(REGION_B).connectRegion(region(REGION_D), createFilter(PACKAGE_D));
		region(REGION_D).connectRegion(region(REGION_B), createFilter());

		region(REGION_B).connectRegion(region(REGION_C), createFilter(PACKAGE_X));
		region(REGION_C).connectRegion(region(REGION_B), createFilter());

		region(REGION_C).connectRegion(region(REGION_D), createFilter(PACKAGE_X));
		region(REGION_D).connectRegion(region(REGION_C), createFilter());

		region(REGION_A).connectRegion(region(REGION_C), createFilter());
		region(REGION_C).connectRegion(region(REGION_A), createFilter());

		region(REGION_D).connectRegion(region(REGION_A), createFilter());
		region(REGION_A).connectRegion(region(REGION_D), createFilter());

		// Find from region A.
		this.candidates.add(packageCapability(BUNDLE_B, PACKAGE_B));
		this.candidates.add(packageCapability(BUNDLE_C, PACKAGE_C));
		this.candidates.add(packageCapability(BUNDLE_D, PACKAGE_D));
		this.candidates.add(packageCapability(BUNDLE_X, PACKAGE_X));

		this.resolverHook.filterMatches(bundleRequirement(BUNDLE_A), this.candidates);
		assertEquals(2, this.candidates.size());
		assertTrue(this.candidates.contains(packageCapability(BUNDLE_D, PACKAGE_D)));
		assertTrue(this.candidates.contains(packageCapability(BUNDLE_X, PACKAGE_X)));

		// Find from region B
		this.candidates.add(packageCapability(BUNDLE_B, PACKAGE_B));
		this.candidates.add(packageCapability(BUNDLE_C, PACKAGE_C));
		this.candidates.add(packageCapability(BUNDLE_D, PACKAGE_D));
		this.candidates.add(packageCapability(BUNDLE_X, PACKAGE_X));

		this.resolverHook.filterMatches(bundleRequirement(BUNDLE_B), this.candidates);
		assertEquals(3, this.candidates.size());
		assertTrue(this.candidates.contains(packageCapability(BUNDLE_B, PACKAGE_B)));
		assertTrue(this.candidates.contains(packageCapability(BUNDLE_D, PACKAGE_D)));
		assertTrue(this.candidates.contains(packageCapability(BUNDLE_X, PACKAGE_X)));
	}

	@Test
	public void testResolveFromSystemBundle() {
		this.candidates.add(packageCapability(BUNDLE_A, PACKAGE_A));

		Bundle stubBundle = new StubBundle(0L, "sys", BUNDLE_VERSION, "");
		this.resolverHook.filterMatches(new StubBundleRequirement(stubBundle), this.candidates);
		assertEquals(1, this.candidates.size());
		assertTrue(this.candidates.contains(packageCapability(BUNDLE_A, PACKAGE_A)));
	}

	@Test
	public void testResolveFromBundleInNoRegion() {
		this.candidates.add(packageCapability(BUNDLE_A, PACKAGE_A));

		Bundle stranger = createBundle("stranger");
		this.resolverHook.filterMatches(new StubBundleRequirement(stranger), this.candidates);
		assertEquals(0, this.candidates.size());
	}

	@Test
	public void testUnimplementedMethods() {
		this.resolverHook.end();
	}

	private BundleCapability packageCapability(final String bundleSymbolicName, String packageName) {
		return new StubPackageCapability(bundleSymbolicName, packageName);
	}

	private BundleCapability bundleCapability(String bundleSymbolicName) {
		return new StubBundleCapability(bundleSymbolicName);
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

	private RegionFilter createFilter(final String... packageNames) throws InvalidSyntaxException {
		Collection<String> filters = new ArrayList<>(packageNames.length);
		for (String pkg : packageNames) {
			filters.add('(' + RegionFilter.VISIBLE_PACKAGE_NAMESPACE + '=' + pkg + ')');
		}
		RegionFilterBuilder builder = digraph.createRegionFilterBuilder();
		for (String filter : filters) {
			builder.allow(RegionFilter.VISIBLE_PACKAGE_NAMESPACE, filter);
		}
		return builder.build();
	}

	private RegionFilter createBundleFilter(String bundleSymbolicName, Version bundleVersion)
			throws InvalidSyntaxException {
		String bundleFilter = "(&(" + RegionFilter.VISIBLE_BUNDLE_NAMESPACE + '=' + bundleSymbolicName + ')' + '('
				+ Constants.BUNDLE_VERSION_ATTRIBUTE + ">=" + (bundleVersion == null ? "0" : bundleVersion.toString())
				+ "))";
		RegionFilterBuilder builder = digraph.createRegionFilterBuilder();
		return builder.allow(RegionFilter.VISIBLE_BUNDLE_NAMESPACE, bundleFilter).build();
	}

	private Bundle createBundle(String bundleSymbolicName) {
		Bundle stubBundle = new StubBundle(this.bundleId++, bundleSymbolicName, BUNDLE_VERSION,
				"loc:" + bundleSymbolicName);
		this.bundles.put(bundleSymbolicName, stubBundle);
		return stubBundle;
	}

	BundleRequirement bundleRequirement(String bundleSymbolicName) {
		return new StubBundleRequirement(bundle(bundleSymbolicName));
	}

	Bundle bundle(String bundleSymbolicName) {
		Bundle bundleA = this.bundles.get(bundleSymbolicName);
		return bundleA;
	}

	final class StubPackageCapability implements BundleCapability {

		private final String bundleSymbolicName;

		private final String packageName;

		StubPackageCapability(String bundleSymbolicName, String packageName) {
			this.bundleSymbolicName = bundleSymbolicName;
			this.packageName = packageName;
		}

		@Override
		public String getNamespace() {
			return BundleRevision.PACKAGE_NAMESPACE;
		}

		@Override
		public Map<String, String> getDirectives() {
			return new HashMap<>();
		}

		@Override
		public Map<String, Object> getAttributes() {
			HashMap<String, Object> attributes = new HashMap<>();
			attributes.put(BundleRevision.PACKAGE_NAMESPACE, this.packageName);
			return attributes;
		}

		@Override
		public BundleRevision getResource() {
			return getRevision();
		}

		@Override
		public BundleRevision getRevision() {
			return new StubBundleRevision(bundle(this.bundleSymbolicName));
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + (this.bundleSymbolicName == null ? 0 : this.bundleSymbolicName.hashCode());
			result = prime * result + (this.packageName == null ? 0 : this.packageName.hashCode());
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
			if (!(obj instanceof StubPackageCapability)) {
				return false;
			}
			StubPackageCapability other = (StubPackageCapability) obj;
			if (!getOuterType().equals(other.getOuterType())) {
				return false;
			}
			if (this.bundleSymbolicName == null) {
				if (other.bundleSymbolicName != null) {
					return false;
				}
			} else if (!this.bundleSymbolicName.equals(other.bundleSymbolicName)) {
				return false;
			}
			if (this.packageName == null) {
				if (other.packageName != null) {
					return false;
				}
			} else if (!this.packageName.equals(other.packageName)) {
				return false;
			}
			return true;
		}

		private RegionResolverHookTests getOuterType() {
			return RegionResolverHookTests.this;
		}

	}

	final class StubBundleCapability implements BundleCapability {

		private final String bundleSymbolicName;

		StubBundleCapability(String bundleSymbolicName) {
			this.bundleSymbolicName = bundleSymbolicName;
		}

		@Override
		public String getNamespace() {
			return BundleRevision.BUNDLE_NAMESPACE;
		}

		@Override
		public Map<String, String> getDirectives() {
			return new HashMap<>();
		}

		@Override
		public Map<String, Object> getAttributes() {
			HashMap<String, Object> attributes = new HashMap<>();
			attributes.put(BundleRevision.BUNDLE_NAMESPACE, bundleSymbolicName);
			return attributes;
		}

		@Override
		public BundleRevision getResource() {
			return getRevision();
		}

		@Override
		public BundleRevision getRevision() {
			return new StubBundleRevision(bundle(this.bundleSymbolicName));
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((bundleSymbolicName == null) ? 0 : bundleSymbolicName.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof StubBundleCapability))
				return false;
			StubBundleCapability other = (StubBundleCapability) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (bundleSymbolicName == null) {
				if (other.bundleSymbolicName != null)
					return false;
			} else if (!bundleSymbolicName.equals(other.bundleSymbolicName))
				return false;
			return true;
		}

		private RegionResolverHookTests getOuterType() {
			return RegionResolverHookTests.this;
		}

	}

	final class StubBundleRequirement implements BundleRequirement {

		private final StubBundleRevision bundleRevision;

		StubBundleRequirement(Bundle bundle) {
			this.bundleRevision = new StubBundleRevision(bundle);
		}

		@Override
		public String getNamespace() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Map<String, String> getDirectives() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Map<String, Object> getAttributes() {
			throw new UnsupportedOperationException();
		}

		@Override
		public BundleRevision getResource() {
			return getRevision();
		}

		@Override
		public BundleRevision getRevision() {
			return this.bundleRevision;
		}

		@Override
		public boolean matches(BundleCapability capability) {
			throw new UnsupportedOperationException();
		}
	}

	final class StubBundleRevision implements BundleRevision {

		private final Bundle bundle;

		StubBundleRevision(Bundle bundle) {
			this.bundle = bundle;
		}

		@Override
		public Bundle getBundle() {
			return this.bundle;
		}

		@Override
		public String getSymbolicName() {
			return this.bundle.getSymbolicName();
		}

		@Override
		public Version getVersion() {
			return this.bundle.getVersion();
		}

		@Override
		public List<BundleCapability> getDeclaredCapabilities(String namespace) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int getTypes() {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<BundleRequirement> getDeclaredRequirements(String namespace) {
			throw new UnsupportedOperationException();
		}

		@Override
		public BundleWiring getWiring() {
			throw new UnsupportedOperationException();
		}

		@SuppressWarnings({ "cast", "unchecked", "rawtypes" })
		@Override
		public List<Capability> getCapabilities(String namespace) {
			return (List<Capability>) (List) getDeclaredCapabilities(namespace);
		}

		@SuppressWarnings({ "cast", "unchecked", "rawtypes" })
		@Override
		public List<Requirement> getRequirements(String namespace) {
			return (List<Requirement>) (List) getDeclaredRequirements(namespace);
		}
	}

}
