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

import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.util.Date;

/**
 * A <code>SignedContent</code> object represents content which may be signed.  A
 * {@link SignedContentFactory} is used to create signed content objects.
 * <p>
 * A <code>SignedContent</code> object is intended to provide information about 
 * the signers of the content, and cannot be used to access the actual data of the content.
 * </p>
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 * @since 3.4
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface SignedContent {

	/**
	 * Returns all entries of the content.  The returned entries can be used
	 * to verify the entry content using {@link SignedContentEntry#verify()} and 
	 * get signer info for each entry in this content using {@link SignedContentEntry#getSignerInfos()}.
	 * Note that this operation may be expensive because it requires an 
	 * exhaustive search for entries over the entire content.
	 * <p>
	 * Unsigned entries are included in the result.  Entries for which signer info exists
	 * but no content is found are also returned. For example, when an entry is removed from 
	 * a signed jar but the jar is not resigned, the signer thinks the entry should exist
	 * but the content got removed.  This would be considered an invalid entry which would fail verification.
	 * </p>
	 * @return all entries of the content
	 */
	public SignedContentEntry[] getSignedEntries();

	/**
	 * Returns the signed entry for the specified name.
	 * @param name the name of the entry
	 * @return the entry or null if the entry could not be found
	 */
	public SignedContentEntry getSignedEntry(String name);

	/**
	 * Returns all the signer infos for this <code>SignedContent</code>.  If the content
	 * is not signed then an empty array is returned.
	 * @return all the signer infos for this <code>SignedContent</code>
	 */
	public SignerInfo[] getSignerInfos();

	/**
	 * Returns true if the content is signed; false otherwise.  This is a convenience method
	 * equivalent to calling <code>{@link #getSignerInfos()}.length > 0</code> 
	 * @return true if the content is signed
	 */
	public boolean isSigned();

	/**
	 * Returns the signing time for the signer info.  If no TSA signers exist then null is returned
	 * @param signerInfo the signer info to get the signing time for
	 * @return the signing time
	 */
	public Date getSigningTime(SignerInfo signerInfo);

	/**
	 * Returns the TSA signer info used to authenticate the signer time of a signer info.
	 * @param signerInfo the signer info to get the TSA signer for
	 * @return the TSA signer info
	 */
	public SignerInfo getTSASignerInfo(SignerInfo signerInfo);

	/**
	 * Checks if the certificates are valid for the specified signer.  If the signer has a singing time 
	 * returned by {@link #getSigningTime(SignerInfo)} then that time is used to check the 
	 * validity of the certificates; otherwise the current time is used.
	 * @param signerInfo the signer info to check validity for.
	 * @throws CertificateExpiredException if one of the certificates of this signer is expired
	 * @throws CertificateNotYetValidException if one of the certificates of this signer is not yet valid
	 */
	public void checkValidity(SignerInfo signerInfo) throws CertificateExpiredException, CertificateNotYetValidException;
}
