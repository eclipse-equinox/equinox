/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
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
 ******************************************************************************/

package org.eclipse.osgi.tests.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.eclipse.osgi.util.ManifestElement;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public class ManifestElementTestCase {

	@Test
	public void testSpacesInValues() throws BundleException {
		ManifestElement[] elements = ManifestElement.parseHeader("test-spaces",
				"\"comp 1\";\"comp 2\";\"comp 3\";attr=\"val 1\";dir:=\"val 2\"");
		assertNotNull("1.0", elements);
		assertEquals("1.1", elements.length, 1);
		String[] components = elements[0].getValueComponents();
		assertEquals("1.2", components.length, 3);

		assertEquals("2.0", components[0], "comp 1");
		assertEquals("2.1", components[1], "comp 2");
		assertEquals("2.2", components[2], "comp 3");

		assertEquals("3.0", elements[0].getAttribute("attr"), "val 1");
		assertEquals("3.1", elements[0].getDirective("dir"), "val 2");
	}

	@Test
	public void testBug238675_01() throws BundleException {
		ManifestElement[] elements = ManifestElement.parseHeader("Bundle-NativeCode", //$NON-NLS-1$
				"\"external:C:/tmp/x.dll\";\"external:C:/tmp/y.dll\"; osname =WindowsXP; osverison = 2.0; processor = x86"); //$NON-NLS-1$
		assertNotNull("1.0", elements);
		assertEquals("1.1", 1, elements.length);
		String[] components = elements[0].getValueComponents();
		assertEquals("1.2", 2, components.length);

		assertEquals("2.0", "external:C:/tmp/x.dll", components[0]);
		assertEquals("2.1", "external:C:/tmp/y.dll", components[1]);
	}

	@Test
	public void testBug238675_02() throws BundleException {
		ManifestElement[] elements = ManifestElement.parseHeader("Bundle-NativeCode", //$NON-NLS-1$
				"\"external:test1:test2\";\"test3:test4:\"; osname =WindowsXP; osverison = 2.0; processor = x86"); //$NON-NLS-1$
		assertNotNull("1.0", elements);
		assertEquals("1.1", 1, elements.length);
		String[] components = elements[0].getValueComponents();
		assertEquals("1.2", 2, components.length);

		assertEquals("2.0", components[0], "external:test1:test2");
		assertEquals("2.1", components[1], "test3:test4:");
	}

	private static final List<String> TEST_MANIFEST = Arrays.asList(//
			"Bundle-ManifestVersion: 2", //
			"Bundle-SymbolicName: test.", //
			" bsn", //
			"Import-Package: test1,", //
			" test2,", //
			" test3", //
			"" //
	);

	@Test
	public void testManifestWithCR() throws IOException, BundleException {
		doManifestTest("\r");
	}

	@Test
	public void testManifestWithLF() throws IOException, BundleException {
		doManifestTest("\n");
	}

	@Test
	public void testManifestWithCRLF() throws IOException, BundleException {
		doManifestTest("\r\n");
	}

	private void doManifestTest(String newLine) throws IOException, BundleException {
		Map<String, String> manifest = getManifest(TEST_MANIFEST, newLine);
		Assert.assertEquals("Wrong Bundle-SymbolicName.", "test.bsn", manifest.get(Constants.BUNDLE_SYMBOLICNAME));
		Assert.assertEquals("Wrong Import-Package.", "test1,test2,test3", manifest.get(Constants.IMPORT_PACKAGE));
	}

	private Map<String, String> getManifest(List<String> manifestLines, String newLine)
			throws IOException, BundleException {
		StringBuilder manifestText = new StringBuilder();
		for (String line : manifestLines) {
			manifestText.append(line).append(newLine);
		}
		return ManifestElement.parseBundleManifest(
				new ByteArrayInputStream(manifestText.toString().getBytes(StandardCharsets.UTF_8)), null);
	}
}
