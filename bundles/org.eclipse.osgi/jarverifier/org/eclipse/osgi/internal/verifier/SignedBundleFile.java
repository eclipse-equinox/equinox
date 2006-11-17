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

import java.io.*;
import java.net.URL;
import java.security.*;
import java.security.cert.*;
import java.util.*;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleFile;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.internal.provisional.verifier.*;
import org.eclipse.osgi.util.NLS;

/**
 * This class wraps a Repository of classes and resources to check and enforce
 * signatures. It requires full signing of the manifest by all signers. If no
 * signatures are found, the classes and resources are retrieved without checks.
 */
public class SignedBundleFile extends BundleFile implements CertificateVerifier, JarVerifierConstant {
	/**
	 * A precomputed MD5 MessageDigest. We will clone this everytime we want to
	 * use it.
	 */
	private static MessageDigest md5;
	/**
	 * A precomputed SHA1 MessageDigest. We will clone this everytime we want to
	 * use it.
	 */
	private static MessageDigest sha1;

	private static DefaultTrustAuthority trustAllAuthority = new DefaultTrustAuthority(0);
	private BundleFile bundleFile;
	CertificateChain[] chains;

	/**
	 * The key of the hashtable will be the name of the entry (type String). The
	 * value will be MessageDigest to use. Before using the MessageDigest
	 * must be cloned.
	 */
	Hashtable digests4entries;
	/**
	 * The key of the hashtable will be the name of the entry (type String). The
	 * value will be byte[] which is an array of one MessageDigest result.
	 */
	Hashtable results4entries;
	String manifestSHAResult = null;
	String manifestMD5Result = null;
	boolean certsInitialized = false;

	SignedBundleFile() {
		// default constructor
	}

	SignedBundleFile(CertificateChain[] chains, Hashtable digests4entries, Hashtable results4entries, String manifestMD5Result, String manifestSHAResult) {
		this.chains = chains;
		this.digests4entries = digests4entries;
		this.results4entries = results4entries;
		this.manifestMD5Result = manifestMD5Result;
		this.manifestSHAResult = manifestSHAResult;
		certsInitialized = true;
		//		isSigned = true;
	}

	/**
	 * Verify the digest listed in each entry in the .SF file with corresponding section in the manifest
	 */
	private void verifyManifestAndSingatureFile(byte[] manifestBytes, byte[] sfBytes) {

		String sf = new String(sfBytes);
		sf = stripContinuations(sf);

		// check if there -Digest-Manfiest: header in the file
		int off = sf.indexOf(digestManifestSearch);
		if (off != -1) {
			int start = sf.lastIndexOf('\n', off);
			String manfiestDigest = null;
			if (start != -1) {
				// Signature-Version has to start the file, so there
				// should always be a newline at the start of
				// Digest-Manifest
				String digestName = sf.substring(start + 1, off);
				if (digestName.equalsIgnoreCase(MD5_STR)) {
					if (manifestMD5Result == null) {
						manifestMD5Result = calculateDigest(getMD5(), manifestBytes);
					}
					manfiestDigest = manifestMD5Result;
				} else if (digestName.equalsIgnoreCase(SHA1_STR)) {
					if (manifestSHAResult == null) {
						manifestSHAResult = calculateDigest(getSHA1(), manifestBytes);
					}
					manfiestDigest = manifestSHAResult;
				}
				off += digestManifestSearchLen;

				// find out the index of first '\n' after the -Digest-Manifest: 
				int nIndex = sf.indexOf('\n', off);
				String digestValue = sf.substring(off, nIndex - 1);

				// check if the the computed digest value of manifest file equals to the digest value in the .sf file
				if (!manfiestDigest.equals(digestValue)) {
					Exception e = new SecurityException(NLS.bind(JarVerifierMessages.Security_File_Is_Tampered, new String[] {bundleFile.getBaseFile().toString()}));
					SignedBundleHook.log(e.getMessage(), FrameworkLogEntry.ERROR, e);
					throw (SecurityException) e;
				}
			}
		}

	}

	/**
	 * Read the .SF file abd assuming that same digest algorithm will be used through out the whole 
	 * .SF file.  That digest algorithm name in the last entry will be returned. 
	 * 
	 * @param SFBuf			a .SF file in bytes 
	 * @return				the digest algorithm name used in the .SF file
	 */
	private String getDigAlgFromSF(byte SFBuf[]) {
		String rtvValue = null;

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
			rtvValue = getMessageDigestName(digestLine);
		}

		return rtvValue;
	}

	/**
	 * @param mfBuf the data from an MF file of a JAR archive
	 * 
	 * This method will populate the "digest type & result" hashtables 
	 * with whatever entries it can correctly parse from the MF file and with the same digest algorithm.
	 * it will 'skip' incorrect entries (TODO: should the correct behavior
	 * be to throw an exception, or return an error code?)...
	 * 
	 * 
	 */
	private void populateManifest(byte mfBuf[], String digAlg) {
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

				String aDigestLine = getDigestLine(entryStr, digAlg);

				if (aDigestLine != null) {
					MessageDigest msgDigestObj = getDigestObjFromString(aDigestLine);
					byte digestResultsList[] = getDigestResultsList(aDigestLine);

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

					if (digests4entries == null) {
						digests4entries = new Hashtable(10);
						results4entries = new Hashtable(10);
					}
					// TODO throw exception if duplicate entry??
					// no need, in theory, there is impossible to have two
					// duplicate entries unless the manifest file
					// is tampered
					if (!digests4entries.contains(entryName)) {
						digests4entries.put(entryName, msgDigestObj);
						results4entries.put(entryName, digestResultsList);
					}

				} // could get lines of digest entries in this MF file entry

			} // could retrieve entry name

			// increment the offset to the ending entry...
			entryStartOffset = entryEndOffset;
		}
	}

	private String stripContinuations(String entry) {
		if (entry.indexOf("\n ") < 0) //$NON-NLS-1$
			return entry;
		StringBuffer buffer = new StringBuffer(entry.length());
		int cont = entry.indexOf("\n "); //$NON-NLS-1$
		int start = 0;
		while (cont >= 0) {
			buffer.append(entry.substring(start, cont - 1));
			start = cont + 2;
			cont = cont + 2 < entry.length() ? entry.indexOf("\n ", cont + 2) : -1; //$NON-NLS-1$
		}
		// get the last one continuation
		if (start < entry.length())
			buffer.append(entry.substring(start));
		return buffer.toString();
	}

	private String getEntryFileName(String manifestEntry) {
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
	 * 
	 * @param manifestEntry contains a single MF file entry of the format
	 * 				   "Name: foo"
	 * 				   "MD5-Digest: [base64 encoded MD5 digest data]"
	 * 				   "SHA1-Digest: [base64 encoded SHA1 digest dat]"
	 * 
	 * @param	manifestEntry	a entry contains either one or multiple digest lines
	 * @param	desireDigestAlg	a string representing the desire digest value to be returned if there are
	 * 							multiple digest lines.
	 * 							If this value is null, return whatever digest value is in the entry.
	 * 
	 * @return this function returns a digest line based on the desire digest algorithm value
	 * 		   (since only MD5 and SHA1 are recognized here),
	 * 		   or a 'null' will be returned if none of the digest algorithms
	 * 		   were recognized.
	 */
	private String getDigestLine(String manifestEntry, String desireDigestAlg) {
		String rtvValue = null;

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
			if (desireDigestAlg != null && desireDigestAlg.equalsIgnoreCase(digAlg)) {
				rtvValue = digestLine;
				break;
			}

			// desireDigestAlg is null, always return the
			rtvValue = digestLine;

			// iterate to next digest line in this entry
			indexDigest = manifestEntry.indexOf(MF_DIGEST_PART, indexEnd);
		}

		// if we couldn't find any digest lines, then we are done
		return rtvValue;
	}

	private MessageDigest getDigestObjFromString(String digestLines) {
		MessageDigest mdList = null;

		if (digestLines != null) {
			// String sDigestLine = digestLines[i];
			int indexDigest = digestLines.indexOf(MF_DIGEST_PART);
			String sDigestAlgType = digestLines.substring(0, indexDigest);
			if (sDigestAlgType.equalsIgnoreCase(MD5_STR)) {
				// remember the "algorithm type" object
				mdList = getMD5();
			} else if (sDigestAlgType.equalsIgnoreCase(SHA1_STR)) {
				// remember the "algorithm type" object
				mdList = getSHA1();
			} else {
				// unknown algorithm type, we will stop processing this entry
				// break;
				throw new SecurityException(NLS.bind(JarVerifierMessages.Algorithm_Not_Supported, sDigestAlgType));
			}
		}
		return mdList;
	}

	/**
	 * Return the Message Digest name
	 * 
	 * @param digLine		the message digest line is in the following format.  That is in the 
	 * 						following format:
	 * 								DIGEST_NAME-digest: digest value
	 * @return				a string representing a message digest.
	 */
	private String getMessageDigestName(String digLine) {
		String rtvValue = null;
		if (digLine != null) {
			int indexDigest = digLine.indexOf(MF_DIGEST_PART);
			if (indexDigest != -1) {
				rtvValue = digLine.substring(0, indexDigest);
			}
		}
		return rtvValue;
	}

	private byte[] getDigestResultsList(String digestLines) {
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

	static private int readFully(InputStream is, byte b[]) throws IOException {
		int count = b.length;
		int offset = 0;
		int rc;
		while ((rc = is.read(b, offset, count)) > 0) {
			count -= rc;
			offset += rc;
		}
		return offset;
	}

	byte[] readIntoArray(BundleEntry be) throws IOException {
		int size = (int) be.getSize();
		InputStream is = be.getInputStream();
		byte b[] = new byte[size];
		int rc = readFully(is, b);
		if (rc != size) {
			throw new IOException("Couldn't read all of " + be.getName() + ": " + rc + " != " + size); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		return b;
	}

	/**
	 * Sets the BundleFile for this singed bundle. It will extract
	 * signatures and digests from the bundle file and validate input streams
	 * before using them from the bundle file.
	 * 
	 * @param bundleFile the BundleFile to extract elements from.
	 * @param the support flags for this signed bundle
	 * @throws IOException
	 */
	void setBundleFile(BundleFile bundleFile, int supportFlags) throws IOException {
		this.bundleFile = bundleFile;
		if (certsInitialized)
			return;
		BundleEntry be = bundleFile.getEntry(META_INF_MANIFEST_MF);
		if (be == null)
			return;

		// read all the signature block file names into a list
		Enumeration en = bundleFile.getEntryPaths(META_INF);
		List signers = new ArrayList(2);
		while (en.hasMoreElements()) {
			String name = (String) en.nextElement();
			if ((name.endsWith(DOT_DSA) || name.endsWith(DOT_RSA)) && name.indexOf('/') == name.lastIndexOf('/'))
				signers.add(name);
		}

		// this means the jar is not signed
		if (signers.size() == 0)
			return;

		byte manifestBytes[] = readIntoArray(be);
		// determine the signer to be used for later
		String latestSigner = findLatestSigner(bundleFile, signers);
		// process the signers
		try {
			ArrayList processors = new ArrayList(signers.size());
			Iterator iSigners = signers.iterator();
			for (int i = 0; iSigners.hasNext(); i++) {
				String signer = (String) iSigners.next();
				PKCS7Processor processor = processSigner(bundleFile, manifestBytes, signer, latestSigner, supportFlags);
				boolean error = false;
				try {
					processor.validateCerts();
					determineCertsTrust(processor, supportFlags);
				} catch (CertificateExpiredException e) {
					// ignore
				} catch (CertificateNotYetValidException e) {
					// ignore
				} catch (InvalidKeyException e) {
					error = true;
				}
				if (!error) {
					// make sure the latestSigner is the first in the list
					if (signer == latestSigner)
						processors.add(0, processor);
					else
						processors.add(processor);
				}
			}
			chains = processors.size() == 0 ? null : (CertificateChain[]) processors.toArray(new CertificateChain[processors.size()]);
		} catch (SignatureException e) {
			throw new SecurityException(NLS.bind(JarVerifierMessages.Signature_Not_Verify_1, new String[] {latestSigner, bundleFile.toString()}));
		}
	}

	private void determineCertsTrust(PKCS7Processor signerPKCS7, int supportFlags) {
		CertificateTrustAuthority trustAuthority;
		if ((supportFlags & SignedBundleHook.VERIFY_TRUST) != 0)
			trustAuthority = SignedBundleHook.getTrustAuthority();
		else
			trustAuthority = trustAllAuthority;
		if (trustAuthority != null)
			signerPKCS7.determineTrust(trustAuthority);
	}

	private PKCS7Processor processSigner(BundleFile bf, byte[] manifestBytes, String signer, String latestSigner, int supportFlags) throws IOException, SignatureException {
		BundleEntry be = bf.getEntry(signer);
		byte pkcs7Bytes[] = readIntoArray(be);
		int dotIndex = signer.lastIndexOf('.');
		be = bf.getEntry(signer.substring(0, dotIndex) + DOT_SF);
		byte sfBytes[] = readIntoArray(be);

		// Step 1, verify the .SF file is signed by the private key that corresponds to the public key 
		// in the .RSA/.DSA file
		PKCS7Processor chain = null;
		try {

			chain = new PKCS7Processor(pkcs7Bytes, 0, pkcs7Bytes.length);

			// call the Step 1 in the Jar File Verification algorithm
			chain.verifySFSignature(sfBytes, 0, sfBytes.length);

			// algorithm used
			String digAlg = getDigAlgFromSF(sfBytes);
			if (digAlg == null) {
				throw new SecurityException(NLS.bind(JarVerifierMessages.SF_File_Parsing_Error, new String[] {bf.toString()}));
			}
			// populate the two digest hashtable instance variable based on the used Message Digest algorithm
			if (latestSigner == signer) { // only do this if it is the latest signer

				// Process the Step 2 in the Jar File Verification algorithm
				// Get the manifest out of the signature file and make sure
				// it matches MANIFEST.MF
				verifyManifestAndSingatureFile(manifestBytes, sfBytes);

				// only populate the manifests if we are verifying content at runtime
				if ((supportFlags & SignedBundleHook.VERIFY_RUNTIME) != 0)
					populateManifest(manifestBytes, digAlg);
			}

			// process the Step 3
			// read each file in the JAR file that has an entry in the .SF file.  
			// also determine the 
			//				verifySFEntriesDigest(bf, sfBytes, manifestBytes);
		} catch (InvalidKeyException e) {
			SignedBundleHook.log(e.getMessage(), FrameworkLogEntry.ERROR, e);
			throw new SecurityException(NLS.bind(JarVerifierMessages.Invalid_Key_Exception, new String[] {bf.getBaseFile().toString(), e.getMessage()}));
		} catch (CertificateException e) {
			SignedBundleHook.log(e.getMessage(), FrameworkLogEntry.ERROR, e);
			throw new SecurityException(NLS.bind(JarVerifierMessages.PKCS7_Cert_Excep, new String[] {bf.getBaseFile().toString(), e.getMessage()}));
		} catch (NoSuchAlgorithmException e) {
			SignedBundleHook.log(e.getMessage(), FrameworkLogEntry.ERROR, e);
			throw new SecurityException(NLS.bind(JarVerifierMessages.PKCS7_No_Such_Algorithm, new String[] {bf.getBaseFile().toString(), e.getMessage()}));
		}

		return chain;
	}

	private String findLatestSigner(BundleFile bf, List names) {
		String result = null;
		long latestTime = Long.MIN_VALUE;
		for (Iterator iNames = names.iterator(); iNames.hasNext();) {
			String name = (String) iNames.next();
			BundleEntry entry = bf.getEntry(name);
			if (entry.getTime() > latestTime) {
				result = name;
				latestTime = entry.getTime();
			}
		}
		return result;
	}

	/**
	 * Returns the Base64 encoded digest of the passed set of bytes.
	 */
	private String calculateDigest(MessageDigest digest, byte[] bytes) {
		String result;
		try {
			digest = (MessageDigest) digest.clone();
			result = new String(Base64.encode(digest.digest(bytes)));
		} catch (CloneNotSupportedException e1) {
			// Won't happen since clone is supported by
			// MessageDigest
			throw new SecurityException(digest.getAlgorithm() + " doesn't support clone()"); //$NON-NLS-1$
		}
		return result;
	}

	public File getFile(String path, boolean nativeCode) {
		return bundleFile.getFile(path, nativeCode);
	}

	public BundleEntry getEntry(String path) {
		BundleEntry be = bundleFile.getEntry(path);
		if (digests4entries == null)
			return be;
		if (be == null) {
			if (digests4entries.get(path) == null)
				return null;
			throw new SecurityException(NLS.bind(JarVerifierMessages.file_is_removed_from_jar, getBaseFile().toString(), path));
		}
		if (be.getName().startsWith(META_INF))
			return be;
		if (!isSigned())
			// If there is no signatures, we just return the regular bundle entry
			return be;
		return new SignedBundleEntry(be);
	}

	public Enumeration getEntryPaths(String path) {
		return bundleFile.getEntryPaths(path);
	}

	public void close() throws IOException {
		bundleFile.close();
	}

	public void open() throws IOException {
		bundleFile.open();
	}

	public boolean containsDir(String dir) {
		return bundleFile.containsDir(dir);
	}

	boolean matchDNChain(String pattern) {
		CertificateChain[] matchChains = getChains();
		for (int i = 0; i < matchChains.length; i++)
			if (matchChains[i].isTrusted() && DNChainMatching.match(matchChains[i].getChain(), pattern))
				return true;
		return false;
	}

	public File getBaseFile() {
		return bundleFile.getBaseFile();
	}

	class SignedBundleEntry extends BundleEntry {
		BundleEntry nestedEntry;

		SignedBundleEntry(BundleEntry nestedEntry) {
			this.nestedEntry = nestedEntry;
		}

		public InputStream getInputStream() throws IOException {
			String name = getName();
			MessageDigest digests = digests4entries == null ? null : (MessageDigest) digests4entries.get(name);
			if (digests == null)
				return null; // return null if the digest does not exist
			byte results[] = (byte[]) results4entries.get(name);

			/**
			 * ELI: it is probbaly best to decode the value of digest into Base64 base.  This will only optimize the 
			 * bad jar case.
			 */

			return new DigestedInputStream(nestedEntry.getInputStream(), digests, results, nestedEntry.getSize());
		}

		public long getSize() {
			return nestedEntry.getSize();
		}

		public String getName() {
			return nestedEntry.getName();
		}

		public long getTime() {
			return nestedEntry.getTime();
		}

		public URL getLocalURL() {
			return nestedEntry.getLocalURL();
		}

		public URL getFileURL() {
			return nestedEntry.getFileURL();
		}

	}

	public void checkContent() throws CertificateException, CertificateExpiredException, SignatureException {
		if (!isSigned() || digests4entries == null)
			return;

		for (Enumeration entries = digests4entries.keys(); entries.hasMoreElements();) {
			String name = (String) entries.nextElement();
			BundleEntry entry = getEntry(name);
			if (entry == null) {
				throw new SecurityException(NLS.bind(JarVerifierMessages.Jar_Is_Tampered, bundleFile.getBaseFile().getName()));
			}
			try {
				entry.getBytes();
			} catch (IOException e) {
				SignedBundleHook.log(e.getMessage(), FrameworkLogEntry.ERROR, e);
				throw new SecurityException(NLS.bind(JarVerifierMessages.File_In_Jar_Is_Tampered, new String[] {name, bundleFile.getBaseFile().toString()}));
			}
		}

		// validate the certs chain and determine the trust
		for (int i = 0; i < chains.length; i++) {
			PKCS7Processor signerPKCS7 = (PKCS7Processor) chains[i];
			try {
				signerPKCS7.validateCerts();
			} catch (InvalidKeyException e) {
				throw new CertificateException(e.getMessage());
			}
			// determine the trust of certificates
			determineCertsTrust(signerPKCS7, SignedBundleHook.VERIFY_ALL);
		}
	}

	public String[] verifyContent() {
		if (!isSigned() || digests4entries == null)
			return EMPTY_STRING;
		ArrayList corrupted = new ArrayList(0); // be optimistic
		for (Enumeration entries = digests4entries.keys(); entries.hasMoreElements();) {
			String name = (String) entries.nextElement();
			BundleEntry entry = getEntry(name);
			if (entry == null)
				corrupted.add(name); // we expected the entry to be here
			else
				try {
					entry.getBytes();
				} catch (IOException e) {
					// must be invalid
					corrupted.add(name);
				}
		}
		return corrupted.size() == 0 ? EMPTY_STRING : (String[]) corrupted.toArray(new String[corrupted.size()]);
	}

	public CertificateChain[] getChains() {
		if (!isSigned())
			return new CertificateChain[0];
		return chains;
	}

	public boolean isSigned() {
		return chains != null;
	}

	static synchronized MessageDigest getMD5() {
		if (md5 == null)
			try {
				md5 = MessageDigest.getInstance(MD5_STR);
			} catch (NoSuchAlgorithmException e) {
				SignedBundleHook.log(e.getMessage(), FrameworkLogEntry.ERROR, e);
			}
		return md5;
	}

	static synchronized MessageDigest getSHA1() {
		if (sha1 == null)
			try {
				sha1 = MessageDigest.getInstance(SHA1_STR);
			} catch (NoSuchAlgorithmException e) {
				SignedBundleHook.log(e.getMessage(), FrameworkLogEntry.ERROR, e);
			}
		return sha1;
	}

	//	static public void main(String args[]) throws IOException {
	//
	//		ZipBundleFile jf = new ZipBundleFile(new File(args[0]), null);
	//		SignedBundleFile sr = new SignedBundleFile();
	//
	//		sr.setBundleFile(jf);
	//
	//		// read the first level directory entries
	//		Enumeration en = sr.getEntryPaths("/"); //$NON-NLS-1$
	//		while (en.hasMoreElements()) {
	//			String filePath = (String) en.nextElement();
	//			System.out.println("main(): " + filePath); //$NON-NLS-1$
	//
	//			// if this file is not a directory file
	//			// then we'll get its input stream for testing
	//			if (filePath.indexOf('/') == -1) {
	//				BundleEntry be = sr.getEntry(filePath);
	//				InputStream is = be.getInputStream();
	//				is.skip(be.getSize());
	//				is.read();
	//				is.close();
	//			}
	//		}
	//
	//		if (!sr.isSigned()) {
	//			System.out.println("No signers present"); //$NON-NLS-1$
	//		} else {
	//			CertificateChain[] chains = sr.getChains();
	//			for (int i = 0; i < chains.length; i++) {
	//				System.out.println(chains[i].getChain());
	//			}
	//		}
	//
	//		System.out.println("Done"); //$NON-NLS-1$		
	//	}
}
