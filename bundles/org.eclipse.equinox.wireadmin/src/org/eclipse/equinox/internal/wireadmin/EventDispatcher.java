/*******************************************************************************
 * Copyright (c) 1997, 2008 by ProSyst Software GmbH
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

import java.util.*;
import org.osgi.framework.*;
import org.osgi.service.wireadmin.*;

/**
 * This class is responsible for dispatching notifications to WireAdminListeners
 * and Consumers and Producers.
 * 
 * @author Stoyan Boshev
 * @author Pavlin Dobrev
 * 
 * @version 1.0
 */
public class EventDispatcher implements Runnable {

	private BundleContext bc;

	private WireAdminImpl wa;

	private Hashtable refToList;

	private Vector events;

	private Object synch = new Object();
	private Object listenersLock = new Object();

	private boolean running = true;

	private Thread dispatcher;

	public EventDispatcher(BundleContext bc, WireAdminImpl wa) {
		this.bc = bc;
		this.wa = wa;
		this.refToList = new Hashtable(5);
		this.events = new Vector(5, 5);

		ServiceReference[] sRefs = null;

		try {
			sRefs = bc.getServiceReferences(WireAdminListener.class.getName(), null);
		} catch (InvalidSyntaxException ise) {
			/* filter is null */
		}

		if (sRefs != null) {
			WireAdminListener listener;

			for (int i = 0; i < sRefs.length; i++) {
				listener = (WireAdminListener) bc.getService(sRefs[i]);

				if (listener != null) {
					refToList.put(sRefs[i], listener);
				}
			}
		}
		dispatcher = new Thread(this, "[WireAdmin] - Event Dispatcher");
		dispatcher.start();
	}

	void addEvent(WireAdminEvent evt) {
		if (refToList.isEmpty()) {
			return;
		}

		if (dispatcher == null) {
			// synchronous
			notifyListeners(new EventData(evt, refToList));
		} else {
			// synchronized (listenersLock) { //because it does not change the
			// Hashtable;
			events.addElement(new EventData(evt, refToList));
			// }
			synchronized (synch) {
				synch.notify();
			}
		}
	}

	void addNotificationEvent(NotificationEvent ne) {
		if (dispatcher == null) {
			// synchronous
			notifyConsumerProducer(ne);
		} else {
			events.addElement(ne);
			synchronized (synch) {
				synch.notify();
			}
		}
	}

	private String printWires(Wire[] wires) {
		if (wires != null) {
			StringBuffer buff = new StringBuffer(100);
			buff.append("\n");
			for (int i = 0; i < wires.length; i++) {
				buff.append(wires[i]).append("\n");
			}
			return buff.toString();
		}
		return "null";
	}

	private void notifyConsumerProducer(NotificationEvent ne) {
		if (Activator.LOG_DEBUG) {
			Activator.log.debug("Notification event " + ((ne.producer != null) ? "; Producer " + ne.producer : "; Consumer " + ne.consumer) + "; source: " + ne.source + "; wires " + printWires(ne.wires), null);

			Activator.log.debug(0, 10001, ((dispatcher != null) ? "asynchronous" : "synchronous"), null, false);
		}
		if (ne.producer != null) {
			try {
				ne.producer.consumersConnected(ne.wires);
			} catch (Throwable t) {
				wa.notifyListeners(ne.source, WireAdminEvent.PRODUCER_EXCEPTION, t);
			}
		} else if (ne.consumer != null) {
			try {
				ne.consumer.producersConnected(ne.wires);
			} catch (Throwable t) {
				wa.notifyListeners(ne.source, WireAdminEvent.CONSUMER_EXCEPTION, t);
			}
		}
	}

	private void notifyListeners(EventData event) {
		WireAdminEvent evt = (WireAdminEvent) event.event;
		Hashtable refToList = event.listeners;
		if (Activator.LOG_DEBUG) {
			Activator.log.debug(0, 10002, getEvent(evt.getType()) + evt.getWire(), evt.getThrowable(), false);
			Activator.log.debug(0, 10001, ((dispatcher != null) ? "asynchronous" : "synchronous"), null, false);
		}

		for (Enumeration en = refToList.keys(); running && en.hasMoreElements();) {
			ServiceReference current = (ServiceReference) en.nextElement();
			Integer accepts = (Integer) current.getProperty(WireConstants.WIREADMIN_EVENTS);
			if ((accepts != null) && ((accepts.intValue() & evt.getType()) == evt.getType())) {
				try {
					((WireAdminListener) refToList.get(current)).wireAdminEvent(evt);
				} catch (Throwable t) {
					if (Activator.LOG_DEBUG) {
						Activator.log.debug(0, 10003, ((WireAdminListener) refToList.get(current)).toString(), t, false);
					}
				}
			}
		}
	}

	/**
	 * @param ref
	 */
	public void removeListener(ServiceReference ref) {
		if (refToList.containsKey(ref)) {
			synchronized (listenersLock) {
				refToList = (Hashtable) refToList.clone();
				if (refToList.remove(ref) != null) {
					bc.ungetService(ref);
				}
			}
		}
	}

	/**
	 * @param ref
	 * @param object
	 */
	public void addListener(ServiceReference ref, Object object) {
		synchronized (listenersLock) {
			refToList = (Hashtable) refToList.clone();
			refToList.put(ref, object);
		}
	}

	public void run() {
		while (running) {
			synchronized (synch) {
				while (running && events.size() == 0) {
					try {
						synch.wait();
					} catch (InterruptedException ie) {
					}
				}
			}

			EventData evt = null;
			NotificationEvent ne = null;
			while (running && events.size() > 0) {
				Object event = events.elementAt(0);
				events.removeElementAt(0);
				if (event instanceof EventData) {
					evt = (EventData) event;
					notifyListeners(evt);
				} else {
					ne = (NotificationEvent) event;
					notifyConsumerProducer(ne);
				}
			}
		}
	}

	void terminate() {
		running = false;

		if (dispatcher != null) {
			synchronized (synch) {
				synch.notify();
			}
		}

		synchronized (listenersLock) {
			for (Enumeration en = refToList.keys(); en.hasMoreElements();) {
				bc.ungetService((ServiceReference) en.nextElement());
			}
			refToList.clear();
			refToList = null;
		}
		events.removeAllElements();
		events = null;
	}

	private String getEvent(int type) {
		switch (type) {
			case WireAdminEvent.WIRE_CREATED :
				return "WIRE_CREATED";
			case WireAdminEvent.WIRE_CONNECTED :
				return "WIRE_CONNECTED";
			case WireAdminEvent.WIRE_UPDATED :
				return "WIRE_UPDATED";
			case WireAdminEvent.WIRE_TRACE :
				return "WIRE_TRACE";
			case WireAdminEvent.WIRE_DISCONNECTED :
				return "WIRE_DISCONNECTED";
			case WireAdminEvent.WIRE_DELETED :
				return "WIRE_DELETED";
			case WireAdminEvent.PRODUCER_EXCEPTION :
				return "PRODUCER_EXCEPTION";
			case WireAdminEvent.CONSUMER_EXCEPTION :
				return "CONSUMER_EXCEPTION";
			default :
				return null;
		}
	}

	class EventData {
		Object event;
		Hashtable listeners;

		public EventData(Object event, Hashtable listenersData) {
			this.event = event;
			listeners = listenersData;
		}
	}
}
