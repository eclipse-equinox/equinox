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

import junit.framework.TestCase;
import org.eclipse.core.tests.harness.PerformanceTestRunner;
import org.eclipse.equinox.region.*;
import org.eclipse.equinox.region.tests.BundleInstaller;
import org.osgi.framework.*;

public class RegionPerformanceTests extends TestCase {
	Bundle testBundle;
	private ServiceReference<RegionDigraph> digraphReference;
	private RegionDigraph digraph;
	private BundleInstaller bundleInstaller;
	private Bundle testsBundle;
	private BundleContext context;

	@Override
	protected void setUp() throws Exception {
		testsBundle = FrameworkUtil.getBundle(this.getClass());

		context = testsBundle.getBundleContext();

		digraphReference = context.getServiceReference(RegionDigraph.class);
		assertNotNull("No digraph found", digraphReference);
		digraph = context.getService(digraphReference);
		assertNotNull("No digraph found", digraph);

		bundleInstaller = new BundleInstaller("bundle_tests", testsBundle); //$NON-NLS-1$
		try {
			testBundle = bundleInstaller.installBundle(AbstractRegionSystemTest.PP1);
		} catch (BundleException e) {
			// TODO should actually be cleaned up!
		}
		testBundle.start();

	}

	@Override
	protected void tearDown() throws Exception {
		for (Region region : digraph) {
			if (!region.contains(0)) {
				digraph.removeRegion(region);
			}
		}
		bundleInstaller.shutdown();
		if (digraphReference != null)
			context.ungetService(digraphReference);
	}

	private void doTestGetBundles(String fingerPrintName, String degradation) {
		final BundleContext bundleContext = testBundle.getBundleContext();
		PerformanceTestRunner runner = new PerformanceTestRunner() {
			protected void test() {
				Bundle[] bundles = bundleContext.getBundles();
				for (Bundle bundle : bundles) {
					bundleContext.getBundle(bundle.getBundleId());
				}
			}
		};
		runner.setRegressionReason(degradation);
		runner.run(this, fingerPrintName, 10, 300);
	}

	public void testGetBundlesNoRegions() {
		doTestGetBundles(null, null);
	}

	public void testGetBundles10Regions() throws BundleException {
		createRegions(10);
		doTestGetBundles(null, null);
	}

	public void testGetBundles100Regions() throws BundleException {
		createRegions(100);
		doTestGetBundles(null, null);
	}

	public void testGetBundles1000Regions() throws BundleException {
		createRegions(1000);
		doTestGetBundles(null, null);
	}

	public void testGetServicesNoRegions() {
		doTestGetServices(null, null);
	}

	public void testGetServices10Regions() throws BundleException {
		createRegions(10);
		doTestGetServices(null, null);
	}

	public void testGetServices100Regions() throws BundleException {
		createRegions(100);
		doTestGetServices(null, null);
	}

	public void testGetServices1000Regions() throws BundleException {
		createRegions(1000);
		doTestGetServices(null, null);
	}

	public void testGetRegionByNameNoRegions() {
		doTestGetRegionByName(null, null);
	}

	public void testGetRegionByName10Regions() throws BundleException {
		createRegions(10);
		doTestGetRegionByName(null, null);
	}

	public void testGetRegionByName100Regions() throws BundleException {
		createRegions(100);
		doTestGetRegionByName(null, null);
	}

	public void testGetRegionByName1000Regions() throws BundleException {
		createRegions(1000);
		doTestGetRegionByName(null, null);
	}

	private void doTestGetServices(String fingerPrintName, String degradation) {
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
		runner.setRegressionReason(degradation);
		runner.run(this, fingerPrintName, 10, 2000);
	}

	private void doTestGetRegionByName(String fingerPrintName, String degradation) {
		final RegionDigraph current = digraph;
		final Region[] regions = current.getRegions().toArray(new Region[0]);
		PerformanceTestRunner runner = new PerformanceTestRunner() {
			protected void test() {
				for (Region region : regions) {
					current.getRegion(region.getName());
				}
			}
		};
		runner.setRegressionReason(degradation);
		runner.run(this, fingerPrintName, 10, 2000);
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
			Region r = digraph.createRegion(getName() + i);
			digraph.connect(system, filter, r);
		}
		System.out.println("Done creating region: " + (System.currentTimeMillis() - time));
	}
}
