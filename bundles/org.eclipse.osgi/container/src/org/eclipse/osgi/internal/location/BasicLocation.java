/*******************************************************************************
 * Copyright (c) 2004, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.location;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration.ConfigValues;
import org.eclipse.osgi.internal.framework.EquinoxContainer;
import org.eclipse.osgi.internal.log.EquinoxLogServices;
import org.eclipse.osgi.internal.messages.Msg;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.util.NLS;

/**
 * Internal class.
 */
public class BasicLocation implements Location {
	private static String DEFAULT_LOCK_FILENAME = ".metadata/.lock"; //$NON-NLS-1$

	final private boolean isReadOnly;
	final private URL defaultValue;
	final private String property;
	final private String dataAreaPrefix;
	final private ConfigValues configValues;
	final private AtomicBoolean debug;
	final private EquinoxContainer container;

	private URL location = null;
	private Location parent;

	// locking related fields
	private File lockFile;
	private Locker locker;

	public BasicLocation(String property, URL defaultValue, boolean isReadOnly, String dataAreaPrefix, ConfigValues configValues, EquinoxContainer container, AtomicBoolean debug) {
		this.property = property;
		this.defaultValue = defaultValue;
		this.isReadOnly = isReadOnly;
		// make sure the prefix ends with '/' if it is not empty/null
		String tempDataAreaPrefix = dataAreaPrefix == null ? "" : dataAreaPrefix; //$NON-NLS-1$
		tempDataAreaPrefix = tempDataAreaPrefix.replace('\\', '/');
		if (tempDataAreaPrefix.length() > 0 && tempDataAreaPrefix.charAt(tempDataAreaPrefix.length() - 1) != '/') {
			tempDataAreaPrefix += '/';
		}
		this.dataAreaPrefix = tempDataAreaPrefix;
		this.configValues = configValues;
		this.container = container;
		this.debug = debug;
	}

	@Override
	public boolean allowsDefault() {
		return defaultValue != null;
	}

	@Override
	public URL getDefault() {
		return defaultValue;
	}

	@Override
	public synchronized Location getParentLocation() {
		return parent;
	}

	@Override
	public synchronized URL getURL() {
		if (location == null && defaultValue != null) {
			if (debug.get()) {
				EquinoxLogServices logServices = container.getLogServices();
				// Note that logServices can be null if we are very early in the startup.
				if (logServices != null) {
					logServices.log(EquinoxContainer.NAME, FrameworkLogEntry.INFO, "Called Location.getURL() when it has not been set for: \"" + property + "\"", new RuntimeException("Call stack for Location.getURL()")); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
				}
			}
			setURL(defaultValue, false);
		}
		return location;
	}

	@Override
	public synchronized boolean isSet() {
		return location != null;
	}

	@Override
	public boolean isReadOnly() {
		return isReadOnly;
	}

	/**
	 * @deprecated
	 */
	@Override
	public boolean setURL(URL value, boolean lock) throws IllegalStateException {
		try {
			return set(value, lock);
		} catch (IOException e) {
			return false;
		}
	}

	@Override
	public synchronized boolean set(URL value, boolean lock) throws IllegalStateException, IOException {
		return set(value, lock, null);
	}

	@Override
	public synchronized boolean set(URL value, boolean lock, String lockFilePath) throws IllegalStateException, IOException {
		if (location != null)
			throw new IllegalStateException(Msg.ECLIPSE_CANNOT_CHANGE_LOCATION);
		File file = null;
		if (value.getProtocol().equalsIgnoreCase("file")) { //$NON-NLS-1$
			try {
				File f = LocationHelper.decodePath(new File(value.getPath()));
				String basePath = f.getCanonicalPath();
				value = LocationHelper.buildURL("file:" + basePath, true); //$NON-NLS-1$
			} catch (IOException e) {
				// do nothing just use the original value
			}
			if (lockFilePath != null && lockFilePath.length() > 0) {
				File givenLockFile = new File(lockFilePath);
				if (givenLockFile.isAbsolute()) {
					file = givenLockFile;
				} else {
					file = new File(value.getPath(), lockFilePath);
				}
			} else {
				file = new File(value.getPath(), DEFAULT_LOCK_FILENAME);
			}
		}
		lock = lock && !isReadOnly;
		if (lock) {
			if (!lock(file, value))
				return false;
		}
		lockFile = file;
		location = value;
		if (property != null)
			configValues.setConfiguration(property, location.toExternalForm());
		return lock;
	}

	public synchronized void setParent(Location value) {
		parent = value;
	}

	@Override
	public synchronized boolean lock() throws IOException {
		if (!isSet())
			throw new IOException(Msg.location_notSet);
		return lock(lockFile, location);
	}

	@Override
	public synchronized boolean isLocked() throws IOException {
		if (!isSet())
			return false;
		return isLocked(lockFile);
	}

	/*
	 * This must be called while holding the synchronization lock for (this)
	 */
	private boolean lock(File lock, URL locationValue) throws IOException {
		if (isReadOnly)
			throw new IOException(NLS.bind(Msg.location_folderReadOnly, lock));
		if (lock == null) {
			if (locationValue != null && !"file".equalsIgnoreCase(locationValue.getProtocol())) //$NON-NLS-1$
				throw new IOException(NLS.bind(Msg.location_notFileProtocol, locationValue));
			throw new IllegalStateException(Msg.location_noLockFile); // this is really unexpected
		}
		if (isLocked())
			return false;
		File parentFile = new File(lock.getParent());
		if (!parentFile.isDirectory()) {
			parentFile.mkdirs();
			if (!parentFile.isDirectory())
				throw new IOException(NLS.bind(Msg.location_folderReadOnly, parentFile));
		}
		setLocker(lock);
		if (locker == null)
			return true;
		boolean locked = false;
		try {
			locked = locker.lock();
			return locked;
		} finally {
			if (!locked)
				locker = null;
		}
	}

	/*
	 * This must be called while holding the synchronization lock for (this)
	 */
	private boolean isLocked(File lock) throws IOException {
		if (lock == null || isReadOnly)
			return true;
		if (!lock.exists())
			return false;
		setLocker(lock);
		return locker.isLocked();
	}

	/*
	 * This must be called while holding the synchronization lock for (this)
	 */
	private void setLocker(File lock) {
		if (locker != null)
			return;
		String lockMode = configValues.getConfiguration(LocationHelper.PROP_OSGI_LOCKING, LocationHelper.LOCKING_NIO);
		locker = LocationHelper.createLocker(lock, lockMode, debug.get());
	}

	@Override
	public synchronized void release() {
		if (locker != null)
			locker.release();
	}

	@Override
	public Location createLocation(Location parentLocation, URL defaultLocation, boolean readonly) {
		BasicLocation result = new BasicLocation(null, defaultLocation, readonly, dataAreaPrefix, configValues, container, debug);
		result.setParent(parentLocation);
		return result;
	}

	@Override
	public URL getDataArea(String filename) throws IOException {
		URL base = getURL();
		if (base == null)
			throw new IOException(Msg.location_notSet);
		String prefix = base.toExternalForm();
		if (prefix.length() > 0 && prefix.charAt(prefix.length() - 1) != '/')
			prefix += '/';
		filename = filename.replace('\\', '/');
		if (filename.length() > 0 && filename.charAt(0) == '/')
			filename = filename.substring(1);
		String spec = prefix + dataAreaPrefix + filename;
		boolean trailingSlash = spec.length() > 0 && spec.charAt(spec.length() - 1) == '/';
		return LocationHelper.buildURL(spec, trailingSlash);
	}
}
