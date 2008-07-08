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
package org.eclipse.equinox.internal.security.osx;

import java.security.SecureRandom;
import javax.crypto.spec.PBEKeySpec;
import org.eclipse.equinox.internal.security.auth.AuthPlugin;
import org.eclipse.equinox.internal.security.osx.nls.OSXProviderMessages;
import org.eclipse.equinox.internal.security.storage.Base64;
import org.eclipse.equinox.security.storage.provider.IPreferencesContainer;
import org.eclipse.equinox.security.storage.provider.PasswordProvider;

public class OSXProvider extends PasswordProvider {

	static {
		System.loadLibrary("keystoreNative"); //$NON-NLS-1$
	}

	static final private String serviceName = "equinox.secure.storage"; //$NON-NLS-1$

	private String accountName = System.getProperty("user.name"); //$NON-NLS-1$

	private native String getPassword(String service, String account) throws SecurityException;

	private native String setPassword(String serviceName, String accountName, String password) throws SecurityException;

	public PBEKeySpec getPassword(IPreferencesContainer container, int passwordType) {
		if (accountName == null)
			return null;

		boolean newPassword = ((passwordType & CREATE_NEW_PASSWORD) != 0);
		boolean passwordChange = ((passwordType & PASSWORD_CHANGE) != 0);

		// simple auth
		if (!newPassword && !passwordChange) {
			try {
				return new PBEKeySpec(getPassword(serviceName, accountName).toCharArray());
			} catch (SecurityException e) {
				AuthPlugin.getDefault().logError(OSXProviderMessages.getPasswordError, e);
				return null;
			}
		}

		try {
			byte[] rawPassword = new byte[64];
			SecureRandom random = new SecureRandom();
			random.setSeed(System.currentTimeMillis());
			random.nextBytes(rawPassword);
			String newPasswordString = Base64.encode(rawPassword);
			// checking again in the retrieval case to minimize possible collisions
			if (!newPassword && !passwordChange) {
				try {
					return new PBEKeySpec(getPassword(serviceName, accountName).toCharArray());
				} catch (SecurityException e) {
					// ignore - we have already logged it above
				}
			}
			// encode the data to ensure it's ascii
			setPassword(serviceName, accountName, newPasswordString);
			return new PBEKeySpec(newPasswordString.toCharArray());
		} catch (SecurityException e) {
			AuthPlugin.getDefault().logError(OSXProviderMessages.setPasswordError, e);
			return null;
		}
	}
}
