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

package org.eclipse.osgi.framework.internal.reliablefile;

import java.io.*;
import java.util.Hashtable;

/**
 * ReliableFile class used by ReliableFileInputStream and ReliableOutputStream.
 * This class encapsulates all the logic for reliable file support.
 */

public class ReliableFile {
	//TODO constants should be all caps
	/**
	 * Extension of tmp file used during writing.
	 * A reliable file with this extension should
	 * never be directly used.
	 */
	public static final String tmpExt = ".tmp";

	/**
	 * Extension of previous generation of the reliable file.
	 * A reliable file with this extension should
	 * never be directly used.
	 */
	public static final String oldExt = ".bak";

	/**
	 * Extension of next generation of the reliable file.
	 * A reliable file with this extension should
	 * never be directly used.
	 */
	public static final String newExt = ".new";

	/** List of active ReliableFile objects: File => ReliableFile */
	private static Hashtable files;

	static {
		files = new Hashtable(30); /* initialize files */
	}

	/** File object for original file */
	private File orgFile;

	/** File object for the temporary output file */
	private File tmpFile;

	/** File object for old data file */
	private File oldFile;

	/** File object for file containing new data */
	private File newFile;

	/** True if this object is open for read or write */
	private boolean locked;

	/** Use code of this object. When zero this object must be removed from files */
	private int use;

	/**
	 * ReliableFile object factory. This method is called by ReliableFileInputStream
	 * and ReliableFileOutputStream to get a ReliableFile object for a target file.
	 * If the object is in the cache, the cached copy is returned.
	 * Otherwise a new ReliableFile object is created and returned.
	 * The use count of the returned ReliableFile object is incremented.
	 *
	 * @param name Name of the target file.
	 * @return A ReliableFile object for the target file.
	 * @throws IOException If the target file is a directory.
	 */
	static ReliableFile getReliableFile(String name) throws IOException {
		return getReliableFile(new File(name));
	}

	/**
	 * ReliableFile object factory. This method is called by ReliableFileInputStream
	 * and ReliableFileOutputStream to get a ReliableFile object for a target file.
	 * If the object is in the cache, the cached copy is returned.
	 * Otherwise a new ReliableFile object is created and returned.
	 * The use count of the returned ReliableFile object is incremented.
	 *
	 * @param file File object for the target file.
	 * @return A ReliableFile object for the target file.
	 * @throws IOException If the target file is a directory.
	 */
	static ReliableFile getReliableFile(File file) throws IOException {
		if (file.isDirectory()) {
			throw new FileNotFoundException(ReliableMsg.formatter.getString("RELIABLEFILE_FILE_IS_DIRECTORY"));
		}

		synchronized (files) {
			ReliableFile reliable = (ReliableFile) files.get(file);

			if (reliable == null) {
				reliable = new ReliableFile(file);

				files.put(file, reliable);
			}

			reliable.use++;

			return reliable;
		}
	}

	/**
	 * Decrement this object's use count. If the use count
	 * drops to zero, remove this object from the cache.
	 *
	 */
	private void release() {
		synchronized (files) {
			use--;

			if (use <= 0) {
				files.remove(orgFile);
			}
		}
	}

	/**
	 * Private constructor used by the static getReliableFile factory methods.
	 *
	 * @param file File object for the target file.
	 */
	private ReliableFile(File file) {
		String name = file.getPath();

		orgFile = file;
		tmpFile = new File(name + tmpExt);
		oldFile = new File(name + oldExt);
		newFile = new File(name + newExt);
		use = 0;
		locked = false;
	}

	/**
	 * Recovers the target file, if necessary, and returns an InputStream
	 * object for reading the target file.
	 *
	 * @return An InputStream object which can be used to read the target file.
	 * @throws IOException If an error occurs preparing the file.
	 */
	synchronized InputStream getInputStream() throws IOException {
		try {
			lock();
		} catch (IOException e) {
			/* the lock request failed; decrement the use count */
			release();

			throw e;
		}

		try {
			recoverFile();

			return new FileInputStream(orgFile.getPath());
		} catch (IOException e) {
			unlock();

			release();

			throw e;
		}
	}

	/**
	 * Close the target file for reading.
	 *
	 * @throws IOException If an error occurs closing the file.
	 */
	/* This method does not need to be synchronized if it only calls release. */
	void closeInputFile() throws IOException {
		unlock();

		release();
	}

	/**
	 * Recovers the target file, if necessary, and returns an OutputStream
	 * object for writing the target file.
	 *
	 * @return An OutputStream object which can be used to write the target file.
	 * @throws IOException If an error occurs preparing the file.
	 */
	synchronized OutputStream getOutputStream(boolean append) throws IOException {
		try {
			lock();
		} catch (IOException e) {
			/* the lock request failed; decrement the use count */
			release();

			throw e;
		}

		try {
			if (append) {
				recoverFile();

				if (orgFile.exists()) {
					cp(orgFile, tmpFile);
				}
			}

			return new FileOutputStream(tmpFile.getPath(), append);
		} catch (IOException e) {
			unlock();

			release();

			throw e;
		}
	}

	/**
	 * Close the target file for reading.
	 *
	 * @throws IOException If an error occurs closing the file.
	 */
	synchronized void closeOutputFile() throws IOException {
		try {
			boolean orgExists = orgFile.exists();
			boolean newExists = newFile.exists();

			if (newExists) {
				rm(oldFile);
				mv(newFile, oldFile);
			}

			mv(tmpFile, newFile);

			if (orgExists) {
				if (newExists) {
					rm(orgFile);
				} else {
					rm(oldFile);
					mv(orgFile, oldFile);
				}
			}

			mv(newFile, orgFile);
		} finally {
			unlock();

			release();
		}
	}

	/**
	 * This method recovers the reliable file if necessary.
	 *
	 * @throws IOException If an error occurs recovering the file.
	 */
	private void recoverFile() throws IOException {
		boolean orgExists = orgFile.exists();
		boolean newExists = newFile.exists();
		boolean oldExists = oldFile.exists();

		if (newExists) {
			if (orgExists && !oldExists) {
				mv(orgFile, oldFile);
			}

			cp(newFile, orgFile);

			if (orgExists || oldExists) {
				rm(newFile);
			} else {
				mv(newFile, oldFile);
			}
		} else {
			if (oldExists && !orgExists) {
				cp(oldFile, orgFile);
			}
		}
	}

	/**
	 * Lock the target file.
	 *
	 * @throws IOException If the file is already locked.
	 */
	private void lock() throws IOException {
		if (locked) {
			//TODO why not a regular IOException? 
			throw new FileNotFoundException(ReliableMsg.formatter.getString("RELIABLEFILE_FILE_LOCKED"));
		}

		locked = true;
	}

	/**
	 * Unlock the target file.
	 */
	private void unlock() {
		locked = false;
	}

	/**
	 * Rename a file.
	 *
	 * @param from The original file.
	 * @param to The new file name.
	 * @throws IOException If the rename failed.
	 */
	private static void mv(File from, File to) throws IOException {
		if (!from.renameTo(to)) {
			throw new IOException(ReliableMsg.formatter.getString("RELIABLEFILE_RENAME_FAILED"));
		}
	}

	/**
	 * Copy a file.
	 *
	 * @param from The original file.
	 * @param to The target file.
	 * @throws IOException If the copy failed.
	 */
	private static final int CP_BUF_SIZE = 4096;

	private static void cp(File from, File to) throws IOException {
		FileInputStream in = null;
		FileOutputStream out = null;

		try {
			out = new FileOutputStream(to);

			int length = (int) from.length();
			if (length > 0) {
				if (length > CP_BUF_SIZE) {
					length = CP_BUF_SIZE;
				}

				in = new FileInputStream(from);

				byte buffer[] = new byte[length];
				int count;
				while ((count = in.read(buffer, 0, length)) > 0) {
					out.write(buffer, 0, count);
				}

				in.close();
				in = null;
			}

			out.close();
			out = null;
		} catch (IOException e) {
			// close open streams
			if (out != null) {
				try {
					out.close();
				} catch (IOException ee) {
				}
			}

			if (in != null) {
				try {
					in.close();
				} catch (IOException ee) {
				}
			}

			throw e;
		}
	}

	/**
	 * Delete a file.
	 *
	 * @param file The file to delete.
	 * @throws IOException If the delete failed.
	 */
	private static void rm(File file) throws IOException {
		if (file.exists() && !file.delete()) {
			throw new IOException(ReliableMsg.formatter.getString("RELIABLEFILE_DELETE_FAILED"));
		}
	}

	/**
	 * Answers a boolean indicating whether or not the specified reliable file
	 * exists on the underlying file system.
	 *
	 * @return <code>true</code> if the specified reliable file exists,
	 * <code>false</code> otherwise.
	 */
	public static boolean exists(File file) {
		if (file.exists()) /* quick test */{
			return true;
		}

		String name = file.getPath();

		return new File(name + oldExt).exists() || new File(name + newExt).exists();
	}

	/**
	 * Delete this reliable file on the underlying file system.
	 *
	 * @throws IOException If the delete failed.
	 */
	private synchronized void delete() throws IOException {
		try {
			lock();
		} catch (IOException e) {
			/* the lock request failed; decrement the use count */
			release();

			throw e;
		}

		try {
			rm(oldFile);
			rm(orgFile);
			rm(newFile);
			rm(tmpFile);
		} finally {
			unlock();

			release();
		}
	}

	/**
	 * Delete the specified reliable file
	 * on the underlying file system.
	 *
	 * @return <code>true</code> if the specified reliable file was deleted,
	 * <code>false</code> otherwise.
	 */
	public static boolean delete(File file) {
		try {
			getReliableFile(file).delete();

			return true;
		} catch (IOException e) {
			return false;
		}
	}
}