/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
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
import org.eclipse.core.runtime.adaptor.testsupport.SimplePlatformAdmin;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.tests.OSGiTest;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.osgi.framework.BundleException;

public abstract class AbstractStateTest extends OSGiTest {
	protected PlatformAdmin platformAdmin;

	protected void setUp() throws Exception {
		super.setUp();
		platformAdmin = new SimplePlatformAdmin(getRandomLocation().toFile(), OSGiTestsActivator.getContext());
	}

	public AbstractStateTest(String testName) {
		super(testName);
	}

	public void assertContains(String tag, Object[] array, Object element) {
		for (int i = 0; i < array.length; i++)
			if (array[i] == element)
				return;
		fail(tag);
	}

	public void assertEquals(State original, State copy) {
		assertEquals("", original, copy);
	}

	public void assertEquals(String tag, BundleDescription original, BundleDescription copy) {
		if (original == copy)
			return;
		assertEquals(tag + ".0", original.getBundleId(), copy.getBundleId());
		assertEquals(tag + ".1", original.getSymbolicName(), copy.getSymbolicName());
		assertEquals(tag + ".2", original.getVersion(), copy.getVersion());
		assertEquals(tag + ".3", original.getLocation(), copy.getLocation());
		assertEquals(tag + ".4", original.isResolved(), copy.isResolved());
		assertEquals(tag + ".5", original.getHost(), copy.getHost());
		ExportPackageDescription[] originalExportPackages = original.getExportPackages();
		ExportPackageDescription[] copyExportPackages = copy.getExportPackages();
		assertEquals(tag + ".6", originalExportPackages.length, copyExportPackages.length);
		for (int i = 0; i < originalExportPackages.length; i++)
			assertEquals(tag + ".7." + i, originalExportPackages[i], copyExportPackages[i]);
		ImportPackageSpecification[] originalImportPackages = original.getImportPackages();
		ImportPackageSpecification[] copyImportPackages = copy.getImportPackages();
		assertEquals(tag + ".8", originalImportPackages.length, copyImportPackages.length);
		for (int i = 0; i < originalImportPackages.length; i++)
			assertEquals(tag + ".9." + i, originalImportPackages[i], copyImportPackages[i]);
		BundleSpecification[] originalRequiredBundles = original.getRequiredBundles();
		BundleSpecification[] copyRequiredBundles = copy.getRequiredBundles();
		assertEquals(tag + ".10", originalRequiredBundles.length, copyRequiredBundles.length);
		for (int i = 0; i < originalRequiredBundles.length; i++)
			assertEquals(tag + ".11." + i, originalRequiredBundles[i], copyRequiredBundles[i]);
		ExportPackageDescription[] originalResolvedImports = original.getResolvedImports();
		ExportPackageDescription[] copyResolvedImports = copy.getResolvedImports();
		assertEquals(tag + ".12", originalResolvedImports.length, copyResolvedImports.length);
		for (int i = 0; i < originalResolvedImports.length; i++)
			assertEquals(tag + ".13." + i, originalResolvedImports[i], copyResolvedImports[i]);
		BundleDescription[] originalResolvedRequires = original.getResolvedRequires();
		BundleDescription[] copyResolvedRequires = copy.getResolvedRequires();
		assertEquals(tag + ".14", originalResolvedRequires.length, copyResolvedRequires.length);
		for (int i = 0; i < originalResolvedRequires.length; i++)
			assertEquals(tag + ".15." + i, originalResolvedRequires[i], copyResolvedRequires[i]);
	}

	public void assertEquals(String tag, ExportPackageDescription original, ExportPackageDescription copy) {
		assertEquals(tag + ".0", original.getName(), copy.getName());
		assertEquals(tag + ".1", original.getVersion(), copy.getVersion());
		assertEquals(tag + ".2", original.getAttributes(), copy.getAttributes());
		assertEquals(tag + ".3", original.getExclude(), copy.getExclude());
		assertEquals(tag + ".4", original.getUses(), copy.getUses());
		assertEquals(tag + ".5", original.getInclude(), copy.getInclude());
		assertEquals(tag + ".6", original.getMandatory(), copy.getMandatory());
	}

	public void assertEquals(String tag, State original, State copy) {
		BundleDescription[] originalBundles = original.getBundles();
		BundleDescription[] copyBundles = copy.getBundles();
		assertEquals(tag + ".1", originalBundles.length, copyBundles.length);
		for (int i = 0; i < originalBundles.length; i++)
			assertEquals(tag + ".2." + i, originalBundles[i], copyBundles[i]);
		assertEquals(tag + ".3", original.isResolved(), copy.isResolved());
		BundleDescription[] originalResolvedBundles = original.getResolvedBundles();
		BundleDescription[] copyResolvedBundles = copy.getResolvedBundles();
		assertEquals(tag + ".4", originalResolvedBundles.length, copyResolvedBundles.length);
		for (int i = 0; i < originalResolvedBundles.length; i++)
			assertEquals(tag + ".5." + i, originalResolvedBundles[i], copyResolvedBundles[i]);
	}

	private void assertVersionConstraintEquals(String tag, VersionConstraint original, VersionConstraint copy) {
		assertEquals(tag + ".0", original == null, copy == null);
		if (original == null)
			return;
		assertEquals(tag + ".1", original.getName(), copy.getName());
		assertEquals(tag + ".2", original.getVersionRange(), copy.getVersionRange());
		assertEquals(tag + ".4", original.getSupplier() == null, copy.getSupplier() == null);
		if (original.getSupplier() != null) {
			Object o = original.getSupplier();
			if (o instanceof BundleDescription)
				assertEquals(tag + ".5", (BundleDescription)original.getSupplier(), (BundleDescription)copy.getSupplier());
			else
				assertEquals(tag + ".5", (ExportPackageDescription)original.getSupplier(), (ExportPackageDescription)copy.getSupplier());
		}
	}

	public void assertEquals(String tag, BundleSpecification original, BundleSpecification copy) {
		assertVersionConstraintEquals(tag + ".0", original, copy);
		if (original == null)
			return;
		assertEquals(tag + ".1", original.isExported(), copy.isExported());
		assertEquals(tag + ".2", original.isOptional(), copy.isOptional());
	}

	public void assertEquals(String tag, ImportPackageSpecification original, ImportPackageSpecification copy) {
		assertVersionConstraintEquals(tag + ".0", original, copy);
		if (original == null)
			return;
		assertEquals(tag + ".1", original.getAttributes(), copy.getAttributes());
		assertEquals(tag + ".2", original.getBundleSymbolicName(), copy.getBundleSymbolicName());
		assertEquals(tag + ".3", original.getBundleVersionRange(), copy.getBundleVersionRange());
		assertEquals(tag + ".5", original.getResolution(), original.getResolution());
	}

	public void assertEquals(String tag, HostSpecification original, HostSpecification copy) {
		assertVersionConstraintEquals(tag + ".0", original, copy);
		if (original == null)
			return;
		BundleDescription[] originalHosts = original.getHosts();
		BundleDescription[] copyHosts = copy.getHosts();
		assertEquals(tag + ".1", originalHosts == null, copyHosts == null);
		if (originalHosts == null)
			return;
		assertEquals(tag + ".2", originalHosts.length, copyHosts.length);
		for (int i = 0; i < originalHosts.length; i++)
			assertEquals(tag + ".3." + i, originalHosts[i], copyHosts[i]);
	}

	public void assertFullyResolved(String tag, BundleDescription bundle) {
		assertTrue(tag + "a", bundle.isResolved());
		ImportPackageSpecification[] packages = bundle.getImportPackages();
		for (int i = 0; i < packages.length; i++)
			assertNotNull(tag + "b_" + i, packages[i].getSupplier());
		HostSpecification host = bundle.getHost();
		if (host != null)
			assertNotNull(tag + "c", host.getHosts());
		BundleSpecification[] requiredBundles = bundle.getRequiredBundles();
		for (int i = 0; i < requiredBundles.length; i++)
			assertNotNull(tag + "d_" + i, requiredBundles[i].getSupplier());
	}

	public void assertFullyUnresolved(String tag, BundleDescription bundle) {
		assertFalse(tag + "a", bundle.isResolved());
		ImportPackageSpecification[] packages = bundle.getImportPackages();
		for (int i = 0; i < packages.length; i++)
			assertNull(tag + "b_" + i, packages[i].getSupplier());
		HostSpecification host = bundle.getHost();
		if (host != null)
			assertNull(tag + "c", host.getHosts());
		BundleSpecification[] requiredBundles = bundle.getRequiredBundles();
		for (int i = 0; i < requiredBundles.length; i++)
			assertNull(tag + "d_" + i, requiredBundles[i].getSupplier());
	}

	public void assertIdentical(String tag, State original, State copy) {
		assertEquals(tag + ".0a", original.isResolved(), copy.isResolved());
		assertEquals(tag + ".0b", original.getTimeStamp(), copy.getTimeStamp());
		assertEquals(tag, original, copy);
	}

	public State buildComplexState() throws BundleException {
		State state = buildEmptyState();
		/*
		 * org.eclipse.b1_1.0 exports org.eclipse.p1_1.0 imports org.eclipse.p2
		 */
		final String B1_LOCATION = "org.eclipse.b1";
		final String B1_MANIFEST = "Bundle-SymbolicName: org.eclipse.b1\n" + "Bundle-Version: 1.0\n" + "Export-Package: org.eclipse.p1;specification-version=1.0\n" + "Import-Package: org.eclipse.p2";
		BundleDescription b1 = state.getFactory().createBundleDescription(parseManifest(B1_MANIFEST), B1_LOCATION, 1);
		state.addBundle(b1);
		/*
		 * org.eclipse.b2_2.0 exports org.eclipse.p2 imports org.eclipse.p1
		 */
		final String B2_LOCATION = "org.eclipse.b2";
		final String B2_MANIFEST = "Bundle-SymbolicName: org.eclipse.b2\n" + "Bundle-Version: 2.0\n" + "Export-Package: org.eclipse.p2\n" + "Import-Package: org.eclipse.p1";
		BundleDescription b2 = state.getFactory().createBundleDescription(parseManifest(B2_MANIFEST), B2_LOCATION, 2);
		state.addBundle(b2);
		/*
		 * org.eclipse.b3_2.0 exports org.eclipse.p2_2.0
		 */
		final String B3_LOCATION = "org.eclipse.b3";
		final String B3_MANIFEST = "Bundle-SymbolicName: org.eclipse.b3\n" + "Bundle-Version: 2.0\n" + "Export-Package: org.eclipse.p2; specification-version=2.0";
		BundleDescription b3 = state.getFactory().createBundleDescription(parseManifest(B3_MANIFEST), B3_LOCATION, 3);
		state.addBundle(b3);
		/*
		 * org.eclipse.b4_1.0 requires org.eclipse.b1_*
		 */
		final String B4_LOCATION = "org.eclipse.b4";
		final String B4_MANIFEST = "Bundle-SymbolicName: org.eclipse.b4\n" + "Bundle-Version: 2.0\n" + "Require-Bundle: org.eclipse.b1";
		BundleDescription b4 = state.getFactory().createBundleDescription(parseManifest(B4_MANIFEST), B4_LOCATION, 4);
		state.addBundle(b4);
		/*
		 * org.eclipse.b5_1.0 fragment for org.eclipse.b3_*
		 */
		final String B5_LOCATION = "org.eclipse.b5";
		final String B5_MANIFEST = "Bundle-SymbolicName: org.eclipse.b5\n" + "Bundle-Version: 1.0\n" + "Fragment-Host: org.eclipse.b3";
		BundleDescription b5 = state.getFactory().createBundleDescription(parseManifest(B5_MANIFEST), B5_LOCATION, 5);
		state.addBundle(b5);
		/*
		 * org.eclipse.b6_1.0 requires org.eclipse.b4
		 */
		final String B6_LOCATION = "org.eclipse.b6";
		final String B6_MANIFEST = "Bundle-SymbolicName: org.eclipse.b6\n" + "Bundle-Version: 1.0\n" + "Require-Bundle: org.eclipse.b4";
		BundleDescription b6 = state.getFactory().createBundleDescription(parseManifest(B6_MANIFEST), B6_LOCATION, 6);
		state.addBundle(b6);
		return state;
	}

	public State buildEmptyState() {
		State state = platformAdmin.getState();
		state.setResolver(platformAdmin.getResolver());
		return state;
	}

	public State buildInitialState() throws BundleException {
		State state = buildEmptyState();
		/*
		 * org.eclipse.b1_1.0 exports org.eclipse.p1_1.0
		 */
		final String SYSTEM_BUNDLE_LOCATION = "org.eclipse.b1";
		final String SYSTEM_BUNDLE_MANIFEST = "Bundle-SymbolicName: org.osgi.framework\n" + "Bundle-Version: 3.0\n" + "Export-Package: org.osgi.framework; specification-version=3.0";
		BundleDescription b0 = state.getFactory().createBundleDescription(parseManifest(SYSTEM_BUNDLE_MANIFEST), SYSTEM_BUNDLE_LOCATION, 0);
		state.addBundle(b0);
		return state;
	}

	public State buildSimpleState() throws BundleException {
		State state = buildEmptyState();
		/*
		 * org.eclipse.b1_1.0 exports org.eclipse.p1_1.0 imports org.eclipse.p2
		 */
		final String B1_LOCATION = "org.eclipse.b1";
		final String B1_MANIFEST = "Bundle-SymbolicName: org.eclipse.b1\n" + "Bundle-Version: 1.0\n" + "Export-Package: org.eclipse.p1;specification-version=1.0\n" + "Import-Package: org.eclipse.p2";
		BundleDescription b1 = state.getFactory().createBundleDescription(parseManifest(B1_MANIFEST), B1_LOCATION, 1);
		state.addBundle(b1);
		/*
		 * org.eclipse.b2_2.0 exports org.eclipse.p2 imports org.eclipse.p1
		 */
		final String B2_LOCATION = "org.eclipse.b2";
		final String B2_MANIFEST = "Bundle-SymbolicName: org.eclipse.b2\n" + "Bundle-Version: 2.0\n" + "Export-Package: org.eclipse.p2\n" + "Import-Package: org.eclipse.p1";
		BundleDescription b2 = state.getFactory().createBundleDescription(parseManifest(B2_MANIFEST), B2_LOCATION, 2);
		state.addBundle(b2);
		/*
		 * org.eclipse.b3_2.0 imports org.eclipse.p1_2.0
		 */
		final String B3_LOCATION = "org.eclipse.b3";
		final String B3_MANIFEST = "Bundle-SymbolicName: org.eclipse.b3\n" + "Bundle-Version: 2.0\n" + "Import-Package: org.eclipse.p1; specification-version=2.0";
		BundleDescription b3 = state.getFactory().createBundleDescription(parseManifest(B3_MANIFEST), B3_LOCATION, 3);
		state.addBundle(b3);
		return state;
	}

	public static Dictionary parseManifest(String manifest) {
		Dictionary entries = new Hashtable();
		StringTokenizer tokenizer = new StringTokenizer(manifest, ":\n");
		while (tokenizer.hasMoreTokens()) {
			String key = tokenizer.nextToken();
			String value = tokenizer.hasMoreTokens() ? tokenizer.nextToken().trim() : "";
			entries.put(key, value);
		}
		return entries;
	}
}
