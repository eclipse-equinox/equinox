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

	private static String LOCK_FILENAME = ".lock";

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

	public void setURL(URL value) throws IllegalStateException {
		if (location != null)
			throw new IllegalStateException("Cannot change the location once it is set");
		location = value;
		System.getProperties().put(property, location.toExternalForm());
		if (location.getProtocol().equalsIgnoreCase("file"))
			lockFile = new File(location.getPath(), LOCK_FILENAME);
	}

	public void setParent(Location value) {
		parent = value;
	}

	public boolean lock() throws IOException {
		fileStream = new FileOutputStream(lockFile, true);
		fileLock = fileStream.getChannel().tryLock();
		return fileLock != null;
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
