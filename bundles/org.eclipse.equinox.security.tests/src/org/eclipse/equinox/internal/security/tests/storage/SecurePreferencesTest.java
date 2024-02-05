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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.equinox.internal.security.storage.friends.InternalExchangeUtils;
import org.eclipse.equinox.internal.security.tests.SecurityTestsActivator;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.StorageException;
import org.junit.Test;
import org.osgi.framework.BundleContext;

abstract public class SecurePreferencesTest extends StorageAbstractTest {

	final private static String sampleLocation = "/SecurePrefsSample/1/secure_storage.equinox";

	final private static String path1 = "/test/abc";
	final private static String path2 = "/test/cvs/eclipse.org";
	final private static String path3 = "/test/cvs/eclipse.org/account1";

	final private static String key = "password";
	final private static String unassignedKey = "unknown";
	final private static String value = "p[[pkknb#";
	final private static String defaultValue = "default";

	final private static String secondKey = "/   sdfdsf / sf";
	final private static String secondValue = "one";

	final private static String clearTextKey = "data";
	final private static String clearTextValue = "-> this should not be encrypted <-";

	final private static String unicodeKey = "unicodeKey";
	final private static String unicodeValue = "va\u0432lue\u0433";

	protected Map<String, Object> getOptions() {
		// Note that if the default password value below is modified,
		// the sample storage file needs to be regenerated.
		return getOptions("password1");
	}

	private void fill(ISecurePreferences preferences) throws StorageException {
		preferences.put(key, value, true); // puts entry at the root node

		ISecurePreferences node1 = preferences.node(path1); // puts entry at the root node + 1
		node1.put(key, value, true);
		node1.put(clearTextKey, clearTextValue, false);

		ISecurePreferences node2 = preferences.node(path2); // puts entry at the root node + 2
		node2.put(key, value, true);
		node2.put(secondKey, secondValue, true);

		ISecurePreferences node3 = preferences.node(path3); // puts entry at the root node + 3
		node3.put(key, value, true);
		node3.put(secondKey, secondValue, true);
		node3.put(clearTextKey, clearTextValue, false);
		node3.put(unicodeKey, unicodeValue, true);

		node2.remove(secondKey);

		assertTrue(isModified(node2));
		assertTrue(isModified(preferences));
	}

	/**
	 * The method reaches into internal classes to check if modified flag is set on
	 * secure preference data.
	 */
	private boolean isModified(ISecurePreferences node) {
		return InternalExchangeUtils.isModified(node);
	}

	private void check(ISecurePreferences preferences) throws StorageException {
		assertFalse(isModified(preferences));
		assertEquals(value, preferences.get(key, defaultValue)); // checks entry at the root node
		assertEquals(defaultValue, preferences.get(unassignedKey, defaultValue));

		ISecurePreferences node1 = preferences.node(path1); // checks entry at the root node + 1
		assertFalse(isModified(node1));
		assertEquals(value, node1.get(key, defaultValue));
		assertEquals(defaultValue, node1.get(unassignedKey, defaultValue));
		assertEquals(clearTextValue, node1.get(clearTextKey, defaultValue));

		ISecurePreferences node2 = preferences.node(path2); // checks entry at the root node + 2
		assertFalse(isModified(node2));
		assertEquals(value, node2.get(key, defaultValue));
		assertNull(node2.get(secondKey, null));
		assertEquals(defaultValue, node2.get(unassignedKey, defaultValue));

		ISecurePreferences node3 = preferences.node(path3); // checks entry at the root node + 3
		assertFalse(isModified(node3));
		assertEquals(value, node3.get(key, defaultValue));
		assertEquals(secondValue, node3.get(secondKey, defaultValue));
		assertEquals(defaultValue, node3.get(unassignedKey, defaultValue));
		assertEquals(clearTextValue, node3.get(clearTextKey, defaultValue));
		assertEquals(unicodeValue, node3.get(unicodeKey, defaultValue));

		String[] leafKeys = node3.keys();
		assertNotNull(leafKeys);
		assertEquals(leafKeys.length, 4);
		findAll(new String[] { clearTextKey, key, secondKey, unicodeKey }, leafKeys);
	}

	/**
	 * Basic test to fill / read Preferences implementation. Also tests removal of a
	 * value and Preferences#keys().
	 */
	@Test
	public void testPreferences() throws IOException, StorageException {
		URL location = getStorageLocation();
		assertNotNull(location);
		{ // block1: fill and save
			ISecurePreferences preferences = newPreferences(getStorageLocation(), getOptions());
			fill(preferences);
			preferences.flush();
			closePreferences(preferences);
		}
		{ // block2: re-load and check
			ISecurePreferences preferences = newPreferences(getStorageLocation(), getOptions());
			check(preferences);
		}
	}

	/**
	 * Test relative names, absolute names, and children names
	 */
	@Test
	public void testNames() throws IOException, StorageException {
		ISecurePreferences preferences = newPreferences(getStorageLocation(), getOptions());
		fill(preferences);

		// check names for the root node
		assertNull(preferences.name());
		assertEquals("/", preferences.absolutePath());

		String[] childrenNames = preferences.node("test").childrenNames();
		assertNotNull(childrenNames);

		boolean order1 = "abc".equals(childrenNames[0]) && "cvs".equals(childrenNames[1]);
		boolean order2 = "abc".equals(childrenNames[1]) && "cvs".equals(childrenNames[0]);
		assertTrue(order1 || order2);
		assertEquals(childrenNames.length, 2);

		// check names for the root node + 1
		ISecurePreferences node1 = preferences.node("test/cvs");
		assertEquals("cvs", node1.name());
		assertEquals("/test/cvs", node1.absolutePath());

		String[] childrenNames1 = node1.childrenNames();
		assertNotNull(childrenNames1);
		assertEquals(childrenNames1.length, 1);
		assertEquals("eclipse.org", childrenNames1[0]);

		// check names for the root node + 2
		ISecurePreferences node2 = node1.node("eclipse.org");
		assertEquals("eclipse.org", node2.name());
		assertEquals("/test/cvs/eclipse.org", node2.absolutePath());

		String[] childrenNames2 = node2.childrenNames();
		assertNotNull(childrenNames2);
		assertEquals(childrenNames2.length, 1);
		assertEquals("account1", childrenNames2[0]);

		// check names for the leaf node
		ISecurePreferences node3 = node2.node("account1");
		assertEquals("account1", node3.name());
		assertEquals("/test/cvs/eclipse.org/account1", node3.absolutePath());

		String[] childrenNames3 = node3.childrenNames();
		assertNotNull(childrenNames3);
		assertEquals(childrenNames3.length, 0);
	}

	/**
	 * Test node existence, resolution: parent -> child; child -> parent, compare
	 * absolute and relative paths.
	 */
	@Test
	public void testNodeResolution() throws IOException, StorageException {
		ISecurePreferences preferences = newPreferences(getStorageLocation(), getOptions());
		fill(preferences);

		// absolute paths and node existence:
		assertTrue(preferences.nodeExists(null));
		ISecurePreferences nodeRoot = preferences.node(null);
		assertNotNull(nodeRoot);

		assertTrue(preferences.nodeExists("/test/cvs"));
		assertFalse(preferences.nodeExists("/test/nonExistent"));
		ISecurePreferences node1 = preferences.node("/test/cvs");
		assertNotNull(node1);

		assertTrue(preferences.nodeExists("/test/cvs/eclipse.org"));
		assertFalse(preferences.nodeExists("/test/nonExistent/cvs"));
		assertFalse(preferences.nodeExists("/test/cvs/nonExistent"));
		ISecurePreferences node2 = preferences.node("/test/cvs/eclipse.org");
		assertNotNull(node2);

		assertTrue(preferences.nodeExists("/test/cvs/eclipse.org/account1"));
		assertFalse(preferences.nodeExists("/test/cvs/nonExistent/cvs"));
		ISecurePreferences node3 = preferences.node("/test/cvs/eclipse.org/account1");
		assertNotNull(node3);

		// relative paths, parents and compare to results from absolute paths:
		assertNull(preferences.parent());
		assertEquals(nodeRoot, preferences);

		assertTrue(nodeRoot.nodeExists("test/cvs"));
		assertFalse(nodeRoot.nodeExists("test/nonExistent"));
		ISecurePreferences relativeNode1 = nodeRoot.node("test/cvs");
		assertNotNull(relativeNode1);
		assertEquals(node1, relativeNode1);
		assertEquals(nodeRoot, relativeNode1.parent().parent());

		assertTrue(relativeNode1.nodeExists("eclipse.org"));
		assertFalse(relativeNode1.nodeExists("nonExistent"));
		ISecurePreferences relativeNode2 = relativeNode1.node("eclipse.org");
		assertNotNull(relativeNode2);
		assertEquals(node2, relativeNode2);
		assertEquals(node1, relativeNode2.parent());

		assertTrue(relativeNode2.nodeExists("account1"));
		assertFalse(relativeNode2.nodeExists("nonExistent"));
		ISecurePreferences relativeNode3 = relativeNode2.node("account1");
		assertNotNull(relativeNode3);
		assertEquals(node3, relativeNode3);
		assertEquals(relativeNode2, relativeNode3.parent());

		// check contents to make sure that traversing did not add and new children
		preferences.flush();
		check(preferences);
	}

	/**
	 * Tests node removal.
	 */
	@Test
	public void testNodeRemoval() throws IOException, StorageException {
		URL location = getStorageLocation();
		assertNotNull(location);

		{ // block1: initial fill and check
			ISecurePreferences preferences = newPreferences(location, getOptions());
			fill(preferences);

			ISecurePreferences nodeToRemove = preferences.node("/test/cvs/eclipse.org");
			assertNotNull(nodeToRemove);
			nodeToRemove.removeNode();

			assertFalse(preferences.nodeExists("/test/cvs/eclipse.org/account1"));
			assertFalse(preferences.nodeExists("/test/cvs/eclipse.org"));

			preferences.flush();
			closePreferences(preferences);
		}

		{ // block2: reload
			ISecurePreferences preferences = newPreferences(location, getOptions());
			assertTrue(preferences.nodeExists(null));
			assertFalse(preferences.nodeExists("/test/cvs/eclipse.org/account1"));
			assertFalse(preferences.nodeExists("/test/cvs/eclipse.org"));

			ISecurePreferences node = preferences.node("/test/cvs");
			String[] children = node.childrenNames();
			assertNotNull(children);
			assertEquals(children.length, 0);

			// test in-memory removal
			ISecurePreferences node2 = preferences.node(null).node("test");
			String[] children2 = node2.childrenNames();
			assertNotNull(children2);
			assertEquals(children2.length, 2);

			ISecurePreferences nodeToRemove2 = node2.node("/test/cvs");
			assertNotNull(nodeToRemove2);
			nodeToRemove2.removeNode();
			String[] children3 = node2.childrenNames();
			assertNotNull(children3);
			assertEquals(children3.length, 1);
			assertEquals("abc", children3[0]);

			preferences.removeNode(); // check the special case - removal of the root node
			boolean exception = false;
			try {
				preferences.nodeExists(null);
			} catch (IllegalStateException e) {
				exception = true;
			}
			assertTrue(exception);
		}
	}

	/**
	 * Tests validation of node paths.
	 */
	@Test
	public void testPathValidation() throws Throwable {
		ISecurePreferences preferences = newPreferences(getStorageLocation(), getOptions());
		boolean exception = false;
		try {
			preferences.node("/test/cvs/eclipse.org//account1");
		} catch (IllegalArgumentException e) {
			exception = true;
		}
		assertTrue(exception);

		exception = false;
		try {
			preferences.node("/test/cvs/eclipse.org/");
		} catch (IllegalArgumentException e) {
			exception = true;
		}
		assertTrue(exception);

		exception = false;
		try {
			preferences.node("/test/cvs/eclipse.org//");
		} catch (IllegalArgumentException e) {
			exception = true;
		}
		assertTrue(exception);
	}

	/**
	 * Tests URL validation.
	 */
	@Test
	public void testLocation() throws MalformedURLException {
		URL invalidURL = new URL("http", "eclipse.org", "testEquinoxFile");
		boolean exception = false;
		try {
			newPreferences(invalidURL, getOptions());
		} catch (IOException e) {
			exception = true;
		}
		assertTrue(exception);
	}

	/**
	 * Tests data types
	 */
	@Test
	public void testDataTypes() throws StorageException, MalformedURLException, IOException {
		ISecurePreferences preferences = newPreferences(getStorageLocation(), getOptions());

		ISecurePreferences node = preferences.node("/test");
		byte[] testArray = new byte[] { 0, 4, 12, 75, 84, 12, 1, (byte) 0xFF };

		boolean encrypt = true;

		node.putBoolean("trueBoolean", true, encrypt);
		node.putBoolean("falseBoolean", false, encrypt);

		node.putInt("oneInteger", 1, encrypt);
		node.putLong("twoLong", 2l, encrypt);
		node.putFloat("threeFloat", 3.12f, encrypt);
		node.putDouble("fourDouble", 4.1d, encrypt);

		assertTrue(node.getBoolean("trueBoolean", false));
		assertFalse(node.getBoolean("falseBoolean", true));
		assertTrue(node.getBoolean("unknownBoolean", true));
		assertFalse(node.getBoolean("unknownBoolean", false));

		assertEquals(node.getInt("oneInteger", 0), 1);
		assertEquals(node.getInt("unknownInteger", 5), 5);

		assertEquals(node.getLong("twoLong", 0), 2l);
		assertEquals(node.getLong("unknownLong", 5), 5l);

		assertEquals(node.getFloat("threeFloat", 0f), 3.12f, 0);
		assertEquals(node.getFloat("unknownFloat", 1.23f), 1.23f, 0);

		assertEquals(node.getDouble("fourDouble", 0), 4.1d, 0);
		assertEquals(node.getDouble("unknownDouble", 1.23d), 1.23d, 0);

		node.putByteArray("fiveArray", testArray, encrypt);
		byte[] array = node.getByteArray("fiveArray", null);
		compareArrays(testArray, array);
	}

	/**
	 * Tests corrupted encrypted data.
	 */
	@Test
	public void testIncorrectData() throws IOException {
		URL location = getFilePath(sampleLocation);
		// Same default password as in the SecurePreferencesTest.getOptions() - same
		// note
		// on regenerating data file.
		ISecurePreferences preferences = newPreferences(location, getOptions("password1"));
		try {
			ISecurePreferences node = preferences.node("/abc");
			boolean exception = false;
			try {
				node.get("password1", "default");
			} catch (StorageException e) {
				assertEquals(StorageException.INTERNAL_ERROR, e.getErrorCode());
				exception = true;
			}
			assertTrue(exception);

			exception = false;
			try {
				node.get("password2", "default");
			} catch (StorageException e) {
				assertEquals(StorageException.INTERNAL_ERROR, e.getErrorCode());
				exception = true;
			}
			assertTrue(exception);
		} finally {
			// make sure we won't try to delete it
			closePreferences(preferences);
		}
	}

	/**
	 * Tests incorrect passwords
	 */
	@Test
	public void testIncorrectPassword() throws IOException {
		URL location = getFilePath(sampleLocation);
		Map<String, Object> options = getOptions("wrong");
		ISecurePreferences preferences = newPreferences(location, options);
		try {
			ISecurePreferences node = preferences.node("/cvs/eclipse.org");
			boolean exception = false;
			try {
				node.get("password", "default");
			} catch (StorageException e) {
				assertEquals(StorageException.INTERNAL_ERROR, e.getErrorCode());
				exception = true;
			}
			assertTrue(exception);
		} finally {
			// make sure we won't try to delete it
			closePreferences(preferences);
		}
	}

	/**
	 * Tests incorrect or unexpected module specifications
	 */
	@Test
	public void testModules() throws IOException {
		URL location = getFilePath(sampleLocation);
		ISecurePreferences preferences = newPreferences(location, getOptions(null));
		try {
			ISecurePreferences node = preferences.node("/cvs/eclipse.org/account1");

			// non-existent module
			boolean exception = false;
			try {
				node.get("password1", "default");
			} catch (StorageException e) {
				assertEquals(StorageException.NO_SECURE_MODULE, e.getErrorCode());
				exception = true;
			}
			assertTrue(exception);

			// empty module and no default password
			exception = false;
			try {
				node.get("password2", "default");
			} catch (StorageException e) {
				assertEquals(StorageException.DECRYPTION_ERROR, e.getErrorCode());
				exception = true;
			}
			assertTrue(exception);
		} finally {
			// make sure we won't try to delete it
			closePreferences(preferences);
		}
	}

	/**
	 * Tests edge cases for data (nulls, empty strings, and so on).
	 */
	@Test
	public void testEdgeCases() throws StorageException, MalformedURLException, IOException {
		byte[] expectedEmptyArray = new byte[0];
		byte[] wrongArray = new byte[] { 1, 2, 3 };

		{ // block1: fill, check, and save
			ISecurePreferences preferences = newPreferences(getStorageLocation(), getOptions());
			ISecurePreferences node = preferences.node("/testEdge");

			node.put("emptyString1", "", true);
			node.put("emptyString2", "", false);
			node.put("nullString1", null, true);
			node.put("nullString2", null, false);

			node.putByteArray("emptyArray1", new byte[0], true);
			node.putByteArray("emptyArray2", new byte[0], false);
			node.putByteArray("nullArray1", null, true);
			node.putByteArray("nullArray2", null, false);

			assertEquals("", node.get("emptyString1", "wrong"));
			assertEquals("", node.get("emptyString2", "wrong"));
			assertNull(node.get("nullString1", "wrong"));
			assertNull(node.get("nullString2", "wrong"));

			compareArrays(expectedEmptyArray, node.getByteArray("emptyArray1", wrongArray));
			compareArrays(expectedEmptyArray, node.getByteArray("emptyArray2", wrongArray));
			assertNull(node.getByteArray("nullString1", wrongArray));
			assertNull(node.getByteArray("nullString2", wrongArray));

			preferences.flush();
			closePreferences(preferences);
		}
		{ // block2: re-load and check
			ISecurePreferences preferences = newPreferences(getStorageLocation(), getOptions());
			ISecurePreferences node = preferences.node("/testEdge");

			assertEquals("", node.get("emptyString1", "wrong"));
			assertEquals("", node.get("emptyString2", "wrong"));
			assertNull(node.get("nullString1", "wrong"));
			assertNull(node.get("nullString2", "wrong"));

			compareArrays(expectedEmptyArray, node.getByteArray("emptyArray1", wrongArray));
			compareArrays(expectedEmptyArray, node.getByteArray("emptyArray2", wrongArray));
			assertNull(node.getByteArray("nullString1", wrongArray));
			assertNull(node.getByteArray("nullString2", wrongArray));
		}
	}

	// assumes all entries are unique and array1 has no null elements
	private void findAll(String[] array1, String[] array2) {
		assertNotNull(array1);
		assertNotNull(array2);
		assertEquals(array1.length, array2.length);
		for (String s : array1) {
			boolean found = false;
			for (String s2 : array2) {
				if (s.equals(s2)) {
					found = true;
					break;
				}
			}
			assertTrue(found);
		}
	}

	private void compareArrays(byte[] array1, byte[] array2) {
		assertNotNull(array1);
		assertNotNull(array2);
		assertEquals(array1.length, array2.length);
		for (int i = 0; i < array1.length; i++)
			assertEquals(array1[i], array2[i]);
	}

	private URL getFilePath(String path) throws IOException {
		BundleContext bundleContext = SecurityTestsActivator.getDefault().getBundleContext();
		URL url = bundleContext.getBundle().getEntry(path);
		return FileLocator.toFileURL(url);
	}

}
