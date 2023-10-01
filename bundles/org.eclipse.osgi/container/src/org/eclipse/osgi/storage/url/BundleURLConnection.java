/*******************************************************************************
 * Copyright (c) 2004, 2021 IBM Corporation and others.
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
 *     Hannes Wellmann - Bug 576644: Implement BundleReference for BundleURLConnection
 *******************************************************************************/

package org.eclipse.osgi.storage.url;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.container.ModuleContainer;
import org.eclipse.osgi.internal.messages.Msg;
import org.eclipse.osgi.storage.bundlefile.BundleEntry;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;

/**
 * URLConnection for BundleClassLoader resources.
 */
public class BundleURLConnection extends URLConnection implements BundleReference {
	/** BundleEntry that the URL is associated. */
	protected final BundleEntry bundleEntry;

	private final ModuleContainer container;

	/** InputStream for this URLConnection. */
	protected InputStream in;

	/** content type for this URLConnection */
	protected String contentType;

	/**
	 * Constructor for a BundleClassLoader resource URLConnection.
	 *
	 * @param url         URL for this URLConnection.
	 * @param bundleEntry BundleEntry that the URLConnection is associated.
	 */
	public BundleURLConnection(URL url, ModuleContainer container, BundleEntry bundleEntry) {
		super(url);

		this.container = container;
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
				throw new IOException(NLS.bind(Msg.RESOURCE_NOT_FOUND_EXCEPTION, getURL()));
			}
		}
	}

	@Override
	public long getContentLengthLong() {
		return bundleEntry.getSize();
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
						return null;
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
		return contentType;
	}

	@Override
	public boolean getDoInput() {
		return true;
	}

	@Override
	public boolean getDoOutput() {
		return false;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		if (!connected) {
			connect();
		}
		return in;
	}

	@Override
	public long getLastModified() {
		long lastModified = bundleEntry.getTime();

		if (lastModified == -1) {
			return (0);
		}
		return lastModified;
	}

	/**
	 * Converts the URL to a common local URL protocol (i.e file: or jar: protocol)
	 * 
	 * @return the local URL using a common local protocol
	 */
	public URL getLocalURL() {
		URL local = bundleEntry.getLocalURL();
		return local == null ? getURL() : local;
	}

	/**
	 * Converts the URL to a URL that uses the file: protocol. The content of this
	 * URL may be downloaded or extracted onto the local filesystem to create a file
	 * URL.
	 * 
	 * @return the local URL that uses the file: protocol
	 */
	public URL getFileURL() {
		return bundleEntry.getFileURL();
	}

	@Override
	public Bundle getBundle() {
		String host = getURL().getHost();
		long bundleId = BundleResourceHandler.parseBundleIDFromURLHost(host);
		Module module = container.getModule(bundleId);
		return module != null ? module.getBundle() : null;
	}
}
