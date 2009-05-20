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
package org.eclipse.equinox.internal.security.ui;

import org.eclipse.osgi.util.NLS;

public class SecurityUIMsg extends NLS {

	public static String IMPORT_FILE;
	public static String WIZARD_SELECT_CERT_FROM_DROP_DOWN;
	public static String WIZARD_BROWSE;

	// confirmation dialog
	public static String CONFIRMATION_DIALOG_TITLE;
	public static String CONFIRMATION_DIALOG_MSG;
	public static String CONFIRMATION_DIALGO_YES;
	public static String CONFIRMATION_DIALGO_NO;

	// msgs for certificate import wizard
	public static String WIZARD_TITLE_FILE_SELECT;
	public static String WIZARD_TITLE_MSG;
	public static String WIZARD_NOT_VALID_CERT;
	public static String WIZARD_ALIAS_NAME_FIELD;
	public static String WIZARD_TARGET_TRUST_ENGINE;
	public static String WIZARD_SELECT_FILE;
	public static String WIZARD_SELECT_CERT;
	public static String WIZARD_IMPORT_CONFIRMATION_TITLE;
	public static String WIZARD_IMPORT_CONFIRMATION_MSG;
	public static String WIZARD_ENGINE_SELECT_TITLE;
	public static String WIZARD_ENGINE_SELECT_MSG;
	public static String WIZARD_PAGE_ENGINE;
	public static String WIZARD_PAGE_CERT_SELECT;
	public static String WIZARD_PAGE_FILE_CERT_SELECT;

	// msgs for required fields in the import wizard
	public static String WIZARD_ERROR_CERT_REQUIRED;
	public static String WIZARD_ERROR_ALIAS_REQUIRED;
	public static String WIZARD_ERROR_ENGINE_REQUIRED;
	public static String WIZARD_ERROR_ALL_REQUIRED;
	public static String WIZARD_ERROR_NO_WRITE_ENGINE;
	public static String WIZARD_FILE_NOT_FOUND;

	public static String STR_CERT_VIEWER_FIELD;
	public static String STR_CERT_VIEWER_VALUE;

	// messages for category page
	public static String CATPAGE_LABEL_STORAGE;
	public static String CATPAGE_LABEL_POLICY;
	public static String CATPAGE_LABEL_CERTIFICATES;
	public static String CATPAGE_LABEL_ADVANCED;

	// messages for default policy page
	//public static String POLPAGE_LABEL_SECTION;
	//public static String POLPAGE_LABEL_TITLE;
	public static String POLPAGE_LABEL_DESC;
	public static String POLPAGE_BUTTON_ALLOW_ANY;
	public static String POLPAGE_BUTTON_ALLOW_ANY_SIGNED;
	public static String POLPAGE_BUTTON_ALLOW_ONLY_TRUSTED;
	public static String POLPAGE_BUTTON_ALLOW_EXPIRED;

	// messages for default certificates page
	public static String CERTPAGE_LABEL_TITLE;
	public static String CERTPAGE_LABEL_LINK;
	public static String CERTPAGE_TABLE_LABEL;
	public static String CERTPAGE_TABLE_HEADER_ISSUEDTO;
	public static String CERTPAGE_TABLE_HEADER_ISSUEDBY;
	public static String CERTPAGE_TABLE_HEADER_PROVIDER;
	public static String CERTPAGE_BUTTON_IMPORT;
	public static String CERTPAGE_BUTTON_EXPORT;
	public static String CERTPAGE_BUTTON_REMOVE;
	public static String CERTPAGE_BUTTON_VIEW;
	public static String CERTPAGE_ERROR_UNKNOWN_FORMAT;

	//messages for advanced page
	public static String ADVPAGE_LABEL_PROVIDER;
	public static String ADVPAGE_LABEL_VERSION;
	public static String ADVPAGE_LABEL_DESCRIPTION;
	public static String ADVPAGE_LABEL_SERVICES;
	public static String ADVPAGE_LABEL_LINK;
	public static String ADVPAGE_LABEL_CLASS;
	public static String ADVPAGE_LABEL_ALIASES;
	public static String ADVPAGE_LABEL_ATTRIBUTES;

	public static String CERTVIEW_LABEL_BASIC;
	public static String CERTVIEW_LABEL_DETAILS;
	public static String CERTVIEW_LABEL_VALIDITY_DATES;

	public static String CERTPROP_X509_FIELD_VALUE;
	public static String CERTPROP_X509_VERSION;
	public static String CERTPROP_X509_SERIAL_NUM;
	public static String CERTPROP_X509_VALID_FROM;
	public static String CERTPROP_X509_VALID_TO;

	public static String CERTPROP_X509_ISSUED_BY;
	public static String CERTPROP_X509_ISSUED_TO;

	public static String CERTPROP_X509_SIG_ALGO;
	public static String CERTPROP_X509_KEY_USAGE;
	public static String CERTPROP_X509_SUB_ALT_NAMES;
	public static String CERTPROP_X509_NAME_CNSTRNTS;
	public static String CERTPROP_X509_CRL_DSTRB_PNTS;
	public static String CERTPROP_X509_CERT_POLICIES;
	public static String CERTPROP_X509_BASIC_CNSTRNTS;
	public static String CERTPROP_X509_THMBPRINT_ALGO;
	public static String CERTPROP_X509_THMBPRINT;
	public static String CERTPROP_X509_THMBPRINTX509_PUBKEY_INFO;
	public static String CERTPROP_X509_THMBPRINTX509_SUBKEY_ID;
	public static String CERTPROP_X509_POLICY_CNSTRNTS;
	public static String CERTPROP_X509_AUTH_KEY_ID;
	public static String CERTPROP_X509_EXKEY_USAGE;

	// X509 Certificate Viewer Labels and data items
	public static String CERTVAL_DEFAULTDIGESTALGO;
	public static String CERTVAL_VERSION;
	public static String CERTVAL_UNDEFINED;

	public static String LABEL_NAMECONSTRAINTS_PERMITTED;
	public static String LABEL_NAMECONSTRAINTS_EXCLUDED;
	public static String LABEL_NAMECONSTRAINTS_ORGANIZATION;
	public static String LABEL_NAMECONSTRAINTS_ISCA;
	public static String LABEL_NAMECONSTRAINTS_PATHLENGTH;
	public static String LABEL_NAMECONSTRAINTS_PATHLENGTH_UNLIMITED;
	public static String LABEL_NAMECONSTRAINTS_NOTCA;

	// These map to specific Extended Key Usage OIDs
	//  Not currently used
	public static String LABEL_EXTKEYUSAGE_SERVERAUTH;
	public static String LABEL_EXTKEYUSAGE_CLIENTAUTH;
	public static String LABEL_EXTKEYUSAGE_CODESIGNING;
	public static String LABEL_EXTKEYUSAGE_EMAILPROTECTION;
	public static String LABEL_EXTKEYUSAGE_IPSEC_ENDENTITY;
	public static String LABEL_EXTKEYUSAGE_IPSEC_TUNNEL;
	public static String LABEL_EXTKEYUSAGE_IPSEC_USER;
	public static String LABEL_EXTKEYUSAGE_TIMESTAMP;
	// End not currently used

	public static String X500_LABEL_CN;
	public static String X500_LABEL_O;
	public static String X500_LABEL_OU;
	public static String X500_LABEL_C;
	public static String X500_LABEL_ST;
	public static String X500_LABEL_L;
	public static String X500_LABEL_STREET;

	// NLS stuff
	private static final String BUNDLE_PACKAGE = "org.eclipse.equinox.internal.security.ui"; //$NON-NLS-1$
	private static final String BUNDLE_FILENAME = "SecurityUIMsg"; //$NON-NLS-1$
	private static final String BUNDLE_NAME = BUNDLE_PACKAGE + "." + BUNDLE_FILENAME; //$NON-NLS-1$

	static {
		NLS.initializeMessages(BUNDLE_NAME, SecurityUIMsg.class);
	}

}
