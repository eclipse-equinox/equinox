/*******************************************************************************
 * Copyright (c) 2005, 2016 IBM Corporation and others.
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
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.osgi.internal.messages.Msg;
import org.eclipse.osgi.util.NLS;

/**
 * A BundleFile that uses a directory as its base file.
 */
public class DirBundleFile extends BundleFile {

	private static final String POINTER_SAME_DIRECTORY_1 = "/.";//$NON-NLS-1$
	private static final String POINTER_SAME_DIRECTORY_2 = "//";//$NON-NLS-1$
	private static final String POINTER_UPPER_DIRECTORY = "..";//$NON-NLS-1$

	private final boolean enableStrictBundleEntryPath;
	private final Map<File, Boolean> doesNotExistCache = new ConcurrentHashMap<>();

	/**
	 * Constructs a DirBundleFile
	 * @param basefile the base file
	 * @throws IOException
	 */
	public DirBundleFile(File basefile, boolean enableStrictBundleEntryPath) throws IOException {
		super(getBaseFile(basefile, enableStrictBundleEntryPath));
		if (!BundleFile.secureAction.exists(basefile) || !BundleFile.secureAction.isDirectory(basefile)) {
			throw new IOException(NLS.bind(Msg.ADAPTOR_DIRECTORY_EXCEPTION, basefile));
		}
		this.enableStrictBundleEntryPath = enableStrictBundleEntryPath;
	}

	private static File getBaseFile(File basefile, boolean enableStrictBundleEntryPath) throws IOException {
		return enableStrictBundleEntryPath ? secureAction.getCanonicalFile(basefile) : basefile;
	}

	@Override
	public File getFile(String path, boolean nativeCode) {
		final boolean checkInBundle = path != null && path.indexOf(POINTER_UPPER_DIRECTORY) >= 0;
		File file = new File(this.basefile, path);
		File parentFile = file.getParentFile();
		if (!parentExists(parentFile)) {
			return null;
		}
		if (!BundleFile.secureAction.exists(file)) {
			cacheIfParentExists(parentFile);
			return null;
		}

		if (!enableStrictBundleEntryPath) {
			// must do an extra check to make sure file is within the bundle (bug 320546)
			if (checkInBundle && !isInBundle(file)) {
				return null;
			}
			return file;
		}
		boolean normalize = false;
		boolean isBundleRoot = false;
		if (path != null) {
			isBundleRoot = path.equals("/");//$NON-NLS-1$
			if (!isBundleRoot) {
				normalize = checkInBundle || path.indexOf(POINTER_SAME_DIRECTORY_1) >= 0 || path.indexOf(POINTER_SAME_DIRECTORY_2) >= 0;
			}
		}
		File canonicalFile;
		try {
			canonicalFile = BundleFile.secureAction.getCanonicalFile(file);
			if (!isBundleRoot) {
				File absoluteFile = BundleFile.secureAction.getAbsoluteFile(file);
				String canonicalPath;
				String absolutePath;
				if (normalize) {
					canonicalPath = canonicalFile.toURI().getPath();
					absolutePath = absoluteFile.toURI().normalize().getPath();
				} else {
					canonicalPath = canonicalFile.getPath();
					absolutePath = absoluteFile.getPath();
				}
				if (!canonicalPath.equals(absolutePath)) {
					return null;
				}
			}
			// must do an extra check to make sure file is within the bundle (bug 320546)
			if (checkInBundle && !isInBundle(file)) {
				return null;
			}
		} catch (IOException e) {
			return null;
		}

		return file;
	}

	boolean isInBundle(File file) {
		try {
			String canonicalizedRoot = BundleFile.secureAction.getCanonicalPath(basefile);
			if (!canonicalizedRoot.endsWith(File.separator)) {
				canonicalizedRoot += File.separator;
			}
			String canonicalizedChild = BundleFile.secureAction.getCanonicalPath(file);
			if (BundleFile.secureAction.isDirectory(file) && !canonicalizedChild.endsWith(File.separator)) {
				canonicalizedChild += File.separator;
			}
			if (!canonicalizedChild.startsWith(canonicalizedRoot)) {
				return false;
			}
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	private void cacheIfParentExists(File parentFile) {
		doesNotExistCache.computeIfAbsent(parentFile, secureAction::isDirectory);
	}

	private boolean parentExists(File parentFile) {
		Boolean exists = doesNotExistCache.get(parentFile);
		return exists == null || exists.booleanValue();
	}

	@Override
	public BundleEntry getEntry(String path) {
		File filePath = getFile(path, false);
		if (filePath == null)
			return null;
		return new FileBundleEntry(filePath, path);
	}

	@Override
	public boolean containsDir(String dir) {
		File dirPath = getFile(dir, false);
		return dirPath != null && BundleFile.secureAction.isDirectory(dirPath);
	}

	@Override
	public Enumeration<String> getEntryPaths(String path, boolean recurse) {
		if (path.length() > 0 && path.charAt(0) == '/')
			path = path.substring(1);
		File pathFile = getFile(path, false);
		if (pathFile == null || !BundleFile.secureAction.isDirectory(pathFile))
			return null;
		String[] fileList = BundleFile.secureAction.list(pathFile);
		if (fileList == null || fileList.length == 0)
			return null;
		String dirPath = path.length() == 0 || path.charAt(path.length() - 1) == '/' ? path : path + '/';

		LinkedHashSet<String> entries = new LinkedHashSet<>();
		for (String s : fileList) {
			java.io.File childFile = new java.io.File(pathFile, s);
			StringBuilder sb = new StringBuilder(dirPath).append(s);
			if (BundleFile.secureAction.isDirectory(childFile)) {
				sb.append("/"); //$NON-NLS-1$
				if (recurse) {
					Enumeration<String> e = getEntryPaths(sb.toString(), true);
					if (e != null)
						entries.addAll(Collections.list(e));
				}
			}
			entries.add(sb.toString());
		}
		return Collections.enumeration(entries);
	}

	@Override
	public void close() {
		// nothing to do.
	}

	@Override
	public void open() {
		// nothing to do.
	}
}
