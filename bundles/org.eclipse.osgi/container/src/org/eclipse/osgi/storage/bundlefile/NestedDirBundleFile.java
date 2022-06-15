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
 *******************************************************************************/

package org.eclipse.osgi.storage.bundlefile;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
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
	private final BundleFile baseBundleFile;
	private final String nestedDirName;
	private final Collection<String> filterPrefixes;

	/**
	 * Constructs a NestedDirBundleFile
	 * @param baseBundlefile the base bundle file
	 * @param nestedDirName
	 */
	public NestedDirBundleFile(BundleFile baseBundlefile, String nestedDirName) {
		this(baseBundlefile, nestedDirName, Collections.emptyList());
	}

	/**
	 * Constructs a NestedDirBundleFile
	 * @param baseBundlefile the base bundle file
	 * @param nestedDirName
	 * @param filterPrefixes the prefixes to filter out for the bundle file
	 */
	public NestedDirBundleFile(BundleFile baseBundlefile, String nestedDirName, Collection<String> filterPrefixes) {
		super(baseBundlefile.getBaseFile());
		this.baseBundleFile = baseBundlefile;
		if (nestedDirName.charAt(nestedDirName.length() - 1) != '/') {
			nestedDirName = nestedDirName + '/';
		}
		this.nestedDirName = nestedDirName;
		this.filterPrefixes = filterPrefixes;
	}

	@Override
	public void close() {
		// do nothing.
	}

	private boolean filterPath(String path) {
		if (path.length() > 0 && path.charAt(0) == '/')
			path = path.substring(1);
		for (String prefix : filterPrefixes) {
			if (path.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}

	private boolean filterDir(String path) {
		if (filterPrefixes.isEmpty()) {
			return false;
		}
		if (path.length() > 0 && path.charAt(path.length() - 1) != '/') {
			path = path + '/';
		}
		return filterPath(path);
	}

	@Override
	public BundleEntry getEntry(String path) {
		if (filterPath(path)) {
			return null;
		}
		return baseBundleFile.getEntry(prependNestedDir(path));
	}

	@Override
	public boolean containsDir(String dir) {
		if (dir == null)
			return false;
		if (filterPath(dir)) {
			return false;
		}
		return baseBundleFile.containsDir(prependNestedDir(dir));
	}

	private String prependNestedDir(String path) {
		if (path.length() > 0 && path.charAt(0) == '/')
			path = path.substring(1);
		return new StringBuilder(nestedDirName).append(path).toString();
	}

	@Override
	public Enumeration<String> getEntryPaths(String path, boolean recurse) {
		if (filterDir(path)) {
			return null;
		}
		final Enumeration<String> basePaths = baseBundleFile.getEntryPaths(prependNestedDir(path), recurse);
		final int cpLength = nestedDirName.length();
		if (basePaths == null)
			return null;
		return new Enumeration<String>() {

		@Override
			public boolean hasMoreElements() {
				return basePaths.hasMoreElements();
			}

		@Override
			public String nextElement() {
				String next = basePaths.nextElement();
				return next.substring(cpLength);
			}
		};
	}

	@Override
	public File getFile(String entry, boolean nativeCode) {
		// getFile is only valid if this is a root bundle file.
		// TODO to catch bugs we probably should throw new UnsupportedOperationException()
		return null;
	}

	/**
	 * @throws IOException
	 */
	@Override
	public void open() throws IOException {
		// do nothing
	}

	@Override
	public String toString() {
		return super.toString() + '[' + nestedDirName + ']';
	}

}
