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

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import org.eclipse.osgi.internal.baseadaptor.AdaptorMsg;
import org.eclipse.osgi.util.NLS;

/**
 * A BundleFile that uses a directory as its base file.
 * @since 3.2
 */
public class DirBundleFile extends BundleFile {

	/**
	 * Constructs a DirBundleFile
	 * @param basefile the base file
	 * @throws IOException
	 */
	public DirBundleFile(File basefile) throws IOException {
		super(basefile);
		if (!BundleFile.secureAction.exists(basefile) || !BundleFile.secureAction.isDirectory(basefile)) {
			throw new IOException(NLS.bind(AdaptorMsg.ADAPTOR_DIRECTORY_EXCEPTION, basefile));
		}
	}

	public File getFile(String path, boolean nativeCode) {
		boolean checkInBundle = path != null && path.indexOf("..") >= 0; //$NON-NLS-1$
		File file = new File(basefile, path);
		if (!BundleFile.secureAction.exists(file)) {
			return null;
		}
		// must do an extra check to make sure file is within the bundle (bug 320546)
		if (checkInBundle) {
			try {
				if (!BundleFile.secureAction.getCanonicalPath(file).startsWith(BundleFile.secureAction.getCanonicalPath(basefile)))
					return null;
			} catch (IOException e) {
				return null;
			}
		}
		return file;
	}

	public BundleEntry getEntry(String path) {
		File filePath = getFile(path, false);
		if (filePath == null)
			return null;
		return new FileBundleEntry(filePath, path);
	}

	public boolean containsDir(String dir) {
		File dirPath = getFile(dir, false);
		return dirPath != null && BundleFile.secureAction.isDirectory(dirPath);
	}

	public Enumeration<String> getEntryPaths(String path) {
		if (path.length() > 0 && path.charAt(0) == '/')
			path = path.substring(1);
		final File pathFile = getFile(path, false);
		if (pathFile == null || !BundleFile.secureAction.isDirectory(pathFile))
			return null;
		final String[] fileList = BundleFile.secureAction.list(pathFile);
		if (fileList == null || fileList.length == 0)
			return null;
		final String dirPath = path.length() == 0 || path.charAt(path.length() - 1) == '/' ? path : path + '/';
		return new Enumeration<String>() {
			int cur = 0;

			public boolean hasMoreElements() {
				return fileList != null && cur < fileList.length;
			}

			public String nextElement() {
				if (!hasMoreElements()) {
					throw new NoSuchElementException();
				}
				java.io.File childFile = new java.io.File(pathFile, fileList[cur]);
				StringBuffer sb = new StringBuffer(dirPath).append(fileList[cur++]);
				if (BundleFile.secureAction.isDirectory(childFile)) {
					sb.append("/"); //$NON-NLS-1$
				}
				return sb.toString();
			}
		};
	}

	public void close() {
		// nothing to do.
	}

	public void open() {
		// nothing to do.
	}
}
