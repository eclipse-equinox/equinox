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
	private ServiceRegistration sr;
	private Boolean locked = Boolean.FALSE;
	private SingletonContainerMgr singletonMgr;
	private ContainerManager containerMgr;
	private String namespace;
	private String type;
	private RuntimeException error;

	protected EclipseAppDescriptor(String namespace, String pid, String type, ContainerManager containerMgr) {
		super(pid);
		this.type = type == null ? ContainerManager.APP_TYPE_MAIN_SINGLETON : type;
		this.namespace = namespace;
		this.containerMgr = containerMgr;
		this.locked = AppManager.isLocked(this) ? Boolean.TRUE : Boolean.FALSE;
	}

	protected EclipseAppDescriptor(String namespace, String pid, String type, ContainerManager containerMgr, RuntimeException error) {
		this(namespace, pid, type, containerMgr);
		this.error = error;
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
			// use the containerMgr to launch the application on the main thread.
			containerMgr.launch(appHandle);
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

	/*
	 * Indicates to this descriptor that the appHandle was destroyed
	 */
	synchronized void appHandleDestroyed() {
		if (singletonMgr != null)
			singletonMgr.unlock();
	}

	void refreshProperties() {
		if (sr != null)
			sr.setProperties(getServiceProperties());
	}

	void setSingletonMgr(SingletonContainerMgr singletonMgr) {
		this.singletonMgr = singletonMgr;
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
		props.put(ApplicationDescriptor.APPLICATION_LAUNCHABLE, singletonMgr == null ? Boolean.TRUE : singletonMgr.isLocked() ? Boolean.FALSE : Boolean.TRUE);
		props.put(ApplicationDescriptor.APPLICATION_LOCKED, locked);
		props.put(ApplicationDescriptor.APPLICATION_VISIBLE, Boolean.TRUE);
		return props;
	}

	private String getLocation() {
		final Bundle bundle = AppManager.getBundle(namespace);
		if (bundle == null)
			return ""; //$NON-NLS-1$
		return AppManager.getLocation(bundle);
	}

	/*
	 * Returns the appHandle.  If it does not exist then one is created.
	 */
	private synchronized EclipseAppHandle createAppHandle() {
		// TODO not sure what instance pid should be used; for now just use the appDesciptor pid because apps are singletons anyway
		EclipseAppHandle newAppHandle;
		if (error == null)
			newAppHandle = new EclipseAppHandle(getApplicationId(), this);
		else
			newAppHandle = new EclipseAppHandle(error, containerMgr);
		ServiceRegistration appHandleReg = (ServiceRegistration) AccessController.doPrivileged(containerMgr.getRegServiceAction(ApplicationHandle.class.getName(), newAppHandle, newAppHandle.getServiceProperties()));
		newAppHandle.setServiceRegistration(appHandleReg);
		return newAppHandle;
	}

	ContainerManager getContainerManager() {
		return containerMgr;
	}

	String getType() {
		return type;
	}

	public boolean matchDNChain(String pattern) {
		Bundle bundle = AppManager.getBundle(namespace);
		if (bundle == null)
			return false;
		return BundleSignerCondition.getCondition(bundle, new ConditionInfo(BundleSignerCondition.class.getName(), new String[] {pattern})).isSatisfied();
	}

	protected boolean isLaunchableSpecific() {
		// TODO Auto-generated method stub
		return true;
	}

	public void unregister() {
		if (sr != null)
			sr.unregister();
	}
}
