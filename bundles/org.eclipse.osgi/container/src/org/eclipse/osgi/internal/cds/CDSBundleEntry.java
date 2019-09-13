/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corp. and others
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which accompanies this
 * distribution and is available at https://www.eclipse.org/legal/epl-2.0/
 * or the Apache License, Version 2.0 which accompanies this distribution and
 * is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * This Source Code may also be made available under the following
 * Secondary Licenses when the conditions for such availability set
 * forth in the Eclipse Public License, v. 2.0 are satisfied: GNU
 * General Public License, version 2 with the GNU Classpath
 * Exception [1] and GNU General Public License, version 2 with the
 * OpenJDK Assembly Exception [2].
 *
 * [1] https://www.gnu.org/software/classpath/license.html
 * [2] http://openjdk.java.net/legal/assembly-exception.html
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0 OR GPL-2.0 WITH Classpath-exception-2.0 OR LicenseRef-GPL-2.0 WITH Assembly-exception
 *******************************************************************************/

package org.eclipse.osgi.internal.cds;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.eclipse.osgi.storage.bundlefile.BundleEntry;

/**
 * A bundle entry for a class that is found in the shared classes cache
 */
public class CDSBundleEntry extends BundleEntry {
	final String path;
	final byte[] classbytes;
	final CDSBundleFile bundleFile;

	/**
	 * The constructor
	 * @param path the path to the class file
	 * @param classbytes the magic cookie bytes for the class in the shared cache
	 * @param bundleFile the bundle file where the class comes from
	 */
	public CDSBundleEntry(String path, byte[] classbytes, CDSBundleFile bundleFile) {
		super();
		this.path = path;
		this.classbytes = classbytes;
		this.bundleFile = bundleFile;
	}

	private BundleEntry getWrappedEntry() {
		BundleEntry entry = bundleFile.getWrappedEntry(path);
		if (entry == null) {
			throw new IllegalStateException("Could not find original entry for the class: " + path); //$NON-NLS-1$
		}
		return entry;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry#getFileURL()
	 * uses the wrapped bundle file to get the actual file url to the content of
	 * the class on disk.
	 * 
	 * This should is likely never to be called.
	 */
	@Override
	public URL getFileURL() {
		return getWrappedEntry().getFileURL();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry#getInputStream()
	 * wraps the classbytes into a ByteArrayInputStream.  This should not be used
	 * by classloading.
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		// someone is trying to get the real bytes of the class file!!
		// just return the entry from the wrapped file instead of the magic cookie
		return getWrappedEntry().getInputStream();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry#getBytes()
	 * if classbytes is not null, it returns the magic cookie for the shared class.  This is used to define 
	 * the class during class loading.
	 * if classbytes is null, it gets the contents from actual BundleEntry and caches it in classbytes.
	 */
	@Override
	public byte[] getBytes() throws IOException {
		return classbytes;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry#getLocalURL()
	 * uses the wrapped bundle file to get the actual local url to the content of
	 * the class on disk.
	 * 
	 * This should is likely never to be called.
	 */
	@Override
	public URL getLocalURL() {
		return getWrappedEntry().getLocalURL();
	}

	@Override
	public String getName() {
		return path;
	}

	@Override
	public long getSize() {
		return getWrappedEntry().getSize();
	}

	@Override
	public long getTime() {
		return getWrappedEntry().getTime();
	}
}
