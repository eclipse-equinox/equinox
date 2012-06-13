/*******************************************************************************
 * Copyright (c) 2003, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.internal.serviceregistry;

import java.lang.reflect.Array;
import java.util.*;
import org.eclipse.osgi.framework.util.Headers;
import org.osgi.framework.Constants;

/**
 * Hashtable for service properties.
 * 
 * Supports case-insensitive key lookup.
 */
class ServiceProperties extends Headers<String, Object> {
	/**
	 * Create a properties object for the service.
	 *
	 * @param props The properties for this service.
	 */
	private ServiceProperties(int size, Dictionary<String, ?> props) {
		super(size);

		if (props == null) {
			return;
		}
		synchronized (props) {
			Enumeration<?> keysEnum = props.keys();

			while (keysEnum.hasMoreElements()) {
				Object key = keysEnum.nextElement();

				if (key instanceof String) {
					String header = (String) key;

					setProperty(header, props.get(header));
				}
			}
		}
	}

	/**
	 * Create a properties object for the service.
	 *
	 * @param props The properties for this service.
	 */
	ServiceProperties(Dictionary<String, ?> props) {
		this((props == null) ? 2 : props.size() + 2, props);
	}

	/**
	 * Get a clone of the value of a service's property.
	 *
	 * @param key header name.
	 * @return Clone of the value of the property or <code>null</code> if there is
	 * no property by that name.
	 */
	Object getProperty(String key) {
		return cloneValue(get(key));
	}

	/**
	 * Get the list of key names for the service's properties.
	 *
	 * @return The list of property key names.
	 */
	synchronized String[] getPropertyKeys() {
		int size = size();

		String[] keynames = new String[size];

		Enumeration<String> keysEnum = keys();

		for (int i = 0; i < size; i++) {
			keynames[i] = keysEnum.nextElement();
		}

		return keynames;
	}

	/**
	 * Put a clone of the property value into this property object.
	 *
	 * @param key Name of property.
	 * @param value Value of property.
	 * @return previous property value.
	 */
	synchronized Object setProperty(String key, Object value) {
		return set(key, cloneValue(value));
	}

	/**
	 * Attempt to clone the value if necessary and possible.
	 *
	 * For some strange reason, you can test to see of an Object is
	 * Cloneable but you can't call the clone method since it is
	 * protected on Object!
	 *
	 * @param value object to be cloned.
	 * @return cloned object or original object if we didn't clone it.
	 */
	private static Object cloneValue(Object value) {
		if (value == null)
			return null;
		if (value instanceof String) /* shortcut String */
			return value;
		if (value instanceof Number) /* shortcut Number */
			return value;
		if (value instanceof Character) /* shortcut Character */
			return value;
		if (value instanceof Boolean) /* shortcut Boolean */
			return value;

		Class<?> clazz = value.getClass();
		if (clazz.isArray()) {
			// Do an array copy
			Class<?> type = clazz.getComponentType();
			int len = Array.getLength(value);
			Object clonedArray = Array.newInstance(type, len);
			System.arraycopy(value, 0, clonedArray, 0, len);
			return clonedArray;
		}
		// must use reflection because Object clone method is protected!!
		try {
			return clazz.getMethod("clone", (Class<?>[]) null).invoke(value, (Object[]) null); //$NON-NLS-1$
		} catch (Exception e) {
			/* clone is not a public method on value's class */
		} catch (Error e) {
			/* JCL does not support reflection; try some well known types */
			if (value instanceof Vector<?>)
				return ((Vector<?>) value).clone();
			if (value instanceof Hashtable<?, ?>)
				return ((Hashtable<?, ?>) value).clone();
		}
		return value;
	}

	public synchronized String toString() {
		String keys[] = getPropertyKeys();

		int size = keys.length;

		StringBuffer sb = new StringBuffer(20 * size);

		sb.append('{');

		int n = 0;
		for (int i = 0; i < size; i++) {
			String key = keys[i];
			if (!key.equals(Constants.OBJECTCLASS)) {
				if (n > 0)
					sb.append(", "); //$NON-NLS-1$

				sb.append(key);
				sb.append('=');
				Object value = get(key);
				if (value.getClass().isArray()) {
					sb.append('[');
					int length = Array.getLength(value);
					for (int j = 0; j < length; j++) {
						if (j > 0)
							sb.append(',');
						sb.append(Array.get(value, j));
					}
					sb.append(']');
				} else {
					sb.append(value);
				}
				n++;
			}
		}

		sb.append('}');

		return sb.toString();
	}
}