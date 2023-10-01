/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
package org.eclipse.equinox.ds.tests.tb4;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Vector;

import org.eclipse.equinox.ds.tests.tbc.BoundMainProvider;
import org.eclipse.equinox.ds.tests.tbc.ComponentContextProvider;
import org.eclipse.equinox.ds.tests.tbc.DSEvent;
import org.eclipse.equinox.ds.tests.tbc.DSEventsProvider;
import org.eclipse.equinox.ds.tests.tbc.DynamicWorker;
import org.eclipse.equinox.ds.tests.tbc.StaticWorker;
import org.osgi.service.component.ComponentContext;

public class AdvancedBounder implements DSEventsProvider, BoundMainProvider, ComponentContextProvider {

	private Hashtable boundServices = new Hashtable();
	private Vector boundServiceEvents = new Vector();
	private ComponentContext ctxt;

	public void activate(ComponentContext ctxt) {
		this.ctxt = ctxt;
	}

	public void deactivate(ComponentContext ctxt) {
		this.ctxt = null;
	}

	public void bindDynamicService(DynamicWorker dynService) {
		boundServiceEvents.addElement(new DSEvent(DSEvent.ACT_BOUND, dynService));
		boundServices.put(BoundMainProvider.DYNAMIC_SERVICE, dynService);
	}

	public void unbindDynamicService(DynamicWorker dynService) {
		boundServiceEvents.addElement(new DSEvent(DSEvent.ACT_UNBOUND, dynService));
	}

	public void bindStaticService(StaticWorker staticService) {
		boundServiceEvents.addElement(new DSEvent(DSEvent.ACT_BOUND, staticService));
		boundServices.put(BoundMainProvider.STATIC_SERVICE, staticService);
	}

	public void unbindStaticService(StaticWorker staticService) {
		boundServiceEvents.addElement(new DSEvent(DSEvent.ACT_UNBOUND, staticService));
	}

	public Dictionary getProperties() {
		return null;
	}

	public DSEvent[] getEvents() {
		DSEvent[] events = new DSEvent[boundServiceEvents.size()];
		boundServiceEvents.copyInto(events);
		return events;
	}

	public Object getBoundService(String serviceName) {
		return boundServices.get(serviceName);
	}

	public void resetEvents() {
		boundServiceEvents.removeAllElements();
	}

	public ComponentContext getComponentContext() {
		return ctxt;
	}

}
