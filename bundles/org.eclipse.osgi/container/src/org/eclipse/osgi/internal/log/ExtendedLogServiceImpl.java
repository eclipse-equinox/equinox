/*******************************************************************************
 * Copyright (c) 2006, 2012 Cognos Incorporated, IBM Corporation and others
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.osgi.internal.log;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.equinox.log.ExtendedLogService;
import org.eclipse.equinox.log.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

public class ExtendedLogServiceImpl implements ExtendedLogService {

	private final ExtendedLogServiceFactory factory;
	private volatile Bundle bundle;
	private final Map<Class<? extends org.osgi.service.log.Logger>, Map<String, Logger>> loggerCache = new HashMap<>();

	public ExtendedLogServiceImpl(ExtendedLogServiceFactory factory, Bundle bundle) {
		this.factory = factory;
		this.bundle = bundle;
		loggerCache.put(org.osgi.service.log.Logger.class, new HashMap<String, Logger>());
		loggerCache.put(org.osgi.service.log.FormatterLogger.class, new HashMap<String, Logger>());
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
		getLogger((String) null).log(sr, level, message, exception);
	}

	public void log(Object context, int level, String message) {
		log(context, level, message, null);
	}

	public void log(Object context, int level, String message, Throwable exception) {
		getLogger((String) null).log(context, level, message, exception);
	}

	public Logger getLogger(String name) {
		return (Logger) getLogger(name, org.osgi.service.log.Logger.class);
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
		return getLogger((String) null).getName();
	}

	public boolean isLoggable(int level) {
		return getLogger((String) null).isLoggable(level);
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

	@Override
	public org.osgi.service.log.Logger getLogger(Class<?> clazz) {
		return getLogger(clazz.getName());
	}

	@Override
	public synchronized <L extends org.osgi.service.log.Logger> L getLogger(String name, Class<L> loggerType) {
		Map<String, Logger> loggers = loggerCache.get(loggerType);
		if (loggers == null) {
			throw new IllegalArgumentException(loggerType.getName());
		}
		Logger logger = loggers.get(name);
		if (logger == null) {
			logger = new FormatterLoggerImpl(this, name);
			loggers.put(name, logger);
		}
		return loggerType.cast(logger);
	}

	@Override
	public <L extends org.osgi.service.log.Logger> L getLogger(Class<?> clazz, Class<L> loggerType) {
		return getLogger(clazz.getName(), loggerType);
	}

	@Override
	public boolean isTraceEnabled() {
		return getLogger((String) null).isTraceEnabled();
	}

	@Override
	public void trace(String message) {
		getLogger((String) null).trace(message);
	}

	@Override
	public void trace(String format, Object arg) {
		getLogger((String) null).trace(format, arg);
	}

	@Override
	public void trace(String format, Object arg1, Object arg2) {
		getLogger((String) null).trace(format, arg1, arg2);
	}

	@Override
	public void trace(String format, Object... arguments) {
		getLogger((String) null).trace(format, arguments);
	}

	@Override
	public boolean isDebugEnabled() {
		return getLogger((String) null).isDebugEnabled();
	}

	@Override
	public void debug(String message) {
		getLogger((String) null).debug(message);
	}

	@Override
	public void debug(String format, Object arg) {
		getLogger((String) null).debug(format, arg);
	}

	@Override
	public void debug(String format, Object arg1, Object arg2) {
		getLogger((String) null).debug(format, arg1, arg2);
	}

	@Override
	public void debug(String format, Object... arguments) {
		getLogger((String) null).debug(format, arguments);
	}

	@Override
	public boolean isInfoEnabled() {
		return getLogger((String) null).isInfoEnabled();
	}

	@Override
	public void info(String message) {
		getLogger((String) null).info(message);
	}

	@Override
	public void info(String format, Object arg) {
		getLogger((String) null).info(format, arg);
	}

	@Override
	public void info(String format, Object arg1, Object arg2) {
		getLogger((String) null).info(format, arg1, arg2);
	}

	@Override
	public void info(String format, Object... arguments) {
		getLogger((String) null).info(format, arguments);
	}

	@Override
	public boolean isWarnEnabled() {
		return getLogger((String) null).isWarnEnabled();
	}

	@Override
	public void warn(String message) {
		getLogger((String) null).warn(message);
	}

	@Override
	public void warn(String format, Object arg) {
		getLogger((String) null).warn(format, arg);
	}

	@Override
	public void warn(String format, Object arg1, Object arg2) {
		getLogger((String) null).warn(format, arg1, arg2);
	}

	@Override
	public void warn(String format, Object... arguments) {
		getLogger((String) null).warn(format, arguments);
	}

	@Override
	public boolean isErrorEnabled() {
		return getLogger((String) null).isErrorEnabled();
	}

	@Override
	public void error(String message) {
		getLogger((String) null).error(message);
	}

	@Override
	public void error(String format, Object arg) {
		getLogger((String) null).error(format, arg);
	}

	@Override
	public void error(String format, Object arg1, Object arg2) {
		getLogger((String) null).error(format, arg1, arg2);
	}

	@Override
	public void error(String format, Object... arguments) {
		getLogger((String) null).error(format, arguments);
	}

	@Override
	public void audit(String message) {
		getLogger((String) null).audit(message);
	}

	@Override
	public void audit(String format, Object arg) {
		getLogger((String) null).audit(format, arg);
	}

	@Override
	public void audit(String format, Object arg1, Object arg2) {
		getLogger((String) null).audit(format, arg1, arg2);
	}

	@Override
	public void audit(String format, Object... arguments) {
		getLogger((String) null).audit(format, arguments);
	}
}
