/*******************************************************************************
 * Copyright (c) 2010, 2013 Composent, Inc. and others.
 * 
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.concurrent.future;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * 
 * A future that allows for an {@link IProgressRunnable} (with optional
 * {@link IProgressMonitor}) to be executed via an {@link IExecutor}.
 * 
 * @since 1.1
 */
public interface IListenableFuture<ResultType> extends IFuture<ResultType> {

	/**
	 * Add an {@link IProgressRunnable} that will be called back/run once the
	 * asynchronous execution is complete. The {@link IProgressRunnable} must be
	 * non-<code>null</code> and the {@link IExecutor} must also be non-
	 * <code>null</code>. The given progressMonitor may be <code>null</code>.
	 * <p>
	 * If the future has already completed by the time this method is called (i.e.
	 * {@link #isDone()} returns <code>true</code>, the progressRunnable will be
	 * executed immediately by the given executor.
	 * 
	 * @param executor         the {@link IExecutor} to use to execute the given
	 *                         {@link IProgressRunnable}. Must not be
	 *                         <code>null</code>.
	 * @param progressRunnable the {@link IProgressRunnable} that will be executed
	 *                         when this future is complete. Must not be
	 *                         <code>null</code>.
	 * @param monitor          an optional progress monitor to be passed to the
	 *                         progressRunnable when executed. May be
	 *                         <code>null</code>.
	 */
	public void addListener(IExecutor executor, IProgressRunnable<ResultType> progressRunnable,
			IProgressMonitor monitor);

}
