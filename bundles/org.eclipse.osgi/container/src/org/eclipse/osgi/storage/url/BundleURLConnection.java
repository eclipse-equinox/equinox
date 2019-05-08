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
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import org.eclipse.osgi.internal.messages.Msg;
import org.eclipse.osgi.storage.bundlefile.BundleEntry;
import org.eclipse.osgi.util.NLS;

/**
 * URLConnection for BundleClassLoader resources.
 */

public class BundleURLConnection extends URLConnection {
	/** BundleEntry that the URL is associated. */
	protected final BundleEntry bundleEntry;

	/** InputStream for this URLConnection. */
	protected InputStream in;

	/** content type for this URLConnection */
	protected String contentType;

	/**
	 * Constructor for a BundleClassLoader resource URLConnection.
	 *
	 * @param url  URL for this URLConnection.
	 * @param bundleEntry  BundleEntry that the URLConnection is associated.
	 */
	public BundleURLConnection(URL url, BundleEntry bundleEntry) {
		super(url);

		this.bundleEntry = bundleEntry;
		this.in = null;
		this.contentType = null;
	}

	@Override
	public synchronized void connect() throws IOException {
		if (!connected) {
			if (bundleEntry != null) {
				in = bundleEntry.getInputStream();
				connected = true;
			} else {
				throw new IOException(NLS.bind(Msg.RESOURCE_NOT_FOUND_EXCEPTION, url));
			}
		}
	}

	@Override
	public int getContentLength() {
		return ((int) bundleEntry.getSize());
	}

	@Override
	public String getContentType() {
		if (contentType == null) {
			contentType = guessContentTypeFromName(bundleEntry.getName());

			if (contentType == null) {
				if (!connected) {
					try {
						connect();
					} catch (IOException e) {
						return (null);
					}
				}
				try {
					if (in.markSupported())
						contentType = guessContentTypeFromStream(in);
				} catch (IOException e) {
					// do nothing
				}
			}
		}

		return (contentType);
	}

	@Override
	public boolean getDoInput() {
		return (true);
	}

	@Override
	public boolean getDoOutput() {
		return (false);
	}

	@Override
	public InputStream getInputStream() throws IOException {
		if (!connected) {
			connect();
		}

		return (in);
	}

	@Override
	public long getLastModified() {
		long lastModified = bundleEntry.getTime();

		if (lastModified == -1) {
			return (0);
		}

		return (lastModified);
	}

	/**
	 * Converts the URL to a common local URL protocol (i.e file: or jar: protocol)
	 * @return the local URL using a common local protocol
	 */
	public URL getLocalURL() {
		return bundleEntry.getLocalURL();
	}

	/**
	 * Converts the URL to a URL that uses the file: protocol.  The content of this
	 * URL may be downloaded or extracted onto the local filesystem to create a file URL.
	 * @return the local URL that uses the file: protocol
	 */
	public URL getFileURL() {
		return bundleEntry.getFileURL();
	}
}
