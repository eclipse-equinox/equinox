/*******************************************************************************
 * Copyright (c) 2005, 2019 Cognos Incorporated, IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Cognos Incorporated - initial API and implementation
 *     IBM Corporation - bug fixes and enhancements
 *     IBH SYSTEMS GmbH - replace custom lock with a ReentrantLock, bug 459002
 *******************************************************************************/
package org.eclipse.equinox.internal.cm;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import org.osgi.framework.*;
import org.osgi.service.cm.*;

/**
 * ConfigurationImpl provides the Configuration implementation. The lock and
 * unlock methods are used for synchronization. Operations outside of
 * ConfigurationImpl that expect to have control of the lock should call
 * checkLocked
 */
class ConfigurationImpl implements Configuration {
	final static String LOCATION_BOUND = "org.eclipse.equinox.cm.location.bound"; //$NON-NLS-1$
	final static String PROPERTIES_NULL = "org.eclipse.equinox.cm.properties.null"; //$NON-NLS-1$
	final static String CHANGE_COUNT = "org.eclipse.equinox.cm.change.count"; //$NON-NLS-1$
	final static String READ_ONLY = "org.eclipse.equinox.cm.readonly"; //$NON-NLS-1$

	private final ConfigurationAdminFactory configurationAdminFactory;
	private final ConfigurationStore configurationStore;
	private final String factoryPid;
	private final String pid;
	/** @GuardedBy lock */
	private String bundleLocation;
	/** @GuardedBy lock */
	private ConfigurationDictionary dictionary;
	/** @GuardedBy lock */
	private boolean deleted = false;
	/** @GuardedBy lock */
	private boolean bound = false;
	/** @GuardedBy lock */
	private long changeCount;
	/** @GuardedBy lock */
	private Object storageToken;
	/** @GuardedBy lock */
	private boolean readOnly = false;
	private final ReentrantLock lock = new ReentrantLock();

	public ConfigurationImpl(ConfigurationAdminFactory configurationAdminFactory, ConfigurationStore configurationStore,
			String factoryPid, String pid, String bundleLocation, boolean bind) {
		this.configurationAdminFactory = configurationAdminFactory;
		this.configurationStore = configurationStore;
		this.factoryPid = factoryPid;
		this.pid = pid;
		this.bundleLocation = bundleLocation;
		this.changeCount = 0;
		this.bound = bind;
	}

	public ConfigurationImpl(ConfigurationAdminFactory configurationAdminFactory, ConfigurationStore configurationStore,
			Dictionary<String, ?> dictionary, Object storageToken) {
		this.configurationAdminFactory = configurationAdminFactory;
		this.configurationStore = configurationStore;
		pid = (String) dictionary.get(Constants.SERVICE_PID);
		factoryPid = (String) dictionary.get(ConfigurationAdmin.SERVICE_FACTORYPID);
		bundleLocation = (String) dictionary.get(ConfigurationAdmin.SERVICE_BUNDLELOCATION);
		Boolean boundProp = (Boolean) dictionary.remove(LOCATION_BOUND);
		this.bound = boundProp == null ? false : boundProp.booleanValue();
		Long changeCountProp = (Long) dictionary.remove(CHANGE_COUNT);
		this.changeCount = changeCountProp == null ? 0 : changeCountProp.longValue();
		Boolean readOnlyProp = (Boolean) dictionary.remove(READ_ONLY);
		this.readOnly = readOnlyProp == null ? false : readOnlyProp.booleanValue();
		Boolean nullProps = (Boolean) dictionary.remove(PROPERTIES_NULL);
		if (nullProps == null || !nullProps.booleanValue()) {
			updateDictionary(dictionary);
		}
		this.storageToken = storageToken;
	}

	void lock() {
		lock.lock();
	}

	void unlock() {
		lock.unlock();
	}

	void checkLocked() {
		if (!lock.isHeldByCurrentThread()) {
			throw new IllegalStateException("Thread not lock owner"); //$NON-NLS-1$
		}
	}

	boolean bind(String callerLocation) {
		lock();
		try {
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
		lock();
		try {
			return bound;
		} finally {
			unlock();
		}
	}

	void unbind(Bundle bundle) {
		lock();
		try {
			String callerLocation = ConfigurationAdminImpl.getLocation(bundle);
			if (bound && callerLocation.equals(bundleLocation)) {
				bundleLocation = null;
				bound = false;
				try {
					save();
				} catch (IOException e) {
					// TODO What should we do here? throw a runtime exception or log?
					e.printStackTrace();
				}
				configurationAdminFactory.notifyLocationChanged(this, callerLocation, factoryPid != null);
				configurationAdminFactory.dispatchEvent(ConfigurationEvent.CM_LOCATION_CHANGED, factoryPid, pid);
			}
		} finally {
			unlock();
		}
	}

	@Override
	public void delete() {
		Object deleteToken;
		lock();
		try {
			checkDeleted();
			checkReadOnly();
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
		if (deleted) {
			throw new IllegalStateException("deleted"); //$NON-NLS-1$
		}
	}

	private void checkReadOnly() {
		if (readOnly) {
			throw new ReadOnlyConfigurationException("read only"); //$NON-NLS-1$
		}
	}

	String getLocation() {
		lock();
		try {
			return bundleLocation;
		} finally {
			unlock();
		}
	}

	@Override
	public String getBundleLocation() {
		lock();
		try {
			checkDeleted();
			configurationAdminFactory.checkConfigurePermission(bundleLocation, null);
			if (bundleLocation != null) {
				return bundleLocation;
			}
			return null;
		} finally {
			unlock();
		}
	}

	String getFactoryPid(boolean checkDeleted) {
		lock();
		try {
			if (checkDeleted) {
				checkDeleted();
			}
			return factoryPid;
		} finally {
			unlock();
		}
	}

	@Override
	public String getFactoryPid() {
		return getFactoryPid(true);
	}

	String getPid(boolean checkDeleted) {
		lock();
		try {
			if (checkDeleted) {
				checkDeleted();
			}
			return pid;
		} finally {
			unlock();
		}
	}

	@Override
	public String getPid() {
		return getPid(true);
	}

	@Override
	public Dictionary<String, Object> getProperties() {
		lock();
		try {
			checkDeleted();
			if (dictionary == null) {
				return null;
			}

			Dictionary<String, Object> copy = dictionary.copy();
			fileAutoProperties(copy, this, false, false);
			return copy;
		} finally {
			unlock();
		}
	}

	Dictionary<String, Object> getAllProperties(boolean includeStorageKeys) {
		lock();
		try {
			if (deleted) {
				return null;
			}
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

	static void fileAutoProperties(Dictionary<String, Object> dictionary, ConfigurationImpl config, boolean includeLoc,
			boolean includeStorageKey) {
		dictionary.put(Constants.SERVICE_PID, config.getPid(false));
		String factoryPid = config.getFactoryPid(false);
		if (factoryPid != null) {
			dictionary.put(ConfigurationAdmin.SERVICE_FACTORYPID, factoryPid);
		} else {
			dictionary.remove(ConfigurationAdmin.SERVICE_FACTORYPID);
		}
		if (includeLoc) {
			String loc = config.getLocation();
			if (loc != null) {
				dictionary.put(ConfigurationAdmin.SERVICE_BUNDLELOCATION, loc);
			}
		} else {
			dictionary.remove(ConfigurationAdmin.SERVICE_BUNDLELOCATION);
		}
		if (includeStorageKey) {
			if (config.dictionary == null) {
				dictionary.put(PROPERTIES_NULL, Boolean.TRUE);
			}
			dictionary.put(CHANGE_COUNT, Long.valueOf(config.getChangeCount()));
			if (config.isBound()) {
				dictionary.put(LOCATION_BOUND, Boolean.TRUE);
			}
			dictionary.put(READ_ONLY, Boolean.valueOf(config.readOnly));
		}
	}

	@Override
	public void setBundleLocation(String bundleLocation) {
		lock();
		try {
			checkDeleted();
			configurationAdminFactory.checkConfigurePermission(this.bundleLocation, null);
			configurationAdminFactory.checkConfigurePermission(bundleLocation, null);
			String oldLocation = this.bundleLocation;
			this.bundleLocation = bundleLocation;
			this.bound = false;
			try {
				save();
			} catch (IOException e) {
				// TODO What should we do here? throw a runtime exception or log?
				e.printStackTrace();
			}
			configurationAdminFactory.notifyLocationChanged(this, oldLocation, factoryPid != null);
			configurationAdminFactory.dispatchEvent(ConfigurationEvent.CM_LOCATION_CHANGED, factoryPid, pid);
		} finally {
			unlock();
		}
	}

	@Override
	public void update() throws IOException {
		lock();
		try {
			checkDeleted();
			checkReadOnly();
			if (dictionary == null) {
				dictionary = new ConfigurationDictionary();
			}
			changeCount++;
			save();
			configurationAdminFactory.notifyConfigurationUpdated(this, factoryPid != null);
		} finally {
			unlock();
		}
	}

	@Override
	public void update(Dictionary<String, ?> properties) throws IOException {
		lock();
		try {
			doUpdate(properties, false);
		} finally {
			unlock();
		}
	}

	@Override
	public boolean updateIfDifferent(Dictionary<String, ?> properties) throws IOException {
		lock();
		try {
			return doUpdate(properties, true);
		} finally {
			unlock();
		}
	}

	private boolean same(Dictionary<String, ?> properties) {
		if (dictionary == null) {
			return false;
		}
		if (dictionary.size() != properties.size()) {
			return false;
		}

		Enumeration<String> keys = properties.keys();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			if (dictionary.get(key) == null) {
				return false;
			}
			Object current = dictionary.get(key);
			Object newValue = properties.get(key);
			if (current.getClass().isArray()) {
				if (!newValue.getClass().isArray()) {
					return false;
				}
				if (!current.getClass().getComponentType().equals(newValue.getClass().getComponentType())) {
					current = convertIfPossible(current);
					newValue = convertIfPossible(newValue);
					if (!current.getClass().getComponentType().equals(newValue.getClass().getComponentType())) {
						return false;
					}
				}
				Class<?> currentComponentType = current.getClass().getComponentType();
				if (long.class.isAssignableFrom(currentComponentType)) {
					if (!Arrays.equals((long[]) current, (long[]) newValue)) {
						return false;
					}
				} else if (int.class.isAssignableFrom(currentComponentType)) {
					if (!Arrays.equals((int[]) current, (int[]) newValue)) {
						return false;
					}
				} else if (short.class.isAssignableFrom(currentComponentType)) {
					if (!Arrays.equals((short[]) current, (short[]) newValue)) {
						return false;
					}
				} else if (char.class.isAssignableFrom(currentComponentType)) {
					if (!Arrays.equals((char[]) current, (char[]) newValue)) {
						return false;
					}
				} else if (byte.class.isAssignableFrom(currentComponentType)) {
					if (!Arrays.equals((byte[]) current, (byte[]) newValue)) {
						return false;
					}
				} else if (double.class.isAssignableFrom(currentComponentType)) {
					if (!Arrays.equals((double[]) current, (double[]) newValue)) {
						return false;
					}
				} else if (float.class.isAssignableFrom(currentComponentType)) {
					if (!Arrays.equals((float[]) current, (float[]) newValue)) {
						return false;
					}
				} else if (boolean.class.isAssignableFrom(currentComponentType)) {
					if (!Arrays.equals((boolean[]) current, (boolean[]) newValue)) {
						return false;
					}
				} else {
					if (!Arrays.equals((Object[]) current, (Object[]) newValue)) {
						return false;
					}
				}

			} else {
				if (!current.equals(newValue)) {
					return false;
				}
			}
		}
		return true;
	}

	private Object convertIfPossible(Object array) {
		Class<?> componentType = array.getClass().getComponentType();
		if (Long.class.isAssignableFrom(componentType)) {
			Long[] original = (Long[]) array;
			long[] converted = new long[original.length];
			for (int i = 0; i < original.length; i++) {
				converted[i] = original[i];
			}
			return converted;
		} else if (Integer.class.isAssignableFrom(componentType)) {
			Integer[] original = (Integer[]) array;
			int[] converted = new int[original.length];
			for (int i = 0; i < original.length; i++) {
				converted[i] = original[i];
			}
			return converted;
		} else if (Short.class.isAssignableFrom(componentType)) {
			Short[] original = (Short[]) array;
			short[] converted = new short[original.length];
			for (int i = 0; i < original.length; i++) {
				converted[i] = original[i];
			}
			return converted;
		} else if (Character.class.isAssignableFrom(componentType)) {
			Character[] original = (Character[]) array;
			char[] converted = new char[original.length];
			for (int i = 0; i < original.length; i++) {
				converted[i] = original[i];
			}
			return converted;
		} else if (Byte.class.isAssignableFrom(componentType)) {
			Byte[] original = (Byte[]) array;
			byte[] converted = new byte[original.length];
			for (int i = 0; i < original.length; i++) {
				converted[i] = original[i];
			}
			return converted;
		} else if (Double.class.isAssignableFrom(componentType)) {
			Double[] original = (Double[]) array;
			double[] converted = new double[original.length];
			for (int i = 0; i < original.length; i++) {
				converted[i] = original[i];
			}
			return converted;
		} else if (Float.class.isAssignableFrom(componentType)) {
			Float[] original = (Float[]) array;
			float[] converted = new float[original.length];
			for (int i = 0; i < original.length; i++) {
				converted[i] = original[i];
			}
			return converted;
		} else if (Boolean.class.isAssignableFrom(componentType)) {
			Boolean[] original = (Boolean[]) array;
			boolean[] converted = new boolean[original.length];
			for (int i = 0; i < original.length; i++) {
				converted[i] = original[i];
			}
			return converted;

		}
		return array;
	}

	private boolean doUpdate(Dictionary<String, ?> properties, boolean checkSame) throws IOException {
		checkDeleted();
		checkReadOnly();
		if (checkSame && same(properties)) {
			return false;
		}
		updateDictionary(properties);
		changeCount++;
		save();
		configurationAdminFactory.notifyConfigurationUpdated(this, factoryPid != null);
		configurationAdminFactory.dispatchEvent(ConfigurationEvent.CM_UPDATED, factoryPid, pid);
		return true;
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
				} else if (value instanceof Collection) {
					newDictionary.put(key, new Vector<>((Collection<?>) value));
				} else {
					newDictionary.put(key, properties.get(key));
				}
			} else {
				throw new IllegalArgumentException(key + " is already present or is a case variant."); //$NON-NLS-1$
			}
		}
		newDictionary.remove(Constants.SERVICE_PID);
		newDictionary.remove(ConfigurationAdmin.SERVICE_FACTORYPID);
		newDictionary.remove(ConfigurationAdmin.SERVICE_BUNDLELOCATION);

		dictionary = newDictionary;
	}

	@Override
	public boolean equals(Object obj) {
		return (obj instanceof ConfigurationImpl) && pid.equals(((ConfigurationImpl) obj).getPid());
	}

	@Override
	public int hashCode() {
		return pid.hashCode();
	}

	boolean isDeleted() {
		lock();
		try {
			return deleted;
		} finally {
			unlock();
		}
	}

	@Override
	public long getChangeCount() {
		lock();
		try {
			checkDeleted();
			return changeCount;
		} finally {
			unlock();
		}
	}

	@Override
	public Dictionary<String, Object> getProcessedProperties(ServiceReference<?> reference) {
		return configurationAdminFactory.modifyConfiguration(reference, this);
	}

	@Override
	public void addAttributes(ConfigurationAttribute... attrs) throws IOException {
		lock();
		try {
			configurationAdminFactory.checkAttributePermission(bundleLocation);
			for (ConfigurationAttribute attr : attrs) {
				if (ConfigurationAttribute.READ_ONLY.equals(attr)) {
					readOnly = true;
				}
			}
			save();
		} finally {
			unlock();
		}
	}

	@Override
	public Set<ConfigurationAttribute> getAttributes() {
		lock();
		try {
			if (readOnly) {
				return EnumSet.of(ConfigurationAttribute.READ_ONLY);
			}
			return Collections.emptySet();
		} finally {
			unlock();
		}
	}

	@Override
	public void removeAttributes(ConfigurationAttribute... attrs) throws IOException {
		lock();
		try {
			configurationAdminFactory.checkAttributePermission(bundleLocation);
			for (ConfigurationAttribute attr : attrs) {
				if (ConfigurationAttribute.READ_ONLY.equals(attr)) {
					readOnly = false;
				}
			}
			save();
		} finally {
			unlock();
		}
	}
}
