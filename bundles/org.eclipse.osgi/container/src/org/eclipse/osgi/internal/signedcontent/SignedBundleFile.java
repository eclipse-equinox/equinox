/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
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

package org.eclipse.osgi.internal.signedcontent;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.util.Date;
import org.eclipse.osgi.signedcontent.SignedContent;
import org.eclipse.osgi.signedcontent.SignedContentEntry;
import org.eclipse.osgi.signedcontent.SignerInfo;
import org.eclipse.osgi.storage.bundlefile.BundleEntry;
import org.eclipse.osgi.storage.bundlefile.BundleFile;
import org.eclipse.osgi.storage.bundlefile.BundleFileWrapper;
import org.eclipse.osgi.util.NLS;

/**
 * This class wraps a Repository of classes and resources to check and enforce
 * signatures. It requires full signing of the manifest by all signers. If no
 * signatures are found, the classes and resources are retrieved without checks.
 */
public class SignedBundleFile extends BundleFileWrapper implements SignedContentConstants, SignedContent {
	SignedContentImpl signedContent;
	private final int supportFlags;
	private final SignedBundleHook signedBundleHook;

	SignedBundleFile(BundleFile bundleFile, SignedContentImpl signedContent, int supportFlags, SignedBundleHook signedBundleHook) {
		super(bundleFile);
		this.signedContent = signedContent;
		this.supportFlags = supportFlags;
		this.signedBundleHook = signedBundleHook;
	}

	void initializeSignedContent() throws IOException, InvalidKeyException, SignatureException, CertificateException, NoSuchAlgorithmException, NoSuchProviderException {
		if (signedContent == null) {
			SignatureBlockProcessor signatureProcessor = new SignatureBlockProcessor(this, supportFlags, signedBundleHook);
			signedContent = signatureProcessor.process();
			if (signedContent != null)
				signedBundleHook.determineTrust(signedContent, supportFlags);
		}
	}

	@Override
	public BundleEntry getEntry(String path) {
		// strip off leading slashes so we can ensure the path matches the one provided in the manifest.
		if (path.length() > 0 && path.charAt(0) == '/')
			path = path.substring(1);
		BundleEntry be = getBundleFile().getEntry(path);
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
				throw new SecurityException(NLS.bind(SignedContentMessages.file_is_removed_from_jar, path, String.valueOf(getBaseFile())));
			return null;
		}
		return new SignedBundleEntry(be);
	}

	class SignedBundleEntry extends BundleEntry {
		BundleEntry nestedEntry;

		SignedBundleEntry(BundleEntry nestedEntry) {
			this.nestedEntry = nestedEntry;
		}

		@Override
		public InputStream getInputStream() throws IOException {
			InputStream in = signedContent.getDigestInputStream(nestedEntry);
			if (in == null)
				throw new SecurityException("Corrupted file: the digest does not exist for the file " + nestedEntry.getName()); //$NON-NLS-1$
			return in;
		}

		@Override
		public long getSize() {
			return nestedEntry.getSize();
		}

		@Override
		public String getName() {
			return nestedEntry.getName();
		}

		@Override
		public long getTime() {
			return nestedEntry.getTime();
		}

		@Override
		public URL getLocalURL() {
			return nestedEntry.getLocalURL();
		}

		@Override
		public URL getFileURL() {
			return nestedEntry.getFileURL();
		}

	}

	SignedContentImpl getSignedContent() {
		return signedContent;
	}

	@Override
	public SignedContentEntry[] getSignedEntries() {
		return signedContent == null ? null : signedContent.getSignedEntries();
	}

	@Override
	public SignedContentEntry getSignedEntry(String name) {
		return signedContent == null ? null : signedContent.getSignedEntry(name);
	}

	@Override
	public SignerInfo[] getSignerInfos() {
		return signedContent == null ? null : signedContent.getSignerInfos();
	}

	@Override
	public Date getSigningTime(SignerInfo signerInfo) {
		return signedContent == null ? null : signedContent.getSigningTime(signerInfo);
	}

	@Override
	public SignerInfo getTSASignerInfo(SignerInfo signerInfo) {
		return signedContent == null ? null : signedContent.getTSASignerInfo(signerInfo);
	}

	@Override
	public boolean isSigned() {
		return signedContent == null ? false : signedContent.isSigned();
	}

	@Override
	public void checkValidity(SignerInfo signerInfo) throws CertificateExpiredException, CertificateNotYetValidException {
		if (signedContent != null)
			signedContent.checkValidity(signerInfo);
	}

}
