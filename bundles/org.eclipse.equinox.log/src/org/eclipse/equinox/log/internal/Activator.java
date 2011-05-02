/*******************************************************************************
 * Copyright (c) 2006, 2011 Cognos Incorporated, IBM Corporation and others
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.equinox.log.internal;

import org.eclipse.equinox.log.ExtendedLogReaderService;
import org.eclipse.equinox.log.ExtendedLogService;
import org.osgi.framework.*;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;

public class Activator implements BundleActivator, BundleListener, FrameworkListener, ServiceListener {

	private static final String EVENT_ADMIN_CLASS = "org.osgi.service.event.EventAdmin"; //$NON-NLS-1$
	private static final String[] LOGSERVICE_CLASSES = {LogService.class.getName(), ExtendedLogService.class.getName()};
	private static final String[] LOGREADERSERVICE_CLASSES = {LogReaderService.class.getName(), ExtendedLogReaderService.class.getName()};

	private ServiceRegistration logReaderServiceRegistration;
	private ServiceRegistration logServiceRegistration;
	private EventAdminAdapter eventAdminAdapter;
	private volatile ExtendedLogReaderServiceFactory logReaderServiceFactory;
	private ExtendedLogServiceFactory logServiceFactory;

	public void start(BundleContext context) throws Exception {
		ServiceReference[] logRef = context.getServiceReferences(ExtendedLogService.class.getName(), null);
		if (logRef != null)
			for (int i = 0; i < logRef.length; i++) {
				Bundle provider = logRef[i].getBundle();
				if (provider != null && provider.getBundleId() == 0)
					return;
			}
		logReaderServiceFactory = new ExtendedLogReaderServiceFactory();
		context.addBundleListener(this);
		context.addServiceListener(this);
		context.addFrameworkListener(this);

		if (checkEventAdmin()) {
			eventAdminAdapter = new EventAdminAdapter(context, logReaderServiceFactory);
			eventAdminAdapter.start();
		}
		logServiceFactory = new ExtendedLogServiceFactory(logReaderServiceFactory);
		context.addBundleListener(logServiceFactory);
		logReaderServiceRegistration = context.registerService(LOGREADERSERVICE_CLASSES, logReaderServiceFactory, null);
		logServiceRegistration = context.registerService(LOGSERVICE_CLASSES, logServiceFactory, null);
	}

	public void stop(BundleContext context) throws Exception {
		if (logServiceRegistration == null)
			return;
		logServiceRegistration.unregister();
		logServiceRegistration = null;
		logReaderServiceRegistration.unregister();
		logReaderServiceRegistration = null;
		logServiceFactory.shutdown();
		logServiceFactory = null;
		if (eventAdminAdapter != null) {
			eventAdminAdapter.stop();
			eventAdminAdapter = null;
		}
		context.removeFrameworkListener(this);
		context.removeServiceListener(this);
		context.removeBundleListener(this);
	}

	private static boolean checkEventAdmin() {
		// cannot support scheduling without the event admin package
		try {
			Class.forName(EVENT_ADMIN_CLASS);
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	/**
	 * BundleListener.bundleChanged method.
	 *
	 */
	public void bundleChanged(BundleEvent event) {
		Bundle bundle = event.getBundle();
		if (logReaderServiceFactory.isLoggable(bundle, null, LogService.LOG_INFO))
			logReaderServiceFactory.log(bundle, null, null, LogService.LOG_INFO, getBundleEventTypeName(event.getType()), null);
	}

	/**
	 * ServiceListener.serviceChanged method.
	 *
	 */
	public void serviceChanged(ServiceEvent event) {
		ServiceReference reference = event.getServiceReference();
		Bundle bundle = reference.getBundle();
		int eventType = event.getType();
		int logType = (eventType == ServiceEvent.MODIFIED) ? LogService.LOG_DEBUG : LogService.LOG_INFO;
		if (logReaderServiceFactory.isLoggable(bundle, null, logType))
			logReaderServiceFactory.log(bundle, null, reference, logType, getServiceEventTypeName(eventType), null);
	}

	/**
	 * FrameworkListener.frameworkEvent method.
	 *
	 */
	public void frameworkEvent(FrameworkEvent event) {
		Bundle bundle = event.getBundle();
		int eventType = event.getType();
		int logType = (eventType == FrameworkEvent.ERROR) ? LogService.LOG_ERROR : LogService.LOG_INFO;
		Throwable throwable = (eventType == FrameworkEvent.ERROR) ? event.getThrowable() : null;
		if (logReaderServiceFactory.isLoggable(bundle, null, logType))
			logReaderServiceFactory.log(bundle, null, null, logType, getFrameworkEventTypeName(eventType), throwable);
	}

	/**
	 * Convert BundleEvent type to a string.
	 *
	 */
	protected static String getBundleEventTypeName(int type) {
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
	protected static String getServiceEventTypeName(int type) {
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
	protected static String getFrameworkEventTypeName(int type) {
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
}
