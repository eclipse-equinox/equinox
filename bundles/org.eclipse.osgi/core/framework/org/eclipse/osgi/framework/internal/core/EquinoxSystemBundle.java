/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.framework.internal.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.adaptor.EclipseStarter;
import org.eclipse.core.runtime.adaptor.LocationManager;
import org.eclipse.osgi.baseadaptor.BaseAdaptor;
import org.eclipse.osgi.framework.adaptor.FrameworkAdaptor;
import org.osgi.framework.*;
import org.osgi.framework.launch.SystemBundle;

public class EquinoxSystemBundle implements SystemBundle {

	private static String FULLPATH = " [fullpath]"; //$NON-NLS-1$
	private volatile Framework framework;
	private volatile Bundle systemBundle;

	public void init(Properties configuration) {
		internalInit(configuration);
	}

	private Framework internalInit(Properties configuration) {
		Framework current = framework;
		if (current != null) {
			// this is not really necessary because the Equinox class will 
			current.close();
			framework = null;
			systemBundle = null;
		}
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			FrameworkProperties.setProperties(configuration);
			FrameworkProperties.initializeProperties();
			// make sure the active framework thread is used
			setEquinoxProperties(configuration);
			current = new Framework(new BaseAdaptor(new String[0]));
			current.launch();
			framework = current;
			systemBundle = current.systemBundle;
		} finally {
			ClassLoader currentCCL = Thread.currentThread().getContextClassLoader();
			if (currentCCL != tccl)
				Thread.currentThread().setContextClassLoader(tccl);
		}
		return current;
	}

	private void setEquinoxProperties(Properties configuration) {
		// always need to use an active thread
		FrameworkProperties.setProperty(Framework.PROP_FRAMEWORK_THREAD, Framework.THREAD_NORMAL);

		// first check props we are required to provide reasonable defaults for
		String windowSystem = configuration == null ? null : configuration.getProperty(SystemBundle.WINDOWSYSTEM);
		if (windowSystem == null) {
			windowSystem = FrameworkProperties.getProperty(EclipseStarter.PROP_WS);
			if (windowSystem != null)
				FrameworkProperties.setProperty(SystemBundle.WINDOWSYSTEM, windowSystem);
		}
		// rest of props can be ignored if the configuration is null
		if (configuration == null)
			return;
		// check each osgi defined property and set the appropriate equinox one
		String security = configuration.getProperty(SystemBundle.SECURITY);
		if (security != null) {
			if (Boolean.valueOf(security).booleanValue())
				FrameworkProperties.setProperty(Framework.PROP_EQUINOX_SECURITY, Framework.SECURITY_OSGI);
			else
				FrameworkProperties.setProperty(Framework.PROP_EQUINOX_SECURITY, security);
		}
		String storage = configuration.getProperty(SystemBundle.STORAGE);
		if (storage != null) {
			FrameworkProperties.setProperty(LocationManager.PROP_CONFIG_AREA, storage);
			FrameworkProperties.setProperty(LocationManager.PROP_CONFIG_AREA_DEFAULT, storage);
		}
		String execPermission = configuration.getProperty(SystemBundle.EXECPERMISSION);
		if (execPermission != null) {
			if (!execPermission.endsWith(FULLPATH))
				execPermission = execPermission + FULLPATH;
			FrameworkProperties.setProperty("osgi.filepermissions.command", execPermission); //$NON-NLS-1$
		}

	}

	public void waitForStop(long timeout) throws InterruptedException {
		Framework current = framework;
		if (current == null)
			return;
		current.waitForStop(timeout);
	}

	public Enumeration findEntries(String path, String filePattern, boolean recurse) {
		Bundle current = systemBundle;
		if (current == null)
			return null;
		return current.findEntries(path, filePattern, recurse);
	}

	public BundleContext getBundleContext() {
		Bundle current = systemBundle;
		if (current == null)
			return null;
		return current.getBundleContext();
	}

	public long getBundleId() {
		return 0;
	}

	public URL getEntry(String path) {
		Bundle current = systemBundle;
		if (current == null)
			return null;
		return current.getEntry(path);
	}

	public Enumeration getEntryPaths(String path) {
		Bundle current = systemBundle;
		if (current == null)
			return null;
		return current.getEntryPaths(path);
	}

	public Dictionary getHeaders() {
		Bundle current = systemBundle;
		if (current == null)
			return null;
		return current.getHeaders();
	}

	public Dictionary getHeaders(String locale) {
		Bundle current = systemBundle;
		if (current == null)
			return null;
		return current.getHeaders(locale);
	}

	public long getLastModified() {
		Bundle current = systemBundle;
		if (current == null)
			return System.currentTimeMillis();
		return current.getLastModified();
	}

	public String getLocation() {
		return Constants.SYSTEM_BUNDLE_LOCATION;
	}

	public ServiceReference[] getRegisteredServices() {
		Bundle current = systemBundle;
		if (current == null)
			return null;
		return current.getRegisteredServices();
	}

	public URL getResource(String name) {
		Bundle current = systemBundle;
		if (current == null)
			return null;
		return current.getResource(name);
	}

	public Enumeration getResources(String name) throws IOException {
		Bundle current = systemBundle;
		if (current == null)
			return null;
		return current.getResources(name);
	}

	public ServiceReference[] getServicesInUse() {
		Bundle current = systemBundle;
		if (current == null)
			return null;
		return current.getServicesInUse();
	}

	public int getState() {
		Bundle current = systemBundle;
		if (current == null)
			return Bundle.INSTALLED;
		return current.getState();
	}

	public String getSymbolicName() {
		return FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME;
	}

	public boolean hasPermission(Object permission) {
		Bundle current = systemBundle;
		if (current == null)
			return false;
		return current.hasPermission(permission);
	}

	public Class loadClass(String name) throws ClassNotFoundException {
		Bundle current = systemBundle;
		if (current == null)
			return null;
		return current.loadClass(name);
	}

	public void start(int options) throws BundleException {
		start();
	}

	/**
	 * @throws BundleException  
	 */
	public void start() throws BundleException {
		Framework current = framework;
		if (getState() == Bundle.ACTIVE)
			return;
		if (getState() != Bundle.STARTING)
			current = internalInit(null);
		current.startLevelManager.doSetStartLevel(1);
	}

	public void stop(int options) throws BundleException {
		stop();
	}

	public void stop() throws BundleException {
		Bundle current = systemBundle;
		if (current == null)
			return;
		current.stop();
	}

	public void uninstall() throws BundleException {
		throw new BundleException(Msg.BUNDLE_SYSTEMBUNDLE_UNINSTALL_EXCEPTION);
	}

	public void update() throws BundleException {
		Bundle current = systemBundle;
		if (current == null)
			return;
		current.update();
	}

	public void update(InputStream in) throws BundleException {
		try {
			in.close();
		} catch (IOException e) {
			// nothing; just being nice
		}
		update();
	}

}
