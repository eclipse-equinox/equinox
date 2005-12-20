/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
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
import org.eclipse.equinox.app.IAppContext;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.registry.IConfigurationElement;
import org.eclipse.equinox.registry.IExtension;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.application.ApplicationHandle;

/*
 * An ApplicationHandle that represents a single instance of a running eclipse application.
 */
public class EclipseAppHandle extends ApplicationHandle implements IAppContext {
	private ServiceRegistration sr;
	private String state = ApplicationHandle.RUNNING;
	private int status = IAppContext.ACTIVE;
	private Object application;
	private Exception appNotFound;
	private Map arguments;

	/*
	 * Used to create a dummy handle to throw an exception when run by the ApplicationLauncher.
	 */
	EclipseAppHandle(Exception appNotFound, ContainerManager containerMgr) {
		this("", new EclipseAppDescriptor("", "", null, containerMgr)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		this.appNotFound = appNotFound;
	}

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
		setAppStatus(IAppContext.STOPPING);
		// now force the appliction to stop
		if (application instanceof IApplication)
			((IApplication) application).stop();
		// make sure the app status is stopped
		setAppStatus(IAppContext.STOPPED);
	}

	void setServiceRegistration(ServiceRegistration sr) {
		this.sr = sr;
	}

	void setApplication(Object application) {
		this.application = application;
	}

	/*
	 * Gets a snapshot of the current service properties.
	 */
	Dictionary getServiceProperties() {
		Dictionary props = new Hashtable(6);
		props.put(ApplicationHandle.APPLICATION_PID, getInstanceId());
		props.put(ApplicationHandle.APPLICATION_STATE, getState());
		props.put(ApplicationHandle.APPLICATION_DESCRIPTOR, getApplicationDescriptor().getApplicationId());
		return props;
	}

	/*
	 * Changes the state of this handle to STOPPING.  
	 * Finally the handle is unregistered if the status is STOPPED
	 */
	public synchronized void setAppStatus(int status) {
		if ((status & IAppContext.ACTIVE) != 0)
			throw new IllegalArgumentException("Cannot set app status to ACTIVE"); //$NON-NLS-1$
		// if status is stopping and the context is already stopping the return
		if ((status & IAppContext.STOPPING) != 0)
			if (ApplicationHandle.STOPPING.equals(state))
				return;
		// in both cases if the the context is not stopping then set it and
		// change the service properties to reflect the state change.
		if (state != ApplicationHandle.STOPPING) {
			state = ApplicationHandle.STOPPING;
			sr.setProperties(getServiceProperties());
		}
		// if the status is stopped then unregister the service
		if ((status & IAppContext.STOPPED) != 0 && (this.status & IAppContext.STOPPED) == 0) {
			sr.unregister();
			((EclipseAppDescriptor)getApplicationDescriptor()).appHandleDestroyed();
		}
		this.status = status;
	}

	public int getAppStatus() {
		return status;
	}

	public Map getArguments() {
		return arguments;
	}

	void setArguments(Map arguments) {
		this.arguments = arguments;
	}

	public IConfigurationElement getConfiguration() {
		IExtension applicationExtension = ((EclipseAppDescriptor)getApplicationDescriptor()).getContainerManager().getAppExtension(getApplicationDescriptor().getApplicationId());
		IConfigurationElement[] configs = applicationExtension.getConfigurationElements();
		if (configs.length == 0)
			throw new RuntimeException(NLS.bind(Messages.application_invalidExtension, getApplicationDescriptor().getApplicationId()));
		return configs[0];
	}

	public Exception getLaunchException() {
		return appNotFound;
	}
}
