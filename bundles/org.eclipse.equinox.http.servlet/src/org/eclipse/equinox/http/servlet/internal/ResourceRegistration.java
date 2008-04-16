/*******************************************************************************
 * Copyright (c) 2005-2007 Cognos Incorporated, IBM Corporation and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Cognos Incorporated - initial API and implementation
 *     IBM Corporation - bug fixes and enhancements
 *******************************************************************************/
package org.eclipse.equinox.http.servlet.internal;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.security.*;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.osgi.service.http.HttpContext;

public class ResourceRegistration extends Registration {
	private static final String LAST_MODIFIED = "Last-Modified"; //$NON-NLS-1$
	private static final String IF_MODIFIED_SINCE = "If-Modified-Since"; //$NON-NLS-1$
	private static final String IF_NONE_MATCH = "If-None-Match"; //$NON-NLS-1$
	private static final String ETAG = "ETag"; //$NON-NLS-1$

	private String internalName;
	HttpContext httpContext;
	ServletContext servletContext;
	private AccessControlContext acc;

	public ResourceRegistration(String internalName, HttpContext context, ServletContext servletContext, AccessControlContext acc) {
		this.internalName = internalName;
		if (internalName.equals("/")) { //$NON-NLS-1$
			this.internalName = ""; //$NON-NLS-1$
		}
		this.httpContext = context;
		this.servletContext = servletContext;
		this.acc = acc;
	}

	public boolean handleRequest(HttpServletRequest req, final HttpServletResponse resp, String alias) throws IOException {
		if (httpContext.handleSecurity(req, resp)) {

			String method = req.getMethod();
			if (method.equals("GET") || method.equals("POST") || method.equals("HEAD")) { //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$

				String pathInfo = HttpServletRequestAdaptor.getDispatchPathInfo(req);
				int aliasLength = alias.equals("/") ? 0 : alias.length(); //$NON-NLS-1$
				String resourcePath = internalName + pathInfo.substring(aliasLength);
				URL resourceURL = httpContext.getResource(resourcePath);
				if (resourceURL == null)
					return false;

				return writeResource(req, resp, resourcePath, resourceURL);
			}
			resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
		}
		return true;
	}

	private boolean writeResource(final HttpServletRequest req, final HttpServletResponse resp, final String resourcePath, final URL resourceURL) throws IOException {
		Boolean result = Boolean.TRUE;
		try {
			result = (Boolean) AccessController.doPrivileged(new PrivilegedExceptionAction() {

				public Object run() throws Exception {
					URLConnection connection = resourceURL.openConnection();
					long lastModified = connection.getLastModified();
					int contentLength = connection.getContentLength();

					// check to ensure that we're dealing with a real resource and in particular not a directory
					if (contentLength == 0) {
						InputStream testStream = null;
						try {
							testStream = connection.getInputStream();
						} catch (Exception e) {
							// this will fail if the target is a directory or the OS will not allow access 
							return Boolean.FALSE;
						} finally {
							if (testStream != null) {
								try {
									testStream.close();
								} catch (IOException e) {
									// ignore
								}
							}
						}
						// reset the connection if everything checks out
						connection = resourceURL.openConnection();
					}

					String etag = null;
					if (lastModified != -1 && contentLength != -1)
						etag = "W/\"" + contentLength + "-" + lastModified + "\""; //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$

					// Check for cache revalidation.
					// We should prefer ETag validation as the guarantees are stronger and all HTTP 1.1 clients should be using it
					String ifNoneMatch = req.getHeader(IF_NONE_MATCH);
					if (ifNoneMatch != null && etag != null && ifNoneMatch.indexOf(etag) != -1) {
						resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
						return Boolean.TRUE;
					}

					long ifModifiedSince = req.getDateHeader(IF_MODIFIED_SINCE);
					// for purposes of comparison we add 999 to ifModifiedSince since the fidelity
					// of the IMS header generally doesn't include milli-seconds
					if (ifModifiedSince > -1 && lastModified > 0 && lastModified <= (ifModifiedSince + 999)) {
						resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
						return Boolean.TRUE;
					}

					// return the full contents regularly
					if (contentLength != -1)
						resp.setContentLength(contentLength);

					String contentType = httpContext.getMimeType(resourcePath);
					if (contentType == null)
						contentType = servletContext.getMimeType(resourcePath);

					if (contentType != null)
						resp.setContentType(contentType);

					if (lastModified > 0)
						resp.setDateHeader(LAST_MODIFIED, lastModified);

					if (etag != null)
						resp.setHeader(ETAG, etag);

					try {
						OutputStream os = resp.getOutputStream();
						int writtenContentLength = writeResourceToOutputStream(connection, os);
						if (contentLength == -1 || contentLength != writtenContentLength)
							resp.setContentLength(writtenContentLength);
					} catch (IllegalStateException e) { // can occur if the response output is already open as a Writer
						Writer writer = resp.getWriter();
						writeResourceToWriter(connection, writer);
						// Since ContentLength is a measure of the number of bytes contained in the body
						// of a message when we use a Writer we lose control of the exact byte count and
						// defer the problem to the Servlet Engine's Writer implementation.
					}
					return Boolean.TRUE;
				}
			}, acc);
		} catch (PrivilegedActionException e) {
			throw (IOException) e.getException();
		}
		return result.booleanValue();
	}

	int writeResourceToOutputStream(URLConnection connection, OutputStream os) throws IOException {
		InputStream is = connection.getInputStream();
		try {
			byte[] buffer = new byte[8192];
			int bytesRead = is.read(buffer);
			int writtenContentLength = 0;
			while (bytesRead != -1) {
				os.write(buffer, 0, bytesRead);
				writtenContentLength += bytesRead;
				bytesRead = is.read(buffer);
			}
			return writtenContentLength;
		} finally {
			if (is != null)
				is.close();
		}
	}

	void writeResourceToWriter(URLConnection connection, Writer writer) throws IOException {
		InputStream is = connection.getInputStream();
		try {
			Reader reader = new InputStreamReader(connection.getInputStream());
			try {
				char[] buffer = new char[8192];
				int charsRead = reader.read(buffer);
				while (charsRead != -1) {
					writer.write(buffer, 0, charsRead);
					charsRead = reader.read(buffer);
				}
			} finally {
				if (reader != null) {
					reader.close(); // will also close input stream
					is = null;
				}
			}
		} finally {
			if (is != null)
				is.close();
		}
	}
}
