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
 * This class is the central class for the event manager. Each
 * program that wishes to use the event manager should construct
 * an EventManager object and use that object to construct
 * ListenerQueue for dispatching events.
 *
 * <p>This example uses the ficticous SomeEvent class and shows how to use this package 
 * to deliver SomeEvents to SomeEventListeners.  
 * <pre>
 *
 * 		// Create an EventManager with a name for an asynchronous event dispatch thread
 * 		EventManager eventManager = new EventManager("SomeEvent Async Event Dispatcher Thread");
 * 		// Create an EventListeners to hold the list of SomeEventListeners
 *		EventListeners eventListeners = new EventListeners();
 *
 *		// Add a SomeEventListener to the listener list
 *	    eventListeners.addListener(someEventListener, null);
 *
 *		// Asynchronously deliver a SomeEvent to registered SomeEventListeners
 *		// Create the listener queue for this event delivery
 *		ListenerQueue listenerQueue = new ListenerQueue(eventManager);
 *		// Add the listeners to the queue and associate them with the event dispatcher
 *		listenerQueue.queueListeners(eventListeners, new EventDispatcher() {
 *	        public void dispatchEvent(Object eventListener, Object listenerObject, 
 *                                    int eventAction, Object eventObject) {
 * 				(SomeEventListener)listener.someEventOccured((SomeEvent)eventObject);
 *			}
 *		});
 *		// Deliver the event to the listeners. 
 *		listenerQueue.dispatchEventAsynchronous(0, new SomeEvent());
 *		
 *		// Remove the listener from the listener list
 *	    eventListeners.removeListener(someEventListener);
 *
 *		// Close EventManager to clean when done to terminate async event dispatch thread
 *		eventManager.close();
 * </pre>
 * 
 * <p>At first blush, this package may seem more complicated than necessary
 * but it has support for some important features. The listener list supports
 * companion objects for each listener object. This is used by the OSGi framework
 * to create wrapper objects for a listener which are passed to the event dispatcher.
 * The ListenerQueue class is used to build the snap shot listeners prior to beginning
 * event dispatch. The OSGi framework uses a 2 level dispatch technique which allows a bundle's
 * listeners to be quickly removed form the listener list. This 2 level technique really means
 * the OSGi framework stores each bundle's listeners is a list and then maintains a list of
 * bundles which have a listener list. The EventDispatcher is also used as the final event
 * deliverer and must cast the listener and event objects to the proper type. The OSGi framework
 * uses EventDispatcher for 2 things. It uses an EventDispacther at the top level (Framework)
 * to build the ListenerQueue of all registered listeners at the time the event is fire. 
 * It also uses an EventDispatcher at the lower level to short circuit delivery of an event 
 * to a bundle that has stopped bewteen the time the ListenerQueue was created and the
 * attempt was made to deliver the event.
 * 
 * <p> The highly dynamic nature of the OSGi framework had necessitated these features for 
 * proper and efficient event delivery.  
 */

public class EventManager {
	static final boolean DEBUG = true;

	/**
	 * EventThread for asynchronous dispatch of events.
	 */
	private EventThread thread;

	/**
	 * EventThread Name
	 */
	protected String threadName;

	/**
	 * EventManager constructor. An EventManager object is responsible for
	 * the delivery of events to listeners via an EventDispatcher.
	 *
	 */
	public EventManager() {
		thread = null;
	}

	public EventManager(String threadName) {
		this();
		this.threadName = threadName;
	}

	/**
	 * This method can be called to release any resources associated with this
	 * EventManager.
	 *
	 */
	public synchronized void close() {
		if (thread != null) {
			thread.close();
			thread = null;
		}
	}

	/**
	 * Returns the EventThread to use for dispatching events asynchronously for
	 * this EventManager.
	 *
	 * @return EventThread to use for dispatching events asynchronously for
	 * this EventManager.
	 */
	synchronized EventThread getEventThread() {
		if (thread == null) {
			/* if there is no thread, then create a new one */
			if (threadName == null) {
				thread = new EventThread();
			} else {
				thread = new EventThread(threadName);
			}
			thread.start(); /* start the new thread */
		}

		return thread;
	}

	/**
	 * This method calls the EventDispatcher object to complete the dispatch of
	 * the event. If there are more elements in the list, call dispatchEvent
	 * on the next item on the list.
	 * This method is package private.
	 *
	 * @param listeners An array of ListElements with each element containing the primary and 
	 * companion object for a listener.
	 * @param dispatcher Call back object which is called to complete the delivery of
	 * the event.
	 * @param eventAction This value was passed by the event source and
	 * is passed to this method. This is passed on to the call back object.
	 * @param eventObject This object was created by the event source and
	 * is passed to this method. This is passed on to the call back object.
	 */
	static void dispatchEvent(ListElement[] listeners, EventDispatcher dispatcher, int eventAction, Object eventObject) {
		int size = listeners.length;
		for (int i = 0; i < size; i++) { /* iterate over the list of listeners */
			ListElement listener = listeners[i];
			try {
				/* Call the EventDispatcher to complete the delivery of the event. */
				dispatcher.dispatchEvent(listener.primary, listener.companion, eventAction, eventObject);
			} catch (Throwable t) {
				/* Consume and ignore any exceptions thrown by the listener */

				if (DEBUG) {
					System.out.println("Exception in " + listener.primary); //$NON-NLS-1$
					t.printStackTrace();
				}
			}
		}
	}
}