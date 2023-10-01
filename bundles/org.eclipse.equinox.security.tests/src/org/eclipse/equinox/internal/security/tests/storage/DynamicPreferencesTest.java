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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import org.eclipse.core.tests.harness.BundleTestingHelper;
import org.eclipse.equinox.internal.security.storage.StorageUtils;
import org.eclipse.equinox.internal.security.tests.SecurityTestsActivator;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.StorageException;
import org.junit.Test;
import org.osgi.framework.*;

/**
 * In those tests listener is used to synchronize with the asynchronous registry
 * processing when bundle is added / removed.
 */
public class DynamicPreferencesTest extends StorageAbstractTest {

	final private static int MAX_TIME_PER_BUNDLE = 10000; // maximum time to wait for bundle event in milliseconds
	public static final String TEST_FILES_ROOT = "Plugin_Testing/";

	final private static String key = "password";
	final private static String unassignedKey = "unknown";
	final private static String value = "p[[pkknb#";
	final private static String defaultValue = "default";

	final private static String key2 = "password2";
	final private static String value2 = "34534534535";

	final private static String clearTextKey = "data";
	final private static String clearTextValue = "-> this should not be encrypted <-";

	private void check(ISecurePreferences prefs) throws StorageException {
		ISecurePreferences node1 = prefs.node("/abc");
		assertEquals(value, node1.get(key, defaultValue));
		assertEquals(defaultValue, node1.get(unassignedKey, defaultValue));
	}

	/**
	 * Test dynamic behavior while secure preferences remain in memory
	 */
	@Test
	public void testDynamicMemory() throws Exception {

		Bundle bundle01 = null;
		Bundle bundle02 = null;
		try {
			// add low priority module
			bundle01 = installBundle("priority/low");
			// fill - this should use the "low" priority bundle
			ISecurePreferences preferences = newPreferences(getStorageLocation(), getOptions());
			ISecurePreferences node1 = preferences.node("/abc");
			node1.put(key, value, true);
			// add high priority module
			bundle02 = installBundle("priority/high");
			// all should work well
			check(preferences);

			// new elements added should go through the high priority module
			// -> add entries on the same node as the first fill
			node1.put(key2, value2, true);
			node1.put(clearTextKey, clearTextValue, false);
			// -> add entries on a different node
			ISecurePreferences node2 = preferences.node("/abc/high");
			node2.put(key, value, true);
			node2.put(clearTextKey, clearTextValue, false);

			// uninstall high priority module
			if (uninstallBundle(bundle02))
				bundle02 = null;

			// check newly filled elements - should cause an exception
			checkUnreadable(node1, key2);
			checkUnreadable(node2, key);

			// the entry created with no encryption should read fine
			assertEquals(clearTextValue, node1.get(clearTextKey, defaultValue));
			assertEquals(clearTextValue, node2.get(clearTextKey, defaultValue));
			// check original elements - should be able to read fine
			check(preferences);
		} finally {
			// in case of exception in the process
			if (bundle02 != null)
				bundle02.uninstall();
			if (bundle01 != null)
				bundle01.uninstall();
		}
	}

	/**
	 * Test dynamic behavior with persisted secure preferences
	 */
	@Test
	public void testDynamicPersisted() throws Exception {
		URL location = getStorageLocation();
		assertNotNull(location);

		Bundle bundle01 = null;
		Bundle bundle02 = null;
		try {
			{ // block1: create and save using low priority module
				bundle01 = installBundle("priority/low");
				// fill - this should use the "low" priority bundle
				ISecurePreferences preferences = newPreferences(location, getOptions());
				ISecurePreferences node1 = preferences.node("/abc");
				node1.put(key, value, true);
				preferences.flush();
				closePreferences(preferences);
			}

			{ // block2: add high priority module and reload
				bundle02 = installBundle("priority/high");
				ISecurePreferences preferences = newPreferences(location, getOptions());
				// all should work well
				check(preferences);
				// new elements added should go through the high priority module
				// -> add entries on the same node as the first fill
				ISecurePreferences node1 = preferences.node("/abc");
				node1.put(key2, value2, true);
				// -> add entries on a different node
				ISecurePreferences node2 = preferences.node("/abc/high");
				node2.put(key, value, true);
				preferences.flush();
				closePreferences(preferences);
			}

			{ // block3: uninstall high priority module and reload
				if (uninstallBundle(bundle02))
					bundle02 = null;
				ISecurePreferences preferences = newPreferences(location, getOptions());
				ISecurePreferences node1 = preferences.node("/abc");
				ISecurePreferences node2 = preferences.node("/abc/high");
				// check newly filled elements - should cause an exception
				checkUnreadable(node1, key2);
				checkUnreadable(node2, key);
				// check original elements - should be able to read fine
				check(preferences);
			}
		} finally {
			// in case of exception in the process
			if (bundle02 != null)
				bundle02.uninstall();
			if (bundle01 != null)
				bundle01.uninstall();
			StorageUtils.delete(location);
		}
	}

	protected void checkUnreadable(ISecurePreferences node, String keyToCheck) {
		// check newly filled elements - should cause an exception
		boolean exception = false;
		try {
			node.get(keyToCheck, defaultValue);
		} catch (StorageException e) {
			exception = true;
		}
		assertTrue(exception);
	}

	protected Map<String, Object> getOptions() {
		// Don't specify default password for those tests; they need to have
		// password providers
		return getOptions(null);
	}

	/**
	 * Synchronizes to ensure bundle XML contribution has been processed before
	 * method returns.
	 */
	protected Bundle installBundle(String bundlePath) throws MalformedURLException, BundleException, IOException {
		BundleContext bundleContext = SecurityTestsActivator.getDefault().getBundleContext();
		Bundle bundle = null;
		WaitingRegistryListener listener = new WaitingRegistryListener();
		listener.register("org.eclipse.equinox.security.secureStorage");

		try {
			bundle = BundleTestingHelper.installBundle("0.1", bundleContext, TEST_FILES_ROOT + bundlePath);
			BundleTestingHelper.refreshPackages(bundleContext, new Bundle[] { bundle });
			// synchronization: listener should receive 1 group of events
			assertTrue(listener.waitFor(1, MAX_TIME_PER_BUNDLE) == 1);
		} finally {
			listener.unregister();
		}
		return bundle;
	}

	/**
	 * Synchronizes to ensure bundle XML contribution has been processed before
	 * method returns.
	 */
	protected boolean uninstallBundle(Bundle bundle) throws BundleException {
		WaitingRegistryListener listener = new WaitingRegistryListener();
		listener.register("org.eclipse.equinox.security.secureStorage");
		try {
			bundle.uninstall();
			bundle = null;
			// synchronization: listener should receive 1 group of events
			// There might be lots of active listeners by the time this test is run
			// so give it some time - hence multiplication by 10
			assertTrue(listener.waitFor(1, 10 * MAX_TIME_PER_BUNDLE) == 1);
			return true;
		} finally {
			listener.unregister();
			// in case of exception in the process
			if (bundle != null)
				bundle.uninstall();
		}
	}

}
