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
import org.osgi.framework.BundleEvent;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * @version $Revision: 1.3 $
 */
public class BundleEventAdapter extends EventAdapter {
	// constants for Event topic substring
	public static final String HEADER = "org/osgi/framework/BundleEvent"; //$NON-NLS-1$
	public static final String INSTALLED = "INSTALLED"; //$NON-NLS-1$
	public static final String STOPPED = "STOPPED"; //$NON-NLS-1$
	public static final String STARTED = "STARTED"; //$NON-NLS-1$
	public static final String UPDATED = "UPDATED"; //$NON-NLS-1$
	public static final String UNINSTALLED = "UNINSTALLED"; //$NON-NLS-1$
	public static final String RESOLVED = "RESOLVED"; //$NON-NLS-1$
	public static final String UNRESOLVED = "UNRESOLVED"; //$NON-NLS-1$
	private BundleEvent event;

	public BundleEventAdapter(BundleEvent event, EventAdmin eventAdmin) {
		super(eventAdmin);
		this.event = event;
	}

	/**
	 * @return event
	 * @see org.eclipse.equinox.internal.event.mapper.EventAdapter#convert()
	 */
	public Event convert() {
		String typename = null;
		switch (event.getType()) {
			case BundleEvent.INSTALLED :
				typename = INSTALLED;
				break;
			case BundleEvent.STOPPED :
				typename = STOPPED;
				break;
			case BundleEvent.STARTED :
				typename = STARTED;
				break;
			case BundleEvent.UPDATED :
				typename = UPDATED;
				break;
			case BundleEvent.UNINSTALLED :
				typename = UNINSTALLED;
				break;
			case BundleEvent.RESOLVED :
				typename = RESOLVED;
				break;
			case BundleEvent.UNRESOLVED :
				typename = UNRESOLVED;
				break;
			default :
				// unknown events must be send as their decimal value
				typename = Integer.toString(event.getType());
		}
		String topic = HEADER + Constants.TOPIC_SEPARATOR + typename;
		Map<String, Object> properties = new HashMap<String, Object>();
		Bundle bundle = event.getBundle();
		if (bundle == null) {
			throw new RuntimeException("BundleEvent.getBundle() returns null"); //$NON-NLS-1$
		}
		putBundleProperties(properties, bundle);
		properties.put(Constants.EVENT, event);
		Event converted = new Event(topic, properties);
		return converted;
	}
}