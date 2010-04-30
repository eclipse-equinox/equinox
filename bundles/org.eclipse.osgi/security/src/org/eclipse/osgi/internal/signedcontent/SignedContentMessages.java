/*******************************************************************************
 * Copyright (c) 2006, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.internal.signedcontent;

import org.eclipse.osgi.util.NLS;

public class SignedContentMessages extends NLS {

	// Jar file is tampered
	public static String file_is_removed_from_jar;
	public static String File_In_Jar_Is_Tampered;
	public static String Security_File_Is_Tampered;
	public static String Signature_Not_Verify;

	// Jar file parsing
	public static String SF_File_Parsing_Error;

	// PKCS7 parsing errors
	public static String PKCS7_SignerInfo_Version_Not_Supported;
	public static String PKCS7_Invalid_File;
	public static String PKCS7_Parse_Signing_Time;

	// Security Exceptions
	public static String Algorithm_Not_Supported;

	public static String Factory_SignedContent_Error;

	public static String Default_Trust_Keystore_Load_Failed;
	public static String Default_Trust_Read_Only;
	public static String Default_Trust_Cert_Not_Found;
	public static String Default_Trust_Existing_Cert;
	public static String Default_Trust_Existing_Alias;

	//	private static final String BUNDLE_PACKAGE = SignedContentMessages.class.getPackage().getName() + ".";
	private static final String BUNDLE_PACKAGE = "org.eclipse.osgi.internal.signedcontent."; //$NON-NLS-1$
	private static final String BUNDLE_FILENAME = "SignedContentMessages"; //$NON-NLS-1$
	private static final String BUNDLE_NAME = BUNDLE_PACKAGE + BUNDLE_FILENAME;

	static {
		NLS.initializeMessages(BUNDLE_NAME, SignedContentMessages.class);
	}
}
