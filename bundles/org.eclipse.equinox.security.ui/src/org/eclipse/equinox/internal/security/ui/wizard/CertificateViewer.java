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

import java.security.cert.Certificate;
import org.eclipse.equinox.internal.security.ui.*;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

public class CertificateViewer {

	private final Composite composite;

	private final TableViewer tableViewer;

	public CertificateViewer(Composite parent) {
		composite = new Composite(parent, SWT.None);
		composite.setLayout(new GridLayout());

		tableViewer = new TableViewer(composite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		Table table = tableViewer.getTable();
		table.setHeaderVisible(true);
		GridData tableData = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
		table.setLayoutData(tableData);

		TableColumn propertyCol = new TableColumn(table, SWT.LEFT);
		propertyCol.setText(SecurityUIMsg.STR_CERT_VIEWER_FIELD);
		propertyCol.setWidth(200);

		TableColumn valueCol = new TableColumn(table, SWT.LEFT);
		valueCol.setText(SecurityUIMsg.STR_CERT_VIEWER_VALUE);
		valueCol.setWidth(300);
	}

	public void setCertificate(Certificate certificate) {
		tableViewer.setContentProvider(new X509CertificateAttributeContentProvider());
		tableViewer.setLabelProvider(new X509CertificateAttributeLabelProvider());
		tableViewer.setInput(certificate);
	}

}
