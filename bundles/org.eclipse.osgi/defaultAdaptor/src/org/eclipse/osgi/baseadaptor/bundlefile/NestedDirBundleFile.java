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

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;

/**
 * A NestedDirBundleFile uses another BundleFile as its source but
 * accesses all of its resources relative to a nested directory within
 * the other BundleFile object.  This is used to support zipped bundles
 * that use a Bundle-ClassPath with an nested directory specified.
 * <p>
 * For Example:
 * <pre>
 * Bundle-ClassPath: nested.jar,nesteddir/
 * </pre>
 */
public class NestedDirBundleFile extends BundleFile {
	BundleFile baseBundleFile;
	String cp;

	/**
	 * Constructs a NestedDirBundleFile
	 * @param baseBundlefile the base bundle file
	 * @param cp
	 */
	public NestedDirBundleFile(BundleFile baseBundlefile, String cp) {
		super(baseBundlefile.basefile);
		this.baseBundleFile = baseBundlefile;
		this.cp = cp;
		if (cp.charAt(cp.length() - 1) != '/') {
			this.cp = this.cp + '/';
		}
	}

	public void close() {
		// do nothing.
	}

	public BundleEntry getEntry(String path) {
		if (path.length() > 0 && path.charAt(0) == '/')
			path = path.substring(1);
		String newpath = new StringBuffer(cp).append(path).toString();
		return baseBundleFile.getEntry(newpath);
	}

	public boolean containsDir(String dir) {
		if (dir == null)
			return false;

		if (dir.length() > 0 && dir.charAt(0) == '/')
			dir = dir.substring(1);
		String newdir = new StringBuffer(cp).append(dir).toString();
		return baseBundleFile.containsDir(newdir);
	}

	public Enumeration getEntryPaths(String path) {
		// getEntryPaths is only valid if this is a root bundle file.
		return null;
	}

	public File getFile(String entry, boolean nativeCode) {
		// getFile is only valid if this is a root bundle file.
		return null;
	}

	public void open() throws IOException {
		// do nothing
	}
}