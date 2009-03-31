/*******************************************************************************
 * Copyright (c) 2006-2009 IBM Corporation and others
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.equinox.log;

import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;

/**
 * Extends the OSGi Log Services <code>LogEntry</code> object to provide additional context information.
 * Otherwise similarly accessible by registering a <code>LogListener</code> object.
 * 
 * @ThreadSafe
 * @see LogListener
 */
public interface ExtendedLogEntry extends LogEntry {

	/**
	 * Returns the logger name associated with this <code>LogEntry</code>
	 * object.
	 * 
	 * @return <code>String</code> containing the logger name associated with this
	 *         <code>LogEntry</code> object;<code>null</code> if no logger name is
	 *         associated with this <code>LogEntry</code> object.
	 */
	String getLoggerName();

	/**
	 * Returns the context associated with this <code>LogEntry</code>
	 * object.
	 * 
	 * @return <code>Object</code> containing the context associated with this
	 *         <code>LogEntry</code> object;<code>null</code> if no context is
	 *         associated with this <code>LogEntry</code> object.
	 */
	Object getContext();

	/**
	 * Returns the thread id of the logging thread associated with this <code>LogEntry</code>
	 * object.
	 * 
	 * @return <code>long</code> containing the thread id associated with this
	 *         <code>LogEntry</code> object.
	 */
	long getThreadId();

	/**
	 * Returns the thread name of the logging thread associated with this <code>LogEntry</code>
	 * object.
	 * 
	 * @return <code>String</code> containing the message associated with this
	 *         <code>LogEntry</code> object.
	 */
	String getThreadName();

	/**
	 * Returns the log sequence number associated with this <code>LogEntry</code>
	 * object. 
	 * 
	 * @return <code>long</code> containing the sequence number associated with this
	 *         <code>LogEntry</code> object.
	 */
	long getSequenceNumber();
}
