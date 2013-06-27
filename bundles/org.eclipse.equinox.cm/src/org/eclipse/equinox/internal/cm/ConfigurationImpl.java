/*******************************************************************************
 * Copyright (c) 2005, 2013 Cognos Incorporated, IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Cognos Incorporated - initial API and implementation
 *     IBM Corporation - bug fixes and enhancements
 *******************************************************************************/
package org.eclipse.equinox.internal.cm;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.cm.*;

/**
 * ConfigurationImpl provides the Configuration implementation.
 * The lock and unlock methods are used for synchronization. Operations outside of
 * ConfigurationImpl that expect to have control of the lock should call checkLocked
 */
class ConfigurationImpl {
	final static String LOCATION_BOUND = "org.eclipse.equinox.cm.location.bound"; //$NON-NLS-1$
	final static String PROPERTIES_NULL = "org.eclipse.equinox.cm.properties.null"; //$NON-NLS-1$
	final static String CHANGE_COUNT = "org.eclipse.equinox.cm.change.count"; //$NON-NLS-1$

	private final ConfigurationAdminFactory configurationAdminFactory;
	private final ConfigurationStore configurationStore;
	/** @GuardedBy this*/
	private String bundleLocation;
	private final String factoryPid;
	private final String pid;
	private ConfigurationDictionary dictionary;
	/** @GuardedBy this*/
	private boolean deleted = false;
	/** @GuardedBy this*/
	private boolean bound = false;
	/** @GuardedBy this*/
	private int lockedCount = 0;
	/** @GuardedBy this*/
	private Thread lockHolder = null;
	/** @GuardedBy this*/
	private long changeCount;
	/** @GuardedBy this*/
	private Object storageToken;

	public ConfigurationImpl(ConfigurationAdminFactory configurationAdminFactory, ConfigurationStore configurationStore, String factoryPid, String pid, String bundleLocation) {
		this.configurationAdminFactory = configurationAdminFactory;
		this.configurationStore = configurationStore;
		this.factoryPid = factoryPid;
		this.pid = pid;
		this.bundleLocation = bundleLocation;
		this.changeCount = 0;
	}

	public ConfigurationImpl(ConfigurationAdminFactory configurationAdminFactory, ConfigurationStore configurationStore, Dictionary<String, ?> dictionary, Object storageToken) {
		this.configurationAdminFactory = configurationAdminFactory;
		this.configurationStore = configurationStore;
		pid = (String) dictionary.get(Constants.SERVICE_PID);
		factoryPid = (String) dictionary.get(ConfigurationAdmin.SERVICE_FACTORYPID);
		bundleLocation = (String) dictionary.get(ConfigurationAdmin.SERVICE_BUNDLELOCATION);
		Boolean boundProp = (Boolean) dictionary.remove(LOCATION_BOUND);
		this.bound = boundProp == null ? false : boundProp.booleanValue();
		Long changeCountProp = (Long) dictionary.remove(CHANGE_COUNT);
		this.changeCount = changeCountProp == null ? 0 : changeCountProp.longValue();
		Boolean nullProps = (Boolean) dictionary.remove(PROPERTIES_NULL);
		if (nullProps == null || !nullProps.booleanValue()) {
			updateDictionary(dictionary);
		}
		this.storageToken = storageToken;
	}

	synchronized void lock() {
		Thread current = Thread.currentThread();
		if (lockHolder != current) {
			boolean interrupted = false;
			try {
				while (lockedCount != 0)
					try {
						wait();
					} catch (InterruptedException e) {
						// although we don't handle an interrupt we should still 
						// save and restore the interrupt for others further up the stack
						interrupted = true;
					}
			} finally {
				if (interrupted)
					current.interrupt(); // restore interrupted status
			}
		}
		lockedCount++;
		lockHolder = current;
	}

	synchronized void unlock() {
		Thread current = Thread.currentThread();
		if (lockHolder != current)
			throw new IllegalStateException("Thread not lock owner"); //$NON-NLS-1$

		lockedCount--;
		if (lockedCount == 0) {
			lockHolder = null;
			notify();
		}
	}

	synchronized void checkLocked() {
		Thread current = Thread.currentThread();
		if (lockHolder != current)
			throw new IllegalStateException("Thread not lock owner"); //$NON-NLS-1$
	}

	boolean bind(String callerLocation) {
		try {
			lock();
			if (bundleLocation == null) {
				bundleLocation = callerLocation;
				bound = true;
				try {
					save();
				} catch (IOException e) {
					// TODO Log or throw runtime exception here?
					e.printStackTrace();
				}
				configurationAdminFactory.dispatchEvent(ConfigurationEvent.CM_LOCATION_CHANGED, factoryPid, pid);
			}
			return (callerLocation.equals(bundleLocation));
		} finally {
			unlock();
		}
	}

	boolean isBound() {
		try {
			lock();
			return bound;
		} finally {
			unlock();
		}
	}

	void unbind(Bundle bundle) {
		try {
			lock();
			String callerLocation = ConfigurationAdminImpl.getLocation(bundle);
			if (bound && callerLocation.equals(bundleLocation)) {
				bundleLocation = null;
				bound = false;
				try {
					save();
				} catch (IOException e) {
					// TODO What should we do here?  throw a runtime exception or log?
					e.printStackTrace();
				}
				configurationAdminFactory.notifyLocationChanged(this, callerLocation, factoryPid != null);
			}
		} finally {
			unlock();
		}
	}

	public void delete() {
		Object deleteToken;
		try {
			lock();
			checkDeleted();
			deleted = true;
			configurationAdminFactory.notifyConfigurationDeleted(this, factoryPid != null);
			configurationAdminFactory.dispatchEvent(ConfigurationEvent.CM_DELETED, factoryPid, pid);
			deleteToken = storageToken;
			storageToken = null;
		} finally {
			unlock();
		}
		configurationStore.removeConfiguration(pid, deleteToken);
	}

	private void checkDeleted() {
		if (deleted)
			throw new IllegalStateException("deleted"); //$NON-NLS-1$
	}

	String getLocation() {
		try {
			lock();
			return bundleLocation;
		} finally {
			unlock();
		}
	}

	public String getBundleLocation(String forBundleLocation) {
		try {
			lock();
			checkDeleted();
			if (forBundleLocation != null && !forBundleLocation.equals(bundleLocation))
				configurationAdminFactory.checkConfigurePermission(bundleLocation, forBundleLocation);
			if (bundleLocation != null)
				return bundleLocation;
			return null;
		} finally {
			unlock();
		}
	}

	String getFactoryPid(boolean checkDeleted) {
		try {
			lock();
			if (checkDeleted)
				checkDeleted();
			return factoryPid;
		} finally {
			unlock();
		}
	}

	public String getFactoryPid() {
		return getFactoryPid(true);
	}

	String getPid(boolean checkDeleted) {
		try {
			lock();
			if (checkDeleted)
				checkDeleted();
			return pid;
		} finally {
			unlock();
		}
	}

	public String getPid() {
		return getPid(true);
	}

	public Dictionary<String, Object> getProperties() {
		try {
			lock();
			checkDeleted();
			if (dictionary == null)
				return null;

			Dictionary<String, Object> copy = dictionary.copy();
			fileAutoProperties(copy, this, false, false);
			return copy;
		} finally {
			unlock();
		}
	}

	Dictionary<String, Object> getAllProperties(boolean includeStorageKeys) {
		try {
			lock();
			if (deleted)
				return null;
			Dictionary<String, Object> copy = getProperties();
			if (copy == null) {
				if (!includeStorageKeys) {
					return null;
				}
				copy = new ConfigurationDictionary();
			}
			fileAutoProperties(copy, this, true, includeStorageKeys);
			return copy;
		} finally {
			unlock();
		}
	}

	private static void fileAutoProperties(Dictionary<String, Object> dictionary, ConfigurationImpl config, boolean includeLoc, boolean includeStorageKey) {
		dictionary.put(Constants.SERVICE_PID, config.getPid(false));
		String factoryPid = config.getFactoryPid(false);
		if (factoryPid != null) {
			dictionary.put(ConfigurationAdmin.SERVICE_FACTORYPID, factoryPid);
		}
		if (includeLoc) {
			String loc = config.getLocation();
			if (loc != null) {
				dictionary.put(ConfigurationAdmin.SERVICE_BUNDLELOCATION, loc);
			}
		}
		if (includeStorageKey) {
			if (config.dictionary == null) {
				dictionary.put(PROPERTIES_NULL, Boolean.TRUE);
			}
			dictionary.put(CHANGE_COUNT, Long.valueOf(config.getChangeCount()));
			if (config.isBound()) {
				dictionary.put(LOCATION_BOUND, Boolean.TRUE);
			}
		}
	}

	public void setBundleLocation(String bundleLocation, String forBundleLocation) {
		try {
			lock();
			checkDeleted();
			configurationAdminFactory.checkConfigurePermission(this.bundleLocation, forBundleLocation);
			configurationAdminFactory.checkConfigurePermission(bundleLocation, forBundleLocation);
			String oldLocation = this.bundleLocation;
			this.bundleLocation = bundleLocation;
			this.bound = false;
			try {
				save();
			} catch (IOException e) {
				// TODO What should we do here?  throw a runtime exception or log?
				e.printStackTrace();
			}
			configurationAdminFactory.notifyLocationChanged(this, oldLocation, factoryPid != null);
			configurationAdminFactory.dispatchEvent(ConfigurationEvent.CM_LOCATION_CHANGED, factoryPid, pid);
		} finally {
			unlock();
		}
	}

	public void update() throws IOException {
		try {
			lock();
			checkDeleted();
			if (dictionary == null)
				dictionary = new ConfigurationDictionary();
			changeCount++;
			save();
			configurationAdminFactory.notifyConfigurationUpdated(this, factoryPid != null);
		} finally {
			unlock();
		}
	}

	public void update(Dictionary<String, ?> properties) throws IOException {
		try {
			lock();
			checkDeleted();
			updateDictionary(properties);
			changeCount++;
			save();
			configurationAdminFactory.notifyConfigurationUpdated(this, factoryPid != null);
			configurationAdminFactory.dispatchEvent(ConfigurationEvent.CM_UPDATED, factoryPid, pid);
		} finally {
			unlock();
		}
	}

	private void save() throws IOException {
		checkLocked();
		storageToken = configurationStore.saveConfiguration(pid, this, this.storageToken);
	}

	private void updateDictionary(Dictionary<String, ?> properties) {
		ConfigurationDictionary newDictionary = new ConfigurationDictionary();
		Enumeration<String> keys = properties.keys();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			if (newDictionary.get(key) == null) {
				Object value = properties.get(key);
				if (value.getClass().isArray()) {
					int arrayLength = Array.getLength(value);
					Object copyOfArray = Array.newInstance(value.getClass().getComponentType(), arrayLength);
					System.arraycopy(value, 0, copyOfArray, 0, arrayLength);
					newDictionary.put(key, copyOfArray);
				} else if (value instanceof Collection)
					newDictionary.put(key, new Vector<Object>((Collection<?>) value));
				else
					newDictionary.put(key, properties.get(key));
			} else
				throw new IllegalArgumentException(key + " is already present or is a case variant."); //$NON-NLS-1$
		}
		newDictionary.remove(Constants.SERVICE_PID);
		newDictionary.remove(ConfigurationAdmin.SERVICE_FACTORYPID);
		newDictionary.remove(ConfigurationAdmin.SERVICE_BUNDLELOCATION);

		dictionary = newDictionary;
	}

	public boolean equals(Object obj) {
		return (obj instanceof ConfigurationImpl) && pid.equals(((ConfigurationImpl) obj).getPid());
	}

	public int hashCode() {
		return pid.hashCode();
	}

	boolean isDeleted() {
		try {
			lock();
			return deleted;
		} finally {
			unlock();
		}
	}

	public long getChangeCount() {
		try {
			lock();
			checkDeleted();
			return changeCount;
		} finally {
			unlock();
		}
	}

	Configuration getConfiguration(String forBundleLocation) {
		return new ConfigurationForBundle(forBundleLocation);
	}

	class ConfigurationForBundle implements Configuration {
		private final String forBundleLocation;

		ConfigurationForBundle(String forBundleLocation) {
			this.forBundleLocation = forBundleLocation;
		}

		public String getPid() {
			return getImpl().getPid();
		}

		public Dictionary<String, Object> getProperties() {
			return getImpl().getProperties();
		}

		public void update(Dictionary<String, ?> properties) throws IOException {
			getImpl().update(properties);
		}

		public void delete() {
			getImpl().delete();
		}

		public String getFactoryPid() {
			return getImpl().getFactoryPid();
		}

		public void update() throws IOException {
			getImpl().update();
		}

		public void setBundleLocation(String location) {
			getImpl().setBundleLocation(location, forBundleLocation);
		}

		public String getBundleLocation() {
			return getImpl().getBundleLocation(forBundleLocation);
		}

		public long getChangeCount() {
			return getImpl().getChangeCount();
		}

		@Override
		public boolean equals(Object other) {
			if (!(other instanceof ConfigurationForBundle)) {
				return false;
			}
			return getImpl().equals(((ConfigurationForBundle) other).getImpl());
		}

		@Override
		public int hashCode() {
			return getImpl().hashCode();
		}

		private ConfigurationImpl getImpl() {
			return ConfigurationImpl.this;
		}
	}
}