/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.security.CodeSigner;
import java.security.Timestamp;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.zip.ZipFile;
import org.eclipse.osgi.internal.debug.Debug;
import org.eclipse.osgi.signedcontent.InvalidContentException;
import org.eclipse.osgi.signedcontent.SignedContent;
import org.eclipse.osgi.signedcontent.SignedContentEntry;
import org.eclipse.osgi.signedcontent.SignerInfo;
import org.eclipse.osgi.storage.bundlefile.BundleFile;
import org.eclipse.osgi.storage.bundlefile.DirBundleFile;
import org.eclipse.osgi.storage.bundlefile.ZipBundleFile;

public class SignedContentFromBundleFile implements SignedContent {
	static abstract class BaseSignerInfo implements SignerInfo {
		private volatile Certificate trustAnchor = null;

		@Override
		public Certificate getTrustAnchor() {
			return trustAnchor;
		}

		@Override
		public boolean isTrusted() {
			return trustAnchor != null;
		}

		@Deprecated
		@Override
		public String getMessageDigestAlgorithm() {
			return "unknown"; //$NON-NLS-1$
		}

		void setTrustAnchor(Certificate anchor) {
			this.trustAnchor = anchor;
		}
	}

	static class TimestampSignerInfo extends BaseSignerInfo {
		private final Timestamp timestamp;

		public TimestampSignerInfo(Timestamp timestamp) {
			this.timestamp = timestamp;
		}

		@Override
		public Certificate[] getCertificateChain() {
			return timestamp.getSignerCertPath().getCertificates().toArray(new Certificate[0]);
		}

		Date getTimestamp() {
			return timestamp.getTimestamp();
		}
	}

	static class CodeSignerInfo extends BaseSignerInfo {
		private final CodeSigner codeSigner;
		private final TimestampSignerInfo timestamp;

		public CodeSignerInfo(CodeSigner codeSigner) {
			this.codeSigner = codeSigner;
			Timestamp ts = codeSigner.getTimestamp();
			this.timestamp = ts == null ? null : new TimestampSignerInfo(ts);
		}

		@Override
		public Certificate[] getCertificateChain() {
			return codeSigner.getSignerCertPath().getCertificates().toArray(new Certificate[0]);
		}

		TimestampSignerInfo getTSASignerInfo() {
			return timestamp;
		}
	}

	static class CodeSignerEntry implements SignedContentEntry {
		private final String name;
		private final List<CodeSignerInfo> signerInfos;

		public CodeSignerEntry(List<CodeSignerInfo> signerInfos, String name) {
			this.name = name;
			this.signerInfos = signerInfos;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public SignerInfo[] getSignerInfos() {
			return signerInfos.toArray(new SignerInfo[0]);
		}

		@Override
		public boolean isSigned() {
			return !signerInfos.isEmpty();
		}

		@Override
		public void verify() throws IOException, InvalidContentException {
			// already verified
		}
	}

	static class CorruptEntry implements SignedContentEntry {
		final InvalidContentException verifyError;
		final String name;

		@Override
		public String getName() {
			return name;
		}

		@Override
		public SignerInfo[] getSignerInfos() {
			return new SignerInfo[0];
		}

		@Override
		public boolean isSigned() {
			return false;
		}

		@Override
		public void verify() throws IOException, InvalidContentException {
			throw verifyError;
		}

		public CorruptEntry(InvalidContentException verifyError, String name) {
			super();
			this.verifyError = verifyError;
			this.name = name;
		}

	}

	private final List<CodeSignerInfo> signerInfos = new ArrayList<>();
	private final Map<String, SignedContentEntry> signedEntries;

	public SignedContentFromBundleFile(BundleFile bundleFile) throws IOException {
		signedEntries = getSignedEntries(() -> {
			try {
				return getJarInputStream(bundleFile);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}, () -> bundleFile, signerInfos);
	}

	public SignedContentFromBundleFile(File bundleFile, Debug debug) throws IOException {
		DirBundleFile tmpDirBundleFile = null;
		if (bundleFile.isDirectory()) {
			try {
				tmpDirBundleFile = new DirBundleFile(bundleFile, false);
			} catch (IOException e) {
				// ignore and move on
			}
		}
		DirBundleFile dirBundleFile = tmpDirBundleFile;
		signedEntries = getSignedEntries(() -> {
			try {
				if (dirBundleFile != null) {
					return getJarInputStream(dirBundleFile);
				}
				return new FileInputStream(bundleFile);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}, () -> {
			try {
				if (dirBundleFile != null) {
					return dirBundleFile;
				}
				// Make sure we have a ZipFile first, this will throw an IOException if not
				// valid.
				// Use SecureAction because it gives better errors about the path on exceptions
				ZipFile temp = SignedBundleHook.secureAction.getZipFile(bundleFile, false);
				temp.close();
				return new ZipBundleFile(bundleFile, null, null, debug, false);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}, signerInfos);
	}

	private static Map<String, SignedContentEntry> getSignedEntries(Supplier<InputStream> input,
			Supplier<BundleFile> bundleFile, List<CodeSignerInfo> signerInfos) throws IOException {
		Map<CodeSigner, CodeSignerInfo> codeSigners = new HashMap<>();
		Map<String, SignedContentEntry> signedEntries = new LinkedHashMap<>();
		try (JarInputStream jarInput = new JarInputStream(input.get())) {

			for (JarEntry entry = jarInput.getNextJarEntry(); entry != null; entry = jarInput.getNextJarEntry()) {
				// drain the entry so we can get the code signer
				try {
					for (byte[] drain = new byte[4096]; jarInput.read(drain, 0, drain.length) != -1;) {
						// nothing
					}
					CodeSigner[] signers = entry.getCodeSigners();
					if (signers != null) {
						List<CodeSignerInfo> entryInfos = new ArrayList<>(signers.length);
						for (CodeSigner codeSigner : signers) {
							CodeSignerInfo info = codeSigners.computeIfAbsent(codeSigner, CodeSignerInfo::new);
							entryInfos.add(info);
						}
						CodeSignerEntry signedEntry = new CodeSignerEntry(entryInfos, entry.getName());
						signedEntries.put(entry.getName(), signedEntry);
					}
				} catch (SecurityException | IOException e) {
					// assume corruption
					signedEntries.put(entry.getName(),
							new CorruptEntry(new InvalidContentException(entry.getName(), e), entry.getName()));
				}
			}
		} catch (SecurityException e) {
			Enumeration<String> paths = bundleFile.get().getEntryPaths("", true); //$NON-NLS-1$
			while (paths.hasMoreElements()) {
				String path = paths.nextElement();
				if (!path.endsWith("/") && !signedEntries.containsKey(path)) { //$NON-NLS-1$
					signedEntries.put(path, new CorruptEntry(new InvalidContentException(path, e), path));
				}
			}
		} catch (UncheckedIOException e) {
			throw e.getCause();
		}
		signerInfos.addAll(codeSigners.values());
		return signedEntries;
	}

	private static InputStream getJarInputStream(BundleFile bundleFile) throws IOException {
		File f = bundleFile.getBaseFile();
		if (f == null || f.isDirectory()) {
			return new BundleToJarInputStream(bundleFile);
		}
		return new FileInputStream(f);
	}

	@Override
	public SignedContentEntry[] getSignedEntries() {
		return signedEntries.values().toArray(new SignedContentEntry[0]);
	}

	@Override
	public SignedContentEntry getSignedEntry(String name) {
		return signedEntries.get(name);
	}

	@Override
	public SignerInfo[] getSignerInfos() {
		return signerInfos.toArray(new SignerInfo[0]);
	}

	@Override
	public boolean isSigned() {
		return !signerInfos.isEmpty();
	}

	@Override
	public Date getSigningTime(SignerInfo signerInfo) {
		if (signerInfo instanceof CodeSignerInfo) {
			TimestampSignerInfo tsInfo = ((CodeSignerInfo) signerInfo).getTSASignerInfo();
			if (tsInfo != null) {
				return tsInfo.getTimestamp();
			}
		}
		return null;
	}

	@Override
	public SignerInfo getTSASignerInfo(SignerInfo signerInfo) {
		if (signerInfo instanceof CodeSignerInfo) {
			return ((CodeSignerInfo) signerInfo).getTSASignerInfo();
		}
		return null;
	}

	@Override
	public void checkValidity(SignerInfo signerInfo)
			throws CertificateExpiredException, CertificateNotYetValidException {
		Date signingTime = getSigningTime(signerInfo);
		Certificate[] certs = signerInfo.getCertificateChain();
		for (Certificate cert : certs) {
			if (!(cert instanceof X509Certificate)) {
				continue;
			}
			if (signingTime == null) {
				((X509Certificate) cert).checkValidity();
			} else {
				((X509Certificate) cert).checkValidity(signingTime);
			}
		}
	}

}
