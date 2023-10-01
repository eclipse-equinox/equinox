/*******************************************************************************
 * Copyright (c) 2014 Raymond Augé and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Raymond Augé <raymond.auge@liferay.com> - Bug 436698
 ******************************************************************************/

package org.eclipse.equinox.http.servlet.internal.util;

import java.util.*;
import org.osgi.framework.ServiceReference;

/**
 * @author Raymond Augé
 */
public class ServiceReferenceMap extends AbstractMap<String, Object> {

	public ServiceReferenceMap(ServiceReference<?> serviceReference) {
		String[] propertyKeys = serviceReference.getPropertyKeys();

		entries = new HashSet<>(propertyKeys.length);

		for (String key : propertyKeys) {
			Map.Entry<String, Object> entry = new ReferenceEntry(key, serviceReference.getProperty(key));

			entries.add(entry);
		}
	}

	@Override
	public Set<java.util.Map.Entry<String, Object>> entrySet() {
		return entries;
	}

	private Set<java.util.Map.Entry<String, Object>> entries;

	private class ReferenceEntry implements Map.Entry<String, Object> {

		ReferenceEntry(String key, Object value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public Object setValue(Object value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object getValue() {
			return value;
		}

		@Override
		public String getKey() {
			return key;
		}

		private String key;
		private Object value;

	}

}
