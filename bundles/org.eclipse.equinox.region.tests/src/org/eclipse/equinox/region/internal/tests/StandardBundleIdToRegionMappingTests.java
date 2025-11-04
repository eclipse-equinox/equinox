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

package org.eclipse.equinox.region.internal.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.eclipse.equinox.region.Region;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.BundleException;

public class StandardBundleIdToRegionMappingTests {

	private Object bundleIdToRegionMapping;

	private Region mockRegion;

	private static final long TEST_BUNDLE_ID = 1L;

	private static final long OTHER_TEST_BUNDLE_ID = 2L;

	@BeforeEach
	public void setUp() throws Exception {
		this.bundleIdToRegionMapping = RegionReflectionUtils.newStandardBundleIdToRegionMapping();
		this.mockRegion = mock(Region.class);
	}

	@Test
	public void testAssociateBundleWithRegion() {
		assertNull(RegionReflectionUtils.getRegion(this.bundleIdToRegionMapping, TEST_BUNDLE_ID));
		RegionReflectionUtils.associateBundleWithRegion(this.bundleIdToRegionMapping, TEST_BUNDLE_ID, mockRegion);
		assertEquals(mockRegion, RegionReflectionUtils.getRegion(this.bundleIdToRegionMapping, TEST_BUNDLE_ID));
	}

	@Test
	public void testAssociateBundleAlreadyAssociatedWithRegion() {
		RegionReflectionUtils.associateBundleWithRegion(this.bundleIdToRegionMapping, TEST_BUNDLE_ID, mockRegion);
		RegionReflectionUtils.associateBundleWithRegion(this.bundleIdToRegionMapping, TEST_BUNDLE_ID, mockRegion);
	}

	@Test
	public void testAssociateBundleAlreadyAssociatedWithOtherRegion() {
		RegionReflectionUtils.associateBundleWithRegion(this.bundleIdToRegionMapping, TEST_BUNDLE_ID, mockRegion);
		assertThrows(BundleException.class, () -> {
			RegionReflectionUtils.associateBundleWithRegion(this.bundleIdToRegionMapping, TEST_BUNDLE_ID,
					mock(Region.class));
		});
	}

	@Test
	public void testDissociateBundle() {
		RegionReflectionUtils.associateBundleWithRegion(this.bundleIdToRegionMapping, TEST_BUNDLE_ID, mockRegion);
		RegionReflectionUtils.dissociateBundleFromRegion(this.bundleIdToRegionMapping, TEST_BUNDLE_ID, mockRegion);
		assertNull(RegionReflectionUtils.getRegion(this.bundleIdToRegionMapping, TEST_BUNDLE_ID));
	}

	@Test
	public void testIsBundleAssociatedWithRegion() {
		assertFalse(RegionReflectionUtils.isBundleAssociatedWithRegion(this.bundleIdToRegionMapping, TEST_BUNDLE_ID,
				mockRegion));
		RegionReflectionUtils.associateBundleWithRegion(this.bundleIdToRegionMapping, TEST_BUNDLE_ID, mockRegion);
		assertFalse(RegionReflectionUtils.isBundleAssociatedWithRegion(this.bundleIdToRegionMapping, TEST_BUNDLE_ID,
				mockRegion));
	}

	@Test
	public void testGetBundleIds() {
		assertEquals(0, RegionReflectionUtils.getBundleIds(this.bundleIdToRegionMapping, mockRegion).size());
		RegionReflectionUtils.associateBundleWithRegion(this.bundleIdToRegionMapping, TEST_BUNDLE_ID, mockRegion);
		RegionReflectionUtils.associateBundleWithRegion(this.bundleIdToRegionMapping, OTHER_TEST_BUNDLE_ID, mockRegion);
		assertEquals(2, RegionReflectionUtils.getBundleIds(this.bundleIdToRegionMapping, mockRegion).size());
		assertTrue(
				RegionReflectionUtils.getBundleIds(this.bundleIdToRegionMapping, mockRegion).contains(TEST_BUNDLE_ID));
		assertTrue(RegionReflectionUtils.getBundleIds(this.bundleIdToRegionMapping, mockRegion)
				.contains(OTHER_TEST_BUNDLE_ID));
	}

	@Test
	public void testClear() {
		RegionReflectionUtils.associateBundleWithRegion(this.bundleIdToRegionMapping, TEST_BUNDLE_ID, mockRegion);
		RegionReflectionUtils.clear(this.bundleIdToRegionMapping);
		assertNull(RegionReflectionUtils.getRegion(this.bundleIdToRegionMapping, TEST_BUNDLE_ID));
	}

	@Test
	public void testGetRegion() {
		assertNull(RegionReflectionUtils.getRegion(this.bundleIdToRegionMapping, TEST_BUNDLE_ID));
		RegionReflectionUtils.associateBundleWithRegion(this.bundleIdToRegionMapping, TEST_BUNDLE_ID, mockRegion);
		RegionReflectionUtils.associateBundleWithRegion(this.bundleIdToRegionMapping, OTHER_TEST_BUNDLE_ID, mockRegion);
		assertNull(RegionReflectionUtils.getRegion(this.bundleIdToRegionMapping, TEST_BUNDLE_ID));
		assertNull(RegionReflectionUtils.getRegion(this.bundleIdToRegionMapping, OTHER_TEST_BUNDLE_ID));
	}

}
