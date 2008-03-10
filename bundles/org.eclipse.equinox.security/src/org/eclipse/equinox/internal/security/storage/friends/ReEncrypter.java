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

import java.util.*;
import org.eclipse.equinox.internal.security.auth.AuthPlugin;
import org.eclipse.equinox.internal.security.auth.nls.SecAuthMessages;
import org.eclipse.equinox.internal.security.storage.SecurePreferencesWrapper;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.osgi.util.NLS;

/**
 * The class will re-encrypt the whole preferences tree (any node on the tree 
 * can be passed in as a starting point).
 */
public class ReEncrypter {

	private class TmpElement {
		private String path; // absolute node path
		private Map values; // <String>key -> <String>value

		public TmpElement(String path, Map values) {
			this.path = path;
			this.values = values;
		}

		public String getPath() {
			return path;
		}

		public Map getValues() {
			return values;
		}
	}

	final private ISecurePreferences root;
	private boolean processedOK = true;

	private ArrayList elements = new ArrayList(); // List<TmpElement> 

	public ReEncrypter(ISecurePreferences prefs) {
		root = prefs.node("/"); //$NON-NLS-1$
	}

	/**
	 * The method will decrypt all data that can be decrypted into a local 
	 * memory structure.
	 */
	public boolean decrypt() {
		decrypt(root);
		return processedOK;
	}

	private void decrypt(ISecurePreferences node) {
		String[] keys = node.keys();
		if (keys.length > 0) {
			Map map = new HashMap(keys.length); // could be less than that
			for (int i = 0; i < keys.length; i++) {
				try {
					if (!node.isEncrypted(keys[i]))
						continue;
					map.put(keys[i], node.get(keys[i], null));
				} catch (StorageException e) {
					// this value will not be re-coded
					String msg = NLS.bind(SecAuthMessages.decryptingError, keys[i], node.absolutePath());
					AuthPlugin.getDefault().logError(msg, e);
					processedOK = false;
				}
			}
			if (map.size() != 0)
				elements.add(new TmpElement(node.absolutePath(), map));
		}
		String[] childrenNames = node.childrenNames();
		for (int i = 0; i < childrenNames.length; i++) {
			decrypt(node.node(childrenNames[i]));
		}
	}

	/**
	 * The method will encrypt all data from the memory structure created by
	 * decrypt using current passwords and providers. The original encrypted
	 * data will be overwritten.
	 */
	public boolean encrypt() {
		InternalExchangeUtils.passwordProvidersLogout();
		// TBD let providers know that this is a new password?
		((SecurePreferencesWrapper) root).clearPasswordVerification();

		boolean result = true;
		for (Iterator i = elements.iterator(); i.hasNext();) {
			TmpElement element = (TmpElement) i.next();
			ISecurePreferences node = root.node(element.getPath());
			Map values = element.getValues();
			for (Iterator j = values.keySet().iterator(); j.hasNext();) {
				String key = (String) j.next();
				try {
					node.put(key, (String) values.get(key), true);
				} catch (StorageException e) {
					// this value will not be re-coded
					String msg = NLS.bind(SecAuthMessages.encryptingError, key, node.absolutePath());
					AuthPlugin.getDefault().logError(msg, e);
					result = false;
				}
			}
		}
		return result;
	}

}
