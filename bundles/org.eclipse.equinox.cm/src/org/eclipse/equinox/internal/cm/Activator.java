/*******************************************************************************
 * Copyright (c) 2006, 2008 Cognos Incorporated, IBM Corporation
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Cognos Incorporated - initial API and implementation
 *     Chris Aniszczyk <zx@us.ibm.com> - bug 209294
 *******************************************************************************/
package org.eclipse.equinox.internal.cm;

import org.osgi.framework.*;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Activator start the ConfigurationAdminFactory but also handles passing in the Service
 * Registration needed by Asynch threads. Asynch threads are controlled by ConfigurationAdminFactory
 * start and stop. It requires some care to handle pending events as the service is registered before
 * activating the threads. (see EventDispatcher)
 */
public class Activator implements BundleActivator {
	private static final String EVENT_ADMIN_CLASS = "org.osgi.service.event.EventAdmin"; //$NON-NLS-1$
	private LogTracker logTracker;
	private ServiceRegistration registration;
	private ConfigurationAdminFactory factory;
	private ConfigurationEventAdapter eventAdapter;
	private static BundleContext bundleContext;

	private static synchronized void setBundleContext(BundleContext context) {
		bundleContext = context;
	}

	public static synchronized String getProperty(String key) {
		if (bundleContext != null)
			return bundleContext.getProperty(key);

		return null;
	}

	public void start(BundleContext context) throws Exception {
		setBundleContext(context);
		logTracker = new LogTracker(context, System.err);
		logTracker.open();
		if (checkEventAdmin()) {
			eventAdapter = new ConfigurationEventAdapter(context);
			eventAdapter.start();
		}
		factory = new ConfigurationAdminFactory(context, logTracker);
		factory.start();
		context.addBundleListener(factory);
		registration = context.registerService(ConfigurationAdmin.class.getName(), factory, null);
	}

	public void stop(BundleContext context) throws Exception {
		registration.unregister();
		registration = null;
		context.removeBundleListener(factory);
		factory.stop();
		factory = null;
		if (eventAdapter != null) {
			eventAdapter.stop();
			eventAdapter = null;
		}
		logTracker.close();
		logTracker = null;
		setBundleContext(null);
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
