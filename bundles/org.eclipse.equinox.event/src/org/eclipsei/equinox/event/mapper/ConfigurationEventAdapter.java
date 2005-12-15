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
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * @version $Revision: 1.4 $
 */
public class ConfigurationEventAdapter extends EventAdapter {
	// constants for Event topic substring
	public static final String	HEADER			= "org/osgi/service/cm/ConfigurationEvent";
	public static final String	CM_UPDATED		= "CM_UPDATED";
	public static final String	CM_DELETED		= "CM_DELETED";
	// constants for Event properties
	public static final String	CM_FACTORY_PID	= "cm.factoryPid";
	public static final String	CM_PID			= "cm.pid";
	private ConfigurationEvent	event;

	public ConfigurationEventAdapter(ConfigurationEvent event,
			EventAdmin eventAdmin) {
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
			case ConfigurationEvent.CM_UPDATED :
				typename = CM_UPDATED;
				break;
			case ConfigurationEvent.CM_DELETED :
				typename = CM_DELETED;
				break;
			default :
				return null;
		}
		String topic = HEADER + Constants.TOPIC_SEPARATOR + typename;
		Hashtable properties = new Hashtable();
		ServiceReference ref = event.getReference();
		if (ref == null) {
			throw new RuntimeException(
					"ServiceEvent.getServiceReference() is null");
		}
		properties.put(CM_PID, event.getPid());
		if (event.getFactoryPid() != null) {
			properties.put(CM_FACTORY_PID, event.getFactoryPid());
		}
		putServiceReferenceProperties(properties, ref);
		// assert objectClass includes
		// "org.osgi.service.cm.ConfigurationAdmin"
		Event converted = new Event(topic, properties);
		return converted;
	}
}