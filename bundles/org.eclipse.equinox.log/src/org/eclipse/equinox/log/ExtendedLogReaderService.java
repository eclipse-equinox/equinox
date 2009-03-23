/*******************************************************************************
 * Copyright (c) 2006, 2008 Cognos Incorporated, IBM Corporation and others
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.equinox.log;

import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;

public interface ExtendedLogReaderService extends LogReaderService {

	public void addLogListener(LogListener listener, LogFilter filter);
}
