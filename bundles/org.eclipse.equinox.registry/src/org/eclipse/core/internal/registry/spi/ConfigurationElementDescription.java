/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.registry.spi;

import org.eclipse.core.runtime.IConfigurationElement;

/**
 * Describes configuration elements (content of an extension) to be added to 
 * the extension registry. Configuration element can contain attributes or 
 * a String value. Configuration element can contain other configuration 
 * elements (children).
 * <p>
 * It is expected that configuration element's name is not null. Attributes and 
 * children of the extension description might be null; value might be null as well.
 * </p>
 * <p>
 * This class can be instantiated.
 * </p>
 * <p>
 * This class is not intended to be subclassed.
 * </p>
 * @see ConfigurationElementAttribute
 */
public final class ConfigurationElementDescription {

	/**
	 * Name of the configuration element.
	 * @see IConfigurationElement#getName()
	 */
	private String name;

	/**
	 * Attributes of the configuration element.
	 * @see IConfigurationElement#getAttribute(String)
	 */
	private ConfigurationElementAttribute[] attributes;

	/**
	 * String value to be stored in this configuration element.
	 * @see IConfigurationElement#getValue()
	 */
	private String value;

	/**
	 * Children of the configuration element.
	 * @see IConfigurationElement#getChildren() 
	 */
	private ConfigurationElementDescription[] children;

	/**
	 * Constructor.
	 * 
	 * @param name - name of the configuration element
	 * @param attributes - attributes of the configuration element. Might be null.
	 * @param value - string value to be stored. Might be null.
	 * @param children - children of the configuration element. Might be null.
	 * @see IConfigurationElement#getName()
	 * @see IConfigurationElement#getChildren() 
	 * @see IConfigurationElement#getAttribute(String)
	 * @see IConfigurationElement#getValue()
	 */
	public ConfigurationElementDescription(String name, ConfigurationElementAttribute[] attributes, String value, ConfigurationElementDescription[] children) {
		this.name = name;
		this.attributes = attributes;
		this.value = value;
		this.children = children;
	}

	/**
	 * Constructor.
	 * 
	 * @param name - name of the configuration element
	 * @param attribute - attribute of the configuration element. Might be null.
	 * @param value - string value to be stored. Might be null.
	 * @param children - children of the configuration element. Might be null.
	 * @see IConfigurationElement#getName()
	 * @see IConfigurationElement#getChildren() 
	 * @see IConfigurationElement#getAttribute(String)
	 * @see IConfigurationElement#getValue()
	 */
	public ConfigurationElementDescription(String name, ConfigurationElementAttribute attribute, String value, ConfigurationElementDescription[] children) {
		this.name = name;
		this.attributes = new ConfigurationElementAttribute[] {attribute};
		this.value = value;
		this.children = children;
	}

	/**
	 * Returns children of the configuration element.
	 * @return - children of the extension
	 * @see IConfigurationElement#getChildren() 
	 */
	public ConfigurationElementDescription[] getChildren() {
		return children;
	}

	/**
	 * Returns name of the configuration element.
	 * @return - extension name
	 * @see IConfigurationElement#getName()
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns attributes of the configuration element.
	 * @return - attributes of the extension description
	 * @see IConfigurationElement#getAttribute(String)
	 */
	public ConfigurationElementAttribute[] getAttributes() {
		return attributes;
	}

	/**
	 * Returns string value stored in the configuration element.
	 * @return - String value to be stored in the element
	 * @see IConfigurationElement#getValue()
	 */
	public String getValue() {
		return value;
	}
}
