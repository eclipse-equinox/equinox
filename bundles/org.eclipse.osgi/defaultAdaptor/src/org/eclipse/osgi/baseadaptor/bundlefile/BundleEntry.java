/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others.
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
import java.net.URL;
import org.eclipse.osgi.framework.debug.Debug;
import org.eclipse.osgi.internal.baseadaptor.AdaptorUtil;

/**
 * A BundleEntry represents one entry of a BundleFile.
 * <p>
 * Clients may extend this class.
 * </p>
 * @since 3.2
 */
public abstract class BundleEntry {
	protected static final int BUF_SIZE = 8 * 1024;

	/**
	 * Return an InputStream for the entry.
	 *
	 * @return InputStream for the entry.
	 * @throws java.io.IOException If an error occurs reading the bundle.
	 */
	public abstract InputStream getInputStream() throws IOException;

	/**
	 * Return the size of the entry (uncompressed).
	 *
	 * @return size of entry.
	 */
	public abstract long getSize();

	/**
	 * Return the name of the entry.
	 *
	 * @return name of entry.
	 */
	public abstract String getName();

	/**
	 * Get the modification time for this BundleEntry.
	 * <p>If the modification time has not been set,
	 * this method will return <tt>-1</tt>.
	 *
	 * @return last modification time.
	 */
	public abstract long getTime();

	/**
	 * Get a URL to the bundle entry that uses a common protocol (i.e. file:
	 * jar: or http: etc.).  
	 * @return a URL to the bundle entry that uses a common protocol
	 */
	public abstract URL getLocalURL();

	/**
	 * Get a URL to the content of the bundle entry that uses the file: protocol.
	 * The content of the bundle entry may be downloaded or extracted to the local
	 * file system in order to create a file: URL.
	 * @return a URL to the content of the bundle entry that uses the file: protocol
	 */
	public abstract URL getFileURL();

	/**
	 * Return the name of this BundleEntry by calling getName().
	 *
	 * @return String representation of this BundleEntry.
	 */
	public String toString() {
		return (getName());
	}

	/**
	 * Used for class loading.  This default implementation gets the input stream from this entry
	 * and copies the content into a byte array.
	 * @return a byte array containing the content of this entry
	 * @throws IOException
	 */
	public byte[] getBytes() throws IOException {
		InputStream in = getInputStream();
		int length = (int) getSize();
		if (Debug.DEBUG_LOADER)
			Debug.println("  about to read " + length + " bytes from " + getName()); //$NON-NLS-1$ //$NON-NLS-2$
		return AdaptorUtil.getBytes(in, length, BUF_SIZE);
	}
}
