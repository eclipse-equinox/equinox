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
 *******************************************************************************/
package org.eclipse.equinox.internal.cm;

import java.io.*;
import java.security.*;
import java.util.*;
import org.eclipse.equinox.internal.cm.reliablefile.*;
import org.osgi.framework.*;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.log.LogService;

/**
 * ConfigurationStore manages all active configurations along with persistence. The current
 * implementation uses a filestore and serialization of the configuration dictionaries to files
 * identified by their pid. Persistence details are in the constructor, saveConfiguration, and
 * deleteConfiguration and can be factored out separately if required.
 */
class ConfigurationStore {

	private final ConfigurationAdminFactory configurationAdminFactory;
	private static final String STORE_DIR = "store"; //$NON-NLS-1$
	private static final String DATA_PRE = "data"; //$NON-NLS-1$
	private static final String CFG_EXT = ".cfg"; //$NON-NLS-1$
	private final Map<String, ConfigurationImpl> configurations = new HashMap<>();
	private int createdPidCount = 0;
	private final File store;

	public ConfigurationStore(ConfigurationAdminFactory configurationAdminFactory, BundleContext context) {
		this.configurationAdminFactory = configurationAdminFactory;
		store = context.getDataFile(STORE_DIR);
		if (store == null)
			return; // no persistent store

		store.mkdir();
		for (File configurationFile : store.listFiles()) {
			String configurationFileName = configurationFile.getName();
			if (!configurationFileName.endsWith(CFG_EXT))
				continue;

			InputStream ris = null;
			ObjectInputStream ois = null;
			boolean deleteFile = false;
			try {
				ris = new ReliableFileInputStream(configurationFile);
				ois = new ObjectInputStream(ris);
				@SuppressWarnings("unchecked")
				Dictionary<String, Object> dictionary = (Dictionary<String, Object>) ois.readObject();
				// before adding, make sure the bundle exists if the location is set
				String location = (String) dictionary.get(ConfigurationAdmin.SERVICE_BUNDLELOCATION);
				if (location != null && context.getBundle(location) == null) {
					Boolean boundProp = (Boolean) dictionary.remove(ConfigurationImpl.LOCATION_BOUND);
					if (boundProp != null && boundProp.booleanValue()) {
						dictionary.remove(ConfigurationAdmin.SERVICE_BUNDLELOCATION);
					}
				}
				ConfigurationImpl config = new ConfigurationImpl(configurationAdminFactory, this, dictionary, configurationFile);
				configurations.put(config.getPid(), config);
			} catch (IOException e) {
				String message = e.getMessage();
				String pid = configurationFileName.substring(0, configurationFileName.length() - 4);
				String errorMessage = "{Configuration Admin - pid = " + pid + "} could not be restored." + ((message == null) ? "" : " " + message); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				configurationAdminFactory.log(LogService.LOG_ERROR, errorMessage);
				deleteFile = true;
			} catch (ClassNotFoundException e) {
				configurationAdminFactory.log(LogService.LOG_ERROR, e.getMessage());
			} finally {
				if (ois != null) {
					try {
						ois.close();
					} catch (IOException e) {
						// ignore 
					}
				}
				if (ris != null) {
					try {
						ris.close();
					} catch (IOException e) {
						// ignore 
					}
				}
			}
			if (deleteFile) {
				ReliableFile.delete(configurationFile);
				configurationFile.delete();
			}
		}
	}

	public Object saveConfiguration(String pid, ConfigurationImpl config, final Object token) throws IOException {
		if (store == null)
			return null; // no persistent store

		config.checkLocked();
		final Dictionary<String, Object> configProperties = config.getAllProperties(true);
		if (configProperties == null) {
			return null;
		}
		try {
			final File storeCopy = store;
			return AccessController.doPrivileged(new PrivilegedExceptionAction<File>() {
				@Override
				public File run() throws Exception {
					File toFile = token == null ? File.createTempFile(DATA_PRE, CFG_EXT, storeCopy) : (File) token;
					writeConfigurationFile(toFile, configProperties);
					return toFile;
				}
			});
		} catch (PrivilegedActionException e) {
			throw (IOException) e.getException();
		}
	}

	void writeConfigurationFile(File configFile, Dictionary<String, Object> configProperties) throws IOException {
		OutputStream ros = null;
		ObjectOutputStream oos = null;
		try {
			configFile.createNewFile();
			ros = new ReliableFileOutputStream(configFile);
			oos = new ObjectOutputStream(ros);
			oos.writeObject(configProperties);
		} finally {
			if (oos != null) {
				try {
					oos.close();
				} catch (IOException e) {
					// ignore 
				}
			}
			if (ros != null) {
				try {
					ros.close();
				} catch (IOException e) {
					// ignore 
				}
			}
		}
	}

	public synchronized void removeConfiguration(String pid, final Object token) {
		configurations.remove(pid);
		if (store == null || token == null)
			return; // no persistent store
		AccessController.doPrivileged(new PrivilegedAction<Object>() {
			@Override
			public Object run() {
				deleteConfigurationFile((File) token);
				return null;
			}
		});
	}

	void deleteConfigurationFile(File configFile) {
		ReliableFile.delete(configFile);
		configFile.delete();
	}

	public synchronized ConfigurationImpl getConfiguration(String pid, String location, boolean bind) {
		ConfigurationImpl config = configurations.get(pid);
		if (config == null) {
			config = new ConfigurationImpl(configurationAdminFactory, this, null, pid, location, bind);
			configurations.put(pid, config);
		}
		return config;
	}

	public synchronized ConfigurationImpl getFactoryConfiguration(String factoryPid, String location, boolean bind, String name) {
		String pid;
		if (name == null) {
			pid = factoryPid + "-" + new Date().getTime() + "-" + createdPidCount++; //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			pid = factoryPid + "~" + name; //$NON-NLS-1$
			ConfigurationImpl config = configurations.get(pid);
			if (config != null) {
				return config;
			}
		}

		ConfigurationImpl config = new ConfigurationImpl(configurationAdminFactory, this, factoryPid, pid, location, bind);
		configurations.put(pid, config);
		return config;
	}

	public synchronized ConfigurationImpl findConfiguration(String pid) {
		return configurations.get(pid);
	}

	public ConfigurationImpl[] getFactoryConfigurations(String factoryPid) {
		List<ConfigurationImpl> resultList = new ArrayList<>();
		synchronized (this) {
			resultList.addAll(configurations.values());
		}
		for (Iterator<ConfigurationImpl> it = resultList.iterator(); it.hasNext();) {
			ConfigurationImpl config = it.next();
			String otherFactoryPid = config.getFactoryPid();
			if (otherFactoryPid == null || !otherFactoryPid.equals(factoryPid))
				it.remove();
		}
		return resultList.toArray(new ConfigurationImpl[resultList.size()]);
	}

	public ConfigurationImpl[] listConfigurations(Filter filter) {
		List<ConfigurationImpl> resultList = new ArrayList<>();
		synchronized (this) {
			resultList.addAll(configurations.values());
		}
		for (Iterator<ConfigurationImpl> it = resultList.iterator(); it.hasNext();) {
			ConfigurationImpl config = it.next();
			Dictionary<String, Object> properties = config.getAllProperties(false);
			if (properties == null || !filter.match(properties)) {
				it.remove();
			}
		}
		int size = resultList.size();
		return size == 0 ? null : (ConfigurationImpl[]) resultList.toArray(new ConfigurationImpl[size]);
	}

	public void unbindConfigurations(Bundle bundle) {
		ConfigurationImpl[] copy;
		synchronized (this) {
			copy = configurations.values().toArray(new ConfigurationImpl[configurations.size()]);
		}
		for (ConfigurationImpl config : copy) {
			config.unbind(bundle);
		}
	}
}