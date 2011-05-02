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

package org.eclipse.equinox.internal.util.impl.tpt.threadpool;

import java.security.AccessControlContext;
import java.security.AccessController;
import org.eclipse.equinox.internal.util.UtilActivator;
import org.eclipse.equinox.internal.util.impl.tpt.ServiceFactoryImpl;
import org.eclipse.equinox.internal.util.ref.Log;
import org.eclipse.equinox.internal.util.threadpool.ThreadPoolFactory;
import org.eclipse.equinox.internal.util.threadpool.ThreadPoolManager;

/**
 * @author Pavlin Dobrev
 * @version 1.0
 */

public class ThreadPoolFactoryImpl extends ServiceFactoryImpl implements ThreadPoolManager, ThreadPoolFactory {

	public static ThreadPoolManagerImpl threadPool;
	private int limit;
	private int used = 0;
	private Job queue;
	private static int defaultPercent;;

	public ThreadPoolFactoryImpl(String bundleName, Log log) {

		super(bundleName, log);
		threadPool = ThreadPoolManagerImpl.getThreadPool();
		defaultPercent = UtilActivator.getInteger("equinox.util.threadpool.percent", 30);
		limit = (ThreadPoolManagerImpl.tMaximum * defaultPercent) / 100;
		if (limit == 0)
			limit = 1;
		queue = new Job();
	}

	public ThreadPoolFactoryImpl(String bundleName, int size) {
		super(bundleName);
		limit = size;
		if (limit == 0)
			limit = 1;
		queue = new Job();
	}

	public ThreadPoolFactoryImpl(String bundleName) {
		this(bundleName, (ThreadPoolManagerImpl.tMaximum * defaultPercent) / 100);
	}

	public Object getInstance(String bundleName) {
		if (threadPool == null)
			throw new RuntimeException("ServiceFactory is currently off!");
		return new ThreadPoolFactoryImpl(bundleName);
	}

	public static void stopThreadPool() {
		ThreadPoolManagerImpl tmp = threadPool;
		threadPool = null;
		tmp.clear();
	}

	public ThreadPoolManager getThreadPool(int size, boolean sizeIsInPercents) {
		if (threadPool == null)
			throw new RuntimeException("[ThreadPool] ThreadPool is inaccessible");

		if (sizeIsInPercents) {
			size = (ThreadPoolManagerImpl.tMaximum * size) / 100;
		}
		if (size <= 0) {
			size = 1;
		}
		return new ThreadPoolFactoryImpl(bundleName, size);
	}

	public void execute(Runnable job, String name) {
		execute(job, Thread.NORM_PRIORITY, name);
	}

	public void execute0(Runnable job, int priority, String name, AccessControlContext acc) {
		if (job == null || name == null) {
			throw new IllegalArgumentException("the job or the name parameter is/are null");
		}

		if (ServiceFactoryImpl.useNames)
			name = name + bundleName;

		ThreadPoolManagerImpl tmp = threadPool;

		if (tmp != null) {
			synchronized (tmp.getSyncMonitor()) {
				if (used >= limit) {
					if (UtilActivator.LOG_DEBUG) {
						UtilActivator.log.debug("In Bundle Queue: " + name + ", bundle queue size: " + queue.counter, null);
					}
					queue.addJob(job, name, priority, this, acc);

					return;
				}

				used++;
			}

			tmp.execute(job, priority, name, this, acc

			);
		} else
			throw new RuntimeException("[ThreadPool] ThreadPool is inaccessible");
	}

	public void execute(Runnable job, int priority, String name, AccessControlContext acc) {
		execute0(job, priority, name, acc

		);
	}

	public void execute(Runnable job, int priority, String name) {
		execute0(job, priority, name, (Log.security() ? AccessController.getContext() : null));
	}

	public Executor getExecutor() {
		ThreadPoolManagerImpl tmp = threadPool;
		if (tmp != null) {
			synchronized (tmp.getSyncMonitor()) {
				if (used < limit) {
					Executor ex = tmp.getExecutor();
					if (ex != null)
						used++;
					return ex;
				}
			}
		}
		return null;
	}

	void finished() {
		Job job = queue.getJob();

		if (job != null) {
			if (UtilActivator.LOG_DEBUG) {
				UtilActivator.log.debug("To threadpool queue: " + job.name + ", queue size: " + threadPool.waiting.counter, null);
			}
			threadPool.waiting.addJob(job);
		} else {
			used--;
		}
	}

	public void reset() {
		if (threadPool != null) {
			threadPool.reset();
		}
	}
}
