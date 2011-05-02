/*******************************************************************************
 * Copyright (c) 2006, 2009 Cognos Incorporated, IBM Corporation and others
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.equinox.log.internal;

import java.security.Permission;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.equinox.log.LogPermission;
import org.osgi.framework.*;

public class ExtendedLogServiceFactory implements ServiceFactory, BundleListener {

	private final Permission logPermission = new LogPermission("*", LogPermission.LOG); //$NON-NLS-1$
	private final ExtendedLogReaderServiceFactory logReaderServiceFactory;
	private Map logServices = new HashMap();

	public ExtendedLogServiceFactory(ExtendedLogReaderServiceFactory logReaderServiceFactory) {
		this.logReaderServiceFactory = logReaderServiceFactory;
	}

	public Object getService(Bundle bundle, ServiceRegistration registration) {
		return getLogService(bundle);
	}

	public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
		// do nothing
	}

	public void bundleChanged(BundleEvent event) {
		if (event.getType() == BundleEvent.UNINSTALLED)
			removeLogService(event.getBundle());
	}

	protected synchronized ExtendedLogServiceImpl getLogService(Bundle bundle) {
		if (logServices == null)
			throw new IllegalStateException("LogService is shutdown."); //$NON-NLS-1$

		ExtendedLogServiceImpl logService = (ExtendedLogServiceImpl) logServices.get(bundle);
		if (logService == null) {
			logService = new ExtendedLogServiceImpl(this, bundle);
			if (bundle.getState() != Bundle.UNINSTALLED)
				logServices.put(bundle, logService);
		}
		return logService;
	}

	protected synchronized void shutdown() {
		if (logServices != null) {
			logServices.clear();
			logServices = null;
		}
	}

	private synchronized void removeLogService(Bundle bundle) {
		if (logServices != null)
			logServices.remove(bundle);
	}

	protected boolean isLoggable(Bundle bundle, String name, int level) {
		return logReaderServiceFactory.isLoggable(bundle, name, level);
	}

	protected void log(Bundle bundle, String name, Object context, int level, String message, Throwable exception) {
		logReaderServiceFactory.log(bundle, name, context, level, message, exception);
	}

	protected void checkLogPermission() throws SecurityException {
		SecurityManager sm = System.getSecurityManager();
		if (sm != null)
			sm.checkPermission(logPermission);
	}
}
