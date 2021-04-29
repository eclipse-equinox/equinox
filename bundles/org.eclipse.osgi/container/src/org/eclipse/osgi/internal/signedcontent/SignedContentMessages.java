/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
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

package org.eclipse.osgi.internal.signedcontent;

import org.eclipse.osgi.util.NLS;

public class SignedContentMessages extends NLS {

	public static String Default_Trust_Keystore_Load_Failed;
	public static String Default_Trust_Read_Only;
	public static String Default_Trust_Cert_Not_Found;
	public static String Default_Trust_Existing_Cert;
	public static String Default_Trust_Existing_Alias;

	private static final String BUNDLE_NAME = "org.eclipse.osgi.internal.signedcontent.SignedContentMessages"; //$NON-NLS-1$

	static {
		NLS.initializeMessages(BUNDLE_NAME, SignedContentMessages.class);
	}
}
