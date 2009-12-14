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
	static final String WIRE_ENTRY = "wire.entry";
	static final String WA_WIRE = "wire";
	static final String WA_WIRE_FLAVORS = "wire.flavors";
	static final String WA_WIRE_SCOPE = "wire.scope";
	static final String WA_WIRE_CONNECTED = "wire.connected";
	static final String WA_WIRE_VALID = "wire.valid";

	BundleContext bc;
	ServiceRegistration waReg;
	Log log;
	ServiceTracker eventAdminTracker;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bc) throws Exception {
		this.bc = bc;
		log = new Log(bc, false);
		log.setDebug(Activator.getBoolean("equinox.wireadmin.redispatcher.debug"));
		log.setPrintOnConsole(Activator.getBoolean("equinox.wireadmin.redispatcher.console"));

		Hashtable props = new Hashtable(3);
		props.put(WireConstants.WIREADMIN_EVENTS, new Integer(Integer.MAX_VALUE));
		waReg = bc.registerService(WireAdminListener.class.getName(), this, props);

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
		if (waReg != null) {
			waReg.unregister();
			waReg = null;
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
			String topicSuffix = null;
			switch (event.getType()) {
				case WireAdminEvent.WIRE_CREATED :
					topicSuffix = WIRE_CREATED;
					break;
				case WireAdminEvent.WIRE_CONNECTED :
					topicSuffix = WIRE_CONNECTED;
					break;
				case WireAdminEvent.WIRE_UPDATED :
					topicSuffix = WIRE_UPDATED;
					break;
				case WireAdminEvent.WIRE_TRACE :
					topicSuffix = WIRE_TRACE;
					break;
				case WireAdminEvent.WIRE_DISCONNECTED :
					topicSuffix = WIRE_DISCONNECTED;
					break;
				case WireAdminEvent.WIRE_DELETED :
					topicSuffix = WIRE_DELETED;
					break;
				case WireAdminEvent.PRODUCER_EXCEPTION :
					topicSuffix = PRODUCER_EXCEPTION;
					break;
				case WireAdminEvent.CONSUMER_EXCEPTION :
					topicSuffix = CONSUMER_EXCEPTION;
					break;
				default : /* ignore: unknown/new events */
					return;
			}
			String topic = WIRE_HEADER + TOPIC_SEPARATOR + topicSuffix;
			if (!hasServiceReferences(topic)) {
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
				props.put(WA_WIRE_CONNECTED, new Boolean(wire.isConnected()));
				if (wire.getFlavors() != null) {
					props.put(WA_WIRE_FLAVORS, wire.getFlavors());
				}
				if (wire.getScope() != null) {
					props.put(WA_WIRE_SCOPE, wire.getScope());
				}
				props.put(WA_WIRE_VALID, new Boolean(wire.isValid()));
			}
			Throwable throwable = event.getThrowable();
			if (throwable != null) {
				addExceptionProps(props, throwable);
			}
			props.put(EVENT, event);
			final Event eaEvent = new Event(topic, (Dictionary) props);
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
	 * This will return true if has at least one ServiceReference which match
	 * given topic.
	 * 
	 * @param topic
	 * @return
	 */
	protected boolean hasServiceReferences(String topic) {
		BundleContext l_bc = bc;
		if (l_bc == null) {
			return false;
		}
		ServiceReference[] sr = null;
		try {/* get all handlers */
			sr = l_bc.getServiceReferences(EventHandler.class.getName(), null);
		} catch (InvalidSyntaxException e) {
			return false;
		}
		if (sr != null && sr.length > 0) {
			TopicPermission perm = new TopicPermission(topic, TopicPermission.SUBSCRIBE);
			for (int i = 0; i < sr.length; i++) {
				try {
					ServiceReference sRef = sr[i];
					Bundle bundle = sRef.getBundle();
					if (bundle != null && (bundle.getState() != Bundle.UNINSTALLED) && bundle.hasPermission(perm)) {
						Object reftopic = sRef.getProperty(EventConstants.EVENT_TOPIC);
						if (reftopic != null) { /*
																											 * otherwise means will receive
																											 * no events
																											 */
							if (reftopic instanceof String[]) { /*
																																		 * even with one
																																		 * element it
																																		 * must be
																																		 * String[]
																																		 */
								String topics[] = (String[]) reftopic;
								for (int j = 0; j < topics.length; j++) {
									if (matchTopic(topics[j], topic)) {
										return true;
									}
								}
							}
						}
					}/* check permission */
				} catch (Throwable t) {
					log.error("Error while checking bundle permissions", t);
				}
			}/* for */
		}/* sr != null && sr.length > 0 */
		return false;
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

}
