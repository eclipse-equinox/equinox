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

import org.eclipse.equinox.app.*;
import org.eclipse.osgi.service.runnable.ApplicationLauncher;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class MainSingletonContainer implements IContainer, ServiceTrackerCustomizer {
	// tracks the application launcher
	private ServiceTracker appLauncherTracker;
	// the application launcher used to launch applications on the main thread.
	private ApplicationLauncher appLauncher;
	private ContainerManager containerMgr;

	public MainSingletonContainer(ContainerManager containerMgr) {
		this.containerMgr = containerMgr;
		appLauncherTracker = new ServiceTracker(containerMgr.getBundleContext(), ApplicationLauncher.class.getName(), this);
		appLauncherTracker.open();
	}

	public IApplication launch(IAppContext appContext) {
		// use the ApplicationLauncher provided by the framework 
		// to ensure it is launched on the main thread
		if (appLauncher == null)
			throw new IllegalStateException();
		MainSingletonApplication app = new MainSingletonApplication((EclipseAppHandle) appContext);
		appLauncher.launch(app, appContext.getArguments() == null ? null : appContext.getArguments().get(ContainerManager.PROP_ECLIPSE_APPLICATION_ARGS));
		return app.getApplication();
	}

	public Object addingService(ServiceReference reference) {
		if (appLauncher != null)
			return null;
		appLauncher = (ApplicationLauncher) containerMgr.getBundleContext().getService(reference);
		if (!Boolean.getBoolean(ContainerManager.PROP_ECLIPSE_APPLICATION_NODEFAULT)) {
			// find the default application
			EclipseAppDescriptor defaultDesc = containerMgr.findDefaultApp();
			// launch the default application
			try {
				defaultDesc.launch(null);
			} catch (Exception e) {
				// TODO should log this!!
			}
		}
		return appLauncher;
	}

	public void modifiedService(ServiceReference reference, Object service) {
		// TODO Auto-generated method stub

	}

	public synchronized void removedService(ServiceReference reference, Object service) {
		if (service == appLauncher) {
			appLauncher = null;
			containerMgr.getBundleContext().ungetService(reference);
		}
	}

	public boolean isSingletonContainer() {
		return true;
	}

}
