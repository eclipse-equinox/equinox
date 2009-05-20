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
package org.eclipse.equinox.internal.security.ui;

import java.security.cert.Certificate;
import org.eclipse.equinox.internal.security.ui.wizard.CertificateViewer;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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

	protected Control createContents(Composite parent) {
		return super.createContents(parent);
	}

	protected Control createDialogArea(Composite parent) {
		setTitle(SecurityUIMsg.CONFIRMATION_DIALOG_TITLE);
		setMessage(SecurityUIMsg.CONFIRMATION_DIALOG_MSG);

		Composite composite = new Composite(parent, SWT.None);
		composite.setLayout(new FillLayout());

		CertificateViewer certViewer = new CertificateViewer(composite);
		certViewer.setCertificate(cert);

		return composite;
	}

	protected void createButtonsForButtonBar(Composite parent) {
		Button yesBtn = createButton(parent, YES, SecurityUIMsg.CONFIRMATION_DIALGO_YES, true);
		yesBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				setReturnCode(YES);
				close();
			}
		});

		Button noBtn = createButton(parent, NO, SecurityUIMsg.CONFIRMATION_DIALGO_NO, false);
		noBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				setReturnCode(NO);
				close();
			}
		});
	}
}
