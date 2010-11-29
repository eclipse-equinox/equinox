/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.event.tests;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

public class EventHandlerHelper implements EventHandler {
	private volatile Event lastEvent;

	public Event clearLastEvent() {
		Event result = lastEvent;
		lastEvent = null;
		return result;
	}

	public void handleEvent(Event event) {
		lastEvent = event;
	}

	public Event lastEvent() {
		return lastEvent;
	}
}
