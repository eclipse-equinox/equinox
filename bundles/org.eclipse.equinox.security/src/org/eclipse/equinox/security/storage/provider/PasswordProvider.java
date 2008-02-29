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
package org.eclipse.equinox.security.storage.provider;

import javax.crypto.spec.PBEKeySpec;

/**
 * Password provider modules should extend this class. Secure storage will
 * ask modules for passwords used to encrypt entries stored in the secure preferences.
 * <p>
 * Password provider modules can be through of as trusted 3rd parties used
 * to provide passwords to open keyrings containing secure preferences.
 * </p><p>
 * Use org.eclipse.equinox.security.secureStorage extension point to contribute 
 * password provider module to the secure storage system.
 * </p> 
 */
abstract public class PasswordProvider {

	/**
	 * This method should return the password used to encrypt entries in the secure 
	 * preferences.
	 * @param container container of the secure preferences
	 * @return password used to encrypt entries in the secure preferences, <code>null</code>
	 * if unable to obtain password
	 */
	abstract public PBEKeySpec login(IPreferencesContainer container);

	/**
	 * A logical equivalent of "logout" for the password providers. 
	 * <p>
	 * The module should clear its cached password if it is the password that can be
	 * re-obtained from some third party (such as asking user to type it in). Modules
	 * should not discard auto generated passwords that are not available from other  
	 * sources.
	 * </p>
	 * @param container container of the secure preferences, might be <code>null</code>
	 * if logout request is not related to a specific container
	 */
	abstract public void logout(IPreferencesContainer container);

	/**
	 * Constructor.
	 */
	public PasswordProvider() {
		// placeholder
	}

	/**
	 * The framework might call this method if it suspects that the password is invalid
	 * (for instance, due to a failed data decryption). 
	 * @param e exception that occurred in the secure preferences processing
	 * @param container container of the secure preferences
	 * @return <code>true</code> if a different password might be provided; <code>false</code>
	 * otherwise. If in doubt, return <code>false</code>
	 */
	public boolean changePassword(Exception e, IPreferencesContainer container) {
		return false;
	}

}
