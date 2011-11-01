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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.easymock.EasyMock;
import org.eclipse.equinox.region.Region;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleException;

public class StandardBundleIdToRegionMappingTests {

	private BundleIdToRegionMapping bundleIdToRegionMapping;

	private Region mockRegion;

	private static final long TEST_BUNDLE_ID = 1L;

	private static final long OTHER_TEST_BUNDLE_ID = 2L;

	@Before
	public void setUp() throws Exception {
		this.bundleIdToRegionMapping = new StandardBundleIdToRegionMapping();
		this.mockRegion = EasyMock.createMock(Region.class);
	}

	@Test
	public void testAssociateBundleWithRegion() throws BundleException {
		assertNull(this.bundleIdToRegionMapping.getRegion(TEST_BUNDLE_ID));
		this.bundleIdToRegionMapping.associateBundleWithRegion(TEST_BUNDLE_ID, mockRegion);
		assertEquals(mockRegion, this.bundleIdToRegionMapping.getRegion(TEST_BUNDLE_ID));
	}

	@Test
	public void testAssociateBundleAlreadyAssociatedWithRegion() throws BundleException {
		this.bundleIdToRegionMapping.associateBundleWithRegion(TEST_BUNDLE_ID, mockRegion);
		this.bundleIdToRegionMapping.associateBundleWithRegion(TEST_BUNDLE_ID, mockRegion);
	}

	@Test(expected = BundleException.class)
	public void testAssociateBundleAlreadyAssociatedWithOtherRegion() throws BundleException {
		this.bundleIdToRegionMapping.associateBundleWithRegion(TEST_BUNDLE_ID, mockRegion);
		this.bundleIdToRegionMapping.associateBundleWithRegion(TEST_BUNDLE_ID, EasyMock.createMock(Region.class));
	}

	@Test
	public void testDissociateBundle() throws BundleException {
		this.bundleIdToRegionMapping.associateBundleWithRegion(TEST_BUNDLE_ID, mockRegion);
		this.bundleIdToRegionMapping.dissociateBundle(TEST_BUNDLE_ID);
		assertNull(this.bundleIdToRegionMapping.getRegion(TEST_BUNDLE_ID));
	}

	@Test
	public void testIsBundleAssociatedWithRegion() throws BundleException {
		assertFalse(this.bundleIdToRegionMapping.isBundleAssociatedWithRegion(TEST_BUNDLE_ID, mockRegion));
		this.bundleIdToRegionMapping.associateBundleWithRegion(TEST_BUNDLE_ID, mockRegion);
		assertTrue(this.bundleIdToRegionMapping.isBundleAssociatedWithRegion(TEST_BUNDLE_ID, mockRegion));
	}

	@Test
	public void testGetBundleIds() throws BundleException {
		assertEquals(0, this.bundleIdToRegionMapping.getBundleIds(mockRegion).size());
		this.bundleIdToRegionMapping.associateBundleWithRegion(TEST_BUNDLE_ID, mockRegion);
		this.bundleIdToRegionMapping.associateBundleWithRegion(OTHER_TEST_BUNDLE_ID, mockRegion);
		assertEquals(2, this.bundleIdToRegionMapping.getBundleIds(mockRegion).size());
		assertTrue(this.bundleIdToRegionMapping.getBundleIds(mockRegion).contains(TEST_BUNDLE_ID));
		assertTrue(this.bundleIdToRegionMapping.getBundleIds(mockRegion).contains(OTHER_TEST_BUNDLE_ID));
	}

	@Test
	public void testClear() throws BundleException {
		this.bundleIdToRegionMapping.associateBundleWithRegion(TEST_BUNDLE_ID, mockRegion);
		this.bundleIdToRegionMapping.clear();
		assertNull(this.bundleIdToRegionMapping.getRegion(TEST_BUNDLE_ID));
	}

	@Test
	public void testGetRegion() throws BundleException {
		assertNull(this.bundleIdToRegionMapping.getRegion(TEST_BUNDLE_ID));
		this.bundleIdToRegionMapping.associateBundleWithRegion(TEST_BUNDLE_ID, mockRegion);
		this.bundleIdToRegionMapping.associateBundleWithRegion(OTHER_TEST_BUNDLE_ID, mockRegion);
		assertEquals(mockRegion, this.bundleIdToRegionMapping.getRegion(TEST_BUNDLE_ID));
		assertEquals(mockRegion, this.bundleIdToRegionMapping.getRegion(OTHER_TEST_BUNDLE_ID));
	}

}
