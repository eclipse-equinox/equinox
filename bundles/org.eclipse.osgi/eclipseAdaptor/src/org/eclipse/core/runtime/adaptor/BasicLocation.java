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

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.osgi.service.datalocation.Location;

public class BasicLocation implements Location {
	private boolean isReadOnly;
	private URL location = null;
	private Location parent;
	private URL defaultValue;
	private String property;

	// locking related fields
	private File lockFile;
	private Locker locker;
	private static final String PROP_OSGI_LOCKING = "osgi.locking"; //$NON-NLS-1$
	private static String LOCK_FILENAME = ".metadata/.lock"; //$NON-NLS-1$

	//TODO could not this constructor take the parent as well?
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

	//TODO use synchronized 
	public URL getURL() {
		if (location == null && defaultValue != null)
			setURL(defaultValue, false);
		return location;
	}

	//TODO use synchronized
	public boolean isSet() {
		return location != null;
	}

	public boolean isReadOnly() {
		return isReadOnly;
	}

	public synchronized boolean setURL(URL value, boolean lock) throws IllegalStateException {
		if (location != null)
			throw new IllegalStateException(EclipseAdaptorMsg.formatter.getString("ECLIPSE_CANNOT_CHANGE_LOCATION")); //$NON-NLS-1$
		File file = null;
		if (value.getProtocol().equalsIgnoreCase("file")) //$NON-NLS-1$
			file = new File(value.getPath(), LOCK_FILENAME);
		lock = lock && !isReadOnly;
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
		if (property != null)
			System.getProperties().put(property, location.toExternalForm());
		return lock;
	}

	//TODO use synchronized or remove if passed in constructor	
	public void setParent(Location value) {
		parent = value;
	}

	public synchronized boolean lock() throws IOException {
		if (!isSet())
			return false;
		return lock(lockFile);
	}

	private boolean lock(File lock) throws IOException {
		if (lock == null || isReadOnly)
			return false;

		File parentFile = lock.getParentFile();
		//TODO if (!parentFile.mkdirs()) would be enough/more correct
		if (!parentFile.exists())
			if (!parentFile.mkdirs())
				return false;

		setLocker(lock);
		if (locker == null)
			return true;
		return locker.lock(); //TODO Why do we let the IOException flow instead of returning false
		//TODO 2: because false means locked by somebody else, exceptions means error locking
	}

	private void setLocker(File lock) {
		if (locker != null)
			return;
		// TODO return early, avoid if/else/if/else...
		String lockMode = System.getProperties().getProperty(PROP_OSGI_LOCKING);
		if (lockMode == null) { //By default set the lock mode to 1.4
			if (runningWithNio()) {
				locker = new Locker_JavaNio(lock);
			} else {
				locker = new Locker_JavaIo(lock);
			}
		} else if ("none".equals(lockMode)) { //$NON-NLS-1$
			return;
		} else if ("java.io".equals(lockMode)) { //$NON-NLS-1$
			locker = new Locker_JavaIo(lock);
		} else if ("java.nio".equals(lockMode)) { //$NON-NLS-1$
			if (runningWithNio()) {
				locker = new Locker_JavaNio(lock);
			} else {
				locker = new Locker_JavaIo(lock);
			}
		} else {
			//TODO need to check NIO is available 
			//	Backup case if an invalid value has been specified
			locker = new Locker_JavaNio(lock);
		}
	}

	//TODO use synchronized
	public void release() {
		if (locker != null)
			locker.release();
	}

	//TODO: isRunningWithNIO or hasNIO or...
	private boolean runningWithNio() {
		try {
			Class c = Class.forName("java.nio.channels.FileLock"); //$NON-NLS-1$
		} catch (ClassNotFoundException e) {
			return false;
		}
		return true;
	}
}