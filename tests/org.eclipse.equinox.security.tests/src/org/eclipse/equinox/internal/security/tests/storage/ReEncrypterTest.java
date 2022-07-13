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
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import org.eclipse.core.tests.harness.BundleTestingHelper;
import org.eclipse.equinox.internal.security.storage.friends.InternalExchangeUtils;
import org.eclipse.equinox.internal.security.storage.friends.ReEncrypter;
import org.eclipse.equinox.internal.security.tests.SecurityTestsActivator;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.StorageException;
import org.junit.Test;
import org.osgi.framework.*;

public class ReEncrypterTest extends StorageAbstractTest {

	final private static int MAX_TIME_PER_BUNDLE = 10000; // maximum time to wait for bundle event in milliseconds
	final private static String TEST_FILES_ROOT = "Plugin_Testing/";

	final private static String key = "password";
	final private static String value = "p[[pkknb#";

	final private static String clearTextKey = "data";
	final private static String clearTextValue = "-> this should not be encrypted <-";

	final private static String defaultValue = "default";

	@Test
	public void testFlushAfterEncrypt() throws IOException, StorageException, BundleException {
		URL location = getStorageLocation();
		assertNotNull(location);

		Bundle bundle = null;
		try {
			bundle = installBundle("controlled_provider");

			{ // block1: fill and save
				ISecurePreferences preferences = newPreferences(location, getOptions());
				fill(preferences);
				preferences.flush();
				closePreferences(preferences);
			}

			{ // block2: re-encrypt
				ISecurePreferences preferences = newPreferences(location, getOptions());
				ReEncrypter reEncrypter = new ReEncrypter(preferences, getModuleID());

				boolean decryptResult = reEncrypter.decrypt();
				assertTrue(decryptResult);

				boolean switchToNewPasswordResult = reEncrypter.switchToNewPassword();
				assertTrue(switchToNewPasswordResult);

				boolean encryptResult = reEncrypter.encrypt();
				assertTrue(encryptResult);
			}

			{ // block3: re-load and check
				ISecurePreferences preferences = newPreferences(location, getOptions());
				check(preferences);
			}
		} finally {
			if (bundle != null)
				bundle.uninstall();
		}
	}

	@Override
	protected String getModuleID() {
		return ControlledPasswordProvider.MODULE_ID;
	}

	protected Map<String, Object> getOptions() {
		// Don't specify default password for those tests; they need to have
		// password providers
		return getOptions(null);
	}

	/**
	 * Fills the secure preferences with some encrypted and non encrypted values.
	 */
	private void fill(ISecurePreferences preferences) throws StorageException {
		assertFalse(isModified(preferences));

		preferences.put(key, value, true); // puts encrypted entry at the root node
		preferences.put(clearTextKey, clearTextValue, false); // puts clear text entry at the root node

		assertTrue(isModified(preferences));
	}

	/**
	 * Checks that there isn't any change in the secure preferences and it contains
	 * the same values preciously saved.
	 */
	private void check(ISecurePreferences preferences) throws StorageException {
		assertFalse(isModified(preferences));

		assertEquals(value, preferences.get(key, defaultValue)); // checks entry at the root node
		assertEquals(clearTextValue, preferences.get(clearTextKey, defaultValue));
	}

	/**
	 * The method reaches into internal classes to check if modified flag is set on
	 * secure preference data.
	 */
	private boolean isModified(ISecurePreferences node) {
		return InternalExchangeUtils.isModified(node);
	}

	/**
	 * Dynamically installs a bundle that should contribute an Extension to the
	 * org.eclipse.equinox.security.secureStorage Extension Point.
	 * 
	 * Copied from DynamicPreferencesTest.
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

}
