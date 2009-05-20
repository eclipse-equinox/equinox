/*******************************************************************************
 * Copyright (c) 2005, 2008 Cognos Incorporated, IBM Corporation and others.
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

import java.io.*;
import java.security.*;
import java.util.*;
import org.eclipse.equinox.internal.cm.reliablefile.*;
import org.osgi.framework.*;
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
	private static final String PID_EXT = ".pid"; //$NON-NLS-1$
	private final Map configurations = new HashMap();
	private int createdPidCount = 0;
	private final File store;

	public ConfigurationStore(ConfigurationAdminFactory configurationAdminFactory, BundleContext context) {
		this.configurationAdminFactory = configurationAdminFactory;
		store = context.getDataFile(STORE_DIR);
		if (store == null)
			return; // no persistent store

		store.mkdir();
		File[] configurationFiles = store.listFiles();
		for (int i = 0; i < configurationFiles.length; ++i) {
			String configurationFileName = configurationFiles[i].getName();
			if (!configurationFileName.endsWith(PID_EXT))
				continue;

			InputStream ris = null;
			ObjectInputStream ois = null;
			boolean deleteFile = false;
			try {
				ris = new ReliableFileInputStream(configurationFiles[i]);
				ois = new ObjectInputStream(ris);
				Dictionary dictionary = (Dictionary) ois.readObject();
				ConfigurationImpl config = new ConfigurationImpl(configurationAdminFactory, this, dictionary);
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
				ReliableFile.delete(configurationFiles[i]);
				configurationFiles[i].delete();
			}
		}
	}

	public void saveConfiguration(String pid, ConfigurationImpl config) throws IOException {
		if (store == null)
			return; // no persistent store

		config.checkLocked();
		final File configFile = new File(store, pid + PID_EXT);
		final Dictionary configProperties = config.getAllProperties();
		try {
			AccessController.doPrivileged(new PrivilegedExceptionAction() {
				public Object run() throws Exception {
					writeConfigurationFile(configFile, configProperties);
					return null;
				}
			});
		} catch (PrivilegedActionException e) {
			throw (IOException) e.getException();
		}
	}

	void writeConfigurationFile(File configFile, Dictionary configProperties) throws IOException {
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

	public synchronized void removeConfiguration(String pid) {
		configurations.remove(pid);
		if (store == null)
			return; // no persistent store
		final File configFile = new File(store, pid + PID_EXT);
		AccessController.doPrivileged(new PrivilegedAction() {
			public Object run() {
				deleteConfigurationFile(configFile);
				return null;
			}
		});
	}

	void deleteConfigurationFile(File configFile) {
		ReliableFile.delete(configFile);
		configFile.delete();
	}

	public synchronized ConfigurationImpl getConfiguration(String pid, String location) {
		ConfigurationImpl config = (ConfigurationImpl) configurations.get(pid);
		if (config == null) {
			config = new ConfigurationImpl(configurationAdminFactory, this, null, pid, location);
			configurations.put(pid, config);
		}
		return config;
	}

	public synchronized ConfigurationImpl createFactoryConfiguration(String factoryPid, String location) {
		String pid = factoryPid + "-" + new Date().getTime() + "-" + createdPidCount++; //$NON-NLS-1$ //$NON-NLS-2$
		ConfigurationImpl config = new ConfigurationImpl(configurationAdminFactory, this, factoryPid, pid, location);
		configurations.put(pid, config);
		return config;
	}

	public synchronized ConfigurationImpl findConfiguration(String pid) {
		return (ConfigurationImpl) configurations.get(pid);
	}

	public synchronized ConfigurationImpl[] getFactoryConfigurations(String factoryPid) {
		List resultList = new ArrayList();
		for (Iterator it = configurations.values().iterator(); it.hasNext();) {
			ConfigurationImpl config = (ConfigurationImpl) it.next();
			String otherFactoryPid = config.getFactoryPid();
			if (otherFactoryPid != null && otherFactoryPid.equals(factoryPid))
				resultList.add(config);
		}
		return (ConfigurationImpl[]) resultList.toArray(new ConfigurationImpl[0]);
	}

	public synchronized ConfigurationImpl[] listConfigurations(Filter filter) {
		List resultList = new ArrayList();
		for (Iterator it = configurations.values().iterator(); it.hasNext();) {
			ConfigurationImpl config = (ConfigurationImpl) it.next();
			Dictionary properties = config.getAllProperties();
			if (properties != null && filter.match(properties))
				resultList.add(config);
		}
		int size = resultList.size();
		return size == 0 ? null : (ConfigurationImpl[]) resultList.toArray(new ConfigurationImpl[size]);
	}

	public synchronized void unbindConfigurations(Bundle bundle) {
		for (Iterator it = configurations.values().iterator(); it.hasNext();) {
			ConfigurationImpl config = (ConfigurationImpl) it.next();
			config.unbind(bundle);
		}
	}
}