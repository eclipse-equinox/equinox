/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.security.tests.storage;

import java.util.concurrent.atomic.AtomicInteger;
import javax.crypto.spec.PBEKeySpec;
import org.eclipse.equinox.security.storage.provider.IPreferencesContainer;
import org.eclipse.equinox.security.storage.provider.PasswordProvider;

/**
 * Password provider which can be provide controlled password generation from
 * tests.
 * <p>
 * Initially the password is 'initialPassword' and on each request to generate a
 * new password it will be 'changedPassword-X' where X is a incremental number.
 */
public class ControlledPasswordProvider extends PasswordProvider {

	// MODULE_ID = lower-case(Bundle-SymbolicName + Extension ID)
	public static final String MODULE_ID = "org.eclipse.equinox.security.controlledpasswordprovider.controlledpasswordprovider";

	private static final String INITIAL_PASSWORD = "initialPassword";
	private static final String CHANGED_PASSWORD = "changedPassword-"; // An incremental number will be appended

	private static PBEKeySpec PASSWORD = new PBEKeySpec(INITIAL_PASSWORD.toCharArray());
	private static AtomicInteger counter = new AtomicInteger(0);

	@Override
	public PBEKeySpec getPassword(IPreferencesContainer container, int passwordType) {

		boolean newPassword = ((passwordType & CREATE_NEW_PASSWORD) != 0);
		boolean passwordChange = ((passwordType & PASSWORD_CHANGE) != 0);

		if (newPassword || passwordChange) {
			PASSWORD = new PBEKeySpec(createNewPassword().toCharArray());
		}

		return PASSWORD;
	}

	@Override
	public boolean retryOnError(Exception e, IPreferencesContainer container) {
		return false;
	}

	private String createNewPassword() {
		return CHANGED_PASSWORD + counter.incrementAndGet();
	}

}
