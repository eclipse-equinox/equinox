/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
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

/**
 * A utility class for manipulating URIs. This class works around some of the
 * undesirable behavior of the {@link java.net.URI} class, and provides additional
 * path manipulation methods that are not available on the URI class.
 * 
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @since org.eclipse.equinox.common 3.5
 */
public final class URIUtil {

	private static final String JAR_SUFFIX = "!/"; //$NON-NLS-1$
	private static final String UNC_PREFIX = "//"; //$NON-NLS-1$
	private static final String SCHEME_FILE = "file"; //$NON-NLS-1$
	private static final String SCHEME_JAR = "jar"; //$NON-NLS-1$

	private static final boolean decodeResolved;
	static {
		decodeResolved = URI.create("foo:/a%20b/").resolve("c").getSchemeSpecificPart().indexOf('%') > 0; //$NON-NLS-1$ //$NON-NLS-2$
	}

	private URIUtil() {
		// prevent instantiation
	}

	/**
	 * Returns a new URI with all the same components as the given base URI,
	 * but with a path component created by appending the given extension to the
	 * base URI's path.
	 * <p>
	 * The important difference between this method
	 * and {@link java.net.URI#resolve(String)} is in the treatment of the final segment.
	 * The URI resolve method drops the last segment if there is no trailing slash as
	 * specified in section 5.2 of RFC 2396. This leads to unpredictable behaviour
	 * when working with file: URIs, because the existence of the trailing slash
	 * depends on the existence of a local file on disk. This method operates
	 * like a traditional path append and always preserves all segments of the base path.
	 * 
	 * @param base The base URI to append to
	 * @param extension The unencoded path extension to be added
	 * @return The appended URI
	 */
	public static URI append(URI base, String extension) {
		try {
			String path = base.getPath();
			if (path == null)
				return appendOpaque(base, extension);
			//if the base is already a directory then resolve will just do the right thing
			URI result;
			if (path.endsWith("/")) {//$NON-NLS-1$
				result = base.resolve(new URI(null, null, extension, null));
				if (decodeResolved) {
					//see bug 267219 - Java 1.4 implementation of URI#resolve incorrectly encoded the ssp
					result = new URI(toUnencodedString(result));
				}
			} else {
				path = path + '/' + extension;
				result = new URI(base.getScheme(), base.getUserInfo(), base.getHost(), base.getPort(), path, base.getQuery(), base.getFragment());
			}
			result = result.normalize();
			//Fix UNC paths that are incorrectly normalized by URI#resolve (see Java bug 4723726)
			String resultPath = result.getPath();
			if (isFileURI(base) && path != null && path.startsWith(UNC_PREFIX) && (resultPath == null || !resultPath.startsWith(UNC_PREFIX)))
				result = new URI(result.getScheme(), ensureUNCPath(result.getSchemeSpecificPart()), result.getFragment());
			return result;
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
	 * Ensures the given path string starts with exactly four leading slashes.
	 */
	private static String ensureUNCPath(String path) {
		int len = path.length();
		StringBuffer result = new StringBuffer(len);
		for (int i = 0; i < 4; i++) {
			//	if we have hit the first non-slash character, add another leading slash
			if (i >= len || result.length() > 0 || path.charAt(i) != '/')
				result.append('/');
		}
		result.append(path);
		return result.toString();
	}

	/**
	 * Returns a URI corresponding to the given unencoded string. This method
	 * will take care of encoding any characters that must be encoded according
	 * to the URI specification. This method must not be called with a string that
	 * already contains an encoded URI, since this will result in the URI escape character ('%')
	 * being escaped itself.
	 * 
	 * @param uriString An unencoded URI string
	 * @return A URI corresponding to the given string
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
		if (scheme != null && scheme.equals(SCHEME_FILE)) {
			//handle relative URI string with scheme (produced by java.net.URL)
			File file = new File(ssp);
			if (file.isAbsolute())
				return file.toURI();
			if (File.separatorChar != '/')
				ssp = ssp.replace(File.separatorChar, '/');
			//relative URIs have a null scheme.
			if (!ssp.startsWith("/"))//$NON-NLS-1$
				scheme = null;
		}
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
	 * part as a path and returns the last segment. Returns <code>null</code> for
	 * a hierarchical URI with an empty path, and for opaque URIs whose scheme-specific
	 * part cannot be interpreted as a path.
	 */
	public static String lastSegment(URI location) {
		String path = location.getPath();
		if (path == null)
			return new Path(location.getSchemeSpecificPart()).lastSegment();
		return new Path(path).lastSegment();
	}

	/**
	 * Returns a new URI which is the same as this URI but with the file extension removed 
	 * from the path part.  If this URI does not have an extension, this path is returned.
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

	/**
	 * Returns true if the two URIs are equal. URIs are considered equal if
	 * {@link URI#equals(Object)} returns true, if the string representation
	 * of the URIs is equal, or if they URIs are represent the same local file.
	 * @param uri1 The first URI to compare
	 * @param uri2 The second URI to compare
	 * @return <code>true</code> if the URIs are the same, and <code>false</code> otherwise.
	 */
	public static boolean sameURI(URI uri1, URI uri2) {
		if (uri1 == uri2)
			return true;
		if (uri1 == null || uri2 == null)
			return false;

		if (uri1.equals(uri2))
			return true;

		if (sameString(uri1.getScheme(), uri2.getScheme()) && sameString(uri1.getSchemeSpecificPart(), uri2.getSchemeSpecificPart()) && sameString(uri1.getFragment(), uri2.getFragment()))
			return true;

		if (uri1.isAbsolute() != uri2.isAbsolute())
			return false;

		// check if we have two local file references that are case variants
		File file1 = toFile(uri1);
		return file1 == null ? false : file1.equals(toFile(uri2));
	}

	private static boolean sameString(String s1, String s2) {
		return (s1 == s2) || s1 != null && s1.equals(s2);
	}

	/**
	 * Returns the URI as a local file, or <code>null</code> if the given
	 * URI does not represent a local file.
	 * @param uri The URI to return the file for
	 * @return The local file corresponding to the given URI, or <code>null</code>
	 */
	public static File toFile(URI uri) {
		if (!isFileURI(uri))
			return null;
		//assume all illegal characters have been properly encoded, so use URI class to unencode
		return new File(uri.getSchemeSpecificPart());
	}

	/**
	 * Returns a Java ARchive (JAR) URI for an entry in a jar or zip file.  The given input URI 
	 * should represent a zip or jar file, but this method will not check for existence or 
	 * validity of a file at the given URI.
	 * <p>
	 * The entry path parameter can optionally be used to obtain the URI of an entry
	 * in a zip or jar file. If an entry path of <code>null</code> is provided, the resulting
	 * URI will represent the jar file itself.
	 * </p>
	 * 
	 * @param uri The URI of a zip or jar file
	 * @param entryPath The path of a file inside the jar, or <code>null</code> to
	 * obtain the URI for the jar file itself.
	 * @return A URI with the "jar" scheme for the given input URI and entry path
	 * @see JarURLConnection
	 */
	public static URI toJarURI(URI uri, IPath entryPath) {
		try {
			if (entryPath == null)
				entryPath = Path.EMPTY;
			//must deconstruct the input URI to obtain unencoded strings, and then pass to URI constructor that will encode the entry path
			return new URI(SCHEME_JAR, uri.getScheme() + ':' + uri.getSchemeSpecificPart() + JAR_SUFFIX + entryPath.toString(), null);
		} catch (URISyntaxException e) {
			//should never happen
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns the URL as a URI. This method will handle URLs that are
	 * not properly encoded (for example they contain unencoded space characters).
	 * 
	 * @param url The URL to convert into a URI
	 * @return A URI representing the given URL
	 */
	public static URI toURI(URL url) throws URISyntaxException {
		//URL behaves differently across platforms so for file: URLs we parse from string form
		if (SCHEME_FILE.equals(url.getProtocol())) {
			String pathString = url.toExternalForm().substring(5);
			//ensure there is a leading slash to handle common malformed URLs such as file:c:/tmp
			if (pathString.indexOf('/') != 0)
				pathString = '/' + pathString;
			else if (pathString.startsWith(UNC_PREFIX) && !pathString.startsWith(UNC_PREFIX, 2)) {
				//URL encodes UNC path with two slashes, but URI uses four (see bug 207103)
				pathString = ensureUNCPath(pathString);
			}
			return new URI(SCHEME_FILE, null, pathString, null);
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
	 * Returns an absolute URI that is created by appending the given relative URI to 
	 * the given base.  If the <tt>relative</tt> URI is already absolute it is simply returned.
	 * <p>
	 * This method is guaranteed to be the inverse of {@link #makeRelative(URI, URI)}.
	 * That is, if R = makeRelative(O, B), then makeAbsolute(R, B), will return the original
	 * URI O.
	 * 
	 * @param relative the relative URI
	 * @param baseURI the base URI
	 * @return an absolute URI
	 */
	public static URI makeAbsolute(URI relative, URI baseURI) {
		if (relative.isAbsolute())
			return relative;
		return append(baseURI, toUnencodedString(relative));
	}

	/**
	 * Returns a URI equivalent to the given original URI, but relative to the given base 
	 * URI if possible.
	 * <p>
	 * This method is equivalent to {@link java.net.URI#relativize}, except for its
	 * handling of file URIs. For file URIs, this method handles file system path devices.
	 * If the URIs are not on the same device, then the original URI is returned.
	 * 
	 * @param original the original URI
	 * @param baseURI the base URI
	 * @return a relative URI
	 */
	public static URI makeRelative(URI original, URI baseURI) {
		// for non-local URIs just use the built in relativize method
		if (!SCHEME_FILE.equals(original.getScheme()) || !SCHEME_FILE.equals(baseURI.getScheme()))
			return baseURI.relativize(original);

		IPath originalPath = new Path(original.getSchemeSpecificPart());
		IPath basePath = new Path(baseURI.getSchemeSpecificPart());

		// make sure we have an absolute path to start
		if (!basePath.isAbsolute())
			return original;
		IPath relativePath = originalPath.makeRelativeTo(basePath);
		//if we could not make it relative, just return the original URI
		if (relativePath == originalPath)
			return original;
		try {
			return new URI(null, null, relativePath.toString(), original.getFragment());
		} catch (URISyntaxException e) {
			//cannot make a relative path, just return the original
			return original;
		}
	}
}