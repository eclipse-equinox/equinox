/*******************************************************************************
 * Copyright (c) 1997-2009 by ProSyst Software GmbH
 * http://www.prosyst.com
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.ds.tests.tb4;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Vector;

import org.eclipse.equinox.ds.tests.tbc.BoundMainProvider;
import org.eclipse.equinox.ds.tests.tbc.ComponentContextProvider;
import org.eclipse.equinox.ds.tests.tbc.DSEvent;
import org.eclipse.equinox.ds.tests.tbc.DSEventsProvider;
import org.osgi.service.component.ComponentContext;

public class BoundReplacer implements DSEventsProvider, BoundMainProvider, ComponentContextProvider {

	private final Hashtable boundServices = new Hashtable();
	private final Vector boundServiceEvents = new Vector();
	private ComponentContext ctxt;

	public void activate(ComponentContext ctxt) {
		this.ctxt = ctxt;
	}

	public void deactivate(ComponentContext ctxt) {
		this.ctxt = null;
	}

	public void bindDynamicService(DynamicService dynService) {
		boundServiceEvents.addElement(new DSEvent(DSEvent.ACT_BOUND, dynService));
		boundServices.put(BoundMainProvider.DYNAMIC_SERVICE, dynService);
	}

	public void unbindDynamicService(DynamicService dynService) {
		boundServiceEvents.addElement(new DSEvent(DSEvent.ACT_UNBOUND, dynService));
	}

	public void bindNamedService(NamedService namedService) {
		boundServiceEvents.addElement(new DSEvent(DSEvent.ACT_BOUND, namedService));
		boundServices.put(BoundMainProvider.NAMED_SERVICE, namedService);
	}

	public void unbindNamedService(NamedService namedService) {
		boundServiceEvents.addElement(new DSEvent(DSEvent.ACT_UNBOUND, namedService));
	}

	@Override
	public Dictionary getProperties() {
		return null;
	}

	@Override
	public DSEvent[] getEvents() {
		DSEvent[] events = new DSEvent[boundServiceEvents.size()];
		boundServiceEvents.copyInto(events);
		return events;
	}

	@Override
	public Object getBoundService(String serviceName) {
		return boundServices.get(serviceName);
	}

	@Override
	public void resetEvents() {
		boundServiceEvents.removeAllElements();
	}

	@Override
	public ComponentContext getComponentContext() {
		return ctxt;
	}

}
