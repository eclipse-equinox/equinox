/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.service.resolver;

/**
 * A runtime exception thrown by a resolver to indicate that a resolver
 * hook threw an unexpected exception and the resolve operation terminated.
 * @since 3.7
 */
public class ResolverHookException extends RuntimeException {
	private static final long serialVersionUID = 5686047743173396286L;

	/**
	 * Constructs a new resolver hook exception.
	 * @param message the message of the exception
	 * @param cause the cause of the exception
	 */
	public ResolverHookException(String message, Throwable cause) {
		super(message, cause);
	}
}
