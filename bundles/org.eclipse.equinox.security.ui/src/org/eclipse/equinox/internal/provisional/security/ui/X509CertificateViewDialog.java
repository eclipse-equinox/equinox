/*******************************************************************************
 * Copyright (c) 2008,  Jay Rosenthal and others
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Jay Rosenthal - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.provisional.security.ui;

import java.security.cert.X509Certificate;
import java.text.DateFormat;
import org.eclipse.equinox.internal.security.ui.Activator;
import org.eclipse.equinox.internal.security.ui.SecurityUIMsg;
import org.eclipse.equinox.internal.security.ui.wizard.CertificateViewer;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

public class X509CertificateViewDialog extends TitleAreaDialog {
	private final static String titleImageName = "/titleAreaCert.gif"; //$NON-NLS-1$
	private X509Certificate theCert;
	private static final DateFormat _df = DateFormat.getDateInstance(DateFormat.LONG);
	private X500PrincipalHelper nameHelper = new X500PrincipalHelper();

	// We use the "bannerFont" for our bold font
	private static Font boldFont = JFaceResources.getBannerFont();
	private Image titleImage;

	public X509CertificateViewDialog(Shell parentShell, X509Certificate cert) {
		super(parentShell);
		this.theCert = cert;
	}

	protected Control createContents(Composite parent) {
		return super.createContents(parent);
	}

	protected Control createDialogArea(Composite parent) {
		nameHelper.setPrincipal(theCert.getSubjectX500Principal());

		setTitle((nameHelper.getCN() != null ? nameHelper.getCN() : nameHelper.getOU()));

		titleImage = Activator.getImageDescriptor(titleImageName).createImage();

		if (titleImage != null)
			setTitleImage(titleImage);

		Composite composite = (Composite) super.createDialogArea(parent);

		TabFolder tabFolder = new TabFolder(composite, SWT.BORDER);
		GridData bdata = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL | GridData.GRAB_VERTICAL);
		tabFolder.setLayoutData(bdata);

		createBasicTab(tabFolder);

		createAdvancedTab(tabFolder);

		return composite;
	}

	private void createBasicTab(TabFolder tabFolder) {
		String displayName = null;
		int labelIndent = 10;
		int dataIdent = 10;

		TabItem basicTab = new TabItem(tabFolder, SWT.NULL);
		basicTab.setText(SecurityUIMsg.CERTVIEW_LABEL_BASIC);
		Composite basicTabComposite = new Composite(tabFolder, SWT.NONE);

		GridLayout tabLayout = new GridLayout();
		tabLayout.numColumns = 2;
		basicTabComposite.setLayout(tabLayout);

		Label issueToLabel = new Label(basicTabComposite, SWT.NONE);
		issueToLabel.setText(SecurityUIMsg.CERTPROP_X509_ISSUED_TO);
		issueToLabel.setFont(boldFont);
		configureLayout(issueToLabel, 2, 0, 0, 0);

		// Display the RDNs of the Subject
		nameHelper.setPrincipal(theCert.getSubjectX500Principal());

		Label CNLabel = new Label(basicTabComposite, SWT.NONE);
		CNLabel.setText(SecurityUIMsg.X500_LABEL_CN);
		configureLayout(CNLabel, 0, 0, labelIndent, 0);

		Label subjectCN = new Label(basicTabComposite, SWT.NONE);
		displayName = nameHelper.getCN();
		subjectCN.setText((displayName != null ? displayName : SecurityUIMsg.CERTVAL_UNDEFINED));
		configureLayout(subjectCN, 0, 0, dataIdent, 0);

		Label OLabel = new Label(basicTabComposite, SWT.NONE);
		OLabel.setText(SecurityUIMsg.X500_LABEL_O);
		configureLayout(OLabel, 0, 0, labelIndent, 0);

		Label subjectO = new Label(basicTabComposite, SWT.NONE);
		displayName = nameHelper.getO();
		subjectO.setText((displayName != null ? displayName : SecurityUIMsg.CERTVAL_UNDEFINED));
		configureLayout(subjectO, 0, 0, dataIdent, 0);

		Label OULabel = new Label(basicTabComposite, SWT.NONE);
		OULabel.setText(SecurityUIMsg.X500_LABEL_OU);
		configureLayout(OULabel, 0, 0, labelIndent, 0);

		Label subjectOU = new Label(basicTabComposite, SWT.NONE);
		displayName = nameHelper.getOU();
		subjectOU.setText((displayName != null ? displayName : SecurityUIMsg.CERTVAL_UNDEFINED));
		configureLayout(subjectOU, 0, 0, dataIdent, 0);

		Label issueByLabel = new Label(basicTabComposite, SWT.NONE);
		issueByLabel.setText(SecurityUIMsg.CERTPROP_X509_ISSUED_BY);
		configureLayout(issueByLabel, 2, 0, 0, 0);
		issueByLabel.setFont(boldFont);

		// Display the RDNs of the Issuer
		nameHelper.setPrincipal(theCert.getIssuerX500Principal());

		Label CNLabel2 = new Label(basicTabComposite, SWT.NONE);
		CNLabel2.setText(SecurityUIMsg.X500_LABEL_CN);
		configureLayout(CNLabel2, 0, 0, labelIndent, 0);

		Label issuerCN = new Label(basicTabComposite, SWT.NONE);
		displayName = nameHelper.getCN();
		issuerCN.setText((displayName != null ? displayName : SecurityUIMsg.CERTVAL_UNDEFINED));
		configureLayout(issuerCN, 0, 0, dataIdent, 0);

		Label OLabel2 = new Label(basicTabComposite, SWT.NONE);
		OLabel2.setText(SecurityUIMsg.X500_LABEL_O);
		configureLayout(OLabel2, 0, 0, labelIndent, 0);

		Label issuerO = new Label(basicTabComposite, SWT.NONE);
		displayName = nameHelper.getO();
		issuerO.setText((displayName != null ? displayName : SecurityUIMsg.CERTVAL_UNDEFINED));
		configureLayout(issuerO, 0, 0, dataIdent, 0);

		Label OULabel2 = new Label(basicTabComposite, SWT.NONE);
		OULabel2.setText(SecurityUIMsg.X500_LABEL_OU);
		configureLayout(OULabel2, 0, 0, labelIndent, 0);

		Label issuerOU = new Label(basicTabComposite, SWT.NONE);
		displayName = nameHelper.getOU();
		issuerOU.setText((displayName != null ? displayName : SecurityUIMsg.CERTVAL_UNDEFINED));
		configureLayout(issuerOU, 0, 0, dataIdent, 0);

		Label datesLabel = new Label(basicTabComposite, SWT.NONE);
		datesLabel.setText(SecurityUIMsg.CERTVIEW_LABEL_VALIDITY_DATES);
		configureLayout(datesLabel, 2, 0, 0, 0);
		datesLabel.setFont(boldFont);

		Label validFrom = new Label(basicTabComposite, SWT.NONE);
		validFrom.setText(SecurityUIMsg.CERTPROP_X509_VALID_FROM);
		configureLayout(validFrom, 0, 0, labelIndent, 0);

		Label fromDate = new Label(basicTabComposite, SWT.NONE);
		fromDate.setText(_df.format(theCert.getNotBefore()));
		configureLayout(fromDate, 0, 0, dataIdent, 0);

		Label validTo = new Label(basicTabComposite, SWT.NONE);
		validTo.setText(SecurityUIMsg.CERTPROP_X509_VALID_TO);
		configureLayout(validTo, 0, 0, labelIndent, 0);

		Label toDate = new Label(basicTabComposite, SWT.NONE);
		toDate.setText(_df.format(theCert.getNotAfter()));
		configureLayout(toDate, 0, 0, dataIdent, 0);

		basicTab.setControl(basicTabComposite);
	}

	protected static void configureLayout(Control c, int horizontalSpan, int verticalSpan, int horizontalIndent, int vertIndent) {
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_CENTER);

		gd.horizontalSpan = horizontalSpan;
		gd.verticalSpan = verticalSpan;
		gd.horizontalIndent = horizontalIndent;
		gd.verticalIndent = vertIndent;

		c.setLayoutData(gd);

	}

	private void createAdvancedTab(final TabFolder tabFolder) {
		TabItem advancedTab = new TabItem(tabFolder, SWT.NULL);
		advancedTab.setText(SecurityUIMsg.CERTVIEW_LABEL_DETAILS);
		Composite advTabComposite = new Composite(tabFolder, SWT.NONE);
		advTabComposite.setLayout(new FillLayout(SWT.VERTICAL));

		CertificateViewer certViewer = new CertificateViewer(advTabComposite);
		certViewer.setCertificate(theCert);
		advancedTab.setControl(advTabComposite);
	}

	protected void setShellStyle(int newShellStyle) {

		super.setShellStyle(newShellStyle | SWT.RESIZE | SWT.DIALOG_TRIM);
	}

	protected void createButtonsForButtonBar(Composite parent) {
		// The default has only a "Close" button, but it returns the CANCEL Id
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, true);

	}

	public boolean close() {
		if (titleImage != null) {
			titleImage.dispose();
		}
		return super.close();
	}

}
