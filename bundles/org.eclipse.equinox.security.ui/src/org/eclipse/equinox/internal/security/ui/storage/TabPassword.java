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

import java.util.*;
import java.util.List;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.equinox.internal.security.storage.friends.*;
import org.eclipse.equinox.internal.security.ui.nls.SecUIMessages;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.LayoutConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.osgi.service.prefs.BackingStoreException;

public class TabPassword {

	private static final String PREFERENCES_PLUGIN = "org.eclipse.equinox.security"; //$NON-NLS-1$

	private final static String PASSWORD_RECOVERY_NODE = "/org.eclipse.equinox.secure.storage/recovery/"; //$NON-NLS-1$

	protected Table providerTable;

	protected Button buttonClearPassword;
	protected Button buttonChangePassword;
	protected Button buttonRecoverPassword;

	protected boolean providerModified = false;

	public TabPassword(TabFolder folder, int index, final Shell shell, int minButtonWidth) {
		TabItem tab = new TabItem(folder, SWT.NONE, index);
		tab.setText(SecUIMessages.tabPassword);
		Composite page = new Composite(folder, SWT.NONE);
		tab.setControl(page);

		Composite topPart = new Composite(page, SWT.NONE);
		GridData topData = new GridData(GridData.FILL, GridData.FILL, true, false);
		topData.horizontalSpan = 2;
		topPart.setLayoutData(topData);
		topPart.setLayout(new GridLayout(2, false));
		new Label(topPart, SWT.NONE).setText(SecUIMessages.providerDescription);

		// Left side
		Composite leftPart = new Composite(page, SWT.NONE);
		leftPart.setLayout(new GridLayout());

		new Label(leftPart, SWT.NONE).setText(SecUIMessages.providersTable);
		providerTable = new Table(leftPart, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION | SWT.CHECK);
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
				if ((e.detail & SWT.CHECK) != 0)
					providerModified = true;
				enableButtons();
			}
		});

		leftPart.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));

		// Right side
		Composite rightPart = new Composite(page, SWT.NONE);
		rightPart.setLayout(new GridLayout());

		buttonClearPassword = new Button(rightPart, SWT.NONE);
		buttonClearPassword.setText(SecUIMessages.logoutButton);
		buttonClearPassword.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false));
		buttonClearPassword.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				InternalExchangeUtils.passwordProvidersReset();
				enableLogout();
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
				enableLogout();
			}
		});
		setButtonSize(buttonChangePassword, minButtonWidth);

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
				enableLogout();
			}
		});
		setButtonSize(buttonRecoverPassword, minButtonWidth);

		enableButtons();

		rightPart.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));

		GridLayoutFactory.fillDefaults().margins(LayoutConstants.getMargins()).numColumns(2).generateLayout(page);
	}

	private void fillProviderTable() {
		TableColumn enabledColumn = new TableColumn(providerTable, SWT.CENTER);
		enabledColumn.setText(SecUIMessages.enabledColumn);
		enabledColumn.setWidth(60);

		TableColumn priorityColumn = new TableColumn(providerTable, SWT.LEFT);
		priorityColumn.setText(SecUIMessages.priorityColumn);
		priorityColumn.setWidth(60);

		TableColumn idColumn = new TableColumn(providerTable, SWT.LEFT);
		idColumn.setText(SecUIMessages.idColumn);
		idColumn.setWidth(300);

		List availableModules = InternalExchangeUtils.passwordProvidersFind();
		HashSet disabledModules = getDisabledModules();
		for (Iterator i = availableModules.iterator(); i.hasNext();) {
			PasswordProviderDescription module = (PasswordProviderDescription) i.next();
			TableItem item = new TableItem(providerTable, SWT.LEFT);
			item.setText(new String[] {null, Integer.toString(module.getPriority()), module.getName()});
			item.setData(module.getId());
			if (disabledModules == null)
				item.setChecked(true);
			else
				item.setChecked(!disabledModules.contains(module.getId()));
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
		enableLogout();
	}

	protected HashSet getDisabledModules() {
		IEclipsePreferences node = new ConfigurationScope().getNode(PREFERENCES_PLUGIN);
		String tmp = node.get(IStorageConstants.DISABLED_PROVIDERS_KEY, null);
		if (tmp == null || tmp.length() == 0)
			return null;
		HashSet modules = new HashSet();
		String[] disabledProviders = tmp.split(","); //$NON-NLS-1$
		for (int i = 0; i < disabledProviders.length; i++) {
			modules.add(disabledProviders[i]);
		}
		return modules;
	}

	public void performDefaults() {
		if (providerTable == null)
			return;
		TableItem[] items = providerTable.getItems();
		for (int i = 0; i < items.length; i++) {
			if (!items[i].getChecked()) {
				items[i].setChecked(true);
				providerModified = true;
			}
		}
	}

	public void performOk() {
		if (!providerModified)
			return;
		// save current selection
		StringBuffer tmp = new StringBuffer();
		boolean first = true;
		TableItem[] items = providerTable.getItems();
		for (int i = 0; i < items.length; i++) {
			if (items[i].getChecked())
				continue;
			if (!first)
				tmp.append(',');
			else
				first = false;
			tmp.append((String) items[i].getData());
		}

		IEclipsePreferences node = new ConfigurationScope().getNode(PREFERENCES_PLUGIN);
		if (first)
			node.remove(IStorageConstants.DISABLED_PROVIDERS_KEY);
		else
			node.put(IStorageConstants.DISABLED_PROVIDERS_KEY, tmp.toString());
		try {
			node.flush();
		} catch (BackingStoreException e) {
			// nothing can be done
		}

		// logout so that previously selected default provider is not reused
		InternalExchangeUtils.passwordProvidersReset();
	}

	public void onActivated() {
		enableLogout();
	}

	protected void enableLogout() {
		buttonClearPassword.setEnabled(InternalExchangeUtils.isLoggedIn());
	}

	protected void setButtonSize(Button button, int minButtonWidth) {
		Dialog.applyDialogFont(button);
		GridData data = new GridData();
		Point minButtonSize = button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		data.widthHint = Math.max(minButtonWidth, minButtonSize.x);
		button.setLayoutData(data);
	}

}
