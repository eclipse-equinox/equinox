/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
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
package org.eclipse.core.runtime;

/**
 * An unchecked exception indicating that an attempt to access an extension
 * registry object that is no longer valid.
 * <p>
 * This exception is thrown by methods on extension registry objects. It is not
 * intended to be instantiated or subclassed by clients.
 * </p>
 * <p>
 * This class can be used without OSGi running.
 * </p>
 * 
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noextend This class is not intended to be subclassed by clients.
 */
public class InvalidRegistryObjectException extends RuntimeException {
	/*
	 * Declare a stable serialVersionUID.
	 */
	private static final long serialVersionUID = 1L;

	private static final String MESSAGE = "Invalid registry object"; //$NON-NLS-1$

	/**
	 * Creates a new exception instance with null as its detail message.
	 */
	public InvalidRegistryObjectException() {
		super(MESSAGE);
	}
}
