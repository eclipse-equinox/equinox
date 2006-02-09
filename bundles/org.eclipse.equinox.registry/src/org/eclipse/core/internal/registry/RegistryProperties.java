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

import org.eclipse.core.internal.registry.osgi.Activator;
import org.osgi.framework.BundleContext;

/**
 * Simple Property mechanism to chain property lookup from local registry properties,
 * to BundleContext properties (if available) or System properties otherwise.
 */
public class RegistryProperties {

	private static Properties registryProperties = new Properties();

	public static String getProperty(String propertyName) {
		String propertyValue = registryProperties.getProperty(propertyName);
		if (propertyValue != null)
			return propertyValue;

		BundleContext bc = Activator.getContext();
		return (bc == null) ? System.getProperty(propertyName) : bc.getProperty(propertyName);
	}

	public static void setProperty(String propertyName, String propertyValue) {
		registryProperties.setProperty(propertyName, propertyValue);
	}
}
