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

import org.eclipse.equinox.internal.security.storage.friends.InternalExchangeUtils;
import org.eclipse.equinox.internal.security.ui.nls.SecUIMessages;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

public class PasswordRecoveryDialog extends TitleAreaDialog {

	private static final String HELP_ID = "org.eclipse.equinox.security.ui.password_recovery_dialog"; //$NON-NLS-1$

	protected Text[] answers;
	protected String moduleID;
	protected String[] answersText = null;
	protected Button okButton;

	final protected String[] questionsText;

	public PasswordRecoveryDialog(String[] questionsText, Shell parentShell, String moduleID) {
		super(parentShell);
		this.questionsText = questionsText;
		this.moduleID = moduleID;
		answers = new Text[questionsText.length];
	}

	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText(SecUIMessages.pswdRecoveryTitle);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(shell, HELP_ID);
	}

	protected void createButtonsForButtonBar(Composite parent) {
		okButton = createButton(parent, IDialogConstants.OK_ID, SecUIMessages.pswRecoveryButtonOK, true);
		okButton.setEnabled(false);
		createButton(parent, IDialogConstants.CANCEL_ID, SecUIMessages.pswRecoveryButtonCancel, false);
	}

	protected boolean isResizable() {
		return true;
	}

	protected Control createDialogArea(Composite parent) {
		Composite compositeTop = (Composite) super.createDialogArea(parent);
		Composite composite = new Composite(compositeTop, SWT.NONE);

		setMessage(SecUIMessages.pswRecoveryMsg, IMessageProvider.INFORMATION);

		for (int i = 0; i < questionsText.length; i++) {
			Group group = new Group(composite, SWT.NONE);
			group.setText(NLS.bind(SecUIMessages.passwordGroup, Integer.toString(i + 1)));
			group.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
			group.setLayout(new GridLayout());

			String question = NLS.bind(SecUIMessages.pswRecoveryQuestion, questionsText[i]);
			new Label(group, SWT.LEFT).setText(question);
			answers[i] = new Text(group, SWT.LEFT | SWT.BORDER);
			answers[i].setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
			answers[i].addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent event) {
					validateOK();
				}
			});
		}

		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayoutFactory.swtDefaults().generateLayout(composite);

		return composite;
	}

	protected void validateOK() {
		boolean valid = true;
		for (int i = 0; i < questionsText.length; i++) {
			if (answers[i] == null)
				continue;
			String question = answers[i].getText();
			if (question == null || question.length() == 0) {
				valid = false;
				break;
			}
		}
		if (valid)
			setMessage(SecUIMessages.pswRecoveryMsg, IMessageProvider.INFORMATION);
		else
			setMessage(SecUIMessages.pswRecoveryWarning, IMessageProvider.WARNING);
		okButton.setEnabled(valid);
	}

	protected void okPressed() {
		answersText = new String[questionsText.length];
		for (int i = 0; i < questionsText.length; i++) {
			answersText[i] = answers[i].getText();
		}

		String password = InternalExchangeUtils.recoverPassword(answersText, SecurePreferencesFactory.getDefault(), moduleID);
		if (password == null) {
			MessageBox prompt = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.YES | SWT.NO);
			prompt.setText(SecUIMessages.pswdRecoveryTitle);
			prompt.setMessage(SecUIMessages.pswNotRecoveredMsg);
			if (prompt.open() == SWT.YES)
				return;
		} else { // even in UI case we use digested and encoded password - makes no sense to show it
			MessageBox prompt = new MessageBox(getShell(), SWT.ICON_INFORMATION | SWT.OK);
			prompt.setText(SecUIMessages.pswdRecoveryTitle);
			prompt.setMessage(SecUIMessages.pswRecoveredMsg);
			prompt.open();
		}

		super.okPressed();
	}

	public String[] getResult() {
		return answersText;
	}

}
