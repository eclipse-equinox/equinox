/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
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

/**
 * A BundleEntry represented by a File object.  The FileBundleEntry class is
 * used for bundles that are installed as extracted zips on a file system.
 * @since 3.2
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
	FileBundleEntry(File file, String name) {
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
	public InputStream getInputStream() throws IOException {
		return BundleFile.secureAction.getFileInputStream(file);
	}

	/**
	 * Return size of the uncompressed entry.
	 *
	 * @return size of entry
	 */
	public long getSize() {
		return BundleFile.secureAction.length(file);
	}

	/**
	 * Return name of the entry.
	 *
	 * @return name of entry
	 */
	public String getName() {
		return (name);
	}

	/**
	 * Get the modification time for this BundleEntry.
	 * <p>If the modification time has not been set,
	 * this method will return <tt>-1</tt>.
	 *
	 * @return last modification time.
	 */
	public long getTime() {
		return BundleFile.secureAction.lastModified(file);
	}

	public URL getLocalURL() {
		return getFileURL();
	}

	@SuppressWarnings("deprecation")
	public URL getFileURL() {
		try {
			return file.toURL();
		} catch (MalformedURLException e) {
			return null;
		}
	}
}
