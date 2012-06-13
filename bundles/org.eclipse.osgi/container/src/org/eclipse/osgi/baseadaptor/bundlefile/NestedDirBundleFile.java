/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
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
 * @since 3.2
 */
public class NestedDirBundleFile extends BundleFile {
	private final BundleFile baseBundleFile;
	private final String cp;

	/**
	 * Constructs a NestedDirBundleFile
	 * @param baseBundlefile the base bundle file
	 * @param cp
	 */
	public NestedDirBundleFile(BundleFile baseBundlefile, String cp) {
		super(baseBundlefile.getBaseFile());
		this.baseBundleFile = baseBundlefile;
		if (cp.charAt(cp.length() - 1) != '/') {
			cp = cp + '/';
		}
		this.cp = cp;
	}

	public void close() {
		// do nothing.
	}

	public BundleEntry getEntry(String path) {
		return baseBundleFile.getEntry(prependNestedDir(path));
	}

	public boolean containsDir(String dir) {
		if (dir == null)
			return false;

		return baseBundleFile.containsDir(prependNestedDir(dir));
	}

	private String prependNestedDir(String path) {
		if (path.length() > 0 && path.charAt(0) == '/')
			path = path.substring(1);
		return new StringBuffer(cp).append(path).toString();
	}

	public Enumeration<String> getEntryPaths(String path) {
		final Enumeration<String> basePaths = baseBundleFile.getEntryPaths(prependNestedDir(path));
		final int cpLength = cp.length();
		if (basePaths == null)
			return null;
		return new Enumeration<String>() {

			public boolean hasMoreElements() {
				return basePaths.hasMoreElements();
			}

			public String nextElement() {
				String next = basePaths.nextElement();
				return next.substring(cpLength);
			}
		};
	}

	public File getFile(String entry, boolean nativeCode) {
		// getFile is only valid if this is a root bundle file.
		// TODO to catch bugs we probably should throw new UnsupportedOperationException()
		return null;
	}

	/**
	 * @throws IOException  
	 */
	public void open() throws IOException {
		// do nothing
	}
}
