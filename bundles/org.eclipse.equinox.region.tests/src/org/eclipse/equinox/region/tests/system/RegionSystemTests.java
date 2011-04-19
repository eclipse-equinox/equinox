/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.region.tests.system;

import java.lang.management.ManagementFactory;
import java.util.*;
import javax.management.*;
import org.eclipse.equinox.region.*;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;

public class RegionSystemTests extends AbstractRegionSystemTest {
	public void testBasic() throws BundleException, InvalidSyntaxException, InterruptedException {
		// get the system region
		Region systemRegion = digraph.getRegion(0);
		// create a disconnected test region
		Region testRegion = digraph.createRegion(getName());
		List<Bundle> bundles = new ArrayList<Bundle>();
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
		digraph.connect(testRegion, digraph.createRegionFilterBuilder().allow(RegionFilter.VISIBLE_BUNDLE_NAMESPACE, "(id=0)").build(), systemRegion);
		// must import Boolean services into systemRegion to test
		digraph.connect(systemRegion, digraph.createRegionFilterBuilder().allow(RegionFilter.VISIBLE_SERVICE_NAMESPACE, "(objectClass=java.lang.Boolean)").build(), testRegion);

		bundleInstaller.resolveBundles(bundles.toArray(new Bundle[bundles.size()]));
		for (Bundle bundle : bundles) {
			assertEquals("Bundle did not resolve: " + bundle.getSymbolicName(), Bundle.RESOLVED, bundle.getState());
			bundle.start();
		}
		BundleContext context = getContext();
		ServiceTracker<Boolean, Boolean> cp2Tracker = new ServiceTracker<Boolean, Boolean>(context, context.createFilter("(&(objectClass=java.lang.Boolean)(bundle.id=" + cp2.getBundleId() + "))"), null);
		ServiceTracker<Boolean, Boolean> sc1Tracker = new ServiceTracker<Boolean, Boolean>(context, context.createFilter("(&(objectClass=java.lang.Boolean)(bundle.id=" + sc1.getBundleId() + "))"), null);

		cp2Tracker.open();
		sc1Tracker.open();

		assertNotNull("The cp2 bundle never found the service.", cp2Tracker.waitForService(2000));
		assertNotNull("The sc1 bundle never found the service.", sc1Tracker.waitForService(2000));
		cp2Tracker.close();
		sc1Tracker.close();
	}

	public void testSingleBundleRegions() throws BundleException, InvalidSyntaxException, InterruptedException {
		// get the system region
		Region systemRegion = digraph.getRegion(0);
		Map<String, Bundle> bundles = new HashMap<String, Bundle>();
		// create a disconnected test region for each test bundle
		for (String location : ALL) {
			Region testRegion = digraph.createRegion(location);
			bundles.put(location, bundleInstaller.installBundle(location, testRegion));
			// Import the system bundle from the systemRegion
			digraph.connect(testRegion, digraph.createRegionFilterBuilder().allow(RegionFilter.VISIBLE_BUNDLE_NAMESPACE, "(id=0)").build(), systemRegion);
			// must import Boolean services into systemRegion to test
			digraph.connect(systemRegion, digraph.createRegionFilterBuilder().allow(RegionFilter.VISIBLE_SERVICE_NAMESPACE, "(objectClass=java.lang.Boolean)").build(), testRegion);
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
		digraph.connect(digraph.getRegion(SP1), digraph.createRegionFilterBuilder().allow(RegionFilter.VISIBLE_PACKAGE_NAMESPACE, "(" + RegionFilter.VISIBLE_PACKAGE_NAMESPACE + "=pkg1.*)").build(), digraph.getRegion(PP1));
		// PP2
		digraph.connect(digraph.getRegion(PP2), digraph.createRegionFilterBuilder().allow(CP1, "(name=" + CP1 + ")").build(), digraph.getRegion(CP1));
		// SP2
		digraph.connect(digraph.getRegion(SP2), digraph.createRegionFilterBuilder().allow(RegionFilter.VISIBLE_PACKAGE_NAMESPACE, "(" + RegionFilter.VISIBLE_PACKAGE_NAMESPACE + "=pkg2.*)").build(), digraph.getRegion(PP2));
		// CP2
		digraph.connect(digraph.getRegion(CP2), digraph.createRegionFilterBuilder().allow(RegionFilter.VISIBLE_PACKAGE_NAMESPACE, "(" + RegionFilter.VISIBLE_PACKAGE_NAMESPACE + "=pkg1.*)").build(), digraph.getRegion(PP1));
		digraph.connect(digraph.getRegion(CP2), digraph.createRegionFilterBuilder().allow(RegionFilter.VISIBLE_SERVICE_NAMESPACE, "(" + Constants.OBJECTCLASS + "=pkg1.*)").build(), digraph.getRegion(SP1));
		// PC1
		digraph.connect(digraph.getRegion(PC1), digraph.createRegionFilterBuilder().allow(RegionFilter.VISIBLE_PACKAGE_NAMESPACE, "(" + RegionFilter.VISIBLE_PACKAGE_NAMESPACE + "=pkg2.*)").build(), digraph.getRegion(PP2));
		// BC1
		digraph.connect(digraph.getRegion(BC1), digraph.createRegionFilterBuilder().allow(RegionFilter.VISIBLE_REQUIRE_NAMESPACE, "(" + RegionFilter.VISIBLE_REQUIRE_NAMESPACE + "=" + PP2 + ")").build(), digraph.getRegion(PP2));
		// SC1
		digraph.connect(digraph.getRegion(SC1), digraph.createRegionFilterBuilder().allow(RegionFilter.VISIBLE_PACKAGE_NAMESPACE, "(" + RegionFilter.VISIBLE_PACKAGE_NAMESPACE + "=pkg2.*)").build(), digraph.getRegion(PP2));
		digraph.connect(digraph.getRegion(SC1), digraph.createRegionFilterBuilder().allow(RegionFilter.VISIBLE_SERVICE_NAMESPACE, "(" + Constants.OBJECTCLASS + "=pkg2.*)").build(), digraph.getRegion(SP2));
		// CC1
		digraph.connect(digraph.getRegion(CC1), digraph.createRegionFilterBuilder().allow(CP2, "(name=" + CP2 + ")").build(), digraph.getRegion(CP2));

		bundleInstaller.resolveBundles(bundles.values().toArray(new Bundle[bundles.size()]));
		for (Bundle bundle : bundles.values()) {
			assertEquals("Bundle did not resolve: " + bundle.getSymbolicName(), Bundle.RESOLVED, bundle.getState());
			bundle.start();
		}
		BundleContext context = getContext();
		ServiceTracker<Boolean, Boolean> cp2Tracker = new ServiceTracker<Boolean, Boolean>(context, context.createFilter("(&(objectClass=java.lang.Boolean)(bundle.id=" + bundles.get(CP2).getBundleId() + "))"), null);
		ServiceTracker<Boolean, Boolean> sc1Tracker = new ServiceTracker<Boolean, Boolean>(context, context.createFilter("(&(objectClass=java.lang.Boolean)(bundle.id=" + bundles.get(SC1).getBundleId() + "))"), null);

		cp2Tracker.open();
		sc1Tracker.open();

		assertNotNull("The cp2 bundle never found the service.", cp2Tracker.waitForService(2000));
		assertNotNull("The sc1 bundle never found the service.", sc1Tracker.waitForService(2000));
		cp2Tracker.close();
		sc1Tracker.close();
	}

	public void testPersistence() throws BundleException, InvalidSyntaxException {
		// get the system region
		Region systemRegion = digraph.getRegion(0);
		Map<String, Bundle> bundles = new HashMap<String, Bundle>();
		// create a disconnected test region for each test bundle
		for (String location : ALL) {
			Region testRegion = digraph.createRegion(location);
			bundles.put(location, bundleInstaller.installBundle(location, testRegion));
			// Import the system bundle from the systemRegion
			digraph.connect(testRegion, digraph.createRegionFilterBuilder().allow(RegionFilter.VISIBLE_BUNDLE_NAMESPACE, "(id=0)").build(), systemRegion);
			// must import Boolean services into systemRegion to test
			digraph.connect(systemRegion, digraph.createRegionFilterBuilder().allow(RegionFilter.VISIBLE_SERVICE_NAMESPACE, "(objectClass=java.lang.Boolean)").build(), testRegion);
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

		regionBundle.stop();
		bundleInstaller.resolveBundles(bundles.values().toArray(new Bundle[bundles.size()]));
		assertEquals(PP1, Bundle.RESOLVED, bundles.get(PP1).getState());
		assertEquals(SP1, Bundle.RESOLVED, bundles.get(SP1).getState());
		assertEquals(CP1, Bundle.RESOLVED, bundles.get(CP1).getState());
		assertEquals(PP2, Bundle.RESOLVED, bundles.get(PP2).getState());
		assertEquals(SP2, Bundle.RESOLVED, bundles.get(SP2).getState());
		assertEquals(CP2, Bundle.RESOLVED, bundles.get(CP2).getState());
		assertEquals(BC1, Bundle.RESOLVED, bundles.get(BC1).getState());
		assertEquals(SC1, Bundle.RESOLVED, bundles.get(SC1).getState());
		assertEquals(CC1, Bundle.RESOLVED, bundles.get(CC1).getState());

		startRegionBundle();

		bundleInstaller.refreshPackages(bundles.values().toArray(new Bundle[bundles.size()]));
		assertEquals(PP1, Bundle.RESOLVED, bundles.get(PP1).getState());
		assertEquals(SP1, Bundle.INSTALLED, bundles.get(SP1).getState());
		assertEquals(CP1, Bundle.RESOLVED, bundles.get(CP1).getState());
		assertEquals(PP2, Bundle.INSTALLED, bundles.get(PP2).getState());
		assertEquals(SP2, Bundle.INSTALLED, bundles.get(SP2).getState());
		assertEquals(CP2, Bundle.INSTALLED, bundles.get(CP2).getState());
		assertEquals(BC1, Bundle.INSTALLED, bundles.get(BC1).getState());
		assertEquals(SC1, Bundle.INSTALLED, bundles.get(SC1).getState());
		assertEquals(CC1, Bundle.INSTALLED, bundles.get(CC1).getState());
	}

	public void testCyclicRegions0() throws BundleException, InvalidSyntaxException, InterruptedException {
		doCyclicRegions(0);
	}

	public void testCyclicRegions10() throws BundleException, InvalidSyntaxException, InterruptedException {
		doCyclicRegions(10);
	}

	public void testCyclicRegions100() throws BundleException, InvalidSyntaxException, InterruptedException {
		doCyclicRegions(100);
	}

	private void doCyclicRegions(int numLevels) throws BundleException, InvalidSyntaxException, InterruptedException {
		String regionName1 = getName() + "_1";
		String regionName2 = getName() + "_2";
		// get the system region
		Region systemRegion = digraph.getRegion(0);
		// create two regions to hold the bundles
		Region testRegion1 = digraph.createRegion(regionName1);
		Region testRegion2 = digraph.createRegion(regionName2);
		// connect to the system bundle
		testRegion1.connectRegion(systemRegion, digraph.createRegionFilterBuilder().allow(RegionFilter.VISIBLE_BUNDLE_NAMESPACE, "(id=0)").build());
		testRegion2.connectRegion(systemRegion, digraph.createRegionFilterBuilder().allow(RegionFilter.VISIBLE_BUNDLE_NAMESPACE, "(id=0)").build());
		// must import Boolean services into systemRegion to test
		systemRegion.connectRegion(testRegion1, digraph.createRegionFilterBuilder().allow(RegionFilter.VISIBLE_SERVICE_NAMESPACE, "(objectClass=java.lang.Boolean)").build());
		systemRegion.connectRegion(testRegion2, digraph.createRegionFilterBuilder().allow(RegionFilter.VISIBLE_SERVICE_NAMESPACE, "(objectClass=java.lang.Boolean)").build());

		Map<String, Bundle> bundles = new HashMap<String, Bundle>();
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
		testRegionFilter1.allow(RegionFilter.VISIBLE_PACKAGE_NAMESPACE, "(" + RegionFilter.VISIBLE_PACKAGE_NAMESPACE + "=pkg2.*)");
		// CP2 -> SP1
		testRegionFilter1.allow(RegionFilter.VISIBLE_SERVICE_NAMESPACE, "(" + Constants.OBJECTCLASS + "=pkg1.*)");
		// PC1 -> PP2
		// this is not needed because we already import pkg2.* above
		//testRegionFilter1.allow(RegionFilter.VISIBLE_PACKAGE_NAMESPACE, "(" + RegionFilter.VISIBLE_PACKAGE_NAMESPACE + "=pkg2.*)");
		// BC1 -> PP2
		testRegionFilter1.allow(RegionFilter.VISIBLE_REQUIRE_NAMESPACE, "(" + RegionFilter.VISIBLE_REQUIRE_NAMESPACE + "=" + PP2 + ")");

		RegionFilterBuilder testRegionFilter2 = digraph.createRegionFilterBuilder();
		//SP1 -> PP1
		testRegionFilter2.allow(RegionFilter.VISIBLE_PACKAGE_NAMESPACE, "(" + RegionFilter.VISIBLE_PACKAGE_NAMESPACE + "=pkg1.*)");
		//SC1 -> SP2
		testRegionFilter2.allow(RegionFilter.VISIBLE_SERVICE_NAMESPACE, "(" + Constants.OBJECTCLASS + "=pkg2.*)");
		//CC1 -> CP2
		testRegionFilter2.allow(CP2, "(name=" + CP2 + ")");

		Region r1, r2 = null;
		for (int i = 0; i <= numLevels; i++) {
			r1 = (i > 0) ? r2 : testRegion1;
			r2 = (i < numLevels) ? digraph.createRegion(getName() + "_level_" + i) : testRegion2;
			r1.connectRegion(r2, testRegionFilter1.build());
			r2.connectRegion(r1, testRegionFilter2.build());
		}

		bundleInstaller.resolveBundles(bundles.values().toArray(new Bundle[bundles.size()]));
		for (Bundle bundle : bundles.values()) {
			assertEquals("Bundle did not resolve: " + bundle.getSymbolicName(), Bundle.RESOLVED, bundle.getState());
			bundle.start();
		}
		BundleContext context = getContext();
		ServiceTracker<Boolean, Boolean> cp2Tracker = new ServiceTracker<Boolean, Boolean>(context, context.createFilter("(&(objectClass=java.lang.Boolean)(bundle.id=" + bundles.get(CP2).getBundleId() + "))"), null);
		ServiceTracker<Boolean, Boolean> sc1Tracker = new ServiceTracker<Boolean, Boolean>(context, context.createFilter("(&(objectClass=java.lang.Boolean)(bundle.id=" + bundles.get(SC1).getBundleId() + "))"), null);

		cp2Tracker.open();
		sc1Tracker.open();

		assertNotNull("The cp2 bundle never found the service.", cp2Tracker.waitForService(2000));
		assertNotNull("The sc1 bundle never found the service.", sc1Tracker.waitForService(2000));
		cp2Tracker.close();
		sc1Tracker.close();
	}

	private static final String REGION_DOMAIN_PROP = "org.eclipse.equinox.region.domain";

	public void testMbeans() throws MalformedObjectNameException, BundleException, InstanceNotFoundException, ReflectionException, MBeanException, AttributeNotFoundException {
		MBeanServer server = ManagementFactory.getPlatformMBeanServer();
		ObjectName digraphName = new ObjectName(REGION_DOMAIN_PROP + ":type=RegionDigraph");
		ObjectName regionNameAllQuery = new ObjectName(REGION_DOMAIN_PROP + ":type=Region,name=*");
		Set<ObjectInstance> digraphs = server.queryMBeans(digraphName, null);
		assertEquals("Expected only one instance of digraph", 1, digraphs.size());
		Set<ObjectInstance> regions = server.queryMBeans(null, regionNameAllQuery);
		assertEquals("Expected only one instance of region", 1, regions.size());

		Region pp1Region = digraph.createRegion(PP1);
		Bundle pp1Bundle = bundleInstaller.installBundle(PP1, pp1Region);
		Region sp1Region = digraph.createRegion(SP1);
		Bundle sp1Bundle = bundleInstaller.installBundle(SP1, sp1Region);

		regions = server.queryMBeans(null, regionNameAllQuery);
		assertEquals("Wrong number of regions", 3, regions.size());

		Set<ObjectInstance> pp1Query = server.queryMBeans(null, new ObjectName(REGION_DOMAIN_PROP + ":type=Region,name=" + PP1));
		assertEquals("Expected only one instance of: " + PP1, 1, pp1Query.size());
		Set<ObjectInstance> sp1Query = server.queryMBeans(null, new ObjectName(REGION_DOMAIN_PROP + ":type=Region,name=" + SP1));
		assertEquals("Expected only one instance of: " + SP1, 1, sp1Query.size());
		ObjectName pp1Name = (ObjectName) server.invoke(digraphs.iterator().next().getObjectName(), "getRegion", new Object[] {PP1}, new String[] {String.class.getName()});
		assertEquals(PP1 + " regions not equal.", pp1Query.iterator().next().getObjectName(), pp1Name);
		ObjectName sp1Name = (ObjectName) server.invoke(digraphs.iterator().next().getObjectName(), "getRegion", new Object[] {SP1}, new String[] {String.class.getName()});
		assertEquals(SP1 + " regions not equal.", sp1Query.iterator().next().getObjectName(), sp1Name);

		// test non existing region
		ObjectName shouldNotExistName = (ObjectName) server.invoke(digraphs.iterator().next().getObjectName(), "getRegion", new Object[] {"ShouldNotExist"}, new String[] {String.class.getName()});
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

		regionBundle.stop();

		// Now make sure we have no mbeans
		digraphs = server.queryMBeans(digraphName, null);
		assertEquals("Wrong number of digraphs", 0, digraphs.size());
		regions = server.queryMBeans(null, regionNameAllQuery);
		assertEquals("Wrong number of regions", 0, regions.size());
	}
}
