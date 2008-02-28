/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.security.ui.nls;

import org.eclipse.osgi.util.NLS;

public class SecUIMessages extends NLS {

	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.security.ui.nls.messages"; //$NON-NLS-1$

	public static String enterPasswordLabel;
	public static String enterKeystorePassword;
	public static String callbackhandlerUnavailable;
	public static String passwordLabel;
	public static String passwordRequired;
	public static String noDigestAlgorithm;

	// login dialog
	public static String buttonLogin;
	public static String buttonExit;
	public static String messageLogin;
	public static String messageEmptyPassword;
	public static String messageNoMatch;
	public static String labelPassword;
	public static String labelConfirm;
	public static String dialogTitle;
	public static String showPassword;
	public static String noDigestPassword;

	// exception handling
	public static String exceptionOccured;
	public static String exceptionTitle;
	public static String exceptionDecode;

	static {
		// load message values from bundle file
		reloadMessages();
	}

	public static void reloadMessages() {
		NLS.initializeMessages(BUNDLE_NAME, SecUIMessages.class);
	}
}