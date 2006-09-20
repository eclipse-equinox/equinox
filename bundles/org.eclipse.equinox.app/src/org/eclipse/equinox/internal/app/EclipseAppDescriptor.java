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
import org.eclipse.equinox.app.IApplicationContext;
import org.osgi.framework.*;
import org.osgi.service.application.ApplicationDescriptor;
import org.osgi.service.application.ApplicationHandle;
import org.osgi.service.condpermadmin.BundleSignerCondition;
import org.osgi.service.condpermadmin.ConditionInfo;

/*
 * An ApplicationDescriptor for an eclipse application.
 */
public class EclipseAppDescriptor extends ApplicationDescriptor {
	static final String APP_TYPE = "eclipse.application.type"; //$NON-NLS-1$
	static final String APP_TYPE_MAIN_THREAD = "main.thread"; //$NON-NLS-1$
	static final String APP_TYPE_ANY_THREAD = "any.thread"; //$NON-NLS-1$
	static final int FLAG_VISIBLE = 0x01;
	static final int FLAG_SINGLETON = 0x02;
	static final int FLAG_TYPE_MAIN_THREAD = 0x04;
	static final int FLAG_TYPE_ANY_THREAD = 0x08;
	private ServiceRegistration sr;
	private Boolean locked = Boolean.FALSE;
	private EclipseAppContainer appContainer;
	private String contributor;
	private int flags;

	protected EclipseAppDescriptor(String contributor, String pid, int flags, EclipseAppContainer appContainer) {
		super(pid);
		this.contributor = contributor;
		this.appContainer = appContainer;
		this.locked = AppPersistence.isLocked(this) ? Boolean.TRUE : Boolean.FALSE;
		this.flags = flags;
	}

	protected Map getPropertiesSpecific(String locale) {
		// just use the service properties; for now we do not localize any properties
		return getServiceProperties();
	}

	protected ApplicationHandle launchSpecific(Map arguments) throws Exception {
		// if this application is locked throw an exception.
		if (getLocked().booleanValue())
			throw new IllegalStateException("Cannot launch a locked application."); //$NON-NLS-1$
		// initialize the appHandle
		EclipseAppHandle appHandle = createAppHandle(arguments);
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
		ServiceRegistration reg = getServiceRegistration();
		if (reg != null)
			try {
				reg.setProperties(getServiceProperties());
			} catch (IllegalStateException e) {
				// this must mean the service was unregistered
				// just ignore
			}
		
	}

	synchronized void setServiceRegistration(ServiceRegistration sr) {
		this.sr = sr;
	}

	private synchronized ServiceRegistration getServiceRegistration() {
		return sr;
	}

	private synchronized Boolean getLocked() {
		return locked;
	}

	/*
	 * Gets a snapshot of the current service properties.
	 */
	Hashtable getServiceProperties() {
		Hashtable props = new Hashtable(10);
		props.put(ApplicationDescriptor.APPLICATION_PID, getApplicationId());
		props.put(ApplicationDescriptor.APPLICATION_CONTAINER, Activator.PI_APP);
		props.put(ApplicationDescriptor.APPLICATION_LOCATION, getLocation());
		Boolean launchable = appContainer.isLocked(this) ? Boolean.FALSE : Boolean.TRUE;
		props.put(ApplicationDescriptor.APPLICATION_LAUNCHABLE, launchable);
		props.put(ApplicationDescriptor.APPLICATION_LOCKED, getLocked());
		Boolean visible = (flags & FLAG_VISIBLE) != 0 ? Boolean.TRUE : Boolean.FALSE;
		props.put(ApplicationDescriptor.APPLICATION_VISIBLE, visible);
		props.put(APP_TYPE, getTypeString());
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
	private EclipseAppHandle createAppHandle(Map arguments) {
		// TODO not sure what instance pid should be used; for now just use the appDesciptor pid because apps are singletons anyway
		EclipseAppHandle newAppHandle = new EclipseAppHandle(getApplicationId(), arguments, this);
		ServiceRegistration appHandleReg = (ServiceRegistration) AccessController.doPrivileged(appContainer.getRegServiceAction(new String[] {ApplicationHandle.class.getName(), IApplicationContext.class.getName()}, newAppHandle, newAppHandle.getServiceProperties()));
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

	public synchronized void unregister() {
		ServiceRegistration temp = sr;
		if (temp != null) {
			sr = null;
			temp.unregister();
		}
	}

	String getTypeString() {
		if ((flags & FLAG_TYPE_ANY_THREAD) != 0)
			return APP_TYPE_ANY_THREAD;
		return APP_TYPE_MAIN_THREAD;
	}

	public int getType() {
		return flags & (FLAG_TYPE_ANY_THREAD | FLAG_TYPE_MAIN_THREAD);
	}

	public boolean isSingleton() {
		return (flags & FLAG_SINGLETON) != 0;
	}
}
