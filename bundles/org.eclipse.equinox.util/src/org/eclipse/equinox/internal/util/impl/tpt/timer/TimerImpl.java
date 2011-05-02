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

package org.eclipse.equinox.internal.util.impl.tpt.timer;

import java.security.*;
import java.util.Hashtable;
import org.eclipse.equinox.internal.util.UtilActivator;
import org.eclipse.equinox.internal.util.impl.tpt.ServiceFactoryImpl;
import org.eclipse.equinox.internal.util.impl.tpt.threadpool.Executor;
import org.eclipse.equinox.internal.util.impl.tpt.threadpool.ThreadPoolFactoryImpl;
import org.eclipse.equinox.internal.util.pool.ObjectPool;
import org.eclipse.equinox.internal.util.ref.Log;
import org.eclipse.equinox.internal.util.timer.Timer;
import org.eclipse.equinox.internal.util.timer.TimerListener;

/**
 * @author Pavlin Dobrev
 * @version 1.0
 */

public class TimerImpl implements Runnable {

	static Hashtable nodes;
	static ObjectPool nodePool;

	static ThreadPoolFactoryImpl threadPool;

	private TimerQueue queue;
	private boolean terminated = false;
	private Object sync = new Object();
	private Thread th;

	public TimerImpl(ThreadPoolFactoryImpl threadPool) {
		nodePool = new ObjectPool(new TimerQueueNode(), 2, 4, 2);
		TimerImpl.threadPool = threadPool;
		nodes = new Hashtable(10);
		queue = new TimerQueue();
		try {
			th = ((ServiceFactoryImpl.privileged()) ? getOne() : new Thread(this, "[Timer] - Main Queue Handler"));
			try {
				String str = UtilActivator.bc.getProperty("equinox.timer.priority");
				if (str != null)
					th.setPriority(Integer.parseInt(str));
			} catch (Throwable ignored) {
			}
			th.start();
		} catch (Exception e) {
			throw new RuntimeException("Can not start Timer thread!" + e.toString());
		}
	}

	public void run() {
		TimerQueueNode n = null;
		while (!terminated) {
			synchronized (sync) {
				if (n == null && queue.isEmpty()) {
					try {
						sync.wait();
					} catch (Exception e) {
					}
					// todo check if isEmpty is necessary
					if (queue.isEmpty() || terminated) {
						continue;
					}
				}
			}
			synchronized (queue) {
				n = queue.getMin();
				while (n != null && !n.enabled) {
					queue.removeMin();
					n.returnInPool();
					n = queue.getMin();
				}
				if (n == null)
					continue;
				long current = System.currentTimeMillis();
				if (n.runOn <= current) {
					switch (n.type) {
						case (Timer.ONE_SHOT_TIMER) : {
							threadPool.execute0(n, n.priority, n.getEName(), n.acc);
							queue.removeMin();
							nodes.remove(n);
							break;
						}
						case (Timer.ONE_SHOT_TIMER_NO_DELAY) : {
							Executor e = threadPool.getExecutor();
							if (e != null) {
								e.setPriorityI(n.priority);
								e.setRunnable(n, n.getEName(), threadPool, n.acc);
							} else {
								Thread th = new Thread(n, n.getEName());
								th.setPriority(n.priority);
								th.start();
							}
							queue.removeMin();
							nodes.remove(n);
							break;
						}
						case (Timer.PERIODICAL_TIMER) : {
							threadPool.execute0(n, n.priority, n.getEName(), n.acc);
							n.runOn += n.period;
							if (n.runOn < current) { // time changed
								n.runOn = current + n.period;
							}
							queue.rescheduleMin(n.runOn);
							break;
						}
						case (Timer.PERIODICAL_TIMER_NO_DELAY) : {
							Executor e = threadPool.getExecutor();
							if (e != null) {
								e.setPriorityI(n.priority);
								e.setRunnable(n, n.getEName(), threadPool, n.acc);
							} else {
								Thread th = new Thread(n, n.getEName());
								th.setPriority(n.priority);
								th.start();
							}
							if (n.runOn < current) { // time changed
								n.runOn = current + n.period;
							}
							queue.rescheduleMin(n.runOn);
						}
					}
					continue;
				}
			}
			synchronized (sync) {
				long tmpWait;
				if (n != null && (tmpWait = n.runOn - System.currentTimeMillis()) > 0) {
					try {
						sync.wait(tmpWait);
					} catch (Exception e) {
						e.printStackTrace();
					}
					TimerQueueNode tmp = null;
					synchronized (queue) {
						tmp = queue.getMin();
						if (tmp != n) {
							n = tmp;
						}
					}
					/* what's this for - we'll continue in any case */
					continue;
				}
			}
		}// while (!terminated)
		nodePool.clear();
		nodePool = null;
		nodes.clear();
		nodes = null;
		queue = null;
	}

	public void terminate() {
		terminated = true;
		synchronized (sync) {
			sync.notify();
		}
		try {
			th.join();
		} catch (InterruptedException ie) {
		}
	}

	private void put(TimerListener listener, int priority, int timerType, long periodMilis, int event, String name, AccessControlContext acc) {
		if (terminated || nodePool == null) {
			throw new RuntimeException("This Instance is a ZOMBIE!!!" + terminated + " " + nodePool);
		}

		TimerQueueNode n = (TimerQueueNode) nodePool.getObject();
		n.setEvent(listener, priority, timerType, System.currentTimeMillis() + periodMilis, periodMilis, event, name, acc);
		TimerQueueNode tmp = (TimerQueueNode) nodes.remove(n);
		if (tmp != null) {
			synchronized (queue) {
				queue.removeTimerNode(tmp);
			}
			tmp.returnInPool();
		}
		nodes.put(n, n);
		TimerQueueNode nx;
		synchronized (queue) {
			queue.add(n);
			nx = queue.getMin();
		}
		if (nx == n) {
			synchronized (sync) {
				sync.notifyAll();
			}
		}
	}

	void addNotifyListener(TimerListener listener, int priority, int timerType, long periodMilis, int event, String name) {
		if (timerType < Timer.ONE_SHOT_TIMER || timerType > Timer.PERIODICAL_TIMER_NO_DELAY) {
			throw new IllegalArgumentException("Invalid Timer Type");
		}
		if (listener != null) {
			if (priority >= Thread.MIN_PRIORITY && priority <= Thread.MAX_PRIORITY) {
				if (periodMilis > 0) {
					AccessControlContext acc = Log.security() ? AccessController.getContext() : null;
					put(listener, priority, timerType, periodMilis, event, name, acc);
				} else {
					throw new IllegalArgumentException("Time period must be positive!");
				}
			} else {
				throw new IllegalArgumentException("Priority must be between Thread.MIN_PRIORITY and Thread.MAX_PRIORITY!");
			}
		} else {
			throw new IllegalArgumentException("The timer listener is null");
		}
	}

	public void removeListener(TimerListener listener, int event) {
		TimerQueueNode rmTmp = (TimerQueueNode) nodePool.getObject();
		rmTmp.setEvent(listener, 0, 0, 0, 0, event, null, null);
		TimerQueueNode old = (TimerQueueNode) nodes.remove(rmTmp);
		if (old != null) {
			synchronized (queue) {
				queue.removeTimerNode(old);
			}
			old.returnInPool();
		}
		rmTmp.returnInPool();
	}

	private class PrivilegedActionImpl implements PrivilegedAction {
		private Runnable runnable = null;
		private boolean locked = false;
		private boolean waiting = false;

		PrivilegedActionImpl() {
		}

		public synchronized void set(Runnable runnable) {
			while (locked) {
				waiting = true;
				try {
					wait();
				} catch (Exception _) {
				}
				waiting = false;
			}
			locked = true;
			this.runnable = runnable;
		}

		public Object run() {
			Runnable runnableLocal = null;
			synchronized (this) {
				runnableLocal = this.runnable;
				this.runnable = null;
				locked = false;
				if (waiting)
					notifyAll();
			}
			return new Thread(runnableLocal, "[Timer] - Main Queue Handler");
		}
	}

	public Thread getOne() throws Exception {
		if (action == null)
			action = new PrivilegedActionImpl();
		action.set(this);
		return (Thread) AccessController.doPrivileged(action);
	}

	PrivilegedActionImpl action = null;
}
