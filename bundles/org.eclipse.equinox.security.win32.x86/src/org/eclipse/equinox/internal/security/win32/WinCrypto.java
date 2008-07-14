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
package org.eclipse.equinox.internal.security.win32;

import java.io.IOException;
import java.security.SecureRandom;

import javax.crypto.spec.PBEKeySpec;

import org.eclipse.equinox.internal.security.auth.AuthPlugin;
import org.eclipse.equinox.internal.security.auth.nls.SecAuthMessages;
import org.eclipse.equinox.internal.security.storage.Base64;
import org.eclipse.equinox.internal.security.win32.nls.WinCryptoMessages;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.equinox.security.storage.provider.IPreferencesContainer;
import org.eclipse.equinox.security.storage.provider.PasswordProvider;

/**
 * Provides interface with native Windows data protection API. This provider
 * auto-generates separate passwords for each secure preferences tree.
 */
public class WinCrypto extends PasswordProvider {

	native public byte[] windecrypt(byte[] encryptedText);

	native public byte[] winencrypt(byte[] clearText);

	static {
		System.loadLibrary("jnicrypt");
	}
	
	private final static String WIN_PROVIDER_NODE = "/org.eclipse.equinox.secure.storage/windows";
	private final static String PASSWORD_KEY = "encryptedPassword";

	/**
	 * The length of the randomly generated password in bytes
	 */
	private final static int PASSWORD_LENGTH = 250;

	public PBEKeySpec getPassword(IPreferencesContainer container, int passwordType) {
		byte[] encryptedPassword;
		if ((passwordType & CREATE_NEW_PASSWORD) == 0)
			encryptedPassword = getEncryptedPassword(container);
		else
			encryptedPassword = null;
		
		if (encryptedPassword != null) {
			byte[] decryptedPassword = windecrypt(encryptedPassword);
			if (decryptedPassword != null) {
				String password = new String(decryptedPassword);
				return new PBEKeySpec(password.toCharArray());
			} else {
				StorageException e = new StorageException(StorageException.ENCRYPTION_ERROR, WinCryptoMessages.decryptPasswordFailed);
				AuthPlugin.getDefault().logError(WinCryptoMessages.decryptPasswordFailed, e);
				return null;
			}
		}

		// add info message in the log
		AuthPlugin.getDefault().logMessage(WinCryptoMessages.newPasswordGenerated);
		
		byte[] rawPassword = new byte[PASSWORD_LENGTH];
		SecureRandom random = new SecureRandom();
		random.setSeed(System.currentTimeMillis());
		random.nextBytes(rawPassword);
		String password = Base64.encode(rawPassword);
		if (savePassword(password, container))
			return new PBEKeySpec(password.toCharArray());
		else
			return null;
	}

	private byte[] getEncryptedPassword(IPreferencesContainer container) {
		ISecurePreferences node = container.getPreferences().node(WIN_PROVIDER_NODE);
		String passwordHint; 
		try {
			passwordHint = node.get(PASSWORD_KEY, null);
		} catch (StorageException e) { // should never happen in this scenario
			AuthPlugin.getDefault().logError(WinCryptoMessages.decryptPasswordFailed, e);
			return null;
		}
		if (passwordHint == null)
			return null;
		return Base64.decode(passwordHint);
	}

	private boolean savePassword(String password, IPreferencesContainer container){
		byte[] data = winencrypt(password.getBytes());
		if (data == null) { // this is bad. Something wrong with OS or JNI.
			StorageException e = new StorageException(StorageException.ENCRYPTION_ERROR, WinCryptoMessages.encryptPasswordFailed);
			AuthPlugin.getDefault().logError(WinCryptoMessages.encryptPasswordFailed, e);
			return false;
		}
		String encodedEncryptyedPassword = Base64.encode(data);
		ISecurePreferences node = container.getPreferences().node(WIN_PROVIDER_NODE);
		try {
			node.put(PASSWORD_KEY, encodedEncryptyedPassword, false); // note we don't recursively try to encrypt
		} catch (StorageException e) { // should never happen in this scenario
			AuthPlugin.getDefault().logError(SecAuthMessages.errorOnSave, e);
			return false;
		}
		try {
			node.flush(); // save right away
		} catch (IOException e) {
			AuthPlugin.getDefault().logError(SecAuthMessages.errorOnSave, e);
			return false;
		}
		return true;
	}

	public boolean retryOnError(Exception e, IPreferencesContainer container) {
		// It would be rather dangerous to allow this password to be changed
		// as it would permanently trash all entries in the secure storage.
		// Rather applications using get...() should handle exceptions and offer to overwrite 
		// data on an entry-by-entry scale.
		return false;
	}

}
