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

import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.util.*;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.osgi.util.NLS;

/**
 * X509CertificateAttributeContentProvider
 *  Structured content provided for an <a href="http://java.sun.com/j2se/1.4.2/docs/api/java/security/cert/X509Certificate.html">X509Certificate </a>
 *  object. 
 *  <p>
 *  Currently this only supports the attributes exposed directly by X509Certificate.  Some commonly used extensions
 *  may not be displayed.  Contributions and enhancements are welcomed.
 *    
 */
public class X509CertificateAttributeContentProvider implements IStructuredContentProvider {

	private static String LABEL_KEYUSAGE_DIGITALSIGNATURE = "digitalSignature"; //$NON-NLS-1$
	private static String LABEL_KEYUSAGE_NONREPUDIATION = "nonRepudiation"; //$NON-NLS-1$
	private static String LABEL_KEYUSAGE_KEYENCIPHERMENT = "keyEncipherment"; //$NON-NLS-1$
	private static String LABEL_KEYUSAGE_DATAENCIPHERMENT = "dataEncipherment"; //$NON-NLS-1$
	private static String LABEL_KEYUSAGE_KEYAGREEMENT = "keyAgreement"; //$NON-NLS-1$
	private static String LABEL_KEYUSAGE_CERTSIGN = "keyCertSign"; //$NON-NLS-1$
	private static String LABEL_KEYUSAGE_CRLSIGN = "cRLSign"; //$NON-NLS-1$
	private static String LABEL_KEYUSAGE_ENCIPHERONLY = "encipherOnly"; //$NON-NLS-1$
	private static String LABEL_KEYUSAGE_DECIPHERONLY = "decipherOnly"; //$NON-NLS-1$

	private ArrayList elements = new ArrayList();
	private Viewer viewer = null;
	private static String listDelim = ", "; //$NON-NLS-1$

	private static final DateFormat _df = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.FULL);

	// TODO - This is a bit ugly but gets the job done... Maybe this should be a class of it's own ??
	private static String keyUsageStrings[] = {LABEL_KEYUSAGE_DIGITALSIGNATURE, LABEL_KEYUSAGE_NONREPUDIATION, LABEL_KEYUSAGE_KEYENCIPHERMENT, LABEL_KEYUSAGE_DATAENCIPHERMENT, LABEL_KEYUSAGE_KEYAGREEMENT, LABEL_KEYUSAGE_CERTSIGN, LABEL_KEYUSAGE_CRLSIGN, LABEL_KEYUSAGE_ENCIPHERONLY, LABEL_KEYUSAGE_DECIPHERONLY};

	public X509CertificateAttributeContentProvider() {
		super();

	}

	public Object[] getElements(Object inputElement) {
		return elements.toArray();
	}

	public void dispose() {
		// TODO Auto-generated method stub

	}

	public void setSelection(X509Certificate curCert) {

		inputChanged(viewer, null, curCert);

		if (viewer != null)
			viewer.refresh();
	}

	public void clear(boolean refresh) {
		elements.clear();
		if (refresh && viewer != null)
			viewer.refresh();
	}

	public void clear() {
		clear(true);
	}

	public void inputChanged(Viewer aViewer, Object oldInput, Object newInput) {
		viewer = aViewer;
		clear(false); // clear the viewer.

		//  Be safe ... check the input
		if (newInput instanceof X509Certificate) {

			X509Certificate theCert = (X509Certificate) newInput;

			X509CertificateAttribute ver = new X509CertificateAttribute(SecurityUIMsg.CERTPROP_X509_VERSION, new Integer(theCert.getVersion()).toString());
			elements.add(ver);

			X509CertificateAttribute serialNum = new X509CertificateAttribute(SecurityUIMsg.CERTPROP_X509_SERIAL_NUM, theCert.getSerialNumber().toString());
			elements.add(serialNum);

			X509CertificateAttribute validFrom = new X509CertificateAttribute(SecurityUIMsg.CERTPROP_X509_VALID_FROM, _df.format(theCert.getNotBefore()), theCert.getNotBefore());
			elements.add(validFrom);

			X509CertificateAttribute validTo = new X509CertificateAttribute(SecurityUIMsg.CERTPROP_X509_VALID_TO, _df.format(theCert.getNotAfter()), theCert.getNotAfter());
			elements.add(validTo);

			X509CertificateAttribute issuedBy = new X509CertificateAttribute(SecurityUIMsg.CERTPROP_X509_ISSUED_BY, theCert.getIssuerX500Principal().getName(), theCert.getIssuerX500Principal());
			elements.add(issuedBy);

			X509CertificateAttribute IssuedToItem = new X509CertificateAttribute(SecurityUIMsg.CERTPROP_X509_ISSUED_TO, theCert.getSubjectX500Principal().getName(), theCert.getSubjectX500Principal());
			elements.add(IssuedToItem);

			X509CertificateAttribute sigAlgoItem = new X509CertificateAttribute(SecurityUIMsg.CERTPROP_X509_SIG_ALGO, theCert.getSigAlgName());
			elements.add(sigAlgoItem);

			boolean keyUsagesArray[] = theCert.getKeyUsage();
			StringBuffer keyUsages = new StringBuffer();
			//
			//  Only set the string field, If we got some data
			if (keyUsagesArray != null && keyUsagesArray.length > 0) {
				for (int i = 0; i < keyUsagesArray.length; i++) {
					if (keyUsagesArray[i])
						keyUsages.append(keyUsageStrings[i] + listDelim);
				}

				X509CertificateAttribute keyUsage = new X509CertificateAttribute(SecurityUIMsg.CERTPROP_X509_KEY_USAGE, (keyUsages.toString()).substring(0, keyUsages.length() - 2), theCert.getKeyUsage());
				elements.add(keyUsage);
			}

			/*
			 * Thumbprint is not actually "in" the certificate.  It is computed on the fly...
			 */
			X509CertificateAttribute thumbPrintItem = new X509CertificateAttribute(SecurityUIMsg.CERTPROP_X509_THMBPRINT, getThumbprint(theCert, "SHA1")); //$NON-NLS-1$
			elements.add(thumbPrintItem);

			PublicKey pubKey = theCert.getPublicKey();
			X509CertificateAttribute pubKeyInfoItem = new X509CertificateAttribute(SecurityUIMsg.CERTPROP_X509_THMBPRINTX509_PUBKEY_INFO, getHex(pubKey.getEncoded()));
			elements.add(pubKeyInfoItem);

			Collection subAltNamesVctr;
			try {
				subAltNamesVctr = theCert.getSubjectAlternativeNames();

				//				StringBuffer bfrSubAltNames = new StringBuffer();
				if (subAltNamesVctr != null && subAltNamesVctr.size() > 0) {
					// TODO - Make alt names into a displayable list...

					// For now just display that they exist..
					X509CertificateAttribute subAltItem = new X509CertificateAttribute(SecurityUIMsg.CERTPROP_X509_SUB_ALT_NAMES, "Has Subject Alternate Names" /*bfrSubAltNames.toString()*/, theCert.getSubjectAlternativeNames()); //$NON-NLS-1$
					elements.add(subAltItem);
				}
			} catch (CertificateParsingException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			int basicCnstrnts = theCert.getBasicConstraints();
			Integer basicConstraint = new Integer(basicCnstrnts);
			StringBuffer basicCnstrntsBfr = new StringBuffer();
			if (basicCnstrnts == -1) {
				// Not a CA
				basicCnstrntsBfr.append(SecurityUIMsg.LABEL_NAMECONSTRAINTS_NOTCA + listDelim);
			} else {
				basicCnstrntsBfr.append(SecurityUIMsg.LABEL_NAMECONSTRAINTS_ISCA + listDelim);
				if (basicCnstrnts == Integer.MAX_VALUE) {
					// MAX_VALUE means "no limit to the allowed length of the certification path."
					basicCnstrntsBfr.append(NLS.bind(SecurityUIMsg.LABEL_NAMECONSTRAINTS_PATHLENGTH, new Object[] {SecurityUIMsg.LABEL_NAMECONSTRAINTS_PATHLENGTH_UNLIMITED}) + listDelim);
				} else {
					basicCnstrntsBfr.append(NLS.bind(SecurityUIMsg.LABEL_NAMECONSTRAINTS_PATHLENGTH, new Object[] {basicConstraint}) + listDelim);
				}
			}

			X509CertificateAttribute basicConstraints = new X509CertificateAttribute(SecurityUIMsg.CERTPROP_X509_BASIC_CNSTRNTS, (basicCnstrntsBfr.toString()).substring(0, basicCnstrntsBfr.length() - 2), basicConstraint);
			elements.add(basicConstraints);

			List exKeyUsg;
			try {
				exKeyUsg = theCert.getExtendedKeyUsage();
				StringBuffer exKeyUsgBfr = new StringBuffer();
				if (exKeyUsg != null && exKeyUsg.size() > 0) {
					for (Iterator exKeyUsgIter = exKeyUsg.iterator(); exKeyUsgIter.hasNext();) {
						exKeyUsgBfr.append(((String) exKeyUsgIter.next()) + listDelim);
					}

					X509CertificateAttribute exKeyUsgProp = new X509CertificateAttribute(SecurityUIMsg.CERTPROP_X509_EXKEY_USAGE, (exKeyUsgBfr.toString()).substring(0, exKeyUsgBfr.length() - 2), theCert.getExtendedKeyUsage());
					elements.add(exKeyUsgProp);
				}

			} catch (CertificateParsingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}

	private String getHex(byte buf[]) {
		String result = ""; //$NON-NLS-1$
		if (buf != null) {
			for (int i = 0; i < buf.length; i++) {
				if (i > 0)
					result += " "; //$NON-NLS-1$

				short sValue = buf[i];
				int curInt = 0;
				curInt += sValue;
				String converted = Integer.toHexString(curInt);

				if (converted.length() > 2)
					converted = converted.substring(converted.length() - 2);

				result += converted.toUpperCase();
			}
		}
		return result;
	}

	private MessageDigest getDigest(String thumAlg) {
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance(thumAlg);
		} catch (Exception e) {
			// TODO - Handle the exception or log it..
		}
		return md;
	}

	private String getThumbprint(X509Certificate curCert, String thumAlg) {
		String thumbPrint = ""; //$NON-NLS-1$
		try {

			MessageDigest md = getDigest(thumAlg);
			md.update(curCert.getEncoded());
			byte rawDigest[] = md.digest();
			thumbPrint = getHex(rawDigest);
		} catch (Exception e) {
			// Might if thumb print algorithm can not be loaded.

		}
		return thumbPrint;
	}

}
