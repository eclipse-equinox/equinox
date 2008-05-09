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
import org.eclipse.equinox.internal.security.storage.SecurePreferencesContainer;
import org.eclipse.equinox.internal.security.storage.SecurePreferencesWrapper;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.equinox.security.storage.provider.IProviderHints;
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
	final private String moduleID;
	private boolean processedOK = true;

	private ArrayList elements = new ArrayList(); // List<TmpElement> 

	public ReEncrypter(ISecurePreferences prefs, String moduleID) {
		this.moduleID = moduleID;
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
					if (!(node instanceof SecurePreferencesWrapper))
						continue;
					String encryptionModule = ((SecurePreferencesWrapper) node).getModule(keys[i]);
					if (encryptionModule == null)
						continue;
					if (!encryptionModule.equals(moduleID))
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
	 * The method try to create new password. 
	 * <p>
	 * <strong>Note</strong> that after the successful completion of this method the secure storage has
	 * new verification string and previously decoded values <b>must</b> be added via encrypt() method 
	 * or they will become unavailable via conventional APIs.
	 * </p>
	 */
	public boolean switchToNewPassword() {
		return ((SecurePreferencesWrapper) root).passwordChanging(moduleID);
	}

	/**
	 * The method will encrypt all data from the memory structure created by decrypt using current 
	 * passwords and providers. The original encrypted data will be overwritten.
	 */
	public boolean encrypt() {
		boolean result = true;

		// we'll directly inject here a requirement to use the specified module to encrypt data
		SecurePreferencesContainer container = ((SecurePreferencesWrapper) root).getContainer();
		Object originalProperty = container.getOption(IProviderHints.REQUIRED_MODULE_ID);
		container.setOption(IProviderHints.REQUIRED_MODULE_ID, moduleID);
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
		if (originalProperty != null)
			container.setOption(IProviderHints.REQUIRED_MODULE_ID, originalProperty);
		else
			container.removeOption(IProviderHints.REQUIRED_MODULE_ID);
		return result;
	}

}
