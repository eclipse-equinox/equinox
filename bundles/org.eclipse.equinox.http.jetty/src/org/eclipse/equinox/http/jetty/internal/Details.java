/*******************************************************************************
 * Copyright (c) 2016 Raymond Augé and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Raymond Augé - initial implementation
 *******************************************************************************/
package org.eclipse.equinox.http.jetty.internal;

import java.util.Dictionary;
import org.eclipse.equinox.http.jetty.JettyConstants;
import org.osgi.framework.BundleContext;

public class Details {

	public static boolean getBoolean(BundleContext dictionary, String key, boolean dflt) {
		String value = dictionary.getProperty(key);
		if (value == null) {
			return dflt;
		}
		return Boolean.parseBoolean(value);
	}

	public static boolean getBoolean(@SuppressWarnings("rawtypes") Dictionary dictionary, String key, boolean dflt) {
		Object value = dictionary.get(key);
		if (value instanceof Boolean) {
			return (Boolean) value;
		} else if (value instanceof String) {
			return Boolean.parseBoolean((String) value);
		}
		return dflt;
	}

	public static boolean getBooleanProp(BundleContext dictionary, String key, boolean dflt) {
		return getBoolean(dictionary, JettyConstants.PROPERTY_PREFIX + key, dflt);
	}

	public static boolean getBooleanProp(@SuppressWarnings("rawtypes") Dictionary dictionary, String key,
			boolean dflt) {
		return getBoolean(dictionary, JettyConstants.PROPERTY_PREFIX + key, dflt);
	}

	public static int getInt(BundleContext dictionary, String key, int dflt) {
		String value = dictionary.getProperty(key);
		if (value == null) {
			return dflt;
		}
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return dflt;
		}
	}

	public static int getInt(@SuppressWarnings("rawtypes") Dictionary dictionary, String key, int dflt) {
		Object value = dictionary.get(key);
		if (value instanceof Integer) {
			return (Integer) value;
		} else if (value instanceof String) {
			try {
				return Integer.parseInt((String) value);
			} catch (NumberFormatException e) {
				return dflt;
			}
		}
		return dflt;
	}

	public static int getIntProp(BundleContext dictionary, String key, int dflt) {
		return getInt(dictionary, JettyConstants.PROPERTY_PREFIX + key, dflt);
	}

	public static int getIntProp(@SuppressWarnings("rawtypes") Dictionary dictionary, String key, int dflt) {
		return getInt(dictionary, JettyConstants.PROPERTY_PREFIX + key, dflt);
	}

	public static long getLong(BundleContext dictionary, String key, long dflt) {
		String value = dictionary.getProperty(key);
		if (value == null) {
			return dflt;
		}
		try {
			return Long.parseLong(value);
		} catch (NumberFormatException e) {
			return dflt;
		}
	}

	public static long getLong(@SuppressWarnings("rawtypes") Dictionary dictionary, String key, long dflt) {
		Object value = dictionary.get(key);
		if (value instanceof Long) {
			return (Long) value;
		} else if (value instanceof String) {
			try {
				return Long.parseLong((String) value);
			} catch (NumberFormatException e) {
				return dflt;
			}
		}
		return dflt;
	}

	public static long getLongProp(BundleContext dictionary, String key, long dflt) {
		return getLong(dictionary, JettyConstants.PROPERTY_PREFIX + key, dflt);
	}

	public static long getLongProp(@SuppressWarnings("rawtypes") Dictionary dictionary, String key, long dflt) {
		return getLong(dictionary, JettyConstants.PROPERTY_PREFIX + key, dflt);
	}

	public static String getString(BundleContext dictionary, String key, String dflt) {
		String value = dictionary.getProperty(key);
		if (value == null) {
			return dflt;
		}
		return value;
	}

	public static String getString(@SuppressWarnings("rawtypes") Dictionary dictionary, String key, String dflt) {
		Object value = dictionary.get(key);
		if (value == null) {
			return dflt;
		} else if (value instanceof String) {
			return (String) value;
		}
		return String.valueOf(value);
	}

	public static String getStringProp(BundleContext dictionary, String key, String dflt) {
		return getString(dictionary, JettyConstants.PROPERTY_PREFIX + key, dflt);
	}

	public static String getStringProp(@SuppressWarnings("rawtypes") Dictionary dictionary, String key, String dflt) {
		return getString(dictionary, JettyConstants.PROPERTY_PREFIX + key, dflt);
	}

}
