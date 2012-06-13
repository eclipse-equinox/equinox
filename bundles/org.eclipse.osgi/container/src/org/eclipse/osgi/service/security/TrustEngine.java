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
package org.eclipse.osgi.service.security;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.Certificate;
import org.eclipse.osgi.internal.signedcontent.TrustEngineListener;

/**
 * A <code>TrustEngine</code> is used to establish the authenticity of a 
 * {@link Certificate} chain.
 * <p>
 * Clients may implement this interface.
 * </p>
 * @since 3.4
 */
public abstract class TrustEngine {
	/**
	 * Returns the certificate trust anchor contained in the specified chain which
	 * was used to establish the authenticity of the chain.  If no 
	 * trust anchor is found in the chain then <code>null</code> is returned.
	 * @param chain - a complete or incomplete certificate chain, implementations *MAY* complete chains
	 * @return - the certificate trust anchor used to establish authenticity
	 * @throws IOException if there is a problem connecting to the backing store
	 */
	public abstract Certificate findTrustAnchor(Certificate[] chain) throws IOException;

	/**
	 * Add a trust anchor point to this trust engine. A trust anchor implies that a certificate, 
	 * and any of its children, is to be considered trusted.  If <code>null</code> is used
	 * as the alias then an alias will be generated based on the trust anchor certificate.
	 * @param anchor - the certificate to add as an anchor point
	 * @param alias - a unique and human-readable 'friendly name' which can be used to reference the certificate.
	 *    A <code>null</code> value may be used.
	 * @return the alias used to store the entry
	 * @throws IOException if there is a problem connecting to the backing store
	 * @throws GeneralSecurityException if there is a certificate problem
	 * @throws IllegalArgumentException if the alias or anchor already exist in this trust engine
	 */
	public String addTrustAnchor(Certificate anchor, String alias) throws IOException, GeneralSecurityException {
		String storedAlias = doAddTrustAnchor(anchor, alias);
		TrustEngineListener listener = TrustEngineListener.getInstance();
		if (listener != null)
			listener.addedTrustAnchor(anchor);
		return storedAlias;
	}

	/**
	 * Add a trust anchor point to this trust engine. A trust anchor implies that a certificate, 
	 * and any of its children, is to be considered trusted.  If <code>null</code> is used
	 * as the alias then an alias will be generated based on the trust anchor certificate.
	 * @param anchor - the certificate to add as an anchor point
	 * @param alias - a unique and human-readable 'friendly name' which can be used to reference the certificate.
	 *    A <code>null</code> value may be used.
	 * @return the alias used to store the entry
	 * @throws IOException if there is a problem connecting to the backing store
	 * @throws GeneralSecurityException if there is a certificate problem
	 * @throws IllegalArgumentException if the alias or anchor already exist in this trust engine
	 */
	protected abstract String doAddTrustAnchor(Certificate anchor, String alias) throws IOException, GeneralSecurityException;

	/**
	 * Remove a trust anchor point from the engine, based on the certificate itself.
	 * @param anchor - the certificate to be removed
	 * @throws IOException if there is a problem connecting to the backing store
	 * @throws GeneralSecurityException if there is a certificate problem
	 */
	public final void removeTrustAnchor(Certificate anchor) throws IOException, GeneralSecurityException {
		doRemoveTrustAnchor(anchor);
		TrustEngineListener listener = TrustEngineListener.getInstance();
		if (listener != null)
			listener.removedTrustAnchor(anchor);
	}

	/**
	 * Remove a trust anchor point from the engine, based on the certificate itself.
	 * @param anchor - the certificate to be removed
	 * @throws IOException if there is a problem connecting to the backing store
	 * @throws GeneralSecurityException if there is a certificate problem
	 */
	protected abstract void doRemoveTrustAnchor(Certificate anchor) throws IOException, GeneralSecurityException;

	/**
	 * Remove a trust anchor point from the engine, based on the human readable "friendly name"
	 * @param alias - the name of the trust anchor
	 * @throws IOException if there is a problem connecting to the backing store
	 * @throws GeneralSecurityException if there is a certificate problem
	 */
	public void removeTrustAnchor(String alias) throws IOException, GeneralSecurityException {
		Certificate existing = getTrustAnchor(alias);
		doRemoveTrustAnchor(alias);
		if (existing != null) {
			TrustEngineListener listener = TrustEngineListener.getInstance();
			if (listener != null)
				listener.removedTrustAnchor(existing);
		}
	}

	/**
	 * Remove a trust anchor point from the engine, based on the human readable "friendly name"
	 * @param alias - the name of the trust anchor
	 * @throws IOException if there is a problem connecting to the backing store
	 * @throws GeneralSecurityException if there is a certificate problem
	 */
	protected abstract void doRemoveTrustAnchor(String alias) throws IOException, GeneralSecurityException;

	/**
	 * Return the certificate associated with the unique "friendly name" in the engine.
	 * @param alias - the friendly name  
	 * @return the associated trust anchor
	 * @throws IOException if there is a problem connecting to the backing store
	 * @throws GeneralSecurityException if there is a certificate problem
	 */
	public abstract Certificate getTrustAnchor(String alias) throws IOException, GeneralSecurityException;

	/**
	 * Return the list of friendly name aliases for the TrustAnchors installed in the engine.
	 * @return string[] - the list of friendly name aliases
	 * @throws IOException if there is a problem connecting to the backing store
	 * @throws GeneralSecurityException if there is a certificate problem
	 */
	public abstract String[] getAliases() throws IOException, GeneralSecurityException;

	/**
	 * Return a value indicate whether this trust engine is read-only.
	 * 
	 * @return	true if this trust engine is read-only false otherwise.
	 */
	public abstract boolean isReadOnly();

	/**
	 * Return a representation string of this trust engine
	 * 
	 * @return	a string
	 */
	public abstract String getName();
}
