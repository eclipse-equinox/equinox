/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.location;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
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
				return adjustTrailingSlash(new File(spec.substring(5)).toURL(), trailingSlash);
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

	private static URL adjustTrailingSlash(URL url, boolean trailingSlash) throws MalformedURLException {
		String file = url.getFile();
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
}
