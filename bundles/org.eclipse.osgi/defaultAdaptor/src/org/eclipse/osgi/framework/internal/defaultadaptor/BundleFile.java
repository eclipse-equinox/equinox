/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
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
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.osgi.framework.debug.Debug;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.framework.internal.protocol.bundle.Handler;

abstract public class BundleFile
{
	/**
	 * The File object for this bundle.
	 */
	protected File bundlefile;

	/**
	 * The BundleData object for this bundle.
	 */
	protected DefaultBundleData bundledata;

	/**
	 * The String value of the Bundle ID
	 */
	protected String bundleID;

	/**
	 * BundleFile constructor
	 * @param bundlefile The File object where this bundle is persistently 
	 * stored.
	 * @param bundledata The BundleData object for this bundle.
	 */
	public BundleFile(File bundlefile, DefaultBundleData bundledata)
	{
		this.bundlefile = bundlefile;
		this.bundledata = bundledata;
		this.bundleID = String.valueOf(bundledata.getBundleID());
	}

	/**
	 * Returns a File for the bundle entry specified by the path.
	 * If required the content of the bundle entry is extracted into a file
	 * on the file system.
	 * @param path The path to the entry to locate a File for.
	 * @return A File object to access the contents of the bundle entry.
	 */
	abstract public File getFile(String path);

	/**
	 * Locates a file name in this bundle and returns a BundleEntry object
	 *
	 * @param path path of the entry to locate in the bundle
	 * @return BundleEntry object or null if the file name
	 *         does not exist in the bundle
	 */
	abstract public BundleEntry getEntry(String path);

	/** 
	 * Allows to access the entries of the bundle. 
	 * Since the bundle content is usually a jar, this 
	 * allows to access the jar contents.
	 * 
	 * GetEntryPaths allows to enumerate the content of "path".
	 * If path is a directory, it is equivalent to listing the directory
	 * contents. The returned names are either files or directories 
	 * themselves. If a returned name is a directory, it finishes with a 
	 * slash. If a returned name is a file, it does not finish with a slash.
	 * @param path path of the entry to locate in the bundle
	 * @return an Enumeration of Strings that indicate the paths found or
	 * null if the path does not exist. 
	 */
	abstract public Enumeration getEntryPaths(String path);

	/**
	 * Closes the BundleFile.
	 * @throws IOException if any error occurs.
	 */
	abstract public void close() throws IOException;

	/**
	 * Opens the BundleFiles.
	 * @throws IOException if any error occurs.
	 */
	abstract public void open() throws IOException;

	/**
	 * Returns a URL to access the contents of the entry specified by the path
	 * @param path 
	 */
	public URL getURL(String path, int cpEntry) {
		BundleEntry bundleEntry = getEntry(path);
		if (bundleEntry == null)
			return null;

		if (System.getSecurityManager() != null) {
			// If we are running in security mode, we should create bundle URLs
			try {
				return new URL(Constants.OSGI_URL_PROTOCOL, bundleID, cpEntry, path, new Handler(bundleEntry));
			} catch (MalformedURLException e) {
				return null;
			}
		} else {
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=42871
			return bundleEntry.getURL();
		}
	}

	static public BundleFile createBundleFile(File bundlefile, DefaultBundleData bundledata) throws IOException {
		if (bundlefile.isDirectory()) {
			return new DirBundleFile(bundlefile,bundledata);
		}
		else {
			return new ZipBundleFile(bundlefile,bundledata);
		}
	}

	static public BundleFile createBundleFile(ZipBundleFile bundlefile,String cp) {
		return new ZipDirBundleFile(bundlefile,cp);
	}

	public static class ZipBundleFile extends BundleFile
	{
		protected ZipFile zipFile;
		protected boolean closed = false;

		protected ZipBundleFile(File bundlefile, DefaultBundleData bundledata)
			throws IOException
		{
			super(bundlefile, bundledata);
			this.closed=true;
			open();
		}

		protected ZipEntry getZipEntry(String path)
		{
			if (path.length() > 0 && path.charAt(0) == '/')
				path = path.substring(1);
			return zipFile.getEntry(path);
		}

		public File getFile(String entry)
		{
			ZipEntry zipEntry = getZipEntry(entry);
			if (zipEntry == null || zipEntry.isDirectory()) 
			{
				return null;
			}

			try
			{
				File bundleGenerationDir = bundledata.getGenerationDir();

				/* if the generation dir exists, then we have place to cache */
				if (bundleGenerationDir.exists())
				{
					String path = ".cp";    /* put all these entries in this subdir */
					String name = zipEntry.getName().replace('/', File.separatorChar);

					if ((name.length() > 1) && (name.charAt(0) == File.separatorChar)) /* if name has a leading slash */
					{
						path = path.concat(name);
					}
					else
					{
						path = path + File.separator + name;
					}

					File nested = new File(bundleGenerationDir, path);

					if (nested.exists())
					{
						/* the entry is already cached */
						if (Debug.DEBUG && Debug.DEBUG_GENERAL)
						{
							Debug.println("File already present: "+nested.getPath());
						}
					}
					else
					{
						InputStream in = zipFile.getInputStream(zipEntry);
						if (in == null)
							return null;
						/* the entry has not been cached */
						if (Debug.DEBUG && Debug.DEBUG_GENERAL)
						{
							Debug.println("Creating file: "+nested.getPath());
						}

						/* create the necessary directories */
						File dir = new File(nested.getParent());

						if (!dir.exists() && !dir.mkdirs())
						{
							if (Debug.DEBUG && Debug.DEBUG_GENERAL)
							{
								Debug.println("Unable to create directory: "+dir.getPath());
							}

							throw new IOException(AdaptorMsg.formatter.getString("ADAPTOR_STORAGE_EXCEPTION"));
						}

						/* copy the entry to the cache */
						DefaultAdaptor.readFile(in, nested);
					}

					return nested;
				}
			}
			catch (IOException e)
			{
				if (Debug.DEBUG && Debug.DEBUG_GENERAL)
				{
					Debug.printStackTrace(e);
				}
			}
			return null;
		}

		public BundleEntry getEntry(String path)
		{
			ZipEntry zipEntry = getZipEntry(path);
			if (zipEntry == null){
				return null;
			}

			return new BundleEntry.ZipBundleEntry(zipFile,zipEntry,bundlefile);

		}

		public Enumeration getEntryPaths(String path){
			if (path == null) {
				return null;
			}

			if (path.startsWith("/")){
				path = path.substring(1);
			}
			if (path.length()>0 & !path.endsWith("/")) {
				path = new StringBuffer(path).append("/").toString();
			}

			Vector vEntries = new Vector();
			Enumeration entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry zipEntry = (ZipEntry)entries.nextElement();
				String entryPath = zipEntry.getName();
				if (entryPath.startsWith(path)){
					if (path.length()<entryPath.length()){
						if (entryPath.lastIndexOf('/')<path.length()) {
							vEntries.add(entryPath);
						}
						else {
							entryPath = entryPath.substring(path.length());
							int i = entryPath.indexOf('/');
							entryPath = path + entryPath.substring(0,i+1);
							if(!vEntries.contains(entryPath)){
								vEntries.add(entryPath);
							}
						}
					}
				}
			}
			return vEntries.elements();
		}

		public void close() throws IOException {
			if (!closed)
			{
				closed = true;
				zipFile.close();
			}
		}

		public void open() throws IOException {
			if (closed)
			{
				zipFile = new ZipFile(this.bundlefile);

				closed = false;
			}
		}

	}

	public static class DirBundleFile extends BundleFile {

		protected DirBundleFile(File bundlefile, DefaultBundleData bundledata)
			throws IOException
		{
			super(bundlefile,bundledata);
			if (!bundlefile.exists() || !bundlefile.isDirectory()){
				throw new IOException(AdaptorMsg.formatter.getString("ADAPTOR_DIRECTORY_EXCEPTION", bundlefile));
			}
		}

		public File getFile(String path)
		{
			File filePath = new File(this.bundlefile, path);
			if (filePath.exists()) {
				return filePath;
			}
			return null;
		}

		public BundleEntry getEntry(String path)
		{
			File filePath = new File(this.bundlefile, path);
			return new BundleEntry.FileBundleEntry(filePath,path);
		}

		public URL getURL(String path, int cpEntry) {
			File filePath = new File(this.bundlefile, path);
			if (!filePath.exists())
				return null;
			

			if (System.getSecurityManager() != null) {
				// If we are running in security mode, we should create bundle URLs
				try {
					BundleEntry bundleEntry = new BundleEntry.FileBundleEntry(filePath,path);
					return new URL(Constants.OSGI_URL_PROTOCOL, bundleID, cpEntry, path, new Handler(bundleEntry));
				} catch (MalformedURLException e) {
					return null;
				}
			} else
				try {
					// https://bugs.eclipse.org/bugs/show_bug.cgi?id=42871
					return filePath.toURL();
				} catch (MalformedURLException e) {
					return null;
				}
		}

		public Enumeration getEntryPaths(final String path){
			final java.io.File pathFile = new java.io.File(bundlefile,path);
			if(!pathFile.exists()) return null;
			if (pathFile.isDirectory()){
				final String[] fileList = pathFile.list();
				return new Enumeration(){
					int cur=0;
					public boolean hasMoreElements() {
						return cur < fileList.length;
					}

					public Object nextElement() {
						if(cur>=fileList.length){
							throw new NoSuchElementException();
						}
						java.io.File childFile = new java.io.File(pathFile,fileList[cur]);
						if (childFile.isDirectory()){
							StringBuffer sb = new StringBuffer(fileList[cur++]).append("/");
							return sb.toString();
						}
						else{
							return fileList[cur++];
						}
					}
				
				};
			} else {
				return new Enumeration(){
					int cur=0;
					public boolean hasMoreElements() {
						return cur < 1;
					}
					public Object nextElement() {
						if (cur==0) {
							cur=1; 
							return path;
						} else 
							throw new NoSuchElementException();
					}
				};
			}
		}

		public void close(){
			// nothing to do.
		}

		public void open() {
			// nothing to do.
		}
	}

	public static class ZipDirBundleFile extends BundleFile {
		ZipBundleFile zipBundlefile;
		String cp;
		public ZipDirBundleFile(ZipBundleFile zipBundlefile, String cp){
			super(zipBundlefile.bundlefile,zipBundlefile.bundledata);
			this.zipBundlefile = zipBundlefile;
			this.cp = cp;
			if (cp.charAt(cp.length()-1) != '/'){
				this.cp = this.cp + '/';
			}
		}

		public void close(){
			// do nothing.
		}

		public BundleEntry getEntry(String path) {
			if (path.length() > 0 && path.charAt(0) == '/')
				path = path.substring(1);
			String newpath = new StringBuffer(cp).append(path).toString();
			return zipBundlefile.getEntry(newpath);
		}

		public Enumeration getEntryPaths(String path){
			// getEntryPaths is only valid if this is a root bundle file.
			return null;
		}

		public File getFile(String entry){
			// getFile is only valid if this is a root bundle file.
			return null;
		}

		public void open(){
			// do nothing
		}
	}
}
