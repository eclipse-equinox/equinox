/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.service.security;

import java.io.*;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.util.*;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.internal.signedcontent.SignedBundleHook;
import org.eclipse.osgi.internal.signedcontent.SignedContentMessages;
import org.eclipse.osgi.service.security.TrustEngine;
import org.eclipse.osgi.util.NLS;

//*potential enhancements*
// 1. reloading from the backing file when it changes
// 3. methods to support lock/unlock
//  3a. Using a callback handler to collect the password
//  3b. managing lock/unlock between multiple threads. dealing with SWT UI thread
// 4. methods to support changing password, etc
// 5. methods to support export, etc
// 6. 'friendly-name' generator
// 7. Listeners for change events
public class KeyStoreTrustEngine extends TrustEngine {

	private KeyStore keyStore;

	private final String type;
	private final String path;
	private final char[] password;
	private final String name;

	/**
	 * Create a new KeyStoreTrustEngine that is backed by a KeyStore 
	 * @param path - path to the keystore
	 * @param type - the type of keystore at the path location
	 * @param password - the password required to unlock the keystore
	 */
	public KeyStoreTrustEngine(String path, String type, char[] password, String name) { //TODO: This should be a *CallbackHandler*
		this.path = path;
		this.type = type;
		this.password = password;
		this.name = name;
	}

	/**
	 * Return the type
	 * @return type - the type for the KeyStore being managed
	 */
	private String getType() {
		return type;
	}

	/**
	 * Return the path
	 * @return - the path for the KeyStore being managed
	 */
	private String getPath() {
		return path;
	}

	/**
	 * Return the password
	 * @return password - the password as a char[] 
	 */
	private char[] getPassword() {
		return password;
	}

	/**
	 * Return the KeyStore managed
	 * @return The KeyStore instance, initialized and loaded
	 * @throws KeyStoreException
	 */
	private synchronized KeyStore getKeyStore() throws IOException, GeneralSecurityException {
		if (null == keyStore) {
			keyStore = KeyStore.getInstance(getType());
			final InputStream in = getInputStream();
			try {
				loadStore(keyStore, in);
			} finally {
				try {
					in.close();
				} catch (IOException e) {
					//ignore secondary failure
				}
			}
		}

		if (keyStore == null)
			throw new KeyStoreException(NLS.bind(SignedContentMessages.Default_Trust_Keystore_Load_Failed, getPath()));

		return keyStore;
	}

	public Certificate findTrustAnchor(Certificate[] certChain) throws IOException {

		if (certChain == null || certChain.length == 0)
			throw new IllegalArgumentException("Certificate chain is required"); //$NON-NLS-1$

		try {
			Certificate rootCert = null;
			KeyStore store = getKeyStore();
			for (int i = 0; i < certChain.length; i++) {
				if (certChain[i] instanceof X509Certificate) {
					if (i == certChain.length - 1) {
						// this is the last certificate in the chain
						// determine if we have a valid root
						X509Certificate cert = (X509Certificate) certChain[i];
						if (cert.getSubjectDN().equals(cert.getIssuerDN())) {
							cert.verify(cert.getPublicKey());
							rootCert = cert; // this is a self-signed certificate
						} else {
							// try to find a parent, we have an incomplete chain
							return findAlternativeRoot(cert, store);
						}
					} else {
						X509Certificate nextX509Cert = (X509Certificate) certChain[i + 1];
						certChain[i].verify(nextX509Cert.getPublicKey());
					}
				}

				synchronized (store) {
					String alias = rootCert == null ? null : store.getCertificateAlias(rootCert);
					if (alias != null)
						return store.getCertificate(alias);
					else if (rootCert != certChain[i]) {
						alias = store.getCertificateAlias(certChain[i]);
						if (alias != null)
							return store.getCertificate(alias);
					}
					// if we have reached the end and the last cert is not found to be a valid root CA
					// then we need to back off the root CA and try to find an alternative
					if (certChain.length > 1 && i == certChain.length - 1 && certChain[i - 1] instanceof X509Certificate)
						return findAlternativeRoot((X509Certificate) certChain[i - 1], store);
				}
			}
		} catch (KeyStoreException e) {
			throw (IOException) new IOException(e.getMessage()).initCause(e);
		} catch (GeneralSecurityException e) {
			SignedBundleHook.log(e.getMessage(), FrameworkLogEntry.WARNING, e);
			return null;
		}
		return null;
	}

	private Certificate findAlternativeRoot(X509Certificate cert, KeyStore store) throws InvalidKeyException, KeyStoreException, NoSuchAlgorithmException, NoSuchProviderException, SignatureException, CertificateException {
		synchronized (store) {
			for (Enumeration<String> e = store.aliases(); e.hasMoreElements();) {
				Certificate nextCert = store.getCertificate(e.nextElement());
				if (nextCert instanceof X509Certificate && ((X509Certificate) nextCert).getSubjectDN().equals(cert.getIssuerDN())) {
					cert.verify(nextCert.getPublicKey());
					return nextCert;
				}
			}
			return null;
		}
	}

	protected String doAddTrustAnchor(Certificate cert, String alias) throws IOException, GeneralSecurityException {
		if (isReadOnly())
			throw new IOException(SignedContentMessages.Default_Trust_Read_Only);
		if (cert == null) {
			throw new IllegalArgumentException("Certificate must be specified"); //$NON-NLS-1$
		}
		try {
			KeyStore store = getKeyStore();
			synchronized (store) {
				String oldAlias = store.getCertificateAlias(cert);
				if (null != oldAlias)
					throw new CertificateException(SignedContentMessages.Default_Trust_Existing_Cert);
				Certificate oldCert = store.getCertificate(alias);
				if (null != oldCert)
					throw new CertificateException(SignedContentMessages.Default_Trust_Existing_Alias);
				store.setCertificateEntry(alias, cert);
				final OutputStream out = getOutputStream();
				try {
					saveStore(store, out);
				} finally {
					safeClose(out);
				}
			}
		} catch (KeyStoreException ke) {
			throw (CertificateException) new CertificateException(ke.getMessage()).initCause(ke);
		}
		return alias;
	}

	protected void doRemoveTrustAnchor(Certificate cert) throws IOException, GeneralSecurityException {
		if (isReadOnly())
			throw new IOException(SignedContentMessages.Default_Trust_Read_Only);
		if (cert == null) {
			throw new IllegalArgumentException("Certificate must be specified"); //$NON-NLS-1$
		}
		try {
			KeyStore store = getKeyStore();
			synchronized (store) {
				String alias = store.getCertificateAlias(cert);
				if (alias == null) {
					throw new CertificateException(SignedContentMessages.Default_Trust_Cert_Not_Found);
				}
				removeTrustAnchor(alias);
			}
		} catch (KeyStoreException ke) {
			throw (CertificateException) new CertificateException(ke.getMessage()).initCause(ke);
		}
	}

	protected void doRemoveTrustAnchor(String alias) throws IOException, GeneralSecurityException {

		if (alias == null) {
			throw new IllegalArgumentException("Alias must be specified"); //$NON-NLS-1$
		}
		try {
			KeyStore store = getKeyStore();
			synchronized (store) {
				Certificate oldCert = store.getCertificate(alias);
				if (oldCert == null)
					throw new CertificateException(SignedContentMessages.Default_Trust_Cert_Not_Found);
				store.deleteEntry(alias);
				final OutputStream out = getOutputStream();
				try {
					saveStore(store, out);
				} finally {
					safeClose(out);
				}
			}
		} catch (KeyStoreException ke) {
			throw (CertificateException) new CertificateException(ke.getMessage()).initCause(ke);
		}
	}

	public Certificate getTrustAnchor(String alias) throws IOException, GeneralSecurityException {

		if (alias == null) {
			throw new IllegalArgumentException("Alias must be specified"); //$NON-NLS-1$
		}

		try {
			KeyStore store = getKeyStore();
			synchronized (store) {
				return store.getCertificate(alias);
			}
		} catch (KeyStoreException ke) {
			throw (CertificateException) new CertificateException(ke.getMessage()).initCause(ke);
		}
	}

	public String[] getAliases() throws IOException, GeneralSecurityException {

		List<String> returnList = new ArrayList<String>();
		try {
			KeyStore store = getKeyStore();
			synchronized (store) {
				for (Enumeration<String> aliases = store.aliases(); aliases.hasMoreElements();) {
					String currentAlias = aliases.nextElement();
					if (store.isCertificateEntry(currentAlias)) {
						returnList.add(currentAlias);
					}
				}
			}
		} catch (KeyStoreException ke) {
			throw (CertificateException) new CertificateException(ke.getMessage()).initCause(ke);
		}
		return returnList.toArray(new String[] {});
	}

	/**
	 * Load using the current password
	 */
	private void loadStore(KeyStore store, InputStream is) throws IOException, GeneralSecurityException {
		store.load(is, getPassword());
	}

	/**
	 * Save using the current password
	 */
	private void saveStore(KeyStore store, OutputStream os) throws IOException, GeneralSecurityException {
		store.store(os, getPassword());
	}

	/**
	 * Closes a stream and ignores any resulting exception. This is useful
	 * when doing stream cleanup in a finally block where secondary exceptions
	 * are not worth logging.
	 */
	private void safeClose(OutputStream out) {
		try {
			if (out != null)
				out.close();
		} catch (IOException e) {
			//ignore
		}
	}

	/**
	 * Get an input stream for the KeyStore managed
	 * @return inputstream - the stream
	 * @throws KeyStoreException
	 */
	private InputStream getInputStream() throws IOException {
		return new FileInputStream(new File(getPath()));
	}

	/**
	 * Get an output stream for the KeyStore managed
	 * @return outputstream - the stream
	 * @throws KeyStoreException
	 */
	private OutputStream getOutputStream() throws IOException {

		File file = new File(getPath());
		if (!file.exists())
			file.createNewFile();

		return new FileOutputStream(file);
	}

	public boolean isReadOnly() {
		return getPassword() == null || !(new File(path).canWrite());
	}

	public String getName() {
		return name;
	}
}
