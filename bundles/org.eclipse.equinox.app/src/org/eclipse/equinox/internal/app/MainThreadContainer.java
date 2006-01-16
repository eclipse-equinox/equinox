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

public class MainThreadContainer implements IContainer, ServiceTrackerCustomizer {
	// tracks the application launcher
	private ServiceTracker appLauncherTracker;
	// the application launcher used to launch applications on the main thread.
	private ApplicationLauncher appLauncher;
	// the default app to launch once we are ready
	private EclipseAppDescriptor defaultDesc;

	public MainThreadContainer() {
		appLauncherTracker = new ServiceTracker(AppPersistenceUtil.getContext(), ApplicationLauncher.class.getName(), this);
		appLauncherTracker.open();
	}

	public IApplication launch(IAppContext appContext) {
		// use the ApplicationLauncher provided by the framework 
		// to ensure it is launched on the main thread
		if (appLauncher == null)
			throw new IllegalStateException();
		MainThreadApplication app = new MainThreadApplication((EclipseAppHandle) appContext);
		appLauncher.launch(app, appContext.getArguments() == null ? null : appContext.getArguments().get(ContainerManager.PROP_ECLIPSE_APPLICATION_ARGS));
		return app.getApplication();
	}

	public Object addingService(ServiceReference reference) {
		if (appLauncher != null)
			return null;
		appLauncher = (ApplicationLauncher) AppPersistenceUtil.getContext().getService(reference);
		if (defaultDesc != null)
			// launch the default application
			try {
				defaultDesc.launch(null);
				defaultDesc = null; // don't want to launch it more than once
			} catch (Exception e) {
				// TODO should log this!!
			}
		return appLauncher;
	}

	public void modifiedService(ServiceReference reference, Object service) {
		// do nothing
	}

	public synchronized void removedService(ServiceReference reference, Object service) {
		if (service == appLauncher) {
			appLauncher = null;
			AppPersistenceUtil.getContext().ungetService(reference);
		}
	}

	public void shutdown() {
		if (appLauncherTracker != null) {
			appLauncherTracker.close();
			appLauncherTracker = null;
		}
	}

	public void setDefaultApp(EclipseAppDescriptor defaultDesc) {
		this.defaultDesc = defaultDesc;
	}
}
