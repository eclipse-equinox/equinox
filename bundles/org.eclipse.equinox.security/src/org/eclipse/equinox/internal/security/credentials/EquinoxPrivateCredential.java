/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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
package org.eclipse.equinox.internal.security.credentials;

import javax.crypto.spec.PBEKeySpec;
import org.eclipse.equinox.security.auth.credentials.IPrivateCredential;

public class EquinoxPrivateCredential implements IPrivateCredential {

	final private PBEKeySpec key;
	final private String loginModuleID;

	public EquinoxPrivateCredential(PBEKeySpec privateKey, String loginModuleID) {
		this.key = privateKey;
		this.loginModuleID = loginModuleID;
	}

	public PBEKeySpec getPrivateKey() {
		return key;
	}

	public String getProviderID() {
		return loginModuleID;
	}

	public void clear() {
		if (key != null)
			key.clearPassword();
	}

}
