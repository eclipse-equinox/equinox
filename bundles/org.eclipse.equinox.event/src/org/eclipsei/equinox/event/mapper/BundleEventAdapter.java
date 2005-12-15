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
import org.osgi.framework.BundleEvent;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * @version $Revision: 1.4 $
 */
public class BundleEventAdapter extends EventAdapter {
	// constants for Event topic substring
	public static final String	HEADER		= "org/osgi/framework/BundleEvent";
	public static final String	INSTALLED	= "INSTALLED";
	public static final String	STOPPED		= "STOPPED";
	public static final String	STARTED		= "STARTED";
	public static final String	UPDATED		= "UPDATED";
	public static final String	UNINSTALLED	= "UNINSTALLED";
	public static final String	RESOLVED	= "RESOLVED";
	public static final String	UNRESOLVED	= "UNRESOLVED";
	private BundleEvent			event;

	public BundleEventAdapter(BundleEvent event, EventAdmin eventAdmin) {
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
				typename = ""+event.getType();
		}
		String topic = HEADER + Constants.TOPIC_SEPARATOR + typename;
		Hashtable properties = new Hashtable();
		Bundle bundle = event.getBundle();
		if (bundle == null) {
			throw new RuntimeException("BundleEvent.getBundle() returns null");
		}
		else {
			putBundleProperties(properties, bundle);
		}
		properties.put(Constants.EVENT, event);
		Event converted = new Event(topic, properties);
		return converted;
	}
}