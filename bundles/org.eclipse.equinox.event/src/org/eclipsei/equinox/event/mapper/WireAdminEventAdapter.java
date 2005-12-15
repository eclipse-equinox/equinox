/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipsei.equinox.event.mapper;

import java.util.Hashtable;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.wireadmin.Wire;
import org.osgi.service.wireadmin.WireAdminEvent;

/**
 * @version $Revision: 1.4 $
 */
public class WireAdminEventAdapter extends EventAdapter {
	// constants for Event topic substring
	public static final String TOPIC = "org/osgi/service/wireadmin/WireAdminEvent";
	public static final String WIRE_CREATED = "WIRE_CREATED";
	public static final String WIRE_CONNECTED_T = "WIRE_CONNECTED";
	public static final String WIRE_UPDATED = "WIRE_UPDATED";
	public static final String WIRE_TRACE = "WIRE_TRACE";
	public static final String WIRE_DISCONNECTED = "WIRE_DISCONNECTED";
	public static final String WIRE_DELETED = "WIRE_DELETED";
	public static final String PRODUCER_EXCEPTION = "PRODUCER_EXCEPTION";
	public static final String CONSUMER_EXCEPTION = "CONSUMER_EXCEPTION";
	// constants for Event properties
	public static final String WIRE = "wire";
	public static final String WIRE_FLAVORS = "wire.flavors";
	public static final String WIRE_SCOPE = "wire.scope";
	public static final String WIRE_CONNECTED_P = "wire.connected";
	public static final String WIRE_VALID = "wire.valid";
	private WireAdminEvent event;

	public WireAdminEventAdapter(WireAdminEvent event, EventAdmin admin) {
		super(admin);
		this.event = event;
	}

	/**
	 * @return
	 * @see org.eclipse.equinox.event.mapper.EventAdapter#convert()
	 */
	public Event convert() {
		String typename = null;
		switch (event.getType()) {
			case WireAdminEvent.WIRE_CONNECTED :
				typename = WIRE_CONNECTED_T;
				break;
			case WireAdminEvent.WIRE_CREATED :
				typename = WIRE_CREATED;
				break;
			case WireAdminEvent.WIRE_UPDATED :
				typename = WIRE_UPDATED;
				break;
			case WireAdminEvent.WIRE_DELETED :
				typename = WIRE_DELETED;
				break;
			case WireAdminEvent.WIRE_DISCONNECTED :
				typename = WIRE_DISCONNECTED;
				break;
			case WireAdminEvent.WIRE_TRACE :
				typename = WIRE_TRACE;
				break;
			case WireAdminEvent.PRODUCER_EXCEPTION :
				typename = PRODUCER_EXCEPTION;
				break;
			case WireAdminEvent.CONSUMER_EXCEPTION :
				typename = CONSUMER_EXCEPTION;
				break;
			default :
				return null;
		}
		String topic = TOPIC + Constants.TOPIC_SEPARATOR + typename;
		Hashtable properties = new Hashtable();
		Throwable t = event.getThrowable();
		if (t != null) {
			putExceptionProperties(properties, t);
		}
		ServiceReference ref = event.getServiceReference();
		if (ref == null) {
			throw new RuntimeException("WireAdminEvent's getServiceReference() returns null.");
		}
		putServiceReferenceProperties(properties, ref);
		Wire wire = event.getWire();
		if (wire != null) {
			properties.put(WIRE, wire);
			if (wire.getFlavors() != null) {
				properties.put(WIRE_FLAVORS, classes2strings(wire.getFlavors()));
			}
			if (wire.getScope() != null) {
				properties.put(WIRE_SCOPE, wire.getScope());
			}
			properties.put(WIRE_CONNECTED_P, new Boolean(wire.isConnected()));
			properties.put(WIRE_VALID, new Boolean(wire.isValid()));
		}
		properties.put(Constants.EVENT, event);
		Event converted = new Event(topic, properties);
		return converted;
	}
}