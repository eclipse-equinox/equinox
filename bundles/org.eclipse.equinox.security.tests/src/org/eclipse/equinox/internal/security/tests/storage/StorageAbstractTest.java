/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import junit.framework.TestCase;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.tests.harness.FileSystemHelper;
import org.eclipse.equinox.internal.security.storage.*;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.provider.IProviderHints;

/**
 * Temp directory is used for storage.
 */
public class StorageAbstractTest extends TestCase {

	final protected String defaultFileName = "secure_storage_test.equinox";

	private List openPreferences = new ArrayList(5); // <ISecurePreferences>

	public StorageAbstractTest() {
		super();
	}

	public StorageAbstractTest(String name) {
		super(name);
	}

	protected String getModuleID() {
		return null;
	}

	protected void tearDown() throws Exception {
		synchronized (openPreferences) {
			for (Iterator i = openPreferences.iterator(); i.hasNext();) {
				ISecurePreferences root = (ISecurePreferences) i.next();
				SecurePreferencesMapper.close((((SecurePreferencesWrapper) root).getContainer().getRootData()));
				URL location = ((SecurePreferencesWrapper) root).getContainer().getLocation();
				StorageUtils.delete(location);
			}
		}
		super.tearDown();
	}

	protected ISecurePreferences newPreferences(URL location, Map options) throws IOException {
		synchronized (openPreferences) {
			ISecurePreferences result = SecurePreferencesFactory.open(location, options);
			openPreferences.add(result);
			return result;
		}
	}

	protected void closePreferences(ISecurePreferences root) {
		synchronized (openPreferences) {
			for (Iterator i = openPreferences.iterator(); i.hasNext();) {
				ISecurePreferences element = (ISecurePreferences) i.next();
				if (element.equals(root)) {
					SecurePreferencesMapper.close((((SecurePreferencesWrapper) root).getContainer().getRootData()));
					i.remove();
				}
			}
		}
	}

	protected Map getOptions(String defaultPassword) {
		Map options = new HashMap();

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
	 * @throws MalformedURLException 
	 */
	protected URL getStorageLocation() throws MalformedURLException {
		IPath tempDir = FileSystemHelper.getTempDir();
		tempDir = tempDir.append(defaultFileName);
		return tempDir.toFile().toURL();
	}
}
