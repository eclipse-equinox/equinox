/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime;

import java.io.File;
import java.net.*;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 * A utility class for manipulating URIs. This class works around some of the
 * broken behavior of the java.net.URI class.
 */
public class URIUtil {

	private static final String SCHEME_FILE = "file"; //$NON-NLS-1$

	/**
	 * Appends the given extension to the path of the give base URI and returns
	 * the corresponding new path.
	 * @param base The base URI to append to
	 * @param extension The path extension to be added
	 * @return The appended URI
	 */
	public static URI append(URI base, String extension) {
		try {
			String path = base.getPath();
			if (path == null)
				return appendOpaque(base, extension);
			//if the base is already a directory then resolve will just do the right thing
			if (path.endsWith("/")) //$NON-NLS-1$
				return base.resolve(extension);
			path = path + "/" + extension; //$NON-NLS-1$
			return new URI(base.getScheme(), base.getUserInfo(), base.getHost(), base.getPort(), path, base.getQuery(), base.getFragment());
		} catch (URISyntaxException e) {
			//shouldn't happen because we started from a valid URI
			throw new RuntimeException(e);
		}
	}

	/**
	 * Special case of appending to an opaque URI. Since opaque URIs
	 * have no path segment the best we can do is append to the scheme-specific part
	 */
	private static URI appendOpaque(URI base, String extension) throws URISyntaxException {
		String ssp = base.getSchemeSpecificPart();
		if (ssp.endsWith("/")) //$NON-NLS-1$
			ssp += extension;
		else
			ssp = ssp + "/" + extension; //$NON-NLS-1$
		return new URI(base.getScheme(), ssp, base.getFragment());
	}

	/**
	 * Returns a URI corresponding to the given unencoded string.
	 * @throws URISyntaxException If the string cannot be formed into a valid URI
	 */
	public static URI fromString(String uriString) throws URISyntaxException {
		int colon = uriString.indexOf(':');
		int hash = uriString.lastIndexOf('#');
		boolean noHash = hash < 0;
		if (noHash)
			hash = uriString.length();
		String scheme = colon < 0 ? null : uriString.substring(0, colon);
		String ssp = uriString.substring(colon + 1, hash);
		String fragment = noHash ? null : uriString.substring(hash + 1);
		//use java.io.File for constructing file: URIs
		if (scheme != null && scheme.equals(SCHEME_FILE))
			return new File(uriString.substring(5)).toURI();
		return new URI(scheme, ssp, fragment);
	}

	/**
	 * Returns whether the given URI refers to a local file system URI.
	 * @param uri The URI to check
	 * @return <code>true</code> if the URI is a local file system location, and <code>false</code> otherwise
	 */
	public static boolean isFileURI(URI uri) {
		return SCHEME_FILE.equalsIgnoreCase(uri.getScheme());
	}

	/**
	 * Returns the last segment of the given URI. For a hierarchical URL this returns
	 * the last segment of the path. For opaque URIs this treats the scheme-specific
	 * part as a path and returns the last segment. Returns null if the URI has no
	 * path or the path is empty.
	 */
	public static String lastSegment(URI location) {
		String path = location.getPath();
		if (path == null)
			return new Path(location.getSchemeSpecificPart()).lastSegment();
		return new Path(path).lastSegment();
	}

	/**
	 * Returns a new URI which is the same as this URI but with
	 * the file extension removed from the path part.  If this URI does not have an 
	 * extension, this path is returned.
	 * <p>
	 * The file extension portion is defined as the string
	 * following the last period (".") character in the last segment.
	 * If there is no period in the last segment, the path has no
	 * file extension portion. If the last segment ends in a period,
	 * the file extension portion is the empty string.
	 * </p>
	 *
	 * @return the new URI
	 */
	public static URI removeFileExtension(URI uri) {
		String lastSegment = lastSegment(uri);
		if (lastSegment == null)
			return uri;
		int lastIndex = lastSegment.lastIndexOf('.');
		if (lastIndex == -1)
			return uri;
		String uriString = uri.toString();
		lastIndex = uriString.lastIndexOf('.');
		uriString = uriString.substring(0, lastIndex);
		return URI.create(uriString);
	}

	/*
	 * Compares two URI for equality.
	 * Return false if one of them is null
	 */
	public static boolean sameURI(URI url1, URI url2) {
		if (url1 == url2)
			return true;
		if (url1 == null || url2 == null)
			return false;
		if (url1.equals(url2))
			return true;

		// check if we have two local file references that are case variants
		File file1 = toFile(url1);
		return file1 == null ? false : file1.equals(toFile(url2));
	}

	/**
	 * Returns the URI as a local file, or <code>null</code> if the given
	 * URI does not represent a local file.
	 * @param uri The URI to return the file for
	 * @return The local file corresponding to the given URI, or <code>null</code>
	 */
	public static File toFile(URI uri) {
		try {
			if (!SCHEME_FILE.equalsIgnoreCase(uri.getScheme()))
				return null;
			//assume all illegal characters have been properly encoded, so use URI class to unencode
			return new File(uri);
		} catch (IllegalArgumentException e) {
			//File constructor does not support non-hierarchical URI
			String path = uri.getPath();
			//path is null for non-hierarchical URI such as file:c:/tmp
			if (path == null)
				path = uri.getSchemeSpecificPart();
			return new File(path);
		}
	}

	/**
	 * Returns the URL as a URI. This method will handle broken URLs that are
	 * not properly encoded (for example they contain unencoded space characters).
	 */
	public static URI toURI(URL url) throws URISyntaxException {
		//URL behaves differently across platforms so for file: URLs we parse from string form
		if (SCHEME_FILE.equals(url.getProtocol())) {
			String pathString = url.toExternalForm().substring(5);
			//ensure there is a leading slash to handle common malformed URLs such as file:c:/tmp
			if (pathString.indexOf('/') != 0)
				pathString = '/' + pathString;
			return new URI(SCHEME_FILE, pathString, null);
		}
		try {
			return new URI(url.toExternalForm());
		} catch (URISyntaxException e) {
			//try multi-argument URI constructor to perform encoding
			return new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
		}
	}

	/**
	 * Returns a URI as a URL.
	 * 
	 * @throws MalformedURLException 
	 */
	public static URL toURL(URI uri) throws MalformedURLException {
		return new URL(uri.toString());
	}

	/**
	 * Returns a string representation of the given URI that doesn't have illegal
	 * characters encoded. This string is suitable for later passing to {@link #fromString(String)}.
	 * @param uri The URI to convert to string format
	 * @return An unencoded string representation of the URI
	 */
	public static String toUnencodedString(URI uri) {
		StringBuffer result = new StringBuffer();
		String scheme = uri.getScheme();
		if (scheme != null)
			result.append(scheme).append(':');
		//there is always a ssp
		result.append(uri.getSchemeSpecificPart());
		String fragment = uri.getFragment();
		if (fragment != null)
			result.append('#').append(fragment);
		return result.toString();
	}

	/**
	 * Return a URI which is absolute to the given base. Return the original URI
	 * if it is already absolute or if either URI has a non-local scheme.
	 * @param original the relative URI
	 * @param baseURI the base URI
	 * @return an absolute URI
	 */
	public static URI makeAbsolute(URI original, URI baseURI) {
		// we only calculate the result if both URIs are local.
		if (!SCHEME_FILE.equals(baseURI.getScheme()))
			return original;
		String scheme = original.getScheme();
		if (scheme != null && !SCHEME_FILE.equals(scheme))
			return original;
		File file = scheme == null ? new File(original.toString()) : toFile(original);
		File base = toFile(baseURI);

		// make sure we have relative path to start
		if (file.isAbsolute())
			return original;

		IPath root = new Path(base.getAbsolutePath());
		try {
			String path = root.addTrailingSeparator().append(file.getPath().replace(':', '}')).toOSString().replace('}', ':');
			return scheme == null ? fromString(SCHEME_FILE + ':' + path) : fromString(scheme + ':' + path);
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return original;
		}
	}

	/**
	 * Return a URI which is considered to be relative to the given base URI.
	 * Return the original URI if either URI has a non-local scheme.
	 * @param original the original URI
	 * @param baseURI the base URI
	 * @return a relative URI
	 */
	public static URI makeRelative(URI original, URI baseURI) {
		// we only calculate the result if both URIs are local.
		if (!SCHEME_FILE.equals(original.getScheme()) || !SCHEME_FILE.equals(baseURI.getScheme()))
			return original;

		File file = toFile(original);
		File base = toFile(baseURI);

		// make sure we have an absolute path to start
		if (!file.isAbsolute())
			return original;

		IPath one = new Path(file.getPath());
		IPath two = new Path(base.getPath());
		String deviceOne = one.getDevice();
		String deviceTwo = two.getDevice();
		// do checking here because we want to return the exact string we got initially if
		// we are unable to make it relative.
		if (deviceOne != deviceTwo && (deviceOne == null || !deviceOne.equalsIgnoreCase(two.getDevice())))
			return original;

		try {
			return fromString(makeRelative(one, two).toString());
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return original;
		}
	}

	/*
	 * Make the given path relative to the specified base.
	 */
	private static IPath makeRelative(IPath original, IPath base) {
		int i = base.matchingFirstSegments(original);
		if (i == 0)
			return original.segmentCount() == 0 ? Path.EMPTY : original;
		IPath result = Path.EMPTY;
		for (int j = 0; j < (base.segmentCount() - i); j++)
			result = result.append(".."); //$NON-NLS-1$
		if (i == original.segmentCount())
			return new Path("."); //$NON-NLS-1$
		result = result.append(original.setDevice(null).removeFirstSegments(i));
		return result;
	}

}
