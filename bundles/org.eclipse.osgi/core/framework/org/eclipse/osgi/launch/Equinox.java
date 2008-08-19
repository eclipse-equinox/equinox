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
package org.eclipse.osgi.launch;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import org.eclipse.osgi.framework.internal.core.FrameworkProperties;
import org.osgi.framework.*;
import org.osgi.framework.launch.SystemBundle;

/**
 * @since 3.5
 */
public class Equinox implements SystemBundle {
	private static final String implName = "org.eclipse.osgi.framework.internal.core.EquinoxSystemBundle";
	/**@GuardedBy this*/
	private SystemBundle impl;
	private final boolean useSeparateCL;

	public Equinox() {
		useSeparateCL = FrameworkProperties.inUse();
	}

	private SystemBundle createImpl() {
		try {
			Class implClazz = getImplClass();
			return (SystemBundle) implClazz.newInstance();
		} catch (ClassNotFoundException e) {
			throw new NoClassDefFoundError(implName);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e.getMessage());
		} catch (InstantiationException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	private Class getImplClass() throws ClassNotFoundException {
		ClassLoader thisCL = this.getClass().getClassLoader();
		if (!(useSeparateCL && (thisCL instanceof URLClassLoader)))
			return Class.forName(implName);
		URL[] cp = ((URLClassLoader) thisCL).getURLs();
		EquinoxFWClassLoader fwCL = new EquinoxFWClassLoader(cp, thisCL);
		return fwCL.loadClass(implName);
	}

	private synchronized SystemBundle getImpl() {
		if (impl == null)
			impl = createImpl();
		return impl;
	}

	public void init(Properties configuration) {
		if ((getState() & (Bundle.ACTIVE | Bundle.STARTING | Bundle.STOPPING)) != 0)
			throw new IllegalStateException("Framework is active!!");
		synchronized (this) {
			if (impl != null && impl.getState() != Bundle.INSTALLED) {
				try {
					impl.stop();
					impl.waitForStop(30000); // timeout after 30 seconds
				} catch (BundleException e) {
					// ignore
				} catch (InterruptedException e) {
					// continue
				}
				impl = null;
			}
		}
		getImpl().init(configuration);
	}

	public void waitForStop(long timeout) throws InterruptedException {
		getImpl().waitForStop(timeout);
	}

	public Enumeration findEntries(String path, String filePattern, boolean recurse) {
		return getImpl().findEntries(path, filePattern, recurse);
	}

	public BundleContext getBundleContext() {
		return getImpl().getBundleContext();
	}

	public long getBundleId() {
		return getImpl().getBundleId();
	}

	public URL getEntry(String path) {
		return getImpl().getEntry(path);
	}

	public Enumeration getEntryPaths(String path) {
		return getImpl().getEntryPaths(path);
	}

	public Dictionary getHeaders() {
		return getImpl().getHeaders();
	}

	public Dictionary getHeaders(String locale) {
		return getImpl().getHeaders(locale);
	}

	public long getLastModified() {
		return getImpl().getLastModified();
	}

	public String getLocation() {
		return getImpl().getLocation();
	}

	public ServiceReference[] getRegisteredServices() {
		return getImpl().getRegisteredServices();
	}

	public URL getResource(String name) {
		return getImpl().getResource(name);
	}

	public Enumeration getResources(String name) throws IOException {
		return getImpl().getResources(name);
	}

	public ServiceReference[] getServicesInUse() {
		return getImpl().getServicesInUse();
	}

	public int getState() {
		return getImpl().getState();
	}

	public String getSymbolicName() {
		return getImpl().getSymbolicName();
	}

	public boolean hasPermission(Object permission) {
		return getImpl().hasPermission(permission);
	}

	public Class loadClass(String name) throws ClassNotFoundException {
		return getImpl().loadClass(name);
	}

	public void start(int options) throws BundleException {
		getImpl().start(options);
	}

	public void start() throws BundleException {
		getImpl().start();
	}

	public void stop(int options) throws BundleException {
		getImpl().stop(options);
	}

	public void stop() throws BundleException {
		getImpl().stop();
	}

	public void uninstall() throws BundleException {
		getImpl().uninstall();
	}

	public void update() throws BundleException {
		getImpl().update();
	}

	public void update(InputStream in) throws BundleException {
		getImpl().update(in);
	}

}
