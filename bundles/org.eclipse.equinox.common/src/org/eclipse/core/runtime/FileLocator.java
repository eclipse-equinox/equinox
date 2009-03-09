/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime;

import java.io.*;
import java.net.URL;
import java.util.Map;
import org.eclipse.core.internal.runtime.Activator;
import org.eclipse.core.internal.runtime.FindSupport;
import org.eclipse.osgi.service.urlconversion.URLConverter;
import org.osgi.framework.Bundle;

/**
 * This class contains a collection of helper methods for finding files in bundles.
 * This class can only be used if the OSGi plugin is available.
 * 
 * @since org.eclipse.equinox.common 3.2
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class FileLocator {

	private FileLocator() {
		// prevent instantiation
	}

	/**
	 * Returns a URL for the given path in the given bundle.  Returns <code>null</code> if the URL
	 * could not be computed or created. 
	 * <p>
	 * This method looks for the path in the given bundle and any attached fragments.  
	 * <code>null</code> is returned if no such entry is found.  Note that
	 * there is no specific order to the fragments.
	 * </p><p>
	 * The following variables may also be used as entries in the provided path:
	 * <ul>
	 *     <li>$nl$ - for language specific information</li>
	 *     <li>$os$ - for operating system specific information</li>
	 *     <li>$ws$ - for windowing system specific information</li>
	 * </ul>
	 * </p><p>
	 * A path of "$nl$/about.properties" in an environment with a default 
	 * locale of en_CA will return a URL corresponding to the first location
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
	 * 	any $arg$ path elements. The map keys correspond to the substitution
	 * 	arguments (eg. "$nl$" or "$os$"). The resulting
	 * 	values must be of type java.lang.String. If the map is <code>null</code>,
	 * 	or does not contain the required substitution argument, the default
	 * 	is used.
	 * @return a URL for the given path or <code>null</code>.  The actual form
	 * 	of the returned URL is not specified.
	 */
	public static URL find(Bundle bundle, IPath path, Map override) {
		return FindSupport.find(bundle, path, override);
	}

	/**
	 * This method is the same as {@link #find(Bundle, IPath, Map)} except multiple entries
	 * can be returned if more than one entry matches the path in the host and 
	 * any of its fragments.
	 * 
	 * @param bundle the bundle in which to search
	 * @param path file path relative to plug-in installation location
	 * @param override map of override substitution arguments to be used for
	 * 	any $arg$ path elements. The map keys correspond to the substitution
	 * 	arguments (eg. "$nl$" or "$os$"). The resulting
	 * 	values must be of type java.lang.String. If the map is <code>null</code>,
	 * 	or does not contain the required substitution argument, the default
	 * 	is used.
	 * @return an array of entries which match the given path.  An empty 
	 * array is returned if no matches are found.
	 * 
	 * @since org.eclipse.equinox.common 3.3
	 */
	public static URL[] findEntries(Bundle bundle, IPath path, Map override) {
		return FindSupport.findEntries(bundle, path, override);
	}

	/**
	 * Returns the URL of a resource inside a bundle corresponding to the given URL.
	 * Returns <code>null</code> if the URL could not be computed or created. 
	 * <p>
	 * This method looks for a bundle resource described by the given input URL,
	 * and returns the URL of the first resource found in the bundle or any attached
	 * fragments.  <code>null</code> is returned if no such entry is found.  Note that
	 * there is no specific order to the fragments.
	 * </p><p>
	 * The following variables may also be used as segments in the path of the provided URL:
	 * <ul>
	 *     <li>$nl$ - for language specific information</li>
	 *     <li>$os$ - for operating system specific information</li>
	 *     <li>$ws$ - for windowing system specific information</li>
	 * </ul>
	 * </p><p>
	 * For example, a URL of "platform:/plugin/org.eclipse.core.runtime/$nl$/about.properties" in an 
	 * environment with a default locale of en_CA will return a URL corresponding to 
	 * the first location about.properties is found according to the following order:
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
	 * </p>
	 * 
	 * @param url The location of a bundle entry that potentially includes the above
	 * environment variables
	 * @return The URL of the bundle entry matching the input URL, or <code>null</code>
	 * if no matching entry could be found. The actual form of the returned URL is not specified.
	 * @since org.eclipse.equinox.common 3.5
	 */
	public static URL find(URL url) {
		return FindSupport.find(url);
	}

	/**
	 * This is a convenience method, fully equivalent to {@link #findEntries(Bundle, IPath, Map)},
	 * with a value of <code>null</code> for the map argument.
	 * 
	 * @param bundle the bundle in which to search
	 * @param path file path relative to plug-in installation location
	 * @return an array of entries which match the given path.  An empty 
	 * array is returned if no matches are found.
	 * 
	 * @since org.eclipse.equinox.common 3.3
	 */
	public static URL[] findEntries(Bundle bundle, IPath path) {
		return FindSupport.findEntries(bundle, path);
	}

	/**
	 * Returns an input stream for the specified file. The file path
	 * must be specified relative to this plug-in's installation location.
	 * Optionally, the path specified may contain $arg$ path elements that can 
	 * be used as substitution arguments.  If this option is used then the $arg$ 
	 * path elements are processed in the same way as {@link #find(Bundle, IPath, Map)}.
	 * <p>
	 * The caller must close the returned stream when done.
	 * </p>
	 *
	 * @param bundle the bundle in which to search
	 * @param file path relative to plug-in installation location
	 * @param substituteArgs <code>true</code> to process substitution arguments, 
	 * and <code>false</code> for the file exactly as specified without processing any
	 * substitution arguments.
	 * @return an input stream
	 * @exception IOException if the given path cannot be found in this plug-in
	 */
	public static InputStream openStream(Bundle bundle, IPath file, boolean substituteArgs) throws IOException {
		return FindSupport.openStream(bundle, file, substituteArgs);
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
	 */
	public static URL toFileURL(URL url) throws IOException {
		URLConverter converter = Activator.getURLConverter(url);
		return converter == null ? url : converter.toFileURL(url);
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
	 * If the protocol is not recognized by this converter, then the original URL is
	 * returned as-is.
	 * </p>
	 * @param url the original URL
	 * @return the resolved URL or the original if the protocol is unknown to this converter
	 * @exception IOException if unable to resolve URL
	 * @throws IOException if an error occurs during the resolution
	 */
	public static URL resolve(URL url) throws IOException {
		URLConverter converter = Activator.getURLConverter(url);
		return converter == null ? url : converter.resolve(url);
	}

	/**
	 * Returns a file for the contents of the specified bundle.  Depending 
	 * on how the bundle is installed the returned file may be a directory or a jar file 
	 * containing the bundle content.  
	 * 
	 * @param bundle the bundle
	 * @return a file with the contents of the bundle
	 * @throws IOException if an error occurs during the resolution
	 * 
	 * @since org.eclipse.equinox.common 3.4
	 */
	public static File getBundleFile(Bundle bundle) throws IOException {
		URL rootEntry = bundle.getEntry("/"); //$NON-NLS-1$
		rootEntry = resolve(rootEntry);
		if ("file".equals(rootEntry.getProtocol())) //$NON-NLS-1$
			return new File(rootEntry.getPath());
		if ("jar".equals(rootEntry.getProtocol())) { //$NON-NLS-1$
			String path = rootEntry.getPath();
			if (path.startsWith("file:")) {
				// strip off the file: and the !/
				path = path.substring(5, path.length() - 2);
				return new File(path);
			}
		}
		throw new IOException("Unknown protocol"); //$NON-NLS-1$
	}

}