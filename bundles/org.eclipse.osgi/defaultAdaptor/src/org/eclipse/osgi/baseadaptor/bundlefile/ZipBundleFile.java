/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Rob Harrop - SpringSource Inc. (bug 253942)
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
 * @since 3.2
 */
public class ZipBundleFile extends BundleFile {

	private final MRUBundleFileList mruList;
	/**
	 * The bundle data
	 */
	protected BaseData bundledata;
	/**
	 * The zip file
	 */
	protected volatile ZipFile zipFile;
	/**
	 * The closed flag
	 */
	protected volatile boolean closed = true;

	private int referenceCount = 0;

	/**
	 * Constructs a ZipBundle File
	 * @param basefile the base file
	 * @param bundledata the bundle data
	 * @throws IOException
	 */
	public ZipBundleFile(File basefile, BaseData bundledata) throws IOException {
		this(basefile, bundledata, null);
	}

	public ZipBundleFile(File basefile, BaseData bundledata, MRUBundleFileList mruList) throws IOException {
		super(basefile);
		if (!BundleFile.secureAction.exists(basefile))
			throw new IOException(NLS.bind(AdaptorMsg.ADAPTER_FILEEXIST_EXCEPTION, basefile));
		this.bundledata = bundledata;
		this.closed = true;
		this.mruList = mruList;
	}

	/**
	 * Checks if the zip file is open
	 * @return true if the zip file is open
	 */
	protected boolean checkedOpen() {
		try {
			return getZipFile() != null;
		} catch (IOException e) {
			if (bundledata != null)
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
	protected synchronized ZipFile getZipFile() throws IOException {
		if (closed) {
			mruListAdd();
			zipFile = basicOpen();
			closed = false;
		} else
			mruListUse();
		return zipFile;
	}

	/**
	* Returns a ZipEntry for the bundle file. Must be called while synchronizing on this object.
	* This method does not ensure that the ZipFile is opened. Callers may need to call getZipfile() prior to calling this 
	* method.
	* @param path the path to an entry
	* @return a ZipEntry or null if the entry does not exist
	*/
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
	protected synchronized File extractDirectory(String dirName) {
		if (!checkedOpen())
			return null;
		Enumeration<? extends ZipEntry> entries = zipFile.entries();
		while (entries.hasMoreElements()) {
			String entryPath = entries.nextElement().getName();
			if (entryPath.startsWith(dirName) && !entryPath.endsWith("/")) //$NON-NLS-1$
				getFile(entryPath, false);
		}
		return getExtractFile(dirName);
	}

	protected File getExtractFile(String entryName) {
		if (bundledata == null)
			return null;
		String path = ".cp"; /* put all these entries in this subdir *///$NON-NLS-1$
		String name = entryName.replace('/', File.separatorChar);
		if ((name.length() > 1) && (name.charAt(0) == File.separatorChar)) /* if name has a leading slash */
			path = path.concat(name);
		else
			path = path + File.separator + name;
		return bundledata.getExtractFile(path);
	}

	public synchronized File getFile(String entry, boolean nativeCode) {
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
					if (Debug.DEBUG_GENERAL)
						Debug.println("File already present: " + nested.getPath()); //$NON-NLS-1$
					if (nested.isDirectory())
						// must ensure the complete directory is extracted (bug 182585)
						extractDirectory(zipEntry.getName());
				} else {
					if (zipEntry.getName().endsWith("/")) { //$NON-NLS-1$
						if (!nested.mkdirs()) {
							if (Debug.DEBUG_GENERAL)
								Debug.println("Unable to create directory: " + nested.getPath()); //$NON-NLS-1$
							throw new IOException(NLS.bind(AdaptorMsg.ADAPTOR_DIRECTORY_CREATE_EXCEPTION, nested.getAbsolutePath()));
						}
						extractDirectory(zipEntry.getName());
					} else {
						InputStream in = zipFile.getInputStream(zipEntry);
						if (in == null)
							return null;
						/* the entry has not been cached */
						if (Debug.DEBUG_GENERAL)
							Debug.println("Creating file: " + nested.getPath()); //$NON-NLS-1$
						/* create the necessary directories */
						File dir = new File(nested.getParent());
						if (!dir.exists() && !dir.mkdirs()) {
							if (Debug.DEBUG_GENERAL)
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
			if (Debug.DEBUG_GENERAL)
				Debug.printStackTrace(e);
		}
		return null;
	}

	public synchronized boolean containsDir(String dir) {
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

		Enumeration<? extends ZipEntry> entries = zipFile.entries();
		ZipEntry zipEntry;
		String entryPath;
		while (entries.hasMoreElements()) {
			zipEntry = entries.nextElement();
			entryPath = zipEntry.getName();
			if (entryPath.startsWith(dir)) {
				return true;
			}
		}
		return false;
	}

	public synchronized BundleEntry getEntry(String path) {
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

	public synchronized Enumeration<String> getEntryPaths(String path) {
		if (!checkedOpen())
			return null;
		if (path == null)
			throw new NullPointerException();

		if (path.length() > 0 && path.charAt(0) == '/')
			path = path.substring(1);
		if (path.length() > 0 && path.charAt(path.length() - 1) != '/')
			path = new StringBuffer(path).append("/").toString(); //$NON-NLS-1$

		List<String> vEntries = new ArrayList<String>();
		Enumeration<? extends ZipEntry> entries = zipFile.entries();
		while (entries.hasMoreElements()) {
			ZipEntry zipEntry = entries.nextElement();
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
		return vEntries.size() == 0 ? null : Collections.enumeration(vEntries);
	}

	public synchronized void close() throws IOException {
		if (!closed) {
			if (referenceCount > 0 && isMruListClosing()) {
				// there are some opened streams to this BundleFile still;
				// wait for them all to close because this is being closed by the MRUBundleFileList
				try {
					wait(1000); // timeout after 1 second
				} catch (InterruptedException e) {
					// do nothing for now ...
				}
				if (referenceCount != 0 || closed)
					// either another thread closed the bundle file or we timed waiting for all the reference inputstreams to close
					// If the referenceCount did not reach zero then this bundle file will remain open until the
					// bundle file is closed explicitly (i.e. bundle is updated/uninstalled or framework is shutdown)
					return;

			}
			closed = true;
			zipFile.close();
			mruListRemove();
		}
	}

	private boolean isMruListClosing() {
		return this.mruList != null && this.mruList.isClosing(this);
	}

	boolean isMruEnabled() {
		return this.mruList != null && this.mruList.isEnabled();
	}

	private void mruListRemove() {
		if (this.mruList != null) {
			this.mruList.remove(this);
		}
	}

	private void mruListUse() {
		if (this.mruList != null) {
			mruList.use(this);
		}
	}

	private void mruListAdd() {
		if (this.mruList != null) {
			mruList.add(this);
		}
	}

	public void open() {
		//do nothing
	}

	synchronized void incrementReference() {
		referenceCount += 1;
	}

	synchronized void decrementReference() {
		referenceCount = Math.max(0, referenceCount - 1);
		// only notify if the referenceCount is zero.
		if (referenceCount == 0)
			notify();
	}
}
