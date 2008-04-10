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

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class StoragePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	protected TabPassword passwordTab;
	protected TabAdvanced advancedTab;

	public StoragePreferencePage() {
		//empty
	}

	public void init(IWorkbench workbench) {
		// nothing to do
	}

	protected Control createContents(Composite parent) {
		Composite pageArea = new Composite(parent, SWT.NONE);
		pageArea.setLayout(new RowLayout());

		TabFolder folder = new TabFolder(parent, SWT.TOP);
		passwordTab = new TabPassword(folder, 0, getShell());
		advancedTab = new TabAdvanced(folder, 1, getShell());
		folder.setSelection(0);
		folder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		return folder;
	}

	protected void performDefaults() {
		if (advancedTab != null)
			advancedTab.performDefaults();
		super.performDefaults();
	}

	public boolean performOk() {
		if (advancedTab != null)
			advancedTab.performOk();
		return super.performOk();
	}
}
