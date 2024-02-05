/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.security.ui.wizard;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.security.cert.*;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.equinox.internal.security.ui.SecurityUIMsg;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

public class CertificateImportCertSelectPage extends WizardPage implements Listener {

	private Composite certPreview;
	private Combo certDropDown;
	private List<Certificate> certList;

	static CertificateFactory certFact;

	static {
		try {
			certFact = CertificateFactory.getInstance("X.509"); //$NON-NLS-1$
		} catch (CertificateException e) {
			// TODO log error here
		}
	}

	protected CertificateImportCertSelectPage(String pageName) {
		super(pageName);
		setTitle(SecurityUIMsg.WIZARD_SELECT_CERT);
		setDescription(SecurityUIMsg.WIZARD_SELECT_CERT_FROM_DROP_DOWN);
	}

	@Override
	public void createControl(Composite parent) {
		Composite certSelectComposite = new Composite(parent, SWT.NONE);
		setControl(certSelectComposite);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.makeColumnsEqualWidth = false;
		certSelectComposite.setLayout(layout);
		certSelectComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));

		certDropDown = new Combo(certSelectComposite, SWT.DROP_DOWN | SWT.READ_ONLY);
		certDropDown.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL));

		// populate the drop down
		populateDropDown();

		certPreview = new Composite(certSelectComposite, SWT.None);
		certPreview.setLayout(new GridLayout());
		GridData gd = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL);
		// gd.horizontalSpan = 2;
		certPreview.setLayoutData(gd);
	}

	private void populateDropDown() {
		CertificateImportWizard certImportWizard = (CertificateImportWizard) getWizard();
		if (certImportWizard.selectedImportFile == null)
			return;

		if (certDropDown.getItemCount() > 0)
			certDropDown.removeAll();

		try {
			certList = new ArrayList<>();
			// For a set or list
			for (Certificate certificate : certFact
					.generateCertificates(new FileInputStream(certImportWizard.selectedImportFile))) {
				certList.add(certificate);
			}

		} catch (CertificateException | FileNotFoundException e) {
			setErrorMessage(e.getMessage());
		}

		for (Certificate certificate : certList) {
			X509Certificate x509Cert = (X509Certificate) certificate;
			String subjectDN = x509Cert.getSubjectDN().getName();
			certDropDown.add(subjectDN);
		}
		certDropDown.addListener(SWT.Selection, this);

	}

	@Override
	public void handleEvent(Event e) {
		if (e.widget == certDropDown) {
			// populate the preview with select cert info
			X509Certificate x509Cert = (X509Certificate) certList.get(certDropDown.getSelectionIndex());
			showCertificate(x509Cert);
			((CertificateImportWizard) getWizard()).selectCert = x509Cert;
			getWizard().getContainer().updateButtons();
		}
	}

	public void showCertificate(X509Certificate cert) {
		Control ctrls[] = certPreview.getChildren();
		for (Control ctrl : ctrls) {
			ctrl.dispose();
		}
		CertificateViewer certViewer = new CertificateViewer(certPreview);
		certViewer.setCertificate(cert);
		certPreview.layout();
	}

	/**
	 * This methods get called before each page gets showed
	 */
	@Override
	public void setVisible(boolean visible) {
		if (visible)
			populateDropDown();
		super.setVisible(visible);

	}

	@Override
	public boolean canFlipToNextPage() {
		return ((CertificateImportWizard) getWizard()).selectCert != null;
	}
}
