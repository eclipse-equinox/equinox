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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;

public class StoragePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private static final String HELP_ID = "org.eclipse.equinox.security.ui.sec_storage_preferences_context"; //$NON-NLS-1$

	protected TabPassword passwordTab;
	protected TabContents contentsTab;
	protected TabAdvanced advancedTab;

	public StoragePreferencePage() {
		//empty
	}

	public void init(IWorkbench workbench) {
		// nothing to do
	}

	protected Control createContents(Composite parent) {

		final TabFolder folder = new TabFolder(parent, SWT.TOP);

		GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.horizontalSpan = 1;
		folder.setLayoutData(gd);

		passwordTab = new TabPassword(folder, 0, getShell());
		contentsTab = new TabContents(folder, 1, getShell());
		advancedTab = new TabAdvanced(folder, 2, getShell());
		folder.setSelection(0);

		folder.addSelectionListener(new SelectionListener() {

			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing to do
			}

			public void widgetSelected(SelectionEvent e) {
				int i = folder.getSelectionIndex();
				if (i == 0 && passwordTab != null) // password page
					passwordTab.onActivated();
			}
		});
		Dialog.applyDialogFont(folder);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), HELP_ID);
		return folder;
	}

	protected void performDefaults() {
		if (passwordTab != null)
			passwordTab.performDefaults();
		if (advancedTab != null)
			advancedTab.performDefaults();
		super.performDefaults();
	}

	public boolean performOk() {
		if (passwordTab != null)
			passwordTab.performOk();
		if (advancedTab != null)
			advancedTab.performOk();
		return super.performOk();
	}
}
