/*******************************************************************************
 * Copyright (c) 2005, 2017 IBM Corporation and others.
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
package org.eclipse.equinox.internal.security.ui;

import java.security.cert.Certificate;
import org.eclipse.equinox.internal.security.ui.wizard.CertificateViewer;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.*;

public class ConfirmationDialog extends TitleAreaDialog {

	public static final int YES = 100;
	public static final int NO = 101;

	private Certificate cert;

	public ConfirmationDialog(Shell parentShell, Certificate cert) {
		super(parentShell);
		this.cert = cert;
	}

	@Override
	protected Control createContents(Composite parent) {
		return super.createContents(parent);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		setTitle(SecurityUIMsg.CONFIRMATION_DIALOG_TITLE);
		setMessage(SecurityUIMsg.CONFIRMATION_DIALOG_MSG);

		Composite composite = new Composite(parent, SWT.None);
		composite.setLayout(new FillLayout());

		CertificateViewer certViewer = new CertificateViewer(composite);
		certViewer.setCertificate(cert);

		return composite;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		Button yesBtn = createButton(parent, YES, SecurityUIMsg.CONFIRMATION_DIALGO_YES, true);
		yesBtn.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			setReturnCode(YES);
			close();
		}));

		Button noBtn = createButton(parent, NO, SecurityUIMsg.CONFIRMATION_DIALGO_NO, false);
		noBtn.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			setReturnCode(NO);
			close();
		}));
	}
}
