/*******************************************************************************
 * Copyright (c) 2005, 2014 Cognos Incorporated, IBM Corporation and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Cognos Incorporated - initial API and implementation
 *     IBM Corporation - bug fixes and enhancements
 *     Raymond Augé <raymond.auge@liferay.com> - Bug 436698
 *     Falko Schumann <falko.schumann@bitctrl.de> - Bug 519955
 *******************************************************************************/
package org.eclipse.equinox.http.servlet.internal.servlet;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.security.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.*;
import org.eclipse.equinox.http.servlet.RangeAwareServletContextHelper;
import org.eclipse.equinox.http.servlet.internal.util.Const;
import org.osgi.service.http.context.ServletContextHelper;

public class ResourceServlet extends HttpServlet {
	private static final long serialVersionUID = 3586876493076122102L;
	private static final String LAST_MODIFIED = "Last-Modified"; //$NON-NLS-1$
	private static final String IF_MODIFIED_SINCE = "If-Modified-Since"; //$NON-NLS-1$
	private static final String IF_NONE_MATCH = "If-None-Match"; //$NON-NLS-1$
	private static final String ETAG = "ETag"; //$NON-NLS-1$
	private static final String RANGE = "Range"; //$NON-NLS-1$
	private static final String ACCEPT_RANGES = "Accept-Ranges"; //$NON-NLS-1$
	private static final String RANGE_UNIT_BYTES = "bytes"; //$NON-NLS-1$
	private static final String CONTENT_RANGE = "Content-Range"; //$NON-NLS-1$

	private final String internalName;
	final ServletContextHelper servletContextHelper;
	private final AccessControlContext acc;

	public ResourceServlet(String internalName, ServletContextHelper servletContextHelper, AccessControlContext acc) {
		if (internalName.equals(Const.SLASH)) {
			internalName = Const.BLANK;
		}
		this.internalName = internalName;
		this.servletContextHelper = servletContextHelper;
		this.acc = acc;
	}

	public void service(HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		String method = req.getMethod();
		if (method.equals("GET") || method.equals("POST") || method.equals("HEAD")) { //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
			String pathInfo = HttpServletRequestWrapperImpl.getDispatchPathInfo(req);
			if (pathInfo == null)
				pathInfo = Const.BLANK;
			String resourcePath = internalName + pathInfo;
			URL resourceURL = servletContextHelper.getResource(resourcePath);
			if (resourceURL != null)
				writeResource(req, resp, resourcePath, resourceURL);
			else
				resp.sendError(HttpServletResponse.SC_NOT_FOUND, "ProxyServlet: " + req.getRequestURI()); //$NON-NLS-1$
		} else {
			resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
		}
	}

	private void writeResource(final HttpServletRequest req, final HttpServletResponse resp, final String resourcePath, final URL resourceURL) throws IOException {
		try {
			AccessController.doPrivileged(new PrivilegedExceptionAction<Boolean>() {

				public Boolean run() throws Exception {
					URLConnection connection = resourceURL.openConnection();
					long lastModified = connection.getLastModified();
					int contentLength = connection.getContentLength();

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

					String rangeHeader = req.getHeader(RANGE);
					Range range = null;
					if (rangeHeader != null) {
						range = Range.createFromRangeHeader(rangeHeader);
						range.completeLength = contentLength;
						range.updateBytePos();

						if (!range.isValid()) {
							resp.setHeader(ACCEPT_RANGES, RANGE_UNIT_BYTES);
							resp.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
							return Boolean.TRUE;
						}
					}

					// return the full contents regularly
					if (contentLength != -1)
						resp.setContentLength(contentLength);

					String filename = new File(resourcePath).getName();
					String contentType = servletContextHelper.getMimeType(filename);
					if (contentType == null)
						contentType = getServletConfig().getServletContext().getMimeType(filename);

					if (contentType != null)
						resp.setContentType(contentType);

					if (lastModified > 0)
						resp.setDateHeader(LAST_MODIFIED, lastModified);

					if (etag != null)
						resp.setHeader(ETAG, etag);

					if (range == null &&
						(servletContextHelper instanceof RangeAwareServletContextHelper) &&
						((RangeAwareServletContextHelper)servletContextHelper).rangeableContentType(contentType, req.getHeader("User-Agent"))) { //$NON-NLS-1$

						range = new Range();
						range.firstBytePos = 0;
						range.completeLength = contentLength;
						range.updateBytePos();
					}

					if (range != null) {
						resp.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
						resp.setHeader(ACCEPT_RANGES, RANGE_UNIT_BYTES);
						resp.setContentLength(range.contentLength());
						resp.setHeader(CONTENT_RANGE, RANGE_UNIT_BYTES + " " + range.firstBytePos + "-" + range.lastBytePos + "/" + range.completeLength); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					}

					if (contentLength != 0) {
						// open the input stream
						try (InputStream is = connection.getInputStream()) {
							// write the resource
							try {
								OutputStream os = resp.getOutputStream();
								int writtenContentLength = writeResourceToOutputStream(is, os, range);
								if (contentLength == -1 || contentLength != writtenContentLength)
									resp.setContentLength(writtenContentLength);
							} catch (IllegalStateException e) { // can occur if the response output is already open as a Writer
								Writer writer = resp.getWriter();
								writeResourceToWriter(is, writer, range);
								// Since ContentLength is a measure of the number of bytes contained in the body
								// of a message when we use a Writer we lose control of the exact byte count and
								// defer the problem to the Servlet Engine's Writer implementation.
							}
						} catch (FileNotFoundException e) {
							// FileNotFoundException may indicate the following scenarios
							// - url is a directory
							// - url is not accessible
							sendError(resp, HttpServletResponse.SC_FORBIDDEN);
						} catch (SecurityException e) {
							// SecurityException may indicate the following scenarios
							// - url is not accessible
							sendError(resp, HttpServletResponse.SC_FORBIDDEN);
						}
					}
					return Boolean.TRUE;
				}
			}, acc);
		} catch (PrivilegedActionException e) {
			throw (IOException) e.getException();
		}
	}

	void sendError(final HttpServletResponse resp, int sc) throws IOException {

		try {
			// we need to reset headers for 302 and 403
			resp.reset();
			resp.sendError(sc);
		} catch (IllegalStateException e) {
			// this could happen if the response has already been committed
		}
	}

	int writeResourceToOutputStream(InputStream is, OutputStream os, Range range) throws IOException {
		if (range != null) {
			if (range.firstBytePos != Range.NOT_SET) {
				is.skip(range.firstBytePos);
			} else {
				is.skip(range.completeLength - range.lastBytePos);
			}
		}

		byte[] buffer = new byte[8192];
		int bytesRead = is.read(buffer);
		int writtenContentLength = 0;
		while (bytesRead != -1 && (range == null || range.lastBytePos == Range.NOT_SET || writtenContentLength < range.lastBytePos)) {
			if (range != null && range.lastBytePos != Range.NOT_SET && (bytesRead + writtenContentLength) > range.lastBytePos) {
				bytesRead = range.contentLength() - writtenContentLength;
			}
			os.write(buffer, 0, bytesRead);
			writtenContentLength += bytesRead;
			bytesRead = is.read(buffer);
		}
		return writtenContentLength;
	}

	void writeResourceToWriter(InputStream is, Writer writer, Range range) throws IOException {
		if (range != null) {
			if (range.firstBytePos != Range.NOT_SET) {
				is.skip(range.firstBytePos);
			} else {
				is.skip(range.completeLength - range.lastBytePos);
			}
		}

		try (Reader reader = new InputStreamReader(is)) {
			char[] buffer = new char[8192];
			int charsRead = reader.read(buffer);
			int writtenContentLength = 0;
			while (charsRead != -1 && (range == null || range.lastBytePos == Range.NOT_SET || writtenContentLength < range.lastBytePos)) {
				if (range != null && range.lastBytePos != Range.NOT_SET && (charsRead + writtenContentLength) > range.lastBytePos) {
					charsRead = range.contentLength() - writtenContentLength;
				}
				writer.write(buffer, 0, charsRead);
				writtenContentLength += charsRead;
				charsRead = reader.read(buffer);
			}
		}
	}

	static class Range {

		private static final Pattern RANGE_PATTERN = Pattern.compile("^(.+)=(\\d+)?-(\\d+)?$"); //$NON-NLS-1$

		static final int NOT_SET = -1;

		String rangeUnit = RANGE_UNIT_BYTES;
		int firstBytePos = NOT_SET;
		int lastBytePos = NOT_SET;
		int completeLength = NOT_SET;

		static Range createFromRangeHeader(String header) {
			Range range = new Range();

			Matcher matcher = RANGE_PATTERN.matcher(header);
			if (!matcher.matches()) {
				// use default values
				return range;
			}

			range.rangeUnit = matcher.group(1);
			if (matcher.group(2) != null) {
				try {
					range.firstBytePos = Integer.parseInt(matcher.group(2));
				} catch (NumberFormatException ignored) {
					// use default value
				}
			}
			if (matcher.group(3) != null) {
				try {
					range.lastBytePos = Integer.parseInt(matcher.group(3));
				} catch (NumberFormatException ignored) {
					// use default value
				}
			}
			return range;
		}

		void updateBytePos() {
			if (lastBytePos == -1 || lastBytePos >= completeLength) {
				lastBytePos = completeLength - 1;
			}
			if (firstBytePos == -1) {
				firstBytePos = completeLength - lastBytePos - 1;
				lastBytePos = completeLength - 1;
			}
		}

		boolean isValid() {
			// we accept bytes unit only
			if (!RANGE_UNIT_BYTES.equals(rangeUnit)) {
				return false;
			}

			if (firstBytePos == NOT_SET && lastBytePos == NOT_SET) {
				return false;
			}

			if (firstBytePos >= completeLength) {
				return false;
			}

			if (lastBytePos >= completeLength) {
				return false;
			}

			return true;
		}

		int contentLength() {
			return lastBytePos - firstBytePos + 1;
		}

	}

}
