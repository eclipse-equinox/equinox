/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
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

	public Certificate[] getCertificateChain() {
		return chain;
	}

	public Certificate getTrustAnchor() {
		return trustAnchor;
	}

	public boolean isTrusted() {
		return trustAnchor != null;
	}

	void setTrustAnchor(Certificate trustAnchor) {
		this.trustAnchor = trustAnchor;
	}

	public String getMessageDigestAlgorithm() {
		return mdAlgorithm;
	}

	public int hashCode() {
		int result = mdAlgorithm.hashCode();
		for (int i = 0; i < chain.length; i++)
			result += chain[i].hashCode();
		// Note that we do not hash based on trustAnchor;
		// this changes dynamically but we need a constant hashCode for purposes of 
		// hashing in a Set.
		return result;
	}

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
