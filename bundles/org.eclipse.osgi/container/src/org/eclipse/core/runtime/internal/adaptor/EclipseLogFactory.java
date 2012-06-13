/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.core.runtime.internal.adaptor;

import java.io.*;
import org.eclipse.equinox.log.Logger;
import org.eclipse.equinox.log.internal.LogServiceManager;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.osgi.framework.*;
import org.osgi.service.log.LogService;

public class EclipseLogFactory implements ServiceFactory<FrameworkLog> {
	final EclipseLogWriter defaultWriter;
	final LogServiceManager logManager;

	public EclipseLogFactory(EclipseLogWriter defaultWriter, LogServiceManager logManager) {
		this.defaultWriter = defaultWriter;
		this.logManager = logManager;
	}

	public FrameworkLog getService(final Bundle bundle, ServiceRegistration<FrameworkLog> registration) {
		return createFrameworkLog(bundle, defaultWriter);
	}

	FrameworkLog createFrameworkLog(Bundle bundle, EclipseLogWriter eclipseWriter) {
		final EclipseLogWriter logWriter = eclipseWriter == null ? defaultWriter : eclipseWriter;
		final Logger logger = bundle == null ? logManager.getSystemBundleLog().getLogger(eclipseWriter.getLoggerName()) : logManager.getSystemBundleLog().getLogger(bundle, logWriter.getLoggerName());
		return new FrameworkLog() {

			public void setWriter(Writer newWriter, boolean append) {
				logWriter.setWriter(newWriter, append);
			}

			public void setFile(File newFile, boolean append) throws IOException {
				logWriter.setFile(newFile, append);
			}

			public void setConsoleLog(boolean consoleLog) {
				logWriter.setConsoleLog(consoleLog);
			}

			public void log(FrameworkLogEntry logEntry) {
				logger.log(logEntry, convertLevel(logEntry), logEntry.getMessage(), logEntry.getThrowable());
			}

			public void log(FrameworkEvent frameworkEvent) {
				Bundle b = frameworkEvent.getBundle();
				Throwable t = frameworkEvent.getThrowable();
				String entry = b.getSymbolicName() == null ? b.getLocation() : b.getSymbolicName();
				int severity;
				switch (frameworkEvent.getType()) {
					case FrameworkEvent.INFO :
						severity = FrameworkLogEntry.INFO;
						break;
					case FrameworkEvent.ERROR :
						severity = FrameworkLogEntry.ERROR;
						break;
					case FrameworkEvent.WARNING :
						severity = FrameworkLogEntry.WARNING;
						break;
					default :
						severity = FrameworkLogEntry.OK;
				}
				FrameworkLogEntry logEntry = new FrameworkLogEntry(entry, severity, 0, "", 0, t, null); //$NON-NLS-1$
				log(logEntry);
			}

			public File getFile() {
				return logWriter.getFile();
			}

			public void close() {
				logWriter.close();
			}
		};
	}

	public void ungetService(Bundle bundle, ServiceRegistration<FrameworkLog> registration, FrameworkLog service) {
		// nothing
	}

	static int convertLevel(FrameworkLogEntry logEntry) {
		switch (logEntry.getSeverity()) {
			case FrameworkLogEntry.ERROR :
				return LogService.LOG_ERROR;
			case FrameworkLogEntry.WARNING :
				return LogService.LOG_WARNING;
			case FrameworkLogEntry.INFO :
				return LogService.LOG_INFO;
			case FrameworkLogEntry.OK :
				return LogService.LOG_DEBUG;
			case FrameworkLogEntry.CANCEL :
			default :
				return 32; // unknown
		}
	}
}
