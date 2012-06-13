/*******************************************************************************
 * Copyright (c) 2006, 2010 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.osgi.internal.signedcontent;

import java.io.*;
import java.security.cert.*;
import java.util.*;
import org.eclipse.osgi.baseadaptor.BaseData;
import org.eclipse.osgi.baseadaptor.hooks.StorageHook;
import org.eclipse.osgi.framework.util.KeyedElement;
import org.eclipse.osgi.signedcontent.SignedContent;
import org.eclipse.osgi.signedcontent.SignerInfo;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

public class SignedStorageHook implements StorageHook {
	public static final String KEY = SignedStorageHook.class.getName();
	public static final int HASHCODE = KEY.hashCode();
	private static final int STORAGE_VERSION = 3;
	private static List<SignerInfo> savedSignerInfo = new ArrayList<SignerInfo>(5);
	private static long firstIDSaved = -1;
	private static long lastIDSaved = -1;
	private static List<SignerInfo> loadedSignerInfo = new ArrayList<SignerInfo>(5);
	private static long lastIDLoaded;

	private BaseData bundledata;
	SignedContentImpl signedContent;

	public int getStorageVersion() {
		return STORAGE_VERSION;
	}

	/**
	 * @throws BundleException  
	 */
	public StorageHook create(BaseData data) throws BundleException {
		SignedStorageHook hook = new SignedStorageHook();
		hook.bundledata = data;
		return hook;
	}

	/**
	 * @throws BundleException  
	 */
	public void initialize(Dictionary<String, String> manifest) throws BundleException {
		// do nothing
	}

	public StorageHook load(BaseData target, DataInputStream is) throws IOException {
		if (lastIDLoaded > target.getBundleID())
			loadedSignerInfo.clear();
		lastIDLoaded = target.getBundleID();
		SignedStorageHook hook = new SignedStorageHook();
		hook.bundledata = target;
		boolean signed = is.readBoolean();
		if (!signed)
			return hook;
		int numSigners = is.readInt();
		SignerInfo[] signerInfos = new SignerInfo[numSigners];
		for (int i = 0; i < numSigners; i++)
			signerInfos[i] = readSignerInfo(is);

		int resultsSize = is.readInt();
		Map<String, Object> contentMDResults = null;
		if (resultsSize > 0) {
			contentMDResults = new HashMap<String, Object>(resultsSize);
			for (int i = 0; i < resultsSize; i++) {
				String path = is.readUTF();
				int numEntrySigners = is.readInt();
				SignerInfo[] entrySigners = new SignerInfo[numEntrySigners];
				byte[][] entryResults = new byte[numEntrySigners][];
				for (int j = 0; j < numEntrySigners; j++) {
					entrySigners[j] = readSignerInfo(is);
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
			SignerInfo tsaSigner = readSignerInfo(is);
			Date signingDate = new Date(is.readLong());
			result.addTSASignerInfo(signerInfos[i], tsaSigner, signingDate);
		}
		hook.signedContent = result;
		return hook;
	}

	public void save(DataOutputStream os) throws IOException {
		getFirstLastID();
		if (firstIDSaved == bundledata.getBundleID())
			savedSignerInfo.clear();
		if (lastIDSaved == bundledata.getBundleID())
			firstIDSaved = lastIDSaved = -1;
		os.writeBoolean(signedContent != null);
		if (signedContent == null)
			return;
		SignerInfo[] signerInfos = signedContent.getSignerInfos();
		os.writeInt(signerInfos.length);
		for (int i = 0; i < signerInfos.length; i++)
			saveSignerInfo(signerInfos[i], os);

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
					saveSignerInfo(entrySigners[i], os);
					os.writeInt(entryResults[i].length);
					os.write(entryResults[i]);
				}
			}

		for (int i = 0; i < signerInfos.length; i++) {
			SignerInfo tsaInfo = signedContent.getTSASignerInfo(signerInfos[i]);
			os.writeBoolean(tsaInfo != null);
			if (tsaInfo == null)
				continue;
			saveSignerInfo(tsaInfo, os);
			Date signingTime = signedContent.getSigningTime(signerInfos[i]);
			os.writeLong(signingTime != null ? signingTime.getTime() : Long.MIN_VALUE);
		}
	}

	private void saveSignerInfo(SignerInfo signerInfo, DataOutputStream os) throws IOException {
		int cacheIdx = savedSignerInfo.indexOf(signerInfo);
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
					throw (IOException) new IOException(e.getMessage()).initCause(e);
				}
				os.writeInt(certBytes.length);
				os.write(certBytes);
			}
		os.writeInt(anchorIndex);
		os.writeUTF(signerInfo.getMessageDigestAlgorithm());
		savedSignerInfo.add(signerInfo);
	}

	private SignerInfo readSignerInfo(DataInputStream is) throws IOException {
		int index = is.readInt();
		if (index >= 0)
			return loadedSignerInfo.get(index);
		int numCerts = is.readInt();
		Certificate[] certs = new Certificate[numCerts];
		for (int i = 0; i < numCerts; i++) {
			int certSize = is.readInt();
			byte[] certBytes = new byte[certSize];
			is.readFully(certBytes);
			try {
				certs[i] = PKCS7Processor.certFact.generateCertificate(new ByteArrayInputStream(certBytes));
			} catch (CertificateException e) {
				throw (IOException) new IOException(e.getMessage()).initCause(e);
			}
		}
		int anchorIdx = is.readInt();
		SignerInfoImpl result = new SignerInfoImpl(certs, anchorIdx >= 0 ? certs[anchorIdx] : null, is.readUTF());
		loadedSignerInfo.add(result);
		return result;
	}

	private void getFirstLastID() {
		if (firstIDSaved >= 0)
			return;
		Bundle[] bundles = bundledata.getAdaptor().getContext().getBundles();
		if (bundles.length > 1) {
			firstIDSaved = bundles[1].getBundleId();
			lastIDSaved = bundles[bundles.length - 1].getBundleId();
		}
	}

	public void copy(StorageHook storageHook) {
		// do nothing
	}

	public void validate() throws IllegalArgumentException {
		// do nothing
	}

	/**
	 * @throws BundleException  
	 */
	public Dictionary<String, String> getManifest(boolean firstLoad) throws BundleException {
		// do nothing
		return null;
	}

	public boolean forgetStatusChange(int status) {
		// do nothing
		return false;
	}

	public boolean forgetStartLevelChange(int startlevel) {
		// do nothing
		return false;
	}

	public int getKeyHashCode() {
		return HASHCODE;
	}

	public boolean compare(KeyedElement other) {
		return other.getKey() == KEY;
	}

	public Object getKey() {
		return KEY;
	}

	public SignedContent getSignedContent() {
		return signedContent;
	}

}
