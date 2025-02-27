/*******************************************************************************
 * Copyright (c) 2006, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.location;

import java.io.*;
import java.net.*;
import org.eclipse.osgi.internal.location.Locker.MockLocker;

/**
 * @since 3.3
 */
public class LocationHelper {
	public static final String PROP_OSGI_LOCKING = "osgi.locking"; //$NON-NLS-1$
	public static final String LOCKING_NONE = "none"; //$NON-NLS-1$
	public static final String LOCKING_IO = "java.io"; //$NON-NLS-1$
	public static final String LOCKING_NIO = "java.nio"; //$NON-NLS-1$

	/**
	 * Builds a URL with the given specification
	 * @param spec the URL specification
	 * @param trailingSlash flag to indicate a trailing slash on the spec
	 * @return a URL
	 */
	@SuppressWarnings("deprecation")
	public static URL buildURL(String spec, boolean trailingSlash) {
		if (spec == null)
			return null;
		if (File.separatorChar == '\\')
			spec = spec.trim();
		boolean isFile = spec.startsWith("file:"); //$NON-NLS-1$
		try {
			if (isFile)
				return adjustTrailingSlash(toFileURL(spec).toURL(), trailingSlash);
			return new URL(spec);
		} catch (MalformedURLException e) {
			// if we failed and it is a file spec, there is nothing more we can do
			// otherwise, try to make the spec into a file URL.
			if (isFile)
				return null;
			try {
				return adjustTrailingSlash(new File(spec).toURL(), trailingSlash);
			} catch (MalformedURLException e1) {
				return null;
			}
		}
	}

	private static File toFileURL(String spec) {
		try {
			// Try to build it from a URI that will be properly decoded.
			return new File(new URI(spec));
		} catch (URISyntaxException | IllegalArgumentException e) {
			return new File(spec.substring(5));
		}
	}

	private static URL adjustTrailingSlash(URL url, boolean trailingSlash) throws MalformedURLException {
		String file = url.getPath();
		if (trailingSlash == (file.endsWith("/"))) //$NON-NLS-1$
			return url;
		file = trailingSlash ? file + "/" : file.substring(0, file.length() - 1); //$NON-NLS-1$
		return new URL(url.getProtocol(), url.getHost(), file);
	}

	public static Locker createLocker(File lock, String lockMode, boolean debug) {
		if (lockMode == null) {
			// try to get the lockMode from the system properties
			lockMode = System.getProperty(PROP_OSGI_LOCKING);
		}
		if (LOCKING_NONE.equals(lockMode)) {
			return new MockLocker();
		}
		if (LOCKING_IO.equals(lockMode)) {
			return new Locker_JavaIo(lock);
		}
		if (LOCKING_NIO.equals(lockMode)) {
			return new Locker_JavaNio(lock, debug);
		}

		//	Backup case if an invalid value has been specified
		return new Locker_JavaNio(lock, debug);
	}

	public static InputStream getStream(URL location) throws IOException {
		if ("file".equalsIgnoreCase(location.getProtocol())) { //$NON-NLS-1$
			// this is done to handle URLs with invalid syntax in the path
			File f = new File(location.getPath());
			if (f.exists()) {
				return new FileInputStream(f);
			}
		}
		return location.openStream();
	}

	public static URLConnection getConnection(URL url) throws IOException {
		if ("file".equalsIgnoreCase(url.getProtocol())) { //$NON-NLS-1$
			try {
				return url.openConnection();
			} catch (IllegalArgumentException e) {
				// this is done to handle URLs with invalid syntax in the path for URIs
				File f = new File(url.getPath());
				if (f.exists()) {
					return f.toURI().toURL().openConnection();
				}
			}
		}
		return url.openConnection();
	}

	public static File decodePath(File file) {
		// Pre-check if file exists, if not, and it contains escape characters,
		// try decoding the absolute path generated by makeAbsolute
		if (!file.exists() && (file.getPath().indexOf('%') >= 0 || file.getPath().indexOf('+') >= 0)) {
			String absolute = file.getAbsolutePath();
			String decodePath = LocationHelper.decode(absolute, true);
			File f = new File(decodePath);
			if (f.exists()) {
				return f;
			}
			decodePath = LocationHelper.decode(absolute, false);
			f = new File(decodePath);
			if (f.exists()) {
				return f;
			}
		}
		return file;
	}

	public static String decode(String urlString, boolean plusEncoded) {
		//first encode '+' characters, because URLDecoder incorrectly converts
		//them to spaces on certain class library implementations.
		if (plusEncoded && urlString.indexOf('+') >= 0) {
			int len = urlString.length();
			StringBuilder buf = new StringBuilder(len);
			for (int i = 0; i < len; i++) {
				char c = urlString.charAt(i);
				if (c == '+')
					buf.append("%2B"); //$NON-NLS-1$
				else
					buf.append(c);
			}
			urlString = buf.toString();
		}
		try {
			return URLDecoder.decode(urlString, "UTF-8"); //$NON-NLS-1$
		} catch (UnsupportedEncodingException | RuntimeException e) {
			// Tried but failed
			// TODO should we throw runtime exception here?
			// May have illegal characters for decoding
			return urlString;
		}
	}
}
