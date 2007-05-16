/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.event;

import org.eclipse.equinox.event.mapper.EventRedeliverer;
import org.osgi.framework.*;

public class Activator implements BundleActivator {
	private EventRedeliverer    eventRedeliverer;
	private ServiceRegistration eventAdminService;
	private EventAdminImpl eventAdmin;
	
	public void start(BundleContext bundleContext) {
		eventAdmin = new EventAdminImpl(bundleContext);
		eventAdmin.start();
		eventAdminService = bundleContext.registerService("org.osgi.service.event.EventAdmin", //$NON-NLS-1$
				eventAdmin,null);
		eventRedeliverer  = new EventRedeliverer(bundleContext);
		eventRedeliverer.open();
	}
	
	public void stop(BundleContext bundleContext) {
		eventRedeliverer.close();
		eventAdminService.unregister();
		eventAdmin.stop();
	}
}
