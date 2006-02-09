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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Represents a directory entry in a ZipBundleFile.  This object is used to 
 * reference a directory entry in a ZipBundleFile when the directory entries are
 * not included in the zip file.
 * @since 3.2
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

	public InputStream getInputStream() throws IOException {
		return null;
	}

	public long getSize() {
		return 0;
	}

	public String getName() {
		return name;
	}

	public long getTime() {
		return 0;
	}

	public URL getLocalURL() {
		try {
			return new URL("jar:file:" + bundleFile.basefile.getAbsolutePath() + "!/" + name); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (MalformedURLException e) {
			//This can not happen, unless the jar protocol is not supported.
			return null;
		}
	}

	public URL getFileURL() {
		try {
			return bundleFile.extractDirectory(name).toURL();
		} catch (MalformedURLException e) {
			// this cannot happen.
			return null;
		}
	}
}