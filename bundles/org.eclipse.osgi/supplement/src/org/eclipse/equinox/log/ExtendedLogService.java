/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.equinox.log;

import org.osgi.framework.Bundle;
import org.osgi.service.log.LogService;

/**
 * Extends the OSGi <code>Log Service</code> to support the use of named loggers that provide
 * some additional context when logging.
 * @ThreadSafe
 * @see Logger
 * @since 3.7
 */
public interface ExtendedLogService extends LogService, Logger {

	/**
	 * Returns the <code>Logger<code> object associated with this logger name for the bundle that retrieved this log service.
	 * If loggerName is null the default <code>Logger</code> for this bundle is returned. 
	 * 
	 * @param loggerName The logger name. 
	 * @return <code>Logger</code> associated with the logger name.
	 */
	public Logger getLogger(String loggerName);

	/**
	 * Returns the logger associated with this logger name and bundle.
	 * 
	 * @param loggerName The logger name. 
	 * @param bundle The bundles associated with this logger. If null the bundle that retrieved this log service is used.
	 * @return <code>Logger</code> associated with the logger name.
	* @throws SecurityException if the caller does not have <code>LogPermission[*,LOG]</code>.
	*/
	public Logger getLogger(Bundle bundle, String loggerName);
}
