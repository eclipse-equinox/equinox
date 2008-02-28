/*******************************************************************************
 * Copyright (c) 2008,  Jay Rosenthal
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Jay Rosenthal - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.security.ui;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

/**
 * X509CertificateAttributeLabelProvider
 * <p>
 * Label provider for a 2 column table that shows the attributes (fields) in an X509 digital
 * certificate. 
 * See <a href="http://java.sun.com/j2se/1.4.2/docs/api/java/security/cert/X509Certificate.html"> X509Certificate </a>
 * <p>
 * The first column is the Attribute name and the second column is the string representation of the 
 * attribute's value.
 * <p>
 * Used by org.eclipse.equinox.security.ui.wizard.CertificateViewer 
 *
 */
public class X509CertificateAttributeLabelProvider extends LabelProvider implements ITableLabelProvider {

	public X509CertificateAttributeLabelProvider() {
		super();

	}

	public Image getColumnImage(Object element, int columnIndex) {

		return null;
	}

	public String getColumnText(Object element, int columnIndex) {
		String text = ""; //$NON-NLS-1$

		if (element instanceof X509CertificateAttribute) {
			X509CertificateAttribute curEntry = (X509CertificateAttribute) element;
			switch (columnIndex) {
				// Attribute/field Name
				case 0 :
					text = curEntry.getDescription();
					break;
				// Attribute/field string value
				case 1 :
					text = curEntry.getStringValue();
					break;
			}
		}
		return text;
	}

}
