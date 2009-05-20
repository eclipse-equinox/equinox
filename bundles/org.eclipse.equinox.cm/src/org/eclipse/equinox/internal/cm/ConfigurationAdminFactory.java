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

import java.security.Permission;
import java.util.Dictionary;
import org.osgi.framework.*;
import org.osgi.service.cm.ConfigurationPermission;
import org.osgi.service.log.LogService;

/**
 * ConfigurationAdminFactory provides a Configuration Admin ServiceFactory but more significantly
 * launches the whole implementation.
 */

public class ConfigurationAdminFactory implements ServiceFactory, BundleListener {

	private final Permission configurationPermission = new ConfigurationPermission("*", ConfigurationPermission.CONFIGURE); //$NON-NLS-1$
	private final EventDispatcher eventDispatcher;
	private final PluginManager pluginManager;
	private final LogService log;
	private final ManagedServiceTracker managedServiceTracker;
	private final ManagedServiceFactoryTracker managedServiceFactoryTracker;
	private final ConfigurationStore configurationStore;

	public ConfigurationAdminFactory(BundleContext context, LogService log) {
		this.log = log;
		configurationStore = new ConfigurationStore(this, context);
		eventDispatcher = new EventDispatcher(context, log);
		pluginManager = new PluginManager(context);
		managedServiceTracker = new ManagedServiceTracker(this, configurationStore, context);
		managedServiceFactoryTracker = new ManagedServiceFactoryTracker(this, configurationStore, context);
	}

	void start() {
		eventDispatcher.start();
		pluginManager.start();
		managedServiceTracker.open();
		managedServiceFactoryTracker.open();
	}

	void stop() {
		managedServiceTracker.close();
		managedServiceFactoryTracker.close();
		eventDispatcher.stop();
		pluginManager.stop();
	}

	public Object getService(Bundle bundle, ServiceRegistration registration) {
		ServiceReference reference = registration.getReference();
		eventDispatcher.setServiceReference(reference);
		return new ConfigurationAdminImpl(this, configurationStore, bundle);
	}

	public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
		// do nothing
	}

	public void bundleChanged(BundleEvent event) {
		if (event.getType() == BundleEvent.UNINSTALLED)
			configurationStore.unbindConfigurations(event.getBundle());
	}

	public void checkConfigurationPermission() throws SecurityException {
		SecurityManager sm = System.getSecurityManager();
		if (sm != null)
			sm.checkPermission(configurationPermission);
	}

	void log(int level, String message) {
		log.log(level, message);
	}

	void log(int level, String message, Throwable exception) {
		log.log(level, message, exception);
	}

	void dispatchEvent(int type, String factoryPid, String pid) {
		eventDispatcher.dispatchEvent(type, factoryPid, pid);
	}

	void notifyConfigurationUpdated(ConfigurationImpl config, boolean isFactory) {
		if (isFactory)
			managedServiceFactoryTracker.notifyUpdated(config);
		else
			managedServiceTracker.notifyUpdated(config);
	}

	void notifyConfigurationDeleted(ConfigurationImpl config, boolean isFactory) {
		if (isFactory)
			managedServiceFactoryTracker.notifyDeleted(config);
		else
			managedServiceTracker.notifyDeleted(config);
	}

	void modifyConfiguration(ServiceReference reference, Dictionary properties) {
		pluginManager.modifyConfiguration(reference, properties);
	}
}