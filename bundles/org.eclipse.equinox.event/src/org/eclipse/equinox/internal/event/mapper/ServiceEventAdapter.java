/*******************************************************************************
 * Copyright (c) 2005, 2018 IBM Corporation.
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

package org.eclipse.equinox.internal.event.mapper;

import java.util.HashMap;
import java.util.Map;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * @version $Revision: 1.3 $
 */
public class ServiceEventAdapter extends EventAdapter {
	// constants for Event topic substring
	public static final String HEADER = "org/osgi/framework/ServiceEvent"; //$NON-NLS-1$
	public static final String UNREGISTERING = "UNREGISTERING"; //$NON-NLS-1$
	public static final String MODIFIED = "MODIFIED"; //$NON-NLS-1$
	public static final String REGISTERED = "REGISTERED"; //$NON-NLS-1$
	private ServiceEvent event;

	public ServiceEventAdapter(ServiceEvent event, EventAdmin eventAdmin) {
		super(eventAdmin);
		this.event = event;
	}

	@Override
	public Event convert() {
		String typename = null;
		switch (event.getType()) {
		case ServiceEvent.REGISTERED:
			typename = REGISTERED;
			break;
		case ServiceEvent.MODIFIED:
			typename = MODIFIED;
			break;
		case ServiceEvent.UNREGISTERING:
			typename = UNREGISTERING;
			break;
		default:
			return null;
		}
		String topic = HEADER + Constants.TOPIC_SEPARATOR + typename;
		Map<String, Object> properties = new HashMap<>();
		ServiceReference<?> ref = event.getServiceReference();
		if (ref != null) {
			putServiceReferenceProperties(properties, ref);
		}
		properties.put(Constants.EVENT, event);
		Event converted = new Event(topic, properties);
		return converted;
	}
}
