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
package org.eclipse.osgi.service.datalocation;

import java.io.*;
import java.util.*;
import org.eclipse.core.runtime.adaptor.BasicLocation;
import org.eclipse.core.runtime.adaptor.Locker;

/**
 * TODO this class must be completed and hooked or removed 
 * File managers provide a facility for tracking the state of files being used and updated
 * by several systems at the same time.  The typical usecase is in shared configuration data
 * areas.
 * <p>
 * The general principle is to maintain a table which maps user-level file name onto actual 
 * disk file.  The latest state of a given file is stored under the given filename. If that file is modified, 
 * a copy is made under a generated name.  When a file manager starts, it starts by
 * reading the current table and thereby obtaining a snapshot of the current directory state.
 * If another entity updates the directory, the file manager is able to detect the change.
 * If the other entity used the file manager mechanism, the file manager can still access the
 * state of the file as it was when the file manager first started.
 * <p>
 * The facilities provided here are cooperative.  That is, all participants must agree to the 
 * conventions and to calling the given API.  There is no capacity to enforce these 
 * conventions or prohibit corruption.
 * </p>
 */
public class FileManager {

	private class Entry {
		int readId;
		int writeId;
		long timeStamp;

		Entry(long timeStamp, int readId, int writeId) {
			this.timeStamp = timeStamp;
			this.readId = readId;
			this.writeId = writeId;
		}

		int getReadId() {
			return readId;
		}

		int getWriteId() {
			return writeId;
		}

		long getTimeStamp() {
			return timeStamp;
		}

		void setReadId(int value) {
			readId = value;
		}

		void setTimeStamp(long value) {
			timeStamp = value;
		}

		void setWriteId(int value) {
			writeId = value;
		}
	}

	// locking related fields
	private Locker locker;
	private File lockFile;
	private File base;
	private File tableFile = null;
	private long tableStamp = 0L;

	private Properties table = new Properties();

	private static final String TABLE_FILE = ".fileTable";
	private static final String LOCK_FILE = ".fileTableLock";

	/**
	 * Returns a new file manager for the area identified by the given base directory.
	 * 
	 * @param base the directory holding the files to be managed
	 * @throws IOException if there is a problem restoring the state of the 
	 * files being managed.
	 */
	public FileManager(File base) throws IOException {
		this.base = base;
		this.tableFile = new File(this.base, TABLE_FILE);
		this.lockFile = new File(this.base, LOCK_FILE);
		updateTable();
	}

	private String getAbsolutePath(String file) {
		return new File(base, file).getAbsolutePath();
	}

	/**
	 * Add the given file name to the list of files managed by this location.
	 * @param file path of the file to manage
	 */
	public void add(String file) {
		File target = new File(getAbsolutePath(file));
		long fileStamp = target.lastModified();
		Entry entry = new Entry(fileStamp, 1, 1);
		table.put(file, entry);
	}

	/**
	 * Update the given target files with the content in the given source files.  
	 * The targets are file paths which are currently managed.  The sources are absolute 
	 * (or relative to the current working directory) file paths containing the new content
	 * for the corresponding target.  The manager must be locked before calling this
	 * method.
	 * 
	 * @param targets the target files to update
	 * @param sources the new content for the target files
	 * @param force whether or not to force updating of files that are out of sync
	 * @throws IOException if there are any problems updating the given files or
	 * if some of the files being updated are out of sync.
	 */
	public void update(String[] targets, String[] sources, boolean force) throws IOException {
		lock();
		try {
			updateTable();
			if (!force) {
				for (int i = 0; i < targets.length; i++)
					if (!isCurrent(targets[i]))
						throw new IOException("Target: " + targets[i] + " is out of date");
			}
			for (int i = 0; i < targets.length; i++) {
				String target = targets[i];
				update(target, sources[i]);
			}
			save();
		} finally {
			release();
		}
	}

	/**
	 * Returns the result of converting a list of comma-separated tokens into an array
	 * 
	 * @return the array of string tokens
	 * @param prop the initial comma-separated string
	 */
	private String[] getArrayFromList(String prop) {
		if (prop == null || prop.trim().equals("")) //$NON-NLS-1$
			return new String[0];
		Vector list = new Vector();
		StringTokenizer tokens = new StringTokenizer(prop, ","); //$NON-NLS-1$
		while (tokens.hasMoreTokens()) {
			String token = tokens.nextToken().trim();
			if (!token.equals("")) //$NON-NLS-1$
				list.addElement(token);
		}
		return list.isEmpty() ? new String[0] : (String[]) list.toArray(new String[list.size()]);
	}

	/**
	 * Returns a list of all the file paths currently being managed.
	 * 
	 * @return the file paths being managed
	 */
	public String[] getFiles() {
		Set set = table.keySet();
		String[] keys = (String[]) set.toArray(new String[set.size()]);
		String[] result = new String[keys.length];
		for (int i = 0; i < keys.length; i++)
			result[i] = new String(keys[i]);
		return result;
	}

	/** 
	 * Returns the directory containing the files being managed by this file manager.
	 * @return the directory containing the managed files
	 */
	public File getBase() {
		return base;
	}

	/** 
	 * Returns the current numeric id (appendage) of the given file.  If the file is not 
	 * current then the content managed by this manager is at the path <code>
	 * file + "." + getId(file)</code>.  -1 is returned if the given target file is not managed.
	 * 
	 * @param target the managed file to access
	 * @return the id of the file
	 */
	public int getId(String target) {
		Entry entry = (Entry) table.get(target);
		if (entry == null)
			return -1;
		return entry.getReadId();
	}

	/**
	 * Returns true if the table entry for the given target matches the 
	 * current state of the filesystem.
	 * 
	 * @param target the managed file to check
	 * @return whether or not the given file matches the disk content
	 */
	public boolean isCurrent(String target) {
		Entry entry = (Entry) table.get(target);
		long tableStamp = entry == null ? -1 : entry.getTimeStamp();
		long fileStamp = new File(getAbsolutePath(target)).lastModified();
		return tableStamp == fileStamp;
	}

	/**
	 * Attempts to lock the state of this manager and returns
	 * <code>true</code> if the lock could be acquired.  
	 * <p>
	 * Locking a manager is advisory only.  That is, it does not prevent other applications from 
	 * modifying the files managed by this manager.
	 * </p>
	 * 
	 * @exception IOException if there was an unexpected problem while acquiring the lock.
	 */
	private boolean lock() throws IOException {
		if (locker == null)
			locker = BasicLocation.createLocker(lockFile, null);
		if (locker == null)
			throw new IOException("unable to create lock manager");
		return locker.lock();
	}

	/**
	 * Returns the actual file location to use when reading the given managed file.
	 * If the file is current then the result will be the same location.  If the file is not current
	 * the result will be a construction of the managed base location, the target path and
	 * the target's id.  <code>null</code> is returned if the given target is not managed.
	 * 
	 * @param target the managed file to lookup
	 * @return the absolute file location to use for the given file or <code>null</code> if
	 * 	the given target is not managed
	 */
	public File lookup(String target) {
		Entry entry = (Entry) table.get(target);
		if (entry == null)
			return null;
		File result = new File(getAbsolutePath(target));
		if (entry.getTimeStamp() == result.lastModified())
			return result;
		return new File(getAbsolutePath(target + "." + entry.getReadId()));
	}

	private void move(String source, String target) {
		File original = new File(source);
		// its ok if the original does not exist.  The table entry will capture 
		// that fact.  There is no need to put something in the filesystem.
		if (!original.exists())
			return;
		original.renameTo(new File(target));
	}

	/**
	 * Saves the state of the file manager and releases any locks held.
	 */
	private void release() throws IOException {
		if (locker == null)
			return;
		locker.release();
	}

	/**
	 * Removes the given file from management by this file manager.
	 * 
	 * @param file the file to remove
	 */
	public void remove(String file) {
		table.remove(file);
	}

	private void updateTable() throws IOException {
		if (!tableFile.exists())
			return;
		long stamp = tableFile.lastModified();
		if (stamp == tableStamp)
			return;
		Properties diskTable = new Properties();
		try {
			FileInputStream input = new FileInputStream(tableFile);
			try {
				diskTable.load(input);
			} finally {
				input.close();
			}
		} catch (IOException e) {
			throw e; // rethrow the exception, we have nothing to add here
		}
		tableStamp = stamp;
		for (Enumeration e = diskTable.keys(); e.hasMoreElements();) {
			String file = (String) e.nextElement();
			String[] elements = getArrayFromList((String) diskTable.get(file));
			if (elements.length > 0) {
				Entry entry = (Entry) table.get(file);
				int id = Integer.parseInt(elements[1]);
				if (entry == null) {
					table.put(file, new Entry(Long.parseLong(elements[0]), id, id));
				} else {
					entry.setWriteId(id);
				}
			}
		}
	}

	/*
	 * This method should be called while the manager is locked.
	 */
	private void save() throws IOException {
		// if the table file has change on disk, update our data structures then rewrite the file.
		if (tableStamp != tableFile.lastModified())
			updateTable();
		Properties props = new Properties();
		for (Enumeration e = table.keys(); e.hasMoreElements();) {
			String file = (String) e.nextElement();
			Entry entry = (Entry) table.get(file);
			String value = Long.toString(entry.getTimeStamp()) + "," + Integer.toString(entry.getWriteId());
			props.put(file, value);
		}

		FileOutputStream fileStream = new FileOutputStream(tableFile);
		try {
			try {
				props.store(fileStream, "safe table"); //$NON-NLS-1$
			} finally {
				fileStream.close();
			}
		} catch (IOException e) {
			throw new IOException("could not save file table");
		}
	}

	private void update(String target, String source) {
		String targetFile = getAbsolutePath(target);
		Entry entry = (Entry) table.get(target);
		int newId = entry.getWriteId();
		// remember the old file 
		move(targetFile, targetFile + "." + newId);
		// update the base file
		move(getAbsolutePath(source), targetFile);
		// update the entry.  read and write ids should be the same since everything is in sync
		newId++;
		entry.setReadId(newId);
		entry.setWriteId(newId);
		entry.setTimeStamp(new File(targetFile).lastModified());
	}

	/**
	 * Returns the timestamp of the given file as recorded by the file manager.
	 * 0 (zero) is returned if the file is not managed.
	 * 
	 * @param target the file to query
	 * @return the managed timestamp of the given file
	 */
	public long getTimeStamp(String target) {
		Entry entry = (Entry) table.get(target);
		if (entry == null)
			return 0L;
		return entry.getTimeStamp();
	}

}