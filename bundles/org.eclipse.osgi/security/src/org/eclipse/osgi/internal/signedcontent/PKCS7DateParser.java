/*******************************************************************************
 * Copyright (c) 2006, 2010 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.osgi.internal.signedcontent;

import java.security.*;
import java.security.cert.CertificateException;
import java.util.*;

public class PKCS7DateParser {

	static Date parseDate(PKCS7Processor pkcs7Processor, String signer, String file) throws CertificateException, NoSuchAlgorithmException, InvalidKeyException, SignatureException, NoSuchProviderException {
		Map<int[], byte[]> unsignedAttrs = pkcs7Processor.getUnsignedAttrs();
		if (unsignedAttrs != null) {
			// get the timestamp construct
			byte[] timeStampConstruct = retrieveTimeStampConstruct(unsignedAttrs);

			// there is a timestamp in the signer info
			if (timeStampConstruct != null) {
				PKCS7Processor timestampProcess = new PKCS7Processor(timeStampConstruct, 0, timeStampConstruct.length, signer, file);
				timestampProcess.verifyCerts();
				pkcs7Processor.setTSACertificates(timestampProcess.getCertificates());
				return timestampProcess.getSigningTime();
			}
		}
		return null;
	}

	private static byte[] retrieveTimeStampConstruct(Map<int[], byte[]> unsignedAttrs) {
		Set<int[]> objIDs = unsignedAttrs.keySet();
		Iterator<int[]> iter = objIDs.iterator();
		while (iter.hasNext()) {
			int[] objID = iter.next();
			if (Arrays.equals(SignedContentConstants.TIMESTAMP_OID, objID)) {
				return unsignedAttrs.get(objID);
			}
		}
		return null;
	}
}
