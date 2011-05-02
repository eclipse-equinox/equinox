/*******************************************************************************
 * Copyright (c) 1997, 2009 by ProSyst Software GmbH
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

import java.security.*;
import org.eclipse.equinox.internal.util.UtilActivator;
import org.eclipse.equinox.internal.util.impl.tpt.ServiceFactoryImpl;
import org.eclipse.equinox.internal.util.pool.ObjectPool;
import org.eclipse.equinox.internal.util.timer.TimerListener;

/**
 * @author Pavlin Dobrev
 * @version 1.0
 */

public class ThreadPoolManagerImpl extends ObjectPool implements TimerListener, PrivilegedAction {

	static ThreadPoolManagerImpl threadPool = null;

	static ObjectPool jobPool;

	private int used = 0;
	static int tMaximum = 0;

	Job waiting = new Job();

	private static String pAutoMaximum = "equinox.util.threadpool.autoMaximum";

	private static String pMin = "equinox.util.threadpool.minThreads";

	private static String pMax = "equinox.util.threadpool.maxThreads";

	private static String pIgnoreMax = "equinox.util.threadpool.ignoreMaximum";

	private static int defMin = 1;

	private static int defMax = 48;

	private static int MAX_WAITING = 20;

	private static float MAX_OVERLOAD = 0.10F;

	private static boolean ignoreMax;
	private static boolean autoMax;

	private ThreadPoolManagerImpl(int i, int j, int m) {
		super((Class) null, i, j, m);
		tMaximum = i * j;
		ignoreMax = UtilActivator.getBoolean(pIgnoreMax);
		autoMax = UtilActivator.getBoolean(pAutoMaximum);
		jobPool = new ObjectPool(waiting, 5, 8, 4);
	}

	public static ThreadPoolManagerImpl getThreadPool() {
		if (threadPool == null) {
			int intSize = UtilActivator.getInteger(pMin, defMin);
			int minFill = intSize;
			int factor = UtilActivator.getInteger(pMax, defMax);
			intSize = intSize < 2 ? 2 : intSize;
			if (intSize > factor) {
				factor = (int) (intSize * 1.5 + 0.5);
			}
			threadPool = new ThreadPoolManagerImpl(intSize, (factor / intSize), minFill);
		}
		return threadPool;
	}

	public void clear() {
		shrink(-1);
		threadPool = null;
	}

	public Object getInstance() throws Exception {
		Object result = (ServiceFactoryImpl.privileged()) ? AccessController.doPrivileged(this) : new Executor();

		return result;
	}

	public Object run() {
		return new Executor();
	}

	public Object getObject() {
		try {
			return super.getObject();
		} catch (Throwable tw) {
			if (ServiceFactoryImpl.log != null) {
				ServiceFactoryImpl.log.error("Unable to create more threads!\r\nActive Thread Pool tasks: " + used, tw);
			}
		}
		return null;
	}

	public Executor getExecutor() {
		synchronized (getSyncMonitor()) {
			if (used < tMaximum || ignoreMax) {
				Executor e = (Executor) getObject();
				if (e != null) {
					used++;
					return e;
				}
			}
		}
		return null;
	}

	protected void shrink(int count) {
		synchronized (getSyncMonitor()) {
			dontExtend = true;
			int x, y;
			for (; nextFree > count; nextFree--) {
				x = nextFree / factor;
				y = nextFree % factor;
				Executor e = (Executor) buff[x][y];
				buff[x][y] = null;
				e.terminate();
			}
		}
	}

	public void shrink() {
		shrink(minimumFill - 1);
		dontExtend = false;
	}

	public boolean releaseObject(Object obj) {
		Job tmp = null;
		Executor x = (Executor) obj;

		synchronized (getSyncMonitor()) {
			x.factory.finished();

			if (used <= tMaximum || ignoreMax) {
				tmp = waiting.getJob();
			}
		}

		if (tmp == null) {
			used--;
			x.clear();
			x.setPriorityI(Thread.NORM_PRIORITY);
			return super.releaseObject(obj);
		}
		if (UtilActivator.LOG_DEBUG) {
			UtilActivator.log.debug(0x0100, 10005, tmp.name, null, false);
		}
		x.setPriorityI(tmp.priority);

		x.setRunnable(tmp.run, tmp.name, tmp.factory, tmp.acc);
		tmp.fullClear();
		jobPool.releaseObject(tmp);
		return true;
	}

	public void timer(int event) {
		int count = 0;
		int all = 0;
		synchronized (getSyncMonitor()) {
			for (int i = 0; i < buff.length; i++) {
				if (buff[i] != null) {
					for (int j = 0; j < buff[i].length; j++) {
						Executor e = (Executor) buff[i][j];
						if (e != null) {
							all++;
							if (!e.accessed)
								count++;
							else
								e.accessed = false;
						}
					}
				}
			}
			if (count > 0 && all > minimumFill - 1 && all > count) {
				/*
				 * keep in mind current thread - shrinking one more, since the
				 * current thread will be back in pool
				 */
				if (count > minimumFill)
					shrink(count - 2);
				else
					shrink(minimumFill - 2);
				dontExtend = false;
			}
		}
	}

	public void execute(Runnable job, int priority, String name, ThreadPoolFactoryImpl factory, AccessControlContext acc) {
		Executor ex = null;
		synchronized (getSyncMonitor()) {
			if (used < tMaximum || ignoreMax) {
				ex = (Executor) getObject();
			}
			if (ex != null) {
				used++;
			} else {
				addInTasksQueue(job, name, priority, factory, acc);

				return;
			}
		}

		ex.setPriorityI(priority);
		ex.setRunnable(job, name, factory, acc);
	}

	private void addInTasksQueue(Runnable job, String name, int priority, ThreadPoolFactoryImpl factory, AccessControlContext acc) {

		waiting.addJob(job, name, priority, factory, acc);
		if (UtilActivator.LOG_DEBUG) {
			UtilActivator.log.debug("In Threadpool Queue: " + name + ", queue size:" + waiting.counter, null);
		}
		if (autoMax && waiting.counter > MAX_WAITING) {
			Executor ex = (Executor) getObject();
			if (ex != null) {
				tMaximum += MAX_WAITING;
				MAX_WAITING += (int) (MAX_WAITING * MAX_OVERLOAD);
				for (Job j = waiting.getJob(); j != null; j = waiting.getJob()) {
					if (ex == null) {
						ex = (Executor) getObject();
					}
					if (ex != null) {
						used++;
						ex.setPriorityI(j.priority);
						ex.setRunnable(j.run, j.name, factory, acc);
						ex = null;
					} else {
						waiting.addJob(j.run, j.name, j.priority, j.factory, acc);
						break;
					}
				}
			}
		}
	}

	public void reset() {
		shrink(-1);
		dontExtend = false;
	}

	public Object getSyncMonitor() {
		return buff;
	}

}
