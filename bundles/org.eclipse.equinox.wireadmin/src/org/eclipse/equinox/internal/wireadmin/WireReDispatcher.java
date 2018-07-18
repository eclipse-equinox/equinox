/*******************************************************************************
 * Copyright (c) 1997-2009 by ProSyst Software GmbH
 * http://www.prosyst.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *    Eurotech
 *******************************************************************************/
package org.eclipse.equinox.internal.wireadmin;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Dictionary;
import java.util.Hashtable;
import org.eclipse.equinox.internal.util.ref.Log;
import org.osgi.framework.*;
import org.osgi.service.event.*;
import org.osgi.service.wireadmin.*;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * This is an implementation of log events redispatching.
 * 
 * /**
 * 
 * @author Pavlin Dobrev
 * @version 1.0
 */
public class WireReDispatcher implements WireAdminListener {

	static final String BUNDLE = "bundle";
	static final String BUNDLE_ID = "bundle.id";
	static final String BUNDLE_SYMBOLICNAME = "bundle.symbolicName";
	static final String EVENT = "event";
	static final String EXCEPTION = "exception";
	static final String EXCEPTION_CLASS = "exception.class";
	static final String EXCEPTION_MESSAGE = "exception.message";
	static final String SERVICE = "service";
	static final String SERVICE_ID = "service.id";
	static final String SERVICE_OBJECTCLASS = "service.objectClass";
	static final String SERVICE_PID = "service.pid";
	static final char TOPIC_SEPARATOR = '/';
	/* ///////WIRE ADMIN EVENTS////////// */
	static final String WIRE_HEADER = "org/osgi/service/wireadmin/WireAdminEvent";
	static final String WIRE_CREATED = "WIRE_CREATED";
	static final String WIRE_CONNECTED = "WIRE_CONNECTED";
	static final String WIRE_UPDATED = "WIRE_UPDATED";
	static final String WIRE_TRACE = "WIRE_TRACE";
	static final String WIRE_DISCONNECTED = "WIRE_DISCONNECTED";
	static final String WIRE_DELETED = "WIRE_DELETED";
	static final String PRODUCER_EXCEPTION = "PRODUCER_EXCEPTION";
	static final String CONSUMER_EXCEPTION = "CONSUMER_EXCEPTION";

	static final String[] EVENT_TOPICS;

	static final String WIRE_ENTRY = "wire.entry";
	static final String WA_WIRE = "wire";
	static final String WA_WIRE_FLAVORS = "wire.flavors";
	static final String WA_WIRE_SCOPE = "wire.scope";
	static final String WA_WIRE_CONNECTED = "wire.connected";
	static final String WA_WIRE_VALID = "wire.valid";

	static {
		EVENT_TOPICS = new String[8];
		EVENT_TOPICS[0] = WIRE_HEADER + TOPIC_SEPARATOR + WIRE_CREATED;
		EVENT_TOPICS[1] = WIRE_HEADER + TOPIC_SEPARATOR + WIRE_CONNECTED;
		EVENT_TOPICS[2] = WIRE_HEADER + TOPIC_SEPARATOR + WIRE_UPDATED;
		EVENT_TOPICS[3] = WIRE_HEADER + TOPIC_SEPARATOR + WIRE_TRACE;
		EVENT_TOPICS[4] = WIRE_HEADER + TOPIC_SEPARATOR + WIRE_DISCONNECTED;
		EVENT_TOPICS[5] = WIRE_HEADER + TOPIC_SEPARATOR + WIRE_DELETED;
		EVENT_TOPICS[6] = WIRE_HEADER + TOPIC_SEPARATOR + PRODUCER_EXCEPTION;
		EVENT_TOPICS[7] = WIRE_HEADER + TOPIC_SEPARATOR + CONSUMER_EXCEPTION;
	}

	BundleContext bc;
	ServiceRegistration waReg;
	Log log;
	ServiceTracker eventAdminTracker;
	ServiceTracker eventHandlerTracker;
	EventHandlerIndex eventHandlerIndex;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bc) throws Exception {
		this.bc = bc;
		log = new Log(bc, false);
		eventHandlerIndex = new EventHandlerIndex();
		log.setDebug(Activator.getBoolean("equinox.wireadmin.redispatcher.debug"));
		log.setPrintOnConsole(Activator.getBoolean("equinox.wireadmin.redispatcher.console"));

		eventHandlerTracker = new ServiceTracker(bc, EventHandler.class.getName(), eventHandlerIndex);
		eventHandlerTracker.open();

		eventAdminTracker = new ServiceTracker(bc, EventAdmin.class.getName(), null);
		eventAdminTracker.open();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop() throws Exception {
		if (eventAdminTracker != null) {
			eventAdminTracker.close();
			eventAdminTracker = null;
		}
		if (eventHandlerTracker != null) {
			eventHandlerTracker.close();
			eventHandlerTracker = null;
		}
		log.close();
		this.bc = null;
	}

	/* ////////UTILITY METHODS//////// */

	/*
	 * Add exception properties needed in event by EventAdmin specification.
	 */
	void addExceptionProps(Hashtable props, Throwable t) {
		props.put(EXCEPTION, t);
		props.put(EXCEPTION_CLASS, t.getClass().getName());
		String message = t.getMessage();
		if (message != null) {
			props.put(EXCEPTION_MESSAGE, t.getMessage());
		}
	}

	/*
	 * Add service properties needed in event by EventAdmin specification.
	 */
	void addServiceProps(Hashtable props, ServiceReference ref) {
		props.put(SERVICE, ref);
		props.put(SERVICE_ID, ref.getProperty(Constants.SERVICE_ID));
		Object tmp = ref.getProperty(Constants.SERVICE_PID);
		if (tmp != null && tmp instanceof String) {
			props.put(SERVICE_PID, tmp);
		}
		tmp = ref.getProperty(Constants.OBJECTCLASS);
		if (tmp != null && tmp instanceof String[]) {
			props.put(SERVICE_OBJECTCLASS, tmp);
		}
	}

	/* ////////LISTENER METHODS//////// */

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.wireadmin.WireAdminListener#wireAdminEvent(org.osgi.service.wireadmin.WireAdminEvent)
	 */
	public void wireAdminEvent(WireAdminEvent event) {
		ServiceTracker st = eventAdminTracker;
		final EventAdmin eventAdmin = st == null ? null : ((EventAdmin) st.getService());
		if (eventAdmin != null) {
			ServiceReference ref = event.getServiceReference();
			if (ref == null) {
				throw new RuntimeException("Wire Admin ServiceReference is null");
			}
			final int topicIndex;
			switch (event.getType()) {
				case WireAdminEvent.WIRE_CREATED :
					topicIndex = 0;
					break;
				case WireAdminEvent.WIRE_CONNECTED :
					topicIndex = 1;
					break;
				case WireAdminEvent.WIRE_UPDATED :
					topicIndex = 2;
					break;
				case WireAdminEvent.WIRE_TRACE :
					topicIndex = 3;
					break;
				case WireAdminEvent.WIRE_DISCONNECTED :
					topicIndex = 4;
					break;
				case WireAdminEvent.WIRE_DELETED :
					topicIndex = 5;
					break;
				case WireAdminEvent.PRODUCER_EXCEPTION :
					topicIndex = 6;
					break;
				case WireAdminEvent.CONSUMER_EXCEPTION :
					topicIndex = 7;
					break;
				default : /* ignore: unknown/new events */
					return;
			}
			if (!eventHandlerIndex.hasEventHandlers(topicIndex)) {
				if (Activator.LOG_DEBUG)
					log.debug(0, 10017, event.toString(), null, false);
				return; /*
															 * no service references for this topic do not bother
															 * EventAdmin
															 */
			}
			Hashtable props = new Hashtable();
			addServiceProps(props, ref);
			Wire wire = event.getWire();
			if (wire != null) {
				props.put(WA_WIRE, wire);
				props.put(WA_WIRE_CONNECTED, wire.isConnected() ? Boolean.TRUE : Boolean.FALSE);
				if (wire.getFlavors() != null) {
					props.put(WA_WIRE_FLAVORS, wire.getFlavors());
				}
				if (wire.getScope() != null) {
					props.put(WA_WIRE_SCOPE, wire.getScope());
				}
				props.put(WA_WIRE_VALID, wire.isValid() ? Boolean.TRUE : Boolean.FALSE);
			}
			Throwable throwable = event.getThrowable();
			if (throwable != null) {
				addExceptionProps(props, throwable);
			}
			props.put(EVENT, event);
			final Event eaEvent = new Event(EVENT_TOPICS[topicIndex], (Dictionary) props);
			AccessController.doPrivileged(new PrivilegedAction() {
				public Object run() {
					eventAdmin.postEvent(eaEvent);
					return null;
				}
			});
			if (Activator.LOG_DEBUG)
				log.debug(0, 10018, event.toString(), null, false);
		}
	}

	/**
	 * Checks if a topic filter string matches the target topic string.
	 * 
	 * @param pattern
	 *            A topic filter like "company/product/*"
	 * @param topic
	 *            Target topic to be checked against like
	 *            "company/product/topicA"
	 * @return true if topicProperty matches topic, false if otherwise.
	 */
	protected static boolean matchTopic(String pattern, String topic) {
		// //two fast checks
		if (pattern.length() > topic.length())
			return false;
		if ("*".equals(pattern) || pattern.equals(topic))
			return true;

		// //assume pattern is not NULL ... or check!
		int index = pattern.indexOf(TOPIC_SEPARATOR);
		if (index == -1 || index == 0 || index == pattern.length() - 1) {
			// //syntax problem
			// //we have no '/' or starts with '/' or ends with '/'
			return false;
		}

		for (index = 0; index < pattern.length(); index++) {
			if (pattern.charAt(index) == '*') {
				// //wildcard!!!
				if (pattern.charAt(index - 1) != TOPIC_SEPARATOR)
					return false; // we have not '/' before '*'
				if (index != pattern.length() - 1)
					return false; // we have something after the '*'
				return true;
			}
			if (pattern.charAt(index) != topic.charAt(index))
				return false;
		}

		if (index != topic.length())
			return false;
		if (pattern.charAt(index - 1) == TOPIC_SEPARATOR)
			return false;
		return true;
	}

	synchronized void register() {
		unregister();

		Hashtable props = new Hashtable(3);
		props.put(WireConstants.WIREADMIN_EVENTS, new Integer(Integer.MAX_VALUE));
		waReg = bc.registerService(WireAdminListener.class.getName(), this, props);
	}

	synchronized void unregister() {
		if (waReg != null) {
			waReg.unregister();
			waReg = null;
		}
	}

	static class EventHandlerTopics {
		int topics;

		public EventHandlerTopics(final int topics) {
			this.topics = topics;
		}
	}

	class EventHandlerIndex implements ServiceTrackerCustomizer {

		final int[] eventHandlerCount = new int[EVENT_TOPICS.length];
		int totalEventHandlerCount = 0;

		public synchronized Object addingService(final ServiceReference reference) {

			final int topics = process(reference);

			if (topics != 0 && totalEventHandlerCount++ == 0) {
				register();
			}

			return new EventHandlerTopics(topics);
		}

		public void modifiedService(ServiceReference reference, Object service) {

			removedService(reference, service);

			final EventHandlerTopics newTopics = (EventHandlerTopics) addingService(reference);

			((EventHandlerTopics) service).topics = newTopics.topics;
		}

		public synchronized void removedService(ServiceReference reference, Object service) {

			final int topics = ((EventHandlerTopics) service).topics;

			if (topics == 0) {
				return;
			}

			remove(topics);

			if (--totalEventHandlerCount == 0) {
				unregister();
			}
		}

		int process(final ServiceReference ref) {

			final Object topic = ref.getProperty(EventConstants.EVENT_TOPIC);

			if (!(topic instanceof String[])) {
				return 0;
			}

			final String[] topics = (String[]) topic;

			int matchingTopics = 0;

			for (int i = 0; i < topics.length; i++) {
				matchingTopics |= processTopicPattern(topics[i]);
			}

			return matchingTopics;
		}

		int processTopicPattern(final String pattern) {
			int matchingTopics = 0;
			for (int i = 0; i < EVENT_TOPICS.length; i++) {
				if (matchTopic(pattern, EVENT_TOPICS[i])) {
					eventHandlerCount[i]++;
					matchingTopics |= 1 << i;
				}
			}
			return matchingTopics;
		}

		void remove(final int topics) {
			for (int i = 0; i < EVENT_TOPICS.length; i++) {
				if ((topics & (1 << i)) != 0) {
					eventHandlerCount[i]--;
				}
			}
		}

		boolean hasEventHandlers(final int topicIndex) {
			return eventHandlerCount[topicIndex] != 0;
		}

	}

}
