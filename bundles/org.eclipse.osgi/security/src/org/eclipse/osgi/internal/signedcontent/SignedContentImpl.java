/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.osgi.internal.signedcontent;

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.*;
import java.util.*;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleFile;
import org.eclipse.osgi.signedcontent.*;
import org.eclipse.osgi.util.NLS;

public class SignedContentImpl implements SignedContent {
	final static SignerInfo[] EMPTY_SIGNERINFO = new SignerInfo[0];
	// the content which is signed
	volatile SignedBundleFile content; // TODO can this be more general?
	// the content entry md results used for entry content verification
	// keyed by entry path -> {SignerInfo[] infos, byte[][] results)}
	private final Map<String, Object> contentMDResults;
	private final SignerInfo[] signerInfos;
	// map of tsa singers keyed by SignerInfo -> {tsa_SignerInfo, signingTime}
	private Map<SignerInfo, Object[]> tsaSignerInfos;
	volatile private boolean checkedValid = false;

	public SignedContentImpl(SignerInfo[] signerInfos, Map<String, Object> contentMDResults) {
		this.signerInfos = signerInfos == null ? EMPTY_SIGNERINFO : signerInfos;
		this.contentMDResults = contentMDResults;
	}

	public SignedContentEntry[] getSignedEntries() {
		if (contentMDResults == null)
			return new SignedContentEntry[0];
		List<SignedContentEntry> results = new ArrayList<SignedContentEntry>(contentMDResults.size());
		for (Map.Entry<String, Object> entry : contentMDResults.entrySet()) {
			String entryName = entry.getKey();
			Object[] mdResult = (Object[]) entry.getValue();
			results.add(new SignedContentEntryImpl(entryName, (SignerInfo[]) mdResult[0]));
		}
		return results.toArray(new SignedContentEntry[results.size()]);
	}

	public SignedContentEntry getSignedEntry(String name) {
		if (contentMDResults == null)
			return null;
		Object[] mdResult = (Object[]) contentMDResults.get(name);
		return mdResult == null ? null : new SignedContentEntryImpl(name, (SignerInfo[]) mdResult[0]);
	}

	public SignerInfo[] getSignerInfos() {
		return signerInfos;
	}

	public Date getSigningTime(SignerInfo signerInfo) {
		if (tsaSignerInfos == null)
			return null;
		Object[] tsaInfo = tsaSignerInfos.get(signerInfo);
		return tsaInfo == null ? null : (Date) tsaInfo[1];
	}

	public SignerInfo getTSASignerInfo(SignerInfo signerInfo) {
		if (tsaSignerInfos == null)
			return null;
		Object[] tsaInfo = tsaSignerInfos.get(signerInfo);
		return tsaInfo == null ? null : (SignerInfo) tsaInfo[0];
	}

	public boolean isSigned() {
		return signerInfos.length > 0;
	}

	public void checkValidity(SignerInfo signer) throws CertificateExpiredException, CertificateNotYetValidException {
		Date signingTime = getSigningTime(signer);
		if (checkedValid)
			return;
		Certificate[] certs = signer.getCertificateChain();
		for (int i = 0; i < certs.length; i++) {
			if (!(certs[i] instanceof X509Certificate))
				continue;
			if (signingTime == null)
				((X509Certificate) certs[i]).checkValidity();
			else
				((X509Certificate) certs[i]).checkValidity(signingTime);
		}
		checkedValid = true;
	}

	void setContent(SignedBundleFile content) {
		this.content = content;
	}

	void setTSASignerInfos(Map<SignerInfo, Object[]> tsaSignerInfos) {
		this.tsaSignerInfos = tsaSignerInfos;
	}

	void addTSASignerInfo(SignerInfo baseInfo, SignerInfo tsaSignerInfo, Date signingTime) {
		// sanity check to make sure the baseInfo is here
		if (!containsInfo(baseInfo))
			throw new IllegalArgumentException("The baseInfo is not found"); //$NON-NLS-1$
		if (tsaSignerInfos == null)
			tsaSignerInfos = new HashMap<SignerInfo, Object[]>(signerInfos.length);
		tsaSignerInfos.put(baseInfo, new Object[] {tsaSignerInfo, signingTime});
	}

	Map<String, Object> getContentMDResults() {
		return contentMDResults;
	}

	private boolean containsInfo(SignerInfo signerInfo) {
		for (int i = 0; i < signerInfos.length; i++)
			if (signerInfo == signerInfos[i])
				return true;
		return false;
	}

	InputStream getDigestInputStream(BundleEntry nestedEntry) throws IOException {
		if (contentMDResults == null)
			return nestedEntry.getInputStream();
		Object[] mdResult = (Object[]) contentMDResults.get(nestedEntry.getName());
		if (mdResult == null)
			return null;
		return new DigestedInputStream(nestedEntry, content, (SignerInfo[]) mdResult[0], (byte[][]) mdResult[1], nestedEntry.getSize());
	}

	public class SignedContentEntryImpl implements SignedContentEntry {
		private final String entryName;
		private final SignerInfo[] entrySigners;

		public SignedContentEntryImpl(String entryName, SignerInfo[] entrySigners) {
			this.entryName = entryName;
			this.entrySigners = entrySigners == null ? EMPTY_SIGNERINFO : entrySigners;
		}

		public String getName() {
			return entryName;
		}

		public SignerInfo[] getSignerInfos() {
			return entrySigners;
		}

		public boolean isSigned() {
			return entrySigners.length > 0;
		}

		public void verify() throws IOException, InvalidContentException {
			BundleFile currentContent = content;
			if (currentContent == null)
				throw new InvalidContentException("The content was not set", null); //$NON-NLS-1$
			BundleEntry entry = null;
			SecurityException exception = null;
			try {
				entry = currentContent.getEntry(entryName);
			} catch (SecurityException e) {
				exception = e;
			}
			if (entry == null)
				throw new InvalidContentException(NLS.bind(SignedContentMessages.file_is_removed_from_jar, entryName, currentContent.getBaseFile().toString()), exception);
			entry.getBytes();
		}
	}
}
