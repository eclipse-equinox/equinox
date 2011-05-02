/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
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
 * Describes properties of configuration elements to be added to the registry. 
 * Properties are represented as pairs of strings: {attribute name; attribute value}.
 * <p>
 * It is expected that both attribute name and attribute value are not null.
 * </p>
 * <p>
 * This class can be instantiated.
 * </p>
 * <p>
 * This class is not intended to be subclassed.
 * </p>
 * @see ConfigurationElementDescription
 * @see IConfigurationElement
 */
public final class ConfigurationElementAttribute {

	/**
	 * Attribute name.
	 * @see IConfigurationElement#getAttributeNames()
	 */
	private String name;

	/**
	 * Attribute value.
	 * @see IConfigurationElement#getAttributeAsIs(String)
	 */
	private String value;

	/**
	 * Constructor.
	 * 
	 * @param name attribute name
	 * @param value attribute value
	 */
	public ConfigurationElementAttribute(String name, String value) {
		this.name = name;
		this.value = value;
	}

	/**
	 * Returns attribute name.
	 * @return attribute name
	 * @see IConfigurationElement#getAttributeNames()
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns value of the attribute.
	 * @return attribute value
	 * @see IConfigurationElement#getAttributeAsIs(String)
	 */
	public String getValue() {
		return value;
	}
}
