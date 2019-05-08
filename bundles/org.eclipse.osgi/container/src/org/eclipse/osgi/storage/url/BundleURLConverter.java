/*******************************************************************************
 * Copyright (c) 2004, 2013 IBM Corporation and others.
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
package org.eclipse.osgi.storage.url;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import org.eclipse.osgi.internal.messages.Msg;
import org.eclipse.osgi.service.urlconversion.URLConverter;
import org.eclipse.osgi.util.NLS;

/**
 * The service implementation that allows bundleresource or bundleentry
 * URLs to be converted to native file URLs on the local file system.
 * 
 * <p>Internal class.</p>
 */
public class BundleURLConverter implements URLConverter {

	/* (non-Javadoc)
	 * @see org.eclipse.osgi.service.urlconversion.URLConverter#toFileURL(java.net.URL)
	 */
	@Override
	public URL toFileURL(URL url) throws IOException {
		URLConnection connection = url.openConnection();
		if (connection instanceof BundleURLConnection) {
			URL result = ((BundleURLConnection) connection).getFileURL();
			/* If we got a connection then we know the resource exists in
			 * the bundle but if connection.getFileURL returned null then there
			 * was a problem extracting the file to disk. See bug 259241.
			 **/
			if (result == null)
				throw new IOException(NLS.bind(Msg.ECLIPSE_PLUGIN_EXTRACTION_PROBLEM, url));
			return result;
		}
		return url;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.osgi.service.urlconversion.URLConverter#resolve(java.net.URL)
	 */
	@Override
	public URL resolve(URL url) throws IOException {
		URLConnection connection = url.openConnection();
		if (connection instanceof BundleURLConnection)
			return ((BundleURLConnection) connection).getLocalURL();
		return url;
	}
}
