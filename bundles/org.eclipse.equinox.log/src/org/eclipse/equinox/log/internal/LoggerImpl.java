/*******************************************************************************
 * Copyright (c) 2006, 2009 Cognos Incorporated, IBM Corporation and others
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.equinox.log.internal;

import org.eclipse.equinox.log.Logger;
import org.osgi.framework.ServiceReference;

public class LoggerImpl implements Logger {

	private final ExtendedLogServiceImpl logServiceImpl;
	private final String name;

	public LoggerImpl(ExtendedLogServiceImpl logServiceImpl, String name) {
		this.logServiceImpl = logServiceImpl;
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public boolean isLoggable(int level) {
		return logServiceImpl.isLoggable(name, level);
	}

	public void log(int level, String message) {
		log(null, level, message, null);
	}

	public void log(int level, String message, Throwable exception) {
		log(null, level, message, exception);
	}

	public void log(ServiceReference sr, int level, String message) {
		log(sr, level, message, null);
	}

	public void log(ServiceReference sr, int level, String message, Throwable exception) {
		logServiceImpl.log(name, sr, level, message, exception);
	}

	public void log(Object context, int level, String message) {
		log(context, level, message, null);
	}

	public void log(Object context, int level, String message, Throwable exception) {
		logServiceImpl.log(name, context, level, message, exception);
	}
}
