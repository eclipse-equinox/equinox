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

package org.eclipse.osgi.framework.internal.protocol.reference;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import org.eclipse.osgi.framework.internal.defaultadaptor.ReferenceInputStream;

/**
 * URLConnection for BundleClassLoader resources.
 */

public class ReferenceURLConnection extends URLConnection {
	protected URL reference;

	/**
	 * Constructor for a BundleClassLoader resource URLConnection.
	 *
	 * @param url  URL for this URLConnection.
	 * @param bundleEntry  BundleEntry that the URLConnection is associated.
	 */
	protected ReferenceURLConnection(URL url) {
		super(url);
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
			URL ref = new URL(url.getPath());
			if (!new File(ref.getFile()).exists())
				throw new FileNotFoundException();
			reference = ref;
		}
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
		return true;
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
		return false;
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

		return new ReferenceInputStream(reference);
	}

}
