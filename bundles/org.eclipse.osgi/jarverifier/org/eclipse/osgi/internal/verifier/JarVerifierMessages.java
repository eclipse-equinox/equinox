/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.internal.verifier;

import org.eclipse.osgi.util.NLS;

public class JarVerifierMessages extends NLS {

	// Jar file is tampered
	public static String Jar_Is_Tampered;
	public static String file_is_removed_from_jar;
	public static String File_In_Jar_Is_Tampered;
	public static String Security_File_Is_Tampered;
	public static String Signature_Not_Verify;
	public static String Signature_Not_Verify_1;

	// Jar file parsing
	public static String SF_File_Parsing_Error;

	// PKCS7 parsing errors
	public static String PKCS7_SignerInfo_Version_Not_Supported;
	public static String PKCS7_Cert_Excep;
	public static String PKCS7_No_Such_Algorithm;
	public static String PKCS7_Parse_Signing_Time;
	public static String PKCS7_Parse_Signing_Time_1;

	// validate certificate chain
	public static String Validate_Certs_Certificate_Exception;

	// Security Exceptions
	public static String Algorithm_Not_Supported;
	public static String No_Such_Algorithm_Excep;
	public static String No_Such_Provider_Excep;
	public static String Invalid_Key_Exception;

	// Certs Trust Determination
	public static String Cert_Verifier_Illegal_Args;
	public static String Cert_Verifier_Not_Trusted;
	public static String Cert_Verifier_Add_Certs;

	//	private static final String BUNDLE_PACKAGE = JarVerifierMessages.class.getPackage().getName() + ".";
	private static final String BUNDLE_PACKAGE = "org.eclipse.osgi.internal.verifier."; //$NON-NLS-1$
	private static final String BUNDLE_FILENAME = "JarVerifierMessages"; //$NON-NLS-1$
	private static final String BUNDLE_NAME = BUNDLE_PACKAGE + BUNDLE_FILENAME;

	static {
		NLS.initializeMessages(BUNDLE_NAME, JarVerifierMessages.class);
	}
}
