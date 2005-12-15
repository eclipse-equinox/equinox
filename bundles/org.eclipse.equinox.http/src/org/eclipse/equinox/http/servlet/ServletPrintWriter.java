/*******************************************************************************
 * Copyright (c) 1999, 2005 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.http.servlet;

import java.io.*;

/**
 * PrintWriter subclass for servlets. This class wraps a
 * ServletOutputStream to support Content-Length generation.
 */
class ServletPrintWriter extends PrintWriter {
	/** underlying ServletOutputStream */
	private ServletOutputStreamImpl out;

	/**
	 * Create the PrintWriter w/o autoflush for the specified encoding.
	 *
	 * @param out Underlying ServletOutputStream.
	 * @param encoding Encoding to use for the
	 * @throws UnsupportedEncodingException if the encoding is not supported.
	 */
	ServletPrintWriter(ServletOutputStreamImpl out, String encoding) throws UnsupportedEncodingException {
		super(new OutputStreamWriter(out, encoding), false);

		this.out = out;
	}

	/**
	 * Close this PrintWriter.
	 *
	 * Tell the underlying ServletOutputStream we are closing.
	 */
	public void close() {
		out.disableFlush(); /* disable flush operation until closed */

		super.close();
	}
}
