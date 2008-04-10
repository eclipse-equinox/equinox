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

import java.util.Iterator;
import java.util.List;
import org.eclipse.equinox.internal.security.storage.friends.InternalExchangeUtils;
import org.eclipse.equinox.internal.security.storage.friends.PasswordProviderDescription;
import org.eclipse.equinox.internal.security.ui.nls.SecUIMessages;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.LayoutConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

public class TabPassword {

	private final static String PASSWORD_RECOVERY_NODE = "/org.eclipse.equinox.secure.storage/recovery/"; //$NON-NLS-1$

	protected Table providerTable;

	protected Button buttonChangePassword;
	protected Button buttonRecoverPassword;

	public TabPassword(TabFolder folder, int index, final Shell shell) {
		TabItem tab = new TabItem(folder, SWT.NONE, index);
		tab.setText(SecUIMessages.tabPassword);
		Composite page = new Composite(folder, SWT.NONE);
		tab.setControl(page);

		// Left side
		Composite leftPart = new Composite(page, SWT.NONE);
		leftPart.setLayout(new GridLayout());

		new Label(leftPart, SWT.NONE).setText(SecUIMessages.providersTable);
		providerTable = new Table(leftPart, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION); //  | SWT.CHECK
		GridData tableData = new GridData(GridData.FILL, GridData.FILL, true, true);
		providerTable.setLayoutData(tableData);
		providerTable.setLinesVisible(true);
		providerTable.setHeaderVisible(true);
		fillProviderTable();

		providerTable.addSelectionListener(new SelectionListener() {

			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}

			public void widgetSelected(SelectionEvent e) {
				enableButtons();
			}
		});

		leftPart.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));

		// Right side
		Composite rightPart = new Composite(page, SWT.NONE);
		rightPart.setLayout(new GridLayout());

		Button buttonClearPassword = new Button(rightPart, SWT.NONE);
		buttonClearPassword.setText(SecUIMessages.logoutButton);
		buttonClearPassword.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false));
		buttonClearPassword.addSelectionListener(new SelectionListener() {

			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			public void widgetSelected(SelectionEvent e) {
				InternalExchangeUtils.passwordProvidersLogout();
			}
		});

		buttonChangePassword = new Button(rightPart, SWT.NONE);
		buttonChangePassword.setText(SecUIMessages.changePasswordButton);
		buttonChangePassword.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false));
		buttonChangePassword.addSelectionListener(new SelectionListener() {

			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			public void widgetSelected(SelectionEvent e) {
				String moduleID = getSelectedModuleID();
				ISecurePreferences rootNode = SecurePreferencesFactory.getDefault();
				ChangePasswordWizardDialog dialog = new ChangePasswordWizardDialog(shell, rootNode, moduleID);
				dialog.open();
			}
		});

		buttonRecoverPassword = new Button(rightPart, SWT.NONE);
		buttonRecoverPassword.setText(SecUIMessages.recoverPasswordButton);
		buttonRecoverPassword.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false));
		buttonRecoverPassword.addSelectionListener(new SelectionListener() {

			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			public void widgetSelected(SelectionEvent e) {
				String moduleID = getSelectedModuleID();
				ISecurePreferences rootNode = SecurePreferencesFactory.getDefault();
				String[] questions = InternalExchangeUtils.getPasswordRecoveryQuestions(rootNode, moduleID);
				if (questions.length == 0)
					return; // no password recovery questions were setup
				PasswordRecoveryDialog dialog = new PasswordRecoveryDialog(questions, shell, moduleID);
				dialog.open();
			}
		});

		enableButtons();

		rightPart.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));

		GridLayoutFactory.fillDefaults().margins(LayoutConstants.getMargins()).numColumns(2).generateLayout(page);
	}

	private void fillProviderTable() {
		TableColumn priorityColumn = new TableColumn(providerTable, SWT.LEFT);
		priorityColumn.setText(SecUIMessages.priorityColumn);
		priorityColumn.setWidth(70);

		TableColumn idColumn = new TableColumn(providerTable, SWT.LEFT);
		idColumn.setText(SecUIMessages.idColumn);
		idColumn.setWidth(300);

		List availableModules = InternalExchangeUtils.passwordProvidersFind();
		for (Iterator i = availableModules.iterator(); i.hasNext();) {
			PasswordProviderDescription module = (PasswordProviderDescription) i.next();
			TableItem item = new TableItem(providerTable, SWT.LEFT);
			item.setText(new String[] {Integer.toString(module.getPriority()), module.getId()});
			item.setData(module.getId());
		}
	}

	protected String getSelectedModuleID() {
		if (providerTable == null)
			return null;
		TableItem[] items = providerTable.getSelection();
		if (items.length == 0)
			return null;
		return (String) items[0].getData();
	}

	protected void enableButtons() {
		String moduleID = getSelectedModuleID();
		if (moduleID == null) { // nothing selected
			buttonChangePassword.setEnabled(false);
			buttonRecoverPassword.setEnabled(false);
		} else {
			buttonChangePassword.setEnabled(true);

			ISecurePreferences rootNode = SecurePreferencesFactory.getDefault();
			String path = PASSWORD_RECOVERY_NODE + moduleID;
			boolean recoveryAvailable = rootNode.nodeExists(path);
			buttonRecoverPassword.setEnabled(recoveryAvailable);
		}
	}

}
