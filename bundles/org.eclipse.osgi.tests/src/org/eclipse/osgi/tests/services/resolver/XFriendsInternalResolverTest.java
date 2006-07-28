/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.services.resolver;

import java.util.*;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.*;

public class XFriendsInternalResolverTest extends AbstractStateTest {
	public static Test suite() {
		return new TestSuite(XFriendsInternalResolverTest.class);
	}

	public XFriendsInternalResolverTest(String name) {
		super(name);
	}

	/**
	 * Tests the x-friends directive.  A bundle should not be allowed to import a package which
	 * declares an x-friends directive and the importer is not a friend.  When a bundle requires
	 * anther bundle which exports packages which declare an x-friends directive it should not 
	 * have access to the packages unless the requiring bundle is a friend.
	 * @throws BundleException
	 */
	public void testXFriends() throws BundleException {
		State state = buildEmptyState();
		int bundleID = 0;
		// test the selection algorithm of the resolver to pick the bundles which
		// resolve the largest set of bundles; with fragments using Import-Package
		Hashtable manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "test.exporter");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.EXPORT_PACKAGE, 
				"test.exporter.foo1; x-friends:=\"test.importer1, test.requirer1\"," +
				"test.exporter.foo2; x-friends:=\"test.importer2, test.requirer2\"," +
				"test.exporter.bar1; x-friends:=\"test.importer1, test.requirer1\"," +
				"test.exporter.bar2; x-friends:=\"test.importer2, test.requirer2\"");
		BundleDescription testExporter = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "test.importer1");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.IMPORT_PACKAGE, 
				"test.exporter.foo1," +
				"test.exporter.bar1," +
				"test.exporter.foo2; resolution:=optional," +
				"test.exporter.bar2; resolution:=optional");
		BundleDescription testImporter1 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "test.importer2");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.IMPORT_PACKAGE, 
				"test.exporter.foo1; resolution:=optional," +
				"test.exporter.bar1; resolution:=optional," +
				"test.exporter.foo2," +
				"test.exporter.bar2");
		BundleDescription testImporter2 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "test.importer3");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.IMPORT_PACKAGE, 
				"test.exporter.foo1," +
				"test.exporter.bar1," +
				"test.exporter.foo2; resolution:=optional," +
				"test.exporter.bar2; resolution:=optional");
		BundleDescription testImporter3 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "test.requirer1");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.REQUIRE_BUNDLE, "test.exporter");
		BundleDescription testRequirer1 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "test.requirer2");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.REQUIRE_BUNDLE, "test.exporter");
		BundleDescription testRequirer2 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		Dictionary[] props = new Dictionary[] {new Hashtable()};
		props[0].put("osgi.resolverMode", "strict");

		state.setPlatformProperties(props);
		state.addBundle(testExporter);
		state.addBundle(testImporter1);
		state.addBundle(testImporter2);
		state.addBundle(testImporter3);
		state.addBundle(testRequirer1);
		state.addBundle(testRequirer2);
		state.resolve();

		// make sure all bundles are resolved that should be
		assertTrue("1.0", testExporter.isResolved());
		assertTrue("1.1", testImporter1.isResolved());
		assertTrue("1.2", testImporter2.isResolved());
		assertFalse("1.3", testImporter3.isResolved()); // should not be resolved
		assertTrue("1.4", testRequirer1.isResolved());
		assertTrue("1.5", testRequirer2.isResolved());

		String[] validPackages1 = {"test.exporter.foo1", "test.exporter.bar1"};
		String[] validPackages2 = {"test.exporter.foo2", "test.exporter.bar2"};
		// make sure the importers only got the packages they are really friends to
		ExportPackageDescription[] imported1 = testImporter1.getResolvedImports();
		assertTrue("2.0", imported1 != null && imported1.length == 2); // should only have 2 resolved imports
		assertTrue("2.1", contains(validPackages1, imported1[0].getName()));
		assertTrue("2.2", contains(validPackages1, imported1[1].getName()));

		ExportPackageDescription[] imported2 = testImporter2.getResolvedImports();
		assertTrue("3.0", imported2 != null && imported2.length == 2); // should only have 2 resolved imports
		assertTrue("3.1", contains(validPackages2, imported2[0].getName()));
		assertTrue("3.2", contains(validPackages2, imported2[1].getName()));

		StateHelper helper = state.getStateHelper();
		ExportPackageDescription[] required1 = helper.getVisiblePackages(testRequirer1);
		assertTrue("4.0", required1 != null && required1.length == 2); // should only have 2 visible imports
		assertTrue("4.1", contains(validPackages1, required1[0].getName()));
		assertTrue("4.2", contains(validPackages1, required1[1].getName()));

		ExportPackageDescription[] required2 = helper.getVisiblePackages(testRequirer2);
		assertTrue("5.0", required2 != null && required2.length == 2); // should only have 2 visible imports
		assertTrue("5.1", contains(validPackages2, required2[0].getName()));
		assertTrue("5.2", contains(validPackages2, required2[1].getName()));
	}

	private ExportPackageDescription[] isConsistent(ExportPackageDescription[] pkgs1, ExportPackageDescription[] pkgs2) {
		for (int i = 0; i < pkgs1.length; i++)
			for (int j = 0; j < pkgs2.length; j++)
				if (pkgs1[i].getName().equals(pkgs2[j].getName()) && pkgs1[i] != pkgs2[j])
					return new ExportPackageDescription[] {pkgs1[i], pkgs2[j]}; 
		return null;
	}

	private boolean contains(Object[] array, Object element) {
		for (int i = 0; i < array.length; i++)
			if (array[i].equals(element))
				return true;
		return false;
	}
}

