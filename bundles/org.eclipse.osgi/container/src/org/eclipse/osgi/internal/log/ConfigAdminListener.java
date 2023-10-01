/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogLevel;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

class ConfigAdminListener implements ServiceTrackerCustomizer<Object, ServiceRegistration<?>> {

	private static final String CLASS_CONFIG_ADMIN = "org.osgi.service.cm.ConfigurationAdmin"; //$NON-NLS-1$
	private static final String METHOD_CONFIG_ADMIN_GET_CONFIGURATION = "getConfiguration"; //$NON-NLS-1$
	private static final String METHOD_CONFIG_ADMIN_LIST_CONFIGURATIONS = "listConfigurations"; //$NON-NLS-1$

	private static final String CLASS_SYNC_CONFIG_LISTENER = "org.osgi.service.cm.SynchronousConfigurationListener"; //$NON-NLS-1$
	private static final String CLASS_CONFIG_EVENT = "org.osgi.service.cm.ConfigurationEvent"; //$NON-NLS-1$
	private static final String METHOD_CONFIG_EVENT_GET_PID = "getPid"; //$NON-NLS-1$
	private static final String METHOD_CONFIG_EVENT_GET_FACTORY_PID = "getFactoryPid"; //$NON-NLS-1$
	private static final String METHOD_CONFIG_EVENT_GET_REFERENCE = "getReference"; //$NON-NLS-1$
	private static final String METHOD_CONFIG_EVENT_GET_TYPE = "getType"; //$NON-NLS-1$
	private static final int CM_UPDATED = 1;
	private static final int CM_DELETED = 2;
	private static final int CM_LOCATION_CHANGED = 3;

	private static final String CLASS_CONFIG = "org.osgi.service.cm.Configuration"; //$NON-NLS-1$
	private static final String METHOD_CONFIG_GET_PROPERTIES = "getProperties"; //$NON-NLS-1$
	private static final String METHOD_CONFIG_GET_PID = "getPid"; //$NON-NLS-1$
	private static final String METHOD_CONFIG_GET_FACTORY_PID = "getFactoryPid"; //$NON-NLS-1$

	private static final String PID_PREFIX_LOG_ADMIN = "org.osgi.service.log.admin"; //$NON-NLS-1$
	// using String constructor here to avoid interning
	private static final String PID_FILTER = '(' + Constants.SERVICE_PID + '=' + PID_PREFIX_LOG_ADMIN + '*' + ')';

	private final ServiceTracker<Object, ServiceRegistration<?>> configTracker;
	final ExtendedLogServiceFactory factory;
	final BundleContext context;

	ConfigAdminListener(BundleContext context, ExtendedLogServiceFactory factory) {
		this.context = context;
		this.configTracker = new ServiceTracker<>(context, CLASS_CONFIG_ADMIN, this);
		this.factory = factory;
	}

	void start() {
		configTracker.open();
	}

	void stop() {
		configTracker.close();
	}

	private ServiceRegistration<?> registerConfigurationListener(ServiceReference<?> configRef) {
		try {
			Class<?> listenerClass = configRef.getBundle().loadClass(CLASS_SYNC_CONFIG_LISTENER);
			return registerProxyConfigListener(configRef, listenerClass);
		} catch (ClassNotFoundException | NoSuchMethodException e) {
			throw new RuntimeException(CLASS_SYNC_CONFIG_LISTENER, e);
		}
	}

	private ServiceRegistration<?> registerProxyConfigListener(ServiceReference<?> configRef, Class<?> listenerClass)
			throws ClassNotFoundException, NoSuchMethodException {
		LoggerContextConfiguration loggerConfiguration = new LoggerContextConfiguration(listenerClass, configRef);
		return loggerConfiguration.register();
	}

	@Override
	public ServiceRegistration<?> addingService(ServiceReference<Object> configRef) {
		return registerConfigurationListener(configRef);
	}

	@Override
	public void modifiedService(ServiceReference<Object> configRef, ServiceRegistration<?> configReg) {
		// Nothing to do
	}

	@Override
	public void removedService(ServiceReference<Object> configRef, ServiceRegistration<?> loggerConfiguration) {
		loggerConfiguration.unregister();
	}

	class LoggerContextConfiguration implements InvocationHandler {
		private final Object listenerProxy;
		private final Object configAdmin;
		private final ServiceReference<?> configAdminRef;

		private final Class<?> configClass;
		private final Method getConfigProperties;
		private final Method getConfigPid;
		private final Method getConfigFactoryPid;

		private final Class<?> configAdminClass;
		private final Method getConfiguration;
		private final Method listConfigurations;

		private final Class<?> configEventClass;
		private final Method getEventPid;
		private final Method getEventFactoryPid;
		private final Method getEventReference;
		private final Method getEventType;

		public LoggerContextConfiguration(Class<?> listenerClass, ServiceReference<?> ref)
				throws ClassNotFoundException, NoSuchMethodException {
			listenerProxy = Proxy.newProxyInstance(listenerClass.getClassLoader(), new Class[] { listenerClass }, this);
			configAdminRef = ref;
			ClassLoader cl = listenerClass.getClassLoader();

			configClass = cl.loadClass(CLASS_CONFIG);
			getConfigProperties = configClass.getMethod(METHOD_CONFIG_GET_PROPERTIES);
			getConfigFactoryPid = configClass.getMethod(METHOD_CONFIG_GET_FACTORY_PID);
			getConfigPid = configClass.getMethod(METHOD_CONFIG_GET_PID);

			configAdminClass = cl.loadClass(CLASS_CONFIG_ADMIN);
			getConfiguration = configAdminClass.getMethod(METHOD_CONFIG_ADMIN_GET_CONFIGURATION, String.class,
					String.class);
			listConfigurations = configAdminClass.getMethod(METHOD_CONFIG_ADMIN_LIST_CONFIGURATIONS, String.class);

			configEventClass = cl.loadClass(CLASS_CONFIG_EVENT);
			getEventPid = configEventClass.getMethod(METHOD_CONFIG_EVENT_GET_PID);
			getEventFactoryPid = configEventClass.getMethod(METHOD_CONFIG_EVENT_GET_FACTORY_PID);
			getEventReference = configEventClass.getMethod(METHOD_CONFIG_EVENT_GET_REFERENCE);
			getEventType = configEventClass.getMethod(METHOD_CONFIG_EVENT_GET_TYPE);

			configAdmin = context.getService(ref);
		}

		public ServiceRegistration<?> register() {
			// register it with the config admin context to ensure consistent class space
			Bundle configBundle = configAdminRef.getBundle();
			BundleContext configContext = configBundle != null ? configAdminRef.getBundle().getBundleContext() : null;
			if (configContext == null) {
				// seems the bundle has stopped!
				return null;
			}
			ServiceRegistration<?> registration = configContext.registerService(CLASS_SYNC_CONFIG_LISTENER,
					listenerProxy, null);

			try {
				Object[] configs = (Object[]) listConfigurations.invoke(configAdmin, PID_FILTER);
				if (configs != null) {
					for (Object config : configs) {
						String factoryPid = (String) getConfigFactoryPid.invoke(config);
						if (factoryPid != null) {
							continue;
						}
						String pid = (String) getConfigPid.invoke(config);
						String contextName = getContextName(pid);
						@SuppressWarnings("unchecked")
						Dictionary<String, Object> configDictionary = (Dictionary<String, Object>) getConfigProperties
								.invoke(config);
						if (configDictionary != null) {
							setLogLevels(contextName, getLogLevels(configDictionary));
						}
					}
				}
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new RuntimeException(e);
			}
			return registration;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] event) throws Throwable {
			// There is only one method on ConfigurationListener, no need to check the
			// method type
			// ConfigurationListener::configurationEvent(ConfigurationEvent)
			if (!configAdminRef.equals(getReference(event))) {
				// ignore other config admin events
				return null;
			}

			String pid = getEventPid(event);
			if (pid == null) {
				// a factory pid or more likely doesn't have the correct prefix; ignore
				return null;
			}

			int type = getType(event);
			if (type == CM_LOCATION_CHANGED) {
				// TODO not sure if we should check location or not
				return null;
			}

			String contextName = getContextName(pid);
			if (type == CM_DELETED) {
				setLogLevels(contextName, Collections.emptyMap());
				return null;
			}

			if (type == CM_UPDATED) {
				Dictionary<String, Object> configDictionary = findConfiguration(pid);
				if (configDictionary == null) {
					// Configuration got deleted before we could get it so treat as deleted
					setLogLevels(contextName, Collections.emptyMap());
					return null;
				}

				Map<String, LogLevel> levelConfig = getLogLevels(configDictionary);
				setLogLevels(contextName, levelConfig);
			}
			return null;
		}

		private String getContextName(String pid) {
			if (PID_PREFIX_LOG_ADMIN.equals(pid)) {
				return null;
			}
			char separator = pid.charAt(PID_PREFIX_LOG_ADMIN.length());
			if (separator != '|') {
				return null;
			}
			int startName = PID_PREFIX_LOG_ADMIN.length() + 1;
			return pid.substring(startName);
		}

		private Map<String, LogLevel> getLogLevels(Dictionary<String, Object> configDictionary) {
			Map<String, LogLevel> result = new HashMap<>(configDictionary.size());
			for (Enumeration<String> keys = configDictionary.keys(); keys.hasMoreElements();) {
				String key = keys.nextElement();
				Object v = configDictionary.get(key);
				if (v instanceof String) {
					try {
						result.put(key, LogLevel.valueOf((String) v));
					} catch (IllegalArgumentException e) {
						// ignore invalid values
					}
				}
			}
			return result;
		}

		private Object getReference(Object[] event) {
			try {
				return getEventReference.invoke(event[0]);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		}

		private String getEventPid(Object[] event) {
			try {
				String factoryPid = (String) getEventFactoryPid.invoke(event[0]);
				if (factoryPid != null) {
					// ignore factory pids
					return null;
				}
				String pid = (String) getEventPid.invoke(event[0]);
				if (pid.startsWith(PID_PREFIX_LOG_ADMIN)) {
					return pid;
				}
				return null;
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		}

		private int getType(Object[] event) {
			try {
				Integer type = (Integer) getEventType.invoke(event[0]);
				return type == null ? 0 : type.intValue();
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		}

		@SuppressWarnings("unchecked")
		private Dictionary<String, Object> findConfiguration(String pid) {
			try {
				Object config = getConfiguration.invoke(configAdmin, pid, null);
				return (Dictionary<String, Object>) getConfigProperties.invoke(config);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		}

		private void setLogLevels(String contextName, Map<String, LogLevel> logLevels) {
			factory.getLoggerAdmin().getLoggerContext(contextName).setLogLevels(logLevels);
		}
	}
}
