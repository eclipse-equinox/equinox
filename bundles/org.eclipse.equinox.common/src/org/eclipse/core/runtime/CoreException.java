/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * A checked exception representing a failure.
 * <p>
 * Core exceptions contain a status object describing the 
 * cause of the exception.
 * </p><p>
 * This class can be used without OSGi running.
 * </p>
 * @see IStatus
 */
public class CoreException extends Exception {

	/**
	 * All serializable objects should have a stable serialVersionUID
	 */
	private static final long serialVersionUID = 1L;

	/** Status object. */
	private IStatus status;

	/**
	 * Creates a new exception with the given status object.  The message
	 * of the given status is used as the exception message.
	 *
	 * @param status the status object to be associated with this exception
	 */
	public CoreException(IStatus status) {
		super(status.getMessage());
		this.status = status;
	}

	/**
	 * Returns the status object for this exception.
	 * <p>
	 *   <b>IMPORTANT:</b><br>
	 *   The result must NOT be used to log a <code>CoreException</code>
	 *   (e.g., using <code>yourPlugin.getLog().log(status);</code>),
	 *   since that code pattern hides the original stacktrace.
	 *   Instead, create a new {@link Status} with your plug-in ID and
	 *   this <code>CoreException</code>, and log that new status.
	 * </p>
	 *
	 * @return a status object
	 */
	public final IStatus getStatus() {
		return status;
	}

	/**
	 * Prints a stack trace out for the exception, and
	 * any nested exception that it may have embedded in
	 * its Status object.
	 */
	public void printStackTrace() {
		printStackTrace(System.err);
	}

	/**
	 * Prints a stack trace out for the exception, and
	 * any nested exception that it may have embedded in
	 * its Status object.
	 * 
	 * @param output the stream to write to
	 */
	public void printStackTrace(PrintStream output) {
		synchronized (output) {
			super.printStackTrace(output);
			if (status.getException() != null) {
				output.print(getClass().getName() + "[" + status.getCode() + "]: "); //$NON-NLS-1$ //$NON-NLS-2$
				status.getException().printStackTrace(output);
			}
		}
	}

	/**
	 * Prints a stack trace out for the exception, and
	 * any nested exception that it may have embedded in
	 * its Status object.
	 * 
	 * @param output the stream to write to
	 */
	public void printStackTrace(PrintWriter output) {
		synchronized (output) {
			super.printStackTrace(output);
			if (status.getException() != null) {
				output.print(getClass().getName() + "[" + status.getCode() + "]: "); //$NON-NLS-1$ //$NON-NLS-2$
				status.getException().printStackTrace(output);
			}
		}
	}

}
