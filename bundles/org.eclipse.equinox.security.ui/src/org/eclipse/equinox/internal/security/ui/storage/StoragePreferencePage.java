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

import java.net.URL;
import java.util.*;
import java.util.List;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.equinox.internal.security.storage.friends.*;
import org.eclipse.equinox.internal.security.ui.nls.SecUIMessages;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.osgi.service.prefs.BackingStoreException;

public class StoragePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String PREFERENCES_PLUGIN = "org.eclipse.equinox.security"; //$NON-NLS-1$

	private IEclipsePreferences eclipseNode = null;
	private String defaultCipherAlgorithm;

	protected Shell parentShell = null;

	private Map availableCiphers = null;
	private Combo cipherSelector = null;

	public StoragePreferencePage() {
		//empty
	}

	public void init(IWorkbench workbench) {
		eclipseNode = new ConfigurationScope().getNode(PREFERENCES_PLUGIN);
		defaultCipherAlgorithm = eclipseNode.get(IStorageConstants.CIPHER_KEY, IStorageConstants.DEFAULT_CIPHER);
		availableCiphers = InternalExchangeUtils.ciphersDetectAvailable();
	}

	protected Control createContents(Composite parent) {
		parentShell = parent.getShell();

		Composite pageArea = new Composite(parent, SWT.NONE);
		pageArea.setLayout(new RowLayout());

		// Default preferences group
		Group defaultPrefsGroup = new Group(pageArea, SWT.NONE);
		defaultPrefsGroup.setText(SecUIMessages.defaultGroup);
		defaultPrefsGroup.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
		defaultPrefsGroup.setLayout(new GridLayout());

		Button buttonChangePassword = new Button(defaultPrefsGroup, SWT.NONE);
		buttonChangePassword.setText(SecUIMessages.changePasswordButton);
		buttonChangePassword.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false));
		buttonChangePassword.addSelectionListener(new SelectionListener() {

			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			public void widgetSelected(SelectionEvent e) {
				Shell shell = (parentShell == null) ? new Shell() : parentShell;
				ISecurePreferences rootNode = SecurePreferencesFactory.getDefault();
				ChangePasswordWizardDialog dialog = new ChangePasswordWizardDialog(shell, rootNode);
				dialog.open();
			}
		});

		Button buttonRecoverPassword = new Button(defaultPrefsGroup, SWT.NONE);
		buttonRecoverPassword.setText(SecUIMessages.recoverPasswordButton);
		buttonRecoverPassword.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false));
		buttonRecoverPassword.addSelectionListener(new SelectionListener() {

			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			public void widgetSelected(SelectionEvent e) {
				ISecurePreferences rootNode = SecurePreferencesFactory.getDefault();
				String[] questions = InternalExchangeUtils.getPasswordRecoveryQuestions(rootNode);
				if (questions.length == 0)
					return; // no password recovery questions were setup
				Shell shell = (parentShell == null) ? new Shell() : parentShell;
				PasswordRecoveryDialog dialog = new PasswordRecoveryDialog(questions, shell);
				dialog.open();
			}
		});

		Button buttonDetele = new Button(defaultPrefsGroup, SWT.NONE);
		buttonDetele.setText(SecUIMessages.deleteButton);
		buttonDetele.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false));
		buttonDetele.addSelectionListener(new SelectionListener() {

			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			public void widgetSelected(SelectionEvent e) {
				deteleDefaultStorage();
			}
		});

		// Providers group of the dialog
		Group providersGroup = new Group(pageArea, SWT.NONE);
		providersGroup.setText(SecUIMessages.providersGroup);
		providersGroup.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
		providersGroup.setLayout(new GridLayout(2, false));

		new Label(providersGroup, SWT.NONE).setText(SecUIMessages.providersTable);

		Button buttonClearPassword = new Button(providersGroup, SWT.NONE);
		buttonClearPassword.setText(SecUIMessages.logoutButton);
		buttonClearPassword.setLayoutData(new GridData(GridData.END, GridData.CENTER, false, false));
		buttonClearPassword.addSelectionListener(new SelectionListener() {

			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			public void widgetSelected(SelectionEvent e) {
				InternalExchangeUtils.passwordProvidersLogout();
			}
		});

		Table providerTable = new Table(providersGroup, SWT.BORDER);
		GridData tableData = new GridData(GridData.FILL, GridData.FILL, true, true);
		tableData.horizontalSpan = 2;
		providerTable.setLayoutData(tableData);
		providerTable.setLinesVisible(true);
		providerTable.setHeaderVisible(true);
		fillProviderTable(providerTable);

		// "Advanced" part of the dialog
		Group advancedGroup = new Group(pageArea, SWT.NONE);
		advancedGroup.setText(SecUIMessages.advancedGroup);
		advancedGroup.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
		advancedGroup.setLayout(new GridLayout());

		Label cipherLabel = new Label(advancedGroup, SWT.NONE);
		cipherLabel.setText(SecUIMessages.selectCipher);

		cipherSelector = new Combo(advancedGroup, SWT.READ_ONLY | SWT.DROP_DOWN);
		GridData gridDataSelector = new GridData(GridData.FILL, GridData.BEGINNING, true, false);
		cipherSelector.setLayoutData(gridDataSelector);
		fillCipherSelector();

		pageArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayoutFactory.swtDefaults().generateLayout(pageArea);

		return pageArea;
	}

	private void fillCipherSelector() {
		int position = 0;
		for (Iterator i = availableCiphers.keySet().iterator(); i.hasNext();) {
			String cipherAlgorithm = (String) i.next();
			cipherSelector.add(cipherAlgorithm, position);
			if (defaultCipherAlgorithm.equals(cipherAlgorithm))
				cipherSelector.select(position);
			position++;
		}
	}

	private void fillProviderTable(Table providerTable) {
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
		}
	}

	protected void deteleDefaultStorage() {
		URL location = InternalExchangeUtils.defaultStorageLocation();
		if (location == null)
			return;
		MessageBox messageBox = new MessageBox(new Shell(), SWT.YES | SWT.NO);
		messageBox.setText(location.getFile().toString());
		messageBox.setMessage(SecUIMessages.confirmDeleteMsg);
		if (messageBox.open() != SWT.YES)
			return;

		// clear the data structure itself in case somebody holds on to it
		ISecurePreferences defaultStorage = SecurePreferencesFactory.getDefault();
		defaultStorage.clear();
		defaultStorage.removeNode();

		// clear it from the list of open storages, delete the file 
		InternalExchangeUtils.defaultStorageDelete();

		// suggest restart in case somebody holds on to the deleted storage
		MessageBox postDeletionBox = new MessageBox(new Shell(), SWT.OK);
		postDeletionBox.setText(SecUIMessages.postDeleteTitle);
		postDeletionBox.setMessage(SecUIMessages.postDeleteMsg);
		postDeletionBox.open();
	}

	protected void performDefaults() {
		eclipseNode.put(IStorageConstants.CIPHER_KEY, IStorageConstants.DEFAULT_CIPHER);
		eclipseNode.put(IStorageConstants.KEY_FACTORY_KEY, IStorageConstants.DEFAULT_KEY_FACTORY);
		try {
			eclipseNode.flush();
		} catch (BackingStoreException e) {
			// nothing can be done
		}

		for (int i = 0; i < cipherSelector.getItemCount(); i++) {
			String item = cipherSelector.getItem(i);
			if (item.equals(IStorageConstants.DEFAULT_CIPHER))
				cipherSelector.select(i);
		}
		super.performDefaults();
	}

	public boolean performOk() {
		String selectedCipherAlgorithm = cipherSelector.getText();
		if (!defaultCipherAlgorithm.equals(selectedCipherAlgorithm)) {
			eclipseNode.put(IStorageConstants.CIPHER_KEY, selectedCipherAlgorithm);
			String keyFactory = (String) availableCiphers.get(selectedCipherAlgorithm);
			eclipseNode.put(IStorageConstants.KEY_FACTORY_KEY, keyFactory);
			defaultCipherAlgorithm = selectedCipherAlgorithm;
			try {
				eclipseNode.flush();
			} catch (BackingStoreException e) {
				// nothing can be done
			}
		}
		return super.performOk();
	}
}
