/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.provisional.verifier;

import java.security.SignatureException;
import java.security.cert.*;

/**
 * A certificate verifier is used to verify the authenticity of a signed 
 * repository.  A certificate verifier is created using a 
 * {@link CertificateVerifierFactory}.
 * <p>
 * <strong>EXPERIMENTAL</strong>. This class or interface has been added as
 * part of a work in progress. There is no guarantee that this API will
 * work or that it will remain the same. Please do not use this API without
 * consulting with the equinox team.
 * </p>
 */
public interface CertificateVerifier {
	/**
	 * Verify the content of the repository.
	 * 
	 * @throws CertificateException			
	 * @throws CertificateExpiredException
	 * @throws CertificateParsingException
	 * @throws SignatureException
	 */
	public void checkContent() throws CertificateException, CertificateExpiredException, SignatureException;

	/**
	 * Verifies the content of the repository.  An array is returned with the entry names 
	 * which are corrupt.  If no entries are corrupt then an empty array is returned.
	 * @return An array of entry names which are corrupt.  An empty array is returned if the 
	 * repository is not corrupt or if the repository is not signed. 
	 */
	public String[] verifyContent();

	/**
	 * Returns true if the repository is signed
	 * @return true if the repository is signed
	 */
	public boolean isSigned();

	/**
	 * Returns all certificate chains of the repository.  All certificate chains
	 * are returned whether they are trusted or not.  If the repository is not signed 
	 * then an empty array is returned.
	 * @return all certificate chains of the repository
	 */
	public CertificateChain[] getChains();
}
