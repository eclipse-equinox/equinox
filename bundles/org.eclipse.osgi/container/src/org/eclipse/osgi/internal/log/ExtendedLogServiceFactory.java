/*******************************************************************************
 * Copyright (c) 2006, 2021 Cognos Incorporated, IBM Corporation and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package org.eclipse.osgi.internal.log;

import java.security.AccessController;
import java.security.Permission;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.eclipse.equinox.log.ExtendedLogService;
import org.eclipse.equinox.log.LogPermission;
import org.eclipse.osgi.framework.util.SecureAction;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogLevel;
import org.osgi.service.log.Logger;
import org.osgi.service.log.admin.LoggerAdmin;
import org.osgi.service.log.admin.LoggerContext;

public class ExtendedLogServiceFactory implements ServiceFactory<ExtendedLogService>, BundleListener {
	static final SecureAction secureAction = AccessController.doPrivileged(SecureAction.createSecureAction());
	final ReentrantReadWriteLock contextsLock = new ReentrantReadWriteLock();
	final LoggerContextTargetMap loggerContextTargetMap = new LoggerContextTargetMap();
	private final Permission logPermission = new LogPermission("*", LogPermission.LOG); //$NON-NLS-1$
	final ExtendedLogReaderServiceFactory logReaderServiceFactory;
	private final LoggerAdmin loggerAdmin = new EquinoxLoggerAdmin();
	private final boolean captureLogEntryLocation;

	public ExtendedLogServiceFactory(ExtendedLogReaderServiceFactory logReaderServiceFactory,
			boolean captureLogEntryLocation) {
		this.logReaderServiceFactory = logReaderServiceFactory;
		this.captureLogEntryLocation = captureLogEntryLocation;
	}

	boolean captureLogEntryLocation() {
		return captureLogEntryLocation;
	}

	@Override
	public ExtendedLogServiceImpl getService(Bundle bundle, ServiceRegistration<ExtendedLogService> registration) {
		return getLogService(bundle);
	}

	@Override
	public void ungetService(Bundle bundle, ServiceRegistration<ExtendedLogService> registration,
			ExtendedLogService service) {
		// do nothing
		// Notice that we do not remove the the LogService impl for the bundle because
		// other bundles
		// still need to be able to get the cached loggers for a bundle.
	}

	@Override
	public void bundleChanged(BundleEvent event) {
		if (event.getType() == BundleEvent.UNINSTALLED)
			removeLogService(event.getBundle());
	}

	ExtendedLogServiceImpl getLogService(Bundle bundle) {
		contextsLock.writeLock().lock();
		try {
			return loggerContextTargetMap.getLogService(bundle, this);
		} finally {
			contextsLock.writeLock().unlock();
		}
	}

	void shutdown() {
		contextsLock.writeLock().lock();
		try {
			loggerContextTargetMap.clear();
		} finally {
			contextsLock.writeLock().unlock();
		}

	}

	void removeLogService(Bundle bundle) {
		contextsLock.writeLock().lock();
		try {
			loggerContextTargetMap.remove(bundle);
		} finally {
			contextsLock.writeLock().unlock();
		}
	}

	boolean isLoggable(Bundle bundle, String name, int level) {
		return logReaderServiceFactory.isLoggable(bundle, name, level);
	}

	void log(Bundle bundle, String name, StackTraceElement stackTraceElement, Object context, LogLevel logLevelEnum,
			int level, String message, ServiceReference<?> ref, Throwable exception) {
		logReaderServiceFactory.log(bundle, name, stackTraceElement, context, logLevelEnum, level, message, ref,
				exception);
	}

	void checkLogPermission() throws SecurityException {
		SecurityManager sm = System.getSecurityManager();
		if (sm != null)
			sm.checkPermission(logPermission);
	}

	EquinoxLoggerContext createEquinoxLoggerContext(String name) {
		return new EquinoxLoggerContext(name);
	}

	LoggerAdmin getLoggerAdmin() {
		return loggerAdmin;
	}

	class EquinoxLoggerAdmin implements LoggerAdmin {
		@Override
		public LoggerContext getLoggerContext(String name) {
			contextsLock.writeLock().lock();
			try {
				return loggerContextTargetMap.createLoggerContext(name, ExtendedLogServiceFactory.this);
			} finally {
				contextsLock.writeLock().unlock();
			}
		}

	}

	class EquinoxLoggerContext implements LoggerContext {
		final String contextName;
		final Map<String, LogLevel> contextLogLevels = new HashMap<>();

		EquinoxLoggerContext(String name) {
			this.contextName = name;
		}

		@Override
		public String getName() {
			return contextName;
		}

		@Override
		public LogLevel getEffectiveLogLevel(final String name) {
			contextsLock.readLock().lock();
			try {
				LogLevel level = null;
				String lookupName = name;
				while ((level = contextLogLevels.get(lookupName)) == null) {
					int lastDot = lookupName.lastIndexOf('.');
					if (lastDot >= 0) {
						lookupName = lookupName.substring(0, lastDot);
					} else {
						break;
					}
				}
				if (level == null) {
					level = contextLogLevels.get(Logger.ROOT_LOGGER_NAME);
				}
				if (level == null && contextName != null) {
					// non-null context name is a non-root context;
					// must check the root context for non-root contexts
					EquinoxLoggerContext rootContext = loggerContextTargetMap.getRootLoggerContext();
					if (rootContext != null) {
						level = rootContext.getEffectiveLogLevel(name);
					}
				}
				if (level == null) {
					level = logReaderServiceFactory.getDefaultLogLevel();
				}
				return level;
			} finally {
				contextsLock.readLock().unlock();
			}
		}

		@Override
		public Map<String, LogLevel> getLogLevels() {
			contextsLock.readLock().lock();
			try {
				return new HashMap<>(contextLogLevels);
			} finally {
				contextsLock.readLock().unlock();
			}
		}

		@Override
		public void setLogLevels(Map<String, LogLevel> logLevels) {
			boolean readLocked = false;
			try {
				contextsLock.writeLock().lock();
				try {
					contextLogLevels.clear();
					contextLogLevels.putAll(logLevels);
					// downgrade to readlock
					contextsLock.readLock().lock();
					readLocked = true;
				} finally {
					contextsLock.writeLock().unlock();
				}
				loggerContextTargetMap.applyLogLevels(this);
			} finally {
				if (readLocked) {
					contextsLock.readLock().unlock();
				}
			}
		}

		@Override
		public void clear() {
			setLogLevels(Collections.emptyMap());
		}

		@Override
		public boolean isEmpty() {
			contextsLock.readLock().lock();
			try {
				return contextLogLevels.isEmpty();
			} finally {
				contextsLock.readLock().unlock();
			}
		}
	}

}
