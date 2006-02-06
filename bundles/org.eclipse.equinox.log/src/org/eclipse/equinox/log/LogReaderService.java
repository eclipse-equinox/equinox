/*******************************************************************************
 * Copyright (c) 1999, 2005 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.log;

import java.util.Enumeration;
import java.util.Vector;
import org.eclipse.osgi.framework.eventmgr.EventDispatcher;
import org.eclipse.osgi.framework.eventmgr.EventListeners;
import org.osgi.framework.Bundle;
import org.osgi.service.log.LogListener;

/**
 * LogReaderService class.
 */

public class LogReaderService implements org.osgi.service.log.LogReaderService, EventDispatcher {
	protected Activator log;
	protected Bundle bundle;
	protected EventListeners logEvent;

	protected LogReaderService(Activator log, Bundle bundle) {
		this.log = log;
		this.bundle = bundle;
		logEvent = null;
	}

	protected void close() {
		Activator tempLog = this.log;
		this.log = null;
		this.bundle = null;

		if (logEvent != null) {
			tempLog.logEvent.removeListener(this);
			logEvent.removeAllListeners();
			logEvent = null;
		}
	}

	public void dispatchEvent(Object l, Object lo, int action, Object object) {
		Activator tempLog = this.log;

		if (tempLog == null) {
			return;
		}

		LogListener listener = (LogListener) lo;
		listener.logged(((LogEntry) object).copy());
	}

	/*
	 * ----------------------------------------------------------------------
	 *      LogReaderService Interface implementation
	 * ----------------------------------------------------------------------
	 */

	/**
	 *  Subscribe to log events.  The LogListener will get a callback each
	 *  time a message is logged.  The requester must have Admin permission.
	 */
	public void addLogListener(LogListener listener) {
		Activator tempLog = this.log;

		if (tempLog == null) {
			return;
		}

		synchronized (tempLog.logEvent) {
			if (logEvent == null) {
				logEvent = new EventListeners();
				tempLog.logEvent.addListener(this, this);
			} else {
				logEvent.removeListener(listener);
			}

			logEvent.addListener(listener, listener);
		}
	}

	/**
	 *  Unsubscribe to log events. The requester must have Admin permission.
	 */
	public void removeLogListener(LogListener listener) {
		Activator tempLog = this.log;

		if (tempLog == null) {
			return;
		}

		if (logEvent != null) {
			synchronized (tempLog.logEvent) {
				logEvent.removeListener(listener);
			}
		}
	}

	/**
	 *  Returns an enumeration of the last log messages.  Each element will
	 *  be of type LogEntry.  Whether the enumeration is of all the logs since
	 *  bootup or the recent past is implementation specific.  Also whether
	 *  informational and debug entries are included in the logging interval
	 *  is implementation specific.  The requester must have Admin permission.
	 */
	public Enumeration getLog() {
		Activator tempLog = this.log;

		if (tempLog == null) {
			return (new Vector(0).elements());
		}

		return (tempLog.logEntries());
	}

}
