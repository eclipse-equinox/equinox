/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.event;

import org.eclipse.equinox.internal.event.mapper.EventRedeliverer;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

public class EventComponent implements EventAdmin {
	private EventRedeliverer eventRedeliverer;
	private EventAdminImpl eventAdmin;

	void activate(BundleContext context) {
		eventAdmin = new EventAdminImpl(context);
		eventAdmin.start();
		eventRedeliverer = new EventRedeliverer(context);
		eventRedeliverer.open();
	}

	void deactivate(BundleContext context) {
		eventRedeliverer.close();
		eventAdmin.stop();
	}

	public void postEvent(Event event) {
		eventAdmin.postEvent(event);
	}

	public void sendEvent(Event event) {
		eventAdmin.sendEvent(event);
	}
}
