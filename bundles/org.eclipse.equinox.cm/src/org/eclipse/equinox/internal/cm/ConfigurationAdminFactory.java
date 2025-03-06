/*******************************************************************************
 * Copyright (c) 2005, 2024 Cognos Incorporated, IBM Corporation and others.
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
 *     Christoph Läubrich - add support for Coordinator
 *******************************************************************************/
package org.eclipse.equinox.internal.cm;

import java.security.Permission;
import java.util.*;
import java.util.function.Predicate;
import org.osgi.framework.*;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationPermission;
import org.osgi.service.coordinator.*;
import org.osgi.util.tracker.ServiceTracker;

/**
 * ConfigurationAdminFactory provides a Configuration Admin ServiceFactory but
 * more significantly launches the whole implementation.
 */

public class ConfigurationAdminFactory implements ServiceFactory<ConfigurationAdmin>, BundleListener {

	static private final Permission allConfigurationPermission = new ConfigurationPermission("*", //$NON-NLS-1$
			ConfigurationPermission.CONFIGURE);
	static private final Permission allAttributePermission = new ConfigurationPermission("*", //$NON-NLS-1$
			ConfigurationPermission.ATTRIBUTE);
	private final EventDispatcher eventDispatcher;
	private final PluginManager pluginManager;
	private final LogTracker log;
	private final ManagedServiceTracker managedServiceTracker;
	private final ManagedServiceFactoryTracker managedServiceFactoryTracker;
	private final ConfigurationStore configurationStore;
	private final ServiceTracker<Coordinator, Coordinator> coordinationServiceTracker;

	public ConfigurationAdminFactory(BundleContext context, LogTracker log) {
		this.log = log;
		configurationStore = new ConfigurationStore(this, context);
		eventDispatcher = new EventDispatcher(context, log, this);
		pluginManager = new PluginManager(context);
		managedServiceTracker = new ManagedServiceTracker(this, configurationStore, context);
		managedServiceFactoryTracker = new ManagedServiceFactoryTracker(this, configurationStore, context);
		coordinationServiceTracker = new ServiceTracker<>(context, Coordinator.class, null);
	}

	void start() {
		coordinationServiceTracker.open();
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
		coordinationServiceTracker.close();
	}

	@Override
	public ConfigurationAdmin getService(Bundle bundle, ServiceRegistration<ConfigurationAdmin> registration) {
		ServiceReference<ConfigurationAdmin> reference = registration.getReference();
		eventDispatcher.setServiceReference(reference);
		return new ConfigurationAdminImpl(this, configurationStore, bundle);
	}

	@Override
	public void ungetService(Bundle bundle, ServiceRegistration<ConfigurationAdmin> registration,
			ConfigurationAdmin service) {
		// do nothing
	}

	@Override
	public void bundleChanged(BundleEvent event) {
		if (event.getType() == BundleEvent.UNINSTALLED) {
			configurationStore.unbindConfigurations(event.getBundle());
		}
	}

	public void checkConfigurePermission(String location, String forBundleLocation) throws SecurityException {
		SecurityManager sm = System.getSecurityManager();
		if (sm != null) {
			if (forBundleLocation == null || !forBundleLocation.equals(location)) {
				if (location == null) {
					sm.checkPermission(allConfigurationPermission);
				} else {
					sm.checkPermission(new ConfigurationPermission(location, ConfigurationPermission.CONFIGURE));
				}
			}
		}
	}

	public boolean checkTargetPermission(String location, ServiceReference<?> ref) {
		SecurityManager sm = System.getSecurityManager();
		if (sm != null) {
			Bundle b = ref.getBundle();
			if (location != null && b != null) {
				String forBundleLocation = ConfigurationAdminImpl.getLocation(b);
				if (!forBundleLocation.equals(location)) {
					if (location != null) {
						return b.hasPermission(new ConfigurationPermission(location, ConfigurationPermission.TARGET));
					}
				}
			}
		}
		return true;
	}

	public void checkAttributePermission(String location) throws SecurityException {
		SecurityManager sm = System.getSecurityManager();
		if (sm != null) {
			if (location == null) {
				sm.checkPermission(allAttributePermission);
			} else {
				sm.checkPermission(new ConfigurationPermission(location, ConfigurationPermission.ATTRIBUTE));
			}
		}
	}

	void warn(String message) {
		log.warn(message);
	}

	void error(String message) {
		log.error(message);
	}

	void error(String message, Throwable exception) {
		log.error(message, exception);
	}

	void dispatchEvent(int type, String factoryPid, String pid) {
		eventDispatcher.dispatchEvent(type, factoryPid, pid);
	}

	void notifyConfigurationUpdated(ConfigurationImpl config, boolean isFactory) {
		if (isFactory) {
			managedServiceFactoryTracker.notifyUpdated(config);
		} else {
			managedServiceTracker.notifyUpdated(config);
		}
	}

	void notifyConfigurationDeleted(ConfigurationImpl config, boolean isFactory) {
		if (isFactory) {
			managedServiceFactoryTracker.notifyDeleted(config);
		} else {
			managedServiceTracker.notifyDeleted(config);
		}
	}

	void notifyLocationChanged(ConfigurationImpl config, String oldLocation, boolean isFactory) {
		if (isFactory) {
			managedServiceFactoryTracker.notifyUpdateLocation(config, oldLocation);
		} else {
			managedServiceTracker.notifyUpdateLocation(config, oldLocation);
		}
	}

	Dictionary<String, Object> modifyConfiguration(ServiceReference<?> reference, ConfigurationImpl config) {
		return pluginManager.modifyConfiguration(reference, config);
	}

	ConfigurationAdminParticipant coordinationParticipant(Coordination coordination) {
		return coordination.getParticipants().stream().filter(ConfigurationAdminParticipant.class::isInstance)
				.map(ConfigurationAdminParticipant.class::cast).findFirst()
				.orElseGet(() -> new ConfigurationAdminParticipant(coordination));
	}

	Optional<Coordination> coordinate() {
		return Optional.ofNullable(coordinationServiceTracker.getService()).map(Coordinator::peek)
				.filter(Predicate.not(Coordination::isTerminated));
	}

	private final class ConfigurationAdminParticipant implements Participant {

		private final Map<Object, Runnable> tasks = new LinkedHashMap<>();

		public ConfigurationAdminParticipant(Coordination coordination) {
			coordination.addParticipant(this);
		}

		@Override
		public void ended(Coordination coordination) throws Exception {
			finish(coordination);
		}

		@Override
		public void failed(Coordination coordination) throws Exception {
			finish(coordination);
		}

		private void finish(Coordination coordination) {
			tasks.values().forEach(Runnable::run);
			tasks.clear();
		}

		public void cancelTask(Object key) {
			tasks.remove(key);
		}

		public void addTask(Object key, Runnable runnable) {
			tasks.put(key, runnable);
		}
	}

	void executeCoordinated(Object key, Runnable runnable) {
		coordinate().ifPresentOrElse(coordination -> coordinationParticipant(coordination).addTask(key, runnable),
				() -> runnable.run());
	}

	void cancelExecuteCoordinated(Object key) {
		coordinate().ifPresent(coordination -> coordinationParticipant(coordination).cancelTask(key));
	}
}
