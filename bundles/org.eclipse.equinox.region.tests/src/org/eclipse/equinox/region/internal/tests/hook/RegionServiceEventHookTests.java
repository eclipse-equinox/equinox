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
import org.eclipse.virgo.teststubs.osgi.framework.*;
import org.junit.*;
import org.osgi.framework.*;
import org.osgi.framework.hooks.service.EventHook;

/**
 * This testcase was based on {@link RegionBundleFindHookTests}.
 */
public class RegionServiceEventHookTests {

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

	private static final String DUPLICATE = "Duplicate";
	private static final String DUPLICATE_FIlTER = DUPLICATE + "*";

	private RegionDigraph digraph;

	@SuppressWarnings("deprecation")
	private EventHook serviceEventHook;

	private Map<String, Region> regions;

	private Map<String, Bundle> bundles;

	private Map<String, ServiceReference<Object>> serviceReferences;

	private ThreadLocal<Region> threadLocal;

	@Before
	public void setUp() throws Exception {
		this.bundleId = 1L;
		this.regions = new HashMap<>();
		this.bundles = new HashMap<>();
		this.serviceReferences = new HashMap<>();

		StubBundle stubSystemBundle = new StubBundle(0L, "osgi.framework", new Version("0"), "loc");
		StubBundleContext stubBundleContext = new StubBundleContext();
		stubBundleContext.addInstalledBundle(stubSystemBundle);
		this.threadLocal = new ThreadLocal<>();
		this.digraph = RegionReflectionUtils.newStandardRegionDigraph(stubBundleContext, this.threadLocal);
		this.serviceEventHook = RegionReflectionUtils.newRegionServiceEventHook(this.digraph);

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
		this.serviceEventHook.event(serviceEvent(BUNDLE_A), contexts);
		assertTrue(contexts.contains(bundleContext(BUNDLE_A)));
	}

	@Test
	public void testEventInDisconnectedRegion() {
		Collection<BundleContext> contexts = bundleContexts(BUNDLE_A);
		this.serviceEventHook.event(serviceEvent(BUNDLE_B), contexts);
		assertFalse(contexts.contains(bundleContext(BUNDLE_A)));
	}

	@Test
	public void testEventConnectedRegionAllowed() throws BundleException, InvalidSyntaxException {
		RegionFilter filter = createFilter(BUNDLE_B);
		region(REGION_A).connectRegion(region(REGION_B), filter);

		Collection<BundleContext> contexts = bundleContexts(BUNDLE_A);
		this.serviceEventHook.event(serviceEvent(BUNDLE_B), contexts);
		assertTrue(contexts.contains(bundleContext(BUNDLE_A)));
	}

	@Test
	public void testEventConnectedRegionFiltering() throws BundleException, InvalidSyntaxException {
		region(REGION_A).connectRegion(region(REGION_B), createFilter(BUNDLE_B));
		Bundle x = createBundle(BUNDLE_X);
		region(REGION_B).addBundle(x);

		Collection<BundleContext> contexts = bundleContexts(BUNDLE_A, BUNDLE_B, BUNDLE_X);
		this.serviceEventHook.event(serviceEvent(BUNDLE_X), contexts);
		assertTrue(contexts.contains(bundleContext(BUNDLE_B)));
		assertTrue(contexts.contains(bundleContext(BUNDLE_X)));
		assertFalse(contexts.contains(bundleContext(BUNDLE_A)));
	}

	@Test
	public void testEventTransitive() throws BundleException, InvalidSyntaxException {
		region(REGION_A).connectRegion(region(REGION_B), createFilter(BUNDLE_C));
		region(REGION_B).connectRegion(region(REGION_C), createFilter(BUNDLE_C));
		region(REGION_C).addBundle(bundle(BUNDLE_X));

		Collection<BundleContext> contexts = bundleContexts(BUNDLE_A, BUNDLE_B, BUNDLE_X);
		this.serviceEventHook.event(serviceEvent(BUNDLE_C), contexts);
		assertTrue(contexts.contains(bundleContext(BUNDLE_B)));
		assertTrue(contexts.contains(bundleContext(BUNDLE_X)));
		assertTrue(contexts.contains(bundleContext(BUNDLE_A)));

		contexts = bundleContexts(BUNDLE_A, BUNDLE_B, BUNDLE_X);
		this.serviceEventHook.event(serviceEvent(BUNDLE_X), contexts);
		assertFalse(contexts.contains(bundleContext(BUNDLE_B)));
		assertTrue(contexts.contains(bundleContext(BUNDLE_X)));
		assertFalse(contexts.contains(bundleContext(BUNDLE_A)));
	}

	@Test
	public void testEventTransitiveDups() throws BundleException, InvalidSyntaxException {
		region(REGION_A).connectRegion(region(REGION_B), createFilter(BUNDLE_C));
		region(REGION_A).connectRegion(region(REGION_C), createFilter(DUPLICATE_FIlTER));
		region(REGION_A).connectRegion(region(REGION_D), createFilter(DUPLICATE_FIlTER));
		region(REGION_B).connectRegion(region(REGION_C), createFilter(DUPLICATE_FIlTER));
		region(REGION_C).connectRegion(region(REGION_D), createFilter(DUPLICATE_FIlTER));
		region(REGION_D).connectRegion(region(REGION_A), createFilter(DUPLICATE_FIlTER));

		Collection<BundleContext> contexts = bundleContexts(BUNDLE_A);
		this.serviceEventHook.event(serviceEvent(DUPLICATE + bundle(BUNDLE_A).getBundleId()), contexts);
		assertTrue(contexts.contains(bundleContext(BUNDLE_A)));

		contexts = bundleContexts(BUNDLE_A);
		this.serviceEventHook.event(serviceEvent(DUPLICATE + bundle(BUNDLE_B).getBundleId()), contexts);
		assertFalse(contexts.contains(bundleContext(BUNDLE_A)));

		contexts = bundleContexts(BUNDLE_A);
		this.serviceEventHook.event(serviceEvent(DUPLICATE + bundle(BUNDLE_C).getBundleId()), contexts);
		assertTrue(contexts.contains(bundleContext(BUNDLE_A)));

		contexts = bundleContexts(BUNDLE_A);
		this.serviceEventHook.event(serviceEvent(DUPLICATE + bundle(BUNDLE_D).getBundleId()), contexts);
		assertTrue(contexts.contains(bundleContext(BUNDLE_A)));
	}

	@Test
	public void testEventInCyclicGraph() throws BundleException, InvalidSyntaxException {
		region(REGION_D).addBundle(bundle(BUNDLE_X));

		region(REGION_A).connectRegion(region(REGION_B), createFilter(BUNDLE_D, BUNDLE_X));
		region(REGION_B).connectRegion(region(REGION_A), createFilter());

		region(REGION_B).connectRegion(region(REGION_D), createFilter(BUNDLE_D));
		region(REGION_D).connectRegion(region(REGION_B), createFilter());

		region(REGION_B).connectRegion(region(REGION_C), createFilter(BUNDLE_X));
		region(REGION_C).connectRegion(region(REGION_B), createFilter());

		region(REGION_C).connectRegion(region(REGION_D), createFilter(BUNDLE_X));
		region(REGION_D).connectRegion(region(REGION_C), createFilter());

		region(REGION_A).connectRegion(region(REGION_C), createFilter());
		region(REGION_C).connectRegion(region(REGION_A), createFilter());

		region(REGION_D).connectRegion(region(REGION_A), createFilter());
		region(REGION_A).connectRegion(region(REGION_D), createFilter());

		Collection<BundleContext> contexts = bundleContexts(BUNDLE_A, BUNDLE_B);
		this.serviceEventHook.event(serviceEvent(BUNDLE_B), contexts);
		assertFalse(contexts.contains(bundleContext(BUNDLE_A)));
		assertTrue(contexts.contains(bundleContext(BUNDLE_B)));

		contexts = bundleContexts(BUNDLE_A, BUNDLE_B);
		this.serviceEventHook.event(serviceEvent(BUNDLE_C), contexts);
		assertFalse(contexts.contains(bundleContext(BUNDLE_A)));
		assertFalse(contexts.contains(bundleContext(BUNDLE_B)));

		contexts = bundleContexts(BUNDLE_A, BUNDLE_B);
		this.serviceEventHook.event(serviceEvent(BUNDLE_D), contexts);
		assertTrue(contexts.contains(bundleContext(BUNDLE_A)));
		assertTrue(contexts.contains(bundleContext(BUNDLE_B)));

		contexts = bundleContexts(BUNDLE_A, BUNDLE_B);
		this.serviceEventHook.event(serviceEvent(BUNDLE_X), contexts);
		assertTrue(contexts.contains(bundleContext(BUNDLE_A)));
		assertTrue(contexts.contains(bundleContext(BUNDLE_B)));
	}

	@Test
	public void testEventFromSystemBundle() {
		Bundle systemBundle = new StubBundle(0L, "sys", BUNDLE_VERSION, "");
		Collection<BundleContext> contexts = new ArrayList<>(Arrays.asList(systemBundle.getBundleContext()));
		this.serviceEventHook.event(serviceEvent(BUNDLE_A), contexts);
		assertTrue(contexts.contains(systemBundle.getBundleContext()));
	}

	@Test
	public void testEventFromBundleInNoRegion() {
		Bundle stranger = createBundle("stranger");
		Collection<BundleContext> contexts = new ArrayList<>(Arrays.asList(stranger.getBundleContext()));
		this.serviceEventHook.event(serviceEvent(BUNDLE_A), contexts);
		assertTrue(contexts.isEmpty());
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

	private RegionFilter createFilter(final String... referenceNames) throws InvalidSyntaxException {
		Collection<String> filters = new ArrayList<>(referenceNames.length);
		for (String referenceName : referenceNames) {
			filters.add('(' + Constants.OBJECTCLASS + '=' + referenceName + ')');
		}
		RegionFilterBuilder builder = digraph.createRegionFilterBuilder();
		for (String filter : filters) {
			builder.allow(RegionFilter.VISIBLE_SERVICE_NAMESPACE, filter);
		}
		String negateFilter = "(!(|" + "(" + RegionFilter.VISIBLE_ALL_NAMESPACE_ATTRIBUTE + "=" + RegionFilter.VISIBLE_SERVICE_NAMESPACE + ")" + "(" + RegionFilter.VISIBLE_ALL_NAMESPACE_ATTRIBUTE + "=" + RegionFilter.VISIBLE_BUNDLE_NAMESPACE + ")" + "))";
		builder.allow(RegionFilter.VISIBLE_ALL_NAMESPACE, negateFilter);
		return builder.build();
	}

	private Bundle createBundle(String bundleSymbolicName) {
		Bundle stubBundle = new StubBundle(this.bundleId++, bundleSymbolicName, BUNDLE_VERSION, "loc:" + bundleSymbolicName);
		this.bundles.put(bundleSymbolicName, stubBundle);
		createServiceReference(stubBundle, bundleSymbolicName);
		return stubBundle;
	}

	private StubServiceReference<Object> createServiceReference(Bundle stubBundle, String referenceName) {
		StubServiceRegistration<Object> stubServiceRegistration = new StubServiceRegistration<>((StubBundleContext) stubBundle.getBundleContext(), referenceName);
		StubServiceReference<Object> stubServiceReference = new StubServiceReference<>(stubServiceRegistration);
		this.serviceReferences.put(referenceName, stubServiceReference);

		StubServiceRegistration<Object> dupServiceRegistration = new StubServiceRegistration<>((StubBundleContext) stubBundle.getBundleContext(), DUPLICATE + stubBundle.getBundleId());
		StubServiceReference<Object> dupServiceReference = new StubServiceReference<>(dupServiceRegistration);
		this.serviceReferences.put(DUPLICATE + stubBundle.getBundleId(), dupServiceReference);
		return stubServiceReference;
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

	private Bundle bundle(String bundleSymbolicName) {
		Bundle bundleA = this.bundles.get(bundleSymbolicName);
		return bundleA;
	}

	private ServiceEvent serviceEvent(String referenceName) {
		return new ServiceEvent(ServiceEvent.REGISTERED, serviceReference(referenceName));
	}

	private ServiceReference<Object> serviceReference(String referenceName) {
		return this.serviceReferences.get(referenceName);
	}

}
