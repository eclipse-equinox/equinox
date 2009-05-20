/*******************************************************************************
 * Copyright (c) 2005, 2008 Cognos Incorporated, IBM Corporation and others..
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
import org.osgi.framework.*;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * ConfigurationAdminImpl provides the ConfigurationAdmin service implementation 
 */
class ConfigurationAdminImpl implements ConfigurationAdmin {

	private final ConfigurationAdminFactory configurationAdminFactory;
	private final Bundle bundle;
	private final ConfigurationStore configurationStore;

	public ConfigurationAdminImpl(ConfigurationAdminFactory configurationAdminFactory, ConfigurationStore configurationStore, Bundle bundle) {
		this.configurationAdminFactory = configurationAdminFactory;
		this.configurationStore = configurationStore;
		this.bundle = bundle;
	}

	public Configuration createFactoryConfiguration(String factoryPid) throws IOException {
		checkPID(factoryPid);
		return configurationStore.createFactoryConfiguration(factoryPid, bundle.getLocation());
	}

	public Configuration createFactoryConfiguration(String factoryPid, String location) throws IOException {
		checkPID(factoryPid);
		this.configurationAdminFactory.checkConfigurationPermission();
		return configurationStore.createFactoryConfiguration(factoryPid, location);
	}

	public Configuration getConfiguration(String pid) throws IOException {
		checkPID(pid);
		ConfigurationImpl config = configurationStore.getConfiguration(pid, bundle.getLocation());
		if (config.getBundleLocation(false) != null && !config.getBundleLocation(false).equals(bundle.getLocation()))
			this.configurationAdminFactory.checkConfigurationPermission();
		config.bind(bundle);
		return config;
	}

	public Configuration getConfiguration(String pid, String location) throws IOException {
		checkPID(pid);
		this.configurationAdminFactory.checkConfigurationPermission();
		return configurationStore.getConfiguration(pid, location);
	}

	public Configuration[] listConfigurations(String filterString) throws IOException, InvalidSyntaxException {
		if (filterString == null)
			filterString = "(" + Constants.SERVICE_PID + "=*)"; //$NON-NLS-1$ //$NON-NLS-2$

		try {
			this.configurationAdminFactory.checkConfigurationPermission();
		} catch (SecurityException e) {
			filterString = "(&(" + ConfigurationAdmin.SERVICE_BUNDLELOCATION + "=" + bundle.getLocation() + ")" + filterString + ")"; //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$ //$NON-NLS-4$
		}
		return configurationStore.listConfigurations(FrameworkUtil.createFilter(filterString));
	}

	private void checkPID(String pid) {
		if (pid == null)
			throw new IllegalArgumentException("PID cannot be null"); //$NON-NLS-1$
	}
}