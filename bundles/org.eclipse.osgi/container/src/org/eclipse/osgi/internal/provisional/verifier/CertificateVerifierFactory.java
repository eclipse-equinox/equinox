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

import java.io.File;
import java.io.IOException;
import org.osgi.framework.Bundle;

/**
 * A factory used to create certificate verifiers.
 * <p>
 * <strong>EXPERIMENTAL</strong>. This class or interface has been added as
 * part of a work in progress. There is no guarantee that this API will
 * work or that it will remain the same. Please do not use this API without
 * consulting with the equinox team.
 * </p>
 */
public interface CertificateVerifierFactory {
	/**
	 * Creates a certificate verifier for the specified content of a repository
	 * @param content the content of the repository
	 * @return a certificate verifier for the specified content of a repository
	 * @throws IOException if an IO exception occurs while reading the repository
	 */
	public CertificateVerifier getVerifier(File content) throws IOException;

	/**
	 * Returns a certificate verifier for the specified bundle.
	 * @param bundle the bundle to get a verifier for 
	 * @return a certificate verifier for the specified bundle.
	 * @throws IOException if an IO exception occurs while reading the bundle content
	 */
	public CertificateVerifier getVerifier(Bundle bundle) throws IOException;
}
