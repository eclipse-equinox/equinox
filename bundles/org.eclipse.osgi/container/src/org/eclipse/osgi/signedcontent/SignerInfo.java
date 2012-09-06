/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.signedcontent;

import java.security.cert.Certificate;

/**
 * A <code>SignerInfo</code> object represents a single signer chain.
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 * @since 3.4
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface SignerInfo {

	/**
	 * Returns the certificate chain
	 * @return the certificate chain
	 */
	public Certificate[] getCertificateChain();

	/**
	 * Returns the certificate trust anchor used to establish authenticity.
	 * If authenticity cannot be established then <code>null</code> is returned.
	 * @return the trust anchor
	 */
	public Certificate getTrustAnchor();

	/**
	 * Returns true if the trust anchor has been authenticated.  This is a convenience 
	 * method equivalent to calling <code>{@link #getTrustAnchor()} != null</code>
	 * @return true if the the signer info is trusted
	 */
	public boolean isTrusted();

	/**
	 * Returns the <code>MessageDigest</code> algorithm used to verify content signed by this 
	 * signer info.
	 * @return the algorithm
	 */
	public String getMessageDigestAlgorithm();

	// TODO need more thought here, TrustEngines could get stale since they are services, leaving off for now unless until we understand the usecase for this.
	//public TrustEngine getTrustEngine();

}
