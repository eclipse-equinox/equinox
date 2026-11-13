/*******************************************************************************
 * Copyright (c) 2026 Eclipse contributors and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.service.runnable;

/**
 * Thrown when the framework is unable to find or launch an application to
 * run. This happens, for example, when no application id was specified and no
 * default application is available, or when the specified application id does
 * not correspond to a registered application.
 * <p>
 * Callers that start the framework (e.g. the Equinox launcher) can catch this
 * specific exception to distinguish this situation from other startup
 * failures, for example to keep the framework (and an active OSGi console)
 * running instead of shutting the process down immediately, allowing the
 * situation to be diagnosed interactively.
 * </p>
 * <p>
 * This class is for internal use by the platform-related plug-ins. Clients
 * outside of the base platform should not reference or subclass this class.
 * </p>
 *
 * @since 1.2
 */
public class NoApplicationException extends IllegalStateException {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a new exception with the specified detail message.
	 *
	 * @param message the detail message
	 */
	public NoApplicationException(String message) {
		super(message);
	}

	/**
	 * Constructs a new exception with the specified detail message and cause.
	 *
	 * @param message the detail message
	 * @param cause   the cause
	 */
	public NoApplicationException(String message, Throwable cause) {
		super(message, cause);
	}
}
