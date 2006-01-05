/*******************************************************************************
 * Copyright (c) 2002, 2005 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.wireadmin;

import org.osgi.framework.*;
import org.osgi.service.prefs.PreferencesService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class Activator implements BundleActivator, ServiceTrackerCustomizer {

	private String wireadminString = "org.osgi.service.wireadmin.WireAdmin";
	private WireAdmin wireadmin;
	private ServiceRegistration wireadminReg;
	private PreferencesService preferencesService;
	private BundleContext context;
	private ServiceTracker prefsTracker;

	/**
	 * @see BundleActivator#start(BundleContext)
	 */
	public void start(BundleContext context) throws Exception {

		this.context = context;
		prefsTracker = new ServiceTracker(context, PreferencesService.class.getName(), this);
		prefsTracker.open();
	}

	/**
	 * @see BundleActivator#stop(BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		if(wireadminReg != null)
		{
			wireadminReg.unregister();
			wireadmin.destroy();
		}

	}

	public Object addingService(ServiceReference reference) {
		prefsTracker.close();
		if (preferencesService == null) {
			preferencesService = (PreferencesService) context.getService(reference);
			registerWireAdminService();
			return preferencesService;
		}
		return null; //we don't want to track a service we are not using
	}

	public void modifiedService(ServiceReference reference, Object service) {

	}

	public void removedService(ServiceReference reference, Object service) {
		if (service == preferencesService) {
			preferencesService = null;
			wireadminReg.unregister();
		}
	}

	public void registerWireAdminService() {
		wireadmin = new WireAdmin(context);
		wireadminReg = context.registerService(wireadminString, wireadmin, null);
		wireadmin.setServiceReference(wireadminReg.getReference());
	}

}
