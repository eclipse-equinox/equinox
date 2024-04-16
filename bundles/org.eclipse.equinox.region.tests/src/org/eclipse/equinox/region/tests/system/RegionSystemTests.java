/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.management.*;
import org.eclipse.equinox.region.*;
import org.eclipse.equinox.region.RegionDigraph.FilteredRegion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.osgi.framework.*;
import org.osgi.framework.hooks.bundle.CollisionHook;
import org.osgi.framework.hooks.bundle.EventHook;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.wiring.*;
import org.osgi.util.tracker.ServiceTracker;

@SuppressWarnings("deprecation") // RegionFilter.VISIBLE_SERVICE_NAMESPACE
public class RegionSystemTests extends AbstractRegionSystemTest {

	@Rule
	public TestName testName = new TestName();

	private static final long TEST_BUNDLE_ID = 452345245L;

	@Test
	public void testBasic() throws BundleException, InvalidSyntaxException, InterruptedException {
		// get the system region
		Region systemRegion = digraph.getRegion(0);
		// create a disconnected test region
		Region testRegion = digraph.createRegion(testName.getMethodName());
		List<Bundle> bundles = new ArrayList<>();
		// Install all test bundles
		Bundle pp1, cp2, sc1;
		bundles.add(pp1 = bundleInstaller.installBundle(PP1, testRegion));
		// should be able to start pp1 because it depends on nothing
		pp1.start();
		// do a sanity check that we have no services available in the isolated region
		assertNull("Found some services.", pp1.getBundleContext().getAllServiceReferences(null, null));
		assertEquals("Found extra bundles in region", 1, pp1.getBundleContext().getBundles().length);
		pp1.stop();

		bundles.add(bundleInstaller.installBundle(SP1, testRegion));
		bundles.add(bundleInstaller.installBundle(CP1, testRegion));
		bundles.add(bundleInstaller.installBundle(PP2, testRegion));
		bundles.add(bundleInstaller.installBundle(SP2, testRegion));
		bundles.add(cp2 = bundleInstaller.installBundle(CP2, testRegion));
		bundles.add(bundleInstaller.installBundle(PC1, testRegion));
		bundles.add(bundleInstaller.installBundle(BC1, testRegion));
		bundles.add(sc1 = bundleInstaller.installBundle(SC1, testRegion));
		bundles.add(bundleInstaller.installBundle(CC1, testRegion));

		// Import the system bundle from the systemRegion
		digraph.connect(testRegion,
				digraph.createRegionFilterBuilder().allow(RegionFilter.VISIBLE_BUNDLE_NAMESPACE, "(id=0)").build(),
				systemRegion);
		// must import Boolean services into systemRegion to test
		digraph.connect(systemRegion,
				digraph.createRegionFilterBuilder()
						.allow(RegionFilter.VISIBLE_SERVICE_NAMESPACE, "(objectClass=java.lang.Boolean)").build(),
				testRegion);

		bundleInstaller.resolveBundles(bundles.toArray(new Bundle[bundles.size()]));
		for (Bundle bundle : bundles) {
			assertEquals("Bundle did not resolve: " + bundle.getSymbolicName(), Bundle.RESOLVED, bundle.getState());
			bundle.start();
		}
		BundleContext context = getContext();
		ServiceTracker<Boolean, Boolean> cp2Tracker = new ServiceTracker<>(context,
				context.createFilter("(&(objectClass=java.lang.Boolean)(bundle.id=" + cp2.getBundleId() + "))"), null);
		ServiceTracker<Boolean, Boolean> sc1Tracker = new ServiceTracker<>(context,
				context.createFilter("(&(objectClass=java.lang.Boolean)(bundle.id=" + sc1.getBundleId() + "))"), null);

		cp2Tracker.open();
		sc1Tracker.open();

		assertNotNull("The cp2 bundle never found the service.", cp2Tracker.waitForService(2000));
		assertNotNull("The sc1 bundle never found the service.", sc1Tracker.waitForService(2000));
		cp2Tracker.close();
		sc1Tracker.close();
	}

	@Test
	public void testSingleBundleRegions() throws BundleException, InvalidSyntaxException, InterruptedException {
		// get the system region
		Region systemRegion = digraph.getRegion(0);
		Map<String, Bundle> bundles = new HashMap<>();
		// create a disconnected test region for each test bundle
		for (String location : ALL) {
			Region testRegion = digraph.createRegion(location);
			bundles.put(location, bundleInstaller.installBundle(location, testRegion));
			// Import the system bundle from the systemRegion
			digraph.connect(testRegion,
					digraph.createRegionFilterBuilder().allow(RegionFilter.VISIBLE_BUNDLE_NAMESPACE, "(id=0)").build(),
					systemRegion);
			// must import Boolean services into systemRegion to test
			digraph.connect(systemRegion,
					digraph.createRegionFilterBuilder()
							.allow(RegionFilter.VISIBLE_SERVICE_NAMESPACE, "(objectClass=java.lang.Boolean)").build(),
					testRegion);
		}

		bundleInstaller.resolveBundles(bundles.values().toArray(new Bundle[bundles.size()]));
		assertEquals(PP1, Bundle.RESOLVED, bundles.get(PP1).getState());
		assertEquals(SP1, Bundle.INSTALLED, bundles.get(SP1).getState());
		assertEquals(CP1, Bundle.RESOLVED, bundles.get(CP1).getState());
		assertEquals(PP2, Bundle.INSTALLED, bundles.get(PP2).getState());
		assertEquals(SP2, Bundle.INSTALLED, bundles.get(SP2).getState());
		assertEquals(CP2, Bundle.INSTALLED, bundles.get(CP2).getState());
		assertEquals(BC1, Bundle.INSTALLED, bundles.get(BC1).getState());
		assertEquals(SC1, Bundle.INSTALLED, bundles.get(SC1).getState());
		assertEquals(CC1, Bundle.INSTALLED, bundles.get(CC1).getState());

		// now make the necessary connections
		// SP1
		digraph.connect(digraph.getRegion(SP1),
				digraph.createRegionFilterBuilder()
						.allow(RegionFilter.VISIBLE_PACKAGE_NAMESPACE,
								"(" + RegionFilter.VISIBLE_PACKAGE_NAMESPACE + "=pkg1.*)")
						.build(),
				digraph.getRegion(PP1));
		// PP2
		digraph.connect(digraph.getRegion(PP2),
				digraph.createRegionFilterBuilder().allow(CP1, "(name=" + CP1 + ")").build(), digraph.getRegion(CP1));
		// SP2
		digraph.connect(digraph.getRegion(SP2),
				digraph.createRegionFilterBuilder()
						.allow(RegionFilter.VISIBLE_PACKAGE_NAMESPACE,
								"(" + RegionFilter.VISIBLE_PACKAGE_NAMESPACE + "=pkg2.*)")
						.build(),
				digraph.getRegion(PP2));
		// CP2
		digraph.connect(digraph.getRegion(CP2),
				digraph.createRegionFilterBuilder()
						.allow(RegionFilter.VISIBLE_PACKAGE_NAMESPACE,
								"(" + RegionFilter.VISIBLE_PACKAGE_NAMESPACE + "=pkg1.*)")
						.build(),
				digraph.getRegion(PP1));
		digraph.connect(digraph.getRegion(CP2), digraph.createRegionFilterBuilder()
				.allow(RegionFilter.VISIBLE_SERVICE_NAMESPACE, "(" + Constants.OBJECTCLASS + "=pkg1.*)").build(),
				digraph.getRegion(SP1));
		// PC1
		digraph.connect(digraph.getRegion(PC1),
				digraph.createRegionFilterBuilder()
						.allow(RegionFilter.VISIBLE_PACKAGE_NAMESPACE,
								"(" + RegionFilter.VISIBLE_PACKAGE_NAMESPACE + "=pkg2.*)")
						.build(),
				digraph.getRegion(PP2));
		// BC1
		digraph.connect(digraph.getRegion(BC1),
				digraph.createRegionFilterBuilder()
						.allow(RegionFilter.VISIBLE_REQUIRE_NAMESPACE,
								"(" + RegionFilter.VISIBLE_REQUIRE_NAMESPACE + "=" + PP2 + ")")
						.build(),
				digraph.getRegion(PP2));
		// SC1
		digraph.connect(digraph.getRegion(SC1),
				digraph.createRegionFilterBuilder()
						.allow(RegionFilter.VISIBLE_PACKAGE_NAMESPACE,
								"(" + RegionFilter.VISIBLE_PACKAGE_NAMESPACE + "=pkg2.*)")
						.build(),
				digraph.getRegion(PP2));
		digraph.connect(digraph.getRegion(SC1), digraph.createRegionFilterBuilder()
				.allow(RegionFilter.VISIBLE_SERVICE_NAMESPACE, "(" + Constants.OBJECTCLASS + "=pkg2.*)").build(),
				digraph.getRegion(SP2));
		// CC1
		digraph.connect(digraph.getRegion(CC1),
				digraph.createRegionFilterBuilder().allow(CP2, "(name=" + CP2 + ")").build(), digraph.getRegion(CP2));

		bundleInstaller.resolveBundles(bundles.values().toArray(new Bundle[bundles.size()]));
		for (Bundle bundle : bundles.values()) {
			assertEquals("Bundle did not resolve: " + bundle.getSymbolicName(), Bundle.RESOLVED, bundle.getState());
			bundle.start();
		}
		BundleContext context = getContext();
		ServiceTracker<Boolean, Boolean> cp2Tracker = new ServiceTracker<>(context, context.createFilter(
				"(&(objectClass=java.lang.Boolean)(bundle.id=" + bundles.get(CP2).getBundleId() + "))"), null);
		ServiceTracker<Boolean, Boolean> sc1Tracker = new ServiceTracker<>(context, context.createFilter(
				"(&(objectClass=java.lang.Boolean)(bundle.id=" + bundles.get(SC1).getBundleId() + "))"), null);

		cp2Tracker.open();
		sc1Tracker.open();

		assertNotNull("The cp2 bundle never found the service.", cp2Tracker.waitForService(2000));
		assertNotNull("The sc1 bundle never found the service.", sc1Tracker.waitForService(2000));
		cp2Tracker.close();
		sc1Tracker.close();
	}

	// public void testPersistence() throws BundleException, InvalidSyntaxException
	// {
	// // get the system region
	// Region systemRegion = digraph.getRegion(0);
	// Map<String, Bundle> bundles = new HashMap<String, Bundle>();
	// // create a disconnected test region for each test bundle
	// for (String location : ALL) {
	// Region testRegion = digraph.createRegion(location);
	// bundles.put(location, bundleInstaller.installBundle(location, testRegion));
	// // Import the system bundle from the systemRegion
	// digraph.connect(testRegion,
	// digraph.createRegionFilterBuilder().allow(RegionFilter.VISIBLE_BUNDLE_NAMESPACE,
	// "(id=0)").build(), systemRegion);
	// // must import Boolean services into systemRegion to test
	// digraph.connect(systemRegion,
	// digraph.createRegionFilterBuilder().allow(RegionFilter.VISIBLE_SERVICE_NAMESPACE,
	// "(objectClass=java.lang.Boolean)").build(), testRegion);
	// }
	//
	// bundleInstaller.resolveBundles(bundles.values().toArray(new
	// Bundle[bundles.size()]));
	// assertEquals(PP1, Bundle.RESOLVED, bundles.get(PP1).getState());
	// assertEquals(SP1, Bundle.INSTALLED, bundles.get(SP1).getState());
	// assertEquals(CP1, Bundle.RESOLVED, bundles.get(CP1).getState());
	// assertEquals(PP2, Bundle.INSTALLED, bundles.get(PP2).getState());
	// assertEquals(SP2, Bundle.INSTALLED, bundles.get(SP2).getState());
	// assertEquals(CP2, Bundle.INSTALLED, bundles.get(CP2).getState());
	// assertEquals(BC1, Bundle.INSTALLED, bundles.get(BC1).getState());
	// assertEquals(SC1, Bundle.INSTALLED, bundles.get(SC1).getState());
	// assertEquals(CC1, Bundle.INSTALLED, bundles.get(CC1).getState());
	//
	// regionBundle.stop();
	// bundleInstaller.resolveBundles(bundles.values().toArray(new
	// Bundle[bundles.size()]));
	// assertEquals(PP1, Bundle.RESOLVED, bundles.get(PP1).getState());
	// assertEquals(SP1, Bundle.RESOLVED, bundles.get(SP1).getState());
	// assertEquals(CP1, Bundle.RESOLVED, bundles.get(CP1).getState());
	// assertEquals(PP2, Bundle.RESOLVED, bundles.get(PP2).getState());
	// assertEquals(SP2, Bundle.RESOLVED, bundles.get(SP2).getState());
	// assertEquals(CP2, Bundle.RESOLVED, bundles.get(CP2).getState());
	// assertEquals(BC1, Bundle.RESOLVED, bundles.get(BC1).getState());
	// assertEquals(SC1, Bundle.RESOLVED, bundles.get(SC1).getState());
	// assertEquals(CC1, Bundle.RESOLVED, bundles.get(CC1).getState());
	//
	// startRegionBundle();
	//
	// bundleInstaller.refreshPackages(bundles.values().toArray(new
	// Bundle[bundles.size()]));
	// assertEquals(PP1, Bundle.RESOLVED, bundles.get(PP1).getState());
	// assertEquals(SP1, Bundle.INSTALLED, bundles.get(SP1).getState());
	// assertEquals(CP1, Bundle.RESOLVED, bundles.get(CP1).getState());
	// assertEquals(PP2, Bundle.INSTALLED, bundles.get(PP2).getState());
	// assertEquals(SP2, Bundle.INSTALLED, bundles.get(SP2).getState());
	// assertEquals(CP2, Bundle.INSTALLED, bundles.get(CP2).getState());
	// assertEquals(BC1, Bundle.INSTALLED, bundles.get(BC1).getState());
	// assertEquals(SC1, Bundle.INSTALLED, bundles.get(SC1).getState());
	// assertEquals(CC1, Bundle.INSTALLED, bundles.get(CC1).getState());
	// }
	//
	// public void testPersistenceBug343020() throws BundleException,
	// InvalidSyntaxException {
	// // get the system region
	// Region systemRegion = digraph.getRegion(0);
	// // create a test region
	// Region testRegion = digraph.createRegion(getName());
	//
	// RegionFilterBuilder builder = digraph.createRegionFilterBuilder();
	// // Import the system bundle from the systemRegion
	// builder.allow(RegionFilter.VISIBLE_BUNDLE_NAMESPACE, "(id=0)");
	// // import PP1
	// builder.allow(RegionFilter.VISIBLE_PACKAGE_NAMESPACE, "(" +
	// RegionFilter.VISIBLE_PACKAGE_NAMESPACE + "=pkg1.*)");
	// digraph.connect(testRegion, builder.build(), systemRegion);
	// // install CP2
	// Bundle cp2 = bundleInstaller.installBundle(CP2, testRegion);
	//
	// bundleInstaller.resolveBundles(new Bundle[] {cp2});
	// assertEquals("Wrong state for pc1.", Bundle.INSTALLED, cp2.getState());
	//
	// regionBundle.stop();
	//
	// // install PP1 there is no region alive
	// bundleInstaller.installBundle(PP1);
	//
	// // start region bundle and confirm we can resolve cp2 now
	// startRegionBundle();
	//
	// bundleInstaller.refreshPackages(new Bundle[] {cp2});
	// assertEquals("Wrong state for pc1.", Bundle.RESOLVED, cp2.getState());
	//
	// // stop region bundle to test uninstalling bundles while stopped
	// regionBundle.stop();
	// cp2.uninstall();
	//
	// startRegionBundle();
	// testRegion = digraph.getRegion(getName());
	// assertNotNull("No test region found.", testRegion);
	// Set<Long> testIds = testRegion.getBundleIds();
	// assertEquals("Wrong number of test ids.", 0, testIds.size());
	// }

	@Test
	public void testCyclicRegions0() throws BundleException, InvalidSyntaxException, InterruptedException {
		doCyclicRegions(0);
	}

	@Test
	public void testCyclicRegions10() throws BundleException, InvalidSyntaxException, InterruptedException {
		doCyclicRegions(10);
	}

	@Test
	public void testCyclicRegions100() throws BundleException, InvalidSyntaxException, InterruptedException {
		doCyclicRegions(100);
	}

	private void doCyclicRegions(int numLevels) throws BundleException, InvalidSyntaxException, InterruptedException {
		String regionName1 = testName.getMethodName() + "_1";
		String regionName2 = testName.getMethodName() + "_2";
		// get the system region
		Region systemRegion = digraph.getRegion(0);
		// create two regions to hold the bundles
		Region testRegion1 = digraph.createRegion(regionName1);
		Region testRegion2 = digraph.createRegion(regionName2);
		// connect to the system bundle
		testRegion1.connectRegion(systemRegion,
				digraph.createRegionFilterBuilder().allow(RegionFilter.VISIBLE_BUNDLE_NAMESPACE, "(id=0)").build());
		testRegion2.connectRegion(systemRegion,
				digraph.createRegionFilterBuilder().allow(RegionFilter.VISIBLE_BUNDLE_NAMESPACE, "(id=0)").build());
		// must import Boolean services into systemRegion to test
		systemRegion.connectRegion(testRegion1, digraph.createRegionFilterBuilder()
				.allow(RegionFilter.VISIBLE_SERVICE_NAMESPACE, "(objectClass=java.lang.Boolean)").build());
		systemRegion.connectRegion(testRegion2, digraph.createRegionFilterBuilder()
				.allow(RegionFilter.VISIBLE_SERVICE_NAMESPACE, "(objectClass=java.lang.Boolean)").build());

		Map<String, Bundle> bundles = new HashMap<>();
		// add bundles to region1
		bundles.put(PP1, bundleInstaller.installBundle(PP1, testRegion1));
		bundles.put(SP2, bundleInstaller.installBundle(SP2, testRegion1));
		bundles.put(CP2, bundleInstaller.installBundle(CP2, testRegion1));
		bundles.put(PC1, bundleInstaller.installBundle(PC1, testRegion1));
		bundles.put(BC1, bundleInstaller.installBundle(BC1, testRegion1));

		// add bundles to region2
		bundles.put(SP1, bundleInstaller.installBundle(SP1, testRegion2));
		bundles.put(CP1, bundleInstaller.installBundle(CP1, testRegion2));
		bundles.put(PP2, bundleInstaller.installBundle(PP2, testRegion2));
		bundles.put(SC1, bundleInstaller.installBundle(SC1, testRegion2));
		bundles.put(CC1, bundleInstaller.installBundle(CC1, testRegion2));

		RegionFilterBuilder testRegionFilter1 = digraph.createRegionFilterBuilder();
		// SP2 -> PP2
		testRegionFilter1.allow(RegionFilter.VISIBLE_PACKAGE_NAMESPACE,
				"(" + RegionFilter.VISIBLE_PACKAGE_NAMESPACE + "=pkg2.*)");
		// CP2 -> SP1
		testRegionFilter1.allow(RegionFilter.VISIBLE_SERVICE_NAMESPACE, "(" + Constants.OBJECTCLASS + "=pkg1.*)");
		// PC1 -> PP2
		// this is not needed because we already import pkg2.* above
		// testRegionFilter1.allow(RegionFilter.VISIBLE_PACKAGE_NAMESPACE, "(" +
		// RegionFilter.VISIBLE_PACKAGE_NAMESPACE + "=pkg2.*)");
		// BC1 -> PP2
		testRegionFilter1.allow(RegionFilter.VISIBLE_REQUIRE_NAMESPACE,
				"(" + RegionFilter.VISIBLE_REQUIRE_NAMESPACE + "=" + PP2 + ")");

		RegionFilterBuilder testRegionFilter2 = digraph.createRegionFilterBuilder();
		// SP1 -> PP1
		testRegionFilter2.allow(RegionFilter.VISIBLE_PACKAGE_NAMESPACE,
				"(" + RegionFilter.VISIBLE_PACKAGE_NAMESPACE + "=pkg1.*)");
		// SC1 -> SP2
		testRegionFilter2.allow(RegionFilter.VISIBLE_SERVICE_NAMESPACE, "(" + Constants.OBJECTCLASS + "=pkg2.*)");
		// CC1 -> CP2
		testRegionFilter2.allow(CP2, "(name=" + CP2 + ")");

		Region r1, r2 = null;
		for (int i = 0; i <= numLevels; i++) {
			r1 = (i > 0) ? r2 : testRegion1;
			r2 = (i < numLevels) ? digraph.createRegion(testName.getMethodName() + "_level_" + i) : testRegion2;
			r1.connectRegion(r2, testRegionFilter1.build());
			r2.connectRegion(r1, testRegionFilter2.build());
		}

		bundleInstaller.resolveBundles(bundles.values().toArray(new Bundle[bundles.size()]));
		for (Bundle bundle : bundles.values()) {
			assertEquals("Bundle did not resolve: " + bundle.getSymbolicName(), Bundle.RESOLVED, bundle.getState());
			bundle.start();
		}
		BundleContext context = getContext();
		ServiceTracker<Boolean, Boolean> cp2Tracker = new ServiceTracker<>(context, context.createFilter(
				"(&(objectClass=java.lang.Boolean)(bundle.id=" + bundles.get(CP2).getBundleId() + "))"), null);
		ServiceTracker<Boolean, Boolean> sc1Tracker = new ServiceTracker<>(context, context.createFilter(
				"(&(objectClass=java.lang.Boolean)(bundle.id=" + bundles.get(SC1).getBundleId() + "))"), null);

		cp2Tracker.open();
		sc1Tracker.open();

		assertNotNull("The cp2 bundle never found the service.", cp2Tracker.waitForService(2000));
		assertNotNull("The sc1 bundle never found the service.", sc1Tracker.waitForService(2000));
		cp2Tracker.close();
		sc1Tracker.close();
	}

	private ServiceRegistration<ResolverHookFactory> disableAllResolves() {
		return getContext().registerService(ResolverHookFactory.class, new ResolverHookFactory() {

			@Override
			public ResolverHook begin(Collection<BundleRevision> triggers) {
				return new ResolverHook() {

					@Override
					public void filterSingletonCollisions(BundleCapability singleton,
							Collection<BundleCapability> collisionCandidates) {
						// nothing;
					}

					@Override
					public void filterResolvable(Collection<BundleRevision> candidates) {
						// prevent all resolves
						candidates.clear();
					}

					@Override
					public void filterMatches(BundleRequirement requirement, Collection<BundleCapability> candidates) {
						// nothing;
					}

					@Override
					public void end() {
						// TODO Auto-generated method stub

					}
				};
			}
		}, null);
	}

	@Test
	public void testSingletons() throws BundleException {
		Region region1 = digraph.createRegion(testName.getMethodName() + "_1");
		Region region2 = digraph.createRegion(testName.getMethodName() + "_2");

		// first install into the same region; higher version 2 should resolve
		Bundle singleton1 = bundleInstaller.installBundle(SINGLETON1, region1);
		Bundle singleton2 = bundleInstaller.installBundle(SINGLETON2, region1);
		assertFalse(bundleInstaller.resolveBundles(new Bundle[] { singleton1, singleton2 }));
		assertEquals("singleton1", Bundle.INSTALLED, singleton1.getState());
		assertEquals("singleton2", Bundle.RESOLVED, singleton2.getState());

		// now install into different regions; both 1 and 2 should resolve
		singleton2.uninstall();
		singleton2 = bundleInstaller.installBundle(SINGLETON2, region2);
		assertTrue(bundleInstaller.resolveBundles(new Bundle[] { singleton1, singleton2 }));
		assertEquals("singleton1", Bundle.RESOLVED, singleton1.getState());
		assertEquals("singleton2", Bundle.RESOLVED, singleton2.getState());

		ServiceRegistration<ResolverHookFactory> disableHook = disableAllResolves();
		try {
			// now refresh to get us to an unresolved state again
			bundleInstaller.refreshPackages(new Bundle[] { singleton1, singleton2 });
			// connect region2 -> region1
			region2.connectRegion(region1,
					digraph.createRegionFilterBuilder().allowAll(RegionFilter.VISIBLE_BUNDLE_NAMESPACE).build());
			// enable resolving again
			disableHook.unregister();
			disableHook = null;

			assertFalse(bundleInstaller.resolveBundles(new Bundle[] { singleton1, singleton2 }));
			assertTrue("One and only singleton bundle should be resolved",
					(singleton1.getState() == Bundle.RESOLVED) ^ (singleton2.getState() == Bundle.RESOLVED));

			singleton2.uninstall();
			disableHook = disableAllResolves();
			// now refresh to get us to an unresolved state again
			bundleInstaller.refreshPackages(new Bundle[] { singleton1, singleton2 });
			// enable resolving again
			disableHook.unregister();
			disableHook = null;

			// make sure singleton1 is resolved first
			assertTrue(bundleInstaller.resolveBundles(new Bundle[] { singleton1 }));
			assertEquals("singleton1", Bundle.RESOLVED, singleton1.getState());
			singleton2 = bundleInstaller.installBundle(SINGLETON2, region2);
			assertFalse(bundleInstaller.resolveBundles(new Bundle[] { singleton2 }));
			assertEquals("singleton2", Bundle.INSTALLED, singleton2.getState());

			singleton1.uninstall();
			disableHook = disableAllResolves();
			// now refresh to get us to an unresolved state again
			bundleInstaller.refreshPackages(new Bundle[] { singleton1, singleton2 });
			// enable resolving again
			disableHook.unregister();
			disableHook = null;

			// make sure singleton2 is resolved first
			assertTrue(bundleInstaller.resolveBundles(new Bundle[] { singleton2 }));
			assertEquals("singleton2", Bundle.RESOLVED, singleton2.getState());
			singleton1 = bundleInstaller.installBundle(SINGLETON1, region1);
			assertFalse(bundleInstaller.resolveBundles(new Bundle[] { singleton1 }));
			assertEquals("singleton1", Bundle.INSTALLED, singleton1.getState());
		} finally {
			if (disableHook != null)
				disableHook.unregister();
		}
	}

	private static final String REGION_DOMAIN_PROP = "org.eclipse.equinox.region.domain";

	@Test
	public void testMbeans() throws MalformedObjectNameException, BundleException, InstanceNotFoundException,
			ReflectionException, MBeanException, AttributeNotFoundException {
		MBeanServer server = ManagementFactory.getPlatformMBeanServer();
		ObjectName digraphName = new ObjectName(REGION_DOMAIN_PROP + ":type=RegionDigraph,*");
		ObjectName regionNameAllQuery = new ObjectName(REGION_DOMAIN_PROP + ":type=Region,name=*,*");
		Set<ObjectInstance> digraphs = server.queryMBeans(null, digraphName);
		assertEquals("Expected only one instance of digraph", 1, digraphs.size());
		Set<ObjectInstance> regions = server.queryMBeans(null, regionNameAllQuery);
		assertEquals("Expected only one instance of region", 1, regions.size());

		Region pp1Region = digraph.createRegion(PP1);
		Bundle pp1Bundle = bundleInstaller.installBundle(PP1, pp1Region);
		Region sp1Region = digraph.createRegion(SP1);
		Bundle sp1Bundle = bundleInstaller.installBundle(SP1, sp1Region);

		regions = server.queryMBeans(null, regionNameAllQuery);
		assertEquals("Wrong number of regions", 3, regions.size());

		Set<ObjectInstance> pp1Query = server.queryMBeans(null,
				new ObjectName(REGION_DOMAIN_PROP + ":type=Region,name=" + PP1 + ",*"));
		assertEquals("Expected only one instance of: " + PP1, 1, pp1Query.size());
		Set<ObjectInstance> sp1Query = server.queryMBeans(null,
				new ObjectName(REGION_DOMAIN_PROP + ":type=Region,name=" + SP1 + ",*"));
		assertEquals("Expected only one instance of: " + SP1, 1, sp1Query.size());
		ObjectName pp1Name = (ObjectName) server.invoke(digraphs.iterator().next().getObjectName(), "getRegion",
				new Object[] { PP1 }, new String[] { String.class.getName() });
		assertEquals(PP1 + " regions not equal.", pp1Query.iterator().next().getObjectName(), pp1Name);
		ObjectName sp1Name = (ObjectName) server.invoke(digraphs.iterator().next().getObjectName(), "getRegion",
				new Object[] { SP1 }, new String[] { String.class.getName() });
		assertEquals(SP1 + " regions not equal.", sp1Query.iterator().next().getObjectName(), sp1Name);

		// test non existing region
		ObjectName shouldNotExistName = (ObjectName) server.invoke(digraphs.iterator().next().getObjectName(),
				"getRegion", new Object[] { "ShouldNotExist" }, new String[] { String.class.getName() });
		assertNull("Should not exist", shouldNotExistName);

		long[] bundleIds = (long[]) server.getAttribute(pp1Name, "BundleIds");
		assertEquals("Wrong number of bundles", 1, bundleIds.length);
		assertEquals("Wrong bundle", pp1Bundle.getBundleId(), bundleIds[0]);
		String name = (String) server.getAttribute(pp1Name, "Name");
		assertEquals("Wrong name", PP1, name);

		bundleIds = (long[]) server.getAttribute(sp1Name, "BundleIds");
		assertEquals("Wrong number of bundles", 1, bundleIds.length);
		assertEquals("Wrong bundle", sp1Bundle.getBundleId(), bundleIds[0]);
		name = (String) server.getAttribute(sp1Name, "Name");
		assertEquals("Wrong name", SP1, name);
	}

	@Test
	public void testBundleCollisionDisconnectedRegions() throws BundleException, InvalidSyntaxException {
		// get the system region
		Region systemRegion = digraph.getRegion(0);
		Collection<Bundle> bundles = new HashSet<>();
		// create 4 disconnected test regions and install each bundle into each region
		int numRegions = 4;
		String regionName = "IsolatedRegion_";
		for (int i = 0; i < numRegions; i++) {
			Region region = digraph.createRegion(regionName + i);
			// Import the system bundle from the systemRegion
			digraph.connect(region,
					digraph.createRegionFilterBuilder().allow(RegionFilter.VISIBLE_BUNDLE_NAMESPACE, "(id=0)").build(),
					systemRegion);
			// must import Boolean services into systemRegion to test
			digraph.connect(systemRegion,
					digraph.createRegionFilterBuilder()
							.allow(RegionFilter.VISIBLE_SERVICE_NAMESPACE, "(objectClass=java.lang.Boolean)").build(),
					region);
			for (String location : ALL) {
				Bundle b = bundleInstaller.installBundle(location, region);
				bundles.add(b);
			}
		}

		assertEquals("Wrong number of bundles installed", numRegions * ALL.size(), bundles.size());
		assertTrue("Could not resolve bundles.",
				bundleInstaller.resolveBundles(bundles.toArray(new Bundle[bundles.size()])));

		// test install of duplicates
		for (int i = 0; i < numRegions; i++) {
			Region region = digraph.getRegion(regionName + i);
			for (String name : ALL) {
				String location = bundleInstaller.getBundleLocation(name);
				try {
					Bundle b = region.installBundle(testName.getMethodName() + "_expectToFail",
							new URL(location).openStream());
					b.uninstall();
					fail("Expected a bundle exception on duplicate bundle installation: " + name);
				} catch (BundleException e) {
					// expected
					assertEquals("Wrong exception type.", BundleException.DUPLICATE_BUNDLE_ERROR, e.getType());
				} catch (IOException e) {
					fail("Failed to open bunldle location: " + e.getMessage());
				}
			}
		}

		// test update to a duplicate
		for (int i = 0; i < numRegions; i++) {
			Region region = digraph.getRegion(regionName + i);

			Bundle regionPP1 = region.getBundle(PP1, new Version(1, 0, 0));

			String locationSP1 = bundleInstaller.getBundleLocation(SP1);
			try {
				regionPP1.update(new URL(locationSP1).openStream());
				fail("Expected a bundle exception on duplicate bundle update: " + region);
			} catch (BundleException e) {
				// expected
				assertEquals("Wrong exception type.", BundleException.DUPLICATE_BUNDLE_ERROR, e.getType());
			} catch (IOException e) {
				fail("Failed to open bunldle location: " + e.getMessage());
			}

			// now uninstall SP1 and try to update PP1 to SP1 again
			Bundle regionSP1 = region.getBundle(SP1, new Version(1, 0, 0));
			regionSP1.uninstall();

			try {
				regionPP1.update(new URL(locationSP1).openStream());
			} catch (IOException e) {
				fail("Failed to open bunldle location: " + e.getMessage());
			}
		}
	}

	@Test
	public void testBundleCollisionUninstalledBundle() throws BundleException {
		Region region1 = digraph.createRegion(testName.getMethodName() + 1);
		Region region2 = digraph.createRegion(testName.getMethodName() + 2);
		Region region3 = digraph.createRegion(testName.getMethodName() + 3);
		// install the same bundle into the first two regions. Should be no collision
		bundleInstaller.installBundle(PP1, region1);
		bundleInstaller.installBundle(PP1, region2);

		ServiceRegistration<CollisionHook> collisionHookREg = getContext().registerService(CollisionHook.class,
				new CollisionHook() {
					@Override
					public void filterCollisions(int operationType, Bundle target,
							Collection<Bundle> collisionCandidates) {
						for (Bundle bundle : collisionCandidates) {
							try {
								bundle.uninstall();
							} catch (BundleException e) {
								e.printStackTrace();
							}
						}
					}
				},
				new Hashtable<String, Object>(Collections.singletonMap(Constants.SERVICE_RANKING, Integer.MAX_VALUE)));
		try {
			bundleInstaller.installBundle(PP1, region3);
		} finally {
			collisionHookREg.unregister();
		}
	}

	@Test
	public void testBundleCollisionConnectedRegions() throws BundleException, InvalidSyntaxException {
		// get the system region
		Region systemRegion = digraph.getRegion(0);
		// create 3 connected test regions and install each bundle into each region
		int numRegions = 4;
		String regionName = "ConnectedRegion_";
		for (int i = 0; i < numRegions; i++) {
			Region region = digraph.createRegion(regionName + i);
			// Import the system bundle from the systemRegion
			digraph.connect(region,
					digraph.createRegionFilterBuilder().allow(RegionFilter.VISIBLE_BUNDLE_NAMESPACE, "(id=0)").build(),
					systemRegion);
			// must import Boolean services into systemRegion to test
			digraph.connect(systemRegion,
					digraph.createRegionFilterBuilder()
							.allow(RegionFilter.VISIBLE_SERVICE_NAMESPACE, "(objectClass=java.lang.Boolean)").build(),
					region);
		}

		Region region0 = digraph.getRegion(regionName + 0);
		Region region1 = digraph.getRegion(regionName + 1);
		Region region2 = digraph.getRegion(regionName + 2);

		// create connections that share the bundles we want
		RegionFilterBuilder filterBuilder = digraph.createRegionFilterBuilder();
		filterBuilder.allow(RegionFilter.VISIBLE_BUNDLE_NAMESPACE, "(bundle-symbolic-name=" + PP1 + ")");
		filterBuilder.allow(RegionFilter.VISIBLE_BUNDLE_NAMESPACE, "(bundle-symbolic-name=" + SP1 + ")");
		filterBuilder.allow(RegionFilter.VISIBLE_BUNDLE_NAMESPACE, "(bundle-symbolic-name=" + CP1 + ")");
		region1.connectRegion(region0, filterBuilder.build());
		region2.connectRegion(region1, filterBuilder.build());

		// install a bundle in each region
		bundleInstaller.installBundle(PP1, region0);
		bundleInstaller.installBundle(SP1, region1);
		bundleInstaller.installBundle(CP1, region2);

		// Should not be able to install SP1 into region0 because that would collide
		// with SP1->region1->region0
		assertInstallFail(SP1, region0);
		// Should not be able to install PP1 into region1 because that would collide
		// with region1->region0->PP1
		assertInstallFail(PP1, region1);
		// Should not be able to install PP1 into region2 because that would collide
		// with region2->region1->region0->PP1
		assertInstallFail(PP1, region2);
		// Should not be able to install CP1 into region0 because that would collide
		// with CP1->region2->region1->region0
		assertInstallFail(CP1, region0);
	}

	private void assertInstallFail(String name, Region region) {
		try {
			Bundle b = bundleInstaller.installBundle(name, region);
			b.uninstall();
			fail("Expected a bundle exception on duplicate bundle install: " + region);
		} catch (BundleException e) {
			// expected
			assertEquals("Wrong exception type.", BundleException.DUPLICATE_BUNDLE_ERROR, e.getType());
		}
	}

	@Test
	public void testDefaultRegion() throws BundleException {
		digraph.setDefaultRegion(null);

		Region systemRegion = digraph.getRegion(0);
		Region pp1Region = digraph.createRegion(PP1);

		Bundle pp1Bundle = bundleInstaller.installBundle(PP1, null);
		Region result = digraph.getRegion(pp1Bundle);
		assertEquals("Wrong region", systemRegion, result);

		pp1Bundle.uninstall();

		digraph.setDefaultRegion(pp1Region);
		pp1Bundle = bundleInstaller.installBundle(PP1, null);
		result = digraph.getRegion(pp1Bundle);
		assertEquals("Wrong region", pp1Region, result);

		digraph.setDefaultRegion(null);
	}

	@Test
	public void testRemoveDefaultRegion() throws BundleException {
		digraph.setDefaultRegion(null);

		Region pp1Region = digraph.createRegion(PP1);
		digraph.setDefaultRegion(pp1Region);
		digraph.removeRegion(pp1Region);
		assertEquals("DefaultRegion is not null", null, digraph.getDefaultRegion());
	}

	@Test
	public void testSetNotExistingDefaultRegion() throws BundleException {
		Region pp1Region = digraph.createRegion(PP1);
		digraph.removeRegion(pp1Region);
		try {
			digraph.setDefaultRegion(pp1Region);
			assertFalse("IllegalArgumentException not thrown for setting non-existing region as default", true);
		} catch (IllegalStateException e) {
			assertNull("DefaultRegion is not null", digraph.getDefaultRegion());
		}
	}

	@Test
	public void testRemoveRegion() throws BundleException {
		Region pp1Region = digraph.createRegion(PP1);
		pp1Region.addBundle(TEST_BUNDLE_ID);
		assertEquals("Region not associated with bundle id", pp1Region, digraph.getRegion(TEST_BUNDLE_ID));
		digraph.removeRegion(pp1Region);
		assertNull("Region still associated with bundle id", digraph.getRegion(TEST_BUNDLE_ID));

		// Adding a bundle to a removed region should not change the digraph and should
		// error
		try {
			pp1Region.addBundle(TEST_BUNDLE_ID);
			fail("Added a bundle to a region which was not part of a digraph");
		} catch (IllegalStateException e) {
			// expected
		}
		assertNull("Region now associated with bundle id", digraph.getRegion(TEST_BUNDLE_ID));

		Region pp2Region = digraph.createRegion(PP2);
		pp2Region.addBundle(TEST_BUNDLE_ID);

		// removing a bundle from a removed region should not change the digraph and
		// should error
		try {
			pp1Region.removeBundle(TEST_BUNDLE_ID);
			fail("Removed a bundle via a region which was not part of a digraph");
		} catch (IllegalStateException e) {
			// Expected
		}
		assertEquals("Wrong region found for the bundle id", pp2Region, digraph.getRegion(TEST_BUNDLE_ID));
	}

	@Test
	public void testInstallAtLocation() throws BundleException, MalformedURLException, IOException {
		// create disconnected test regions
		Region r1 = digraph.createRegion(testName.getMethodName() + ".1");
		Region r2 = digraph.createRegion(testName.getMethodName() + ".2");

		String location = bundleInstaller.getBundleLocation(PP1);
		Bundle b1 = null;
		Bundle b2 = null;
		String l1 = null;
		String l2 = null;
		try {
			URL url = new URL(location);
			b1 = r1.installBundle(location + ".1", url.openStream());
			l1 = b1.getLocation();
			b2 = r2.installBundleAtLocation(location + ".2", url.openStream());
			l2 = b2.getLocation();
		} finally {
			if (b1 != null) {
				try {
					b1.uninstall();
				} catch (BundleException e) {
					// ignore
				}
			}
			if (b2 != null) {
				try {
					b2.uninstall();
				} catch (BundleException e) {
					// ignore
				}
			}
		}
		assertEquals("Wrong location found.", location + ".1#" + r1.getName(), l1);
		assertEquals("Wrong location found.", location + ".2", l2);
	}

	@Test
	public void testInvalidRegionName() {
		Collection<String> invalidNames = new ArrayList<>();
		invalidNames.addAll(Arrays.asList(":", "bad:Name", ":bad::name:", ":badname", "badname:"));
		invalidNames.addAll(Arrays.asList("=", "bad=Name", "=bad==name=", "=badname", "badname="));
		invalidNames.addAll(Arrays.asList("\n", "bad\nName", "\nbad\n\nname\n", "\nbadname", "badname\n"));
		invalidNames.addAll(Arrays.asList("*", "bad*Name", "*bad**name*", "*badname", "badname*"));
		invalidNames.addAll(Arrays.asList("?", "bad?Name", "?bad??name?", "?badname", "badname?"));
		invalidNames.addAll(Arrays.asList(",", "bad,Name", ",bad,,name,", ",badname", "badname,"));
		invalidNames.addAll(Arrays.asList("\"", "bad\"Name", "\"bad\"\"name\"", "\"badname", "badname\""));
		invalidNames.addAll(Arrays.asList("\\", "bad\\Name", "\\bad\\\\name\\", "\\badname", "badname\\"));

		for (String invalidName : invalidNames) {
			try {
				digraph.createRegion(invalidName);
				fail("Expected failure to create region.");
			} catch (IllegalArgumentException e) {
				// expected
			} catch (BundleException e) {
				fail("Unexpected bundle exception: " + e.getMessage());
			}
		}

	}

	@Test
	public void testHigherRankedEventHookResolve() throws BundleException {
		final List<BundleEvent> events = new CopyOnWriteArrayList<>();
		SynchronousBundleListener listener = new SynchronousBundleListener() {
			@Override
			public void bundleChanged(BundleEvent event) {
				events.add(event);
			}
		};
		getContext().addBundleListener(listener);
		// register a higher ranked bundle EventHook that causes a bundle to resolve
		// while processing INSTALLED events
		ServiceRegistration<EventHook> bundleEventHook = getContext().registerService(EventHook.class, new EventHook() {
			@Override
			public void event(BundleEvent event, Collection<BundleContext> contexts) {
				// force resolution if event is INSTALLED
				if (event.getType() == BundleEvent.INSTALLED) {
					getContext().getBundle(Constants.SYSTEM_BUNDLE_LOCATION).adapt(FrameworkWiring.class)
							.resolveBundles(Collections.singleton(event.getBundle()));
				}
			}
		}, new Hashtable<String, Object>(Collections.singletonMap(Constants.SERVICE_RANKING, Integer.MAX_VALUE)));
		Bundle b = null;
		try {
			// install a bundle with no dependencies
			b = bundleInstaller.installBundle(CP1);
			b.start();
		} finally {
			getContext().removeBundleListener(listener);
			bundleEventHook.unregister();
		}

		// Had to change the expected order to be out of order now since region hooks
		// are always first
		for (int eventType : new int[] { BundleEvent.RESOLVED, BundleEvent.INSTALLED, BundleEvent.STARTING,
				BundleEvent.STARTED }) {
			if (events.isEmpty()) {
				fail("No events left, expecting event: " + eventType);
			}
			BundleEvent event = events.remove(0);
			assertEquals("Wrong event type.", eventType, event.getType());
			assertEquals("Wrong bundle.", b, event.getBundle());
		}
	}

	@Test
	public void testHigherRankedEventHookUninstall() throws BundleException {
		// register a higher ranked bundle EventHook that causes a bundle to resolve
		// while processing INSTALLED events
		ServiceRegistration<EventHook> bundleEventHook = getContext().registerService(EventHook.class, new EventHook() {
			@Override
			public void event(BundleEvent event, Collection<BundleContext> contexts) {
				// force uninstall if event is INSTALLED (evil)
				if (event.getType() == BundleEvent.INSTALLED) {
					try {
						event.getBundle().uninstall();
					} catch (BundleException e) {
						e.printStackTrace();
					}
				}
			}
		}, new Hashtable<String, Object>(Collections.singletonMap(Constants.SERVICE_RANKING, Integer.MAX_VALUE)));
		Bundle b = null;
		try {
			// install a bundle with no dependencies
			b = bundleInstaller.installBundle(CP1);
		} finally {
			bundleEventHook.unregister();
		}

		assertNull("Found region for uninstalled bundle.", digraph.getRegion(b));
	}

	@Test
	public void testReplaceConnection() throws BundleException, InvalidSyntaxException {
		// get the system region
		Region systemRegion = digraph.getRegion(0);
		Map<String, Bundle> bundles = new HashMap<>();
		// create a disconnected test region for each test bundle
		for (String location : new String[] { PP1, SP1 }) {
			Region testRegion = digraph.createRegion(location);
			bundles.put(location, bundleInstaller.installBundle(location, testRegion));
			// Import the system bundle from the systemRegion
			digraph.connect(testRegion,
					digraph.createRegionFilterBuilder().allow(RegionFilter.VISIBLE_BUNDLE_NAMESPACE, "(id=0)").build(),
					systemRegion);
			// must import Boolean services into systemRegion to test
			digraph.connect(systemRegion,
					digraph.createRegionFilterBuilder()
							.allow(RegionFilter.VISIBLE_SERVICE_NAMESPACE, "(objectClass=java.lang.Boolean)").build(),
					testRegion);
		}

		bundleInstaller.resolveBundles(bundles.values().toArray(new Bundle[bundles.size()]));
		assertEquals(PP1, Bundle.RESOLVED, bundles.get(PP1).getState());
		assertEquals(SP1, Bundle.INSTALLED, bundles.get(SP1).getState());

		// now make a connection that does not let the necessary package through
		RegionFilter badRegionFilter = digraph.createRegionFilterBuilder()
				.allow(RegionFilter.VISIBLE_PACKAGE_NAMESPACE, "(" + RegionFilter.VISIBLE_PACKAGE_NAMESPACE + "=bad)")
				.build();

		Set<FilteredRegion> edges = digraph.getRegion(SP1).getEdges();
		assertEquals("Wrong number of edges.", 1, edges.size());

		// use replace and verify a new edge is added if the connection did not exist
		// already
		assertNull("Found existing connection.",
				digraph.replaceConnection(digraph.getRegion(SP1), badRegionFilter, digraph.getRegion(PP1)));
		edges = digraph.getRegion(SP1).getEdges();
		assertEquals("Wrong number of edges.", 2, edges.size());

		// still should not resolve
		bundleInstaller.resolveBundles(bundles.values().toArray(new Bundle[bundles.size()]));
		assertEquals(PP1, Bundle.RESOLVED, bundles.get(PP1).getState());
		assertEquals(SP1, Bundle.INSTALLED, bundles.get(SP1).getState());

		// reconnect to let the package though
		RegionFilter goodRegionFilter = digraph.createRegionFilterBuilder()
				.allow(RegionFilter.VISIBLE_PACKAGE_NAMESPACE,
						"(" + RegionFilter.VISIBLE_PACKAGE_NAMESPACE + "=pkg1.*)")
				.build();
		RegionFilter existingFilter = digraph.replaceConnection(digraph.getRegion(SP1), goodRegionFilter,
				digraph.getRegion(PP1));
		assertEquals("Wrong existing filter found.", badRegionFilter, existingFilter);

		// number of edges must remain 2 since we use reconnect
		edges = digraph.getRegion(SP1).getEdges();
		assertEquals("Wrong number of edges.", 2, edges.size());
		// should resolve now

		bundleInstaller.resolveBundles(bundles.values().toArray(new Bundle[bundles.size()]));
		for (Bundle bundle : bundles.values()) {
			assertEquals("Bundle did not resolve: " + bundle.getSymbolicName(), Bundle.RESOLVED, bundle.getState());
			bundle.start();
		}

		// now remove the connection
		existingFilter = digraph.replaceConnection(digraph.getRegion(SP1), null, digraph.getRegion(PP1));
		assertEquals("Wrong existing filter found.", goodRegionFilter, existingFilter);
		edges = digraph.getRegion(SP1).getEdges();
		assertEquals("Wrong number of edges.", 1, edges.size());

		bundleInstaller.refreshPackages(bundles.values().toArray(new Bundle[bundles.size()]));
		// should not resolve again
		bundleInstaller.resolveBundles(bundles.values().toArray(new Bundle[bundles.size()]));
		assertEquals(PP1, Bundle.ACTIVE, bundles.get(PP1).getState());
		assertEquals(SP1, Bundle.INSTALLED, bundles.get(SP1).getState());
	}

}
