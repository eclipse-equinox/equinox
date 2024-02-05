/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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

import java.security.cert.X509Certificate;
import org.eclipse.equinox.internal.security.ui.SecurityUIMsg;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.service.security.TrustEngine;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

public class CertificateImportWizard extends Wizard implements IImportWizard {

	// ImportWizardPage mainPage;
	CertificateImportFileSelectPage selectCertFilePage;
	CertificateImportCertSelectPage selectCertPage;
	CertificateImportTrustEngineSelectPage selectTrustEnginePage;
	CertificateImportConfirmationPage certConfirmPage;

	String selectedImportFile;
	String aliasName;
	X509Certificate selectCert;
	TrustEngine selectTrustEngine;

	public CertificateImportWizard() {
		super();
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		// nothing
	}

	@Override
	public void addPages() {
		// mainPage = new ImportWizardPage(SecurityUIMsg.IMPORT_FILE); //NON-NLS-1
		// addPage(mainPage);

		selectCertFilePage = new CertificateImportFileSelectPage(SecurityUIMsg.WIZARD_PAGE_FILE_CERT_SELECT);
		addPage(selectCertFilePage);

		selectCertPage = new CertificateImportCertSelectPage(SecurityUIMsg.WIZARD_PAGE_CERT_SELECT);
		addPage(selectCertPage);

		selectTrustEnginePage = new CertificateImportTrustEngineSelectPage(SecurityUIMsg.WIZARD_PAGE_ENGINE);
		addPage(selectTrustEnginePage);

		certConfirmPage = new CertificateImportConfirmationPage(SecurityUIMsg.WIZARD_IMPORT_CONFIRMATION_TITLE);
		addPage(certConfirmPage);
	}

	@Override
	public boolean canFinish() {
		return getContainer().getCurrentPage() == certConfirmPage;
	}

	@Override
	public boolean performFinish() {
		try {
			selectTrustEngine.addTrustAnchor(selectCert, aliasName);
		} catch (Exception e) {
			certConfirmPage.setErrorMessage(e.getMessage());
			return false;
		}
		return true;
	}
}
