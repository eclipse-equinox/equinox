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
import java.nio.channels.FileLock;
import java.util.*;

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
		int id;
		long timeStamp;
		
		Entry(long timeStamp, int id) {
			this.timeStamp = timeStamp;
			this.id = id;
		}
		int getId() {
			return id;
		}
		long getTimeStamp() {
			return timeStamp;
		}
		void setId(int value) {
			id = value;
		}
		void setTimeStamp(long value) {
			timeStamp = value;
		}
	}

	// locking related fields
	private FileLock fileLock;
	private FileOutputStream fileStream;
	private File base;
	private File tableFile = null;
	private long tableStamp = 0L;
	
	private Properties table = new Properties();
	private ArrayList changed = new ArrayList(5);

	private static final String TABLE_FILE = ".fileTable";

	
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
		restore();
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
		Entry entry = new Entry(fileStamp, 1);
		table.put(file, entry);
		changed.add(file);
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
	 * @throws IOException if there are any problems updating the given files
	 */
	public void update(String[] targets, String[] sources) throws IOException {
		if (fileStream == null)
			throw new IllegalStateException("Manager must be locked to update");
		for (int i = 0; i < targets.length; i++)
			if (!isCurrent(targets[i]))
				throw new IOException("Target: " + targets[i] + " is out of date");
		for (int i = 0; i < targets.length; i++) {
			String target = targets[i];
			remember(target);
			update(target, sources[i]);
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
		Entry entry = (Entry)table.get(target);
		if (entry == null)
			return -1;
		return entry.getId();
	}

	/**
	 * Returns true if the table entry for the given target matches the 
	 * current state of the filesystem.
	 * 
	 * @param target the managed file to check
	 * @return whether or not the given file matches the disk content
	 */
	public boolean isCurrent(String target) {
		Entry entry = (Entry)table.get(target);
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
	public boolean lock() throws IOException {
		fileStream = new FileOutputStream(tableFile, true);
		fileLock = fileStream.getChannel().tryLock();
		return fileLock != null;
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
		Entry entry = (Entry)table.get(target);
		if (entry == null)
			return null;
		File result = new File(getAbsolutePath(target));
		if (entry.getTimeStamp() == result.lastModified())
			return result;
		return new File(getAbsolutePath(target + "." + entry.getId()));
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
	 * 
	 * @throws IOException if a problem is encountered while saving the manager information
	 */
	public void release() throws IOException {
		if (fileStream != null) {
			save();
			try {
				fileStream.close();
			} catch (IOException e) {
				//don't complain, we're making a best effort to clean up
			}
			fileStream = null;
		}
		if (fileLock != null) {
			try {
				fileLock.release();
			} catch (IOException e) {
				//don't complain, we're making a best effort to clean up
			}
			fileLock = null;
		}
	}

	private void remember(String target) {
		int id = getId(target);
		target = getAbsolutePath(target);
		String destination = target + "." + id;
		move(target, destination);
	}
	
	/**
	 * Removes the given file from management by this file manager.
	 * 
	 * @param file the file to remove
	 */
	public void remove(String file) {
		table.remove(file);
	}

	private void restore() throws IOException {
		if (!tableFile.exists())
			return;
		try {
			FileInputStream input = new FileInputStream(tableFile);
			try {
				tableStamp = tableFile.lastModified();
				table.load(input);
			} finally {
				input.close();
			}
		} catch (IOException e) {
			throw new IOException("could not restore file table");
		}
		for (Enumeration e = table.keys(); e.hasMoreElements();) {
			String file = (String)e.nextElement();
			// if the entry has changed internally, update the value.
			String[] elements = getArrayFromList((String)table.get(file));
			if (changed.indexOf(file) == -1) {
				Entry entry = new Entry(Long.parseLong(elements[0]), Integer.parseInt(elements[1]));
				table.put(file, entry);
			} else {
				Entry entry = (Entry)table.get(file);
				entry.setId(entry.getId() + 1);
				table.put(file, entry);
			}
		}
	}

	/*
	 * This method should be called while the manager is locked.
	 */
	private void save() throws IOException {
		// if the table file has change on disk, update our data structures then rewrite the file.
		if (tableStamp != tableFile.lastModified())
			restore();
		Properties props = new Properties();
		for (Enumeration e = table.keys(); e.hasMoreElements();) {
			String file = (String)e.nextElement();
			Entry entry = (Entry)table.get(file);
			String value = Long.toString(entry.getTimeStamp()) + "," + Integer.toString(entry.getId());
			props.put(file, value);
		}
		
		try {
			props.store(fileStream, "safe table"); //$NON-NLS-1$
		} catch (IOException e) {
			throw new IOException("could not save file table");
		}
	}

	private void update(String target, String source) {
		String targetFile = getAbsolutePath(target);
		move(getAbsolutePath(source), targetFile);
		Entry entry = (Entry)table.get(target);
		entry.setTimeStamp(new File(targetFile).lastModified());
		entry.setId(entry.getId() + 1);
		changed.add(target);
	}

	/**
	 * Returns the timestamp of the given file as recorded by the file manager.
	 * 0 (zero) is returned if the file is not managed.
	 * 
	 * @param target the file to query
	 * @return the managed timestamp of the given file
	 */
	public long getTimeStamp(String target) {
		Entry entry = (Entry)table.get(target);
		if (entry == null)
			return 0L;
		return entry.getTimeStamp();
	}

}
