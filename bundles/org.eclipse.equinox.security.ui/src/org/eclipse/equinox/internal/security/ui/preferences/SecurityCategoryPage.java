/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.security.ui.preferences;

import org.eclipse.equinox.internal.security.ui.SecurityUIMsg;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.PreferenceLinkArea;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

public class SecurityCategoryPage extends PreferencePage implements IWorkbenchPreferencePage {

	public SecurityCategoryPage() {
		//empty
	}

	public void init(IWorkbench workbench) {
		this.noDefaultAndApplyButton();
	}

	protected Control createContents(Composite parent) {
		Composite pageArea = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		pageArea.setLayout(layout);

		PreferenceLinkArea storageLinkArea = new PreferenceLinkArea(pageArea, SWT.NONE, "org.eclipse.equinox.security.ui.storage", SecurityUIMsg.CATPAGE_LABEL_STORAGE, (IWorkbenchPreferenceContainer) getContainer(), null); //$NON-NLS-1$
		storageLinkArea.getControl().setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));

		Dialog.applyDialogFont(pageArea);
		//PreferenceLinkArea certsLinkArea = new PreferenceLinkArea(pageArea, SWT.NONE, "org.eclipse.equinox.security.ui.certificates", SecurityUIMsg.CATPAGE_LABEL_CERTIFICATES, (IWorkbenchPreferenceContainer) getContainer(), null); //$NON-NLS-1$
		//certsLinkArea.getControl().setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));

		return pageArea;
	}
}
