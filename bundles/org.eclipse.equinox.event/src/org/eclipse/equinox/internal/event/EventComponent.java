/*******************************************************************************
 * Copyright (c) 2009, 2023 IBM Corporation and others.
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
 *     Christoph Läubrich - switch to annotations
 *******************************************************************************/
package org.eclipse.equinox.internal.event;

import org.eclipse.equinox.internal.event.mapper.EventRedeliverer;
import org.osgi.annotation.bundle.Capability;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.*;
import org.osgi.service.event.*;

@Component(service = EventAdmin.class)
@Capability(namespace = "osgi.implementation", name = EventConstants.EVENT_ADMIN_IMPLEMENTATION, uses = Event.class, version = EventConstants.EVENT_ADMIN_SPECIFICATION_VERSION)
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
