/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.cm;

import java.util.Dictionary;
import java.util.Hashtable;
import org.osgi.framework.*;
import org.osgi.service.cm.*;
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
	private ServiceRegistration<ConfigurationListener> configListenerRegistration;
	private final ServiceTracker<EventAdmin, EventAdmin> eventAdminTracker;

	public ConfigurationEventAdapter(BundleContext context) {
		this.context = context;
		eventAdminTracker = new ServiceTracker<>(context, EventAdmin.class, null);
	}

	public void start() throws Exception {
		eventAdminTracker.open();
		configListenerRegistration = context.registerService(ConfigurationListener.class, this, null);
	}

	public void stop() throws Exception {
		configListenerRegistration.unregister();
		configListenerRegistration = null;
		eventAdminTracker.close();
	}

	@Override
	public void configurationEvent(ConfigurationEvent event) {
		EventAdmin eventAdmin = eventAdminTracker.getService();
		if (eventAdmin == null) {
			return;
		}
		String typename = null;
		switch (event.getType()) {
		case ConfigurationEvent.CM_UPDATED:
			typename = CM_UPDATED;
			break;
		case ConfigurationEvent.CM_DELETED:
			typename = CM_DELETED;
			break;
		default: // do nothing
			return;
		}
		String topic = TOPIC + TOPIC_SEPARATOR + typename;
		ServiceReference<ConfigurationAdmin> ref = event.getReference();
		if (ref == null) {
			throw new RuntimeException("ServiceEvent.getServiceReference() is null"); //$NON-NLS-1$
		}
		Hashtable<String, Object> properties = new Hashtable<>();
		properties.put(CM_PID, event.getPid());
		if (event.getFactoryPid() != null) {
			properties.put(CM_FACTORY_PID, event.getFactoryPid());
		}
		putServiceReferenceProperties(properties, ref);
		Event convertedEvent = new Event(topic, (Dictionary<String, Object>) properties);
		eventAdmin.postEvent(convertedEvent);
	}

	private void putServiceReferenceProperties(Hashtable<String, Object> properties, ServiceReference<?> ref) {
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
