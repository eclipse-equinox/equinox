/*******************************************************************************
 * Copyright (c) 2008, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.launch;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import org.eclipse.osgi.internal.framework.EquinoxContainer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.framework.connect.ModuleConnector;
import org.osgi.framework.launch.Framework;

/**
 * The System Bundle implementation for the Equinox Framework.
 *
 * @since 3.5
 */
public class Equinox implements Framework {

	private final Framework systemBundle;

	public Equinox(Map<String, ?> configuration) {
		this(configuration, null);
	}

	/**
	 * @since 3.16
	 */
	public Equinox(Map<String, ?> configuration, ModuleConnector moduleConnector) {
		EquinoxContainer container = new EquinoxContainer(configuration, moduleConnector);
		systemBundle = (Framework) container.getStorage().getModuleContainer().getModule(0).getBundle();
	}

	@Override
	public int getState() {
		return systemBundle.getState();
	}

	@Override
	public Dictionary<String, String> getHeaders() {
		return systemBundle.getHeaders();
	}

	@Override
	public ServiceReference<?>[] getRegisteredServices() {
		return systemBundle.getRegisteredServices();
	}

	@Override
	public ServiceReference<?>[] getServicesInUse() {
		return systemBundle.getServicesInUse();
	}

	@Override
	public boolean hasPermission(Object permission) {
		return systemBundle.hasPermission(permission);
	}

	@Override
	public URL getResource(String name) {
		return systemBundle.getResource(name);
	}

	@Override
	public Dictionary<String, String> getHeaders(String locale) {
		return systemBundle.getHeaders(locale);
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		return systemBundle.loadClass(name);
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		return systemBundle.getResources(name);
	}

	@Override
	public long getLastModified() {
		return systemBundle.getLastModified();
	}

	@Override
	public BundleContext getBundleContext() {
		return systemBundle.getBundleContext();
	}

	@Override
	public Map<X509Certificate, List<X509Certificate>> getSignerCertificates(int signersType) {
		return systemBundle.getSignerCertificates(signersType);
	}

	@Override
	public Version getVersion() {
		return systemBundle.getVersion();
	}

	@Override
	public File getDataFile(String filename) {
		return systemBundle.getDataFile(filename);
	}

	@Override
	public int compareTo(Bundle o) {
		return systemBundle.compareTo(o);
	}

	@Override
	public void start(int options) throws BundleException {
		systemBundle.start(options);
	}

	@Override
	public void start() throws BundleException {
		systemBundle.start();
	}

	@Override
	public void stop(int options) throws BundleException {
		systemBundle.stop(options);
	}

	@Override
	public void stop() throws BundleException {
		systemBundle.stop();
	}

	@Override
	public void update(InputStream input) throws BundleException {
		systemBundle.update(input);
	}

	@Override
	public void update() throws BundleException {
		systemBundle.update();
	}

	@Override
	public void uninstall() throws BundleException {
		systemBundle.uninstall();
	}

	@Override
	public long getBundleId() {
		return systemBundle.getBundleId();
	}

	@Override
	public String getLocation() {
		return systemBundle.getLocation();
	}

	@Override
	public String getSymbolicName() {
		return systemBundle.getSymbolicName();
	}

	@Override
	public Enumeration<String> getEntryPaths(String path) {
		return systemBundle.getEntryPaths(path);
	}

	@Override
	public URL getEntry(String path) {
		return systemBundle.getEntry(path);
	}

	@Override
	public Enumeration<URL> findEntries(String path, String filePattern, boolean recurse) {
		return systemBundle.findEntries(path, filePattern, recurse);
	}

	@Override
	public <A> A adapt(Class<A> type) {
		return systemBundle.adapt(type);
	}

	@Override
	public void init() throws BundleException {
		systemBundle.init();
	}

	/**
	 * @since 3.10
	 */
	@Override
	public void init(FrameworkListener... listeners) throws BundleException {
		systemBundle.init(listeners);
	}

	@Override
	public FrameworkEvent waitForStop(long timeout) throws InterruptedException {
		return systemBundle.waitForStop(timeout);
	}
}
