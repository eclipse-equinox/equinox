/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.internal.defaultadaptor;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public abstract class BundleEntry {
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

	public abstract URL getLocalURL();

	/**
	 * Return the name of this BundleEntry by calling getName().
	 *
	 * @return String representation of this BundleEntry.
	 */
	public String toString() {
		return (getName());
	}

	public static class ZipBundleEntry extends BundleEntry {
		/**
		 * ZipFile for this entry.
		 */
		private ZipFile zipFile;
		/**
		 * ZipEntry for this entry.
		 */
		private ZipEntry zipEntry;

		/**
		 * The BundleFile for this entry.
		 */
		private BundleFile bundleFile;

		/**
		 * Constructs the BundleEntry using a ZipEntry.
		 * @param bundleFile BundleFile object this entry is a member of
		 * @param entry ZipEntry object of this entry
		 */
		ZipBundleEntry(ZipFile zipFile, ZipEntry entry, BundleFile bundleFile) {
			this.zipFile = zipFile;
			this.zipEntry = entry;
			this.bundleFile = bundleFile;
		}

		/**
		 * Return an InputStream for the entry.
		 *
		 * @return InputStream for the entry
		 * @exception java.io.IOException
		 */
		public InputStream getInputStream() throws IOException {
			return (zipFile.getInputStream(zipEntry));
		}
		/**
		 * Return size of the uncompressed entry.
		 *
		 * @return size of entry
		 */
		public long getSize() {
			return (zipEntry.getSize());
		}
		/**
		 * Return name of the entry.
		 *
		 * @return name of entry
		 */
		public String getName() {
			return (zipEntry.getName());
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

		public URL getLocalURL() {
			try {
				File file = bundleFile.getFile(zipEntry.getName());
				return file.toURL();
				//return new URL("jar:file:" + bundleFile.getAbsolutePath() + "!/" + zipEntry.getName());
			} catch (MalformedURLException e) {
				//This can not happen. 
				return null;
			}
		}
	}

	public static class FileBundleEntry extends BundleEntry {
		/**
		 * File for this entry.
		 */
		private File file;

		private String name;

		/**
		 * Constructs the BundleEntry using a ZipEntry.
		 * @param bundleFile BundleFile object this entry is a member of
		 * @param entry ZipEntry object of this entry
		 */
		FileBundleEntry(File file, String name) {
			this.file = file;
			this.name = name;
		}

		/**
		 * Return an InputStream for the entry.
		 *
		 * @return InputStream for the entry
		 * @exception java.io.IOException
		 */
		public InputStream getInputStream() throws IOException {
			return (new FileInputStream(file));
		}
		/**
		 * Return size of the uncompressed entry.
		 *
		 * @return size of entry
		 */
		public long getSize() {
			return (file.length());
		}
		/**
		 * Return name of the entry.
		 *
		 * @return name of entry
		 */
		public String getName() {
			return (name);
		}

		/**
		 * Get the modification time for this BundleEntry.
		 * <p>If the modification time has not been set,
		 * this method will return <tt>-1</tt>.
		 *
		 * @return last modification time.
		 */
		public long getTime() {
			return file.lastModified();
		}

		public URL getLocalURL() {
			try {
				return file.toURL();
			} catch (MalformedURLException e) {
				return null;
			}
		}
	}

	public static class DirBundleEntry extends BundleEntry {

		/**
		 * File for this entry.
		 */
		private File file;
		private String name;

		public DirBundleEntry(File file, String name){
			this.name = name;
			this.file = file;
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
				return new URL("jar:file:" + file.getAbsolutePath() + "!/" + name);
			} catch (MalformedURLException e) {
				//This can not happen. 
				return null;
			}
		}
	}
}
