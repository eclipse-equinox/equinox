/*******************************************************************************
 * Copyright (c) 2007, 2012 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.osgi.internal.signedcontent;

import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.*;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleFile;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.signedcontent.SignerInfo;
import org.eclipse.osgi.util.NLS;

public class SignatureBlockProcessor implements SignedContentConstants {
	private final SignedBundleFile signedBundle;
	private List<SignerInfo> signerInfos = new ArrayList<SignerInfo>();
	private Map<String, Object> contentMDResults = new HashMap<String, Object>();
	// map of tsa singers keyed by SignerInfo -> {tsa_SignerInfo, signingTime}
	private Map<SignerInfo, Object[]> tsaSignerInfos;
	private final int supportFlags;

	public SignatureBlockProcessor(SignedBundleFile signedContent, int supportFlags) {
		this.signedBundle = signedContent;
		this.supportFlags = supportFlags;
	}

	public SignedContentImpl process() throws IOException, InvalidKeyException, SignatureException, CertificateException, NoSuchAlgorithmException, NoSuchProviderException {
		BundleFile wrappedBundleFile = signedBundle.getWrappedBundleFile();
		BundleEntry be = wrappedBundleFile.getEntry(META_INF_MANIFEST_MF);
		if (be == null)
			return createUnsignedContent();

		// read all the signature block file names into a list
		Enumeration<String> en = wrappedBundleFile.getEntryPaths(META_INF);
		List<String> signers = new ArrayList<String>(2);
		while (en.hasMoreElements()) {
			String name = en.nextElement();
			if ((name.endsWith(DOT_DSA) || name.endsWith(DOT_RSA)) && name.indexOf('/') == name.lastIndexOf('/'))
				signers.add(name);
		}

		// this means the jar is not signed
		if (signers.size() == 0)
			return createUnsignedContent();

		byte manifestBytes[] = readIntoArray(be);
		// process the signers

		for (Iterator<String> iSigners = signers.iterator(); iSigners.hasNext();)
			processSigner(wrappedBundleFile, manifestBytes, iSigners.next());

		// done processing now create a SingedContent to return
		SignerInfo[] allSigners = signerInfos.toArray(new SignerInfo[signerInfos.size()]);
		for (Iterator<Map.Entry<String, Object>> iResults = contentMDResults.entrySet().iterator(); iResults.hasNext();) {
			Map.Entry<String, Object> entry = iResults.next();
			@SuppressWarnings("unchecked")
			List<Object>[] value = (List<Object>[]) entry.getValue();
			SignerInfo[] entrySigners = value[0].toArray(new SignerInfo[value[0].size()]);
			byte[][] entryResults = value[1].toArray(new byte[value[1].size()][]);
			entry.setValue(new Object[] {entrySigners, entryResults});
		}
		SignedContentImpl result = new SignedContentImpl(allSigners, (supportFlags & SignedBundleHook.VERIFY_RUNTIME) != 0 ? contentMDResults : null);
		result.setContent(signedBundle);
		result.setTSASignerInfos(tsaSignerInfos);
		return result;
	}

	private SignedContentImpl createUnsignedContent() {
		SignedContentImpl result = new SignedContentImpl(new SignerInfo[0], contentMDResults);
		result.setContent(signedBundle);
		return result;
	}

	private void processSigner(BundleFile bf, byte[] manifestBytes, String signer) throws IOException, SignatureException, InvalidKeyException, CertificateException, NoSuchAlgorithmException, NoSuchProviderException {
		BundleEntry be = bf.getEntry(signer);
		byte pkcs7Bytes[] = readIntoArray(be);
		int dotIndex = signer.lastIndexOf('.');
		be = bf.getEntry(signer.substring(0, dotIndex) + DOT_SF);
		byte sfBytes[] = readIntoArray(be);

		// Step 1, verify the .SF file is signed by the private key that corresponds to the public key 
		// in the .RSA/.DSA file
		String baseFile = bf.getBaseFile() != null ? bf.getBaseFile().toString() : null;
		PKCS7Processor processor = new PKCS7Processor(pkcs7Bytes, 0, pkcs7Bytes.length, signer, baseFile);
		// call the Step 1 in the Jar File Verification algorithm
		processor.verifySFSignature(sfBytes, 0, sfBytes.length);
		// algorithm used
		String digAlg = getDigAlgFromSF(sfBytes);
		if (digAlg == null)
			throw new SignatureException(NLS.bind(SignedContentMessages.SF_File_Parsing_Error, new String[] {bf.toString()}));
		// get the digest results
		// Process the Step 2 in the Jar File Verification algorithm
		// Get the manifest out of the signature file and make sure
		// it matches MANIFEST.MF
		verifyManifestAndSignatureFile(manifestBytes, sfBytes);

		// create a SignerInfo with the processed information
		SignerInfoImpl signerInfo = new SignerInfoImpl(processor.getCertificates(), null, digAlg);
		if ((supportFlags & SignedBundleHook.VERIFY_RUNTIME) != 0)
			// only populate the manifests digest information for verifying content at runtime
			populateMDResults(manifestBytes, signerInfo);
		signerInfos.add(signerInfo);
		// check for tsa signers
		Certificate[] tsaCerts = processor.getTSACertificates();
		Date signingTime = processor.getSigningTime();
		if (tsaCerts != null && signingTime != null) {
			SignerInfoImpl tsaSignerInfo = new SignerInfoImpl(tsaCerts, null, digAlg);
			if (tsaSignerInfos == null)
				tsaSignerInfos = new HashMap<SignerInfo, Object[]>(2);
			tsaSignerInfos.put(signerInfo, new Object[] {tsaSignerInfo, signingTime});
		}
	}

	/**
	 * Verify the digest listed in each entry in the .SF file with corresponding section in the manifest
	 * @throws SignatureException 
	 */
	private void verifyManifestAndSignatureFile(byte[] manifestBytes, byte[] sfBytes) throws SignatureException {

		String sf = new String(sfBytes);
		sf = stripContinuations(sf);

		// check if there -Digest-Manfiest: header in the file
		int off = sf.indexOf(digestManifestSearch);
		if (off != -1) {
			int start = sf.lastIndexOf('\n', off);
			String manifestDigest = null;
			if (start != -1) {
				// Signature-Version has to start the file, so there
				// should always be a newline at the start of
				// Digest-Manifest
				String digestName = sf.substring(start + 1, off);
				if (digestName.equalsIgnoreCase(MD5_STR))
					manifestDigest = calculateDigest(getMessageDigest(MD5_STR), manifestBytes);
				else if (digestName.equalsIgnoreCase(SHA1_STR))
					manifestDigest = calculateDigest(getMessageDigest(SHA1_STR), manifestBytes);
				else
					manifestDigest = calculateDigest(getMessageDigest(digestName), manifestBytes);
				off += digestManifestSearchLen;

				// find out the index of first '\n' after the -Digest-Manifest: 
				int nIndex = sf.indexOf('\n', off);
				String digestValue = sf.substring(off, nIndex - 1);

				// check if the the computed digest value of manifest file equals to the digest value in the .sf file
				if (!digestValue.equals(manifestDigest)) {
					SignatureException se = new SignatureException(NLS.bind(SignedContentMessages.Security_File_Is_Tampered, new String[] {signedBundle.getBaseFile().toString()}));
					SignedBundleHook.log(se.getMessage(), FrameworkLogEntry.ERROR, se);
					throw se;
				}
			}
		}
	}

	private void populateMDResults(byte mfBuf[], SignerInfo signerInfo) throws NoSuchAlgorithmException {
		// need to make a string from the MF file data bytes
		String mfStr = new String(mfBuf);

		// start parsing each entry in the MF String
		int entryStartOffset = mfStr.indexOf(MF_ENTRY_NEWLN_NAME);
		int length = mfStr.length();

		while ((entryStartOffset != -1) && (entryStartOffset < length)) {

			// get the start of the next 'entry', i.e. the end of this entry
			int entryEndOffset = mfStr.indexOf(MF_ENTRY_NEWLN_NAME, entryStartOffset + 1);
			if (entryEndOffset == -1) {
				// if there is no next entry, then the end of the string
				// is the end of this entry
				entryEndOffset = mfStr.length();
			}

			// get the string for this entry only, since the entryStartOffset
			// points to the '\n' befor the 'Name: ' we increase it by 1
			// this is guaranteed to not go past end-of-string and be less
			// then entryEndOffset.
			String entryStr = mfStr.substring(entryStartOffset + 1, entryEndOffset);
			entryStr = stripContinuations(entryStr);

			// entry points to the start of the next 'entry'
			String entryName = getEntryFileName(entryStr);

			// if we could retrieve an entry name, then we will extract
			// digest type list, and the digest value list
			if (entryName != null) {

				String aDigestLine = getDigestLine(entryStr, signerInfo.getMessageDigestAlgorithm());

				if (aDigestLine != null) {
					String msgDigestAlgorithm = getDigestAlgorithmFromString(aDigestLine);
					if (!msgDigestAlgorithm.equalsIgnoreCase(signerInfo.getMessageDigestAlgorithm()))
						continue; // TODO log error?
					byte digestResult[] = getDigestResultsList(aDigestLine);

					//
					// only insert this entry into the table if its
					// "well-formed",
					// i.e. only if we could extract its name, digest types, and
					// digest-results
					//
					// sanity check, if the 2 lists are non-null, then their
					// counts must match
					//
					//					if ((msgDigestObj != null) && (digestResultsList != null)
					//							&& (1 != digestResultsList.length)) {
					//						throw new RuntimeException(
					//								"Errors occurs when parsing the manifest file stream!"); //$NON-NLS-1$
					//					}
					@SuppressWarnings("unchecked")
					List<Object>[] mdResult = (List<Object>[]) contentMDResults.get(entryName);
					if (mdResult == null) {
						@SuppressWarnings("unchecked")
						List<Object>[] arrayLists = new ArrayList[2];
						mdResult = arrayLists;
						mdResult[0] = new ArrayList<Object>();
						mdResult[1] = new ArrayList<Object>();
						contentMDResults.put(entryName, mdResult);
					}
					mdResult[0].add(signerInfo);
					mdResult[1].add(digestResult);
				} // could get lines of digest entries in this MF file entry
			} // could retrieve entry name
				// increment the offset to the ending entry...
			entryStartOffset = entryEndOffset;
		}
	}

	private static byte[] getDigestResultsList(String digestLines) {
		byte resultsList[] = null;
		if (digestLines != null) {
			// for each digest-line retrieve the digest result
			// for (int i = 0; i < digestLines.length; i++) {
			String sDigestLine = digestLines;
			int indexDigest = sDigestLine.indexOf(MF_DIGEST_PART);
			indexDigest += MF_DIGEST_PART.length();
			// if there is no data to extract for this digest value
			// then we will fail...
			if (indexDigest >= sDigestLine.length()) {
				resultsList = null;
				// break;
			}
			// now attempt to base64 decode the result
			String sResult = sDigestLine.substring(indexDigest);
			try {
				resultsList = Base64.decode(sResult.getBytes());
			} catch (Throwable t) {
				// malformed digest result, no longer processing this entry
				resultsList = null;
			}
		}
		return resultsList;
	}

	private static String getDigestAlgorithmFromString(String digestLines) throws NoSuchAlgorithmException {
		if (digestLines != null) {
			// String sDigestLine = digestLines[i];
			int indexDigest = digestLines.indexOf(MF_DIGEST_PART);
			String sDigestAlgType = digestLines.substring(0, indexDigest);
			if (sDigestAlgType.equalsIgnoreCase(MD5_STR)) {
				// remember the "algorithm type"
				return MD5_STR;
			} else if (sDigestAlgType.equalsIgnoreCase(SHA1_STR)) {
				// remember the "algorithm type" object
				return SHA1_STR;
			} else {
				return sDigestAlgType;
			}
		}
		return null;
	}

	private static String getEntryFileName(String manifestEntry) {
		// get the beginning of the name
		int nameStart = manifestEntry.indexOf(MF_ENTRY_NAME);
		if (nameStart == -1) {
			return null;
		}
		// check where the name ends
		int nameEnd = manifestEntry.indexOf('\n', nameStart);
		if (nameEnd == -1) {
			return null;
		}
		// if there is a '\r' before the '\n', then we'll strip it
		if (manifestEntry.charAt(nameEnd - 1) == '\r') {
			nameEnd--;
		}
		// get to the beginning of the actual name...
		nameStart += MF_ENTRY_NAME.length();
		if (nameStart >= nameEnd) {
			return null;
		}
		return manifestEntry.substring(nameStart, nameEnd);
	}

	/**
	 * Returns the Base64 encoded digest of the passed set of bytes.
	 */
	private static String calculateDigest(MessageDigest digest, byte[] bytes) {
		return new String(Base64.encode(digest.digest(bytes)));
	}

	static synchronized MessageDigest getMessageDigest(String algorithm) {
		try {
			return MessageDigest.getInstance(algorithm);
		} catch (NoSuchAlgorithmException e) {
			SignedBundleHook.log(e.getMessage(), FrameworkLogEntry.ERROR, e);
		}
		return null;
	}

	/**
	 * Read the .SF file abd assuming that same digest algorithm will be used through out the whole 
	 * .SF file.  That digest algorithm name in the last entry will be returned. 
	 * 
	 * @param SFBuf			a .SF file in bytes 
	 * @return				the digest algorithm name used in the .SF file
	 */
	private static String getDigAlgFromSF(byte SFBuf[]) {
		// need to make a string from the MF file data bytes
		String mfStr = new String(SFBuf);
		String entryStr = null;

		// start parsing each entry in the MF String
		int entryStartOffset = mfStr.indexOf(MF_ENTRY_NEWLN_NAME);
		int length = mfStr.length();

		while ((entryStartOffset != -1) && (entryStartOffset < length)) {

			// get the start of the next 'entry', i.e. the end of this entry
			int entryEndOffset = mfStr.indexOf(MF_ENTRY_NEWLN_NAME, entryStartOffset + 1);
			if (entryEndOffset == -1) {
				// if there is no next entry, then the end of the string
				// is the end of this entry
				entryEndOffset = mfStr.length();
			}

			// get the string for this entry only, since the entryStartOffset
			// points to the '\n' befor the 'Name: ' we increase it by 1
			// this is guaranteed to not go past end-of-string and be less
			// then entryEndOffset.
			entryStr = mfStr.substring(entryStartOffset + 1, entryEndOffset);
			entryStr = stripContinuations(entryStr);
			break;
		}

		if (entryStr != null) {
			// process the entry to retrieve the digest algorith name
			String digestLine = getDigestLine(entryStr, null);

			// throw parsing
			return getMessageDigestName(digestLine);
		}
		return null;
	}

	/**
	 * 
	 * @param manifestEntry contains a single MF file entry of the format
	 * 				   "Name: foo"
	 * 				   "MD5-Digest: [base64 encoded MD5 digest data]"
	 * 				   "SHA1-Digest: [base64 encoded SHA1 digest dat]"
	 * 
	 * @param	desireDigestAlg	a string representing the desire digest value to be returned if there are
	 * 							multiple digest lines.
	 * 							If this value is null, return whatever digest value is in the entry.
	 * 
	 * @return this function returns a digest line based on the desire digest algorithm value
	 * 		   (since only MD5 and SHA1 are recognized here),
	 * 		   or a 'null' will be returned if none of the digest algorithms
	 * 		   were recognized.
	 */
	private static String getDigestLine(String manifestEntry, String desireDigestAlg) {
		String result = null;

		// find the first digest line
		int indexDigest = manifestEntry.indexOf(MF_DIGEST_PART);
		// if we didn't find any digests at all, then we are done
		if (indexDigest == -1)
			return null;

		// while we continue to find digest entries
		// note: in the following loop we bail if any of the lines
		//		 look malformed...
		while (indexDigest != -1) {
			// see where this digest line begins (look to left)
			int indexStart = manifestEntry.lastIndexOf('\n', indexDigest);
			if (indexStart == -1)
				return null;
			// see where it ends (look to right)
			int indexEnd = manifestEntry.indexOf('\n', indexDigest);
			if (indexEnd == -1)
				return null;
			// strip off ending '\r', if any
			int indexEndToUse = indexEnd;
			if (manifestEntry.charAt(indexEndToUse - 1) == '\r')
				indexEndToUse--;
			// indexStart points to the '\n' before this digest line
			int indexStartToUse = indexStart + 1;
			if (indexStartToUse >= indexEndToUse)
				return null;

			// now this may be a valid digest line, parse it a bit more
			// to see if this is a preferred digest algorithm
			String digestLine = manifestEntry.substring(indexStartToUse, indexEndToUse);
			String digAlg = getMessageDigestName(digestLine);
			if (desireDigestAlg != null) {
				if (desireDigestAlg.equalsIgnoreCase(digAlg))
					return digestLine;
			}

			// desireDigestAlg is null, always return the digestLine
			result = digestLine;

			// iterate to next digest line in this entry
			indexDigest = manifestEntry.indexOf(MF_DIGEST_PART, indexEnd);
		}

		// if we couldn't find any digest lines, then we are done
		return result;
	}

	/**
	 * Return the Message Digest name
	 * 
	 * @param digLine		the message digest line is in the following format.  That is in the 
	 * 						following format:
	 * 								DIGEST_NAME-digest: digest value
	 * @return				a string representing a message digest.
	 */
	private static String getMessageDigestName(String digLine) {
		String rtvValue = null;
		if (digLine != null) {
			int indexDigest = digLine.indexOf(MF_DIGEST_PART);
			if (indexDigest != -1) {
				rtvValue = digLine.substring(0, indexDigest);
			}
		}
		return rtvValue;
	}

	private static String stripContinuations(String entry) {
		if (entry.indexOf("\n ") < 0 && entry.indexOf("\r ") < 0) //$NON-NLS-1$//$NON-NLS-2$
			return entry;
		StringBuffer buffer = new StringBuffer(entry);
		removeAll(buffer, "\r\n "); //$NON-NLS-1$
		removeAll(buffer, "\n "); //$NON-NLS-1$
		removeAll(buffer, "\r "); //$NON-NLS-1$
		return buffer.toString();
	}

	private static StringBuffer removeAll(StringBuffer buffer, String toRemove) {
		int index = buffer.indexOf(toRemove);
		int length = toRemove.length();
		while (index > 0) {
			buffer.replace(index, index + length, ""); //$NON-NLS-1$
			index = buffer.indexOf(toRemove, index);
		}
		return buffer;
	}

	private static byte[] readIntoArray(BundleEntry be) throws IOException {
		int size = (int) be.getSize();
		InputStream is = be.getInputStream();
		try {
			byte b[] = new byte[size];
			int rc = readFully(is, b);
			if (rc != size) {
				throw new IOException("Couldn't read all of " + be.getName() + ": " + rc + " != " + size); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			return b;
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				// do nothing;
			}
		}
	}

	private static int readFully(InputStream is, byte b[]) throws IOException {
		int count = b.length;
		int offset = 0;
		int rc;
		while ((rc = is.read(b, offset, count)) > 0) {
			count -= rc;
			offset += rc;
		}
		return offset;
	}
}
