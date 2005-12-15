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

import java.util.Dictionary;
import java.util.Hashtable;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * @version $Revision: 1.4 $
 */
public class UPnPEventAdapter extends EventAdapter {
	// constants for Event topic substring
	public static final String	TOPIC			= "org/osgi/service/upnp/UPnPEvent";
	// constants for Event properties
	public static final String	UPNP_DEVICEID	= "upnp.deviceId";
	public static final String	UPNP_SERVICEID	= "upnp.serviceId";
	public static final String	UPNP_EVENTS		= "upnp.events";
	private String				deviceId;
	private String				serviceId;
	private Dictionary			events;

	public UPnPEventAdapter(String deviceId, String serviceId,
			Dictionary events, EventAdmin eventAdmin) {
		super(eventAdmin);
		this.deviceId = deviceId;
		this.serviceId = serviceId;
		this.events = events;
	}

	/**
	 * @return
	 * @see org.eclipse.equinox.event.mapper.EventAdapter#convert()
	 */
	public Event convert() {
		Hashtable properties = new Hashtable();
		if (deviceId != null) {
			properties.put(UPNP_DEVICEID, deviceId);
		}
		if (serviceId != null) {
			properties.put(UPNP_SERVICEID, serviceId);
		}
		if (events != null) {
			properties.put(UPNP_EVENTS, events);
		}
		Event converted = new Event(TOPIC, properties);
		return converted;
	}
}