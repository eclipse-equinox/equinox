package org.eclipse.core.runtime.adaptor;

import java.io.File;
import java.io.IOException;

/**
 */
public interface FileManager {
	/**
	 * Add the given file name to the list of files managed by this location.
	 * @param file path of the file to manage
	 */
	public void add(String file);

	/**
	 * Update the given target files with the content in the given source files.  
	 * The targets are file paths which are currently managed.  The sources are absolute 
	 * (or relative to the current working directory) file paths containing the new content
	 * for the corresponding target.
	 * 
	 * @param targets the target files to update
	 * @param sources the new content for the target files
	 * @throws IOException if there are any problems updating the given files
	 */
	public void update(String[] targets, String[] sources) throws IOException;

	/**
	 * Returns a list of all the file paths currently being managed.
	 * 
	 * @return the file paths being managed
	 */
	public String[] getFiles();

	/** 
	 * Returns the current numeric id (appendage) of the given file.  If the file is not 
	 * current then the content managed by this manager is at the path <code>
	 * file + "." + getId(file)</code>.
	 * 
	 * @param target the managed file to access
	 * @return the id of the file
	 */
	public int getId(String target);

	/**
	 * Returns true if the table entry for the given target matches the 
	 * current state of the filesystem.
	 * 
	 * @param target the managed file to check
	 * @return whether or not the given file matches the disk content
	 */
	public boolean isCurrent(String target);

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
	public boolean lock() throws IOException;

	/**
	 * Returns the actual file location to use when reading the given managed file.
	 * If the file is current then the result will be the same location.  If the file is not current
	 * the result will be a construction of the managed base location, the target path and
	 * the target's id.
	 * 
	 * @param target the managed file to lookup
	 * @return the actual file location to use for the given file
	 */
	public File lookup(String target);

	public String getBase();
	
	public void release();
	
	public long getTimeStamp(String target);

	public void remove(String file);

	public void save() throws IOException;
}