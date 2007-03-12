/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.osgi.internal.verifier;

import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.*;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;

public class PKCS7DateParser {

	static Date parseDate(PKCS7Processor pkcs7Processor) throws IOException {
		return hasTimeStamp(pkcs7Processor);
	}

	private static Date hasTimeStamp(PKCS7Processor pkcs7) throws IOException {
		Map unsignedAttrs = pkcs7.getUnsignedAttrs();
		if (unsignedAttrs != null) {
			// get the timestamp constrcut
			byte[] timeStampConstruct = retrieveTimeStampConstruct(unsignedAttrs);

			// there is a timestamp in the signer info
			if (timeStampConstruct != null) {

				try {
					PKCS7Processor timestampProcess = new PKCS7Processor(timeStampConstruct, 0, timeStampConstruct.length);
					timestampProcess.validateCerts();
					pkcs7.setTSACertificates(timestampProcess.getCertificates());
					return timestampProcess.getSigningTime();
				} catch (CertificateException e) {
					SignedBundleHook.log(e.getMessage(), FrameworkLogEntry.ERROR, e);
					throw new IOException(JarVerifierMessages.PKCS7_Parse_Signing_Time);
				} catch (NoSuchAlgorithmException e) {
					SignedBundleHook.log(e.getMessage(), FrameworkLogEntry.ERROR, e);
					throw new SecurityException(JarVerifierMessages.No_Such_Algorithm_Excep);
				} catch (InvalidKeyException e) {
					throw new IOException("InvalidKeyException occurs when verifying the certs from tsa certificates: " + e.getMessage()); //$NON-NLS-1$
				} catch (SignatureException e) {
					throw new IOException(JarVerifierMessages.Signature_Not_Verify);
				}

			}
		}
		return null;
	}

	private static byte[] retrieveTimeStampConstruct(Map unsignedAttrs) {
		Set objIDs = unsignedAttrs.keySet();
		Iterator iter = objIDs.iterator();
		while (iter.hasNext()) {
			int[] objID = (int[]) iter.next();
			if (Arrays.equals(JarVerifierConstant.TIMESTAMP_OID, objID)) {
				return (byte[]) unsignedAttrs.get(objID);
			}
		}
		return null;
	}
}
