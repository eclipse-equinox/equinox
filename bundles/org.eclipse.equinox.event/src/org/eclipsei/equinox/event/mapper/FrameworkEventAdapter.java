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
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkEvent;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * @version $Revision: 1.4 $
 */
public class FrameworkEventAdapter extends EventAdapter {
	// constants for Event topic substring
	public static final String	HEADER				= "org/osgi/framework/FrameworkEvent";
	public static final String	STARTLEVEL_CHANGED	= "STARTLEVEL_CHANGED";
	public static final String	STARTED				= "STARTED";
	public static final String	PACKAGES_REFRESHED	= "PACKAGES_REFRESHED";
	public static final String	ERROR				= "ERROR";
	protected FrameworkEvent	event;

	public FrameworkEventAdapter(FrameworkEvent event, EventAdmin eventAdmin) {
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
			case FrameworkEvent.ERROR :
				typename = ERROR;
				break;
			case FrameworkEvent.PACKAGES_REFRESHED :
				typename = PACKAGES_REFRESHED;
				break;
			case FrameworkEvent.STARTED :
				typename = STARTED;
				break;
			case FrameworkEvent.STARTLEVEL_CHANGED :
				typename = STARTLEVEL_CHANGED;
				break;
			default :
				return null;
		}
		String topic = HEADER + Constants.TOPIC_SEPARATOR + typename;
		Hashtable properties = new Hashtable();
		Bundle bundle = event.getBundle();
		if (bundle != null) {
			putBundleProperties(properties, bundle);
		}
		Throwable t = event.getThrowable();
		if (t != null) {
			putExceptionProperties(properties, t);
		}
		properties.put(Constants.EVENT, event);
		Event converted = new Event(topic, properties);
		return converted;
	}
}