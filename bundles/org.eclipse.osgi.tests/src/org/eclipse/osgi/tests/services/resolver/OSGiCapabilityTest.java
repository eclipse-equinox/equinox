/*******************************************************************************
 * Copyright (c) 2010, 2017 IBM Corporation and others.
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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.osgi.framework.util.CaseInsensitiveDictionaryMap;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

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
			CaseInsensitiveDictionaryMap<String, String> headers = new CaseInsensitiveDictionaryMap<String, String>();
			ManifestElement.parseBundleManifest(url.openStream(), headers);
			return headers.asUnmodifiableDictionary();
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

		checkGenericBasics(4, c1.getResolvedGenericRequires(), p1.getSelectedGenericCapabilities());
		checkGenericBasics(4, c2.getResolvedGenericRequires(), p2.getSelectedGenericCapabilities());
		checkGenericBasics(4, c3.getResolvedGenericRequires(), p3.getSelectedGenericCapabilities());

		if (p1Manifest.indexOf(".osgi.") != -1) {
			checkForNonEffectiveCapability(p1);
			checkForNonEffectiveCapability(p2);
			checkForNonEffectiveCapability(p3);
		}
		if (c1Manifest.indexOf(".osgi.") != -1) {
			checkForNonEffectiveRequirement(c1);
			checkForNonEffectiveRequirement(c2);
			checkForNonEffectiveRequirement(c3);
		}

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

		checkGenericBasics(4, c1.getResolvedGenericRequires(), p1.getSelectedGenericCapabilities());
		checkGenericBasics(4, c2.getResolvedGenericRequires(), p2.getSelectedGenericCapabilities());
		checkGenericBasics(4, c3.getResolvedGenericRequires(), p3.getSelectedGenericCapabilities());

		if (p1Manifest.indexOf(".osgi.") != -1) {
			checkForNonEffectiveCapability(p1);
			checkForNonEffectiveCapability(p2);
			checkForNonEffectiveCapability(p3);
		}
		if (c1Manifest.indexOf(".osgi.") != -1) {
			checkForNonEffectiveRequirement(c1);
			checkForNonEffectiveRequirement(c2);
			checkForNonEffectiveRequirement(c3);
		}
	}

	private static final String notEffective = "not.effective";

	private void checkForNonEffectiveCapability(BundleDescription p1) {
		List nonEffectiveCaps = p1.getCapabilities(notEffective);
		assertNotNull(nonEffectiveCaps);
		assertEquals("Wrong number of not.effective", 1, nonEffectiveCaps.size());
		Capability c = (Capability) nonEffectiveCaps.get(0);
		assertEquals("Wrong effective value", Constants.EFFECTIVE_ACTIVE, c.getDirectives().get(Constants.EFFECTIVE_DIRECTIVE));
	}

	private void checkForNonEffectiveRequirement(BundleDescription c1) {
		List nonEffectiveReqs = c1.getRequirements(notEffective);
		assertNotNull(nonEffectiveReqs);
		assertEquals("Wrong number of not.effective", 1, nonEffectiveReqs.size());
		Requirement r = (Requirement) nonEffectiveReqs.get(0);
		assertEquals("Wrong effective value", Constants.EFFECTIVE_ACTIVE, r.getDirectives().get(Constants.EFFECTIVE_DIRECTIVE));
	}

	private void checkGenericBasics(int expectedCnt, GenericDescription[] genRequired, GenericDescription[] genProvided) {
		checkGenericBasics(expectedCnt, genRequired, genProvided, null);
	}

	private void checkGenericBasics(int expectedCnt, GenericDescription[] genRequired, GenericDescription[] genProvided, GenericDescription fragIdentity) {
		assertEquals("Expected number of capabilities do not match", expectedCnt, genRequired.length);
		assertEquals("Specs do not match Descs", genRequired.length, genProvided.length + (fragIdentity == null ? 0 : 1));
		Collection providedCollection = new ArrayList(Arrays.asList(genProvided));
		for (int i = 0; i < genRequired.length; i++) {
			if (IdentityNamespace.IDENTITY_NAMESPACE.equals(genRequired[i].getType()) && genRequired[i].getSupplier().getHost() != null)
				assertEquals("Wrong fragment provider: " + genRequired[i], fragIdentity, genRequired[i]);
			else
				assertTrue("Wrong provider for requirement: " + genRequired[i], providedCollection.remove(genRequired[i]));
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

		checkGenericBasics(6, c1.getResolvedGenericRequires(), p1.getSelectedGenericCapabilities(), p1Frag.getSelectedGenericCapabilities()[0]);

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

		checkGenericBasics(6, c1.getResolvedGenericRequires(), p1.getSelectedGenericCapabilities(), p1Frag.getSelectedGenericCapabilities()[0]);

		state.setResolver(platformAdminService.createResolver());
		state.resolve(new BundleDescription[] {p1});

		assertTrue("p1", p1.isResolved());
		assertTrue("p1Frag", p1Frag.isResolved());
		assertTrue("c1", c1.isResolved());
		assertTrue("c1Frag", c1Frag.isResolved());
		assertTrue("p4", p4.isResolved());

		checkGenericBasics(6, c1.getResolvedGenericRequires(), p1.getSelectedGenericCapabilities(), p1Frag.getSelectedGenericCapabilities()[0]);
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

		checkGenericBasics(4, c1.getResolvedGenericRequires(), p1.getSelectedGenericCapabilities());

		File stateDir = getContext().getDataFile(getName() + 1); //$NON-NLS-1$
		stateDir.mkdirs();
		try {
			state.getFactory().writeState(state, stateDir);
			state = state.getFactory().readState(stateDir);
			state.setResolver(platformAdminService.createResolver());
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

		checkGenericBasics(4, c1.getResolvedGenericRequires(), p1.getSelectedGenericCapabilities());

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

		checkGenericBasics(6, c1.getResolvedGenericRequires(), p1.getSelectedGenericCapabilities(), p1Frag.getSelectedGenericCapabilities()[0]);

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

		checkGenericBasics(6, c1.getResolvedGenericRequires(), p1.getSelectedGenericCapabilities(), p1Frag.getSelectedGenericCapabilities()[0]);
	}

	public void testGenericUses() throws BundleException {
		State state = buildEmptyState();
		long bundleID = 0;
		Dictionary manifest;

		manifest = loadManifest("p5.v100.osgi.MF");
		BundleDescription p5v100 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);
		manifest = loadManifest("p5.v110.osgi.MF");
		BundleDescription p5v110 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);
		manifest = loadManifest("p6.v100.osgi.MF");
		BundleDescription p6v100 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);
		manifest = loadManifest("p6.v110.osgi.MF");
		BundleDescription p6v110 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);
		manifest = loadManifest("p7.v100.osgi.MF");
		BundleDescription p7v100 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);
		manifest = loadManifest("p7.v110.osgi.MF");
		BundleDescription p7v110 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);
		manifest = loadManifest("c4.v100.osgi.MF");
		BundleDescription c4v100 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);
		manifest = loadManifest("c4.v110.osgi.MF");
		BundleDescription c4v110 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);
		manifest = loadManifest("c4.v120.osgi.MF");
		BundleDescription c4v120 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);
		manifest = loadManifest("c4.v130.osgi.MF");
		BundleDescription c4v130 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		state.addBundle(p5v100);
		state.addBundle(p5v110);
		state.addBundle(p6v100);
		state.addBundle(p6v110);
		state.addBundle(p7v100);
		state.addBundle(p7v110);
		state.addBundle(c4v100);
		state.addBundle(c4v110);
		state.addBundle(c4v120);
		state.addBundle(c4v130);

		state.resolve();

		assertTrue("p5v100", p5v100.isResolved());
		assertTrue("p5v110", p5v110.isResolved());
		assertTrue("p6v100", p6v100.isResolved());
		assertTrue("p6v110", p6v110.isResolved());
		assertTrue("p7v100", p7v100.isResolved());
		assertTrue("p7v110", p7v110.isResolved());
		assertTrue("c4v100", c4v100.isResolved());
		assertTrue("c4v110", c4v110.isResolved());
		assertTrue("c4v120", c4v120.isResolved());
		assertTrue("c4v130", c4v130.isResolved());

		state.linkDynamicImport(c4v120, "p6");
		state.linkDynamicImport(c4v120, "p7");

		GenericDescription[] p5v100Capability = p5v100.getSelectedGenericCapabilities();
		ExportPackageDescription[] p6v100Exports = p6v100.getSelectedExports();
		ExportPackageDescription[] p7v100Exports = p7v100.getSelectedExports();
		ExportPackageDescription[] expectedPackages = new ExportPackageDescription[] {p6v100Exports[0], p7v100Exports[0]};

		checkUsedImports(c4v100, expectedPackages);
		checkUsedImports(c4v110, expectedPackages);
		checkUsedImports(c4v120, expectedPackages);

		BundleDescription[] expectedRequired = new BundleDescription[] {p6v100, p7v100};
		checkUsedRequires(c4v130, expectedRequired);

		checkUsedCapability(c4v100, p5v100Capability);
		checkUsedCapability(c4v110, p5v100Capability);
		checkUsedCapability(c4v120, p5v100Capability);
		checkUsedCapability(c4v130, p5v100Capability);
	}

	public void testOSGiCardinalityUses() throws BundleException {
		State state = buildEmptyState();
		long bundleID = 0;
		Dictionary manifest;

		manifest = loadManifest("p5.v100.osgi.MF");
		BundleDescription p5v100 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);
		manifest = loadManifest("p5.v101.osgi.MF");
		BundleDescription p5v101 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);
		manifest = loadManifest("p5.v110.osgi.MF");
		BundleDescription p5v110 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);
		manifest = loadManifest("p6.v100.osgi.MF");
		BundleDescription p6v100 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);
		manifest = loadManifest("p6.v110.osgi.MF");
		BundleDescription p6v110 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);
		manifest = loadManifest("p7.v100.osgi.MF");
		BundleDescription p7v100 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);
		manifest = loadManifest("p7.v110.osgi.MF");
		BundleDescription p7v110 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);
		manifest = loadManifest("c6.v100.osgi.MF");
		BundleDescription c6v100 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);
		manifest = loadManifest("c6.v110.osgi.MF");
		BundleDescription c6v110 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);
		manifest = loadManifest("c6.v120.osgi.MF");
		BundleDescription c6v120 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);
		manifest = loadManifest("c6.v130.osgi.MF");
		BundleDescription c6v130 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);
		manifest = loadManifest("c6.v140.osgi.MF");
		BundleDescription c6v140 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		state.addBundle(p5v100);
		state.addBundle(p5v101);
		state.addBundle(p5v110);
		state.addBundle(p6v100);
		state.addBundle(p6v110);
		state.addBundle(p7v100);
		state.addBundle(p7v110);
		state.addBundle(c6v100);
		state.addBundle(c6v110);
		state.addBundle(c6v120);
		state.addBundle(c6v130);
		state.addBundle(c6v140);

		state.resolve();

		assertTrue("p5v100", p5v100.isResolved());
		assertTrue("p5v100", p5v101.isResolved());
		assertTrue("p5v110", p5v110.isResolved());
		assertTrue("p6v100", p6v100.isResolved());
		assertTrue("p6v110", p6v110.isResolved());
		assertTrue("p7v100", p7v100.isResolved());
		assertTrue("p7v110", p7v110.isResolved());
		assertTrue("c6v100", c6v100.isResolved());
		assertTrue("c6v110", c6v110.isResolved());
		assertTrue("c6v120", c6v120.isResolved());
		assertTrue("c6v130", c6v130.isResolved());
		assertTrue("c6v140", c6v140.isResolved());

		state.linkDynamicImport(c6v120, "p6");
		state.linkDynamicImport(c6v120, "p7");

		GenericDescription[] p5v100Capability = p5v100.getSelectedGenericCapabilities();
		GenericDescription[] p5v101Capability = p5v101.getSelectedGenericCapabilities();
		List expectedCapabilityList = new ArrayList();
		expectedCapabilityList.addAll(Arrays.asList(p5v100Capability));
		expectedCapabilityList.addAll(Arrays.asList(p5v101Capability));
		for (Iterator iCapabilities = expectedCapabilityList.iterator(); iCapabilities.hasNext();) {
			if (!"namespace.5".equals(((GenericDescription) iCapabilities.next()).getType())) {
				iCapabilities.remove();
			}
		}

		ExportPackageDescription[] p6v100Exports = p6v100.getSelectedExports();
		ExportPackageDescription[] p7v100Exports = p7v100.getSelectedExports();
		ExportPackageDescription[] expectedPackages = new ExportPackageDescription[] {p6v100Exports[0], p7v100Exports[0]};

		checkUsedImports(c6v100, expectedPackages);
		checkUsedImports(c6v110, expectedPackages);
		checkUsedImports(c6v120, expectedPackages);

		BundleDescription[] expectedRequired = new BundleDescription[] {p6v100, p7v100};
		checkUsedRequires(c6v130, expectedRequired);
		checkUsedRequires(c6v140, expectedRequired);

		GenericDescription[] expectedCapabilities = (GenericDescription[]) expectedCapabilityList.toArray(new GenericDescription[expectedCapabilityList.size()]);
		checkUsedCapability(c6v100, expectedCapabilities);
		checkUsedCapability(c6v110, expectedCapabilities);
		checkUsedCapability(c6v120, expectedCapabilities);
		checkUsedCapability(c6v130, expectedCapabilities);
		checkUsedCapability(c6v140, expectedCapabilities);

		manifest = loadManifest("c6.v150.osgi.MF");
		BundleDescription c6v150 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);
		manifest = loadManifest("c6.v160.osgi.MF");
		BundleDescription c6v160 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		state.addBundle(c6v150);
		state.addBundle(c6v160);
		state.resolve();

		assertFalse("c6v150", c6v150.isResolved());
		assertFalse("c6v160", c6v160.isResolved());
	}

	public void testDeclaringIdentityCapability() {
		State state = buildEmptyState();
		Hashtable manifest = new Hashtable();
		long bundleID = 0;
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "genericCapability");
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0");
		StringBuilder capabililty = new StringBuilder();
		capabililty.append("testFailure:osgi.identity; test=failure");
		manifest.put(GenericCapabilityTest.GENERIC_CAPABILITY, capabililty.toString());

		try {
			state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);
			fail("Expected failure to create description that specifies osgi.identity capability");
		} catch (BundleException e) {
			// expected
		}

		manifest.remove(GenericCapabilityTest.GENERIC_CAPABILITY);
		manifest.put(Constants.PROVIDE_CAPABILITY, "osgi.identity; osgi.identity=testFailure; test=failure");
		try {
			state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);
			fail("Expected failure to create description that specifies osgi.identity capability");
		} catch (BundleException e) {
			// expected
		}
	}

	public void testOSGiCardinality() throws BundleException {
		State state = buildEmptyState();
		long bundleID = 0;
		Dictionary manifest;

		manifest = loadManifest("p1.osgi.MF");
		BundleDescription p1 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);
		manifest = loadManifest("p2.osgi.MF");
		BundleDescription p2 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);
		manifest = loadManifest("p3.osgi.MF");
		BundleDescription p3 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);
		manifest = loadManifest("c5.osgi.MF");
		BundleDescription c5 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		state.addBundle(p1);
		state.addBundle(p2);
		state.addBundle(p3);
		state.addBundle(c5);

		state.resolve();

		assertTrue("p1", p1.isResolved());
		assertTrue("p2", p2.isResolved());
		assertTrue("p3", p3.isResolved());
		assertTrue("c5", c5.isResolved());

		BundleWiring c5Wiring = c5.getWiring();
		List requiredWires = c5Wiring.getRequiredWires(null);
		assertEquals("Wrong number of required wires.", 3, requiredWires.size());
		List expectedCapabilities = new ArrayList();
		expectedCapabilities.addAll(p1.getCapabilities("namespace.1"));
		expectedCapabilities.addAll(p2.getCapabilities("namespace.1"));
		expectedCapabilities.addAll(p3.getCapabilities("namespace.1"));
		for (Iterator iWires = requiredWires.iterator(); iWires.hasNext();) {
			BundleWire wire = (BundleWire) iWires.next();
			expectedCapabilities.remove(wire.getCapability());
		}
		assertTrue("Unexpected capability wire: " + requiredWires, expectedCapabilities.isEmpty());
	}

	private void checkUsedImports(BundleDescription importer, ExportPackageDescription[] expectedPackages) {
		ExportPackageDescription[] imported = importer.getResolvedImports();
		assertEquals("Wrong number of imports for bundle: " + importer, expectedPackages.length, imported.length);
		for (int i = 0; i < imported.length; i++) {
			assertEquals("Wrong imported package from bundle: " + importer, expectedPackages[i], imported[i]);
		}
	}

	private void checkUsedRequires(BundleDescription requirer, BundleDescription[] expectedRequired) {
		BundleDescription[] required = requirer.getResolvedRequires();
		assertEquals("Wrong number of imports for bundle: " + requirer, expectedRequired.length, required.length);
		for (int i = 0; i < required.length; i++) {
			assertEquals("Wrong required bundle from bundle: " + requirer, expectedRequired[i], required[i]);
		}
	}

	private void checkUsedCapability(BundleDescription requirer, GenericDescription[] expectedCapabilities) {
		GenericDescription[] required = requirer.getResolvedGenericRequires();
		assertEquals("Wrong number of capabilities for bundle: " + requirer, expectedCapabilities.length, required.length);
		Collection providedCollection = new ArrayList(Arrays.asList(expectedCapabilities));
		for (int i = 0; i < required.length; i++) {
			assertTrue("Wrong provider for requirement: " + required[i], providedCollection.remove(required[i]));
		}
	}
}
