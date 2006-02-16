/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import org.osgi.framework.Bundle;

/**
 * This class contains collection of helper methods aimed at finding files in bundles.
 * This class can only be used if OSGi plugin is available.
 * <p>
 * The class is not intended to be subclassed or instantiated by clients.
 * </p>
 * @since 3.2
 * @deprecated clients should use {@link FileLocator} instead. This class will
 * 	be removed before the 3.2 release (in the next week!).
 */
public final class BundleFinder {

	/**
	 * Returns a URL for the given path in the given bundle.  Returns <code>null</code> if the URL
	 * could not be computed or created.
	 * 
	 * @param bundle the bundle in which to search
	 * @param path path relative to plug-in installation location 
	 * @return a URL for the given path or <code>null</code>.  The actual form
	 * of the returned URL is not specified.
	 * @see #find(Bundle, IPath, Map)
	 * @deprecated use {@link FileLocator#find(Bundle, IPath, Map)}
	 */
	public static URL find(Bundle bundle, IPath path) {
		return FileLocator.find(bundle, path, null);
	}

	/**
	 * Returns a URL for the given path in the given bundle.  Returns <code>null</code> if the URL
	 * could not be computed or created.
	 * <p>
	 * find looks for this path in given bundle and any attached fragments.  
	 * <code>null</code> is returned if no such entry is found.  Note that
	 * there is no specific order to the fragments.
	 * </p><p>
	 * The following arguments may also be used
	 * <pre>
	 *     $nl$ - for language specific information
	 *     $os$ - for operating system specific information
	 *     $ws$ - for windowing system specific information
	 * </pre>
	 * </p><p>
	 * A path of $nl$/about.properties in an environment with a default 
	 * locale of en_CA will return a URL corresponding to the first place
	 * about.properties is found according to the following order:
	 * <pre>
	 *     plugin root/nl/en/CA/about.properties
	 *     fragment1 root/nl/en/CA/about.properties
	 *     fragment2 root/nl/en/CA/about.properties
	 *     ...
	 *     plugin root/nl/en/about.properties
	 *     fragment1 root/nl/en/about.properties
	 *     fragment2 root/nl/en/about.properties
	 *     ...
	 *     plugin root/about.properties
	 *     fragment1 root/about.properties
	 *     fragment2 root/about.properties
	 *     ...
	 * </pre>
	 * </p><p>
	 * The current environment variable values can be overridden using 
	 * the override map argument or <code>null</code> can be specified
	 * if this is not desired.
	 * </p>
	 * 
	 * @param bundle the bundle in which to search
	 * @param path file path relative to plug-in installation location
	 * @param override map of override substitution arguments to be used for
	 * any $arg$ path elements. The map keys correspond to the substitution
	 * arguments (eg. "$nl$" or "$os$"). The resulting
	 * values must be of type java.lang.String. If the map is <code>null</code>,
	 * or does not contain the required substitution argument, the default
	 * is used.
	 * @return a URL for the given path or <code>null</code>.  The actual form
	 * of the returned URL is not specified.
	 * @deprecated use {@link FileLocator#find(Bundle, IPath, Map)}
	 */
	public static URL find(Bundle bundle, IPath path, Map override) {
		return FileLocator.find(bundle, path, override);
	}

	/**
	 * Returns an input stream for the specified file. The file path
	 * must be specified relative to this plug-in's installation location.
	 * Optionally, the platform searches for the correct localized version
	 * of the specified file using the users current locale, and Java
	 * naming convention for localized resource files (locale suffix appended 
	 * to the specified file extension).
	 * <p>
	 * The caller must close the returned stream when done.
	 * </p>
	 *
	 * @param bundle the bundle in which to search
	 * @param file path relative to plug-in installation location
	 * @param localized <code>true</code> for the localized version
	 *   of the file, and <code>false</code> for the file exactly
	 *   as specified
	 * @return an input stream
	 * @exception IOException if the given path cannot be found in this plug-in
	 * @deprecated use {@link FileLocator#openStream(Bundle, IPath, boolean)}
	 */
	public static InputStream openStream(Bundle bundle, IPath file, boolean localized) throws IOException {
		return FileLocator.openStream(bundle, file, localized);
	}

	/**
	 * Returns an input stream for the specified file. The file path
	 * must be specified relative this the plug-in's installation location.
	 *
	 * @param bundle the bundle in which to search
	 * @param file path relative to plug-in installation location
	 * @return an input stream
	 * @exception IOException if the given path cannot be found in this plug-in
	 * 
	 * @see #openStream(Bundle,IPath,boolean)
	 * @deprecated use {@link FileLocator#openStream(Bundle, IPath, boolean)}
	 */
	public static final InputStream openStream(Bundle bundle, IPath file) throws IOException {
		return FileLocator.openStream(bundle, file, false);
	}

	/**
	 * Converts a URL that uses a user-defined protocol into a URL that uses the file
	 * protocol. The contents of the URL may be extracted into a cache on the file-system
	 * in order to get a file URL. 
	 * <p>
	 * If the protocol for the given URL is not recognized by this converter, the original
	 * URL is returned as-is.
	 * </p>
	 * @param url the original URL
	 * @return the converted file URL or the original URL passed in if it is 
	 * 	not recognized by this converter
	 * @throws IOException if an error occurs during the conversion
	 * @deprecated use {@link FileLocator#toFileURL(URL)}
	 */
	public static URL toFileURL(URL url) throws IOException {
		return FileLocator.toFileURL(url);
	}

	/**
	 * Converts a URL that uses a client-defined protocol into a URL that uses a
	 * protocol which is native to the Java class library (file, jar, http, etc).
	 * <p>
	 * Note however that users of this API should not assume too much about the
	 * results of this method.  While it may consistently return a file: URL in certain
	 * installation configurations, others may result in jar: or http: URLs.
	 * </p>
	 * <p>
	 * If the protocol is not reconized by this converter, then the original URL is
	 * returned as-is.
	 * </p>
	 * @param url the original URL
	 * @return the resolved URL or the original if the protocol is unknown to this converter
	 * @exception IOException if unable to resolve URL
	 * @throws IOException if an error occurs during the resolution
	 * @deprecated use {@link FileLocator#resolve(URL)}
	 */
	public static URL resolve(URL url) throws IOException {
		return FileLocator.resolve(url);
	}
}
