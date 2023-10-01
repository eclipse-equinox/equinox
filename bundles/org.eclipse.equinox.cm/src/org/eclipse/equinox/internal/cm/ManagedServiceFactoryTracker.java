/*******************************************************************************
 * Copyright (c) 2005, 2018 Cognos Incorporated, IBM Corporation and others.
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

import java.util.*;
import org.osgi.framework.*;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * ManagedServiceFactoryTracker tracks... ManagedServiceFactory(s) and notifies them about related configuration changes
 */
class ManagedServiceFactoryTracker extends ServiceTracker<ManagedServiceFactory, ManagedServiceFactory> {

	final ConfigurationAdminFactory configurationAdminFactory;
	private final ConfigurationStore configurationStore;

	/** @GuardedBy targets*/
	private final TargetMap targets = new TargetMap();

	private final SerializedTaskQueue queue = new SerializedTaskQueue("ManagedServiceFactory Update Queue"); //$NON-NLS-1$

	public ManagedServiceFactoryTracker(ConfigurationAdminFactory configurationAdminFactory, ConfigurationStore configurationStore, BundleContext context) {
		super(context, ManagedServiceFactory.class.getName(), null);
		this.configurationAdminFactory = configurationAdminFactory;
		this.configurationStore = configurationStore;
	}

	void notifyDeleted(ConfigurationImpl config) {
		config.checkLocked();
		String configLoc = config.getLocation();
		if (configLoc == null) {
			return;
		}
		boolean isMultiple = configLoc.startsWith("?"); //$NON-NLS-1$
		String factoryPid = config.getFactoryPid(false);
		List<ServiceReference<ManagedServiceFactory>> references = getManagedServiceFactoryReferences(factoryPid);
		for (ServiceReference<ManagedServiceFactory> ref : references) {
			if (!hasMoreSpecificConfigPids(ref, factoryPid)) {
				boolean hasLocPermission = configurationAdminFactory.checkTargetPermission(configLoc, ref);
				ManagedServiceFactory serviceFactory = getService(ref);
				if (hasLocPermission && serviceFactory != null) {
					if (isMultiple || ConfigurationAdminImpl.getLocation(ref.getBundle()).equals(configLoc)) {
						asynchDeleted(serviceFactory, config.getPid(false));
					}
				}
			}
		}
	}

	void notifyUpdated(ConfigurationImpl config) {
		config.checkLocked();
		String configLoc = config.getLocation();
		boolean isMultiple = configLoc != null && configLoc.startsWith("?"); //$NON-NLS-1$
		String factoryPid = config.getFactoryPid();
		List<ServiceReference<ManagedServiceFactory>> references = getManagedServiceFactoryReferences(factoryPid);
		for (ServiceReference<ManagedServiceFactory> ref : references) {
			if (!hasMoreSpecificConfigPids(ref, factoryPid)) {
				boolean hasLocPermission = configurationAdminFactory.checkTargetPermission(configLoc, ref);
				ManagedServiceFactory serviceFactory = getService(ref);
				if (hasLocPermission && serviceFactory != null) {
					if (isMultiple || config.bind(ConfigurationAdminImpl.getLocation(ref.getBundle()))) {
						Dictionary<String, Object> properties = configurationAdminFactory.modifyConfiguration(ref, config);
						asynchUpdated(serviceFactory, config.getPid(), properties);
					}
				}
			}
		}
	}

	void notifyUpdateLocation(ConfigurationImpl config, String oldLocation) {
		config.checkLocked();
		String configLoc = config.getLocation();
		if (configLoc == null ? oldLocation == null : configLoc.equals(oldLocation)) {
			// same location do nothing
			return;
		}
		boolean oldIsMultiple = oldLocation != null && oldLocation.startsWith("?"); //$NON-NLS-1$
		boolean newIsMultiple = configLoc != null && configLoc.startsWith("?"); //$NON-NLS-1$
		String factoryPid = config.getFactoryPid();
		List<ServiceReference<ManagedServiceFactory>> references = getManagedServiceFactoryReferences(factoryPid);
		for (ServiceReference<ManagedServiceFactory> ref : references) {
			if (!hasMoreSpecificConfigPids(ref, factoryPid)) {
				boolean hasOldPermission = configurationAdminFactory.checkTargetPermission(oldLocation, ref);
				boolean hasNewPermission = configurationAdminFactory.checkTargetPermission(configLoc, ref);
				ManagedServiceFactory serviceFactory = getService(ref);
				if (serviceFactory != null) {
					boolean delete = false;
					boolean update = false;
					String targetLocation = ConfigurationAdminImpl.getLocation(ref.getBundle());
					if (hasOldPermission != hasNewPermission) {
						if (hasOldPermission) {
							delete = oldIsMultiple || targetLocation.equals(oldLocation);
						} else {
							update = newIsMultiple || config.bind(targetLocation);
						}
					} else {
						// location has changed, this may be a bound configuration
						if (targetLocation.equals(oldLocation)) {
							delete = true;
						} else {
							update = newIsMultiple || config.bind(targetLocation);
						}
					}
					if (delete) {
						asynchDeleted(serviceFactory, config.getPid());
					} else if (update) {
						Dictionary<String, Object> properties = configurationAdminFactory.modifyConfiguration(ref, config);
						asynchUpdated(serviceFactory, config.getPid(), properties);
					}
					// do not break on !isMultiple since we need to check if the other refs apply no matter what
				}
			}
		}
	}

	private boolean hasMoreSpecificConfigPids(ServiceReference<ManagedServiceFactory> ref, String pid) {
		List<List<String>> qualifiedPidsLists;
		synchronized (targets) {
			qualifiedPidsLists = targets.getQualifiedPids(ref);
		}
		for (List<String> qualifiedPids : qualifiedPidsLists) {
			for (String qualifiedPid : qualifiedPids) {
				if (qualifiedPid.length() <= pid.length() || !qualifiedPid.startsWith(pid)) {
					break;
				}
				ConfigurationImpl config = configurationStore.findConfiguration(qualifiedPid);
				if (config != null) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public ManagedServiceFactory addingService(ServiceReference<ManagedServiceFactory> reference) {
		ManagedServiceFactory service = context.getService(reference);
		if (service == null)
			return null;

		addReference(reference, service);
		return service;
	}

	@Override
	public void modifiedService(ServiceReference<ManagedServiceFactory> reference, ManagedServiceFactory service) {
		List<String> newPids = TargetMap.getPids(reference.getProperty(Constants.SERVICE_PID));
		synchronized (targets) {
			List<List<String>> previousPids = targets.getQualifiedPids(reference);
			if (newPids.size() == previousPids.size()) {
				boolean foundAll = false;
				for (String newPid : newPids) {
					foundAll = false;
					for (List<String> pids : previousPids) {
						if (pids.contains(newPid)) {
							foundAll = true;
							break;
						}
					}
					if (!foundAll) {
						break;
					}
				}
				if (foundAll) {
					return;
				}
			}
		}

		untrackManagedServiceFactory(reference);
		addingService(reference);
	}

	@Override
	public void removedService(ServiceReference<ManagedServiceFactory> reference, ManagedServiceFactory service) {
		untrackManagedServiceFactory(reference);

		context.ungetService(reference);
	}

	private void addReference(ServiceReference<ManagedServiceFactory> reference, ManagedServiceFactory service) {
		List<List<String>> qualifiedPidLists = trackManagedServiceFactory(reference);
		updateManagedServiceFactory(qualifiedPidLists, reference, service);
	}

	private void updateManagedServiceFactory(List<List<String>> qualifiedPidLists, ServiceReference<ManagedServiceFactory> reference, ManagedServiceFactory serviceFactory) {
		for (List<String> qualifiedPids : qualifiedPidLists) {
			qualifiedPids: for (String qualifiedPid : qualifiedPids) {
				ConfigurationImpl[] configs = configurationStore.getFactoryConfigurations(qualifiedPid);
				try {
					for (ConfigurationImpl config : configs) {
						config.lock();
					}
					boolean foundConfig = false;
					for (ConfigurationImpl config : configs) {
						if (config.isDeleted()) {
							// ignore this config
						} else {
							String location = config.getLocation();
							boolean shouldBind = location == null || !location.startsWith("?"); //$NON-NLS-1$
							boolean hasLocPermission = configurationAdminFactory.checkTargetPermission(location, reference);
							if (hasLocPermission) {
								if (shouldBind && config.bind(ConfigurationAdminImpl.getLocation(reference.getBundle())) || !shouldBind) {
									Dictionary<String, Object> properties = configurationAdminFactory.modifyConfiguration(reference, config);
									asynchUpdated(serviceFactory, config.getPid(), properties);
									foundConfig = true;
								} else {
									configurationAdminFactory.log(LogService.LOG_WARNING, "Configuration for " + Constants.SERVICE_PID + "=" + config.getPid() + " could not be bound to " + ConfigurationAdminImpl.getLocation(reference.getBundle())); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
								}
							}
						}
					}
					if (foundConfig) {
						break qualifiedPids;
					}
				} finally {
					for (ConfigurationImpl config : configs)
						config.unlock();
				}
			}
		}

	}

	private List<List<String>> trackManagedServiceFactory(ServiceReference<ManagedServiceFactory> reference) {
		synchronized (targets) {
			return targets.add(reference);
		}
	}

	private void untrackManagedServiceFactory(ServiceReference<ManagedServiceFactory> reference) {
		synchronized (targets) {
			targets.remove(reference);
		}
	}

	private List<ServiceReference<ManagedServiceFactory>> getManagedServiceFactoryReferences(String pid) {
		synchronized (targets) {
			@SuppressWarnings("rawtypes")
			List temp = targets.getTargets(pid);
			@SuppressWarnings("unchecked")
			List<ServiceReference<ManagedServiceFactory>> refs = temp;
			Collections.sort(refs, Collections.reverseOrder());
			return refs;
		}
	}

	private void asynchDeleted(final ManagedServiceFactory service, final String pid) {
		queue.put(new Runnable() {
			@Override
			public void run() {
				try {
					service.deleted(pid);
				} catch (Throwable t) {
					configurationAdminFactory.log(LogService.LOG_ERROR, t.getMessage(), t);
				}
			}
		});
	}

	private void asynchUpdated(final ManagedServiceFactory service, final String pid, final Dictionary<String, Object> properties) {
		if (properties == null) {
			return;
		}
		queue.put(new Runnable() {
			@Override
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
