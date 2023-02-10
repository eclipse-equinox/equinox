/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
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
package org.eclipse.equinox.internal.event;

import org.eclipse.equinox.internal.event.mapper.EventRedeliverer;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.*;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

@Component(name = "org.eclipse.equinox.event", service = EventAdmin.class)
public class EventComponent implements EventAdmin {
	private EventRedeliverer eventRedeliverer;
	private EventAdminImpl eventAdmin;

	@Activate
	void activate(BundleContext context) {
		eventAdmin = new EventAdminImpl(context);
		eventAdmin.start();
		eventRedeliverer = new EventRedeliverer(context, eventAdmin);
		eventRedeliverer.open();
	}

	@Deactivate
	void deactivate(BundleContext context) {
		eventRedeliverer.close();
		eventAdmin.stop();
	}

	@Override
	public void postEvent(Event event) {
		eventAdmin.postEvent(event);
	}

	@Override
	public void sendEvent(Event event) {
		eventAdmin.sendEvent(event);
	}
}
