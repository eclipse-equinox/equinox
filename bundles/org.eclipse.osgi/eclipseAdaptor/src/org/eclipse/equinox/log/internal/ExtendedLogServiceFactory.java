/*******************************************************************************
 * Copyright (c) 2006, 2011 Cognos Incorporated, IBM Corporation and others
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.equinox.log.internal;

import java.security.Permission;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.equinox.log.ExtendedLogService;
import org.eclipse.equinox.log.LogPermission;
import org.osgi.framework.*;

public class ExtendedLogServiceFactory implements ServiceFactory<ExtendedLogService>, BundleListener {

	private final Permission logPermission = new LogPermission("*", LogPermission.LOG); //$NON-NLS-1$
	private final ExtendedLogReaderServiceFactory logReaderServiceFactory;
	private final Map<Bundle, ExtendedLogService> logServices = new HashMap<Bundle, ExtendedLogService>();

	public ExtendedLogServiceFactory(ExtendedLogReaderServiceFactory logReaderServiceFactory) {
		this.logReaderServiceFactory = logReaderServiceFactory;
	}

	public ExtendedLogServiceImpl getService(Bundle bundle, ServiceRegistration<ExtendedLogService> registration) {
		return getLogService(bundle);
	}

	public void ungetService(Bundle bundle, ServiceRegistration<ExtendedLogService> registration, ExtendedLogService service) {
		// do nothing
		// Notice that we do not remove the the LogService impl for the bundle because other bundles
		// still need to be able to get the cached loggers for a bundle.
	}

	public void bundleChanged(BundleEvent event) {
		if (event.getType() == BundleEvent.UNINSTALLED)
			removeLogService(event.getBundle());
	}

	synchronized ExtendedLogServiceImpl getLogService(Bundle bundle) {
		ExtendedLogServiceImpl logService = (ExtendedLogServiceImpl) logServices.get(bundle);
		if (logService == null) {
			logService = new ExtendedLogServiceImpl(this, bundle);
			if (bundle != null && bundle.getState() != Bundle.UNINSTALLED)
				logServices.put(bundle, logService);
		}
		return logService;
	}

	synchronized void shutdown() {
		logServices.clear();
	}

	synchronized void removeLogService(Bundle bundle) {
		logServices.remove(bundle);
	}

	boolean isLoggable(Bundle bundle, String name, int level) {
		return logReaderServiceFactory.isLoggable(bundle, name, level);
	}

	void log(Bundle bundle, String name, Object context, int level, String message, Throwable exception) {
		logReaderServiceFactory.log(bundle, name, context, level, message, exception);
	}

	void checkLogPermission() throws SecurityException {
		SecurityManager sm = System.getSecurityManager();
		if (sm != null)
			sm.checkPermission(logPermission);
	}
}
