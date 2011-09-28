/*******************************************************************************
 *  Copyright (c) 2000, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.runtime;

import java.util.ArrayList;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.log.*;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.osgi.framework.Bundle;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogService;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * A log writer that writes log entries.  
 * <p>
 * Note that this class just provides a bridge from the old ILog interface
 * to the new extended log service
 */
public class PlatformLogWriter implements SynchronousLogListener, LogFilter {
	public static final String EQUINOX_LOGGER_NAME = "org.eclipse.equinox.logger"; //$NON-NLS-1$
	private final ExtendedLogService logService;
	private final PackageAdmin packageAdmin;
	private final Bundle bundle;

	public PlatformLogWriter(ExtendedLogService logService, PackageAdmin packageAdmin, Bundle bundle) {
		this.logService = logService;
		this.packageAdmin = packageAdmin;
		this.bundle = bundle;
	}

	void logging(IStatus status) {
		Bundle b = getBundle(status);
		Logger equinoxLog = logService.getLogger(b, EQUINOX_LOGGER_NAME);
		equinoxLog.log(getLog(status), getLevel(status), status.getMessage(), status.getException());
	}

	public static int getLevel(IStatus status) {
		switch (status.getSeverity()) {
			case IStatus.ERROR :
				return LogService.LOG_ERROR;
			case IStatus.WARNING :
				return LogService.LOG_WARNING;
			case IStatus.INFO :
				return LogService.LOG_INFO;
			case IStatus.OK :
				return LogService.LOG_DEBUG;
			case IStatus.CANCEL :
			default :
				return 32; // unknown
		}
	}

	public static FrameworkLogEntry getLog(IStatus status) {
		Throwable t = status.getException();
		ArrayList childlist = new ArrayList();

		int stackCode = t instanceof CoreException ? 1 : 0;
		// ensure a substatus inside a CoreException is properly logged 
		if (stackCode == 1) {
			IStatus coreStatus = ((CoreException) t).getStatus();
			if (coreStatus != null) {
				childlist.add(getLog(coreStatus));
			}
		}

		if (status.isMultiStatus()) {
			IStatus[] children = status.getChildren();
			for (int i = 0; i < children.length; i++) {
				childlist.add(getLog(children[i]));
			}
		}

		FrameworkLogEntry[] children = (FrameworkLogEntry[]) (childlist.size() == 0 ? null : childlist.toArray(new FrameworkLogEntry[childlist.size()]));

		return new FrameworkLogEntry(status, status.getPlugin(), status.getSeverity(), status.getCode(), status.getMessage(), stackCode, t, children);
	}

	private Bundle getBundle(IStatus status) {
		String pluginID = status.getPlugin();
		if (pluginID == null)
			return bundle;
		Bundle[] bundles = packageAdmin.getBundles(pluginID, null);
		return bundles == null || bundles.length == 0 ? bundle : bundles[0];
	}

	public boolean isLoggable(Bundle bundle, String loggerName, int logLevel) {
		return EQUINOX_LOGGER_NAME.equals(loggerName) && RuntimeLog.hasListeners();
	}

	public void logged(LogEntry entry) {
		RuntimeLog.logToListeners(convertToStatus(entry));
	}

	public static IStatus convertToStatus(LogEntry logEntry) {
		Object context = null;
		if (logEntry instanceof ExtendedLogEntry)
			context = ((ExtendedLogEntry) logEntry).getContext();
		if (context instanceof IStatus)
			return (IStatus) context;
		if (context instanceof FrameworkLogEntry) {
			FrameworkLogEntry fLogEntry = (FrameworkLogEntry) context;
			context = fLogEntry.getContext();
			if (context instanceof IStatus)
				return (IStatus) context;
			return convertToStatus(fLogEntry);
		}
		return convertRawEntryToStatus(logEntry);
	}

	private static IStatus convertToStatus(FrameworkLogEntry entry) {
		FrameworkLogEntry[] children = entry.getChildren();
		if (children != null) {
			IStatus[] statusChildren = new Status[children.length];
			for (int i = 0; i < statusChildren.length; i++)
				statusChildren[i] = convertToStatus(children[i]);
			return new MultiStatus(entry.getEntry(), entry.getBundleCode(), statusChildren, entry.getMessage(), entry.getThrowable());
		}
		return new Status(entry.getSeverity(), entry.getEntry(), entry.getBundleCode(), entry.getMessage(), entry.getThrowable());
	}

	private static IStatus convertRawEntryToStatus(LogEntry logEntry) {
		int severity;
		switch (logEntry.getLevel()) {
			case LogService.LOG_ERROR :
				severity = IStatus.ERROR;
				break;
			case LogService.LOG_WARNING :
				severity = IStatus.WARNING;
				break;
			case LogService.LOG_INFO :
				severity = IStatus.INFO;
				break;
			case LogService.LOG_DEBUG :
				severity = IStatus.OK;
				break;
			default :
				severity = -1;
				break;
		}
		Bundle bundle = logEntry.getBundle();
		return new Status(severity, bundle == null ? null : bundle.getSymbolicName(), logEntry.getMessage(), logEntry.getException());
	}
}
