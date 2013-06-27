/*******************************************************************************
 * Copyright (c) 2004, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.storage.bundlefile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.util.Enumeration;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.framework.util.SecureAction;
import org.eclipse.osgi.storage.url.BundleResourceHandler;
import org.eclipse.osgi.storage.url.bundleresource.Handler;

/**
 * The BundleFile API is used by Adaptors to read resources out of an 
 * installed Bundle in the Framework.
 * <p>
 * Clients may extend this class.
 * </p>
 * @since 3.2
 */
abstract public class BundleFile {
	static final SecureAction secureAction = AccessController.doPrivileged(SecureAction.createSecureAction());
	/**
	 * The File object for this BundleFile.
	 */
	protected File basefile;
	private int mruIndex = -1;

	/**
	 * BundleFile constructor
	 * @param basefile The File object where this BundleFile is 
	 * persistently stored.
	 */
	public BundleFile(File basefile) {
		this.basefile = basefile;
	}

	/**
	 * Returns a File for the bundle entry specified by the path.
	 * If required the content of the bundle entry is extracted into a file
	 * on the file system.
	 * @param path The path to the entry to locate a File for.
	 * @param nativeCode true if the path is native code.
	 * @return A File object to access the contents of the bundle entry.
	 */
	abstract public File getFile(String path, boolean nativeCode);

	/**
	 * Locates a file name in this bundle and returns a BundleEntry object
	 *
	 * @param path path of the entry to locate in the bundle
	 * @return BundleEntry object or null if the file name
	 *         does not exist in the bundle
	 */
	abstract public BundleEntry getEntry(String path);

	/** 
	 * Allows to access the entries of the bundle. 
	 * Since the bundle content is usually a jar, this 
	 * allows to access the jar contents.
	 * 
	 * GetEntryPaths allows to enumerate the content of "path".
	 * If path is a directory, it is equivalent to listing the directory
	 * contents. The returned names are either files or directories 
	 * themselves. If a returned name is a directory, it finishes with a 
	 * slash. If a returned name is a file, it does not finish with a slash.
	 * @param path path of the entry to locate in the bundle
	 * @return an Enumeration of Strings that indicate the paths found or
	 * null if the path does not exist. 
	 */
	abstract public Enumeration<String> getEntryPaths(String path);

	/**
	 * Closes the BundleFile.
	 * @throws IOException if any error occurs.
	 */
	abstract public void close() throws IOException;

	/**
	 * Opens the BundleFiles.
	 * @throws IOException if any error occurs.
	 */
	abstract public void open() throws IOException;

	/**
	 * Determines if any BundleEntries exist in the given directory path.
	 * @param dir The directory path to check existence of.
	 * @return true if the BundleFile contains entries under the given directory path;
	 * false otherwise.
	 */
	abstract public boolean containsDir(String dir);

	/**
	 * Returns a URL to access the contents of the entry specified by the path
	 * @param path the path to the resource
	 * @param hostModule the host module
	 * @param index the resource index
	 * @return a URL to access the contents of the entry specified by the path
	 */
	public URL getResourceURL(String path, Module hostModule, int index) {
		BundleEntry bundleEntry = getEntry(path);
		if (bundleEntry == null)
			return null;
		long hostBundleID = hostModule.getId();
		path = fixTrailingSlash(path, bundleEntry);
		try {
			//use the constant string for the protocol to prevent duplication
			return secureAction.getURL(BundleResourceHandler.OSGI_RESOURCE_URL_PROTOCOL, Long.toString(hostBundleID) + BundleResourceHandler.BID_FWKID_SEPARATOR + Integer.toString(hostModule.getContainer().hashCode()), index, path, new Handler(hostModule.getContainer(), bundleEntry));
		} catch (MalformedURLException e) {
			return null;
		}
	}

	/**
	 * Returns the base file for this BundleFile
	 * @return the base file for this BundleFile
	 */
	public File getBaseFile() {
		return basefile;
	}

	void setMruIndex(int index) {
		mruIndex = index;
	}

	int getMruIndex() {
		return mruIndex;
	}

	public String toString() {
		return String.valueOf(basefile);
	}

	public static String fixTrailingSlash(String path, BundleEntry entry) {
		if (path.length() == 0)
			return "/"; //$NON-NLS-1$
		if (path.charAt(0) != '/')
			path = '/' + path;
		String name = entry.getName();
		if (name.length() == 0)
			return path;
		boolean pathSlash = path.charAt(path.length() - 1) == '/';
		boolean entrySlash = name.length() > 0 && name.charAt(name.length() - 1) == '/';
		if (entrySlash != pathSlash) {
			if (entrySlash)
				path = path + '/';
			else
				path = path.substring(0, path.length() - 1);
		}
		return path;
	}

}
