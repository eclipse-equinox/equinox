/******************************************************************************
 * Copyright (c) 2010, 2013 EclipseSource and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   EclipseSource - initial API and implementation
 *   Gunnar Wagenknecht - added support for generics
 ******************************************************************************/
package org.eclipse.equinox.concurrent.future;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Abstract implementation of {@link IExecutor} and {@link IRunnableExecutor}.
 * @since 1.1
 * 
 */
public abstract class AbstractExecutor implements IRunnableExecutor, IExecutor {

	public void execute(final Runnable runnable) {
		execute(new IProgressRunnable<Object>() {
			public Object run(IProgressMonitor monitor) throws Exception {
				runnable.run();
				return null;
			}
		}, null);
	}

	public abstract <ResultType> IFuture<ResultType> execute(
			IProgressRunnable<? extends ResultType> runnable,
			IProgressMonitor monitor);

	/**
	 * Create an {@link AbstractFuture} instance. Subclasses must override to
	 * define the concrete type of future to return from
	 * {@link #execute(IProgressRunnable, IProgressMonitor)}.
	 * 
	 * @param progressMonitor
	 *            any progress monitor to provide to the future upon
	 *            construction. May be <code>null</code>.
	 * @return the created future
	 */
	protected abstract AbstractFuture<?> createFuture(
			IProgressMonitor progressMonitor);

}
