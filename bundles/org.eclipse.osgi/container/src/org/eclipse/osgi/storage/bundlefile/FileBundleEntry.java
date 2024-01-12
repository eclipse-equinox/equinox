/*******************************************************************************
 * Copyright (c) 2005, 2013 IBM Corporation and others.
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
 *******************************************************************************/

package org.eclipse.osgi.storage.bundlefile;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * A BundleEntry represented by a File object.  The FileBundleEntry class is
 * used for bundles that are installed as extracted zips on a file system.
 */
public class FileBundleEntry extends BundleEntry {
	/**
	 * File for this entry.
	 */
	private final File file;
	/**
	 * The name for this entry
	 */
	private final String name;

	/**
	 * Constructs the BundleEntry using a File.
	 * @param file BundleFile object this entry is a member of
	 * @param name the name of this BundleEntry
	 */
	public FileBundleEntry(File file, String name) {
		this.file = file;
		boolean endsInSlash = name.length() > 0 && name.charAt(name.length() - 1) == '/';
		if (BundleFile.secureAction.isDirectory(file)) {
			if (!endsInSlash)
				name += '/';
		} else if (endsInSlash)
			name = name.substring(0, name.length() - 1);
		this.name = name;
	}

	/**
	 * Return an InputStream for the entry.
	 *
	 * @return InputStream for the entry
	 * @exception java.io.IOException
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		return BundleFile.secureAction.getFileInputStream(file);
	}

	/**
	 * Return size of the uncompressed entry.
	 *
	 * @return size of entry
	 */
	@Override
	public long getSize() {
		return BundleFile.secureAction.length(file);
	}

	/**
	 * Return name of the entry.
	 *
	 * @return name of entry
	 */
	@Override
	public String getName() {
		return (name);
	}

	/**
	 * Get the modification time for this BundleEntry.
	 * <p>If the modification time has not been set,
	 * this method will return <code>-1</code>.
	 *
	 * @return last modification time.
	 */
	@Override
	public long getTime() {
		return BundleFile.secureAction.lastModified(file);
	}

	@Override
	public URL getLocalURL() {
		return getFileURL();
	}

	@SuppressWarnings("deprecation")
	@Override
	public URL getFileURL() {
		try {
			return file.toURL();
		} catch (MalformedURLException e) {
			return null;
		}
	}
}
