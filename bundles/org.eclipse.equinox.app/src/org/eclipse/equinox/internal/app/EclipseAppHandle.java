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

import java.util.*;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.application.ApplicationHandle;

/*
 * An ApplicationHandle that represents a single instance of a running eclipse application.
 */
public class EclipseAppHandle extends ApplicationHandle {
	/**
	 * Indicates the application is active
	 */
	public static final int ACTIVE = 0x01;
	/**
	 * Indicates the application is stopping
	 */
	public static final int STOPPING = 0x02;
	/**
	 * Indicates the application is stopped
	 */
	public static final int STOPPED = 0x04;

	private ServiceRegistration sr;
	private String state = ApplicationHandle.RUNNING;
	private int status = EclipseAppHandle.ACTIVE;
	private MainThreadRunnable appRunnable;
	private Map arguments;

	/*
	 * Constructs a handle for a single running instance of a eclipse application.
	 */
	EclipseAppHandle(String instanceId, EclipseAppDescriptor descriptor) {
		super(instanceId, descriptor);
	}

	synchronized public String getState() {
		return state;
	}

	protected void destroySpecific() {
		// when this method is called we must force the application to exit.
		// first set the status to stopping
		setAppStatus(EclipseAppHandle.STOPPING);
		// now force the appliction to stop
		IApplication application = appRunnable == null ? null : appRunnable.getApplication();
		if (application != null)
			application.stop();
		// make sure the app status is stopped
		setAppStatus(EclipseAppHandle.STOPPED);
	}

	void setServiceRegistration(ServiceRegistration sr) {
		this.sr = sr;
	}

	void setAppRunnable(MainThreadRunnable appRunnable) {
		this.appRunnable = appRunnable;
	}

	/*
	 * Gets a snapshot of the current service properties.
	 */
	Dictionary getServiceProperties() {
		Dictionary props = new Hashtable(6);
		props.put(ApplicationHandle.APPLICATION_PID, getInstanceId());
		props.put(ApplicationHandle.APPLICATION_STATE, getState());
		props.put(ApplicationHandle.APPLICATION_DESCRIPTOR, getApplicationDescriptor().getApplicationId());
		props.put(EclipseAppDescriptor.APP_TYPE, EclipseAppDescriptor.APP_TYPE_MAIN_TREAD);
		return props;
	}

	/*
	 * Changes the state of this handle to STOPPING.  
	 * Finally the handle is unregistered if the status is STOPPED
	 */
	synchronized void setAppStatus(int status) {
		if ((status & EclipseAppHandle.ACTIVE) != 0)
			throw new IllegalArgumentException("Cannot set app status to ACTIVE"); //$NON-NLS-1$
		// if status is stopping and the context is already stopping the return
		if ((status & EclipseAppHandle.STOPPING) != 0)
			if (ApplicationHandle.STOPPING == state)
				return;
		// in both cases if the handle is not stopping then set it and
		// change the service properties to reflect the state change.
		if (state != ApplicationHandle.STOPPING) {
			state = ApplicationHandle.STOPPING;
			sr.setProperties(getServiceProperties());
		}
		// if the status is stopped then unregister the service
		if ((status & EclipseAppHandle.STOPPED) != 0 && (this.status & EclipseAppHandle.STOPPED) == 0) {
			sr.unregister();
			((EclipseAppDescriptor) getApplicationDescriptor()).getContainerManager().unlock();
		}
		this.status = status;
	}

	int getAppStatus() {
		return status;
	}

	Map getArguments() {
		return arguments;
	}

	void setArguments(Map arguments) {
		this.arguments = arguments;
	}

	IConfigurationElement getConfiguration() {
		IExtension applicationExtension = ((EclipseAppDescriptor) getApplicationDescriptor()).getContainerManager().getAppExtension(getApplicationDescriptor().getApplicationId());
		IConfigurationElement[] configs = applicationExtension.getConfigurationElements();
		if (configs.length == 0)
			throw new RuntimeException(NLS.bind(Messages.application_invalidExtension, getApplicationDescriptor().getApplicationId()));
		return configs[0];
	}

}
