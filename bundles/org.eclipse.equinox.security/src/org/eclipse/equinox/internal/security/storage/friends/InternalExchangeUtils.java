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
package org.eclipse.equinox.internal.security.storage.friends;

import java.net.URL;
import java.util.*;
import org.eclipse.equinox.internal.security.storage.*;
import org.eclipse.equinox.internal.security.storage.PasswordProviderSelector.ExtStorageModule;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;

/**
 * Collection of utilities that gives friends additional access into
 * internals of the secure storage.
 */
public class InternalExchangeUtils {

	/**
	 * Detects ciphers supplied by the current JVM that can be used with
	 * the secure storage. Returns Map of pairs: supported cipher algorithm to 
	 * a supported key factory algorithm.
	 */
	static public Map ciphersDetectAvailable() {
		return new JavaEncryption().detect();
	}

	/**
	 * Clears cached passwords from the open storages and password providers.
	 */
	static public void passwordProvidersLogout() {
		PasswordProviderSelector.getInstance().logout();
	}

	/**
	 * Gathers list of available password providers. Note: this method does not try
	 * to instantiate providers, hence, providers listed as available by this method
	 * might fail on instantiation and not be available for the actual use.
	 * @return available password providers as described in extensions
	 */
	static public List passwordProvidersFind() {
		List availableModules = PasswordProviderSelector.getInstance().findAvailableModules(null);
		List result = new ArrayList(availableModules.size());
		for (Iterator i = availableModules.iterator(); i.hasNext();) {
			ExtStorageModule module = (ExtStorageModule) i.next();
			result.add(new PasswordProviderDescription(module.moduleID, module.priority));
		}
		return result;
	}

	/**
	 * Returns location of default storage
	 * @return location of the default storage, might be null
	 */
	static public URL defaultStorageLocation() {
		ISecurePreferences defaultStorage = SecurePreferencesFactory.getDefault();
		if (defaultStorage == null)
			return null;
		return ((SecurePreferencesWrapper) defaultStorage).getContainer().getLocation();
	}

	/**
	 * Closes open default storage, if any, and deletes the actual file.
	 */
	static public void defaultStorageDelete() {
		ISecurePreferences defaultStorage = SecurePreferencesFactory.getDefault();
		if (defaultStorage == null)
			return;
		URL location = defaultStorageLocation();
		if (location == null)
			return;

		// clear the default preferences store from the mapper
		SecurePreferencesMapper.clearDefault();

		// delete the actual file
		if (StorageUtils.exists(location))
			StorageUtils.delete(location);
	}

}
