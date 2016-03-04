/*******************************************************************************
 * Copyright (c) 2006, 2016 Cognos Incorporated, IBM Corporation and others
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.osgi.internal.log;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.*;
import org.eclipse.equinox.log.*;
import org.eclipse.osgi.internal.framework.EquinoxContainer;
import org.osgi.framework.*;
import org.osgi.service.log.*;
import org.osgi.service.log.admin.LoggerAdmin;

public class LogServiceManager implements BundleListener, FrameworkListener, ServiceListener {
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

	public LogServiceManager(int maxHistory, LogListener... systemListeners) {
		logReaderServiceFactory = new ExtendedLogReaderServiceFactory(maxHistory);
		logServiceFactory = new ExtendedLogServiceFactory(logReaderServiceFactory);
		systemBundleLog = logServiceFactory.getLogService(new MockSystemBundle());
		for (LogListener logListener : systemListeners) {
			if (logListener instanceof LogFilter)
				logReaderServiceFactory.addLogListener(logListener, (LogFilter) logListener);
			else
				logReaderServiceFactory.addLogListener(logListener, ExtendedLogReaderServiceFactory.NULL_LOGGER_FILTER);
		}

	}

	public void start(BundleContext context) {
		systemBundleLog.setBundle(context.getBundle());
		context.addBundleListener(this);
		context.addServiceListener(this);
		context.addFrameworkListener(this);

		context.addBundleListener(logServiceFactory);
		logReaderServiceRegistration = context.registerService(LOGREADERSERVICE_CLASSES, logReaderServiceFactory, null);
		logServiceRegistration = context.registerService(LOGSERVICE_CLASSES, logServiceFactory, null);
		Hashtable<String, Object> loggerAdminProps = new Hashtable<String, Object>();
		// TODO the constant for log service id will like be defined
		loggerAdminProps.put("osgi.log.service.id", logServiceRegistration.getReference().getProperty(Constants.SERVICE_ID)); //$NON-NLS-1$
		loggerAdminRegistration = context.registerService(LoggerAdmin.class, logServiceFactory.getLoggerAdmin(), loggerAdminProps);

		eventAdminAdapter = new EventAdminAdapter(context, logReaderServiceFactory);
		eventAdminAdapter.start();
	}

	public void stop(BundleContext context) {
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
	public void bundleChanged(BundleEvent event) {
		Bundle bundle = event.getBundle();
		if (logReaderServiceFactory.isLoggable(bundle, LOGGER_BUNDLE_EVENT, LogService.LOG_INFO)) {
			LoggerImpl logger = (LoggerImpl) systemBundleLog.getLogger(LOGGER_BUNDLE_EVENT);
			logger.log(bundle, null, null, LogService.LOG_INFO, getBundleEventTypeName(event.getType()), null);
		}
	}

	/**
	 * ServiceListener.serviceChanged method.
	 *
	 */
	public void serviceChanged(ServiceEvent event) {
		ServiceReference<?> reference = event.getServiceReference();
		Bundle bundle = reference.getBundle();
		int eventType = event.getType();
		@SuppressWarnings("deprecation")
		int logType = (eventType == ServiceEvent.MODIFIED) ? LogService.LOG_DEBUG : LogService.LOG_INFO;
		if (logReaderServiceFactory.isLoggable(bundle, LOGGER_SERVICE_EVENT, logType)) {
			LoggerImpl logger = (LoggerImpl) systemBundleLog.getLogger(LOGGER_SERVICE_EVENT);
			logger.log(bundle, null, null, logType, getServiceEventTypeName(eventType), null);
		}
	}

	/**
	 * FrameworkListener.frameworkEvent method.
	 *
	 */
	public void frameworkEvent(FrameworkEvent event) {
		Bundle bundle = event.getBundle();
		int eventType = event.getType();
		@SuppressWarnings("deprecation")
		int logType = (eventType == FrameworkEvent.ERROR) ? LogService.LOG_ERROR : LogService.LOG_INFO;
		if (logReaderServiceFactory.isLoggable(bundle, LOGGER_FRAMEWORK_EVENT, logType)) {
			LoggerImpl logger = (LoggerImpl) systemBundleLog.getLogger(LOGGER_FRAMEWORK_EVENT);
			logger.log(bundle, null, null, logType, getFrameworkEventTypeName(eventType), event.getThrowable());
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

		public int compareTo(Bundle o) {
			long idcomp = getBundleId() - o.getBundleId();
			return (idcomp < 0L) ? -1 : ((idcomp > 0L) ? 1 : 0);
		}

		public int getState() {
			return Bundle.RESOLVED;
		}

		public void start(int options) {
			// nothing
		}

		public void start() {
			// nothing
		}

		public void stop(int options) {
			// nothing
		}

		public void stop() {
			// nothing
		}

		public void update(InputStream input) {
			// nothing
		}

		public void update() {
			// nothing
		}

		public void uninstall() {
			// nothing
		}

		public Dictionary<String, String> getHeaders() {
			return new Hashtable<>();
		}

		public long getBundleId() {
			return 0;
		}

		public String getLocation() {
			return Constants.SYSTEM_BUNDLE_LOCATION;
		}

		public ServiceReference<?>[] getRegisteredServices() {
			return null;
		}

		public ServiceReference<?>[] getServicesInUse() {
			return null;
		}

		public boolean hasPermission(Object permission) {
			return true;
		}

		public URL getResource(String name) {
			return null;
		}

		public Dictionary<String, String> getHeaders(String locale) {
			return null;
		}

		public String getSymbolicName() {
			return EquinoxContainer.NAME;
		}

		public Class<?> loadClass(String name) throws ClassNotFoundException {
			throw new ClassNotFoundException();
		}

		public Enumeration<URL> getResources(String name) {
			return null;
		}

		public Enumeration<String> getEntryPaths(String path) {
			return null;
		}

		public URL getEntry(String path) {
			return null;
		}

		public long getLastModified() {
			return System.currentTimeMillis();
		}

		public Enumeration<URL> findEntries(String path, String filePattern, boolean recurse) {
			return null;
		}

		public BundleContext getBundleContext() {
			return null;
		}

		public Map<X509Certificate, List<X509Certificate>> getSignerCertificates(int signersType) {
			return new HashMap<>();
		}

		public Version getVersion() {
			return new Version(0, 0, 0);
		}

		public <A> A adapt(Class<A> type) {
			return null;
		}

		public File getDataFile(String filename) {
			return null;
		}

	}
}
