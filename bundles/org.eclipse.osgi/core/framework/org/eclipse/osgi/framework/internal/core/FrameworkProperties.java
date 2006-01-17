/*******************************************************************************
 * Copyright (c) 2006 Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.eclipse.osgi.framework.internal.core;

import java.util.Properties;
import java.util.PropertyPermission;

/*
 * This class should be used in ALL places in the framework implementation to get "system" properties.
 * The static methods on this class should be used instead of the System#getProperty, System#setProperty etc methods.
 */
public class FrameworkProperties {
	
	private static Properties properties;

	// A flag of some sort will have to be supported. 
	// Many existing plugins get framework propeties directly from System instead of BundleContext. 
	// Note that the OSGi TCK is one example where this property MUST be set to false because many TCK bundles set and read system properties.
	private static final String USING_SYSTEM_PROPERTIES_KEY = "osgi.framework.useSystemProperties"; //$NON-NLS-1$

	static {
		Properties systemProperties = System.getProperties();
		String usingSystemProperties = systemProperties.getProperty(USING_SYSTEM_PROPERTIES_KEY);
		if (usingSystemProperties == null || usingSystemProperties.equalsIgnoreCase(Boolean.TRUE.toString())) {
			properties = systemProperties;
		} else {
			// use systemProperties for a snapshot
			// also see requirements in Bundlecontext.getProperty(...))
			properties = new Properties();
			// snapshot of System properties for uses of getProperties who expect to see framework properties set as System properties
			// we need to do this for all system properties because the properties object is used to back
			// BundleContext#getProperty method which expects all system properties to be available
			properties.putAll(systemProperties);
		}
	}
		
	public static Properties getProperties() {
		SecurityManager sm = System.getSecurityManager();
		if (sm != null)
			sm.checkPropertiesAccess();
		return properties;
	}
	
    public static String getProperty(String key) {
    	return getProperty(key, null);
    }

	public static String getProperty(String key, String defaultValue) {
		SecurityManager sm = System.getSecurityManager();
		if (sm != null)
			sm.checkPropertyAccess(key);
    	return properties.getProperty(key, defaultValue);
    }

    public static String setProperty(String key, String value) {
		SecurityManager sm = System.getSecurityManager();
		if (sm != null)
			sm.checkPermission(new PropertyPermission(key, "write")); //$NON-NLS-1$
    	return (String) properties.put(key, value);
    }

    public static String clearProperty(String key) {
		SecurityManager sm = System.getSecurityManager();
		if (sm != null)
			sm.checkPermission(new PropertyPermission(key, "write")); //$NON-NLS-1$
        return (String) properties.remove(key);
    }    
}
