/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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

import org.eclipse.core.runtime.PluginVersionIdentifier;
import org.eclipse.core.tests.harness.CoreTest;

@Deprecated
public class PluginVersionIdentifierTest extends CoreTest {

	public PluginVersionIdentifierTest() {
		super(null);
	}

	public PluginVersionIdentifierTest(String name) {
		super(name);
	}

	public void testConstructor() {

		assertEquals("1.0", "123.456.789", new PluginVersionIdentifier(123, 456, 789).toString());
		assertEquals("1.1", "123.456.789", new PluginVersionIdentifier("123.456.789").toString());
		assertEquals("1.2", "123.456.0", new PluginVersionIdentifier("123.456").toString());

		assertTrue("2.0", "0.123.456" != new PluginVersionIdentifier("123.456").toString());

		try {
			new PluginVersionIdentifier("-1.123.456");
		} catch (Exception e) {
			assertTrue("3.0", true);
		}

		PluginVersionIdentifier plugin = new PluginVersionIdentifier("1.123.456");
		assertTrue("4.0", plugin.getMajorComponent() == 1);
		assertTrue("4.1", plugin.getMinorComponent() == 123);
		assertTrue("4.2", plugin.getServiceComponent() == 456);

	}

	// should test the hashcode() method that is currently missing.
	public void testEqual() {

		assertTrue("1.0", new PluginVersionIdentifier(123, 456, 789).equals(new PluginVersionIdentifier("123.456.789")));
		assertTrue("1.1", !new PluginVersionIdentifier(123, 456, 789).equals(new PluginVersionIdentifier("123.456")));

	}

	public void testComparisons() {

		PluginVersionIdentifier plugin1 = new PluginVersionIdentifier("1.896.456");
		PluginVersionIdentifier plugin2 = new PluginVersionIdentifier("1.123.456");
		PluginVersionIdentifier plugin3 = new PluginVersionIdentifier("2.123.456");
		PluginVersionIdentifier plugin4 = new PluginVersionIdentifier("2.123.222");

		assertTrue("1.0", plugin1.isGreaterThan(plugin2));
		assertTrue("1.1", plugin3.isGreaterThan(plugin2));
		assertTrue("1.2", !plugin1.isGreaterThan(plugin4));

		assertTrue("2.0", plugin3.isEquivalentTo(plugin4));
		assertTrue("2.1", !plugin1.isEquivalentTo(plugin2));
		assertTrue("2.2", !plugin1.isEquivalentTo(plugin3));

		assertTrue("3.0", plugin1.isCompatibleWith(plugin2));
		assertTrue("3.1", !plugin1.isCompatibleWith(plugin3));

	}

	public void testValidate() {
		// success cases
		assertTrue("1.0", PluginVersionIdentifier.validateVersion("1").isOK());
		assertTrue("1.1", PluginVersionIdentifier.validateVersion("1.0").isOK());
		assertTrue("1.2", PluginVersionIdentifier.validateVersion("1.0.2").isOK());
		assertTrue("1.3", PluginVersionIdentifier.validateVersion("1.0.2.3456").isOK());
		assertTrue("1.3", PluginVersionIdentifier.validateVersion("1.2.3.-4").isOK());

		// failure cases
		assertTrue("2.0", !PluginVersionIdentifier.validateVersion("").isOK());
		assertTrue("2.1", !PluginVersionIdentifier.validateVersion("-1").isOK());
		assertTrue("2.2", !PluginVersionIdentifier.validateVersion(null).isOK());
		assertTrue("2.3", !PluginVersionIdentifier.validateVersion("/").isOK());
		assertTrue("2.4", !PluginVersionIdentifier.validateVersion("1.foo.2").isOK());
		assertTrue("2.5", !PluginVersionIdentifier.validateVersion("1./.3").isOK());
		assertTrue("2.6", !PluginVersionIdentifier.validateVersion(".").isOK());
		assertTrue("2.7", !PluginVersionIdentifier.validateVersion(".1").isOK());
		assertTrue("2.8", !PluginVersionIdentifier.validateVersion("1.2.").isOK());
		assertTrue("2.9", !PluginVersionIdentifier.validateVersion("1.2.3.4.5").isOK());
		assertTrue("2.10", !PluginVersionIdentifier.validateVersion("1.-2").isOK());
		assertTrue("2.11", !PluginVersionIdentifier.validateVersion("1.2.-3").isOK());
	}
}
