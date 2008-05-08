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
import org.eclipse.jface.layout.*;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
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

	public TabPassword(TabFolder folder, int index, final Shell shell) {
		TabItem tab = new TabItem(folder, SWT.NONE, index);
		tab.setText(SecUIMessages.tabPassword);
		Composite page = new Composite(folder, SWT.NONE);
		page.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
		tab.setControl(page);

		Group passwordGroup = new Group(page, SWT.NONE);
		passwordGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		passwordGroup.setLayout(new GridLayout(2, false));
		passwordGroup.setText(SecUIMessages.passwordCacheGroup);

		buttonClearPassword = new Button(passwordGroup, SWT.PUSH);
		buttonClearPassword.setText(SecUIMessages.logoutButton);
		buttonClearPassword.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false));
		buttonClearPassword.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				InternalExchangeUtils.passwordProvidersReset();
				enableLogout();
			}
		});
		setButtonSize(buttonClearPassword);

		Label passwordNote = new Label(passwordGroup, SWT.WRAP);
		GridData labelData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		labelData.widthHint = 340;
		passwordNote.setLayoutData(labelData);
		passwordNote.setText(SecUIMessages.passwordCacheNote);

		Group providersGroup = new Group(page, SWT.NONE);
		providersGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		providersGroup.setLayout(new GridLayout());
		providersGroup.setText(SecUIMessages.providerGroup);

		Label providersNote = new Label(providersGroup, SWT.WRAP);
		GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gridData.widthHint = 500;
		providersNote.setLayoutData(gridData);
		providersNote.setText(SecUIMessages.providerDescription);

		Composite providersComp = new Composite(providersGroup, SWT.NONE);
		providersComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout gridLayout = new GridLayout(2, false);
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		providersComp.setLayout(gridLayout);

		providerTable = new Table(providersComp, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION | SWT.CHECK);
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
		GridDataFactory.defaultsFor(providerTable).span(1, 2).applyTo(providerTable);

		buttonChangePassword = new Button(providersComp, SWT.PUSH);
		buttonChangePassword.setText(SecUIMessages.changePasswordButton);
		buttonChangePassword.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING, false, false));
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
		setButtonSize(buttonChangePassword);

		buttonRecoverPassword = new Button(providersComp, SWT.PUSH);
		buttonRecoverPassword.setText(SecUIMessages.recoverPasswordButton);
		buttonRecoverPassword.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING, false, false));
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
		setButtonSize(buttonRecoverPassword);

		enableButtons();
		GridLayoutFactory.fillDefaults().margins(LayoutConstants.getSpacing()).generateLayout(page);
	}

	private void fillProviderTable() {
		TableColumn idColumn = new TableColumn(providerTable, SWT.LEAD);
		idColumn.setText(SecUIMessages.idColumn);

		TableColumn priorityColumn = new TableColumn(providerTable, SWT.CENTER);
		priorityColumn.setText(SecUIMessages.priorityColumn);

		List availableModules = InternalExchangeUtils.passwordProvidersFind();
		HashSet disabledModules = getDisabledModules();
		for (Iterator i = availableModules.iterator(); i.hasNext();) {
			PasswordProviderDescription module = (PasswordProviderDescription) i.next();
			TableItem item = new TableItem(providerTable, SWT.NONE);
			item.setText(new String[] {module.getName(), Integer.toString(module.getPriority())});
			item.setData(module.getId());
			if (disabledModules == null)
				item.setChecked(true);
			else
				item.setChecked(!disabledModules.contains(module.getId()));
		}

		TableLayout layout = new TableLayout();
		layout.addColumnData(new ColumnWeightData(5));
		layout.addColumnData(new ColumnWeightData(1));
		providerTable.setLayout(layout);
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

	protected void setButtonSize(Button button) {
		GridDataFactory.defaultsFor(button).align(SWT.FILL, SWT.BEGINNING).grab(false, false).applyTo(button);
	}

}
