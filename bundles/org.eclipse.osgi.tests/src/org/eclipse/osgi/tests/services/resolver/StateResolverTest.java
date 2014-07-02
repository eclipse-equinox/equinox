/*******************************************************************************
 * Copyright (c) 2003, 2014 IBM Corporation and others.
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
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.wiring.*;
import org.osgi.resource.Capability;

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
		dependent = state.getStateHelper().getDependentBundles(new BundleDescription[] {state.getBundle(2)});
		assertEquals("1.0", 1, dependent.length); //$NON-NLS-1$
		assertEquals("1.1", state.getBundle(2), dependent[0]); //$NON-NLS-1$
		dependent = state.getStateHelper().getDependentBundles(new BundleDescription[] {state.getBundle(1)});
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
		prereqs = state.getStateHelper().getPrerequisites(state.getResolvedBundles());
		assertEquals("1.0", 6, prereqs.length); //$NON-NLS-1$
		prereqs = state.getStateHelper().getPrerequisites(new BundleDescription[] {state.getBundle(1)});
		assertEquals("2.0", 2, prereqs.length); //$NON-NLS-1$
		assertContains("2.1", prereqs, state.getBundle(1)); //$NON-NLS-1$
		assertContains("2.2", prereqs, state.getBundle(3)); //$NON-NLS-1$
		prereqs = state.getStateHelper().getPrerequisites(new BundleDescription[] {state.getBundle(2)});
		assertEquals("3.0", 3, prereqs.length); //$NON-NLS-1$
		assertContains("3.1", prereqs, state.getBundle(1)); //$NON-NLS-1$
		assertContains("3.2", prereqs, state.getBundle(2)); //$NON-NLS-1$
		assertContains("3.3", prereqs, state.getBundle(3)); //$NON-NLS-1$
		prereqs = state.getStateHelper().getPrerequisites(new BundleDescription[] {state.getBundle(3)});
		assertEquals("4.0", 1, prereqs.length); //$NON-NLS-1$
		assertContains("4.1", prereqs, state.getBundle(3)); //$NON-NLS-1$
		prereqs = state.getStateHelper().getPrerequisites(new BundleDescription[] {state.getBundle(4)});
		assertEquals("5.0", 3, prereqs.length); //$NON-NLS-1$
		assertContains("5.1", prereqs, state.getBundle(1)); //$NON-NLS-1$
		assertContains("5.2", prereqs, state.getBundle(3)); //$NON-NLS-1$
		assertContains("5.3", prereqs, state.getBundle(4)); //$NON-NLS-1$
		prereqs = state.getStateHelper().getPrerequisites(new BundleDescription[] {state.getBundle(5)});
		assertEquals("6.0", 2, prereqs.length); //$NON-NLS-1$
		assertContains("6.1", prereqs, state.getBundle(3)); //$NON-NLS-1$
		assertContains("6.2", prereqs, state.getBundle(5)); //$NON-NLS-1$
		prereqs = state.getStateHelper().getPrerequisites(new BundleDescription[] {state.getBundle(6)});
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
		State state = buildEmptyState();
		BundleDescription b1 = state.getFactory().createBundleDescription(parseManifest(B1_MANIFEST), B1_LOCATION, 1);
		BundleDescription b2 = state.getFactory().createBundleDescription(parseManifest(B2_MANIFEST), B2_LOCATION, 2);
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
		State state = buildEmptyState();
		BundleDescription b1 = state.getFactory().createBundleDescription(parseManifest(B_MANIFEST), B_LOCATION, 1);
		BundleDescription b2 = state.getFactory().createBundleDescription(parseManifest(B_MANIFEST), B_LOCATION, 2);
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

		// test the selection algorithm of the resolver to pick the bundles with the largest version
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
		assertTrue("2.2", rcp20.isResolved()); //$NON-NLS-1$
		assertFalse("2.3", gef10.isResolved()); //$NON-NLS-1$
		assertFalse("2.4", sdk10.isResolved()); //$NON-NLS-1$
		assertFalse("2.5", platform10.isResolved()); //$NON-NLS-1$
		assertFalse("2.6", rcp10.isResolved()); //$NON-NLS-1$
	}

	public void testSingletonsSelection2() throws BundleException {
		State state = buildEmptyState();

		// test the selection algorithm of the resolver to pick the bundles with the largest version; test with cycle added
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
		BundleDescription cycle10 = state.getFactory().createBundleDescription(state, manifest, "cycle10", 4); //$NON-NLS-1$

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
		assertTrue("2.2", rcp20.isResolved()); //$NON-NLS-1$
		assertFalse("2.3", gef10.isResolved()); //$NON-NLS-1$
		assertFalse("2.4", cycle10.isResolved()); //$NON-NLS-1$
		assertFalse("2.5", sdk10.isResolved()); //$NON-NLS-1$
		assertFalse("2.6", platform10.isResolved()); //$NON-NLS-1$
		assertFalse("2.7", rcp10.isResolved()); //$NON-NLS-1$
	}

	public void testSingletonsSelection3() throws BundleException {
		State state = buildEmptyState();
		int bundleID = 0;
		// test the selection algorithm of the resolver to pick the bundles with the largest version; with fragments
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
		assertTrue("2.2", rcp20.isResolved()); //$NON-NLS-1$
		assertTrue("2.2.1", rcp_frag210.isResolved()); //$NON-NLS-1$
		assertTrue("2.2.2", rcp_frag10.isResolved()); //$NON-NLS-1$
		assertFalse("2.3", gef10.isResolved()); //$NON-NLS-1$
		assertFalse("2.4", sdk10.isResolved()); //$NON-NLS-1$
		assertFalse("2.5", platform10.isResolved()); //$NON-NLS-1$
		assertFalse("2.6", rcp10.isResolved()); //$NON-NLS-1$

	}

	public void testSingletonsSelection4() throws BundleException {
		State state = buildEmptyState();
		int bundleID = 0;
		// test the selection algorithm of the resolver to pick the bundles with the largest version; with fragments using Import-Package
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
		assertTrue("2.2", rcp20.isResolved()); //$NON-NLS-1$
		assertTrue("2.2.1", rcp_frag10.isResolved()); //$NON-NLS-1$
		assertTrue("2.2.2", rcp_frag210.isResolved()); //$NON-NLS-1$
		assertFalse("2.3", gef10.isResolved()); //$NON-NLS-1$
		assertFalse("2.4", sdk10.isResolved()); //$NON-NLS-1$
		assertFalse("2.5", platform10.isResolved()); //$NON-NLS-1$
		assertFalse("2.6", rcp10.isResolved()); //$NON-NLS-1$

	}

	public void testSingletonsSelection5() throws BundleException {
		State state = buildEmptyState();
		// test the selection algorithm of the resolver to pick the bundles with the largest version
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
		manifest.put(Constants.FRAGMENT_HOST, "requires; bundle-version=\"[1.1,1.2)\""); //$NON-NLS-1$
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

		assertTrue("1.0", base11.isResolved()); //$NON-NLS-1$
		assertTrue("1.1", requires11.isResolved()); //$NON-NLS-1$
		assertTrue("1.2", frag11.isResolved()); //$NON-NLS-1$
		assertTrue("1.3", fragb11.isResolved()); //$NON-NLS-1$
		assertTrue("1.4", import11.isResolved()); //$NON-NLS-1$
		assertFalse("1.5", base10.isResolved()); //$NON-NLS-1$
		assertFalse("1.6", requires10.isResolved()); //$NON-NLS-1$
		assertFalse("1.7", frag10.isResolved()); //$NON-NLS-1$
		assertFalse("1.8", fragb10.isResolved()); //$NON-NLS-1$
		assertFalse("1.9", import10.isResolved()); //$NON-NLS-1$
	}

	public void testSingletonsSelection6() throws BundleException {
		State state = buildEmptyState();

		// test the selection algorithm of the resolver to pick the bundles with the largest version
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
		assertTrue("2.2", rcp20.isResolved()); //$NON-NLS-1$
		assertFalse("2.3", gef10.isResolved()); //$NON-NLS-1$
		assertFalse("2.4", sdk10.isResolved()); //$NON-NLS-1$
		assertFalse("2.5", platform10.isResolved()); //$NON-NLS-1$
		assertFalse("2.6", rcp10.isResolved()); //$NON-NLS-1$
	}

	public void testSingletonsSelection7() throws BundleException {
		State state = buildEmptyState();
		long id = 0;
		// test the selection algorithm of the resolver to pick the bundles with the largest version
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

		// Bug 353103 need to create a bundle that has the same unresolved imports as other bundles
		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "aFrag4"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "a"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "b, c, d, e"); //$NON-NLS-1$
		BundleDescription aFrag4 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), id++); //$NON-NLS-1$

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
		state.addBundle(aFrag4);
		state.addBundle(b);
		state.resolve();

		assertTrue("0.1", a.isResolved()); //$NON-NLS-1$
		assertFalse("0.2", aFrag1.isResolved()); //$NON-NLS-1$
		assertFalse("0.3", aFrag2.isResolved()); //$NON-NLS-1$
		assertTrue("0.4", aFrag3.isResolved()); //$NON-NLS-1$
		assertFalse("0.5", aFrag4.isResolved()); //$NON-NLS-1$
		assertTrue("0.6", b.isResolved()); //$NON-NLS-1$

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

		// Bug 353103 need to create a bundle that has the same unresolved imports as other bundles
		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "aFrag4"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "a"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "b, c, d, e"); //$NON-NLS-1$
		BundleDescription aFrag4 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), id++); //$NON-NLS-1$

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
		state.addBundle(aFrag4);
		state.addBundle(b);
		state.resolve();

		assertTrue("0.1", a.isResolved()); //$NON-NLS-1$
		assertFalse("0.2", aFrag1.isResolved()); //$NON-NLS-1$
		assertFalse("0.3", aFrag2.isResolved()); //$NON-NLS-1$
		assertTrue("0.4", aFrag3.isResolved()); //$NON-NLS-1$
		assertFalse("0.5", aFrag4.isResolved()); //$NON-NLS-1$
		assertTrue("0.6", b.isResolved()); //$NON-NLS-1$

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

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "pkg.b, pkg.system.b"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, "J2SE-1.4"); //$NON-NLS-1$
		BundleDescription a = state.getFactory().createBundleDescription(state, manifest, "A", bundleID++); //$NON-NLS-1$

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "pkg.b"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "pkg.system.b"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, "J2SE-1.2"); //$NON-NLS-1$
		BundleDescription b = state.getFactory().createBundleDescription(state, manifest, "B", bundleID++); //$NON-NLS-1$

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "C"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "pkg.d, pkg.system.b"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_CAPABILITY, "osgi.ee; filter:=\"(&(osgi.ee=JavaSE)(version>=1.4))\""); //$NON-NLS-1$
		BundleDescription c = state.getFactory().createBundleDescription(state, manifest, "C", bundleID++); //$NON-NLS-1$

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "D"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "pkg.d"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "pkg.system.b"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_CAPABILITY, "osgi.ee; filter:=\"(&(osgi.ee=JavaSE)(version>=1.2))\""); //$NON-NLS-1$
		BundleDescription d = state.getFactory().createBundleDescription(state, manifest, "D", bundleID++); //$NON-NLS-1$

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "system.b"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "pkg.system.b"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, "J2SE-1.2"); //$NON-NLS-1$
		BundleDescription systemB = state.getFactory().createBundleDescription(state, manifest, "system.b", bundleID++); //$NON-NLS-1$

		Dictionary[] props = new Dictionary[] {new Hashtable(), new Hashtable()};
		props[0].put("org.osgi.framework.system.packages", "pkg.system.a, pkg.system.c"); //$NON-NLS-1$ //$NON-NLS-2$
		props[0].put("org.osgi.framework.executionenvironment", "J2SE-1.2"); //$NON-NLS-1$ //$NON-NLS-2$
		props[0].put("org.osgi.framework.system.capabilities", "osgi.ee; osgi.ee=\"JavaSE\"; version:Version=\"1.2\"");
		props[1].put("org.osgi.framework.system.packages", "pkg.system.a, pkg.system.b, pkg.system.c"); //$NON-NLS-1$ //$NON-NLS-2$
		props[1].put("org.osgi.framework.executionenvironment", "J2SE-1.4"); //$NON-NLS-1$ //$NON-NLS-2$
		props[1].put("org.osgi.framework.system.capabilities", "osgi.ee; osgi.ee=\"JavaSE\"; version:List<Version>=\"1.2, 1.3, 1.4\"");

		state.setPlatformProperties(props);
		state.addBundle(systemBundle);
		state.addBundle(a);
		state.addBundle(b);
		state.addBundle(c);
		state.addBundle(d);
		state.addBundle(systemB);
		state.resolve();

		Collection ids = systemBundle.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
		assertNotNull("Null osgi.identity", ids);
		assertEquals("Wrong number of identities", 1, ids.size());

		assertTrue("1.0", systemBundle.isResolved()); //$NON-NLS-1$
		assertTrue("1.1", a.isResolved()); //$NON-NLS-1$
		assertTrue("1.2", b.isResolved()); //$NON-NLS-1$
		assertTrue("1.3", c.isResolved()); //$NON-NLS-1$
		assertTrue("1.4", d.isResolved()); //$NON-NLS-1$
		assertTrue("1.5", systemB.isResolved()); //$NON-NLS-1$

		assertTrue("2.0", a.getResolvedImports()[1].getExporter() == systemBundle); //$NON-NLS-1$
		assertTrue("2.1", b.getResolvedImports()[0].getExporter() == systemB); //$NON-NLS-1$
		assertTrue("2.2", c.getResolvedImports()[1].getExporter() == systemBundle); //$NON-NLS-1$
		assertTrue("2.3", d.getResolvedImports()[0].getExporter() == systemB); //$NON-NLS-1$

		// now test the uses clause for pkg.b such that bundle 'A' will be forced to used
		// pkg.system.b from bundle 'system.b'
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "pkg.b; uses:=\"pkg.system.b\""); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "pkg.system.b"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, "J2SE-1.2"); //$NON-NLS-1$
		BundleDescription b_updated = state.getFactory().createBundleDescription(state, manifest, "B", b.getBundleId()); //$NON-NLS-1$
		state.updateBundle(b_updated);

		// now test the uses clause for pkg.d such that bundle 'C' will be forced to used
		// pkg.system.b from bundle 'system.b'
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "D"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "pkg.d; uses:=\"pkg.system.b\""); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "pkg.system.b"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, "J2SE-1.2"); //$NON-NLS-1$
		BundleDescription d_updated = state.getFactory().createBundleDescription(state, manifest, "D", d.getBundleId()); //$NON-NLS-1$
		state.updateBundle(d_updated);
		state.resolve(new BundleDescription[] {b_updated, d_updated});

		assertTrue("3.0", systemBundle.isResolved()); //$NON-NLS-1$
		assertTrue("3.1", a.isResolved()); //$NON-NLS-1$
		assertTrue("3.2", b_updated.isResolved()); //$NON-NLS-1$
		assertTrue("3.3", systemB.isResolved()); //$NON-NLS-1$

		assertTrue("2.0", a.getResolvedImports()[1].getExporter() == systemB); //$NON-NLS-1$
		assertTrue("2.1", b_updated.getResolvedImports()[0].getExporter() == systemB); //$NON-NLS-1$
		assertTrue("2.2", c.getResolvedImports()[1].getExporter() == systemB); //$NON-NLS-1$
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
		state.setResolver(platformAdminService.getResolver());
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

	public void testEECapabilityRequirement() throws BundleException {
		State state = buildEmptyState();
		int bundleID = 0;
		Hashtable manifest = new Hashtable();

		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "org.eclipse.osgi"); //$NON-NLS-1$
		BundleDescription systemBundle = state.getFactory().createBundleDescription(state, manifest, "org.eclipse.osgi", bundleID++); //$NON-NLS-1$
		state.addBundle(systemBundle);

		List bundles = new ArrayList();
		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, "OSGi/Minimum-1.1"); //$NON-NLS-1$
		BundleDescription a = state.getFactory().createBundleDescription(state, manifest, "A", bundleID++); //$NON-NLS-1$
		bundles.add(a);
		state.addBundle(a);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, "CDC-1.1/Foundation-1.1"); //$NON-NLS-1$
		BundleDescription b = state.getFactory().createBundleDescription(state, manifest, "B", bundleID++); //$NON-NLS-1$
		bundles.add(b);
		state.addBundle(b);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "C"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, "J2SE-1.4"); //$NON-NLS-1$
		BundleDescription c = state.getFactory().createBundleDescription(state, manifest, "C", bundleID++); //$NON-NLS-1$
		bundles.add(c);
		state.addBundle(c);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "D"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, "AA/BB"); //$NON-NLS-1$
		BundleDescription d = state.getFactory().createBundleDescription(state, manifest, "D", bundleID++); //$NON-NLS-1$
		bundles.add(d);
		state.addBundle(d);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "E"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, "CC-XX/DD-YY"); //$NON-NLS-1$
		BundleDescription e = state.getFactory().createBundleDescription(state, manifest, "E", bundleID++); //$NON-NLS-1$
		bundles.add(e);
		state.addBundle(e);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "F"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, "EE-2.0/FF-YY"); //$NON-NLS-1$
		BundleDescription f = state.getFactory().createBundleDescription(state, manifest, "F", bundleID++); //$NON-NLS-1$
		bundles.add(f);
		state.addBundle(f);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "G"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, "GG-XX/HH-1.0"); //$NON-NLS-1$
		BundleDescription g = state.getFactory().createBundleDescription(state, manifest, "G", bundleID++); //$NON-NLS-1$
		bundles.add(g);
		state.addBundle(g);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "H"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, "II-1.0/JJ-2.0"); //$NON-NLS-1$
		BundleDescription h = state.getFactory().createBundleDescription(state, manifest, "H", bundleID++); //$NON-NLS-1$
		bundles.add(h);
		state.addBundle(h);

		Dictionary[] props = new Dictionary[] {new Hashtable(), new Hashtable(), new Hashtable(), new Hashtable(), new Hashtable(), new Hashtable(), new Hashtable(), new Hashtable()};
		props[0].put("org.osgi.framework.executionenvironment", "OSGi/Minimum-1.0, OSGi/Minimum-1.1, OSGi/Minimum-1.2"); //$NON-NLS-1$ //$NON-NLS-2$
		props[1].put("org.osgi.framework.executionenvironment", "CDC-1.0/Foundation-1.0, CDC-1.1/Foundation-1.1"); //$NON-NLS-1$ //$NON-NLS-2$
		props[2].put("org.osgi.framework.executionenvironment", "J2SE-1.2, J2SE-1.3, J2SE-1.4, J2SE-1.5, JavaSE-1.6"); //$NON-NLS-1$ //$NON-NLS-2$
		props[3].put("org.osgi.framework.executionenvironment", "AA/BB");
		props[4].put("org.osgi.framework.executionenvironment", "CC-XX/DD-YY");
		props[5].put("org.osgi.framework.executionenvironment", "EE-1.0/FF-YY, EE-2.0/FF-YY");
		props[6].put("org.osgi.framework.executionenvironment", "GG-XX/HH-1.0, GG-XX/HH-2.0");
		props[7].put("org.osgi.framework.executionenvironment", "II-1.0/JJ-2.0");
		state.setPlatformProperties(props);

		state.resolve();

		checkEECapabilities(systemBundle.getWiring().getCapabilities("osgi.ee"), bundles);

		state.setPlatformProperties(new Hashtable());
		state.resolve(false);
		assertTrue("2.0", systemBundle.isResolved()); //$NON-NLS-1$
		assertFalse("2.1", a.isResolved()); //$NON-NLS-1$
		assertFalse("2.2", b.isResolved()); //$NON-NLS-1$
		assertFalse("2.3", c.isResolved()); //$NON-NLS-1$
		assertFalse("2.4", d.isResolved()); //$NON-NLS-1$
		assertFalse("2.5", e.isResolved()); //$NON-NLS-1$
		assertFalse("2.7", f.isResolved()); //$NON-NLS-1$
		assertFalse("2.8", g.isResolved()); //$NON-NLS-1$
		assertFalse("2.9", h.isResolved()); //$NON-NLS-1$

		props[0].put("org.osgi.framework.executionenvironment", "OSGi/Minimum-1.1"); //$NON-NLS-1$ //$NON-NLS-2$
		props[1].put("org.osgi.framework.executionenvironment", "CDC-1.1/Foundation-1.1"); //$NON-NLS-1$ //$NON-NLS-2$
		props[2].put("org.osgi.framework.executionenvironment", "J2SE-1.4"); //$NON-NLS-1$ //$NON-NLS-2$
		props[3].put("org.osgi.framework.executionenvironment", "AA/BB");
		props[4].put("org.osgi.framework.executionenvironment", "CC-XX/DD-YY");
		props[5].put("org.osgi.framework.executionenvironment", "EE-2.0/FF-YY");
		props[6].put("org.osgi.framework.executionenvironment", "GG-XX/HH-1.0");
		props[7].put("org.osgi.framework.executionenvironment", "II-1.0/JJ-2.0");
		props[0].put("org.osgi.framework.system.capabilities", "osgi.ee; osgi.ee=\"OSGi/Minimum\"; version:List<Version>=\"1.0, 1.1, 1.2\""); //$NON-NLS-1$ //$NON-NLS-2$
		props[1].put("org.osgi.framework.system.capabilities", "osgi.ee; osgi.ee=\"CDC/Foundation\"; version:List<Version>=\"1.0, 1.1\""); //$NON-NLS-1$ //$NON-NLS-2$
		props[2].put("org.osgi.framework.system.capabilities", "osgi.ee; osgi.ee=\"JavaSE\"; version:List<Version>=\"1.2, 1.3, 1.4, 1.5, 1.6\""); //$NON-NLS-1$ //$NON-NLS-2$
		props[3].put("org.osgi.framework.system.capabilities", "osgi.ee; osgi.ee=\"AA/BB\""); //$NON-NLS-1$ //$NON-NLS-2$
		props[4].put("org.osgi.framework.system.capabilities", "osgi.ee; osgi.ee=\"CC-XX/DD-YY\""); //$NON-NLS-1$ //$NON-NLS-2$
		props[5].put("org.osgi.framework.system.capabilities", "osgi.ee; osgi.ee=\"EE/FF-YY\"; version:List<Version>=\"1.0, 2.0\""); //$NON-NLS-1$ //$NON-NLS-2$
		props[6].put("org.osgi.framework.system.capabilities", "osgi.ee; osgi.ee=\"GG-XX/HH\"; version:List<Version>=\"1.0, 2.0\""); //$NON-NLS-1$ //$NON-NLS-2$
		props[7].put("org.osgi.framework.system.capabilities", "osgi.ee; osgi.ee=\"II-1.0/JJ-2.0\""); //$NON-NLS-1$ //$NON-NLS-2$

		state.setPlatformProperties(props);
		state.resolve(false);

		checkEECapabilities(systemBundle.getWiring().getCapabilities("osgi.ee"), bundles);
	}

	private void checkEECapabilities(List eeCapabilities, List bundles) {
		assertEquals("Wrong number of ee capabilities.", 8, eeCapabilities.size());
		for (int i = 0; i < eeCapabilities.size(); i++) {
			Capability eeCap = (Capability) eeCapabilities.get(i);
			assertEquals("Wrong namespace: " + eeCap, "osgi.ee", eeCap.getNamespace());
			Map attrs = eeCap.getAttributes();
			switch (i) {
				case 0 :
					assertEquals("Wrong ee name: " + i, "OSGi/Minimum", attrs.get("osgi.ee"));
					List v0 = (List) attrs.get("version");
					assertEquals("Wrong number of versions: " + i, 3, v0.size());
					assertTrue("Does not contain 1.0.", v0.contains(new Version("1.0")));
					assertTrue("Does not contain 1.1.", v0.contains(new Version("1.1")));
					assertTrue("Does not contain 1.2.", v0.contains(new Version("1.2")));
					break;
				case 1 :
					assertEquals("Wrong ee name: " + i, "CDC/Foundation", attrs.get("osgi.ee"));
					List v1 = (List) attrs.get("version");
					assertEquals("Wrong number of versions: " + i, 2, v1.size());
					assertTrue("Does not contain 1.0.", v1.contains(new Version("1.0")));
					assertTrue("Does not contain 1.1.", v1.contains(new Version("1.1")));
					break;
				case 2 :
					assertEquals("Wrong ee name: " + i, "JavaSE", attrs.get("osgi.ee"));
					List v2 = (List) attrs.get("version");
					assertEquals("Wrong number of versions: " + i, 5, v2.size());
					assertTrue("Does not contain 1.2.", v2.contains(new Version("1.2")));
					assertTrue("Does not contain 1.3.", v2.contains(new Version("1.3")));
					assertTrue("Does not contain 1.4.", v2.contains(new Version("1.4")));
					assertTrue("Does not contain 1.5.", v2.contains(new Version("1.5")));
					assertTrue("Does not contain 1.6.", v2.contains(new Version("1.6")));
					break;
				case 3 :
					assertEquals("Wrong ee name: " + i, "AA/BB", attrs.get("osgi.ee"));
					List v3 = (List) attrs.get("version");
					assertNull("versions is not null", v3);
					break;
				case 4 :
					assertEquals("Wrong ee name: " + i, "CC-XX/DD-YY", attrs.get("osgi.ee"));
					List v4 = (List) attrs.get("version");
					assertNull("versions is not null", v4);
					break;
				case 5 :
					assertEquals("Wrong ee name: " + i, "EE/FF-YY", attrs.get("osgi.ee"));
					List v5 = (List) attrs.get("version");
					assertEquals("Wrong number of versions: " + i, 2, v5.size());
					assertTrue("Does not contain 1.0.", v5.contains(new Version("1.0")));
					assertTrue("Does not contain 2.0.", v5.contains(new Version("2.0")));
					break;
				case 6 :
					assertEquals("Wrong ee name: " + i, "GG-XX/HH", attrs.get("osgi.ee"));
					List v6 = (List) attrs.get("version");
					assertEquals("Wrong number of versions: " + i, 2, v6.size());
					assertTrue("Does not contain 1.0.", v6.contains(new Version("1.0")));
					assertTrue("Does not contain 2.0.", v6.contains(new Version("2.0")));
					break;
				case 7 :
					assertEquals("Wrong ee name: " + i, "II-1.0/JJ-2.0", attrs.get("osgi.ee"));
					List v7 = (List) attrs.get("version");
					assertNull("versions is not null", v7);
					break;
				default :
					break;
			}
		}

		assertEquals("Wrong number of bundles.", eeCapabilities.size(), bundles.size());
		for (int i = 0; i < bundles.size(); i++) {
			BundleDescription bundle = (BundleDescription) bundles.get(i);
			assertTrue("The bundle is not resolved: " + bundle, bundle.isResolved());
			BundleWiring wiring = bundle.getWiring();
			List eeWires = wiring.getRequiredWires("osgi.ee");
			assertNotNull("ee wires is null.", eeWires);
			assertEquals("Wrong number of ee wires", 1, eeWires.size());
			BundleWire wire = (BundleWire) eeWires.get(0);
			assertEquals("Wired to wrong ee capability.", eeCapabilities.get(i), wire.getCapability());
		}
	}

	public void testEECapabilityRequirement1() throws BundleException {
		State state = buildEmptyState();
		int bundleID = 0;
		Hashtable manifest = new Hashtable();

		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "org.eclipse.osgi"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0"); //$NON-NLS-1$
		BundleDescription systemBundle = state.getFactory().createBundleDescription(state, manifest, "org.eclipse.osgi", bundleID++); //$NON-NLS-1$

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, "CDC-1.0/Foundation-1.0"); //$NON-NLS-1$
		BundleDescription a = state.getFactory().createBundleDescription(state, manifest, "A", bundleID++); //$NON-NLS-1$

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, "OSGi/Minimum-1.1"); //$NON-NLS-1$
		BundleDescription b = state.getFactory().createBundleDescription(state, manifest, "B", bundleID++); //$NON-NLS-1$

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "C"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, "J2SE-1.4"); //$NON-NLS-1$
		BundleDescription c = state.getFactory().createBundleDescription(state, manifest, "C", bundleID++); //$NON-NLS-1$

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "D"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, "J2SE-1.4, OSGi/Minimum-1.1"); //$NON-NLS-1$
		BundleDescription d = state.getFactory().createBundleDescription(state, manifest, "D", bundleID++); //$NON-NLS-1$

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "D"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, "AA-BB"); //$NON-NLS-1$
		BundleDescription e = state.getFactory().createBundleDescription(state, manifest, "D", bundleID++); //$NON-NLS-1$

		Dictionary[] props = new Dictionary[] {new Hashtable(), new Hashtable(), new Hashtable()};
		props[0].put("org.osgi.framework.executionenvironment", "OSGi/Minimum-1.0, OSGi/Minimum-1.1, OSGi/Minimum-1.2, CDC-1.0/Foundation-1.0, CDC-1.1/Foundation-1.1, AA-BB"); //$NON-NLS-1$ //$NON-NLS-2$
		props[1].put("org.osgi.framework.executionenvironment", "OSGi/Minimum-1.0, OSGi/Minimum-1.1, OSGi/Minimum-1.2, JRE-1.1, J2SE-1.2, J2SE-1.3, J2SE-1.4"); //$NON-NLS-1$ //$NON-NLS-2$
		props[2].put("org.osgi.framework.executionenvironment", "OSGi/Minimum-1.0, OSGi/Minimum-1.1, JRE-1.1, J2SE-1.2, J2SE-1.3"); //$NON-NLS-1$ //$NON-NLS-2$

		state.setPlatformProperties(props);
		state.addBundle(systemBundle);
		state.addBundle(a);
		state.addBundle(b);
		state.addBundle(c);
		state.addBundle(d);
		state.addBundle(e);

		state.resolve();

		assertTrue("1.0", systemBundle.isResolved()); //$NON-NLS-1$
		assertTrue("1.1", a.isResolved()); //$NON-NLS-1$
		assertTrue("1.2", b.isResolved()); //$NON-NLS-1$
		assertTrue("1.3", c.isResolved()); //$NON-NLS-1$
		assertTrue("1.4", d.isResolved()); //$NON-NLS-1$
		assertTrue("1.5", e.isResolved()); //$NON-NLS-1$

		BundleWiring wiringA = a.getWiring();
		BundleWiring wiringB = b.getWiring();
		BundleWiring wiringC = c.getWiring();
		BundleWiring wiringD = d.getWiring();
		BundleWiring wiringE = e.getWiring();

		List wiresA = wiringA.getRequiredWires("osgi.ee");
		assertEquals("2.0", 1, wiresA.size());
		List wiresB = wiringB.getRequiredWires("osgi.ee");
		assertEquals("2.1", 1, wiresB.size());
		List wiresC = wiringC.getRequiredWires("osgi.ee");
		assertEquals("2.2", 1, wiresC.size());
		List wiresD = wiringD.getRequiredWires("osgi.ee");
		assertEquals("2.3", 1, wiresD.size());
		List wiresE = wiringE.getRequiredWires("osgi.ee");
		assertEquals("2.4", 1, wiresE.size());

		props[0].put("org.osgi.framework.system.capabilities", "osgi.ee; osgi.ee=\"OSGi/Minimum\"; version:List<Version>=\"1.0, 1.1, 1.2\", osgi.ee; osgi.ee=\"CDC/Foundation\"; version:List<Version>=\"1.0, 1.1\", osgi.ee; osgi.ee=\"AA-BB\""); //$NON-NLS-1$ //$NON-NLS-2$
		props[1].put("org.osgi.framework.system.capabilities", "osgi.ee; osgi.ee=\"OSGi/Minimum\"; version:List<Version>=\"1.0, 1.1, 1.2\", osgi.ee; osgi.ee=\"JavaSE\"; version:List<Version>=\"1.0, 1.1, 1.2, 1.3, 1.4\""); //$NON-NLS-1$ //$NON-NLS-2$
		props[2].put("org.osgi.framework.system.capabilities", "osgi.ee; osgi.ee=\"OSGi/Minimum\"; version:List<Version>=\"1.0, 1.1\", osgi.ee; osgi.ee=\"JavaSE\"; version:List<Version>=\"1.0, 1.1, 1.2, 1.3\""); //$NON-NLS-1$ //$NON-NLS-2$

		state.setPlatformProperties(props);
		state.resolve(false);

		assertTrue("1.0", systemBundle.isResolved()); //$NON-NLS-1$
		assertTrue("1.1", a.isResolved()); //$NON-NLS-1$
		assertTrue("1.2", b.isResolved()); //$NON-NLS-1$
		assertTrue("1.3", c.isResolved()); //$NON-NLS-1$
		assertTrue("1.4", d.isResolved()); //$NON-NLS-1$
		assertTrue("1.5", e.isResolved()); //$NON-NLS-1$

		wiringA = a.getWiring();
		wiringB = b.getWiring();
		wiringC = c.getWiring();
		wiringD = d.getWiring();
		wiringE = e.getWiring();

		wiresA = wiringA.getRequiredWires("osgi.ee");
		assertEquals("2.0", 1, wiresA.size());
		wiresB = wiringB.getRequiredWires("osgi.ee");
		assertEquals("2.1", 1, wiresB.size());
		wiresC = wiringC.getRequiredWires("osgi.ee");
		assertEquals("2.2", 1, wiresC.size());
		wiresD = wiringD.getRequiredWires("osgi.ee");
		assertEquals("2.3", 1, wiresD.size());
		wiresE = wiringE.getRequiredWires("osgi.ee");
		assertEquals("2.4", 1, wiresE.size());
	}

	public void testEEBug377510() throws BundleException {
		State state = buildEmptyState();
		int bundleID = 0;
		Hashtable manifest = new Hashtable();

		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "org.eclipse.osgi"); //$NON-NLS-1$
		BundleDescription systemBundle = state.getFactory().createBundleDescription(state, manifest, "org.eclipse.osgi", bundleID++); //$NON-NLS-1$
		state.addBundle(systemBundle);

		List bundles = new ArrayList();
		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "test");
		manifest.put(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, "JavaSE-1.6"); //$NON-NLS-1$
		BundleDescription a = state.getFactory().createBundleDescription(state, manifest, "A", bundleID++); //$NON-NLS-1$
		bundles.add(a);
		state.addBundle(a);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A.FRAG"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "A");
		manifest.put(Constants.IMPORT_PACKAGE, "test");
		manifest.put(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, "J2SE-1.5"); //$NON-NLS-1$
		BundleDescription aFrag = state.getFactory().createBundleDescription(state, manifest, "A.FRAG", bundleID++); //$NON-NLS-1$
		bundles.add(aFrag);
		state.addBundle(aFrag);

		Dictionary[] props = new Dictionary[] {new Hashtable(), new Hashtable(), new Hashtable(), new Hashtable(), new Hashtable(), new Hashtable(), new Hashtable(), new Hashtable()};
		props[0].put("org.osgi.framework.executionenvironment", "J2SE-1.5");
		props[0].put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, "test");
		props[1].put("org.osgi.framework.executionenvironment", "J2SE-1.5, JavaSE-1.6");
		props[1].put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, "test");
		state.setPlatformProperties(props);

		state.resolve();

		assertTrue("host not resolved", a.isResolved());
		assertTrue("fragment not resolved", aFrag.isResolved());
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

	public void testNativeCodeResolution05() throws BundleException {
		State state = buildEmptyState();
		Dictionary[] props = new Dictionary[] {new Hashtable(), new Hashtable(), new Hashtable()};

		props[0].put("osgi.ws", "win32"); //$NON-NLS-1$ //$NON-NLS-2$
		props[0].put("osgi.os", "win32"); //$NON-NLS-1$ //$NON-NLS-2$
		props[0].put("osgi.arch", "x86"); //$NON-NLS-1$ //$NON-NLS-2$
		props[0].put("osgi.nl", "en_US"); //$NON-NLS-1$ //$NON-NLS-2$

		props[1].put("osgi.ws", "win32"); //$NON-NLS-1$ //$NON-NLS-2$
		props[1].put("osgi.os", "win32"); //$NON-NLS-1$ //$NON-NLS-2$
		props[1].put("osgi.arch", "x86_64"); //$NON-NLS-1$ //$NON-NLS-2$
		props[1].put("osgi.nl", "en_US"); //$NON-NLS-1$ //$NON-NLS-2$

		props[2].put("osgi.ws", "win32"); //$NON-NLS-1$ //$NON-NLS-2$
		props[2].put("osgi.os", "win32"); //$NON-NLS-1$ //$NON-NLS-2$
		props[2].put("osgi.arch", "x86"); //$NON-NLS-1$ //$NON-NLS-2$
		props[2].put("osgi.nl", "fr_FR"); //$NON-NLS-1$ //$NON-NLS-2$

		state.setPlatformProperties(props);

		Hashtable manifest = new Hashtable();
		long bundleID = 0;

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "NativeBundle1"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_NATIVECODE, "Bundle-NativeCode: nativefile1.txt;processor=x86;osname=win32; language=en"); //$NON-NLS-1$
		BundleDescription testNativeBundle1 = state.getFactory().createBundleDescription(state, manifest, "NativeBundle", bundleID++); //$NON-NLS-1$

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "NativeBundle2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_NATIVECODE, "Bundle-NativeCode: nativefile1.txt;processor=x86_64;osname=win32; language=en"); //$NON-NLS-1$
		BundleDescription testNativeBundle2 = state.getFactory().createBundleDescription(state, manifest, "NativeBundle", bundleID++); //$NON-NLS-1$

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "NativeBundle3"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_NATIVECODE, "Bundle-NativeCode: nativefile1.txt;processor=x86;osname=win32; language=fr"); //$NON-NLS-1$
		BundleDescription testNativeBundle3 = state.getFactory().createBundleDescription(state, manifest, "NativeBundle", bundleID++); //$NON-NLS-1$

		state.addBundle(testNativeBundle1);
		state.addBundle(testNativeBundle2);
		state.addBundle(testNativeBundle3);
		state.resolve();
		assertTrue("1.0", testNativeBundle1.isResolved()); //$NON-NLS-1$
		assertTrue("2.0", testNativeBundle2.isResolved()); //$NON-NLS-1$
		assertTrue("3.0", testNativeBundle3.isResolved()); //$NON-NLS-1$
	}

	public void testNativeCodeResolution06() throws BundleException {
		State state = buildEmptyState();
		Dictionary[] props = new Dictionary[] {new Hashtable(), new Hashtable(), new Hashtable()};

		// empty props[0]

		props[1].put("osgi.ws", "win32"); //$NON-NLS-1$ //$NON-NLS-2$
		props[1].put("osgi.os", "win32"); //$NON-NLS-1$ //$NON-NLS-2$
		props[1].put("osgi.arch", "x86_64"); //$NON-NLS-1$ //$NON-NLS-2$

		props[2].put("osgi.ws", "win32"); //$NON-NLS-1$ //$NON-NLS-2$
		props[2].put("osgi.os", "win32"); //$NON-NLS-1$ //$NON-NLS-2$
		props[2].put("osgi.arch", "x86"); //$NON-NLS-1$ //$NON-NLS-2$

		state.setPlatformProperties(props);

		Hashtable manifest = new Hashtable();
		long bundleID = 0;

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "NativeBundle1"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_NATIVECODE, "Bundle-NativeCode: nativefile1.txt;processor=x86;osname=win32"); //$NON-NLS-1$
		BundleDescription testNativeBundle1 = state.getFactory().createBundleDescription(state, manifest, "NativeBundle", bundleID++); //$NON-NLS-1$

		state.addBundle(testNativeBundle1);
		state.resolve();
		assertTrue("1.0", testNativeBundle1.isResolved()); //$NON-NLS-1$
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

	public void testRequirements() throws BundleException, InvalidSyntaxException, IOException {
		State state = buildEmptyState();
		long bundleID = 0;
		Dictionary manifest;

		manifest = loadManifest("r1.MF");
		BundleDescription hostDescription = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);
		state.addBundle(hostDescription);
		manifest = loadManifest("r2.MF");
		BundleDescription fragDescription = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);
		state.addBundle(fragDescription);

		doTestRequirements(hostDescription, fragDescription);

		File stateCache = OSGiTestsActivator.getContext().getDataFile("statecache"); //$NON-NLS-1$
		stateCache.mkdirs();
		StateObjectFactory.defaultFactory.writeState(state, stateCache);
		state = StateObjectFactory.defaultFactory.readState(stateCache);

		hostDescription = state.getBundle(0);
		fragDescription = state.getBundle(1);

		doTestRequirements(hostDescription, fragDescription);
	}

	private void doTestRequirements(BundleRevision hostRevision, BundleRevision fragRevision) throws InvalidSyntaxException {
		List pReqs = hostRevision.getDeclaredRequirements(BundleRevision.PACKAGE_NAMESPACE);
		assertEquals("Wrong number of reqs: " + BundleRevision.PACKAGE_NAMESPACE, 8, pReqs.size());

		Map matchingAttrs = new HashMap();
		matchingAttrs.put(BundleRevision.PACKAGE_NAMESPACE, "importer.pkg1");
		matchingAttrs.put(Constants.VERSION_ATTRIBUTE, new Version("1.1"));

		checkRequirement((BundleRequirement) pReqs.get(0), matchingAttrs, null, null, true);

		matchingAttrs.put(BundleRevision.PACKAGE_NAMESPACE, "importer.pkg2");
		checkRequirement((BundleRequirement) pReqs.get(1), matchingAttrs, null, "(resolution=optional)", true);

		matchingAttrs.put(BundleRevision.PACKAGE_NAMESPACE, "importer.pkg3");
		checkRequirement((BundleRequirement) pReqs.get(2), matchingAttrs, null, "(resolution=mandatory)", true);

		matchingAttrs.put(BundleRevision.PACKAGE_NAMESPACE, "importer.pkg4");
		checkRequirement((BundleRequirement) pReqs.get(3), matchingAttrs, null, null, true);

		matchingAttrs.put(BundleRevision.PACKAGE_NAMESPACE, "importer.pkg5");
		checkRequirement((BundleRequirement) pReqs.get(4), matchingAttrs, null, null, true);

		matchingAttrs.put(BundleRevision.PACKAGE_NAMESPACE, "importer.pkg6");
		checkRequirement((BundleRequirement) pReqs.get(5), matchingAttrs, null, null, true);

		matchingAttrs.put(BundleRevision.PACKAGE_NAMESPACE, "importer.pkg7");
		checkRequirement((BundleRequirement) pReqs.get(6), matchingAttrs, null, null, true);

		matchingAttrs.put(BundleRevision.PACKAGE_NAMESPACE, "importer.pkg8");
		matchingAttrs.put("a1", "v1");
		matchingAttrs.put("a2", "v2");
		checkRequirement((BundleRequirement) pReqs.get(7), matchingAttrs, null, "(&(resolution=optional)(d1=v1)(d2=v2))", true);

		matchingAttrs.clear();
		List bReqs = hostRevision.getDeclaredRequirements(BundleRevision.BUNDLE_NAMESPACE);
		assertEquals("Wrong number of reqs: " + BundleRevision.BUNDLE_NAMESPACE, 8, bReqs.size());

		matchingAttrs.put(BundleRevision.BUNDLE_NAMESPACE, "requirer.b1");
		matchingAttrs.put(Constants.BUNDLE_VERSION_ATTRIBUTE, new Version("1.1"));
		checkRequirement((BundleRequirement) bReqs.get(0), matchingAttrs, null, "(&(visibility=reexport)(resolution=optional))", true);

		matchingAttrs.put(BundleRevision.BUNDLE_NAMESPACE, "requirer.b2");
		checkRequirement((BundleRequirement) bReqs.get(1), matchingAttrs, null, "(&(|(visibility=private)(!(visibility=*)))(|(resolution=mandatory)(!(resolution=*))))", true);

		matchingAttrs.put(BundleRevision.BUNDLE_NAMESPACE, "requirer.b3");
		checkRequirement((BundleRequirement) bReqs.get(2), matchingAttrs, null, null, true);

		matchingAttrs.put(BundleRevision.BUNDLE_NAMESPACE, "requirer.b4");
		checkRequirement((BundleRequirement) bReqs.get(3), matchingAttrs, null, null, true);

		matchingAttrs.put(BundleRevision.BUNDLE_NAMESPACE, "requirer.b5");
		checkRequirement((BundleRequirement) bReqs.get(4), matchingAttrs, null, null, true);

		matchingAttrs.put(BundleRevision.BUNDLE_NAMESPACE, "requirer.b6");
		checkRequirement((BundleRequirement) bReqs.get(5), matchingAttrs, null, null, true);

		matchingAttrs.put(BundleRevision.BUNDLE_NAMESPACE, "requirer.b7");
		checkRequirement((BundleRequirement) bReqs.get(6), matchingAttrs, null, null, true);

		matchingAttrs.put(BundleRevision.BUNDLE_NAMESPACE, "requirer.b8");
		matchingAttrs.put("a1", "v1");
		matchingAttrs.put("a2", "v2");
		checkRequirement((BundleRequirement) bReqs.get(7), matchingAttrs, null, "(&(resolution=optional)(d1=v1)(d2=v2))", true);

		matchingAttrs.clear();
		List cReqs = hostRevision.getDeclaredRequirements("require.c1");
		assertEquals("Wrong number of reqs: require.c1", 1, cReqs.size());

		matchingAttrs.put("a1", "v1");
		checkRequirement((BundleRequirement) cReqs.get(0), matchingAttrs, null, "(&(|(effective=resolve)(!(effective=*)))(resolution=optional)(filter=\\(a1=v1\\)))", true);

		cReqs = hostRevision.getDeclaredRequirements("require.c2");
		assertEquals("Wrong number of reqs: require.c2", 1, cReqs.size());
		matchingAttrs.clear();
		checkRequirement((BundleRequirement) cReqs.get(0), matchingAttrs, null, "(&(|(effective=resolve)(!(effective=*)))(|(resolution=mandatory)(!(resolution=*))))", false);

		cReqs = hostRevision.getDeclaredRequirements("require.c3");
		assertEquals("Wrong number of reqs: require.c3", 1, cReqs.size());
		matchingAttrs.clear();
		checkRequirement((BundleRequirement) cReqs.get(0), matchingAttrs, "(&(a1=v1)(a2=v2))", "(&(resolution=optional)(d1=v1)(d2=v2))", false);

		List fReqs = fragRevision.getDeclaredRequirements(BundleRevision.HOST_NAMESPACE);
		assertEquals("Wrong number of host reqs: " + BundleRevision.HOST_NAMESPACE, 1, fReqs.size());
		matchingAttrs.clear();
		matchingAttrs.put(BundleRevision.HOST_NAMESPACE, "r1");
		matchingAttrs.put(Constants.BUNDLE_VERSION_ATTRIBUTE, new Version(1, 1, 1));
		matchingAttrs.put("a1", "v1");
		matchingAttrs.put("a2", "v2");
		checkRequirement((BundleRequirement) fReqs.get(0), matchingAttrs, null, "(&(d1=v1)(d2=v2))", true);
	}

	private void checkRequirement(BundleRequirement req, Map matchingAttrs, String attributesFilterSpec, String directivesFilterSpec, boolean reqFilter) throws InvalidSyntaxException {
		if (attributesFilterSpec == null) {
			assertTrue("Requirement attrs is not empty: " + req, req.getAttributes().isEmpty());
		} else {
			Filter filter = FrameworkUtil.createFilter(attributesFilterSpec);
			assertTrue("Cannot match attribute filter: " + filter, filter.matches(req.getAttributes()));
		}
		Map directives = req.getDirectives();
		String reqFilterSpec = (String) directives.get(Constants.FILTER_DIRECTIVE);
		if (reqFilterSpec != null) {
			Filter filter = FrameworkUtil.createFilter(reqFilterSpec);
			assertTrue("Cannot match requirement filter: " + filter, filter.matches(matchingAttrs));
		} else {
			if (reqFilter)
				fail("Requirement must have filter directive: " + req);
		}
		if (directivesFilterSpec != null) {
			Filter directivesFilter = FrameworkUtil.createFilter(directivesFilterSpec);
			assertTrue("Cannot match directive filter: " + directivesFilterSpec, directivesFilter.matches(req.getDirectives()));
		}
	}

	public void testCapabilities() throws InvalidSyntaxException, BundleException, IOException {
		State state = buildEmptyState();
		long bundleID = 0;
		Dictionary manifest;

		manifest = loadManifest("r1.MF");
		BundleDescription hostDescription = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);
		state.addBundle(hostDescription);

		doTestCapabilities(hostDescription);

		File stateCache = OSGiTestsActivator.getContext().getDataFile("statecache"); //$NON-NLS-1$
		stateCache.mkdirs();
		StateObjectFactory.defaultFactory.writeState(state, stateCache);
		state = StateObjectFactory.defaultFactory.readState(stateCache);

		hostDescription = state.getBundle(0);
	}

	private void doTestCapabilities(BundleRevision hostRevision) throws InvalidSyntaxException {
		List bundleCaps = hostRevision.getDeclaredCapabilities(BundleRevision.BUNDLE_NAMESPACE);
		assertEquals("Wrong number of: " + BundleRevision.BUNDLE_NAMESPACE, 1, bundleCaps.size());
		checkCapability((BundleCapability) bundleCaps.get(0), "(&(osgi.wiring.bundle=r1)(a1=v1)(a2=v2))", "(&(d1=v1)(d2=v2)(singleton=true)(fragment-attachment=resolve-time))");

		List hostCaps = hostRevision.getDeclaredCapabilities(BundleRevision.HOST_NAMESPACE);
		assertEquals("Wrong number of: " + BundleRevision.HOST_NAMESPACE, 1, hostCaps.size());
		checkCapability((BundleCapability) hostCaps.get(0), "(&(osgi.wiring.host=r1)(a1=v1)(a2=v2))", "(&(d1=v1)(d2=v2)(singleton=true)(fragment-attachment=resolve-time))");

		List pkgCaps = hostRevision.getDeclaredCapabilities(BundleRevision.PACKAGE_NAMESPACE);
		assertEquals("Wrong number of: " + BundleRevision.PACKAGE_NAMESPACE, 3, pkgCaps.size());
		checkCapability((BundleCapability) pkgCaps.get(0), "(&(osgi.wiring.package=exporter.pkg1)(bundle-symbolic-name=r1)(bundle-version=1.0)(version=1.0))", null);
		checkCapability((BundleCapability) pkgCaps.get(1), "(&(osgi.wiring.package=exporter.pkg2)(bundle-symbolic-name=r1)(bundle-version=1.0)(a1=v1)(a2=v2))", "(&(include=C1,C2)(exclude=C3,C4)(mandatory=a1,a2)(uses=importer.pkg1,importer.pkg2))");
		checkCapability((BundleCapability) pkgCaps.get(2), "(&(osgi.wiring.package=exporter.pkg3)(bundle-symbolic-name=r1)(bundle-version=1.0)(a1=v1)(a2=v2))", "(&(mandatory=a1,a2)(d1=v1)(d2=v2))");

		List gCaps = hostRevision.getDeclaredCapabilities("provide.c1");
		assertEquals("Wrong number of: provide.c1", 1, gCaps.size());
		checkCapability((BundleCapability) gCaps.get(0), "(&(a1=v1)(a2=v2))", "(&(|(effective=resolve)(!(effective=*)))(uses=importer.pkg1,importer.pkg2))");

		gCaps = hostRevision.getDeclaredCapabilities("provide.c2");
		assertEquals("Wrong number of: provide.c1", 1, gCaps.size());
		checkCapability((BundleCapability) gCaps.get(0), "(&(a1=v1)(a2=v2))", "(&(|(effective=resolve)(!(effective=*)))(uses=importer.pkg1,importer.pkg2)(d1=v1)(d2=v2))");

	}

	private void checkCapability(BundleCapability cap, String attributeFilterSpec, String directiveFilterSpec) throws InvalidSyntaxException {
		Filter attributesFilter = FrameworkUtil.createFilter(attributeFilterSpec);
		assertTrue("Cannot match attribute filter: " + attributesFilter, attributesFilter.matches(cap.getAttributes()));

		if (directiveFilterSpec != null) {
			Filter directivesFilter = FrameworkUtil.createFilter(directiveFilterSpec);
			assertTrue("Cannot match directive filter: " + directivesFilter, directivesFilter.matches(cap.getDirectives()));
		}
	}

	public void testRanges() throws BundleException {
		State state = buildEmptyState();
		int bundleID = 0;

		Hashtable manifest = new Hashtable();
		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "a"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "a; version=1.0.0");
		manifest.put(Constants.PROVIDE_CAPABILITY, "test.a; version:Version=1.0.0");
		BundleDescription a = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + manifest.get(Constants.BUNDLE_VERSION), bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "b"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "a; bundle-version=\"[1.0, 1.1)\""); //$NON-NLS-1$
		BundleDescription b = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + manifest.get(Constants.BUNDLE_VERSION), bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "c"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "a; version=\"[1.0,2.0)\""); //$NON-NLS-1$
		BundleDescription c = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + manifest.get(Constants.BUNDLE_VERSION), bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "d"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "a; bundle-version=\"[1.0,2.0)\""); //$NON-NLS-1$
		BundleDescription d = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + manifest.get(Constants.BUNDLE_VERSION), bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "e"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_CAPABILITY, "test.a; filter:=\"(version>=1.0.0)\""); //$NON-NLS-1$
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
		assertTrue("E is not resolved", e.isResolved()); //$NON-NLS-1$
	}

	public void testBug369880() throws BundleException {
		State state = buildEmptyState();
		Dictionary[] props = new Dictionary[] {new Hashtable()};
		props[0].put("org.osgi.framework.executionenvironment", "test"); //$NON-NLS-1$ //$NON-NLS-2$
		state.setPlatformProperties(props);
		int bundleID = 0;

		Hashtable manifest = new Hashtable();
		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "org.eclipse.osgi"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, "test");
		BundleDescription a = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + manifest.get(Constants.BUNDLE_VERSION), bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "b"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "org.eclipse.osgi; bundle-version=\"[1.0, 1.1)\""); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, "test");
		BundleDescription b = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + manifest.get(Constants.BUNDLE_VERSION), bundleID++);

		state.addBundle(a);
		state.resolve();
		assertTrue("A is not resolved", a.isResolved()); //$NON-NLS-1$

		state.addBundle(b);
		state.resolve();
		assertTrue("B is not resolved", b.isResolved()); //$NON-NLS-1$

		BundleWiring aWiring = a.getWiring();
		List aRequirements = a.getRequirements("osgi.ee");
		List aRequiredWires = aWiring.getRequiredWires("osgi.ee");
		assertEquals("Wrong number of osgi.ee requirements from system bundle", 1, aRequirements.size());
		assertEquals("Wrong number of wires from system bundle", 1, aRequiredWires.size());

		BundleWiring bWiring = b.getWiring();
		List bRequirements = b.getRequirements("osgi.ee");
		List bRequiredWires = bWiring.getRequiredWires("osgi.ee");
		assertEquals("Wrong number of osgi.ee requirements from fragment", 1, bRequirements.size());
		assertEquals("Wrong number of wires from fragment", 1, bRequiredWires.size());
	}

	public void testResolveFragmentEE01() throws BundleException {
		State state = buildEmptyState();
		Dictionary[] props = new Dictionary[] {new Hashtable()};
		props[0].put("org.osgi.framework.executionenvironment", "test"); //$NON-NLS-1$ //$NON-NLS-2$
		state.setPlatformProperties(props);
		int bundleID = 0;

		Hashtable manifest = new Hashtable();
		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "org.eclipse.osgi"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, "test");
		BundleDescription a = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + manifest.get(Constants.BUNDLE_VERSION), bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "b"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "org.eclipse.osgi; bundle-version=\"[1.0, 1.1)\""); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_CAPABILITY, "osgi.ee; filter:=\"(osgi.ee=test)\"");
		BundleDescription b = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + manifest.get(Constants.BUNDLE_VERSION), bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "c"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_CAPABILITY, "osgi.identity; filter:=\"(osgi.identity=b)\""); //$NON-NLS-1$
		BundleDescription c = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + manifest.get(Constants.BUNDLE_VERSION), bundleID++);

		state.addBundle(b);
		state.addBundle(a);
		state.addBundle(c);
		state.resolve();
		assertTrue("A is not resolved", a.isResolved()); //$NON-NLS-1$
		assertTrue("B is not resolved", b.isResolved()); //$NON-NLS-1$
		assertTrue("C is not resolved", c.isResolved()); //$NON-NLS-1$

		BundleWiring aWiring = a.getWiring();
		List aRequirements = a.getRequirements("osgi.ee");
		List aRequiredWires = aWiring.getRequiredWires("osgi.ee");
		assertEquals("Wrong number of osgi.ee requirements from system bundle", 1, aRequirements.size());
		assertEquals("Wrong number of wires from system bundle", 1, aRequiredWires.size());

		BundleWiring bWiring = b.getWiring();
		List bRequirements = b.getRequirements("osgi.ee");
		List bRequiredWires = bWiring.getRequiredWires("osgi.ee");
		assertEquals("Wrong number of osgi.ee requirements from fragment", 1, bRequirements.size());
		assertEquals("Wrong number of wires from fragment", 1, bRequiredWires.size());

		BundleWiring cWiring = c.getWiring();
		List cRequirements = c.getRequirements("osgi.identity");
		List cRequiredWires = cWiring.getRequiredWires("osgi.identity");
		assertEquals("Wrong number of osgi.identity requirements from c", 1, cRequirements.size());
		assertEquals("Wrong number of wires from c", 1, cRequiredWires.size());

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "b"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "org.eclipse.osgi; bundle-version=\"[1.0, 1.1)\""); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_CAPABILITY, "osgi.ee; filter:=\"(osgi.ee=fail)\"");
		b = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + manifest.get(Constants.BUNDLE_VERSION), b.getBundleId());
		state.updateBundle(b);
		state.resolve(false);
		assertTrue("A is not resolved", a.isResolved()); //$NON-NLS-1$
		assertFalse("B is not resolved", b.isResolved()); //$NON-NLS-1$
		assertFalse("C is not resolved", c.isResolved()); //$NON-NLS-1$
	}

	public void testResolveFragmentEE02() throws BundleException, IOException {
		State state = buildEmptyState();
		Dictionary[] props = new Dictionary[] {new Hashtable()};
		props[0].put("org.osgi.framework.executionenvironment", "test"); //$NON-NLS-1$ //$NON-NLS-2$
		state.setPlatformProperties(props);
		int bundleID = 0;

		Hashtable manifest = new Hashtable();
		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "org.eclipse.osgi"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, "test");
		BundleDescription a = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + manifest.get(Constants.BUNDLE_VERSION), bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "b"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, "test");
		BundleDescription b = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + manifest.get(Constants.BUNDLE_VERSION), bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "c"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "d");
		manifest.put(Constants.FRAGMENT_HOST, "b");
		manifest.put(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, "test");
		BundleDescription c = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + manifest.get(Constants.BUNDLE_VERSION), bundleID++);

		state.addBundle(b);
		state.addBundle(a);
		state.addBundle(c);
		state.resolve();
		assertTrue("A is not resolved", a.isResolved()); //$NON-NLS-1$
		assertTrue("B is not resolved", b.isResolved()); //$NON-NLS-1$
		assertFalse("C is resolved", c.isResolved()); //$NON-NLS-1$

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "d"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "d");
		manifest.put(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, "test");
		BundleDescription d = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + manifest.get(Constants.BUNDLE_VERSION), bundleID++);

		state.addBundle(d);
		state.resolve();
		assertTrue("A is not resolved", a.isResolved()); //$NON-NLS-1$
		assertTrue("B is not resolved", b.isResolved()); //$NON-NLS-1$
		assertFalse("C is resolved", c.isResolved()); //$NON-NLS-1$
		assertTrue("D is not resolved", d.isResolved()); //$NON-NLS-1$

		state.resolve(new BundleDescription[] {b});
		assertTrue("A is not resolved", a.isResolved()); //$NON-NLS-1$
		assertTrue("B is not resolved", b.isResolved()); //$NON-NLS-1$
		assertTrue("C is not resolved", c.isResolved()); //$NON-NLS-1$
		assertTrue("D is not resolved", d.isResolved()); //$NON-NLS-1$

		BundleWiring aWiring = a.getWiring();
		List aRequirements = a.getRequirements("osgi.ee");
		List aRequiredWires = aWiring.getRequiredWires("osgi.ee");
		assertEquals("Wrong number of osgi.ee requirements from system bundle", 1, aRequirements.size());
		assertEquals("Wrong number of wires from system bundle", 1, aRequiredWires.size());

		BundleWiring bWiring = b.getWiring();
		List bRequirements = b.getRequirements("osgi.ee");
		List bRequiredWires = bWiring.getRequiredWires("osgi.ee");
		assertEquals("Wrong number of osgi.ee requirements from fragment", 1, bRequirements.size());
		assertEquals("Wrong number of wires from fragment", 1, bRequiredWires.size());

		BundleWiring cWiring = c.getWiring();
		List cRequirements = c.getRequirements("osgi.ee");
		List cRequiredWires = cWiring.getRequiredWires("osgi.ee");
		assertEquals("Wrong number of osgi.ee requirements from c", 1, cRequirements.size());
		assertEquals("Wrong number of wires from c", 1, cRequiredWires.size());

		File stateCache = OSGiTestsActivator.getContext().getDataFile("statecache"); //$NON-NLS-1$
		stateCache.mkdirs();
		StateObjectFactory.defaultFactory.writeState(state, stateCache);
		state = StateObjectFactory.defaultFactory.readState(stateCache);

		a = state.getBundle("org.eclipse.osgi", null);
		b = state.getBundle("b", null);
		c = state.getBundle("c", null);
		d = state.getBundle("d", null);

		aWiring = a.getWiring();
		aRequirements = a.getRequirements("osgi.ee");
		aRequiredWires = aWiring.getRequiredWires("osgi.ee");
		assertEquals("Wrong number of osgi.ee requirements from system bundle", 1, aRequirements.size());
		assertEquals("Wrong number of wires from system bundle", 1, aRequiredWires.size());

		bWiring = b.getWiring();
		bRequirements = b.getRequirements("osgi.ee");
		bRequiredWires = bWiring.getRequiredWires("osgi.ee");
		assertEquals("Wrong number of osgi.ee requirements from fragment", 1, bRequirements.size());
		assertEquals("Wrong number of wires from fragment", 1, bRequiredWires.size());

		cWiring = c.getWiring();
		cRequirements = c.getRequirements("osgi.ee");
		cRequiredWires = cWiring.getRequiredWires("osgi.ee");
		assertEquals("Wrong number of osgi.ee requirements from c", 1, cRequirements.size());
		assertEquals("Wrong number of wires from c", 1, cRequiredWires.size());
	}

	public void testBug376322() throws BundleException, IOException {
		State state = buildEmptyState();
		int bundleID = 0;

		Hashtable manifest = new Hashtable();
		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "org.eclipse.osgi"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		BundleDescription a = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + manifest.get(Constants.BUNDLE_VERSION), bundleID++);
		state.addBundle(a);

		String longExport = "aQute.libg.header,aQute.libg.version,com.liferay.alloy.util,com.liferay.counter.model.impl;version=\"6.2.0\",com.liferay.counter.model;version=\"6.2.0\",com.liferay.counter.service.base;version=\"6.2.0\",com.liferay.counter.service.impl;version=\"6.2.0\",com.liferay.counter.service.persistence;version=\"6.2.0\",com.liferay.counter.service;version=\"6.2.0\",com.liferay.counter;version=\"6.2.0\",com.liferay.mail.dependencies.fedora.cyrus;version=\"6.2.0\",com.liferay.mail.dependencies.fedora.ksh;version=\"6.2.0\",com.liferay.mail.dependencies.fedora.sendmail;version=\"6.2.0\",com.liferay.mail.dependencies.fedora;version=\"6.2.0\",com.liferay.mail.dependencies;version=\"6.2.0\",com.liferay.mail.messaging;version=\"6.2.0\",com.liferay.mail.model;version=\"6.2.0\",com.liferay.mail.service.impl;version=\"6.2.0\",com.liferay.mail.service.persistence;version=\"6.2.0\",com.liferay.mail.service;version=\"6.2.0\",com.liferay.mail.util;version=\"6.2.0\",com.liferay.mail;version=\"6.2.0\",com.liferay.portal.action;version=\"6.2.0\",com.liferay.portal.apache.bridges.struts;version=\"6.2.0\",com.liferay.portal.apache.bridges;version=\"6.2.0\",com.liferay.portal.apache;version=\"6.2.0\",com.liferay.portal.asset;version=\"6.2.0\",com.liferay.portal.atom;version=\"6.2.0\",com.liferay.portal.audit;version=\"6.2.0\",com.liferay.portal.bean;version=\"6.2.0\",com.liferay.portal.bi.rules;version=\"6.2.0\",com.liferay.portal.bi;version=\"6.2.0\",com.liferay.portal.cache.cluster.clusterlink.messaging;version=\"6.2.0\",com.liferay.portal.cache.cluster.clusterlink;version=\"6.2.0\",com.liferay.portal.cache.cluster;version=\"6.2.0\",com.liferay.portal.cache.ehcache;version=\"6.2.0\",com.liferay.portal.cache.key;version=\"6.2.0\",com.liferay.portal.cache.keypool;version=\"6.2.0\",com.liferay.portal.cache.memcached;version=\"6.2.0\",com.liferay.portal.cache.memory;version=\"6.2.0\",com.liferay.portal.cache.transactional;version=\"6.2.0\",com.liferay.portal.cache;version=\"6.2.0\",com.liferay.portal.captcha.recaptcha;version=\"6.2.0\",com.liferay.portal.captcha.simplecaptcha;version=\"6.2.0\",com.liferay.portal.captcha;version=\"6.2.0\",com.liferay.portal.ccpp;version=\"6.2.0\",com.liferay.portal.cluster;version=\"6.2.0\",com.liferay.portal.configuration.easyconf;version=\"6.2.0\",com.liferay.portal.configuration;version=\"6.2.0\",com.liferay.portal.convert.action;version=\"6.2.0\",com.liferay.portal.convert.messaging;version=\"6.2.0\",com.liferay.portal.convert;version=\"6.2.0\",com.liferay.portal.dao.db;version=\"6.2.0\",com.liferay.portal.dao.jdbc.aop;version=\"6.2.0\",com.liferay.portal.dao.jdbc.pool.c3p0;version=\"6.2.0\",com.liferay.portal.dao.jdbc.pool;version=\"6.2.0\",com.liferay.portal.dao.jdbc.spring;version=\"6.2.0\",com.liferay.portal.dao.jdbc.util;version=\"6.2.0\",com.liferay.portal.dao.jdbc;version=\"6.2.0\",com.liferay.portal.dao.orm.common;version=\"6.2.0\",com.liferay.portal.dao.orm.hibernate.jmx;version=\"6.2.0\",com.liferay.portal.dao.orm.hibernate.region;version=\"6.2.0\",com.liferay.portal.dao.orm.hibernate;version=\"6.2.0\",com.liferay.portal.dao.orm.jpa;version=\"6.2.0\",com.liferay.portal.dao.orm;version=\"6.2.0\",com.liferay.portal.dao.shard.advice;version=\"6.2.0\",com.liferay.portal.dao.shard;version=\"6.2.0\",com.liferay.portal.dao;version=\"6.2.0\",com.liferay.portal.definitions;version=\"6.2.0\",com.liferay.portal.dependencies;version=\"6.2.0\",com.liferay.portal.deploy.auto.exploded.tomcat;version=\"6.2.0\",com.liferay.portal.deploy.auto.exploded;version=\"6.2.0\",com.liferay.portal.deploy.auto;version=\"6.2.0\",com.liferay.portal.deploy.dependencies;version=\"6.2.0\",com.liferay.portal.deploy.hot.messaging;version=\"6.2.0\",com.liferay.portal.deploy.hot;version=\"6.2.0\",com.liferay.portal.deploy.sandbox;version=\"6.2.0\",com.liferay.portal.deploy;version=\"6.2.0\",com.liferay.portal.editor.fckeditor.command.impl;version=\"6.2.0\",com.liferay.portal.editor.fckeditor.command;version=\"6.2.0\",com.liferay.portal.editor.fckeditor.exception;version=\"6.2.0\",com.liferay.portal.editor.fckeditor.receiver.impl;version=\"6.2.0\",com.liferay.portal.editor.fckeditor.receiver;version=\"6.2.0\",com.liferay.portal.editor.fckeditor;version=\"6.2.0\",com.liferay.portal.editor;version=\"6.2.0\",com.liferay.portal.events.dependencies;version=\"6.2.0\",com.liferay.portal.events;version=\"6.2.0\",com.liferay.portal.executor;version=\"6.2.0\",com.liferay.portal.facebook;version=\"6.2.0\",com.liferay.portal.format;version=\"6.2.0\",com.liferay.portal.freemarker;version=\"6.2.0\",com.liferay.portal.googleapps;version=\"6.2.0\",com.liferay.portal.im;version=\"6.2.0\",com.liferay.portal.image;version=\"6.2.0\",com.liferay.portal.increment;version=\"6.2.0\",com.liferay.portal.javadoc;version=\"6.2.0\",com.liferay.portal.jcr.jackrabbit.dependencies;version=\"6.2.0\",com.liferay.portal.jcr.jackrabbit;version=\"6.2.0\",com.liferay.portal.jcr;version=\"6.2.0\",com.liferay.portal.jericho;version=\"6.2.0\",com.liferay.portal.json.jabsorb.serializer;version=\"6.2.0\",com.liferay.portal.json.jabsorb;version=\"6.2.0\",com.liferay.portal.json.transformer;version=\"6.2.0\",com.liferay.portal.json;version=\"6.2.0\",com.liferay.portal.jsonwebservice.action;version=\"6.2.0\",com.liferay.portal.jsonwebservice;version=\"6.2.0\",com.liferay.portal.kernel.annotation;version=\"6.2.0\",com.liferay.portal.kernel.atom;version=\"6.2.0\",com.liferay.portal.kernel.audit;version=\"6.2.0\",com.liferay.portal.kernel.bean;version=\"6.2.0\",com.liferay.portal.kernel.bi.reporting.messaging;version=\"6.2.0\",com.liferay.portal.kernel.bi.reporting.servlet;version=\"6.2.0\",com.liferay.portal.kernel.bi.reporting;version=\"6.2.0\",com.liferay.portal.kernel.bi.rules;version=\"6.2.0\",com.liferay.portal.kernel.bi;version=\"6.2.0\",com.liferay.portal.kernel.cache.cluster;version=\"6.2.0\",com.liferay.portal.kernel.cache.key;version=\"6.2.0\",com.liferay.portal.kernel.cache;version=\"6.2.0\",com.liferay.portal.kernel.cal;version=\"6.2.0\",com.liferay.portal.kernel.captcha;version=\"6.2.0\",com.liferay.portal.kernel.cluster.messaging;version=\"6.2.0\",com.liferay.portal.kernel.cluster;version=\"6.2.0\",com.liferay.portal.kernel.concurrent;version=\"6.2.0\",com.liferay.portal.kernel.configuration;version=\"6.2.0\",com.liferay.portal.kernel.dao.db;version=\"6.2.0\",com.liferay.portal.kernel.dao.jdbc;version=\"6.2.0\",com.liferay.portal.kernel.dao.orm;version=\"6.2.0\",com.liferay.portal.kernel.dao.search;version=\"6.2.0\",com.liferay.portal.kernel.dao.shard;version=\"6.2.0\",com.liferay.portal.kernel.dao;version=\"6.2.0\",com.liferay.portal.kernel.deploy.auto;version=\"6.2.0\",com.liferay.portal.kernel.deploy.hot;version=\"6.2.0\",com.liferay.portal.kernel.deploy.sandbox;version=\"6.2.0\",com.liferay.portal.kernel.deploy;version=\"6.2.0\",com.liferay.portal.kernel.editor;version=\"6.2.0\",com.liferay.portal.kernel.events;version=\"6.2.0\",com.liferay.portal.kernel.exception,com.liferay.portal.kernel.exception;version=\"6.2.0\",com.liferay.portal.kernel.executor;version=\"6.2.0\",com.liferay.portal.kernel.facebook;version=\"6.2.0\",com.liferay.portal.kernel.format;version=\"6.2.0\",com.liferay.portal.kernel.freemarker;version=\"6.2.0\",com.liferay.portal.kernel.googleapps.comparator;version=\"6.2.0\",com.liferay.portal.kernel.googleapps;version=\"6.2.0\",com.liferay.portal.kernel.image;version=\"6.2.0\",com.liferay.portal.kernel.increment;version=\"6.2.0\",com.liferay.portal.kernel.io.delta;version=\"6.2.0\",com.liferay.portal.kernel.io.unsync;version=\"6.2.0\",com.liferay.portal.kernel.io;version=\"6.2.0\",com.liferay.portal.kernel.javadoc;version=\"6.2.0\",com.liferay.portal.kernel.jmx.model;version=\"6.2.0\",com.liferay.portal.kernel.jmx;version=\"6.2.0\",com.liferay.portal.kernel.jndi;version=\"6.2.0\",com.liferay.portal.kernel.json;version=\"6.2.0\",com.liferay.portal.kernel.jsonwebservice;version=\"6.2.0\",com.liferay.portal.kernel.language;version=\"6.2.0\",com.liferay.portal.kernel.lar;version=\"6.2.0\",com.liferay.portal.kernel.ldap;version=\"6.2.0\",com.liferay.portal.kernel.log;version=\"6.2.0\",com.liferay.portal.kernel.mail;version=\"6.2.0\",com.liferay.portal.kernel.management.jmx;version=\"6.2.0\",com.liferay.portal.kernel.management;version=\"6.2.0\",com.liferay.portal.kernel.memory;version=\"6.2.0\",com.liferay.portal.kernel.messaging.async;version=\"6.2.0\",com.liferay.portal.kernel.messaging.config;version=\"6.2.0\",com.liferay.portal.kernel.messaging.jmx;version=\"6.2.0\",com.liferay.portal.kernel.messaging.proxy;version=\"6.2.0\",com.liferay.portal.kernel.messaging.sender;version=\"6.2.0\",com.liferay.portal.kernel.messaging;version=\"6.2.0\",com.liferay.portal.kernel.metadata;version=\"6.2.0\",com.liferay.portal.kernel.mobile.device.rulegroup.action;version=\"6.2.0\",com.liferay.portal.kernel.mobile.device.rulegroup.rule;version=\"6.2.0\",com.liferay.portal.kernel.mobile.device.rulegroup;version=\"6.2.0\",com.liferay.portal.kernel.mobile.device;version=\"6.2.0\",com.liferay.portal.kernel.mobile;version=\"6.2.0\",com.liferay.portal.kernel.monitoring.statistics;version=\"6.2.0\",com.liferay.portal.kernel.monitoring;version=\"6.2.0\",com.liferay.portal.kernel.nio.charset;version=\"6.2.0\",com.liferay.portal.kernel.nio;version=\"6.2.0\",com.liferay.portal.kernel.notifications;version=\"6.2.0\",com.liferay.portal.kernel.oauth;version=\"6.2.0\",com.liferay.portal.kernel.parsers.bbcode;version=\"6.2.0\",com.liferay.portal.kernel.parsers;version=\"6.2.0\",com.liferay.portal.kernel.plugin;version=\"6.2.0\",com.liferay.portal.kernel.poller.comet;version=\"6.2.0\",com.liferay.portal.kernel.poller;version=\"6.2.0\",com.liferay.portal.kernel.pop;version=\"6.2.0\",com.liferay.portal.kernel.portlet;version=\"6.2.0\",com.liferay.portal.kernel.process.log;version=\"6.2.0\",com.liferay.portal.kernel.process;version=\"6.2.0\",com.liferay.portal.kernel.repository.cmis.search;version=\"6.2.0\",com.liferay.portal.kernel.repository.cmis;version=\"6.2.0\",com.liferay.portal.kernel.repository.model;version=\"6.2.0\",com.liferay.portal.kernel.repository.search;version=\"6.2.0\",com.liferay.portal.kernel.repository;version=\"6.2.0\",com.liferay.portal.kernel.resource;version=\"6.2.0\",com.liferay.portal.kernel.sanitizer;version=\"6.2.0\",com.liferay.portal.kernel.scheduler.config;version=\"6.2.0\",com.liferay.portal.kernel.scheduler.messaging;version=\"6.2.0\",com.liferay.portal.kernel.scheduler;version=\"6.2.0\",com.liferay.portal.kernel.scripting;version=\"6.2.0\",com.liferay.portal.kernel.search.facet.collector;version=\"6.2.0\",com.liferay.portal.kernel.search.facet.config;version=\"6.2.0\",com.liferay.portal.kernel.search.facet.util;version=\"6.2.0\",com.liferay.portal.kernel.search.facet;version=\"6.2.0\",com.liferay.portal.kernel.search.messaging;version=\"6.2.0\",com.liferay.portal.kernel.search;version=\"6.2.0\",com.liferay.portal.kernel.security.jaas;version=\"6.2.0\",com.liferay.portal.kernel.security;version=\"6.2.0\",com.liferay.portal.kernel.servlet.filters.compoundsessionid;version=\"6.2.0\",com.liferay.portal.kernel.servlet.filters.invoker;version=\"6.2.0\",com.liferay.portal.kernel.servlet.filters;version=\"6.2.0\",com.liferay.portal.kernel.servlet.taglib.aui;version=\"6.2.0\",com.liferay.portal.kernel.servlet.taglib.ui;version=\"6.2.0\",com.liferay.portal.kernel.servlet.taglib;version=\"6.2.0\",com.liferay.portal.kernel.servlet;version=\"6.2.0\",com.liferay.portal.kernel.spring.aop;version=\"6.2.0\",com.liferay.portal.kernel.spring.context;version=\"6.2.0\",com.liferay.portal.kernel.spring;version=\"6.2.0\",com.liferay.portal.kernel.staging.permission;version=\"6.2.0\",com.liferay.portal.kernel.staging;version=\"6.2.0\",com.liferay.portal.kernel.struts;version=\"6.2.0\",com.liferay.portal.kernel.templateparser;version=\"6.2.0\",com.liferay.portal.kernel.test;version=\"6.2.0\",com.liferay.portal.kernel.transaction;version=\"6.2.0\",com.liferay.portal.kernel.upgrade.util;version=\"6.2.0\",com.liferay.portal.kernel.upgrade;version=\"6.2.0\",com.liferay.portal.kernel.upload;version=\"6.2.0\",com.liferay.portal.kernel.util;version=\"6.2.0\",com.liferay.portal.kernel.uuid;version=\"6.2.0\",com.liferay.portal.kernel.velocity;version=\"6.2.0\",com.liferay.portal.kernel.webcache;version=\"6.2.0\",com.liferay.portal.kernel.webdav;version=\"6.2.0\",com.liferay.portal.kernel.workflow.comparator;version=\"6.2.0\",com.liferay.portal.kernel.workflow.messaging;version=\"6.2.0\",com.liferay.portal.kernel.workflow.permission;version=\"6.2.0\",com.liferay.portal.kernel.workflow;version=\"6.2.0\",com.liferay.portal.kernel.xml.simple;version=\"6.2.0\",com.liferay.portal.kernel.xml;version=\"6.2.0\",com.liferay.portal.kernel.xmlrpc;version=\"6.2.0\",com.liferay.portal.kernel.zip;version=\"6.2.0\",com.liferay.portal.kernel;version=\"6.2.0\",com.liferay.portal.language;version=\"6.2.0\",com.liferay.portal.lar;version=\"6.2.0\",com.liferay.portal.license.util;version=\"6.2.0\",com.liferay.portal.license;version=\"6.2.0\",com.liferay.portal.liveusers.messaging;version=\"6.2.0\",com.liferay.portal.liveusers;version=\"6.2.0\",com.liferay.portal.log;version=\"6.2.0\",com.liferay.portal.management;version=\"6.2.0\",com.liferay.portal.messaging.async;version=\"6.2.0\",com.liferay.portal.messaging.proxy;version=\"6.2.0\",com.liferay.portal.messaging;version=\"6.2.0\",com.liferay.portal.metadata;version=\"6.2.0\",com.liferay.portal.mobile.device.messaging;version=\"6.2.0\",com.liferay.portal.mobile.device.rulegroup.action.impl;version=\"6.2.0\",com.liferay.portal.mobile.device.rulegroup.action;version=\"6.2.0\",com.liferay.portal.mobile.device.rulegroup.rule.impl;version=\"6.2.0\",com.liferay.portal.mobile.device.rulegroup.rule;version=\"6.2.0\",com.liferay.portal.mobile.device.rulegroup;version=\"6.2.0\",com.liferay.portal.mobile.device;version=\"6.2.0\",com.liferay.portal.mobile;version=\"6.2.0\",com.liferay.portal.model.impl;version=\"6.2.0\",com.liferay.portal.model;version=\"6.2.0\",com.liferay.portal.monitoring.jmx;version=\"6.2.0\",com.liferay.portal.monitoring.messaging;version=\"6.2.0\",com.liferay.portal.monitoring.statistics.portal;version=\"6.2.0\",com.liferay.portal.monitoring.statistics.portlet;version=\"6.2.0\",com.liferay.portal.monitoring.statistics.service;version=\"6.2.0\",com.liferay.portal.monitoring.statistics;version=\"6.2.0\",com.liferay.portal.monitoring;version=\"6.2.0\",com.liferay.portal.notifications;version=\"6.2.0\",com.liferay.portal.oauth;version=\"6.2.0\",com.liferay.portal.osgi.service;version=\"6.2.0\",com.liferay.portal.osgi;version=\"6.2.0\",com.liferay.portal.parsers.bbcode;version=\"6.2.0\",com.liferay.portal.parsers.creole.ast.extension;version=\"6.2.0\",com.liferay.portal.parsers.creole.ast.link;version=\"6.2.0\",com.liferay.portal.parsers.creole.ast.table;version=\"6.2.0\",com.liferay.portal.parsers.creole.ast;version=\"6.2.0\",com.liferay.portal.parsers.creole.grammar;version=\"6.2.0\",com.liferay.portal.parsers.creole.parser;version=\"6.2.0\",com.liferay.portal.parsers.creole.visitor.impl;version=\"6.2.0\",com.liferay.portal.parsers.creole.visitor;version=\"6.2.0\",com.liferay.portal.parsers.creole;version=\"6.2.0\",com.liferay.portal.parsers;version=\"6.2.0\",com.liferay.portal.plugin;version=\"6.2.0\",com.liferay.portal.poller.comet;version=\"6.2.0\",com.liferay.portal.poller.messaging;version=\"6.2.0\",com.liferay.portal.poller;version=\"6.2.0\",com.liferay.portal.pop.messaging;version=\"6.2.0\",com.liferay.portal.pop;version=\"6.2.0\",com.liferay.portal.repository.cmis.model;version=\"6.2.0\",com.liferay.portal.repository.cmis;version=\"6.2.0\",com.liferay.portal.repository.liferayrepository.model;version=\"6.2.0\",com.liferay.portal.repository.liferayrepository.util;version=\"6.2.0\",com.liferay.portal.repository.liferayrepository;version=\"6.2.0\",com.liferay.portal.repository.proxy;version=\"6.2.0\",com.liferay.portal.repository.search;version=\"6.2.0\",com.liferay.portal.repository.util;version=\"6.2.0\",com.liferay.portal.repository;version=\"6.2.0\",com.liferay.portal.sanitizer;version=\"6.2.0\",com.liferay.portal.scheduler.job;version=\"6.2.0\",com.liferay.portal.scheduler.messaging;version=\"6.2.0\",com.liferay.portal.scheduler.quartz;version=\"6.2.0\",com.liferay.portal.scheduler;version=\"6.2.0\",com.liferay.portal.scripting.beanshell;version=\"6.2.0\",com.liferay.portal.scripting.groovy;version=\"6.2.0\",com.liferay.portal.scripting.javascript;version=\"6.2.0\",com.liferay.portal.scripting.python;version=\"6.2.0\",com.liferay.portal.scripting.ruby;version=\"6.2.0\",com.liferay.portal.scripting;version=\"6.2.0\",com.liferay.portal.search.generic;version=\"6.2.0\",com.liferay.portal.search.lucene.cluster;version=\"6.2.0\",com.liferay.portal.search.lucene.dump;version=\"6.2.0\",com.liferay.portal.search.lucene.highlight;version=\"6.2.0\",com.liferay.portal.search.lucene.messaging;version=\"6.2.0\",com.liferay.portal.search.lucene;version=\"6.2.0\",com.liferay.portal.search;version=\"6.2.0\",com.liferay.portal.security.auth;version=\"6.2.0\",com.liferay.portal.security.jaas.ext.jboss;version=\"6.2.0\",com.liferay.portal.security.jaas.ext.jetty;version=\"6.2.0\",com.liferay.portal.security.jaas.ext.jonas;version=\"6.2.0\",com.liferay.portal.security.jaas.ext.resin;version=\"6.2.0\",com.liferay.portal.security.jaas.ext.tomcat;version=\"6.2.0\",com.liferay.portal.security.jaas.ext.weblogic;version=\"6.2.0\",com.liferay.portal.security.jaas.ext;version=\"6.2.0\",com.liferay.portal.security.jaas;version=\"6.2.0\",com.liferay.portal.security.lang;version=\"6.2.0\",com.liferay.portal.security.ldap;version=\"6.2.0\",com.liferay.portal.security.ntlm.msrpc;version=\"6.2.0\",com.liferay.portal.security.ntlm;version=\"6.2.0\",com.liferay.portal.security.permission.comparator;version=\"6.2.0\",com.liferay.portal.security.permission;version=\"6.2.0\",com.liferay.portal.security.pwd;version=\"6.2.0\",com.liferay.portal.security;version=\"6.2.0\",com.liferay.portal.service,com.liferay.portal.service.base;version=\"6.2.0\",com.liferay.portal.service.http;version=\"6.2.0\",com.liferay.portal.service.impl;version=\"6.2.0\",com.liferay.portal.service.permission;version=\"6.2.0\",com.liferay.portal.service.persistence.impl;version=\"6.2.0\",com.liferay.portal.service.persistence;version=\"6.2.0\",com.liferay.portal.service;version=\"6.2.0\",com.liferay.portal.servlet.filters.absoluteredirects;version=\"6.2.0\",com.liferay.portal.servlet.filters.audit;version=\"6.2.0\",com.liferay.portal.servlet.filters.autologin;version=\"6.2.0\",com.liferay.portal.servlet.filters.cache;version=\"6.2.0\",com.liferay.portal.servlet.filters.charbufferpool;version=\"6.2.0\",com.liferay.portal.servlet.filters.compoundsessionid;version=\"6.2.0\",com.liferay.portal.servlet.filters.doubleclick;version=\"6.2.0\",com.liferay.portal.servlet.filters.dynamiccss;version=\"6.2.0\",com.liferay.portal.servlet.filters.etag;version=\"6.2.0\",com.liferay.portal.servlet.filters.fragment;version=\"6.2.0\",com.liferay.portal.servlet.filters.gzip;version=\"6.2.0\",com.liferay.portal.servlet.filters.header;version=\"6.2.0\",com.liferay.portal.servlet.filters.i18n;version=\"6.2.0\",com.liferay.portal.servlet.filters.ignore;version=\"6.2.0\",com.liferay.portal.servlet.filters.language;version=\"6.2.0\",com.liferay.portal.servlet.filters.minifier;version=\"6.2.0\",com.liferay.portal.servlet.filters.monitoring.jmx;version=\"6.2.0\",com.liferay.portal.servlet.filters.monitoring;version=\"6.2.0\",com.liferay.portal.servlet.filters.secure;version=\"6.2.0\",com.liferay.portal.servlet.filters.servletauthorizing;version=\"6.2.0\",com.liferay.portal.servlet.filters.servletcontextinclude;version=\"6.2.0\",com.liferay.portal.servlet.filters.sessionid;version=\"6.2.0\",com.liferay.portal.servlet.filters.sessionmaxallowed;version=\"6.2.0\",com.liferay.portal.servlet.filters.sso.cas;version=\"6.2.0\",com.liferay.portal.servlet.filters.sso.ntlm;version=\"6.2.0\",com.liferay.portal.servlet.filters.sso.opensso;version=\"6.2.0\",com.liferay.portal.servlet.filters.sso;version=\"6.2.0\",com.liferay.portal.servlet.filters.strip;version=\"6.2.0\",com.liferay.portal.servlet.filters.themepreview;version=\"6.2.0\",com.liferay.portal.servlet.filters.threaddump;version=\"6.2.0\",com.liferay.portal.servlet.filters.threadlocal;version=\"6.2.0\",com.liferay.portal.servlet.filters.unsyncprintwriterpool;version=\"6.2.0\",com.liferay.portal.servlet.filters.validhostname;version=\"6.2.0\",com.liferay.portal.servlet.filters.validhtml;version=\"6.2.0\",com.liferay.portal.servlet.filters.virtualhost;version=\"6.2.0\",com.liferay.portal.servlet.filters;version=\"6.2.0\",com.liferay.portal.servlet.taglib.ui;version=\"6.2.0\",com.liferay.portal.servlet.taglib;version=\"6.2.0\",com.liferay.portal.servlet;version=\"6.2.0\",com.liferay.portal.setup;version=\"6.2.0\",com.liferay.portal.sharepoint.dws;version=\"6.2.0\",com.liferay.portal.sharepoint.methods;version=\"6.2.0\",com.liferay.portal.sharepoint;version=\"6.2.0\",com.liferay.portal.spring.annotation;version=\"6.2.0\",com.liferay.portal.spring.aop;version=\"6.2.0\",com.liferay.portal.spring.bean;version=\"6.2.0\",com.liferay.portal.spring.context;version=\"6.2.0\",com.liferay.portal.spring.hibernate;version=\"6.2.0\",com.liferay.portal.spring.jndi;version=\"6.2.0\",com.liferay.portal.spring.jpa;version=\"6.2.0\",com.liferay.portal.spring.remoting;version=\"6.2.0\",com.liferay.portal.spring.servlet;version=\"6.2.0\",com.liferay.portal.spring.transaction;version=\"6.2.0\",com.liferay.portal.spring.util;version=\"6.2.0\",com.liferay.portal.spring;version=\"6.2.0\",com.liferay.portal.staging.permission;version=\"6.2.0\",com.liferay.portal.staging;version=\"6.2.0\",com.liferay.portal.struts;version=\"6.2.0\",com.liferay.portal.theme;version=\"6.2.0\",com.liferay.portal.tools.comparator;version=\"6.2.0\",com.liferay.portal.tools.dependencies;version=\"6.2.0\",com.liferay.portal.tools.deploy;version=\"6.2.0\",com.liferay.portal.tools.jspc.common;version=\"6.2.0\",com.liferay.portal.tools.jspc.resin;version=\"6.2.0\",com.liferay.portal.tools.jspc;version=\"6.2.0\",com.liferay.portal.tools.samplesqlbuilder.dependencies;version=\"6.2.0\",com.liferay.portal.tools.samplesqlbuilder;version=\"6.2.0\",com.liferay.portal.tools.servicebuilder.dependencies;version=\"6.2.0\",com.liferay.portal.tools.servicebuilder;version=\"6.2.0\",com.liferay.portal.tools.sql.dependencies;version=\"6.2.0\",com.liferay.portal.tools.sql;version=\"6.2.0\",com.liferay.portal.tools;version=\"6.2.0\",com.liferay.portal.upgrade.util;version=\"6.2.0\",com.liferay.portal.upgrade.v5_2_5_to_6_0_0.util;version=\"6.2.0\",com.liferay.portal.upgrade.v5_2_5_to_6_0_0;version=\"6.2.0\",com.liferay.portal.upgrade.v5_2_7_to_6_0_0;version=\"6.2.0\",com.liferay.portal.upgrade.v5_2_8_to_6_0_5.util;version=\"6.2.0\",com.liferay.portal.upgrade.v5_2_8_to_6_0_5;version=\"6.2.0\",com.liferay.portal.upgrade.v6_0_0.util;version=\"6.2.0\",com.liferay.portal.upgrade.v6_0_0;version=\"6.2.0\",com.liferay.portal.upgrade.v6_0_1.util;version=\"6.2.0\",com.liferay.portal.upgrade.v6_0_12_to_6_1_0;version=\"6.2.0\",com.liferay.portal.upgrade.v6_0_1;version=\"6.2.0\",com.liferay.portal.upgrade.v6_0_2;version=\"6.2.0\",com.liferay.portal.upgrade.v6_0_3;version=\"6.2.0\",com.liferay.portal.upgrade.v6_0_5.util;version=\"6.2.0\",com.liferay.portal.upgrade.v6_0_5;version=\"6.2.0\",com.liferay.portal.upgrade.v6_0_6;version=\"6.2.0\",com.liferay.portal.upgrade.v6_1_0.util;version=\"6.2.0\",com.liferay.portal.upgrade.v6_1_0;version=\"6.2.0\",com.liferay.portal.upgrade.v6_1_1;version=\"6.2.0\",com.liferay.portal.upgrade.v6_2_0;version=\"6.2.0\",com.liferay.portal.upgrade;version=\"6.2.0\",com.liferay.portal.upload;version=\"6.2.0\",com.liferay.portal.util.comparator;version=\"6.2.0\",com.liferay.portal.util.dependencies;version=\"6.2.0\",com.liferay.portal.util;version=\"6.2.0\",com.liferay.portal.uuid;version=\"6.2.0\",com.liferay.portal.velocity;version=\"6.2.0\",com.liferay.portal.verify;version=\"6.2.0\",com.liferay.portal.webcache;version=\"6.2.0\",com.liferay.portal.webdav.methods;version=\"6.2.0\",com.liferay.portal.webdav;version=\"6.2.0\",com.liferay.portal.webserver.dependencies;version=\"6.2.0\",com.liferay.portal.webserver;version=\"6.2.0\",com.liferay.portal.words.dependencies;version=\"6.2.0\",com.liferay.portal.words;version=\"6.2.0\",com.liferay.portal.workflow.permission;version=\"6.2.0\",com.liferay.portal.workflow;version=\"6.2.0\",com.liferay.portal.xml.xpath;version=\"6.2.0\",com.liferay.portal.xml;version=\"6.2.0\",com.liferay.portal.xmlrpc;version=\"6.2.0\",com.liferay.portal.zip;version=\"6.2.0\",com.liferay.portal;version=\"6.2.0\",com.liferay.portlet.activities.action;version=\"6.2.0\",com.liferay.portlet.activities;version=\"6.2.0\",com.liferay.portlet.addressbook;version=\"6.2.0\",com.liferay.portlet.admin.action;version=\"6.2.0\",com.liferay.portlet.admin.dependencies;version=\"6.2.0\",com.liferay.portlet.admin.messaging;version=\"6.2.0\",com.liferay.portlet.admin.util;version=\"6.2.0\",com.liferay.portlet.admin;version=\"6.2.0\",com.liferay.portlet.amazonrankings.action;version=\"6.2.0\",com.liferay.portlet.amazonrankings.model;version=\"6.2.0\",com.liferay.portlet.amazonrankings.util;version=\"6.2.0\",com.liferay.portlet.amazonrankings;version=\"6.2.0\",com.liferay.portlet.announcements.action;version=\"6.2.0\",com.liferay.portlet.announcements.dependencies;version=\"6.2.0\",com.liferay.portlet.announcements.messaging;version=\"6.2.0\",com.liferay.portlet.announcements.model.impl;version=\"6.2.0\",com.liferay.portlet.announcements.model;version=\"6.2.0\",com.liferay.portlet.announcements.service.base;version=\"6.2.0\",com.liferay.portlet.announcements.service.http;version=\"6.2.0\",com.liferay.portlet.announcements.service.impl;version=\"6.2.0\",com.liferay.portlet.announcements.service.permission;version=\"6.2.0\",com.liferay.portlet.announcements.service.persistence;version=\"6.2.0\",com.liferay.portlet.announcements.service;version=\"6.2.0\",com.liferay.portlet.announcements.util;version=\"6.2.0\",com.liferay.portlet.announcements;version=\"6.2.0\",com.liferay.portlet.asset.action;version=\"6.2.0\",com.liferay.portlet.asset.model.impl;version=\"6.2.0\",com.liferay.portlet.asset.model;version=\"6.2.0\",com.liferay.portlet.asset.service.base;version=\"6.2.0\",com.liferay.portlet.asset.service.http;version=\"6.2.0\",com.liferay.portlet.asset.service.impl;version=\"6.2.0\",com.liferay.portlet.asset.service.permission;version=\"6.2.0\",com.liferay.portlet.asset.service.persistence;version=\"6.2.0\",com.liferay.portlet.asset.service;version=\"6.2.0\",com.liferay.portlet.asset.util.comparator;version=\"6.2.0\",com.liferay.portlet.asset.util;version=\"6.2.0\",com.liferay.portlet.asset;version=\"6.2.0\",com.liferay.portlet.assetcategoryadmin.action;version=\"6.2.0\",com.liferay.portlet.assetcategoryadmin;version=\"6.2.0\",com.liferay.portlet.assetpublisher.action;version=\"6.2.0\",com.liferay.portlet.assetpublisher.search;version=\"6.2.0\",com.liferay.portlet.assetpublisher.util;version=\"6.2.0\",com.liferay.portlet.assetpublisher;version=\"6.2.0\",com.liferay.portlet.assettagadmin.action;version=\"6.2.0\",com.liferay.portlet.assettagadmin;version=\"6.2.0\",com.liferay.portlet.blogs.action;version=\"6.2.0\",com.liferay.portlet.blogs.asset;version=\"6.2.0\",com.liferay.portlet.blogs.atom;version=\"6.2.0\",com.liferay.portlet.blogs.dependencies;version=\"6.2.0\",com.liferay.portlet.blogs.lar;version=\"6.2.0\",com.liferay.portlet.blogs.messaging;version=\"6.2.0\",com.liferay.portlet.blogs.model.impl;version=\"6.2.0\",com.liferay.portlet.blogs.model;version=\"6.2.0\",com.liferay.portlet.blogs.service.base;version=\"6.2.0\",com.liferay.portlet.blogs.service.http;version=\"6.2.0\",com.liferay.portlet.blogs.service.impl;version=\"6.2.0\",com.liferay.portlet.blogs.service.permission;version=\"6.2.0\",com.liferay.portlet.blogs.service.persistence;version=\"6.2.0\",com.liferay.portlet.blogs.service;version=\"6.2.0\",com.liferay.portlet.blogs.social;version=\"6.2.0\",com.liferay.portlet.blogs.util.comparator;version=\"6.2.0\",com.liferay.portlet.blogs.util;version=\"6.2.0\",com.liferay.portlet.blogs.workflow;version=\"6.2.0\",com.liferay.portlet.blogs;version=\"6.2.0\",com.liferay.portlet.blogsadmin.search;version=\"6.2.0\",com.liferay.portlet.blogsadmin;version=\"6.2.0\",com.liferay.portlet.bookmarks.action;version=\"6.2.0\",com.liferay.portlet.bookmarks.asset;version=\"6.2.0\",com.liferay.portlet.bookmarks.lar;version=\"6.2.0\",com.liferay.portlet.bookmarks.model.impl;version=\"6.2.0\",com.liferay.portlet.bookmarks.model;version=\"6.2.0\",com.liferay.portlet.bookmarks.service.base;version=\"6.2.0\",com.liferay.portlet.bookmarks.service.http;version=\"6.2.0\",com.liferay.portlet.bookmarks.service.impl;version=\"6.2.0\",com.liferay.portlet.bookmarks.service.permission;version=\"6.2.0\",com.liferay.portlet.bookmarks.service.persistence;version=\"6.2.0\",com.liferay.portlet.bookmarks.service;version=\"6.2.0\",com.liferay.portlet.bookmarks.social;version=\"6.2.0\",com.liferay.portlet.bookmarks.util.comparator;version=\"6.2.0\",com.liferay.portlet.bookmarks.util;version=\"6.2.0\",com.liferay.portlet.bookmarks;version=\"6.2.0\",com.liferay.portlet.calendar.action;version=\"6.2.0\",com.liferay.portlet.calendar.asset;version=\"6.2.0\",com.liferay.portlet.calendar.dependencies;version=\"6.2.0\",com.liferay.portlet.calendar.lar;version=\"6.2.0\",com.liferay.portlet.calendar.messaging;version=\"6.2.0\",com.liferay.portlet.calendar.model.impl;version=\"6.2.0\",com.liferay.portlet.calendar.model;version=\"6.2.0\",com.liferay.portlet.calendar.service.base;version=\"6.2.0\",com.liferay.portlet.calendar.service.http;version=\"6.2.0\",com.liferay.portlet.calendar.service.impl;version=\"6.2.0\",com.liferay.portlet.calendar.service.permission;version=\"6.2.0\",com.liferay.portlet.calendar.service.persistence;version=\"6.2.0\",com.liferay.portlet.calendar.service;version=\"6.2.0\",com.liferay.portlet.calendar.social;version=\"6.2.0\",com.liferay.portlet.calendar.util.comparator;version=\"6.2.0\",com.liferay.portlet.calendar.util;version=\"6.2.0\",com.liferay.portlet.calendar;version=\"6.2.0\",com.liferay.portlet.communities.messaging;version=\"6.2.0\",com.liferay.portlet.communities;version=\"6.2.0\",com.liferay.portlet.currencyconverter.action;version=\"6.2.0\",com.liferay.portlet.currencyconverter.model;version=\"6.2.0\",com.liferay.portlet.currencyconverter.util;version=\"6.2.0\",com.liferay.portlet.currencyconverter;version=\"6.2.0\",com.liferay.portlet.directory.asset;version=\"6.2.0\",com.liferay.portlet.directory.util;version=\"6.2.0\",com.liferay.portlet.directory.workflow;version=\"6.2.0\",com.liferay.portlet.directory;version=\"6.2.0\",com.liferay.portlet.documentlibrary.action;version=\"6.2.0\",com.liferay.portlet.documentlibrary.antivirus;version=\"6.2.0\",com.liferay.portlet.documentlibrary.asset;version=\"6.2.0\",com.liferay.portlet.documentlibrary.atom;version=\"6.2.0\",com.liferay.portlet.documentlibrary.lar;version=\"6.2.0\",com.liferay.portlet.documentlibrary.messaging;version=\"6.2.0\",com.liferay.portlet.documentlibrary.model.impl;version=\"6.2.0\",com.liferay.portlet.documentlibrary.model;version=\"6.2.0\",com.liferay.portlet.documentlibrary.search;version=\"6.2.0\",com.liferay.portlet.documentlibrary.service.base;version=\"6.2.0\",com.liferay.portlet.documentlibrary.service.http;version=\"6.2.0\",com.liferay.portlet.documentlibrary.service.impl;version=\"6.2.0\",com.liferay.portlet.documentlibrary.service.permission;version=\"6.2.0\",com.liferay.portlet.documentlibrary.service.persistence;version=\"6.2.0\",com.liferay.portlet.documentlibrary.service;version=\"6.2.0\",com.liferay.portlet.documentlibrary.sharepoint;version=\"6.2.0\",com.liferay.portlet.documentlibrary.social;version=\"6.2.0\",com.liferay.portlet.documentlibrary.store;version=\"6.2.0\",com.liferay.portlet.documentlibrary.util.comparator;version=\"6.2.0\",com.liferay.portlet.documentlibrary.util;version=\"6.2.0\",com.liferay.portlet.documentlibrary.webdav;version=\"6.2.0\",com.liferay.portlet.documentlibrary.workflow;version=\"6.2.0\",com.liferay.portlet.documentlibrary;version=\"6.2.0\",com.liferay.portlet.dynamicdatalists.action;version=\"6.2.0\",com.liferay.portlet.dynamicdatalists.asset;version=\"6.2.0\",com.liferay.portlet.dynamicdatalists.dependencies;version=\"6.2.0\",com.liferay.portlet.dynamicdatalists.model.impl;version=\"6.2.0\",com.liferay.portlet.dynamicdatalists.model;version=\"6.2.0\",com.liferay.portlet.dynamicdatalists.search;version=\"6.2.0\",com.liferay.portlet.dynamicdatalists.service.base;version=\"6.2.0\",com.liferay.portlet.dynamicdatalists.service.http;version=\"6.2.0\",com.liferay.portlet.dynamicdatalists.service.impl;version=\"6.2.0\",com.liferay.portlet.dynamicdatalists.service.permission;version=\"6.2.0\",com.liferay.portlet.dynamicdatalists.service.persistence;version=\"6.2.0\",com.liferay.portlet.dynamicdatalists.service;version=\"6.2.0\",com.liferay.portlet.dynamicdatalists.util.comparator;version=\"6.2.0\",com.liferay.portlet.dynamicdatalists.util;version=\"6.2.0\",com.liferay.portlet.dynamicdatalists.workflow;version=\"6.2.0\",com.liferay.portlet.dynamicdatalists;version=\"6.2.0\",com.liferay.portlet.dynamicdatamapping.action;version=\"6.2.0\",com.liferay.portlet.dynamicdatamapping.dependencies.alloy;version=\"6.2.0\",com.liferay.portlet.dynamicdatamapping.dependencies.ddm;version=\"6.2.0\",com.liferay.portlet.dynamicdatamapping.dependencies.readonly;version=\"6.2.0\",com.liferay.portlet.dynamicdatamapping.dependencies;version=\"6.2.0\",com.liferay.portlet.dynamicdatamapping.lar;version=\"6.2.0\",com.liferay.portlet.dynamicdatamapping.model.impl;version=\"6.2.0\",com.liferay.portlet.dynamicdatamapping.model;version=\"6.2.0\",com.liferay.portlet.dynamicdatamapping.search;version=\"6.2.0\",com.liferay.portlet.dynamicdatamapping.service.base;version=\"6.2.0\",com.liferay.portlet.dynamicdatamapping.service.http;version=\"6.2.0\",com.liferay.portlet.dynamicdatamapping.service.impl;version=\"6.2.0\",com.liferay.portlet.dynamicdatamapping.service.permission;version=\"6.2.0\",com.liferay.portlet.dynamicdatamapping.service.persistence;version=\"6.2.0\",com.liferay.portlet.dynamicdatamapping.service;version=\"6.2.0\",com.liferay.portlet.dynamicdatamapping.storage.query;version=\"6.2.0\",com.liferay.portlet.dynamicdatamapping.storage;version=\"6.2.0\",com.liferay.portlet.dynamicdatamapping.util.comparator;version=\"6.2.0\",com.liferay.portlet.dynamicdatamapping.util;version=\"6.2.0\",com.liferay.portlet.dynamicdatamapping;version=\"6.2.0\",com.liferay.portlet.expando.action;version=\"6.2.0\",com.liferay.portlet.expando.model.impl;version=\"6.2.0\",com.liferay.portlet.expando.model;version=\"6.2.0\",com.liferay.portlet.expando.service.base;version=\"6.2.0\",com.liferay.portlet.expando.service.http;version=\"6.2.0\",com.liferay.portlet.expando.service.impl;version=\"6.2.0\",com.liferay.portlet.expando.service.permission;version=\"6.2.0\",com.liferay.portlet.expando.service.persistence;version=\"6.2.0\",com.liferay.portlet.expando.service;version=\"6.2.0\",com.liferay.portlet.expando.util.comparator;version=\"6.2.0\",com.liferay.portlet.expando.util;version=\"6.2.0\",com.liferay.portlet.expando;version=\"6.2.0\",com.liferay.portlet.flags.action;version=\"6.2.0\",com.liferay.portlet.flags.dependencies;version=\"6.2.0\",com.liferay.portlet.flags.messaging;version=\"6.2.0\",com.liferay.portlet.flags.service.base;version=\"6.2.0\",com.liferay.portlet.flags.service.http;version=\"6.2.0\",com.liferay.portlet.flags.service.impl;version=\"6.2.0\",com.liferay.portlet.flags.service;version=\"6.2.0\",com.liferay.portlet.flags;version=\"6.2.0\",com.liferay.portlet.grouppages.action;version=\"6.2.0\",com.liferay.portlet.grouppages;version=\"6.2.0\",com.liferay.portlet.helloworld;version=\"6.2.0\",com.liferay.portlet.iframe.action;version=\"6.2.0\",com.liferay.portlet.iframe.util;version=\"6.2.0\",com.liferay.portlet.iframe;version=\"6.2.0\",com.liferay.portlet.imagegallerydisplay.action;version=\"6.2.0\",com.liferay.portlet.imagegallerydisplay.util;version=\"6.2.0\",com.liferay.portlet.imagegallerydisplay;version=\"6.2.0\",com.liferay.portlet.invitation.action;version=\"6.2.0\",com.liferay.portlet.invitation.dependencies;version=\"6.2.0\",com.liferay.portlet.invitation.util;version=\"6.2.0\",com.liferay.portlet.invitation;version=\"6.2.0\",com.liferay.portlet.journal.action;version=\"6.2.0\",com.liferay.portlet.journal.asset;version=\"6.2.0\",com.liferay.portlet.journal.atom;version=\"6.2.0\",com.liferay.portlet.journal.dependencies;version=\"6.2.0\",com.liferay.portlet.journal.lar;version=\"6.2.0\",com.liferay.portlet.journal.messaging;version=\"6.2.0\",com.liferay.portlet.journal.model.impl;version=\"6.2.0\",com.liferay.portlet.journal.model;version=\"6.2.0\",com.liferay.portlet.journal.search;version=\"6.2.0\",com.liferay.portlet.journal.service.base;version=\"6.2.0\",com.liferay.portlet.journal.service.http;version=\"6.2.0\",com.liferay.portlet.journal.service.impl;version=\"6.2.0\",com.liferay.portlet.journal.service.permission;version=\"6.2.0\",com.liferay.portlet.journal.service.persistence;version=\"6.2.0\",com.liferay.portlet.journal.service;version=\"6.2.0\",com.liferay.portlet.journal.util.comparator;version=\"6.2.0\",com.liferay.portlet.journal.util;version=\"6.2.0\",com.liferay.portlet.journal.webdav;version=\"6.2.0\",com.liferay.portlet.journal.workflow;version=\"6.2.0\",com.liferay.portlet.journal;version=\"6.2.0\",com.liferay.portlet.journalarticles.action;version=\"6.2.0\",com.liferay.portlet.journalarticles;version=\"6.2.0\",com.liferay.portlet.journalcontent.action;version=\"6.2.0\",com.liferay.portlet.journalcontent.util;version=\"6.2.0\",com.liferay.portlet.journalcontent;version=\"6.2.0\",com.liferay.portlet.journalcontentsearch.action;version=\"6.2.0\",com.liferay.portlet.journalcontentsearch.util;version=\"6.2.0\",com.liferay.portlet.journalcontentsearch;version=\"6.2.0\",com.liferay.portlet.language.action;version=\"6.2.0\",com.liferay.portlet.language;version=\"6.2.0\",com.liferay.portlet.layoutconfiguration.util.velocity;version=\"6.2.0\",com.liferay.portlet.layoutconfiguration.util.xml;version=\"6.2.0\",com.liferay.portlet.layoutconfiguration.util;version=\"6.2.0\",com.liferay.portlet.layoutconfiguration;version=\"6.2.0\",com.liferay.portlet.layoutprototypes.action;version=\"6.2.0\",com.liferay.portlet.layoutprototypes;version=\"6.2.0\",com.liferay.portlet.layoutsadmin.action;version=\"6.2.0\",com.liferay.portlet.layoutsadmin.util;version=\"6.2.0\",com.liferay.portlet.layoutsadmin;version=\"6.2.0\",com.liferay.portlet.layoutsetprototypes.action;version=\"6.2.0\",com.liferay.portlet.layoutsetprototypes;version=\"6.2.0\",com.liferay.portlet.login.action;version=\"6.2.0\",com.liferay.portlet.login.util;version=\"6.2.0\",com.liferay.portlet.login;version=\"6.2.0\",com.liferay.portlet.messageboards.action;version=\"6.2.0\",com.liferay.portlet.messageboards.asset;version=\"6.2.0\",com.liferay.portlet.messageboards.dependencies;version=\"6.2.0\",com.liferay.portlet.messageboards.lar;version=\"6.2.0\",com.liferay.portlet.messageboards.messaging;version=\"6.2.0\",com.liferay.portlet.messageboards.model.impl;version=\"6.2.0\",com.liferay.portlet.messageboards.model;version=\"6.2.0\",com.liferay.portlet.messageboards.pop;version=\"6.2.0\",com.liferay.portlet.messageboards.service.base;version=\"6.2.0\",com.liferay.portlet.messageboards.service.http;version=\"6.2.0\",com.liferay.portlet.messageboards.service.impl;version=\"6.2.0\",com.liferay.portlet.messageboards.service.permission;version=\"6.2.0\",com.liferay.portlet.messageboards.service.persistence;version=\"6.2.0\",com.liferay.portlet.messageboards.service;version=\"6.2.0\",com.liferay.portlet.messageboards.social;version=\"6.2.0\",com.liferay.portlet.messageboards.util.comparator;version=\"6.2.0\",com.liferay.portlet.messageboards.util;version=\"6.2.0\",com.liferay.portlet.messageboards.workflow;version=\"6.2.0\",com.liferay.portlet.messageboards;version=\"6.2.0\",com.liferay.portlet.mobiledevicerules.action;version=\"6.2.0\",com.liferay.portlet.mobiledevicerules.model.impl;version=\"6.2.0\",com.liferay.portlet.mobiledevicerules.model;version=\"6.2.0\",com.liferay.portlet.mobiledevicerules.search;version=\"6.2.0\",com.liferay.portlet.mobiledevicerules.service.base;version=\"6.2.0\",com.liferay.portlet.mobiledevicerules.service.http;version=\"6.2.0\",com.liferay.portlet.mobiledevicerules.service.impl;version=\"6.2.0\",com.liferay.portlet.mobiledevicerules.service.permission;version=\"6.2.0\",com.liferay.portlet.mobiledevicerules.service.persistence;version=\"6.2.0\",com.liferay.portlet.mobiledevicerules.service;version=\"6.2.0\",com.liferay.portlet.mobiledevicerules.util;version=\"6.2.0\",com.liferay.portlet.mobiledevicerules;version=\"6.2.0\",com.liferay.portlet.monitoring.action;version=\"6.2.0\",com.liferay.portlet.monitoring;version=\"6.2.0\",com.liferay.portlet.myaccount.action;version=\"6.2.0\",com.liferay.portlet.myaccount;version=\"6.2.0\",com.liferay.portlet.mypages.action;version=\"6.2.0\",com.liferay.portlet.mypages;version=\"6.2.0\",com.liferay.portlet.myplaces.action;version=\"6.2.0\",com.liferay.portlet.myplaces;version=\"6.2.0\",com.liferay.portlet.nestedportlets.action;version=\"6.2.0\",com.liferay.portlet.nestedportlets;version=\"6.2.0\",com.liferay.portlet.network.model;version=\"6.2.0\",com.liferay.portlet.network.util;version=\"6.2.0\",com.liferay.portlet.network;version=\"6.2.0\",com.liferay.portlet.pagecomments.lar;version=\"6.2.0\",com.liferay.portlet.pagecomments;version=\"6.2.0\",com.liferay.portlet.pageratings.lar;version=\"6.2.0\",com.liferay.portlet.pageratings;version=\"6.2.0\",com.liferay.portlet.passwordpoliciesadmin.action;version=\"6.2.0\",com.liferay.portlet.passwordpoliciesadmin.search;version=\"6.2.0\",com.liferay.portlet.passwordpoliciesadmin.util;version=\"6.2.0\",com.liferay.portlet.passwordpoliciesadmin;version=\"6.2.0\",com.liferay.portlet.plugininstaller.action;version=\"6.2.0\",com.liferay.portlet.plugininstaller;version=\"6.2.0\",com.liferay.portlet.pluginsadmin.action;version=\"6.2.0\",com.liferay.portlet.pluginsadmin;version=\"6.2.0\",com.liferay.portlet.polls.action;version=\"6.2.0\",com.liferay.portlet.polls.lar;version=\"6.2.0\",com.liferay.portlet.polls.model.impl;version=\"6.2.0\",com.liferay.portlet.polls.model;version=\"6.2.0\",com.liferay.portlet.polls.service.base;version=\"6.2.0\",com.liferay.portlet.polls.service.http;version=\"6.2.0\",com.liferay.portlet.polls.service.impl;version=\"6.2.0\",com.liferay.portlet.polls.service.permission;version=\"6.2.0\",com.liferay.portlet.polls.service.persistence;version=\"6.2.0\",com.liferay.portlet.polls.service;version=\"6.2.0\",com.liferay.portlet.polls.util;version=\"6.2.0\",com.liferay.portlet.polls;version=\"6.2.0\",com.liferay.portlet.pollsdisplay.action;version=\"6.2.0\",com.liferay.portlet.pollsdisplay;version=\"6.2.0\",com.liferay.portlet.portalsettings.action;version=\"6.2.0\",com.liferay.portlet.portalsettings;version=\"6.2.0\",com.liferay.portlet.portletconfiguration.action;version=\"6.2.0\",com.liferay.portlet.portletconfiguration.util;version=\"6.2.0\",com.liferay.portlet.portletconfiguration;version=\"6.2.0\",com.liferay.portlet.portletsharing.action;version=\"6.2.0\",com.liferay.portlet.portletsharing;version=\"6.2.0\",com.liferay.portlet.quicknote.action;version=\"6.2.0\",com.liferay.portlet.quicknote;version=\"6.2.0\",com.liferay.portlet.ratings.action;version=\"6.2.0\",com.liferay.portlet.ratings.model.impl;version=\"6.2.0\",com.liferay.portlet.ratings.model;version=\"6.2.0\",com.liferay.portlet.ratings.service.base;version=\"6.2.0\",com.liferay.portlet.ratings.service.http;version=\"6.2.0\",com.liferay.portlet.ratings.service.impl;version=\"6.2.0\",com.liferay.portlet.ratings.service.persistence;version=\"6.2.0\",com.liferay.portlet.ratings.service;version=\"6.2.0\",com.liferay.portlet.ratings;version=\"6.2.0\",com.liferay.portlet.requests.action;version=\"6.2.0\",com.liferay.portlet.requests;version=\"6.2.0\",com.liferay.portlet.rolesadmin.action;version=\"6.2.0\",com.liferay.portlet.rolesadmin.search;version=\"6.2.0\",com.liferay.portlet.rolesadmin.util;version=\"6.2.0\",com.liferay.portlet.rolesadmin;version=\"6.2.0\",com.liferay.portlet.rss.action;version=\"6.2.0\",com.liferay.portlet.rss.lar;version=\"6.2.0\",com.liferay.portlet.rss.util;version=\"6.2.0\",com.liferay.portlet.rss;version=\"6.2.0\",com.liferay.portlet.search.action;version=\"6.2.0\",com.liferay.portlet.search;version=\"6.2.0\",com.liferay.portlet.shopping.action;version=\"6.2.0\",com.liferay.portlet.shopping.dependencies;version=\"6.2.0\",com.liferay.portlet.shopping.model.impl;version=\"6.2.0\",com.liferay.portlet.shopping.model;version=\"6.2.0\",com.liferay.portlet.shopping.search;version=\"6.2.0\",com.liferay.portlet.shopping.service.base;version=\"6.2.0\",com.liferay.portlet.shopping.service.http;version=\"6.2.0\",com.liferay.portlet.shopping.service.impl;version=\"6.2.0\",com.liferay.portlet.shopping.service.permission;version=\"6.2.0\",com.liferay.portlet.shopping.service.persistence;version=\"6.2.0\",com.liferay.portlet.shopping.service;version=\"6.2.0\",com.liferay.portlet.shopping.util.comparator;version=\"6.2.0\",com.liferay.portlet.shopping.util;version=\"6.2.0\",com.liferay.portlet.shopping;version=\"6.2.0\",com.liferay.portlet.sites.action;version=\"6.2.0\",com.liferay.portlet.sites.dependencies;version=\"6.2.0\",com.liferay.portlet.sites.search;version=\"6.2.0\",com.liferay.portlet.sites.util;version=\"6.2.0\",com.liferay.portlet.sites;version=\"6.2.0\",com.liferay.portlet.sitesadmin.search;version=\"6.2.0\",com.liferay.portlet.sitesadmin;version=\"6.2.0\",com.liferay.portlet.social.model.impl;version=\"6.2.0\",com.liferay.portlet.social.model;version=\"6.2.0\",com.liferay.portlet.social.service.base;version=\"6.2.0\",com.liferay.portlet.social.service.http;version=\"6.2.0\",com.liferay.portlet.social.service.impl;version=\"6.2.0\",com.liferay.portlet.social.service.persistence;version=\"6.2.0\",com.liferay.portlet.social.service;version=\"6.2.0\",com.liferay.portlet.social.util.comparator;version=\"6.2.0\",com.liferay.portlet.social.util;version=\"6.2.0\",com.liferay.portlet.social;version=\"6.2.0\",com.liferay.portlet.socialactivity.action;version=\"6.2.0\",com.liferay.portlet.socialactivity;version=\"6.2.0\",com.liferay.portlet.softwarecatalog.action;version=\"6.2.0\",com.liferay.portlet.softwarecatalog.model.impl;version=\"6.2.0\",com.liferay.portlet.softwarecatalog.model;version=\"6.2.0\",com.liferay.portlet.softwarecatalog.service.base;version=\"6.2.0\",com.liferay.portlet.softwarecatalog.service.http;version=\"6.2.0\",com.liferay.portlet.softwarecatalog.service.impl;version=\"6.2.0\",com.liferay.portlet.softwarecatalog.service.permission;version=\"6.2.0\",com.liferay.portlet.softwarecatalog.service.persistence;version=\"6.2.0\",com.liferay.portlet.softwarecatalog.service;version=\"6.2.0\",com.liferay.portlet.softwarecatalog.util.comparator;version=\"6.2.0\",com.liferay.portlet.softwarecatalog.util;version=\"6.2.0\",com.liferay.portlet.softwarecatalog;version=\"6.2.0\",com.liferay.portlet.stagingbar.action;version=\"6.2.0\",com.liferay.portlet.stagingbar;version=\"6.2.0\",com.liferay.portlet.tagscompiler.util;version=\"6.2.0\",com.liferay.portlet.tagscompiler;version=\"6.2.0\",com.liferay.portlet.translator.model;version=\"6.2.0\",com.liferay.portlet.translator.util;version=\"6.2.0\",com.liferay.portlet.translator;version=\"6.2.0\",com.liferay.portlet.unitconverter.model;version=\"6.2.0\",com.liferay.portlet.unitconverter.util;version=\"6.2.0\",com.liferay.portlet.unitconverter;version=\"6.2.0\",com.liferay.portlet.usergroupsadmin.action;version=\"6.2.0\",com.liferay.portlet.usergroupsadmin.search;version=\"6.2.0\",com.liferay.portlet.usergroupsadmin;version=\"6.2.0\",com.liferay.portlet.usersadmin.action;version=\"6.2.0\",com.liferay.portlet.usersadmin.atom;version=\"6.2.0\",com.liferay.portlet.usersadmin.search;version=\"6.2.0\",com.liferay.portlet.usersadmin.util;version=\"6.2.0\",com.liferay.portlet.usersadmin;version=\"6.2.0\",com.liferay.portlet.webproxy.action;version=\"6.2.0\",com.liferay.portlet.webproxy;version=\"6.2.0\",com.liferay.portlet.wiki.action;version=\"6.2.0\",com.liferay.portlet.wiki.asset;version=\"6.2.0\",com.liferay.portlet.wiki.dependencies;version=\"6.2.0\",com.liferay.portlet.wiki.engines.antlrwiki.translator.internal;version=\"6.2.0\",com.liferay.portlet.wiki.engines.antlrwiki.translator;version=\"6.2.0\",com.liferay.portlet.wiki.engines.antlrwiki;version=\"6.2.0\",com.liferay.portlet.wiki.engines.jspwiki.filters;version=\"6.2.0\",com.liferay.portlet.wiki.engines.jspwiki.plugin;version=\"6.2.0\",com.liferay.portlet.wiki.engines.jspwiki;version=\"6.2.0\",com.liferay.portlet.wiki.engines.mediawiki.matchers;version=\"6.2.0\",com.liferay.portlet.wiki.engines.mediawiki;version=\"6.2.0\",com.liferay.portlet.wiki.engines;version=\"6.2.0\",com.liferay.portlet.wiki.importers.mediawiki;version=\"6.2.0\",com.liferay.portlet.wiki.importers;version=\"6.2.0\",com.liferay.portlet.wiki.lar;version=\"6.2.0\",com.liferay.portlet.wiki.model.impl;version=\"6.2.0\",com.liferay.portlet.wiki.model;version=\"6.2.0\",com.liferay.portlet.wiki.security.permission;version=\"6.2.0\",com.liferay.portlet.wiki.security;version=\"6.2.0\",com.liferay.portlet.wiki.service.base;version=\"6.2.0\",com.liferay.portlet.wiki.service.http;version=\"6.2.0\",com.liferay.portlet.wiki.service.impl;version=\"6.2.0\",com.liferay.portlet.wiki.service.permission;version=\"6.2.0\",com.liferay.portlet.wiki.service.persistence;version=\"6.2.0\",com.liferay.portlet.wiki.service;version=\"6.2.0\",com.liferay.portlet.wiki.social;version=\"6.2.0\",com.liferay.portlet.wiki.translators;version=\"6.2.0\",com.liferay.portlet.wiki.util.comparator;version=\"6.2.0\",com.liferay.portlet.wiki.util;version=\"6.2.0\",com.liferay.portlet.wiki.workflow;version=\"6.2.0\",com.liferay.portlet.wiki;version=\"6.2.0\",com.liferay.portlet.wikidisplay.action;version=\"6.2.0\",com.liferay.portlet.wikidisplay;version=\"6.2.0\",com.liferay.portlet.workflowdefinitionlinks.action;version=\"6.2.0\",com.liferay.portlet.workflowdefinitionlinks;version=\"6.2.0\",com.liferay.portlet.workflowdefinitions.action;version=\"6.2.0\",com.liferay.portlet.workflowdefinitions;version=\"6.2.0\",com.liferay.portlet.workflowinstances.action;version=\"6.2.0\",com.liferay.portlet.workflowinstances;version=\"6.2.0\",com.liferay.portlet.workflowtasks.action;version=\"6.2.0\",com.liferay.portlet.workflowtasks.search;version=\"6.2.0\",com.liferay.portlet.workflowtasks;version=\"6.2.0\",com.liferay.portlet.xslcontent.action;version=\"6.2.0\",com.liferay.portlet.xslcontent.util;version=\"6.2.0\",com.liferay.portlet.xslcontent;version=\"6.2.0\",com.liferay.portlet;version=\"6.2.0\",com.liferay.taglib.aui.base;version=\"6.2.0\",com.liferay.taglib.aui;version=\"6.2.0\",com.liferay.taglib.core;version=\"6.2.0\",com.liferay.taglib.faces.converter;version=\"6.2.0\",com.liferay.taglib.faces.util;version=\"6.2.0\",com.liferay.taglib.faces.validator;version=\"6.2.0\",com.liferay.taglib.faces;version=\"6.2.0\",com.liferay.taglib.portlet;version=\"6.2.0\",com.liferay.taglib.portletext;version=\"6.2.0\",com.liferay.taglib.security;version=\"6.2.0\",com.liferay.taglib.theme;version=\"6.2.0\",com.liferay.taglib.ui;version=\"6.2.0\",com.liferay.taglib.util;version=\"6.2.0\",com.liferay.taglib;version=\"6.2.0\",com.liferay.util.ant;version=\"6.2.0\",com.liferay.util.aspectj;version=\"6.2.0\",com.liferay.util.axis;version=\"6.2.0\",com.liferay.util.bean,com.liferay.util.bean;version=\"6.2.0\",com.liferay.util.bridges.alloy;version=\"6.2.0\",com.liferay.util.bridges.bsf;version=\"6.2.0\",com.liferay.util.bridges.common;version=\"6.2.0\",com.liferay.util.bridges.groovy;version=\"6.2.0\",com.liferay.util.bridges.javascript;version=\"6.2.0\",com.liferay.util.bridges.jsf.common.comparator;version=\"6.2.0\",com.liferay.util.bridges.jsf.common;version=\"6.2.0\",com.liferay.util.bridges.jsf.icefaces;version=\"6.2.0\",com.liferay.util.bridges.jsf.myfaces;version=\"6.2.0\",com.liferay.util.bridges.jsf.sun;version=\"6.2.0\",com.liferay.util.bridges.jsf;version=\"6.2.0\",com.liferay.util.bridges.jsp;version=\"6.2.0\",com.liferay.util.bridges.mvc;version=\"6.2.0\",com.liferay.util.bridges.php;version=\"6.2.0\",com.liferay.util.bridges.python;version=\"6.2.0\",com.liferay.util.bridges.ruby;version=\"6.2.0\",com.liferay.util.bridges.scripting;version=\"6.2.0\",com.liferay.util.bridges.struts;version=\"6.2.0\",com.liferay.util.bridges.wai;version=\"6.2.0\",com.liferay.util.bridges;version=\"6.2.0\",com.liferay.util.cal;version=\"6.2.0\",com.liferay.util.dao.orm.hibernate;version=\"6.2.0\",com.liferay.util.dao.orm;version=\"6.2.0\",com.liferay.util.dao;version=\"6.2.0\",com.liferay.util.derby;version=\"6.2.0\",com.liferay.util.diff;version=\"6.2.0\",com.liferay.util.format;version=\"6.2.0\",com.liferay.util.freemarker;version=\"6.2.0\",com.liferay.util.jazzy;version=\"6.2.0\",com.liferay.util.json;version=\"6.2.0\",com.liferay.util.ldap;version=\"6.2.0\",com.liferay.util.log4j;version=\"6.2.0\",com.liferay.util.lucene;version=\"6.2.0\",com.liferay.util.mail;version=\"6.2.0\",com.liferay.util.poi;version=\"6.2.0\",com.liferay.util.portlet;version=\"6.2.0\",com.liferay.util.service;version=\"6.2.0\",com.liferay.util.servlet.filters;version=\"6.2.0\",com.liferay.util.servlet;version=\"6.2.0\",com.liferay.util.sl4fj;version=\"6.2.0\",com.liferay.util.spring.transaction;version=\"6.2.0\",com.liferay.util.spring;version=\"6.2.0\",com.liferay.util.transport;version=\"6.2.0\",com.liferay.util.xml.descriptor;version=\"6.2.0\",com.liferay.util.xml;version=\"6.2.0\",com.liferay.util;version=\"6.2.0\",com.liferay;version=\"6.2.0\",freemarker.ext.servlet,freemarker.template,javax.annotation.security;version=\"1.1.0\",javax.annotation;version=\"1.1.0\",javax.el;version=\"2.2.0\",javax.faces.convert,javax.faces.webapp,javax.mail.event;version=\"1.4\",javax.mail.internet;version=\"1.4\",javax.mail.search;version=\"1.4\",javax.mail.util;version=\"1.4\",javax.mail;version=\"1.4\",javax.portlet.filter;version=\"2.0.0\",javax.portlet;version=\"2.0.0\",javax.servlet.annotation;version=\"3.0.0\",javax.servlet.descriptor;version=\"3.0.0\",javax.servlet.http;version=\"3.0.0\",javax.servlet.jsp.el;version=\"2.2\",javax.servlet.jsp.jstl.core;version=\"1.2\",javax.servlet.jsp.jstl.fmt;version=\"1.2\",javax.servlet.jsp.jstl.sql;version=\"1.2\",javax.servlet.jsp.jstl.tlv;version=\"1.2\",javax.servlet.jsp.resources;version=\"2.2\",javax.servlet.jsp.tagext;version=\"2.2\",javax.servlet.jsp;version=\"2.2\",javax.servlet.resources;version=\"3.0.0\",javax.servlet;version=\"3.0.0\",org.aopalliance.aop,org.aopalliance.intercept,org.apache.commons.codec,org.apache.commons.codec.binary,org.apache.commons.codec.digest,org.apache.commons.codec.languages,org.apache.commons.codec.net,org.apache.commons.fileupload,org.apache.commons.fileupload.disk,org.apache.commons.fileupload.portlet,org.apache.commons.fileupload.servlet,org.apache.commons.fileupload.util,org.apache.commons.io,org.apache.commons.io.comparator,org.apache.commons.io.filefilter,org.apache.commons.io.input,org.apache.commons.io.output,org.apache.commons.lang,org.apache.commons.lang.builder,org.apache.commons.lang.enums,org.apache.commons.lang.exception,org.apache.commons.lang.math,org.apache.commons.lang.mutable,org.apache.commons.lang.reflect,org.apache.commons.lang.text,org.apache.commons.lang.time,org.apache.commons.logging;version=\"1.1.1\",org.apache.el,org.apache.jasper,org.apache.jasper.runtime,org.apache.jasper.servlet,org.apache.log4j,org.apache.log4j.chainsaw,org.apache.log4j.config,org.apache.log4j.helpers,org.apache.log4j.jdbc,org.apache.log4j.jmx,org.apache.log4j.lf5,org.apache.log4j.lf5.util,org.apache.log4j.lf5.viewer,org.apache.log4j.net,org.apache.log4j.nt,org.apache.log4j.or,org.apache.log4j.or.jms,org.apache.log4j.or.sax,org.apache.log4j.pattern,org.apache.log4j.spi,org.apache.log4j.varia,org.apache.log4j.xml,org.apache.naming,org.apache.naming.factory,org.apache.naming.java,org.apache.naming.resources,org.apache.naming.resources.jndi,org.apache.taglibs.standard.functions,org.apache.taglibs.standard.resources,org.apache.taglibs.standard.tag.common.core,org.apache.taglibs.standard.tag.rt.core,org.apache.taglibs.standard.tei,org.apache.taglibs.standard.tlv,org.apache.tomcat,org.eclipse.jdt.core.compiler;version=\"3.7.0.I20110613-1736\",org.eclipse.jdt.core;version=\"3.7.0.I20110613-1736\",org.eclipse.jdt.internal.antadapter;version=\"3.7.0.I20110613-1736\",org.eclipse.jdt.internal.compiler.apt.dispatch;version=\"3.7.0.I20110613-1736\",org.eclipse.jdt.internal.compiler.apt.model;version=\"3.7.0.I20110613-1736\",org.eclipse.jdt.internal.compiler.apt.util;version=\"3.7.0.I20110613-1736\",org.eclipse.jdt.internal.compiler.ast;version=\"3.7.0.I20110613-1736\",org.eclipse.jdt.internal.compiler.batch;version=\"3.7.0.I20110613-1736\",org.eclipse.jdt.internal.compiler.classfmt;version=\"3.7.0.I20110613-1736\",org.eclipse.jdt.internal.compiler.codegen;version=\"3.7.0.I20110613-1736\",org.eclipse.jdt.internal.compiler.env;version=\"3.7.0.I20110613-1736\",org.eclipse.jdt.internal.compiler.flow;version=\"3.7.0.I20110613-1736\",org.eclipse.jdt.internal.compiler.impl;version=\"3.7.0.I20110613-1736\",org.eclipse.jdt.internal.compiler.lookup;version=\"3.7.0.I20110613-1736\",org.eclipse.jdt.internal.compiler.parser.diagnose;version=\"3.7.0.I20110613-1736\",org.eclipse.jdt.internal.compiler.parser;version=\"3.7.0.I20110613-1736\",org.eclipse.jdt.internal.compiler.problem;version=\"3.7.0.I20110613-1736\",org.eclipse.jdt.internal.compiler.tool;version=\"3.7.0.I20110613-1736\",org.eclipse.jdt.internal.compiler.util;version=\"3.7.0.I20110613-1736\",org.eclipse.jdt.internal.compiler;version=\"3.7.0.I20110613-1736\",org.objectweb.asm,org.objectweb.asm.commons,org.objectweb.asm.signature,org.slf4j.impl;version=\"6.2.0\",org.slf4j;version=\"6.2.0\",org.springframework.aop.aspectj.annotation;version=\"3.0.6.RELEASE\",org.springframework.aop.aspectj.autoproxy;version=\"3.0.6.RELEASE\",org.springframework.aop.aspectj;version=\"3.0.6.RELEASE\",org.springframework.aop.config;version=\"3.0.6.RELEASE\",org.springframework.aop.framework.adapter;version=\"3.0.6.RELEASE\",org.springframework.aop.framework.autoproxy.target;version=\"3.0.6.RELEASE\",org.springframework.aop.framework.autoproxy;version=\"3.0.6.RELEASE\",org.springframework.aop.framework;version=\"3.0.6.RELEASE\",org.springframework.aop.interceptor;version=\"3.0.6.RELEASE\",org.springframework.aop.scope;version=\"3.0.6.RELEASE\",org.springframework.aop.support.annotation;version=\"3.0.6.RELEASE\",org.springframework.aop.support;version=\"3.0.6.RELEASE\",org.springframework.aop.target.dynamic;version=\"3.0.6.RELEASE\",org.springframework.aop.target;version=\"3.0.6.RELEASE\",org.springframework.aop;version=\"3.0.6.RELEASE\",org.springframework.asm.commons;version=\"3.0.6.RELEASE\",org.springframework.asm.signature;version=\"3.0.6.RELEASE\",org.springframework.asm;version=\"3.0.6.RELEASE\",org.springframework.beans.annotation;version=\"3.0.6.RELEASE\",org.springframework.beans.factory.access.el;version=\"3.0.6.RELEASE\",org.springframework.beans.factory.access;version=\"3.0.6.RELEASE\",org.springframework.beans.factory.annotation;version=\"3.0.6.RELEASE\",org.springframework.beans.factory.aspectj;version=\"3.0.6.RELEASE\",org.springframework.beans.factory.config;version=\"3.0.6.RELEASE\",org.springframework.beans.factory.parsing;version=\"3.0.6.RELEASE\",org.springframework.beans.factory.serviceloader;version=\"3.0.6.RELEASE\",org.springframework.beans.factory.support;version=\"3.0.6.RELEASE\",org.springframework.beans.factory.wiring;version=\"3.0.6.RELEASE\",org.springframework.beans.factory.xml;version=\"3.0.6.RELEASE\",org.springframework.beans.factory;version=\"3.0.6.RELEASE\",org.springframework.beans.propertyeditors;version=\"3.0.6.RELEASE\",org.springframework.beans.support;version=\"3.0.6.RELEASE\",org.springframework.beans;version=\"3.0.6.RELEASE\",org.springframework.cache.ehcache;version=\"3.0.6.RELEASE\",org.springframework.context.access;version=\"3.0.6.RELEASE\",org.springframework.context.annotation;version=\"3.0.6.RELEASE\",org.springframework.context.config;version=\"3.0.6.RELEASE\",org.springframework.context.event;version=\"3.0.6.RELEASE\",org.springframework.context.expression;version=\"3.0.6.RELEASE\",org.springframework.context.i18n;version=\"3.0.6.RELEASE\",org.springframework.context.support;version=\"3.0.6.RELEASE\",org.springframework.context.weaving;version=\"3.0.6.RELEASE\",org.springframework.context;version=\"3.0.6.RELEASE\",org.springframework.core.annotation;version=\"3.0.6.RELEASE\",org.springframework.core.convert.converter;version=\"3.0.6.RELEASE\",org.springframework.core.convert.support;version=\"3.0.6.RELEASE\",org.springframework.core.convert;version=\"3.0.6.RELEASE\",org.springframework.core.enums;version=\"3.0.6.RELEASE\",org.springframework.core.io.support;version=\"3.0.6.RELEASE\",org.springframework.core.io;version=\"3.0.6.RELEASE\",org.springframework.core.serializer.support;version=\"3.0.6.RELEASE\",org.springframework.core.serializer;version=\"3.0.6.RELEASE\",org.springframework.core.style;version=\"3.0.6.RELEASE\",org.springframework.core.task.support;version=\"3.0.6.RELEASE\",org.springframework.core.task;version=\"3.0.6.RELEASE\",org.springframework.core.type.classreading;version=\"3.0.6.RELEASE\",org.springframework.core.type.filter;version=\"3.0.6.RELEASE\",org.springframework.core.type;version=\"3.0.6.RELEASE\",org.springframework.core;version=\"3.0.6.RELEASE\",org.springframework.dao.annotation;version=\"3.0.6.RELEASE\",org.springframework.dao.support;version=\"3.0.6.RELEASE\",org.springframework.dao;version=\"3.0.6.RELEASE\",org.springframework.ejb.access;version=\"3.0.6.RELEASE\",org.springframework.ejb.config;version=\"3.0.6.RELEASE\",org.springframework.ejb.interceptor;version=\"3.0.6.RELEASE\",org.springframework.ejb.support;version=\"3.0.6.RELEASE\",org.springframework.expression.common;version=\"3.0.6.RELEASE\",org.springframework.expression.spel.ast;version=\"3.0.6.RELEASE\",org.springframework.expression.spel.generated;version=\"3.0.6.RELEASE\",org.springframework.expression.spel.standard;version=\"3.0.6.RELEASE\",org.springframework.expression.spel.support;version=\"3.0.6.RELEASE\",org.springframework.expression.spel;version=\"3.0.6.RELEASE\",org.springframework.expression;version=\"3.0.6.RELEASE\",org.springframework.format.annotation;version=\"3.0.6.RELEASE\",org.springframework.format.datetime.joda;version=\"3.0.6.RELEASE\",org.springframework.format.datetime;version=\"3.0.6.RELEASE\",org.springframework.format.number;version=\"3.0.6.RELEASE\",org.springframework.format.support;version=\"3.0.6.RELEASE\",org.springframework.format;version=\"3.0.6.RELEASE\",org.springframework.http.client.support;version=\"3.0.6.RELEASE\",org.springframework.http.client;version=\"3.0.6.RELEASE\",org.springframework.http.converter.feed;version=\"3.0.6.RELEASE\",org.springframework.http.converter.json;version=\"3.0.6.RELEASE\",org.springframework.http.converter.xml;version=\"3.0.6.RELEASE\",org.springframework.http.converter;version=\"3.0.6.RELEASE\",org.springframework.http.server;version=\"3.0.6.RELEASE\",org.springframework.http;version=\"3.0.6.RELEASE\",org.springframework.instrument.classloading.glassfish;version=\"3.0.6.RELEASE\",org.springframework.instrument.classloading.jboss;version=\"3.0.6.RELEASE\",org.springframework.instrument.classloading.oc4j;version=\"3.0.6.RELEASE\",org.springframework.instrument.classloading.weblogic;version=\"3.0.6.RELEASE\",org.springframework.instrument.classloading;version=\"3.0.6.RELEASE\",org.springframework.jca.cci.connection;version=\"3.0.6.RELEASE\",org.springframework.jca.cci.core.support;version=\"3.0.6.RELEASE\",org.springframework.jca.cci.core;version=\"3.0.6.RELEASE\",org.springframework.jca.cci.object;version=\"3.0.6.RELEASE\",org.springframework.jca.cci;version=\"3.0.6.RELEASE\",org.springframework.jca.context;version=\"3.0.6.RELEASE\",org.springframework.jca.endpoint;version=\"3.0.6.RELEASE\",org.springframework.jca.support;version=\"3.0.6.RELEASE\",org.springframework.jca.work.glassfish;version=\"3.0.6.RELEASE\",org.springframework.jca.work.jboss;version=\"3.0.6.RELEASE\",org.springframework.jca.work;version=\"3.0.6.RELEASE\",org.springframework.jdbc.config;version=\"3.0.6.RELEASE\",org.springframework.jdbc.core.metadata;version=\"3.0.6.RELEASE\",org.springframework.jdbc.core.namedparam;version=\"3.0.6.RELEASE\",org.springframework.jdbc.core.simple;version=\"3.0.6.RELEASE\",org.springframework.jdbc.core.support;version=\"3.0.6.RELEASE\",org.springframework.jdbc.core;version=\"3.0.6.RELEASE\",org.springframework.jdbc.datasource.embedded;version=\"3.0.6.RELEASE\",org.springframework.jdbc.datasource.init;version=\"3.0.6.RELEASE\",org.springframework.jdbc.datasource.lookup;version=\"3.0.6.RELEASE\",org.springframework.jdbc.datasource;version=\"3.0.6.RELEASE\",org.springframework.jdbc.object;version=\"3.0.6.RELEASE\",org.springframework.jdbc.support.incrementer;version=\"3.0.6.RELEASE\",org.springframework.jdbc.support.lob;version=\"3.0.6.RELEASE\",org.springframework.jdbc.support.nativejdbc;version=\"3.0.6.RELEASE\",org.springframework.jdbc.support.rowset;version=\"3.0.6.RELEASE\",org.springframework.jdbc.support.xml;version=\"3.0.6.RELEASE\",org.springframework.jdbc.support;version=\"3.0.6.RELEASE\",org.springframework.jdbc;version=\"3.0.6.RELEASE\",org.springframework.jms.config;version=\"3.0.6.RELEASE\",org.springframework.jms.connection;version=\"3.0.6.RELEASE\",org.springframework.jms.core.support;version=\"3.0.6.RELEASE\",org.springframework.jms.core;version=\"3.0.6.RELEASE\",org.springframework.jms.listener.adapter;version=\"3.0.6.RELEASE\",org.springframework.jms.listener.endpoint;version=\"3.0.6.RELEASE\",org.springframework.jms.listener;version=\"3.0.6.RELEASE\",org.springframework.jms.remoting;version=\"3.0.6.RELEASE\",org.springframework.jms.support.converter;version=\"3.0.6.RELEASE\",org.springframework.jms.support.destination;version=\"3.0.6.RELEASE\",org.springframework.jms.support;version=\"3.0.6.RELEASE\",org.springframework.jms;version=\"3.0.6.RELEASE\",org.springframework.jmx.access;version=\"3.0.6.RELEASE\",org.springframework.jmx.export.annotation;version=\"3.0.6.RELEASE\",org.springframework.jmx.export.assembler;version=\"3.0.6.RELEASE\",org.springframework.jmx.export.metadata;version=\"3.0.6.RELEASE\",org.springframework.jmx.export.naming;version=\"3.0.6.RELEASE\",org.springframework.jmx.export.notification;version=\"3.0.6.RELEASE\",org.springframework.jmx.export;version=\"3.0.6.RELEASE\",org.springframework.jmx.support;version=\"3.0.6.RELEASE\",org.springframework.jmx;version=\"3.0.6.RELEASE\",org.springframework.jndi.support;version=\"3.0.6.RELEASE\",org.springframework.jndi;version=\"3.0.6.RELEASE\",org.springframework.mail.javamail;version=\"3.0.6.RELEASE\",org.springframework.mail;version=\"3.0.6.RELEASE\",org.springframework.mock.staticmock;version=\"3.0.6.RELEASE\",org.springframework.orm.hibernate3.annotation;version=\"3.0.6.RELEASE\",org.springframework.orm.hibernate3.support;version=\"3.0.6.RELEASE\",org.springframework.orm.hibernate3;version=\"3.0.6.RELEASE\",org.springframework.orm.ibatis.support;version=\"3.0.6.RELEASE\",org.springframework.orm.ibatis;version=\"3.0.6.RELEASE\",org.springframework.orm.jdo.support;version=\"3.0.6.RELEASE\",org.springframework.orm.jdo;version=\"3.0.6.RELEASE\",org.springframework.orm.jpa.aspectj;version=\"3.0.6.RELEASE\",org.springframework.orm.jpa.persistenceunit;version=\"3.0.6.RELEASE\",org.springframework.orm.jpa.support;version=\"3.0.6.RELEASE\",org.springframework.orm.jpa.vendor;version=\"3.0.6.RELEASE\",org.springframework.orm.jpa;version=\"3.0.6.RELEASE\",org.springframework.orm;version=\"3.0.6.RELEASE\",org.springframework.oxm.castor;version=\"3.0.6.RELEASE\",org.springframework.oxm.config;version=\"3.0.6.RELEASE\",org.springframework.oxm.jaxb;version=\"3.0.6.RELEASE\",org.springframework.oxm.jibx;version=\"3.0.6.RELEASE\",org.springframework.oxm.mime;version=\"3.0.6.RELEASE\",org.springframework.oxm.support;version=\"3.0.6.RELEASE\",org.springframework.oxm.xmlbeans;version=\"3.0.6.RELEASE\",org.springframework.oxm.xstream;version=\"3.0.6.RELEASE\",org.springframework.oxm;version=\"3.0.6.RELEASE\",org.springframework.remoting.caucho;version=\"3.0.6.RELEASE\",org.springframework.remoting.httpinvoker;version=\"3.0.6.RELEASE\",org.springframework.remoting.jaxrpc;version=\"3.0.6.RELEASE\",org.springframework.remoting.jaxws;version=\"3.0.6.RELEASE\",org.springframework.remoting.rmi;version=\"3.0.6.RELEASE\",org.springframework.remoting.soap;version=\"3.0.6.RELEASE\",org.springframework.remoting.support;version=\"3.0.6.RELEASE\",org.springframework.remoting;version=\"3.0.6.RELEASE\",org.springframework.scheduling.annotation;version=\"3.0.6.RELEASE\",org.springframework.scheduling.aspectj;version=\"3.0.6.RELEASE\",org.springframework.scheduling.backportconcurrent;version=\"3.0.6.RELEASE\",org.springframework.scheduling.commonj;version=\"3.0.6.RELEASE\",org.springframework.scheduling.concurrent;version=\"3.0.6.RELEASE\",org.springframework.scheduling.config;version=\"3.0.6.RELEASE\",org.springframework.scheduling.quartz;version=\"3.0.6.RELEASE\",org.springframework.scheduling.support;version=\"3.0.6.RELEASE\",org.springframework.scheduling.timer;version=\"3.0.6.RELEASE\",org.springframework.scheduling;version=\"3.0.6.RELEASE\",org.springframework.scripting.bsh;version=\"3.0.6.RELEASE\",org.springframework.scripting.config;version=\"3.0.6.RELEASE\",org.springframework.scripting.groovy;version=\"3.0.6.RELEASE\",org.springframework.scripting.jruby;version=\"3.0.6.RELEASE\",org.springframework.scripting.support;version=\"3.0.6.RELEASE\",org.springframework.scripting;version=\"3.0.6.RELEASE\",org.springframework.stereotype;version=\"3.0.6.RELEASE\",org.springframework.transaction.annotation;version=\"3.0.6.RELEASE\",org.springframework.transaction.aspectj;version=\"3.0.6.RELEASE\",org.springframework.transaction.config;version=\"3.0.6.RELEASE\",org.springframework.transaction.interceptor;version=\"3.0.6.RELEASE\",org.springframework.transaction.jta;version=\"3.0.6.RELEASE\",org.springframework.transaction.support;version=\"3.0.6.RELEASE\",org.springframework.transaction;version=\"3.0.6.RELEASE\",org.springframework.ui.context.support;version=\"3.0.6.RELEASE\",org.springframework.ui.context;version=\"3.0.6.RELEASE\",org.springframework.ui.freemarker;version=\"3.0.6.RELEASE\",org.springframework.ui.jasperreports;version=\"3.0.6.RELEASE\",org.springframework.ui.velocity;version=\"3.0.6.RELEASE\",org.springframework.ui;version=\"3.0.6.RELEASE\",org.springframework.util.comparator;version=\"3.0.6.RELEASE\",org.springframework.util.xml;version=\"3.0.6.RELEASE\",org.springframework.util;version=\"3.0.6.RELEASE\",org.springframework.validation.beanvalidation;version=\"3.0.6.RELEASE\",org.springframework.validation.support;version=\"3.0.6.RELEASE\",org.springframework.validation;version=\"3.0.6.RELEASE\",org.springframework.web.bind.annotation.support;version=\"3.0.6.RELEASE\",org.springframework.web.bind.annotation;version=\"3.0.6.RELEASE\",org.springframework.web.bind.support;version=\"3.0.6.RELEASE\",org.springframework.web.bind;version=\"3.0.6.RELEASE\",org.springframework.web.client.support;version=\"3.0.6.RELEASE\",org.springframework.web.client;version=\"3.0.6.RELEASE\",org.springframework.web.context.request;version=\"3.0.6.RELEASE\",org.springframework.web.context.support;version=\"3.0.6.RELEASE\",org.springframework.web.context;version=\"3.0.6.RELEASE\",org.springframework.web.filter;version=\"3.0.6.RELEASE\",org.springframework.web.jsf.el;version=\"3.0.6.RELEASE\",org.springframework.web.jsf;version=\"3.0.6.RELEASE\",org.springframework.web.multipart.commons;version=\"3.0.6.RELEASE\",org.springframework.web.multipart.support;version=\"3.0.6.RELEASE\",org.springframework.web.multipart;version=\"3.0.6.RELEASE\",org.springframework.web.portlet.bind.annotation;version=\"3.0.6.RELEASE\",org.springframework.web.portlet.bind;version=\"3.0.6.RELEASE\",org.springframework.web.portlet.context;version=\"3.0.6.RELEASE\",org.springframework.web.portlet.handler;version=\"3.0.6.RELEASE\",org.springframework.web.portlet.multipart;version=\"3.0.6.RELEASE\",org.springframework.web.portlet.mvc.annotation;version=\"3.0.6.RELEASE\",org.springframework.web.portlet.mvc;version=\"3.0.6.RELEASE\",org.springframework.web.portlet.util;version=\"3.0.6.RELEASE\",org.springframework.web.portlet;version=\"3.0.6.RELEASE\",org.springframework.web.servlet.config;version=\"3.0.6.RELEASE\",org.springframework.web.servlet.handler;version=\"3.0.6.RELEASE\",org.springframework.web.servlet.i18n;version=\"3.0.6.RELEASE\",org.springframework.web.servlet.mvc.annotation;version=\"3.0.6.RELEASE\",org.springframework.web.servlet.mvc.multiaction;version=\"3.0.6.RELEASE\",org.springframework.web.servlet.mvc.support;version=\"3.0.6.RELEASE\",org.springframework.web.servlet.mvc;version=\"3.0.6.RELEASE\",org.springframework.web.servlet.resource;version=\"3.0.6.RELEASE\",org.springframework.web.servlet.support;version=\"3.0.6.RELEASE\",org.springframework.web.servlet.tags.form;version=\"3.0.6.RELEASE\",org.springframework.web.servlet.tags;version=\"3.0.6.RELEASE\",org.springframework.web.servlet.theme;version=\"3.0.6.RELEASE\",org.springframework.web.servlet.view.document;version=\"3.0.6.RELEASE\",org.springframework.web.servlet.view.feed;version=\"3.0.6.RELEASE\",org.springframework.web.servlet.view.freemarker;version=\"3.0.6.RELEASE\",org.springframework.web.servlet.view.jasperreports;version=\"3.0.6.RELEASE\",org.springframework.web.servlet.view.json;version=\"3.0.6.RELEASE\",org.springframework.web.servlet.view.tiles2;version=\"3.0.6.RELEASE\",org.springframework.web.servlet.view.tiles;version=\"3.0.6.RELEASE\",org.springframework.web.servlet.view.velocity;version=\"3.0.6.RELEASE\",org.springframework.web.servlet.view.xml;version=\"3.0.6.RELEASE\",org.springframework.web.servlet.view.xslt;version=\"3.0.6.RELEASE\",org.springframework.web.servlet.view;version=\"3.0.6.RELEASE\",org.springframework.web.servlet;version=\"3.0.6.RELEASE\",org.springframework.web.struts;version=\"3.0.6.RELEASE\",org.springframework.web.util;version=\"3.0.6.RELEASE\",org.springframework.web;version=\"3.0.6.RELEASE\"";
		Dictionary[] props = new Dictionary[] {new Hashtable()};
		props[0].put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, longExport);
		state.setPlatformProperties(props);

		File stateCache = OSGiTestsActivator.getContext().getDataFile("statecache"); //$NON-NLS-1$
		stateCache.mkdirs();
		StateObjectFactory.defaultFactory.writeState(state, stateCache);
		state = StateObjectFactory.defaultFactory.readState(stateCache);
	}
}
//testFragmentUpdateNoVersionChanged()
//testFragmentUpdateVersionChanged()
//testHostUpdateNoVersionChanged()
//testHostUpdateVersionChanged()
