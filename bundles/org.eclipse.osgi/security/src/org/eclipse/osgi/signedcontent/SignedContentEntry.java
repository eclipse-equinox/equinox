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

import java.io.IOException;

// encapsulates the status of an entry: isSigned, timestamp info, SignerInfos, etc
// implemented by SignedBundleFile.SignedBundleEntry
/**
 * A <code>SignedContentEntry</code> represents a content entry which may be
 * signed.
 * <p>
 * A <code>SignedContentEntry</code> object is intended to provide information about 
 * the signers of the content entry, and cannot be used to access the actual data of the entry.
 * </p>
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 * @since 3.4
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface SignedContentEntry {
	/**
	 * Returns the name of the entry.
	 * @return the name of the entry.
	 */
	public String getName();

	/**
	 * Returns the signer infos for this <code>SignedContentEntry</code>.  If the entry
	 * is not signed then an empty array is returned.
	 * @return the signer infos for this <code>SignedContentEntry</code>
	 */
	public SignerInfo[] getSignerInfos();

	/**
	 * Returns true if the entry is signed; false otherwise.  This is a convenience method
	 * equivalent to calling <code>{@link #getSignerInfos()}.length > 0</code> 
	 * @return true if the content is signed
	 */
	public boolean isSigned();

	// Does the digest of this entry match what is expected?
	// TODO: what does this mean in the face of multiple signers
	/**
	 * Verifies the content of this this entry is valid.
	 * @throws IOException if an error occurred reading the entry content
	 * @throws InvalidContentException if the entry content is not valid
	 */
	public void verify() throws IOException, InvalidContentException;

}
