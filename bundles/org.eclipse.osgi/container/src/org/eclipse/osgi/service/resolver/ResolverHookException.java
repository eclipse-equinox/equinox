/*******************************************************************************
 * Copyright (c) 2011, 2012 IBM Corporation and others.
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
package org.eclipse.osgi.service.resolver;

/**
 * A runtime exception thrown by a resolver to indicate that a resolver hook
 * threw an unexpected exception and the resolve operation terminated.
 * 
 * @since 3.7
 */
public class ResolverHookException extends RuntimeException {
	private static final long serialVersionUID = 5686047743173396286L;

	/**
	 * Constructs a new resolver hook exception.
	 * 
	 * @param message the message of the exception
	 * @param cause   the cause of the exception
	 */
	public ResolverHookException(String message, Throwable cause) {
		super(message, cause);
	}
}
