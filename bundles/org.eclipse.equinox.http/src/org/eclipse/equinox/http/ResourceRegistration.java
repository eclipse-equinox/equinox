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
package org.eclipse.equinox.http;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.*;
import org.eclipse.equinox.http.servlet.HttpServletRequestImpl;
import org.eclipse.equinox.http.servlet.HttpServletResponseImpl;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;

/**
 This class contains methods used to send requested resources a client.
 */
public class ResourceRegistration extends HttpServlet implements Registration {

	protected Bundle bundle;
	protected HttpContext httpContext;
	protected String alias;
	protected String path;
	protected Http http;
	protected SecureAction secureAction;

	/**
	 * The constructor
	 */
	protected ResourceRegistration(Bundle bundle, String alias, String path, HttpContext httpContext, Http http) {
		this.bundle = bundle;
		this.alias = alias;
		this.path = path;
		this.httpContext = httpContext;
		this.http = http;

		secureAction = new SecureAction();
	}

	public Bundle getBundle() {
		return (bundle);
	}

	public HttpContext getHttpContext() {
		return (httpContext);
	}

	/**
	 * This method returns the correct MIME type of a given URI by first checking
	 * the HttpContext::getMimeType and, if null, checking the httpservice's MIMETypes table.
	 * @return java.lang.String
	 * @param URI java.lang.String
	 */
	private String computeMimeType(String name, URLConnection conn) {
		String mimeType = httpContext.getMimeType(name);
		if (mimeType != null) {
			return (mimeType);
		}

		mimeType = conn.getContentType();
		if (mimeType != null) {
			return (mimeType);
		}

		return (http.getMimeType(name));
	}

	public void destroy() {
		alias = null;
		path = null;
	}

	public String getAlias() {
		return (alias);
	}

	/** This method is called by Http::handleConnection.  It is called when a request comes in for
	 * a resource registered by this registration.
	 */
	public void service(HttpServletRequestImpl request, HttpServletResponseImpl response) throws ServletException, IOException {
		/* set additional data for the servlet request */
		request.init(alias, null);

		super.service(request, response);
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String filename = getFilename(request.getRequestURI());

		URL url = httpContext.getResource(filename);

		if (url == null) {
			// We got null back from httpContext.getResource
			// In this case we want to keep looking to see if another alias matches
			throw new ResourceUnavailableException();
		}

		URLConnection conn = secureAction.openURL(url);

		long modifiedSince = request.getDateHeader("If-Modified-Since"); //$NON-NLS-1$
		if (modifiedSince >= 0) {
			long modified = conn.getLastModified();
			if ((modified > 0) && (modifiedSince >= modified)) {
				response.setStatus(response.SC_NOT_MODIFIED);
				return;
			}
		}

		InputStream in;
		try {
			in = conn.getInputStream();
		} catch (IOException ex) {
			response.sendError(response.SC_NOT_FOUND);
			return;
		}

		try {
			int contentlength = conn.getContentLength();
			if (contentlength >= 0) {
				response.setContentLength(contentlength);
			}

			String mimeType = computeMimeType(filename, conn);
			response.setContentType(mimeType);

			// We want to use a writer if we are sending text
			if (mimeType.startsWith("text/")) //$NON-NLS-1$
			{
				PrintWriter writer = response.getWriter();

				writer.flush(); /* write the headers and unbuffer the output */

				BufferedReader reader = new BufferedReader(new InputStreamReader(in));

				char buffer[] = new char[4096];
				int read;
				while ((read = reader.read(buffer, 0, buffer.length)) != -1) {
					writer.write(buffer, 0, read);
				}
			} else {
				ServletOutputStream out = response.getOutputStream();

				out.flush(); /* write the headers and unbuffer the output */

				byte buffer[] = new byte[4096];
				int read;
				while ((read = in.read(buffer, 0, buffer.length)) != -1) {
					out.write(buffer, 0, read);
				}
			}
		} finally {
			in.close();
		}
	}

	protected void doHead(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String filename = getFilename(request.getRequestURI());

		URL url = httpContext.getResource(filename);

		if (url == null) {
			// We got null back from httpContext.getResource
			// In this case we want to keep looking to see if another alias matches
			throw new ResourceUnavailableException();
		}
		URLConnection conn = secureAction.openURL(url);
		int contentlength = conn.getContentLength();
		if (contentlength >= 0) {
			response.setContentLength(contentlength);

			String mimeType = computeMimeType(filename, conn);
			response.setContentType(mimeType);
		} else {
			super.doHead(request, response);
		}
	}

	protected String getFilename(String filename) {
		//If the requested URI is equal to the Registeration's alias, send the file
		//corresponding to the alias.  Otherwise, we have request for a file in an
		//registered directory (the file was not directly registered itself).
		if (filename.equals(alias)) {
			filename = path;
		} else {
			// The file we re looking for is the registered resource (alias) + the rest of the
			// filename that is not part of the registered resource.  For example, if we export
			// /a to /tmp and we have a request for /a/b/foo.txt, then /tmp is our directory
			// (file.toString()) and /b/foo.txt is the rest.
			// The result is that we open the file /tmp/b/foo.txt.

			int aliaslen = alias.length();
			int pathlen = path.length();

			if (pathlen == 1) /* path == "/" */
			{
				if (aliaslen > 1) /* alias != "/" */
				{
					filename = filename.substring(aliaslen);
				}
			} else /* path != "/" */
			{
				StringBuffer buf = new StringBuffer(aliaslen + pathlen);
				buf.append(path);

				if (aliaslen == 1) /* alias == "/" */
				{
					buf.append(filename);
				} else /* alias != "/" */
				{
					buf.append(filename.substring(aliaslen));
				}

				filename = buf.toString();
			}
		}

		return (filename);
	}
}
