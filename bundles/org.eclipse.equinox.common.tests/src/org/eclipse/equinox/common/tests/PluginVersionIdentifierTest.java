/*******************************************************************************
 * Copyright (c) 2000, 2025 IBM Corporation and others.
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
package org.eclipse.equinox.common.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.runtime.PluginVersionIdentifier;
import org.junit.Test;

@Deprecated
@SuppressWarnings("removal")
public class PluginVersionIdentifierTest {

	@Deprecated
	@Test
	public void testConstructor() {

		assertEquals("123.456.789", new PluginVersionIdentifier(123, 456, 789).toString());
		assertEquals("123.456.789", new PluginVersionIdentifier("123.456.789").toString());
		assertEquals("123.456.0", new PluginVersionIdentifier("123.456").toString());

		assertNotEquals("0.123.456", new PluginVersionIdentifier("123.456").toString());

		assertThrows(Exception.class,()->
			new PluginVersionIdentifier("-1.123.456"));

		PluginVersionIdentifier plugin = new PluginVersionIdentifier("1.123.456");
		assertEquals(1, plugin.getMajorComponent());
		assertEquals(123, plugin.getMinorComponent());
		assertEquals(456, plugin.getServiceComponent());

	}

	// should test the hashcode() method that is currently missing.
	@Deprecated
	@Test
	public void testEqual() {

		assertTrue(new PluginVersionIdentifier(123, 456, 789).equals(new PluginVersionIdentifier("123.456.789")));
		assertFalse(new PluginVersionIdentifier(123, 456, 789).equals(new PluginVersionIdentifier("123.456")));

	}

	@Deprecated
	@Test
	public void testComparisons() {

		PluginVersionIdentifier plugin1 = new PluginVersionIdentifier("1.896.456");
		PluginVersionIdentifier plugin2 = new PluginVersionIdentifier("1.123.456");
		PluginVersionIdentifier plugin3 = new PluginVersionIdentifier("2.123.456");
		PluginVersionIdentifier plugin4 = new PluginVersionIdentifier("2.123.222");

		assertTrue(plugin1.isGreaterThan(plugin2));
		assertTrue(plugin3.isGreaterThan(plugin2));
		assertFalse(plugin1.isGreaterThan(plugin4));

		assertTrue(plugin3.isEquivalentTo(plugin4));
		assertFalse(plugin1.isEquivalentTo(plugin2));
		assertFalse(plugin1.isEquivalentTo(plugin3));

		assertTrue(plugin1.isCompatibleWith(plugin2));
		assertFalse(plugin1.isCompatibleWith(plugin3));

	}

	@Deprecated
	@Test
	public void testValidate() {
		// success cases
		assertTrue(PluginVersionIdentifier.validateVersion("1").isOK());
		assertTrue(PluginVersionIdentifier.validateVersion("1.0").isOK());
		assertTrue(PluginVersionIdentifier.validateVersion("1.0.2").isOK());
		assertTrue(PluginVersionIdentifier.validateVersion("1.0.2.3456").isOK());
		assertTrue(PluginVersionIdentifier.validateVersion("1.2.3.-4").isOK());

		// failure cases
		assertFalse(PluginVersionIdentifier.validateVersion("").isOK());
		assertFalse(PluginVersionIdentifier.validateVersion("-1").isOK());
		assertFalse(PluginVersionIdentifier.validateVersion(null).isOK());
		assertFalse(PluginVersionIdentifier.validateVersion("/").isOK());
		assertFalse(PluginVersionIdentifier.validateVersion("1.foo.2").isOK());
		assertFalse(PluginVersionIdentifier.validateVersion("1./.3").isOK());
		assertFalse(PluginVersionIdentifier.validateVersion(".").isOK());
		assertFalse(PluginVersionIdentifier.validateVersion(".1").isOK());
		assertFalse(PluginVersionIdentifier.validateVersion("1.2.").isOK());
		assertFalse(PluginVersionIdentifier.validateVersion("1.2.3.4.5").isOK());
		assertFalse(PluginVersionIdentifier.validateVersion("1.-2").isOK());
		assertFalse(PluginVersionIdentifier.validateVersion("1.2.-3").isOK());
	}
}
