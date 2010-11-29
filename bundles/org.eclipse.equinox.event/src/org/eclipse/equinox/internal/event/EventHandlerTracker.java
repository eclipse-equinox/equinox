/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.event;

import java.security.Permission;
import java.util.*;
import org.eclipse.osgi.framework.eventmgr.EventDispatcher;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

public class EventHandlerTracker extends ServiceTracker<EventHandler, EventHandlerWrapper> implements EventDispatcher<EventHandlerWrapper, Permission, Event> {

	private final LogService log;
	//* List<EventHandlerWrapper> of all handlers with topic of "*"
	private final List<EventHandlerWrapper> globalWildcard;
	// Map<String,List<EventHandlerWrapper>> key is topic prefix of partial wildcard
	private final Map<String, List<EventHandlerWrapper>> partialWildcard;
	// Map<String,List<EventHandlerWrapper>> key is topic name
	private final Map<String, List<EventHandlerWrapper>> topicName;

	public EventHandlerTracker(BundleContext context, LogService log) {
		super(context, EventHandler.class.getName(), null);
		this.log = log;
		globalWildcard = new ArrayList<EventHandlerWrapper>();
		partialWildcard = new HashMap<String, List<EventHandlerWrapper>>();
		topicName = new HashMap<String, List<EventHandlerWrapper>>();
	}

	public EventHandlerWrapper addingService(ServiceReference<EventHandler> reference) {
		EventHandlerWrapper wrapper = new EventHandlerWrapper(reference, context, log);
		synchronized (this) {
			if (wrapper.init()) {
				bucket(wrapper);
			}
		}
		return wrapper;
	}

	public void modifiedService(ServiceReference<EventHandler> reference, EventHandlerWrapper service) {
		synchronized (this) {
			unbucket(service);
			if (service.init()) {
				bucket(service);
				return;
			}
		}

		service.flush(); // needs to be called outside sync region
	}

	public void removedService(ServiceReference<EventHandler> reference, EventHandlerWrapper service) {
		synchronized (this) {
			unbucket(service);
		}
		service.flush(); // needs to be called outside sync region
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
				List<EventHandlerWrapper> wrappers = partialWildcard.get(key);
				if (wrappers == null) {
					wrappers = new ArrayList<EventHandlerWrapper>();
					partialWildcard.put(key, wrappers);
				}
				wrappers.add(wrapper);
			}
			// simple topic name
			else {
				List<EventHandlerWrapper> wrappers = topicName.get(topic);
				if (wrappers == null) {
					wrappers = new ArrayList<EventHandlerWrapper>();
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
				List<EventHandlerWrapper> wrappers = partialWildcard.get(key);
				if (wrappers != null) {
					wrappers.remove(wrapper);
					if (wrappers.isEmpty()) {
						partialWildcard.remove(key);
					}
				}
			}
			// simple topic name
			else {
				List<EventHandlerWrapper> wrappers = topicName.get(topic);
				if (wrappers != null) {
					wrappers.remove(wrapper);
					if (wrappers.isEmpty()) {
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
	 * @return a set of handlers
	 */
	public synchronized Set<EventHandlerWrapper> getHandlers(final String topic) {
		// Use a set to remove duplicates
		Set<EventHandlerWrapper> handlers = new HashSet<EventHandlerWrapper>();

		// Add the "*" handlers
		handlers.addAll(globalWildcard);

		// Add the handlers with partial matches
		if (partialWildcard.size() > 0) {
			int index = topic.lastIndexOf('/');
			while (index >= 0) {
				String subTopic = topic.substring(0, index);
				List<EventHandlerWrapper> wrappers = partialWildcard.get(subTopic);
				if (wrappers != null) {
					handlers.addAll(wrappers);
				}
				// Strip the last level from the topic. For example, org/osgi/framework becomes org/osgi.
				// Wildcard topics are inserted into the map with the "/*" stripped off.
				index = subTopic.lastIndexOf('/');
			}
		}

		// Add the handlers for matching topic names
		List<EventHandlerWrapper> wrappers = topicName.get(topic);
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
	public void dispatchEvent(EventHandlerWrapper eventListener, Permission listenerObject, int eventAction, Event eventObject) {
		eventListener.handleEvent(eventObject, listenerObject);
	}
}
