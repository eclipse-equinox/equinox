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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.Set;

/**
 * This class is the central class for the Event Manager. Each
 * program that wishes to use the Event Manager should construct
 * an EventManager object and use that object to construct
 * ListenerQueue for dispatching events. CopyOnWriteIdentityMap objects
 * must be used to manage listener lists.
 *
 * <p>This example uses the fictitious SomeEvent class and shows how to use this package 
 * to deliver a SomeEvent to a set of SomeEventListeners.  
 * <pre>
 *
 * 	// Create an EventManager with a name for an asynchronous event dispatch thread
 * 	EventManager eventManager = new EventManager("SomeEvent Async Event Dispatcher Thread");
 * 	// Create a CopyOnWriteIdentityMap to hold the list of SomeEventListeners
 *	Map eventListeners = new CopyOnWriteIdentityMap();
 *
 *	// Add a SomeEventListener to the listener list
 *	eventListeners.put(someEventListener, null);
 *
 *	// Asynchronously deliver a SomeEvent to registered SomeEventListeners
 *	// Create the listener queue for this event delivery
 *	ListenerQueue listenerQueue = new ListenerQueue(eventManager);
 *	// Add the listeners to the queue and associate them with the event dispatcher
 *	listenerQueue.queueListeners(eventListeners.entrySet(), new EventDispatcher() {
 *		public void dispatchEvent(Object eventListener, Object listenerObject, 
 *                                    int eventAction, Object eventObject) {
 * 			try {
 *				(SomeEventListener)eventListener.someEventOccured((SomeEvent)eventObject);
 * 			} catch (Throwable t) {
 * 				// properly log/handle any Throwable thrown by the listener
 * 			}
 *		}
 *	});
 *	// Deliver the event to the listeners. 
 *	listenerQueue.dispatchEventAsynchronous(0, new SomeEvent());
 *		
 *	// Remove the listener from the listener list
 *	eventListeners.remove(someEventListener);
 *
 *	// Close EventManager to clean when done to terminate async event dispatch thread.
 *	// Note that closing the event manager while asynchronously delivering events 
 *	// may cause some events to not be delivered before the async event dispatch 
 *	// thread terminates
 *	eventManager.close();
 * </pre>
 * 
 * <p>At first glance, this package may seem more complicated than necessary
 * but it has support for some important features. The listener list supports
 * companion objects for each listener object. This is used by the OSGi framework
 * to create wrapper objects for a listener which are passed to the event dispatcher.
 * The ListenerQueue class is used to build a snap shot of the listeners prior to beginning
 * event dispatch. 
 * 
 * The OSGi framework uses a 2 level listener list for each listener type (4 types). 
 * Level one is managed per framework instance and contains the list of BundleContexts which have 
 * registered a listener. Level 2 is managed per BundleContext for the listeners in that 
 * context. This allows all the listeners of a bundle to be easily and atomically removed from 
 * the level one list. To use a "flat" list for all bundles would require the list to know which 
 * bundle registered a listener object so that the list could be traversed when stopping a bundle 
 * to remove all the bundle's listeners. 
 * 
 * When an event is fired, a snapshot list (ListenerQueue) must be made of the current listeners before delivery 
 * is attempted. The snapshot list is necessary to allow the listener list to be modified while the 
 * event is being delivered to the snapshot list. The memory cost of the snapshot list is
 * low since the ListenerQueue object uses the copy-on-write semantics 
 * of the CopyOnWriteIdentityMap. This guarantees the snapshot list is never modified once created.
 * 
 * The OSGi framework also uses a 2 level dispatch technique (EventDispatcher).
 * Level one dispatch is used by the framework to add the level 2 listener list of each 
 * BundleContext to the snapshot in preparation for delivery of the event.
 * Level 2 dispatch is used as the final event deliverer and must cast the listener 
 * and event objects to the proper type before calling the listener. Level 2 dispatch
 * will cancel delivery of an event 
 * to a bundle that has stopped between the time the snapshot was created and the
 * attempt was made to deliver the event.
 * 
 * <p> The highly dynamic nature of the OSGi framework had necessitated these features for 
 * proper and efficient event delivery.  
 * @since 3.1
 * @noextend This class is not intended to be subclassed by clients.
 */

public class EventManager {
	static final boolean DEBUG = false;

	/**
	 * EventThread for asynchronous dispatch of events.
	 * Access to this field must be protected by a synchronized region.
	 */
	private EventThread<?, ?, ?> thread;

	/** 
	 * Once closed, an attempt to create a new EventThread will result in an 
	 * IllegalStateException. 
	 */
	private boolean closed;

	/**
	 * Thread name used for asynchronous event delivery
	 */
	protected final String threadName;

	/**
	 * The thread group used for asynchronous event delivery
	 */
	protected final ThreadGroup threadGroup;

	/**
	 * EventManager constructor. An EventManager object is responsible for
	 * the delivery of events to listeners via an EventDispatcher.
	 *
	 */
	public EventManager() {
		this(null, null);
	}

	/**
	 * EventManager constructor. An EventManager object is responsible for
	 * the delivery of events to listeners via an EventDispatcher.
	 *
	 * @param threadName The name to give the event thread associated with
	 * this EventManager.  A <code>null</code> value is allowed.
	 */
	public EventManager(String threadName) {
		this(threadName, null);
	}

	/**
	 * EventManager constructor. An EventManager object is responsible for
	 * the delivery of events to listeners via an EventDispatcher.
	 *
	 * @param threadName The name to give the event thread associated with
	 * this EventManager.  A <code>null</code> value is allowed.
	 * @param threadGroup The thread group to use for the asynchronous event
	 * thread associated with this EventManager. A <code>null</code> value is allowed.
	 * @since 3.4
	 */
	public EventManager(String threadName, ThreadGroup threadGroup) {
		thread = null;
		closed = false;
		this.threadName = threadName;
		this.threadGroup = threadGroup;
	}

	/**
	 * This method can be called to release any resources associated with this
	 * EventManager.
	 * <p>
	 * Closing this EventManager while it is asynchronously delivering events 
	 * may cause some events to not be delivered before the async event dispatch 
	 * thread terminates.
	 */
	public synchronized void close() {
		if (closed) {
			return;
		}
		if (thread != null) {
			thread.close();
			thread = null;
		}
		closed = true;
	}

	/**
	 * Returns the EventThread to use for dispatching events asynchronously for
	 * this EventManager.
	 *
	 * @return EventThread to use for dispatching events asynchronously for
	 * this EventManager.
	 */
	synchronized <K, V, E> EventThread<K, V, E> getEventThread() {
		if (closed) {
			throw new IllegalStateException();
		}
		if (thread == null) {
			/* if there is no thread, then create a new one */
			thread = AccessController.doPrivileged(new PrivilegedAction<EventThread<K, V, E>>() {
				public EventThread<K, V, E> run() {
					EventThread<K, V, E> t = new EventThread<K, V, E>(threadGroup, threadName);
					return t;
				}
			});
			/* start the new thread */
			thread.start();
		}

		@SuppressWarnings("unchecked")
		EventThread<K, V, E> result = (EventThread<K, V, E>) thread;
		return result;
	}

	/**
	 * This method calls the EventDispatcher object to complete the dispatch of
	 * the event. If there are more elements in the list, call dispatchEvent
	 * on the next item on the list.
	 * This method is package private.
	 *
	 * @param listeners A Set of entries from a CopyOnWriteIdentityMap map.
	 * @param dispatcher Call back object which is called to complete the delivery of
	 * the event.
	 * @param eventAction This value was passed by the event source and
	 * is passed to this method. This is passed on to the call back object.
	 * @param eventObject This object was created by the event source and
	 * is passed to this method. This is passed on to the call back object.
	 */
	static <K, V, E> void dispatchEvent(Set<Map.Entry<K, V>> listeners, EventDispatcher<K, V, E> dispatcher, int eventAction, E eventObject) {
		for (Map.Entry<K, V> listener : listeners) { /* iterate over the list of listeners */
			final K eventListener = listener.getKey();
			final V listenerObject = listener.getValue();
			try {
				/* Call the EventDispatcher to complete the delivery of the event. */
				dispatcher.dispatchEvent(eventListener, listenerObject, eventAction, eventObject);
			} catch (Throwable t) {
				/* Consume and ignore any exceptions thrown by the listener */
				if (DEBUG) {
					System.out.println("Exception in " + eventListener); //$NON-NLS-1$
					t.printStackTrace();
				}
			}
		}
	}

	/**
	 * This package private class is used for asynchronously dispatching events.
	 */

	static class EventThread<K, V, E> extends Thread {
		private static int nextThreadNumber;

		/**
		 * Queued is a nested top-level (non-member) class. This class
		 * represents the items which are placed on the asynch dispatch queue.
		 * This class is private.
		 */
		private static class Queued<K, V, E> {
			/** listener list for this event */
			final Set<Map.Entry<K, V>> listeners;
			/** dispatcher of this event */
			final EventDispatcher<K, V, E> dispatcher;
			/** action for this event */
			final int action;
			/** object for this event */
			final E object;
			/** next item in event queue */
			Queued<K, V, E> next;

			/**
			 * Constructor for event queue item
			 *
			 * @param l Listener list for this event
			 * @param d Dispatcher for this event
			 * @param a Action for this event
			 * @param o Object for this event
			 */
			Queued(Set<Map.Entry<K, V>> l, EventDispatcher<K, V, E> d, int a, E o) {
				listeners = l;
				dispatcher = d;
				action = a;
				object = o;
				next = null;
			}
		}

		/** item at the head of the event queue */
		private Queued<K, V, E> head;
		/** item at the tail of the event queue */
		private Queued<K, V, E> tail;
		/** if false the thread must terminate */
		private volatile boolean running;

		/**
		 * Constructor for the event thread. 
		 * @param threadName Name of the EventThread 
		 */
		EventThread(ThreadGroup threadGroup, String threadName) {
			super(threadGroup, threadName == null ? getNextName() : threadName);
			running = true;
			head = null;
			tail = null;

			setDaemon(true); /* Mark thread as daemon thread */
		}

		private static synchronized String getNextName() {
			return "EventManagerThread-" + nextThreadNumber++; //$NON-NLS-1$
		}

		/**
		 * Constructor for the event thread. 
		 * @param threadName Name of the EventThread 
		 */
		EventThread(String threadName) {
			this(null, threadName);
		}

		/**
		 * Constructor for the event thread.
		 */
		EventThread() {
			this(null, null);
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
			try {
				while (true) {
					Queued<K, V, E> item = getNextEvent();
					if (item == null) {
						return;
					}
					EventManager.dispatchEvent(item.listeners, item.dispatcher, item.action, item.object);
					// Bug 299589: since the call to getNextEvent() will eventually block for a long time, we need to make sure that the 'item'
					// variable is cleared of the previous value before the call to getNextEvent(). See VM SPec 2.5.7 for why the compiler 
					// will not automatically clear this variable for each loop iteration.
					item = null;
				}
			} catch (RuntimeException e) {
				if (EventManager.DEBUG) {
					e.printStackTrace();
				}
				throw e;
			} catch (Error e) {
				if (EventManager.DEBUG) {
					e.printStackTrace();
				}
				throw e;
			}
		}

		/**
		 * This methods takes the input parameters and creates a Queued
		 * object and queues it.
		 * The thread is notified.
		 *
		 * @param l Listener list for this event
		 * @param d Dispatcher for this event
		 * @param a Action for this event
		 * @param o Object for this event
		 */
		synchronized void postEvent(Set<Map.Entry<K, V>> l, EventDispatcher<K, V, E> d, int a, E o) {
			if (!isAlive()) { /* If the thread is not alive, throw an exception */
				throw new IllegalStateException();
			}

			Queued<K, V, E> item = new Queued<K, V, E>(l, d, a, o);

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
		 * @return The Queued removed from the top of the queue or null
		 * if the thread has been requested to stop.
		 */
		private synchronized Queued<K, V, E> getNextEvent() {
			while (running && (head == null)) {
				try {
					wait();
				} catch (InterruptedException e) {
					// If interrupted, we will loop back up and check running
				}
			}

			if (!running) { /* if we are stopping */
				return null;
			}

			Queued<K, V, E> item = head;
			head = item.next;
			if (head == null) {
				tail = null;
			}

			return item;
		}
	}
}
