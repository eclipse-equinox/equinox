/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.service.datalocation;

import java.io.*;
import java.util.*;
import org.eclipse.core.runtime.adaptor.*;
import org.eclipse.core.runtime.adaptor.BasicLocation;
import org.eclipse.core.runtime.adaptor.Locker;

/**
 * File managers provide a facility for tracking the state of files being used and updated by several
 * systems at the same time. The typical usecase is in shared configuration data areas.
 * <p>
 * The general principle is to maintain a table which maps user-level file name
 * onto actual disk file. The filename is actually never used, and the file is always stored under the
 * given filename suffixed by an integer. If a file needs to be modified, it is written into a new file whose name suffix 
 * is incremented.
 * Once the instance has been created, open() must be called before performing any other operation.
 * On open the fileManager starts by reading the current table and
 * thereby obtaining a snapshot of the current directory state. If another
 * entity updates the directory, the file manager is able to detect the change.
 * Given that the file is unique, if another entity used the file manager mechanism, the file manager can
 * still access the state of the file as it was when the file manager first started.
 * <p>
 * The facilities provided here are cooperative. That is, all participants must
 * agree to the conventions and to calling the given API. There is no capacity
 * to enforce these conventions or prohibit corruption.
 * </p>
 */
public class FileManager {
	private class Entry {
		int readId;
		int writeId;

		Entry(int readId, int writeId) {
			this.readId = readId;
			this.writeId = writeId;
		}

		int getReadId() {
			return readId;
		}

		int getWriteId() {
			return writeId;
		}

		void setReadId(int value) {
			readId = value;
		}

		void setWriteId(int value) {
			writeId = value;
		}
	}

	private File base; //The folder managed
	private File managerRoot; //The folder that will contain all the file related to the functionning of the manager (typically a subdir of base)

	private String lockMode = null;
	private File tableFile = null;
	private File lockFile; // The lock file for the table (this file is the same for all the instances)
	private Locker locker; // The locker for the lock

	private File instanceFile = null; //The file reprensenting the running instance. It is created when the table file is read.
	private Locker instanceLocker = null; //The locker for the instance file.

	// locking related fields
	private long tableStamp = 0L;

	private Properties table = new Properties();

	private static final String MANAGER_FOLDER = ".manager"; //$NON-NLS-1$
	private static final String TABLE_FILE = ".fileTable"; //$NON-NLS-1$
	private static final String LOCK_FILE = ".fileTableLock"; //$NON-NLS-1$

	/**
	 * Returns a new file manager for the area identified by the given base
	 * directory.
	 * 
	 * @param base the directory holding the files to be managed
	 * @param lockMode the lockMode to use for the given filemanager. It can have one the 3 values: none, java.io, java.nio 
	 * and also supports null in which case the lock strategy will be the global one.  
	 */
	public FileManager(File base, String lockMode) {
		this.base = base;
		this.lockMode = lockMode;
		this.managerRoot = new File(base, MANAGER_FOLDER);
		this.managerRoot.mkdirs();
		this.tableFile = new File(managerRoot, TABLE_FILE);
		this.lockFile = new File(managerRoot, LOCK_FILE);
	}

	private void initializeInstanceFile() throws IOException {
		if (instanceFile != null)
			return;
		this.instanceFile = File.createTempFile(".tmp", ".instance", managerRoot); //$NON-NLS-1$//$NON-NLS-2$
		this.instanceFile.deleteOnExit();
		instanceLocker = BasicLocation.createLocker(instanceFile, lockMode);
		instanceLocker.lock();
	}

	private String getAbsolutePath(String file) {
		return new File(base, file).getAbsolutePath();
	}

	/**
	 * Add the given file name to the list of files managed by this location.
	 * 
	 * @param file path of the file to manage
	 * @throws IOException if there are any problems adding the given file to the manager
	 */
	public void add(String file) throws IOException {
		if (! lock())
			throw new IOException(EclipseAdaptorMsg.formatter.getString("fileManager.cannotLock")); //$NON-NLS-1$
		try {
			updateTable();
			Entry entry = (Entry) table.get(file);
			if (entry == null) {
				table.put(file, new Entry(0, 1));
				save();
			}
		} finally {
			release();
		}
	}

	/**
	 * Update the given target files with the content in the given source files.
	 * The targets are file paths which are currently managed. The sources are
	 * absolute (or relative to the current working directory) file paths
	 * containing the new content for the corresponding target.
	 * 
	 * @param targets the target files to update
	 * @param sources the new content for the target files
	 * @throws IOException if there are any problems updating the given files
	 */
	public void update(String[] targets, String[] sources) throws IOException {
		if (! lock())
			throw new IOException(EclipseAdaptorMsg.formatter.getString("fileManager.cannotLock")); //$NON-NLS-1$;
		try {
			updateTable();
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
	 * Returns the directory containing the files being managed by this file
	 * manager.
	 * 
	 * @return the directory containing the managed files
	 */
	public File getBase() {
		return base;
	}

	/**
	 * Returns the current numeric id (appendage) of the given file.
	 * <code>file + "." + getId(file)</code>. -1 is returned if the given
	 * target file is not managed.
	 * 
	 * @param target
	 *                   the managed file to access
	 * @return the id of the file
	 */
	public int getId(String target) {
		Entry entry = (Entry) table.get(target);
		if (entry == null)
			return -1;
		return entry.getReadId();
	}

	/**
	 * Attempts to lock the state of this manager and returns <code>true</code>
	 * if the lock could be acquired.
	 * <p>
	 * Locking a manager is advisory only. That is, it does not prevent other
	 * applications from modifying the files managed by this manager.
	 * </p>
	 * 
	 * @exception IOException
	 *                         if there was an unexpected problem while acquiring the
	 *                         lock.
	 */
	private boolean lock() throws IOException {
		if (locker == null)
			locker = BasicLocation.createLocker(lockFile, lockMode);
		if (locker == null)
			throw new IOException(EclipseAdaptorMsg.formatter.getString("fileManager.cannotLock")); //$NON-NLS-1$
		return locker.lock();
	}

	/**
	 * Returns the actual file location to use when reading the given managed file. 
	 * <code>null</code> can be returned if the given target is not managed and add is set to false.
	 * 
	 * @param target the managed file to lookup
	 * @param add indicate whether the file should be added to the manager if it is not managed.
	 * @throws IOException if the add flag is set to true and the addition of the file failed
	 * @return the absolute file location to use for the given file or
	 *               <code>null</code> if the given target is not managed
	 */
	public File lookup(String target, boolean add) throws IOException {
		Entry entry = (Entry) table.get(target);
		if (entry == null) {
			if (add) { 
				add(target);
				entry = (Entry) table.get(target);
			} else {
				return null;
			}
		}
		return new File(getAbsolutePath(target + '.' + entry.getReadId()));
	}

	private void move(String source, String target) {
		File original = new File(source);
		// its ok if the original does not exist. The table entry will capture
		// that fact. There is no need to put something in the filesystem.
		if (!original.exists())
			return;
		original.renameTo(new File(target));
	}

	/**
	 * Saves the state of the file manager and releases any locks held.
	 */
	private void release() {
		if (locker == null)
			return;
		locker.release();
	}

	/**
	 * Removes the given file from management by this file manager.
	 * 
	 * @param file the file to remove
	 */
	public void remove(String file) throws IOException {
		// The removal needs to be done eagerly, so the value is effectively removed from the disktable. 
		// Otherwise, an updateTable() caused by an update(,)  could cause the file to readded to the local table.
		if (! lock())
			throw new IOException(EclipseAdaptorMsg.formatter.getString("fileManager.cannotLock")); //$NON-NLS-1$;
		try {
			updateTable();
			table.remove(file);
			save();
		} finally {
			release();
		}
	}

	private void updateTable() throws IOException {
		if (!tableFile.exists())
			return;
		long stamp = tableFile.lastModified();
		if (stamp == tableStamp)
			return;
		initializeInstanceFile();
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
			String readNumber = diskTable.getProperty(file);
			if (readNumber != null) {
				Entry entry = (Entry) table.get(file);
				int id = Integer.parseInt(readNumber);
				if (entry == null) {
					table.put(file, new Entry(id, id + 1));
				} else {
					entry.setWriteId(id + 1);
				}
			}
		}
	}

	/*
	 * This method should be called while the manager is locked.
	 */
	private void save() throws IOException {
		// if the table file has change on disk, update our data structures then
		// rewrite the file.
		if (tableStamp != tableFile.lastModified())
			updateTable();
		Properties props = new Properties();
		for (Enumeration e = table.keys(); e.hasMoreElements();) {
			String file = (String) e.nextElement();
			Entry entry = (Entry) table.get(file);
			String value = Integer.toString(entry.getWriteId() - 1); //In the table we save the write  number  - 1, because the read number can be totally different.
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
			throw new IOException(EclipseAdaptorMsg.formatter.getString("fileManager.couldNotSave")); //$NON-NLS-1$
		}
	}

	private void update(String target, String source) {
		Entry entry = (Entry) table.get(target);
		int newId = entry.getWriteId();
		move(getAbsolutePath(source), getAbsolutePath(target) + '.' + newId);
		// update the entry. read and write ids should be the same since
		// everything is in sync
		entry.setReadId(newId);
		entry.setWriteId(newId + 1);
	}

	/**
	 * This methods remove all the temporary files that have been created by the fileManager.
	 * This removal is only done if the instance of eclipse calling this method is the last instance using this fileManager.
	 * @throws IOException
	 */
	private void cleanup() throws IOException {
		//Iterate through the temp files and delete them all, except the one representing this filemanager.
		String[] files = managerRoot.list();
		if (files != null)
			for (int i = 0; i < files.length; i++) {
				if (files[i].endsWith(".instance") && instanceFile != null && !files[i].equalsIgnoreCase(instanceFile.getName())) { //$NON-NLS-1$
					Locker tmpLocker = BasicLocation.createLocker(new File(managerRoot, files[i]), lockMode);
					if (tmpLocker.lock()) {
						//If I can lock it is a file that has been left behind by a crash
						new File(managerRoot, files[i]).delete();
						tmpLocker.release();
					} else {
						tmpLocker.release();
						return; //The file is still being locked by somebody else
					}
				}
			}

		//If we are here it is because we are the last instance running. After locking the table and getting its latest content, remove all the backup files and change the table
		//If the exception comes from lock, another instance may have been started after we cleaned up, therefore we abort
		if (! lock())
			throw new IOException(EclipseAdaptorMsg.formatter.getString("fileManager.cannotLock")); //$NON-NLS-1$
		try {
			updateTable();
			Collection managedFiles = table.entrySet();
			for (Iterator iter = managedFiles.iterator(); iter.hasNext();) {
				Map.Entry fileEntry = (Map.Entry) iter.next();
				String fileName = (String) fileEntry.getKey();
				Entry info = (Entry) fileEntry.getValue();
				//Because we are cleaning up, we are giving up the values from our table, and we must delete all the files that are not referenced by the table
				String readId = Integer.toString(info.getWriteId() - 1);
				deleteCopies(fileName, readId);
			}
		} catch (IOException e) {
			//If the exception comes from the updateTable(), there has been a problem in reading the file.		 
			//If an exception occured in the save, then the table won't be up to date!
			throw e;
		} finally {
			release();
		}
	}

	private void deleteCopies(String fileName, String exceptionNumber) {
		String notToDelete = fileName + '.' + exceptionNumber;
		String[] files = base.list();
		if (files == null)
			return;
		for (int i = 0; i < files.length; i++) {
			if (files[i].startsWith(fileName + '.') && !files[i].equals(notToDelete)) //$NON-NLS-1$
				new File(base, files[i]).delete();
		}
	}

	/**
	 * This methods declare the fileManager as closed. From thereon, the instance can no longer be used.
	 * It is important to close the manager as it also cleanup old copies of the managed files.
	 */
	public void close() {
		try {
			cleanup();
		} catch (IOException e) {
			//Ignore and close.
		}
		if (instanceLocker != null)
			instanceLocker.release();
		
		if (instanceFile != null)
			instanceFile.delete();
	}

	/**
	 * This methods opens the fileManager, which loads the table in memory. This method must be called before any operation on the filemanager.
	 * @param wait indicates if the open operation must wait in case of contention on the lock file.
	 */
	public void open(boolean wait) throws IOException {
		boolean locked = lock();
		if (! locked && wait==false)
			throw new IOException(EclipseAdaptorMsg.formatter.getString("fileManager.cannotLock")); //$NON-NLS-1$;
		
		//wait for the lock to be released
		if (! locked) {
			do {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					// Ignore the exception and keep waiting
				}
			} while(lock());
		}
		
		try {
			updateTable();
		} finally {
			release();
		}
	}
}
