/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
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
import java.util.Dictionary;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.osgi.framework.util.Headers;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public class OSGiCapabilityTest extends AbstractStateTest {
	private static final String MANIFEST_ROOT = "test_files/genericCapability/";

	public static Test suite() {
		return new TestSuite(OSGiCapabilityTest.class);
	}

	public OSGiCapabilityTest(String name) {
		super(name);
	}

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

	public void testGenericsOSGiOSGi() throws BundleException {
		doGenericBasicsTest("p1.osgi.MF", "p2.osgi.MF", "p3.osgi.MF", "c1.osgi.MF", "c2.osgi.MF", "c3.osgi.MF");
	}

	public void testGenericsOSGiEquinox() throws BundleException {
		doGenericBasicsTest("p1.osgi.MF", "p2.osgi.MF", "p3.osgi.MF", "c1.equinox.MF", "c2.equinox.MF", "c3.equinox.MF");
	}

	public void testGenericsOSGiNameEquinox() throws BundleException {
		doGenericBasicsTest("p1.osgi.name.MF", "p2.osgi.name.MF", "p3.osgi.name.MF", "c1.equinox.MF", "c2.equinox.MF", "c3.equinox.MF");
	}

	public void testGenericsOSGiNameOSGi() throws BundleException {
		doGenericBasicsTest("p1.osgi.name.MF", "p2.osgi.name.MF", "p3.osgi.name.MF", "c1.osgi.MF", "c2.osgi.MF", "c3.osgi.MF");
	}

	public void testGenericsOSGiNameEquinoxName() throws BundleException {
		doGenericBasicsTest("p1.osgi.name.MF", "p2.osgi.name.MF", "p3.osgi.name.MF", "c1.equinox.name.MF", "c2.equinox.name.MF", "c3.equinox.name.MF");
	}

	public void testGenericsEquinoxOSGi() throws BundleException {
		doGenericBasicsTest("p1.equinox.MF", "p2.equinox.MF", "p3.equinox.MF", "c1.osgi.MF", "c2.osgi.MF", "c3.osgi.MF");
	}

	public void testGenericsEquinoxEquinox() throws BundleException {
		doGenericBasicsTest("p1.equinox.MF", "p2.equinox.MF", "p3.equinox.MF", "c1.equinox.MF", "c2.equinox.MF", "c3.equinox.MF");
	}

	private void doGenericBasicsTest(String p1Manifest, String p2Manifest, String p3Manifest, String c1Manifest, String c2Manifest, String c3Manifest) throws BundleException {
		State state = buildEmptyState();
		long bundleID = 0;
		Dictionary manifest;

		manifest = loadManifest(p1Manifest);
		BundleDescription p1 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);
		manifest = loadManifest(p2Manifest);
		BundleDescription p2 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);
		manifest = loadManifest(p3Manifest);
		BundleDescription p3 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);
		manifest = loadManifest(c1Manifest);
		BundleDescription c1 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);
		manifest = loadManifest(c2Manifest);
		BundleDescription c2 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);
		manifest = loadManifest(c3Manifest);
		BundleDescription c3 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		state.addBundle(p3);
		state.addBundle(p2);
		state.addBundle(p1);
		state.addBundle(c1);
		state.addBundle(c2);
		state.addBundle(c3);

		state.resolve();
		assertTrue("p1", p1.isResolved());
		assertTrue("p2", p2.isResolved());
		assertTrue("p3", p3.isResolved());
		assertTrue("c1", c1.isResolved());
		assertTrue("c2", c2.isResolved());
		assertTrue("c3", c3.isResolved());

		checkGenericBasics(3, c1.getResolvedGenericRequires(), p1.getSelectedGenericCapabilities());
		checkGenericBasics(3, c2.getResolvedGenericRequires(), p2.getSelectedGenericCapabilities());
		checkGenericBasics(3, c3.getResolvedGenericRequires(), p3.getSelectedGenericCapabilities());

		File stateDir = getContext().getDataFile(getName()); //$NON-NLS-1$
		stateDir.mkdirs();
		try {
			state.getFactory().writeState(state, stateDir);
			state = state.getFactory().readState(stateDir);
		} catch (IOException e) {
			fail("Error writing/reading state.", e);
		}
		p1 = state.getBundle(p1.getBundleId());
		p2 = state.getBundle(p2.getBundleId());
		p3 = state.getBundle(p3.getBundleId());
		c1 = state.getBundle(c1.getBundleId());
		c2 = state.getBundle(c2.getBundleId());
		c3 = state.getBundle(c3.getBundleId());
		assertTrue("p1", p1.isResolved());
		assertTrue("p2", p2.isResolved());
		assertTrue("p3", p3.isResolved());
		assertTrue("c1", c1.isResolved());
		assertTrue("c2", c2.isResolved());
		assertTrue("c3", c3.isResolved());

		checkGenericBasics(3, c1.getResolvedGenericRequires(), p1.getSelectedGenericCapabilities());
		checkGenericBasics(3, c2.getResolvedGenericRequires(), p2.getSelectedGenericCapabilities());
		checkGenericBasics(3, c3.getResolvedGenericRequires(), p3.getSelectedGenericCapabilities());
	}

	private void checkGenericBasics(int expectedCnt, GenericDescription[] genRequired, GenericDescription[] genProvided) {
		assertEquals("Expected number of capabilities do not match", expectedCnt, genRequired.length);
		assertEquals("Specs do not match Descs", genRequired.length, genProvided.length);
		for (int i = 0; i < genRequired.length; i++) {
			assertEquals("Wrong provider for requirement.", genProvided[i], genRequired[i]);
		}
	}

	public void testGenericFragments01() throws BundleException {
		State state = buildEmptyState();
		long bundleID = 0;
		Dictionary manifest;

		manifest = loadManifest("p1.osgi.MF");
		BundleDescription p1 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);
		manifest = loadManifest("p1.osgi.frag.MF");
		BundleDescription p1Frag = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);
		manifest = loadManifest("c1.osgi.MF");
		BundleDescription c1 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);
		manifest = loadManifest("c1.osgi.frag.MF");
		BundleDescription c1Frag = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);
		manifest = loadManifest("p4.osgi.MF");
		BundleDescription p4 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		state.addBundle(p1);
		state.addBundle(p1Frag);
		state.addBundle(c1);
		state.addBundle(c1Frag);
		state.addBundle(p4);

		state.resolve();
		assertTrue("p1", p1.isResolved());
		assertTrue("p1Frag", p1Frag.isResolved());
		assertTrue("c1", c1.isResolved());
		assertTrue("c1Frag", c1Frag.isResolved());
		assertTrue("p4", p4.isResolved());

		checkGenericBasics(4, c1.getResolvedGenericRequires(), p1.getSelectedGenericCapabilities());

		File stateDir = getContext().getDataFile(getName()); //$NON-NLS-1$
		stateDir.mkdirs();
		try {
			state.getFactory().writeState(state, stateDir);
			state = state.getFactory().readState(stateDir);
		} catch (IOException e) {
			fail("Error writing/reading state.", e);
		}
		p1 = state.getBundle(p1.getBundleId());
		p1Frag = state.getBundle(p1Frag.getBundleId());
		c1 = state.getBundle(c1.getBundleId());
		c1Frag = state.getBundle(c1Frag.getBundleId());
		p4 = state.getBundle(p4.getBundleId());

		assertTrue("p1", p1.isResolved());
		assertTrue("p1Frag", p1Frag.isResolved());
		assertTrue("c1", c1.isResolved());
		assertTrue("c1Frag", c1Frag.isResolved());
		assertTrue("p4", p4.isResolved());

		checkGenericBasics(4, c1.getResolvedGenericRequires(), p1.getSelectedGenericCapabilities());

		state.setResolver(platformAdmin.createResolver());
		state.resolve(new BundleDescription[] {p1});

		assertTrue("p1", p1.isResolved());
		assertTrue("p1Frag", p1Frag.isResolved());
		assertTrue("c1", c1.isResolved());
		assertTrue("c1Frag", c1Frag.isResolved());
		assertTrue("p4", p4.isResolved());

		checkGenericBasics(4, c1.getResolvedGenericRequires(), p1.getSelectedGenericCapabilities());
	}

	public void testGenericFragments02() throws BundleException {
		State state = buildEmptyState();
		long bundleID = 0;
		Dictionary manifest;

		manifest = loadManifest("p1.osgi.MF");
		BundleDescription p1 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);
		manifest = loadManifest("p1.osgi.frag.MF");
		BundleDescription p1Frag = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);
		manifest = loadManifest("c1.osgi.MF");
		BundleDescription c1 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);
		manifest = loadManifest("c1.osgi.frag.MF");
		BundleDescription c1Frag = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		state.addBundle(p1);
		state.addBundle(p1Frag);
		state.addBundle(c1);
		state.addBundle(c1Frag);

		state.resolve();
		assertTrue("p1", p1.isResolved());
		assertFalse("p1Frag", p1Frag.isResolved());
		assertTrue("c1", c1.isResolved());
		assertFalse("c1Frag", c1Frag.isResolved());

		checkGenericBasics(3, c1.getResolvedGenericRequires(), p1.getSelectedGenericCapabilities());

		File stateDir = getContext().getDataFile(getName() + 1); //$NON-NLS-1$
		stateDir.mkdirs();
		try {
			state.getFactory().writeState(state, stateDir);
			state = state.getFactory().readState(stateDir);
			state.setResolver(platformAdmin.createResolver());
		} catch (IOException e) {
			fail("Error writing/reading state.", e);
		}
		p1 = state.getBundle(p1.getBundleId());
		p1Frag = state.getBundle(p1Frag.getBundleId());
		c1 = state.getBundle(c1.getBundleId());
		c1Frag = state.getBundle(c1Frag.getBundleId());
		assertTrue("p1", p1.isResolved());
		assertFalse("p1Frag", p1Frag.isResolved());
		assertTrue("c1", c1.isResolved());
		assertFalse("c1Frag", c1Frag.isResolved());

		checkGenericBasics(3, c1.getResolvedGenericRequires(), p1.getSelectedGenericCapabilities());

		manifest = loadManifest("p4.osgi.MF");
		BundleDescription p4 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);
		state.addBundle(p4);

		// have to force host to re-resolve because we are adding new constraints
		state.resolve(new BundleDescription[] {p1});
		assertTrue("p1", p1.isResolved());
		assertTrue("p1Frag", p1Frag.isResolved());
		assertTrue("c1", c1.isResolved());
		assertTrue("c1Frag", c1Frag.isResolved());
		assertTrue("p4", p4.isResolved());

		checkGenericBasics(4, c1.getResolvedGenericRequires(), p1.getSelectedGenericCapabilities());

		stateDir = getContext().getDataFile(getName() + 2); //$NON-NLS-1$
		stateDir.mkdirs();
		try {
			state.getFactory().writeState(state, stateDir);
			state = state.getFactory().readState(stateDir);
		} catch (IOException e) {
			fail("Error writing/reading state.", e);
		}
		p1 = state.getBundle(p1.getBundleId());
		p1Frag = state.getBundle(p1Frag.getBundleId());
		c1 = state.getBundle(c1.getBundleId());
		c1Frag = state.getBundle(c1Frag.getBundleId());
		p4 = state.getBundle(p4.getBundleId());
		assertTrue("p1", p1.isResolved());
		assertTrue("p1Frag", p1Frag.isResolved());
		assertTrue("c1", c1.isResolved());
		assertTrue("c1Frag", c1Frag.isResolved());
		assertTrue("p4", p4.isResolved());

		checkGenericBasics(4, c1.getResolvedGenericRequires(), p1.getSelectedGenericCapabilities());
	}
}
