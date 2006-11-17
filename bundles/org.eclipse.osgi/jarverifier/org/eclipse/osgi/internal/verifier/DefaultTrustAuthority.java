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

import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import org.eclipse.osgi.internal.provisional.verifier.CertificateTrustAuthority;
import org.eclipse.osgi.util.NLS;

public class DefaultTrustAuthority implements CertificateTrustAuthority {
	// the KeyStores that we determine trust from.  This only gets intialized the 
	// supportFlags include the VERIFY_TRUST flag
	private  KeyStores keyStores;
	// used to indicate if we should check the KeyStores object for trust.
	private int supportFlags;
	public DefaultTrustAuthority(int supportFlags) {
		this.supportFlags = supportFlags;
	}
	public void checkTrust(Certificate[] certChain) throws CertificateException {
		if (certChain == null || certChain.length == 0) {
			throw new IllegalArgumentException(JarVerifierMessages.Cert_Verifier_Illegal_Args);
		}
		KeyStores stores = getKeyStores();
		// stores == null when the supportFlags includes the VERIFY_TRUST flag
		if (stores != null && !stores.isTrusted(certChain[certChain.length - 1])) {
			throw new CertificateException(NLS.bind(JarVerifierMessages.Cert_Verifier_Not_Trusted, new String[] {certChain[0].toString()}));
		}
	}

	private synchronized KeyStores getKeyStores() {
		if (((supportFlags & SignedBundleHook.VERIFY_TRUST) == 0) || keyStores != null)
			return keyStores;
		keyStores = new KeyStores();
		return keyStores;
	}
	public void addTrusted(Certificate[] certs) throws CertificateException {
		// do nothing for now ...
	}

}
