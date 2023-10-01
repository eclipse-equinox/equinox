/*******************************************************************************
 * Copyright (c) 2006, 2012 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.internal.log;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import org.eclipse.equinox.log.Logger;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;

class EquinoxLogFactory implements ServiceFactory<FrameworkLog> {
	final EquinoxLogWriter defaultWriter;
	final LogServiceManager logManager;

	public EquinoxLogFactory(EquinoxLogWriter defaultWriter, LogServiceManager logManager) {
		this.defaultWriter = defaultWriter;
		this.logManager = logManager;
	}

	@Override
	public FrameworkLog getService(final Bundle bundle, ServiceRegistration<FrameworkLog> registration) {
		return createFrameworkLog(bundle, defaultWriter);
	}

	FrameworkLog createFrameworkLog(Bundle bundle, EquinoxLogWriter eclipseWriter) {
		final EquinoxLogWriter logWriter = eclipseWriter == null ? defaultWriter : eclipseWriter;
		final Logger logger = bundle == null ? logManager.getSystemBundleLog().getLogger(eclipseWriter.getLoggerName())
				: logManager.getSystemBundleLog().getLogger(bundle, logWriter.getLoggerName());
		return new FrameworkLog() {

			@Override
			public void setWriter(Writer newWriter, boolean append) {
				logWriter.setWriter(newWriter, append);
			}

			@Override
			public void setFile(File newFile, boolean append) throws IOException {
				logWriter.setFile(newFile, append);
			}

			@Override
			public void setConsoleLog(boolean consoleLog) {
				logWriter.setConsoleLog(consoleLog);
			}

			@Override
			public void log(FrameworkLogEntry logEntry) {
				logger.log(logEntry, convertLevel(logEntry), logEntry.getMessage(), logEntry.getThrowable());
			}

			@Override
			public void log(FrameworkEvent frameworkEvent) {
				Bundle b = frameworkEvent.getBundle();
				Throwable t = frameworkEvent.getThrowable();
				String entry = b.getSymbolicName() == null ? b.getLocation() : b.getSymbolicName();
				int severity;
				switch (frameworkEvent.getType()) {
				case FrameworkEvent.INFO:
					severity = FrameworkLogEntry.INFO;
					break;
				case FrameworkEvent.ERROR:
					severity = FrameworkLogEntry.ERROR;
					break;
				case FrameworkEvent.WARNING:
					severity = FrameworkLogEntry.WARNING;
					break;
				default:
					severity = FrameworkLogEntry.OK;
				}
				FrameworkLogEntry logEntry = new FrameworkLogEntry(entry, severity, 0, "", 0, t, null); //$NON-NLS-1$
				log(logEntry);
			}

			@Override
			public File getFile() {
				return logWriter.getFile();
			}

			@Override
			public void close() {
				logWriter.close();
			}
		};
	}

	@Override
	public void ungetService(Bundle bundle, ServiceRegistration<FrameworkLog> registration, FrameworkLog service) {
		// nothing
	}

	@SuppressWarnings("deprecation")
	static int convertLevel(FrameworkLogEntry logEntry) {
		switch (logEntry.getSeverity()) {
		case FrameworkLogEntry.ERROR:
			return LogService.LOG_ERROR;
		case FrameworkLogEntry.WARNING:
			return LogService.LOG_WARNING;
		case FrameworkLogEntry.INFO:
			return LogService.LOG_INFO;
		case FrameworkLogEntry.OK:
			return LogService.LOG_DEBUG;
		case FrameworkLogEntry.CANCEL:
		default:
			return 32; // unknown
		}
	}
}
