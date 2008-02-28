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

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.equinox.internal.security.auth.AuthPlugin;
import org.eclipse.equinox.internal.security.auth.nls.SecAuthMessages;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.osgi.util.NLS;

// TBD on the UI side provide handling to change "preferred" algorithm using preferences page.
// Bonus: Default preferences: after the change offer to re-encrypt or delete old default secure prefs.

/**
 * Note that algorithm detection skips aliases:
 *    Alg.Alias.Cipher.ABC
 * only a few aliases are useful and it will be harder to separate human-readable
 * aliases from internal ones.
 *
 */
public class JavaEncryption {

	private final static String SECRET_KEY_FACTORY = "SecretKeyFactory."; //$NON-NLS-1$
	private final static String CIPHER = "Cipher."; //$NON-NLS-1$

	private final static String sampleText = "sample text for roundtrip testing"; //$NON-NLS-1$
	private final static PasswordExt samplePassword = new PasswordExt(new PBEKeySpec("password1".toCharArray()), "abc"); //$NON-NLS-1$ //$NON-NLS-2$

	static private final int SALT_ITERATIONS = 10;

	private String keyFactoryAlgorithm = null;
	private String cipherAlgorithm = null;

	private boolean initialized = false;

	public JavaEncryption() {
		// placeholder
	}

	public String getKeyFactoryAlgorithm() {
		return keyFactoryAlgorithm;
	}

	public String getCipherAlgorithm() {
		return cipherAlgorithm;
	}

	synchronized public void setAlgorithms(String cipherAlgorithm, String keyFactoryAlgorithm) {
		this.cipherAlgorithm = cipherAlgorithm;
		this.keyFactoryAlgorithm = keyFactoryAlgorithm;
	}

	private void init() throws StorageException {
		if (initialized)
			return;
		initialized = true;

		if (cipherAlgorithm != null && keyFactoryAlgorithm != null) {
			if (roundtrip(cipherAlgorithm, keyFactoryAlgorithm))
				return;
			// this is a bad situation - JVM cipher no longer available. Both log and throw an exception
			String msg = NLS.bind(SecAuthMessages.noAlgorithm, cipherAlgorithm);
			StorageException e = new StorageException(StorageException.INTERNAL_ERROR, msg);
			AuthPlugin.getDefault().logError(msg, e);
			throw e;
		}
		if (cipherAlgorithm == null || keyFactoryAlgorithm == null) {
			IEclipsePreferences eclipseNode = new ConfigurationScope().getNode(AuthPlugin.PI_AUTH);
			cipherAlgorithm = eclipseNode.get(IStorageConstants.CIPHER_KEY, IStorageConstants.DEFAULT_CIPHER);
			keyFactoryAlgorithm = eclipseNode.get(IStorageConstants.KEY_FACTORY_KEY, IStorageConstants.DEFAULT_KEY_FACTORY);
		}
		if (roundtrip(cipherAlgorithm, keyFactoryAlgorithm))
			return;
		String unavailableCipher = cipherAlgorithm;

		HashMap availableCiphers = detect();
		if (availableCiphers.size() == 0)
			throw new StorageException(StorageException.INTERNAL_ERROR, SecAuthMessages.noAlgorithms);

		// use first available
		cipherAlgorithm = (String) availableCiphers.keySet().iterator().next();
		keyFactoryAlgorithm = (String) availableCiphers.get(cipherAlgorithm);

		String msg = NLS.bind(SecAuthMessages.usingAlgorithm, unavailableCipher, cipherAlgorithm);
		AuthPlugin.getDefault().logMessage(msg);
	}

	synchronized public CryptoData encrypt(PasswordExt passwordExt, byte[] clearText) throws StorageException {
		init();
		try {
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(keyFactoryAlgorithm);
			SecretKey key = keyFactory.generateSecret(passwordExt.getPassword());

			byte[] salt = new byte[8];
			SecureRandom random = new SecureRandom();
			random.nextBytes(salt);
			PBEParameterSpec entropy = new PBEParameterSpec(salt, SALT_ITERATIONS);

			Cipher c = Cipher.getInstance(cipherAlgorithm);
			c.init(Cipher.ENCRYPT_MODE, key, entropy);

			byte[] result = c.doFinal(clearText);
			return new CryptoData(passwordExt.getModuleID(), salt, result);
		} catch (InvalidKeyException e) {
			handle(e, StorageException.ENCRYPTION_ERROR);
			return null;
		} catch (InvalidAlgorithmParameterException e) {
			handle(e, StorageException.ENCRYPTION_ERROR);
			return null;
		} catch (IllegalBlockSizeException e) {
			handle(e, StorageException.ENCRYPTION_ERROR);
			return null;
		} catch (BadPaddingException e) {
			handle(e, StorageException.ENCRYPTION_ERROR);
			return null;
		} catch (InvalidKeySpecException e) {
			handle(e, StorageException.INTERNAL_ERROR);
			return null;
		} catch (NoSuchPaddingException e) {
			handle(e, StorageException.INTERNAL_ERROR);
			return null;
		} catch (NoSuchAlgorithmException e) {
			handle(e, StorageException.INTERNAL_ERROR);
			return null;
		}
	}

	synchronized public byte[] decrypt(PasswordExt passwordExt, CryptoData encryptedData) throws StorageException, IllegalStateException, IllegalBlockSizeException, BadPaddingException {
		init();
		try {
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(keyFactoryAlgorithm);
			SecretKey key = keyFactory.generateSecret(passwordExt.getPassword());

			PBEParameterSpec entropy = new PBEParameterSpec(encryptedData.getSalt(), SALT_ITERATIONS);

			Cipher c = Cipher.getInstance(cipherAlgorithm);
			c.init(Cipher.DECRYPT_MODE, key, entropy);

			byte[] result = c.doFinal(encryptedData.getData());
			return result;
		} catch (InvalidAlgorithmParameterException e) {
			handle(e, StorageException.INTERNAL_ERROR);
			return null;
		} catch (InvalidKeyException e) {
			handle(e, StorageException.INTERNAL_ERROR);
			return null;
		} catch (InvalidKeySpecException e) {
			handle(e, StorageException.INTERNAL_ERROR);
			return null;
		} catch (NoSuchPaddingException e) {
			handle(e, StorageException.INTERNAL_ERROR);
			return null;
		} catch (NoSuchAlgorithmException e) {
			handle(e, StorageException.INTERNAL_ERROR);
			return null;
		}
	}

	private void handle(Exception e, int internalCode) throws StorageException {
		if (AuthPlugin.DEBUG_LOGIN_FRAMEWORK)
			e.printStackTrace();
		StorageException exception = new StorageException(internalCode, e);
		throw exception;
	}

	/////////////////////////////////////////////////////////////////////////////////////
	// Algorithm detection

	/**
	 * Result: Map:
	 *    <String>cipher -> <String>keyFactory
	 */
	public HashMap detect() {
		Set ciphers = findProviders(CIPHER);
		Set keyFactories = findProviders(SECRET_KEY_FACTORY);
		HashMap availableCiphers = new HashMap(ciphers.size());

		for (Iterator i = ciphers.iterator(); i.hasNext();) {
			String cipher = (String) i.next();
			// check if there is a key factory with the same name
			if (keyFactories.contains(cipher)) {
				if (roundtrip(cipher, cipher)) {
					availableCiphers.put(cipher, cipher);
					continue;
				}
			}
			for (Iterator j = keyFactories.iterator(); j.hasNext();) {
				String keyFactory = (String) j.next();
				if (roundtrip(cipher, keyFactory)) {
					availableCiphers.put(cipher, keyFactory);
					continue;
				}
			}
		}
		return availableCiphers;
	}

	private Set findProviders(String prefix) {
		Provider[] providers = Security.getProviders();
		Set algorithms = new HashSet();
		int prefixLength = prefix.length();
		for (int i = 0; i < providers.length; i++) {
			for (Iterator j = providers[i].entrySet().iterator(); j.hasNext();) {
				Map.Entry entry = (Map.Entry) j.next();
				Object key = entry.getKey();
				if (key == null)
					continue;
				if (!(key instanceof String))
					continue;
				String value = (String) key;
				if (value.indexOf(' ') != -1) // skips properties like "[Cipher.ABC SupportedPaddings]"
					continue;
				if (value.startsWith(prefix)) {
					String keyFactory = value.substring(prefixLength);
					algorithms.add(keyFactory);
				}
			}
		}
		return algorithms;
	}

	private boolean roundtrip(String testCipher, String testKeyFactory) {
		String storedCipherAlgorithm = cipherAlgorithm;
		String storedKeyAlgorithm = keyFactoryAlgorithm;
		try {
			cipherAlgorithm = testCipher;
			keyFactoryAlgorithm = testKeyFactory;
			CryptoData encrypted = encrypt(samplePassword, sampleText.getBytes());
			byte[] roundtripBytes = decrypt(samplePassword, encrypted);
			String result = new String(roundtripBytes);
			return sampleText.equals(result);
		} catch (Exception e) {
			// internal implementation throws both checked and unchecked
			// exceptions (without much documentation to go on), so have to use catch-all
			return false;
		} finally { // reset back
			cipherAlgorithm = storedCipherAlgorithm;
			keyFactoryAlgorithm = storedKeyAlgorithm;

		}
	}

}
