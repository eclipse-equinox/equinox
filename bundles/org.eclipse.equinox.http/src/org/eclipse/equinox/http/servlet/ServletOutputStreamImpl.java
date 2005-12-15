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
import javax.servlet.ServletOutputStream;

/**
 * The implementation of javax.servlet.ServletOutputStream.
 *
 * When a flush or close method is called on an implementation of this class, any
 * data buffered by the servlet engine is sent to the client and the response is
 * considered to be "committed". Note that calling close on an object of this type
 * doesn't necessarily close the underlying socket stream.
 */

//BUGBUG - override print methods for better performance!!!!
class ServletOutputStreamImpl extends ServletOutputStream {
	/** Actual output stream */
	private OutputStream realOut;

	/** response object */
	private HttpServletResponseImpl response;

	/** false if the ServletOutputStream has been closed */
	private boolean open;

	/** Place to buffer the output data */
	private ByteArrayOutputStream buffer;

	/** Place to buffer the output data */
	private OutputStream out;

	/** true if we need to write the response headers */
	private boolean writeHeaders;

	/** true if the flush method should flush */
	private boolean flush;

	/** true if this ServletOutputStream is closing */
	private boolean closing;

	ServletOutputStreamImpl(OutputStream realOut, HttpServletResponseImpl response) {
		this.realOut = realOut;
		this.response = response;

		// BUGBUG Make the default buffer size configurable.
		buffer = new ByteArrayOutputStream(8192); /* start with a 8k buffer */
		out = buffer; /* begin with buffer */
		open = true;
		writeHeaders = true;
		closing = false;
		flush = true;
	}

	/**
	 * This method is called by a wrapper to disable normal flush
	 * function until the close method is called.
	 */
	synchronized void disableFlush() {
		flush = false; /* disable flush until we are closed */
	}

	public synchronized void close() throws IOException {
		if (open) {
			closing = true; /* allow content size to be calculated */
			flush = true; /* enable normal flush function */

			flush();

			open = false; /* disable this ServletOutputStream */
		}
	}

	public synchronized void flush() throws IOException {
		if (open) {
			if (writeHeaders) {
				if (flush) {
					/* These must be set before calling writeHeaders */
					writeHeaders = false;
					out = realOut;

					/* write the response headers */
					response.writeHeaders(closing ? buffer.size() : -1);

					/* copy the buffered output to the real OutputStream */
					buffer.writeTo(out);

					/* dereference buffer so it may be garbage collected */
					buffer = null;

					out.flush();
				}
			} else {
				out.flush();
			}
		} else {
			throw new IOException("closed"); //$NON-NLS-1$
		}
	}

	public synchronized void write(byte[] bytes) throws IOException {
		if (open) {
			out.write(bytes, 0, bytes.length);
		} else {
			throw new IOException("closed"); //$NON-NLS-1$
		}
	}

	public synchronized void write(byte[] bytes, int off, int len) throws IOException {
		if (open) {
			out.write(bytes, off, len);
		} else {
			throw new IOException("closed"); //$NON-NLS-1$
		}
	}

	public synchronized void write(int b) throws IOException {
		if (open) {
			out.write(b);
		} else {
			throw new IOException("closed"); //$NON-NLS-1$
		}
	}
}
