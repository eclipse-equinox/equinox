/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.adaptor.core;

import java.io.IOException;
import java.net.URL;
import java.util.Dictionary;

import org.eclipse.osgi.framework.adaptor.BundleData;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.framework.internal.core.Msg;
import org.eclipse.osgi.framework.util.Headers;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * An abstract BundleData class that has default implementations that most
 * BundleData implementations can use.
 */
public abstract class AbstractBundleData implements BundleData {

	/**
	 * The BundleManfifest for this BundleData.
	 */
	protected Dictionary manifest = null;

	/**
	 * The Bundle object for this BundleData.
	 */
	protected Bundle bundle;

	/**
	 * Return the BundleManifest for the BundleData.  If the manifest
	 * field is null this method will parse the bundle manifest file and
	 * construct a BundleManifest file to return.  If the manifest field is
	 * not null then the manifest object is returned.
	 * @return BundleManifest for the BundleData.
	 * @throws BundleException if an error occurred while reading the
	 * bundle manifest data.
	 */
	public Dictionary getManifest() throws BundleException {
		if (manifest == null) {
			synchronized (this) {
				// make sure the manifest is still null after we have aquired the lock.
				if (manifest == null) {
					URL url = getEntry(Constants.OSGI_BUNDLE_MANIFEST);
					if (url == null){
						throw new BundleException(Msg.formatter.getString("MANIFEST_NOT_FOUND_EXCEPTION",Constants.OSGI_BUNDLE_MANIFEST,getLocation()));
					}
					try {
						manifest = Headers.parseManifest(url.openStream());
					} catch (IOException e) {
						throw new BundleException(Msg.formatter.getString("MANIFEST_NOT_FOUND_EXCEPTION",Constants.OSGI_BUNDLE_MANIFEST,getLocation()), e);
					}
				}
			}
		}
		return manifest;
	}

	/**
	 * Returns a Dictionary of all manifest headers.  Returns the 
	 * BundleManifest object of this BundleData.  Note that the core 
	 * implementation of BundleManifest extends the Dictionary class.
	 * @return A Dictionary of all manifest headers.
	 */
	public Dictionary getHeaders(){
		try {
			return getManifest();
		} catch (BundleException e) {
			return null;
		}
	}

	/**
	 * Sets the Bundle object for this BundleData.
	 * @param bundle The Bundle Object for this BundleData.
	 */
	public void setBundle(Bundle bundle){
		this.bundle = bundle;
	}
}
