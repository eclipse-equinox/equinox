/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
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
 * The EventQueue is used to build the set of listeners and then dispatch
 * events to those listeners. An EventQueue object is associated with a
 * specific EventManager object.
 *
 * <p>EventQueue objects are created on demand to build a set of listeners
 * that should receive a specific event. Once the set is created, the event
 * can then be synchronously or asynchronously delivered to the set of
 * listeners. After the event has been dispatched for delivery, the
 * EventQueue object should be discarded. A new EventQueue object should be
 * created for the delivery of another specific event.
 */

public class EventQueue {
	/**
	 * EventManager with which this queue is associated.
	 */
	protected EventManager manager;
	/**
	 * Set of listeners (list of listener lists).
	 */
	protected ListenerList queue;

	/**
	 * EventQueue constructor. This method creates an empty event queue.
	 *
	 * @param manager The EventManager this queue is associated with.
	 * @exception java.lang.NullPointerException if manager is null.
	 */
	public EventQueue(EventManager manager) {
		this.manager = manager;
		if (manager == null) {
			throw new NullPointerException();
		}

		queue = null;
	}

	/**
	 * Build the set of listeners. This method can be called multiple times, prior to
	 * calling one of the dispatchEvent methods, to build the set of listeners for the
	 * delivery of a specific event. The current list of listeners in the given ListenerList,
	 * at the time this method is called, is added to the set.
	 *
	 * @param listeners An EventListeners object to add to the queue. All listeners
	 * previously added to
	 * the EventListeners object will be called when an event is dispatched.
	 * @param source An EventSource object to use when dispatching an event
	 * to the listeners on this ListenerList.
	 */
	public synchronized void queueListeners(EventListeners listeners, EventSource source) {
		if (listeners != null) {
			ListenerList list = listeners.list;

			if (list != null) {
				queue = ListenerList.addListener(queue, list, source);
			}
		}
	}

	/**
	 * Asynchronously dispatch an event to the set of listeners. An event dispatch thread
	 * maintained by the associated EventManager is used to deliver the events.
	 * This method may return immediately to the caller.
	 *
	 * @param eventAction This value is passed back to the event source when the call back
	 * is made to the EventSource object along with each listener.
	 * @param eventObject This object is passed back to the event source when the call back
	 * is made to the EventSource object along with each listener.
	 */
	public void dispatchEventAsynchronous(int eventAction, Object eventObject) {
		manager.dispatchEventAsynchronous(queue, eventAction, eventObject);
	}

	/**
	 * Synchronously dispatch an event to the set of listeners. The event may
	 * be dispatched on the current thread or an event dispatch thread
	 * maintained by the associated EventManager.
	 * This method will not return to the caller until an EventSource
	 * has been called (and returned) for each listener.
	 *
	 * @param eventAction This value is passed back to the event source when the call back
	 * is made to the EventSource object along with each listener.
	 * @param eventObject This object is passed back to the event source when the call back
	 * is made to the EventSource object along with each listener.
	 */
	public void dispatchEventSynchronous(int eventAction, Object eventObject) {
		manager.dispatchEventSynchronous(queue, eventAction, eventObject);
	}

}
