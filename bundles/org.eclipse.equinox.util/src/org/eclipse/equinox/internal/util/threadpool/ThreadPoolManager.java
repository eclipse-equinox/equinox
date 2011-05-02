/*******************************************************************************
 * Copyright (c) 1997, 2008 by ProSyst Software GmbH
 * http://www.prosyst.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.util.threadpool;

/**
 * The ThreadPoolManager is responsible for the management of a thread pool,
 * whose purpose is to provide created and started threads to clients, using
 * multiple but short-lived threads.
 * 
 * The ThreadPoolManager's main task is to accept Runnable objects, to pass them
 * to threads from the pool, and after the job is finished - to return back the
 * threads in pool.
 * 
 * @author Pavlin Dobrev
 * @version 1.0
 */

public interface ThreadPoolManager {

	/**
	 * Executes the job, by passing it to an idle thread process. If no idle
	 * threads are available, the job is moved to the pool of waiting jobs, and
	 * will be executed later.
	 * 
	 * @param job
	 *            job to be executed
	 * @param threadName
	 *            name of job; the name will be assigned to the thread, in which
	 *            the job will be processed
	 * @exception IllegalArgumentException
	 *                If any of the arguments is null
	 */
	public void execute(Runnable job, String threadName) throws IllegalArgumentException;

	/**
	 * Executes the job, by passing it to an idle thread process. If no idle
	 * threads are available, the job is moved to the pool of waiting jobs, and
	 * will be executed later.
	 * 
	 * @param job
	 *            Runnable job to be executed
	 * @param priority 
	 *            the priority of the job
	 * @param threadName
	 *            name of job; the name will be assigned to the thread, in which
	 *            the job will be processed
	 * @exception IllegalArgumentException
	 *                If any of the arguments is null
	 */
	public void execute(Runnable job, int priority, String threadName) throws IllegalArgumentException;

	/**
	 * All idle threads exit. New threads will be created when it is necessary.
	 */
	public void reset();
}
