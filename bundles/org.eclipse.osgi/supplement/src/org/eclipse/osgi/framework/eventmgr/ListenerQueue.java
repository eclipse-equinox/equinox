/*******************************************************************************
 * Copyright (c) 2003, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.eventmgr;

import java.util.Map;
import java.util.Set;
import org.eclipse.osgi.framework.eventmgr.EventManager.EventThread;

/**
 * The ListenerQueue is used to snapshot the list of listeners at the time the event
 * is fired. The snapshot list is then used to dispatch
 * events to those listeners. A ListenerQueue object is associated with a
 * specific EventManager object. ListenerQueue objects constructed with the same
 * EventManager object will get in-order delivery of events when
 * using asynchronous delivery. No delivery order is guaranteed for synchronous
 * delivery to avoid any potential deadly embraces.
 *
 * <p>ListenerQueue objects are created as necessary to build a list of listeners
 * that should receive a specific event or events. Once the list is created, the event
 * can then be synchronously or asynchronously delivered to the list of
 * listeners. After the event has been dispatched for delivery, the
 * ListenerQueue object should be discarded as it is likely the list of listeners is stale.
 * A new ListenerQueue object should be created when it is time to deliver 
 * another event. The Sets used to build the list of listeners must not change after being 
 * added to the list.
 * @since 3.1
 */
public class ListenerQueue<K, V, E> {
	/**
	 * EventManager with which this queue is associated.
	 */
	protected final EventManager manager;
	/**
	 * A list of listener lists.
	 */
	private final Map<Set<Map.Entry<K, V>>, EventDispatcher<K, V, E>> queue;

	/**
	 * Once the listener queue has been used to dispatch an event, 
	 * you cannot add modify the queue.
	 * Access to this field must be protected by a synchronized region.
	 */
	private boolean readOnly;

	/**
	 * ListenerQueue constructor. This method creates an empty snapshot list.
	 *
	 * @param manager The EventManager this queue is associated with.
	 * @throws IllegalArgumentException If manager is null.
	 */
	public ListenerQueue(EventManager manager) {
		if (manager == null) {
			throw new IllegalArgumentException();
		}

		this.manager = manager;
		queue = new CopyOnWriteIdentityMap<Set<Map.Entry<K, V>>, EventDispatcher<K, V, E>>();
		readOnly = false;
	}

	/**
	 * Add a listener list to the snapshot list. This method can be called multiple times, prior to
	 * calling one of the dispatchEvent methods, to build the set of listeners for the
	 * delivery of a specific event. The current list of listeners in the specified EventListeners
	 * object is added to the snapshot list.
	 *
	 * @param listeners An EventListeners object to add to the queue. The current listeners
	 * in the EventListeners object will be called when an event is dispatched.
	 * @param dispatcher An EventDispatcher object to use when dispatching an event
	 * to the listeners on the specified EventListeners.
	 * @throws IllegalStateException If called after one of the dispatch methods has been called.
	 * @deprecated As of 3.5. Replaced by {@link #queueListeners(Set, EventDispatcher)}.
	 */
	public void queueListeners(EventListeners<K, V> listeners, EventDispatcher<K, V, E> dispatcher) {
		queueListeners(listeners.entrySet(), dispatcher);
	}

	/**
	 * Add a set of listeners to the snapshot list. This method can be called multiple times, prior to
	 * calling one of the dispatchEvent methods, to build the list of listeners for the
	 * delivery of a specific event. The specified listeners
	 * are added to the snapshot list.
	 *
	 * @param listeners A Set of Map.Entries to add to the queue. This is typically the entrySet
	 * from a CopyOnWriteIdentityMap object. This set must not change after being added to this
	 * snapshot list.
	 * @param dispatcher An EventDispatcher object to use when dispatching an event
	 * to the specified listeners.
	 * @throws IllegalStateException If called after one of the dispatch methods has been called.
	 * @since 3.5
	 */
	public synchronized void queueListeners(Set<Map.Entry<K, V>> listeners, EventDispatcher<K, V, E> dispatcher) {
		if (readOnly) {
			throw new IllegalStateException();
		}

		if (!listeners.isEmpty()) {
			queue.put(listeners, dispatcher); // enqueue the list and its dispatcher
		}
	}

	/**
	 * Asynchronously dispatch an event to the snapshot list. An event dispatch thread
	 * maintained by the associated EventManager is used to deliver the events.
	 * This method may return immediately to the caller.
	 *
	 * @param eventAction This value is passed to the EventDispatcher.
	 * @param eventObject This object is passed to the EventDispatcher.
	 */
	public void dispatchEventAsynchronous(int eventAction, E eventObject) {
		synchronized (this) {
			readOnly = true;
		}
		EventThread<K, V, E> eventThread = manager.getEventThread();
		synchronized (eventThread) { /* synchronize on the EventThread to ensure no interleaving of posting to the event thread */
			for (Map.Entry<Set<Map.Entry<K, V>>, EventDispatcher<K, V, E>> entry : queue.entrySet()) { /* iterate over the list of listener lists */
				eventThread.postEvent(entry.getKey(), entry.getValue(), eventAction, eventObject);
			}
		}
	}

	/**
	 * Synchronously dispatch an event to the snapshot list. The event may
	 * be dispatched on the current thread or an event dispatch thread
	 * maintained by the associated EventManager.
	 * This method will not return to the caller until the EventDispatcher
	 * has been called (and has returned) for each listener on the queue.
	 *
	 * @param eventAction This value is passed to the EventDispatcher.
	 * @param eventObject This object is passed to the EventDispatcher.
	 */
	public void dispatchEventSynchronous(int eventAction, E eventObject) {
		synchronized (this) {
			readOnly = true;
		}
		// We can't guarantee any delivery order for synchronous events.
		// Attempts to do so result in deadly embraces.
		for (Map.Entry<Set<Map.Entry<K, V>>, EventDispatcher<K, V, E>> entry : queue.entrySet()) { /* iterate over the list of listener lists */
			EventManager.dispatchEvent(entry.getKey(), entry.getValue(), eventAction, eventObject);
		}
	}
}
