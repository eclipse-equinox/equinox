/*******************************************************************************
 * Copyright (c) 2012, 2022 IBM Corporation and others.
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
package org.eclipse.osgi.tests.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;

public class BasicTest extends AbstractResourceTest {
	private interface CapabilityProvider {
		List getCapabilities(String namespace);
	}

	static class BasicCapability implements Capability {
		private final Map attributes;
		private final Map directives;
		private final String namespace;

		public BasicCapability(String namespace) {
			this(namespace, Collections.EMPTY_MAP);
		}

		public BasicCapability(String namespace, Map attributes) {
			this(namespace, attributes, Collections.EMPTY_MAP);
		}

		public BasicCapability(String namespace, Map attributes, Map directives) {
			this.namespace = namespace;
			this.attributes = attributes;
			this.directives = directives;
		}

		@Override
		public String getNamespace() {
			return namespace;
		}

		@Override
		public Map getDirectives() {
			return directives;
		}

		@Override
		public Map getAttributes() {
			return attributes;
		}

		@Override
		public Resource getResource() {
			return null;
		}
	}

	private Bundle tb1;
	private Bundle tb2;
	private Bundle tb3;
	private Bundle tb4;
	private Bundle tf1;
	private Bundle tf2;

	@Test
	public void testRequirementMatches() throws Exception {
		Bundle tb5 = installer.installBundle("resource.tb5");
		Resource requirer = tb5.adapt(BundleRevision.class);
		Capability capability1 = createCapability1();
		List requirements = requirer.getRequirements(capability1.getNamespace());
		assertRequirements(requirements, 3);
		Requirement requirement1 = (Requirement) requirements.get(0);
		// Match requirement with just a namespace (no filter, attributes, or
		// directives).
		assertRequirementMatches(requirement1, capability1);
		Capability capability2 = createCapability2();
		// Different namespaces should not match.
		assertNotRequirementMatches(requirement1, capability2);
		Requirement requirement2 = (Requirement) requirements.get(1);
		// Make sure the expected attributes are present.
		assertAttribute(requirement2, "b", "a");
		// Match requirement with namespace and attributes. Requirement attributes have
		// no impact on matching.
		assertRequirementMatches(requirement2, capability1);
		Requirement requirement3 = (Requirement) requirements.get(2);
		assertAttribute(requirement3, "b", "a");
		assertAttribute(requirement3, "bar", "foo");
		assertFilterDirective(requirement3);
		// Match requirement with namespace, attributes, and filter (no other
		// directives).
		assertRequirementMatches(requirement3, capability1);
		Requirement requirement4 = (Requirement) requirements.get(3);
		assertAttribute(requirement4, "a", "b");
		assertFilterDirective(requirement4);
		// Filters should not match.
		assertNotRequirementMatches(requirement4, capability1);
		requirements = requirer.getRequirements(capability2.getNamespace());
		assertRequirements(requirements, 0);
		Requirement requirement5 = (Requirement) requirements.get(0);
		assertAttribute(requirement5, "bar", "foo");
		assertAttribute(requirement5, "y", "x");
		assertFilterDirective(requirement5);
		assertDirective(requirement5, "mandatory", "foo,x");
		// Mandatory directive should have no impact on generic capabilities or
		// requirements.
		assertRequirementMatches(requirement5, capability2);
	}

	@Test
	public void testIdentity() throws Exception {
		tb1 = installer.installBundle("resource.tb1");
		tb2 = installer.installBundle("resource.tb2");
		tb3 = installer.installBundle("resource.tb3");
		tb4 = installer.installBundle("resource.tb4");
		tf1 = installer.installBundle("resource.tf1");
		tf2 = installer.installBundle("resource.tf2");
		installer.resolveBundles(null);
		tb1.start();
		tb2.start();
		tb3.start();
		tb4.start();
		assertEquals("tf1 not resolved", tf1.getState(), Bundle.RESOLVED);
		assertEquals("tf2 not resolved", tf2.getState(), Bundle.RESOLVED);
		assertTb1();
		assertTb2();
		assertTb3();
		assertTb4();
		assertTf1();
		assertTf2();
	}

	/*
	 * TB1 Requirements: None Capabilities:
	 * osgi.identity;osgi.identity=resource.tb1;version=1.0.0;type=osgi.bundle
	 * osgi.wiring.host;osgi.wiring.host=resource.tb1;version=1.0.0 Wires:
	 * osgi.identity <-> TB3 osgi.wiring.host <-> TF1 TB4 <-> osgi.identity (via
	 * TF1)
	 */
	private void assertTb1() {
		// Get the revision for TB1.
		BundleRevision revision = tb1.adapt(BundleRevision.class);
		// Make sure TB1's symbolic name and version match the manifest.
		String symbolicName = revision.getSymbolicName();
		assertSymbolicName("resource.tb1", symbolicName);
		Version version = revision.getVersion();
		assertVersion("1.0.0", version);
		// Make sure TB1's type is correct.
		String type = getType(revision);
		assertType(IdentityNamespace.TYPE_BUNDLE, type);
		Map arbitraryAttrs = new HashMap();
		arbitraryAttrs.put("attr1", "a1");
		arbitraryAttrs.put("attr2", "a2");
		Map arbitraryDirs = new HashMap();
		arbitraryDirs.put("dir1", "d1");
		arbitraryDirs.put("dir2", "d2");
		// Check TB1's osgi.identity capability from the revision.
		Capability capability = getIdentityCapability(revision);
		assertIdentityCapability(capability, symbolicName, version, type, arbitraryAttrs, arbitraryDirs);
		// Check TB1's osgi.identity capability from the resource.
		Resource resource = revision;
		capability = getIdentityCapability(resource);
		assertIdentityCapability(capability, symbolicName, version, type, arbitraryAttrs, arbitraryDirs);
		// Check TB1's osgi.identity capability from the wiring.
		BundleWiring wiring = tb1.adapt(BundleWiring.class);
		capability = getIdentityCapability(wiring);
		assertIdentityCapability(capability, symbolicName, version, type, arbitraryAttrs, arbitraryDirs);
		// There should be 1 provided osgi.identity wire (TB1 -> TB3).
		List wires = wiring.getProvidedWires(IdentityNamespace.IDENTITY_NAMESPACE);
		assertWires(wires, 1);
		// Check the osgi.identity wire between TB1 and TB3.
		Wire wire = (Wire) wires.get(0);
		BundleRevision requirer = tb3.adapt(BundleRevision.class);
		Requirement requirement = getIdentityRequirement(requirer, 1);
		assertIdentityWire(wire, capability, revision, requirement, requirer);
		// There should be 1 required osgi.identity wire (TB4 -> TB1 via TF1).
		wires = wiring.getRequiredWires(IdentityNamespace.IDENTITY_NAMESPACE);
		assertWires(wires, 1);
		// Check the osgi.identity wire between TB4 and TB1 (via TF1).
		wire = (Wire) wires.get(0);
		BundleRevision provider = tb4.adapt(BundleRevision.class);
		capability = getIdentityCapability(provider);
		requirement = getIdentityRequirement(tf1.adapt(BundleRevision.class), 0);
		assertIdentityWire(wire, capability, provider, requirement, revision);
	}

	/*
	 * TB2 Requirements: None Capabilities: None Wires: None
	 */
	private void assertTb2() {
		final BundleRevision revision = tb2.adapt(BundleRevision.class);
		assertNotIdentityCapability(namespace -> revision.getDeclaredCapabilities(namespace));
		final Resource resource = revision;
		assertNotIdentityCapability(namespace -> resource.getCapabilities(namespace));
		final BundleWiring wiring = tb2.adapt(BundleWiring.class);
		assertNotIdentityCapability(namespace -> wiring.getCapabilities(namespace));
	}

	/*
	 * TB3 Requirements:
	 * osgi.identity;osgi.identity=resource.tb1;version=1.0.0;type=osgi.bundle
	 * osgi.identity;osgi.identity=resource.tf1;version=1.0.0;type=osgi.fragment
	 * Capabilities:
	 * osgi.identity;osgi.identity=resource.tb3;version=1.0.0;type=osgi.bundle
	 * osgi.wiring.host;osgi.wiring.host=resource.tb3;version=1.0.0 Wires: TB1 <->
	 * osgi.identity TF1 <-> osgi.identity
	 */
	private void assertTb3() {
		// Get the revision for TB3.
		BundleRevision revision = tb3.adapt(BundleRevision.class);
		// Make sure TB3's symbolic name and version match the manifest.
		String symbolicName = revision.getSymbolicName();
		assertSymbolicName("resource.tb3", symbolicName);
		Version version = revision.getVersion();
		assertVersion("1.0.0", version);
		// Make sure TB3's type is correct.
		String type = getType(revision);
		assertType(IdentityNamespace.TYPE_BUNDLE, type);
		// Check TB3's osgi.identity capability from the revision.
		Capability capability = getIdentityCapability(revision);
		assertIdentityCapability(capability, symbolicName, version, type, Collections.EMPTY_MAP, Collections.EMPTY_MAP);
		Resource resource = revision;
		// Check TB3's osgi.identity capability from the resource.
		capability = getIdentityCapability(resource);
		assertIdentityCapability(capability, symbolicName, version, type, Collections.EMPTY_MAP, Collections.EMPTY_MAP);
		// Check TB3's osgi.identity capability from the wiring.
		BundleWiring wiring = tb3.adapt(BundleWiring.class);
		capability = getIdentityCapability(wiring);
		assertIdentityCapability(capability, symbolicName, version, type, Collections.EMPTY_MAP, Collections.EMPTY_MAP);
		// There should be 2 required osgi.identity wires (TB1 -> TB3 and TF1 -> TB3).
		List wires = wiring.getRequiredWires(IdentityNamespace.IDENTITY_NAMESPACE);
		assertWires(wires, 2);
		// Check the osgi.identity wire between TB1 and TB3.
		Wire wire = (Wire) wires.get(1);
		Requirement requirement = getIdentityRequirement(revision, 1);
		BundleRevision provider = tb1.adapt(BundleRevision.class);
		capability = getIdentityCapability(provider);
		assertIdentityWire(wire, capability, provider, requirement, revision);
		// Check the osgi.identity wire between TF1 and TB3.
		wire = (Wire) wires.get(0);
		requirement = getIdentityRequirement(revision, 0);
		provider = tf1.adapt(BundleRevision.class);
		capability = getIdentityCapability(provider);
		assertIdentityWire(wire, capability, provider, requirement, revision);
	}

	/*
	 * TB4 Requirements: None Capabilities:
	 * osgi.identity;osgi.identity=resource.tb4;version=1.0.0;type=osgi.bundle
	 * osgi.wiring.host;osgi.wiring.host=resource.tb4;version=1.0.0 Wires:
	 * osgi.identity <-> TB1 (via TF1)
	 */
	private void assertTb4() {
		// Get the revision for TB4.
		BundleRevision revision = tb4.adapt(BundleRevision.class);
		// Make sure TB4's symbolic name and version match the manifest.
		String symbolicName = revision.getSymbolicName();
		assertSymbolicName("resource.tb4", symbolicName);
		Version version = revision.getVersion();
		assertVersion("1.0.0", version);
		// Make sure TB4's type is correct.
		String type = getType(revision);
		assertType(IdentityNamespace.TYPE_BUNDLE, type);
		// Check TB4's osgi.identity capability from the revision.
		Capability capability = getIdentityCapability(revision);
		assertIdentityCapability(capability, symbolicName, version, type, Collections.EMPTY_MAP, Collections.EMPTY_MAP);
		Resource resource = revision;
		// Check TB4's osgi.identity capability from the resource.
		capability = getIdentityCapability(resource);
		assertIdentityCapability(capability, symbolicName, version, type, Collections.EMPTY_MAP, Collections.EMPTY_MAP);
		BundleWiring wiring = tb4.adapt(BundleWiring.class);
		// Check TB4's osgi.identity capability from the wiring.
		capability = getIdentityCapability(wiring);
		assertIdentityCapability(capability, symbolicName, version, type, Collections.EMPTY_MAP, Collections.EMPTY_MAP);
		// There should be 1 provided osgi.identity wire (TB4 -> TB1 via TF1).
		List wires = wiring.getProvidedWires(IdentityNamespace.IDENTITY_NAMESPACE);
		assertWires(wires, 1);
		// Check the osgi.identity wire between TB4 and TB1 (via TF1).
		Wire wire = (Wire) wires.get(0);
		// The requirer will be TB1's revision since fragment requirements are merged
		// into the host...
		BundleRevision requirer = tb1.adapt(BundleRevision.class);
		// ...but the requirement will come from the fragment.
		Requirement requirement = getIdentityRequirement(tf1.adapt(BundleRevision.class), 0);
		assertIdentityWire(wire, capability, revision, requirement, requirer);
	}

	/*
	 * TF1 Requirements:
	 * osgi.wiring.host;osgi.wiring.host=resource.tb1;version=1.0.0
	 * osgi.identity;osgi.identity=resource.tb4;version=1.0.0;type=osgi.bundle
	 * Capabilities:
	 * osgi.identity;osgi.identity=resource.tf1;version=1.0.0;type=osgi.fragment
	 * Wires: TB1 <-> osgi.wiring.host osgi.identity <-> TB3
	 */
	private void assertTf1() {
		// Get the revision for TF1.
		BundleRevision revision = tf1.adapt(BundleRevision.class);
		// Make sure TF1's symbolic name and version match the manifest.
		String symbolicName = revision.getSymbolicName();
		assertSymbolicName("resource.tf1", symbolicName);
		Version version = revision.getVersion();
		assertVersion("1.0.0", version);
		// Make sure TF1's type is correct.
		String type = getType(revision);
		assertType(IdentityNamespace.TYPE_FRAGMENT, type);
		// Check TF1's osgi.identity capability from the revision.
		Capability capability = getIdentityCapability(revision);
		assertIdentityCapability(capability, symbolicName, version, type, Collections.EMPTY_MAP, Collections.EMPTY_MAP);
		// Check TF1's osgi.identity capability from the resource.
		Resource resource = revision;
		capability = getIdentityCapability(resource);
		assertIdentityCapability(capability, symbolicName, version, type, Collections.EMPTY_MAP, Collections.EMPTY_MAP);
		// Check TF1's osgi.identity capability from the wiring.
		BundleWiring wiring = tf1.adapt(BundleWiring.class);
		capability = getIdentityCapability(wiring);
		assertIdentityCapability(capability, symbolicName, version, type, Collections.EMPTY_MAP, Collections.EMPTY_MAP);
		// There should be 1 provided osgi.identity wire (TF1 -> TB3).
		List wires = wiring.getProvidedWires(IdentityNamespace.IDENTITY_NAMESPACE);
		assertWires(wires, 1);
		// Check the osgi.identity wire between TF1 and TB3.
		Wire wire = (Wire) wires.get(0);
		BundleRevision requirer = tb3.adapt(BundleRevision.class);
		Requirement requirement = getIdentityRequirement(requirer, 0);
		assertIdentityWire(wire, capability, revision, requirement, requirer);
	}

	/*
	 * TF2 Requirements:
	 * osgi.wiring.host;osgi.wiring.host=resource.tb1;version=1.0.0 Capabilities:
	 * osgi.identity;osgi.identity=resource.tf2;version=1.0.0;type=osgi.fragment
	 * Wires: TB1 <-> osgi.wiring.host
	 */
	private void assertTf2() {
		// Get the revision for TF2.
		BundleRevision revision = tf2.adapt(BundleRevision.class);
		// Make sure TF1's symbolic name and version match the manifest.
		String symbolicName = revision.getSymbolicName();
		assertSymbolicName("resource.tf2", symbolicName);
		Version version = revision.getVersion();
		assertVersion("0.0.0", version);
		// Make sure TF1's type is correct.
		String type = getType(revision);
		assertType(IdentityNamespace.TYPE_FRAGMENT, type);
		// Check TF1's osgi.identity capability from the revision.
		Capability capability = getIdentityCapability(revision);
		assertIdentityCapability(capability, symbolicName, version, type, Collections.EMPTY_MAP, Collections.EMPTY_MAP);
		// Check TF1's osgi.identity capability from the resource.
		Resource resource = revision;
		capability = getIdentityCapability(resource);
		assertIdentityCapability(capability, symbolicName, version, type, Collections.EMPTY_MAP, Collections.EMPTY_MAP);
		// Check TF1's osgi.identity capability from the wiring.
		BundleWiring wiring = tf2.adapt(BundleWiring.class);
		capability = getIdentityCapability(wiring);
		assertIdentityCapability(capability, symbolicName, version, type, Collections.EMPTY_MAP, Collections.EMPTY_MAP);
		// There should be 0 provided osgi.identity wire (TF1 -> TB3).
		List wires = wiring.getProvidedWires(IdentityNamespace.IDENTITY_NAMESPACE);
		assertWires(wires, 0);

	}

	private void assertAttribute(Requirement requirement, String name, Object expected) {
		assertEquals("Wrong attribute: " + name, expected, requirement.getAttributes().get(name));
	}

	private void assertCapabilities(List capabilities, int size) {
		assertNotNull("Null capabilities", capabilities);
		assertEquals("Wrong number of capabilities", size, capabilities.size());
	}

	private void assertDirective(Requirement requirement, String name, String expected) {
		assertEquals("Wrong directive", expected, requirement.getDirectives().get(name));
	}

	private void assertFilterDirective(Requirement requirement) {
		assertNotNull("Missing filter directive",
				requirement.getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE));
	}

	private void assertIdentityCapability(Capability capability, String symbolicName, Version version, String type,
			Map arbitraryAttrs, Map arbitraryDirs) {
		assertEquals("Wrong namespace", IdentityNamespace.IDENTITY_NAMESPACE, capability.getNamespace());
		assertEquals("Wrong number of attributes", 3 + arbitraryAttrs.size(), capability.getAttributes().size());
		// The osgi.identity attribute contains the symbolic name of the resource.
		assertSymbolicName(symbolicName, (String) capability.getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE));
		// The version attribute must be of type Version.
		// The version attribute contains the version of the resource.
		assertVersion(version,
				(Version) capability.getAttributes().get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE));
		// The type attribute must be of type String.
		// The type attribute contains the resource type.
		assertType(type, (String) capability.getAttributes().get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE));
		Map attributes = capability.getAttributes();
		for (Object element : arbitraryAttrs.entrySet()) {
			Map.Entry entry = (Entry) element;
			assertEquals("Wrong attribute: " + entry.getKey(), entry.getValue(), attributes.get(entry.getKey()));
		}
		Map directives = capability.getDirectives();
		for (Object element : arbitraryDirs.entrySet()) {
			Map.Entry entry = (Entry) element;
			assertEquals("Wrong directive: " + entry.getKey(), entry.getValue(), directives.get(entry.getKey()));
		}
	}

	private void assertIdentityWire(Wire wire, Capability capability, Resource provider, Requirement requirement,
			Resource requirer) {
		assertEquals("Wrong capability", capability, wire.getCapability());
		assertEquals("Wrong provider", provider, wire.getProvider());
		assertEquals("Wrong requirement", requirement, wire.getRequirement());
		assertEquals("Wrong requirer", requirer, wire.getRequirer());
		assertRequirementMatches(requirement, capability);
	}

	private void assertNotIdentityCapability(CapabilityProvider provider) {
		List capabilities = provider.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
		// A resource with no symbolic name must not provide an identity capability.
		assertCapabilities(capabilities, 0);
	}

	private void assertNotRequirementMatches(Requirement requirement, Capability capability) {
		if (!(requirement instanceof BundleRequirement) || !(capability instanceof BundleCapability))
			return;
		assertFalse("Requirement matches capability",
				((BundleRequirement) requirement).matches((BundleCapability) capability));
	}

	private void assertRequirementMatches(Requirement requirement, Capability capability) {
		if (!(requirement instanceof BundleRequirement) || !(capability instanceof BundleCapability))
			return;
		assertTrue("Requirement does not match capability",
				((BundleRequirement) requirement).matches((BundleCapability) capability));
	}

	private void assertRequirements(List requirements, int index) {
		assertNotNull("Null requirements", requirements);
		assertTrue("Wrong number of requirements", requirements.size() > index);
	}

	private void assertSymbolicName(String expected, String actual) {
		assertEquals("Wrong symbolic name", expected, actual);
	}

	private void assertType(String expected, String actual) {
		assertEquals("Wrong type", expected, actual);
	}

	private void assertVersion(String expected, Version actual) {
		assertVersion(Version.parseVersion(expected), actual);
	}

	private void assertVersion(Version expected, Version actual) {
		assertEquals("Wrong version", expected, actual);
	}

	private void assertWires(List wires, int size) {
		assertNotNull("Null wires", wires);
		assertEquals("Wrong number of wires", size, wires.size());
	}

	private Capability createCapability1() {
		String namespace = "capability.1";
		Map attributes = new HashMap();
		attributes.put("a", "b");
		return new BasicCapability(namespace, attributes);
	}

	private Capability createCapability2() {
		String namespace = "capability.2";
		Map attributes = new HashMap();
		attributes.put("foo", "bar");
		attributes.put("x", "y");
		Map directives = new HashMap();
		directives.put(Namespace.RESOLUTION_MANDATORY, "foo,x");
		return new BasicCapability(namespace, attributes, directives);
	}

	private Capability getIdentityCapability(BundleRevision revision) {
		List capabilities = revision.getDeclaredCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
		assertCapabilities(capabilities, 1);
		Capability capability = (Capability) capabilities.get(0);
		assertNotNull(capability);
		return capability;
	}

	private Capability getIdentityCapability(BundleWiring wiring) {
		List capabilities = wiring.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
		assertCapabilities(capabilities, 1);
		Capability capability = (Capability) capabilities.get(0);
		assertNotNull(capability);
		return capability;
	}

	private Capability getIdentityCapability(Resource resource) {
		List capabilities = resource.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
		assertCapabilities(capabilities, 1);
		Capability capability = (Capability) capabilities.get(0);
		assertNotNull(capability);
		return capability;
	}

	private Requirement getIdentityRequirement(BundleRevision revision, int index) {
		List requirements = revision.getDeclaredRequirements(IdentityNamespace.IDENTITY_NAMESPACE);
		assertRequirements(requirements, index);
		Requirement requirement = (Requirement) requirements.get(index);
		assertNotNull(requirement);
		return requirement;
	}

	private String getType(BundleRevision revision) {
		return (revision.getTypes() & BundleRevision.TYPE_FRAGMENT) == 0 ? IdentityNamespace.TYPE_BUNDLE
				: IdentityNamespace.TYPE_FRAGMENT;
	}
}
