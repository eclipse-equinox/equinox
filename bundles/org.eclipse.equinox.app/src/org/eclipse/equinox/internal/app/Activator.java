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

package org.eclipse.equinox.internal.app;

import org.eclipse.equinox.registry.IExtensionRegistry;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class Activator implements BundleActivator, ServiceTrackerCustomizer {
	public static final String PI_APP = "org.eclipse.equinox.app"; //$NON-NLS-1$
	public static boolean DEBUG = false;
	private BundleContext bContext;
	private ContainerManager containerMgr;
	// tracks the extension registry
	private ServiceTracker registryTracker;

	public void start(BundleContext context) throws Exception {
		this.bContext = context;
		getDebugOptions(context);
		// set the app manager context before starting the containerMgr
		AppPersistenceUtil.setBundleContext(context);
		registryTracker = new ServiceTracker(context, IExtensionRegistry.class.getName(), this);
		registryTracker.open();
		// start the app commands for the console
		try {
			AppCommands.create(context);
		} catch (NoClassDefFoundError e) {
			// catch incase CommandProvider is not available
		}
	}

	public void stop(BundleContext context) throws Exception {
		// stop the app commands for the console
		try {
			AppCommands.destroy(context);
		} catch (NoClassDefFoundError e) {
			// catch incase CommandProvider is not available
		}
		// close the registry tracker; this will stop the containerMgr if it was started
		registryTracker.close();
		registryTracker = null;
		// unset the app manager context after the containerMgr has been stopped
		AppPersistenceUtil.setBundleContext(null);
		bContext = null;
	}

	private void getDebugOptions(BundleContext context) {
		ServiceReference debugRef = context.getServiceReference(DebugOptions.class.getName());
		if (debugRef == null)
			return;
		DebugOptions debugOptions = (DebugOptions) context.getService(debugRef);
		DEBUG = debugOptions.getBooleanOption(PI_APP + "/debug", false); //$NON-NLS-1$
		context.ungetService(debugRef);
	}

	public Object addingService(ServiceReference reference) {
		if (containerMgr != null)
			return null;
		// create and start the app containerMgr
		IExtensionRegistry registry = (IExtensionRegistry) bContext.getService(reference);
		containerMgr = new ContainerManager(bContext, registry);
		containerMgr.startManager();
		return registry;
	}

	public void modifiedService(ServiceReference reference, Object service) {
		// do nothing
	}

	public void removedService(ServiceReference reference, Object service) {
		if (containerMgr == null)
			return;
		// stop the app containerMgr
		containerMgr.stopManager();
		containerMgr = null;
	}
}
