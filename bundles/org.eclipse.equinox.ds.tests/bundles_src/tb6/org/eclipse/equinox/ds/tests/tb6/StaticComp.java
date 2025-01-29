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
package org.eclipse.equinox.ds.tests.tb6;

import java.util.Dictionary;
import java.util.Vector;

import org.eclipse.equinox.ds.tests.tbc.ComponentContextProvider;
import org.eclipse.equinox.ds.tests.tbc.DSEvent;
import org.eclipse.equinox.ds.tests.tbc.DSEventsProvider;
import org.eclipse.equinox.ds.tests.tbc.BoundTester;
import org.osgi.framework.ServiceReference;

import org.osgi.service.component.ComponentContext;

public class StaticComp implements DSEventsProvider, BoundTester, ComponentContextProvider {

	private ComponentContext ctxt;
	private final Vector componentEvents = new Vector();
	private ReferencedComp rc;

	public void activate(ComponentContext ctxt) {
		this.ctxt = ctxt;
		componentEvents.addElement(new DSEvent(DSEvent.ACT_ACTIVATE, null));
	}

	public void deactivate(ComponentContext ctxt) {
		this.ctxt = null;
		componentEvents.addElement(new DSEvent(DSEvent.ACT_DEACTIVATE, null));
	}

	public void bind(ReferencedComp rc) {
		this.rc = rc;
		componentEvents.addElement(new DSEvent(DSEvent.ACT_BOUND, rc));
	}

	public void unbind(ReferencedComp rc) {
		if (this.rc == rc) {
			this.rc = null;
			componentEvents.addElement(new DSEvent(DSEvent.ACT_UNBOUND, rc));
		}
	}

	@Override
	public DSEvent[] getEvents() {
		DSEvent[] events = new DSEvent[componentEvents.size()];
		componentEvents.copyInto(events);
		return events;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.ds.tests.tbc.DSEventsProvider#resetComponentEvents()
	 */
	@Override
	public void resetEvents() {
		componentEvents.removeAllElements();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.ds.tests.tbc.PropertiesProvider#getProperties()
	 */
	@Override
	public Dictionary getProperties() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.ds.tests.tbc.BoundTester#getBoundObject(int)
	 */
	@Override
	public ServiceReference getBoundServiceRef(int index) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.ds.tests.tbc.BoundTester#getBoundObjectsCount()
	 */
	@Override
	public int getBoundObjectsCount() {
		return (this.rc != null ? 1 : 0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.ds.tests.tbc.BoundTester#getBoundService(int)
	 */
	@Override
	public Object getBoundService(int index) {
		return this.rc;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.equinox.ds.tests.tbc.ComponentContextProvider#getComponentContext
	 * ()
	 */
	@Override
	public ComponentContext getComponentContext() {
		return ctxt;
	}

}
