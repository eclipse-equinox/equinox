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
	public static String initializing;
	public static String generalDialogTitle;
	public static String secureStorageInitialization;
	public static String initCancelled;

	// login dialog
	public static String buttonLogin;
	public static String buttonExit;
	public static String messageLogin;
	public static String messageLoginChange;
	public static String messageEmptyPassword;
	public static String messageNoMatch;
	public static String labelPassword;
	public static String labelConfirm;
	public static String dialogTitle;
	public static String passwordChangeTitle;
	public static String showPassword;
	public static String noDigestPassword;
	public static String locationGroup;

	// exception handling
	public static String exceptionTitle;
	public static String exceptionDecode;

	// Storage properties page
	public static String selectCipher;
	public static String logoutButton;
	public static String enabledColumn;
	public static String priorityColumn;
	public static String idColumn;
	public static String descriptionColumn;
	public static String defaultGroup;
	public static String changePasswordButton;
	public static String recoverPasswordButton;
	public static String deleteButton;
	public static String confirmDeleteMsg;
	public static String postDeleteTitle;
	public static String postDeleteMsg;
	public static String tabPassword;
	public static String tabAdvanced;
	public static String tabContents;
	public static String saveButton;
	public static String exportButton;
	public static String locationButton;
	public static String providerDescription;
	public static String passwordCacheGroup;
	public static String providerGroup;
	public static String passwordCacheNote;
	public static String providerDetails;

	// secure storage view
	public static String generalTitle;
	public static String keysTable;
	public static String keysColumn;
	public static String valuesColumn;
	public static String nodesContextMenu;
	public static String rootNodeName;
	public static String addNodeCommand;
	public static String addNodeCommandTip;
	public static String removeNodeCommand;
	public static String removeNodeCommandTip;
	public static String refreshNodesCommand;
	public static String refreshNodesCommandTip;
	public static String addValueCommand;
	public static String addValueCommandTmp;
	public static String removeValueCommand;
	public static String removeValueCommandTmp;
	public static String showValueCommand;
	public static String showValueCommandTmp;
	public static String encryptValueCommand;
	public static String encryptValueCommandTmp;
	public static String decryptValueCommand;
	public static String decryptValueCommandTmp;
	public static String failedDecrypt;
	public static String failedEncrypt;

	// new node dialog
	public static String newNodeTitle;
	public static String newNodeLabel;
	public static String newNodeMsg;
	public static String newNodeOK;
	public static String newNodeCancel;
	public static String newNodeInvalid;

	// add value dialog
	public static String addValueTitle;
	public static String addValueKeyLabel;
	public static String addValueValueLabel;
	public static String addValueEncryptLabel;
	public static String addValueOK;
	public static String addValueCancel;
	public static String addValueMsg;
	public static String addValueInvalid;

	// remove value dialog
	public static String removeValueTitle;
	public static String removeValueMsg;

	// show value dialog
	public static String showValueTitle;
	public static String showValueMsg;

	// export dialog
	public static String exportDialogTitle;
	public static String exportDialogMsg;
	public static String exportDialogInvalidMsg;
	public static String exportDialogFileLabel;
	public static String exportDialogBrowse;
	public static String exportDialogOK;
	public static String exportDialogCancel;
	public static String fileSelectTitle;

	// re-code wizard
	public static String changePasswordWizardTitle;
	public static String wizardDecodeTitle;
	public static String wizardDecode;
	public static String wizardDecodeLabel;
	public static String wizardEncodeTitle;
	public static String wizardEncode;
	public static String wizardEncodeLabel;
	public static String wizardDecodeWarning;
	public static String wizardSwitchError;
	public static String wizardDoneTitle;
	public static String wizardDone;
	public static String passwordChangeDone;

	// challenge-response dialog
	public static String pswdRecoveryOptionTitle;
	public static String pswdRecoveryOptionMsg;
	public static String passwordRecoveryTitle;
	public static String passwordRecoveryTitleMsg;
	public static String passwordMsg;
	public static String passwordRecoveryLabel;
	public static String passwordErrMsg;
	public static String passwordGroup;
	public static String passwordQuestion;
	public static String passwordAnswer;
	public static String passwordButtonOK;
	public static String passwordButtonCancel;

	// password recovery dialog
	public static String pswdRecoveryTitle;
	public static String pswRecoveryMsg;
	public static String pswRecoveryWarning;
	public static String pswRecoveryQuestion;
	public static String pswRecoveryAnswer;
	public static String pswRecoveryButtonOK;
	public static String pswRecoveryButtonCancel;
	public static String pswRecoveredMsg;
	public static String pswNotRecoveredMsg;
	public static String pswJobName;

	static {
		// load message values from bundle file
		reloadMessages();
	}

	public static void reloadMessages() {
		NLS.initializeMessages(BUNDLE_NAME, SecUIMessages.class);
	}
}