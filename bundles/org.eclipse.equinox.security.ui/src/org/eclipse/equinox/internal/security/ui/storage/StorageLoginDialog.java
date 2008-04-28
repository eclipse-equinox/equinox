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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.spec.PBEKeySpec;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.internal.security.ui.Activator;
import org.eclipse.equinox.internal.security.ui.nls.SecUIMessages;
import org.eclipse.equinox.security.storage.EncodingUtils;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

public class StorageLoginDialog extends TitleAreaDialog {

	private static final String DIALOG_SETTINGS_SECTION_NEW = "StorageLoginDialogNew"; //$NON-NLS-1$
	private static final String DIALOG_SETTINGS_SECTION_OLD = "StorageLoginDialogOld"; //$NON-NLS-1$

	private static final String HELP_ID = Activator.PLUGIN_ID + ".StorageLoginDialog"; //$NON-NLS-1$

	private static final ImageDescriptor dlgImageDescriptor = ImageDescriptor.createFromFile(StorageLoginDialog.class, "/icons/storage/login_wiz.png"); //$NON-NLS-1$

	private static final String DIGEST_ALGORITHM = "MD5"; //$NON-NLS-1$

	protected Text password;
	protected Text confirm;

	protected Button showPassword;
	protected Button okButton;

	protected PBEKeySpec generatedPassword;

	final protected boolean confirmPassword;
	final protected boolean passwordChange;
	final protected String location;

	private Image dlgTitleImage = null;

	public StorageLoginDialog(boolean confirmPassword, boolean passwordChange, String location) {
		super(StorageUtils.getShell());
		this.confirmPassword = confirmPassword;
		this.passwordChange = passwordChange;
		this.location = location;
	}

	public PBEKeySpec getGeneratedPassword() {
		return generatedPassword;
	}

	protected void createButtonsForButtonBar(Composite parent) {
		okButton = createButton(parent, IDialogConstants.OK_ID, SecUIMessages.buttonLogin, true);
		okButton.setEnabled(false);
		createButton(parent, IDialogConstants.CANCEL_ID, SecUIMessages.buttonExit, false);
	}

	protected IDialogSettings getDialogBoundsSettings() {
		IDialogSettings settings = Activator.getDefault().getDialogSettings();
		String settingsID = (confirmPassword) ? DIALOG_SETTINGS_SECTION_NEW : DIALOG_SETTINGS_SECTION_OLD;
		IDialogSettings section = settings.getSection(settingsID);
		if (section == null)
			section = settings.addNewSection(settingsID);
		return section;
	}

	protected boolean isResizable() {
		return true;
	}

	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText(SecUIMessages.generalDialogTitle);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(shell, HELP_ID);
	}

	protected Control createContents(Composite parent) {
		Control contents = super.createContents(parent);
		dlgTitleImage = dlgImageDescriptor.createImage();
		setTitleImage(dlgTitleImage);
		return contents;
	}

	protected Control createDialogArea(Composite parent) {
		Composite compositeTop = (Composite) super.createDialogArea(parent);

		String titleMsg;
		if (confirmPassword)
			titleMsg = SecUIMessages.passwordChangeTitle;
		else if (passwordChange)
			titleMsg = SecUIMessages.messageLoginChange;
		else
			titleMsg = SecUIMessages.dialogTitle;
		setTitle(titleMsg);

		Composite composite = new Composite(compositeTop, SWT.NONE);

		new Label(composite, SWT.LEFT).setText(SecUIMessages.labelPassword);
		password = new Text(composite, SWT.LEFT | SWT.BORDER);
		password.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent event) {
				okButton.setEnabled(validatePassword());
			}
		});

		if (confirmPassword) {
			new Label(composite, SWT.LEFT).setText(SecUIMessages.labelConfirm);
			confirm = new Text(composite, SWT.LEFT | SWT.BORDER);
			confirm.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent event) {
					okButton.setEnabled(validatePassword());
				}
			});
		} else
			confirm = null;

		new Label(composite, SWT.LEFT); // filler
		showPassword = new Button(composite, SWT.CHECK | SWT.RIGHT);
		showPassword.setText(SecUIMessages.showPassword);
		showPassword.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				passwordVisibility();
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				passwordVisibility();
			}
		});
		showPassword.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		// by default don't display password as clear text
		showPassword.setSelection(false);
		passwordVisibility();

		if (location != null) {
			Group locationGroup = new Group(composite, SWT.NONE);
			locationGroup.setText(SecUIMessages.locationGroup);
			GridData groupData = new GridData(SWT.FILL, SWT.FILL, true, true);
			groupData.horizontalSpan = 2;
			locationGroup.setLayoutData(groupData);
			locationGroup.setLayout(new GridLayout());

			Label locationLabel = new Label(locationGroup, SWT.WRAP);
			locationLabel.setText(new Path(location).toOSString());
		}

		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayoutFactory.swtDefaults().numColumns(2).generateLayout(composite);

		return compositeTop;
	}

	protected void passwordVisibility() {
		boolean selected = showPassword.getSelection();
		if (selected) {
			password.setEchoChar('\0');
			if (confirm != null)
				confirm.setEchoChar('\0');
		} else {
			password.setEchoChar('*');
			if (confirm != null)
				confirm.setEchoChar('*');
		}
	}

	protected boolean validatePassword() {
		String password1 = password.getText();
		if ((password1 == null) || (password1.length() == 0)) {
			setMessage(SecUIMessages.messageEmptyPassword, IMessageProvider.ERROR);
			return false;
		}
		if (confirm != null) {
			String password2 = confirm.getText();
			if (!password1.equals(password2)) {
				setMessage(SecUIMessages.messageNoMatch, IMessageProvider.WARNING);
				return false;
			}
		}
		setMessage("", IMessageProvider.NONE); //$NON-NLS-1$
		return true;
	}

	protected void okPressed() {
		String internalPassword;
		try {
			// normally use digest of what was entered
			MessageDigest digest = MessageDigest.getInstance(DIGEST_ALGORITHM);
			byte[] digested = digest.digest(new String(password.getText()).getBytes());
			internalPassword = EncodingUtils.encodeBase64(digested);
		} catch (NoSuchAlgorithmException e) {
			// just use the text as is
			Activator.log(IStatus.WARNING, SecUIMessages.noDigestPassword, new Object[] {DIGEST_ALGORITHM}, e);
			internalPassword = password.getText();
		}
		generatedPassword = new PBEKeySpec(internalPassword.toCharArray());

		super.okPressed();
	}
}
