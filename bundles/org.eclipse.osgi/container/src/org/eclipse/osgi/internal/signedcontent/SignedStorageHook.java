/*******************************************************************************
 * Copyright (c) 2006, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.osgi.internal.signedcontent;

import java.io.*;
import java.security.cert.*;
import java.util.*;
import org.eclipse.osgi.internal.hookregistry.StorageHookFactory;
import org.eclipse.osgi.signedcontent.SignedContent;
import org.eclipse.osgi.signedcontent.SignerInfo;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.osgi.framework.BundleException;

public class SignedStorageHook extends StorageHookFactory<List<SignerInfo>, List<SignerInfo>, SignedStorageHook.StorageHookImpl> {
	private static final int STORAGE_VERSION = 4;

	@Override
	public int getStorageVersion() {
		return STORAGE_VERSION;
	}

	@Override
	public List<SignerInfo> createSaveContext() {
		return new ArrayList<>();
	}

	@Override
	public List<SignerInfo> createLoadContext(int version) {
		return new ArrayList<>();
	}

	@Override
	protected StorageHookImpl createStorageHook(Generation generation) {
		return new StorageHookImpl(generation);
	}

	static class StorageHookImpl extends StorageHookFactory.StorageHook<List<SignerInfo>, List<SignerInfo>> {
		SignedContentImpl signedContent;

		public StorageHookImpl(Generation generation) {
			super(generation, SignedStorageHook.class);
		}

		@Override
		public void initialize(Dictionary<String, String> manifest) throws BundleException {
			// do nothing
		}

		@Override
		public void load(List<SignerInfo> loadContext, DataInputStream is) throws IOException {
			boolean signed = is.readBoolean();
			if (!signed)
				return;
			int numSigners = is.readInt();
			SignerInfo[] signerInfos = new SignerInfo[numSigners];
			for (int i = 0; i < numSigners; i++)
				signerInfos[i] = readSignerInfo(is, loadContext);

			int resultsSize = is.readInt();
			Map<String, Object> contentMDResults = null;
			if (resultsSize > 0) {
				contentMDResults = new HashMap<>(resultsSize);
				for (int i = 0; i < resultsSize; i++) {
					String path = is.readUTF();
					int numEntrySigners = is.readInt();
					SignerInfo[] entrySigners = new SignerInfo[numEntrySigners];
					byte[][] entryResults = new byte[numEntrySigners][];
					for (int j = 0; j < numEntrySigners; j++) {
						entrySigners[j] = readSignerInfo(is, loadContext);
						int resultSize = is.readInt();
						entryResults[j] = new byte[resultSize];
						is.readFully(entryResults[j]);
					}
					contentMDResults.put(path, new Object[] {entrySigners, entryResults});
				}
			}
			SignedContentImpl result = new SignedContentImpl(signerInfos, contentMDResults);
			for (int i = 0; i < numSigners; i++) {
				boolean hasTSA = is.readBoolean();
				if (!hasTSA)
					continue;
				SignerInfo tsaSigner = readSignerInfo(is, loadContext);
				Date signingDate = new Date(is.readLong());
				result.addTSASignerInfo(signerInfos[i], tsaSigner, signingDate);
			}
			signedContent = result;
		}

		private SignerInfo readSignerInfo(DataInputStream is, List<SignerInfo> loadContext) throws IOException {
			int index = is.readInt();
			if (index >= 0)
				return loadContext.get(index);
			int numCerts = is.readInt();
			Certificate[] certs = new Certificate[numCerts];
			for (int i = 0; i < numCerts; i++) {
				int certSize = is.readInt();
				byte[] certBytes = new byte[certSize];
				is.readFully(certBytes);
				try {
					certs[i] = PKCS7Processor.certFact.generateCertificate(new ByteArrayInputStream(certBytes));
				} catch (CertificateException e) {
					throw new IOException(e.getMessage(), e);
				}
			}
			int anchorIdx = is.readInt();
			SignerInfoImpl result = new SignerInfoImpl(certs, anchorIdx >= 0 ? certs[anchorIdx] : null, is.readUTF());
			loadContext.add(result);
			return result;
		}

		@Override
		public void save(List<SignerInfo> saveContext, DataOutputStream os) throws IOException {
			os.writeBoolean(signedContent != null);
			if (signedContent == null)
				return;
			SignerInfo[] signerInfos = signedContent.getSignerInfos();
			os.writeInt(signerInfos.length);
			for (SignerInfo signerInfo : signerInfos) {
				saveSignerInfo(signerInfo, os, saveContext);
			}

			// keyed by entry path -> {SignerInfo[] infos, byte[][] results)}
			Map<String, Object> contentMDResults = signedContent.getContentMDResults();
			os.writeInt(contentMDResults == null ? -1 : contentMDResults.size());
			if (contentMDResults != null)
				for (Map.Entry<String, Object> entry : contentMDResults.entrySet()) {
					String path = entry.getKey();
					os.writeUTF(path);
					Object[] signerResults = (Object[]) entry.getValue();
					SignerInfo[] entrySigners = (SignerInfo[]) signerResults[0];
					byte[][] entryResults = (byte[][]) signerResults[1];
					os.writeInt(entrySigners.length);
					for (int i = 0; i < entrySigners.length; i++) {
						saveSignerInfo(entrySigners[i], os, saveContext);
						os.writeInt(entryResults[i].length);
						os.write(entryResults[i]);
					}
				}

			for (SignerInfo signerInfo : signerInfos) {
				SignerInfo tsaInfo = signedContent.getTSASignerInfo(signerInfo);
				os.writeBoolean(tsaInfo != null);
				if (tsaInfo == null)
					continue;
				saveSignerInfo(tsaInfo, os, saveContext);
				Date signingTime = signedContent.getSigningTime(signerInfo);
				os.writeLong(signingTime != null ? signingTime.getTime() : Long.MIN_VALUE);
			}
		}

		private void saveSignerInfo(SignerInfo signerInfo, DataOutputStream os, List<SignerInfo> saveContext) throws IOException {
			int cacheIdx = saveContext.indexOf(signerInfo);
			os.writeInt(cacheIdx);
			if (cacheIdx >= 0)
				return;
			Certificate[] certs = signerInfo.getCertificateChain();
			int anchorIndex = -1;
			os.writeInt(certs == null ? 0 : certs.length);
			if (certs != null)
				for (int i = 0; i < certs.length; i++) {
					if (certs[i].equals(signerInfo.getTrustAnchor()))
						anchorIndex = i;
					byte[] certBytes;
					try {
						certBytes = certs[i].getEncoded();
					} catch (CertificateEncodingException e) {
						throw new IOException(e.getMessage(), e);
					}
					os.writeInt(certBytes.length);
					os.write(certBytes);
				}
			os.writeInt(anchorIndex);
			os.writeUTF(signerInfo.getMessageDigestAlgorithm());
			saveContext.add(signerInfo);
		}

		public SignedContent getSignedContent() {
			return signedContent;
		}
	}

}
