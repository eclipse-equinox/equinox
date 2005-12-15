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
import java.io.PrintWriter;
import java.util.Locale;
import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.equinox.http.HttpMsg;
import org.eclipse.equinox.http.Registration;

public class RequestDispatcherImpl implements RequestDispatcher {
	protected Registration reg;
	protected String requestURI;

	class Response implements HttpServletResponse {
		protected ServletResponse response;

		protected Response(ServletResponse response) {
			this.response = response;
		}

		public String getCharacterEncoding() {
			return (null);
		}

		public PrintWriter getWriter() throws IOException {
			return (response.getWriter());
		}

		public ServletOutputStream getOutputStream() throws IOException {
			return (response.getOutputStream());
		}

		public String encodeRedirectURL(String url) {
			return (null);
		}

		public String encodeRedirectUrl(String url) {
			return (null);
		}

		public String encodeURL(String url) {
			return (null);
		}

		public String encodeUrl(String url) {
			return (null);
		}

		public void sendRedirect(String url) {
		}

		public boolean containsHeader(String header) {
			return (false);
		}

		public void addCookie(Cookie cookie) {
		}

		public void sendError(int errorCode) {
		}

		public void sendError(int errorCode, String error) {
		}

		public void setContentLength(int length) {
		}

		public void setContentType(String contentType) {
		}

		public void setStatus(int status) {
		}

		public void setStatus(int status, String statusPhrase) {
		}

		public void setHeader(String key, String value) {
		}

		public void setIntHeader(String key, int value) {
		}

		public void setDateHeader(String key, long date) {
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

		/* 
		 * JavaServlet 2.4 API - For complilation only - Not Implemented
		 */
		public String getContentType() {
			throw new UnsupportedOperationException(HttpMsg.HTTP_ONLY_SUPPORTS_2_1);
		}

		/* 
		 * JavaServlet 2.4 API - For complilation only - Not Implemented
		 */
		public void setCharacterEncoding(String arg0) {
			throw new UnsupportedOperationException(HttpMsg.HTTP_ONLY_SUPPORTS_2_1);

		}

	}

	public RequestDispatcherImpl(Registration reg, String requestURI) {
		this.reg = reg;
		this.requestURI = requestURI;
	}

	/**
	 * Used for forwarding a request from this servlet to another
	 * resource on the server. This method is useful when one servlet
	 * does preliminary processing of a request and wants to let
	 * another object generate the response.
	 *
	 * <p>The <tt>request</tt> object passed to the target object
	 * will have its request URL path and other path parameters
	 * adjusted to reflect the target URL path of the target ojbect.
	 *
	 * <p>You cannot use this method if a <tt>ServletOutputStream</tt>
	 * object or <tt>PrintWriter</tt> object has been obtained from
	 * the response. In that case, the method throws an
	 * <tt>IllegalStateException</tt>
	 *
	 * @param request the client's request on the servlet
	 * @param response the client's response from the servlet
	 * @exception ServletException if a servlet exception is thrown by the
	 *            target servlet
	 * @exception IOException if an I/O Exception occurs
	 * @exception IllegalStateException if the ServletOutputStream or a writer
	 *            had allready been obtained from the response object
	 */
	public void forward(ServletRequest req, ServletResponse res) throws ServletException, IOException {
		// BUGBUG If the response has already been committed, then an
		// IllegalStateException must be thrown. Otherwise, the response
		// buffer must be cleared before calling.
		// Servlet 2.2 Section 8.4
		try {
			HttpServletRequestImpl request = (HttpServletRequestImpl) req;
			HttpServletResponseImpl response = (HttpServletResponseImpl) res;

			if (response.gotStreamOrWriter()) {
				throw new IllegalStateException();
			}

			String newRequestLine = request.getMethod() + " " + requestURI + " " + request.getProtocol(); //$NON-NLS-1$ //$NON-NLS-2$
			request.parseRequestLine(newRequestLine);

			reg.service(request, response);

			// BUGBUG Response must be committed and closed before returning.
			// Servlet 2.2 Section 8.4
		} catch (ClassCastException e) {
			throw new ServletException(HttpMsg.HTTP_SERVLET_EXCEPTION, e);
		}
	}

	/**
	 * Used for including the content generated by another server
	 * resource in the body of a response. In essence, this method
	 * enables programmatic server side includes.
	 *
	 * <p>The request object passed to the target object will reflect
	 * the request URL path and path info of the calling request.
	 * The response object only has access to the calling servlet's
	 * <tt>ServletOutputStream</tt> object or <tt>PrintWriter</tt>
	 * object.
	 *
	 * <p>An included servlet cannot set headers. If the included
	 * servlet calls a method that may need to set headers (such as
	 * sessions might need to), the method is not guaranteed to work.
	 * As a servlet developer, you must ensure that any methods
	 * that might need direct access to headers are properly resolved.
	 * To ensure that a session works correctly, start the session
	 * outside of the included servlet, even if you use session tracking.
	 *
	 * @param request the client's request on the servlet
	 * @param response the client's response from the servlet
	 * @exception ServletException if a servlet exception is thrown by the
	 *            target servlet
	 * @exception IOException if the ServletOutputStream or a writer
	 *            had already been obtained from the response object
	 */
	public void include(ServletRequest req, ServletResponse res) throws ServletException, IOException {
		// BUGBUG need to set javax.servlet.include.* request
		// attributes before calling.
		// Servlet 2.2 Section 8.3.1
		try {
			ServletResponse response = new Response(res);

			reg.service(req, response);
		} catch (ClassCastException e) {
			throw new ServletException(HttpMsg.HTTP_SERVLET_EXCEPTION, e);
		}
	}
}
