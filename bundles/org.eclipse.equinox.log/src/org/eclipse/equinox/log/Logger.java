/*******************************************************************************
 * Copyright (c) 2006, 2008 Cognos Incorporated, IBM Corporation and others
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.equinox.log;

import org.osgi.framework.ServiceReference;

public interface Logger {
	public void log(int level, String message);

	public void log(int level, String message, Throwable exception);

	public void log(ServiceReference sr, int level, String message);

	public void log(ServiceReference sr, int level, String message, Throwable exception);

	// new methods
	public void log(Object context, int level, String message);

	public void log(Object context, int level, String message, Throwable exception);

	public boolean isLoggable(int level);

	public String getName();
}
