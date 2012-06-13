/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.osgi.internal.signedcontent;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.*;
import java.util.*;
import org.eclipse.osgi.internal.provisional.verifier.*;
import org.eclipse.osgi.signedcontent.*;
import org.osgi.framework.Bundle;

public class LegacyVerifierFactory implements CertificateVerifierFactory {
	private final SignedContentFactory signedContentFactory;

	public LegacyVerifierFactory(SignedContentFactory signedContentFactory) {
		this.signedContentFactory = signedContentFactory;
	}

	public CertificateVerifier getVerifier(File content) throws IOException {
		try {
			return new LegacyVerifier(signedContentFactory.getSignedContent(content));
		} catch (GeneralSecurityException e) {
			throw (IOException) new IOException(e.getMessage()).initCause(e);
		}
	}

	public CertificateVerifier getVerifier(Bundle bundle) throws IOException {
		try {
			return new LegacyVerifier(signedContentFactory.getSignedContent(bundle));
		} catch (GeneralSecurityException e) {
			throw (IOException) new IOException(e.getMessage()).initCause(e);
		}
	}

	static class LegacyVerifier implements CertificateVerifier {
		private final SignedContent signedContent;

		public LegacyVerifier(SignedContent signedContent) {
			this.signedContent = signedContent;
		}

		public void checkContent() throws CertificateException, CertificateExpiredException {
			SignedContentEntry[] entries = signedContent.getSignedEntries();
			for (int i = 0; i < entries.length; i++) {
				try {
					entries[i].verify();
				} catch (InvalidContentException e) {
					throw (SecurityException) new SecurityException(e.getMessage()).initCause(e);
				} catch (IOException e) {
					throw (SecurityException) new SecurityException(e.getMessage()).initCause(e);
				}
			}
			SignerInfo[] infos = signedContent.getSignerInfos();
			for (int i = 0; i < infos.length; i++)
				signedContent.checkValidity(infos[i]);
		}

		public CertificateChain[] getChains() {
			SignerInfo infos[] = signedContent.getSignerInfos();
			CertificateChain[] chains = new CertificateChain[infos.length];
			for (int i = 0; i < chains.length; i++)
				chains[i] = new LegacyChain(infos[i], signedContent);
			return chains;
		}

		public boolean isSigned() {
			return signedContent.isSigned();
		}

		public String[] verifyContent() {
			List<String> invalidContent = new ArrayList<String>(0);
			SignedContentEntry[] entries = signedContent.getSignedEntries();
			for (int i = 0; i < entries.length; i++) {
				try {
					entries[i].verify();
				} catch (InvalidContentException e) {
					invalidContent.add(entries[i].getName());
				} catch (IOException e) {
					invalidContent.add(entries[i].getName());
				}
			}
			return invalidContent.toArray(new String[invalidContent.size()]);
		}
	}

	static class LegacyChain implements CertificateChain {
		private final SignerInfo signerInfo;
		private final SignedContent content;

		public LegacyChain(SignerInfo signerInfo, SignedContent content) {
			this.signerInfo = signerInfo;
			this.content = content;
		}

		public Certificate[] getCertificates() {
			return signerInfo.getCertificateChain();
		}

		public String getChain() {
			StringBuffer sb = new StringBuffer();
			Certificate[] certs = getCertificates();
			for (int i = 0; i < certs.length; i++) {
				X509Certificate x509Cert = ((X509Certificate) certs[i]);
				sb.append(x509Cert.getSubjectDN().getName());
				sb.append("; "); //$NON-NLS-1$
			}
			return sb.toString();
		}

		public Certificate getRoot() {
			Certificate[] certs = getCertificates();
			return certs.length > 0 ? certs[certs.length - 1] : null;
		}

		public Certificate getSigner() {
			Certificate[] certs = getCertificates();
			return certs.length > 0 ? certs[0] : null;
		}

		public Date getSigningTime() {
			return content.getSigningTime(signerInfo);
		}

		public boolean isTrusted() {
			return signerInfo.isTrusted();
		}

	}
}
