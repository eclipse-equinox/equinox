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
package org.eclipse.equinox.internal.util.event;

import java.security.AccessController;
import java.security.PrivilegedAction;
import org.eclipse.equinox.internal.util.UtilActivator;

/**
 * Abstract class for asynchronous event dispatching
 *
 * @author Pavlin Dobrev
 * @version 1.0
 */

public abstract class EventThread implements Runnable {

	/**
	 * The last callbacked listener. If the events thread is not responding,
	 * subclasses can take the appropriate actions - remove the listener for
	 * example
	 */
	public Object bad;

	/**
	 * The event queue. This object must be used for synchronization with the
	 * events thread's state
	 */
	protected Queue queue;

	/**
	 * The state of the thread.
	 * <li> bit 0: 0 = started / 1 = stopped;
	 * <li> bit 1: 0 not waiting / 1 = waiting
	 */
	protected int state;

	/**
	 * The time spent in the current callback, or 0 if the thread is not in a
	 * callback
	 */
	protected long time = 0;
	/**
	 * Instancies counter. Subclasses must not modify it.
	 */
	protected int counter = 1;

	/**
	 * The event to be dispatched
	 */
	protected Object element;
	protected String baseName;
	protected String name;
	protected Thread thread;
	protected ThreadGroup group;
	private static PrivilegedActionImpl privilegedAction = null;

	/**
	 * Constructs the first instance of the EventThread
	 * 
	 * @param group
	 *            The ThreadGroup of the thread, or null for the current thread
	 *            group
	 * @param name
	 *            The base name of the thread. The <code> counter  </code> value
	 *            will be added at the end of the string to construct the full
	 *            name.
	 * @param size
	 *            The initial number of elements of the events queue
	 */
	public EventThread(ThreadGroup group, String name, int size) {
		makeThread(this.group = group, this.name = name + '0');
		baseName = name;
		queue = new Queue(size);
		int priority = getThreadPriority();
		if (priority != Thread.NORM_PRIORITY)
			thread.setPriority(priority);
	}

	/**
	 * Constructs the first instance of the EventThread
	 * 
	 * @param group
	 *            The ThreadGroup of the thread, or null for the current thread
	 *            group
	 * @param name
	 *            The base name of the thread. The <code> counter  </code> value
	 *            will be added at the end of the string to construct the full
	 *            name.
	 * @param queue
	 *            The events queue
	 */
	public EventThread(ThreadGroup group, String name, Queue queue) {
		makeThread(this.group = group, this.name = name + '0');
		baseName = name;
		this.queue = queue;
		int priority = getThreadPriority();
		if (priority != Thread.NORM_PRIORITY)
			thread.setPriority(priority);
	}

	/**
	 * Constructs a new EventThread, after the <code> old </code> event thread
	 * has stopped responding
	 * 
	 * @param old
	 *            The previous instance
	 */
	protected EventThread(EventThread old) {
		makeThread(group = old.thread.getThreadGroup(), name = old.baseName + old.counter++);
		baseName = old.baseName;
		counter = old.counter;
		queue = old.queue;
		int priority = getThreadPriority();
		if (priority != Thread.NORM_PRIORITY)
			thread.setPriority(priority);
	}

	public void start() {
		thread.start();
	}

	/**
	 * Adds an event in the event queue. The method must be synchronized
	 * outside, on the <code> queue </code> field.
	 * 
	 * @param event
	 *            The event to add
	 * @param check
	 *            If true, the method will check if the EventThread is still
	 *            responding
	 */
	public void addEvent(Object event, boolean check) {
		try {
			queue.put(event);
		} catch (Throwable t) {
			print(t);
			return;
		}
		if ((state & 2) != 0)
			queue.notify();
		else if (check && checkTime())
			try {
				state |= 1;
				newEventDispatcher(); // must call start
			} catch (Throwable t) {
				print(t);
				state &= 254;
			}
	}

	/**
	 * Processes the event queue. Sets the event to be dispathed in the
	 * <code> element </code> field and calls <cope> processEvent </code>
	 */
	public void run() {
		synchronized (queue) {
			queue.notifyAll();
		}
		while (true) {
			try {
				synchronized (queue) {
					if ((state & 1) != 0)
						return; // closed
					while ((element = queue.get()) == null)
						try {
							state |= 2; // waiting
							queue.wait();
							if ((state & 1) != 0)
								return; // closed
							state &= 253; // not waiting
						} catch (InterruptedException ie) {
						}
				}
				processEvent();

			} catch (Throwable t) {
				print(t);
				try { // fix memory leak
					throw new Exception();
				} catch (Exception _) {
				}
			}
		}
	}

	private void makeThread(ThreadGroup group, String name) {
		try {
			if (privilegedAction == null) {
				privilegedAction = new PrivilegedActionImpl();
			}
			privilegedAction.set(group, this, name);
			thread = (Thread) AccessController.doPrivileged(privilegedAction);
			// thread = new Thread(group, this, name);
			// thread.setDaemon(false);
			// if (!disableContextClassLoader)
			// thread.setContextClassLoader(null);
		} catch (RuntimeException re) {
			throw re;
		} catch (Exception exc) {
			throw new RuntimeException(exc.toString());
		}
	}

	public Thread getThread() {
		return thread;
	}

	public String getName() {
		return name;
	}

	/**
	 * Returns the desired thread priority. Called in the constructors of the
	 * class in order to set the returned value to the thread.
	 * 
	 * @return
	 */
	public abstract int getThreadPriority();

	/**
	 * Performs the actual event delivery. The event is stored in the
	 * <code> element </code> field. The method is supposed to perform the
	 * following for every listener:
	 * <li> synchronized on <code> queue </code> check the state of the thread -
	 * if it si closed - return
	 * <li> set the fields <code> bad and time <code> 
	 * <li> callback 
	 * <li> set bad to null and time to 0
	 */
	public abstract void processEvent();

	/**
	 * Checks if the thread is still active. The fields <code> time </code> and
	 * <code> bad </code> must be used. The method is called from the addEvent
	 * method - thus should be synchronizes on the <code> queue </code> field
	 * outside and additional synchronization is not needed.
	 */
	public abstract boolean checkTime();

	/**
	 * The method must create a new EventThread instance, using
	 * <code> super.EventThread(this) </code> and start it.
	 */
	public abstract void newEventDispatcher();

	/**
	 * Logs the error.
	 * 
	 * @param t
	 */
	public abstract void print(Throwable t);
}

class PrivilegedActionImpl implements PrivilegedAction {
	private ThreadGroup group;
	private Runnable runnable;
	private String name;

	private boolean locked = false;
	private int waiting = 0;

	void set(ThreadGroup group, Runnable runnable, String name) {
		lock();
		this.group = group;
		this.runnable = runnable;
		this.name = name;
	}

	public Object run() {
		ThreadGroup group = this.group;
		Runnable runnable = this.runnable;
		String name = this.name;
		unlock();
		Thread th = new Thread(group, runnable, name);
		if (!UtilActivator.getBoolean("equinox.disableContextClassLoader"))
			th.setContextClassLoader(null);
		th.setDaemon(false);
		return th;
	}

	private synchronized void lock() {
		while (locked)
			try {
				waiting++;
				wait();
				waiting--;
			} catch (Exception exc) {
			}
		locked = true;
	}

	private synchronized void unlock() {
		locked = false;
		group = null;
		runnable = null;
		name = null;
		if (waiting > 0)
			notifyAll();
	}
}
