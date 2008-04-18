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
package org.eclipse.equinox.internal.security.ui.storage.view;

import org.eclipse.equinox.internal.security.ui.Activator;
import org.eclipse.equinox.internal.security.ui.nls.SecUIMessages;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

public class NewValueDialog extends TitleAreaDialog {

	private static final String HELP_ID = Activator.PLUGIN_ID + ".NewValueDialog"; //$NON-NLS-1$

	private static final ImageDescriptor dlgImageDescriptor = ImageDescriptor.createFromFile(NodesView.class, "/icons/storage/new_value_wiz.png"); //$NON-NLS-1$

	private final String[] existingKeys;

	protected Text keyText;
	protected Text valueText;
	protected Button encryptButton;
	protected Button okButton;
	protected String key;
	protected String value;
	protected boolean encrypt;

	private Image dlgTitleImage = null;

	public NewValueDialog(String[] existingKeys, Shell parentShell) {
		super(parentShell);
		this.existingKeys = existingKeys;
	}

	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText(SecUIMessages.generalTitle);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(shell, HELP_ID);
	}

	protected void createButtonsForButtonBar(Composite parent) {
		okButton = createButton(parent, IDialogConstants.OK_ID, SecUIMessages.addValueOK, true);
		okButton.setEnabled(false);
		createButton(parent, IDialogConstants.CANCEL_ID, SecUIMessages.addValueCancel, false);
	}

	protected Control createContents(Composite parent) {
		Control contents = super.createContents(parent);
		setTitle(SecUIMessages.addValueTitle);
		setMessage(SecUIMessages.addValueMsg);
		dlgTitleImage = dlgImageDescriptor.createImage();
		setTitleImage(dlgTitleImage);
		return contents;
	}

	protected Control createDialogArea(Composite parent) {
		Composite compositeTop = (Composite) super.createDialogArea(parent);
		Composite composite = new Composite(compositeTop, SWT.NONE);

		new Label(composite, SWT.LEFT).setText(SecUIMessages.addValueKeyLabel);
		keyText = new Text(composite, SWT.LEFT | SWT.BORDER);
		keyText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent event) {
				okButton.setEnabled(validName());
			}
		});

		new Label(composite, SWT.LEFT).setText(SecUIMessages.addValueValueLabel);
		valueText = new Text(composite, SWT.LEFT | SWT.BORDER);

		encryptButton = new Button(composite, SWT.CHECK);
		encryptButton.setText(SecUIMessages.addValueEncryptLabel);
		encryptButton.setSelection(true); // encrypt by default

		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayoutFactory.swtDefaults().generateLayout(composite);

		return composite;
	}

	protected boolean validName() {
		String tmp = keyText.getText();
		boolean valid;
		if ((tmp == null) || (tmp.length() == 0))
			valid = false;
		else {
			valid = true;
			for (int i = 0; i < existingKeys.length; i++) {
				if (existingKeys[i].equals(tmp)) {
					valid = false;
					break;
				}
			}
			valid = (tmp.indexOf('/') == -1);
		}
		if (valid)
			setMessage(SecUIMessages.addValueMsg, IMessageProvider.NONE);
		else
			setMessage(SecUIMessages.addValueInvalid, IMessageProvider.ERROR);
		return valid;
	}

	protected void okPressed() {
		key = keyText.getText();
		value = valueText.getText();
		encrypt = encryptButton.getSelection();
		super.okPressed();
	}

	public String getKey() {
		return key;
	}

	public String getValue() {
		return value;
	}

	public boolean encrypt() {
		return encrypt;
	}

}
