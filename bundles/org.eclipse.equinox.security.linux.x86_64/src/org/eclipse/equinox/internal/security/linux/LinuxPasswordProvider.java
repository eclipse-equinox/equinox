/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Julien HENRY - Linux implementation
 *     Red Hat Inc. - add validation method to handle KDE failures
 *******************************************************************************/
package org.eclipse.equinox.internal.security.linux;

import java.security.SecureRandom;

import javax.crypto.spec.PBEKeySpec;

import org.eclipse.equinox.internal.security.auth.AuthPlugin;
import org.eclipse.equinox.internal.security.linux.nls.LinuxPasswordProviderMessages;
import org.eclipse.equinox.internal.security.storage.Base64;
import org.eclipse.equinox.internal.security.storage.provider.IValidatingPasswordProvider;
import org.eclipse.equinox.security.storage.provider.IPreferencesContainer;
import org.eclipse.equinox.security.storage.provider.PasswordProvider;

public class LinuxPasswordProvider extends PasswordProvider implements IValidatingPasswordProvider {

	/**
	 * The length of the randomly generated password in bytes
	 */
	private static final int PASSWORD_LENGTH = 64;

	private native String getMasterPassword() throws SecurityException;

	private native void saveMasterPassword(String password) throws SecurityException;
	
	private native boolean canUnlock() throws SecurityException;

	static {
		System.loadLibrary("keystorelinuxnative"); //$NON-NLS-1$
	}

	@Override
	public PBEKeySpec getPassword(IPreferencesContainer container, int passwordType) {

		boolean newPassword = (passwordType & CREATE_NEW_PASSWORD) != 0;
		boolean passwordChange = (passwordType & PASSWORD_CHANGE) != 0;

		if (!newPassword && !passwordChange) {
			try {
				return new PBEKeySpec(getMasterPassword().toCharArray());
			} catch (SecurityException e) {
				AuthPlugin.getDefault().logError(LinuxPasswordProviderMessages.getMasterPasswordError, e);
				return null;
			}
		}

		byte[] rawPassword = new byte[PASSWORD_LENGTH];
		SecureRandom random = new SecureRandom();
		random.setSeed(System.currentTimeMillis());
		random.nextBytes(rawPassword);
		String newPasswordString = Base64.encode(rawPassword);

		// add info message in the log
		AuthPlugin.getDefault().logMessage(LinuxPasswordProviderMessages.newMasterPasswordGenerated);

		try {
			saveMasterPassword(newPasswordString);
			return new PBEKeySpec(newPasswordString.toCharArray());
		} catch (SecurityException e) {
			AuthPlugin.getDefault().logError(LinuxPasswordProviderMessages.saveMasterPasswordError, e);
			return null;
		}
	}


	@Override
	public boolean isValid() {
		try {
		return canUnlock();
	} catch (SecurityException e) {
		return false; 
	}
	}

}
