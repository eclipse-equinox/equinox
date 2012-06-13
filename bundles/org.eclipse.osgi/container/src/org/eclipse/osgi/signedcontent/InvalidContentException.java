/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.signedcontent;

import java.io.IOException;

/**
 * Indicates that signed content is invalid according to one of the signers.
 * @since 3.4
 * @noextend This class is not intended to be subclassed by clients.
 */
public class InvalidContentException extends IOException {
	private static final long serialVersionUID = -399150159330289387L;
	// TODO may want to add error codes to indicate the reason for the invalid/corruption error.
	private final Throwable cause;

	/**
	 * Constructs an <code>InvalidContentException</code> with the specified detail
	 * message and cause. 
	 *
	 * @param message the exception message
	 * @param cause the cause, may be <code>null</code>
	 */
	public InvalidContentException(String message, Throwable cause) {
		super(message);
		this.cause = cause;
	}

	/**
	 * Returns the cause of this exception or <code>null</code> if no cause
	 * was specified when this exception was created.
	 * 
	 * @return The cause of this exception or <code>null</code> if no cause was created.
	 */
	public Throwable getCause() {
		return cause;
	}

	/**
	 * The cause of this exception can only be set when constructed.
	 * 
	 * @param t Cause of the exception.
	 * @return This object.
	 * @throws java.lang.IllegalStateException This method will always throw an
	 *         <code>IllegalStateException</code> since the cause of this
	 *         exception can only be set when constructed.
	 */
	public Throwable initCause(Throwable t) {
		throw new IllegalStateException();
	}
}
