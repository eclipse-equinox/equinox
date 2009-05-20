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

import java.io.File;
import org.eclipse.equinox.internal.security.ui.SecurityUIMsg;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

public class CertificateImportFileSelectPage extends WizardPage implements Listener {

	private Button browseDirectoriesButton;
	private Text filePathField;

	protected CertificateImportFileSelectPage(String pageName) {
		super(pageName);
		setTitle(pageName);
		setDescription(SecurityUIMsg.WIZARD_TITLE_FILE_SELECT);
	}

	public void createControl(Composite parent) {
		Composite certSelectComposite = new Composite(parent, SWT.NONE);
		setControl(certSelectComposite);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.makeColumnsEqualWidth = false;
		layout.marginWidth = 0;
		certSelectComposite.setLayout(layout);
		certSelectComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// new project from directory radio button
		Label certSelectLabel = new Label(certSelectComposite, SWT.RADIO);
		certSelectLabel.setText(SecurityUIMsg.WIZARD_SELECT_FILE);

		// project location entry field
		this.filePathField = new Text(certSelectComposite, SWT.BORDER);

		this.filePathField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));

		// browse button
		browseDirectoriesButton = new Button(certSelectComposite, SWT.PUSH);
		browseDirectoriesButton.setText(SecurityUIMsg.WIZARD_BROWSE);
		browseDirectoriesButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleLocationFileButtonPressed();
			}
		});

		addListeners();
	}

	private void addListeners() {
		browseDirectoriesButton.addListener(SWT.Selection, this);
		filePathField.addListener(SWT.KeyUp, this);
	}

	/**
	 * The browse button has been selected. Select the location.
	 */
	protected void handleLocationFileButtonPressed() {
		final FileDialog certFileDialog = new FileDialog(filePathField.getShell(), SWT.OPEN);
		certFileDialog.setText(SecurityUIMsg.WIZARD_SELECT_FILE);
		certFileDialog.setFilterPath(filePathField.getText());
		certFileDialog.setFilterExtensions(new String[] {"*.cer", "*.p7b", "*.der"}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		String selectedCert = certFileDialog.open();
		if (selectedCert != null) {
			filePathField.setText(selectedCert);
		}
	}

	public boolean canFlipToNextPage() {
		return (filePathField.getText().length() < 1 || null != getErrorMessage()) ? false : true;
	}

	public void handleEvent(Event e) {
		if (e.widget == browseDirectoriesButton || e.widget == filePathField)
			if (filePathField.getText().length() < 1)
				setErrorMessage(SecurityUIMsg.WIZARD_ERROR_CERT_REQUIRED);
			else
				setErrorMessage(null);
		getWizard().getContainer().updateButtons();
	}

	public IWizardPage getNextPage() {
		File file = new File(filePathField.getText().trim());
		if (file.isDirectory() || !file.exists()) {
			setErrorMessage(NLS.bind(SecurityUIMsg.WIZARD_FILE_NOT_FOUND, new String[] {filePathField.getText()}));
			return null;
		}
		saveFileSelection();

		return super.getNextPage();
	}

	private void saveFileSelection() {
		CertificateImportWizard certImportWizard = (CertificateImportWizard) getWizard();
		certImportWizard.selectedImportFile = filePathField.getText().trim();
	}
}
