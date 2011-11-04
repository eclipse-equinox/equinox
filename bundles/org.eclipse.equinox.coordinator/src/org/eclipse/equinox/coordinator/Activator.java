/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.coordinator;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.coordinator.Coordinator;

public class Activator implements BundleActivator {
	// Static so the factory can be used by both DS and standard OSGi.
	static volatile CoordinatorServiceFactory factory;

	private ServiceRegistration<Coordinator> registration;

	public void start(BundleContext bundleContext) throws Exception {
		// Instantiate the factory to be used by both DS and standard OSGi. In the case of DS, the
		// start method is guaranteed to be called before any components are created.
		factory = new CoordinatorServiceFactory(bundleContext);
		if (Boolean.valueOf(bundleContext.getProperty("equinox.use.ds")).booleanValue()) //$NON-NLS-1$
			return; // If this property is set we assume DS is being used.
		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		// TODO Add desired properties (bundle vendor, etc.).
		@SuppressWarnings({"unchecked"})
		// Use local variable to avoid suppressing unchecked warnings at method level.
		ServiceRegistration<Coordinator> reg = (ServiceRegistration<Coordinator>) bundleContext.registerService(Coordinator.class.getName(), factory, properties);
		this.registration = reg;
	}

	public void stop(BundleContext bundleContext) throws Exception {
		// Will be null when using DS.
		if (registration != null)
			registration.unregister();
		factory.shutdown();
		CoordinationWeakReference.processOrphanedCoordinations();
	}
}
