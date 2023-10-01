/*******************************************************************************
 * Copyright (c) 2005, 2020 IBM Corporation and others.
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

package org.eclipse.equinox.internal.event.mapper;

import org.osgi.framework.*;
import org.osgi.service.event.EventAdmin;

/**
 * Main class to redeliver framework published events via EventAdmin.
 * 
 * @version $Revision: 1.3 $
 */
public class EventRedeliverer implements FrameworkListener, BundleListener, AllServiceListener {
	private final EventAdmin eventAdmin;
	private final BundleContext bc;

	public EventRedeliverer(BundleContext bc, EventAdmin eventAdmin) {
		this.bc = bc;
		this.eventAdmin = eventAdmin;
	}

	public void close() {
		bc.removeFrameworkListener(this);
		bc.removeBundleListener(this);
		bc.removeServiceListener(this);
	}

	/**
	 * register event listeners which are necessary to obtain events to be mapped
	 */
	public void open() {
		// add legacy event listener for framework level event
		bc.addFrameworkListener(this);
		bc.addBundleListener(this);
		bc.addServiceListener(this);
	}

	@Override
	public void frameworkEvent(FrameworkEvent event) {
		new FrameworkEventAdapter(event, eventAdmin).redeliver();
	}

	@Override
	public void bundleChanged(BundleEvent event) {
		new BundleEventAdapter(event, eventAdmin).redeliver();
	}

	@Override
	public void serviceChanged(ServiceEvent event) {
		new ServiceEventAdapter(event, eventAdmin).redeliver();
	}
}
