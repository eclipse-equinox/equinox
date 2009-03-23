/*******************************************************************************
 * Copyright (c) 2006, 2008 Cognos Incorporated, IBM Corporation and others
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

public class Activator implements BundleActivator {

	private static final String EVENT_ADMIN_CLASS = "org.osgi.service.event.EventAdmin"; //$NON-NLS-1$
	private static final String[] LOGSERVICE_CLASSES = {LogService.class.getName(), ExtendedLogService.class.getName()};
	private static final String[] LOGREADERSERVICE_CLASSES = {LogReaderService.class.getName(), ExtendedLogReaderService.class.getName()};

	private ServiceRegistration logReaderServiceRegistration;
	private ServiceRegistration logServiceRegistration;
	private EventAdminAdapter eventAdminAdapter;
	private ExtendedLogServiceFactory logServiceFactory;

	public void start(BundleContext context) throws Exception {
		ExtendedLogReaderServiceFactory logReaderServiceFactory = new ExtendedLogReaderServiceFactory();
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
		logServiceRegistration.unregister();
		logServiceRegistration = null;
		logReaderServiceRegistration.unregister();
		logServiceFactory.shutdown();
		logServiceFactory = null;
		if (eventAdminAdapter != null) {
			eventAdminAdapter.stop();
			eventAdminAdapter = null;
		}
		logReaderServiceRegistration = null;
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

}
