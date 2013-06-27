/*******************************************************************************
 * Copyright (c) 2006, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.internal.signedcontent;

import java.io.*;
import java.net.URL;
import java.security.*;
import java.security.cert.*;
import java.util.Date;
import java.util.Enumeration;
import org.eclipse.osgi.signedcontent.*;
import org.eclipse.osgi.storage.bundlefile.BundleEntry;
import org.eclipse.osgi.storage.bundlefile.BundleFile;
import org.eclipse.osgi.util.NLS;

/**
 * This class wraps a Repository of classes and resources to check and enforce
 * signatures. It requires full signing of the manifest by all signers. If no
 * signatures are found, the classes and resources are retrieved without checks.
 */
public class SignedBundleFile extends BundleFile implements SignedContentConstants, SignedContent {
	private BundleFile wrappedBundleFile;
	SignedContentImpl signedContent;
	private final int supportFlags;
	private final SignedBundleHook signedBundleHook;

	SignedBundleFile(SignedContentImpl signedContent, int supportFlags, SignedBundleHook signedBundleHook) {
		super(null);
		this.signedContent = signedContent;
		this.supportFlags = supportFlags;
		this.signedBundleHook = signedBundleHook;
	}

	void setBundleFile(BundleFile bundleFile) throws IOException, InvalidKeyException, SignatureException, CertificateException, NoSuchAlgorithmException, NoSuchProviderException {
		wrappedBundleFile = bundleFile;
		if (signedContent == null) {
			SignatureBlockProcessor signatureProcessor = new SignatureBlockProcessor(this, supportFlags, signedBundleHook);
			signedContent = signatureProcessor.process();
			if (signedContent != null)
				signedBundleHook.determineTrust(signedContent, supportFlags);
		}
	}

	public File getFile(String path, boolean nativeCode) {
		return wrappedBundleFile.getFile(path, nativeCode);
	}

	public BundleEntry getEntry(String path) {
		// strip off leading slashes so we can ensure the path matches the one provided in the manifest.
		if (path.length() > 0 && path.charAt(0) == '/')
			path = path.substring(1);
		BundleEntry be = wrappedBundleFile.getEntry(path);
		if ((supportFlags & SignedBundleHook.VERIFY_RUNTIME) == 0 || signedContent == null)
			return be;
		if (path.startsWith(META_INF)) {
			int lastSlash = path.lastIndexOf('/');
			if (lastSlash == META_INF.length() - 1) {
				if (path.equals(META_INF_MANIFEST_MF) || path.endsWith(DOT_DSA) || path.endsWith(DOT_RSA) || path.endsWith(DOT_SF) || path.indexOf(SIG_DASH) == META_INF.length())
					return be;
				SignedContentEntry signedEntry = signedContent.getSignedEntry(path);
				if (signedEntry == null)
					// TODO this is to allow 1.4 signed bundles to work, it would be better if we could detect 1.4 signed bundles and only do this for them.
					return be;
			}
		}
		if (be == null) {
			// double check that no signer thinks it should exist
			SignedContentEntry signedEntry = signedContent.getSignedEntry(path);
			if (signedEntry != null)
				throw new SecurityException(NLS.bind(SignedContentMessages.file_is_removed_from_jar, path, getBaseFile().toString()));
			return null;
		}
		return new SignedBundleEntry(be);
	}

	public Enumeration<String> getEntryPaths(String path) {
		return wrappedBundleFile.getEntryPaths(path);
	}

	public void close() throws IOException {
		wrappedBundleFile.close();
	}

	public void open() throws IOException {
		wrappedBundleFile.open();
	}

	public boolean containsDir(String dir) {
		return wrappedBundleFile.containsDir(dir);
	}

	public File getBaseFile() {
		return wrappedBundleFile.getBaseFile();
	}

	class SignedBundleEntry extends BundleEntry {
		BundleEntry nestedEntry;

		SignedBundleEntry(BundleEntry nestedEntry) {
			this.nestedEntry = nestedEntry;
		}

		public InputStream getInputStream() throws IOException {
			InputStream in = signedContent.getDigestInputStream(nestedEntry);
			if (in == null)
				throw new SecurityException("Corrupted file: the digest does not exist for the file " + nestedEntry.getName()); //$NON-NLS-1$
			return in;
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

	BundleFile getWrappedBundleFile() {
		return wrappedBundleFile;
	}

	SignedContentImpl getSignedContent() {
		return signedContent;
	}

	public SignedContentEntry[] getSignedEntries() {
		return signedContent == null ? null : signedContent.getSignedEntries();
	}

	public SignedContentEntry getSignedEntry(String name) {
		return signedContent == null ? null : signedContent.getSignedEntry(name);
	}

	public SignerInfo[] getSignerInfos() {
		return signedContent == null ? null : signedContent.getSignerInfos();
	}

	public Date getSigningTime(SignerInfo signerInfo) {
		return signedContent == null ? null : signedContent.getSigningTime(signerInfo);
	}

	public SignerInfo getTSASignerInfo(SignerInfo signerInfo) {
		return signedContent == null ? null : signedContent.getTSASignerInfo(signerInfo);
	}

	public boolean isSigned() {
		return signedContent == null ? false : signedContent.isSigned();
	}

	public void checkValidity(SignerInfo signerInfo) throws CertificateExpiredException, CertificateNotYetValidException {
		if (signedContent != null)
			signedContent.checkValidity(signerInfo);
	}

}
