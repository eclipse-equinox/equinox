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

import java.io.*;
import java.net.URL;
import java.util.*;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.PBEKeySpec;
import org.eclipse.equinox.internal.security.auth.nls.SecAuthMessages;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.equinox.security.storage.provider.IProviderHints;

/**
 * Root secure preference node. In addition to usual things it stores location, modified
 * status, encryption algorithm, and performs save and load. 
 */
public class SecurePreferencesRoot extends SecurePreferences implements IStorageConstants {

	private static final String VERSION_KEY = "org.eclipse.equinox.security.preferences.version"; //$NON-NLS-1$
	private static final String VERSION_VALUE = "1"; //$NON-NLS-1$

	/**
	 * Node path reserved for persisted preferences of the modules.
	 */
	static final public String PROVIDER_PATH = "org.eclipse.equinox.security.storage.impl"; //$NON-NLS-1$

	/**
	 * Description of the property file - information only
	 */
	final private static String description = "Equinox secure storage version 1.0"; //$NON-NLS-1$

	/**
	 * The node used by the secure preferences itself
	 */
	private final static String PROVIDER_NODE = "/org.eclipse.equinox.secure.storage"; //$NON-NLS-1$

	/**
	 * Node used to store password verification tokens
	 */
	private final static String PASSWORD_VERIFICATION_NODE = PROVIDER_NODE + "/verification"; //$NON-NLS-1$

	/**
	 * Text used to verify password
	 */
	private final static String PASSWORD_VERIFICATION_SAMPLE = "-> brown fox jumped over lazy dog <-"; //$NON-NLS-1$

	final private URL location;

	private boolean modified = false;

	private JavaEncryption cipher = new JavaEncryption();

	private Map passwordCache = new HashMap(5); // cached passwords: module ID -> PasswordExt 

	public SecurePreferencesRoot(URL location) throws IOException {
		super(null, null);
		this.location = location;
		load();
	}

	public URL getLocation() {
		return location;
	}

	public JavaEncryption getCipher() {
		return cipher;
	}

	public boolean isModified() {
		return modified;
	}

	public void setModified(boolean modified) {
		this.modified = modified;
	}

	public void load() throws IOException {
		if (location == null)
			return;

		Properties properties = new Properties();
		InputStream is = null;
		try {
			is = StorageUtils.getInputStream(location);
			if (is != null)
				properties.load(is);
		} finally {
			if (is != null)
				is.close();
		}

		// In future new versions could be added
		Object version = properties.get(VERSION_KEY);
		if ((version != null) && !VERSION_VALUE.equals(version))
			return;
		properties.remove(VERSION_KEY);

		// Process encryption algorithms
		if (properties.containsKey(CIPHER_KEY) && properties.containsKey(KEY_FACTORY_KEY)) {
			Object cipherAlgorithm = properties.get(CIPHER_KEY);
			Object keyFactoryAlgorithm = properties.get(KEY_FACTORY_KEY);
			if ((cipherAlgorithm instanceof String) && (keyFactoryAlgorithm instanceof String))
				cipher.setAlgorithms((String) cipherAlgorithm, (String) keyFactoryAlgorithm);
			properties.remove(CIPHER_KEY);
			properties.remove(KEY_FACTORY_KEY);
		}

		for (Iterator i = properties.keySet().iterator(); i.hasNext();) {
			Object externalKey = i.next();
			Object value = properties.get(externalKey);
			if (!(externalKey instanceof String))
				continue;
			if (!(value instanceof String))
				continue;
			PersistedPath storedPath = new PersistedPath((String) externalKey);
			if (storedPath.getKey() == null)
				continue;

			SecurePreferences node = node(storedPath.getPath());
			// don't use regular put() method as that would mark node as dirty
			node.internalPut(storedPath.getKey(), (String) value);
		}
	}

	public void flush() throws IOException {
		if (location == null)
			return;
		if (!modified)
			return;

		Properties properties = new Properties();
		properties.put(VERSION_KEY, VERSION_VALUE);

		// remember encyption algorithms
		String cipherAlgorithm = cipher.getCipherAlgorithm();
		if (cipherAlgorithm != null) {
			properties.put(CIPHER_KEY, cipherAlgorithm);
			properties.put(KEY_FACTORY_KEY, cipher.getKeyFactoryAlgorithm());
		}

		// save all user properties
		flush(properties, null);

		// output
		OutputStream stream = null;
		try {
			stream = StorageUtils.getOutputStream(location);
			properties.store(stream, description);
			modified = false;
		} finally {
			if (stream != null)
				stream.close();
		}
	}

	public PasswordExt getModulePassword(String moduleID, SecurePreferencesContainer container) throws StorageException {
		if (DEFAULT_PASSWORD_ID.equals(moduleID)) // this should never happen but add this check just in case
			throw new StorageException(StorageException.NO_PASSWORD, SecAuthMessages.loginNoPassword);

		PasswordProviderModuleExt moduleExt = PasswordProviderSelector.getInstance().findStorageModule(moduleID);
		synchronized (passwordCache) {
			String key = moduleExt.getID();
			if (passwordCache.containsKey(key))
				return (PasswordExt) passwordCache.get(key);

			// is there password verification string already?
			SecurePreferences node = node(PASSWORD_VERIFICATION_NODE);
			boolean newPassword = !node.hasKey(key);
			container.setOption(IProviderHints.NEW_PASSWORD, new Boolean(newPassword));

			boolean validPassword = false;
			PasswordExt passwordExt = null;

			for (int i = 0; i < MAX_ATTEMPTS; i++) {
				PBEKeySpec password = moduleExt.login(container);
				if (password == null)
					return null;
				passwordExt = new PasswordExt(password, key);
				if (newPassword) {
					CryptoData encryptedValue = getCipher().encrypt(passwordExt, PASSWORD_VERIFICATION_SAMPLE.getBytes());
					node.internalPut(key, encryptedValue.toString());
					markModified();
					validPassword = true;
					break;
				}
				// verify password using sample text
				String encryptedData = node.internalGet(key);
				CryptoData data = new CryptoData(encryptedData);
				try {
					byte[] decryptedData = getCipher().decrypt(passwordExt, data);
					if (PASSWORD_VERIFICATION_SAMPLE.equals(new String(decryptedData))) {
						validPassword = true;
						break;
					}
				} catch (IllegalBlockSizeException e) {
					if (!moduleExt.changePassword(e, container))
						break;
					moduleExt.logout(container);
				} catch (BadPaddingException e) {
					if (!moduleExt.changePassword(e, container))
						break;
					moduleExt.logout(container);
				}
			}
			if (validPassword) {
				passwordCache.put(key, passwordExt);
				return passwordExt;
			}
			throw new StorageException(StorageException.NO_PASSWORD, SecAuthMessages.loginNoPassword);
		}
	}

	public void clearPasswordCache() {
		synchronized (passwordCache) {
			passwordCache.clear();
		}
	}
}
