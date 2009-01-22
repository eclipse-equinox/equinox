/******************************************************************************
* Copyright (c) 2009 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.concurrent.future;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * <p>
 * An executor that implements running the given{@link IProgressRunnable}s
 * via a new {@link Thread}.
 * </p>
 * <p>
 * The {@link #execute(IProgressRunnable, IProgressMonitor)} method on this class will
 * create a new Thread (with name provided as result of {@link #createThreadName(IProgressRunnable)},
 * that will run the {@link IProgressRunnable} and set the result in the future returned
 * from {@link #execute(IProgressRunnable, IProgressMonitor)}.
 * <p>
 * Subclasses may extend the behavior of this ThreadsExecutor.
 * </p>
 */
public class ThreadsExecutor extends AbstractExecutor {

	public ThreadsExecutor() {
		// nothing
	}

	protected String createThreadName(IProgressRunnable runnable) {
		return "ThreadsExecutor(" + runnable.toString() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Create a runnable given an {@link IProgressRunnable} and an {@link ISafeProgressRunner} to
	 * run the runnable.
	 * @param runner the safe progress runner to run the runnable
	 * @param progressRunnable the runnable to run.
	 * @return Runnable that when run will use the safe progress runner to run the progressRunnable
	 */
	protected Runnable createRunnable(final ISafeProgressRunner runner, final IProgressRunnable progressRunnable) {
		return new Runnable() {
			public void run() {
				runner.runWithProgress(progressRunnable);
			}
		};
	}

	/**
	 * Configure the given thread prior to starting it.  Subclasses may override as
	 * appropriate to configure the given thread appropriately.  The default implementation
	 * calls {@link Thread#setDaemon(boolean)}.
	 * 
	 * @param thread the thread to configure
	 */
	protected void configureThreadForExecution(Thread thread) {
		// By default, we'll make the thread a daemon thread
		thread.setDaemon(true);
	}

	/**
	 * Create an {@link AbstractFuture} with the given IProgressMonitor.
	 * @param monitor a progress monitor to associate with the future.  May be <code>null</code>.
	 */
	protected AbstractFuture createFuture(IProgressMonitor monitor) {
		return new SingleOperationFuture(monitor);
	}

	public synchronized IFuture execute(IProgressRunnable runnable, IProgressMonitor monitor) throws IllegalThreadStateException {
		Assert.isNotNull(runnable);
		// Now create future
		AbstractFuture sof = createFuture(monitor);
		// Create the thread for this operation
		Thread thread = new Thread(createRunnable(sof, runnable), createThreadName(runnable));
		configureThreadForExecution(thread);
		// start thread
		thread.start();
		return sof;
	}

}
