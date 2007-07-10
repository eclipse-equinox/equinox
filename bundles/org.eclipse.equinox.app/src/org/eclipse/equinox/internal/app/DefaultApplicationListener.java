/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.app;

import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.service.runnable.ApplicationRunnable;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
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
	private ServiceTracker handleTracker; // tracks the default application handle
	private Object result;

	public Object run(Object context) {
		BundleContext bc = Activator.getContext();
		// we assume there is only one handle created with eclipse.application.default set to true
		ServiceReference[] defaultHandle = null;
		try {
			String handleFilter = "(" + EclipseAppDescriptor.APP_DEFAULT + "=true)"; //$NON-NLS-1$ //$NON-NLS-2$
			defaultHandle = bc.getServiceReferences(ApplicationHandle.class.getName(), handleFilter);
		} catch (InvalidSyntaxException e) {
			// do nothing; already tested the filter above
		}
		if (defaultHandle == null || defaultHandle.length == 0 || defaultHandle.length > 1) {
			return null; // application may have ended already
		}
		handleTracker = new ServiceTracker(bc, defaultHandle[0], this);
		handleTracker.open();
		try {
			if (handleTracker.getService() == null)
				return null; // this check should not be needed; just a safety check
			while (waitOnRunning()) {
				EclipseAppHandle mainHandle = getMainHandle();
				if (mainHandle != null) {
					// while we were waiting for the default application to end someone asked for a main threaded app to launch
					// note that we cannot hold the this lock while launching a main threaded application
					try {
						mainHandle.run(null);
					} catch (Exception e) {
						String message = NLS.bind(Messages.application_error_starting, mainHandle.getInstanceId());
						Activator.log(new FrameworkLogEntry(Activator.PI_APP, FrameworkLogEntry.WARNING, 0, message, 0, e, null));
					}
					setMainHandle(null);
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

	private synchronized void setMainHandle(EclipseAppHandle mainHandle) {
		launchMainApp = mainHandle;
	}

	private synchronized boolean waitOnRunning() {
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
		ApplicationHandle handle =(ApplicationHandle) handleTracker.getService();
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

	synchronized Object getResult() {
		return result;
	}
}
