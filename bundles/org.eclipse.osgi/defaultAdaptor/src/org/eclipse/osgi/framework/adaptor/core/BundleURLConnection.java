/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
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
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

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

	/**
	 * Establishes the connection to the resource specified by this <code>URL</code>
	 * with this <code>method</code>, along with other options that can only be set before
	 * this connection is made.
	 *
	 * @see 		connected
	 * @see 		java.io.IOException
	 * @see 		URLStreamHandler
	 */
	public synchronized void connect() throws IOException {
		if (!connected) {
			if (bundleEntry != null) {
				in = bundleEntry.getInputStream();
				connected = true;
			} else {
				throw new IOException(AdaptorMsg.formatter.getString("RESOURCE_NOT_FOUND_EXCEPTION", url));
			}
		}
	}

	/**
	 * Answers the length of the content or body in the response header in bytes.
	 * Answer -1 if <code> Content-Length </code> cannot be found in the response header.
	 *
	 * @return int		The length of the content
	 *
	 * @see			getContentType()
	 */
	public int getContentLength() {
		if (!connected) {
			try {
				connect();
			} catch (IOException e) {
				return (-1);
			}
		}

		return ((int) bundleEntry.getSize());
	}

	/**
	 * Answers the type of the content.
	 * Answers <code> null </code> if there's no such field.
	 *
	 * @return java.lang.String		The type of the content
	 *
	 * @see			 guessContentTypeFromName()
	 * @see			 guessContentTypeFromStream()
	 */
	public String getContentType() {
		if (!connected) {
			try {
				connect();
			} catch (IOException e) {
				return (null);
			}
		}

		if (contentType == null) {
			contentType = guessContentTypeFromName(bundleEntry.getName());

			if (contentType == null) {
				try {
					InputStream in = bundleEntry.getInputStream();

					try {
						contentType = guessContentTypeFromStream(in);
					} finally {
						if (in != null) {
							try {
								in.close();
							} catch (IOException ee) {
							}
						}
					}
				} catch (IOException e) {
				}
			}
		}

		return (contentType);
	}

	/**
	 * Answers whether this connection supports input.
	 *
	 * @return boolean		true if this connection supports input, false otherwise
	 *
	 * @see			setDoInput()
	 * @see			doInput
	 */
	public boolean getDoInput() {
		return (true);
	}

	/**
	 * Answers whether this connection supports output.
	 *
	 * @return boolean		true if this connection supports output, false otherwise
	 *
	 * @see			setDoOutput()
	 * @see			doOutput
	 */
	public boolean getDoOutput() {
		return (false);
	}

	/**
	 * Creates an InputStream for reading from this URL Connection.
	 * It throws UnknownServiceException by default.
	 * This method should be overridden by its subclasses
	 *
	 * @return 		InputStream		The InputStream to read from
	 * @exception 	IOException 	If an InputStream could not be created
	 *
	 * @see 		getContent()
	 * @see 		getOutputStream()
	 * @see 		java.io.InputStream
	 * @see 		java.io.IOException
	 *
	 */
	public InputStream getInputStream() throws IOException {
		if (!connected) {
			connect();
		}

		return (in);
	}

	/**
	 * Answers the value of the field <code>Last-Modified</code> in the response header,
	 * 		 	0 if no such field exists
	 *
	 * @return The last modified time.
	 */
	public long getLastModified() {
		if (!connected) {
			try {
				connect();
			} catch (IOException e) {
				return (0);
			}
		}

		long lastModified = bundleEntry.getTime();

		if (lastModified == -1) {
			return (0);
		}

		return (lastModified);
	}

	public URL getLocalURL() {
		return bundleEntry.getLocalURL();
	}

	public URL getFileURL() {
		return bundleEntry.getFileURL();
	}
}
