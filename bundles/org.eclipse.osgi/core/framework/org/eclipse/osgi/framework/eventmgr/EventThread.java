/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.eventmgr;

/**
 * This package private class is used for asynchronously dispatching events.
 */

class EventThread extends Thread {
	/**
	 * Queued is a nested top-level (non-member) class. This class
	 * represents the items which are placed on the asynch dispatch queue.
	 * This class is private.
	 */
	private static class Queued {
		/** listener list for this event */
		private final ListElement[] listeners;
		/** dispatcher of this event */
		private final EventDispatcher dispatcher;
		/** action for this event */
		private final int action;
		/** object for this event */
		private final Object object;
		/** next item in event queue */
		Queued next;

		/**
		 * Constructor for event queue item
		 *
		 * @param l Listener list for this event
		 * @param d Dispatcher for this event
		 * @param a Action for this event
		 * @param e Object for this event
		 */
		Queued(ListElement[] l, EventDispatcher d, int a, Object o) {
			listeners = l;
			dispatcher = d;
			action = a;
			object = o;
			next = null;
		}

		/**
		 * This method will dispatch this event queue item to its listeners
		 */
		void dispatchEvent() {
			EventManager.dispatchEvent(listeners, dispatcher, action, object);
		}
	}

	/** item at the head of the event queue */
	private Queued head;
	/** item at the tail of the event queue */
	private Queued tail;
	/** if false the thread must terminate */
	private volatile boolean running;

	/**
	 * Constructor for the event thread. 
	 * @param threadName Name of the EventThread 
	 */
	EventThread(String threadName) {
		super(threadName);
		init();
	}

	/**
	 * Constructor for the event thread.
	 */
	EventThread() {
		super();
		init();
	}

	void init() {
		running = true;
		head = null;
		tail = null;

		setDaemon(true); /* Mark thread as daemon thread */
	}

	/**
	 * Stop thread.
	 */
	void close() {
		running = false;
		interrupt();
	}

	/**
	 * This method pulls events from
	 * the queue and dispatches them.
	 */
	public void run() {
		while (running) {
			try {
				getNextEvent().dispatchEvent();
			} catch (Throwable t) {
			}
		}
	}

	/**
	 * This methods takes the input parameters and creates an Queued
	 * and queues it.
	 * The thread is notified.
	 *
	 * @param l Listener list for this event
	 * @param d Dispatcher for this event
	 * @param a Action for this event
	 * @param o Object for this event
	 */
	synchronized void postEvent(ListElement[] l, EventDispatcher d, int a, Object o) {
		Queued item = new Queued(l, d, a, o);

		if (head == null) /* if the queue was empty */
		{
			head = item;
			tail = item;
		} else /* else add to end of queue */
		{
			tail.next = item;
			tail = item;
		}

		notify();
	}

	/**
	 * This method is called by the thread to remove
	 * items from the queue so that they can be dispatched to their listeners.
	 * If the queue is empty, the thread waits.
	 *
	 * @return The Queued removed from the top of the queue.
	 */
	private synchronized Queued getNextEvent() throws InterruptedException {
		while (running && (head == null)) {
			try {
				wait();
			} catch (InterruptedException e) {
			}
		}

		if (!running) /* if we are stopping */
		{
			throw new InterruptedException(); /* throw an exception */
		}

		Queued item = head;
		head = item.next;
		if (head == null) {
			tail = null;
		}

		return item;
	}
}