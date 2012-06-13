/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Rob Harrop - SpringSource Inc. (bug 253942)
 *******************************************************************************/
package org.eclipse.osgi.baseadaptor.bundlefile;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.ZipEntry;

/**
 * A BundleEntry represented by a ZipEntry in a ZipFile.  The ZipBundleEntry
 * class is used for bundles that are installed as a ZipFile on a file system.
 * @since 3.2
 */
public class ZipBundleEntry extends BundleEntry {
	/**
	 * ZipEntry for this entry.
	 */
	protected final ZipEntry zipEntry;

	/**
	 * The BundleFile for this entry.
	 */
	protected final ZipBundleFile bundleFile;

	/**
	 * Constructs the BundleEntry using a ZipEntry.
	 * @param bundleFile BundleFile object this entry is a member of
	 * @param zipEntry ZipEntry object of this entry
	 */
	ZipBundleEntry(ZipEntry zipEntry, ZipBundleFile bundleFile) {
		this.zipEntry = zipEntry;
		this.bundleFile = bundleFile;
	}

	/**
	 * Return an InputStream for the entry.
	 *
	 * @return InputStream for the entry
	 * @exception java.io.IOException
	 */
	public InputStream getInputStream() throws IOException {
		ZipBundleFile zipBundleFile = bundleFile;

		if (!zipBundleFile.isMruEnabled())
			return bundleFile.getZipFile().getInputStream(zipEntry);

		zipBundleFile.incrementReference();
		InputStream result = null;
		try {
			return result = new ZipBundleEntryInputStream(zipBundleFile.getZipFile().getInputStream(zipEntry));
		} finally {
			if (result == null)
				// an exception occurred; decrement the reference
				zipBundleFile.decrementReference();
		}
	}

	/**
	 * Return size of the uncompressed entry.
	 *
	 * @return size of entry
	 */
	public long getSize() {
		return zipEntry.getSize();
	}

	/**
	 * Return name of the entry.
	 *
	 * @return name of entry
	 */
	public String getName() {
		return zipEntry.getName();
	}

	/**
	 * Get the modification time for this BundleEntry.
	 * <p>If the modification time has not been set,
	 * this method will return <tt>-1</tt>.
	 *
	 * @return last modification time.
	 */
	public long getTime() {
		return zipEntry.getTime();
	}

	@SuppressWarnings("deprecation")
	public URL getLocalURL() {
		try {
			return new URL("jar:" + bundleFile.basefile.toURL() + "!/" + zipEntry.getName()); //$NON-NLS-1$//$NON-NLS-2$
		} catch (MalformedURLException e) {
			//This can not happen. 
			return null;
		}
	}

	@SuppressWarnings("deprecation")
	public URL getFileURL() {
		try {
			File file = bundleFile.getFile(zipEntry.getName(), false);
			if (file != null)
				return file.toURL();
		} catch (MalformedURLException e) {
			//This can not happen. 
		}
		return null;
	}

	private class ZipBundleEntryInputStream extends InputStream {
		private final InputStream stream;
		private boolean closed = false;

		public ZipBundleEntryInputStream(InputStream stream) {
			this.stream = stream;
		}

		public int available() throws IOException {
			return stream.available();
		}

		public void close() throws IOException {
			try {
				stream.close();
			} finally {
				synchronized (this) {
					if (closed)
						return;
					closed = true;
				}
				bundleFile.decrementReference();
			}
		}

		public void mark(int var0) {
			stream.mark(var0);
		}

		public boolean markSupported() {
			return stream.markSupported();
		}

		public int read() throws IOException {
			return stream.read();
		}

		public int read(byte[] var0, int var1, int var2) throws IOException {
			return stream.read(var0, var1, var2);
		}

		public int read(byte[] var0) throws IOException {
			return stream.read(var0);
		}

		public void reset() throws IOException {
			stream.reset();
		}

		public long skip(long var0) throws IOException {
			return stream.skip(var0);
		}
	}
}
