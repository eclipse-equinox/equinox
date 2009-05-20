/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.security.ui.wizard;

import org.eclipse.equinox.internal.security.ui.SecurityUIMsg;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class CertificateImportConfirmationPage extends WizardPage {

	private CertificateViewer certViewer;
	private Label trustEngineLabel;
	private Composite composite;

	protected CertificateImportConfirmationPage(String pageName) {
		super(pageName);
		setTitle(SecurityUIMsg.WIZARD_IMPORT_CONFIRMATION_TITLE);
		setDescription(SecurityUIMsg.WIZARD_IMPORT_CONFIRMATION_MSG);
	}

	public void createControl(Composite parent) {
		composite = new Composite(parent, SWT.NONE);
		setControl(composite);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.makeColumnsEqualWidth = false;
		layout.marginWidth = 0;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		certViewer = new CertificateViewer(composite);
		trustEngineLabel = new Label(composite, SWT.None);
		trustEngineLabel.setText(""); //$NON-NLS-1$
	}

	public void setVisible(boolean visible) {
		if (visible) {
			CertificateImportWizard certImporWiz = (CertificateImportWizard) getWizard();
			certViewer.setCertificate(certImporWiz.selectCert);
			trustEngineLabel.setText(certImporWiz.selectTrustEngine.getName());
			composite.layout();
		}
		super.setVisible(visible);
	}
}
