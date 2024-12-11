/*******************************************************************************
 * Copyright (c) 2015 Google Inc and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.core.runtime;

/**
 * A functional interface for a runnable that can be cancelled and can report
 * progress using the progress monitor passed to the
 * {@link #run(IProgressMonitor)} method.
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
	 * cancellation. If the progress monitor has been canceled, the runnable should
	 * finish its execution at the earliest convenience and throw an
	 * {@link OperationCanceledException}. A {@link CoreException} with a status of
	 * severity {@link IStatus#CANCEL} has the same effect as an
	 * {@link OperationCanceledException}.
	 *
	 * @param monitor a progress monitor, or {@code null} if progress reporting and
	 *                cancellation are not desired. The monitor is only valid for
	 *                the duration of the invocation of this method. The receiver is
	 *                not responsible for calling {@link IProgressMonitor#done()} on
	 *                the given monitor, and the caller must not rely on
	 *                {@link IProgressMonitor#done()} having been called by the
	 *                receiver.
	 * @exception CoreException              if this operation fails
	 * @exception OperationCanceledException if this operation is canceled
	 */
	public void run(IProgressMonitor monitor) throws CoreException;
}
