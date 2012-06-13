/*******************************************************************************
 * Copyright (c) 2006, 2012 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.osgi.internal.signedcontent;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.text.*;
import java.util.*;
import javax.security.auth.x500.X500Principal;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.util.NLS;

/**
 * This class processes a PKCS7 file. See RFC 2315 for specifics.
 */
public class PKCS7Processor implements SignedContentConstants {

	static CertificateFactory certFact;

	static {
		try {
			certFact = CertificateFactory.getInstance("X.509"); //$NON-NLS-1$
		} catch (CertificateException e) {
			SignedBundleHook.log(e.getMessage(), FrameworkLogEntry.ERROR, e);
		}
	}

	private final String signer;
	private final String file;

	private Certificate[] certificates;
	private Certificate[] tsaCertificates;

	// key(object id) = value(structure)
	private Map<int[], byte[]> signedAttrs;

	//	key(object id) = value(structure)
	private Map<int[], byte[]> unsignedAttrs;

	// store the signature of a signerinfo
	private byte signature[];
	private String digestAlgorithm;
	private String signatureAlgorithm;

	private Certificate signerCert;
	private Date signingTime;

	private static String oid2String(int oid[]) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < oid.length; i++) {
			if (i > 0)
				sb.append('.');
			sb.append(oid[i]);
		}
		return sb.toString();
	}

	private static String findEncryption(int encOid[]) throws NoSuchAlgorithmException {
		if (Arrays.equals(DSA_OID, encOid)) {
			return "DSA"; //$NON-NLS-1$
		}
		if (Arrays.equals(RSA_OID, encOid)) {
			return "RSA"; //$NON-NLS-1$
		}
		throw new NoSuchAlgorithmException("No algorithm found for " + oid2String(encOid)); //$NON-NLS-1$
	}

	private static String findDigest(int digestOid[]) throws NoSuchAlgorithmException {
		if (Arrays.equals(SHA1_OID, digestOid)) {
			return SHA1_STR;
		}
		if (Arrays.equals(SHA224_OID, digestOid)) {
			return SHA224_STR;
		}
		if (Arrays.equals(SHA256_OID, digestOid)) {
			return SHA256_STR;
		}
		if (Arrays.equals(SHA384_OID, digestOid)) {
			return SHA384_STR;
		}
		if (Arrays.equals(SHA512_OID, digestOid)) {
			return SHA512_STR;
		}
		if (Arrays.equals(SHA512_224_OID, digestOid)) {
			return SHA512_224_STR;
		}
		if (Arrays.equals(SHA512_256_OID, digestOid)) {
			return SHA512_256_STR;
		}
		if (Arrays.equals(MD5_OID, digestOid)) {
			return MD5_STR;
		}
		if (Arrays.equals(MD2_OID, digestOid)) {
			return MD2_STR;
		}
		throw new NoSuchAlgorithmException("No algorithm found for " + oid2String(digestOid)); //$NON-NLS-1$
	}

	public PKCS7Processor(byte pkcs7[], int pkcs7Offset, int pkcs7Length, String signer, String file) throws CertificateException, NoSuchAlgorithmException, InvalidKeyException, SignatureException, NoSuchProviderException {
		this.signer = signer;
		this.file = file;
		// First grab the certificates
		List<Certificate> certs = null;

		BERProcessor bp = new BERProcessor(pkcs7, pkcs7Offset, pkcs7Length);

		// Just do a sanity check and make sure we are actually doing a PKCS7
		// stream
		// PKCS7: Step into the ContentType
		bp = bp.stepInto();
		if (!Arrays.equals(bp.getObjId(), SIGNEDDATA_OID)) {
			throw new SignatureException(NLS.bind(SignedContentMessages.PKCS7_Invalid_File, signer, file));
		}

		// PKCS7: Process the SignedData structure
		bp.stepOver(); // (**wrong comments**) skip over the oid
		bp = bp.stepInto(); // go into the Signed data
		bp = bp.stepInto(); // It is a structure;
		bp.stepOver(); // Yeah, yeah version = 1
		bp.stepOver(); // We'll see the digest stuff again; digestAlgorithms

		// process the encapContentInfo structure
		processEncapContentInfo(bp);

		bp.stepOver();

		// PKCS7: check if the class tag is 0
		if (bp.classOfTag == BERProcessor.CONTEXTSPECIFIC_TAGCLASS && bp.tag == 0) {
			// process the certificate elements inside the signeddata strcuture
			certs = processCertificates(bp);
		}

		if (certs == null || certs.size() < 1)
			throw new SignatureException("There are no certificates in the .RSA/.DSA file!"); //$NON-NLS-1$

		// Okay, here are our certificates.
		bp.stepOver();
		if (bp.classOfTag == BERProcessor.UNIVERSAL_TAGCLASS && bp.tag == 1) {
			bp.stepOver(); // Don't use the CRLs if present
		}

		processSignerInfos(bp, certs);

		// construct the cert path
		certs = constructCertPath(certs, signerCert);

		// initialize the certificates
		certificates = certs.toArray(new Certificate[certs.size()]);
		verifyCerts();
		// if this pkcs7process is tsa asn.1 block, the signingTime should already be set
		if (signingTime == null)
			signingTime = PKCS7DateParser.parseDate(this, signer, file);
	}

	private void processEncapContentInfo(BERProcessor bp) throws SignatureException {
		// check immediately if TSTInfo is there
		BERProcessor encapContentBERS = bp.stepInto();
		if (Arrays.equals(encapContentBERS.getObjId(), TIMESTAMP_TST_OID)) {

			// eContent
			encapContentBERS.stepOver();
			BERProcessor encapContentBERS1 = encapContentBERS.stepInto();

			// obtain eContent octet structure
			byte bytesman[] = encapContentBERS1.getBytes();
			BERProcessor eContentStructure = new BERProcessor(bytesman, 0, bytesman.length);

			// pointing at 'version Integer' now
			BERProcessor eContentBER = eContentStructure.stepInto();
			int tsaVersion = eContentBER.getIntValue().intValue();

			if (tsaVersion != 1)
				throw new SignatureException("Not a version 1 time-stamp token"); //$NON-NLS-1$

			// policty : TSAPolicyId
			eContentBER.stepOver();

			// messageImprint : MessageImprint
			eContentBER.stepOver();

			// serialNumber : INTEGER
			eContentBER.stepOver();

			// genTime : GeneralizedTime
			eContentBER.stepOver();

			// check time ends w/ 'Z'
			String dateString = new String(eContentBER.getBytes());
			if (!dateString.endsWith("Z")) //$NON-NLS-1$
				throw new SignatureException("Wrong dateformat used in time-stamp token"); //$NON-NLS-1$

			// create the appropriate date time string format
			// date format could be yyyyMMddHHmmss[.s...]Z or yyyyMMddHHmmssZ
			int dotIndex = dateString.indexOf('.');
			StringBuffer dateFormatSB = new StringBuffer("yyyyMMddHHmmss"); //$NON-NLS-1$
			if (dotIndex != -1) {
				// yyyyMMddHHmmss[.s...]Z, find out number of s in the bracket
				int noS = dateString.indexOf('Z') - 1 - dotIndex;
				dateFormatSB.append('.');

				// append s	
				for (int i = 0; i < noS; i++) {
					dateFormatSB.append('s');
				}
			}
			dateFormatSB.append("'Z'"); //$NON-NLS-1$

			try {
				// if the current locale is th_TH, or ja_JP_JP, then our dateFormat object will end up with
				// a calendar such as Buddhist or Japanese Imperial Calendar, and the signing time will be 
				// incorrect ... so always use English as the locale for parsing the time, resulting in a 
				// Gregorian calendar
				DateFormat dateFormt = new SimpleDateFormat(dateFormatSB.toString(), Locale.ENGLISH);
				dateFormt.setTimeZone(TimeZone.getTimeZone("GMT")); //$NON-NLS-1$
				signingTime = dateFormt.parse(dateString);
			} catch (ParseException e) {
				throw (SignatureException) new SignatureException(SignedContentMessages.PKCS7_Parse_Signing_Time).initCause(e);
			}
		}
	}

	private List<Certificate> constructCertPath(List<Certificate> certs, Certificate targetCert) {
		List<Certificate> certsList = new ArrayList<Certificate>();
		certsList.add(targetCert);

		X509Certificate currentCert = (X509Certificate) targetCert;
		int numIteration = certs.size();
		int i = 0;
		while (i < numIteration) {

			X500Principal subject = currentCert.getSubjectX500Principal();
			X500Principal issuer = currentCert.getIssuerX500Principal();

			if (subject.equals(issuer)) {
				// the cert path has been constructed
				break;
			}

			currentCert = null;
			Iterator<Certificate> itr = certs.iterator();

			while (itr.hasNext()) {
				X509Certificate tempCert = (X509Certificate) itr.next();

				if (tempCert.getSubjectX500Principal().equals(issuer)) {
					certsList.add(tempCert);
					currentCert = tempCert;
				}
			}

			i++;
		}

		return certsList;
	}

	public void verifyCerts() throws InvalidKeyException, SignatureException, CertificateException, NoSuchAlgorithmException, NoSuchProviderException {
		if (certificates == null || certificates.length == 0) {
			throw new CertificateException("There are no certificates in the signature block file!"); //$NON-NLS-1$
		}

		int len = certificates.length;

		// check the certs validity and signatures
		for (int i = 0; i < len; i++) {
			X509Certificate currentX509Cert = (X509Certificate) certificates[i];
			if (i == len - 1) {
				if (currentX509Cert.getSubjectDN().equals(currentX509Cert.getIssuerDN()))
					currentX509Cert.verify(currentX509Cert.getPublicKey());
			} else {
				X509Certificate nextX509Cert = (X509Certificate) certificates[i + 1];
				currentX509Cert.verify(nextX509Cert.getPublicKey());
			}
		}
	}

	private Certificate processSignerInfos(BERProcessor bp, List<Certificate> certs) throws CertificateException, NoSuchAlgorithmException, SignatureException {
		// We assume there is only one SingerInfo element 

		// PKCS7: SignerINFOS processing
		bp = bp.stepInto(); // Step into the set of signerinfos
		bp = bp.stepInto(); // Step into the signerinfo sequence

		// make sure the version is 1
		BigInteger signerInfoVersion = bp.getIntValue();
		if (signerInfoVersion.intValue() != 1) {
			throw new CertificateException(SignedContentMessages.PKCS7_SignerInfo_Version_Not_Supported);
		}

		// PKCS7: version CMSVersion 
		bp.stepOver(); // Skip the version

		// PKCS7: sid [SignerIdentifier : issuerAndSerialNumber or subjectKeyIdentifer]
		BERProcessor issuerAndSN = bp.stepInto();
		X500Principal signerIssuer = new X500Principal(new ByteArrayInputStream(issuerAndSN.buffer, issuerAndSN.offset, issuerAndSN.endOffset - issuerAndSN.offset));
		issuerAndSN.stepOver();
		BigInteger sn = issuerAndSN.getIntValue();

		// initilize the newSignerCert to the issuer cert of leaf cert
		Certificate newSignerCert = null;

		Iterator<Certificate> itr = certs.iterator();
		// PKCS7: compuare the issuers in the issuerAndSN BER equals to the issuers in Certs generated at the beginning of this method
		// it seems like there is no neeed, cause both ways use the same set of bytes
		while (itr.hasNext()) {
			X509Certificate cert = (X509Certificate) itr.next();
			if (cert.getIssuerX500Principal().equals(signerIssuer) && cert.getSerialNumber().equals(sn)) {
				newSignerCert = cert;
				break;
			}
		}

		if (newSignerCert == null)
			throw new CertificateException("Signer certificate not in pkcs7block"); //$NON-NLS-1$

		// set the signer cert
		signerCert = newSignerCert;

		// PKCS7: skip over the sid [SignerIdentifier : issuerAndSerialNumber or subjectKeyIdentifer]
		bp.stepOver(); // skip the issuer name and serial number

		// PKCS7: digestAlgorithm DigestAlgorithmIdentifier
		BERProcessor digestAlg = bp.stepInto();
		digestAlgorithm = findDigest(digestAlg.getObjId());

		// PKCS7: check if the next one if context class for signedAttrs
		bp.stepOver(); // skip the digest alg

		// process the signed attributes if there is any
		processSignedAttributes(bp);

		// PKCS7: signatureAlgorithm for this SignerInfo
		BERProcessor encryptionAlg = bp.stepInto();
		signatureAlgorithm = findEncryption(encryptionAlg.getObjId());
		bp.stepOver(); // skip the encryption alg

		// PKCS7: signature
		signature = bp.getBytes();

		// PKCS7: Step into the unsignedAttrs, 
		bp.stepOver();

		// process the unsigned attributes if there is any
		processUnsignedAttributes(bp);

		return newSignerCert;
	}

	private void processUnsignedAttributes(BERProcessor bp) throws SignatureException {

		if (bp.classOfTag == BERProcessor.CONTEXTSPECIFIC_TAGCLASS && bp.tag == 1) {

			// there are some unsignedAttrs are found!!
			unsignedAttrs = new HashMap<int[], byte[]>();

			// step into a set of unsigned attributes, I believe, when steps 
			// into here, the 'poiter' is pointing to the first element
			BERProcessor unsignedAttrsBERS = bp.stepInto();
			do {
				// process the unsignedAttrsBER by getting the attr type first,
				// then the strcuture for the type
				BERProcessor unsignedAttrBER = unsignedAttrsBERS.stepInto();

				// check if it is timestamp attribute type
				int[] objID = unsignedAttrBER.getObjId();
				// if(Arrays.equals(TIMESTAMP_OID, objID)) {
				// System.out.println("This is a timestamp type, to continue");
				// }

				// get the structure for the attribute type
				unsignedAttrBER.stepOver();
				byte[] structure = unsignedAttrBER.getBytes();
				unsignedAttrs.put(objID, structure);
				unsignedAttrsBERS.stepOver();
			} while (!unsignedAttrsBERS.endOfSequence());
		}
	}

	private void processSignedAttributes(BERProcessor bp) throws SignatureException {
		if (bp.classOfTag == BERProcessor.CONTEXTSPECIFIC_TAGCLASS) {

			// process the signed attributes
			signedAttrs = new HashMap<int[], byte[]>();

			BERProcessor signedAttrsBERS = bp.stepInto();
			do {
				BERProcessor signedAttrBER = signedAttrsBERS.stepInto();
				int[] signedAttrObjID = signedAttrBER.getObjId();

				// step over to the attribute value
				signedAttrBER.stepOver();

				byte[] signedAttrStructure = signedAttrBER.getBytes();

				signedAttrs.put(signedAttrObjID, signedAttrStructure);

				signedAttrsBERS.stepOver();
			} while (!signedAttrsBERS.endOfSequence());
			bp.stepOver();
		}
	}

	public Certificate[] getCertificates() {
		return certificates == null ? new Certificate[0] : certificates;
	}

	public void verifySFSignature(byte data[], int dataOffset, int dataLength) throws InvalidKeyException, NoSuchAlgorithmException, SignatureException {
		Signature sig = Signature.getInstance(digestAlgorithm + "with" + signatureAlgorithm); //$NON-NLS-1$
		sig.initVerify(signerCert.getPublicKey());
		sig.update(data, dataOffset, dataLength);
		if (!sig.verify(signature)) {
			throw new SignatureException(NLS.bind(SignedContentMessages.Signature_Not_Verify, signer, file));
		}
	}

	/**
	 * Return a map of signed attributes, the key(objid) = value(PKCSBlock in bytes for the key)
	 * 
	 * @return  map if there is any signed attributes, null otherwise
	 */
	public Map<int[], byte[]> getUnsignedAttrs() {
		return unsignedAttrs;
	}

	/**
	 * Return a map of signed attributes, the key(objid) = value(PKCSBlock in bytes for the key)
	 * 
	 * @return  map if there is any signed attributes, null otherwise
	 */
	public Map<int[], byte[]> getSignedAttrs() {
		return signedAttrs;
	}

	/**
	 * 
	 * @param bp
	 * @return		a List of certificates from target cert to root cert in order
	 * 
	 * @throws CertificateException
	 * @throws SignatureException 
	 */
	private List<Certificate> processCertificates(BERProcessor bp) throws CertificateException, SignatureException {
		List<Certificate> rtvList = new ArrayList<Certificate>(3);

		// Step into the first certificate-element
		BERProcessor certsBERS = bp.stepInto();

		do {
			X509Certificate x509Cert = (X509Certificate) certFact.generateCertificate(new ByteArrayInputStream(certsBERS.buffer, certsBERS.offset, certsBERS.endOffset - certsBERS.offset));

			if (x509Cert != null) {
				rtvList.add(x509Cert);
			}

			// go to the next cert element
			certsBERS.stepOver();
		} while (!certsBERS.endOfSequence());

		//		Collections.reverse(rtvList);
		return rtvList;
	}

	public Date getSigningTime() {
		return signingTime;
	}

	void setTSACertificates(Certificate[] tsaCertificates) {
		this.tsaCertificates = tsaCertificates;
	}

	public Certificate[] getTSACertificates() {
		return (tsaCertificates == null) ? new Certificate[0] : tsaCertificates;
	}

}
