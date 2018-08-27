/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package org.eclipse.equinox.log;

import org.osgi.service.log.LogListener;

/**
 * Marker interface to denotes a log listener that should be called on the logging thread
 * @see LogListener
 * @since 3.7
 */
public interface SynchronousLogListener extends LogListener {
	//
}
