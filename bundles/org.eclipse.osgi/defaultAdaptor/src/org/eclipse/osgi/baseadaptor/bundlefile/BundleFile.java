/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.baseadaptor.bundlefile;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.framework.internal.core.FrameworkProperties;
import org.eclipse.osgi.framework.internal.protocol.bundleresource.Handler;
import org.eclipse.osgi.framework.util.SecureAction;
import org.eclipse.osgi.util.ManifestElement;

/**
 * The BundleFile API is used by Adaptors to read resources out of an 
 * installed Bundle in the Framework.
 * <p>
 * Clients may extend this class.
 * </p>
 * @since 3.2
 */
abstract public class BundleFile {
	protected static final String PROP_SETPERMS_CMD = "osgi.filepermissions.command"; //$NON-NLS-1$
	static final SecureAction secureAction = new SecureAction();
	/**
	 * The File object for this BundleFile.
	 */
	protected File basefile;
	private int mruIndex = -1;

	/**
	 * Default constructor
	 *
	 */
	public BundleFile() {
		// do nothing
	}

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
	abstract public Enumeration getEntryPaths(String path);

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
	 * @param hostBundleID the host bundle ID
	 * @return a URL to access the contents of the entry specified by the path
	 */
	public URL getResourceURL(String path, long hostBundleID) {
		return getResourceURL(path, hostBundleID, 0);
	}

	/**
	 * Returns a URL to access the contents of the entry specified by the path
	 * @param path the path to the resource
	 * @param hostBundleID the host bundle ID
	 * @param index the resource index
	 * @return a URL to access the contents of the entry specified by the path
	 */
	public URL getResourceURL(String path, long hostBundleID, int index) {
		BundleEntry bundleEntry = getEntry(path);
		if (bundleEntry == null)
			return null;
		if (path.length() == 0 || path.charAt(0) != '/')
			path = '/' + path;
		try {
			//use the constant string for the protocol to prevent duplication
			return secureAction.getURL(Constants.OSGI_RESOURCE_URL_PROTOCOL, Long.toString(hostBundleID), index, path, new Handler(bundleEntry));
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

	protected static void setPermissions(File nested) {
		String commandProp = FrameworkProperties.getProperty(PROP_SETPERMS_CMD);
		if (commandProp == null)
			return;
		String[] temp = ManifestElement.getArrayFromList(commandProp, " "); //$NON-NLS-1$
		ArrayList command = new ArrayList(temp.length + 1);
		boolean foundFullPath = false;
		for (int i = 0; i < temp.length; i++) {
			if ("[fullpath]".equals(temp[i])) { //$NON-NLS-1$
				command.add(nested.getAbsolutePath());
				foundFullPath = true;
			}
			else 
				command.add(temp[i]);
		}
		if (!foundFullPath)
			command.add(nested.getAbsolutePath());
		try {
			Runtime.getRuntime().exec((String[]) command.toArray(new String[command.size()])).waitFor();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
