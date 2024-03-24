/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
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
package org.eclipse.equinox.internal.security.win32.nls;

import org.eclipse.osgi.util.NLS;

public class WinCryptoMessages extends NLS {

	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.security.win32.nls.messages"; //$NON-NLS-1$

	// Windows module
	public static String encryptPasswordFailed;
	public static String decryptPasswordFailed;
	public static String newPasswordGenerated;

	static {
		// load message values from bundle file
		reloadMessages();
	}

	public static void reloadMessages() {
		NLS.initializeMessages(BUNDLE_NAME, WinCryptoMessages.class);
	}
}
