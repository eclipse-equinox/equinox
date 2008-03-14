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
package org.eclipse.equinox.internal.security.storage;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import org.eclipse.equinox.internal.security.auth.AuthPlugin;
import org.eclipse.equinox.internal.security.auth.nls.SecAuthMessages;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.osgi.util.NLS;

public class SecurePreferencesMapper {

	static private ISecurePreferences defaultPreferences = null;

	static private Map preferences = new HashMap(); // URL.toString() -> SecurePreferencesRoot

	static public ISecurePreferences getDefault() {
		if (defaultPreferences == null) {
			try {
				defaultPreferences = open(null, null);
			} catch (IOException e) {
				AuthPlugin.getDefault().logError(SecAuthMessages.keyringNotAvailable, e);
			}
		}
		return defaultPreferences;
	}

	static public void clearDefault() {
		if (defaultPreferences == null)
			return;

		try {
			defaultPreferences.flush();
		} catch (IOException e) {
			// ignore in this context
		}
		close((((SecurePreferencesWrapper) defaultPreferences).getContainer().getRootData()));
		defaultPreferences = null;
	}

	static public ISecurePreferences open(URL location, Map options) throws IOException {
		// 1) process location
		if (location == null)
			location = StorageUtils.getDefaultLocation();
		if (!StorageUtils.isFile(location))
			// at this time we only accept file URLs; check URL type right away
			throw new IOException(NLS.bind(SecAuthMessages.loginFileURL, location.toString()));

		// 2) see if there is already SecurePreferencesRoot at that location; if not open a new one
		String key = location.toString();
		SecurePreferencesRoot root;
		if (preferences.containsKey(key))
			root = (SecurePreferencesRoot) preferences.get(key);
		else {
			root = new SecurePreferencesRoot(location);
			preferences.put(key, root);
		}

		// 3) create container with the options passed in
		SecurePreferencesContainer container = new SecurePreferencesContainer(root, options);
		return container.getPreferences();
	}

	static public void stop() {
		synchronized (preferences) {
			for (Iterator i = preferences.values().iterator(); i.hasNext();) {
				SecurePreferencesRoot provider = (SecurePreferencesRoot) i.next();
				try {
					provider.flush();
				} catch (IOException e) {
					AuthPlugin.getDefault().logError(SecAuthMessages.errorOnSave, e);
				}
			}
			preferences.clear();
		}
	}

	static public void clearPasswordCache() {
		synchronized (preferences) {
			for (Iterator i = preferences.values().iterator(); i.hasNext();) {
				SecurePreferencesRoot provider = (SecurePreferencesRoot) i.next();
				provider.clearPasswordCache();
			}
		}
	}

	// Not exposed as API; mostly intended for testing
	static public void close(SecurePreferencesRoot root) {
		if (root == null)
			return;
		synchronized (preferences) {
			for (Iterator i = preferences.values().iterator(); i.hasNext();) {
				SecurePreferencesRoot provider = (SecurePreferencesRoot) i.next();
				if (!root.equals(provider))
					continue;
				i.remove();
				break;
			}
		}
	}

}
