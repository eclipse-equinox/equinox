/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime.adaptor;

import java.io.*;
import java.nio.channels.FileLock;
import java.util.*;

public class BasicFileManager implements FileManager {
	
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
	private Properties table;

	private File tableFile = null;
	private static final String TABLE_FILE = ".fileTable";

	public BasicFileManager(String base) throws IOException {
		this.base = new File(base);
		this.tableFile = new File(this.base, TABLE_FILE);
		restore();
	}

	private String getAbsolutePath(String file) {
		return new File(base, file).getAbsolutePath();
	}
	public void add(String file) {
		File target = new File(getAbsolutePath(file));
		long fileStamp = target.lastModified();
		if (fileStamp == 0L)
			return;
		Entry entry = new Entry(fileStamp, 1);
		table.put(file, entry);
	}

	public void update(String[] targets, String[] sources) throws IOException {
		for (int i = 0; i < targets.length; i++)
			if (!isCurrent(targets[i]))
				throw new IOException("Target: " + targets[i] + " is out of date");
		for (int i = 0; i < targets.length; i++) {
			String target = targets[i];
			remember(target);
			update(target, sources[i]);
			// update the table id and timestamp
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

	public String[] getFiles() {
		Set set = table.keySet();
		String[] keys = (String[]) set.toArray(new String[set.size()]);
		String[] result = new String[keys.length];
		for (int i = 0; i < keys.length; i++)
			result[i] = new String(keys[i]);
		return result;
	}

	public String getBase() {
		return base.getAbsolutePath();
	}
	/**
	 * @param target
	 * @return
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
	 * @param target
	 * @return
	 */
	public boolean isCurrent(String target) {
		Entry entry = (Entry)table.get(target);
		long tableStamp = entry == null ? -1 : entry.getTimeStamp();
		long fileStamp = new File(getAbsolutePath(target)).lastModified();
		return tableStamp == fileStamp;
	}

	public boolean lock() throws IOException {
		fileStream = new FileOutputStream(tableFile, true);
		fileLock = fileStream.getChannel().tryLock();
		return fileLock != null;
	}

	public File lookup(String target) {
		Entry entry = (Entry)table.get(target);
		long tableStamp = entry == null ? -1 : entry.getTimeStamp();
		File result = new File(getAbsolutePath(target));
		long fileStamp = result.lastModified();
		if (tableStamp == fileStamp)
			return result;
		return new File(target + "." + entry.getId());
	}

	/**
	 * @param source
	 * @param target
	 */
	private void move(String source, String target) {
		File original = new File(source);
		original.renameTo(new File(target));
	}

	public void release() {
		if (fileLock != null) {
			try {
				fileLock.release();
			} catch (IOException e) {
				//don't complain, we're making a best effort to clean up
			}
			fileLock = null;
		}
		if (fileStream != null) {
			try {
				fileStream.close();
			} catch (IOException e) {
				//don't complain, we're making a best effort to clean up
			}
			fileStream = null;
		}
	}

	/**
	 * @param target
	 */
	private void remember(String target) {
		int id = getId(target);
		target = getAbsolutePath(target);
		String destination = target + "." + id;
		move(target, destination);
	}
	public void remove(String file) {
		table.remove(file);
	}

	private void restore() throws IOException {
		if (!tableFile.exists())
			return;
		try {
			FileInputStream input = new FileInputStream(tableFile);
			try {
				table.load(input);
			} finally {
				input.close();
			}
		} catch (IOException e) {
			throw new IOException("could not restore file table");
		}
		for (Enumeration e = table.keys(); e.hasMoreElements();) {
			String file = (String)e.nextElement();
			String[] elements = getArrayFromList((String)table.get(file));
			Entry entry = new Entry(Long.parseLong(elements[0]), Integer.parseInt(elements[1]));
			table.put(file, entry);
		}
	}

	public void save() throws IOException {
		Properties props = new Properties();
		for (Enumeration e = table.keys(); e.hasMoreElements();) {
			String file = (String)e.nextElement();
			Entry entry = (Entry)table.get(file);
			String value = Long.toString(entry.getTimeStamp()) + "," + Integer.toString(entry.getId());
			props.put(file, value);
		}
		try {
			FileOutputStream output = new FileOutputStream(tableFile);
			try {
				props.store(output, "safe table"); //$NON-NLS-1$
			} finally {
				output.close();
			}
		} catch (IOException e) {
			throw new IOException("could not save file table");
		}
	}

	/**
	 * @param target
	 * @param string
	 */
	private void update(String target, String source) {
		move(source, getAbsolutePath(target));
	}

	public long getTimeStamp(String target) {
		Entry entry = (Entry)table.get(target);
		if (entry == null)
			return 0L;
		return entry.getTimeStamp();
	}

}
