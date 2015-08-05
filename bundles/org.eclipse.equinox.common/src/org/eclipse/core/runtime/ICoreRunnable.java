/*******************************************************************************
 * Copyright (c) 2015 Google Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.core.runtime;

/**
 * A functional interface for a runnable that can be cancelled and can report progress
 * using the progress monitor passed to the {@link #run(IProgressMonitor)} method.
 * <p>
 * Clients may implement this interface.
 *
 * @since 3.8
 */
public interface ICoreRunnable {
	/**
	 * Executes this runnable.
	 * <p>
	 * The provided monitor can be used to report progress and respond to
	 * cancellation.  If the progress monitor has been canceled, the runnable
	 * should finish its execution at the earliest convenience and throw
	 * a {@link CoreException} with a status of severity {@link IStatus#CANCEL}.
	 * The singleton cancel status {@link Status#CANCEL_STATUS} can be used for
	 * this purpose.  The monitor is only valid for the duration of the invocation
	 * of this method.
	 *
	 * @param monitor the monitor to be used for reporting progress and
	 *     responding to cancellation. The monitor is never {@code null}.
	 *     It is the caller's responsibility to call {@link IProgressMonitor#done()}
	 *     after this method returns or throws an exception.
	 * @exception CoreException if this operation fails.
	 */
	public void run(IProgressMonitor monitor) throws CoreException;
}
