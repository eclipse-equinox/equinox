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

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * <p>
 * Contract for the actual execution of {@link IProgressRunnable}s.  Instances of
 * this interface must be able to provide resources to eventually execute a given
 * {@link IProgressRunnable}, upon calling {@link #execute(IProgressRunnable, IProgressMonitor)}. 
 * </p>
 * <p>
 * Note that implementations may decide what/how to execute the given IProgressRunnable (i.e.
 * via a {@link Thread}, or a Job, or a ThreadPool or some other invocation mechanism.  
 * But the intended contract of {@link #execute(IProgressRunnable, IProgressMonitor)} is that
 * the {@link IProgressRunnable#run(IProgressMonitor)} method will be invoked by
 * this executor in a timely manner <b>without</b> blocking.
 * </p>
 * 
 * @see IProgressRunnable
 * @see IFuture
 * @see #execute(IProgressRunnable, IProgressMonitor)
 */
public interface IExecutor {

	/**
	 * <p>
	 * Execute the given {@link IProgressRunnable} (i.e. call {@link IProgressRunnable#run(IProgressMonitor)}.
	 * Will return a non-<code>null</code> instance of {@link IFuture} that allows
	 * clients to inspect the state of the execution and retrieve any results via {@link IFuture#get()}
	 * or {@link IFuture#get(long)}.
	 * </p>
	 * <p>
	 * Note that implementers may decide whether to invoke {@link IProgressRunnable#run(IProgressMonitor)} 
	 * asynchronously or synchronously, but since IProgressRunnables are frequently going to be longer-running
	 * operations, implementers should proceed carefully before implementing with synchronous (blocking)
	 * invocation.  Implementers should typically implement via some non-blocking asynchronous invocation 
	 * mechanism, e.g. Threads, Jobs, ThreadPools etc.
	 * </p>
	 * @param runnable the {@link IProgressRunnable} to invoke.  Must not be <code>null</code>.
	 * @param monitor any {@link IProgressMonitor} to be passed to the runnable.  May be <code>null</code>.
	 * @return {@link IFuture} to allow for inspection of the state of the computation by clients,
	 * as well as access to any return values of {@link IProgressRunnable#run(IProgressMonitor)}.  Will not
	 * be <code>null</code>.
	 */
	IFuture execute(IProgressRunnable runnable, IProgressMonitor monitor);

}
