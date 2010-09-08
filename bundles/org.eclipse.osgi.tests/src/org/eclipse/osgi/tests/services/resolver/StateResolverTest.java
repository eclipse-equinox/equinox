/*******************************************************************************
 * Copyright (c) 2003, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.services.resolver;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.osgi.framework.util.Headers;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.osgi.framework.*;

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
		assertNotNull("0.1", b0); //$NON-NLS-1$
		assertFullyResolved("0.2", b0); //$NON-NLS-1$
		Hashtable manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "org.eclipse.b1"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "org.osgi.framework"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "org.eclipse.p1; specification-version=1.0"); //$NON-NLS-1$
		BundleDescription b1 = null;
		try {
			b1 = state.getFactory().createBundleDescription(manifest, "org.eclipse.b1", 1); //$NON-NLS-1$
			state.removeBundle(1);
			state.addBundle(b1);
		} catch (BundleException e) {
			fail("Failed to create BundleDescription: " + e.getMessage()); //$NON-NLS-1$
		}
		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "org.eclipse.b2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "org.osgi.framework,org.eclipse.p1"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "org.eclipse.p2; specification-version=1.0"); //$NON-NLS-1$
		BundleDescription b2 = null;
		try {
			b2 = state.getFactory().createBundleDescription(manifest, "org.eclipse.b2", 2); //$NON-NLS-1$
			state.removeBundle(2);
			state.addBundle(b2);
		} catch (BundleException e) {
			fail("Failed to create BundleDescription: " + e.getMessage()); //$NON-NLS-1$
		}
		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "org.eclipse.b3"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "org.osgi.framework,org.eclipse.p2"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "org.eclipse.p3; specification-version=1.0"); //$NON-NLS-1$
		BundleDescription b3 = null;
		try {
			b3 = state.getFactory().createBundleDescription(manifest, "org.eclipse.b3", 3); //$NON-NLS-1$
			state.removeBundle(3);
			state.addBundle(b3);
		} catch (BundleException e) {
			fail("Failed to create BundleDescription: " + e.getMessage()); //$NON-NLS-1$
		}
		delta = state.resolve(new BundleDescription[] {state.getBundle(1)});
		deltas = delta.getChanges();
		assertEquals("1.0", 3, deltas.length); //$NON-NLS-1$
		Map deltasMap = new HashMap();
		for (int i = 0; i < deltas.length; i++)
			deltasMap.put(new Long(deltas[i].getBundle().getBundleId()), deltas[i]);
		assertNotNull("1.1", deltasMap.get(new Long(1))); //$NON-NLS-1$
		assertNotNull("1.2", deltasMap.get(new Long(2))); //$NON-NLS-1$
		assertNotNull("1.3", deltasMap.get(new Long(3))); //$NON-NLS-1$
		assertEquals("2.1", (BundleDelta.RESOLVED | BundleDelta.ADDED), ((BundleDelta) deltasMap.get(new Long(1))).getType()); //$NON-NLS-1$
		assertEquals("2.2", (BundleDelta.RESOLVED | BundleDelta.ADDED), ((BundleDelta) deltasMap.get(new Long(2))).getType()); //$NON-NLS-1$
		assertEquals("2.3", (BundleDelta.RESOLVED | BundleDelta.ADDED), ((BundleDelta) deltasMap.get(new Long(3))).getType()); //$NON-NLS-1$
	}

	public void testBasicResolution() throws BundleException {
		State state = buildSimpleState();
		StateDelta delta = state.resolve();
		BundleDescription b1 = state.getBundle(1);
		BundleDescription b2 = state.getBundle(2);
		BundleDescription b3 = state.getBundle(3);
		assertNotNull("0.1", b1); //$NON-NLS-1$
		assertNotNull("0.2", b2); //$NON-NLS-1$
		assertNotNull("0.3", b3); //$NON-NLS-1$
		BundleDelta[] changes = delta.getChanges();
		assertEquals("1.0", 3, changes.length); //$NON-NLS-1$
		BundleDelta[] additions = delta.getChanges(BundleDelta.ADDED, false);
		assertEquals("2.0", 3, additions.length); //$NON-NLS-1$
		BundleDelta[] resolutions = delta.getChanges(BundleDelta.RESOLVED, false);
		assertEquals("3.0", 2, resolutions.length); //$NON-NLS-1$
		Map deltasMap = new HashMap();
		for (int i = 0; i < resolutions.length; i++)
			deltasMap.put(resolutions[i].getBundle().getSymbolicName(), resolutions[i]);
		assertNotNull("3.1", deltasMap.get(b1.getSymbolicName())); //$NON-NLS-1$
		assertNotNull("3.2", deltasMap.get(b2.getSymbolicName())); //$NON-NLS-1$
		// TODO why do we expect unresolved deltas here when the bundle was not resolved in the first place?
		//BundleDelta[] unresolutions = delta.getChanges(BundleDelta.UNRESOLVED, false);
		//assertEquals("4.0", 1, unresolutions.length);
		//assertEquals("4.1", unresolutions[0].getBundle(), b3);
		assertFullyResolved("5.1", b1); //$NON-NLS-1$
		assertFullyResolved("5.2", b2); //$NON-NLS-1$
		assertFullyUnresolved("5.3", b3); //$NON-NLS-1$
	}

	public void testComplexResolution() throws BundleException {
		State state = buildComplexState();
		StateDelta delta = state.resolve();
		BundleDescription b1 = state.getBundle("org.eclipse.b1", Version.parseVersion("1.0")); //$NON-NLS-1$ //$NON-NLS-2$
		BundleDescription b2 = state.getBundle("org.eclipse.b2", Version.parseVersion("2.0")); //$NON-NLS-1$ //$NON-NLS-2$
		BundleDescription b3 = state.getBundle("org.eclipse.b3", Version.parseVersion("2.0")); //$NON-NLS-1$ //$NON-NLS-2$
		BundleDescription b4 = state.getBundle("org.eclipse.b4", Version.parseVersion("2.0")); //$NON-NLS-1$ //$NON-NLS-2$
		BundleDescription b5 = state.getBundle("org.eclipse.b5", Version.parseVersion("1.0")); //$NON-NLS-1$ //$NON-NLS-2$
		BundleDescription b6 = state.getBundle("org.eclipse.b6", Version.parseVersion("1.0")); //$NON-NLS-1$ //$NON-NLS-2$
		assertNotNull("0.1", b1); //$NON-NLS-1$
		assertNotNull("0.2", b2); //$NON-NLS-1$
		assertNotNull("0.3", b3); //$NON-NLS-1$
		assertNotNull("0.4", b4); //$NON-NLS-1$
		assertNotNull("0.5", b5); //$NON-NLS-1$
		assertNotNull("0.6", b6); //$NON-NLS-1$
		BundleDelta[] changes = delta.getChanges();
		assertEquals("1.0", 6, changes.length); //$NON-NLS-1$
		BundleDelta[] additions = delta.getChanges(BundleDelta.ADDED, false);
		assertEquals("2.0", 6, additions.length); //$NON-NLS-1$
		BundleDelta[] resolutions = delta.getChanges(BundleDelta.RESOLVED, false);
		assertEquals("3.0", 6, resolutions.length); //$NON-NLS-1$
		Map deltasMap = new HashMap();
		for (int i = 0; i < resolutions.length; i++)
			deltasMap.put(resolutions[i].getBundle().getSymbolicName(), resolutions[i]);
		assertNotNull("3.1", deltasMap.get(b1.getSymbolicName())); //$NON-NLS-1$
		assertNotNull("3.2", deltasMap.get(b2.getSymbolicName())); //$NON-NLS-1$
		assertNotNull("3.3", deltasMap.get(b3.getSymbolicName())); //$NON-NLS-1$
		assertNotNull("3.4", deltasMap.get(b4.getSymbolicName())); //$NON-NLS-1$
		assertNotNull("3.5", deltasMap.get(b5.getSymbolicName())); //$NON-NLS-1$
		assertNotNull("3.6", deltasMap.get(b6.getSymbolicName())); //$NON-NLS-1$
		BundleDelta[] unresolutions = delta.getChanges(BundleDelta.UNRESOLVED, false);
		assertEquals("4.0", 0, unresolutions.length); //$NON-NLS-1$
		assertFullyResolved("5.1", b1); //$NON-NLS-1$
		assertFullyResolved("5.2", b2); //$NON-NLS-1$
		assertFullyResolved("5.3", b3); //$NON-NLS-1$
		assertFullyResolved("5.4", b4); //$NON-NLS-1$
		assertFullyResolved("5.5", b5); //$NON-NLS-1$
		assertFullyResolved("5.6", b6); //$NON-NLS-1$
	}

	public void testDependentBundles() throws BundleException {
		State state = buildComplexState();
		state.resolve();
		BundleDescription[] dependent;
		dependent = platformAdmin.getStateHelper().getDependentBundles(new BundleDescription[] {state.getBundle(2)});
		assertEquals("1.0", 1, dependent.length); //$NON-NLS-1$
		assertEquals("1.1", state.getBundle(2), dependent[0]); //$NON-NLS-1$
		dependent = platformAdmin.getStateHelper().getDependentBundles(new BundleDescription[] {state.getBundle(1)});
		assertEquals("2.0", 4, dependent.length); //$NON-NLS-1$
		assertContains("2.1", dependent, state.getBundle(1)); //$NON-NLS-1$
		assertContains("2.2", dependent, state.getBundle(2)); //$NON-NLS-1$
		assertContains("2.3", dependent, state.getBundle(4)); //$NON-NLS-1$
		assertContains("2.4", dependent, state.getBundle(6)); //$NON-NLS-1$
	}

	public void testPrerequisiteBundle() throws BundleException {
		State state = buildComplexState();
		state.resolve();
		BundleDescription[] prereqs;
		prereqs = platformAdmin.getStateHelper().getPrerequisites(state.getResolvedBundles());
		assertEquals("1.0", 6, prereqs.length); //$NON-NLS-1$
		prereqs = platformAdmin.getStateHelper().getPrerequisites(new BundleDescription[] {state.getBundle(1)});
		assertEquals("2.0", 2, prereqs.length); //$NON-NLS-1$
		assertContains("2.1", prereqs, state.getBundle(1)); //$NON-NLS-1$
		assertContains("2.2", prereqs, state.getBundle(3)); //$NON-NLS-1$
		prereqs = platformAdmin.getStateHelper().getPrerequisites(new BundleDescription[] {state.getBundle(2)});
		assertEquals("3.0", 3, prereqs.length); //$NON-NLS-1$
		assertContains("3.1", prereqs, state.getBundle(1)); //$NON-NLS-1$
		assertContains("3.2", prereqs, state.getBundle(2)); //$NON-NLS-1$
		assertContains("3.3", prereqs, state.getBundle(3)); //$NON-NLS-1$
		prereqs = platformAdmin.getStateHelper().getPrerequisites(new BundleDescription[] {state.getBundle(3)});
		assertEquals("4.0", 1, prereqs.length); //$NON-NLS-1$
		assertContains("4.1", prereqs, state.getBundle(3)); //$NON-NLS-1$
		prereqs = platformAdmin.getStateHelper().getPrerequisites(new BundleDescription[] {state.getBundle(4)});
		assertEquals("5.0", 3, prereqs.length); //$NON-NLS-1$
		assertContains("5.1", prereqs, state.getBundle(1)); //$NON-NLS-1$
		assertContains("5.2", prereqs, state.getBundle(3)); //$NON-NLS-1$
		assertContains("5.3", prereqs, state.getBundle(4)); //$NON-NLS-1$
		prereqs = platformAdmin.getStateHelper().getPrerequisites(new BundleDescription[] {state.getBundle(5)});
		assertEquals("6.0", 2, prereqs.length); //$NON-NLS-1$
		assertContains("6.1", prereqs, state.getBundle(3)); //$NON-NLS-1$
		assertContains("6.2", prereqs, state.getBundle(5)); //$NON-NLS-1$
		prereqs = platformAdmin.getStateHelper().getPrerequisites(new BundleDescription[] {state.getBundle(6)});
		assertEquals("6.0", 4, prereqs.length); //$NON-NLS-1$
		assertContains("6.1", prereqs, state.getBundle(1)); //$NON-NLS-1$
		assertContains("6.2", prereqs, state.getBundle(3)); //$NON-NLS-1$
		assertContains("6.3", prereqs, state.getBundle(4)); //$NON-NLS-1$
		assertContains("6.4", prereqs, state.getBundle(6)); //$NON-NLS-1$
	}

	// temporarily disabled
	public void testLinkageChange() throws BundleException {
		State state = buildEmptyState();
		// don't add b1 for now
		String B1_LOCATION = "org.eclipse.b1"; //$NON-NLS-1$
		final String B1_MANIFEST = "Bundle-SymbolicName: org.eclipse.b1\n" + "Bundle-Version: 1.0\n"; //$NON-NLS-1$ //$NON-NLS-2$
		BundleDescription b1 = state.getFactory().createBundleDescription(parseManifest(B1_MANIFEST), B1_LOCATION, (long) (Math.random() * Long.MAX_VALUE));
		// b2 requires b1 optionally, so should resolve
		String B2_LOCATION = "org.eclipse.b2"; //$NON-NLS-1$
		final String B2_MANIFEST = "Bundle-SymbolicName: org.eclipse.b2\n" + "Bundle-Version: 2.0\n" + "Require-Bundle: org.eclipse.b1;optional=true"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		BundleDescription b2 = state.getFactory().createBundleDescription(parseManifest(B2_MANIFEST), B2_LOCATION, (long) (Math.random() * Long.MAX_VALUE));
		// b3 requires b1, so should not resolve
		String B3_LOCATION = "org.eclipse.b3"; //$NON-NLS-1$
		final String B3_MANIFEST = "Bundle-SymbolicName: org.eclipse.b3\n" + "Bundle-Version: 2.0\n" + "Require-Bundle: org.eclipse.b1"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		BundleDescription b3 = state.getFactory().createBundleDescription(parseManifest(B3_MANIFEST), B3_LOCATION, (long) (Math.random() * Long.MAX_VALUE));
		// add b2 and b3
		state.addBundle(b2);
		state.addBundle(b3);
		StateDelta delta = state.resolve();
		// ensure we didn't add b1 yet
		assertFullyUnresolved("0.1", b1); //$NON-NLS-1$
		BundleDelta[] changes = delta.getChanges();
		assertEquals("1.0", 2, changes.length); //$NON-NLS-1$
		BundleDelta[] additions = delta.getChanges(BundleDelta.ADDED, false);
		assertEquals("2.0", 2, additions.length); //$NON-NLS-1$
		BundleDelta[] resolutions = delta.getChanges(BundleDelta.RESOLVED, false);
		assertEquals("3.0", 1, resolutions.length); //$NON-NLS-1$
		assertEquals("3.1", b2, resolutions[0].getBundle()); //$NON-NLS-1$
		BundleDelta[] addtionsNotResolved = delta.getChanges(BundleDelta.ADDED, true);
		assertEquals("4.0", 1, addtionsNotResolved.length); //$NON-NLS-1$
		assertEquals("4.1", b3, addtionsNotResolved[0].getBundle()); //$NON-NLS-1$
		state.addBundle(b1);
		delta = state.resolve();
		changes = delta.getChanges();
		assertEquals("5.0", 2, changes.length); //$NON-NLS-1$
		additions = delta.getChanges(BundleDelta.ADDED, false);
		assertEquals("6.0", 1, additions.length); //$NON-NLS-1$
		assertEquals("6.1", b1, additions[0].getBundle()); //$NON-NLS-1$
		resolutions = delta.getChanges(BundleDelta.RESOLVED, false);
		assertEquals("7.0", 2, resolutions.length); //$NON-NLS-1$
		BundleDelta[] existingResolved = delta.getChanges(BundleDelta.RESOLVED, true);
		assertEquals("8.0", 1, existingResolved.length); //$NON-NLS-1$
		assertEquals("8.1", b3, existingResolved[0].getBundle()); //$NON-NLS-1$
		// TODO linkage changed types are no longer valid
		//BundleDelta[] optionalLinkageChanged = delta.getChanges(BundleDelta.OPTIONAL_LINKAGE_CHANGED, true);
		//assertEquals("9.0", 1, optionalLinkageChanged.length);
		//assertEquals("9.1", b2, optionalLinkageChanged[0].getBundle());
		delta = state.resolve(new BundleDescription[] {b2});
		changes = delta.getChanges();
		assertEquals("9.0", 1, changes.length); //$NON-NLS-1$
		resolutions = delta.getChanges(BundleDelta.RESOLVED, true);
		assertEquals("9.1", 1, resolutions.length); //$NON-NLS-1$
		assertEquals("9.2", b2, resolutions[0].getBundle()); //$NON-NLS-1$
		assertFullyResolved("10.1", b1); //$NON-NLS-1$
		assertFullyResolved("10.2", b2); //$NON-NLS-1$
		assertFullyResolved("10.3", b3); //$NON-NLS-1$
	}

	// temporarily disabled
	public void testReinstall() throws BundleException {
		State state = buildComplexState();
		StateDelta delta = state.resolve();
		// remove bundle 4 - should cause 6 to be unresolved
		state.removeBundle(4);
		delta = state.resolve();
		assertEquals("1.0", 1, delta.getChanges(BundleDelta.REMOVED | BundleDelta.UNRESOLVED | BundleDelta.REMOVAL_COMPLETE, true).length); //$NON-NLS-1$
		assertEquals("1.1", 4, delta.getChanges(BundleDelta.REMOVED | BundleDelta.UNRESOLVED | BundleDelta.REMOVAL_COMPLETE, true)[0].getBundle().getBundleId()); //$NON-NLS-1$
		assertEquals("2.0", 1, delta.getChanges(BundleDelta.UNRESOLVED, true).length); //$NON-NLS-1$
		assertEquals("2.1", 6, delta.getChanges(BundleDelta.UNRESOLVED, true)[0].getBundle().getBundleId()); //$NON-NLS-1$
		// reinstall bundle 4 - should cause 6 to be resolved again
		BundleDescription b4 = delta.getChanges(BundleDelta.REMOVED | BundleDelta.UNRESOLVED | BundleDelta.REMOVAL_COMPLETE, true)[0].getBundle();
		state.addBundle(b4);
		delta = state.resolve();
		assertEquals("3.0", 1, delta.getChanges(BundleDelta.ADDED | BundleDelta.RESOLVED, true).length); //$NON-NLS-1$
		assertEquals("3.1", 4, delta.getChanges(BundleDelta.ADDED | BundleDelta.RESOLVED, true)[0].getBundle().getBundleId()); //$NON-NLS-1$
		assertEquals("4.0", 1, delta.getChanges(BundleDelta.RESOLVED, true).length); //$NON-NLS-1$
		assertEquals("4.1", 6, delta.getChanges(BundleDelta.RESOLVED, true)[0].getBundle().getBundleId()); //$NON-NLS-1$
	}

	public void testRemoval() throws BundleException {
		String B1_LOCATION = "org.eclipse.b1"; //$NON-NLS-1$
		final String B1_MANIFEST = "Bundle-SymbolicName: org.eclipse.b1\n" + "Bundle-Version: 1.0\n"; //$NON-NLS-1$ //$NON-NLS-2$
		String B2_LOCATION = "org.eclipse.b2"; //$NON-NLS-1$
		final String B2_MANIFEST = "Bundle-SymbolicName: org.eclipse.b2\n" + "Bundle-Version: 1.0\n"; //$NON-NLS-1$ //$NON-NLS-2$
		State state = platformAdmin.getState();
		state.setResolver(platformAdmin.getResolver());
		BundleDescription b1 = platformAdmin.getFactory().createBundleDescription(parseManifest(B1_MANIFEST), B1_LOCATION, 1);
		BundleDescription b2 = platformAdmin.getFactory().createBundleDescription(parseManifest(B2_MANIFEST), B2_LOCATION, 2);
		state.addBundle(b1);
		state.addBundle(b2);
		StateDelta delta = state.resolve();
		assertTrue("1.1", contains(state.getResolvedBundles(), b1)); //$NON-NLS-1$
		assertTrue("1.2", contains(state.getResolvedBundles(), b2)); //$NON-NLS-1$
		BundleDelta[] changes = delta.getChanges();
		assertEquals("1.3", 2, changes.length); //$NON-NLS-1$
		assertEquals("1.4 - " + changes[0].getBundle(), (BundleDelta.ADDED | BundleDelta.RESOLVED), changes[0].getType()); //$NON-NLS-1$
		assertEquals("1.5 - " + changes[1].getBundle(), (BundleDelta.ADDED | BundleDelta.RESOLVED), changes[1].getType()); //$NON-NLS-1$
		assertFullyResolved("1.6", b1); //$NON-NLS-1$
		assertFullyResolved("1.7", b2); //$NON-NLS-1$
		// remove a resolved bundle
		state.removeBundle(b1);
		assertTrue("2.0", !contains(state.getResolvedBundles(), b1)); //$NON-NLS-1$
		assertTrue("2.1", contains(state.getResolvedBundles(), b2)); //$NON-NLS-1$
		delta = state.resolve();
		changes = delta.getChanges();
		assertEquals("2.2", 1, changes.length); //$NON-NLS-1$
		assertEquals("2.3", b1, changes[0].getBundle()); //$NON-NLS-1$
		assertEquals("2.4", BundleDelta.REMOVED | BundleDelta.UNRESOLVED, changes[0].getType()); //$NON-NLS-1$
	}

	public void testRemoveAndAdd() throws BundleException {
		String B_LOCATION = "org.eclipse.b"; //$NON-NLS-1$
		final String B_MANIFEST = "Bundle-SymbolicName: org.eclipse.b\n" + "Bundle-Version: 1.0\n"; //$NON-NLS-1$ //$NON-NLS-2$
		State state = platformAdmin.getState();
		state.setResolver(platformAdmin.getResolver());
		BundleDescription b1 = platformAdmin.getFactory().createBundleDescription(parseManifest(B_MANIFEST), B_LOCATION, 1);
		BundleDescription b2 = platformAdmin.getFactory().createBundleDescription(parseManifest(B_MANIFEST), B_LOCATION, 2);
		state.addBundle(b1);
		StateDelta delta = state.resolve();
		BundleDelta[] changes = delta.getChanges();
		assertEquals("1.0", 1, changes.length); //$NON-NLS-1$
		assertEquals("1.1", b1, changes[0].getBundle()); //$NON-NLS-1$
		assertEquals("1.2", (BundleDelta.ADDED | BundleDelta.RESOLVED), changes[0].getType()); //$NON-NLS-1$
		assertFullyResolved("1.3", b1); //$NON-NLS-1$
		state.removeBundle(b1);
		state.addBundle(b2);
		delta = state.resolve();
		assertEquals("2.0", 2, delta.getChanges().length); //$NON-NLS-1$
		assertEquals("2.1", 1, delta.getChanges(BundleDelta.UNRESOLVED | BundleDelta.REMOVED, true).length); //$NON-NLS-1$
		assertEquals("2.2", b1, delta.getChanges(BundleDelta.UNRESOLVED | BundleDelta.REMOVED, true)[0].getBundle()); //$NON-NLS-1$
		assertEquals("2.3", 1, delta.getChanges(BundleDelta.RESOLVED | BundleDelta.ADDED, true).length); //$NON-NLS-1$
		assertEquals("2.3", b2, delta.getChanges(BundleDelta.RESOLVED | BundleDelta.ADDED, true)[0].getBundle()); //$NON-NLS-1$
	}

	public void testRemovalResolve() throws BundleException {
		State state = buildInitialState();
		StateDelta delta = state.resolve();
		BundleDelta[] deltas = delta.getChanges();
		BundleDescription b0 = state.getBundle(0);
		assertNotNull("0.1", b0); //$NON-NLS-1$
		assertFullyResolved("0.2", b0); //$NON-NLS-1$
		Hashtable manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "org.eclipse.b1"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "org.osgi.framework"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "org.eclipse.p1; specification-version=1.0"); //$NON-NLS-1$
		BundleDescription b1 = null;
		try {
			b1 = state.getFactory().createBundleDescription(manifest, "org.eclipse.b1", 1); //$NON-NLS-1$
			state.removeBundle(1);
			state.addBundle(b1);
		} catch (BundleException e) {
			fail("Failed to create BundleDescription: " + e.getMessage()); //$NON-NLS-1$
		}
		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "org.eclipse.b2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "org.osgi.framework,org.eclipse.p1"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "org.eclipse.p2; specification-version=1.0"); //$NON-NLS-1$
		BundleDescription b2 = null;
		try {
			b2 = state.getFactory().createBundleDescription(manifest, "org.eclipse.b2", 2); //$NON-NLS-1$
			state.removeBundle(2);
			state.addBundle(b2);
		} catch (BundleException e) {
			fail("Failed to create BundleDescription: " + e.getMessage()); //$NON-NLS-1$
		}
		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "org.eclipse.b3"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "org.osgi.framework,org.eclipse.p2"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "org.eclipse.p3; specification-version=1.0"); //$NON-NLS-1$
		BundleDescription b3 = null;
		try {
			b3 = state.getFactory().createBundleDescription(manifest, "org.eclipse.b3", 3); //$NON-NLS-1$
			state.removeBundle(3);
			state.addBundle(b3);
		} catch (BundleException e) {
			fail("Failed to create BundleDescription: " + e.getMessage()); //$NON-NLS-1$
		}
		delta = state.resolve(false);
		deltas = delta.getChanges();
		assertEquals("1.0", 4, deltas.length); //$NON-NLS-1$
		Map deltasMap = new HashMap();
		for (int i = 0; i < deltas.length; i++)
			deltasMap.put(new Long(deltas[i].getBundle().getBundleId()), deltas[i]);
		assertNotNull("1.1", deltasMap.get(new Long(1))); //$NON-NLS-1$
		assertNotNull("1.2", deltasMap.get(new Long(2))); //$NON-NLS-1$
		assertNotNull("1.3", deltasMap.get(new Long(3))); //$NON-NLS-1$
		assertEquals("2.1", (BundleDelta.RESOLVED | BundleDelta.ADDED), ((BundleDelta) deltasMap.get(new Long(1))).getType()); //$NON-NLS-1$
		assertEquals("2.2", (BundleDelta.RESOLVED | BundleDelta.ADDED), ((BundleDelta) deltasMap.get(new Long(2))).getType()); //$NON-NLS-1$
		assertEquals("2.3", (BundleDelta.RESOLVED | BundleDelta.ADDED), ((BundleDelta) deltasMap.get(new Long(3))).getType()); //$NON-NLS-1$
		state.removeBundle(1);
		delta = state.resolve(false);
		deltas = delta.getChanges();
		b2 = state.getBundle(2);
		b3 = state.getBundle(3);
		assertEquals("3.1", false, b2.isResolved()); //$NON-NLS-1$
		assertEquals("3.2", false, b3.isResolved()); //$NON-NLS-1$
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
		assertNotNull("0.1", b0); //$NON-NLS-1$
		assertFullyResolved("0.2", b0); //$NON-NLS-1$
		Hashtable manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "org.eclipse.b1"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "org.osgi.framework"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "org.eclipse.p1; specification-version=1.0"); //$NON-NLS-1$
		BundleDescription b1 = null;
		try {
			b1 = state.getFactory().createBundleDescription(manifest, "org.eclipse.b1", 1); //$NON-NLS-1$
			state.removeBundle(1);
			state.addBundle(b1);
		} catch (BundleException e) {
			fail("Failed to create BundleDescription: " + e.getMessage()); //$NON-NLS-1$
		}
		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "org.eclipse.b2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "org.osgi.framework,org.eclipse.p1"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "org.eclipse.p2; specification-version=1.0"); //$NON-NLS-1$
		BundleDescription b2 = null;
		try {
			b2 = state.getFactory().createBundleDescription(manifest, "org.eclipse.b2", 2); //$NON-NLS-1$
			state.removeBundle(2);
			state.addBundle(b2);
		} catch (BundleException e) {
			fail("Failed to create BundleDescription: " + e.getMessage()); //$NON-NLS-1$
		}
		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "org.eclipse.b3"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "org.osgi.framework,org.eclipse.p2"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "org.eclipse.p3; specification-version=1.0"); //$NON-NLS-1$
		BundleDescription b3 = null;
		try {
			b3 = state.getFactory().createBundleDescription(manifest, "org.eclipse.b3", 3); //$NON-NLS-1$
			state.removeBundle(3);
			state.addBundle(b3);
		} catch (BundleException e) {
			fail("Failed to create BundleDescription: " + e.getMessage()); //$NON-NLS-1$
		}
		delta = state.resolve(new BundleDescription[] {state.getBundle(1)});
		deltas = delta.getChanges();
		assertEquals("1.0", 3, deltas.length); //$NON-NLS-1$
		Map deltasMap = new HashMap();
		for (int i = 0; i < deltas.length; i++)
			deltasMap.put(new Long(deltas[i].getBundle().getBundleId()), deltas[i]);
		assertNotNull("1.1", deltasMap.get(new Long(1))); //$NON-NLS-1$
		assertNotNull("1.2", deltasMap.get(new Long(2))); //$NON-NLS-1$
		assertNotNull("1.3", deltasMap.get(new Long(3))); //$NON-NLS-1$
		assertEquals("2.1", (BundleDelta.RESOLVED | BundleDelta.ADDED), ((BundleDelta) deltasMap.get(new Long(1))).getType()); //$NON-NLS-1$
		assertEquals("2.2", (BundleDelta.RESOLVED | BundleDelta.ADDED), ((BundleDelta) deltasMap.get(new Long(2))).getType()); //$NON-NLS-1$
		assertEquals("2.3", (BundleDelta.RESOLVED | BundleDelta.ADDED), ((BundleDelta) deltasMap.get(new Long(3))).getType()); //$NON-NLS-1$
		delta = state.resolve(new BundleDescription[] {state.getBundle(1)});
		deltas = delta.getChanges();
		assertEquals("3.0", 3, deltas.length); //$NON-NLS-1$
		deltasMap = new HashMap();
		for (int i = 0; i < deltas.length; i++)
			deltasMap.put(new Long(deltas[i].getBundle().getBundleId()), deltas[i]);
		assertNotNull("3.1", deltasMap.get(new Long(1))); //$NON-NLS-1$
		assertNotNull("3.2", deltasMap.get(new Long(2))); //$NON-NLS-1$
		assertNotNull("3.3", deltasMap.get(new Long(3))); //$NON-NLS-1$
		assertEquals("3.4", BundleDelta.RESOLVED, ((BundleDelta) deltasMap.get(new Long(1))).getType()); //$NON-NLS-1$
		assertEquals("3.5", BundleDelta.RESOLVED, ((BundleDelta) deltasMap.get(new Long(2))).getType()); //$NON-NLS-1$
		assertEquals("3.6", BundleDelta.RESOLVED, ((BundleDelta) deltasMap.get(new Long(3))).getType()); //$NON-NLS-1$

	}

	public void testUpdate() throws BundleException {
		State state = buildEmptyState();
		String B1_LOCATION = "org.eclipse.b"; //$NON-NLS-1$
		final String B1_RESOLVED = "Bundle-SymbolicName: org.eclipse.b1\n" + "Bundle-Version: 1.0\n"; //$NON-NLS-1$ //$NON-NLS-2$
		final String B1_UNRESOLVED = "Bundle-SymbolicName: org.eclipse.b1\n" + "Bundle-Version: 2.0\nRequire-Bundle: non.existant.bundle\n"; //$NON-NLS-1$ //$NON-NLS-2$
		BundleDescription b1 = state.getFactory().createBundleDescription(parseManifest(B1_RESOLVED), B1_LOCATION, 1);
		assertTrue("0.9", state.addBundle(b1)); //$NON-NLS-1$
		StateDelta delta = state.resolve();
		b1 = state.getBundleByLocation(b1.getLocation());
		BundleDelta[] changes = delta.getChanges();
		assertEquals("1.0", 1, changes.length); //$NON-NLS-1$
		assertEquals("1.1", b1, changes[0].getBundle()); //$NON-NLS-1$
		assertEquals("1.2", (BundleDelta.ADDED | BundleDelta.RESOLVED), changes[0].getType()); //$NON-NLS-1$
		assertFullyResolved("1.3", b1); //$NON-NLS-1$
		assertTrue("1.8", contains(state.getResolvedBundles(), b1)); //$NON-NLS-1$
		BundleDescription b11 = state.getFactory().createBundleDescription(parseManifest(B1_UNRESOLVED), B1_LOCATION, 1);
		assertTrue("1.8b", state.updateBundle(b11)); //$NON-NLS-1$
		b11 = state.getBundle(b11.getBundleId());
		assertTrue("1.9", !contains(state.getResolvedBundles(), b11)); //$NON-NLS-1$
		delta = state.resolve();
		changes = delta.getChanges();
		assertEquals("2.0", 2, changes.length); //$NON-NLS-1$
		HashMap deltasMap = new HashMap();
		for (int i = 0; i < changes.length; i++)
			deltasMap.put(changes[i].getBundle(), changes[i]);
		assertNotNull("2.1", deltasMap.get(b1)); //$NON-NLS-1$
		assertNotNull("2.2", deltasMap.get(b11)); //$NON-NLS-1$
		assertEquals("2.3", BundleDelta.UNRESOLVED, ((BundleDelta) deltasMap.get(b1)).getType()); //$NON-NLS-1$
		assertEquals("2.4", BundleDelta.UPDATED, ((BundleDelta) deltasMap.get(b11)).getType()); //$NON-NLS-1$
		BundleDescription b111 = state.getFactory().createBundleDescription(parseManifest(B1_RESOLVED), B1_LOCATION, 1);
		assertTrue("3.0", state.updateBundle(b111)); //$NON-NLS-1$
		b111 = state.getBundle(b111.getBundleId());
		assertTrue("3.1", !contains(state.getResolvedBundles(), b111)); //$NON-NLS-1$
		delta = state.resolve();
		changes = delta.getChanges();
		assertEquals("3.2", 1, changes.length); //$NON-NLS-1$
		assertEquals("3.1", b111, changes[0].getBundle()); //$NON-NLS-1$
		assertEquals("3.2", BundleDelta.UPDATED | BundleDelta.RESOLVED, changes[0].getType()); //$NON-NLS-1$
		assertFullyResolved("3.3", b111); //$NON-NLS-1$
	}

	public void testSingletons() throws BundleException {
		State state = buildEmptyState();
		Hashtable manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "test.host; singleton:=true"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		BundleDescription testHost100 = state.getFactory().createBundleDescription(state, manifest, "test.host100", 0); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "test.host; singleton:=true"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.1"); //$NON-NLS-1$
		BundleDescription testHost101 = state.getFactory().createBundleDescription(state, manifest, "test.host101", 1); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "test.frag; singleton:=true"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "test.host; bundle-version=\"[1.0.0,2.0.0)\""); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		BundleDescription testFrag100 = state.getFactory().createBundleDescription(state, manifest, "test.frag100", 2); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "test.frag; singleton:=true"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "test.host; bundle-version=\"[1.0.0,2.0.0)\""); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.1"); //$NON-NLS-1$
		BundleDescription testFrag101 = state.getFactory().createBundleDescription(state, manifest, "test.frag101", 3); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "test.dependent; singleton:=true"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "test.host; bundle-version=\"[1.0.1,2.0.0)\""); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		BundleDescription testDependent = state.getFactory().createBundleDescription(state, manifest, "test.frag101", 4); //$NON-NLS-1$

		state.addBundle(testHost100);
		state.addBundle(testFrag100);
		state.addBundle(testHost101);
		state.addBundle(testFrag101);
		state.addBundle(testDependent);
		state.resolve();
		assertFalse("1.0", testHost100.isResolved()); //$NON-NLS-1$
		assertTrue("1.1", testHost101.isResolved()); //$NON-NLS-1$
		assertFalse("1.2", testFrag100.isResolved()); //$NON-NLS-1$
		assertTrue("1.3", testFrag101.isResolved()); //$NON-NLS-1$
		assertTrue("1.4", testDependent.isResolved()); //$NON-NLS-1$
	}

	public void testSingletonsSameVersion() throws BundleException {
		// this is a testcase to handle how PDE build is using the state
		// with multiple singleton bundles installed with the same BSN and version
		State state = buildEmptyState();
		Hashtable manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "test.host; singleton:=true"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		BundleDescription testHost100 = state.getFactory().createBundleDescription(state, manifest, "test.host100", 0); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "test.host; singleton:=true"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		BundleDescription testHost101 = state.getFactory().createBundleDescription(state, manifest, "test.host101", 1); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "test.frag; singleton:=true"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "test.host; bundle-version=\"[1.0.0,2.0.0)\""); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		BundleDescription testFrag100 = state.getFactory().createBundleDescription(state, manifest, "test.frag100", 2); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "test.frag; singleton:=true"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "test.host; bundle-version=\"[1.0.0,2.0.0)\""); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		BundleDescription testFrag101 = state.getFactory().createBundleDescription(state, manifest, "test.frag101", 3); //$NON-NLS-1$

		state.addBundle(testHost100);
		state.addBundle(testFrag100);
		state.resolve();
		state.addBundle(testHost101);
		state.addBundle(testFrag101);
		state.resolve();
		assertTrue("1.0", testHost100.isResolved()); //$NON-NLS-1$
		assertFalse("1.1", testHost101.isResolved()); //$NON-NLS-1$
		assertTrue("1.2", testFrag100.isResolved()); //$NON-NLS-1$
		assertFalse("1.3", testFrag101.isResolved()); //$NON-NLS-1$
	}

	public void testSingletonsSelection1() throws BundleException {
		State state = buildEmptyState();

		// test the selection algorithm of the resolver to pick the bundles which
		// resolve the largest set of bundles
		Hashtable manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "sdk; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "platform; bundle-version=\"[1.0,2.0]\""); //$NON-NLS-1$
		BundleDescription sdk10 = state.getFactory().createBundleDescription(state, manifest, "sdk10", 0); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "platform; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "rcp; bundle-version=\"[1.0,2.0]\""); //$NON-NLS-1$
		BundleDescription platform10 = state.getFactory().createBundleDescription(state, manifest, "platform10", 1); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "rcp; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		BundleDescription rcp10 = state.getFactory().createBundleDescription(state, manifest, "rcp10", 2); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "gef; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "rcp; bundle-version=\"[1.0,1.0]\""); //$NON-NLS-1$
		BundleDescription gef10 = state.getFactory().createBundleDescription(state, manifest, "gef10", 3); //$NON-NLS-1$

		state.addBundle(sdk10);
		state.addBundle(platform10);
		state.addBundle(rcp10);
		state.addBundle(gef10);
		state.resolve();

		assertTrue("1.0", sdk10.isResolved()); //$NON-NLS-1$
		assertTrue("1.1", platform10.isResolved()); //$NON-NLS-1$
		assertTrue("1.2", rcp10.isResolved()); //$NON-NLS-1$
		assertTrue("1.3", gef10.isResolved()); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "sdk; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "2.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "platform; bundle-version=\"[1.0,2.0]\""); //$NON-NLS-1$
		BundleDescription sdk20 = state.getFactory().createBundleDescription(state, manifest, "sdk20", 4); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "platform; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "2.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "rcp; bundle-version=\"[1.0,2.0]\""); //$NON-NLS-1$
		BundleDescription platform20 = state.getFactory().createBundleDescription(state, manifest, "platform20", 5); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "rcp; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "2.0"); //$NON-NLS-1$
		BundleDescription rcp20 = state.getFactory().createBundleDescription(state, manifest, "rcp20", 6); //$NON-NLS-1$

		state.addBundle(sdk20);
		state.addBundle(platform20);
		state.addBundle(rcp20);
		state.resolve(false);

		assertTrue("2.0", sdk20.isResolved()); //$NON-NLS-1$
		assertTrue("2.1", platform20.isResolved()); //$NON-NLS-1$
		assertTrue("2.2", rcp10.isResolved()); //$NON-NLS-1$
		assertTrue("2.3", gef10.isResolved()); //$NON-NLS-1$
		assertFalse("2.4", sdk10.isResolved()); //$NON-NLS-1$
		assertFalse("2.5", platform10.isResolved()); //$NON-NLS-1$
		assertFalse("2.6", rcp20.isResolved()); //$NON-NLS-1$
	}

	public void testSingletonsSelection2() throws BundleException {
		State state = buildEmptyState();

		// test the selection algorithm of the resolver to pick the bundles which
		// resolve the largest set of bundles; test with cycle added
		Hashtable manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "sdk; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "platform; bundle-version=\"[1.0,2.0]\", cycle"); //$NON-NLS-1$
		BundleDescription sdk10 = state.getFactory().createBundleDescription(state, manifest, "sdk10", 0); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "platform; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "rcp; bundle-version=\"[1.0,2.0]\""); //$NON-NLS-1$
		BundleDescription platform10 = state.getFactory().createBundleDescription(state, manifest, "platform10", 1); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "rcp; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		BundleDescription rcp10 = state.getFactory().createBundleDescription(state, manifest, "rcp10", 2); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "gef; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "rcp; bundle-version=\"[1.0,1.0]\", sdk"); //$NON-NLS-1$
		BundleDescription gef10 = state.getFactory().createBundleDescription(state, manifest, "gef10", 3); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "cycle; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "gef"); //$NON-NLS-1$
		BundleDescription cycle10 = state.getFactory().createBundleDescription(state, manifest, "gef10", 4); //$NON-NLS-1$

		state.addBundle(sdk10);
		state.addBundle(platform10);
		state.addBundle(rcp10);
		state.addBundle(gef10);
		state.addBundle(cycle10);
		state.resolve();

		assertTrue("1.0", sdk10.isResolved()); //$NON-NLS-1$
		assertTrue("1.1", platform10.isResolved()); //$NON-NLS-1$
		assertTrue("1.2", rcp10.isResolved()); //$NON-NLS-1$
		assertTrue("1.3", gef10.isResolved()); //$NON-NLS-1$
		assertTrue("1.4", cycle10.isResolved()); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "sdk; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "2.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "platform; bundle-version=\"[1.0,2.0]\""); //$NON-NLS-1$
		BundleDescription sdk20 = state.getFactory().createBundleDescription(state, manifest, "sdk20", 5); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "platform; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "2.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "rcp; bundle-version=\"[1.0,2.0]\""); //$NON-NLS-1$
		BundleDescription platform20 = state.getFactory().createBundleDescription(state, manifest, "platform20", 6); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "rcp; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "2.0"); //$NON-NLS-1$
		BundleDescription rcp20 = state.getFactory().createBundleDescription(state, manifest, "rcp20", 7); //$NON-NLS-1$

		state.addBundle(sdk20);
		state.addBundle(platform20);
		state.addBundle(rcp20);
		state.resolve(false);

		assertTrue("2.0", sdk20.isResolved()); //$NON-NLS-1$
		assertTrue("2.1", platform20.isResolved()); //$NON-NLS-1$
		assertTrue("2.2", rcp10.isResolved()); //$NON-NLS-1$
		assertTrue("2.3", gef10.isResolved()); //$NON-NLS-1$
		assertTrue("2.4", cycle10.isResolved()); //$NON-NLS-1$
		assertFalse("2.5", sdk10.isResolved()); //$NON-NLS-1$
		assertFalse("2.6", platform10.isResolved()); //$NON-NLS-1$
		assertFalse("2.7", rcp20.isResolved()); //$NON-NLS-1$
	}

	public void testSingletonsSelection3() throws BundleException {
		State state = buildEmptyState();
		int bundleID = 0;
		// test the selection algorithm of the resolver to pick the bundles which
		// resolve the largest set of bundles; with fragments
		Hashtable manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "sdk; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "platform; bundle-version=\"[1.0,2.0]\""); //$NON-NLS-1$
		BundleDescription sdk10 = state.getFactory().createBundleDescription(state, manifest, "sdk10", bundleID++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "sdk.frag; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "sdk"); //$NON-NLS-1$
		BundleDescription sdk_frag10 = state.getFactory().createBundleDescription(state, manifest, "sdk.frag10", bundleID++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "sdk.frag2; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "sdk; bundle-version=2.0"); //$NON-NLS-1$
		BundleDescription sdk_frag210 = state.getFactory().createBundleDescription(state, manifest, "sdk.frag210", bundleID++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "platform; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "rcp; bundle-version=\"[1.0,2.0]\""); //$NON-NLS-1$
		BundleDescription platform10 = state.getFactory().createBundleDescription(state, manifest, "platform10", bundleID++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "platform.frag; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "platform"); //$NON-NLS-1$
		BundleDescription platform_frag10 = state.getFactory().createBundleDescription(state, manifest, "platform.frag10", bundleID++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "platform.frag2; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "platform; bundle-version=2.0"); //$NON-NLS-1$
		BundleDescription platform_frag210 = state.getFactory().createBundleDescription(state, manifest, "platform.frag210", bundleID++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "rcp; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		BundleDescription rcp10 = state.getFactory().createBundleDescription(state, manifest, "rcp10", bundleID++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "rcp.frag; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "rcp"); //$NON-NLS-1$
		BundleDescription rcp_frag10 = state.getFactory().createBundleDescription(state, manifest, "rcp.frag10", bundleID++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "rcp.frag2; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "rcp; bundle-version=2.0"); //$NON-NLS-1$
		BundleDescription rcp_frag210 = state.getFactory().createBundleDescription(state, manifest, "rcp.frag210", bundleID++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "gef; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "rcp; bundle-version=\"[1.0,1.0]\""); //$NON-NLS-1$
		BundleDescription gef10 = state.getFactory().createBundleDescription(state, manifest, "gef10", bundleID++); //$NON-NLS-1$

		state.addBundle(sdk10);
		state.addBundle(sdk_frag10);
		state.addBundle(sdk_frag210);
		state.addBundle(platform10);
		state.addBundle(platform_frag10);
		state.addBundle(platform_frag210);
		state.addBundle(rcp10);
		state.addBundle(rcp_frag10);
		state.addBundle(rcp_frag210);
		state.addBundle(gef10);
		state.resolve();

		assertTrue("1.0", sdk10.isResolved()); //$NON-NLS-1$
		assertTrue("1.0.1", sdk_frag10.isResolved()); //$NON-NLS-1$
		assertFalse("1.0.2", sdk_frag210.isResolved()); //$NON-NLS-1$
		assertTrue("1.1", platform10.isResolved()); //$NON-NLS-1$
		assertTrue("1.1.1", platform_frag10.isResolved()); //$NON-NLS-1$
		assertFalse("1.1.2", platform_frag210.isResolved()); //$NON-NLS-1$
		assertTrue("1.2", rcp10.isResolved()); //$NON-NLS-1$
		assertTrue("1.2.1", rcp_frag10.isResolved()); //$NON-NLS-1$
		assertFalse("1.2.2", rcp_frag210.isResolved()); //$NON-NLS-1$
		assertTrue("1.3", gef10.isResolved()); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "sdk; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "2.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "platform; bundle-version=\"[1.0,2.0]\""); //$NON-NLS-1$
		BundleDescription sdk20 = state.getFactory().createBundleDescription(state, manifest, "sdk20", bundleID++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "platform; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "2.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "rcp; bundle-version=\"[1.0,2.0]\""); //$NON-NLS-1$
		BundleDescription platform20 = state.getFactory().createBundleDescription(state, manifest, "platform20", bundleID++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "rcp; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "2.0"); //$NON-NLS-1$
		BundleDescription rcp20 = state.getFactory().createBundleDescription(state, manifest, "rcp20", bundleID++); //$NON-NLS-1$

		state.addBundle(sdk20);
		state.addBundle(platform20);
		state.addBundle(rcp20);
		state.resolve(false);

		assertTrue("2.0", sdk20.isResolved()); //$NON-NLS-1$
		assertTrue("2.0.1", sdk_frag10.isResolved()); //$NON-NLS-1$
		assertTrue("2.0.2", sdk_frag210.isResolved()); //$NON-NLS-1$
		assertTrue("2.1", platform20.isResolved()); //$NON-NLS-1$
		assertTrue("2.1.1", platform_frag10.isResolved()); //$NON-NLS-1$
		assertTrue("2.1.2", platform_frag210.isResolved()); //$NON-NLS-1$
		assertTrue("2.2", rcp10.isResolved()); //$NON-NLS-1$
		assertTrue("2.2.1", rcp_frag10.isResolved()); //$NON-NLS-1$
		assertTrue("2.3", gef10.isResolved()); //$NON-NLS-1$
		assertFalse("2.4", sdk10.isResolved()); //$NON-NLS-1$
		assertFalse("2.5", platform10.isResolved()); //$NON-NLS-1$
		assertFalse("2.6", rcp20.isResolved()); //$NON-NLS-1$
		assertFalse("2.2.2", rcp_frag210.isResolved()); //$NON-NLS-1$
	}

	public void testSingletonsSelection4() throws BundleException {
		State state = buildEmptyState();
		int bundleID = 0;
		// test the selection algorithm of the resolver to pick the bundles which
		// resolve the largest set of bundles; with fragments using Import-Package
		Hashtable manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "sdk; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "sdk; version=1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "platform; version=\"[1.0,2.0]\""); //$NON-NLS-1$
		BundleDescription sdk10 = state.getFactory().createBundleDescription(state, manifest, "sdk10", bundleID++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "sdk.frag; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "sdk"); //$NON-NLS-1$
		BundleDescription sdk_frag10 = state.getFactory().createBundleDescription(state, manifest, "sdk.frag10", bundleID++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "sdk.frag2; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "sdk; bundle-version=2.0"); //$NON-NLS-1$
		BundleDescription sdk_frag210 = state.getFactory().createBundleDescription(state, manifest, "sdk.frag210", bundleID++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "platform; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "platform; version=1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "rcp; version=\"[1.0,2.0]\""); //$NON-NLS-1$
		BundleDescription platform10 = state.getFactory().createBundleDescription(state, manifest, "platform10", bundleID++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "platform.frag; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "platform"); //$NON-NLS-1$
		BundleDescription platform_frag10 = state.getFactory().createBundleDescription(state, manifest, "platform.frag10", bundleID++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "platform.frag2; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "platform; bundle-version=2.0"); //$NON-NLS-1$
		BundleDescription platform_frag210 = state.getFactory().createBundleDescription(state, manifest, "platform.frag210", bundleID++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "rcp; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "rcp; version=1.0"); //$NON-NLS-1$
		BundleDescription rcp10 = state.getFactory().createBundleDescription(state, manifest, "rcp10", bundleID++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "rcp.frag; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "rcp"); //$NON-NLS-1$
		BundleDescription rcp_frag10 = state.getFactory().createBundleDescription(state, manifest, "rcp.frag10", bundleID++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "rcp.frag2; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "rcp; bundle-version=2.0"); //$NON-NLS-1$
		BundleDescription rcp_frag210 = state.getFactory().createBundleDescription(state, manifest, "rcp.frag210", bundleID++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "gef; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "rcp; version=\"[1.0,1.0]\""); //$NON-NLS-1$
		BundleDescription gef10 = state.getFactory().createBundleDescription(state, manifest, "gef10", bundleID++); //$NON-NLS-1$

		state.addBundle(sdk10);
		state.addBundle(sdk_frag10);
		state.addBundle(sdk_frag210);
		state.addBundle(platform10);
		state.addBundle(platform_frag10);
		state.addBundle(platform_frag210);
		state.addBundle(rcp10);
		state.addBundle(rcp_frag10);
		state.addBundle(rcp_frag210);
		state.addBundle(gef10);
		state.resolve();

		assertTrue("1.0", sdk10.isResolved()); //$NON-NLS-1$
		assertTrue("1.0.1", sdk_frag10.isResolved()); //$NON-NLS-1$
		assertFalse("1.0.2", sdk_frag210.isResolved()); //$NON-NLS-1$
		assertTrue("1.1", platform10.isResolved()); //$NON-NLS-1$
		assertTrue("1.1.1", platform_frag10.isResolved()); //$NON-NLS-1$
		assertFalse("1.1.2", platform_frag210.isResolved()); //$NON-NLS-1$
		assertTrue("1.2", rcp10.isResolved()); //$NON-NLS-1$
		assertTrue("1.2.1", rcp_frag10.isResolved()); //$NON-NLS-1$
		assertFalse("1.2.2", rcp_frag210.isResolved()); //$NON-NLS-1$
		assertTrue("1.3", gef10.isResolved()); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "sdk; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "2.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "sdk; version=2.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "platform; version=\"[1.0,2.0]\""); //$NON-NLS-1$
		BundleDescription sdk20 = state.getFactory().createBundleDescription(state, manifest, "sdk20", bundleID++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "platform; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "2.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "platform; version=2.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "rcp; version=\"[1.0,2.0]\""); //$NON-NLS-1$
		BundleDescription platform20 = state.getFactory().createBundleDescription(state, manifest, "platform20", bundleID++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "rcp; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.EXPORT_PACKAGE, "rcp; version=2.0"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "2.0"); //$NON-NLS-1$
		BundleDescription rcp20 = state.getFactory().createBundleDescription(state, manifest, "rcp20", bundleID++); //$NON-NLS-1$

		state.addBundle(sdk20);
		state.addBundle(platform20);
		state.addBundle(rcp20);
		state.resolve(false);

		assertTrue("2.0", sdk20.isResolved()); //$NON-NLS-1$
		assertTrue("2.0.1", sdk_frag10.isResolved()); //$NON-NLS-1$
		assertTrue("2.0.2", sdk_frag210.isResolved()); //$NON-NLS-1$
		assertTrue("2.1", platform20.isResolved()); //$NON-NLS-1$
		assertTrue("2.1.1", platform_frag10.isResolved()); //$NON-NLS-1$
		assertTrue("2.1.2", platform_frag210.isResolved()); //$NON-NLS-1$
		assertTrue("2.2", rcp10.isResolved()); //$NON-NLS-1$
		assertTrue("2.2.1", rcp_frag10.isResolved()); //$NON-NLS-1$
		assertTrue("2.3", gef10.isResolved()); //$NON-NLS-1$
		assertFalse("2.4", sdk10.isResolved()); //$NON-NLS-1$
		assertFalse("2.5", platform10.isResolved()); //$NON-NLS-1$
		assertFalse("2.6", rcp20.isResolved()); //$NON-NLS-1$
		assertFalse("2.2.2", rcp_frag210.isResolved()); //$NON-NLS-1$
	}

	public void testSingletonsSelection5() throws BundleException {
		State state = buildEmptyState();

		long id = 0;
		Hashtable manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "base; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		BundleDescription base10 = state.getFactory().createBundleDescription(state, manifest, "base10", id++); //$NON-NLS-1$

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "base; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.1"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, "J2SE-1.6"); //$NON-NLS-1$
		BundleDescription base11 = state.getFactory().createBundleDescription(state, manifest, "base11", id++); //$NON-NLS-1$

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "requires; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "base; bundle-version=\"[1.0,1.1)\""); //$NON-NLS-1$
		BundleDescription requires10 = state.getFactory().createBundleDescription(state, manifest, "requires10", id++); //$NON-NLS-1$

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "requires; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.1"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "base; bundle-version=\"[1.1,1.2)\""); //$NON-NLS-1$
		BundleDescription requires11 = state.getFactory().createBundleDescription(state, manifest, "requires11", id++); //$NON-NLS-1$

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "frag; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "requires; bundle-version=\"[1.0,1.1)\""); //$NON-NLS-1$
		BundleDescription frag10 = state.getFactory().createBundleDescription(state, manifest, "frag10", id++); //$NON-NLS-1$

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "frag; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.1"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "requires; bundle-version=\"[1.1,1.2)\""); //$NON-NLS-1$
		BundleDescription frag11 = state.getFactory().createBundleDescription(state, manifest, "frag11", id++); //$NON-NLS-1$

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "fragb; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "requires; bundle-version=\"[1.0,1.1)\""); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "fragb; version=1.0"); //$NON-NLS-1$
		BundleDescription fragb10 = state.getFactory().createBundleDescription(state, manifest, "frag10", id++); //$NON-NLS-1$

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "fragb; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.1"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "requires; bundle-version=\"[1.0,1.1)\""); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "fragb; version=1.1"); //$NON-NLS-1$
		BundleDescription fragb11 = state.getFactory().createBundleDescription(state, manifest, "frag11", id++); //$NON-NLS-1$

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "import"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "fragb; version=\"[1.0,1.0]\""); //$NON-NLS-1$
		BundleDescription import10 = state.getFactory().createBundleDescription(state, manifest, "import10", id++); //$NON-NLS-1$

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "import"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.1"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "fragb; version=\"[1.1,1.1]\""); //$NON-NLS-1$
		BundleDescription import11 = state.getFactory().createBundleDescription(state, manifest, "import11", id++); //$NON-NLS-1$

		state.addBundle(base10);
		state.addBundle(base11);
		state.addBundle(requires10);
		state.addBundle(requires11);
		state.addBundle(frag10);
		state.addBundle(frag11);
		state.addBundle(fragb10);
		state.addBundle(fragb11);
		state.addBundle(import10);
		state.addBundle(import11);
		state.resolve();

		assertTrue("1.0", base10.isResolved()); //$NON-NLS-1$
		assertTrue("1.1", requires10.isResolved()); //$NON-NLS-1$
		assertTrue("1.2", frag10.isResolved()); //$NON-NLS-1$
		assertTrue("1.3", fragb11.isResolved()); //$NON-NLS-1$
		assertTrue("1.4", import11.isResolved()); //$NON-NLS-1$
		assertFalse("1.5", base11.isResolved()); //$NON-NLS-1$
		assertFalse("1.6", requires11.isResolved()); //$NON-NLS-1$
		assertFalse("1.7", frag11.isResolved()); //$NON-NLS-1$
		assertFalse("1.8", fragb10.isResolved()); //$NON-NLS-1$
		assertFalse("1.9", import10.isResolved()); //$NON-NLS-1$
	}

	public void testSingletonsSelection6() throws BundleException {
		State state = buildEmptyState();

		// test the selection algorithm of the resolver to pick the bundles which
		// resolve the largest set of bundles
		Hashtable manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "sdk; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "platform; bundle-version=\"[1.0,2.0]\""); //$NON-NLS-1$
		BundleDescription sdk10 = state.getFactory().createBundleDescription(state, manifest, "sdk10", 0); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "platform; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "rcp; bundle-version=\"[1.0,2.0]\""); //$NON-NLS-1$
		BundleDescription platform10 = state.getFactory().createBundleDescription(state, manifest, "platform10", 1); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "rcp; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		BundleDescription rcp10 = state.getFactory().createBundleDescription(state, manifest, "rcp10", 2); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "gef; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "rcp; bundle-version=\"[1.0,1.0]\""); //$NON-NLS-1$
		BundleDescription gef10 = state.getFactory().createBundleDescription(state, manifest, "gef10", 3); //$NON-NLS-1$

		state.addBundle(sdk10);
		state.addBundle(platform10);
		state.addBundle(rcp10);
		state.addBundle(gef10);
		state.resolve();

		assertTrue("1.0", sdk10.isResolved()); //$NON-NLS-1$
		assertTrue("1.1", platform10.isResolved()); //$NON-NLS-1$
		assertTrue("1.2", rcp10.isResolved()); //$NON-NLS-1$
		assertTrue("1.3", gef10.isResolved()); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "sdk; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "2.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "platform; bundle-version=\"[1.0,2.0]\""); //$NON-NLS-1$
		BundleDescription sdk20 = state.getFactory().createBundleDescription(state, manifest, "sdk20", 4); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "platform; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "2.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "rcp; bundle-version=\"[1.0,2.0]\""); //$NON-NLS-1$
		BundleDescription platform20 = state.getFactory().createBundleDescription(state, manifest, "platform20", 5); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "rcp; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "2.0"); //$NON-NLS-1$
		BundleDescription rcp20 = state.getFactory().createBundleDescription(state, manifest, "rcp20", 6); //$NON-NLS-1$

		state.removeBundle(sdk10);
		state.removeBundle(platform10);
		state.removeBundle(rcp10);
		state.removeBundle(gef10);

		// reorder the bundles to test that order of bundles does not effect resolution outcome
		state.addBundle(sdk20);
		state.addBundle(platform20);
		state.addBundle(rcp20);
		state.addBundle(sdk10);
		state.addBundle(platform10);
		state.addBundle(rcp10);
		state.addBundle(gef10);
		state.resolve(false);

		assertTrue("2.0", sdk20.isResolved()); //$NON-NLS-1$
		assertTrue("2.1", platform20.isResolved()); //$NON-NLS-1$
		assertTrue("2.2", rcp10.isResolved()); //$NON-NLS-1$
		assertTrue("2.3", gef10.isResolved()); //$NON-NLS-1$
		assertFalse("2.4", sdk10.isResolved()); //$NON-NLS-1$
		assertFalse("2.5", platform10.isResolved()); //$NON-NLS-1$
		assertFalse("2.6", rcp20.isResolved()); //$NON-NLS-1$
	}

	public void testSingletonsSelection7() throws BundleException {
		State state = buildEmptyState();
		long id = 0;
		// test the selection algorithm of the resolver to pick the bundles which
		// resolve the largest set of bundles
		Hashtable manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "singleton; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "singleton"); //$NON-NLS-1$
		BundleDescription singleton1 = state.getFactory().createBundleDescription(state, manifest, "singleton1", id++); //$NON-NLS-1$

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "a"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "singleton"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "a;version=1.0.0"); //$NON-NLS-1$
		BundleDescription a1 = state.getFactory().createBundleDescription(state, manifest, "a1", id++); //$NON-NLS-1$

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "b"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "a"); //$NON-NLS-1$
		BundleDescription b = state.getFactory().createBundleDescription(state, manifest, "b", id++); //$NON-NLS-1$

		state.addBundle(singleton1);
		state.addBundle(a1);
		state.addBundle(b);
		state.resolve();

		assertTrue("1.0", singleton1.isResolved()); //$NON-NLS-1$
		assertTrue("1.1", a1.isResolved()); //$NON-NLS-1$
		assertTrue("1.2", b.isResolved()); //$NON-NLS-1$

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "singleton; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.1"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "singleton"); //$NON-NLS-1$
		BundleDescription singleton2 = state.getFactory().createBundleDescription(state, manifest, "singleton2", id++); //$NON-NLS-1$

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "a"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.1"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "singleton"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "a;version=1.0.1"); //$NON-NLS-1$
		BundleDescription a2 = state.getFactory().createBundleDescription(state, manifest, "a2", id++); //$NON-NLS-1$

		state.addBundle(singleton2);
		state.addBundle(a2);
		state.resolve(new BundleDescription[] {singleton1, singleton2, a1, a2});

		assertFalse("2.0", singleton1.isResolved()); //$NON-NLS-1$
		assertTrue("2.1", singleton2.isResolved()); //$NON-NLS-1$
		assertTrue("2.2", a1.isResolved()); //$NON-NLS-1$
		assertTrue("2.3", a2.isResolved()); //$NON-NLS-1$
		assertTrue("2.4", b.isResolved()); //$NON-NLS-1$

		ExportPackageDescription[] imports = b.getResolvedImports();
		assertEquals("Unexpected number of imports", 1, imports.length); //$NON-NLS-1$
		assertEquals("Unexpected exporter", a2, imports[0].getExporter()); //$NON-NLS-1$
	}

	public void testNonSingletonsSameVersion() throws BundleException {
		// this is a testcase to handle how PDE build is using the state
		// with multiple singleton bundles installed with the same BSN and version
		State state = buildEmptyState();
		Hashtable manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "test.host"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		BundleDescription testHost100 = state.getFactory().createBundleDescription(state, manifest, "test.host100", 0); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "test.host; singleton:=true"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		BundleDescription testHost101 = state.getFactory().createBundleDescription(state, manifest, "test.host101", 1); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "test.frag"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "test.host; bundle-version=\"[1.0.0,2.0.0)\""); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		BundleDescription testFrag100 = state.getFactory().createBundleDescription(state, manifest, "test.frag100", 2); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "test.frag; singleton:=true"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "test.host; bundle-version=\"[1.0.0,2.0.0)\""); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		BundleDescription testFrag101 = state.getFactory().createBundleDescription(state, manifest, "test.frag101", 3); //$NON-NLS-1$

		state.addBundle(testHost100);
		state.addBundle(testFrag100);
		state.resolve();
		state.addBundle(testHost101);
		state.addBundle(testFrag101);
		state.resolve();
		assertTrue("1.0", testHost100.isResolved()); //$NON-NLS-1$
		assertTrue("1.1", testHost101.isResolved()); //$NON-NLS-1$
		assertTrue("1.2", testFrag100.isResolved()); //$NON-NLS-1$
		assertTrue("1.3", testFrag101.isResolved()); //$NON-NLS-1$
	}

	public void testTransitiveUses() throws BundleException {
		State state = buildEmptyState();
		Hashtable manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A1"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "a"); //$NON-NLS-1$
		BundleDescription a1_100 = state.getFactory().createBundleDescription(state, manifest, "a1_100", 0); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "a"); //$NON-NLS-1$
		BundleDescription a2_100 = state.getFactory().createBundleDescription(state, manifest, "a2_100", 1); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B1"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "b; uses:=a"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "a;bundle-symbolic-name=A2"); //$NON-NLS-1$
		BundleDescription b1_100 = state.getFactory().createBundleDescription(state, manifest, "b1_100", 2); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "C1"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "c; uses:=b"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "b"); //$NON-NLS-1$
		BundleDescription c1_100 = state.getFactory().createBundleDescription(state, manifest, "c1_100", 3); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "D1"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "a, c"); //$NON-NLS-1$
		BundleDescription d1_100 = state.getFactory().createBundleDescription(state, manifest, "d1_100", 4); //$NON-NLS-1$

		state.addBundle(a1_100);
		state.addBundle(a2_100);
		state.addBundle(b1_100);
		state.addBundle(c1_100);
		state.addBundle(d1_100);
		state.resolve();

		ExportPackageDescription[] b1ResolvedImports = b1_100.getResolvedImports();
		ExportPackageDescription[] d1ResolvedImports = d1_100.getResolvedImports();
		ExportPackageDescription[] isConsistent = isConsistent(b1ResolvedImports, d1ResolvedImports);
		assertNull("1.1 Packages are not consistent: " + isConsistent, isConsistent); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B1"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "b; uses:=a"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "a;bundle-symbolic-name=A1"); //$NON-NLS-1$
		b1_100 = state.getFactory().createBundleDescription(state, manifest, "b1_100", 2); //$NON-NLS-1$
		state.updateBundle(b1_100);
		state.resolve();

		b1ResolvedImports = b1_100.getResolvedImports();
		d1ResolvedImports = d1_100.getResolvedImports();
		isConsistent = isConsistent(b1ResolvedImports, d1ResolvedImports);
		assertNull("1.2 Packages are not consistent: " + isConsistent, isConsistent); //$NON-NLS-1$
	}

	public void testMultipleExportsUses01() throws BundleException {
		State state = buildEmptyState();
		Hashtable manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A1"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "a; version=2.0; uses:=d, d; version=2.0"); //$NON-NLS-1$
		BundleDescription a1_100 = state.getFactory().createBundleDescription(state, manifest, "a1_100", 0); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "a; version=1.0; uses:=d, d; version=1.0"); //$NON-NLS-1$
		BundleDescription a2_100 = state.getFactory().createBundleDescription(state, manifest, "a2_100", 1); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B1"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "b, b; mandatory:=\"test\"; test=value; uses:=d"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "a;bundle-symbolic-name=A2, d"); //$NON-NLS-1$
		BundleDescription b1_100 = state.getFactory().createBundleDescription(state, manifest, "b1_100", 2); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "C1"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "c; uses:=b"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "b; test=value, d"); //$NON-NLS-1$
		BundleDescription c1_100 = state.getFactory().createBundleDescription(state, manifest, "c1_100", 3); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "D1"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "a, c, d"); //$NON-NLS-1$
		BundleDescription d1_100 = state.getFactory().createBundleDescription(state, manifest, "d1_100", 4); //$NON-NLS-1$

		state.addBundle(a1_100);
		state.addBundle(a2_100);
		state.addBundle(b1_100);
		state.addBundle(c1_100);
		state.addBundle(d1_100);
		state.resolve();

		ExportPackageDescription[] b1ResolvedImports = b1_100.getResolvedImports();
		ExportPackageDescription[] d1ResolvedImports = d1_100.getResolvedImports();
		ExportPackageDescription[] isConsistent = isConsistent(b1ResolvedImports, d1ResolvedImports);
		assertNull("1.1 Packages are not consistent: " + isConsistent, isConsistent); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B1"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "b; uses:=a"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "a;bundle-symbolic-name=A1"); //$NON-NLS-1$
		b1_100 = state.getFactory().createBundleDescription(state, manifest, "b1_100", 2); //$NON-NLS-1$
		state.updateBundle(b1_100);
		state.resolve();

		b1ResolvedImports = b1_100.getResolvedImports();
		d1ResolvedImports = d1_100.getResolvedImports();
		isConsistent = isConsistent(b1ResolvedImports, d1ResolvedImports);
		assertNull("1.2 Packages are not consistent: " + isConsistent, isConsistent); //$NON-NLS-1$
	}

	public void testRequireBundleUses() throws BundleException {
		State state = buildEmptyState();
		int id = 0;
		Hashtable manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "a"); //$NON-NLS-1$
		BundleDescription a_100 = state.getFactory().createBundleDescription(state, manifest, "a_100", id++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "2.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "a"); //$NON-NLS-1$
		BundleDescription a_200 = state.getFactory().createBundleDescription(state, manifest, "a_200", id++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "b; uses:=a"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "A; visibility:=reexport"); //$NON-NLS-1$
		BundleDescription b_100 = state.getFactory().createBundleDescription(state, manifest, "b_100", id++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "C"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "c; uses:=b"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "A, B"); //$NON-NLS-1$
		BundleDescription c_100 = state.getFactory().createBundleDescription(state, manifest, "c_100", id++); //$NON-NLS-1$

		state.addBundle(a_100);
		state.addBundle(b_100);
		// first resolve just A and B
		state.resolve();
		assertTrue("0.1", a_100.isResolved()); //$NON-NLS-1$
		assertTrue("0.2", b_100.isResolved()); //$NON-NLS-1$
		// now add A v2 and resolve it
		state.addBundle(a_200);
		state.resolve();
		assertTrue("1.1", a_200.isResolved()); //$NON-NLS-1$
		// now add C and make sure it does not get packages from A v2
		state.addBundle(c_100);
		state.resolve();
		assertTrue("1.2", c_100.isResolved()); //$NON-NLS-1$

		ExportPackageDescription[] c1ResolvedImports = state.getStateHelper().getVisiblePackages(c_100);
		assertTrue("2.1", c1ResolvedImports.length == 2); //$NON-NLS-1$
		int index = c1ResolvedImports[0].getName().equals("a") ? 0 : c1ResolvedImports[1].getName().equals("a") ? 1 : -1; //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("2.2", index >= 0); //$NON-NLS-1$
		assertEquals("2.2", c1ResolvedImports[index].getExporter(), a_100); //$NON-NLS-1$
	}

	public void testCyclicTransitiveUses() throws BundleException {
		State state = buildEmptyState();
		Hashtable manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A1"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "a; uses:=d"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "d"); //$NON-NLS-1$
		BundleDescription a1_100 = state.getFactory().createBundleDescription(state, manifest, "a1_100", 0); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "a; uses:=d"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "d"); //$NON-NLS-1$
		BundleDescription a2_100 = state.getFactory().createBundleDescription(state, manifest, "a2_100", 1); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B1"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "b; uses:=a"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "a;bundle-symbolic-name=A2"); //$NON-NLS-1$
		BundleDescription b1_100 = state.getFactory().createBundleDescription(state, manifest, "b1_100", 2); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "C1"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "c; uses:=b"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "b"); //$NON-NLS-1$
		BundleDescription c1_100 = state.getFactory().createBundleDescription(state, manifest, "c1_100", 3); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "D1"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "d; uses:=c"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "a, c"); //$NON-NLS-1$
		BundleDescription d1_100 = state.getFactory().createBundleDescription(state, manifest, "d1_100", 4); //$NON-NLS-1$

		state.addBundle(a1_100);
		state.addBundle(a2_100);
		state.addBundle(b1_100);
		state.addBundle(c1_100);
		state.addBundle(d1_100);
		state.resolve();

		assertFalse("0.1", a1_100.isResolved()); //$NON-NLS-1$
		assertTrue("0.2", a2_100.isResolved()); //$NON-NLS-1$
		assertTrue("0.3", b1_100.isResolved()); //$NON-NLS-1$
		assertTrue("0.4", c1_100.isResolved()); //$NON-NLS-1$
		assertTrue("0.5", d1_100.isResolved()); //$NON-NLS-1$

		ExportPackageDescription[] b1ResolvedImports = b1_100.getResolvedImports();
		ExportPackageDescription[] d1ResolvedImports = d1_100.getResolvedImports();
		ExportPackageDescription[] isConsistent = isConsistent(b1ResolvedImports, d1ResolvedImports);
		assertNull("1.1 Packages are not consistent: " + isConsistent, isConsistent); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B1"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "b; uses:=a"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "a;bundle-symbolic-name=A1"); //$NON-NLS-1$
		b1_100 = state.getFactory().createBundleDescription(state, manifest, "b1_100", 2); //$NON-NLS-1$
		state.updateBundle(b1_100);
		state.resolve();

		assertTrue("2.1", a1_100.isResolved()); //$NON-NLS-1$
		assertFalse("2.2", a2_100.isResolved()); //$NON-NLS-1$
		assertTrue("2.3", b1_100.isResolved()); //$NON-NLS-1$
		assertTrue("2.4", c1_100.isResolved()); //$NON-NLS-1$
		assertTrue("2.5", d1_100.isResolved()); //$NON-NLS-1$

		b1ResolvedImports = b1_100.getResolvedImports();
		d1ResolvedImports = d1_100.getResolvedImports();
		isConsistent = isConsistent(b1ResolvedImports, d1ResolvedImports);
		assertNull("3.1 Packages are not consistent: " + isConsistent, isConsistent); //$NON-NLS-1$
	}

	public void testFragmentTransitiveUses() throws BundleException {
		State state = buildEmptyState();
		Hashtable manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A1"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "a; uses:=d"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "d"); //$NON-NLS-1$
		BundleDescription a1_100 = state.getFactory().createBundleDescription(state, manifest, "a1_100", 0); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A1.Frag"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "A1"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "a.frag; uses:=a"); //$NON-NLS-1$
		BundleDescription a1frag_100 = state.getFactory().createBundleDescription(state, manifest, "a1frag_100", 1); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "a; uses:=d, a.frag; uses:=a"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "d"); //$NON-NLS-1$
		BundleDescription a2_100 = state.getFactory().createBundleDescription(state, manifest, "a2_100", 2); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B1"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "b; uses:=a"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "a;bundle-symbolic-name=A2, a.frag"); //$NON-NLS-1$
		BundleDescription b1_100 = state.getFactory().createBundleDescription(state, manifest, "b1_100", 3); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "C1"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "c; uses:=b"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "b"); //$NON-NLS-1$
		BundleDescription c1_100 = state.getFactory().createBundleDescription(state, manifest, "c1_100", 4); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "D1"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "d; uses:=c"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "a.frag, c"); //$NON-NLS-1$
		BundleDescription d1_100 = state.getFactory().createBundleDescription(state, manifest, "d1_100", 5); //$NON-NLS-1$

		state.addBundle(a1_100);
		state.addBundle(a1frag_100);
		state.addBundle(a2_100);
		state.addBundle(b1_100);
		state.addBundle(c1_100);
		state.addBundle(d1_100);
		state.resolve();

		assertFalse("0.1", a1_100.isResolved()); //$NON-NLS-1$
		assertTrue("0.2", a2_100.isResolved()); //$NON-NLS-1$
		assertTrue("0.3", b1_100.isResolved()); //$NON-NLS-1$
		assertTrue("0.4", c1_100.isResolved()); //$NON-NLS-1$
		assertTrue("0.5", d1_100.isResolved()); //$NON-NLS-1$

		ExportPackageDescription[] b1ResolvedImports = b1_100.getResolvedImports();
		ExportPackageDescription[] d1ResolvedImports = d1_100.getResolvedImports();
		ExportPackageDescription[] isConsistent = isConsistent(b1ResolvedImports, d1ResolvedImports);
		assertNull("1.1 Packages are not consistent: " + isConsistent, isConsistent); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B1"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "b; uses:=a"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "a;bundle-symbolic-name=A1"); //$NON-NLS-1$
		b1_100 = state.getFactory().createBundleDescription(state, manifest, "b1_100", 2); //$NON-NLS-1$
		state.updateBundle(b1_100);
		state.resolve();

		assertTrue("2.1", a1_100.isResolved()); //$NON-NLS-1$
		assertFalse("2.2", a2_100.isResolved()); //$NON-NLS-1$
		assertTrue("2.3", b1_100.isResolved()); //$NON-NLS-1$
		assertTrue("2.4", c1_100.isResolved()); //$NON-NLS-1$
		assertTrue("2.5", d1_100.isResolved()); //$NON-NLS-1$

		b1ResolvedImports = b1_100.getResolvedImports();
		d1ResolvedImports = d1_100.getResolvedImports();
		isConsistent = isConsistent(b1ResolvedImports, d1ResolvedImports);
		assertNull("3.1 Packages are not consistent: " + isConsistent, isConsistent); //$NON-NLS-1$
	}

	public void testFragmentUses01() throws BundleException {
		long id = 0;
		State state = buildEmptyState();

		Hashtable manifest = new Hashtable();
		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A1"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "a"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "C2"); //$NON-NLS-1$
		BundleDescription a1 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + manifest.get(Constants.BUNDLE_VERSION), id++); //$NON-NLS-1$

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A1.Frag"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "A1"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "b; good=true"); //$NON-NLS-1$
		BundleDescription a1frag = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + manifest.get(Constants.BUNDLE_VERSION), id++); //$NON-NLS-1$

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B1"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "b; good=true; uses:=c"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "c; good=true"); //$NON-NLS-1$
		BundleDescription b1 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + manifest.get(Constants.BUNDLE_VERSION), id++); //$NON-NLS-1$

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "C1"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "c; good=true"); //$NON-NLS-1$
		BundleDescription c1 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + manifest.get(Constants.BUNDLE_VERSION), id++); //$NON-NLS-1$

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "C2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "c; bad=true"); //$NON-NLS-1$
		BundleDescription c2 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + manifest.get(Constants.BUNDLE_VERSION), id++); //$NON-NLS-1$

		state.addBundle(a1);
		state.addBundle(a1frag);
		state.addBundle(b1);
		state.addBundle(c1);
		state.addBundle(c2);
		state.resolve();

		assertTrue("0.1", a1.isResolved()); //$NON-NLS-1$
		assertFalse("0.2", a1frag.isResolved()); //$NON-NLS-1$
		assertTrue("0.3", b1.isResolved()); //$NON-NLS-1$
		assertTrue("0.4", c1.isResolved()); //$NON-NLS-1$
		assertTrue("0.5", c2.isResolved()); //$NON-NLS-1$
	}

	public void testFragmentUses02() throws BundleException {
		long id = 0;
		State state = buildEmptyState();
		Dictionary[] props = new Dictionary[] {new Hashtable()};
		props[0].put("osgi.resolverMode", "development"); //$NON-NLS-1$ //$NON-NLS-2$
		state.setPlatformProperties(props);

		Hashtable manifest = new Hashtable();
		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A1"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "a"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "C2"); //$NON-NLS-1$
		BundleDescription a1 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + manifest.get(Constants.BUNDLE_VERSION), id++); //$NON-NLS-1$

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A1.Frag"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "A1"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "b; good=true"); //$NON-NLS-1$
		BundleDescription a1frag = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + manifest.get(Constants.BUNDLE_VERSION), id++); //$NON-NLS-1$

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B1"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "b; good=true; uses:=c"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "c; good=true"); //$NON-NLS-1$
		BundleDescription b1 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + manifest.get(Constants.BUNDLE_VERSION), id++); //$NON-NLS-1$

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "C1"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "c; good=true"); //$NON-NLS-1$
		BundleDescription c1 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + manifest.get(Constants.BUNDLE_VERSION), id++); //$NON-NLS-1$

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "C2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "c; bad=true"); //$NON-NLS-1$
		BundleDescription c2 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + manifest.get(Constants.BUNDLE_VERSION), id++); //$NON-NLS-1$

		state.addBundle(a1);
		state.addBundle(a1frag);
		state.addBundle(b1);
		state.addBundle(c1);
		state.addBundle(c2);
		state.resolve();

		assertTrue("0.1", a1.isResolved()); //$NON-NLS-1$
		// uses constraints are ignored when in dev mode (see 261849)
		assertTrue("0.2", a1frag.isResolved()); //$NON-NLS-1$
		assertTrue("0.3", b1.isResolved()); //$NON-NLS-1$
		assertTrue("0.4", c1.isResolved()); //$NON-NLS-1$
		assertTrue("0.5", c2.isResolved()); //$NON-NLS-1$
	}

	public void testCyclicUsesExportDrop() throws BundleException {
		State state = buildEmptyState();
		Hashtable manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "W"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "a; b; version=1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "c, a, b"); //$NON-NLS-1$
		BundleDescription w1_100 = state.getFactory().createBundleDescription(state, manifest, "w1_100", 0); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "X"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "c"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "d"); //$NON-NLS-1$
		BundleDescription x1_100 = state.getFactory().createBundleDescription(state, manifest, "x1_100", 1); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "Y"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "d"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "a"); //$NON-NLS-1$
		BundleDescription y1_100 = state.getFactory().createBundleDescription(state, manifest, "y1_100", 2); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "Z"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "a; version = 2.0"); //$NON-NLS-1$
		BundleDescription z1_100 = state.getFactory().createBundleDescription(state, manifest, "z1_100", 3); //$NON-NLS-1$

		state.addBundle(w1_100);
		state.addBundle(x1_100);
		state.addBundle(y1_100);
		state.addBundle(z1_100);
		state.resolve();

		assertTrue("0.1", w1_100.isResolved()); //$NON-NLS-1$
		assertTrue("0.2", x1_100.isResolved()); //$NON-NLS-1$
		assertTrue("0.3", y1_100.isResolved()); //$NON-NLS-1$
		assertTrue("0.4", z1_100.isResolved()); //$NON-NLS-1$

		assertEquals("1.1", 1, w1_100.getSelectedExports().length); //$NON-NLS-1$
		assertEquals("1.2", "b", w1_100.getSelectedExports()[0].getName()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testRemovalPending() throws BundleException {
		State state = buildEmptyState();
		Hashtable wManifest = new Hashtable();
		wManifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		wManifest.put(Constants.BUNDLE_SYMBOLICNAME, "W"); //$NON-NLS-1$
		wManifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		wManifest.put(Constants.EXPORT_PACKAGE, "a; b; version=1.0"); //$NON-NLS-1$
		wManifest.put(Constants.IMPORT_PACKAGE, "a, b"); //$NON-NLS-1$
		BundleDescription w1_100 = state.getFactory().createBundleDescription(state, wManifest, "w1_100", 0); //$NON-NLS-1$

		Hashtable manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "X"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "a"); //$NON-NLS-1$
		BundleDescription x1_100 = state.getFactory().createBundleDescription(state, manifest, "x1_100", 1); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "Y"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "a"); //$NON-NLS-1$
		BundleDescription y1_100 = state.getFactory().createBundleDescription(state, manifest, "y1_100", 2); //$NON-NLS-1$

		state.addBundle(w1_100);
		state.addBundle(x1_100);
		state.addBundle(y1_100);

		state.resolve();

		assertTrue("0.1", w1_100.isResolved()); //$NON-NLS-1$
		assertTrue("0.2", x1_100.isResolved()); //$NON-NLS-1$
		assertTrue("0.3", y1_100.isResolved()); //$NON-NLS-1$

		BundleDescription w1_100_prime = state.getFactory().createBundleDescription(state, wManifest, "w1_100", 0); //$NON-NLS-1$
		state.updateBundle(w1_100_prime);
		state.resolve(new BundleDescription[0]);

		assertTrue("1.1", w1_100_prime.isResolved()); //$NON-NLS-1$
		assertTrue("1.2", x1_100.isResolved()); //$NON-NLS-1$
		assertTrue("1.3", y1_100.isResolved()); //$NON-NLS-1$

		ExportPackageDescription[] exports_w1_100 = w1_100.getSelectedExports();
		ExportPackageDescription[] imports_w1_100_prime = w1_100_prime.getResolvedImports();
		ExportPackageDescription[] isConsistent = isConsistent(exports_w1_100, imports_w1_100_prime);
		assertNull("2.1 Packages are not consistent: " + isConsistent, isConsistent); //$NON-NLS-1$

		state.resolve(new BundleDescription[] {w1_100});
		assertTrue("3.1", w1_100_prime.isResolved()); //$NON-NLS-1$
		assertTrue("3.2", x1_100.isResolved()); //$NON-NLS-1$
		assertTrue("3.3", y1_100.isResolved()); //$NON-NLS-1$

		ExportPackageDescription[] exports_w1_100_prime = w1_100_prime.getSelectedExports();
		imports_w1_100_prime = w1_100_prime.getResolvedImports();
		isConsistent = isConsistent(exports_w1_100_prime, imports_w1_100_prime);
		assertNull("4.1 Packages are not consistent: " + isConsistent, isConsistent); //$NON-NLS-1$
	}

	public void testFragmentConstraints01() throws BundleException {
		int id = 0;
		State state = buildEmptyState();
		Hashtable manifest = new Hashtable();

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "D1"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "a.frag"); //$NON-NLS-1$
		BundleDescription d1_100 = state.getFactory().createBundleDescription(state, manifest, "c1_100", id++); //$NON-NLS-1$

		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A1"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "a; uses:=d"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "b; version=2.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "C1; bundle-version=\"[2.0, 3.0)\""); //$NON-NLS-1$
		BundleDescription a1_100 = state.getFactory().createBundleDescription(state, manifest, "a1_100", id++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A1.Frag"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "A1"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "a.frag"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "b; version=1.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "C1; bundle-version=\"[2.0, 3.0)\""); //$NON-NLS-1$
		BundleDescription a1frag_100 = state.getFactory().createBundleDescription(state, manifest, "a1frag_100", id++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B1"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "b; version=2.1"); //$NON-NLS-1$
		BundleDescription b1_100 = state.getFactory().createBundleDescription(state, manifest, "b1_100", id++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "C1"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "2.0.1"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "c"); //$NON-NLS-1$
		BundleDescription c1_100 = state.getFactory().createBundleDescription(state, manifest, "c1_100", id++); //$NON-NLS-1$

		state.addBundle(d1_100);
		state.addBundle(a1_100);
		state.addBundle(a1frag_100);
		state.addBundle(b1_100);
		state.addBundle(c1_100);
		state.resolve();

		assertTrue("0.1", a1_100.isResolved()); //$NON-NLS-1$
		assertTrue("0.2", a1frag_100.isResolved()); //$NON-NLS-1$
		assertTrue("0.3", b1_100.isResolved()); //$NON-NLS-1$
		assertTrue("0.4", c1_100.isResolved()); //$NON-NLS-1$
		assertTrue("0.5", d1_100.isResolved()); //$NON-NLS-1$

		// now use a fragment that has conflicting imports/requires with the host
		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A1.Frag"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "A1"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "a.frag"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "b; version=2.1"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "C1; bundle-version=\"[2.5, 4.0)\""); //$NON-NLS-1$
		a1frag_100 = state.getFactory().createBundleDescription(state, manifest, "a1frag_100", a1frag_100.getBundleId()); //$NON-NLS-1$
		state.updateBundle(a1frag_100);
		state.resolve(new BundleDescription[] {a1frag_100});

		assertTrue("1.1", a1_100.isResolved()); //$NON-NLS-1$
		assertFalse("1.2", a1frag_100.isResolved()); //$NON-NLS-1$
		assertTrue("1.3", b1_100.isResolved()); //$NON-NLS-1$
		assertTrue("1.4", c1_100.isResolved()); //$NON-NLS-1$
		assertFalse("1.5", d1_100.isResolved()); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A1.Frag"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "A1"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "a.frag"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "b; version=\"[1.0,2.0)\""); //$NON-NLS-1$
		a1frag_100 = state.getFactory().createBundleDescription(state, manifest, "a1frag_100", a1frag_100.getBundleId()); //$NON-NLS-1$
		state.updateBundle(a1frag_100);
		state.resolve(new BundleDescription[] {a1frag_100});

		assertTrue("2.1", a1_100.isResolved()); //$NON-NLS-1$
		assertFalse("2.2", a1frag_100.isResolved()); //$NON-NLS-1$
		assertTrue("2.3", b1_100.isResolved()); //$NON-NLS-1$
		assertTrue("2.4", c1_100.isResolved()); //$NON-NLS-1$
		assertFalse("2.5", d1_100.isResolved()); //$NON-NLS-1$

		// now use a fragment that has conflicting imports/requires with the host
		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A1.Frag"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "A1"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "a.frag"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "C1; bundle-version=\"[1.0, 1.5)\""); //$NON-NLS-1$
		a1frag_100 = state.getFactory().createBundleDescription(state, manifest, "a1frag_100", a1frag_100.getBundleId()); //$NON-NLS-1$
		state.updateBundle(a1frag_100);
		state.resolve(new BundleDescription[] {a1frag_100});

		assertTrue("3.1", a1_100.isResolved()); //$NON-NLS-1$
		assertFalse("3.2", a1frag_100.isResolved()); //$NON-NLS-1$
		assertTrue("3.3", b1_100.isResolved()); //$NON-NLS-1$
		assertTrue("3.4", c1_100.isResolved()); //$NON-NLS-1$
		assertFalse("3.5", d1_100.isResolved()); //$NON-NLS-1$
	}

	public void testFragmentConstraints02() throws BundleException {
		int id = 0;
		State state = buildEmptyState();
		Hashtable manifest = new Hashtable();

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "a"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "a"); //$NON-NLS-1$
		BundleDescription a = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), id++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "aFrag1"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "a"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "b, c, d"); //$NON-NLS-1$
		BundleDescription aFrag1 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), id++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "aFrag2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "a"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "b, c, e"); //$NON-NLS-1$
		BundleDescription aFrag2 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), id++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "aFrag3"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "a"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "b, c"); //$NON-NLS-1$
		BundleDescription aFrag3 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), id++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "b"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "a, b, c"); //$NON-NLS-1$
		BundleDescription b = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), id++); //$NON-NLS-1$

		state.addBundle(a);
		state.addBundle(aFrag1);
		state.addBundle(aFrag2);
		state.addBundle(aFrag3);
		state.addBundle(b);
		state.resolve();

		assertTrue("0.1", a.isResolved()); //$NON-NLS-1$
		assertFalse("0.2", aFrag1.isResolved()); //$NON-NLS-1$
		assertFalse("0.3", aFrag2.isResolved()); //$NON-NLS-1$
		assertTrue("0.4", aFrag3.isResolved()); //$NON-NLS-1$
		assertTrue("0.5", b.isResolved()); //$NON-NLS-1$

		ExportPackageDescription[] aResolvedImports = a.getResolvedImports();
		ExportPackageDescription[] bSelectedExports = b.getSelectedExports();

		assertEquals("1.0", 3, aResolvedImports.length); //$NON-NLS-1$
		assertEquals("1.1", 3, bSelectedExports.length); //$NON-NLS-1$
		for (int i = 0; i < aResolvedImports.length; i++) {
			assertEquals(bSelectedExports[i], aResolvedImports[i]);
		}
	}

	public void testFragmentConstraints03() throws BundleException {
		// same as testFragmentConstraints02 but with a cycle
		int id = 0;
		State state = buildEmptyState();
		Hashtable manifest = new Hashtable();

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "a"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "a"); //$NON-NLS-1$
		BundleDescription a = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), id++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "aFrag1"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "a"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "b, c, d"); //$NON-NLS-1$
		BundleDescription aFrag1 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), id++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "aFrag2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "a"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "b, c, e"); //$NON-NLS-1$
		BundleDescription aFrag2 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), id++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "aFrag3"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "a"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "b, c"); //$NON-NLS-1$
		BundleDescription aFrag3 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), id++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "b"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "a, b, c"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x"); //$NON-NLS-1$
		BundleDescription b = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), id++); //$NON-NLS-1$

		state.addBundle(a);
		state.addBundle(aFrag1);
		state.addBundle(aFrag2);
		state.addBundle(aFrag3);
		state.addBundle(b);
		state.resolve();

		assertTrue("0.1", a.isResolved()); //$NON-NLS-1$
		assertFalse("0.2", aFrag1.isResolved()); //$NON-NLS-1$
		assertFalse("0.3", aFrag2.isResolved()); //$NON-NLS-1$
		assertTrue("0.4", aFrag3.isResolved()); //$NON-NLS-1$
		assertTrue("0.5", b.isResolved()); //$NON-NLS-1$

		ExportPackageDescription[] aResolvedImports = a.getResolvedImports();
		ExportPackageDescription[] aSelectedExports = a.getSelectedExports();
		ExportPackageDescription[] bResolvedImports = b.getResolvedImports();
		ExportPackageDescription[] bSelectedExports = b.getSelectedExports();

		assertEquals("1.0", 3, aResolvedImports.length); //$NON-NLS-1$
		assertEquals("1.1", 3, bSelectedExports.length); //$NON-NLS-1$
		for (int i = 0; i < aResolvedImports.length; i++)
			assertEquals("1.2", bSelectedExports[i], aResolvedImports[i]); //$NON-NLS-1$
		assertEquals("2.0", 1, aSelectedExports.length); //$NON-NLS-1$
		assertEquals("2.1", 1, bResolvedImports.length); //$NON-NLS-1$
		assertEquals("2.2", aSelectedExports[0], bResolvedImports[0]); //$NON-NLS-1$
	}

	public void testFragmentConstraints04() throws BundleException {
		int id = 0;
		State state = buildEmptyState();
		Hashtable manifest = new Hashtable();

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "a"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "b"); //$NON-NLS-1$
		BundleDescription a = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), id++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "aFrag1"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "a"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "c, d, e"); //$NON-NLS-1$
		BundleDescription aFrag1 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), id++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "aFrag2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "a"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "c, d, f"); //$NON-NLS-1$
		BundleDescription aFrag2 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), id++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "aFrag3"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "a"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "c, d"); //$NON-NLS-1$
		BundleDescription aFrag3 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), id++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "b"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "b1, b2, b3"); //$NON-NLS-1$
		BundleDescription b = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), id++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "c"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "c1, c2, c3"); //$NON-NLS-1$
		BundleDescription c = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), id++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "d"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "d1, d2, d3"); //$NON-NLS-1$
		BundleDescription d = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), id++); //$NON-NLS-1$

		state.addBundle(a);
		state.addBundle(aFrag1);
		state.addBundle(aFrag2);
		state.addBundle(aFrag3);
		state.addBundle(b);
		state.addBundle(c);
		state.addBundle(d);
		state.resolve();

		assertTrue("0.1", a.isResolved()); //$NON-NLS-1$
		assertFalse("0.2", aFrag1.isResolved()); //$NON-NLS-1$
		assertFalse("0.3", aFrag2.isResolved()); //$NON-NLS-1$
		assertTrue("0.4", aFrag3.isResolved()); //$NON-NLS-1$
		assertTrue("0.5", b.isResolved()); //$NON-NLS-1$
		assertTrue("0.6", c.isResolved()); //$NON-NLS-1$
		assertTrue("0.7", d.isResolved()); //$NON-NLS-1$

		BundleDescription[] aResolvedRequires = a.getResolvedRequires();
		assertEquals("1.0", 3, aResolvedRequires.length); //$NON-NLS-1$
		assertEquals("1.1", b, aResolvedRequires[0]); //$NON-NLS-1$
		assertEquals("1.2", c, aResolvedRequires[1]); //$NON-NLS-1$
		assertEquals("1.3", d, aResolvedRequires[2]); //$NON-NLS-1$
	}

	public void testFragmentConstraints05() throws BundleException {
		// same as testFragmentConstraints04 but with a cycle
		int id = 0;
		State state = buildEmptyState();
		Hashtable manifest = new Hashtable();

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "a"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "b"); //$NON-NLS-1$
		BundleDescription a = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), id++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "aFrag1"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "a"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "c, d, e"); //$NON-NLS-1$
		BundleDescription aFrag1 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), id++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "aFrag2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "a"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "c, d, f"); //$NON-NLS-1$
		BundleDescription aFrag2 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), id++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "aFrag3"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "a"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "c, d"); //$NON-NLS-1$
		BundleDescription aFrag3 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), id++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "b"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "b1, b2, b3"); //$NON-NLS-1$
		BundleDescription b = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), id++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "c"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "c1, c2, c3"); //$NON-NLS-1$
		BundleDescription c = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), id++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "d"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "a"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "d1, d2, d3"); //$NON-NLS-1$
		BundleDescription d = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), id++); //$NON-NLS-1$

		state.addBundle(a);
		state.addBundle(aFrag1);
		state.addBundle(aFrag2);
		state.addBundle(aFrag3);
		state.addBundle(b);
		state.addBundle(c);
		state.addBundle(d);
		state.resolve();

		assertTrue("0.1", a.isResolved()); //$NON-NLS-1$
		assertFalse("0.2", aFrag1.isResolved()); //$NON-NLS-1$
		assertFalse("0.3", aFrag2.isResolved()); //$NON-NLS-1$
		assertTrue("0.4", aFrag3.isResolved()); //$NON-NLS-1$
		assertTrue("0.5", b.isResolved()); //$NON-NLS-1$
		assertTrue("0.6", c.isResolved()); //$NON-NLS-1$
		assertTrue("0.7", d.isResolved()); //$NON-NLS-1$

		BundleDescription[] aResolvedRequires = a.getResolvedRequires();
		assertEquals("1.0", 3, aResolvedRequires.length); //$NON-NLS-1$
		assertEquals("1.1", b, aResolvedRequires[0]); //$NON-NLS-1$
		assertEquals("1.2", c, aResolvedRequires[1]); //$NON-NLS-1$
		assertEquals("1.3", d, aResolvedRequires[2]); //$NON-NLS-1$
		BundleDescription[] dResolvedRequires = d.getResolvedRequires();
		assertEquals("2.0", 1, dResolvedRequires.length); //$NON-NLS-1$
		assertEquals("2.1", a, dResolvedRequires[0]); //$NON-NLS-1$
	}

	public void testFragmentConstraints06() throws BundleException {
		int id = 0;
		State state = buildEmptyState();
		Hashtable manifest = new Hashtable();

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "a"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "a"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "b"); //$NON-NLS-1$
		BundleDescription a = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), id++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "aFrag1"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "a"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "b, c, d"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "c, d, e"); //$NON-NLS-1$
		BundleDescription aFrag1 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), id++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "aFrag2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "a"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "b, c, e"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "c, d, f"); //$NON-NLS-1$
		BundleDescription aFrag2 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), id++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "aFrag3"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "a"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "b, c"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "c, d"); //$NON-NLS-1$
		BundleDescription aFrag3 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), id++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "b"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "a, b, c"); //$NON-NLS-1$
		BundleDescription b = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), id++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "c"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "c1, c2, c3"); //$NON-NLS-1$
		BundleDescription c = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), id++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "d"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "d1, d2, d3"); //$NON-NLS-1$
		BundleDescription d = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), id++); //$NON-NLS-1$

		state.addBundle(a);
		state.addBundle(aFrag1);
		state.addBundle(aFrag2);
		state.addBundle(aFrag3);
		state.addBundle(b);
		state.addBundle(c);
		state.addBundle(d);
		state.resolve();

		assertTrue("0.1", a.isResolved()); //$NON-NLS-1$
		assertFalse("0.2", aFrag1.isResolved()); //$NON-NLS-1$
		assertFalse("0.3", aFrag2.isResolved()); //$NON-NLS-1$
		assertTrue("0.4", aFrag3.isResolved()); //$NON-NLS-1$
		assertTrue("0.5", b.isResolved()); //$NON-NLS-1$
		assertTrue("0.6", c.isResolved()); //$NON-NLS-1$
		assertTrue("0.7", d.isResolved()); //$NON-NLS-1$

		ExportPackageDescription[] aResolvedImports = a.getResolvedImports();
		ExportPackageDescription[] bSelectedExports = b.getSelectedExports();

		assertEquals("1.0", 3, aResolvedImports.length); //$NON-NLS-1$
		assertEquals("1.1", 3, bSelectedExports.length); //$NON-NLS-1$
		for (int i = 0; i < aResolvedImports.length; i++) {
			assertEquals(bSelectedExports[i], aResolvedImports[i]);
		}

		BundleDescription[] aResolvedRequires = a.getResolvedRequires();
		assertEquals("1.0", 3, aResolvedRequires.length); //$NON-NLS-1$
		assertEquals("1.1", b, aResolvedRequires[0]); //$NON-NLS-1$
		assertEquals("1.2", c, aResolvedRequires[1]); //$NON-NLS-1$
		assertEquals("1.3", d, aResolvedRequires[2]); //$NON-NLS-1$
	}

	public void testFragmentConstraints07() throws BundleException {
		// same as testFragmentConstraints06 but with a cycle
		int id = 0;
		State state = buildEmptyState();
		Hashtable manifest = new Hashtable();

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "a"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "a"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "b"); //$NON-NLS-1$
		BundleDescription a = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), id++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "aFrag1"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "a"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "b, c, d"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "c, d, e"); //$NON-NLS-1$
		BundleDescription aFrag1 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), id++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "aFrag2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "a"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "b, c, e"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "c, d, f"); //$NON-NLS-1$
		BundleDescription aFrag2 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), id++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "aFrag3"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "a"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "b, c"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "c, d"); //$NON-NLS-1$
		BundleDescription aFrag3 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), id++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "b"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "a, b, c"); //$NON-NLS-1$
		BundleDescription b = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), id++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "c"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "c1, c2, c3"); //$NON-NLS-1$
		BundleDescription c = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), id++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "d"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "d1, d2, d3"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "a"); //$NON-NLS-1$
		BundleDescription d = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), id++); //$NON-NLS-1$

		state.addBundle(a);
		state.addBundle(aFrag1);
		state.addBundle(aFrag2);
		state.addBundle(aFrag3);
		state.addBundle(b);
		state.addBundle(c);
		state.addBundle(d);
		state.resolve();

		assertTrue("0.1", a.isResolved()); //$NON-NLS-1$
		assertFalse("0.2", aFrag1.isResolved()); //$NON-NLS-1$
		assertFalse("0.3", aFrag2.isResolved()); //$NON-NLS-1$
		assertTrue("0.4", aFrag3.isResolved()); //$NON-NLS-1$
		assertTrue("0.5", b.isResolved()); //$NON-NLS-1$
		assertTrue("0.6", c.isResolved()); //$NON-NLS-1$
		assertTrue("0.7", d.isResolved()); //$NON-NLS-1$

		ExportPackageDescription[] aResolvedImports = a.getResolvedImports();
		ExportPackageDescription[] aSelectedExports = a.getSelectedExports();
		ExportPackageDescription[] bSelectedExports = b.getSelectedExports();
		ExportPackageDescription[] dResolvedImports = d.getResolvedImports();

		assertEquals("1.0", 3, aResolvedImports.length); //$NON-NLS-1$
		assertEquals("1.1", 3, bSelectedExports.length); //$NON-NLS-1$
		for (int i = 0; i < aResolvedImports.length; i++) {
			assertEquals(bSelectedExports[i], aResolvedImports[i]);
		}

		BundleDescription[] aResolvedRequires = a.getResolvedRequires();
		assertEquals("1.0", 3, aResolvedRequires.length); //$NON-NLS-1$
		assertEquals("1.1", b, aResolvedRequires[0]); //$NON-NLS-1$
		assertEquals("1.2", c, aResolvedRequires[1]); //$NON-NLS-1$
		assertEquals("1.3", d, aResolvedRequires[2]); //$NON-NLS-1$
		assertEquals("2.0", 1, aSelectedExports.length); //$NON-NLS-1$
		assertEquals("2.1", 1, dResolvedImports.length); //$NON-NLS-1$
		assertEquals("2.2", aSelectedExports[0], dResolvedImports[0]); //$NON-NLS-1$
	}

	public void testFragmentsBug188199() throws BundleException {
		State state = buildEmptyState();
		int bundleID = 0;
		// test the selection algorithm of the resolver to pick the bundles which
		// resolve the largest set of bundles; with fragments using Import-Package
		Hashtable manifest = new Hashtable();

		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "a"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "c"); //$NON-NLS-1$
		BundleDescription a = state.getFactory().createBundleDescription(state, manifest, "A", bundleID++); //$NON-NLS-1$

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "AFrag"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "A"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "a.frag"); //$NON-NLS-1$
		BundleDescription aFrag = state.getFactory().createBundleDescription(state, manifest, "A", bundleID++); //$NON-NLS-1$

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "a, a.frag"); //$NON-NLS-1$
		BundleDescription b = state.getFactory().createBundleDescription(state, manifest, "B", bundleID++); //$NON-NLS-1$

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "C"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "c"); //$NON-NLS-1$
		BundleDescription c = state.getFactory().createBundleDescription(state, manifest, "C", bundleID++); //$NON-NLS-1$

		state.addBundle(a);
		state.addBundle(aFrag);
		state.addBundle(b);
		state.addBundle(c);
		state.resolve();
		assertTrue("0.1", a.isResolved()); //$NON-NLS-1$
		assertTrue("0.2", aFrag.isResolved()); //$NON-NLS-1$
		assertTrue("0.3", b.isResolved()); //$NON-NLS-1$
		assertTrue("0.4", c.isResolved()); //$NON-NLS-1$

		state.removeBundle(c);
		state.resolve(false);
		assertFalse("1.1", a.isResolved()); //$NON-NLS-1$
		assertFalse("1.2", aFrag.isResolved()); //$NON-NLS-1$
		assertFalse("1.3", b.isResolved()); //$NON-NLS-1$

		state.addBundle(c);
		state.resolve();
		assertTrue("2.1", a.isResolved()); //$NON-NLS-1$
		assertTrue("2.2", aFrag.isResolved()); //$NON-NLS-1$
		assertTrue("2.3", b.isResolved()); //$NON-NLS-1$
		assertTrue("2.4", c.isResolved()); //$NON-NLS-1$

		ExportPackageDescription[] aExports = a.getSelectedExports();
		ExportPackageDescription[] bImports = b.getResolvedImports();
		assertTrue("3.1", aExports.length == 2); //$NON-NLS-1$
		assertTrue("3.2", bImports.length == 2); //$NON-NLS-1$
		assertTrue("3.3", aExports[0] == bImports[0]); //$NON-NLS-1$
		assertTrue("3.4", aExports[1] == bImports[1]); //$NON-NLS-1$
	}

	public void testFragmentsMultipleVersion() throws BundleException {
		State state = buildEmptyState();
		int bundleID = 0;
		// test the selection algorithm of the resolver to pick the bundles which
		// resolve the largest set of bundles; with fragments using Import-Package
		Hashtable manifest = new Hashtable();

		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		BundleDescription a1 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + '_' + (String) manifest.get(Constants.BUNDLE_VERSION), bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "AFrag"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "A"); //$NON-NLS-1$
		BundleDescription aFrag1 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + '_' + (String) manifest.get(Constants.BUNDLE_VERSION), bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "2.0"); //$NON-NLS-1$
		BundleDescription a2 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + '_' + (String) manifest.get(Constants.BUNDLE_VERSION), bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "AFrag"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "2.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "A"); //$NON-NLS-1$
		BundleDescription aFrag2 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + '_' + (String) manifest.get(Constants.BUNDLE_VERSION), bundleID++);

		state.addBundle(a1);
		state.addBundle(aFrag1);
		state.addBundle(a2);
		state.addBundle(aFrag2);
		state.resolve();
		assertTrue("0.1", a1.isResolved()); //$NON-NLS-1$
		assertTrue("0.2", aFrag1.isResolved()); //$NON-NLS-1$
		assertTrue("0.3", a2.isResolved()); //$NON-NLS-1$
		assertTrue("0.4", aFrag2.isResolved()); //$NON-NLS-1$

		state.removeBundle(a2);
		state.resolve(false);
		assertTrue("1.1", a1.isResolved()); //$NON-NLS-1$
		assertTrue("1.2", aFrag2.isResolved()); //$NON-NLS-1$
		assertFalse("1.3", aFrag1.isResolved()); //$NON-NLS-1$
		assertEquals("1.4", a1, aFrag2.getHost().getSupplier()); //$NON-NLS-1$
	}

	public void testPlatformProperties01() throws BundleException {
		State state = buildEmptyState();
		int bundleID = 0;
		// test the selection algorithm of the resolver to pick the bundles which
		// resolve the largest set of bundles; with fragments using Import-Package
		Hashtable manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "org.eclipse.osgi"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		BundleDescription systemBundle = state.getFactory().createBundleDescription(state, manifest, "org.eclipse.osgi", bundleID++); //$NON-NLS-1$

		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "pkg.b, pkg.system.b"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, "J2SE-1.4"); //$NON-NLS-1$
		BundleDescription a = state.getFactory().createBundleDescription(state, manifest, "A", bundleID++); //$NON-NLS-1$

		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "pkg.b"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "pkg.system.b"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, "J2SE-1.2"); //$NON-NLS-1$
		BundleDescription b = state.getFactory().createBundleDescription(state, manifest, "B", bundleID++); //$NON-NLS-1$

		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "system.b"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "pkg.system.b"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, "J2SE-1.2"); //$NON-NLS-1$
		BundleDescription systemB = state.getFactory().createBundleDescription(state, manifest, "system.b", bundleID++); //$NON-NLS-1$

		Dictionary[] props = new Dictionary[] {new Hashtable(), new Hashtable()};
		props[0].put("org.osgi.framework.system.packages", "pkg.system.a, pkg.system.c"); //$NON-NLS-1$ //$NON-NLS-2$
		props[0].put("org.osgi.framework.executionenvironment", "J2SE-1.2"); //$NON-NLS-1$ //$NON-NLS-2$
		props[1].put("org.osgi.framework.system.packages", "pkg.system.a, pkg.system.b, pkg.system.c"); //$NON-NLS-1$ //$NON-NLS-2$
		props[1].put("org.osgi.framework.executionenvironment", "J2SE-1.4"); //$NON-NLS-1$ //$NON-NLS-2$

		state.setPlatformProperties(props);
		state.addBundle(systemBundle);
		state.addBundle(a);
		state.addBundle(b);
		state.addBundle(systemB);
		state.resolve();

		assertTrue("1.0", systemBundle.isResolved()); //$NON-NLS-1$
		assertTrue("1.1", a.isResolved()); //$NON-NLS-1$
		assertTrue("1.2", b.isResolved()); //$NON-NLS-1$
		assertTrue("1.3", systemB.isResolved()); //$NON-NLS-1$

		assertTrue("2.0", a.getResolvedImports()[1].getExporter() == systemBundle); //$NON-NLS-1$
		assertTrue("2.1", b.getResolvedImports()[0].getExporter() == systemB); //$NON-NLS-1$

		// now test the uses clause for pkg.b such that bundle 'A' will be forced to used
		// pkg.system from bundle 'system.b'
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "pkg.b; uses:=\"pkg.system.b\""); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "pkg.system.b"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, "J2SE-1.2"); //$NON-NLS-1$
		BundleDescription b_updated = state.getFactory().createBundleDescription(state, manifest, "B", b.getBundleId()); //$NON-NLS-1$
		state.updateBundle(b_updated);
		state.resolve(new BundleDescription[] {b_updated});

		assertTrue("3.0", systemBundle.isResolved()); //$NON-NLS-1$
		assertTrue("3.1", a.isResolved()); //$NON-NLS-1$
		assertTrue("3.2", b_updated.isResolved()); //$NON-NLS-1$
		assertTrue("3.3", systemB.isResolved()); //$NON-NLS-1$

		assertTrue("2.0", a.getResolvedImports()[1].getExporter() == systemB); //$NON-NLS-1$
		assertTrue("2.1", b_updated.getResolvedImports()[0].getExporter() == systemB); //$NON-NLS-1$
	}

	public void testPlatformProperties02() throws BundleException {
		// same as 01 except use alias system.bundle to another name "test.system.bundle"
		State state = buildEmptyState();
		int bundleID = 0;
		// test the selection algorithm of the resolver to pick the bundles which
		// resolve the largest set of bundles; with fragments using Import-Package
		Hashtable manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "test.system.bundle"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		BundleDescription systemBundle = state.getFactory().createBundleDescription(state, manifest, "test.system.bundle", bundleID++); //$NON-NLS-1$

		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "pkg.b, pkg.system.b"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, "J2SE-1.4"); //$NON-NLS-1$
		BundleDescription a = state.getFactory().createBundleDescription(state, manifest, "A", bundleID++); //$NON-NLS-1$

		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "pkg.b"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "pkg.system.b"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, "J2SE-1.2"); //$NON-NLS-1$
		BundleDescription b = state.getFactory().createBundleDescription(state, manifest, "B", bundleID++); //$NON-NLS-1$

		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "system.b"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "pkg.system.b"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, "J2SE-1.2"); //$NON-NLS-1$
		BundleDescription systemB = state.getFactory().createBundleDescription(state, manifest, "system.b", bundleID++); //$NON-NLS-1$

		Dictionary[] props = new Dictionary[] {new Hashtable(), new Hashtable()};
		props[0].put("org.osgi.framework.system.packages", "pkg.system.a, pkg.system.c"); //$NON-NLS-1$ //$NON-NLS-2$
		props[0].put("org.osgi.framework.executionenvironment", "J2SE-1.2"); //$NON-NLS-1$ //$NON-NLS-2$
		props[0].put("osgi.system.bundle", "test.system.bundle"); // set the system.bundle to another system bundle (other than org.eclipse.osgi) //$NON-NLS-1$ //$NON-NLS-2$
		props[1].put("org.osgi.framework.system.packages", "pkg.system.a, pkg.system.b, pkg.system.c"); //$NON-NLS-1$ //$NON-NLS-2$
		props[1].put("org.osgi.framework.executionenvironment", "J2SE-1.4"); //$NON-NLS-1$ //$NON-NLS-2$

		state.setPlatformProperties(props);
		state.addBundle(systemBundle);
		state.addBundle(a);
		state.addBundle(b);
		state.addBundle(systemB);
		state.resolve();

		assertTrue("1.0", systemBundle.isResolved()); //$NON-NLS-1$
		assertTrue("1.1", a.isResolved()); //$NON-NLS-1$
		assertTrue("1.2", b.isResolved()); //$NON-NLS-1$
		assertTrue("1.3", systemB.isResolved()); //$NON-NLS-1$

		assertTrue("2.0", a.getResolvedImports()[1].getExporter() == systemBundle); //$NON-NLS-1$
		assertTrue("2.1", b.getResolvedImports()[0].getExporter() == systemB); //$NON-NLS-1$

		// now test the uses clause for pkg.b such that bundle 'A' will be forced to used
		// pkg.system from bundle 'system.b'
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "pkg.b; uses:=\"pkg.system.b\""); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "pkg.system.b"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, "J2SE-1.2"); //$NON-NLS-1$
		BundleDescription b_updated = state.getFactory().createBundleDescription(state, manifest, "B", b.getBundleId()); //$NON-NLS-1$
		state.updateBundle(b_updated);
		state.resolve(new BundleDescription[] {b_updated});

		assertTrue("3.0", systemBundle.isResolved()); //$NON-NLS-1$
		assertTrue("3.1", a.isResolved()); //$NON-NLS-1$
		assertTrue("3.2", b_updated.isResolved()); //$NON-NLS-1$
		assertTrue("3.3", systemB.isResolved()); //$NON-NLS-1$

		assertTrue("2.0", a.getResolvedImports()[1].getExporter() == systemB); //$NON-NLS-1$
		assertTrue("2.1", b_updated.getResolvedImports()[0].getExporter() == systemB); //$NON-NLS-1$
	}

	public void testPlatformProperties03() throws BundleException {
		// test that require-bundle, fragment-host, and import-package of system.bundle can be aliased properly
		State state = buildEmptyState();
		int bundleID = 0;
		String configuredSystemBundle = "org.eclipse.osgi"; //$NON-NLS-1$
		// test the selection algorithm of the resolver to pick the bundles which
		// resolve the largest set of bundles; with fragments using Import-Package
		Hashtable manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, configuredSystemBundle);
		manifest.put(Constants.EXPORT_PACKAGE, "system.bundle.exported.pkg"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		BundleDescription systemBundle = state.getFactory().createBundleDescription(state, manifest, configuredSystemBundle, bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "system.bundle"); //$NON-NLS-1$
		BundleDescription a = state.getFactory().createBundleDescription(state, manifest, "A", bundleID++); //$NON-NLS-1$

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, configuredSystemBundle);
		BundleDescription b = state.getFactory().createBundleDescription(state, manifest, "B", bundleID++); //$NON-NLS-1$

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "C"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "system.bundle.exported.pkg; bundle-symbolic-name=\"system.bundle\""); //$NON-NLS-1$
		BundleDescription c = state.getFactory().createBundleDescription(state, manifest, "C", bundleID++); //$NON-NLS-1$

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "D"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "system.bundle.exported.pkg; bundle-symbolic-name=\"" + configuredSystemBundle + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		BundleDescription d = state.getFactory().createBundleDescription(state, manifest, "D", bundleID++); //$NON-NLS-1$

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "E"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "pkg.system.a; bundle-symbolic-name=\"system.bundle\""); //$NON-NLS-1$
		BundleDescription e = state.getFactory().createBundleDescription(state, manifest, "E", bundleID++); //$NON-NLS-1$

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "F"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "pkg.system.a; bundle-symbolic-name=\"" + configuredSystemBundle + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		BundleDescription f = state.getFactory().createBundleDescription(state, manifest, "F", bundleID++); //$NON-NLS-1$

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "G"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "system.bundle"); //$NON-NLS-1$
		BundleDescription g = state.getFactory().createBundleDescription(state, manifest, "G", bundleID++); //$NON-NLS-1$

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "H"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, configuredSystemBundle);
		BundleDescription h = state.getFactory().createBundleDescription(state, manifest, "H", bundleID++); //$NON-NLS-1$

		Dictionary[] props = new Dictionary[] {new Hashtable(), new Hashtable()};
		props[0].put("org.osgi.framework.system.packages", "pkg.system.a, pkg.system.b"); //$NON-NLS-1$ //$NON-NLS-2$
		props[0].put("org.osgi.framework.executionenvironment", "J2SE-1.2"); //$NON-NLS-1$ //$NON-NLS-2$

		state.setPlatformProperties(props);
		state.addBundle(systemBundle);
		state.addBundle(a);
		state.addBundle(b);
		state.addBundle(c);
		state.addBundle(d);
		state.addBundle(e);
		state.addBundle(f);
		state.addBundle(g);
		state.addBundle(h);
		state.resolve();

		assertTrue("1.0", systemBundle.isResolved()); //$NON-NLS-1$
		assertTrue("1.1", a.isResolved()); //$NON-NLS-1$
		assertTrue("1.2", b.isResolved()); //$NON-NLS-1$
		assertTrue("1.3", c.isResolved()); //$NON-NLS-1$
		assertTrue("1.4", d.isResolved()); //$NON-NLS-1$
		assertTrue("1.5", e.isResolved()); //$NON-NLS-1$
		assertTrue("1.6", f.isResolved()); //$NON-NLS-1$
		assertTrue("1.7", g.isResolved()); //$NON-NLS-1$
		assertTrue("1.8", h.isResolved()); //$NON-NLS-1$

		assertTrue("2.0", a.getResolvedRequires()[0] == systemBundle); //$NON-NLS-1$
		assertTrue("2.1", b.getResolvedRequires()[0] == systemBundle); //$NON-NLS-1$
		assertTrue("2.2", c.getResolvedImports()[0].getExporter() == systemBundle); //$NON-NLS-1$
		assertTrue("2.3", d.getResolvedImports()[0].getExporter() == systemBundle); //$NON-NLS-1$
		assertTrue("2.4", e.getResolvedImports()[0].getExporter() == systemBundle); //$NON-NLS-1$
		assertTrue("2.5", f.getResolvedImports()[0].getExporter() == systemBundle); //$NON-NLS-1$
		assertTrue("2.6", g.getHost().getHosts()[0] == systemBundle); //$NON-NLS-1$
		assertTrue("2.7", h.getHost().getHosts()[0] == systemBundle); //$NON-NLS-1$
	}

	public void testPlatformProperties04() throws BundleException {
		// same as 03 except use a different system.bundle alias other than org.eclipse.osgi
		// test that require-bundle, fragment-host, and import-package of system.bundle can be aliased properly
		State state = buildEmptyState();
		int bundleID = 0;
		String configuredSystemBundle = "test.system.bundle"; //$NON-NLS-1$
		// test the selection algorithm of the resolver to pick the bundles which
		// resolve the largest set of bundles; with fragments using Import-Package
		Hashtable manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, configuredSystemBundle);
		manifest.put(Constants.EXPORT_PACKAGE, "system.bundle.exported.pkg"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		BundleDescription systemBundle = state.getFactory().createBundleDescription(state, manifest, configuredSystemBundle, bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "system.bundle"); //$NON-NLS-1$
		BundleDescription a = state.getFactory().createBundleDescription(state, manifest, "A", bundleID++); //$NON-NLS-1$

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, configuredSystemBundle);
		BundleDescription b = state.getFactory().createBundleDescription(state, manifest, "B", bundleID++); //$NON-NLS-1$

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "C"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "system.bundle.exported.pkg; bundle-symbolic-name=\"system.bundle\""); //$NON-NLS-1$
		BundleDescription c = state.getFactory().createBundleDescription(state, manifest, "C", bundleID++); //$NON-NLS-1$

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "D"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "system.bundle.exported.pkg; bundle-symbolic-name=\"" + configuredSystemBundle + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		BundleDescription d = state.getFactory().createBundleDescription(state, manifest, "D", bundleID++); //$NON-NLS-1$

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "E"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "pkg.system.a; bundle-symbolic-name=\"system.bundle\""); //$NON-NLS-1$
		BundleDescription e = state.getFactory().createBundleDescription(state, manifest, "E", bundleID++); //$NON-NLS-1$

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "F"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "pkg.system.a; bundle-symbolic-name=\"" + configuredSystemBundle + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		BundleDescription f = state.getFactory().createBundleDescription(state, manifest, "F", bundleID++); //$NON-NLS-1$

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "G"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "system.bundle"); //$NON-NLS-1$
		BundleDescription g = state.getFactory().createBundleDescription(state, manifest, "G", bundleID++); //$NON-NLS-1$

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "H"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, configuredSystemBundle);
		BundleDescription h = state.getFactory().createBundleDescription(state, manifest, "H", bundleID++); //$NON-NLS-1$

		Dictionary[] props = new Dictionary[] {new Hashtable(), new Hashtable()};
		props[0].put("org.osgi.framework.system.packages", "pkg.system.a, pkg.system.b"); //$NON-NLS-1$ //$NON-NLS-2$
		props[0].put("org.osgi.framework.executionenvironment", "J2SE-1.2"); //$NON-NLS-1$ //$NON-NLS-2$
		props[0].put("osgi.system.bundle", configuredSystemBundle); // set the system.bundle to another system bundle (other than org.eclipse.osgi) //$NON-NLS-1$

		state.setPlatformProperties(props);
		state.addBundle(systemBundle);
		state.addBundle(a);
		state.addBundle(b);
		state.addBundle(c);
		state.addBundle(d);
		state.addBundle(e);
		state.addBundle(f);
		state.addBundle(g);
		state.addBundle(h);
		state.resolve();

		assertTrue("1.0", systemBundle.isResolved()); //$NON-NLS-1$
		assertTrue("1.1", a.isResolved()); //$NON-NLS-1$
		assertTrue("1.2", b.isResolved()); //$NON-NLS-1$
		assertTrue("1.3", c.isResolved()); //$NON-NLS-1$
		assertTrue("1.4", d.isResolved()); //$NON-NLS-1$
		assertTrue("1.5", e.isResolved()); //$NON-NLS-1$
		assertTrue("1.6", f.isResolved()); //$NON-NLS-1$
		assertTrue("1.7", g.isResolved()); //$NON-NLS-1$
		assertTrue("1.8", h.isResolved()); //$NON-NLS-1$

		assertTrue("2.0", a.getResolvedRequires()[0] == systemBundle); //$NON-NLS-1$
		assertTrue("2.1", b.getResolvedRequires()[0] == systemBundle); //$NON-NLS-1$
		assertTrue("2.2", c.getResolvedImports()[0].getExporter() == systemBundle); //$NON-NLS-1$
		assertTrue("2.3", d.getResolvedImports()[0].getExporter() == systemBundle); //$NON-NLS-1$
		assertTrue("2.4", e.getResolvedImports()[0].getExporter() == systemBundle); //$NON-NLS-1$
		assertTrue("2.5", f.getResolvedImports()[0].getExporter() == systemBundle); //$NON-NLS-1$
		assertTrue("2.6", g.getHost().getHosts()[0] == systemBundle); //$NON-NLS-1$
		assertTrue("2.7", h.getHost().getHosts()[0] == systemBundle); //$NON-NLS-1$
	}

	public void testPlatformPropertiesBug188075() throws BundleException, IOException {
		State state = buildEmptyState();
		int bundleID = 0;

		Hashtable manifest = new Hashtable();

		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put("Eclipse-PlatformFilter", "(!(test=value))"); //$NON-NLS-1$ //$NON-NLS-2$
		BundleDescription a = state.getFactory().createBundleDescription(state, manifest, "A", bundleID++); //$NON-NLS-1$

		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put("Eclipse-PlatformFilter", "(test=value)"); //$NON-NLS-1$ //$NON-NLS-2$
		BundleDescription b = state.getFactory().createBundleDescription(state, manifest, "B", bundleID++); //$NON-NLS-1$

		Dictionary props = new Hashtable();
		props.put("test", "value"); //$NON-NLS-1$ //$NON-NLS-2$

		state.setPlatformProperties(props);
		state.addBundle(a);
		state.addBundle(b);
		state.resolve();

		assertFalse("1.1", a.isResolved()); //$NON-NLS-1$
		assertTrue("1.2", b.isResolved()); //$NON-NLS-1$

		BundleContext context = OSGiTestsActivator.getContext();
		File stateCache = context.getDataFile("statecache"); //$NON-NLS-1$
		stateCache.mkdirs();
		StateObjectFactory.defaultFactory.writeState(state, stateCache);
		state = StateObjectFactory.defaultFactory.readState(stateCache);
		props = state.getPlatformProperties()[0];
		assertEquals("2.0", "value", props.get("test")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		BundleDescription aCache = state.getBundle("A", null); //$NON-NLS-1$
		BundleDescription bCache = state.getBundle("B", null); //$NON-NLS-1$
		assertFalse("2.1", aCache.isResolved()); //$NON-NLS-1$
		assertTrue("2.2", bCache.isResolved()); //$NON-NLS-1$
	}

	public void testPlatformPropertiesBug207500a() throws BundleException, IOException {
		State state = buildEmptyState();
		int bundleID = 0;

		Hashtable manifest = new Hashtable();

		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put("Eclipse-PlatformFilter", "(test=value1)"); //$NON-NLS-1$ //$NON-NLS-2$
		BundleDescription a = state.getFactory().createBundleDescription(state, manifest, "A", bundleID++); //$NON-NLS-1$

		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put("Eclipse-PlatformFilter", "(test=value2)"); //$NON-NLS-1$ //$NON-NLS-2$
		BundleDescription b = state.getFactory().createBundleDescription(state, manifest, "B", bundleID++); //$NON-NLS-1$

		Dictionary props = new Hashtable();
		props.put("test", new CatchAllValue("*")); //$NON-NLS-1$ //$NON-NLS-2$

		state.setPlatformProperties(props);
		state.addBundle(a);
		state.addBundle(b);
		state.resolve();

		// Both should resolve
		assertTrue("1.1", a.isResolved()); //$NON-NLS-1$
		assertTrue("1.2", b.isResolved()); //$NON-NLS-1$

		BundleContext context = OSGiTestsActivator.getContext();
		File stateCache = context.getDataFile("statecache"); //$NON-NLS-1$
		stateCache.mkdirs();
		StateObjectFactory.defaultFactory.writeState(state, stateCache);
		state = StateObjectFactory.defaultFactory.readState(stateCache);
		props = state.getPlatformProperties()[0];
		// we do not persist custom properties
		assertNull("2.0", props.get("test")); //$NON-NLS-1$ //$NON-NLS-2$
		BundleDescription aCache = state.getBundle("A", null); //$NON-NLS-1$
		BundleDescription bCache = state.getBundle("B", null); //$NON-NLS-1$
		assertTrue("2.1", aCache.isResolved()); //$NON-NLS-1$
		assertTrue("2.2", bCache.isResolved()); //$NON-NLS-1$

		// re-resolve without the custom value
		state.setResolver(platformAdmin.getResolver());
		state.resolve(false);
		// both should fail to resolve
		assertFalse("3.1", aCache.isResolved()); //$NON-NLS-1$
		assertFalse("3.2", bCache.isResolved()); //$NON-NLS-1$
	}

	public void testPlatformPropertiesBug207500b() throws BundleException, IOException {
		State state = buildEmptyState();
		int bundleID = 0;

		Hashtable manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "a"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_NATIVECODE, "libwrapper-linux-x86-32.so; wrapper-linux-x86-32; osname=linux; processor=x86"); //$NON-NLS-1$
		BundleDescription a = state.getFactory().createBundleDescription(state, manifest, "A", bundleID++); //$NON-NLS-1$

		Dictionary props = new Hashtable();
		props.put("osgi.os", new CatchAllValue("*")); //$NON-NLS-1$ //$NON-NLS-2$
		props.put("osgi.arch", new CatchAllValue("*")); //$NON-NLS-1$ //$NON-NLS-2$
		props.put("osgi.lang", new CatchAllValue("*")); //$NON-NLS-1$ //$NON-NLS-2$
		props.put("osgi.ws", new CatchAllValue("*")); //$NON-NLS-1$ //$NON-NLS-2$

		state.setPlatformProperties(props);
		state.addBundle(a);
		state.resolve();

		// should resolve
		assertTrue("1.1", a.isResolved()); //$NON-NLS-1$
	}

	public void testPlatformPropertiesBug246640a() throws BundleException {
		State state = buildEmptyState();
		int bundleID = 0;
		String configuredSystemBundle = "org.eclipse.osgi; singleton:=true"; //$NON-NLS-1$
		// test the selection algorithm of the resolver to pick the bundles which
		// resolve the largest set of bundles; with fragments using Import-Package
		Hashtable manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, configuredSystemBundle);
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		BundleDescription systemBundle1 = state.getFactory().createBundleDescription(state, manifest, configuredSystemBundle + manifest.get(Constants.BUNDLE_VERSION), bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, configuredSystemBundle);
		manifest.put(Constants.BUNDLE_VERSION, "2.0"); //$NON-NLS-1$
		BundleDescription systemBundle2 = state.getFactory().createBundleDescription(state, manifest, configuredSystemBundle + manifest.get(Constants.BUNDLE_VERSION), bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "pkg.system.b"); //$NON-NLS-1$
		BundleDescription a = state.getFactory().createBundleDescription(state, manifest, "A", bundleID++); //$NON-NLS-1$

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "require.system.b"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "org.eclipse.osgi; bundle-version=2.0"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, "J2SE-1.2"); //$NON-NLS-1$
		BundleDescription requireSystemBundle = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		Dictionary[] props = new Dictionary[] {new Hashtable(), new Hashtable()};
		props[0].put("org.osgi.framework.system.packages", "pkg.system.a, pkg.system.c"); //$NON-NLS-1$ //$NON-NLS-2$
		props[0].put("org.osgi.framework.executionenvironment", "J2SE-1.2"); //$NON-NLS-1$ //$NON-NLS-2$
		props[1].put("org.osgi.framework.system.packages", "pkg.system.a, pkg.system.b, pkg.system.c"); //$NON-NLS-1$ //$NON-NLS-2$
		props[1].put("org.osgi.framework.executionenvironment", "J2SE-1.4"); //$NON-NLS-1$ //$NON-NLS-2$

		state.setPlatformProperties(props);
		state.addBundle(systemBundle1);
		state.addBundle(systemBundle2);
		state.addBundle(a);
		state.addBundle(requireSystemBundle);
		state.resolve();

		assertFalse("1.0", systemBundle1.isResolved()); //$NON-NLS-1$
		assertTrue("1.0.1", systemBundle2.isResolved()); //$NON-NLS-1$
		assertTrue("1.1", a.isResolved()); //$NON-NLS-1$
		assertTrue("1.4", requireSystemBundle.isResolved()); //$NON-NLS-1$

		assertTrue("2.0", a.getResolvedImports()[0].getExporter() == systemBundle2); //$NON-NLS-1$
	}

	public void testPlatformPropertiesBug246640b() throws BundleException {
		State state = buildEmptyState();
		int bundleID = 0;
		String configuredSystemBundle = "test.system.bundle; singleton:=true"; //$NON-NLS-1$
		// test the selection algorithm of the resolver to pick the bundles which
		// resolve the largest set of bundles; with fragments using Import-Package
		Hashtable manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, configuredSystemBundle);
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		BundleDescription systemBundle1 = state.getFactory().createBundleDescription(state, manifest, configuredSystemBundle + manifest.get(Constants.BUNDLE_VERSION), bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, configuredSystemBundle);
		manifest.put(Constants.BUNDLE_VERSION, "2.0"); //$NON-NLS-1$
		BundleDescription systemBundle2 = state.getFactory().createBundleDescription(state, manifest, configuredSystemBundle + manifest.get(Constants.BUNDLE_VERSION), bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "pkg.system.b"); //$NON-NLS-1$
		BundleDescription a = state.getFactory().createBundleDescription(state, manifest, "A", bundleID++); //$NON-NLS-1$

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "require.system.b"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "test.system.bundle; bundle-version=2.0"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, "J2SE-1.2"); //$NON-NLS-1$
		BundleDescription requireSystemBundle = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		Dictionary[] props = new Dictionary[] {new Hashtable(), new Hashtable()};
		props[0].put("osgi.system.bundle", "test.system.bundle"); // set the system.bundle to another system bundle (other than org.eclipse.osgi) //$NON-NLS-1$ //$NON-NLS-2$
		props[0].put("org.osgi.framework.system.packages", "pkg.system.a, pkg.system.c"); //$NON-NLS-1$ //$NON-NLS-2$
		props[0].put("org.osgi.framework.executionenvironment", "J2SE-1.2"); //$NON-NLS-1$ //$NON-NLS-2$
		props[1].put("org.osgi.framework.system.packages", "pkg.system.a, pkg.system.b, pkg.system.c"); //$NON-NLS-1$ //$NON-NLS-2$
		props[1].put("org.osgi.framework.executionenvironment", "J2SE-1.4"); //$NON-NLS-1$ //$NON-NLS-2$

		state.setPlatformProperties(props);
		state.addBundle(systemBundle1);
		state.addBundle(systemBundle2);
		state.addBundle(a);
		state.addBundle(requireSystemBundle);
		state.resolve();

		assertFalse("1.0", systemBundle1.isResolved()); //$NON-NLS-1$
		assertTrue("1.0.1", systemBundle2.isResolved()); //$NON-NLS-1$
		assertTrue("1.1", a.isResolved()); //$NON-NLS-1$
		assertTrue("1.4", requireSystemBundle.isResolved()); //$NON-NLS-1$

		assertTrue("2.0", a.getResolvedImports()[0].getExporter() == systemBundle2); //$NON-NLS-1$
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

	private static final String MANIFEST_ROOT = "test_files/resolverTests/";

	private Dictionary loadManifest(String manifest) {
		URL url = getContext().getBundle().getEntry(MANIFEST_ROOT + manifest);
		try {
			return Headers.parseManifest(url.openStream());
		} catch (IOException e) {
			fail("Unexpected error loading manifest: " + manifest, e);
		} catch (BundleException e) {
			fail("Unexpected error loading manifest: " + manifest, e);
		}
		return null;
	}

	public void testSelectionPolicy() throws BundleException {
		State state = buildEmptyState();
		Resolver resolver = state.getResolver();
		resolver.setSelectionPolicy(new Comparator() {
			public int compare(Object o1, Object o2) {
				if (!(o1 instanceof BaseDescription) || !(o2 instanceof BaseDescription))
					throw new IllegalArgumentException();
				Version v1 = null;
				Version v2 = null;
				v1 = ((BaseDescription) o1).getVersion();
				v2 = ((BaseDescription) o2).getVersion();
				// only take version in to account and use lower versions over higher ones
				return v1.compareTo(v2);
			}
		});
		Hashtable manifest = new Hashtable();
		long bundleID = 0;
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "test.host; singleton:=true"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		BundleDescription testHost100 = state.getFactory().createBundleDescription(state, manifest, "test.host100", bundleID++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "test.host; singleton:=true"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.1"); //$NON-NLS-1$
		BundleDescription testHost101 = state.getFactory().createBundleDescription(state, manifest, "test.host101", bundleID++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "test.frag; singleton:=true"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "test.host; bundle-version=\"[1.0.0,2.0.0)\""); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		BundleDescription testFrag100 = state.getFactory().createBundleDescription(state, manifest, "test.frag100", bundleID++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "test.frag; singleton:=true"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "test.host; bundle-version=\"[1.0.0,2.0.0)\""); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.1"); //$NON-NLS-1$
		BundleDescription testFrag101 = state.getFactory().createBundleDescription(state, manifest, "test.frag101", bundleID++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "test.dependent; singleton:=true"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "test.host; bundle-version=\"[1.0.0,2.0.0)\""); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		BundleDescription testDependent = state.getFactory().createBundleDescription(state, manifest, "test.frag101", bundleID++); //$NON-NLS-1$

		state.addBundle(testHost100);
		state.addBundle(testFrag100);
		state.addBundle(testHost101);
		state.addBundle(testFrag101);
		state.addBundle(testDependent);
		state.resolve();
		assertTrue("1.0", testHost100.isResolved()); //$NON-NLS-1$
		assertFalse("1.1", testHost101.isResolved()); //$NON-NLS-1$
		assertTrue("1.2", testFrag100.isResolved()); //$NON-NLS-1$
		assertFalse("1.3", testFrag101.isResolved()); //$NON-NLS-1$
		assertTrue("1.4", testDependent.isResolved()); //$NON-NLS-1$
	}

	public void testBug187616() throws BundleException {
		State state = buildEmptyState();
		Hashtable manifest = new Hashtable();
		long bundleID = 0;
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "test.host; singleton:=true"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		BundleDescription testHost100 = state.getFactory().createBundleDescription(state, manifest, "test.host100", bundleID++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "test.host; singleton:=true"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.1"); //$NON-NLS-1$
		BundleDescription testHost101 = state.getFactory().createBundleDescription(state, manifest, "test.host101", bundleID++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "test.frag; singleton:=true"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "test.host; bundle-version=\"[1.0.0,2.0.0)\""); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		BundleDescription testFrag100 = state.getFactory().createBundleDescription(state, manifest, "test.frag100", bundleID++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "test.frag; singleton:=true"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "test.host; bundle-version=\"[1.0.0,2.0.0)\""); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.1"); //$NON-NLS-1$
		BundleDescription testFrag101 = state.getFactory().createBundleDescription(state, manifest, "test.frag101", bundleID++); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "test.dependent; singleton:=true"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "test.host; bundle-version=\"[1.0.0,2.0.0)\""); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		BundleDescription testDependent = state.getFactory().createBundleDescription(state, manifest, "test.frag101", bundleID++); //$NON-NLS-1$

		state.addBundle(testHost100);
		state.addBundle(testFrag100);
		state.addBundle(testHost101);
		state.addBundle(testFrag101);
		state.addBundle(testDependent);
		StateDelta stateDelta = state.resolve();
		assertFalse("1.0", testHost100.isResolved()); //$NON-NLS-1$
		assertTrue("1.1", testHost101.isResolved()); //$NON-NLS-1$
		assertFalse("1.2", testFrag100.isResolved()); //$NON-NLS-1$
		assertTrue("1.3", testFrag101.isResolved()); //$NON-NLS-1$
		assertTrue("1.4", testDependent.isResolved()); //$NON-NLS-1$
		BundleDelta[] bDelta = stateDelta.getChanges(BundleDelta.ADDED | BundleDelta.RESOLVED, false);
		assertTrue("2.0", bDelta.length == 5); //$NON-NLS-1$
	}

	public void testBug217150() throws BundleException {
		State state = buildEmptyState();
		Hashtable manifest = new Hashtable();
		long bundleID = 0;
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "test.host; singleton:=true"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		BundleDescription testHost100 = state.getFactory().createBundleDescription(state, manifest, "test.host100", bundleID++); //$NON-NLS-1$

		try {
			testHost100.getFragments();
			fail("Expected to get an exception here!!!"); //$NON-NLS-1$
		} catch (IllegalStateException e) {
			// Expected exception.
		}

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "test.frag; singleton:=true"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "test.host; bundle-version=\"[1.0.0,2.0.0)\""); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		BundleDescription testFrag100 = state.getFactory().createBundleDescription(state, manifest, "test.frag100", bundleID++); //$NON-NLS-1$
		BundleDescription[] hosts = testFrag100.getHost().getHosts();
		assertNotNull("hosts is null", hosts); //$NON-NLS-1$
		assertEquals("Unexpected number of hosts", 0, hosts.length); //$NON-NLS-1$
	}

	public void testNativeCodeResolution01() throws BundleException {
		State state = buildEmptyState();
		Dictionary[] props = new Dictionary[] {new Hashtable()};
		props[0].put("osgi.ws", "win32"); //$NON-NLS-1$ //$NON-NLS-2$
		props[0].put("osgi.os", "win32"); //$NON-NLS-1$ //$NON-NLS-2$
		props[0].put("osgi.arch", "x86"); //$NON-NLS-1$ //$NON-NLS-2$
		props[0].put("osgi.nl", "en_US"); //$NON-NLS-1$ //$NON-NLS-2$
		state.setPlatformProperties(props);

		Hashtable manifest = new Hashtable();
		long bundleID = 0;
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "NativeBundle"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_NATIVECODE, "Bundle-NativeCode: nativefile1.txt;processor=x86;osname=Windows2000;osname=\"Windows 2003\";osname=Windows95;osname=Windows98;osname=WindowsNT;osname=WindowsXP;osname=\"Windows NT (unknown)\";osname=\"Windows Vista\"; language=en"); //$NON-NLS-1$
		BundleDescription testNativeBundle = state.getFactory().createBundleDescription(state, manifest, "NativeBundle", bundleID++); //$NON-NLS-1$
		state.addBundle(testNativeBundle);
		state.resolve();
		assertTrue("1.0", testNativeBundle.isResolved()); //$NON-NLS-1$

	}

	public void testNativeCodeResolution02() throws BundleException {
		State state = buildEmptyState();
		Dictionary[] props = new Dictionary[] {new Hashtable()};
		props[0].put(Constants.FRAMEWORK_OS_NAME, "WIN32"); // Use different case for osname //$NON-NLS-1$
		props[0].put(Constants.FRAMEWORK_PROCESSOR, "x86"); //$NON-NLS-1$
		state.setPlatformProperties(props);

		Hashtable manifest = new Hashtable();
		long bundleID = 0;
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "NativeBundle"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_NATIVECODE, "Bundle-NativeCode: nativefile1.txt;processor=x86;osname=Windows2000;osname=\"Windows 2003\";osname=Windows95;osname=Windows98;osname=WindowsNT;osname=WindowsXP;osname=\"Windows NT (unknown)\";osname=\"Windows Vista\""); //$NON-NLS-1$
		BundleDescription testNativeBundle = state.getFactory().createBundleDescription(state, manifest, "NativeBundle", bundleID++); //$NON-NLS-1$
		state.addBundle(testNativeBundle);
		state.resolve();
		assertTrue("1.0", testNativeBundle.isResolved()); //$NON-NLS-1$
	}

	public void testNativeCodeResolution03() throws BundleException {
		State state = buildEmptyState();
		Dictionary[] props = new Dictionary[] {new Hashtable()};
		props[0].put(Constants.FRAMEWORK_OS_NAME, "win32"); //$NON-NLS-1$
		props[0].put(Constants.FRAMEWORK_PROCESSOR, "X86"); // Use different case for processor //$NON-NLS-1$
		state.setPlatformProperties(props);

		Hashtable manifest = new Hashtable();
		long bundleID = 0;
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "NativeBundle"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_NATIVECODE, "Bundle-NativeCode: nativefile1.txt;processor=x86;osname=Windows2000;osname=\"Windows 2003\";osname=Windows95;osname=Windows98;osname=WindowsNT;osname=WindowsXP;osname=\"Windows NT (unknown)\";osname=\"Windows Vista\""); //$NON-NLS-1$
		BundleDescription testNativeBundle = state.getFactory().createBundleDescription(state, manifest, "NativeBundle", bundleID++); //$NON-NLS-1$
		state.addBundle(testNativeBundle);
		state.resolve();
		assertTrue("1.0", testNativeBundle.isResolved()); //$NON-NLS-1$
	}

	public void testNativeCodeResolution04() throws BundleException {
		State state = buildEmptyState();
		Dictionary[] props = new Dictionary[] {new Hashtable()};
		props[0].put(Constants.FRAMEWORK_OS_NAME, "DoesNotExist"); // Use different case for osname //$NON-NLS-1$
		props[0].put(Constants.FRAMEWORK_PROCESSOR, "InVaLid"); // Use different case for processor //$NON-NLS-1$
		state.setPlatformProperties(props);

		Hashtable manifest = new Hashtable();
		long bundleID = 0;
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "NativeBundle"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_NATIVECODE, "Bundle-NativeCode: nativefile1.txt;processor=iNvAlid;osname=doeSnoTexist"); //$NON-NLS-1$
		BundleDescription testNativeBundle = state.getFactory().createBundleDescription(state, manifest, "NativeBundle", bundleID++); //$NON-NLS-1$
		state.addBundle(testNativeBundle);
		state.resolve();
		assertTrue("1.0", testNativeBundle.isResolved()); //$NON-NLS-1$
	}

	public void testMultiStateAdd01() throws BundleException {
		State state1 = buildEmptyState();

		// test the selection algorithm of the resolver to pick the bundles which
		// resolve the largest set of bundles
		Hashtable manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "sdk; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "platform; bundle-version=\"[1.0,2.0]\""); //$NON-NLS-1$
		BundleDescription sdk10 = state1.getFactory().createBundleDescription(state1, manifest, "sdk10", 0); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "platform; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "rcp; bundle-version=\"[1.0,2.0]\""); //$NON-NLS-1$
		BundleDescription platform10 = state1.getFactory().createBundleDescription(state1, manifest, "platform10", 1); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "rcp; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		BundleDescription rcp10 = state1.getFactory().createBundleDescription(state1, manifest, "rcp10", 2); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "gef; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "rcp; bundle-version=\"[1.0,1.0]\""); //$NON-NLS-1$
		BundleDescription gef10 = state1.getFactory().createBundleDescription(state1, manifest, "gef10", 3); //$NON-NLS-1$

		state1.addBundle(sdk10);
		state1.addBundle(platform10);
		state1.addBundle(rcp10);
		state1.addBundle(gef10);
		state1.resolve();
		assertTrue("1.0", sdk10.isResolved()); //$NON-NLS-1$
		assertTrue("1.1", platform10.isResolved()); //$NON-NLS-1$
		assertTrue("1.2", rcp10.isResolved()); //$NON-NLS-1$
		assertTrue("1.3", gef10.isResolved()); //$NON-NLS-1$

		State state2 = buildEmptyState();
		try {
			state2.addBundle(rcp10);
			fail("Expected IllegalStateException on adding to multiple states"); //$NON-NLS-1$
		} catch (IllegalStateException e) {
			// expected
		}
	}

	public void testMultiStateAdd02() throws BundleException {
		State state1 = buildEmptyState();

		// test the selection algorithm of the resolver to pick the bundles which
		// resolve the largest set of bundles
		Hashtable manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "sdk; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "platform; bundle-version=\"[1.0,2.0]\""); //$NON-NLS-1$
		BundleDescription sdk10 = state1.getFactory().createBundleDescription(state1, manifest, "sdk10", 0); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "platform; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "rcp; bundle-version=\"[1.0,2.0]\""); //$NON-NLS-1$
		BundleDescription platform10 = state1.getFactory().createBundleDescription(state1, manifest, "platform10", 1); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "rcp; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		BundleDescription rcp10 = state1.getFactory().createBundleDescription(state1, manifest, "rcp10", 2); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "gef; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "rcp; bundle-version=\"[1.0,1.0]\""); //$NON-NLS-1$
		BundleDescription gef10 = state1.getFactory().createBundleDescription(state1, manifest, "gef10", 3); //$NON-NLS-1$

		state1.addBundle(sdk10);
		state1.addBundle(platform10);
		state1.addBundle(rcp10);
		state1.addBundle(gef10);
		state1.resolve();
		assertTrue("1.0", sdk10.isResolved()); //$NON-NLS-1$
		assertTrue("1.1", platform10.isResolved()); //$NON-NLS-1$
		assertTrue("1.2", rcp10.isResolved()); //$NON-NLS-1$
		assertTrue("1.3", gef10.isResolved()); //$NON-NLS-1$

		// remove the rcp10 bundle.  The bundle will be removal pending
		// this should still throw an exception until the removal is no longer pending
		state1.removeBundle(rcp10);
		State state2 = buildEmptyState();
		try {
			state2.addBundle(rcp10);
			fail("Expected IllegalStateException on adding to multiple states"); //$NON-NLS-1$
		} catch (IllegalStateException e) {
			// expected
		}
	}

	public void testMultiStateAdd03() throws BundleException {
		State state1 = buildEmptyState();

		// test the selection algorithm of the resolver to pick the bundles which
		// resolve the largest set of bundles
		Hashtable manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "sdk; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "platform; bundle-version=\"[1.0,2.0]\""); //$NON-NLS-1$
		BundleDescription sdk10 = state1.getFactory().createBundleDescription(state1, manifest, "sdk10", 0); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "platform; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "rcp; bundle-version=\"[1.0,2.0]\""); //$NON-NLS-1$
		BundleDescription platform10 = state1.getFactory().createBundleDescription(state1, manifest, "platform10", 1); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "rcp; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		BundleDescription rcp10 = state1.getFactory().createBundleDescription(state1, manifest, "rcp10", 2); //$NON-NLS-1$

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "gef; " + Constants.SINGLETON_DIRECTIVE + ":=true"); //$NON-NLS-1$ //$NON-NLS-2$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "rcp; bundle-version=\"[1.0,1.0]\""); //$NON-NLS-1$
		BundleDescription gef10 = state1.getFactory().createBundleDescription(state1, manifest, "gef10", 3); //$NON-NLS-1$

		state1.addBundle(sdk10);
		state1.addBundle(platform10);
		state1.addBundle(rcp10);
		state1.addBundle(gef10);
		state1.resolve();
		assertTrue("1.0", sdk10.isResolved()); //$NON-NLS-1$
		assertTrue("1.1", platform10.isResolved()); //$NON-NLS-1$
		assertTrue("1.2", rcp10.isResolved()); //$NON-NLS-1$
		assertTrue("1.3", gef10.isResolved()); //$NON-NLS-1$

		// remove the rcp10 bundle.  The bundle will be removal pending
		// this should still throw an exception until the removal is no longer pending
		state1.removeBundle(rcp10);
		state1.resolve(new BundleDescription[] {rcp10});
		State state2 = buildEmptyState();

		try {
			state2.addBundle(rcp10);
		} catch (IllegalStateException e) {
			fail("Unexpected IllegalStateException on adding to state", e); //$NON-NLS-1$
		}
	}

	private State createBug266935State() throws BundleException {
		State state = buildEmptyState();
		int bundleID = 0;
		// test the selection algorithm of the resolver to pick the bundles which
		// resolve the largest set of bundles; with fragments
		Hashtable manifest = new Hashtable();
		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "a"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		BundleDescription a = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + manifest.get(Constants.BUNDLE_VERSION), bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "b"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "a"); //$NON-NLS-1$
		BundleDescription b = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + manifest.get(Constants.BUNDLE_VERSION), bundleID++);

		state.addBundle(a);
		state.addBundle(b);

		state.resolve();
		assertTrue("A is not resolved", a.isResolved()); //$NON-NLS-1$
		assertTrue("B is not resolved", b.isResolved()); //$NON-NLS-1$
		return state;
	}

	private BundleDescription updateStateBug266935(State state, BundleDescription a) throws BundleException {
		Hashtable manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, a.getSymbolicName());
		manifest.put(Constants.BUNDLE_VERSION, a.getVersion().toString());
		BundleDescription newA = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + manifest.get(Constants.BUNDLE_VERSION), a.getBundleId());
		state.updateBundle(newA);
		return newA;
	}

	public void testBug266935_01() throws BundleException {
		State state = createBug266935State();
		BundleDescription a = state.getBundle("a", new Version("1.0")); //$NON-NLS-1$ //$NON-NLS-2$
		BundleDescription newA = updateStateBug266935(state, a);
		state.removeBundle(a);
		state.resolve(new BundleDescription[] {newA});
	}

	public void testBug266935_02() throws BundleException {
		State state = createBug266935State();
		BundleDescription a = state.getBundle("a", new Version("1.0")); //$NON-NLS-1$ //$NON-NLS-2$
		updateStateBug266935(state, a);
		state.removeBundle(a);
		state.resolve(new BundleDescription[] {a});
	}

	public void testBug266935_03() throws BundleException {
		State state = createBug266935State();
		BundleDescription a = state.getBundle("a", new Version("1.0")); //$NON-NLS-1$ //$NON-NLS-2$
		BundleDescription newA = updateStateBug266935(state, a);
		state.removeBundle(newA);
		state.resolve(new BundleDescription[] {newA});
	}

	public void testBug266935_04() throws BundleException {
		State state = createBug266935State();
		BundleDescription a = state.getBundle("a", new Version("1.0")); //$NON-NLS-1$ //$NON-NLS-2$
		BundleDescription newA = updateStateBug266935(state, a);
		state.removeBundle(newA);
		state.resolve(new BundleDescription[] {a});
	}

	public void testBug320124() throws BundleException {
		State state = buildEmptyState();
		int bundleID = 0;

		Hashtable manifest = new Hashtable();
		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "a"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		BundleDescription a = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + manifest.get(Constants.BUNDLE_VERSION), bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "b"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "a"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "d"); //$NON-NLS-1$
		BundleDescription b = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + manifest.get(Constants.BUNDLE_VERSION), bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "c"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "a"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "e"); //$NON-NLS-1$
		BundleDescription c = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + manifest.get(Constants.BUNDLE_VERSION), bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "d"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "d"); //$NON-NLS-1$
		BundleDescription d = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + manifest.get(Constants.BUNDLE_VERSION), bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "e"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "e"); //$NON-NLS-1$
		BundleDescription e = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + manifest.get(Constants.BUNDLE_VERSION), bundleID++);

		state.addBundle(a);
		state.addBundle(b);
		state.addBundle(c);
		state.addBundle(d);
		state.addBundle(e);

		state.resolve();
		assertTrue("A is not resolved", a.isResolved()); //$NON-NLS-1$
		assertTrue("B is not resolved", b.isResolved()); //$NON-NLS-1$
		assertTrue("C is not resolved", c.isResolved()); //$NON-NLS-1$
		assertTrue("D is not resolved", d.isResolved()); //$NON-NLS-1$

		ExportPackageDescription[] visible = state.getStateHelper().getVisiblePackages(a, StateHelper.VISIBLE_INCLUDE_EE_PACKAGES | StateHelper.VISIBLE_INCLUDE_ALL_HOST_WIRES);

		assertEquals("Wrong number of visible", 2, visible.length);
	}

	public static class CatchAllValue {
		public CatchAllValue(String s) {
			//do nothing
		}

		public boolean equals(Object obj) {
			return true;
		}
	}

	public void testBug324618() throws BundleException {
		State state = buildEmptyState();
		long bundleID = 0;
		Dictionary manifest;

		manifest = loadManifest("p1.MF");
		BundleDescription p1 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);
		manifest = loadManifest("p2.MF");
		BundleDescription p2 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);
		manifest = loadManifest("c1.MF");
		BundleDescription c1 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		state.addBundle(p1);
		state.addBundle(p2);
		state.addBundle(c1);

		state.resolve();

		ExportPackageDescription x = state.linkDynamicImport(c1, "x");
		assertNotNull("x dynamic import is null", x);
		ExportPackageDescription xSub = state.linkDynamicImport(c1, "x.sub");
		assertNotNull("x.sub dynamic import is null", xSub);
		assertEquals("The exporter is not the same for x and x.sub", x.getExporter(), xSub.getExporter());

		ExportPackageDescription xExtra = state.linkDynamicImport(c1, "x.extra");
		assertNotNull("x.extra dynamic import is null", xExtra);
	}
}
//testFragmentUpdateNoVersionChanged()
//testFragmentUpdateVersionChanged()
//testHostUpdateNoVersionChanged()
//testHostUpdateVersionChanged()
