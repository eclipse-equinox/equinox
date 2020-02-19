/*******************************************************************************
 * Copyright (c) 2007, 2012 IBM Corporation and others.
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

import java.security.cert.Certificate;
import org.eclipse.osgi.signedcontent.SignerInfo;

public class SignerInfoImpl implements SignerInfo {
	private final Certificate[] chain;
	private final String mdAlgorithm;
	volatile private Certificate trustAnchor;

	public SignerInfoImpl(Certificate[] chain, Certificate trustAnchor, String mdAlgorithm) {
		this.chain = chain;
		this.trustAnchor = trustAnchor;
		this.mdAlgorithm = mdAlgorithm;
	}

	@Override
	public Certificate[] getCertificateChain() {
		return chain;
	}

	@Override
	public Certificate getTrustAnchor() {
		return trustAnchor;
	}

	@Override
	public boolean isTrusted() {
		return trustAnchor != null;
	}

	void setTrustAnchor(Certificate trustAnchor) {
		this.trustAnchor = trustAnchor;
	}

	@Override
	public String getMessageDigestAlgorithm() {
		return mdAlgorithm;
	}

	@Override
	public int hashCode() {
		int result = mdAlgorithm.hashCode();
		for (Certificate cert : chain) {
			result += cert.hashCode();
		}
		// Note that we do not hash based on trustAnchor;
		// this changes dynamically but we need a constant hashCode for purposes of
		// hashing in a Set.
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof SignerInfo))
			return false;
		if (obj == this)
			return true;
		SignerInfo other = (SignerInfo) obj;
		if (!mdAlgorithm.equals(other.getMessageDigestAlgorithm()))
			return false;
		Certificate[] otherCerts = other.getCertificateChain();
		if (otherCerts.length != chain.length)
			return false;
		for (int i = 0; i < chain.length; i++)
			if (!chain[i].equals(otherCerts[i]))
				return false;
		return trustAnchor == null ? other.getTrustAnchor() == null : trustAnchor.equals(other.getTrustAnchor());
	}
}
