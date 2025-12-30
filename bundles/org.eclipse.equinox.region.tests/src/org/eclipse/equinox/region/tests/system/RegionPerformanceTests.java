/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.region.tests.system;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import org.eclipse.core.tests.harness.PerformanceTestRunner;
import org.eclipse.equinox.region.*;
import org.eclipse.equinox.region.tests.BundleInstaller;
import org.junit.jupiter.api.*;
import org.osgi.framework.*;

public class RegionPerformanceTests {
	Bundle testBundle;
	private ServiceReference<RegionDigraph> digraphReference;
	private RegionDigraph digraph;
	private BundleInstaller bundleInstaller;
	private Bundle testsBundle;
	private BundleContext context;

	private TestInfo testInfo;

	@BeforeEach
	void setUp(TestInfo testInfo) throws Exception {
		testsBundle = FrameworkUtil.getBundle(this.getClass());

		context = testsBundle.getBundleContext();

		digraphReference = context.getServiceReference(RegionDigraph.class);
		assertNotNull(digraphReference, "No digraph found");
		digraph = context.getService(digraphReference);
		assertNotNull(digraph, "No digraph found");

		bundleInstaller = new BundleInstaller("bundle_tests", testsBundle); //$NON-NLS-1$
		testBundle = bundleInstaller.installBundle(AbstractRegionSystemTest.PP1);
		testBundle.start();

		this.testInfo = testInfo;
	}

	@AfterEach
	void tearDown() throws Exception {
		for (Region region : digraph) {
			if (!region.contains(0)) {
				digraph.removeRegion(region);
			}
		}
		bundleInstaller.shutdown();
		if (digraphReference != null)
			context.ungetService(digraphReference);
	}

	private void doTestGetBundles() throws Exception {
		final BundleContext bundleContext = testBundle.getBundleContext();
		PerformanceTestRunner runner = new PerformanceTestRunner() {
			protected void test() {
				Bundle[] bundles = bundleContext.getBundles();
				for (Bundle bundle : bundles) {
					bundleContext.getBundle(bundle.getBundleId());
				}
			}
		};
		runner.run(getClass(), testInfo.getDisplayName(), 10, 300);
	}

	@Test
	public void testGetBundlesNoRegions() throws Exception {
		doTestGetBundles();
	}

	@Test
	public void testGetBundles10Regions() throws Exception {
		createRegions(10);
		doTestGetBundles();
	}

	@Test
	public void testGetBundles100Regions() throws Exception {
		createRegions(100);
		doTestGetBundles();
	}

	@Test
	public void testGetBundles1000Regions() throws Exception {
		createRegions(1000);
		doTestGetBundles();
	}

	@Test
	public void testGetServicesNoRegions() throws Exception {
		doTestGetServices();
	}

	@Test
	public void testGetServices10Regions() throws Exception {
		createRegions(10);
		doTestGetServices();
	}

	@Test
	public void testGetServices100Regions() throws Exception {
		createRegions(100);
		doTestGetServices();
	}

	@Test
	public void testGetServices1000Regions() throws Exception {
		createRegions(1000);
		doTestGetServices();
	}

	@Test
	public void testGetRegionByNameNoRegions() throws Exception {
		doTestGetRegionByName();
	}

	@Test
	public void testGetRegionByName10Regions() throws Exception {
		createRegions(10);
		doTestGetRegionByName();
	}

	@Test
	public void testGetRegionByName100Regions() throws Exception {
		createRegions(100);
		doTestGetRegionByName();
	}

	@Test
	public void testGetRegionByName1000Regions() throws Exception {
		createRegions(1000);
		doTestGetRegionByName();
	}

	private void doTestGetServices() throws Exception {
		final BundleContext bundleContext = testBundle.getBundleContext();
		PerformanceTestRunner runner = new PerformanceTestRunner() {
			protected void test() {
				try {
					bundleContext.getServiceReferences(RegionDigraph.class, null);
				} catch (InvalidSyntaxException e) {
					fail(e.getMessage());
				}
			}
		};
		runner.run(getClass(), testInfo.getDisplayName(), 10, 2000);
	}

	private void doTestGetRegionByName() throws Exception {
		final RegionDigraph current = digraph;
		final Region[] regions = current.getRegions().toArray(new Region[0]);
		PerformanceTestRunner runner = new PerformanceTestRunner() {
			protected void test() {
				for (Region region : regions) {
					current.getRegion(region.getName());
				}
			}
		};
		runner.run(getClass(), testInfo.getDisplayName(), 10, 2000);
	}

	@SuppressWarnings("deprecation") // VISIBLE_SERVICE_NAMESPACE
	private void createRegions(final int numRegions) throws BundleException {
		System.out.println("Starting region create: " + numRegions);
		long time = System.currentTimeMillis();
		Region system = digraph.getRegion(0);
		RegionFilterBuilder builder = digraph.createRegionFilterBuilder();
		builder.allowAll(RegionFilter.VISIBLE_BUNDLE_NAMESPACE);
		builder.allowAll(RegionFilter.VISIBLE_HOST_NAMESPACE);
		builder.allowAll(RegionFilter.VISIBLE_PACKAGE_NAMESPACE);
		builder.allowAll(RegionFilter.VISIBLE_REQUIRE_NAMESPACE);
		builder.allowAll(RegionFilter.VISIBLE_SERVICE_NAMESPACE);
		RegionFilter filter = builder.build();
		for (int i = 0; i < numRegions; i++) {
			Region r = digraph.createRegion(testInfo.getDisplayName() + i);
			digraph.connect(system, filter, r);
		}
		System.out.println("Done creating region: " + (System.currentTimeMillis() - time));
	}
}
