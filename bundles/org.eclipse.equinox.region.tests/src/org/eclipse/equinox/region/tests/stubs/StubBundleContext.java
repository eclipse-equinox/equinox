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
import java.util.*;
import org.osgi.framework.*;

/**
 * A simple stub implementation of BundleContext for testing purposes. Unlike
 * the Virgo stubs, this implementation explicitly fires OSGi events without
 * using AspectJ.
 */
public class StubBundleContext implements BundleContext {
	private final StubBundle bundle;
	private final List<BundleListener> bundleListeners = new ArrayList<>();
	private final List<ServiceListener> serviceListeners = new ArrayList<>();
	private final List<FrameworkListener> frameworkListeners = new ArrayList<>();
	private final Map<Long, StubBundle> installedBundles = new HashMap<>();
	private final List<StubServiceRegistration<?>> serviceRegistrations = new ArrayList<>();
	private long nextBundleId = 2L;

	public StubBundleContext() {
		this(new StubBundle(1L, "test.bundle", org.osgi.framework.Version.emptyVersion, "location"));
	}

	public StubBundleContext(StubBundle bundle) {
		this.bundle = bundle;
		if (bundle != null && bundle.getBundleId() == 0L) {
			// System bundle
			installedBundles.put(0L, bundle);
		}
	}

	@Override
	public String getProperty(String key) {
		return System.getProperty(key);
	}

	@Override
	public Bundle getBundle() {
		return bundle;
	}

	@Override
	public Bundle installBundle(String location) throws BundleException {
		return installBundle(location, null);
	}

	@Override
	public Bundle installBundle(String location, InputStream input) throws BundleException {
		StubBundle newBundle = new StubBundle(nextBundleId++, location, org.osgi.framework.Version.emptyVersion,
				location);
		installedBundles.put(newBundle.getBundleId(), newBundle);
		newBundle.setState(Bundle.INSTALLED);
		return newBundle;
	}

	public void addInstalledBundle(StubBundle bundle) {
		installedBundles.put(bundle.getBundleId(), bundle);
	}

	@Override
	public Bundle getBundle(long id) {
		return installedBundles.get(id);
	}

	@Override
	public Bundle[] getBundles() {
		return installedBundles.values().toArray(new Bundle[0]);
	}

	@Override
	public void addBundleListener(BundleListener listener) {
		synchronized (bundleListeners) {
			if (!bundleListeners.contains(listener)) {
				bundleListeners.add(listener);
			}
		}
	}

	@Override
	public void removeBundleListener(BundleListener listener) {
		synchronized (bundleListeners) {
			bundleListeners.remove(listener);
		}
	}

	public void fireBundleEvent(BundleEvent event) {
		List<BundleListener> listeners;
		synchronized (bundleListeners) {
			listeners = new ArrayList<>(bundleListeners);
		}
		for (BundleListener listener : listeners) {
			try {
				listener.bundleChanged(event);
			} catch (Exception e) {
				// Swallow to allow all listeners to be called
			}
		}
	}

	public List<BundleListener> getBundleListeners() {
		synchronized (bundleListeners) {
			return new ArrayList<>(bundleListeners);
		}
	}

	@Override
	public void addServiceListener(ServiceListener listener) {
		synchronized (serviceListeners) {
			if (!serviceListeners.contains(listener)) {
				serviceListeners.add(listener);
			}
		}
	}

	@Override
	public void addServiceListener(ServiceListener listener, String filter) throws InvalidSyntaxException {
		addServiceListener(listener);
	}

	@Override
	public void removeServiceListener(ServiceListener listener) {
		synchronized (serviceListeners) {
			serviceListeners.remove(listener);
		}
	}

	public void fireServiceEvent(ServiceEvent event) {
		List<ServiceListener> listeners;
		synchronized (serviceListeners) {
			listeners = new ArrayList<>(serviceListeners);
		}
		for (ServiceListener listener : listeners) {
			try {
				listener.serviceChanged(event);
			} catch (Exception e) {
				// Swallow to allow all listeners to be called
			}
		}
	}

	public List<ServiceListener> getServiceListeners() {
		synchronized (serviceListeners) {
			return new ArrayList<>(serviceListeners);
		}
	}

	@Override
	public void addFrameworkListener(FrameworkListener listener) {
		synchronized (frameworkListeners) {
			if (!frameworkListeners.contains(listener)) {
				frameworkListeners.add(listener);
			}
		}
	}

	@Override
	public void removeFrameworkListener(FrameworkListener listener) {
		synchronized (frameworkListeners) {
			frameworkListeners.remove(listener);
		}
	}

	@Override
	public ServiceRegistration<?> registerService(String clazz, Object service, Dictionary<String, ?> properties) {
		return registerService(new String[] { clazz }, service, properties);
	}

	@Override
	public ServiceRegistration<?> registerService(String[] clazzes, Object service, Dictionary<String, ?> properties) {
		StubServiceRegistration<?> registration = new StubServiceRegistration<>(this, clazzes, service, properties);
		serviceRegistrations.add(registration);
		fireServiceEvent(new ServiceEvent(ServiceEvent.REGISTERED, registration.getReference()));
		return registration;
	}

	@Override
	public <S> ServiceRegistration<S> registerService(Class<S> clazz, S service, Dictionary<String, ?> properties) {
		@SuppressWarnings("unchecked")
		StubServiceRegistration<S> registration = (StubServiceRegistration<S>) registerService(clazz.getName(), service,
				properties);
		return registration;
	}

	@Override
	public <S> ServiceRegistration<S> registerService(Class<S> clazz, org.osgi.framework.ServiceFactory<S> factory,
			Dictionary<String, ?> properties) {
		// For testing purposes, we don't distinguish between ServiceFactory and regular
		// service
		@SuppressWarnings("unchecked")
		StubServiceRegistration<S> registration = (StubServiceRegistration<S>) registerService(clazz.getName(), null,
				properties);
		return registration;
	}

	@Override
	public ServiceReference<?> getServiceReference(String clazz) {
		ServiceReference<?>[] refs = getAllServiceReferences(clazz, null);
		return (refs != null && refs.length > 0) ? refs[0] : null;
	}

	@Override
	public <S> ServiceReference<S> getServiceReference(Class<S> clazz) {
		@SuppressWarnings("unchecked")
		ServiceReference<S> ref = (ServiceReference<S>) getServiceReference(clazz.getName());
		return ref;
	}

	@Override
	public ServiceReference<?>[] getServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
		return getAllServiceReferences(clazz, filter);
	}

	@Override
	public ServiceReference<?>[] getAllServiceReferences(String clazz, String filter) {
		List<ServiceReference<?>> refs = new ArrayList<>();
		for (StubServiceRegistration<?> reg : serviceRegistrations) {
			if (clazz == null || reg.matchesClass(clazz)) {
				refs.add(reg.getReference());
			}
		}
		return refs.isEmpty() ? null : refs.toArray(new ServiceReference<?>[0]);
	}

	@Override
	public <S> Collection<ServiceReference<S>> getServiceReferences(Class<S> clazz, String filter)
			throws InvalidSyntaxException {
		List<ServiceReference<S>> refs = new ArrayList<>();
		for (StubServiceRegistration<?> reg : serviceRegistrations) {
			if (clazz == null || reg.matchesClass(clazz.getName())) {
				@SuppressWarnings("unchecked")
				ServiceReference<S> ref = (ServiceReference<S>) reg.getReference();
				refs.add(ref);
			}
		}
		return refs;
	}

	@Override
	public <S> S getService(ServiceReference<S> reference) {
		for (StubServiceRegistration<?> reg : serviceRegistrations) {
			if (reg.getReference().equals(reference)) {
				@SuppressWarnings("unchecked")
				S service = (S) reg.getService();
				return service;
			}
		}
		return null;
	}

	@Override
	public boolean ungetService(ServiceReference<?> reference) {
		return false;
	}

	@Override
	public <S> ServiceObjects<S> getServiceObjects(ServiceReference<S> reference) {
		return null;
	}

	@Override
	public File getDataFile(String filename) {
		return null;
	}

	@Override
	public Filter createFilter(String filter) throws InvalidSyntaxException {
		return new StubFilter(filter);
	}

	@Override
	public Bundle getBundle(String location) {
		for (StubBundle b : installedBundles.values()) {
			if (location.equals(b.getLocation())) {
				return b;
			}
		}
		return null;
	}
}
