/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.cm;

import java.util.Dictionary;
import java.util.Hashtable;
import org.osgi.framework.*;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.util.tracker.ServiceTracker;

public class ConfigurationEventAdapter implements ConfigurationListener {
	// constants for Event topic substring
	public static final String TOPIC = "org/osgi/service/cm/ConfigurationEvent"; //$NON-NLS-1$
	public static final char TOPIC_SEPARATOR = '/';
	// constants for Event types
	public static final String CM_UPDATED = "CM_UPDATED"; //$NON-NLS-1$
	public static final String CM_DELETED = "CM_DELETED"; //$NON-NLS-1$
	// constants for Event properties
	public static final String CM_FACTORY_PID = "cm.factoryPid"; //$NON-NLS-1$
	public static final String CM_PID = "cm.pid"; //$NON-NLS-1$
	public static final String SERVICE = "service"; //$NON-NLS-1$
	public static final String SERVICE_ID = "service.id"; //$NON-NLS-1$
	public static final String SERVICE_OBJECTCLASS = "service.objectClass"; //$NON-NLS-1$
	public static final String SERVICE_PID = "service.pid"; //$NON-NLS-1$

	private final BundleContext context;
	private ServiceRegistration configListenerRegistration;
	private final ServiceTracker eventAdminTracker;

	public ConfigurationEventAdapter(BundleContext context) {
		this.context = context;
		eventAdminTracker = new ServiceTracker(context, EventAdmin.class.getName(), null);
	}

	public void start() throws Exception {
		eventAdminTracker.open();
		configListenerRegistration = context.registerService(ConfigurationListener.class.getName(), this, null);
	}

	public void stop() throws Exception {
		configListenerRegistration.unregister();
		configListenerRegistration = null;
		eventAdminTracker.close();
	}

	public void configurationEvent(ConfigurationEvent event) {
		EventAdmin eventAdmin = (EventAdmin) eventAdminTracker.getService();
		if (eventAdmin == null) {
			return;
		}
		String typename = null;
		switch (event.getType()) {
			case ConfigurationEvent.CM_UPDATED :
				typename = CM_UPDATED;
				break;
			case ConfigurationEvent.CM_DELETED :
				typename = CM_DELETED;
				break;
			default : // do nothing
				return;
		}
		String topic = TOPIC + TOPIC_SEPARATOR + typename;
		ServiceReference ref = event.getReference();
		if (ref == null) {
			throw new RuntimeException("ServiceEvent.getServiceReference() is null"); //$NON-NLS-1$
		}
		Hashtable properties = new Hashtable();
		properties.put(CM_PID, event.getPid());
		if (event.getFactoryPid() != null) {
			properties.put(CM_FACTORY_PID, event.getFactoryPid());
		}
		putServiceReferenceProperties(properties, ref);
		Event convertedEvent = new Event(topic, (Dictionary) properties);
		eventAdmin.postEvent(convertedEvent);
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
