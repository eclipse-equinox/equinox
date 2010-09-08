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
package org.eclipse.osgi.launch;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.*;
import org.eclipse.osgi.framework.internal.core.FrameworkProperties;
import org.eclipse.osgi.framework.util.Headers;
import org.eclipse.osgi.internal.baseadaptor.DevClassPathHelper;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.*;
import org.osgi.framework.launch.Framework;

/**
 * The System Bundle implementation for the Equinox Framework.
 * 
 * @since 3.5
 */
public class Equinox implements Framework {
	private static final String implName = "org.eclipse.osgi.framework.internal.core.EquinoxLauncher"; //$NON-NLS-1$
	/**@GuardedBy this*/
	private Framework impl;
	private final boolean useSeparateCL;
	private final Map<String, Object> configuration;

	public Equinox(Map<String, ?> configuration) {
		SecurityManager sm = System.getSecurityManager();
		if (sm != null)
			sm.checkPermission(new AllPermission());
		useSeparateCL = FrameworkProperties.inUse();
		@SuppressWarnings("unchecked")
		final Map<String, Object> empty = Collections.EMPTY_MAP;
		this.configuration = configuration == null ? empty : new HashMap<String, Object>(configuration);
	}

	private Framework createImpl() {
		if (System.getSecurityManager() == null)
			return createImpl0();
		return AccessController.doPrivileged(new PrivilegedAction<Framework>() {
			public Framework run() {
				return createImpl0();
			}
		});
	}

	Framework createImpl0() {
		try {
			Class<?> implClazz = getImplClass();
			Constructor<?> constructor = implClazz.getConstructor(new Class[] {Map.class});
			return (Framework) constructor.newInstance(new Object[] {configuration});
		} catch (ClassNotFoundException e) {
			throw new NoClassDefFoundError(implName);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (NoSuchMethodException e) {
			throw new NoSuchMethodError(e.getMessage());
		} catch (InstantiationException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	private Class<?> getImplClass() throws ClassNotFoundException {
		ClassLoader thisCL = this.getClass().getClassLoader();
		if (!(useSeparateCL && (thisCL instanceof URLClassLoader)))
			return Class.forName(implName);
		URL[] cp = getFrameworkURLs((URLClassLoader) thisCL);
		EquinoxFWClassLoader fwCL = new EquinoxFWClassLoader(cp, thisCL);
		return fwCL.loadClass(implName);
	}

	private URL[] getFrameworkURLs(URLClassLoader frameworkLoader) {
		// use the classpath of the framework class loader
		URL[] cp = frameworkLoader.getURLs();
		List<URL> result = new ArrayList<URL>(cp.length);
		for (int i = 0; i < cp.length; i++) {
			// need to add only the urls for the framework and any framework fragments
			InputStream manifest = null;
			try {
				if (cp[i].getFile().endsWith("/")) { //$NON-NLS-1$
					manifest = new URL(cp[i], org.eclipse.osgi.framework.internal.core.Constants.OSGI_BUNDLE_MANIFEST).openStream();
				} else {
					manifest = new URL("jar:" + cp[i].toExternalForm() + "!/" + org.eclipse.osgi.framework.internal.core.Constants.OSGI_BUNDLE_MANIFEST).openStream(); //$NON-NLS-1$ //$NON-NLS-2$
				}
				Map<String, String> headers = ManifestElement.parseBundleManifest(manifest, new Headers<String, String>(10));
				String bsnSpec = getValue(headers, Constants.BUNDLE_SYMBOLICNAME);
				if (bsnSpec == null)
					continue;
				String internalBSN = org.eclipse.osgi.framework.internal.core.Constants.getInternalSymbolicName();
				if (internalBSN.equals(bsnSpec)) {
					// this is the framework
					addDevClassPaths(cp[i], bsnSpec, result);
					result.add(cp[i]);
				} else {
					if (!isFrameworkFragment(headers, internalBSN))
						continue;
					// this is for a framework extension
					addDevClassPaths(cp[i], bsnSpec, result);
					result.add(cp[i]);
				}
			} catch (IOException e) {
				continue; // no manifest;
			} catch (BundleException e) {
				continue; // bad manifest;
			} finally {
				if (manifest != null)
					try {
						manifest.close();
					} catch (IOException e) {
						// ignore
					}
			}
		}
		return result.toArray(new URL[result.size()]);
	}

	private void addDevClassPaths(URL cp, String bsn, List<URL> result) {
		if (!cp.getPath().endsWith("/")) //$NON-NLS-1$
			return;
		String[] devPaths = DevClassPathHelper.getDevClassPath(bsn);
		if (devPaths == null)
			return;
		for (int i = 0; i < devPaths.length; i++)
			try {
				char lastChar = devPaths[i].charAt(devPaths[i].length() - 1);
				URL url;
				if ((devPaths[i].endsWith(".jar") || (lastChar == '/' || lastChar == '\\'))) //$NON-NLS-1$
					url = new URL(cp, devPaths[i]);
				else
					url = new URL(cp, devPaths[i] + "/"); //$NON-NLS-1$
				result.add(url);
			} catch (MalformedURLException e) {
				continue;
			}
	}

	private boolean isFrameworkFragment(Map<String, String> headers, String internalBSN) {
		String hostBSN = getValue(headers, Constants.FRAGMENT_HOST);
		return internalBSN.equals(hostBSN) || Constants.SYSTEM_BUNDLE_SYMBOLICNAME.equals(hostBSN);
	}

	private String getValue(Map<String, String> headers, String key) {
		String headerSpec = headers.get(key);
		if (headerSpec == null)
			return null;
		ManifestElement[] elements;
		try {
			elements = ManifestElement.parseHeader(key, headerSpec);
		} catch (BundleException e) {
			return null;
		}
		if (elements == null)
			return null;
		return elements[0].getValue();
	}

	private synchronized Framework getImpl() {
		if (impl == null)
			impl = createImpl();
		return impl;
	}

	public void init() throws BundleException {
		getImpl().init();
	}

	public FrameworkEvent waitForStop(long timeout) throws InterruptedException {
		return getImpl().waitForStop(timeout);
	}

	public Enumeration<URL> findEntries(String path, String filePattern, boolean recurse) {
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

	public Enumeration<String> getEntryPaths(String path) {
		return getImpl().getEntryPaths(path);
	}

	public Dictionary<String, String> getHeaders() {
		return getImpl().getHeaders();
	}

	public Dictionary<String, String> getHeaders(String locale) {
		return getImpl().getHeaders(locale);
	}

	public long getLastModified() {
		return getImpl().getLastModified();
	}

	public String getLocation() {
		return getImpl().getLocation();
	}

	public ServiceReference<?>[] getRegisteredServices() {
		return getImpl().getRegisteredServices();
	}

	public URL getResource(String name) {
		return getImpl().getResource(name);
	}

	public Enumeration<URL> getResources(String name) throws IOException {
		return getImpl().getResources(name);
	}

	public ServiceReference<?>[] getServicesInUse() {
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

	public Class<?> loadClass(String name) throws ClassNotFoundException {
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

	public Map<X509Certificate, List<X509Certificate>> getSignerCertificates(int signersType) {
		return getImpl().getSignerCertificates(signersType);
	}

	public Version getVersion() {
		return getImpl().getVersion();
	}

	public <A> A adapt(Class<A> adapterType) {
		return getImpl().adapt(adapterType);
	}

	public int compareTo(Bundle o) {
		return getImpl().compareTo(o);
	}

	public File getDataFile(String filename) {
		return getImpl().getDataFile(filename);
	}

}
