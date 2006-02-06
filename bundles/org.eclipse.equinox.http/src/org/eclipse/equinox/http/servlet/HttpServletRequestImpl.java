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
import java.security.Principal;
import java.util.*;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.*;

import org.eclipse.equinox.http.*;
import org.eclipse.equinox.socket.SocketInterface;
import org.eclipse.osgi.util.NLS;
import org.osgi.service.http.HttpContext;

/**
 * Implementation of the HttpServletRequest interface
 *
 * Per Servlet 2.2 Section 3.3.3.3, this object need not be thread-safe.
 */

public class HttpServletRequestImpl implements HttpServletRequest {

	protected Http http;
	protected HttpServletResponseImpl response;
	protected SocketInterface socket;
	protected ServletInputStreamImpl servletInputStream;
	protected String scheme;
	protected String authType = null;
	protected String remoteUser = null;

	protected int contentLength = -2;
	protected String contentType = null;
	protected String serverName = null;

	protected Hashtable parameters = null;
	protected Hashtable attributes = null;
	protected BufferedReader reader = null;
	protected ServletInputStream inputstream = null;
	protected Cookie[] cookies;

	//request-line variables
	protected String method = null;
	protected String reqURI = null; /* URI decoded */
	protected String protocol = null;
	protected String servletPath = null;
	protected String pathInfo = null; /* URI decoded */
	protected String queryString = null;
	protected String charset = null;

	protected Hashtable headers = null;
	protected HttpSessionImpl session;
	protected String requestedSessionId = null;
	protected ServletContextImpl servletContext;

	protected boolean parsedQueryData = false;

	public HttpServletRequestImpl(SocketInterface socket, Http http, HttpServletResponseImpl response) throws IOException {
		this.response = response;
		this.socket = socket;
		this.http = http;
		scheme = socket.getScheme();

		servletInputStream = new ServletInputStreamImpl(socket.getInputStream());

		response.setRequest(this);

		parseHeaders(); /* allocate headers Hashtable */
	}

	/**
	 * Initialize additional request data.
	 *
	 * @param servletPathParam URI alias for this request
	 * @param servletContextParam ServletContext for this request
	 */
	public void init(String servletPathParam, ServletContextImpl servletContextParam) {
		// BUGBUG Need to deal with context path
		// Servlet 2.2 Section 5.4
		this.servletPath = servletPathParam;
		this.servletContext = servletContextParam;

		String tempPathInfo = reqURI.substring(servletPathParam.length());
		if ((tempPathInfo.length() == 0) || tempPathInfo.equals("/")) //$NON-NLS-1$
		{
			/* leave as null */
		} else {
			this.pathInfo = tempPathInfo;
		}

		if (authType == null) {
			Object obj = getAttribute(HttpContext.AUTHENTICATION_TYPE);
			if (obj instanceof String) {
				authType = (String) obj;
			}
		}

		if (remoteUser == null) {
			Object obj = getAttribute(HttpContext.REMOTE_USER);
			if (obj instanceof String) {
				remoteUser = (String) obj;
			}
		}
	}

	/**
	 * Returns the value of the named attribute of the request, or
	 * null if the attribute does not exist.  This method allows
	 * access to request information not already provided by the other
	 * methods in this interface.  Attribute names should follow the
	 * same convention as package names.
	 * The following predefined attributes are provided.
	 *
	 * <TABLE BORDER>
	 * <tr>
	 *	<th>Attribute Name</th>
	 *	<th>Attribute Type</th>
	 *	<th>Description</th>
	 *	</tr>
	 *
	 * <tr>
	 *	<td VALIGN=TOP>javax.net.ssl.cipher_suite</td>
	 *	<td VALIGN=TOP>string</td>
	 *	<td>The string name of the SSL cipher suite in use, if the
	 *		request was made using SSL</td>
	 *	</tr>
	 *
	 * <tr>
	 *	<td VALIGN=TOP>javax.net.ssl.peer_certificates</td>
	 *	<td VALIGN=TOP>array of javax.security.cert.X509Certificate</td>
	 *	<td>The chain of X.509 certificates which authenticates the client.
	 *		This is only available when SSL is used with client
	 *		authentication is used.</td>
	 *	</tr>
	 *
	 * <tr>
	 *	<td VALIGN=TOP>javax.net.ssl.session</td>
	 *	<td VALIGN=TOP>javax.net.ssl.SSLSession</td>
	 *	<td>An SSL session object, if the request was made using SSL.</td>
	 *	</tr>
	 *
	 * </TABLE>
	 *
	 * <BR>
	 * <P>The package (and hence attribute) names beginning with java.*,
	 * and javax.* are reserved for use by Javasoft. Similarly, com.sun.*
	 * is reserved for use by Sun Microsystems.
	 *
	 * @param name the name of the attribute whose value is required
	 */
	public Object getAttribute(String name) {
		if (attributes != null) {
			return (attributes.get(name));
		}
		return (null);
	}

	/**
	 * Returns an enumeration of attribute names contained in this request.
	 */

	public Enumeration getAttributeNames() {
		if (attributes != null) {
			return (attributes.keys());
		}
		return (new Vector(0).elements());
	}

	/**
	 * Gets the authentication scheme of this request.  Same as the CGI
	 * variable AUTH_TYPE.
	 *
	 * @return this request's authentication scheme, or null if none.
	 */
	public String getAuthType() {
		return (authType);
	}

	/**
	 * Returns the character set encoding for the input of this request.
	 */
	public String getCharacterEncoding() {
		if (contentType == null) {
			getContentType(); /* parse the content type */
		}

		return (charset);
	}

	/**
	 * Returns the size of the request entity data, or -1 if not known.
	 * Same as the CGI variable CONTENT_LENGTH.
	 */
	public int getContentLength() {
		if (contentLength == -2) {
			contentLength = getIntHeaderUpper("CONTENT-LENGTH"); //$NON-NLS-1$
		}
		return (contentLength);
	}

	/**
	 * Returns the Internet Media Type of the request entity data, or
	 * null if not known. Same as the CGI variable CONTENT_TYPE.
	 */

	public String getContentType() {
		if (contentType == null) {
			contentType = getHeaderUpper("CONTENT-TYPE"); //$NON-NLS-1$

			if (contentType != null) {
				int index = contentType.indexOf(';', 0);
				if (index >= 0) {
					Tokenizer tokenizer = new Tokenizer(contentType);

					// TODO: verify next statement. It was String mimetype = tokenizer.getToken(";"); 
					tokenizer.getToken(";"); //$NON-NLS-1$
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

		return (contentType);
	}

	/**
	 * Gets an array of cookies found in this request.  If no cookies are present, an empty
	 * array was returned.
	 * @return the array of cookies found in this request
	 */
	public Cookie[] getCookies() {
		parseCookies();

		return ((Cookie[]) cookies.clone());
	}

	/**
	 * Gets the value of the requested date header field of this
	 * request.  If the header can't be converted to a date, the method
	 * throws an IllegalArgumentException.  The case of the header
	 * field name is ignored.
	 *
	 * From HTTP/1.1 RFC 2616
	 * 3.3.1 Full Date
	 *
	 *    HTTP applications have historically allowed three different formats
	 *    for the representation of date/time stamps:
	 *
	 *       Sun, 06 Nov 1994 08:49:37 GMT  ; RFC 822, updated by RFC 1123
	 *       Sunday, 06-Nov-94 08:49:37 GMT ; RFC 850, obsoleted by RFC 1036
	 *       Sun Nov  6 08:49:37 1994       ; ANSI C's asctime() format
	 *
	 *    The first format is preferred as an Internet standard and represents
	 *    a fixed-length subset of that defined by RFC 1123 [8] (an update to
	 *    RFC 822 [9]). The second format is in common use, but is based on the
	 *    obsolete RFC 850 [12] date format and lacks a four-digit year.
	 *    HTTP/1.1 clients and servers that parse the date value MUST accept
	 *    all three formats (for compatibility with HTTP/1.0), though they MUST
	 *    only generate the RFC 1123 format for representing HTTP-date values
	 *    in header fields. See section 19.3 for further information.
	 *
	 *       Note: Recipients of date values are encouraged to be robust in
	 *       accepting date values that may have been sent by non-HTTP
	 *       applications, as is sometimes the case when retrieving or posting
	 *       messages via proxies/gateways to SMTP or NNTP.
	 *
	 *    All HTTP date/time stamps MUST be represented in Greenwich Mean Time
	 *    (GMT), without exception. For the purposes of HTTP, GMT is exactly
	 *    equal to UTC (Coordinated Universal Time). This is indicated in the
	 *    first two formats by the inclusion of "GMT" as the three-letter
	 *    abbreviation for time zone, and MUST be assumed when reading the
	 *    asctime format. HTTP-date is case sensitive and MUST NOT include
	 *    additional LWS beyond that specifically included as SP in the
	 *    grammar.
	 *
	 *        HTTP-date    = rfc1123-date | rfc850-date | asctime-date
	 *        rfc1123-date = wkday "," SP date1 SP time SP "GMT"
	 *        rfc850-date  = weekday "," SP date2 SP time SP "GMT"
	 *        asctime-date = wkday SP date3 SP time SP 4DIGIT
	 *        date1        = 2DIGIT SP month SP 4DIGIT
	 *                       ; day month year (e.g., 02 Jun 1982)
	 *        date2        = 2DIGIT "-" month "-" 2DIGIT
	 *                       ; day-month-year (e.g., 02-Jun-82)
	 *        date3        = month SP ( 2DIGIT | ( SP 1DIGIT ))
	 *                       ; month day (e.g., Jun  2)
	 *        time         = 2DIGIT ":" 2DIGIT ":" 2DIGIT
	 *                       ; 00:00:00 - 23:59:59
	 *        wkday        = "Mon" | "Tue" | "Wed"
	 *                     | "Thu" | "Fri" | "Sat" | "Sun"
	 *        weekday      = "Monday" | "Tuesday" | "Wednesday"
	 *                     | "Thursday" | "Friday" | "Saturday" | "Sunday"
	 *        month        = "Jan" | "Feb" | "Mar" | "Apr"
	 *                     | "May" | "Jun" | "Jul" | "Aug"
	 *                     | "Sep" | "Oct" | "Nov" | "Dec"
	 *
	 *       Note: HTTP requirements for the date/time stamp format apply only
	 *       to their usage within the protocol stream. Clients and servers are
	 *       not required to use these formats for user presentation, request
	 *       logging, etc.
	 *
	 * @param name the String containing the name of the requested
	 * header field
	 * @return the value the requested date header field, or -1 if not
	 * found.
	 */
	public long getDateHeader(String name) {
		//headers are stored as strings and must be converted
		String date = getHeader(name);

		if (date != null) {
			HttpDate d = new HttpDate(date);

			if (d.isValid()) {
				return (d.getAsLong()); // Parsed OK, so get the value as a Long
			}
			throw new IllegalArgumentException();
		}

		return (-1);
	}

	/**
	 * Gets the value of the requested header field of this request.
	 * The case of the header field name is ignored.
	 *
	 * @param name the String containing the name of the requested
	 * header field
	 * @return the value of the requested header field, or null if not
	 * known.
	 */

	//This should be case insensitive
	public String getHeader(String name) {
		return ((String) headers.get(name.toUpperCase()));
	}

	/**
	 * Gets the header names for this request.
	 *
	 * @return an enumeration of strings representing the header names
	 * for this request. Some server implementations do not allow
	 * headers to be accessed in this way, in which case this method
	 * will return null.
	 */
	public Enumeration getHeaderNames() {
		return (headers.keys());
	}

	protected String getHeaderUpper(String name) {
		return ((String) headers.get(name));
	}

	/**
	 * Returns an input stream for reading binary data in the request body.
	 *
	 * @see getReader
	 * @exception IllegalStateException if getReader has been
	 *	called on this same request.
	 * @exception IOException on other I/O related errors.
	 */
	public ServletInputStream getInputStream() {
		if (inputstream == null) {
			synchronized (this) {
				if (inputstream == null) {
					if (reader != null) {
						throw new IllegalStateException();
					}

					inputstream = servletInputStream.getServletInputStream(getContentLength());
				}
			}
		}

		return (inputstream);
	}

	/**
	 * Gets the value of the specified integer header field of this
	 * request.  The case of the header field name is ignored.  If the
	 * header can't be converted to an integer, the method throws a
	 * NumberFormatException.
	 *
	 * @param name the String containing the name of the requested
	 * header field
	 * @return the value of the requested header field, or -1 if not
	 * found.
	 */

	//This lookup is case insensitive
	public int getIntHeader(String name) {
		String value = getHeader(name);

		if (value != null) {
			return (Integer.parseInt(value));
		}

		return (-1);
	}

	protected int getIntHeaderUpper(String name) {
		String value = getHeaderUpper(name);

		if (value != null) {
			return (Integer.parseInt(value));
		}

		return (-1);
	}

	/**
	 * Gets the HTTP method (for example, GET, POST, PUT) with which
	 * this request was made. Same as the CGI variable REQUEST_METHOD.
	 *
	 * @return the HTTP method with which this request was made
	 */
	public String getMethod() {
		return (method);
	}

	/**
	 * Returns a string containing the lone value of the specified
	 * parameter, or null if the parameter does not exist. For example,
	 * in an HTTP servlet this method would return the value of the
	 * specified query string parameter. Servlet writers should use
	 * this method only when they are sure that there is only one value
	 * for the parameter.  If the parameter has (or could have)
	 * multiple values, servlet writers should use
	 * getParameterValues. If a multiple valued parameter name is
	 * passed as an argument, the return value is implementation
	 * dependent.
	 *
	 * @see #getParameterValues
	 *
	 * @param name the name of the parameter whose value is required.
	 */
	public String getParameter(String name) {
		String[] values = getParameterValues(name);

		if ((values != null) && (values.length > 0)) {
			return (values[0]);
		}

		return (null);
	}

	/**
	 * Returns the parameter names for this request as an enumeration
	 * of strings, or an empty enumeration if there are no parameters
	 * or the input stream is empty.  The input stream would be empty
	 * if all the data had been read from the stream returned by the
	 * method getInputStream.
	 */
	public Enumeration getParameterNames() {
		if (!parsedQueryData) {
			parseQueryData();
		}

		if (parameters != null) {
			return (parameters.keys());
		}

		return (new Vector(0).elements());
	}

	/**
	 * Returns the values of the specified parameter for the request as
	 * an array of strings, or null if the named parameter does not
	 * exist. For example, in an HTTP servlet this method would return
	 * the values of the specified query string or posted form as an
	 * array of strings.
	 *
	 * @param name the name of the parameter whose value is required.
	 * @see javax.servlet.ServletRequest#getParameter
	 */
	public String[] getParameterValues(String name) {
		if (!parsedQueryData) {
			parseQueryData();
		}

		if (parameters != null) {
			return ((String[]) parameters.get(name));
		}

		return (null);
	}

	/**
	 * Gets any optional extra path information following the servlet
	 * path of this request's URI, but immediately preceding its query
	 * string. Same as the CGI variable PATH_INFO.
	 *
	 * @return the optional path information following the servlet
	 * path, but before the query string, in this request's URI; null
	 * if this request's URI contains no extra path information
	 */
	public String getPathInfo() {
		return (pathInfo);
	}

	/**
	 * Gets any optional extra path information following the servlet
	 * path of this request's URI, but immediately preceding its query
	 * string, and translates it to a real path.  Similar to the CGI
	 * variable PATH_TRANSLATED
	 *
	 * @return extra path information translated to a real path or null
	 * if no extra path information is in the request's URI
	 */
	public String getPathTranslated() {
		// JSP 1.0 Section B.5
		return servletContext.getRealPath(getPathInfo());
	}

	/**
	 * Returns the protocol and version of the request as a string of
	 * the form <code>&lt;protocol&gt;/&lt;major version&gt;.&lt;minor
	 * version&gt</code>.  Same as the CGI variable SERVER_PROTOCOL.
	 */
	public String getProtocol() {
		return (protocol);
	}

	/**
	 * Gets any query string that is part of the HTTP request URI.
	 * Same as the CGI variable QUERY_STRING.
	 *
	 * @return query string that is part of this request's URI, or null
	 * if it contains no query string
	 */
	public String getQueryString() {
		return (queryString);
	}

	/**
	 * Returns a buffered reader for reading text in the request body.
	 * This translates character set encodings as appropriate.
	 *
	 * @see getInputStream
	 *
	 * @exception UnsupportedEncodingException if the character set encoding
	 *  is unsupported, so the text can't be correctly decoded.
	 * @exception IllegalStateException if getInputStream has been
	 *	called on this same request.
	 * @exception IOException on other I/O related errors.
	 */
	public BufferedReader getReader() {
		if (reader == null) {
			synchronized (this) {
				if (reader == null) {
					if (inputstream != null) {
						throw new IllegalStateException();
					}

					// BUGBUG Must create reader with charset getCharacterEncoding or iso-8859-1 if null.
					// Servlet 2.3 Section 4.9
					reader = new BufferedReader(new InputStreamReader(servletInputStream.getServletInputStream(getContentLength())));
				}
			}
		}

		return (reader);
	}

	/**
	 * Applies alias rules to the specified virtual path and returns
	 * the corresponding real path, or null if the translation can not
	 * be performed for any reason.  For example, an HTTP servlet would
	 * resolve the path using the virtual docroot, if virtual hosting
	 * is enabled, and with the default docroot otherwise.  Calling
	 * this method with the string "/" as an argument returns the
	 * document root.
	 *
	 * @param path the virtual path to be translated to a real path
	 * *deprecated
	 */
	public String getRealPath(String path) {
		return servletContext.getRealPath(path);
	}

	/**
	 * Returns the IP address of the agent that sent the request.
	 * Same as the CGI variable REMOTE_ADDR.
	 */
	public String getRemoteAddr() {
		return (socket.getInetAddress().getHostAddress());
	}

	/**
	 * Returns the fully qualified host name of the agent that sent the
	 * request. Same as the CGI variable REMOTE_HOST.
	 */
	public String getRemoteHost() {
		return (socket.getInetAddress().getHostName());
	}

	/**
	 * Gets the name of the user making this request.  The user name is
	 * set with HTTP authentication.  Whether the user name will
	 * continue to be sent with each subsequent communication is
	 * browser-dependent.  Same as the CGI variable REMOTE_USER.
	 *
	 * @return the name of the user making this request, or null if not
	 * known.
	 */
	public String getRemoteUser() {
		return (remoteUser);
	}

	/**
	 * Returns the session id specified with this request.  This may differ from
	 * the session id in the current session if the session id given by the
	 * client was invalid for whatever reason and a new session was created.
	 * This method will return null if the request does not have a session
	 * associated with it.
	 *
	 * @return the session id specified by this request, or null if the
	 * request did not specify a session id
	 *
	 * @see #isRequestedSessionIdValid */
	public String getRequestedSessionId() {
		parseCookies(); /* allocate cookies array */

		if (requestedSessionId == null) {
			String sessionCookieName = HttpSessionImpl.sessionCookieName;
			int numCookies = cookies.length;
			for (int i = 0; i < numCookies; i++) {
				Cookie cookie = cookies[i];
				if (sessionCookieName.equals(cookie.getName())) {
					requestedSessionId = cookie.getValue();
					break;
				}
			}
		}

		return (requestedSessionId);
	}

	/**
	 * Gets, from the first line of the HTTP request, the part of this
	 * request's URI that is to the left of any query string.
	 * For example,
	 *
	 * <blockquote>
	 * <table>
	 * <tr align=left><th>First line of HTTP request<th>
	 * <th>Return from <code>getRequestURI</code>
	 * <tr><td>POST /some/path.html HTTP/1.1<td><td>/some/path.html
	 * <tr><td>GET http://foo.bar/a.html HTTP/1.0
	 * <td><td>http://foo.bar/a.html
	 * <tr><td>HEAD /xyz?a=b HTTP/1.1<td><td>/xyz
	 * </table>
	 * </blockquote>
	 *
	 * <p>To reconstruct a URL with a URL scheme and host, use the
	 * method javax.servlet.http.HttpUtils.getRequestURL, which returns
	 * a StringBuffer.
	 *
	 * @return this request's URI
	 * @see javax.servlet.http.HttpUtils#getRequestURL
	 */
	public String getRequestURI() {
		// BUGBUG this should probably be URI encoded?
		// Servlet 2.2 Section 5.4
		return (reqURI);
	}

	/**
	 * Returns the scheme of the URL used in this request, for example
	 * "http", "https", or "ftp".  Different schemes have different
	 * rules for constructing URLs, as noted in RFC 1738.  The URL used
	 * to create a request may be reconstructed using this scheme, the
	 * server name and port, and additional information such as URIs.
	 */
	public String getScheme() {
		return (scheme);
	}

	/**
	 * Returns the host name of the server that received the request.
	 * Same as the CGI variable SERVER_NAME.
	 */
	public String getServerName() {

		if (serverName == null) {
			String value = getHeaderUpper("HOST"); //$NON-NLS-1$
			if (value != null) {
				int n = value.indexOf(':');
				if (n < 0) {
					serverName = value;
				} else {
					serverName = value.substring(0, n).trim();
				}
			} else {
				serverName = socket.getLocalAddress().getHostName();
			}
		}
		return serverName;
	}

	/**
	 * Returns the port number on which this request was received.
	 * Same as the CGI variable SERVER_PORT.
	 */
	public int getServerPort() {
		return (socket.getLocalPort());
	}

	/**
	 * Gets the part of this request's URI that refers to the servlet
	 * being invoked. Analogous to the CGI variable SCRIPT_NAME.
	 *
	 * @return the servlet being invoked, as contained in this
	 * request's URI
	 */
	public String getServletPath() {
		return (servletPath);
	}

	/**
	 * Returns the current valid session associated with this request.
	 * A session will be created for the
	 * request if there is not already a session associated with the request.
	 *
	 * To ensure the session is properly
	 * maintained, the servlet developer must call this method before the
	 * response is committed.
	 *
	 * @return the session associated with this request.
	 */
	public HttpSession getSession() {
		return (getSession(true));
	}

	/**
	 * Returns the current valid session associated with this request.
	 * If there is not already a session associated with the request,
	 * a session will be created for the request only
	 * if the argument is true.
	 *
	 * To ensure the session is properly
	 * maintained, the servlet developer must call this method before the
	 * response is committed.
	 *
	 * If the create flag is set to false and no session
	 * is associated with this request, then this method will return null.
	 *
	 * <p><b>Note</b>: to ensure the session is properly maintained,
	 * the servlet developer must call this method (at least once)
	 * before any output is written to the response.
	 *
	 * <p>Additionally, application-writers need to be aware that newly
	 * created sessions (that is, sessions for which
	 * <code>HttpSession.isNew</code> returns true) do not have any
	 * application-specific state.
	 *
	 * @return the session associated with this request or null if
	 * create was false and no valid session is associated
	 * with this request.
	 */
	public synchronized HttpSession getSession(boolean create) {
		if (session != null) /* if session cached in this request */
		{
			/* test to see if the session is still valid */
			if (session.isValid(false)) {
				return (session);
			}

			session = null; /* dereference invalid session */
		} else {
			/* Session is not cached in this request
			 * Check to see if the client requested a session id.
			 */

			String sessionId = getRequestedSessionId();
			if (sessionId != null) {
				session = http.getSession(sessionId);

				if (session != null) /* valid session in cache */
				{
					return (session);
				}
			}
		}

		// we didn't get a valid session, so create one if desired
		if (create) {
			session = new HttpSessionImpl(http);
			response.addCookie(session.getCookie());
			return (session);
		}

		// Nothing we did produced a valid session, and the caller
		// didn't ask us to create one.
		return (null);
	}

	/**
	 * Checks whether the session id specified by this request came in
	 * as a cookie.  (The requested session may not be one returned by
	 * the <code>getSession</code> method.)
	 *
	 * @return true if the session id specified by this request came in
	 * as a cookie; false otherwise
	 *
	 * @see #getSession
	 */
	public boolean isRequestedSessionIdFromCookie() {
		/* We always use cookies. If there is a requestedSessionId,
		 * it came from a Cookie.
		 */
		return (getRequestedSessionId() != null);
	}

	/**
	 * Checks whether the session id specified by this request came in
	 * as part of the URL.  (The requested session may not be the one
	 * returned by the <code>getSession</code> method.)
	 *
	 * @return true if the session id specified by the request for this
	 * session came in as part of the URL; false otherwise
	 *
	 * @see #getSession
	 *
	 * @deprecated use isRequestSessionIdFromURL() instead
	 */
	public boolean isRequestedSessionIdFromUrl() {
		return (isRequestedSessionIdFromURL());
	}

	/**
	 * Checks whether the session id specified by this request came in
	 * as part of the URL.  (The requested session may not be the one
	 * returned by the <code>getSession</code> method.)
	 *
	 * @return true if the session id specified by the request for this
	 * session came in as part of the URL; false otherwise
	 *
	 * @see #getSession
	 */

	public boolean isRequestedSessionIdFromURL() {
		/* We do not support URL rewriting. We use cookies. */
		return (false);
	}

	/**
	 * This method checks whether this request is associated with a session
	 * that is currently valid.  If the session used by the request is not valid,
	 * it will not be returned via the getSession method.
	 *
	 * @return true if the request session is valid.
	 *
	 * @see #getRequestedSessionId
	 * @see javax.servlet.http.HttpSessionContext
	 * @see #getSession
	 */
	public boolean isRequestedSessionIdValid() {
		HttpSession currentSession = getSession(false); /* get current session, if any */

		if (currentSession != null) /* if there is a session, see if it the requested session */
		{
			return (currentSession.getId().equals(getRequestedSessionId()));
		}

		return (false);
	}

	protected synchronized void parseCookies() {
		if (cookies == null) {
			nocookies: {
				String cookieHeader = getHeaderUpper("COOKIE"); //$NON-NLS-1$
				if (cookieHeader == null) {
					break nocookies;
				}
				Vector cookieVector = new Vector(20);
				int cookieVersion = 0;

				//parse through cookie header for all cookies

				Tokenizer tokenizer = new Tokenizer(cookieHeader);

				String name = tokenizer.getToken("="); //$NON-NLS-1$
				char c = tokenizer.getChar();
				String value;

				if (name.equals("$Version")) //$NON-NLS-1$
				{
					if (c != '=') {
						if (Http.DEBUG) {
							http.logDebug("Cookie parse error", new Exception()); //$NON-NLS-1$
						}
						break nocookies;
					}

					value = tokenizer.getString(";,"); //$NON-NLS-1$

					try {
						cookieVersion = Integer.parseInt(value);
					} catch (NumberFormatException e) {
						if (Http.DEBUG) {
							http.logDebug("Cookie version error", e); //$NON-NLS-1$
						}
					}

					name = null;
				}

				parseloop: while (true) {
					if (name == null) {
						name = tokenizer.getToken("="); //$NON-NLS-1$
						c = tokenizer.getChar();
					}

					if (c != '=') {
						if (Http.DEBUG) {
							http.logDebug("Cookie parse error", new Exception()); //$NON-NLS-1$
						}
						break nocookies;
					}

					value = tokenizer.getString(";,"); //$NON-NLS-1$
					c = tokenizer.getChar();

					Cookie cookie;
					try {
						cookie = new Cookie(name, value);
					} catch (IllegalArgumentException e) {
						if (Http.DEBUG) {
							http.logDebug("Cookie constructor error", e); //$NON-NLS-1$
						}
						break nocookies;
					}
					cookie.setVersion(cookieVersion);

					cookieVector.addElement(cookie);

					if (c == '\0') {
						break parseloop;
					}

					name = tokenizer.getToken("="); //$NON-NLS-1$
					c = tokenizer.getChar();
					if (name.equals("$Path")) //$NON-NLS-1$
					{
						if (c != '=') {
							if (Http.DEBUG) {
								http.logDebug("Cookie parse error", new Exception()); //$NON-NLS-1$
							}
							break nocookies;
						}
						cookie.setPath(tokenizer.getString(";,")); //$NON-NLS-1$

						c = tokenizer.getChar();
						if (c == '\0') {
							break parseloop;
						}

						name = tokenizer.getToken("="); //$NON-NLS-1$
						c = tokenizer.getChar();
					}

					if (name.equals("$Domain")) //$NON-NLS-1$
					{
						if (c != '=') {
							if (Http.DEBUG) {
								http.logDebug("Cookie parse error", new Exception()); //$NON-NLS-1$
							}
							break nocookies;
						}
						cookie.setDomain(tokenizer.getString(";,")); //$NON-NLS-1$

						c = tokenizer.getChar();
						if (c == '\0') {
							break parseloop;
						}

						name = null;
					}
				}

				if (cookieVector.size() > 0) {
					cookies = new Cookie[cookieVector.size()];
					cookieVector.copyInto(cookies);
					return;
				}
			}

			cookies = new Cookie[0];
		}
	}

	protected void parseHeaders() throws IOException {
		headers = new Hashtable(31);
		byte[] buffer = new byte[4096];

		/* The first line in an http request is always the request-line. */
		String line = readHeaderLine(buffer);

		if (line.length() == 0) {
			throw new InterruptedIOException(HttpMsg.HTTP_NO_HEADER_LINE_READ_EXCEPTION);
		}

		socket.markActive(); /* indicate we are processing a request */

		parseRequestLine(line);

		/* Now we get to the headers. */
		// BUGBUG Headers can be repeated! getHeader must return the first header
		// in the request. The (2.2) getHeaders method can be used to get all
		// the headers' values.
		// Servlet 2.2 Section 5.3
		boolean firstLine = true;
		String header = null;
		StringBuffer value = new StringBuffer(256);
		while (true) {
			line = readHeaderLine(buffer);

			if (line.length() == 0) { //End of headers
				if (!firstLine) /* flush last line */
				{
					headers.put(header, value.toString().trim());
				}
				break;
			}

			//          System.out.println(line);

			char c = line.charAt(0);
			if ((c == ' ') || (c == '\t')) /* continuation */
			{
				if (firstLine) /* if no previous line */
				{
					throw new IOException(NLS.bind(HttpMsg.HTTP_INVALID_HEADER_LINE_EXCEPTION, line));
				}
				value.append(line.substring(1));
				continue;
			}

			if (!firstLine) {
				headers.put(header, value.toString().trim());
				value.setLength(0); /* clear StringBuffer */
			}

			//use ':' as a delimeter to separate the key and the value
			int colon = line.indexOf(':', 0);

			// Our keys are saved as upper case so we can do case-insensitive
			// searches on them.
			header = line.substring(0, colon).toUpperCase();
			value.append(line.substring(colon + 1));
			firstLine = false;
		}//while
	}

	/**
	 * This methods MUST only be called by one of the getParameter methods
	 * Servlet 2.2 Section 5.1
	 */
	protected synchronized void parseQueryData() {
		if (!parsedQueryData) {
			try {
				/* Request parameters must come from BOTH the query string
				 * and the POST data. Query string must be processed before POST data.
				 * Servlet 2.2 Section 5.1.
				 */

				if (queryString != null) {
					if (parameters == null) {
						parameters = new Hashtable();
					}

					parseQueryString(parameters, queryString, null);
				}

				/* POST data must only be read if the following conditions are
				 * true
				 * 1. getScheme is "http" or "https"
				 * 2. getMethod is "POST"
				 * 3. getContextType is "application/x-www-form-urlencoded"
				 * 4. servlet calls getParameter* method.
				 * Servlet 2.2 Section 5.1
				 */
				String content_type = getContentType();

				if (content_type != null) {
					int index = content_type.indexOf(';', 0);
					if (index >= 0) {
						content_type = content_type.substring(0, index).trim();
					}
				}

				if ("POST".equals(method) && //$NON-NLS-1$
						("http".equals(scheme) || "https".equals(scheme)) && //$NON-NLS-1$ //$NON-NLS-2$
						"application/x-www-form-urlencoded".equals(content_type) //$NON-NLS-1$
				) {
					int content_length = getContentLength();
					if (content_length > 0) {
						//                      System.out.println("Read POST data");
						/* Read the post data from the ServletInputStream */
						ServletInputStream in = getInputStream();
						byte buffer[] = new byte[content_length];
						int bytesRead = 0;
						while (bytesRead < content_length) {
							int count;

							try {
								count = in.read(buffer, bytesRead, content_length - bytesRead);
							} catch (IOException e) {
								throw new IllegalArgumentException();
							}

							if (count < 1) {
								break;
							}

							bytesRead += count;
						}

						String encoding = getCharacterEncoding();

						/* Must use charset getCharacterEncoding or iso-8859-1 if null.
						 * Servlet 2.3 Section 4.9
						 */

						String postData = URI.convert(buffer, 0, bytesRead, encoding);

						if (parameters == null) {
							parameters = new Hashtable();
						}

						parseQueryString(parameters, postData, encoding);
					}
				}
			} catch (Exception e) {
				//Bad query string, ignore, log and continue
				http.logError(HttpMsg.HTTP_QUERYDATA_PARSE_EXCEPTION, e);
			}

			parsedQueryData = true;
		}
	}

	/**
	 * Parses a query string and builds a hashtable of key-value
	 * pairs, where the values are arrays of strings.  The query string
	 * should have the form of a string packaged by the GET or POST
	 * method.  (For example, it should have its key-value pairs
	 * delimited by ampersands (&) and its keys separated from its
	 * values by equal signs (=).)
	 *
	 * <p> A key can appear one or more times in the query string.
	 * Each time a key appears, its corresponding value is inserted
	 * into its string array in the hash table.  (So keys that appear
	 * once in the query string have, in the hash table, a string array
	 * of length one as their value, keys that appear twice have a
	 * string array of length two, etc.)
	 *
	 * <p> When the keys and values are moved into the hashtable, any
	 * plus signs (+) are returned to spaces and characters sent in
	 * hexadecimal notation (%xx) are converted back to characters.
	 *
	 * @param data query string to be parsed
	 * @param result a hashtable built from the parsed key-value pairs; the
	 *.hashtable's values are arrays of strings
	 * @exception IllegalArgumentException if the query string is
	 * invalid.
	 */
	protected void parseQueryString(Hashtable result, String data, String encoding) {
		if (data == null) {
			throw new IllegalArgumentException();
		}

		//      System.out.println("Querystring: " + data);

		data = data.trim(); /* Strip CRLF if present */

		int len = data.length();

		if (len >= 0) {
			int begin = 0;

			while (true) {
				int end = data.indexOf('&', begin);
				if (end == -1) {
					end = len;
				}

				int equals = data.indexOf('=', begin);

				String key;
				String value;
				if ((equals >= end) || (equals == -1)) {
					key = URI.decode(data, begin, end, encoding);
					value = ""; //$NON-NLS-1$
				} else {
					key = URI.decode(data, begin, equals, encoding);
					value = URI.decode(data, equals + 1, end, encoding);
				}

				String[] values = (String[]) result.get(key);

				if (values == null) {
					values = new String[1];
					values[0] = value;
					result.put(key, values);
				} else {
					int length = values.length;
					String[] newvalues = new String[length + 1];
					System.arraycopy(values, 0, newvalues, 0, length);
					newvalues[length] = value;
					result.put(key, newvalues);
				}

				if (end == len) {
					break;
				}

				begin = end + 1;
			}
		}
	}

	/**
	 * This method was created in VisualAge.
	 * @return java.lang.String
	 * @param requestLine java.lang.String
	 */
	protected void parseRequestLine(String requestLine) {
		if (Http.DEBUG) {
			http.logDebug("Http Request Line=\"" + requestLine + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		//      System.out.println("Http Request Line=\"" + requestLine + "\"");

		int space = requestLine.indexOf(' ', 0);
		method = requestLine.substring(0, space);

		int nextspace = requestLine.lastIndexOf(' ');
		protocol = requestLine.substring(nextspace + 1);

		int query = requestLine.indexOf('?', space + 1);

		if ((query >= nextspace) || (query == -1)) {
			reqURI = URI.decode(requestLine, space + 1, nextspace, null);
		} else {
			reqURI = URI.decode(requestLine, space + 1, query, null);
			queryString = requestLine.substring(query + 1, nextspace);
		}
	}

	/**
	 * This method is only used by the constructor (albiet indirectly)
	 * @return java.lang.String
	 */
	protected String readHeaderLine(byte[] buffer) throws IOException {
		int read = servletInputStream.readLine(buffer, 0, buffer.length);
		if (read <= 0) {
			throw new InterruptedIOException(HttpMsg.HTTP_NO_HEADER_LINE_READ_EXCEPTION);
		}

		// BUGBUG should use 8859_1 encoding to make string
		/* create String from byte array using 0 for high byte of chars */
		String line = URI.convert(buffer, 0, read, null);

		if (line.endsWith("\n")) //$NON-NLS-1$
		{
			return (line.trim()); /* trim removes trailing CRLF */
		}

		try {
			response.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
		} finally {
			response.close();
		}

		throw new IOException(NLS.bind(HttpMsg.HTTP_HEADER_LINE_TOO_LONG_EXCEPTION, new Integer(buffer.length)));
	}

	/**
	 * This method places an attribute into the request for later use by
	 * other objects which will have access to this request object such as
	 * nested servlets.
	 *
	 * @param name Attribute name
	 * @param object Attribute value
	 */
	public void setAttribute(String name, Object val) {
		if (attributes == null) {
			synchronized (this) {
				if (attributes == null) {
					attributes = new Hashtable(31);
				}
			}
		}

		if (val == null) {
			attributes.remove(name);
		} else {
			attributes.put(name, val);
		}
	}

	/**
	 * @see javax.servlet.http.HttpServletRequest#getContextPath()
	 */
	public String getContextPath() throws UnsupportedOperationException {
		throw new UnsupportedOperationException(HttpMsg.HTTP_ONLY_SUPPORTS_2_1);
	}

	/**
	 * @see javax.servlet.http.HttpServletRequest#getHeaders(String)
	 */
	public Enumeration getHeaders(String name) throws UnsupportedOperationException {
		throw new UnsupportedOperationException(HttpMsg.HTTP_ONLY_SUPPORTS_2_1);
	}

	/**
	 * @see javax.servlet.http.HttpServletRequest#getRequestURL()
	 */
	public StringBuffer getRequestURL() throws UnsupportedOperationException {
		throw new UnsupportedOperationException(HttpMsg.HTTP_ONLY_SUPPORTS_2_1);
	}

	/**
	 * @see javax.servlet.http.HttpServletRequest#getUserPrincipal()
	 */
	public Principal getUserPrincipal() throws UnsupportedOperationException {
		throw new UnsupportedOperationException(HttpMsg.HTTP_ONLY_SUPPORTS_2_1);
	}

	/**
	 * @see javax.servlet.http.HttpServletRequest#isUserInRole(String)
	 */
	public boolean isUserInRole(String role) throws UnsupportedOperationException {
		throw new UnsupportedOperationException(HttpMsg.HTTP_ONLY_SUPPORTS_2_1);
	}

	/**
	 * @see javax.servlet.ServletRequest#getLocale()
	 */
	public Locale getLocale() throws UnsupportedOperationException {
		throw new UnsupportedOperationException(HttpMsg.HTTP_ONLY_SUPPORTS_2_1);
	}

	/**
	 * @see javax.servlet.ServletRequest#getLocales()
	 */
	public Enumeration getLocales() throws UnsupportedOperationException {
		throw new UnsupportedOperationException(HttpMsg.HTTP_ONLY_SUPPORTS_2_1);
	}

	/**
	 * @see javax.servlet.ServletRequest#getParameterMap()
	 */
	public Map getParameterMap() throws UnsupportedOperationException {
		throw new UnsupportedOperationException(HttpMsg.HTTP_ONLY_SUPPORTS_2_1);
	}

	/**
	 * @see javax.servlet.ServletRequest#getRequestDispatcher(String)
	 */
	public RequestDispatcher getRequestDispatcher(String path) throws UnsupportedOperationException {
		throw new UnsupportedOperationException(HttpMsg.HTTP_ONLY_SUPPORTS_2_1);
	}

	/**
	 * @see javax.servlet.ServletRequest#isSecure()
	 */
	public boolean isSecure() throws UnsupportedOperationException {
		throw new UnsupportedOperationException(HttpMsg.HTTP_ONLY_SUPPORTS_2_1);
	}

	/**
	 * @see javax.servlet.ServletRequest#removeAttribute(String)
	 */
	public void removeAttribute(String name) throws UnsupportedOperationException {
		throw new UnsupportedOperationException(HttpMsg.HTTP_ONLY_SUPPORTS_2_1);
	}

	/**
	 * @see javax.servlet.ServletRequest#setCharacterEncoding(String)
	 */
	public void setCharacterEncoding(String env) throws UnsupportedEncodingException, UnsupportedOperationException {
		throw new UnsupportedOperationException(HttpMsg.HTTP_ONLY_SUPPORTS_2_1);
	}

	/* For compilation only.  Will not implement.
	 * 
	 */
	public String getLocalAddr() {
		//return(socket.getInetAddress().getLocalHost().getHostAddress());
		throw new UnsupportedOperationException(HttpMsg.HTTP_ONLY_SUPPORTS_2_1);
	}

	/* For compilation only.  Will not implement.
	 * 
	 */
	public String getLocalName() {
		//return(socket.getInetAddress().getLocalHost().getHostName());
		throw new UnsupportedOperationException(HttpMsg.HTTP_ONLY_SUPPORTS_2_1);
	}

	/* For compilation only.  Will not implement.
	 * 
	 */
	public int getLocalPort() {
		throw new UnsupportedOperationException(HttpMsg.HTTP_ONLY_SUPPORTS_2_1);
	}

	/* For compilation only.  Will not implement.
	 * 
	 */
	public int getRemotePort() {
		throw new UnsupportedOperationException(HttpMsg.HTTP_ONLY_SUPPORTS_2_1);
	}
}
