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
package org.eclipse.core.runtime.adaptor;

import java.io.*;
import java.net.URL;
import java.nio.channels.FileLock;
import org.eclipse.osgi.service.datalocation.Location;

public class BasicLocation implements Location {
	private boolean isReadOnly;
	private URL location = null;
	private Location parent;
	private URL defaultValue;
	private String property;
	
	// locking related fields
	private FileLock fileLock;
	private FileOutputStream fileStream;
	private File lockFile;

	private static String LOCK_FILENAME = ".metadata/.lock";

	public BasicLocation(String property, URL defaultValue, boolean isReadOnly) {
		super();
		this.property = property;
		this.defaultValue = defaultValue;
		this.isReadOnly = isReadOnly;
	}

	public boolean allowsDefault() {
		return defaultValue != null;
	}

	public URL getDefault() {
		return defaultValue;
	}
	
	public Location getParentLocation() {
		return parent;
	}

	public URL getURL() {
		if (location == null && defaultValue != null)
			setURL(defaultValue);
		return location;
	}

	public boolean isSet() {
		return location != null;
	}

	public boolean isReadOnly() {
		return isReadOnly;
	}

	/**
	 * @deprecated
	 */
	public void setURL(URL value) throws IllegalStateException {
		setURL(value , false);
	}

	public synchronized boolean setURL(URL value, boolean lock) throws IllegalStateException {
		if (location != null)
			throw new IllegalStateException("Cannot change the location once it is set");
		File file = null;
		if (value.getProtocol().equalsIgnoreCase("file")) {
			file = new File(value.getPath(), LOCK_FILENAME);
			boolean creation = file.mkdirs();
			if (! creation)
				return false;
		}
		if (lock) {
			try {
				if (!lock(file))
					return false;
			} catch (IOException e) {
				return false;
			}
		}
		lockFile = file;
		location = value;
		System.getProperties().put(property, location.toExternalForm());
		return true;
	}

	public void setParent(Location value) {
		parent = value;
	}

	public synchronized boolean lock() throws IOException {
		if (!isSet())
			return false;
		return lock(lockFile);
	}

	private boolean lock(File lock) throws IOException {
		if (lock == null)
			return false;
		fileStream = new FileOutputStream(lock, true);
		fileLock = fileStream.getChannel().tryLock();
		if (fileLock != null) 
			return true;
		fileStream.close();
		fileStream = null;
		fileLock = null;
		return false;
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
}
