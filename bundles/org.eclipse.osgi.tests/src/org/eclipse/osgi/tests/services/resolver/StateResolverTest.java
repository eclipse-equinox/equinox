/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
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
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public class StateResolverTest extends AbstractStateTest {
	public static Test suite() {
		return new TestSuite(StateResolverTest.class);
	}

	public StateResolverTest(String name) {
		super(name);
	}

	/**
	 * Tests adding 3 new bundles to an already resolved state and then
	 * resolving only one of the bundles. The result should be all 3 added
	 * bundles being resolved.
	 *  
	 */
	public void testAdd3Resolve1() throws BundleException {
		State state = buildInitialState();
		StateDelta delta = state.resolve();
		BundleDelta[] deltas = delta.getChanges();
		BundleDescription b0 = state.getBundle(0);
		assertNotNull("0.1", b0);
		assertFullyResolved("0.2", b0);
		Hashtable manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "org.eclipse.b1");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.IMPORT_PACKAGE, "org.osgi.framework");
		manifest.put(Constants.EXPORT_PACKAGE, "org.eclipse.p1; specification-version=1.0");
		BundleDescription b1 = null;
		try {
			b1 = state.getFactory().createBundleDescription(manifest, "org.eclipse.b1", 1);
			state.removeBundle(1);
			state.addBundle(b1);
		} catch (BundleException e) {
			fail("Failed to create BundleDescription: " + e.getMessage());
		}
		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "org.eclipse.b2");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.IMPORT_PACKAGE, "org.osgi.framework,org.eclipse.p1");
		manifest.put(Constants.EXPORT_PACKAGE, "org.eclipse.p2; specification-version=1.0");
		BundleDescription b2 = null;
		try {
			b2 = state.getFactory().createBundleDescription(manifest, "org.eclipse.b2", 2);
			state.removeBundle(2);
			state.addBundle(b2);
		} catch (BundleException e) {
			fail("Failed to create BundleDescription: " + e.getMessage());
		}
		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "org.eclipse.b3");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.IMPORT_PACKAGE, "org.osgi.framework,org.eclipse.p2");
		manifest.put(Constants.EXPORT_PACKAGE, "org.eclipse.p3; specification-version=1.0");
		BundleDescription b3 = null;
		try {
			b3 = state.getFactory().createBundleDescription(manifest, "org.eclipse.b3", 3);
			state.removeBundle(3);
			state.addBundle(b3);
		} catch (BundleException e) {
			fail("Failed to create BundleDescription: " + e.getMessage());
		}
		delta = state.resolve(new BundleDescription[] {state.getBundle(1)});
		deltas = delta.getChanges();
		assertEquals("1.0", 3, deltas.length);
		Map deltasMap = new HashMap();
		for (int i = 0; i < deltas.length; i++)
			deltasMap.put(new Long(deltas[i].getBundle().getBundleId()), deltas[i]);
		assertNotNull("1.1", deltasMap.get(new Long(1)));
		assertNotNull("1.2", deltasMap.get(new Long(2)));
		assertNotNull("1.3", deltasMap.get(new Long(3)));
		assertEquals("2.1", (BundleDelta.RESOLVED | BundleDelta.ADDED), ((BundleDelta) deltasMap.get(new Long(1))).getType());
		assertEquals("2.2", (BundleDelta.RESOLVED | BundleDelta.ADDED), ((BundleDelta) deltasMap.get(new Long(2))).getType());
		assertEquals("2.3", (BundleDelta.RESOLVED | BundleDelta.ADDED), ((BundleDelta) deltasMap.get(new Long(3))).getType());
	}

	public void testBasicResolution() throws BundleException {
		State state = buildSimpleState();
		StateDelta delta = state.resolve();
		BundleDescription b1 = state.getBundle(1);
		BundleDescription b2 = state.getBundle(2);
		BundleDescription b3 = state.getBundle(3);
		assertNotNull("0.1", b1);
		assertNotNull("0.2", b2);
		assertNotNull("0.3", b3);
		BundleDelta[] changes = delta.getChanges();
		assertEquals("1.0", 3, changes.length);
		BundleDelta[] additions = delta.getChanges(BundleDelta.ADDED, false);
		assertEquals("2.0", 3, additions.length);
		BundleDelta[] resolutions = delta.getChanges(BundleDelta.RESOLVED, false);
		assertEquals("3.0", 2, resolutions.length);
		Map deltasMap = new HashMap();
		for (int i = 0; i < resolutions.length; i++)
			deltasMap.put(resolutions[i].getBundle().getSymbolicName(), resolutions[i]);
		assertNotNull("3.1", deltasMap.get(b1.getSymbolicName()));
		assertNotNull("3.2", deltasMap.get(b2.getSymbolicName()));
		// TODO why do we expect unresolved deltas here when the bundle was not resolved in the first place?
		//BundleDelta[] unresolutions = delta.getChanges(BundleDelta.UNRESOLVED, false);
		//assertEquals("4.0", 1, unresolutions.length);
		//assertEquals("4.1", unresolutions[0].getBundle(), b3);
		assertFullyResolved("5.1", b1);
		assertFullyResolved("5.2", b2);
		assertFullyUnresolved("5.3", b3);
	}

	public void testComplexResolution() throws BundleException {
		State state = buildComplexState();
		StateDelta delta = state.resolve();
		BundleDescription b1 = state.getBundle("org.eclipse.b1", Version.parseVersion("1.0"));
		BundleDescription b2 = state.getBundle("org.eclipse.b2", Version.parseVersion("2.0"));
		BundleDescription b3 = state.getBundle("org.eclipse.b3", Version.parseVersion("2.0"));
		BundleDescription b4 = state.getBundle("org.eclipse.b4", Version.parseVersion("2.0"));
		BundleDescription b5 = state.getBundle("org.eclipse.b5", Version.parseVersion("1.0"));
		BundleDescription b6 = state.getBundle("org.eclipse.b6", Version.parseVersion("1.0"));
		assertNotNull("0.1", b1);
		assertNotNull("0.2", b2);
		assertNotNull("0.3", b3);
		assertNotNull("0.4", b4);
		assertNotNull("0.5", b5);
		assertNotNull("0.6", b6);
		BundleDelta[] changes = delta.getChanges();
		assertEquals("1.0", 6, changes.length);
		BundleDelta[] additions = delta.getChanges(BundleDelta.ADDED, false);
		assertEquals("2.0", 6, additions.length);
		BundleDelta[] resolutions = delta.getChanges(BundleDelta.RESOLVED, false);
		assertEquals("3.0", 6, resolutions.length);
		Map deltasMap = new HashMap();
		for (int i = 0; i < resolutions.length; i++)
			deltasMap.put(resolutions[i].getBundle().getSymbolicName(), resolutions[i]);
		assertNotNull("3.1", deltasMap.get(b1.getSymbolicName()));
		assertNotNull("3.2", deltasMap.get(b2.getSymbolicName()));
		assertNotNull("3.3", deltasMap.get(b3.getSymbolicName()));
		assertNotNull("3.4", deltasMap.get(b4.getSymbolicName()));
		assertNotNull("3.5", deltasMap.get(b5.getSymbolicName()));
		assertNotNull("3.6", deltasMap.get(b6.getSymbolicName()));
		BundleDelta[] unresolutions = delta.getChanges(BundleDelta.UNRESOLVED, false);
		assertEquals("4.0", 0, unresolutions.length);
		assertFullyResolved("5.1", b1);
		assertFullyResolved("5.2", b2);
		assertFullyResolved("5.3", b3);
		assertFullyResolved("5.4", b4);
		assertFullyResolved("5.5", b5);
		assertFullyResolved("5.6", b6);
	}

	public void testDependentBundles() throws BundleException {
		State state = buildComplexState();
		state.resolve();
		BundleDescription[] dependent;
		dependent = platformAdmin.getStateHelper().getDependentBundles(new BundleDescription[] {state.getBundle(2)});
		assertEquals("1.0", 1, dependent.length);
		assertEquals("1.1", state.getBundle(2), dependent[0]);
		dependent = platformAdmin.getStateHelper().getDependentBundles(new BundleDescription[] {state.getBundle(1)});
		assertEquals("2.0", 4, dependent.length);
		assertContains("2.1", dependent, state.getBundle(1));
		assertContains("2.2", dependent, state.getBundle(2));
		assertContains("2.3", dependent, state.getBundle(4));
		assertContains("2.4", dependent, state.getBundle(6));
	}

	// temporarily disabled
	public void testLinkageChange() throws BundleException {
		State state = buildEmptyState();
		// don't add b1 for now
		String B1_LOCATION = "org.eclipse.b1";
		final String B1_MANIFEST = "Bundle-SymbolicName: org.eclipse.b1\n" + "Bundle-Version: 1.0\n";
		BundleDescription b1 = state.getFactory().createBundleDescription(parseManifest(B1_MANIFEST), B1_LOCATION, (long) (Math.random() * Long.MAX_VALUE));
		// b2 requires b1 optionally, so should resolve
		String B2_LOCATION = "org.eclipse.b2";
		final String B2_MANIFEST = "Bundle-SymbolicName: org.eclipse.b2\n" + "Bundle-Version: 2.0\n" + "Require-Bundle: org.eclipse.b1;optional=true";
		BundleDescription b2 = state.getFactory().createBundleDescription(parseManifest(B2_MANIFEST), B2_LOCATION, (long) (Math.random() * Long.MAX_VALUE));
		// b3 requires b1, so should not resolve
		String B3_LOCATION = "org.eclipse.b3";
		final String B3_MANIFEST = "Bundle-SymbolicName: org.eclipse.b3\n" + "Bundle-Version: 2.0\n" + "Require-Bundle: org.eclipse.b1";
		BundleDescription b3 = state.getFactory().createBundleDescription(parseManifest(B3_MANIFEST), B3_LOCATION, (long) (Math.random() * Long.MAX_VALUE));
		// add b2 and b3
		state.addBundle(b2);
		state.addBundle(b3);
		StateDelta delta = state.resolve();
		// ensure we didn't add b1 yet
		assertFullyUnresolved("0.1", b1);
		BundleDelta[] changes = delta.getChanges();
		assertEquals("1.0", 2, changes.length);
		BundleDelta[] additions = delta.getChanges(BundleDelta.ADDED, false);
		assertEquals("2.0", 2, additions.length);
		BundleDelta[] resolutions = delta.getChanges(BundleDelta.RESOLVED, false);
		assertEquals("3.0", 1, resolutions.length);
		assertEquals("3.1", b2, resolutions[0].getBundle());
		BundleDelta[] addtionsNotResolved = delta.getChanges(BundleDelta.ADDED, true);
		assertEquals("4.0", 1, addtionsNotResolved.length);
		assertEquals("4.1", b3, addtionsNotResolved[0].getBundle());
		state.addBundle(b1);
		delta = state.resolve();
		changes = delta.getChanges();
		assertEquals("5.0", 2, changes.length);
		additions = delta.getChanges(BundleDelta.ADDED, false);
		assertEquals("6.0", 1, additions.length);
		assertEquals("6.1", b1, additions[0].getBundle());
		resolutions = delta.getChanges(BundleDelta.RESOLVED, false);
		assertEquals("7.0", 2, resolutions.length);
		BundleDelta[] existingResolved = delta.getChanges(BundleDelta.RESOLVED, true);
		assertEquals("8.0", 1, existingResolved.length);
		assertEquals("8.1", b3, existingResolved[0].getBundle());
		// TODO linkage changed types are no longer valid
		//BundleDelta[] optionalLinkageChanged = delta.getChanges(BundleDelta.OPTIONAL_LINKAGE_CHANGED, true);
		//assertEquals("9.0", 1, optionalLinkageChanged.length);
		//assertEquals("9.1", b2, optionalLinkageChanged[0].getBundle());
		delta = state.resolve(new BundleDescription[] {b2});
		changes = delta.getChanges();
		assertEquals("9.0", 1, changes.length);
		resolutions = delta.getChanges(BundleDelta.RESOLVED, true);
		assertEquals("9.1", 1, resolutions.length);
		assertEquals("9.2", b2, resolutions[0].getBundle());
		assertFullyResolved("10.1", b1);
		assertFullyResolved("10.2", b2);
		assertFullyResolved("10.3", b3);
	}

	// temporarily disabled
	public void testReinstall() throws BundleException {
		State state = buildComplexState();
		StateDelta delta = state.resolve();
		// remove bundle 4 - should cause 6 to be unresolved
		state.removeBundle(4);
		delta = state.resolve();
		assertEquals("1.0", 1, delta.getChanges(BundleDelta.REMOVED | BundleDelta.UNRESOLVED | BundleDelta.REMOVAL_COMPLETE, true).length);
		assertEquals("1.1", 4, delta.getChanges(BundleDelta.REMOVED | BundleDelta.UNRESOLVED | BundleDelta.REMOVAL_COMPLETE, true)[0].getBundle().getBundleId());
		assertEquals("2.0", 1, delta.getChanges(BundleDelta.UNRESOLVED, true).length);
		assertEquals("2.1", 6, delta.getChanges(BundleDelta.UNRESOLVED, true)[0].getBundle().getBundleId());
		// reinstall bundle 4 - should cause 6 to be resolved again
		BundleDescription b4 = delta.getChanges(BundleDelta.REMOVED | BundleDelta.UNRESOLVED | BundleDelta.REMOVAL_COMPLETE, true)[0].getBundle();
		state.addBundle(b4);
		delta = state.resolve();
		assertEquals("3.0", 1, delta.getChanges(BundleDelta.ADDED | BundleDelta.RESOLVED, true).length);
		assertEquals("3.1", 4, delta.getChanges(BundleDelta.ADDED | BundleDelta.RESOLVED, true)[0].getBundle().getBundleId());
		assertEquals("4.0", 1, delta.getChanges(BundleDelta.RESOLVED, true).length);
		assertEquals("4.1", 6, delta.getChanges(BundleDelta.RESOLVED, true)[0].getBundle().getBundleId());
	}

	public void testRemoval() throws BundleException {
		String B1_LOCATION = "org.eclipse.b1";
		final String B1_MANIFEST = "Bundle-SymbolicName: org.eclipse.b1\n" + "Bundle-Version: 1.0\n";
		String B2_LOCATION = "org.eclipse.b2";
		final String B2_MANIFEST = "Bundle-SymbolicName: org.eclipse.b2\n" + "Bundle-Version: 1.0\n";
		State state = platformAdmin.getState();
		state.setResolver(platformAdmin.getResolver());
		BundleDescription b1 = platformAdmin.getFactory().createBundleDescription(parseManifest(B1_MANIFEST), B1_LOCATION, 1);
		BundleDescription b2 = platformAdmin.getFactory().createBundleDescription(parseManifest(B2_MANIFEST), B2_LOCATION, 2);
		state.addBundle(b1);
		state.addBundle(b2);
		StateDelta delta = state.resolve();
		assertTrue("1.1", contains(state.getResolvedBundles(), b1));
		assertTrue("1.2", contains(state.getResolvedBundles(), b2));
		BundleDelta[] changes = delta.getChanges();
		assertEquals("1.3", 2, changes.length);
		assertEquals("1.4 - " + changes[0].getBundle(), (BundleDelta.ADDED | BundleDelta.RESOLVED), changes[0].getType());
		assertEquals("1.5 - " + changes[1].getBundle(), (BundleDelta.ADDED | BundleDelta.RESOLVED), changes[1].getType());
		assertFullyResolved("1.6", b1);
		assertFullyResolved("1.7", b2);
		// remove a resolved bundle
		state.removeBundle(b1);
		assertTrue("2.0", !contains(state.getResolvedBundles(), b1));
		assertTrue("2.1", contains(state.getResolvedBundles(), b2));
		delta = state.resolve();
		changes = delta.getChanges();
		assertEquals("2.2", 1, changes.length);
		assertEquals("2.3", b1, changes[0].getBundle());
		assertEquals("2.4", BundleDelta.REMOVED | BundleDelta.UNRESOLVED, changes[0].getType());
	}

	public void testRemoveAndAdd() throws BundleException {
		String B_LOCATION = "org.eclipse.b";
		final String B_MANIFEST = "Bundle-SymbolicName: org.eclipse.b\n" + "Bundle-Version: 1.0\n";
		State state = platformAdmin.getState();
		state.setResolver(platformAdmin.getResolver());
		BundleDescription b1 = platformAdmin.getFactory().createBundleDescription(parseManifest(B_MANIFEST), B_LOCATION, 1);
		BundleDescription b2 = platformAdmin.getFactory().createBundleDescription(parseManifest(B_MANIFEST), B_LOCATION, 2);
		state.addBundle(b1);
		StateDelta delta = state.resolve();
		BundleDelta[] changes = delta.getChanges();
		assertEquals("1.0", 1, changes.length);
		assertEquals("1.1", b1, changes[0].getBundle());
		assertEquals("1.2", (BundleDelta.ADDED | BundleDelta.RESOLVED), changes[0].getType());
		assertFullyResolved("1.3", b1);
		state.removeBundle(b1);
		state.addBundle(b2);
		delta = state.resolve();
		assertEquals("2.0", 2, delta.getChanges().length);
		assertEquals("2.1", 1, delta.getChanges(BundleDelta.UNRESOLVED | BundleDelta.REMOVED, true).length);
		assertEquals("2.2", b1, delta.getChanges(BundleDelta.UNRESOLVED | BundleDelta.REMOVED, true)[0].getBundle());
		assertEquals("2.3", 1, delta.getChanges(BundleDelta.RESOLVED | BundleDelta.ADDED, true).length);
		assertEquals("2.3", b2, delta.getChanges(BundleDelta.RESOLVED | BundleDelta.ADDED, true)[0].getBundle());
	}

	public void testRemovalResolve() throws BundleException {
		State state = buildInitialState();
		StateDelta delta = state.resolve();
		BundleDelta[] deltas = delta.getChanges();
		BundleDescription b0 = state.getBundle(0);
		assertNotNull("0.1", b0);
		assertFullyResolved("0.2", b0);
		Hashtable manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "org.eclipse.b1");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.IMPORT_PACKAGE, "org.osgi.framework");
		manifest.put(Constants.EXPORT_PACKAGE, "org.eclipse.p1; specification-version=1.0");
		BundleDescription b1 = null;
		try {
			b1 = state.getFactory().createBundleDescription(manifest, "org.eclipse.b1", 1);
			state.removeBundle(1);
			state.addBundle(b1);
		} catch (BundleException e) {
			fail("Failed to create BundleDescription: " + e.getMessage());
		}
		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "org.eclipse.b2");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.IMPORT_PACKAGE, "org.osgi.framework,org.eclipse.p1");
		manifest.put(Constants.EXPORT_PACKAGE, "org.eclipse.p2; specification-version=1.0");
		BundleDescription b2 = null;
		try {
			b2 = state.getFactory().createBundleDescription(manifest, "org.eclipse.b2", 2);
			state.removeBundle(2);
			state.addBundle(b2);
		} catch (BundleException e) {
			fail("Failed to create BundleDescription: " + e.getMessage());
		}
		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "org.eclipse.b3");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.IMPORT_PACKAGE, "org.osgi.framework,org.eclipse.p2");
		manifest.put(Constants.EXPORT_PACKAGE, "org.eclipse.p3; specification-version=1.0");
		BundleDescription b3 = null;
		try {
			b3 = state.getFactory().createBundleDescription(manifest, "org.eclipse.b3", 3);
			state.removeBundle(3);
			state.addBundle(b3);
		} catch (BundleException e) {
			fail("Failed to create BundleDescription: " + e.getMessage());
		}
		delta = state.resolve(false);
		deltas = delta.getChanges();
		assertEquals("1.0", 4, deltas.length);
		Map deltasMap = new HashMap();
		for (int i = 0; i < deltas.length; i++)
			deltasMap.put(new Long(deltas[i].getBundle().getBundleId()), deltas[i]);
		assertNotNull("1.1", deltasMap.get(new Long(1)));
		assertNotNull("1.2", deltasMap.get(new Long(2)));
		assertNotNull("1.3", deltasMap.get(new Long(3)));
		assertEquals("2.1", (BundleDelta.RESOLVED | BundleDelta.ADDED), ((BundleDelta) deltasMap.get(new Long(1))).getType());
		assertEquals("2.2", (BundleDelta.RESOLVED | BundleDelta.ADDED), ((BundleDelta) deltasMap.get(new Long(2))).getType());
		assertEquals("2.3", (BundleDelta.RESOLVED | BundleDelta.ADDED), ((BundleDelta) deltasMap.get(new Long(3))).getType());
		state.removeBundle(1);
		delta = state.resolve(false);
		deltas = delta.getChanges();
		b2 = state.getBundle(2);
		b3 = state.getBundle(3);
		assertEquals("3.1", false, b2.isResolved());
		assertEquals("3.2", false, b3.isResolved());
	}

	/**
	 * Tests adding 3 new bundles to an already resolved state and then
	 * resolving only one of the bundles. The result should be all 3 added
	 * bundles being resolved. Then re-resolving the same bundle. The result
	 * should be only the one bundle being resolved.
	 *  
	 */
	public void testReresolveBundle() throws BundleException {
		State state = buildInitialState();
		StateDelta delta = state.resolve();
		BundleDelta[] deltas = delta.getChanges();
		BundleDescription b0 = state.getBundle(0);
		assertNotNull("0.1", b0);
		assertFullyResolved("0.2", b0);
		Hashtable manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "org.eclipse.b1");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.IMPORT_PACKAGE, "org.osgi.framework");
		manifest.put(Constants.EXPORT_PACKAGE, "org.eclipse.p1; specification-version=1.0");
		BundleDescription b1 = null;
		try {
			b1 = state.getFactory().createBundleDescription(manifest, "org.eclipse.b1", 1);
			state.removeBundle(1);
			state.addBundle(b1);
		} catch (BundleException e) {
			fail("Failed to create BundleDescription: " + e.getMessage());
		}
		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "org.eclipse.b2");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.IMPORT_PACKAGE, "org.osgi.framework,org.eclipse.p1");
		manifest.put(Constants.EXPORT_PACKAGE, "org.eclipse.p2; specification-version=1.0");
		BundleDescription b2 = null;
		try {
			b2 = state.getFactory().createBundleDescription(manifest, "org.eclipse.b2", 2);
			state.removeBundle(2);
			state.addBundle(b2);
		} catch (BundleException e) {
			fail("Failed to create BundleDescription: " + e.getMessage());
		}
		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "org.eclipse.b3");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.IMPORT_PACKAGE, "org.osgi.framework,org.eclipse.p2");
		manifest.put(Constants.EXPORT_PACKAGE, "org.eclipse.p3; specification-version=1.0");
		BundleDescription b3 = null;
		try {
			b3 = state.getFactory().createBundleDescription(manifest, "org.eclipse.b3", 3);
			state.removeBundle(3);
			state.addBundle(b3);
		} catch (BundleException e) {
			fail("Failed to create BundleDescription: " + e.getMessage());
		}
		delta = state.resolve(new BundleDescription[] {state.getBundle(1)});
		deltas = delta.getChanges();
		assertEquals("1.0", 3, deltas.length);
		Map deltasMap = new HashMap();
		for (int i = 0; i < deltas.length; i++)
			deltasMap.put(new Long(deltas[i].getBundle().getBundleId()), deltas[i]);
		assertNotNull("1.1", deltasMap.get(new Long(1)));
		assertNotNull("1.2", deltasMap.get(new Long(2)));
		assertNotNull("1.3", deltasMap.get(new Long(3)));
		assertEquals("2.1", (BundleDelta.RESOLVED | BundleDelta.ADDED), ((BundleDelta) deltasMap.get(new Long(1))).getType());
		assertEquals("2.2", (BundleDelta.RESOLVED | BundleDelta.ADDED), ((BundleDelta) deltasMap.get(new Long(2))).getType());
		assertEquals("2.3", (BundleDelta.RESOLVED | BundleDelta.ADDED), ((BundleDelta) deltasMap.get(new Long(3))).getType());
		delta = state.resolve(new BundleDescription[] {state.getBundle(1)});
		deltas = delta.getChanges();
		assertEquals("3.0", 3, deltas.length);
		deltasMap = new HashMap();
		for (int i = 0; i < deltas.length; i++)
			deltasMap.put(new Long(deltas[i].getBundle().getBundleId()), deltas[i]);
		assertNotNull("3.1", deltasMap.get(new Long(1)));
		assertNotNull("3.2", deltasMap.get(new Long(2)));
		assertNotNull("3.3", deltasMap.get(new Long(3)));
		assertEquals("3.4", BundleDelta.RESOLVED, ((BundleDelta) deltasMap.get(new Long(1))).getType());
		assertEquals("3.5", BundleDelta.RESOLVED, ((BundleDelta) deltasMap.get(new Long(2))).getType());
		assertEquals("3.6", BundleDelta.RESOLVED, ((BundleDelta) deltasMap.get(new Long(3))).getType());

	}

	public void testUpdate() throws BundleException {
		State state = buildEmptyState();
		String B1_LOCATION = "org.eclipse.b";
		final String B1_RESOLVED = "Bundle-SymbolicName: org.eclipse.b1\n" + "Bundle-Version: 1.0\n";
		final String B1_UNRESOLVED = "Bundle-SymbolicName: org.eclipse.b1\n" + "Bundle-Version: 2.0\nRequire-Bundle: non.existant.bundle\n";
		BundleDescription b1 = state.getFactory().createBundleDescription(parseManifest(B1_RESOLVED), B1_LOCATION, 1);
		assertTrue("0.9", state.addBundle(b1));
		StateDelta delta = state.resolve();
		b1 = state.getBundleByLocation(b1.getLocation());
		BundleDelta[] changes = delta.getChanges();
		assertEquals("1.0", 1, changes.length);
		assertEquals("1.1", b1, changes[0].getBundle());
		assertEquals("1.2", (BundleDelta.ADDED | BundleDelta.RESOLVED), changes[0].getType());
		assertFullyResolved("1.3", b1);
		assertTrue("1.8", contains(state.getResolvedBundles(), b1));
		BundleDescription b11 = state.getFactory().createBundleDescription(parseManifest(B1_UNRESOLVED), B1_LOCATION, 1);
		assertTrue("1.8b", state.updateBundle(b11));
		b11 = state.getBundle(b11.getBundleId());
		assertTrue("1.9", !contains(state.getResolvedBundles(), b11));
		delta = state.resolve();
		changes = delta.getChanges();
		assertEquals("2.0", 2, changes.length);
		HashMap deltasMap = new HashMap();
		for (int i = 0; i < changes.length; i++)
			deltasMap.put(changes[i].getBundle(), changes[i]);
		assertNotNull("2.1", deltasMap.get(b1));
		assertNotNull("2.2", deltasMap.get(b11));
		assertEquals("2.3", BundleDelta.UNRESOLVED, ((BundleDelta) deltasMap.get(b1)).getType());
		assertEquals("2.4", BundleDelta.UPDATED, ((BundleDelta) deltasMap.get(b11)).getType());
		BundleDescription b111 = state.getFactory().createBundleDescription(parseManifest(B1_RESOLVED), B1_LOCATION, 1);
		assertTrue("3.0", state.updateBundle(b111));
		b111 = state.getBundle(b111.getBundleId());
		assertTrue("3.1", !contains(state.getResolvedBundles(), b111));
		delta = state.resolve();
		changes = delta.getChanges();
		assertEquals("3.2", 1, changes.length);
		assertEquals("3.1", b111, changes[0].getBundle());
		assertEquals("3.2", BundleDelta.UPDATED | BundleDelta.RESOLVED, changes[0].getType());
		assertFullyResolved("3.3", b111);
	}

	private boolean contains(Object[] array, Object element) {
		for (int i = 0; i < array.length; i++)
			if (array[i].equals(element))
				return true;
		return false;
	}

}
//testFragmentUpdateNoVersionChanged()
//testFragmentUpdateVersionChanged()
//testHostUpdateNoVersionChanged()
//testHostUpdateVersionChanged()
