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
import java.security.MessageDigest;
import java.security.cert.*;
import java.util.*;
import org.eclipse.osgi.baseadaptor.BaseData;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleFile;
import org.eclipse.osgi.baseadaptor.hooks.StorageHook;
import org.eclipse.osgi.framework.util.KeyedElement;
import org.eclipse.osgi.internal.provisional.verifier.CertificateChain;
import org.osgi.framework.BundleException;

public class SignedStorageHook implements StorageHook {
	static final String KEY = SignedStorageHook.class.getName();
	static final int HASHCODE = KEY.hashCode();
	private static final int STORAGE_VERSION = 1;
	private static ArrayList saveChainCache = new ArrayList(5);
	private static long lastIDSaved;
	private static ArrayList loadChainCache = new ArrayList(5);
	private static long lastIDLoaded;

	private BaseData bundledata;
	SignedBundleFile signedBundleFile;
	public int getStorageVersion() {
		return STORAGE_VERSION;
	}

	public StorageHook create(BaseData bundledata) throws BundleException {
		SignedStorageHook hook = new SignedStorageHook();
		hook.bundledata = bundledata;
		return hook;
	}

	public void initialize(Dictionary manifest) throws BundleException {
		// do nothing
	}

	public StorageHook load(BaseData target, DataInputStream is) throws IOException {
		if (lastIDLoaded > target.getBundleID())
			loadChainCache.clear();
		lastIDLoaded = target.getBundleID();
		SignedStorageHook hook = new SignedStorageHook();
		hook.bundledata = target;
		boolean signed = is.readBoolean();
		if (!signed)
			return hook;
		int numChains = is.readInt();
		CertificateChain[] chains = new CertificateChain[numChains];
		for (int i = 0; i < numChains; i++) {
			int chainIdx = is.readInt();
			if (chainIdx >= 0) {
				chains[i] = (CertificateChain) loadChainCache.get(chainIdx);
				if (chains[i] == null)
					throw new IOException("Invalid chain cache."); //$NON-NLS-1$
				continue;
			}
			String chain = is.readUTF();

			boolean trusted = is.readBoolean();
			int numCerts = is.readInt();
			byte[][] certsBytes = new byte[numCerts][];
			for (int j = 0; j < certsBytes.length; j++) {
				int numBytes = is.readInt();
				certsBytes[j] = new byte[numBytes];
				is.readFully(certsBytes[j]);
			}
			try {
				chains[i] = new PKCS7Processor(chain, trusted, certsBytes);
			} catch (CertificateException e) {
				throw new IOException(e.getMessage());
			}
			loadChainCache.add(chains[i]);
		}
		int numEntries = is.readInt();
		Hashtable digests = new Hashtable(numEntries);
		Hashtable results = new Hashtable(numEntries);
		for (int i = 0; i < numEntries; i++) {
			String entry = is.readUTF();
			int numMDs = is.readInt();
			MessageDigest[] mds = new MessageDigest[numMDs];
			byte[][] result = new byte[numMDs][];
			for (int j = 0; j < numMDs; j++) {
				if (is.readInt() == 0)
					mds[j] = SignedBundleFile.md5;
				else
					mds[j] = SignedBundleFile.sha1;
				result[j] = new byte[is.readInt()];
				is.readFully(result[j]);
				digests.put(entry, mds);
				results.put(entry, result);
			}
		}
		String md5Result = null;
		String shaResult = null;
		if (is.readBoolean())
			md5Result = is.readUTF();
		if (is.readBoolean())
			shaResult = is.readUTF();
		hook.signedBundleFile = new SignedBundleFile(chains, digests, results, md5Result, shaResult);
		return hook;
	}

	public void save(DataOutputStream os) throws IOException {
		if (lastIDSaved > bundledata.getBundleID())
			saveChainCache.clear();
		lastIDSaved = bundledata.getBundleID();
		BundleFile bundleFile = bundledata.getBundleFile();
		CertificateChain[] chains = null;
		String md5Result = null;
		String shaResult = null;
		Hashtable digests = null;
		Hashtable results = null;
		if (bundleFile instanceof SignedBundleFile) {
			SignedBundleFile signedFile = (SignedBundleFile) bundleFile;
			chains = signedFile.chains;
			md5Result = signedFile.manifestMD5Result;
			shaResult = signedFile.manifestSHAResult;
			digests = signedFile.digests4entries;
			results = signedFile.results4entries;
		}
		os.writeBoolean(chains != null);
		if (chains == null)
			return;
		os.writeInt(chains.length);
		for (int i = 0; i < chains.length; i++) {
			int cacheIdx = saveChainCache.indexOf(chains[i]);
			os.writeInt(cacheIdx);
			if (cacheIdx >= 0)
				continue;
			saveChainCache.add(chains[i]);
			os.writeUTF(chains[i].getChain());
			os.writeBoolean(chains[i].isTrusted());
			Certificate[] certs = chains[i].getCertificates();
			os.writeInt(certs == null ? 0 : certs.length);
			if (certs != null)
				for (int j = 0; j < certs.length; j++) {
					byte[] certBytes;
					try {
						certBytes = certs[j].getEncoded();
					} catch (CertificateEncodingException e) {
						throw new IOException(e.getMessage());
					}
					os.writeInt(certBytes.length);
					os.write(certBytes);
				}
		}
		os.writeInt(digests.size());
		for (Enumeration entries = digests.keys(); entries.hasMoreElements();) {
			String entry = (String) entries.nextElement();
			MessageDigest[] mds = (MessageDigest[]) digests.get(entry);
			byte[][] result = (byte[][]) results.get(entry);
			os.writeUTF(entry);
			os.writeInt(mds.length);
			for (int i = 0; i < mds.length; i++) {
				if (mds[i] == SignedBundleFile.md5)
					os.writeInt(0);
				else
					os.writeInt(1);
				os.writeInt(result[i].length);
				os.write(result[i]);
			}
		}
		os.writeBoolean(md5Result != null);
		if (md5Result != null)
			os.writeUTF(md5Result);
		os.writeBoolean(shaResult != null);
		if (shaResult != null)
			os.writeUTF(shaResult);
	}

	public void copy(StorageHook storageHook) {
		// do nothing
	}

	public void validate() throws IllegalArgumentException {
		// do nothing
	}

	public Dictionary getManifest(boolean firstLoad) throws BundleException {
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

	public boolean matchDNChain(String pattern) {
		BundleFile base = bundledata.getBundleFile();
		if (!(base instanceof SignedBundleFile))
			return false;
		return ((SignedBundleFile) base).matchDNChain(pattern);
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


}
