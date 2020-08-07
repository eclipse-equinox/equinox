/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
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
package org.eclipse.equinox.internal.security.ui.storage.view;

import org.eclipse.equinox.internal.security.ui.Activator;
import org.eclipse.equinox.internal.security.ui.nls.SecUIMessages;
import org.eclipse.equinox.internal.security.ui.storage.IStorageConst;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

public class NewNodeDialog extends TitleAreaDialog {

	private static final String HELP_ID = Activator.PLUGIN_ID + ".NewNodeDialog"; //$NON-NLS-1$

	protected Text nodeName;
	protected Button okButton;
	protected String name;

	public NewNodeDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText(SecUIMessages.newNodeTitle);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(shell, HELP_ID);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		okButton = createButton(parent, IDialogConstants.OK_ID, SecUIMessages.newNodeOK, true);
		okButton.setEnabled(false);
		createButton(parent, IDialogConstants.CANCEL_ID, SecUIMessages.newNodeCancel, false);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		setMessage(SecUIMessages.newNodeMsg);

		new Label(composite, SWT.LEFT).setText(SecUIMessages.newNodeLabel);
		nodeName = new Text(composite, SWT.LEFT | SWT.BORDER);
		nodeName.addModifyListener(event -> okButton.setEnabled(validName()));

		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayoutFactory.swtDefaults().generateLayout(composite);

		return composite;
	}

	protected boolean validName() {
		String tmp = nodeName.getText();
		boolean valid;
		if ((tmp == null) || (tmp.length() == 0))
			valid = false;
		else if (tmp.equals(IStorageConst.STORAGE_ID))
			valid = false;
		else
			valid = (tmp.indexOf('/') == -1);
		if (valid)
			setMessage(SecUIMessages.newNodeMsg, IMessageProvider.NONE);
		else
			setMessage(SecUIMessages.newNodeInvalid, IMessageProvider.ERROR);
		return valid;
	}

	@Override
	protected void okPressed() {
		name = nodeName.getText();
		super.okPressed();
	}

	public String getNodeName() {
		return name;
	}

}
