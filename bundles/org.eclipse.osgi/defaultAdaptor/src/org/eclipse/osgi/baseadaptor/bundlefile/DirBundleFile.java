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
import java.util.NoSuchElementException;
import org.eclipse.osgi.internal.baseadaptor.AdaptorMsg;
import org.eclipse.osgi.util.NLS;

/**
 * A BundleFile that uses a directory as its base file.
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
		File filePath = new File(this.basefile, path);
		if (BundleFile.secureAction.exists(filePath)) {
			return filePath;
		}
		return null;
	}

	public BundleEntry getEntry(String path) {
		File filePath = new File(this.basefile, path);
		if (!BundleFile.secureAction.exists(filePath)) {
			return null;
		}
		return new FileBundleEntry(filePath, path);
	}

	public boolean containsDir(String dir) {
		File dirPath = new File(this.basefile, dir);
		return BundleFile.secureAction.exists(dirPath) && BundleFile.secureAction.isDirectory(dirPath);
	}

	public Enumeration getEntryPaths(final String path) {
		final java.io.File pathFile = new java.io.File(basefile, path);
		if (!BundleFile.secureAction.exists(pathFile))
			return null;
		if (BundleFile.secureAction.isDirectory(pathFile)) {
			final String[] fileList = BundleFile.secureAction.list(pathFile);
			if (fileList == null || fileList.length == 0)
				return null;
			final String dirPath = path.length() == 0 || path.charAt(path.length() - 1) == '/' ? path : path + '/';
			return new Enumeration() {
				int cur = 0;

				public boolean hasMoreElements() {
					return fileList != null && cur < fileList.length;
				}

				public Object nextElement() {
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
		return new Enumeration() {
			int cur = 0;

			public boolean hasMoreElements() {
				return cur < 1;
			}

			public Object nextElement() {
				if (cur == 0) {
					cur = 1;
					return path;
				}
				throw new NoSuchElementException();
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