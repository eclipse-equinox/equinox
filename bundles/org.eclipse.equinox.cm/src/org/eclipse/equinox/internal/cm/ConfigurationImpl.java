/*******************************************************************************
 * Copyright (c) 2005, 2010 Cognos Incorporated, IBM Corporation and others.
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
class ConfigurationImpl implements Configuration {

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
	private Bundle boundBundle;
	/** @GuardedBy this*/
	private int lockedCount = 0;
	/** @GuardedBy this*/
	private Thread lockHolder = null;

	public ConfigurationImpl(ConfigurationAdminFactory configurationAdminFactory, ConfigurationStore configurationStore, String factoryPid, String pid, String bundleLocation) {
		this.configurationAdminFactory = configurationAdminFactory;
		this.configurationStore = configurationStore;
		this.factoryPid = factoryPid;
		this.pid = pid;
		this.bundleLocation = bundleLocation;
	}

	public ConfigurationImpl(ConfigurationAdminFactory configurationAdminFactory, ConfigurationStore configurationStore, Dictionary dictionary) {
		this.configurationAdminFactory = configurationAdminFactory;
		this.configurationStore = configurationStore;
		pid = (String) dictionary.get(Constants.SERVICE_PID);
		factoryPid = (String) dictionary.get(ConfigurationAdmin.SERVICE_FACTORYPID);
		bundleLocation = (String) dictionary.get(ConfigurationAdmin.SERVICE_BUNDLELOCATION);
		updateDictionary(dictionary);
	}

	protected synchronized void lock() {
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

	protected synchronized void unlock() {
		Thread current = Thread.currentThread();
		if (lockHolder != current)
			throw new IllegalStateException("Thread not lock owner"); //$NON-NLS-1$

		lockedCount--;
		if (lockedCount == 0) {
			lockHolder = null;
			notify();
		}
	}

	protected synchronized void checkLocked() {
		Thread current = Thread.currentThread();
		if (lockHolder != current)
			throw new IllegalStateException("Thread not lock owner"); //$NON-NLS-1$
	}

	protected boolean bind(Bundle bundle) {
		try {
			lock();
			if (boundBundle == null && (bundleLocation == null || bundleLocation.equals(bundle.getLocation())))
				boundBundle = bundle;
			return (boundBundle == bundle);
		} finally {
			unlock();
		}
	}

	protected void unbind(Bundle bundle) {
		try {
			lock();
			if (boundBundle == bundle)
				boundBundle = null;
		} finally {
			unlock();
		}
	}

	public void delete() throws IOException {
		try {
			lock();
			checkDeleted();
			deleted = true;
			configurationAdminFactory.notifyConfigurationDeleted(this, factoryPid != null);
			configurationAdminFactory.dispatchEvent(ConfigurationEvent.CM_DELETED, factoryPid, pid);
		} finally {
			unlock();
		}
		configurationStore.removeConfiguration(pid);
	}

	private void checkDeleted() {
		if (deleted)
			throw new IllegalStateException("deleted"); //$NON-NLS-1$
	}

	public String getBundleLocation() {
		return getBundleLocation(true);
	}

	protected String getBundleLocation(boolean checkPermission) {
		try {
			lock();
			checkDeleted();
			if (checkPermission)
				configurationAdminFactory.checkConfigurationPermission();
			if (bundleLocation != null)
				return bundleLocation;
			if (boundBundle != null)
				return boundBundle.getLocation();
			return null;
		} finally {
			unlock();
		}
	}

	protected String getFactoryPid(boolean checkDeleted) {
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

	protected String getPid(boolean checkDeleted) {
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

	public Dictionary getProperties() {
		try {
			lock();
			checkDeleted();
			if (dictionary == null)
				return null;

			Dictionary copy = dictionary.copy();
			copy.put(Constants.SERVICE_PID, pid);
			if (factoryPid != null)
				copy.put(ConfigurationAdmin.SERVICE_FACTORYPID, factoryPid);

			return copy;
		} finally {
			unlock();
		}
	}

	protected Dictionary getAllProperties() {
		try {
			lock();
			if (deleted)
				return null;
			Dictionary copy = getProperties();
			if (copy == null)
				return null;
			String boundLocation = getBundleLocation(false);
			if (boundLocation != null)
				copy.put(ConfigurationAdmin.SERVICE_BUNDLELOCATION, boundLocation);
			return copy;
		} finally {
			unlock();
		}
	}

	public void setBundleLocation(String bundleLocation) {
		try {
			lock();
			checkDeleted();
			configurationAdminFactory.checkConfigurationPermission();
			this.bundleLocation = bundleLocation;
			boundBundle = null; // always reset the boundBundle when setBundleLocation is called
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
			configurationStore.saveConfiguration(pid, this);
			configurationAdminFactory.notifyConfigurationUpdated(this, factoryPid != null);
		} finally {
			unlock();
		}
	}

	public void update(Dictionary properties) throws IOException {
		try {
			lock();
			checkDeleted();
			updateDictionary(properties);
			configurationStore.saveConfiguration(pid, this);
			configurationAdminFactory.notifyConfigurationUpdated(this, factoryPid != null);
			configurationAdminFactory.dispatchEvent(ConfigurationEvent.CM_UPDATED, factoryPid, pid);
		} finally {
			unlock();
		}
	}

	private void updateDictionary(Dictionary properties) {
		ConfigurationDictionary newDictionary = new ConfigurationDictionary();
		Enumeration keys = properties.keys();
		while (keys.hasMoreElements()) {
			Object key = keys.nextElement();
			if (newDictionary.get(key) == null) {
				Object value = properties.get(key);
				if (value.getClass().isArray()) {
					int arrayLength = Array.getLength(value);
					Object copyOfArray = Array.newInstance(value.getClass().getComponentType(), arrayLength);
					System.arraycopy(value, 0, copyOfArray, 0, arrayLength);
					newDictionary.put(key, copyOfArray);
				} else if (value instanceof Collection)
					newDictionary.put(key, new Vector((Collection) value));
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
		return (obj instanceof Configuration) && pid.equals(((Configuration) obj).getPid());
	}

	public int hashCode() {
		return pid.hashCode();
	}

	protected boolean isDeleted() {
		try {
			lock();
			return deleted;
		} finally {
			unlock();
		}
	}
}