/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.signedcontent;

import java.io.File;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import org.osgi.framework.Bundle;

/**
 * A factory used to create {@link SignedContent} objects.
 * <p>
 * The framework will register a factory implementation as an OSGi service.
 * This service can be used to get <code>SignedContent</code> for a bundle.
 * It can also be used to get <code>SignedContent</code> for a repository file.
 * The supported formats for file repositories are jar files and  directories containing the 
 * content of an extracted jar.
 * </p>
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 * @since 3.4
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface SignedContentFactory {
	/**
	 * Returns a <code>SignedContent</code> object for the specified content of a repository.
	 * @param content the content of the repository
	 * @return signed content for the specified repository
	 * @throws IOException if an IO exception occurs while reading the repository
	 * @throws NoSuchProviderException if there's no security provider for the signed content
	 * @throws NoSuchAlgorithmException if the cryptographic algorithm is not available for the signed content
	 * @throws CertificateException if there is a problem with one of the certificates of the signed content
	 * @throws SignatureException if there is a problem with one of the signatures of the signed content
	 * @throws InvalidKeyException if there is a problem with one of the certificate keys of the signed content
	 */
	public SignedContent getSignedContent(File content) throws IOException, InvalidKeyException, SignatureException, CertificateException, NoSuchAlgorithmException, NoSuchProviderException;

	/**
	 * Returns a <code>SignedContent</code> object for the specified bundle.
	 * @param bundle the bundle to get a signed content for. 
	 * @return signed content for the specified bundle.
	 * @throws IOException if an IO exception occurs while reading the bundle content
	 * @throws NoSuchProviderException if there's no security provider for the signed content
	 * @throws NoSuchAlgorithmException if the cryptographic algorithm is not available for the signed content
	 * @throws CertificateException if there is a problem with one of the certificates of the signed content
	 * @throws SignatureException if there is a problem with one of the signatures of the signed content
	 * @throws InvalidKeyException if there is a problem with one of the certificate keys of the signed content
	 */
	public SignedContent getSignedContent(Bundle bundle) throws IOException, InvalidKeyException, SignatureException, CertificateException, NoSuchAlgorithmException, NoSuchProviderException;
}
