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
import java.util.*;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.equinox.http.*;
import org.eclipse.equinox.socket.SocketInterface;

/**
 * The implementation of the HttpServletResponse interface.
 *
 * Per Servlet 2.2 Section 3.3.3.3, this object need not be thread-safe.
 **/

public class HttpServletResponseImpl implements HttpServletResponse {
	private Http http;
	protected HttpServletRequestImpl request;
	private SocketInterface socket;
	protected ServletOutputStreamImpl servletOutputStream;
	protected String charset = null;
	protected static final String defaultCharset = "ISO-8859-1"; //$NON-NLS-1$

	protected int contentLength = 0;
	protected String contentType = null;
	protected Vector cookies = null;
	protected Hashtable headers = null;
	protected int status = SC_OK;
	protected String statusString = "OK"; //$NON-NLS-1$

	private boolean gotOutputStream = false;
	protected PrintWriter writer = null;

	private boolean keepAlive = false;

	public HttpServletResponseImpl(SocketInterface socket, Http http) throws IOException {
		this.http = http;
		this.socket = socket;
		servletOutputStream = new ServletOutputStreamImpl(socket.getOutputStream(), this);
	}

	/**
	 * Called by the request in its constructor.
	 */
	protected void setRequest(HttpServletRequestImpl request) {
		this.request = request;
	}

	/**
	 * Adds the specified cookie to the response.  It can be called
	 * multiple times to set more than one cookie.
	 *
	 * @param cookie the Cookie to return to the client
	 */
	public void addCookie(Cookie cookie) {
		if (cookies == null) {
			synchronized (this) {
				if (cookies == null) {
					cookies = new Vector(20);
				}
			}
		}

		synchronized (cookies) {
			if (!cookies.contains(cookie)) {
				//System.out.println("addCookie: "+cookie.getName()+"="+cookie.getValue());
				cookies.addElement(cookie);
			}
		}

		//http.logDebug("ServletResponse::addCookie:  added cookie "+cookie.getName());
	}

	/**
	 * Checks whether the response message header has a field with
	 * the specified name.
	 *
	 * @param name the header field name
	 * @return true if the response message header has a field with
	 * the specified name; false otherwise
	 */
	public boolean containsHeader(String name) {
		if (headers == null) {
			return (false);
		}

		Object value = headers.get(name);

		if (value != null) {
			return (true);
		}

		Enumeration headerEnumeration = headers.keys();

		while (headerEnumeration.hasMoreElements()) {
			String key = (String) headerEnumeration.nextElement();

			if (name.equalsIgnoreCase(key)) {
				return (true);
			}
		}

		return (false);
	}

	/**
	 * Encodes the specified URL for use in the
	 * <code>sendRedirect</code> method or, if encoding is not needed,
	 * returns the URL unchanged.  The implementation of this method
	 * should include the logic to determine whether the session ID
	 * needs to be encoded in the URL.  Because the rules for making
	 * this determination differ from those used to decide whether to
	 * encode a normal link, this method is seperate from the
	 * <code>encodeUrl</code> method.
	 *
	 * <p>All URLs sent to the HttpServletResponse.sendRedirect
	 * method should be run through this method.  Otherwise, URL
	 * rewriting cannot be used with browsers which do not support
	 * cookies.
	 *
	 * @param url the url to be encoded.
	 * @return the encoded URL if encoding is needed; the unchanged URL
	 * otherwise.
	 * deprecated
	 *
	 * @see #sendRedirect
	 * @see #encodeUrl
	 */

	public String encodeRedirectUrl(String url) {
		return (encodeRedirectURL(url));
	}

	/**
	 * Encodes the specified URL for use in the
	 * <code>sendRedirect</code> method or, if encoding is not needed,
	 * returns the URL unchanged.  The implementation of this method
	 * should include the logic to determine whether the session ID
	 * needs to be encoded in the URL.  Because the rules for making
	 * this determination differ from those used to decide whether to
	 * encode a normal link, this method is seperate from the
	 * <code>encodeUrl</code> method.
	 *
	 * <p>All URLs sent to the HttpServletResponse.sendRedirect
	 * method should be run through this method.  Otherwise, URL
	 * rewriting canont be used with browsers which do not support
	 * cookies.
	 *
	 * <p>After this method is called, the response should be considered
	 * to be committed and should not be written to.
	 *
	 * @param url the url to be encoded.
	 * @return the encoded URL if encoding is needed; the unchanged URL
	 * otherwise.
	 *
	 * @see #sendRedirect
	 * @see #encodeUrl
	 */

	public String encodeRedirectURL(String url) {
		return (url);
	}

	/**
	 * Encodes the specified URL by including the session ID in it,
	 * or, if encoding is not needed, returns the URL unchanged.
	 * The implementation of this method should include the logic to
	 * determine whether the session ID needs to be encoded in the URL.
	 * For example, if the browser supports cookies, or session
	 * tracking is turned off, URL encoding is unnecessary.
	 *
	 * <p>All URLs emitted by a Servlet should be run through this
	 * method.  Otherwise, URL rewriting cannot be used with browsers
	 * which do not support cookies.
	 *
	 * @param url the url to be encoded.
	 * @return the encoded URL if encoding is needed; the unchanged URL
	 * otherwise.
	 * deprecated
	 */
	public String encodeUrl(String url) {
		return (encodeURL(url));
	}

	/**
	 * Encodes the specified URL by including the session ID in it,
	 * or, if encoding is not needed, returns the URL unchanged.
	 * The implementation of this method should include the logic to
	 * determine whether the session ID needs to be encoded in the URL.
	 * For example, if the browser supports cookies, or session
	 * tracking is turned off, URL encoding is unnecessary.
	 *
	 * <p>All URLs emitted by a Servlet should be run through this
	 * method.  Otherwise, URL rewriting cannot be used with browsers
	 * which do not support cookies.
	 *
	 * @param url the url to be encoded.
	 * @return the encoded URL if encoding is needed; the unchanged URL
	 * otherwise.
	 */

	public String encodeURL(String url) {
		return (url);
	}

	/**
	 * Close the response output.
	 *
	 */
	public void close() {
		if (writer != null) {
			writer.close();
		} else {
			try {
				servletOutputStream.close();
			} catch (IOException e) {
			}
		}

		if (!keepAlive) /* if the no Keep-Alive, then close socket */
		{
			try {
				socket.close();
			} catch (IOException e) {
			}
		}
	}

	/**
	 * Returns the character set encoding used for this MIME body.
	 * The character encoding is either the one specified in the
	 * assigned content type, or one which the client understands
	 * as specified in the Accept-Charset header of the request.
	 * If no charset can be determined, it defaults to the
	 * value of the System property 'file.encoding' if that
	 * is a supported character set. If it is not supported, then
	 * we use <tt>ISO-8859-1</tt> as the character set.
	 */
	public String getCharacterEncoding() {
		if (charset == null) {
			OutputStream dummy = new ByteArrayOutputStream();
			// BUGBUG Per the spec, the default charset is ISO8859_1
			// Servlet 2.3 Section 5.4

			/* Get the default file encoding charset */
			String fileEncoding = (new SecureAction()).getProperty("file.encoding", defaultCharset); //$NON-NLS-1$
			if ((fileEncoding == null) || (fileEncoding.length() < 1)) {
				fileEncoding = defaultCharset;
			}
			String fileEncodingAlias = null;

			/* We need to look at Accept-Charset from the request to
			 * select the encoding charset.
			 */
			String accept = request.getHeaderUpper("ACCEPT-CHARSET"); //$NON-NLS-1$

			if (accept != null) {
				/*
				 * We prefer the file.encoding charset if specified
				 * in the Accept-Charset and supported by the JRE. Otherwise,
				 * we simply select the first value supported by the
				 * JRE. We do not support the q value in Accept-Charset.
				 */

				Tokenizer tokenizer = new Tokenizer(accept);

				parseloop: while (true) {
					String acceptCharset = tokenizer.getToken(",;"); //$NON-NLS-1$

					if (!acceptCharset.equals("*")) //$NON-NLS-1$
					{
						try /* is charset valid? */
						{
							String encodingAlias = new OutputStreamWriter(dummy, acceptCharset).getEncoding();

							if (charset == null) /* charset has not been set */
							{
								charset = acceptCharset;
							} else {
								if (fileEncodingAlias == null) {
									try {
										fileEncodingAlias = new OutputStreamWriter(dummy, fileEncoding).getEncoding();
									} catch (UnsupportedEncodingException ee) {
									}
								}

								if (encodingAlias.equals(fileEncodingAlias)) {
									charset = acceptCharset; /* prefer the file.encoding charset */

									break parseloop;
								}
							}
						} catch (UnsupportedEncodingException e) {
							/* charset is not supported */
						}
					}

					char c = tokenizer.getChar();

					if (c == ';') {
						tokenizer.getToken(","); /* ignore q value *///$NON-NLS-1$
						c = tokenizer.getChar();
					}

					if (c == '\0') {
						break parseloop;
					}
				}
			}

			if (charset == null) /* if Accept-Charset did not produce a valid charset */
			{
				charset = fileEncoding;

				try /* is charset valid? */
				{
					new OutputStreamWriter(dummy, charset);
				} catch (UnsupportedEncodingException e) {
					/* charset is not supported */
					charset = defaultCharset;
				}
			}
		}

		return (charset);
	}

	/**
	 * Returns an output stream for writing binary response data.
	 *
	 * @see getWriter
	 * @exception IllegalStateException if getWriter has been
	 *	called on this same request.
	 * @exception IOException if an I/O exception has occurred
	 */
	public ServletOutputStream getOutputStream() {
		if (!gotOutputStream) {
			synchronized (this) {
				if (writer != null) {
					throw new IllegalStateException();
				}
				gotOutputStream = true;
			}
		}

		return (servletOutputStream);
	}

	/**
	 * Returns a print writer for writing formatted text responses.  The
	 * MIME type of the response will be modified, if necessary, to reflect
	 * the character encoding used, through the <em>charset=...</em>
	 * property.  This means that the content type must be set before
	 * calling this method.
	 *
	 * @see getOutputStream
	 * @see setContentType
	 *
	 * @exception UnsupportedEncodingException if no such charset can
	 * be provided
	 * @exception IllegalStateException if getOutputStream has been
	 *	called on this same request.
	 * @exception IOException on other errors.
	 */
	public PrintWriter getWriter() throws IOException {
		if (writer == null) {
			synchronized (this) {
				if (writer == null) {
					if (gotOutputStream) {
						throw new IllegalStateException();
					}

					String charset = getCharacterEncoding();

					writer = new ServletPrintWriter(servletOutputStream, charset);

					if (contentType == null) {
						// BUGBUG Must not set a default content type.
						// Servlet 2.3 Section 5.3
						contentType = "text/plain; charset=" + charset; //$NON-NLS-1$

						setHeader("Content-Type", contentType); //$NON-NLS-1$
					} else {
						if (contentType.toLowerCase().indexOf("charset=") == -1) // 99372 //$NON-NLS-1$
						{
							contentType = contentType + "; charset=" + charset; //$NON-NLS-1$

							setHeader("Content-Type", contentType); //$NON-NLS-1$
						}
					}
				}
			}
		}

		return (writer);
	}

	boolean gotStreamOrWriter() {
		return (writer != null || gotOutputStream);
	}

	/**
	 * Sends an error response to the client using the specified
	 * status code and a default message.
	 * @param sc the status code
	 * @exception IOException If an I/O error has occurred.
	 */
	public void sendError(int statusCode) throws IOException {
		// BUGBUG Must clear buffer if response has not yet been committed
		// or throw an IllegalStateException.
		sendError(statusCode, http.getStatusPhrase(statusCode));
	}

	/**
	 * Sends an error response to the client using the specified status
	 * code and descriptive message.  If setStatus has previously been
	 * called, it is reset to the error status code.  The message is
	 * sent as the body of an HTML page, which is returned to the user
	 * to describe the problem.  The page is sent with a default HTML
	 * header; the message is enclosed in simple body tags
	 * (&lt;body&gt;&lt;/body&gt;).
	 *
	 * @param sc the status code
	 * @param msg the detail message
	 * @exception IOException If an I/O error has occurred.
	 */
	public void sendError(int sc, String msg) throws IOException {
		// BUGBUG Must clear buffer if response has not yet been committed
		// or throw an IllegalStateException.
		status = sc;
		statusString = msg;

		if (Http.DEBUG) {
			http.logDebug("Error: " + sc + " - " + msg); //$NON-NLS-1$ //$NON-NLS-2$
		}

		servletOutputStream.print("<html><body><h3>"); //$NON-NLS-1$
		servletOutputStream.print(sc);
		servletOutputStream.print(" - "); //$NON-NLS-1$
		servletOutputStream.print(msg);
		servletOutputStream.print("</h3></body></html>"); //$NON-NLS-1$
		servletOutputStream.close(); /* commit and close the response */

	}

	/**
	 * Sends a temporary redirect response to the client using the
	 * specified redirect location URL.  The URL must be absolute (for
	 * example, <code><em>https://hostname/path/file.html</em></code>).
	 * Relative URLs are not permitted here.
	 *
	 * @param location the redirect location URL
	 * @exception IOException If an I/O error has occurred.
	 */
	public void sendRedirect(String location) throws IOException {
		// BUGBUG The input URL may be relative. We must translate to
		// a fully qualified URL (it should already have been encoded).
		// If this cannot be done, throw an IllegalArgument exception.
		// BUGBUG Must clear buffer if response has not yet been committed
		// or throw an IllegalStateException.
		setStatus(SC_MOVED_TEMPORARILY);
		setHeader("Location", location); //$NON-NLS-1$
		servletOutputStream.close(); /* commit and close the response */
	}

	/**
	 * Sets the content length for this response.
	 *
	 * @param len the content length
	 */

	public void setContentLength(int len) {
		// BUGBUG response should be considered commited and closed
		// when content length has been written.
		// Not sure if content length is bytes or chars?
		contentLength = len;
		setIntHeader("Content-Length", len); //$NON-NLS-1$
	}

	/**
	 * Sets the content type for this response.  This type may later
	 * be implicitly modified by addition of properties such as the MIME
	 * <em>charset=&lt;value&gt;</em> if the service finds it necessary,
	 * and the appropriate media type property has not been set.
	 *
	 * <p>This response property may only be assigned one time.  If a
	 * writer is to be used to write a text response, this method must
	 * be called before the method <code>getWriter</code>.  If an
	 * output stream will be used to write a response, this method must
	 * be called before the output stream is used to write response
	 * data.
	 *
	 * @param type the content's MIME type
	 * @see getOutputStream
	 * @see getWriter */
	public void setContentType(String type) {
		if (contentType == null) {
			synchronized (this) {
				if (contentType == null) {
					contentType = type;

					setHeader("Content-Type", type); //$NON-NLS-1$

					int index = type.indexOf(';', 0);
					if (index >= 0) {
						Tokenizer tokenizer = new Tokenizer(type);

						String mime_type = tokenizer.getToken(";"); //$NON-NLS-1$
						tokenizer.getChar(); /* eat semicolon */

						parseloop: while (true) {
							String attribute = tokenizer.getToken("="); //$NON-NLS-1$
							char c = tokenizer.getChar();

							if (c != '=') {
								break parseloop; /* invalid content type */
							}

							String value = tokenizer.getString(";"); //$NON-NLS-1$
							c = tokenizer.getChar();

							if ("charset".equalsIgnoreCase(attribute)) //$NON-NLS-1$
							{
								charset = value;
							}

							if (c == '\0') {
								break parseloop;
							}
						}
					}
				}
			}
		}
	}

	/**
	 * This method was created in VisualAge.
	 */
	protected void setCookies() {
		if (cookies == null) {
			return;
		}

		int numCookies = cookies.size();

		if (numCookies > 0) {
			StringBuffer value = new StringBuffer(256);

			for (int i = 0; i < numCookies; i++) {
				Cookie cookie = (Cookie) cookies.elementAt(i);

				if (i > 0) {
					value.append(',');
				}

				value.append(cookie.getName());
				value.append("=\""); //$NON-NLS-1$
				value.append(cookie.getValue());
				value.append('\"');
				if ((cookie.getVersion()) != 0) {
					value.append(";Version="); //$NON-NLS-1$
					value.append(cookie.getVersion());
				}
				if ((cookie.getComment()) != null) {
					value.append(";Comment=\""); //$NON-NLS-1$
					value.append(cookie.getComment());
					value.append('\"');
				}
				if ((cookie.getDomain()) != null) {
					value.append(";Domain=\""); //$NON-NLS-1$
					value.append(cookie.getDomain());
					value.append('\"');
				}
				if ((cookie.getMaxAge()) != -1) {
					value.append(";Max-Age=\""); //$NON-NLS-1$
					value.append(cookie.getMaxAge());
					value.append('\"');
				}
				if ((cookie.getPath()) != null) {
					value.append(";Path=\""); //$NON-NLS-1$
					value.append(cookie.getPath());
					value.append('\"');
				}
				if (cookie.getSecure()) {
					value.append(";Secure"); //$NON-NLS-1$
				}
			}

			setHeader("Set-Cookie", value.toString()); //$NON-NLS-1$
			//System.out.println("Set-Cookie: "+value.toString());
		}
	}

	/**
	 *
	 * Adds a field to the response header with the given name and
	 * date-valued field.  The date is specified in terms of
	 * milliseconds since the epoch.  If the date field had already
	 * been set, the new value overwrites the previous one.  The
	 * <code>containsHeader</code> method can be used to test for the
	 * presence of a header before setting its value.
	 *
	 * @param name the name of the header field
	 * @param value the header field's date value
	 *
	 * @see #containsHeader
	 */
	public void setDateHeader(String name, long date) {
		HttpDate d = new HttpDate(date);
		setHeader(name, d.toString());
	}

	/**
	 *
	 * Adds a field to the response header with the given name and value.
	 * If the field had already been set, the new value overwrites the
	 * previous one.  The <code>containsHeader</code> method can be
	 * used to test for the presence of a header before setting its
	 * value.
	 *
	 * @param name the name of the header field
	 * @param value the header field's value
	 *
	 * @see #containsHeader
	 */
	public void setHeader(String name, String value) {
		// BUGBUG Headers set after the response is committed must be ignored
		// Servlet 2.2 Section 6.2
		if (headers == null) {
			synchronized (this) {
				if (headers == null) {
					headers = new Hashtable(31);
				}
			}
		}
		headers.put(name, value);
	}

	/**
	 * Adds a field to the response header with the given name and
	 * integer value.  If the field had already been set, the new value
	 * overwrites the previous one.  The <code>containsHeader</code>
	 * method can be used to test for the presence of a header before
	 * setting its value.
	 *
	 * @param name the name of the header field
	 * @param value the header field's integer value
	 *
	 * @see #containsHeader
	 */
	public void setIntHeader(String name, int value) {
		setHeader(name, String.valueOf(value));
	}

	/**
	 * Sets the status code for this response.  This method is used to
	 * set the return status code when there is no error (for example,
	 * for the status codes SC_OK or SC_MOVED_TEMPORARILY).  If there
	 * is an error, the <code>sendError</code> method should be used
	 * instead.
	 *
	 * @param sc the status code
	 *
	 * @see #sendError
	 */
	public void setStatus(int statusCode) {
		status = statusCode;
		statusString = http.getStatusPhrase(statusCode);
	}

	/**
	 * Sets the status code and message for this response.  If the
	 * field had already been set, the new value overwrites the
	 * previous one.  The message is sent as the body of an HTML
	 * page, which is returned to the user to describe the problem.
	 * The page is sent with a default HTML header; the message
	 * is enclosed in simple body tags (&lt;body&gt;&lt;/body&gt;).
	 *
	 * @param sc the status code
	 * @param sm the status message
	 * deprecated
	 */
	public void setStatus(int si, String ss) {
		status = si;
		statusString = ss;

	}

	/**
	 * Write the response headers to the ServletOutputStream.
	 *
	 * @param length Content length of the of the buffered content
	 * or -1 if the length is unknown.
	 */
	void writeHeaders(int length) throws IOException {
		setCookies();

		if ((length != -1) && !containsHeader("Content-Length")) //$NON-NLS-1$
		{
			setContentLength(length);
		}

		if (containsHeader("Content-Length")) //$NON-NLS-1$
		{
			String requestConnection = request.getHeaderUpper("CONNECTION"); //$NON-NLS-1$

			if (requestConnection != null) {
				if (requestConnection.toLowerCase().indexOf("keep-alive") >= 0) //$NON-NLS-1$
				{
					setHeader("Connection", "Keep-Alive"); //$NON-NLS-1$ //$NON-NLS-2$

					keepAlive = true;
				}
			}
		}

		if (!keepAlive) {
			setHeader("Connection", "close"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		servletOutputStream.print("HTTP/1.0 "); //$NON-NLS-1$
		servletOutputStream.print(status);
		servletOutputStream.print(" "); //$NON-NLS-1$
		servletOutputStream.println(statusString);

		//      System.out.print("HTTP/1.0 ");
		//      System.out.print(status);
		//      System.out.print(" ");
		//      System.out.println(statusString);

		/* Write response headers */
		if (headers != null) {
			Enumeration headerEnumeration = headers.keys();
			while (headerEnumeration.hasMoreElements()) {
				String name = (String) headerEnumeration.nextElement();
				String value = (String) headers.get(name);
				servletOutputStream.print(name);
				servletOutputStream.print(": "); //$NON-NLS-1$
				servletOutputStream.println(value);

				//              System.out.print(name);
				//              System.out.print(": ");
				//              System.out.println(value);
			}
		}

		servletOutputStream.println(); /* Terminate the headers */

		//      System.out.println();
	}

	/**
	 * @see javax.servlet.http.HttpServletResponse#addDateHeader(String, long)
	 */
	public void addDateHeader(String name, long date) throws UnsupportedOperationException {
		throw new UnsupportedOperationException(HttpMsg.HTTP_ONLY_SUPPORTS_2_1);
	}

	/**
	 * @see javax.servlet.http.HttpServletResponse#addHeader(String, String)
	 */
	public void addHeader(String name, String value) throws UnsupportedOperationException {
		throw new UnsupportedOperationException(HttpMsg.HTTP_ONLY_SUPPORTS_2_1);
	}

	/**
	 * @see javax.servlet.http.HttpServletResponse#addIntHeader(String, int)
	 */
	public void addIntHeader(String name, int value) throws UnsupportedOperationException {
		throw new UnsupportedOperationException(HttpMsg.HTTP_ONLY_SUPPORTS_2_1);
	}

	/**
	 * @see javax.servlet.ServletResponse#flushBuffer()
	 */
	public void flushBuffer() throws IOException, UnsupportedOperationException {
		throw new UnsupportedOperationException(HttpMsg.HTTP_ONLY_SUPPORTS_2_1);
	}

	/**
	 * @see javax.servlet.ServletResponse#getBufferSize()
	 */
	public int getBufferSize() throws UnsupportedOperationException {
		throw new UnsupportedOperationException(HttpMsg.HTTP_ONLY_SUPPORTS_2_1);
	}

	/**
	 * @see javax.servlet.ServletResponse#getLocale()
	 */
	public Locale getLocale() throws UnsupportedOperationException {
		throw new UnsupportedOperationException(HttpMsg.HTTP_ONLY_SUPPORTS_2_1);
	}

	/**
	 * @see javax.servlet.ServletResponse#isCommitted()
	 */
	public boolean isCommitted() throws UnsupportedOperationException {
		throw new UnsupportedOperationException(HttpMsg.HTTP_ONLY_SUPPORTS_2_1);
	}

	/**
	 * @see javax.servlet.ServletResponse#reset()
	 */
	public void reset() throws UnsupportedOperationException {
		throw new UnsupportedOperationException(HttpMsg.HTTP_ONLY_SUPPORTS_2_1);
	}

	/**
	 * @see javax.servlet.ServletResponse#resetBuffer()
	 */
	public void resetBuffer() throws UnsupportedOperationException {
		throw new UnsupportedOperationException(HttpMsg.HTTP_ONLY_SUPPORTS_2_1);
	}

	/**
	 * @see javax.servlet.ServletResponse#setBufferSize(int)
	 */
	public void setBufferSize(int size) throws UnsupportedOperationException {
		throw new UnsupportedOperationException(HttpMsg.HTTP_ONLY_SUPPORTS_2_1);
	}

	/**
	 * @see javax.servlet.ServletResponse#setLocale(Locale)
	 */
	public void setLocale(Locale loc) throws UnsupportedOperationException {
		throw new UnsupportedOperationException(HttpMsg.HTTP_ONLY_SUPPORTS_2_1);
	}

	/* JavaServlet 2.4 API - For compilation only.  Will not implement.
	 * 
	 */
	public String getContentType() {
		//return contentType;
		throw new UnsupportedOperationException(HttpMsg.HTTP_ONLY_SUPPORTS_2_1);
	}

	/* For compilation only.  Will not implement.
	 * 
	 */
	public void setCharacterEncoding(String arg0) {
		//this.contentType = contentType;
		throw new UnsupportedOperationException(HttpMsg.HTTP_ONLY_SUPPORTS_2_1);

	}
}
