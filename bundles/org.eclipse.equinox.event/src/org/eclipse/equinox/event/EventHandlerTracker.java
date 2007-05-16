/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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
import java.util.*;
import org.eclipse.osgi.framework.eventmgr.EventDispatcher;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

public class EventHandlerTracker extends ServiceTracker implements EventDispatcher{
	private final LogService log;
	//* List<EventHandlerWrapper> of all handlers with topic of "*"
	private final List globalWildcard;
	// Map<String,List<EventHandlerWrapper>> key is topic prefix of partial wildcard
	private final Map partialWildcard;
	// Map<String,List<EventHandlerWrapper>> key is topic name
	private final Map topicName;

	public EventHandlerTracker(BundleContext context, LogService log) {
		super(context, EventHandler.class.getName(), null);
		this.log = log;
		globalWildcard = new ArrayList();
		partialWildcard = new HashMap();
		topicName = new HashMap();
	}

	public Object addingService(ServiceReference reference) {
		EventHandlerWrapper wrapper = new EventHandlerWrapper(reference, context, log);
		synchronized (this) {
			if (wrapper.init()) {
				bucket(wrapper);
			}
		}
		return wrapper;
	}

	public void modifiedService(ServiceReference reference, Object service) {
		EventHandlerWrapper wrapper = (EventHandlerWrapper)service;
		synchronized (this) {
			unbucket(wrapper);
			if (wrapper.init()) {
				bucket(wrapper);
				return;
			}
		}
		
		wrapper.flush(); // needs to be called outside sync region
	}

	public void removedService(ServiceReference reference, Object service) {
		EventHandlerWrapper wrapper = (EventHandlerWrapper)service;
		synchronized (this) {
			unbucket(wrapper);
		}
		wrapper.flush(); // needs to be called outside sync region
	}

	/**
	 * Place the wrapper into the appropriate buckets.
	 * This is a performance optimization for event delivery.
	 * 
	 * @param wrapper The wrapper to place in buckets.
	 * @GuardedBy this
	 */
	private void bucket(EventHandlerWrapper wrapper) {
		final String[] topics = wrapper.getTopics();
		final int length = (topics == null) ? 0 : topics.length;
		for (int i = 0; i < length; i++) {
			String topic = topics[i];
			// global wildcard
			if (topic.equals("*")) { //$NON-NLS-1$
				globalWildcard.add(wrapper);
			}
			// partial wildcard
			else if (topic.endsWith("/*")) { //$NON-NLS-1$
				String key = topic.substring(0, topic.length() - 2); // Strip off "/*" from the end
				List wrappers = (List) partialWildcard.get(key);
				if (wrappers == null) {
					wrappers = new ArrayList();
					partialWildcard.put(key, wrappers);
				}
				wrappers.add(wrapper);
			}
			// simple topic name
			else {
				List wrappers = (List) topicName.get(topic);
				if (wrappers == null) {
					wrappers = new ArrayList();
					topicName.put(topic, wrappers);
				}
				wrappers.add(wrapper);
			}
		}
	}
	
	/**
	 * Remove the wrapper from the buckets.
	 * 
	 * @param wrapper The wrapper to remove from the buckets.
	 * @GuardedBy this
	 */
	private void unbucket(EventHandlerWrapper wrapper) {
		final String[] topics = wrapper.getTopics();
		final int length = (topics == null) ? 0 : topics.length;
		for (int i = 0; i < length; i++) {
			String topic = topics[i];
			// global wilcard
			if (topic.equals("*")) { //$NON-NLS-1$
				globalWildcard.remove(wrapper);
			}
			// partial wildcard
			else if (topic.endsWith("/*")) { //$NON-NLS-1$
				String key = topic.substring(0, topic.length() - 2); // Strip off "/*" from the end
				List wrappers = (List) partialWildcard.get(key);
				if (wrappers != null) {
					wrappers.remove(wrapper);
					if (wrappers.size() == 0) {
						partialWildcard.remove(key);
					}
				}
			}
			// simple topic name
			else {
				List wrappers = (List) topicName.get(topic);
				if (wrappers != null) {
					wrappers.remove(wrapper);
					if (wrappers.size() == 0) {
						topicName.remove(topic);
					}
				}
			}
		}
	}
	
	/**
	 * Return the set of handlers which subscribe to the event topic.
	 * A set is used to ensure a handler is not called for an event more than once.
	 * 
	 * @param topic
	 * @return
	 */
	public synchronized Set getHandlers(final String topic) {
		// Use a set to remove duplicates
		Set handlers = new HashSet();
		
		// Add the "*" handlers
		handlers.addAll(globalWildcard);
		
		// Add the handlers with partial matches
		if (partialWildcard.size() > 0) {
			int index = topic.length();
			while (index >= 0) {
				String subTopic = topic.substring(0, index); // First subtopic is the complete topic.
				List wrappers = (List) partialWildcard.get(subTopic);
				if (wrappers != null) {
					handlers.addAll(wrappers);
				}
				// Strip the last level from the topic. For example, org/osgi/framework becomes org/osgi.
				// Wildcard topics are inserted into the map with the "/*" stripped off.
				index = subTopic.lastIndexOf('/');
			}
		}
		
		// Add the handlers for matching topic names
		List wrappers = (List) topicName.get(topic);
		if (wrappers != null) {
			handlers.addAll(wrappers);
		}

		return handlers;
	}
	
	/**
	 * Dispatches Event to EventHandlers
	 * 
	 * @param eventListener
	 * @param listenerObject
	 * @param eventAction
	 * @param eventObject
	 * @see org.eclipse.osgi.framework.eventmgr.EventDispatcher#dispatchEvent(java.lang.Object,
	 *      java.lang.Object, int, java.lang.Object)
	 */
	public void dispatchEvent(Object eventListener, Object listenerObject, int eventAction, Object eventObject) {
		((EventHandlerWrapper) eventListener).handleEvent((Event) eventObject, (Permission)listenerObject);
	}
}
