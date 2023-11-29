/*******************************************************************************
 * Copyright (c) 2006, 2012 IBM Corporation and others.
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

package org.eclipse.osgi.internal.provisional.verifier;

import java.security.cert.Certificate;
import java.security.cert.CertificateException;

/**
 * A CertificateTrustAuthority is used to check if certificate chains are trusted.
 */
public interface CertificateTrustAuthority {

	/**
	 * Determines if the certificates are trusted.  This method will throw a
	 * <code>CertificateException</code> if the specified certificate chain is not trusted.
	 * @param certChain a chain of certificates
	 * @throws CertificateException if the certficates are not trusted
	 */
	public void checkTrust(Certificate[] certChain) throws CertificateException;

	/**
	 * Add the specified certificate chain as a trusted certificate chain.
	 *
	 * @param certChain a chain of certificates
	 */
	public void addTrusted(Certificate[] certChain) throws CertificateException;
}
