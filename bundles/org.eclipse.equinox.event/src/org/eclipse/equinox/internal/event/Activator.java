/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.event;

import org.osgi.framework.*;
import org.osgi.service.event.EventAdmin;

public class Activator implements BundleActivator {
	private static final String PROP_USE_DS = "equinox.use.ds"; //$NON-NLS-1$
	private ServiceRegistration<EventAdmin> eventAdminService;
	private EventComponent eventAdmin;

	public void start(BundleContext bundleContext) throws InvalidSyntaxException {
		if (Boolean.valueOf(bundleContext.getProperty(PROP_USE_DS)).booleanValue())
			return; // If this property is set we assume DS is being used.
		String serviceName = EventAdmin.class.getName();
		Filter serviceFilter = bundleContext.createFilter("(objectclass=" + serviceName + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		//don't register the service if this bundle has already registered it declaratively
		ServiceReference<?>[] refs = bundleContext.getBundle().getRegisteredServices();
		if (refs != null) {
			for (int i = 0; i < refs.length; i++)
				if (serviceFilter.match(refs[i]))
					return; // We found a service registered by this bundle already
		}

		eventAdmin = new EventComponent();
		eventAdmin.activate(bundleContext);
		eventAdminService = bundleContext.registerService(EventAdmin.class, eventAdmin, null);
	}

	public void stop(BundleContext bundleContext) {
		if (eventAdmin != null) {
			eventAdminService.unregister();
			eventAdmin.deactivate(bundleContext);
		}
	}
}
