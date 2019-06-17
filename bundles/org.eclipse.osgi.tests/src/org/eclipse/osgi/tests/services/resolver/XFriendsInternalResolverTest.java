/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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

	public void testVisiblePackages001() throws BundleException {
		State state = buildEmptyState();
		int bundleID = 0;

		Hashtable manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "a.base");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.EXPORT_PACKAGE, "a.split.pkg; a.base=split; mandatory:=a.base");
		BundleDescription aBase = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "a.extra");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.REQUIRE_BUNDLE, "a.base");
		manifest.put(Constants.EXPORT_PACKAGE, "a.split.pkg");
		BundleDescription aExtra = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "b");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.REQUIRE_BUNDLE, "a.extra");
		manifest.put(Constants.EXPORT_PACKAGE, "test.base.exporter.require");
		BundleDescription b= state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		state.addBundle(aBase);
		state.addBundle(aExtra);
		state.addBundle(b);
		state.resolve();

		// make sure all bundles are resolved that should be
		assertTrue("1.0", aBase.isResolved());
		assertTrue("1.1", aExtra.isResolved());
		assertTrue("1.2", b.isResolved());

		StateHelper helper = state.getStateHelper();
		ExportPackageDescription[] visImporter = helper.getVisiblePackages(b);
		assertTrue("2.0", visImporter != null && visImporter.length == 2);
		assertEquals("2.1", visImporter[0].getName(), "a.split.pkg");
		assertEquals("2.2", visImporter[1].getName(), "a.split.pkg");
		BundleDescription exporter1 = visImporter[0].getExporter();
		BundleDescription exporter2 = visImporter[1].getExporter();
		assertTrue("2.3", exporter1 != exporter2);
		assertTrue("2.4", exporter1 == aBase || exporter1 == aExtra);
		assertTrue("2.4", exporter2 == aBase || exporter2 == aExtra);
	}

	public void testVisiblePackages002() throws BundleException {
		State state = buildEmptyState();
		int bundleID = 0;

		Hashtable manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "test.base.exporter");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.EXPORT_PACKAGE, 
				"test.base; base.exporter=split; mandatory:=base.exporter");
		BundleDescription baseExporter = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "test.base.exporter.require");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.REQUIRE_BUNDLE, "test.base.exporter");
		manifest.put(Constants.EXPORT_PACKAGE, 
				"test.base; base.exporter.require=split; mandatory:=base.exporter.require");
		BundleDescription baseExporterRequire = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "test.base.exporter.require2");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.REQUIRE_BUNDLE, 
				"test.base.exporter.require");
		manifest.put(Constants.EXPORT_PACKAGE, "test.base.exporter.require");
		BundleDescription baseExporterRequire2 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);


		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "test.base.importer");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.REQUIRE_BUNDLE, 
				"test.base.exporter.require");
		BundleDescription baseImporter = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "test.base.importer2");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.REQUIRE_BUNDLE, "test.base.exporter.require2");
		manifest.put(Constants.EXPORT_PACKAGE, "test.base");
		BundleDescription baseImporter2 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "test.base.importer3");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.IMPORT_PACKAGE, "test.base; bundle-symbolic-name=test.base.importer2");
		BundleDescription baseImporter3 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		Dictionary[] props = new Dictionary[] {new Hashtable()};
		props[0].put("osgi.resolverMode", "strict");

		state.setPlatformProperties(props);
		state.addBundle(baseExporter);
		state.addBundle(baseExporterRequire);
		state.addBundle(baseExporterRequire2);
		state.addBundle(baseImporter);
		state.addBundle(baseImporter2);
		state.addBundle(baseImporter3);
		state.resolve();

		// make sure all bundles are resolved that should be
		assertTrue("1.0", baseExporter.isResolved());
		assertTrue("1.1", baseExporterRequire.isResolved());
		assertTrue("1.2", baseExporterRequire2.isResolved());
		assertTrue("1.3", baseImporter.isResolved());
		assertTrue("1.4", baseImporter2.isResolved());
		assertTrue("1.5", baseImporter3.isResolved());

		StateHelper helper = state.getStateHelper();
		ExportPackageDescription[] visImporter = helper.getVisiblePackages(baseImporter);
		assertTrue("2.0", visImporter != null && visImporter.length == 2);
		assertEquals("2.1", visImporter[0].getName(), "test.base");
		assertEquals("2.2", visImporter[1].getName(), "test.base");
		BundleDescription exporter1 = visImporter[0].getExporter();
		BundleDescription exporter2 = visImporter[1].getExporter();
		assertTrue("2.3", exporter1 != exporter2);
		assertTrue("2.4", exporter1 == baseExporter || exporter1 == baseExporterRequire);
		assertTrue("2.5", exporter2 == baseExporter || exporter2 == baseExporterRequire);

		ExportPackageDescription[] visImporter2 = helper.getVisiblePackages(baseImporter2);
		assertTrue("3.0", visImporter2 != null && visImporter2.length == 1);
		assertEquals("3.1", visImporter2[0].getName(), "test.base.exporter.require");
		assertTrue("3.2", visImporter2[0].getExporter() == baseExporterRequire2);

		ExportPackageDescription[] visImporter3 = helper.getVisiblePackages(baseImporter3);
		assertTrue("4.0", visImporter3 != null && visImporter3.length == 1);
		assertEquals("4.1", visImporter3[0].getName(), "test.base");
		assertTrue("4.2", visImporter3[0].getExporter() == baseImporter2);
	}

	public void testVisiblePackages003() throws BundleException {
		State state = buildEmptyState();
		int bundleID = 0;

		Hashtable manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "E");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.EXPORT_PACKAGE, "test.base; E=split; mandatory:=E");
		BundleDescription e = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "D");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.REQUIRE_BUNDLE, "E");
		BundleDescription d = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "C");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.REQUIRE_BUNDLE, "D");
		manifest.put(Constants.EXPORT_PACKAGE, "test.base; D=split; mandatory:=D");
		BundleDescription c = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.REQUIRE_BUNDLE, "F; " + Constants.VISIBILITY_DIRECTIVE + ":=" + Constants.VISIBILITY_REEXPORT );
		BundleDescription b = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "F");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.EXPORT_PACKAGE, "test.base; F=split; mandatory:=F");
		BundleDescription f = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.REQUIRE_BUNDLE, "B; " + Constants.VISIBILITY_DIRECTIVE + ":=" + Constants.VISIBILITY_REEXPORT + 
																	",C");
		BundleDescription a = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		Dictionary[] props = new Dictionary[] {new Hashtable()};
		props[0].put("osgi.resolverMode", "strict");

		state.setPlatformProperties(props);
		state.addBundle(f);
		state.addBundle(e);
		state.addBundle(d);
		state.addBundle(c);
		state.addBundle(b);
		state.addBundle(a);

		state.resolve();

		// make sure all bundles are resolved that should be
		assertTrue("1.0", a.isResolved());
		assertTrue("1.1", b.isResolved());
		assertTrue("1.2", c.isResolved());
		assertTrue("1.3", d.isResolved());
		assertTrue("1.4", e.isResolved());
		assertTrue("1.5", f.isResolved());

		StateHelper helper = state.getStateHelper();
		ExportPackageDescription[] visImporter = helper.getVisiblePackages(a);
		assertTrue("2.0", visImporter != null && visImporter.length == 2);
		assertEquals("2.1", visImporter[0].getName(), "test.base");
		assertEquals("2.2", visImporter[1].getName(), "test.base");
		BundleDescription exporter1 = visImporter[0].getExporter();
		BundleDescription exporter2 = visImporter[1].getExporter();
		assertTrue("2.3", exporter1 != exporter2);
		assertTrue("2.4", exporter1 == c || exporter1 == f);
		assertTrue("2.5", exporter2 == c || exporter2 == f);
	}

	private boolean contains(Object[] array, Object element) {
		for (Object o : array) {
			if (o.equals(element)) {
				return true;
			}
		}
		return false;
	}
}

