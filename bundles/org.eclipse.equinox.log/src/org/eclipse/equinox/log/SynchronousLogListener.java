/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.equinox.log;

import org.osgi.service.log.LogListener;

/**
 * Marker interface to denotes a log listener that should be called on the logging thread
 * @see LogListener
 */
public interface SynchronousLogListener extends LogListener {
	//
}
