/*******************************************************************************
 * Copyright (c) 2006, 2011 Cognos Incorporated, IBM Corporation and others
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.equinox.log.internal;

import java.util.HashMap;
import org.eclipse.equinox.log.ExtendedLogService;
import org.eclipse.equinox.log.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

public class ExtendedLogServiceImpl implements ExtendedLogService, LogService {

	private final ExtendedLogServiceFactory factory;
	private volatile Bundle bundle;
	private final HashMap<String, Logger> loggerCache = new HashMap<String, Logger>();

	public ExtendedLogServiceImpl(ExtendedLogServiceFactory factory, Bundle bundle) {
		this.factory = factory;
		this.bundle = bundle;
	}

	public void log(int level, String message) {
		log(null, level, message, null);
	}

	public void log(int level, String message, Throwable exception) {
		log(null, level, message, exception);
	}

	@SuppressWarnings("rawtypes")
	public void log(ServiceReference sr, int level, String message) {
		log(sr, level, message, null);
	}

	@SuppressWarnings("rawtypes")
	public void log(ServiceReference sr, int level, String message, Throwable exception) {
		getLogger(null).log(sr, level, message, exception);
	}

	public void log(Object context, int level, String message) {
		log(context, level, message, null);
	}

	public void log(Object context, int level, String message, Throwable exception) {
		getLogger(null).log(context, level, message, exception);
	}

	public synchronized Logger getLogger(String name) {
		Logger logger = loggerCache.get(name);
		if (logger == null) {
			logger = new LoggerImpl(this, name);
			loggerCache.put(name, logger);
		}
		return logger;
	}

	public Logger getLogger(Bundle logBundle, String name) {
		if (logBundle == null || logBundle == bundle)
			return getLogger(name);
		// only check permission if getting another bundles log
		factory.checkLogPermission();
		ExtendedLogService bundleLogService = factory.getLogService(logBundle);
		return bundleLogService.getLogger(name);
	}

	public String getName() {
		return getLogger(null).getName();
	}

	public boolean isLoggable(int level) {
		return getLogger(null).isLoggable(level);
	}

	// package private methods called from Logger
	boolean isLoggable(String name, int level) {
		return factory.isLoggable(bundle, name, level);
	}

	// package private methods called from Logger
	void log(String name, Object context, int level, String message, Throwable exception) {
		factory.log(bundle, name, context, level, message, exception);
	}

	void setBundle(Bundle bundle) {
		this.bundle = bundle;
	}
}
