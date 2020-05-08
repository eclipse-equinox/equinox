/*******************************************************************************
 * Copyright (c) 2006, 2020 Cognos Incorporated, IBM Corporation and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package org.eclipse.osgi.internal.log;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import org.eclipse.equinox.log.ExtendedLogReaderService;
import org.eclipse.equinox.log.ExtendedLogService;
import org.eclipse.equinox.log.LogFilter;
import org.eclipse.osgi.internal.framework.BundleContextImpl;
import org.eclipse.osgi.internal.framework.EquinoxContainer;
import org.osgi.framework.AllServiceListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.framework.Version;
import org.osgi.service.log.LogLevel;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;
import org.osgi.service.log.LoggerFactory;
import org.osgi.service.log.admin.LoggerAdmin;

public class LogServiceManager implements SynchronousBundleListener, FrameworkListener, AllServiceListener {
	private static final String LOGGER_FRAMEWORK_EVENT = "Events.Framework"; //$NON-NLS-1$
	private static final String LOGGER_BUNDLE_EVENT = "Events.Bundle"; //$NON-NLS-1$
	private static final String LOGGER_SERVICE_EVENT = "Events.Service"; //$NON-NLS-1$

	private static final String[] LOGSERVICE_CLASSES = {LogService.class.getName(), LoggerFactory.class.getName(), ExtendedLogService.class.getName()};
	private static final String[] LOGREADERSERVICE_CLASSES = {LogReaderService.class.getName(), ExtendedLogReaderService.class.getName()};

	private ServiceRegistration<?> logReaderServiceRegistration;
	private ServiceRegistration<?> logServiceRegistration;
	private ServiceRegistration<LoggerAdmin> loggerAdminRegistration;
	private final ExtendedLogReaderServiceFactory logReaderServiceFactory;
	private final ExtendedLogServiceFactory logServiceFactory;
	private final ExtendedLogServiceImpl systemBundleLog;
	private EventAdminAdapter eventAdminAdapter;
	private ConfigAdminListener configAdminListener;

	public LogServiceManager(int maxHistory, LogLevel defaultLevel, boolean captureLogEntryLocation, LogListener... systemListeners) {
		logReaderServiceFactory = new ExtendedLogReaderServiceFactory(maxHistory, defaultLevel);
		logServiceFactory = new ExtendedLogServiceFactory(logReaderServiceFactory, captureLogEntryLocation);
		systemBundleLog = logServiceFactory.getLogService(new MockSystemBundle());
		for (LogListener logListener : systemListeners) {
			if (logListener instanceof LogFilter)
				logReaderServiceFactory.addLogListener(logListener, (LogFilter) logListener);
			else
				logReaderServiceFactory.addLogListener(logListener, ExtendedLogReaderServiceFactory.NULL_LOGGER_FILTER);
		}

	}

	public void start(BundleContext context) {
		logReaderServiceFactory.start(((BundleContextImpl) context).getContainer());
		systemBundleLog.setBundle(context.getBundle());
		context.addBundleListener(this);
		context.addServiceListener(this);
		context.addFrameworkListener(this);

		context.addBundleListener(logServiceFactory);
		logReaderServiceRegistration = context.registerService(LOGREADERSERVICE_CLASSES, logReaderServiceFactory, null);
		logServiceRegistration = context.registerService(LOGSERVICE_CLASSES, logServiceFactory, null);
		Hashtable<String, Object> loggerAdminProps = new Hashtable<>();
		// TODO the constant for log service id will like be defined
		loggerAdminProps.put("osgi.log.service.id", logServiceRegistration.getReference().getProperty(Constants.SERVICE_ID)); //$NON-NLS-1$
		loggerAdminRegistration = context.registerService(LoggerAdmin.class, logServiceFactory.getLoggerAdmin(), loggerAdminProps);

		eventAdminAdapter = new EventAdminAdapter(context, logReaderServiceFactory);
		eventAdminAdapter.start();
		configAdminListener = new ConfigAdminListener(context, logServiceFactory);
		configAdminListener.start();
	}

	public void stop(BundleContext context) {
		configAdminListener.stop();
		configAdminListener = null;
		eventAdminAdapter.stop();
		eventAdminAdapter = null;
		loggerAdminRegistration.unregister();
		loggerAdminRegistration = null;
		logServiceRegistration.unregister();
		logServiceRegistration = null;
		logReaderServiceRegistration.unregister();
		logReaderServiceRegistration = null;
		logServiceFactory.shutdown();
		context.removeFrameworkListener(this);
		context.removeServiceListener(this);
		context.removeBundleListener(this);
		logReaderServiceFactory.stop();
	}

	public ExtendedLogService getSystemBundleLog() {
		return systemBundleLog;
	}

	LoggerAdmin getLoggerAdmin() {
		return logServiceFactory.getLoggerAdmin();
	}

	/**
	 * BundleListener.bundleChanged method.
	 *
	 */
	@SuppressWarnings("deprecation")
	@Override
	public void bundleChanged(BundleEvent event) {
		Bundle bundle = event.getBundle();
		String bsn = (bundle == null) ? null : bundle.getSymbolicName();
		String loggerName = (bsn == null) ? LOGGER_BUNDLE_EVENT : LOGGER_BUNDLE_EVENT + "." + bsn; //$NON-NLS-1$
		if (logReaderServiceFactory.isLoggable(bundle, loggerName, LogService.LOG_INFO)) {
			LoggerImpl logger = (LoggerImpl) systemBundleLog.getLogger(loggerName);
			int eventType = event.getType();
			logger.log(bundle, event, null, LogService.LOG_INFO, getBundleEventTypeName(eventType), null, null);
		}
	}

	/**
	 * ServiceListener.serviceChanged method.
	 *
	 */
	@Override
	public void serviceChanged(ServiceEvent event) {
		ServiceReference<?> reference = event.getServiceReference();
		Bundle bundle = reference.getBundle();
		int eventType = event.getType();
		String bsn = (bundle == null) ? null : bundle.getSymbolicName();
		String loggerName = (bsn == null) ? LOGGER_SERVICE_EVENT : LOGGER_SERVICE_EVENT + "." + bsn; //$NON-NLS-1$
		@SuppressWarnings("deprecation")
		int logType = (eventType == ServiceEvent.MODIFIED) ? LogService.LOG_DEBUG : LogService.LOG_INFO;
		if (logReaderServiceFactory.isLoggable(bundle, loggerName, logType)) {
			LoggerImpl logger = (LoggerImpl) systemBundleLog.getLogger(loggerName);
			logger.log(bundle, event, null, logType, getServiceEventTypeName(eventType), reference, null);
		}
	}

	/**
	 * FrameworkListener.frameworkEvent method.
	 *
	 */
	@SuppressWarnings("deprecation")
	@Override
	public void frameworkEvent(FrameworkEvent event) {
		Bundle bundle = event.getBundle();
		int eventType = event.getType();
		int logType;
		switch (eventType) {
			case FrameworkEvent.ERROR :
				logType = LogService.LOG_ERROR;
				break;
			case FrameworkEvent.WARNING :
				logType = LogService.LOG_WARNING;
				break;
			default :
				logType = LogService.LOG_INFO;
				break;
		}
		String bsn = (bundle == null) ? null : bundle.getSymbolicName();
		String loggerName = (bsn == null) ? LOGGER_FRAMEWORK_EVENT : LOGGER_FRAMEWORK_EVENT + "." + bsn; //$NON-NLS-1$
		if (logReaderServiceFactory.isLoggable(bundle, loggerName, logType)) {
			LoggerImpl logger = (LoggerImpl) systemBundleLog.getLogger(loggerName);
			logger.log(bundle, event, null, logType, getFrameworkEventTypeName(eventType), null, event.getThrowable());
		}
	}

	/**
	 * Convert BundleEvent type to a string.
	 *
	 */
	private static String getBundleEventTypeName(int type) {
		switch (type) {
			case BundleEvent.INSTALLED :
				return ("BundleEvent INSTALLED"); //$NON-NLS-1$

			case BundleEvent.RESOLVED :
				return ("BundleEvent RESOLVED"); //$NON-NLS-1$

			case BundleEvent.STARTED :
				return ("BundleEvent STARTED"); //$NON-NLS-1$

			case BundleEvent.STARTING :
				return ("BundleEvent STARTING"); //$NON-NLS-1$

			case BundleEvent.STOPPED :
				return ("BundleEvent STOPPED"); //$NON-NLS-1$

			case BundleEvent.STOPPING :
				return ("BundleEvent STOPPING"); //$NON-NLS-1$

			case BundleEvent.UNINSTALLED :
				return ("BundleEvent UNINSTALLED"); //$NON-NLS-1$

			case BundleEvent.UNRESOLVED :
				return ("BundleEvent UNRESOLVED"); //$NON-NLS-1$

			case BundleEvent.UPDATED :
				return ("BundleEvent UPDATED"); //$NON-NLS-1$

			default :
				return ("BundleEvent " + Integer.toHexString(type)); //$NON-NLS-1$
		}
	}

	/**
	 * Convert ServiceEvent type to a string.
	 *
	 */
	private static String getServiceEventTypeName(int type) {
		switch (type) {
			case ServiceEvent.REGISTERED :
				return ("ServiceEvent REGISTERED"); //$NON-NLS-1$

			case ServiceEvent.MODIFIED :
				return ("ServiceEvent MODIFIED"); //$NON-NLS-1$

			case ServiceEvent.UNREGISTERING :
				return ("ServiceEvent UNREGISTERING"); //$NON-NLS-1$

			default :
				return ("ServiceEvent " + Integer.toHexString(type)); //$NON-NLS-1$
		}
	}

	/**
	 * Convert FrameworkEvent type to a string.
	 *
	 */
	private static String getFrameworkEventTypeName(int type) {
		switch (type) {
			case FrameworkEvent.ERROR :
				return ("FrameworkEvent ERROR"); //$NON-NLS-1$

			case FrameworkEvent.INFO :
				return ("FrameworkEvent INFO"); //$NON-NLS-1$

			case FrameworkEvent.PACKAGES_REFRESHED :
				return ("FrameworkEvent PACKAGES REFRESHED"); //$NON-NLS-1$

			case FrameworkEvent.STARTED :
				return ("FrameworkEvent STARTED"); //$NON-NLS-1$

			case FrameworkEvent.STARTLEVEL_CHANGED :
				return ("FrameworkEvent STARTLEVEL CHANGED"); //$NON-NLS-1$

			case FrameworkEvent.WARNING :
				return ("FrameworkEvent WARNING"); //$NON-NLS-1$

			default :
				return ("FrameworkEvent " + Integer.toHexString(type)); //$NON-NLS-1$
		}
	}

	static class MockSystemBundle implements Bundle {

		@Override
		public int compareTo(Bundle o) {
			long idcomp = getBundleId() - o.getBundleId();
			return (idcomp < 0L) ? -1 : ((idcomp > 0L) ? 1 : 0);
		}

		@Override
		public int getState() {
			return Bundle.RESOLVED;
		}

		@Override
		public void start(int options) {
			// nothing
		}

		@Override
		public void start() {
			// nothing
		}

		@Override
		public void stop(int options) {
			// nothing
		}

		@Override
		public void stop() {
			// nothing
		}

		@Override
		public void update(InputStream input) {
			// nothing
		}

		@Override
		public void update() {
			// nothing
		}

		@Override
		public void uninstall() {
			// nothing
		}

		@Override
		public Dictionary<String, String> getHeaders() {
			return new Hashtable<>();
		}

		@Override
		public long getBundleId() {
			return 0;
		}

		@Override
		public String getLocation() {
			return Constants.SYSTEM_BUNDLE_LOCATION;
		}

		@Override
		public ServiceReference<?>[] getRegisteredServices() {
			return null;
		}

		@Override
		public ServiceReference<?>[] getServicesInUse() {
			return null;
		}

		@Override
		public boolean hasPermission(Object permission) {
			return true;
		}

		@Override
		public URL getResource(String name) {
			return null;
		}

		@Override
		public Dictionary<String, String> getHeaders(String locale) {
			return null;
		}

		@Override
		public String getSymbolicName() {
			return EquinoxContainer.NAME;
		}

		@Override
		public Class<?> loadClass(String name) throws ClassNotFoundException {
			throw new ClassNotFoundException();
		}

		@Override
		public Enumeration<URL> getResources(String name) {
			return null;
		}

		@Override
		public Enumeration<String> getEntryPaths(String path) {
			return null;
		}

		@Override
		public URL getEntry(String path) {
			return null;
		}

		@Override
		public long getLastModified() {
			return System.currentTimeMillis();
		}

		@Override
		public Enumeration<URL> findEntries(String path, String filePattern, boolean recurse) {
			return null;
		}

		@Override
		public BundleContext getBundleContext() {
			return null;
		}

		@Override
		public Map<X509Certificate, List<X509Certificate>> getSignerCertificates(int signersType) {
			return new HashMap<>();
		}

		@Override
		public Version getVersion() {
			return new Version(0, 0, 0);
		}

		@Override
		public <A> A adapt(Class<A> type) {
			return null;
		}

		@Override
		public File getDataFile(String filename) {
			return null;
		}

	}
}
