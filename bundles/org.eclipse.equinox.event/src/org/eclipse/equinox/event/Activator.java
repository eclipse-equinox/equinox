/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.event;

import org.eclipsei.equinox.event.mapper.EventRedeliverer;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {
	private EventRedeliverer    _eventRedeliverer= null;
	private ServiceRegistration _eventAdminService = null;
	public void start(BundleContext bundleContext) {
		
		_eventAdminService = bundleContext.registerService("org.osgi.service.event.EventAdmin", //$NON-NLS-1$
				new EventAdminImpl(bundleContext),null);
		_eventRedeliverer  = new EventRedeliverer(bundleContext);
		_eventRedeliverer.open();
		
	}
	
	public void stop(BundleContext bundleContext) {
		
		_eventRedeliverer.close();
		_eventRedeliverer=null;
		_eventAdminService.unregister();
		_eventAdminService=null;
			
	}

}
