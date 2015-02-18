/*******************************************************************************
 * Copyright (c) Dec 2, 2014 Liferay, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Liferay, Inc. - initial API and implementation and/or initial 
 *                    documentation
 ******************************************************************************/

package org.eclipse.equinox.http.servlet.internal.util;

import java.util.*;
import org.osgi.framework.ServiceReference;

public class ServiceProperties {
	static public boolean parseBoolean(
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

	static public Map<String, String> parseInitParams(
		ServiceReference<?> serviceReference, String prefix) {

		Map<String, String> initParams = new HashMap<String, String>();

		for (String key : serviceReference.getPropertyKeys()) {
			if (key.startsWith(prefix)) {
				initParams.put(
					key.substring(prefix.length()),
					String.valueOf(serviceReference.getProperty(key)));
			}
		}

		return Collections.unmodifiableMap(initParams);
	}

	static public String parseName(Object property, Object object) {
		if (property == null) {
			return object.getClass().getName();
		}

		return String.valueOf(property);
	}

}
