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

import java.util.Dictionary;
import java.util.Hashtable;
import org.osgi.framework.*;

/**
 * A simple stub implementation of ServiceRegistration for testing purposes.
 * Unlike the Virgo stubs, this implementation explicitly fires OSGi events
 * without using AspectJ.
 */
public class StubServiceRegistration<S> implements ServiceRegistration<S> {
	private final StubBundleContext bundleContext;
	private final StubServiceReference<S> reference;
	private final S service;
	private Dictionary<String, ?> properties;
	private boolean unregistered = false;

	public StubServiceRegistration(StubBundleContext bundleContext, String[] clazzes, S service,
			Dictionary<String, ?> properties) {
		this.bundleContext = bundleContext;
		this.service = service;
		this.properties = properties != null ? properties : new Hashtable<>();
		this.reference = new StubServiceReference<>(bundleContext.getBundle(), this, clazzes, this.properties);
	}

	public StubServiceRegistration(StubBundleContext bundleContext, String... objectClasses) {
		this(bundleContext, objectClasses, null, null);
	}

	@Override
	public ServiceReference<S> getReference() {
		return reference;
	}

	@Override
	public void setProperties(Dictionary<String, ?> properties) {
		if (unregistered) {
			throw new IllegalStateException("Service has been unregistered");
		}
		this.properties = properties != null ? properties : new Hashtable<>();
		reference.setProperties(this.properties);
		bundleContext.fireServiceEvent(new ServiceEvent(ServiceEvent.MODIFIED, reference));
	}

	@Override
	public void unregister() {
		if (unregistered) {
			throw new IllegalStateException("Service has already been unregistered");
		}
		unregistered = true;
		bundleContext.fireServiceEvent(new ServiceEvent(ServiceEvent.UNREGISTERING, reference));
	}

	public S getService() {
		return service;
	}

	public StubBundleContext getBundleContext() {
		return bundleContext;
	}

	public boolean matchesClass(String className) {
		for (String clazz : reference.getClazzes()) {
			if (clazz.equals(className)) {
				return true;
			}
		}
		return false;
	}
}
