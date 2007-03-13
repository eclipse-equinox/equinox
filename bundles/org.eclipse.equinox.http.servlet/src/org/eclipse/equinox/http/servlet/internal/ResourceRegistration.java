/*******************************************************************************
 * Copyright (c) 2005 Cognos Incorporated.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Cognos Incorporated - initial API and implementation
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
				URL testURL = httpContext.getResource(resourcePath);
				if (testURL == null || resourcePath.endsWith("/")) { //$NON-NLS-1$
					return false;
				}
				return writeResource(req, resp, resourcePath);
			}
			resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
		}
		return true;
	}

	private boolean writeResource(final HttpServletRequest req, final HttpServletResponse resp, final String resourcePath) throws IOException {
		Boolean result = Boolean.TRUE;
		try {
			result = (Boolean) AccessController.doPrivileged(new PrivilegedExceptionAction() {

				public Object run() throws Exception {
					URL url = httpContext.getResource(resourcePath);
					if (url == null)
						return Boolean.FALSE;

					URLConnection connection = url.openConnection();
					long lastModified = connection.getLastModified();
					int contentLength = connection.getContentLength();
					
					// check to ensure that we're dealing with a real resource and in particular not a directory
					if (contentLength <= 0)
						return Boolean.FALSE;

					String etag = null;
					if (lastModified != -1 && contentLength != -1)
						etag = "W/\"" + contentLength + "-" + lastModified + "\""; //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$

					// Check for cache revalidation.
					// We should prefer ETag validation as the guarantees are stronger and all HTTP 1.1 clients should be using it
					String ifNoneMatch = req.getHeader(IF_NONE_MATCH);
					if (ifNoneMatch != null && etag != null && ifNoneMatch.indexOf(etag) != -1) {
						resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
						return Boolean.TRUE;
					} else {
						long ifModifiedSince = req.getDateHeader(IF_MODIFIED_SINCE);
						// for purposes of comparison we add 999 to ifModifiedSince since the fidelity
						// of the IMS header generally doesn't include milli-seconds
						if (ifModifiedSince > -1 && lastModified > 0 && lastModified <= (ifModifiedSince + 999)) {
							resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
							return Boolean.TRUE;
						}
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

					InputStream is = null;
					try {
						is = connection.getInputStream();
						OutputStream os = resp.getOutputStream();
						byte[] buffer = new byte[8192];
						int bytesRead = is.read(buffer);
						int writtenContentLength = 0;
						while (bytesRead != -1) {
							os.write(buffer, 0, bytesRead);
							writtenContentLength += bytesRead;
							bytesRead = is.read(buffer);
						}
						if (contentLength == -1 || contentLength != writtenContentLength)
							resp.setContentLength(writtenContentLength);
					} finally {
						if (is != null)
							is.close();
					}
					return Boolean.TRUE;
				}
			}, acc);
		} catch (PrivilegedActionException e) {
			throw (IOException) e.getException();
		}
		return result.booleanValue();
	}

}
