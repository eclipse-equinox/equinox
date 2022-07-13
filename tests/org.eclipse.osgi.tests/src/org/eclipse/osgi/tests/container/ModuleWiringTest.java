/*******************************************************************************
 * Copyright (c) 2021, 2021 Hannes Wellmann and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Hannes Wellmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.container;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.container.ModuleCapability;
import org.eclipse.osgi.container.ModuleContainer;
import org.eclipse.osgi.container.ModuleRequirement;
import org.eclipse.osgi.container.ModuleRevision;
import org.eclipse.osgi.container.ModuleWire;
import org.eclipse.osgi.container.ModuleWiring;
import org.eclipse.osgi.tests.container.dummys.DummyContainerAdaptor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Wire;

public class ModuleWiringTest extends AbstractTest {

	private ModuleWiring wiring;

	@Before
	public void setUpBefore() throws BundleException, IOException {
		// Based on TestModuleContainer.testSimpleResolve
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();

		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, container);
		ModuleRevision systemRevision = systemBundle.getCurrentRevision();
		container.resolve(Arrays.asList(systemBundle), true);
		ModuleWiring systemWiring = systemRevision.getWiring();
		assertNotNull("system wiring is null", systemWiring);

		Module b1 = installDummyModule("b1_v1.MF", "b1", container);
		ModuleRevision b1Revision = b1.getCurrentRevision();
		container.resolve(Arrays.asList(b1), true);
		wiring = b1Revision.getWiring();
		assertNotNull("b1 wiring is null", wiring);
	}

	// --- test org.eclipse.osgi.container.ModuleWiring specific methods ---

	@Test
	public void testGetModuleCapabilities_isUnmodifiable() {
		List<ModuleCapability> capabilities = wiring.getModuleCapabilities(null);
		assertUnmodifiable(capabilities);
	}

	@Test
	public void testGetModuleRequirements_isUnmodifiable() {
		List<ModuleRequirement> requirements = wiring.getModuleRequirements(null);
		assertUnmodifiable(requirements);
	}

	@Test
	public void testGetProvidedModuleWires_isUnmodifiable() {
		List<ModuleWire> wiries = wiring.getProvidedModuleWires(null);
		assertUnmodifiable(wiries);
	}

	@Test
	public void testGetRequiredModuleWires_isUnmodifiable() {
		List<ModuleWire> wiries = wiring.getRequiredModuleWires(null);
		assertUnmodifiable(wiries);
	}

	// --- test org.osgi.framework.wiring.BundleWiring specific methods ---

	@Test
	public void testGetCapabilities_isModifiable() {
		List<BundleCapability> capabilities = wiring.getCapabilities(null);
		assertModifiable(capabilities);
	}

	@Test
	public void testGetRequirements_isModifiable() {
		List<BundleRequirement> requirements = wiring.getRequirements(null);
		assertModifiable(requirements);
	}

	@Test
	public void testGetProvidedWires_isModifiable() {
		List<BundleWire> wiries = wiring.getProvidedWires(null);
		assertModifiable(wiries);
	}

	@Test
	public void testGetRequiredWires_isModifiable() {
		List<BundleWire> wiries = wiring.getRequiredWires(null);
		assertModifiable(wiries);
	}

	// --- test org.osgi.resource.Wiring specific methods ---

	@Test
	public void testGetResourceCapabilities_isModifiable() {
		List<Capability> capabilities = wiring.getResourceCapabilities(null);
		assertModifiable(capabilities);
	}

	@Test
	public void testGetResourceRequirements_isModifiable() {
		List<Requirement> requirements = wiring.getResourceRequirements(null);
		assertModifiable(requirements);
	}

	@Test
	public void testGetProvidedResourceWires_isModifiable() {
		List<Wire> wiries = wiring.getProvidedResourceWires(null);
		assertModifiable(wiries);
	}

	@Test
	public void testGetRequiredResourceWires_isModifiable() {
		List<Wire> wiries = wiring.getRequiredResourceWires(null);
		assertModifiable(wiries);
	}

	// --- utility methods ---

	private static <T> void assertUnmodifiable(List<T> list) {
		Assert.assertThrows(RuntimeException.class, () -> list.add(null));
		Assert.assertThrows(RuntimeException.class, () -> list.remove(0));
		Assert.assertThrows(RuntimeException.class, () -> list.set(0, null));
	}

	private static <T> void assertModifiable(List<T> list) {
		List<T> copy = new ArrayList<>(list);
		list.add(null);
		copy.add(null);
		assertEquals(copy, list);

		list.set(0, null);
		copy.set(0, null);
		assertEquals(copy, list);

		list.remove(0);
		copy.remove(0);
		assertEquals(copy, list);
	}
}
