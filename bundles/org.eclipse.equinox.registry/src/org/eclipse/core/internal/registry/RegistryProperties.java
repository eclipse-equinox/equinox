/*******************************************************************************
 * Copyright (c) 2006 Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.eclipse.core.internal.registry;

import java.util.Properties;
import org.eclipse.core.internal.runtime.RuntimeLog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * Simple Property mechanism to chain property lookup from local registry properties,
 * to BundleContext properties (if available) or System properties otherwise.
 */
public class RegistryProperties {
	
	public static final String empty = ""; //$NON-NLS-1$

	private static Properties registryProperties = new Properties();
	private static Object context = null; // BundleContext, but specified as Object to avoid class loading 

	public static void setContext(Object object) {
		context = object;
	}

	public static String getProperty(String propertyName) {
		String propertyValue = registryProperties.getProperty(propertyName);
		if (propertyValue != null)
			return propertyValue;

		return getContextProperty(propertyName);
	}

	public static String getProperty(String property, String defaultValue) {
		String result = RegistryProperties.getProperty(property);
		return result == null ? defaultValue : result;
	}

	public static void setProperty(String propertyName, String propertyValue) {
		registryProperties.setProperty(propertyName, propertyValue);
	}

	// The registry could be used as a stand-alone utility without OSGi.
	// Try to obtain the property from the OSGi context, but only use bundleContext if
	// it was already set by Activator indicating that OSGi layer is present. 
	private static String getContextProperty(final String propertyName) {
		if (context == null)
			return System.getProperty(propertyName);

		final String[] result = new String[1];
		try {
			// Wrap BundleContext into an inner class to make sure it will only get loaded 
			// if OSGi layer is present.
			Runnable innerClass = new Runnable() {
				public void run() {
					org.osgi.framework.BundleContext bundleContext = (org.osgi.framework.BundleContext) context;
					result[0] = bundleContext.getProperty(propertyName);
				}
			};
			innerClass.run();
		} catch (Exception e) {
			// If we are here, it is likely means that context was set, but OSGi layer
			// is not present or non-standard. This should not happen, but let's give
			// the program a chance to continue - properties should have reasonable 
			// default values.
			IStatus status = new Status(Status.ERROR, IRegistryConstants.RUNTIME_NAME, 0, e.getMessage(), e);
			RuntimeLog.log(status);
			return null;
		}
		return result[0];
	}
}
