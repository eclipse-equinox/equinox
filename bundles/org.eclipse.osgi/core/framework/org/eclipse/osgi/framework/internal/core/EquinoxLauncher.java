/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.framework.internal.core;

import java.io.*;
import java.net.URL;
import java.security.*;
import java.util.*;
import org.eclipse.core.runtime.adaptor.EclipseStarter;
import org.eclipse.core.runtime.adaptor.LocationManager;
import org.eclipse.osgi.baseadaptor.BaseAdaptor;
import org.eclipse.osgi.framework.adaptor.FrameworkAdaptor;
import org.osgi.framework.*;

public class EquinoxLauncher implements org.osgi.framework.launch.Framework {

	private volatile Framework framework;
	private volatile Bundle systemBundle;
	private final Map configuration;
	private volatile ConsoleManager consoleMgr = null;

	public EquinoxLauncher(Map configuration) {
		this.configuration = configuration;
	}

	public void init() {
		checkAdminPermission(AdminPermission.EXECUTE);
		if (System.getSecurityManager() == null)
			internalInit();
		else {
			AccessController.doPrivileged(new PrivilegedAction() {
				public Object run() {
					internalInit();
					return null;
				}
			});
		}
	}

	synchronized Framework internalInit() {
		if ((getState() & (Bundle.ACTIVE | Bundle.STARTING | Bundle.STOPPING)) != 0)
			return framework; // no op

		if (System.getSecurityManager() != null && configuration.get(Constants.FRAMEWORK_SECURITY) != null)
			throw new SecurityException("Cannot specify the \"" + Constants.FRAMEWORK_SECURITY + "\" configuration property when a security manager is already installed."); //$NON-NLS-1$ //$NON-NLS-2$

		Framework current = framework;
		if (current != null) {
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
			consoleMgr = ConsoleManager.startConsole(current);
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

	private void setEquinoxProperties(Map configuration) {
		Object threadBehavior = configuration == null ? null : configuration.get(Framework.PROP_FRAMEWORK_THREAD);
		if (threadBehavior == null) {
			if (FrameworkProperties.getProperty(Framework.PROP_FRAMEWORK_THREAD) == null)
				FrameworkProperties.setProperty(Framework.PROP_FRAMEWORK_THREAD, Framework.THREAD_NORMAL);
		} else {
			FrameworkProperties.setProperty(Framework.PROP_FRAMEWORK_THREAD, (String) threadBehavior);
		}

		// first check props we are required to provide reasonable defaults for
		Object windowSystem = configuration == null ? null : configuration.get(Constants.FRAMEWORK_WINDOWSYSTEM);
		if (windowSystem == null) {
			windowSystem = FrameworkProperties.getProperty(EclipseStarter.PROP_WS);
			if (windowSystem != null)
				FrameworkProperties.setProperty(Constants.FRAMEWORK_WINDOWSYSTEM, (String) windowSystem);
		}
		// rest of props can be ignored if the configuration is null
		if (configuration == null)
			return;
		// check each osgi defined property and set the appropriate equinox one
		Object security = configuration.get(Constants.FRAMEWORK_SECURITY);
		if (security != null) {
			if (Framework.SECURITY_OSGI.equals(security))
				FrameworkProperties.setProperty(Framework.PROP_EQUINOX_SECURITY, Framework.SECURITY_OSGI);
			else if (security instanceof String)
				FrameworkProperties.setProperty(Framework.PROP_EQUINOX_SECURITY, (String) security);
		}
		Object storage = configuration.get(Constants.FRAMEWORK_STORAGE);
		if (storage != null && storage instanceof String)
			FrameworkProperties.setProperty(LocationManager.PROP_CONFIG_AREA, (String) storage);
		Object clean = configuration.get(Constants.FRAMEWORK_STORAGE_CLEAN);
		if (Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT.equals(clean)) {
			// remove this so we only clean on first init
			configuration.remove(Constants.FRAMEWORK_STORAGE_CLEAN);
			FrameworkProperties.setProperty(EclipseStarter.PROP_CLEAN, Boolean.TRUE.toString());
		}
		Object parentCL = configuration.get(Constants.FRAMEWORK_BUNDLE_PARENT);
		if (Constants.FRAMEWORK_BUNDLE_PARENT_FRAMEWORK.equals(parentCL))
			parentCL = "fwk"; //$NON-NLS-1$
		if (parentCL instanceof String)
			FrameworkProperties.setProperty("osgi.parentClassloader", (String) parentCL); //$NON-NLS-1$
	}

	public FrameworkEvent waitForStop(long timeout) throws InterruptedException {
		Framework current = framework;
		if (current == null)
			return new FrameworkEvent(FrameworkEvent.STOPPED, this, null);
		return current.waitForStop(timeout);
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
		checkAdminPermission(AdminPermission.EXECUTE);
		if (System.getSecurityManager() == null)
			internalStart();
		else
			try {
				AccessController.doPrivileged(new PrivilegedExceptionAction() {
					public Object run() {
						internalStart();
						return null;
					}
				});
			} catch (PrivilegedActionException e) {
				throw (BundleException) e.getException();
			}
	}

	private void checkAdminPermission(String actions) {
		SecurityManager sm = System.getSecurityManager();
		if (sm != null)
			sm.checkPermission(new AdminPermission(this, actions));
	}

	void internalStart() {
		if (getState() == Bundle.ACTIVE)
			return;
		Framework current = internalInit();
		int level = 1;
		try {
			level = Integer.parseInt((String) configuration.get(Constants.FRAMEWORK_BEGINNING_STARTLEVEL));
		} catch (Throwable t) {
			// do nothing
		}
		current.startLevelManager.doSetStartLevel(level);
	}

	public void stop(int options) throws BundleException {
		stop();
	}

	public void stop() throws BundleException {
		Bundle current = systemBundle;
		if (current == null)
			return;
		ConsoleManager currentConsole = consoleMgr;
		if (currentConsole != null) {
			currentConsole.stopConsole();
			consoleMgr = null;
		}
		current.stop();
	}

	public void uninstall() throws BundleException {
		throw new BundleException(Msg.BUNDLE_SYSTEMBUNDLE_UNINSTALL_EXCEPTION, BundleException.INVALID_OPERATION);
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

	public Map getSignerCertificates(int signersType) {
		Bundle current = systemBundle;
		if (current != null)
			return current.getSignerCertificates(signersType);
		return Collections.EMPTY_MAP;
	}

	public Version getVersion() {
		Bundle current = systemBundle;
		if (current != null)
			return current.getVersion();
		return Version.emptyVersion;
	}

	public <A> A adapt(Class<A> adapterType) {
		Bundle current = systemBundle;
		if (current != null) {
			return current.adapt(adapterType);
		}
		return null;
	}

	public int compareTo(Bundle o) {
		// TODO need to implement
		throw new UnsupportedOperationException("need to implement");
	}

	public File getDataFile(String filename) {
		Bundle current = systemBundle;
		if (current != null)
			return current.getDataFile(filename);
		return null;
	}
}
