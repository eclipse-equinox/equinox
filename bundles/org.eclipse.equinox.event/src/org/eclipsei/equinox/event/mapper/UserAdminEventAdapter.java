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
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdminEvent;

/**
 * @version $Revision: 1.4 $
 */
public class UserAdminEventAdapter extends EventAdapter {
	// constants for Event topic substring
	public static final String	TOPIC			= "org/osgi/service/useradmin/UserAdminEvent";
	public static final String	ROLE_CREATED	= "ROLE_CREATED";
	public static final String	ROLE_CHANGED	= "ROLE_CHANGED";
	public static final String	ROLE_REMOVED	= "ROLE_REMOVED";
	// constants for Event properties
	public static final String	ROLE			= "role";
	public static final String	ROLE_NAME		= "role.name";
	public static final String	ROLE_TYPE		= "role.type";
	private UserAdminEvent		event;

	public UserAdminEventAdapter(UserAdminEvent event, EventAdmin eventAdmin) {
		super(eventAdmin);
		this.event = event;
	}

	/**
	 * @return
	 * @see org.eclipse.equinox.event.mapper.EventAdapter#convert()
	 */
	public Event convert() {
		String typename = null;
		switch (event.getType()) {
			case UserAdminEvent.ROLE_CREATED :
				typename = ROLE_CREATED;
				break;
			case UserAdminEvent.ROLE_CHANGED :
				typename = ROLE_CHANGED;
				break;
			case UserAdminEvent.ROLE_REMOVED :
				typename = ROLE_REMOVED;
				break;
			default :
				return null;
		}
		String topic = TOPIC + Constants.TOPIC_SEPARATOR + typename;
		Hashtable properties = new Hashtable();
		ServiceReference ref = event.getServiceReference();
		if (ref == null) {
			throw new RuntimeException(
					"UserAdminEvent's getServiceReference() returns null.");
		}
		putServiceReferenceProperties(properties, ref);
		Role role = event.getRole();
		if (role == null) {
			throw new RuntimeException(
					"UserAdminEvent's getRole() returns null.");
		}
		if (role != null) {
			properties.put(ROLE, role);
			properties.put(ROLE_NAME, role.getName());
			properties.put(ROLE_TYPE, new Integer(role.getType()));
		}
		properties.put(Constants.EVENT, event);
		Event converted = new Event(topic, properties);
		return converted;
	}
}