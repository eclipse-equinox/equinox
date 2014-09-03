/*******************************************************************************
 * Copyright (c) 2014 Raymond Augé and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Raymond Augé <raymond.auge@liferay.com> - Bug 436698
 ******************************************************************************/

package org.eclipse.equinox.http.servlet.internal.customizer;

import org.eclipse.equinox.http.servlet.internal.HttpServiceRuntimeImpl;

import java.util.HashMap;
import java.util.Map;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * @author Raymond Augé
 */
public abstract class RegistrationServiceTrackerCustomizer<S, T>
	implements ServiceTrackerCustomizer<S, T> {

	public RegistrationServiceTrackerCustomizer(
		BundleContext bundleContext, HttpServiceRuntimeImpl httpServiceRuntime) {

		this.bundleContext = bundleContext;
		this.httpServiceRuntime = httpServiceRuntime;
	}

	protected boolean parseBoolean(
		ServiceReference<?> serviceReference, String property) {

		Object value = serviceReference.getProperty(property);

		if (Boolean.class.isInstance(value)) {
			return ((Boolean)value).booleanValue();
		}
		if (String.class.isInstance(value)) {
			return Boolean.valueOf((String)value);
		}

		return false;
	}

	protected Map<String, String> parseInitParams(
		ServiceReference<?> serviceReference, String prefix) {

		Map<String, String> initParams = new HashMap<String, String>();

		for (String key : serviceReference.getPropertyKeys()) {
			if (key.startsWith(prefix)) {
				initParams.put(
					key.substring(prefix.length()),
					String.valueOf(serviceReference.getProperty(key)));
			}
		}

		return initParams;
	}

	protected String parseName(Object property, Object object) {
		if (property == null) {
			return object.getClass().getName();
		}

		return String.valueOf(property);
	}

	protected BundleContext bundleContext;
	protected HttpServiceRuntimeImpl httpServiceRuntime;

}