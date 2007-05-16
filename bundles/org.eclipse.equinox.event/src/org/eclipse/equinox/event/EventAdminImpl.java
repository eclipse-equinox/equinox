/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.event;

import java.security.Permission;
import java.util.Iterator;
import java.util.Set;
import org.eclipse.osgi.framework.eventmgr.*;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.*;
import org.osgi.service.log.LogService;

/**
 * Implementation of org.osgi.service.event.EventAdmin. EventAdminImpl uses
 * org.eclipse.osgi.framework.eventmgr.EventManager. It is assumeed
 * org.eclipse.osgi.framework.eventmgr package is exported by some other bundle.
 */
public class EventAdminImpl implements EventAdmin {
	private final LogTracker log;
	private final EventHandlerTracker handlers;
	private volatile EventManager		eventManager;

	/**
	 * Constructer for EventAdminImpl.
	 * 
	 * @param context BundleContext
	 */
	EventAdminImpl(BundleContext context) {
		super();
		log = new LogTracker(context, System.out);
		handlers = new EventHandlerTracker(context, log);
	}
	
	/**
	 * This method should be called before registering EventAdmin service
	 */
	void start() {
		log.open();
		eventManager = new EventManager(EventAdminMsg.EVENT_ASYNC_THREAD_NAME);
		handlers.open();
	}

	/**
	 * This method should be called after unregistering EventAdmin service
	 */
	void stop() {
		handlers.close();
		eventManager.close();
		eventManager = null;	// signify we have stopped
		log.close();
	}

	/**
	 * @param event
	 * @see org.osgi.service.event.EventAdmin#postEvent(org.osgi.service.event.Event)
	 */
	public void postEvent(Event event) {
		dispatchEvent(event, true);
	}

	/**
	 * @param event
	 * @see org.osgi.service.event.EventAdmin#sendEvent(org.osgi.service.event.Event)
	 */
	public void sendEvent(Event event) {
		dispatchEvent(event, false);
	}

	/**
	 * Internal main method for sendEvent() and postEvent(). Dispatching an
	 * event to EventHandler. All exceptions are logged except when dealing with
	 * LogEntry.
	 * 
	 * @param event to be delivered
	 * @param isAsync must be set to true for syncronous event delivery, false
	 *        for asyncronous delivery.
	 */
	private void dispatchEvent(Event event, boolean isAsync) {
		// keep a local copy in case we are stopped in the middle of dispatching
		EventManager currentManager = eventManager;
		if (currentManager == null) {
			// EventAdmin is stopped
			return;
		}
		if (event == null) {
			log.log(LogService.LOG_ERROR, EventAdminMsg.EVENT_NULL_EVENT);
			// continue from here will result in an NPE below; the spec for EventAdmin does not allow for null here
		}
		
		String topic = event.getTopic();
		
		try {
			checkTopicPermissionPublish(topic);
		} catch (SecurityException e) {
			String msg = NLS.bind(EventAdminMsg.EVENT_NO_TOPICPERMISSION_PUBLISH, event.getTopic());
			log.log(LogService.LOG_ERROR, msg);
			// must throw a security exception here according to the EventAdmin spec
			throw e;
		}
		
		Set eventHandlers = handlers.getHandlers(topic);
		// If there are no handlers, then we are done
		if (eventHandlers.size() == 0) {
			return;
		}
		
		SecurityManager sm = System.getSecurityManager();
		Permission perm = (sm == null) ? null : new TopicPermission(topic, TopicPermission.SUBSCRIBE);
		
		EventListeners listeners = new EventListeners();
		Iterator iter = eventHandlers.iterator();
		while (iter.hasNext()) {
			EventHandlerWrapper wrapper = (EventHandlerWrapper) iter.next();
			listeners.addListener(wrapper, perm);
		}
		
		// Create the listener queue for this event delivery
		ListenerQueue listenerQueue = new ListenerQueue(currentManager);
		// Add the listeners to the queue and associate them with the event
		// dispatcher
		listenerQueue.queueListeners(listeners, handlers);
		// Deliver the event to the listeners.
		if (isAsync) {
			listenerQueue.dispatchEventAsynchronous(0, event);
		}
		else {
			listenerQueue.dispatchEventSynchronous(0, event);
		}
	}

	/**
	 * Checks if the caller bundle has right PUBLISH TopicPermision.
	 * 
	 * @param topic
	 * @throws SecurityException if the caller does not have the right to PUBLISH TopicPermission
	 */
	private void checkTopicPermissionPublish(String topic) throws SecurityException{
		SecurityManager sm = System.getSecurityManager();
		if (sm == null)
			return;
		sm.checkPermission(new TopicPermission(topic, TopicPermission.PUBLISH));
	}

}
