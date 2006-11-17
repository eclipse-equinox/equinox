/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.verifier;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.text.*;
import java.util.*;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;

public class PKCS7DateParser {

	private static final String _19 = "19"; //$NON-NLS-1$
	private static final String _20 = "20"; //$NON-NLS-1$

	private static final DateFormat dateFormt;
	static {
		dateFormt = new SimpleDateFormat("yyyyMMddHHmmss"); //$NON-NLS-1$
		dateFormt.setTimeZone(TimeZone.getTimeZone("GMT")); //$NON-NLS-1$
	}

	static Date parseDate(PKCS7Processor pkcs7Processor) throws IOException {
		return hasTimeStamp(pkcs7Processor);
	}

	private static Date hasTimeStamp(PKCS7Processor pkcs7) throws IOException {
		Date rtvDateTime = null;
		Map unsignedAttrs = pkcs7.getUnsignedAttrs();
		if (unsignedAttrs != null) {
			// get the timestamp constrcut
			byte[] timeStampConstruct = retrieveTimeStampConstruct(unsignedAttrs);
			// there is a timestamp in the signer info
			if (timeStampConstruct != null) {
				// parse the timestamp constrcut pkcs7 again to retrieve the
				// timestamp in the signed attribtues
				PKCS7Processor timestampProcess = null;
				try {
					timestampProcess = new PKCS7Processor(timeStampConstruct, 0, timeStampConstruct.length);
				} catch (CertificateException e) {
					SignedBundleHook.log(e.getMessage(), FrameworkLogEntry.ERROR, e);
					throw new IOException(JarVerifierMessages.PKCS7_Parse_Signing_Time);
				} catch (NoSuchAlgorithmException e) {
					SignedBundleHook.log(e.getMessage(), FrameworkLogEntry.ERROR, e);
					throw new SecurityException(JarVerifierMessages.No_Such_Algorithm_Excep);
				}

				Map signedAttrs = timestampProcess.getSignedAttrs();
				if (signedAttrs != null) {
					rtvDateTime = getSigningTime(signedAttrs);
				}
			}
		}

		return rtvDateTime;
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

	private static Date getSigningTime(Map signedAttrs) {
		Date returnDate = null;

		// iterate through each object id in the map and find out if signing time objid is there
		Iterator iter = signedAttrs.keySet().iterator();

		while (iter.hasNext()) {
			int[] objID = (int[]) iter.next();
			if (Arrays.equals(JarVerifierConstant.SIGNING_TIME, objID)) {

				// the siging time obj id is in the map, we need to process the signing time bytes
				byte[] signingTime = (byte[]) signedAttrs.get(objID);

				// create a BERProcess using the bytes
				BERProcessor signingTimeBER = new BERProcessor(signingTime, 0, signingTime.length);
				byte unprocessedSigningTimeBytes[] = signingTimeBER.getBytes();

				String dateString = new String(unprocessedSigningTimeBytes);

				if (dateString.length() == 13) {
					String yyStr = dateString.substring(0, 2);

					int yy = Integer.parseInt(yyStr);
					if (yy < 50) {
						dateString = _20 + dateString;
					} else {
						dateString = _19 + dateString;
					}

					try {
						DateFormat cloneDateFormat = (DateFormat) dateFormt.clone();
						returnDate = cloneDateFormat.parse(dateString);
						break;
					} catch (ParseException e) {
						throw new SecurityException(JarVerifierMessages.PKCS7_Parse_Signing_Time_1);
					}
				}
			}
		}
		return returnDate;
	}
}
