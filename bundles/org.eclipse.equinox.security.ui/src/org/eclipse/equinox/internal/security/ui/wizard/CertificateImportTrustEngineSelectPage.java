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

import java.util.ArrayList;
import org.eclipse.equinox.internal.security.ui.Activator;
import org.eclipse.equinox.internal.security.ui.SecurityUIMsg;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.service.security.TrustEngine;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

public class CertificateImportTrustEngineSelectPage extends WizardPage implements Listener {

	private Text aliasField;
	private Combo trustEngineCombo;
	private ArrayList trustEngines = new ArrayList();

	protected CertificateImportTrustEngineSelectPage(String pageName) {
		super(pageName);
		setTitle(SecurityUIMsg.WIZARD_ENGINE_SELECT_TITLE);
		setDescription(SecurityUIMsg.WIZARD_ENGINE_SELECT_MSG);
	}

	public void createControl(Composite parent) {
		Composite certSelectComposite = new Composite(parent, SWT.NONE);
		setControl(certSelectComposite);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.makeColumnsEqualWidth = false;
		layout.marginWidth = 0;
		certSelectComposite.setLayout(layout);
		certSelectComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Label aliasLabel = new Label(certSelectComposite, SWT.None);
		aliasLabel.setText(SecurityUIMsg.WIZARD_ALIAS_NAME_FIELD);

		aliasField = new Text(certSelectComposite, SWT.None);
		aliasField.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL));

		/*
		 *  create the trust engine area
		 */
		Label targetEngineLabel = new Label(certSelectComposite, SWT.None);
		targetEngineLabel.setText(SecurityUIMsg.WIZARD_TARGET_TRUST_ENGINE);

		trustEngineCombo = new Combo(certSelectComposite, SWT.DROP_DOWN | SWT.READ_ONLY);
		TrustEngine[] availableEngines = Activator.getTrustEngines();
		trustEngines.clear();

		// get a list of trust engine and fill the combo
		for (int i = 0; i < availableEngines.length; i++) {
			if (!availableEngines[i].isReadOnly()) {
				trustEngines.add(availableEngines[i]);
				trustEngineCombo.add(availableEngines[i].getName());
			}

		}
		if (trustEngineCombo.getItemCount() == 0)
			setErrorMessage(SecurityUIMsg.WIZARD_ERROR_NO_WRITE_ENGINE);
		else
			trustEngineCombo.setVisibleItemCount(trustEngines.size());

		addListeners();
	}

	private void addListeners() {
		aliasField.addListener(SWT.KeyUp, this);
		trustEngineCombo.addListener(SWT.Selection, this);
	}

	public void handleEvent(Event e) {
		if (e.widget == aliasField) {
			if (aliasField.getText().length() < 1) {
				setErrorMessage(SecurityUIMsg.WIZARD_ERROR_ALIAS_REQUIRED);
			} else {
				setErrorMessage(null);
				((CertificateImportWizard) getWizard()).aliasName = aliasField.getText();
			}
		}

		if (e.widget == trustEngineCombo) {
			if (trustEngineCombo.getSelectionIndex() == -1) {
				setErrorMessage(SecurityUIMsg.WIZARD_ERROR_ENGINE_REQUIRED);
			} else {
				setErrorMessage(null);
				((CertificateImportWizard) getWizard()).selectTrustEngine = (TrustEngine) trustEngines.get(trustEngineCombo.getSelectionIndex());
			}
		}
		getWizard().getContainer().updateButtons();
	}

	public boolean canFlipToNextPage() {
		return (aliasField.getText().length() > 0) && (trustEngineCombo.getSelectionIndex() != -1);
	}
}
