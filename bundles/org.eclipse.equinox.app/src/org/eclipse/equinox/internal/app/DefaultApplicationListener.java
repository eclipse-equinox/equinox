/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.app;

import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.service.runnable.ApplicationRunnable;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.ServiceReference;
import org.osgi.service.application.ApplicationHandle;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Listens for the default ApplicationHandle which run on any thread to be destroyed.  This is used to force the main
 * thread to wait while a default application runs on another thread.
 * 
 * A main threaded application may be launched using this class to launch the main threaded application.
 */
public class DefaultApplicationListener implements ApplicationRunnable, ServiceTrackerCustomizer {
	private boolean running = true; // indicates the default application is running
	private EclipseAppHandle launchMainApp; // a handle to a main threaded application
	private final ServiceTracker handleTracker; // tracks the default application handle
	private Object result; // holds the result from the default application

	public DefaultApplicationListener(EclipseAppHandle defaultApp) {
		ServiceReference defaultRef = defaultApp.getServiceReference();
		if (defaultRef == null) {
			// service has been unregistered; application has ended already, 
			// save the result for latter
			result = defaultApp.waitForResult(100);
			handleTracker = null;
			return;
		}
		ServiceTracker defaultAppTracker = new ServiceTracker(Activator.getContext(), defaultRef, this);
		defaultAppTracker.open();
		EclipseAppHandle trackedApp = (EclipseAppHandle) defaultAppTracker.getService();
		if (trackedApp == null) {
			// close tracker since we do not care about tracking the app (bug 215764)
			defaultAppTracker.close();
			// service has been unregistered; application has ended aready,
			// save the result for latter
			result = defaultApp.waitForResult(100);
			handleTracker = null;
		} else {
			handleTracker = defaultAppTracker;
		}
	}

	public Object run(Object context) {
		if (handleTracker == null)
			return getResult(); // app has ended, return the result
		EclipseAppHandle anyThreadedDefaultApp = (EclipseAppHandle) handleTracker.getService();
		if (anyThreadedDefaultApp != null)
			// We now need to actual launch the application; this will run the application on another thread.
			AnyThreadAppLauncher.launchEclipseApplication(anyThreadedDefaultApp);
		try {
			while (waitOnRunning()) {
				EclipseAppHandle mainHandle = getMainHandle();
				if (mainHandle != null) {
					// while we were waiting for the default application to end someone asked for a main threaded app to launch
					// note that we cannot hold the this lock while launching a main threaded application
					try {
						mainHandle.run(null);
					} catch (Throwable e) {
						String message = NLS.bind(Messages.application_error_starting, mainHandle.getInstanceId());
						Activator.log(new FrameworkLogEntry(Activator.PI_APP, FrameworkLogEntry.WARNING, 0, message, 0, e, null));
					}
					unsetMainHandle(mainHandle);
				}
			}
		} finally {
			handleTracker.close();
		}
		return getResult();
	}

	private synchronized EclipseAppHandle getMainHandle() {
		return launchMainApp;
	}

	private synchronized void unsetMainHandle(EclipseAppHandle mainHandle) {
		if (launchMainApp == mainHandle)
			launchMainApp = null;
	}

	private synchronized boolean waitOnRunning() {
		if (!running)
			return false;
		try {
			wait(100);
		} catch (InterruptedException e) {
			// do nothing
		}
		return running;
	}

	public void stop() {
		if (handleTracker == null)
			return;
		// force the default application to quit
		ApplicationHandle handle = (ApplicationHandle) handleTracker.getService();
		if (handle != null) {
			try {
				handle.destroy();
			} catch (Throwable t) {
				String message = NLS.bind(Messages.application_error_stopping, handle.getInstanceId());
				Activator.log(new FrameworkLogEntry(Activator.PI_APP, FrameworkLogEntry.WARNING, 0, message, 0, t, null));
			}
		}
	}

	public Object addingService(ServiceReference reference) {
		return Activator.getContext().getService(reference);
	}

	public void modifiedService(ServiceReference reference, Object service) {
		// do nothing
	}

	synchronized public void removedService(ServiceReference reference, Object service) {
		running = false;
		// only wait for 5 seconds; this may timeout if forcing an application to quit takes too long
		// this should never timeout if the application exited normally.
		result = ((EclipseAppHandle) service).waitForResult(5000);
		EclipseAppHandle mainHandle = getMainHandle();
		if (mainHandle != null)
			// default application has quit; now force the main threaded application to quit
			try {
				mainHandle.destroy();
			} catch (Throwable t) {
				String message = NLS.bind(Messages.application_error_stopping, mainHandle.getInstanceId());
				Activator.log(new FrameworkLogEntry(Activator.PI_APP, FrameworkLogEntry.WARNING, 0, message, 0, t, null));
			}
		this.notify();
	}

	synchronized void launch(EclipseAppHandle app) {
		launchMainApp = app;
		this.notify();
	}

	private synchronized Object getResult() {
		return result;
	}
}
