/*******************************************************************************
 * Copyright (c) 2006, 2011 Cognos Incorporated, IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.eclipse.osgi.framework.internal.core;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.security.CodeSource;
import java.util.*;
import org.eclipse.core.runtime.internal.adaptor.EclipseAdaptorMsg;
import org.eclipse.osgi.util.NLS;

/*
 * This class should be used in ALL places in the framework implementation to get "system" properties.
 * The static methods on this class should be used instead of the System#getProperty, System#setProperty etc methods.
 */
public class FrameworkProperties {

	/**@GuardedBy FrameworkProperties.class*/
	private static Properties properties;

	// A flag of some sort will have to be supported. 
	// Many existing plugins get framework propeties directly from System instead of BundleContext. 
	// Note that the OSGi TCK is one example where this property MUST be set to false because many TCK bundles set and read system properties.
	private static final String USING_SYSTEM_PROPERTIES_KEY = "osgi.framework.useSystemProperties"; //$NON-NLS-1$
	private static final String PROP_FRAMEWORK = "osgi.framework"; //$NON-NLS-1$
	private static final String PROP_INSTALL_AREA = "osgi.install.area"; //$NON-NLS-1$

	public static Properties getProperties() {
		SecurityManager sm = System.getSecurityManager();
		if (sm != null)
			sm.checkPropertiesAccess();
		return internalGetProperties(null);
	}

	public static String getProperty(String key) {
		return getProperty(key, null);
	}

	public static String getProperty(String key, String defaultValue) {
		SecurityManager sm = System.getSecurityManager();
		if (sm != null)
			sm.checkPropertyAccess(key);
		return internalGetProperties(null).getProperty(key, defaultValue);
	}

	public static String setProperty(String key, String value) {
		SecurityManager sm = System.getSecurityManager();
		if (sm != null)
			sm.checkPermission(new PropertyPermission(key, "write")); //$NON-NLS-1$
		return (String) internalGetProperties(null).put(key, value);
	}

	public static String clearProperty(String key) {
		SecurityManager sm = System.getSecurityManager();
		if (sm != null)
			sm.checkPermission(new PropertyPermission(key, "write")); //$NON-NLS-1$
		return (String) internalGetProperties(null).remove(key);
	}

	private static synchronized Properties internalGetProperties(String usingSystemProperties) {
		if (properties == null) {
			Properties systemProperties = System.getProperties();
			if (usingSystemProperties == null)
				usingSystemProperties = systemProperties.getProperty(USING_SYSTEM_PROPERTIES_KEY);
			if (usingSystemProperties == null || usingSystemProperties.equalsIgnoreCase(Boolean.TRUE.toString())) {
				properties = systemProperties;
			} else {
				// use systemProperties for a snapshot
				// also see requirements in Bundlecontext.getProperty(...))
				properties = new Properties();
				// snapshot of System properties for uses of getProperties who expect to see framework properties set as System properties
				// we need to do this for all system properties because the properties object is used to back
				// BundleContext#getProperty method which expects all system properties to be available
				synchronized (systemProperties) {
					// bug 360198 - must synchronize on systemProperties to avoid concurrent modification exception
					properties.putAll(systemProperties);
				}
			}
		}
		return properties;
	}

	public static synchronized void setProperties(Map<String, String> input) {
		if (input == null) {
			// just use internal props;  note that this will reuse a previous set of properties if they were set
			internalGetProperties("false"); //$NON-NLS-1$
			return;
		}
		properties = null;
		Properties toSet = internalGetProperties("false"); //$NON-NLS-1$
		for (Iterator<String> keys = input.keySet().iterator(); keys.hasNext();) {
			String key = keys.next();
			Object value = input.get(key);
			if (value instanceof String) {
				toSet.setProperty(key, (String) value);
				continue;
			}
			value = input.get(key);
			if (value != null)
				toSet.put(key, value);
			else
				toSet.remove(key);
		}
	}

	public static synchronized boolean inUse() {
		return properties != null;
	}

	public static void initializeProperties() {
		// initialize some framework properties that must always be set
		if (getProperty(PROP_FRAMEWORK) == null || getProperty(PROP_INSTALL_AREA) == null) {
			CodeSource cs = FrameworkProperties.class.getProtectionDomain().getCodeSource();
			if (cs == null)
				throw new IllegalArgumentException(NLS.bind(EclipseAdaptorMsg.ECLIPSE_STARTUP_PROPS_NOT_SET, PROP_FRAMEWORK + ", " + PROP_INSTALL_AREA)); //$NON-NLS-1$
			URL url = cs.getLocation();
			// allow props to be preset
			if (getProperty(PROP_FRAMEWORK) == null)
				setProperty(PROP_FRAMEWORK, url.toExternalForm());
			if (getProperty(PROP_INSTALL_AREA) == null) {
				String filePart = url.getFile();
				setProperty(PROP_INSTALL_AREA, filePart.substring(0, filePart.lastIndexOf('/')));
			}
		}
		// always decode these properties
		setProperty(PROP_FRAMEWORK, decode(getProperty(PROP_FRAMEWORK)));
		setProperty(PROP_INSTALL_AREA, decode(getProperty(PROP_INSTALL_AREA)));
	}

	public static String decode(String urlString) {
		//try to use Java 1.4 method if available
		try {
			Class<? extends URLDecoder> clazz = URLDecoder.class;
			Method method = clazz.getDeclaredMethod("decode", new Class[] {String.class, String.class}); //$NON-NLS-1$
			//first encode '+' characters, because URLDecoder incorrectly converts 
			//them to spaces on certain class library implementations.
			if (urlString.indexOf('+') >= 0) {
				int len = urlString.length();
				StringBuffer buf = new StringBuffer(len);
				for (int i = 0; i < len; i++) {
					char c = urlString.charAt(i);
					if (c == '+')
						buf.append("%2B"); //$NON-NLS-1$
					else
						buf.append(c);
				}
				urlString = buf.toString();
			}
			Object result = method.invoke(null, new Object[] {urlString, "UTF-8"}); //$NON-NLS-1$
			if (result != null)
				return (String) result;
		} catch (Exception e) {
			//JDK 1.4 method not found -- fall through and decode by hand
		}
		//decode URL by hand
		boolean replaced = false;
		byte[] encodedBytes = urlString.getBytes();
		int encodedLength = encodedBytes.length;
		byte[] decodedBytes = new byte[encodedLength];
		int decodedLength = 0;
		for (int i = 0; i < encodedLength; i++) {
			byte b = encodedBytes[i];
			if (b == '%') {
				byte enc1 = encodedBytes[++i];
				byte enc2 = encodedBytes[++i];
				b = (byte) ((hexToByte(enc1) << 4) + hexToByte(enc2));
				replaced = true;
			}
			decodedBytes[decodedLength++] = b;
		}
		if (!replaced)
			return urlString;
		try {
			return new String(decodedBytes, 0, decodedLength, "UTF-8"); //$NON-NLS-1$
		} catch (UnsupportedEncodingException e) {
			//use default encoding
			return new String(decodedBytes, 0, decodedLength);
		}
	}

	private static int hexToByte(byte b) {
		switch (b) {
			case '0' :
				return 0;
			case '1' :
				return 1;
			case '2' :
				return 2;
			case '3' :
				return 3;
			case '4' :
				return 4;
			case '5' :
				return 5;
			case '6' :
				return 6;
			case '7' :
				return 7;
			case '8' :
				return 8;
			case '9' :
				return 9;
			case 'A' :
			case 'a' :
				return 10;
			case 'B' :
			case 'b' :
				return 11;
			case 'C' :
			case 'c' :
				return 12;
			case 'D' :
			case 'd' :
				return 13;
			case 'E' :
			case 'e' :
				return 14;
			case 'F' :
			case 'f' :
				return 15;
			default :
				throw new IllegalArgumentException("Switch error decoding URL"); //$NON-NLS-1$
		}
	}
}
