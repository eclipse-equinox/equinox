/*******************************************************************************
 * Copyright (c) 2004, 2021 IBM Corporation and others.
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
 *     Hannes Wellmann - Bug 576643: Clean up and unify Bundle resource classes
 *     Hannes Wellmann - Bug 576644: Implement BundleReference for BundleURLConnection
 *******************************************************************************/

package org.eclipse.osgi.storage.url;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Objects;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.container.ModuleContainer;
import org.eclipse.osgi.internal.messages.Msg;
import org.eclipse.osgi.storage.bundlefile.BundleEntry;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.AdminPermission;
import org.osgi.framework.Bundle;

/**
 * URLStreamHandler the bundleentry and bundleresource protocols.
 */

public abstract class BundleResourceHandler extends URLStreamHandler {
	// Bundle resource URL protocol
	public static final String OSGI_RESOURCE_URL_PROTOCOL = "bundleresource"; //$NON-NLS-1$
	// Bundle entry URL protocol
	public static final String OSGI_ENTRY_URL_PROTOCOL = "bundleentry"; //$NON-NLS-1$

	public static final String SECURITY_CHECKED = "SECURITY_CHECKED"; //$NON-NLS-1$
	public static final String SECURITY_UNCHECKED = "SECURITY_UNCHECKED"; //$NON-NLS-1$
	public static final String BID_FWKID_SEPARATOR = ".fwk"; //$NON-NLS-1$
	protected final ModuleContainer container;
	protected BundleEntry bundleEntry;

	protected BundleResourceHandler(ModuleContainer container, BundleEntry bundleEntry) {
		this.container = container;
		this.bundleEntry = bundleEntry;
	}

	/**
	 * Parse reference URL.
	 */
	@Override
	protected void parseURL(URL url, String str, int start, int end) {
		if (end < start)
			return;
		if (url.getPath() != null)
			// A call to a URL constructor has been made that uses an authorized URL as its
			// context.
			// Null out bundleEntry because it will not be valid for the new path
			bundleEntry = null;
		String spec = ""; //$NON-NLS-1$
		if (start < end)
			spec = str.substring(start, end);
		end -= start;
		// Default is to use path and bundleId from context
		String path = url.getPath();
		String host = url.getHost();
		int resIndex = url.getPort();
		if (resIndex < 0) // -1 indicates port was not set; must default to 0
			resIndex = 0;
		int pathIdx = 0;
		if (spec.startsWith("//")) { //$NON-NLS-1$
			int bundleIdIdx = 2;
			pathIdx = spec.indexOf('/', bundleIdIdx);
			if (pathIdx == -1) {
				pathIdx = end;
				// Use default
				path = ""; //$NON-NLS-1$
			}
			int bundleIdEnd = spec.indexOf(':', bundleIdIdx);
			if (bundleIdEnd > pathIdx || bundleIdEnd == -1)
				bundleIdEnd = pathIdx;
			if (bundleIdEnd < pathIdx - 1)
				try {
					resIndex = Integer.parseInt(spec.substring(bundleIdEnd + 1, pathIdx));
				} catch (NumberFormatException e) {
					// do nothing; results in resIndex == 0
				}
			host = spec.substring(bundleIdIdx, bundleIdEnd);
		}
		if (pathIdx < end && spec.charAt(pathIdx) == '/')
			path = spec.substring(pathIdx, end);
		else if (end > pathIdx) {
			if (path == null || path.equals("")) //$NON-NLS-1$
				path = "/"; //$NON-NLS-1$
			int last = path.lastIndexOf('/') + 1;
			if (last == 0)
				path = spec.substring(pathIdx, end);
			else
				path = path.substring(0, last) + spec.substring(pathIdx, end);
		}
		if (path == null)
			path = ""; //$NON-NLS-1$
		// modify path if there's any relative references
		// see RFC2396 Section 5.2
		// Note: For ".." references above the root the approach taken is removing them
		// from the resolved path
		if (path.endsWith("/.") || path.endsWith("/..")) //$NON-NLS-1$ //$NON-NLS-2$
			path = path + '/';
		int dotIndex;
		while ((dotIndex = path.indexOf("/./")) >= 0) //$NON-NLS-1$
			path = path.substring(0, dotIndex + 1) + path.substring(dotIndex + 3);
		while ((dotIndex = path.indexOf("/../")) >= 0) { //$NON-NLS-1$
			if (dotIndex != 0)
				path = path.substring(0, path.lastIndexOf('/', dotIndex - 1)) + path.substring(dotIndex + 3);
			else
				path = path.substring(dotIndex + 3);
		}
		while ((dotIndex = path.indexOf("//")) >= 0) //$NON-NLS-1$
			path = path.substring(0, dotIndex + 1) + path.substring(dotIndex + 2);

		// Check the permission of the caller to see if they
		// are allowed access to the resource.
		String authorized = SECURITY_UNCHECKED;
		long bundleId = parseBundleIDFromURLHost(host);
		Module module = getModule(bundleId);
		if (checkAuthorization(module))
			authorized = SECURITY_CHECKED;
		// Always force the use of the hash from the adaptor
		host = createURLHostForBundleID(container, bundleId);
		// Setting the authority portion of the URL to SECURITY_ATHORIZED
		// ensures that this URL was created by using this parseURL
		// method. The openConnection method will only open URLs
		// that have the authority set to this.
		setURL(url, url.getProtocol(), host, resIndex, authorized, null, path, null, url.getRef());
	}

	private Module getModule(long id) {
		return container.getModule(id);
	}

	/**
	 * Establishes a connection to the resource specified by <code>URL</code>. Since
	 * different protocols may have unique ways of connecting, it must be overridden
	 * by the subclass.
	 *
	 * @return java.net.URLConnection
	 * @param url java.net.URL
	 *
	 * @exception IOException thrown if an IO error occurs during connection
	 *                        establishment
	 */
	@Override
	protected URLConnection openConnection(URL url) throws IOException {
		if (bundleEntry != null) { // if the bundleEntry is not null then return quick
			return new BundleURLConnection(url, container, bundleEntry);
		}
		String host = url.getHost();
		if (host == null) {
			throw new IOException(NLS.bind(Msg.URL_NO_BUNDLE_ID, url.toExternalForm()));
		}
		long bundleID;
		try {
			bundleID = parseBundleIDFromURLHost(host);
		} catch (NumberFormatException nfe) {
			throw (MalformedURLException) new MalformedURLException(NLS.bind(Msg.URL_INVALID_BUNDLE_ID, host))
					.initCause(nfe);
		}
		Module module = getModule(bundleID);
		if (module == null) {
			throw new IOException(NLS.bind(Msg.URL_NO_BUNDLE_FOUND, url.toExternalForm()));
		}
		// check to make sure that this URL was created using the
		// parseURL method. This ensures the security check was done
		// at URL construction.
		if (!url.getAuthority().equals(SECURITY_CHECKED)) {
			// No admin security check was made better check now.
			checkAuthorization(module);
		}
		return new BundleURLConnection(url, container, findBundleEntry(url, module));
	}

	/**
	 * Finds the bundle entry for this protocal. This is handled differently for
	 * Bundle.gerResource() and Bundle.getEntry() because getResource uses the
	 * bundle classloader and getEntry only used the base bundle file.
	 * 
	 * @param url    The URL to find the entry for.
	 * @param module the module to find the entry for.
	 * @return the bundle entry
	 */
	abstract protected BundleEntry findBundleEntry(URL url, Module module) throws IOException;

	/**
	 * Converts a bundle URL to a String.
	 *
	 * @param url the URL.
	 * @return a string representation of the URL.
	 */
	@Override
	protected String toExternalForm(URL url) {
		StringBuilder result = new StringBuilder(url.getProtocol());
		result.append("://"); //$NON-NLS-1$

		String host = url.getHost();
		if (host != null && host.length() > 0) {
			result.append(host);
		}
		int index = url.getPort();
		if (index > 0) {
			result.append(':').append(index);
		}
		String path = url.getPath();
		if (path != null) {
			if (path.length() > 0 && path.charAt(0) != '/') { // if name doesn't have a leading slash
				result.append("/"); //$NON-NLS-1$
			}
			result.append(path);
		}
		String ref = url.getRef();
		if (ref != null && ref.length() > 0) {
			result.append('#').append(ref);
		}
		return result.toString();
	}

	@Override
	protected int hashCode(URL url) {
		int hash = 0;
		String protocol = url.getProtocol();
		if (protocol != null)
			hash += protocol.hashCode();

		String host = url.getHost();
		if (host != null)
			hash += host.hashCode();

		hash += url.getPort();

		String path = url.getPath();
		if (path != null)
			hash += path.hashCode();

		hash += container.hashCode();
		return hash;
	}

	@Override
	protected boolean equals(URL url1, URL url2) {
		return sameFile(url1, url2);
	}

	@Override
	protected synchronized InetAddress getHostAddress(URL url) {
		return null;
	}

	@Override
	protected boolean hostsEqual(URL url1, URL url2) {
		return equalsIgnoreCase(url1.getHost(), url2.getHost());
	}

	@Override
	protected boolean sameFile(URL url1, URL url2) {
		// do a hashcode test to allow each handler to check the adaptor first
		if (url1.hashCode() != url2.hashCode())
			return false;
		return equalsIgnoreCase(url1.getProtocol(), url2.getProtocol()) && hostsEqual(url1, url2)
				&& url1.getPort() == url2.getPort() && Objects.equals(url1.getPath(), url2.getPath());
		// note that the authority is not checked here because it can be different for
		// two
		// URLs depending on how they were constructed.
	}

	protected boolean checkAuthorization(Module module) {
		SecurityManager sm = System.getSecurityManager();
		if (sm == null)
			return true;
		Bundle moduleBundle = module == null ? null : module.getBundle();
		if (moduleBundle == null)
			return false;
		sm.checkPermission(new AdminPermission(moduleBundle, AdminPermission.RESOURCE));
		return true;
	}

	private static boolean equalsIgnoreCase(String s1, String s2) {
		return s1 != null ? s1.equalsIgnoreCase(s2) : s2 == null;
	}

	public static String createURLHostForBundleID(ModuleContainer container, long bundleId) {
		return bundleId + BID_FWKID_SEPARATOR + container.hashCode();
	}

	static long parseBundleIDFromURLHost(String host) {
		int dotIndex = host.indexOf(BID_FWKID_SEPARATOR);
		return (dotIndex >= 0 && dotIndex < host.length() - 1) ? Long.parseLong(host.substring(0, dotIndex))
				: Long.parseLong(host);
	}
}
