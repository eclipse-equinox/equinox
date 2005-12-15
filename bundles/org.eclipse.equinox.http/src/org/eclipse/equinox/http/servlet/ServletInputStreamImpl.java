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

import java.io.IOException;
import java.io.InputStream;
import javax.servlet.ServletInputStream;

/**
 * An input stream for reading servlet requests, it provides an
 * efficient readLine method.  This is an abstract class, to be
 * implemented by a network services writer.  For some application
 * protocols, such as the HTTP POST and PUT methods, servlet writers
 * use the input stream to get data from clients.  They access the
 * input stream via the ServletRequest's getInputStream method,
 * available from within the servlet's service method.  Subclasses of
 * ServletInputStream must provide an implementation of the read()
 * method.
 *
 * <p>
 * This InputStream Object just forwards all requests to the real InputStream object
 * It is intended that that would be the real Sock InputStream.  This has some
 * problems if we ever need to restart the stream with another Servlet/etc, so this
 * may need to change some day change.
 */
public class ServletInputStreamImpl extends ServletInputStream {
	private InputStream in;
	private ServletInputStream servletInputStream = null;

	protected ServletInputStreamImpl(InputStream in) {
		super();
		this.in = in;
	}

	private void checkOpen() throws IOException {
		if (in == null) {
			throw new IOException("ServletInputStream closed"); //$NON-NLS-1$
		}
	}

	/**
	 * Returns the number of bytes that can be read from this input
	 * stream without blocking. The available method of
	 * <code>InputStream</code> returns <code>0</code>. This method
	 * <B>should</B> be overridden by subclasses.
	 *
	 * @return     the number of bytes that can be read from this input stream
	 *             without blocking.
	 * @exception  IOException  if an I/O error occurs.
	 * @since	   JDK1.0
	 */
	public int available() throws IOException {
		checkOpen();

		return in.available();
	}

	/**
	 * Closes this input stream and releases any system resources
	 * associated with the stream.
	 * <p>
	 * The <code>close</code> method of <code>InputStream</code> does nothing.
	 *
	 * @exception  IOException  if an I/O error occurs.
	 * @since      JDK1.0
	 */
	public void close() throws IOException {
		in = null;
	}

	/**
	 * Return the ServletInputStream to use for reading data after the
	 * HTTP headers have been read. If a Content-Length header is available
	 *
	 * @param len Content-Length of InputStream
	 * @return InputStream for use by servlets
	 * @exception
	 */
	ServletInputStream getServletInputStream(int len) {
		if (servletInputStream == null) {
			synchronized (this) {
				if (servletInputStream == null) {
					if (len > 0) {
						servletInputStream = new ContentLength(this, len);
					} else {
						servletInputStream = this;
					}
				}
			}
		}

		return servletInputStream;
	}

	/**
	 * Marks the current position in this input stream. A subsequent
	 * call to the <code>reset</code> method repositions this stream at
	 * the last marked position so that subsequent reads re-read the same
	 * bytes.
	 * <p>
	 * The <code>readlimit</code> arguments tells this input stream to
	 * allow that many bytes to be read before the mark position gets
	 * invalidated.
	 * <p>
	 * The <code>mark</code> method of <code>InputStream</code> does nothing.
	 *
	 * @param   readlimit   the maximum limit of bytes that can be read before
	 *                      the mark position becomes invalid.
	 * @see     java.io.InputStream#reset()
	 * @since   JDK1.0
	 */
	public void mark(int readLimit) {
		in.mark(readLimit);
	}

	/**
	 * Tests if this input stream supports the <code>mark</code>
	 * and <code>reset</code> methods. The <code>markSupported</code>
	 * method of <code>InputStream</code> returns <code>false</code>.
	 *
	 * @return  <code>true</code> if this true type supports the mark and reset
	 *          method; <code>false</code> otherwise.
	 * @see     java.io.InputStream#mark(int)
	 * @see     java.io.InputStream#reset()
	 * @since   JDK1.0
	 */
	public boolean markSupported() {
		return in.markSupported();
	}

	/**
	 * Reads the next byte of data from this input stream. The value
	 * byte is returned as an <code>int</code> in the range
	 * <code>0</code> to <code>255</code>. If no byte is available
	 * because the end of the stream has been reached, the value
	 * <code>-1</code> is returned. This method blocks until input data
	 * is available, the end of the stream is detected, or an exception
	 * is thrown.
	 * <p>
	 * A subclass must provide an implementation of this method.
	 *
	 * @return     the next byte of data, or <code>-1</code> if the end of the
	 *             stream is reached.
	 * @exception  IOException  if an I/O error occurs.
	 * @since      JDK1.0
	 */
	public int read() throws IOException {
		checkOpen();

		return in.read();
	}

	/**
	 * Reads up to <code>b.length</code> bytes of data from this input
	 * stream into an array of bytes.
	 * <p>
	 * The <code>read</code> method of <code>InputStream</code> calls
	 * the <code>read</code> method of three arguments with the arguments
	 * <code>b</code>, <code>0</code>, and <code>b.length</code>.
	 *
	 * @param      b   the buffer into which the data is read.
	 * @return     the total number of bytes read into the buffer, or
	 *             <code>-1</code> is there is no more data because the end of
	 *             the stream has been reached.
	 * @exception  IOException  if an I/O error occurs.
	 * @see        java.io.InputStream#read(byte[], int, int)
	 * @since      JDK1.0
	 */
	public int read(byte[] b) throws IOException {
		checkOpen();

		return in.read(b, 0, b.length);
	}

	/**
	 * Reads up to <code>len</code> bytes of data from this input stream
	 * into an array of bytes. This method blocks until some input is
	 * available. If the argument <code>b</code> is <code>null</code>, a
	 * <code>NullPointerException</code> is thrown.
	 * <p>
	 * The <code>read</code> method of <code>InputStream</code> reads a
	 * single byte at a time using the read method of zero arguments to
	 * fill in the array. Subclasses are encouraged to provide a more
	 * efficient implementation of this method.
	 *
	 * @param      b     the buffer into which the data is read.
	 * @param      off   the start offset of the data.
	 * @param      len   the maximum number of bytes read.
	 * @return     the total number of bytes read into the buffer, or
	 *             <code>-1</code> if there is no more data because the end of
	 *             the stream has been reached.
	 * @exception  IOException  if an I/O error occurs.
	 * @see        java.io.InputStream#read()
	 * @since      JDK1.0
	 */
	public int read(byte[] b, int off, int len) throws IOException {
		checkOpen();

		return in.read(b, off, len);
	}

	/**
	 * Repositions this stream to the position at the time the
	 * <code>mark</code> method was last called on this input stream.
	 * <p>
	 * The <code>reset</code> method of <code>InputStream</code> throws
	 * an <code>IOException</code>, because input streams, by default, do
	 * not support <code>mark</code> and <code>reset</code>.
	 * <p>
	 * Stream marks are intended to be used in
	 * situations where you need to read ahead a little to see what's in
	 * the stream. Often this is most easily done by invoking some
	 * general parser. If the stream is of the type handled by the
	 * parser, it just chugs along happily. If the stream is not of
	 * that type, the parser should toss an exception when it fails,
	 * which, if it happens within readlimit bytes, allows the outer
	 * code to reset the stream and try another parser.
	 *
	 * @exception  IOException  if this stream has not been marked or if the
	 *               mark has been invalidated.
	 * @see     java.io.InputStream#mark(int)
	 * @see     java.io.IOException
	 * @since   JDK1.0
	 */
	public void reset() throws IOException {
		checkOpen();

		in.reset();
	}

	/**
	 * Skips over and discards <code>n</code> bytes of data from this
	 * input stream. The <code>skip</code> method may, for a variety of
	 * reasons, end up skipping over some smaller number of bytes,
	 * possibly <code>0</code>. The actual number of bytes skipped is
	 * returned.
	 * <p>
	 * The <code>skip</code> method of <code>InputStream</code> creates
	 * a byte array of length <code>n</code> and then reads into it until
	 * <code>n</code> bytes have been read or the end of the stream has
	 * been reached. Subclasses are encouraged to provide a more
	 * efficient implementation of this method.
	 *
	 * @param      n   the number of bytes to be skipped.
	 * @return     the actual number of bytes skipped.
	 * @exception  IOException  if an I/O error occurs.
	 * @since      JDK1.0
	 */
	public long skip(long len) throws IOException {
		checkOpen();

		return in.skip(len);
	}

	/**
	 * ServletInputStream which limit readable data to the specified content length.
	 * After the number of bytes have been read, EOF(-1) is returned from the read
	 * method.
	 *
	 */
	static class ContentLength extends ServletInputStream {
		private int contentLength;
		private int mark;
		private ServletInputStream in;

		ContentLength(ServletInputStream in, int len) {
			super();
			this.in = in;
			contentLength = len;
			mark = len;
		}

		/**
		 * Returns the number of bytes that can be read from this input
		 * stream without blocking. The available method of
		 * <code>InputStream</code> returns <code>0</code>. This method
		 * <B>should</B> be overridden by subclasses.
		 *
		 * @return     the number of bytes that can be read from this input stream
		 *             without blocking.
		 * @exception  IOException  if an I/O error occurs.
		 * @since	   JDK1.0
		 */
		public int available() throws IOException {
			int avail = in.available();

			if (contentLength < avail) {
				return contentLength;
			}

			return avail;
		}

		/**
		 * Closes this input stream and releases any system resources
		 * associated with the stream.
		 * <p>
		 * The <code>close</code> method of <code>InputStream</code> does nothing.
		 *
		 * @exception  IOException  if an I/O error occurs.
		 * @since      JDK1.0
		 */
		public void close() throws IOException {
			in.close();
		}

		/**
		 * Marks the current position in this input stream. A subsequent
		 * call to the <code>reset</code> method repositions this stream at
		 * the last marked position so that subsequent reads re-read the same
		 * bytes.
		 * <p>
		 * The <code>readlimit</code> arguments tells this input stream to
		 * allow that many bytes to be read before the mark position gets
		 * invalidated.
		 * <p>
		 * The <code>mark</code> method of <code>InputStream</code> does nothing.
		 *
		 * @param   readlimit   the maximum limit of bytes that can be read before
		 *                      the mark position becomes invalid.
		 * @see     java.io.InputStream#reset()
		 * @since   JDK1.0
		 */
		public void mark(int readLimit) {
			in.mark(readLimit);

			mark = contentLength;
		}

		/**
		 * Tests if this input stream supports the <code>mark</code>
		 * and <code>reset</code> methods. The <code>markSupported</code>
		 * method of <code>InputStream</code> returns <code>false</code>.
		 *
		 * @return  <code>true</code> if this true type supports the mark and reset
		 *          method; <code>false</code> otherwise.
		 * @see     java.io.InputStream#mark(int)
		 * @see     java.io.InputStream#reset()
		 * @since   JDK1.0
		 */
		public boolean markSupported() {
			return in.markSupported();
		}

		/**
		 * Reads the next byte of data from this input stream. The value
		 * byte is returned as an <code>int</code> in the range
		 * <code>0</code> to <code>255</code>. If no byte is available
		 * because the end of the stream has been reached, the value
		 * <code>-1</code> is returned. This method blocks until input data
		 * is available, the end of the stream is detected, or an exception
		 * is thrown.
		 * <p>
		 * A subclass must provide an implementation of this method.
		 *
		 * @return     the next byte of data, or <code>-1</code> if the end of the
		 *             stream is reached.
		 * @exception  IOException  if an I/O error occurs.
		 * @since      JDK1.0
		 */
		public int read() throws IOException {
			if (contentLength <= 0) {
				return -1;
			}

			int read = in.read();

			contentLength--;

			return read;
		}

		/**
		 * Reads up to <code>b.length</code> bytes of data from this input
		 * stream into an array of bytes.
		 * <p>
		 * The <code>read</code> method of <code>InputStream</code> calls
		 * the <code>read</code> method of three arguments with the arguments
		 * <code>b</code>, <code>0</code>, and <code>b.length</code>.
		 *
		 * @param      b   the buffer into which the data is read.
		 * @return     the total number of bytes read into the buffer, or
		 *             <code>-1</code> is there is no more data because the end of
		 *             the stream has been reached.
		 * @exception  IOException  if an I/O error occurs.
		 * @see        java.io.InputStream#read(byte[], int, int)
		 * @since      JDK1.0
		 */
		public int read(byte[] b) throws IOException {
			if (contentLength <= 0) {
				return -1;
			}

			int len = b.length;

			if (contentLength < len) {
				len = contentLength;
			}

			int read = in.read(b, 0, len);

			contentLength -= read;

			return read;
		}

		/**
		 * Reads up to <code>len</code> bytes of data from this input stream
		 * into an array of bytes. This method blocks until some input is
		 * available. If the argument <code>b</code> is <code>null</code>, a
		 * <code>NullPointerException</code> is thrown.
		 * <p>
		 * The <code>read</code> method of <code>InputStream</code> reads a
		 * single byte at a time using the read method of zero arguments to
		 * fill in the array. Subclasses are encouraged to provide a more
		 * efficient implementation of this method.
		 *
		 * @param      b     the buffer into which the data is read.
		 * @param      off   the start offset of the data.
		 * @param      len   the maximum number of bytes read.
		 * @return     the total number of bytes read into the buffer, or
		 *             <code>-1</code> if there is no more data because the end of
		 *             the stream has been reached.
		 * @exception  IOException  if an I/O error occurs.
		 * @see        java.io.InputStream#read()
		 * @since      JDK1.0
		 */
		public int read(byte[] b, int off, int len) throws IOException {
			if (contentLength <= 0) {
				return -1;
			}

			if (contentLength < len) {
				len = contentLength;
			}

			int read = in.read(b, off, len);

			contentLength -= read;

			return read;
		}

		/**
		 * Repositions this stream to the position at the time the
		 * <code>mark</code> method was last called on this input stream.
		 * <p>
		 * The <code>reset</code> method of <code>InputStream</code> throws
		 * an <code>IOException</code>, because input streams, by default, do
		 * not support <code>mark</code> and <code>reset</code>.
		 * <p>
		 * Stream marks are intended to be used in
		 * situations where you need to read ahead a little to see what's in
		 * the stream. Often this is most easily done by invoking some
		 * general parser. If the stream is of the type handled by the
		 * parser, it just chugs along happily. If the stream is not of
		 * that type, the parser should toss an exception when it fails,
		 * which, if it happens within readlimit bytes, allows the outer
		 * code to reset the stream and try another parser.
		 *
		 * @exception  IOException  if this stream has not been marked or if the
		 *               mark has been invalidated.
		 * @see     java.io.InputStream#mark(int)
		 * @see     java.io.IOException
		 * @since   JDK1.0
		 */
		public void reset() throws IOException {
			in.reset();

			contentLength = mark;
		}

		/**
		 * Skips over and discards <code>n</code> bytes of data from this
		 * input stream. The <code>skip</code> method may, for a variety of
		 * reasons, end up skipping over some smaller number of bytes,
		 * possibly <code>0</code>. The actual number of bytes skipped is
		 * returned.
		 * <p>
		 * The <code>skip</code> method of <code>InputStream</code> creates
		 * a byte array of length <code>n</code> and then reads into it until
		 * <code>n</code> bytes have been read or the end of the stream has
		 * been reached. Subclasses are encouraged to provide a more
		 * efficient implementation of this method.
		 *
		 * @param      n   the number of bytes to be skipped.
		 * @return     the actual number of bytes skipped.
		 * @exception  IOException  if an I/O error occurs.
		 * @since      JDK1.0
		 */
		public long skip(long len) throws IOException {
			if (contentLength <= 0) {
				return 0;
			}

			if (contentLength < len) {
				len = contentLength;
			}

			long skipped = in.skip(len);

			contentLength -= skipped;

			return skipped;
		}
	}
}
