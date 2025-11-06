/*******************************************************************************
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.equinox.region.tests.stubs;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.*;
import org.osgi.framework.*;

/**
 * A simple stub implementation of Bundle for testing purposes. Unlike the Virgo
 * stubs, this implementation explicitly fires OSGi events without using
 * AspectJ.
 */
public class StubBundle implements Bundle {
	private final long bundleId;
	private final String symbolicName;
	private final Version version;
	private final String location;
	private int state = Bundle.INSTALLED;
	private StubBundleContext bundleContext;
	private Dictionary<String, String> headers = new Hashtable<>();

	public StubBundle(long bundleId, String symbolicName, Version version, String location) {
		this.bundleId = bundleId;
		this.symbolicName = symbolicName;
		this.version = version;
		this.location = location;
		this.bundleContext = new StubBundleContext(this);
	}

	public StubBundle(String symbolicName, Version version) {
		this(1L, symbolicName, version, "/");
	}

	@Override
	public long getBundleId() {
		return bundleId;
	}

	@Override
	public String getSymbolicName() {
		return symbolicName;
	}

	@Override
	public Version getVersion() {
		return version;
	}

	@Override
	public String getLocation() {
		return location;
	}

	@Override
	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
		fireStateChangeEvent(state);
	}

	private void fireStateChangeEvent(int newState) {
		if (bundleContext == null) {
			return;
		}
		int eventType = -1;
		switch (newState) {
		case Bundle.INSTALLED:
			eventType = BundleEvent.INSTALLED;
			break;
		case Bundle.RESOLVED:
			eventType = BundleEvent.RESOLVED;
			break;
		case Bundle.STARTING:
			eventType = BundleEvent.STARTING;
			break;
		case Bundle.ACTIVE:
			eventType = BundleEvent.STARTED;
			break;
		case Bundle.STOPPING:
			eventType = BundleEvent.STOPPING;
			break;
		case Bundle.UNINSTALLED:
			eventType = BundleEvent.UNINSTALLED;
			break;
		}
		if (eventType != -1) {
			bundleContext.fireBundleEvent(new BundleEvent(eventType, this));
		}
	}

	@Override
	public void start() throws BundleException {
		start(0);
	}

	@Override
	public void start(int options) throws BundleException {
		setState(Bundle.STARTING);
		setState(Bundle.ACTIVE);
	}

	@Override
	public void stop() throws BundleException {
		stop(0);
	}

	@Override
	public void stop(int options) throws BundleException {
		setState(Bundle.STOPPING);
		setState(Bundle.RESOLVED);
	}

	@Override
	public void update() throws BundleException {
		setState(Bundle.INSTALLED);
		bundleContext.fireBundleEvent(new BundleEvent(BundleEvent.UPDATED, this));
	}

	@Override
	public void update(InputStream input) throws BundleException {
		update();
	}

	@Override
	public void uninstall() throws BundleException {
		setState(Bundle.UNINSTALLED);
	}

	@Override
	public BundleContext getBundleContext() {
		return bundleContext;
	}

	@Override
	public Dictionary<String, String> getHeaders() {
		return headers;
	}

	@Override
	public Dictionary<String, String> getHeaders(String locale) {
		return headers;
	}

	public void setHeaders(Dictionary<String, String> headers) {
		this.headers = headers;
	}

	@Override
	public ServiceReference<?>[] getRegisteredServices() {
		return null;
	}

	@Override
	public ServiceReference<?>[] getServicesInUse() {
		return null;
	}

	@Override
	public boolean hasPermission(Object permission) {
		return true;
	}

	@Override
	public URL getResource(String name) {
		return null;
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		return getClass().getClassLoader().loadClass(name);
	}

	@Override
	public Enumeration<URL> getResources(String name) {
		return null;
	}

	@Override
	public Enumeration<String> getEntryPaths(String path) {
		return null;
	}

	@Override
	public URL getEntry(String path) {
		return null;
	}

	@Override
	public long getLastModified() {
		return 0;
	}

	@Override
	public Enumeration<URL> findEntries(String path, String filePattern, boolean recurse) {
		return null;
	}

	@Override
	public Map<X509Certificate, java.util.List<X509Certificate>> getSignerCertificates(int signersType) {
		return null;
	}

	@Override
	public <A> A adapt(Class<A> type) {
		return null;
	}

	@Override
	public File getDataFile(String filename) {
		return null;
	}

	@Override
	public int compareTo(Bundle o) {
		return Long.compare(bundleId, o.getBundleId());
	}
}
