/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import javax.crypto.spec.PBEKeySpec;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.tests.harness.FileSystemHelper;
import org.eclipse.equinox.internal.security.storage.*;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.provider.IProviderHints;
import org.junit.After;

/**
 * Temp directory is used for storage.
 */
public class StorageAbstractTest {

	final protected String defaultFileName = "secure_storage_test.equinox";

	private List<ISecurePreferences> openPreferences = new ArrayList<>(5); // <ISecurePreferences>

	protected String getModuleID() {
		return null;
	}

	@After
	public void tearDown() throws Exception {
		synchronized (openPreferences) {
			for (ISecurePreferences root : openPreferences) {
				SecurePreferencesMapper.close((((SecurePreferencesWrapper) root).getContainer().getRootData()));
				URL location = ((SecurePreferencesWrapper) root).getContainer().getLocation();
				StorageUtils.delete(location);
			}
		}
	}

	protected ISecurePreferences newPreferences(URL location, Map<String, Object> options) throws IOException {
		synchronized (openPreferences) {
			ISecurePreferences result = SecurePreferencesFactory.open(location, options);
			openPreferences.add(result);
			return result;
		}
	}

	protected void closePreferences(ISecurePreferences root) {
		synchronized (openPreferences) {
			for (Iterator<ISecurePreferences> i = openPreferences.iterator(); i.hasNext();) {
				ISecurePreferences element = i.next();
				if (element.equals(root)) {
					SecurePreferencesMapper.close((((SecurePreferencesWrapper) root).getContainer().getRootData()));
					i.remove();
				}
			}
		}
	}

	protected Map<String, Object> getOptions(String defaultPassword) {
		Map<String, Object> options = new HashMap<>();

		if (defaultPassword != null) {
			PBEKeySpec password = new PBEKeySpec(defaultPassword.toCharArray());
			options.put(IProviderHints.DEFAULT_PASSWORD, password);
		}

		String requiredID = getModuleID();
		if (requiredID != null)
			options.put(IProviderHints.REQUIRED_MODULE_ID, requiredID);

		options.put(IProviderHints.PROMPT_USER, Boolean.FALSE);
		return options;
	}

	/**
	 * Might consider switching to configuration location.
	 * 
	 * @throws MalformedURLException
	 */
	@SuppressWarnings("deprecation")
	protected URL getStorageLocation() throws MalformedURLException {
		IPath tempDir = FileSystemHelper.getTempDir();
		tempDir = tempDir.append(defaultFileName);
		return tempDir.toFile().toURL();
	}
}
