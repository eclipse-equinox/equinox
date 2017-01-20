/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.ds;

import java.util.List;
import org.apache.felix.scr.ScrService;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.osgi.framework.*;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

@SuppressWarnings("deprecation")
public class Activator implements BundleActivator, ServiceTrackerCustomizer<ServiceComponentRuntime, ServiceRegistration<ScrService>> {

	private BundleContext bc;
	private Bundle scr;
	private ServiceTracker<ServiceComponentRuntime, ServiceRegistration<ScrService>> tracker;

	public void start(BundleContext context) throws Exception {
		this.bc = context;
		ServiceReference<EnvironmentInfo> envInfoRef = context.getServiceReference(EnvironmentInfo.class);
		EnvironmentInfo envInfo = null;
		if (envInfoRef != null) {
			envInfo = context.getService(envInfoRef);
		}
		if (envInfo != null) {
			envInfo.setProperty("ds.delayed.keepInstances", "true"); //$NON-NLS-1$//$NON-NLS-2$
			envInfo.setProperty("equinox.use.ds", "true"); //$NON-NLS-1$//$NON-NLS-2$
			context.ungetService(envInfoRef);
		} else {
			System.setProperty("ds.delayed.keepInstances", "true"); //$NON-NLS-1$//$NON-NLS-2$
			System.setProperty("equinox.use.ds", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		BundleWiring wiring = context.getBundle().adapt(BundleWiring.class);
		List<BundleWire> required = wiring.getRequiredWires(BundleNamespace.BUNDLE_NAMESPACE);
		if (required.isEmpty()) {
			throw new IllegalStateException("No org.apache.felix.scr bundle found!"); //$NON-NLS-1$
		}
		scr = required.get(0).getProvider().getBundle();
		if (!"org.apache.felix.scr".equals(scr.getSymbolicName())) { //$NON-NLS-1$
			throw new IllegalStateException("Required wrong bundle: " + scr); //$NON-NLS-1$
		}
		BundleStartLevel equinoxSDstartLevel = context.getBundle().adapt(BundleStartLevel.class);
		BundleStartLevel scrStartLevel = scr.adapt(BundleStartLevel.class);
		scrStartLevel.setStartLevel(equinoxSDstartLevel.getStartLevel());
		scr.start(Bundle.START_TRANSIENT);
		tracker = new ServiceTracker<ServiceComponentRuntime, ServiceRegistration<ScrService>>(context, ServiceComponentRuntime.class, this);
		tracker.open();
	}

	public void stop(BundleContext context) throws Exception {
		tracker.close();
		scr.stop(Bundle.STOP_TRANSIENT);
	}

	@Override
	public ServiceRegistration<ScrService> addingService(ServiceReference<ServiceComponentRuntime> reference) {
		ServiceComponentRuntime scrService = bc.getService(reference);
		if (scr != null) {
			return bc.registerService(ScrService.class, new ScrServiceImpl(scrService, bc), null);
		}
		return null;
	}

	@Override
	public void modifiedService(ServiceReference<ServiceComponentRuntime> reference, ServiceRegistration<ScrService> reg) {
		// do nothing
	}

	@Override
	public void removedService(ServiceReference<ServiceComponentRuntime> reference, ServiceRegistration<ScrService> reg) {
		reg.unregister();
		bc.ungetService(reference);
	}

}
