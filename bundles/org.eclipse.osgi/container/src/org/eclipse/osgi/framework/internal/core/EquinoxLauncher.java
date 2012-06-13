/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
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
import java.security.cert.X509Certificate;
import java.util.*;
import org.eclipse.core.runtime.adaptor.EclipseStarter;
import org.eclipse.osgi.baseadaptor.BaseAdaptor;
import org.eclipse.osgi.framework.adaptor.FrameworkAdaptor;
import org.osgi.framework.*;

public class EquinoxLauncher implements org.osgi.framework.launch.Framework {

	private volatile Framework framework;
	private volatile Bundle systemBundle;
	private final Map<String, String> configuration;
	private volatile ConsoleManager consoleMgr = null;

	public EquinoxLauncher(Map<String, String> configuration) {
		this.configuration = configuration;
	}

	public void init() {
		checkAdminPermission(AdminPermission.EXECUTE);
		if (System.getSecurityManager() == null)
			internalInit();
		else {
			AccessController.doPrivileged(new PrivilegedAction<Object>() {
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

	private void setEquinoxProperties(Map<String, String> configuration) {
		Object threadBehavior = configuration == null ? null : configuration.get(Framework.PROP_FRAMEWORK_THREAD);
		if (threadBehavior == null) {
			if (FrameworkProperties.getProperty(Framework.PROP_FRAMEWORK_THREAD) == null)
				FrameworkProperties.setProperty(Framework.PROP_FRAMEWORK_THREAD, Framework.THREAD_NORMAL);
		} else {
			FrameworkProperties.setProperty(Framework.PROP_FRAMEWORK_THREAD, (String) threadBehavior);
		}

		// set the compatibility boot delegation flag to false to get "standard" OSGi behavior WRT boot delegation (bug 344850)
		if (FrameworkProperties.getProperty(Constants.OSGI_COMPATIBILITY_BOOTDELEGATION) == null)
			FrameworkProperties.setProperty(Constants.OSGI_COMPATIBILITY_BOOTDELEGATION, "false"); //$NON-NLS-1$
		// set the support for multiple host to true to get "standard" OSGi behavior (bug 344850)
		if (FrameworkProperties.getProperty("osgi.support.multipleHosts") == null) //$NON-NLS-1$
			FrameworkProperties.setProperty("osgi.support.multipleHosts", "true"); //$NON-NLS-1$ //$NON-NLS-2$
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
		// check each osgi clean property and set the appropriate equinox one
		Object clean = configuration.get(Constants.FRAMEWORK_STORAGE_CLEAN);
		if (Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT.equals(clean)) {
			// remove this so we only clean on first init
			configuration.remove(Constants.FRAMEWORK_STORAGE_CLEAN);
			FrameworkProperties.setProperty(EclipseStarter.PROP_CLEAN, Boolean.TRUE.toString());
		}
	}

	public FrameworkEvent waitForStop(long timeout) throws InterruptedException {
		Framework current = framework;
		if (current == null)
			return new FrameworkEvent(FrameworkEvent.STOPPED, this, null);
		return current.waitForStop(timeout);
	}

	public Enumeration<URL> findEntries(String path, String filePattern, boolean recurse) {
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

	public Enumeration<String> getEntryPaths(String path) {
		Bundle current = systemBundle;
		if (current == null)
			return null;
		return current.getEntryPaths(path);
	}

	public Dictionary<String, String> getHeaders() {
		Bundle current = systemBundle;
		if (current == null)
			return null;
		return current.getHeaders();
	}

	public Dictionary<String, String> getHeaders(String locale) {
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

	public ServiceReference<?>[] getRegisteredServices() {
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

	public Enumeration<URL> getResources(String name) throws IOException {
		Bundle current = systemBundle;
		if (current == null)
			return null;
		return current.getResources(name);
	}

	public ServiceReference<?>[] getServicesInUse() {
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

	public Class<?> loadClass(String name) throws ClassNotFoundException {
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
				AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
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
			level = Integer.parseInt(configuration.get(Constants.FRAMEWORK_BEGINNING_STARTLEVEL));
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

	public Map<X509Certificate, List<X509Certificate>> getSignerCertificates(int signersType) {
		Bundle current = systemBundle;
		if (current != null)
			return current.getSignerCertificates(signersType);
		@SuppressWarnings("unchecked")
		final Map<X509Certificate, List<X509Certificate>> empty = Collections.EMPTY_MAP;
		return empty;
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
		Bundle current = systemBundle;
		if (current != null)
			return current.compareTo(o);
		throw new IllegalStateException();
	}

	public File getDataFile(String filename) {
		Bundle current = systemBundle;
		if (current != null)
			return current.getDataFile(filename);
		return null;
	}
}
