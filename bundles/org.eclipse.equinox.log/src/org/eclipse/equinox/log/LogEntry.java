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

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

/**
 * The memory space aware LogEntry implementation.
 */
public class LogEntry implements org.osgi.service.log.LogEntry, Cloneable {
	/**
	 * The severity of the log entry that should be one of the
	 * four levels defined in the <code>LogService</code> interface.
	 */
	protected int level;

	/** The <code>System.currentTimeMillis()</code> the entry was made. */
	protected long time;

	/** The bundle that created the log entry. */
	protected Bundle bundle;

	/** The service reference that caused the log entry. */
	protected ServiceReference serviceReference;

	/** The log message. */
	protected String message;

	/**
	 * The copy of the exception that caused the log
	 * or <code>null</code>.
	 */
	protected LoggedException loggedException;

	/**
	 * Creates a log entry,
	 *
	 * @param	level		the severity (one of 4 as defined by LogService)
	 * @param	message		the message text
	 * @param	bundle		the bundle that created the entry
	 * @param	reference	the reference to the service associated with the entry
	 * @param	exception	the exception that caused that log entry or <code>null</code>
	 */
	protected LogEntry(int level, String message, Bundle bundle, ServiceReference reference, Throwable exception) {
		this.time = System.currentTimeMillis();
		this.level = level;

		// make a copy of the message so it lives in this memory space
		if (message != null) {
			this.message = new String(message.toCharArray());
		}

		this.bundle = bundle;
		this.serviceReference = reference;

		// Make a copy of the exception in this memory space
		if (exception != null) {
			this.loggedException = new LoggedException(exception);
		}
	}

	/**
	 * Returns the bundle that created this log entry.
	 */
	public Bundle getBundle() {
		return (bundle);
	}

	/**
	 * Returns the service that this log entry is associated with.
	 */
	public ServiceReference getServiceReference() {
		return (serviceReference);
	}

	/**
	 * Returns the severity of the log entry.
	 * Should be one of the four levels defined in LogService.
	 */
	public int getLevel() {
		return (level);
	}

	/**
	 * Returns the human readable message that was
	 * recorded with this log entry.
	 */
	public String getMessage() {
		return (message);
	}

	/**
	 * Returns the exception that was recorded with this log entry.
	 * 
	 * A {@link LoggedException) that contains information about
	 * the original exception is returned.
	 */
	public Throwable getException() {
		return (loggedException);
	}

	/**
	 * Returns the time log entry was created.
	 */
	public long getTime() {
		return (time);
	}

	/**
	 * Returns a copy of this log entry. The copy will contain
	 * references to clopies of the internal data, not references 
	 * to the original internal data of this <code>LogEntry</code> object. 
	 *
	 * @return  A deep copy of this log entry.
	 */
	public LogEntry copy() {
		LogEntry copy = new LogEntry(level, message, bundle, serviceReference, loggedException);
		copy.time = time;
		return (copy);
	}
}
