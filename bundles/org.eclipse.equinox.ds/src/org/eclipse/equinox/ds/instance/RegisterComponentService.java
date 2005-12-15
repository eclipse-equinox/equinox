/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.ds.instance;

import java.util.Hashtable;
import java.util.List;
import org.eclipse.equinox.ds.Log;
import org.eclipse.equinox.ds.model.ComponentDescription;
import org.eclipse.equinox.ds.model.ComponentDescriptionProp;
import org.osgi.framework.*;
import org.osgi.service.component.ComponentException;
import org.osgi.service.component.ComponentInstance;

/**
 * Static utility class to register a Component Configuration's provided service.
 * A ServiceFactory is used to enable lazy activation
 * 
 * @version $Revision: 1.2 $
 */
abstract class RegisterComponentService {

	/* set this to true to compile in debug messages */
	private static final boolean DEBUG = false;

	// cannot instantiate - this is a utility class
	private RegisterComponentService() {
	}

	/**
	 * Register the Component Configuration's service
	 * 
	 * @param ip - InstanceProcess
	 * @param cdp - ComponentDescription plus Properties
	 */
	static void registerService(InstanceProcess instanceProcess, ComponentDescriptionProp cdp) {

		ComponentDescription cd = cdp.getComponentDescription();

		//make final references for use by anonymous inner class
		final InstanceProcess finalInstanceProcess = instanceProcess;
		final ComponentDescriptionProp finalCDP = cdp;

		List servicesProvided = cd.getServicesProvided();
		String[] servicesProvidedArray = (String[]) servicesProvided.toArray(new String[servicesProvided.size()]);

		// register the service using a ServiceFactory
		ServiceRegistration serviceRegistration = null;
		if (cd.getService().isServicefactory()) {
			// register the service using a ServiceFactory
			serviceRegistration = cd.getBundleContext().registerService(servicesProvidedArray, new ServiceFactory() {
				// map of Bundle:componentInstance
				Hashtable instances;

				// ServiceFactory.getService method.
				public Object getService(Bundle bundle, ServiceRegistration registration) {
					if (DEBUG)
						System.out.println("RegisterComponentServiceFactory:getService: registration:" + registration);

					ComponentInstance componentInstance = null;
					try {
						componentInstance = finalInstanceProcess.buildDispose.buildComponentConfigInstance(bundle, finalCDP);
					} catch (ComponentException e) {
						Log.log(1, "[SCR] Error attempting to register a Service Factory.", e);
					}

					if (componentInstance != null) {
						// save so we can dispose later
						synchronized (this) {
							if (instances == null) {
								instances = new Hashtable();
							}
						}
						instances.put(bundle, componentInstance);
					}
					return componentInstance.getInstance();
				}

				// ServiceFactory.ungetService method.
				public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
					if (DEBUG)
						System.out.println("RegisterComponentServiceFactory:ungetService: registration = " + registration);
					((ComponentInstance) instances.get(bundle)).dispose();
					instances.remove(bundle);
					synchronized (this) {
						if (instances.isEmpty()) {
							instances = null;
						}
					}
				}
			}, cdp.getProperties());
		} else {
			// servicefactory=false
			// always return the same instance
			serviceRegistration = cd.getBundleContext().registerService(servicesProvidedArray, new ServiceFactory() {

				int references = 0;

				//keep track of whether the componentInstance was created
				//by this class' getService method or if it already 
				//existed - if we create it then we have to dispose
				//of it when it is no longer in use (references == 0)
				boolean disposeComponentInstance = false;

				// ServiceFactory.getService method.
				public Object getService(Bundle bundle, ServiceRegistration registration) {
					if (DEBUG)
						System.out.println("RegisterComponentService: getService: registration = " + registration);

					synchronized (this) {

						if (finalCDP.getInstances().isEmpty()) {
							try {
								//track that we created this instance
								//so we know to dispose of it later
								finalInstanceProcess.buildDispose.buildComponentConfigInstance(null, finalCDP);
								disposeComponentInstance = true;
							} catch (ComponentException e) {
								Log.log(1, "[SCR] Error attempting to register Service.", e);
								return null;
							}
						}
						references++;
					}
					return ((ComponentInstance) finalCDP.getInstances().get(0)).getInstance();
				}

				// ServiceFactory.ungetService method.
				public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
					if (DEBUG)
						System.out.println("RegisterComponentService: ungetService: registration = " + registration);

					synchronized (this) {
						references--;
						if (references < 1 && disposeComponentInstance) {
							// if disposeComponentInstance then we 
							// created it in getService so we should
							// dispose of it now
							((ComponentInstance) finalCDP.getInstances().get(0)).dispose();
							disposeComponentInstance = false;
						}
					}
				}
			}, cdp.getProperties());
		}

		if (DEBUG)
			System.out.println("RegisterComponentService: register: " + serviceRegistration);

		cdp.setServiceRegistration(serviceRegistration);
	}

}
