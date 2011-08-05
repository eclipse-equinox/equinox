/*******************************************************************************
 * Copyright (c) 2004, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.storagemanager;

import java.io.*;
import java.security.AccessController;
import java.util.*;
import org.eclipse.core.runtime.internal.adaptor.*;
import org.eclipse.osgi.framework.internal.reliablefile.*;
import org.eclipse.osgi.framework.util.SecureAction;

/**
 * Storage managers provide a facility for tracking the state of a group of files having 
 * relationship with each others and being updated by several entities at the same time. 
 * The typical usecase is in shared configuration data areas.
 * <p>
 * The facilities provided here are cooperative. That is, all participants must
 * agree to the conventions and to calling the given API. There is no capacity
 * to enforce these conventions or prohibit corruption.
 * </p>
 * <p>
 * Clients can not extend this class
 * </p>
 * <p>
 * Example
 * <pre>
 * //Open the storage manager
 * org.eclipse.osgi.storagemanager.StorageManager cacheStorageManager = new StorageManager("d:/sharedFolder/bar/", false); //$NON-NLS-1$
 * try {
 *	 cacheStorageManager.open(true);
 * } catch (IOException e) {
 * // Ignore the exception. The registry will be rebuilt from source.
 * }
 *
 * //To read from a file 
 * java.io.File fileA = cacheStorageManager.lookup("fileA", false));
 * java.io.File fileB = cacheStorageManager.lookup("fileB", false));
 * //Do the reading code 
 * new java.io.FileOutputStream(fileA);
 *
 * //To write in files 
 * cacheStorageManager.add("fileC"); //add the file to the filemanager (in this case we assume it is not already here)
 * cacheStorageManager.add("fileD");
 *
 * // The file is never written directly into the file name, so we create some temporary file
 * java.io.File fileC = cacheStorageManager.createTempFile("fileC");
 * java.io.File fileD = cacheStorageManager.createTempFile("fileD");
 *
 * //Do the actual writing here...
 *
 * //Finally update the storagemanager with the actual file to manage. 
 * cacheStorageManager.update(new String[] {"fileC", "fileD"}, new String[] {fileC.getName(), fileD.getName()};
 *
 * //Close the file manager at the end
 * cacheStorageManager.close();
 * </pre>
 * </p>
 * <p>
 * Implementation details <br>
 * The following implementation details are provided to help with understanding the 
 * behavior of this class.
 * The general principle is to maintain a table which maps user-level file names
 * onto an actual disk files.  If a file needs to be modified, 
 * it is stored into a new file.  The old content is not removed from disk until all entities
 * have closed there instance of the storage manager.
 * Once the instance has been created, open() must be called before performing any other operation.
 * On open the storage manager obtains a snapshot of the current managed files contents. If an
 * entity updates a managed file, the storage manager will save the content for that instance of the 
 * storage manager, all other storage manager instances will still have access to that managed file's 
 * content as it was when the instance was first opened.
 * </p>
 * @since 3.2
 */

// Note the implementation of this class originated from the following deprecated classes: 
// /org.eclipse.osgi/eclipseAdaptor/src/org/eclipse/core/runtime/adaptor/FileManager.java
// /org.eclipse.osgi/eclipseAdaptor/src/org/eclipse/core/runtime/adaptor/StreamManager.java
public final class StorageManager {
	private static final int FILETYPE_STANDARD = 0;
	private static final int FILETYPE_RELIABLEFILE = 1;
	private static final SecureAction secure = AccessController.doPrivileged(SecureAction.createSecureAction());
	private static final String MANAGER_FOLDER = ".manager"; //$NON-NLS-1$
	private static final String TABLE_FILE = ".fileTable"; //$NON-NLS-1$
	private static final String LOCK_FILE = ".fileTableLock"; //$NON-NLS-1$
	private static final int MAX_LOCK_WAIT = 5000; // 5 seconds 
	// these should be static but the tests expect to be able to create new managers after changing this setting dynamically
	private final boolean useReliableFiles = Boolean.valueOf(secure.getProperty("osgi.useReliableFiles")).booleanValue(); //$NON-NLS-1$
	private final boolean tempCleanup = Boolean.valueOf(secure.getProperty("osgi.embedded.cleanTempFiles")).booleanValue(); //$NON-NLS-1$
	private final boolean openCleanup = Boolean.valueOf(secure.getProperty("osgi.embedded.cleanupOnOpen")).booleanValue(); //$NON-NLS-1$
	private final boolean saveCleanup = Boolean.valueOf(secure.getProperty("osgi.embedded.cleanupOnSave")).booleanValue(); //$NON-NLS-1$

	private class Entry {
		int readId;
		int writeId;
		int fileType;

		Entry(int readId, int writeId, int type) {
			this.readId = readId;
			this.writeId = writeId;
			this.fileType = type;
		}

		int getReadId() {
			return readId;
		}

		int getWriteId() {
			return writeId;
		}

		int getFileType() {
			return fileType;
		}

		void setReadId(int value) {
			readId = value;
		}

		void setWriteId(int value) {
			writeId = value;
		}

		void setFileType(int type) {
			fileType = type;
		}
	}

	private final File base; //The folder managed
	private final File managerRoot; //The folder that will contain all the file related to the functionning of the manager (typically a subdir of base)

	private final String lockMode;
	private final File tableFile;
	private final File lockFile; // The lock file for the table (this file is the same for all the instances)
	private Locker locker; // The locker for the lock

	private File instanceFile; //The file representing the running instance. It is created when the table file is read.
	private Locker instanceLocker = null; //The locker for the instance file.
	private final boolean readOnly; // Whether this storage manager is in read-only mode
	private boolean open; // Whether this storage manager is open for use

	// locking related fields
	private int tableStamp = -1;

	private final Properties table = new Properties();

	/**
	 * Returns a new storage manager for the area identified by the given base
	 * directory.
	 * 
	 * @param base the directory holding the files to be managed
	 * @param lockMode the lockMode to use for the storage manager. It can have one the 3 values: none, java.io, java.nio 
	 * and also supports null in which case the lock strategy will be the global one.  
	 */
	public StorageManager(File base, String lockMode) {
		this(base, lockMode, false);
	}

	/**
	 * Returns a new storage manager for the area identified by the given base
	 * directory.
	 * 
	 * @param base the directory holding the files to be managed
	 * @param lockMode the lockMode to use for the storage manager. It can have one the 3 values: none, java.io, java.nio 
	 * and also supports null in which case the lock strategy will be the global one.  
	 * @param readOnly true if the managed files are read-only
	 */
	public StorageManager(File base, String lockMode, boolean readOnly) {
		this.base = base;
		this.lockMode = lockMode;
		this.managerRoot = new File(base, MANAGER_FOLDER);
		this.tableFile = new File(managerRoot, TABLE_FILE);
		this.lockFile = new File(managerRoot, LOCK_FILE);
		this.readOnly = readOnly;
		open = false;
	}

	private void initializeInstanceFile() throws IOException {
		if (instanceFile != null || readOnly)
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
	 * Add the given managed file name to the list of files managed by this manager.
	 * 
	 * @param managedFile name of the file to manage
	 * @throws IOException if there are any problems adding the given file name to the manager
	 */
	public void add(String managedFile) throws IOException {
		add(managedFile, FILETYPE_STANDARD);
	}

	/* (non-Javadoc
	 * Add the given file name to the list of files managed by this manager.
	 * 
	 * @param managedFile name of the file to manage.
	 * @param fileType the file type. 
	 * @throws IOException if there are any problems adding the given file to the manager
	 */
	private void add(String managedFile, int fileType) throws IOException {
		if (!open)
			throw new IOException(EclipseAdaptorMsg.fileManager_notOpen);
		if (readOnly)
			throw new IOException(EclipseAdaptorMsg.fileManager_illegalInReadOnlyMode);
		if (!lock(true))
			throw new IOException(EclipseAdaptorMsg.fileManager_cannotLock);
		try {
			updateTable();
			Entry entry = (Entry) table.get(managedFile);
			if (entry == null) {
				entry = new Entry(0, 1, fileType);
				table.put(managedFile, entry);
				// if this managed file existed before, ensure there is not an old
				// version on the disk to avoid name collisions. If version found,
				// us the oldest generation+1 for the write ID.
				int oldestGeneration = findOldestGeneration(managedFile);
				if (oldestGeneration != 0)
					entry.setWriteId(oldestGeneration + 1);
				save();
			} else {
				if (entry.getFileType() != fileType) {
					entry.setFileType(fileType);
					updateTable();
					save();
				}
			}
		} finally {
			release();
		}
	}

	/* (non-Javadoc)
	 * Find the oldest generation of a file still available on disk 
	 * @param file the file from which to obtain the oldest generation.
	 * @return the oldest generation of the file or 0 if the file does
	 * not exist. 
	 */
	private int findOldestGeneration(String managedFile) {
		String[] files = base.list();
		int oldestGeneration = 0;
		if (files != null) {
			String name = managedFile + '.';
			int len = name.length();
			for (int i = 0; i < files.length; i++) {
				if (!files[i].startsWith(name))
					continue;
				try {
					int generation = Integer.parseInt(files[i].substring(len));
					if (generation > oldestGeneration)
						oldestGeneration = generation;
				} catch (NumberFormatException e) {
					continue;
				}
			}
		}
		return oldestGeneration;
	}

	/**
	 * Update the given managed files with the content in the given source files.
	 * The managedFiles is a list of managed file names which are currently managed. 
	 * If a managed file name is not currently managed it will be added as a 
	 * managed file for this storage manager.
	 * The sources are absolute (or relative to the current working directory) 
	 * file paths containing the new content for the corresponding managed files.
	 * 
	 * @param managedFiles the managed files to update
	 * @param sources the new content for the managed files
	 * @throws IOException if there are any problems updating the given managed files
	 */
	public void update(String[] managedFiles, String[] sources) throws IOException {
		if (!open)
			throw new IOException(EclipseAdaptorMsg.fileManager_notOpen);
		if (readOnly)
			throw new IOException(EclipseAdaptorMsg.fileManager_illegalInReadOnlyMode);
		if (!lock(true))
			throw new IOException(EclipseAdaptorMsg.fileManager_cannotLock);
		try {
			updateTable();
			int[] originalReadIDs = new int[managedFiles.length];
			boolean error = false;
			for (int i = 0; i < managedFiles.length; i++) {
				originalReadIDs[i] = getId(managedFiles[i]);
				if (!update(managedFiles[i], sources[i]))
					error = true;
			}
			if (error) {
				// restore the original readIDs to avoid inconsistency for this group
				for (int i = 0; i < managedFiles.length; i++) {
					Entry entry = (Entry) table.get(managedFiles[i]);
					entry.setReadId(originalReadIDs[i]);
				}
				throw new IOException(EclipseAdaptorMsg.fileManager_updateFailed);
			}
			save(); //save only if no errors
		} finally {
			release();
		}
	}

	/**
	 * Returns a list of all the managed files currently being managed.
	 * 
	 * @return the names of the managed files
	 */
	public String[] getManagedFiles() {
		if (!open)
			return null;
		Set<Object> set = table.keySet();
		@SuppressWarnings("cast")
		String[] keys = (String[]) set.toArray(new String[set.size()]);
		String[] result = new String[keys.length];
		for (int i = 0; i < keys.length; i++)
			result[i] = new String(keys[i]);
		return result;
	}

	/**
	 * Returns the directory containing the files being managed by this storage
	 * manager.
	 * 
	 * @return the directory containing the managed files
	 */
	public File getBase() {
		return base;
	}

	/**
	 * Returns the current numeric id (appendage) of the given managed file.
	 * <code>managedFile + "." + getId(target)</code>. A value of -1 is returned 
	 * if the given name is not managed.
	 * 
	 * @param managedFile the name of the managed file
	 * @return the id of the managed file
	 */
	public int getId(String managedFile) {
		if (!open)
			return -1;
		Entry entry = (Entry) table.get(managedFile);
		if (entry == null)
			return -1;
		return entry.getReadId();
	}

	/**
	 * Returns if readOnly state this storage manager is using.
	 * 
	 * @return if this storage manager update state is read-only.
	 */
	public boolean isReadOnly() {
		return readOnly;
	}

	/* (non-Javadoc)
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
	private boolean lock(boolean wait) throws IOException {
		if (readOnly)
			return false;
		if (locker == null) {
			locker = BasicLocation.createLocker(lockFile, lockMode);
			if (locker == null)
				throw new IOException(EclipseAdaptorMsg.fileManager_cannotLock);
		}
		boolean locked = locker.lock();
		if (locked || !wait)
			return locked;
		//Someone else must have the directory locked, but they should release it quickly
		long start = System.currentTimeMillis();
		while (true) {
			try {
				Thread.sleep(200); // 5x per second
			} catch (InterruptedException e) {/*ignore*/
			}
			locked = locker.lock();
			if (locked)
				return true;
			// never wait longer than 5 seconds
			long time = System.currentTimeMillis() - start;
			if (time > MAX_LOCK_WAIT)
				return false;
		}
	}

	/**
	 * Returns the actual file location to use when reading the given managed file. 
	 * A value of <code>null</code> can be returned if the given managed file name is not 
	 * managed and add is set to false.  
	 * <p>
	 * The returned file should be considered read-only.  Any updates to the content of this
	 * file should be done using {@link #update(String[], String[])}.
	 * 
	 * @param managedFile the managed file to lookup
	 * @param add indicate whether the managed file name should be added to the manager if 
	 * it is not already managed.
	 * @throws IOException if the add flag is set to true and the addition of the managed file failed
	 * @return the absolute file location to use for the given managed file or
	 *               <code>null</code> if the given managed file is not managed
	 */
	public File lookup(String managedFile, boolean add) throws IOException {
		if (!open)
			throw new IOException(EclipseAdaptorMsg.fileManager_notOpen);
		Entry entry = (Entry) table.get(managedFile);
		if (entry == null) {
			if (add) {
				add(managedFile);
				entry = (Entry) table.get(managedFile);
			} else {
				return null;
			}
		}
		return new File(getAbsolutePath(managedFile + '.' + entry.getReadId()));
	}

	private boolean move(String source, String managedFile) {
		File original = new File(source);
		File targetFile = new File(managedFile);
		// its ok if the original does not exist. The table entry will capture
		// that fact. There is no need to put something in the filesystem.
		if (!original.exists() || targetFile.exists())
			return false;
		return original.renameTo(targetFile);
	}

	/**
	 * Saves the state of the storage manager and releases any locks held.
	 */
	private void release() {
		if (locker == null)
			return;
		locker.release();
	}

	/**
	 * Removes the given managed file from management by this storage manager.
	 * 
	 * @param managedFile the managed file to remove
	 * @throws IOException if an error occured removing the managed file
	 */
	public void remove(String managedFile) throws IOException {
		if (!open)
			throw new IOException(EclipseAdaptorMsg.fileManager_notOpen);
		if (readOnly)
			throw new IOException(EclipseAdaptorMsg.fileManager_illegalInReadOnlyMode);
		// The removal needs to be done eagerly, so the value is effectively removed from the disktable. 
		// Otherwise, an updateTable() caused by an update(,)  could cause the file to readded to the local table.
		if (!lock(true))
			throw new IOException(EclipseAdaptorMsg.fileManager_cannotLock);
		try {
			updateTable();
			table.remove(managedFile);
			save();
		} finally {
			release();
		}
	}

	private void updateTable() throws IOException {
		int stamp;
		stamp = ReliableFile.lastModifiedVersion(tableFile);
		if (stamp == tableStamp || stamp == -1)
			return;
		Properties diskTable = new Properties();
		InputStream input = new ReliableFileInputStream(tableFile);
		try {
			diskTable.load(input);
		} finally {
			try {
				input.close();
			} catch (IOException e) {
				// ignore
			}
		}
		tableStamp = stamp;
		for (Enumeration<Object> e = diskTable.keys(); e.hasMoreElements();) {
			String file = (String) e.nextElement();
			String value = diskTable.getProperty(file);
			if (value != null) {
				Entry entry = (Entry) table.get(file);
				// check front of value for ReliableFile
				int id;
				int fileType;
				int idx = value.indexOf(',');
				if (idx != -1) {
					id = Integer.parseInt(value.substring(0, idx));
					fileType = Integer.parseInt(value.substring(idx + 1));
				} else {
					id = Integer.parseInt(value);
					fileType = FILETYPE_STANDARD;
				}
				if (entry == null) {
					table.put(file, new Entry(id, id + 1, fileType));
				} else {
					entry.setWriteId(id + 1);
					//don't change type
				}
			}
		}
	}

	/*
	 * This method should be called while the manager is locked.
	 */
	private void save() throws IOException {
		if (readOnly)
			return;
		// if the table file has change on disk, update our data structures then
		// rewrite the file.
		updateTable();

		Properties props = new Properties();
		for (Enumeration<Object> e = table.keys(); e.hasMoreElements();) {
			String file = (String) e.nextElement();
			Entry entry = (Entry) table.get(file);
			String value;
			if (entry.getFileType() != FILETYPE_STANDARD) {
				value = Integer.toString(entry.getWriteId() - 1) + ',' + //In the table we save the write  number  - 1, because the read number can be totally different.
						Integer.toString(entry.getFileType());
			} else {
				value = Integer.toString(entry.getWriteId() - 1); //In the table we save the write  number  - 1, because the read number can be totally different.
			}
			props.put(file, value);
		}
		ReliableFileOutputStream fileStream = new ReliableFileOutputStream(tableFile);
		boolean error = true;
		try {
			props.store(fileStream, "safe table"); //$NON-NLS-1$
			fileStream.close();
			error = false;
		} finally {
			if (error)
				fileStream.abort();
		}
		// bug 259981 we should clean up
		if (saveCleanup) {
			try {
				cleanup(false);
			} catch (IOException ex) {
				// If IOException is thrown from our custom method.
				// log and swallow for now.
				System.out.println("Unexpected IOException is thrown inside cleanupWithLock. Please look below for stacktrace");
				ex.printStackTrace(System.out);
			}
		}
		tableStamp = ReliableFile.lastModifiedVersion(tableFile);
	}

	private boolean update(String managedFile, String source) throws IOException {
		Entry entry = (Entry) table.get(managedFile);
		if (entry == null)
			add(managedFile);
		int newId = entry.getWriteId();
		// attempt to rename the file to the next generation
		boolean success = move(getAbsolutePath(source), getAbsolutePath(managedFile) + '.' + newId);
		if (!success) {
			//possible the next write generation file exists? Lets determine the largest
			//generation number, then use that + 1.
			newId = findOldestGeneration(managedFile) + 1;
			success = move(getAbsolutePath(source), getAbsolutePath(managedFile) + '.' + newId);
		}
		if (!success)
			return false;
		// update the entry. read and write ids should be the same since
		// everything is in sync
		entry.setReadId(newId);
		entry.setWriteId(newId + 1);
		return true;
	}

	/**
	 * This methods remove all the temporary files that have been created by the storage manager.
	 * This removal is only done if the instance of eclipse calling this method is the last instance using this storage manager.
	 * @throws IOException
	 */
	private void cleanup(boolean doLock) throws IOException {
		if (readOnly)
			return;
		//Lock first, so someone else can not start while we're in the middle of cleanup
		if (doLock && !lock(true))
			throw new IOException(EclipseAdaptorMsg.fileManager_cannotLock);
		try {
			//Iterate through the temp files and delete them all, except the one representing this storage manager.
			String[] files = managerRoot.list();
			if (files != null) {
				for (int i = 0; i < files.length; i++) {
					if (files[i].endsWith(".instance") && (instanceFile == null || !files[i].equalsIgnoreCase(instanceFile.getName()))) { //$NON-NLS-1$
						Locker tmpLocker = BasicLocation.createLocker(new File(managerRoot, files[i]), lockMode);
						if (tmpLocker.lock()) {
							//If I can lock it is a file that has been left behind by a crash
							tmpLocker.release();
							new File(managerRoot, files[i]).delete();
						} else {
							tmpLocker.release();
							return; //The file is still being locked by somebody else
						}
					}
				}
			}

			//If we are here it is because we are the last instance running. After locking the table and getting its latest content, remove all the backup files and change the table
			updateTable();
			Collection<Map.Entry<Object, Object>> managedFiles = table.entrySet();
			for (Iterator<Map.Entry<Object, Object>> iter = managedFiles.iterator(); iter.hasNext();) {
				Map.Entry<Object, Object> fileEntry = iter.next();
				String fileName = (String) fileEntry.getKey();
				Entry info = (Entry) fileEntry.getValue();
				if (info.getFileType() == FILETYPE_RELIABLEFILE) {
					ReliableFile.cleanupGenerations(new File(base, fileName));
				} else {
					//Because we are cleaning up, we are giving up the values from our table, and we must delete all the files that are not referenced by the table
					String readId = Integer.toString(info.getWriteId() - 1);
					deleteCopies(fileName, readId);
				}
			}

			if (tempCleanup) {
				files = base.list();
				if (files != null) {
					for (int i = 0; i < files.length; i++) {
						if (files[i].endsWith(ReliableFile.tmpExt)) {
							new File(base, files[i]).delete();
						}
					}
				}
			}
		} finally {
			if (doLock)
				release();
		}
	}

	private void deleteCopies(String fileName, String exceptionNumber) {
		String notToDelete = fileName + '.' + exceptionNumber;
		String[] files = base.list();
		if (files == null)
			return;
		for (int i = 0; i < files.length; i++) {
			if (files[i].startsWith(fileName + '.') && !files[i].equals(notToDelete))
				new File(base, files[i]).delete();
		}
	}

	/**
	 * This method declares the storage manager as closed. From thereon, the instance can no longer be used.
	 * It is important to close the manager as it also cleans up old copies of the managed files.
	 */
	public void close() {
		if (!open)
			return;
		open = false;
		if (readOnly)
			return;
		try {
			cleanup(true);
		} catch (IOException e) {
			//Ignore and close.
		}
		if (instanceLocker != null)
			instanceLocker.release();

		if (instanceFile != null)
			instanceFile.delete();
	}

	/**
	 * This methods opens the storage manager. 
	 * This method must be called before any operation on the storage manager.
	 * @param wait indicates if the open operation must wait in case of contention on the lock file.
	 * @throws IOException if an error occurred opening the storage manager
	 */
	public void open(boolean wait) throws IOException {
		if (!readOnly) {
			managerRoot.mkdirs();
			if (!managerRoot.exists())
				throw new IOException(EclipseAdaptorMsg.fileManager_cannotLock);
			if (openCleanup)
				cleanup(true);
			boolean locked = lock(wait);
			if (!locked && wait)
				throw new IOException(EclipseAdaptorMsg.fileManager_cannotLock);
		}

		try {
			initializeInstanceFile();
			updateTable();
			open = true;
		} finally {
			release();
		}
	}

	/**
	 * Creates a new unique empty temporary-file in the storage manager base directory. The file name
	 * must be at least 3 characters. This file can later be used to update a managed file.
	 * <p>
	 * Note that {@link File#deleteOnExit()} is not called on the returned file.
	 * </p>
	 * @param file the file name to create temporary file from.
	 * @return the newly-created empty file.
	 * @throws IOException if the file can not be created.
	 * @see #update(String[], String[])
	 */
	public File createTempFile(String file) throws IOException {
		if (readOnly)
			throw new IOException(EclipseAdaptorMsg.fileManager_illegalInReadOnlyMode);
		File tmpFile = File.createTempFile(file, ReliableFile.tmpExt, base);
		// bug 350106: do not use deleteOnExit()  If clients really want that the
		// they can call it themselves.
		return tmpFile;
	}

	/**
	 * Returns a managed <code>InputStream</code> for a managed file. 
	 * <code>null</code> can be returned if the given name is not managed. 
	 * 
	 * @param managedFile the name of the managed file to open.
	 * @return an input stream to the managed file or 
	 * <code>null</code> if the given name is not managed.
	 * @throws IOException if the content is missing, corrupt or an error occurs.
	 */
	public InputStream getInputStream(String managedFile) throws IOException {
		return getInputStream(managedFile, ReliableFile.OPEN_BEST_AVAILABLE);
	}

	/**
	 * Returns a managed input stream set for the managed file names. 
	 * Elements of the returned set may be <code>null</code> if a given name is not managed.
	 * This method should be used for managed file sets which use the output streams returned 
	 * by the {@link #getOutputStreamSet(String[])} to save data.
	 * 
	 * @param managedFiles the names of the managed files to open.
	 * @return a set input streams to the given managed files.
	 * @throws IOException if the content of one of the managed files is missing, corrupt or an error occurs.
	 */
	public InputStream[] getInputStreamSet(String[] managedFiles) throws IOException {
		InputStream[] streams = new InputStream[managedFiles.length];
		for (int i = 0; i < streams.length; i++)
			streams[i] = getInputStream(managedFiles[i], ReliableFile.OPEN_FAIL_ON_PRIMARY);
		return streams;
	}

	private InputStream getInputStream(String managedFiles, int openMask) throws IOException {
		if (useReliableFiles) {
			int id = getId(managedFiles);
			if (id == -1)
				return null;
			return new ReliableFileInputStream(new File(getBase(), managedFiles), id, openMask);
		}
		File lookup = lookup(managedFiles, false);
		if (lookup == null)
			return null;
		return new FileInputStream(lookup);
	}

	/**
	 * Returns a <code>ManagedOutputStream</code> for a managed file.  
	 * Closing the ouput stream will update the storage manager with the 
	 * new content of the managed file.
	 * 
	 * @param managedFile the name of the managed file to write.
	 * @return a managed output stream for the managed file.
	 * @throws IOException if an error occurs opening the managed file.
	 */
	public ManagedOutputStream getOutputStream(String managedFile) throws IOException {
		if (useReliableFiles) {
			ReliableFileOutputStream out = new ReliableFileOutputStream(new File(getBase(), managedFile));
			return new ManagedOutputStream(out, this, managedFile, null);
		}
		File tmpFile = createTempFile(managedFile);
		return new ManagedOutputStream(new FileOutputStream(tmpFile), this, managedFile, tmpFile);
	}

	/**
	 * Returns an array of <code>ManagedOutputStream</code> for a set of managed files.
	 * When all managed output streams in the set have been closed, the storage manager
	 * will be updated with the new content of the managed files. 
	 * Aborting any one of the streams will cause the entire content of the set to abort 
	 * and be discarded.
	 * 
	 * @param managedFiles list of names of the managed file to write.
	 * @return an array of managed output streams respectively of managed files.
	 * @throws IOException if an error occurs opening the managed files.
	 */
	public ManagedOutputStream[] getOutputStreamSet(String[] managedFiles) throws IOException {
		int count = managedFiles.length;
		ManagedOutputStream[] streams = new ManagedOutputStream[count];
		int idx = 0;
		try {
			for (; idx < count; idx++) {
				ManagedOutputStream newStream = getOutputStream(managedFiles[idx]);
				newStream.setStreamSet(streams);
				streams[idx] = newStream;
			}
		} catch (IOException e) {
			// cleanup
			for (int jdx = 0; jdx < idx; jdx++)
				streams[jdx].abort();
			throw e;
		}
		return streams;
	}

	/* (non-Javadoc)
	 * Instructs this manager to abort and discard a managed output stream.
	 * This method should be used if any errors occur after opening a managed
	 * output stream where the contents should not be saved.
	 * If this output stream is part of a set, all other managed output streams in this set
	 * will also be closed and aborted.
	 * @param out the managed output stream
	 * @see #getOutputStream(String)
	 * @see #getOutputStreamSet(String[])
	 */
	void abortOutputStream(ManagedOutputStream out) {
		ManagedOutputStream[] set = out.getStreamSet();
		if (set == null) {
			set = new ManagedOutputStream[] {out};
		}
		synchronized (set) {
			for (int idx = 0; idx < set.length; idx++) {
				out = set[idx];
				if (out.getOutputFile() == null) {
					// this is a ReliableFileOutpuStream
					ReliableFileOutputStream rfos = (ReliableFileOutputStream) out.getOutputStream();
					rfos.abort();
				} else {
					// plain FileOutputStream();
					if (out.getState() == ManagedOutputStream.ST_OPEN) {
						try {
							out.getOutputStream().close();
						} catch (IOException e) {/*do nothing*/
						}
					}
					out.getOutputFile().delete();
				}
				out.setState(ManagedOutputStream.ST_CLOSED);
			}
		}
	}

	/* (non-Javadoc)
	 * Close the managed output stream and update the new content to  
	 * this manager. If this managed output stream is part of a set, only after closing
	 * all managed output streams in the set will storage manager be updated.
	 * 
	 * @param smos the output stream.
	 * @throws IOException if an errors occur.
	 * @see #getOutputStream(String)
	 * @see #getOutputStreamSet(String[])
	 */
	void closeOutputStream(ManagedOutputStream smos) throws IOException {
		if (smos.getState() != ManagedOutputStream.ST_OPEN)
			return;
		ManagedOutputStream[] streamSet = smos.getStreamSet();
		if (smos.getOutputFile() == null) {
			// this is a ReliableFileOutputStream
			ReliableFileOutputStream rfos = (ReliableFileOutputStream) smos.getOutputStream();
			// manage file deletes
			File file = rfos.closeIntermediateFile();
			smos.setState(ManagedOutputStream.ST_CLOSED);
			String target = smos.getTarget();
			if (streamSet == null) {
				add(target, StorageManager.FILETYPE_RELIABLEFILE);
				update(new String[] {smos.getTarget()}, new String[] {file.getName()});
				ReliableFile.fileUpdated(new File(getBase(), smos.getTarget()));
			}
		} else {
			// this is a plain old file output steam
			OutputStream out = smos.getOutputStream();
			out.flush();
			try {
				((FileOutputStream) out).getFD().sync();
			} catch (SyncFailedException e) {/*ignore*/
			}
			out.close();
			smos.setState(ManagedOutputStream.ST_CLOSED);
			String target = smos.getTarget();
			if (streamSet == null) {
				add(target, StorageManager.FILETYPE_STANDARD);
				update(new String[] {target}, new String[] {smos.getOutputFile().getName()});
			}
		}

		if (streamSet != null) {
			synchronized (streamSet) {
				//check all the streams to see if there are any left open....
				for (int idx = 0; idx < streamSet.length; idx++) {
					if (streamSet[idx].getState() == ManagedOutputStream.ST_OPEN)
						return; //done
				}
				//all streams are closed, we need to update storage manager
				String[] targets = new String[streamSet.length];
				String[] sources = new String[streamSet.length];
				for (int idx = 0; idx < streamSet.length; idx++) {
					smos = streamSet[idx];
					targets[idx] = smos.getTarget();
					File outputFile = smos.getOutputFile();
					if (outputFile == null) {
						// this is a ReliableFile 
						add(smos.getTarget(), StorageManager.FILETYPE_RELIABLEFILE);
						ReliableFileOutputStream rfos = (ReliableFileOutputStream) smos.getOutputStream();
						File file = rfos.closeIntermediateFile(); //multiple calls to close() ok
						sources[idx] = file.getName();
						ReliableFile.fileUpdated(new File(getBase(), smos.getTarget()));
					} else {
						add(smos.getTarget(), StorageManager.FILETYPE_STANDARD);
						sources[idx] = outputFile.getName();
					}
				}
				update(targets, sources);
			}
		}
	}
}