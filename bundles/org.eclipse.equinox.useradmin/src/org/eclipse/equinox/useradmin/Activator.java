/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.useradmin;

import java.util.Hashtable;
import org.osgi.framework.*;
import org.osgi.service.prefs.PreferencesService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**  This is the bundle activator for the UserAdmin bundle.
 */

public class Activator implements BundleActivator, ServiceFactory, ServiceTrackerCustomizer {
	/*
	 * ----------------------------------------------------------------------
	 *      BundleActivator Interface implementation
	 * ----------------------------------------------------------------------
	 */

	protected ServiceRegistration registration;
	protected UserAdmin userAdmin;
	protected static String userAdminClazz = "org.osgi.service.useradmin.UserAdmin"; //$NON-NLS-1$
	protected PreferencesService prefs;
	protected BundleContext context;
	protected ServiceTracker prefsTracker;

    public Activator() {
    	//a public constructor is required for a BundleActivator
    }
	
	/**
	 * Required by BundleActivator Interface.
	 */
	public void start(BundleContext context_) throws Exception {
		this.context = context_;
		prefsTracker = new ServiceTracker(context, PreferencesService.class.getName(), this);
		prefsTracker.open();
	}

	/**
	 * Required by BundleActivator Interface.
	 */
	public void stop(BundleContext context_) throws Exception {
		prefsTracker.close();
        unregisterUserAdminService();
	}


	public Object getService(Bundle bundle, ServiceRegistration registration_) {
		userAdmin.setServiceReference(registration_.getReference());
		return userAdmin;
	}

	public void ungetService(Bundle bundle, ServiceRegistration registration_, Object service) {
	   //do nothing
	}

	public Object addingService(ServiceReference reference) {
		if (prefs == null) {
			prefs = (PreferencesService) context.getService(reference);
			try {
				registerUserAdminService();
			} catch (Exception ex) {
				return null;
			}
			return prefs;
		}
		return null; //we don't want to track a service we are not using
	}

	public void modifiedService(ServiceReference reference, Object service) {
            // do nothing
	}

	public void removedService(ServiceReference reference, Object service) {
		if (service == prefs) {
			prefs = null;
		}
		unregisterUserAdminService();
	}


	/**
	 * Register the UserAdmin service.
	 */
	protected void registerUserAdminService() throws Exception {
		Hashtable properties = new Hashtable(7);

		properties.put(Constants.SERVICE_VENDOR, UserAdminMsg.Service_Vendor);
		properties.put(Constants.SERVICE_DESCRIPTION, UserAdminMsg.OSGi_User_Admin_service_IBM_Implementation_3);
		properties.put(Constants.SERVICE_PID, getClass().getName());

		userAdmin = new UserAdmin(prefs, context);
		registration = context.registerService(userAdminClazz, this, properties);
		userAdmin.setServiceReference(registration.getReference());
	}
	
	protected void unregisterUserAdminService() {
		if(registration != null)
		{
			registration.unregister();
			registration = null;
			userAdmin.destroy();
			userAdmin = null;
		}
	}
}
