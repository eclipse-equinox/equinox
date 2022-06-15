/*******************************************************************************
 * Copyright (c) 2005, 2021 IBM Corporation and others.
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
 *     Rob Harrop - SpringSource Inc. (bug 253942)
 *******************************************************************************/

package org.eclipse.osgi.storage.bundlefile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.eclipse.osgi.internal.debug.Debug;
import org.eclipse.osgi.internal.messages.Msg;
import org.eclipse.osgi.storage.BundleInfo;
import org.eclipse.osgi.util.NLS;

/**
 * A BundleFile that uses a ZipFile as it base file.
 */
public class ZipBundleFile extends CloseableBundleFile<ZipEntry> {

	final boolean verify;
	/**
	 * The zip file
	 */
	volatile ZipFile zipFile;

	public ZipBundleFile(File basefile, BundleInfo.Generation generation, MRUBundleFileList mruList, Debug debug,
			boolean verify) throws IOException {
		super(basefile, generation, mruList, debug);
		this.verify = verify;
		if (!BundleFile.secureAction.exists(basefile))
			throw new IOException(NLS.bind(Msg.ADAPTER_FILEEXIST_EXCEPTION, basefile));
	}

	@Override
	protected void doOpen() throws IOException {
		zipFile = BundleFile.secureAction.getZipFile(this.basefile, verify);
	}

	/**
	* Returns a ZipEntry for the bundle file. Must be called while holding the open lock.
	* This method does not ensure that the ZipFile is opened. Callers may need to call getZipfile() prior to calling this
	* method.
	* @param path the path to an entry
	* @return a ZipEntry or null if the entry does not exist
	*/
	private ZipEntry getZipEntry(String path) {
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

	@Override
	protected BundleEntry findEntry(String path) {
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

	@Override
	protected void doClose() throws IOException {
		zipFile.close();
	}

	@Override
	protected void postClose() {
		zipFile = null;
	}

	@Override
	protected InputStream doGetInputStream(ZipEntry entry) throws IOException {
		return zipFile.getInputStream(entry);
	}

	@Override
	protected Iterable<String> getPaths() {
		return () -> {
			final Enumeration<? extends ZipEntry> entries = zipFile.entries();
			return new Iterator<String>() {
				@Override
				public boolean hasNext() {
					return entries.hasMoreElements();
				}

				@Override
				public String next() {
					ZipEntry entry = entries.nextElement();
					return entry.getName();
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		};
	}
}
