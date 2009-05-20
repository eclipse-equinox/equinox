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

import java.util.*;
import java.util.Map.Entry;
import org.osgi.framework.*;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * ManagedServiceFactoryTracker tracks... ManagedServiceFactory(s) and notifies them about related configuration changes
 */
class ManagedServiceFactoryTracker extends ServiceTracker {

	final ConfigurationAdminFactory configurationAdminFactory;
	private final ConfigurationStore configurationStore;

	// managedServiceFactoryReferences guards both managedServiceFactories and managedServiceFactoryReferences
	private final Map managedServiceFactories = new HashMap();
	private final Map managedServiceFactoryReferences = new HashMap();

	private final SerializedTaskQueue queue = new SerializedTaskQueue("ManagedServiceFactory Update Queue"); //$NON-NLS-1$

	public ManagedServiceFactoryTracker(ConfigurationAdminFactory configurationAdminFactory, ConfigurationStore configurationStore, BundleContext context) {
		super(context, ManagedServiceFactory.class.getName(), null);
		this.configurationAdminFactory = configurationAdminFactory;
		this.configurationStore = configurationStore;
	}

	protected void notifyDeleted(ConfigurationImpl config) {
		config.checkLocked();
		String factoryPid = config.getFactoryPid(false);
		ServiceReference reference = getManagedServiceFactoryReference(factoryPid);
		if (reference != null && config.bind(reference.getBundle()))
			asynchDeleted(getManagedServiceFactory(factoryPid), config.getPid(false));
	}

	protected void notifyUpdated(ConfigurationImpl config) {
		config.checkLocked();
		String factoryPid = config.getFactoryPid();
		ServiceReference reference = getManagedServiceFactoryReference(factoryPid);
		if (reference != null && config.bind(reference.getBundle())) {
			Dictionary properties = config.getProperties();
			configurationAdminFactory.modifyConfiguration(reference, properties);
			asynchUpdated(getManagedServiceFactory(factoryPid), config.getPid(), properties);
		}
	}

	public Object addingService(ServiceReference reference) {
		String factoryPid = (String) reference.getProperty(Constants.SERVICE_PID);
		if (factoryPid == null)
			return null;

		ManagedServiceFactory service = (ManagedServiceFactory) context.getService(reference);
		if (service == null)
			return null;

		synchronized (configurationStore) {
			add(reference, factoryPid, service);
		}
		return service;
	}

	public void modifiedService(ServiceReference reference, Object service) {
		String factoryPid = (String) reference.getProperty(Constants.SERVICE_PID);
		synchronized (configurationStore) {
			if (getManagedServiceFactory(factoryPid) == service)
				return;
			String previousPid = getPidForManagedServiceFactory(service);
			remove(reference, previousPid);
			addingService(reference);
		}
	}

	public void removedService(ServiceReference reference, Object service) {
		String factoryPid = (String) reference.getProperty(Constants.SERVICE_PID);
		synchronized (configurationStore) {
			remove(reference, factoryPid);
		}
		context.ungetService(reference);
	}

	private void add(ServiceReference reference, String factoryPid, ManagedServiceFactory service) {
		ConfigurationImpl[] configs = configurationStore.getFactoryConfigurations(factoryPid);
		try {
			for (int i = 0; i < configs.length; ++i)
				configs[i].lock();

			if (trackManagedServiceFactory(factoryPid, reference, service)) {
				for (int i = 0; i < configs.length; ++i) {
					if (configs[i].isDeleted()) {
						// ignore this config
					} else if (configs[i].bind(reference.getBundle())) {
						Dictionary properties = configs[i].getProperties();
						configurationAdminFactory.modifyConfiguration(reference, properties);
						asynchUpdated(service, configs[i].getPid(), properties);
					} else {
						configurationAdminFactory.log(LogService.LOG_WARNING, "Configuration for " + Constants.SERVICE_PID + "=" + configs[i].getPid() + " could not be bound to " + reference.getBundle().getLocation()); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
					}
				}
			}
		} finally {
			for (int i = 0; i < configs.length; ++i)
				configs[i].unlock();
		}
	}

	private void remove(ServiceReference reference, String factoryPid) {
		ConfigurationImpl[] configs = configurationStore.getFactoryConfigurations(factoryPid);
		try {
			for (int i = 0; i < configs.length; ++i)
				configs[i].lock();
			untrackManagedServiceFactory(factoryPid, reference);
		} finally {
			for (int i = 0; i < configs.length; ++i)
				configs[i].unlock();
		}
	}

	private boolean trackManagedServiceFactory(String factoryPid, ServiceReference reference, ManagedServiceFactory service) {
		synchronized (managedServiceFactoryReferences) {
			if (managedServiceFactoryReferences.containsKey(factoryPid)) {
				configurationAdminFactory.log(LogService.LOG_WARNING, ManagedServiceFactory.class.getName() + " already registered for " + Constants.SERVICE_PID + "=" + factoryPid); //$NON-NLS-1$ //$NON-NLS-2$
				return false;
			}
			managedServiceFactoryReferences.put(factoryPid, reference);
			managedServiceFactories.put(factoryPid, service);
			return true;
		}
	}

	private void untrackManagedServiceFactory(String factoryPid, ServiceReference reference) {
		synchronized (managedServiceFactoryReferences) {
			managedServiceFactoryReferences.remove(factoryPid);
			managedServiceFactories.remove(factoryPid);
		}
	}

	private ManagedServiceFactory getManagedServiceFactory(String factoryPid) {
		synchronized (managedServiceFactoryReferences) {
			return (ManagedServiceFactory) managedServiceFactories.get(factoryPid);
		}
	}

	private ServiceReference getManagedServiceFactoryReference(String factoryPid) {
		synchronized (managedServiceFactoryReferences) {
			return (ServiceReference) managedServiceFactoryReferences.get(factoryPid);
		}
	}

	private String getPidForManagedServiceFactory(Object service) {
		synchronized (managedServiceFactoryReferences) {
			for (Iterator it = managedServiceFactories.entrySet().iterator(); it.hasNext();) {
				Entry entry = (Entry) it.next();
				if (entry.getValue() == service)
					return (String) entry.getKey();
			}
			return null;
		}
	}

	private void asynchDeleted(final ManagedServiceFactory service, final String pid) {
		queue.put(new Runnable() {
			public void run() {
				try {
					service.deleted(pid);
				} catch (Throwable t) {
					configurationAdminFactory.log(LogService.LOG_ERROR, t.getMessage(), t);
				}
			}
		});
	}

	private void asynchUpdated(final ManagedServiceFactory service, final String pid, final Dictionary properties) {
		queue.put(new Runnable() {
			public void run() {
				try {
					service.updated(pid, properties);
				} catch (ConfigurationException e) {
					// we might consider doing more for ConfigurationExceptions 
					Throwable cause = e.getCause();
					configurationAdminFactory.log(LogService.LOG_ERROR, e.getMessage(), cause != null ? cause : e);
				} catch (Throwable t) {
					configurationAdminFactory.log(LogService.LOG_ERROR, t.getMessage(), t);
				}
			}
		});
	}
}