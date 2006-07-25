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

import java.security.AccessController;
import java.util.*;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.application.ApplicationDescriptor;
import org.osgi.service.application.ApplicationHandle;
import org.osgi.service.condpermadmin.BundleSignerCondition;
import org.osgi.service.condpermadmin.ConditionInfo;

/*
 * An ApplicationDescriptor for an eclipse application.
 */
public class EclipseAppDescriptor extends ApplicationDescriptor {
	static final String APP_TYPE = "eclipse.application.type"; //$NON-NLS-1$
	static final String APP_TYPE_MAIN_TREAD = "main.thread"; //$NON-NLS-1$
	private ServiceRegistration sr;
	private Boolean locked = Boolean.FALSE;
	private EclipseAppContainer appContainer;
	private String contributor;
	private boolean visible;

	protected EclipseAppDescriptor(String contributor, String pid, boolean visible, EclipseAppContainer appContainer) {
		super(pid);
		this.contributor = contributor;
		this.appContainer = appContainer;
		this.locked = AppPersistence.isLocked(this) ? Boolean.TRUE : Boolean.FALSE;
		this.visible = visible;
	}

	protected Map getPropertiesSpecific(String locale) {
		// just use the service properties; for now we do not localize any properties
		return getServiceProperties();
	}

	protected synchronized ApplicationHandle launchSpecific(Map arguments) throws Exception {
		// we only allow one application to run at a time; do not launch an application
		// if one is already launched or if this application is locked.
		if (locked.booleanValue())
			throw new IllegalStateException("Cannot launch a locked application."); //$NON-NLS-1$
		// initialize the appHandle
		EclipseAppHandle appHandle = createAppHandle();
		appHandle.setArguments(arguments);
		try {
			// use the appContainer to launch the application on the main thread.
			appContainer.launch(appHandle);
		} catch (Throwable t) {
			// be sure to destroy the appHandle if an error occurs
			appHandle.destroy();
			if (t instanceof Exception)
				throw (Exception) t;
			throw (Error) t;
		}
		return appHandle;
	}

	protected synchronized void lockSpecific() {
		locked = Boolean.TRUE;
		// make sure the service properties are updated with the latest lock info
		refreshProperties();
	}

	protected synchronized void unlockSpecific() {
		locked = Boolean.FALSE;
		// make sure the service properties are updated with the latest lock info
		refreshProperties();
	}

	void refreshProperties() {
		if (sr != null)
			sr.setProperties(getServiceProperties());
	}

	void setServiceRegistration(ServiceRegistration sr) {
		this.sr = sr;
	}

	/*
	 * Gets a snapshot of the current service properties.
	 */
	Hashtable getServiceProperties() {
		Hashtable props = new Hashtable(10);
		props.put(ApplicationDescriptor.APPLICATION_PID, getApplicationId());
		props.put(ApplicationDescriptor.APPLICATION_CONTAINER, Activator.PI_APP);
		props.put(ApplicationDescriptor.APPLICATION_LOCATION, getLocation());
		props.put(ApplicationDescriptor.APPLICATION_LAUNCHABLE, appContainer.isLocked() ? Boolean.FALSE : Boolean.TRUE);
		props.put(ApplicationDescriptor.APPLICATION_LOCKED, locked);
		props.put(ApplicationDescriptor.APPLICATION_VISIBLE, visible ? Boolean.TRUE : Boolean.FALSE);
		props.put(APP_TYPE, APP_TYPE_MAIN_TREAD);
		return props;
	}

	private String getLocation() {
		final Bundle bundle = Activator.getBundle(contributor);
		if (bundle == null)
			return ""; //$NON-NLS-1$
		return Activator.getLocation(bundle);
	}

	/*
	 * Returns the appHandle.  If it does not exist then one is created.
	 */
	private synchronized EclipseAppHandle createAppHandle() {
		// TODO not sure what instance pid should be used; for now just use the appDesciptor pid because apps are singletons anyway
		EclipseAppHandle newAppHandle = new EclipseAppHandle(getApplicationId(), this);
		ServiceRegistration appHandleReg = (ServiceRegistration) AccessController.doPrivileged(appContainer.getRegServiceAction(ApplicationHandle.class.getName(), newAppHandle, newAppHandle.getServiceProperties()));
		newAppHandle.setServiceRegistration(appHandleReg);
		return newAppHandle;
	}

	EclipseAppContainer getContainerManager() {
		return appContainer;
	}

	public boolean matchDNChain(String pattern) {
		Bundle bundle = Activator.getBundle(contributor);
		if (bundle == null)
			return false;
		return BundleSignerCondition.getCondition(bundle, new ConditionInfo(BundleSignerCondition.class.getName(), new String[] {pattern})).isSatisfied();
	}

	protected boolean isLaunchableSpecific() {
		return true;
	}

	public void unregister() {
		ServiceRegistration temp = sr;
		if (temp != null) {
			sr = null;
			temp.unregister();
		}
	}
}
