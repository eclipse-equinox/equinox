/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.useradmin;

import java.util.Dictionary;
import java.util.Hashtable;
import org.osgi.framework.*;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.useradmin.*;
import org.osgi.service.useradmin.Role;
import org.osgi.util.tracker.ServiceTracker;

public class UserAdminEventAdapter implements UserAdminListener {
	// constants for Event topic substring
	public static final String TOPIC = "org/osgi/service/useradmin/UserAdmin"; //$NON-NLS-1$
	public static final char TOPIC_SEPARATOR = '/';
	public static final String EVENT = "event"; //$NON-NLS-1$

	public static final String SERVICE = "service"; //$NON-NLS-1$
	public static final String SERVICE_ID = "service.id"; //$NON-NLS-1$
	public static final String SERVICE_OBJECTCLASS = "service.objectClass"; //$NON-NLS-1$
	public static final String SERVICE_PID = "service.pid"; //$NON-NLS-1$

	public static final String ROLE_CREATED = "ROLE_CREATED"; //$NON-NLS-1$
	public static final String ROLE_CHANGED = "ROLE_CHANGED"; //$NON-NLS-1$
	public static final String ROLE_REMOVED = "ROLE_REMOVED"; //$NON-NLS-1$
	// constants for Event properties
	public static final String ROLE = "role"; //$NON-NLS-1$
	public static final String ROLE_NAME = "role.name"; //$NON-NLS-1$
	public static final String ROLE_TYPE = "role.type"; //$NON-NLS-1$

	private BundleContext context;
	private ServiceRegistration userAdminRegistration;
	private volatile ServiceTracker eventAdminTracker;

	public UserAdminEventAdapter(BundleContext context) {
		this.context = context;
	}

	public void start() throws Exception {
		Hashtable props = new Hashtable(3);
		userAdminRegistration = context.registerService(UserAdminListener.class.getName(), this, props);

		eventAdminTracker = new ServiceTracker(context, EventAdmin.class.getName(), null);
		eventAdminTracker.open();
	}

	public void stop() throws Exception {
		ServiceTracker currentTracker = eventAdminTracker;
		if (currentTracker != null) {
			currentTracker.close();
			eventAdminTracker = null;
		}
		if (userAdminRegistration != null) {
			userAdminRegistration.unregister();
			userAdminRegistration = null;
		}
		this.context = null;
	}

	public void roleChanged(UserAdminEvent event) {
		ServiceTracker currentTracker = eventAdminTracker;
		EventAdmin eventAdmin = currentTracker == null ? null : ((EventAdmin) currentTracker.getService());
		if (eventAdmin != null) {
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
					return;
			}
			String topic = TOPIC + TOPIC_SEPARATOR + typename;
			Hashtable properties = new Hashtable();
			ServiceReference ref = event.getServiceReference();
			if (ref == null) {
				throw new RuntimeException("UserAdminEvent's getServiceReference() returns null."); //$NON-NLS-1$
			}
			putServiceReferenceProperties(properties, ref);
			Role role = event.getRole();
			if (role == null) {
				throw new RuntimeException("UserAdminEvent's getRole() returns null."); //$NON-NLS-1$
			}
			if (role != null) {
				properties.put(ROLE, role);
				properties.put(ROLE_NAME, role.getName());
				properties.put(ROLE_TYPE, new Integer(role.getType()));
			}
			properties.put(EVENT, event);
			Event convertedEvent = new Event(topic, (Dictionary) properties);
			eventAdmin.postEvent(convertedEvent);
		}
	}

	public void putServiceReferenceProperties(Hashtable properties, ServiceReference ref) {
		properties.put(SERVICE, ref);
		properties.put(SERVICE_ID, ref.getProperty(org.osgi.framework.Constants.SERVICE_ID));
		Object o = ref.getProperty(org.osgi.framework.Constants.SERVICE_PID);
		if ((o != null) && (o instanceof String)) {
			properties.put(SERVICE_PID, o);
		}
		Object o2 = ref.getProperty(org.osgi.framework.Constants.OBJECTCLASS);
		if ((o2 != null) && (o2 instanceof String[])) {
			properties.put(SERVICE_OBJECTCLASS, o2);
		}
	}

}