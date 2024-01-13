/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package org.eclipse.equinox.log;

import org.osgi.framework.Bundle;
import org.osgi.service.log.LogEntry;

/**
 * A <code>LogFilter</code> is used to pre-filter log requests before sending events to a <code>LogListener</code>.
 * @ThreadSafe
 * @see ExtendedLogReaderService#addLogListener(org.osgi.service.log.LogListener, LogFilter)
 * @since 3.7
 */
public interface LogFilter {

	/**
	 * @param bundle     The logging bundle
	 * @param loggerName The name of the <code>Logger</code>
	 * @param logLevel   The log level or severity
	 * @see LogEntry
	 * @see Logger
	 * @see ExtendedLogReaderService#addLogListener(org.osgi.service.log.LogListener,
	 *      LogFilter)
	 */
	boolean isLoggable(Bundle bundle, String loggerName, int logLevel);
}
