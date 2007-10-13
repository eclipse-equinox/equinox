/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.app;

import java.net.URL;
import java.security.AccessController;
import java.util.*;
import org.eclipse.equinox.app.IApplicationContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.application.*;
import org.osgi.service.condpermadmin.BundleSignerCondition;
import org.osgi.service.condpermadmin.ConditionInfo;

/*
 * An ApplicationDescriptor for an eclipse application.
 */
public class EclipseAppDescriptor extends ApplicationDescriptor {
	static final String APP_TYPE = "eclipse.application.type"; //$NON-NLS-1$
	static final String APP_DEFAULT = "eclipse.application.default"; //$NON-NLS-1$
	static final String APP_TYPE_MAIN_THREAD = "main.thread"; //$NON-NLS-1$
	static final String APP_TYPE_ANY_THREAD = "any.thread"; //$NON-NLS-1$
	static final int FLAG_VISIBLE = 0x01;
	static final int FLAG_CARD_SINGLETON_GLOGAL = 0x02;
	static final int FLAG_CARD_SINGLETON_SCOPED = 0x04;
	static final int FLAG_CARD_UNLIMITED = 0x08;
	static final int FLAG_CARD_LIMITED = 0x10;
	static final int FLAG_TYPE_MAIN_THREAD = 0x20;
	static final int FLAG_TYPE_ANY_THREAD = 0x40;
	static final int FLAG_DEFAULT_APP = 0x80;
	private long instanceID = 0;
	private ServiceRegistration sr;
	private Boolean locked = Boolean.FALSE;
	private final EclipseAppContainer appContainer;
	private final Bundle contributor;
	private final int flags;
	private final int cardinality;
	private final String name;
	private final URL iconURL;
	private final boolean[] registrationLock = new boolean[] {true};

	protected EclipseAppDescriptor(Bundle contributor, String pid, String name, String iconPath, int flags, int cardinality, EclipseAppContainer appContainer) {
		super(pid);
		this.name = name;
		this.contributor = contributor;
		this.appContainer = appContainer;
		this.locked = AppPersistence.isLocked(this) ? Boolean.TRUE : Boolean.FALSE;
		this.flags = flags;
		this.cardinality = cardinality;
		URL iconResult = null;
		// this bit of code is complex because we want to search fragments;
		// that can only be done by using the Bundle.findEntries method which
		// requires the path to be split up between the base and the file name!!
		if (iconPath != null && iconPath.length() > 0) {
			if (iconPath.charAt(0) == '/')
				iconPath = iconPath.substring(1);
			String baseIconDir = "/"; //$NON-NLS-1$
			String iconFile = iconPath;
			int lastSlash = iconPath.lastIndexOf('/');
			if (lastSlash > 0 && lastSlash < iconPath.length() - 1) {
				baseIconDir = iconPath.substring(0, lastSlash);
				iconFile = iconPath.substring(lastSlash + 1);
			}
			Enumeration urls = contributor.findEntries(baseIconDir, iconFile, false);
			if (urls != null && urls.hasMoreElements())
				iconResult = (URL) urls.nextElement();
		}
		this.iconURL = iconResult;
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
			try {
				appHandle.destroy();
			} catch (Throwable destroyError) {
				// ignore and clean up
			}
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

	void setServiceRegistration(ServiceRegistration sr) {
		synchronized (registrationLock) {
			this.sr = sr;
			registrationLock[0] = sr != null;
			registrationLock.notifyAll();
		}

	}

	private ServiceRegistration getServiceRegistration() {
		synchronized (registrationLock) {
			if (sr == null && registrationLock[0])
				try {
					registrationLock.wait(1000); // timeout after 1 second
				} catch (InterruptedException e) {
					// nothing
				}
			return sr;
		}
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
		if (name != null)
			props.put(ApplicationDescriptor.APPLICATION_NAME, name);
		props.put(ApplicationDescriptor.APPLICATION_CONTAINER, Activator.PI_APP);
		props.put(ApplicationDescriptor.APPLICATION_LOCATION, getLocation());
		Boolean launchable = appContainer.isLocked(this) == 0 ? Boolean.TRUE : Boolean.FALSE;
		props.put(ApplicationDescriptor.APPLICATION_LAUNCHABLE, launchable);
		props.put(ApplicationDescriptor.APPLICATION_LOCKED, getLocked());
		Boolean visible = (flags & FLAG_VISIBLE) != 0 ? Boolean.TRUE : Boolean.FALSE;
		props.put(ApplicationDescriptor.APPLICATION_VISIBLE, visible);
		props.put(APP_TYPE, getThreadTypeString());
		if ((flags & FLAG_DEFAULT_APP) != 0)
			props.put(APP_DEFAULT, Boolean.TRUE);
		if (iconURL != null)
			props.put(ApplicationDescriptor.APPLICATION_ICON, iconURL);
		return props;
	}

	private String getLocation() {
		if (contributor == null)
			return ""; //$NON-NLS-1$
		return Activator.getLocation(contributor);
	}

	/*
	 * Returns the appHandle.  If it does not exist then one is created.
	 */
	private EclipseAppHandle createAppHandle(Map arguments) throws ApplicationException {
		EclipseAppHandle newAppHandle = new EclipseAppHandle(getInstanceID(), arguments, this);
		appContainer.lock(newAppHandle);
		ServiceRegistration appHandleReg = (ServiceRegistration) AccessController.doPrivileged(appContainer.getRegServiceAction(new String[] {ApplicationHandle.class.getName(), IApplicationContext.class.getName()}, newAppHandle, newAppHandle.getServiceProperties()));
		newAppHandle.setServiceRegistration(appHandleReg);
		return newAppHandle;
	}

	EclipseAppContainer getContainerManager() {
		return appContainer;
	}

	public boolean matchDNChain(String pattern) {
		if (contributor == null)
			return false;
		return BundleSignerCondition.getCondition(contributor, new ConditionInfo(BundleSignerCondition.class.getName(), new String[] {pattern})).isSatisfied();
	}

	protected boolean isLaunchableSpecific() {
		return true;
	}

	public void unregister() {
		ServiceRegistration temp = getServiceRegistration();
		if (temp != null) {
			setServiceRegistration(null);
			temp.unregister();
		}
	}

	String getThreadTypeString() {
		if ((flags & FLAG_TYPE_ANY_THREAD) != 0)
			return APP_TYPE_ANY_THREAD;
		return APP_TYPE_MAIN_THREAD;
	}

	int getThreadType() {
		return flags & (FLAG_TYPE_ANY_THREAD | FLAG_TYPE_MAIN_THREAD);
	}

	int getCardinalityType() {
		return flags & (FLAG_CARD_SINGLETON_GLOGAL | FLAG_CARD_SINGLETON_SCOPED | FLAG_CARD_LIMITED | FLAG_CARD_UNLIMITED);
	}

	int getCardinality() {
		return cardinality;
	}

	private synchronized String getInstanceID() {
		// make sure the instanceID has not reached the max
		if (instanceID == Long.MAX_VALUE)
			instanceID = 0;
		// create a unique instance id
		return getApplicationId() + "." + instanceID++; //$NON-NLS-1$
	}
}
