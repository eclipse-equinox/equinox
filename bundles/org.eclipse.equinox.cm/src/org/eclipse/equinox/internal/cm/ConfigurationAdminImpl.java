/*******************************************************************************
 * Copyright (c) 2005, 2019 Cognos Incorporated, IBM Corporation and others..
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

	public ConfigurationAdminImpl(ConfigurationAdminFactory configurationAdminFactory,
			ConfigurationStore configurationStore, Bundle bundle) {
		this.configurationAdminFactory = configurationAdminFactory;
		this.configurationStore = configurationStore;
		this.bundleLocation = ConfigurationAdminImpl.getLocation(bundle);
	}

	@Override
	public Configuration createFactoryConfiguration(String factoryPid) {
		return internalGetConfiguration(factoryPid, bundleLocation, true, true, null);
	}

	@Override
	public Configuration createFactoryConfiguration(String factoryPid, String location) {
		return internalGetConfiguration(factoryPid, location, true, false, null);
	}

	@Override
	public Configuration getConfiguration(String pid) {
		return internalGetConfiguration(pid, bundleLocation, false, true, null);
	}

	@Override
	public Configuration getConfiguration(String pid, String location) {
		return internalGetConfiguration(pid, location, false, false, null);
	}

	@Override
	public Configuration getFactoryConfiguration(String factoryPid, String name) {
		return internalGetConfiguration(factoryPid, bundleLocation, true, true, name);
	}

	@Override
	public Configuration getFactoryConfiguration(String factoryPid, String name, String location) {
		return internalGetConfiguration(factoryPid, location, true, false, name);
	}

	private Configuration internalGetConfiguration(String pid, String location, boolean factory, boolean bind,
			String name) {
		checkPID(pid);
		this.configurationAdminFactory.checkConfigurePermission(location, bundleLocation);

		ConfigurationImpl config;
		if (factory) {
			config = configurationStore.getFactoryConfiguration(pid, location, bind, name);
		} else {
			config = configurationStore.getConfiguration(pid, location, bind);
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

	@Override
	public Configuration[] listConfigurations(String filterString) throws InvalidSyntaxException {
		if (filterString == null) {
			filterString = "(" + Constants.SERVICE_PID + "=*)"; //$NON-NLS-1$ //$NON-NLS-2$
		}

		ConfigurationImpl[] configs = configurationStore.listConfigurations(FrameworkUtil.createFilter(filterString));
		if (configs == null) {
			return null;
		}

		List<Configuration> result = new ArrayList<>(configs.length);
		SecurityManager sm = System.getSecurityManager();
		for (ConfigurationImpl config : configs) {
			try {
				if (sm != null) {
					this.configurationAdminFactory.checkConfigurePermission(config.getLocation(), bundleLocation);
				}
				result.add(config);
			} catch (SecurityException e) {
				// ignore;
			}
		}
		return result.size() == 0 ? null : result.toArray(new Configuration[result.size()]);
	}

	private void checkPID(String pid) {
		if (pid == null) {
			throw new IllegalArgumentException("PID cannot be null"); //$NON-NLS-1$
		}
	}

	static String getLocation(final Bundle bundle) {
		return AccessController.doPrivileged(new PrivilegedAction<String>() {
			@Override
			public String run() {
				return bundle.getLocation();
			}
		});
	}
}
