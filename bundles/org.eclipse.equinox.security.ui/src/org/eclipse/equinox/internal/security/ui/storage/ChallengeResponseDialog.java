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

import org.eclipse.equinox.internal.security.ui.Activator;
import org.eclipse.equinox.internal.security.ui.nls.SecUIMessages;
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

public class ChallengeResponseDialog extends TitleAreaDialog {

	private static final String HELP_ID = Activator.PLUGIN_ID + ".ChallengeResponseDialog"; //$NON-NLS-1$

	protected Text[] questions;
	protected Text[] answers;

	protected String[] questionsText = null;
	protected String[] answersText = null;

	protected Button okButton;
	final protected int size;

	public ChallengeResponseDialog(int numberOfQuestions, Shell parentShell) {
		super(parentShell);
		this.size = numberOfQuestions;
		questions = new Text[this.size];
		answers = new Text[this.size];
	}

	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText(SecUIMessages.passwordRecoveryTitle);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(shell, HELP_ID);
	}

	protected void createButtonsForButtonBar(Composite parent) {
		okButton = createButton(parent, IDialogConstants.OK_ID, SecUIMessages.passwordButtonOK, true);
		okButton.setEnabled(false);
		createButton(parent, IDialogConstants.CANCEL_ID, SecUIMessages.passwordButtonCancel, false);
	}

	protected boolean isResizable() {
		return true;
	}

	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);

		setMessage(SecUIMessages.passwordMsg, IMessageProvider.INFORMATION);

		Label storyLabel = new Label(composite, SWT.LEFT);
		storyLabel.setText(SecUIMessages.passwordRecoveryLabel);
		GridData storyData = new GridData(GridData.FILL, GridData.BEGINNING, true, false);
		storyData.horizontalIndent = 10;
		storyLabel.setLayoutData(storyData);

		for (int i = 0; i < size; i++) {
			Group group = new Group(composite, SWT.NONE);
			group.setText(NLS.bind(SecUIMessages.passwordGroup, Integer.toString(i + 1)));
			group.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
			group.setLayout(new GridLayout());

			new Label(group, SWT.LEFT).setText(SecUIMessages.passwordQuestion);
			questions[i] = new Text(group, SWT.LEFT | SWT.BORDER);
			questions[i].setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
			questions[i].addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent event) {
					validateOK();
				}
			});

			new Label(group, SWT.LEFT).setText(SecUIMessages.passwordAnswer);
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
		for (int i = 0; i < size; i++) {
			if (questions[i] == null)
				continue;
			String question = questions[i].getText();
			if (question == null || question.length() == 0) {
				valid = false;
				break;
			}
			if (answers[i] == null)
				continue;
			String answer = answers[i].getText();
			if (answer == null || answer.length() == 0) {
				valid = false;
				break;
			}
		}
		if (valid)
			setMessage(SecUIMessages.passwordMsg, IMessageProvider.INFORMATION);
		else
			setMessage(SecUIMessages.passwordErrMsg, IMessageProvider.WARNING);
		okButton.setEnabled(valid);
	}

	protected void okPressed() {
		questionsText = new String[size];
		answersText = new String[size];

		for (int i = 0; i < size; i++) {
			questionsText[i] = questions[i].getText();
			answersText[i] = answers[i].getText();
		}
		super.okPressed();
	}

	public String[][] getResult() {
		if (questionsText == null || answersText == null)
			return null;
		return new String[][] {questionsText, answersText};
	}

}
