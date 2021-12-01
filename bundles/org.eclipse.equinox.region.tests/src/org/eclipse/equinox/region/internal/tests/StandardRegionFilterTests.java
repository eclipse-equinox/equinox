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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.*;
import org.eclipse.equinox.region.RegionFilter;
import org.eclipse.equinox.region.RegionFilterBuilder;
import org.eclipse.virgo.teststubs.osgi.framework.*;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.*;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;

public class StandardRegionFilterTests {

	private static final String BUNDLE_SYMBOLIC_NAME = "A";

	private static final Version BUNDLE_VERSION = new Version("0");

	private StubBundle stubBundle;

	private String packageImportPolicy = "(" + BundleRevision.PACKAGE_NAMESPACE + "=foo)";

	private String serviceImportPolicy = "(" + Constants.OBJECTCLASS + "=foo.Service)";

	private BundleCapability fooPackage;

	private BundleCapability barPackage;

	private BundleCapability fooServiceCapability;
	private ServiceRegistration<Object> fooService;

	private BundleCapability barServiceCapability;
	private ServiceRegistration<Object> barService;

	@Before
	public void setUp() throws Exception {
		this.stubBundle = new StubBundle(BUNDLE_SYMBOLIC_NAME, BUNDLE_VERSION);
		this.fooService = new StubServiceRegistration<>(new StubBundleContext(), "foo.Service");
		this.barService = new StubServiceRegistration<>(new StubBundleContext(), "bar.Service");

		this.fooPackage = mock(BundleCapability.class);
		Map<String, Object> fooAttrs = new HashMap<>();
		fooAttrs.put(BundleRevision.PACKAGE_NAMESPACE, "foo");
		when(fooPackage.getNamespace()).thenReturn(BundleRevision.PACKAGE_NAMESPACE);
		when(fooPackage.getAttributes()).thenReturn(fooAttrs);

		this.fooServiceCapability = mock(BundleCapability.class);
		Map<String, Object> fooServiceAttrs = new HashMap<>();
		fooServiceAttrs.put(Constants.OBJECTCLASS, "foo.Service");
		when(fooServiceCapability.getNamespace()).thenReturn(RegionFilter.VISIBLE_OSGI_SERVICE_NAMESPACE);
		when(fooServiceCapability.getAttributes()).thenReturn(fooServiceAttrs);

		this.barPackage = mock(BundleCapability.class);
		Map<String, Object> barAttrs = new HashMap<>();
		barAttrs.put(BundleRevision.PACKAGE_NAMESPACE, "bar");
		when(barPackage.getNamespace()).thenReturn(BundleRevision.PACKAGE_NAMESPACE);
		when(barPackage.getAttributes()).thenReturn(barAttrs);

		this.barServiceCapability = mock(BundleCapability.class);
		Map<String, Object> barServiceAttrs = new HashMap<>();
		barServiceAttrs.put(Constants.OBJECTCLASS, "bar.Service");
		when(barServiceCapability.getNamespace()).thenReturn(RegionFilter.VISIBLE_OSGI_SERVICE_NAMESPACE);
		when(barServiceCapability.getAttributes()).thenReturn(barServiceAttrs);
	}

	private RegionFilter createBundleFilter(String bundleSymbolicName, Version bundleVersion)
			throws InvalidSyntaxException {
		String filter = "(&(" + RegionFilter.VISIBLE_BUNDLE_NAMESPACE + "=" + bundleSymbolicName + ")("
				+ Constants.BUNDLE_VERSION_ATTRIBUTE + ">=" + bundleVersion + "))";
		return RegionReflectionUtils.newStandardRegionFilterBuilder()
				.allow(RegionFilter.VISIBLE_BUNDLE_NAMESPACE, filter).build();
	}

	private RegionFilter createRegionFilter(String namespace, Collection<String> filters)
			throws InvalidSyntaxException {
		RegionFilterBuilder builder = RegionReflectionUtils.newStandardRegionFilterBuilder();
		for (String filter : filters) {
			builder.allow(namespace, filter);
		}
		return builder.build();
	}

	@Test
	public void testBundleAllow() throws InvalidSyntaxException {
		RegionFilter regionFilter = createBundleFilter(BUNDLE_SYMBOLIC_NAME, BUNDLE_VERSION);
		assertTrue(regionFilter.isAllowed(stubBundle));
	}

	@Test
	public void testBundleAllNotAllowed() {
		RegionFilter regionFilter = RegionReflectionUtils.newStandardRegionFilterBuilder().build();
		assertFalse(regionFilter.isAllowed(stubBundle));
	}

	@Test
	public void testBundleAllAllowed() {
		RegionFilter regionFilter = RegionReflectionUtils.newStandardRegionFilterBuilder()
				.allowAll(RegionFilter.VISIBLE_BUNDLE_NAMESPACE).build();
		assertTrue(regionFilter.isAllowed(stubBundle));
	}

	@Test
	public void testBundleNotAllowedInRange() throws InvalidSyntaxException {
		RegionFilter regionFilter = createBundleFilter(BUNDLE_SYMBOLIC_NAME, new Version(1, 0, 0));
		assertFalse(regionFilter.isAllowed(stubBundle));
	}

	@Test
	public void testCapabilityAllowed() throws InvalidSyntaxException {
		RegionFilter regionFilter = createRegionFilter(RegionFilter.VISIBLE_PACKAGE_NAMESPACE,
				Arrays.asList(packageImportPolicy));
		assertTrue(regionFilter.isAllowed(fooPackage));
		assertEquals(Arrays.asList(this.packageImportPolicy),
				regionFilter.getSharingPolicy().get(RegionFilter.VISIBLE_PACKAGE_NAMESPACE));
	}

	@Test
	public void testCapabilityAllNotAllowed() {
		RegionFilter regionFilter = RegionReflectionUtils.newStandardRegionFilterBuilder().build();
		assertFalse(regionFilter.isAllowed(barPackage));
	}

	@Test
	public void testCapabilityAllAllowed() {
		RegionFilter regionFilter = RegionReflectionUtils.newStandardRegionFilterBuilder()
				.allowAll(RegionFilter.VISIBLE_PACKAGE_NAMESPACE).build();
		assertTrue(regionFilter.isAllowed(barPackage));
	}

	@Test
	public void testCapabilityNotAllowed() throws InvalidSyntaxException {
		RegionFilter regionFilter = createRegionFilter(RegionFilter.VISIBLE_PACKAGE_NAMESPACE,
				Arrays.asList(packageImportPolicy));
		assertFalse(regionFilter.isAllowed(barPackage));
		assertEquals(Arrays.asList(this.packageImportPolicy),
				regionFilter.getSharingPolicy().get(RegionFilter.VISIBLE_PACKAGE_NAMESPACE));
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testServiceAllowed() throws InvalidSyntaxException {
		RegionFilter regionFilter = createRegionFilter(RegionFilter.VISIBLE_SERVICE_NAMESPACE,
				Arrays.asList(serviceImportPolicy));
		assertTrue(regionFilter.isAllowed(fooService.getReference()));
		assertTrue(regionFilter.isAllowed(fooServiceCapability));
		assertEquals(Arrays.asList(serviceImportPolicy),
				regionFilter.getSharingPolicy().get(RegionFilter.VISIBLE_SERVICE_NAMESPACE));

		regionFilter = createRegionFilter(RegionFilter.VISIBLE_OSGI_SERVICE_NAMESPACE,
				Arrays.asList(serviceImportPolicy));
		assertTrue(regionFilter.isAllowed(fooService.getReference()));
		assertTrue(regionFilter.isAllowed(fooServiceCapability));
		assertNull(regionFilter.getSharingPolicy().get(RegionFilter.VISIBLE_SERVICE_NAMESPACE));
		assertEquals(Arrays.asList(serviceImportPolicy),
				regionFilter.getSharingPolicy().get(RegionFilter.VISIBLE_OSGI_SERVICE_NAMESPACE));
	}

	@Test
	public void testServiceAllNotAllowed() {
		RegionFilter regionFilter = RegionReflectionUtils.newStandardRegionFilterBuilder().build();
		assertFalse(regionFilter.isAllowed(fooService.getReference()));
		assertFalse(regionFilter.isAllowed(fooServiceCapability));
	}

	@Test
	public void testServiceAllAllowed() {
		@SuppressWarnings("deprecation")
		RegionFilter regionFilter = RegionReflectionUtils.newStandardRegionFilterBuilder()
				.allowAll(RegionFilter.VISIBLE_SERVICE_NAMESPACE).build();
		assertTrue(regionFilter.isAllowed(fooService.getReference()));
		assertTrue(regionFilter.isAllowed(fooServiceCapability));

		regionFilter = RegionReflectionUtils.newStandardRegionFilterBuilder()
				.allowAll(RegionFilter.VISIBLE_OSGI_SERVICE_NAMESPACE).build();
		assertTrue(regionFilter.isAllowed(fooService.getReference()));
		assertTrue(regionFilter.isAllowed(fooServiceCapability));
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testServiceNotAllowed() throws InvalidSyntaxException {
		RegionFilter regionFilter = createRegionFilter(RegionFilter.VISIBLE_SERVICE_NAMESPACE,
				Arrays.asList(serviceImportPolicy));
		assertFalse(regionFilter.isAllowed(barService.getReference()));
		assertFalse(regionFilter.isAllowed(barServiceCapability));
		assertEquals(Arrays.asList(serviceImportPolicy),
				regionFilter.getSharingPolicy().get(RegionFilter.VISIBLE_SERVICE_NAMESPACE));
		assertEquals(Arrays.asList(serviceImportPolicy),
				regionFilter.getSharingPolicy().get(RegionFilter.VISIBLE_OSGI_SERVICE_NAMESPACE));

		regionFilter = createRegionFilter(RegionFilter.VISIBLE_OSGI_SERVICE_NAMESPACE,
				Arrays.asList(serviceImportPolicy));
		assertFalse(regionFilter.isAllowed(barService.getReference()));
		assertFalse(regionFilter.isAllowed(barServiceCapability));
		assertNull(regionFilter.getSharingPolicy().get(RegionFilter.VISIBLE_SERVICE_NAMESPACE));
		assertEquals(Arrays.asList(serviceImportPolicy),
				regionFilter.getSharingPolicy().get(RegionFilter.VISIBLE_OSGI_SERVICE_NAMESPACE));
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testAllNamespaceForService() throws InvalidSyntaxException {
		RegionFilter negateNonServices = RegionReflectionUtils.newStandardRegionFilterBuilder().allow(
				RegionFilter.VISIBLE_ALL_NAMESPACE,
				"(" + RegionFilter.VISIBLE_ALL_NAMESPACE_ATTRIBUTE + "=" + RegionFilter.VISIBLE_SERVICE_NAMESPACE + ")")
				.build();
		assertFalse(negateNonServices.isAllowed(stubBundle));
		assertFalse(negateNonServices.isAllowed(fooPackage));
		assertFalse(negateNonServices.isAllowed(barPackage));
		assertTrue(negateNonServices.isAllowed(fooService.getReference()));
		assertTrue(negateNonServices.isAllowed(fooServiceCapability));
		assertTrue(negateNonServices.isAllowed(barService.getReference()));
		assertTrue(negateNonServices.isAllowed(barServiceCapability));

		negateNonServices = RegionReflectionUtils.newStandardRegionFilterBuilder()
				.allow(RegionFilter.VISIBLE_ALL_NAMESPACE, "(" + RegionFilter.VISIBLE_ALL_NAMESPACE_ATTRIBUTE + "="
						+ RegionFilter.VISIBLE_OSGI_SERVICE_NAMESPACE + ")")
				.build();
		assertFalse(negateNonServices.isAllowed(stubBundle));
		assertFalse(negateNonServices.isAllowed(fooPackage));
		assertFalse(negateNonServices.isAllowed(barPackage));
		assertTrue(negateNonServices.isAllowed(fooService.getReference()));
		assertTrue(negateNonServices.isAllowed(fooServiceCapability));
		assertTrue(negateNonServices.isAllowed(barService.getReference()));
		assertTrue(negateNonServices.isAllowed(barServiceCapability));
	}

	@Test
	public void testAllNamespace() throws InvalidSyntaxException {
		RegionFilter regionFilterNotAllowed = RegionReflectionUtils.newStandardRegionFilterBuilder()
				.allow(RegionFilter.VISIBLE_ALL_NAMESPACE, "(all=namespace)").build();
		assertFalse(regionFilterNotAllowed.isAllowed(stubBundle));
		assertFalse(regionFilterNotAllowed.isAllowed(fooPackage));
		assertFalse(regionFilterNotAllowed.isAllowed(barPackage));
		assertFalse(regionFilterNotAllowed.isAllowed(fooService.getReference()));
		assertFalse(regionFilterNotAllowed.isAllowed(fooServiceCapability));
		assertFalse(regionFilterNotAllowed.isAllowed(barService.getReference()));
		assertFalse(regionFilterNotAllowed.isAllowed(barServiceCapability));

		RegionFilter regionFilterAllAllowed = RegionReflectionUtils.newStandardRegionFilterBuilder()
				.allowAll(RegionFilter.VISIBLE_ALL_NAMESPACE).build();
		assertTrue(regionFilterAllAllowed.isAllowed(stubBundle));
		assertTrue(regionFilterAllAllowed.isAllowed(fooPackage));
		assertTrue(regionFilterAllAllowed.isAllowed(barPackage));
		assertFalse(regionFilterNotAllowed.isAllowed(fooService.getReference()));
		assertFalse(regionFilterNotAllowed.isAllowed(fooServiceCapability));
		assertFalse(regionFilterNotAllowed.isAllowed(barService.getReference()));
		assertFalse(regionFilterNotAllowed.isAllowed(barServiceCapability));
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testNegativeAllNamespace() throws InvalidSyntaxException {
		RegionFilter negateServices = RegionReflectionUtils.newStandardRegionFilterBuilder()
				.allow(RegionFilter.VISIBLE_ALL_NAMESPACE, "(!(" + RegionFilter.VISIBLE_ALL_NAMESPACE_ATTRIBUTE + "="
						+ RegionFilter.VISIBLE_SERVICE_NAMESPACE + "))")
				.build();
		assertTrue(negateServices.isAllowed(stubBundle));
		assertTrue(negateServices.isAllowed(fooPackage));
		assertTrue(negateServices.isAllowed(barPackage));
		assertFalse(negateServices.isAllowed(fooService.getReference()));
		assertFalse(negateServices.isAllowed(fooServiceCapability));
		assertFalse(negateServices.isAllowed(barService.getReference()));
		assertFalse(negateServices.isAllowed(barServiceCapability));

		negateServices = RegionReflectionUtils.newStandardRegionFilterBuilder()
				.allow(RegionFilter.VISIBLE_ALL_NAMESPACE, "(!(" + RegionFilter.VISIBLE_ALL_NAMESPACE_ATTRIBUTE + "="
						+ RegionFilter.VISIBLE_OSGI_SERVICE_NAMESPACE + "))")
				.build();
		assertTrue(negateServices.isAllowed(stubBundle));
		assertTrue(negateServices.isAllowed(fooPackage));
		assertTrue(negateServices.isAllowed(barPackage));
		assertFalse(negateServices.isAllowed(fooService.getReference()));
		assertFalse(negateServices.isAllowed(fooServiceCapability));
		assertFalse(negateServices.isAllowed(barService.getReference()));
		assertFalse(negateServices.isAllowed(barServiceCapability));
	}
}
