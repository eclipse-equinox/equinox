/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
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
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.eclipse.osgi.baseadaptor.BaseData;
import org.eclipse.osgi.framework.debug.Debug;
import org.eclipse.osgi.internal.baseadaptor.AdaptorMsg;
import org.eclipse.osgi.internal.baseadaptor.AdaptorUtil;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.FrameworkEvent;

/**
 * A BundleFile that uses a ZipFile as it base file.
 */
public class ZipBundleFile extends BundleFile {
	protected static MRUBundleFileList mruList = new MRUBundleFileList();

	/**
	 * The bundle data
	 */
	protected BaseData bundledata;
	/**
	 * The zip file
	 */
	protected ZipFile zipFile;
	/**
	 * The closed flag
	 */
	protected boolean closed = true;

	/**
	 * Constructs a ZipBundle File
	 * @param basefile the base file
	 * @param bundledata the bundle data
	 * @throws IOException
	 */
	public ZipBundleFile(File basefile, BaseData bundledata) throws IOException {
		super(basefile);
		if (!BundleFile.secureAction.exists(basefile))
			throw new IOException(NLS.bind(AdaptorMsg.ADAPTER_FILEEXIST_EXCEPTION, basefile));
		this.bundledata = bundledata;
		this.closed = true;
	}

	/**
	 * Checks if the zip file is open
	 * @return true if the zip file is open
	 */
	protected boolean checkedOpen() {
		try {
			return getZipFile() != null;
		} catch (IOException e) {

			bundledata.getAdaptor().getEventPublisher().publishFrameworkEvent(FrameworkEvent.ERROR, bundledata.getBundle(), e);
			return false;
		}
	}

	/**
	 * Opens the ZipFile for this bundle file
	 * @return an open ZipFile for this bundle file
	 * @throws IOException
	 */
	protected ZipFile basicOpen() throws IOException {
		return BundleFile.secureAction.getZipFile(this.basefile);
	}

	/**
	 * Returns an open ZipFile for this bundle file.  If an open
	 * ZipFile does not exist then a new one is created and
	 * returned.
	 * @return an open ZipFile for this bundle
	 * @throws IOException
	 */
	protected ZipFile getZipFile() throws IOException {
		if (closed) {
			mruList.add(this);
			zipFile = basicOpen();
			closed = false;
		} else
			mruList.use(this);
		return zipFile;
	}

	protected ZipEntry getZipEntry(String path) {
		if (path.length() > 0 && path.charAt(0) == '/')
			path = path.substring(1);
		ZipEntry entry = zipFile.getEntry(path);
		if (entry != null && entry.getSize() == 0 && !entry.isDirectory()) {
			// work around the directory bug see bug 83542
			ZipEntry dirEntry = zipFile.getEntry(path + '/');
			if (dirEntry != null)
				entry = dirEntry;
		}
		return entry;
	}

	/**
	 * Extracts a directory and all sub content to disk
	 * @param dirName the directory name to extract
	 * @return the File used to extract the content to.  A value
	 * of <code>null</code> is returned if the directory to extract does 
	 * not exist or if content extraction is not supported.
	 */
	protected File extractDirectory(String dirName) {
		if (!checkedOpen())
			return null;
		Enumeration entries = zipFile.entries();
		while (entries.hasMoreElements()) {
			String entryPath = ((ZipEntry) entries.nextElement()).getName();
			if (entryPath.startsWith(dirName) && !entryPath.endsWith("/")) //$NON-NLS-1$
				getFile(entryPath, false);
		}
		return getExtractFile(dirName);
	}

	protected File getExtractFile(String entryName) {
		String path = ".cp"; /* put all these entries in this subdir *///$NON-NLS-1$
		String name = entryName.replace('/', File.separatorChar);
		if ((name.length() > 1) && (name.charAt(0) == File.separatorChar)) /* if name has a leading slash */
			path = path.concat(name);
		else
			path = path + File.separator + name;
		return bundledata.getExtractFile(path);
	}

	public File getFile(String entry, boolean nativeCode) {
		if (!checkedOpen())
			return null;
		ZipEntry zipEntry = getZipEntry(entry);
		if (zipEntry == null)
			return null;

		try {
			File nested = getExtractFile(zipEntry.getName());
			if (nested != null) {
				if (nested.exists()) {
					/* the entry is already cached */
					if (Debug.DEBUG && Debug.DEBUG_GENERAL)
						Debug.println("File already present: " + nested.getPath()); //$NON-NLS-1$
				} else {
					if (zipEntry.getName().endsWith("/")) { //$NON-NLS-1$
						if (!nested.mkdirs()) {
							if (Debug.DEBUG && Debug.DEBUG_GENERAL)
								Debug.println("Unable to create directory: " + nested.getPath()); //$NON-NLS-1$
							throw new IOException(NLS.bind(AdaptorMsg.ADAPTOR_DIRECTORY_CREATE_EXCEPTION, nested.getAbsolutePath()));
						}
						extractDirectory(zipEntry.getName());
					} else {
						InputStream in = zipFile.getInputStream(zipEntry);
						if (in == null)
							return null;
						/* the entry has not been cached */
						if (Debug.DEBUG && Debug.DEBUG_GENERAL)
							Debug.println("Creating file: " + nested.getPath()); //$NON-NLS-1$
						/* create the necessary directories */
						File dir = new File(nested.getParent());
						if (!dir.exists() && !dir.mkdirs()) {
							if (Debug.DEBUG && Debug.DEBUG_GENERAL)
								Debug.println("Unable to create directory: " + dir.getPath()); //$NON-NLS-1$
							throw new IOException(NLS.bind(AdaptorMsg.ADAPTOR_DIRECTORY_CREATE_EXCEPTION, dir.getAbsolutePath()));
						}
						/* copy the entry to the cache */
						AdaptorUtil.readFile(in, nested);
						if (nativeCode)
							setPermissions(nested);
					}
				}

				return nested;
			}
		} catch (IOException e) {
			if (Debug.DEBUG && Debug.DEBUG_GENERAL)
				Debug.printStackTrace(e);
		}
		return null;
	}

	public boolean containsDir(String dir) {
		if (!checkedOpen())
			return false;
		if (dir == null)
			return false;

		if (dir.length() == 0)
			return true;

		if (dir.charAt(0) == '/') {
			if (dir.length() == 1)
				return true;
			dir = dir.substring(1);
		}

		if (dir.length() > 0 && dir.charAt(dir.length() - 1) != '/')
			dir = dir + '/';

		Enumeration entries = zipFile.entries();
		ZipEntry zipEntry;
		String entryPath;
		while (entries.hasMoreElements()) {
			zipEntry = (ZipEntry) entries.nextElement();
			entryPath = zipEntry.getName();
			if (entryPath.startsWith(dir)) {
				return true;
			}
		}
		return false;
	}

	public BundleEntry getEntry(String path) {
		if (!checkedOpen())
			return null;
		ZipEntry zipEntry = getZipEntry(path);
		if (zipEntry == null) {
			if (path.length() == 0 || path.charAt(path.length() - 1) == '/') {
				// this is a directory request lets see if any entries exist in this directory
				if (containsDir(path))
					return new DirZipBundleEntry(this, path);
			}
			return null;
		}

		return new ZipBundleEntry(zipEntry, this);

	}

	public Enumeration getEntryPaths(String path) {
		if (!checkedOpen())
			return null;
		if (path == null)
			throw new NullPointerException();

		if (path.length() > 0 && path.charAt(0) == '/')
			path = path.substring(1);
		if (path.length() > 0 && path.charAt(path.length() - 1) != '/')
			path = new StringBuffer(path).append("/").toString(); //$NON-NLS-1$

		Vector vEntries = new Vector();
		Enumeration entries = zipFile.entries();
		while (entries.hasMoreElements()) {
			ZipEntry zipEntry = (ZipEntry) entries.nextElement();
			String entryPath = zipEntry.getName();
			if (entryPath.startsWith(path)) {
				if (path.length() < entryPath.length()) {
					if (entryPath.lastIndexOf('/') < path.length()) {
						vEntries.add(entryPath);
					} else {
						entryPath = entryPath.substring(path.length());
						int slash = entryPath.indexOf('/');
						entryPath = path + entryPath.substring(0, slash + 1);
						if (!vEntries.contains(entryPath))
							vEntries.add(entryPath);
					}
				}
			}
		}
		return vEntries.size() == 0 ? null : vEntries.elements();
	}

	public void close() throws IOException {
		if (!closed) {
			closed = true;
			zipFile.close();
			mruList.remove(this);
		}
	}

	public void open() throws IOException {
		//do nothing
	}

}