/*******************************************************************************
 * Copyright (c) 2006, 2008 Cognos Incorporated, IBM Corporation and others
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.equinox.log;

import org.osgi.framework.Bundle;
import org.osgi.service.log.LogService;

public interface ExtendedLogService extends LogService, Logger {

	public Logger getLogger(String loggerName);

	/**
	* @throws SecurityException if the caller does not have <code>LogPermission[*,LOG]</code>.
	*/
	public Logger getLogger(Bundle bundle, String loggerName);
}
