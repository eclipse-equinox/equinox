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
import org.osgi.service.cm.*;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * ManagedServiceTracker tracks... ManagedServices and notifies them about related configuration changes
 */
class ManagedServiceTracker extends ServiceTracker<ManagedService, ManagedService> {

	final ConfigurationAdminFactory configurationAdminFactory;
	private final ConfigurationStore configurationStore;

	/** @GuardedBy targets*/
	private final TargetMap targets = new TargetMap();

	private final SerializedTaskQueue queue = new SerializedTaskQueue("ManagedService Update Queue"); //$NON-NLS-1$

	public ManagedServiceTracker(ConfigurationAdminFactory configurationAdminFactory, ConfigurationStore configurationStore, BundleContext context) {
		super(context, ManagedService.class.getName(), null);
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
		String pid = config.getPid(false);
		List<ServiceReference<ManagedService>> references = getManagedServiceReferences(pid);
		for (ServiceReference<ManagedService> ref : references) {
			if (!hasMoreSpecificConfigPids(ref, pid)) {
				boolean hasLocPermission = configurationAdminFactory.checkTargetPermission(configLoc, ref);
				ManagedService service = getService(ref);
				if (hasLocPermission && service != null) {
					if (isMultiple || ConfigurationAdminImpl.getLocation(ref.getBundle()).equals(configLoc)) {
						// search for other matches
						List<List<String>> qualifiedPidLists;
						synchronized (targets) {
							qualifiedPidLists = targets.getQualifiedPids(ref);
						}
						updateManagedService(qualifiedPidLists, ref, service);
					}
				}
			}
		}
	}

	void notifyUpdated(ConfigurationImpl config) {
		config.checkLocked();
		String configLoc = config.getLocation();
		boolean isMultiple = configLoc != null && configLoc.startsWith("?"); //$NON-NLS-1$
		String pid = config.getPid();
		List<ServiceReference<ManagedService>> references = getManagedServiceReferences(pid);
		for (ServiceReference<ManagedService> ref : references) {
			if (!hasMoreSpecificConfigPids(ref, pid)) {
				boolean hasLocPermission = configurationAdminFactory.checkTargetPermission(configLoc, ref);
				ManagedService service = getService(ref);
				if (hasLocPermission && service != null) {
					if (isMultiple || config.bind(ConfigurationAdminImpl.getLocation(ref.getBundle()))) {
						Dictionary<String, Object> properties = configurationAdminFactory.modifyConfiguration(ref, config);
						asynchUpdated(service, properties);
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
		String pid = config.getPid();
		List<ServiceReference<ManagedService>> references = getManagedServiceReferences(pid);
		for (ServiceReference<ManagedService> ref : references) {
			if (!hasMoreSpecificConfigPids(ref, pid)) {
				boolean hasOldPermission = configurationAdminFactory.checkTargetPermission(oldLocation, ref);
				boolean hasNewPermission = configurationAdminFactory.checkTargetPermission(configLoc, ref);
				ManagedService service = getService(ref);
				if (service != null) {
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
						// search for other matches
						List<List<String>> qualifiedPidLists;
						synchronized (targets) {
							qualifiedPidLists = targets.getQualifiedPids(ref);
						}
						updateManagedService(qualifiedPidLists, ref, service);
					} else if (update) {
						Dictionary<String, Object> properties = configurationAdminFactory.modifyConfiguration(ref, config);
						asynchUpdated(service, properties);
					}
					// do not break on !isMultiple since we need to check if the other refs apply no matter what
				}
			}
		}
	}

	private boolean hasMoreSpecificConfigPids(ServiceReference<ManagedService> ref, String pid) {
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
	public ManagedService addingService(ServiceReference<ManagedService> reference) {
		ManagedService service = context.getService(reference);
		if (service == null)
			return null;

		addReference(reference, service);
		return service;
	}

	@Override
	public void modifiedService(ServiceReference<ManagedService> reference, ManagedService service) {
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

		untrackManagedService(reference);
		addingService(reference);
	}

	@Override
	public void removedService(ServiceReference<ManagedService> reference, ManagedService service) {
		untrackManagedService(reference);

		context.ungetService(reference);
	}

	private void addReference(ServiceReference<ManagedService> reference, ManagedService service) {
		List<List<String>> qualifiedPidLists = trackManagedService(reference);
		updateManagedService(qualifiedPidLists, reference, service);
	}

	private void updateManagedService(List<List<String>> qualifiedPidLists, ServiceReference<ManagedService> reference, ManagedService service) {
		for (List<String> qualifiedPids : qualifiedPidLists) {
			boolean foundConfig = false;
			qualifiedPids: for (String qualifiedPid : qualifiedPids) {
				ConfigurationImpl config = configurationStore.findConfiguration(qualifiedPid);
				if (config != null) {
					try {
						config.lock();
						if (!config.isDeleted()) {
							if (config.getFactoryPid() != null) {
								configurationAdminFactory.log(LogService.LOG_WARNING, "Configuration for " + Constants.SERVICE_PID + "=" + qualifiedPid + " should only be used by a " + ManagedServiceFactory.class.getName()); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
							}
							String location = config.getLocation();
							boolean shouldBind = location == null || !location.startsWith("?"); //$NON-NLS-1$
							boolean hasLocPermission = configurationAdminFactory.checkTargetPermission(location, reference);
							if (hasLocPermission) {
								if ((shouldBind && config.bind(ConfigurationAdminImpl.getLocation(reference.getBundle()))) || !shouldBind) {
									Dictionary<String, Object> properties = configurationAdminFactory.modifyConfiguration(reference, config);
									asynchUpdated(service, properties);
									foundConfig = true;
									break qualifiedPids;
								}
								configurationAdminFactory.log(LogService.LOG_WARNING, "Configuration for " + Constants.SERVICE_PID + "=" + qualifiedPid + " could not be bound to " + ConfigurationAdminImpl.getLocation(reference.getBundle())); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
							}
						}
					} finally {
						config.unlock();
					}
				}
			}
			if (!foundConfig) {
				// This seems questionable to me, but is required for the spec.
				// if a ManagedService has multiple pids, watch out!!
				asynchUpdated(service, null);
			}
		}
	}

	private List<List<String>> trackManagedService(ServiceReference<ManagedService> reference) {
		synchronized (targets) {
			return targets.add(reference);
		}
	}

	private void untrackManagedService(ServiceReference<ManagedService> reference) {
		synchronized (targets) {
			targets.remove(reference);
		}
	}

	private List<ServiceReference<ManagedService>> getManagedServiceReferences(String pid) {
		synchronized (targets) {
			@SuppressWarnings("rawtypes")
			List temp = targets.getTargets(pid);
			@SuppressWarnings("unchecked")
			List<ServiceReference<ManagedService>> refs = temp;
			Collections.sort(refs, Collections.reverseOrder());
			return refs;
		}
	}

	private void asynchUpdated(final ManagedService service, final Dictionary<String, ?> properties) {
		queue.put(new Runnable() {
			@Override
			public void run() {
				try {
					service.updated(properties);
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
