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
 * Represents a directory entry in a ZipBundleFile.  This object is used to
 * reference a directory entry in a ZipBundleFile when the directory entries are
 * not included in the zip file.
 */
public class DirZipBundleEntry extends BundleEntry {

	/**
	 * ZipBundleFile for this entry.
	 */
	private ZipBundleFile bundleFile;
	/**
	 * The name for this entry
	 */
	String name;

	public DirZipBundleEntry(ZipBundleFile bundleFile, String name) {
		this.name = (name.length() > 0 && name.charAt(0) == '/') ? name.substring(1) : name;
		this.bundleFile = bundleFile;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return new ByteArrayInputStream(new byte[0]);
	}

	@Override
	public long getSize() {
		return 0;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public long getTime() {
		return 0;
	}

	@SuppressWarnings("deprecation")
	@Override
	public URL getLocalURL() {
		try {
			return new URL("jar:" + bundleFile.basefile.toURL() + "!/" + name); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (MalformedURLException e) {
			//This can not happen, unless the jar protocol is not supported.
			return null;
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public URL getFileURL() {
		try {
			return bundleFile.extractDirectory(name).toURL();
		} catch (MalformedURLException e) {
			// this cannot happen.
			return null;
		}
	}
}
