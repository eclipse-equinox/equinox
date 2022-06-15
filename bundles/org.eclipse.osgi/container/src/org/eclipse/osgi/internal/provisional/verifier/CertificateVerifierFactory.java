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
