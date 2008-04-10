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
package org.eclipse.equinox.internal.security.ui.storage;

import org.eclipse.equinox.internal.security.storage.friends.ReEncrypter;
import org.eclipse.equinox.internal.security.ui.nls.SecUIMessages;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

public class ChangePasswordWizardDialog extends WizardDialog {

	final private ReEncrypter reEncrypter;

	private boolean decryptedOK;
	private boolean recodeDone;

	public ChangePasswordWizardDialog(Shell parentShell, ISecurePreferences node, String moduleID) {
		super(parentShell, new ChangePasswordWizard());
		reEncrypter = new ReEncrypter(node, moduleID);
	}

	public boolean isDecryptedOK() {
		return decryptedOK;
	}

	public boolean isRecodeDone() {
		return recodeDone;
	}

	protected void nextPressed() {
		IWizardPage currentPage = getCurrentPage();
		if (currentPage instanceof ChangePasswordWizard.DecodePage) { // decrypt
			if (!reEncrypter.decrypt()) {
				MessageBox messageBox = new MessageBox(getShell(), SWT.YES | SWT.NO | SWT.ICON_WARNING);
				messageBox.setText(SecUIMessages.changePasswordWizardTitle);
				messageBox.setMessage(SecUIMessages.wizardDecodeWarning);
				if (messageBox.open() == SWT.YES) {
					setReturnCode(CANCEL);
					close();
					return;
				}
			}
		} else if (currentPage instanceof ChangePasswordWizard.EncodePage) { // encrypt
			if (!reEncrypter.switchToNewPassword()) {
				MessageBox messageBox = new MessageBox(getShell(), SWT.OK | SWT.ICON_ERROR);
				messageBox.setText(SecUIMessages.changePasswordWizardTitle);
				messageBox.setMessage(SecUIMessages.wizardSwitchError);
				messageBox.open();
				close();
				return;
			}
			reEncrypter.encrypt();
			recodeDone = true;
		}
		super.nextPressed();
	}

}
