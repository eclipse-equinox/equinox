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
 * A simple stub implementation of ServiceReference for testing purposes.
 */
public class StubServiceReference<S> implements ServiceReference<S> {
	private final Bundle bundle;
	private final String[] clazzes;
	private Dictionary<String, Object> properties;
	private static long nextServiceId = 1L;
	private final long serviceId;

	public StubServiceReference(Bundle bundle, StubServiceRegistration<S> registration, String[] clazzes,
			Dictionary<String, ?> properties) {
		this.bundle = bundle;
		this.clazzes = clazzes;
		this.properties = copyDictionary(properties);
		this.serviceId = nextServiceId++;
	}

	public StubServiceReference(StubServiceRegistration<S> registration) {
		this(registration.getBundleContext().getBundle(), registration, new String[] { "unknown.Service" }, null);
	}

	private static Dictionary<String, Object> copyDictionary(Dictionary<String, ?> source) {
		Dictionary<String, Object> result = new Hashtable<>();
		if (source != null) {
			java.util.Enumeration<String> keys = source.keys();
			while (keys.hasMoreElements()) {
				String key = keys.nextElement();
				result.put(key, source.get(key));
			}
		}
		return result;
	}

	@Override
	public Object getProperty(String key) {
		if (Constants.SERVICE_ID.equals(key)) {
			return serviceId;
		}
		if (Constants.OBJECTCLASS.equals(key)) {
			return clazzes;
		}
		return properties.get(key);
	}

	@Override
	public String[] getPropertyKeys() {
		int extraKeys = 2; // SERVICE_ID and OBJECTCLASS
		String[] keys = new String[properties.size() + extraKeys];
		int i = 0;
		keys[i++] = Constants.SERVICE_ID;
		keys[i++] = Constants.OBJECTCLASS;
		java.util.Enumeration<String> propKeys = properties.keys();
		while (propKeys.hasMoreElements()) {
			keys[i++] = propKeys.nextElement();
		}
		return keys;
	}

	@Override
	public Bundle getBundle() {
		return bundle;
	}

	@Override
	public Bundle[] getUsingBundles() {
		return null;
	}

	@Override
	public boolean isAssignableTo(Bundle bundle, String className) {
		return true;
	}

	@Override
	public int compareTo(Object reference) {
		if (!(reference instanceof ServiceReference)) {
			return 0;
		}
		ServiceReference<?> other = (ServiceReference<?>) reference;
		Long thisId = (Long) getProperty(Constants.SERVICE_ID);
		Long otherId = (Long) other.getProperty(Constants.SERVICE_ID);
		return thisId.compareTo(otherId);
	}

	public String[] getClazzes() {
		return clazzes;
	}

	public void setProperties(Dictionary<String, ?> properties) {
		this.properties = copyDictionary(properties);
	}

	@Override
	public Dictionary<String, Object> getProperties() {
		Hashtable<String, Object> result = new Hashtable<>();
		result.put(Constants.SERVICE_ID, serviceId);
		result.put(Constants.OBJECTCLASS, clazzes);
		java.util.Enumeration<String> keys = properties.keys();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			result.put(key, properties.get(key));
		}
		return result;
	}

	@Override
	public <A> A adapt(Class<A> type) {
		return null;
	}
}
