/*******************************************************************************
 * Copyright (c) 2005, 2013 Cognos Incorporated, IBM Corporation and others..
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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import org.osgi.framework.*;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * ConfigurationAdminImpl provides the ConfigurationAdmin service implementation 
 */
class ConfigurationAdminImpl implements ConfigurationAdmin {

	private final ConfigurationAdminFactory configurationAdminFactory;
	private final String bundleLocation;
	private final ConfigurationStore configurationStore;

	public ConfigurationAdminImpl(ConfigurationAdminFactory configurationAdminFactory, ConfigurationStore configurationStore, Bundle bundle) {
		this.configurationAdminFactory = configurationAdminFactory;
		this.configurationStore = configurationStore;
		this.bundleLocation = ConfigurationAdminImpl.getLocation(bundle);
	}

	public Configuration createFactoryConfiguration(String factoryPid) {
		return internalGetConfiguration(factoryPid, bundleLocation, true, true);

	}

	public Configuration createFactoryConfiguration(String factoryPid, String location) {
		return internalGetConfiguration(factoryPid, location, true, false);
	}

	public Configuration getConfiguration(String pid) {
		return internalGetConfiguration(pid, bundleLocation, false, true);
	}

	public Configuration getConfiguration(String pid, String location) {
		return internalGetConfiguration(pid, location, false, false);
	}

	private Configuration internalGetConfiguration(String pid, String location, boolean factory, boolean bind) {
		checkPID(pid);
		this.configurationAdminFactory.checkConfigurePermission(location, bundleLocation);

		ConfigurationImpl config;
		if (factory) {
			config = configurationStore.createFactoryConfiguration(pid, location);
		} else {
			config = configurationStore.getConfiguration(pid, location);
		}

		String configLocation = config.getLocation();
		if (bind) {
			if (configLocation != null) {
				this.configurationAdminFactory.checkConfigurePermission(configLocation, bundleLocation);
			} else {
				config.bind(bundleLocation);
			}
		} else {
			this.configurationAdminFactory.checkConfigurePermission(configLocation, bundleLocation);
		}
		return config;
	}

	public Configuration[] listConfigurations(String filterString) throws InvalidSyntaxException {
		if (filterString == null)
			filterString = "(" + Constants.SERVICE_PID + "=*)"; //$NON-NLS-1$ //$NON-NLS-2$

		ConfigurationImpl[] configs = configurationStore.listConfigurations(FrameworkUtil.createFilter(filterString));
		if (configs == null) {
			return null;
		}

		List<Configuration> result = new ArrayList<Configuration>(configs.length);
		SecurityManager sm = System.getSecurityManager();
		for (int i = 0; i < configs.length; i++) {
			try {
				if (sm != null) {
					this.configurationAdminFactory.checkConfigurePermission(configs[i].getLocation(), bundleLocation);
				}
				result.add(configs[i]);
			} catch (SecurityException e) {
				// ignore;
			}
		}
		return result.size() == 0 ? null : result.toArray(new Configuration[result.size()]);
	}

	private void checkPID(String pid) {
		if (pid == null)
			throw new IllegalArgumentException("PID cannot be null"); //$NON-NLS-1$
	}

	static String getLocation(final Bundle bundle) {
		return AccessController.doPrivileged(new PrivilegedAction<String>() {
			public String run() {
				return bundle.getLocation();
			}
		});
	}
}