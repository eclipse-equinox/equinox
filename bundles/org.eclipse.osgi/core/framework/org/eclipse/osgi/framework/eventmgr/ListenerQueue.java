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
 * The ListenerQueue is used to snapshot the set of listeners at the time the event
 * is fired. The snapshot list is then used to dispatch
 * events to those listeners. A ListenerQueue object is associated with a
 * specific EventManager object. ListenerQueue objects that use the same
 * EventManager object will get in order delivery of events
 * within each delivery class: synchronous and asynchronous.
 *
 * <p>ListenerQueue objects are created as necesssary to build a set of listeners
 * that should receive a specific event or events. Once the set is created, the event
 * can then be synchronously or asynchronously delivered to the set of
 * listeners. After the event has been dispatched for delivery, the
 * ListenerQueue object should be discarded as it is likely the list of listeners is stale.
 * A new ListenerQueue object should be created when it is time to deliver 
 * another event.
 */

public class ListenerQueue {
	/**
	 * EventManager with which this queue is associated.
	 */
	protected EventManager manager;
	/**
	 * A list of listener lists.
	 */
	protected ElementList queue;

	/**
	 * Once the listener queue has been used to dispatch an event, 
	 * you cannot add modify the queue.
	 */
	private boolean readOnly;

	/**
	 * ListenerQueue constructor. This method creates an empty event queue.
	 *
	 * @param manager The EventManager this queue is associated with.
	 * @throws IllegalArgumentException If manager is null.
	 */
	public ListenerQueue(EventManager manager) {
		if (manager == null) {
			throw new IllegalArgumentException();
		}

		this.manager = manager;
		queue = new ElementList();
		readOnly = false;
	}

	/**
	 * Build the set of listeners. This method can be called multiple times, prior to
	 * calling one of the dispatchEvent methods, to build the set of listeners for the
	 * delivery of a specific event. The current list of listeners in the specified EventListeners,
	 * at the time this method is called, is added to the set.
	 *
	 * @param listeners An EventListeners object to add to the queue. All listeners
	 * previously added to
	 * the EventListeners object will be called when an event is dispatched.
	 * @param dispatcher An EventDispatcher object to use when dispatching an event
	 * to the listeners on this ElementList.
	 * @throws IllegalStateException If called after one of the dispatch methods has been called.
	 */
	public synchronized void queueListeners(EventListeners listeners, EventDispatcher dispatcher) {
		if (readOnly) {
			throw new IllegalStateException();
		}

		if (listeners != null) {
			ListElement[] list = listeners.getListeners();

			if (list.length > 0) {
				queue.addElement(new ListElement(list, dispatcher));
			}
		}
	}

	/**
	 * Asynchronously dispatch an event to the set of listeners. An event dispatch thread
	 * maintained by the associated EventManager is used to deliver the events.
	 * This method may return immediately to the caller.
	 *
	 * @param eventAction This value is passed to the EventDispatcher.
	 * @param eventObject This object is passed to the EventDispatcher.
	 */
	public void dispatchEventAsynchronous(int eventAction, Object eventObject) {
		ListElement[] lists;

		synchronized (this) {
			readOnly = true;
			lists = queue.getElements();
		}
		EventThread eventThread = manager.getEventThread();
		synchronized (eventThread) {	/* synchronize on the EventThread to ensure no interleaving of posting to the event thread */
			int size = lists.length;
			for (int i = 0; i < size; i++) { /* iterate over the list of listener lists */
				ListElement list = lists[i];
				eventThread.postEvent((ListElement[]) list.primary, (EventDispatcher) list.companion, eventAction, eventObject);
			}
		}
	}

	/**
	 * Synchronously dispatch an event to the set of listeners. The event may
	 * be dispatched on the current thread or an event dispatch thread
	 * maintained by the associated EventManager.
	 * This method will not return to the caller until the EventDispatcher
	 * has been called (and returned) for each listener on the queue.
	 *
	 * @param eventAction This value is passed to the EventDispatcher.
	 * @param eventObject This object is passed to the EventDispatcher.
	 */
	public void dispatchEventSynchronous(int eventAction, Object eventObject) {
		ListElement[] lists;

		synchronized (this) {
			readOnly = true;
			lists = queue.getElements();
		}
		synchronized (manager) {	/* synchronize on the EventManager to ensure no interleaving of event delivery */
			int size = lists.length;
			for (int i = 0; i < size; i++) { /* iterate over the list of listener lists */
				ListElement list = lists[i];
				EventManager.dispatchEvent((ListElement[]) list.primary, (EventDispatcher) list.companion, eventAction, eventObject);
			}
		}
	}
}