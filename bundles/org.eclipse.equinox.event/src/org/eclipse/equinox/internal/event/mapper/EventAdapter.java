/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.event.mapper;

import java.util.Map;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * @version $Revision: 1.4 $
 */
public abstract class EventAdapter {
	final EventAdmin eventAdmin;

	/**
	 * @param eventAdmin
	 */
	public EventAdapter(EventAdmin eventAdmin) {
		this.eventAdmin = eventAdmin;
	}

	/**
	 * @return event
	 */
	public abstract Event convert();

	public void redeliver() {
		Event converted = convert();
		if (converted != null) {
			redeliverInternal(converted);
		}
	}

	/**
	 * subclasses should override this method if it wants to use sendEvent()
	 * instead.
	 */
	protected void redeliverInternal(Event converted) {
		eventAdmin.postEvent(converted);
	}

	public void putBundleProperties(Map<String, Object> properties, Bundle bundle) {
		// assertion bundle != null
		properties.put(Constants.BUNDLE_ID, new Long(bundle.getBundleId()));
		String symbolicName = bundle.getSymbolicName();
		if (symbolicName != null) {
			properties.put(Constants.BUNDLE_SYMBOLICNAME, symbolicName);
		}
		properties.put(Constants.BUNDLE, bundle);
	}

	public void putExceptionProperties(Map<String, Object> properties, Throwable t) {
		// assertion t != null
		properties.put(Constants.EXCEPTION, t);
		properties.put(Constants.EXCEPTION_CLASS, t.getClass().getName());
		String message = t.getMessage();
		if (message != null) {
			properties.put(Constants.EXCEPTION_MESSAGE, t.getMessage());
		}
	}

	public void putServiceReferenceProperties(Map<String, Object> properties, ServiceReference<?> ref) {
		// assertion ref != null
		properties.put(Constants.SERVICE, ref);
		properties.put(Constants.SERVICE_ID, ref.getProperty(org.osgi.framework.Constants.SERVICE_ID));
		Object o = ref.getProperty(org.osgi.framework.Constants.SERVICE_PID);
		if ((o != null) && (o instanceof String)) {
			properties.put(Constants.SERVICE_PID, o);
		}
		Object o2 = ref.getProperty(org.osgi.framework.Constants.OBJECTCLASS);
		if ((o2 != null) && (o2 instanceof String[])) {
			properties.put(Constants.SERVICE_OBJECTCLASS, o2);
		}
	}

	/*
	 * Utility function for converting classes into strings
	 */
	public String[] classes2strings(Class<?>[] classes) {
		if ((classes == null) || (classes.length == 0))
			return null;
		String[] strings = new String[classes.length];
		for (int i = 0; i < classes.length; i++) {
			strings[i] = classes[i].getName();
		}
		return strings;
	}
}