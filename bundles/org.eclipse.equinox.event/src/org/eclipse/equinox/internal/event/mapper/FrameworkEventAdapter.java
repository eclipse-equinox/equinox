/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.event.mapper;

import java.util.HashMap;
import java.util.Map;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkEvent;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * @version $Revision: 1.3 $
 */
public class FrameworkEventAdapter extends EventAdapter {
	// constants for Event topic substring
	public static final String HEADER = "org/osgi/framework/FrameworkEvent"; //$NON-NLS-1$
	public static final String STARTLEVEL_CHANGED = "STARTLEVEL_CHANGED"; //$NON-NLS-1$
	public static final String STARTED = "STARTED"; //$NON-NLS-1$
	public static final String PACKAGES_REFRESHED = "PACKAGES_REFRESHED"; //$NON-NLS-1$
	public static final String ERROR = "ERROR"; //$NON-NLS-1$
	protected FrameworkEvent event;

	public FrameworkEventAdapter(FrameworkEvent event, EventAdmin eventAdmin) {
		super(eventAdmin);
		this.event = event;
	}

	/**
	 * @see org.eclipse.equinox.internal.event.mapper.EventAdapter#convert()
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
		Map<String, Object> properties = new HashMap<String, Object>();
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