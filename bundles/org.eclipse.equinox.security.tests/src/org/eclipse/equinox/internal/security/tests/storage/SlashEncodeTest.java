/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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
package org.eclipse.equinox.internal.security.tests.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import org.eclipse.equinox.internal.security.storage.StorageUtils;
import org.eclipse.equinox.security.storage.*;
import org.junit.Test;

public class SlashEncodeTest extends StorageAbstractTest {

	final private static String[] decodedSlash = {"root", "ro/ot", "/root", "root/", "ro/ot/me", "ro//ot"};
	final private static String[] encodedSlash = {"root", "ro\\2fot", "\\2froot", "root\\2f", "ro\\2fot\\2fme", "ro\\2f\\2fot"};

	final private static String[] decodedBackSlash = {"ro\\ot", "\\root", "root\\", "ro\\ot\\me", "ro\\\\ot"};
	final private static String[] encodedBackSlash = {"ro\\5cot", "\\5croot", "root\\5c", "ro\\5cot\\5cme", "ro\\5c\\5cot"};

	final private static String[] decodedMixSlash = {"r/o\\ot", "r\\o/ot", "/\\root", "root\\/", "\\5cro\\2f ot"};
	final private static String[] encodedMixSlash = {"r\\2fo\\5cot", "r\\5co\\2fot", "\\2f\\5croot", "root\\5c\\2f", "\\5c5cro\\5c2f ot"};

	/**
	 * Tests forward slash
	 */
	@Test
	public void testForwardSlash() {
		for (int i = 0; i < decodedSlash.length; i++) {
			String tmp = EncodingUtils.encodeSlashes(decodedSlash[i]);
			assertEquals(encodedSlash[i], tmp);
			assertEquals(decodedSlash[i], EncodingUtils.decodeSlashes(tmp));
		}
	}

	/**
	 * Tests backward slash
	 */
	@Test
	public void testBackwardSlash() {
		for (int i = 0; i < decodedBackSlash.length; i++) {
			String tmp = EncodingUtils.encodeSlashes(decodedBackSlash[i]);
			assertEquals(encodedBackSlash[i], tmp);
			assertEquals(decodedBackSlash[i], EncodingUtils.decodeSlashes(tmp));
		}
	}

	/**
	 * Tests mixed slashes
	 */
	@Test
	public void testMixSlash() {
		for (int i = 0; i < decodedMixSlash.length; i++) {
			String tmp = EncodingUtils.encodeSlashes(decodedMixSlash[i]);
			assertEquals(encodedMixSlash[i], tmp);
			assertEquals(decodedMixSlash[i], EncodingUtils.decodeSlashes(tmp));
		}
	}

	/**
	 * Tests edge conditions: null or empty arguments 
	 */
	@Test
	public void testEdge() {
		assertNull(EncodingUtils.encodeSlashes(null));
		assertNull(EncodingUtils.decodeSlashes(null));

		String encoded = EncodingUtils.encodeSlashes("");
		assertNotNull(encoded);
		assertEquals("", encoded);
	}

	protected Map<String, Object> getOptions() {
		// Password value really doesn't matter here; we specify it to avoid
		// triggering UI elements in case default password provider has the 
		// highest priority in the tested configuration
		return getOptions("password1");
	}

	/**
	 * Tests preferences node name using slash encoding
	 * @throws IOException 
	 * @throws BackingStoreException 
	 */
	@Test
	public void testPreferencesWithSlashes() throws IOException, StorageException {
		URL location = getStorageLocation();
		assertNotNull(location);
		{ // block1: fill and test in memory
			ISecurePreferences preferences = newPreferences(location, getOptions());
			String safePath = EncodingUtils.encodeSlashes("ro/ot");
			ISecurePreferences node = preferences.node(safePath);
			node.put("password", "test", true);

			assertFalse(preferences.nodeExists("ro"));
			assertFalse(preferences.nodeExists("ro/ot"));
			assertTrue(preferences.nodeExists(safePath));

			preferences.flush();
			closePreferences(preferences);
		}

		{ // block2: reload and check
			ISecurePreferences preferences = newPreferences(location, getOptions());
			String children[] = preferences.childrenNames();
			assertNotNull(children);
			assertEquals(children.length, 1);
			ISecurePreferences nodeRet = preferences.node(children[0]);
			String absolutePath = EncodingUtils.decodeSlashes(nodeRet.absolutePath());
			assertEquals("/ro/ot", absolutePath);
			assertEquals("test", nodeRet.get("password", null));
		}
		StorageUtils.delete(location);
	}
}
