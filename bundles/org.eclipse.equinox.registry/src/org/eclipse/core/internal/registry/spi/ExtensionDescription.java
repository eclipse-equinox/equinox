/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.registry.spi;

/**
 * Describes an extension to be added to the extension registry.
 * 
 * It is expected that extension name is not null. Properties and children of 
 * the extension description might be null; value might be null as well.
 * 
 * This class is not intended to be subclassed.
 * 
 * @since org.eclipse.equinox.registry 1.0
 */
public final class ExtensionDescription {

	/**
	 * Name of the extension.
	 */
	private String elementName;

	/**
	 * Properties of the extension element. 
	 * @see ExtensionProperty
	 */
	private ExtensionProperty[] properties;

	/**
	 * String value to be stored in this configuration element.
	 */
	private String value;

	/**
	 * Children of the extension element.
	 */
	private ExtensionDescription[] children;

	/**
	 * Constructor.
	 * @param name - name of the extension
	 * @param properties - properties of the extension
	 * @param value - string value to be stored
	 * @param children - children of the extension element
	 */
	public ExtensionDescription(String name, ExtensionProperty[] properties, String value, ExtensionDescription[] children) {
		this.elementName = name;
		this.properties = properties;
		this.value = value;
		this.children = children;
	}

	/**
	 * Constructor.
	 * @param name - name of the extension
	 * @param property - property of the extension
	 * @param value - string value to be stored
	 * @param children - children of the extension element
	 */
	public ExtensionDescription(String name, ExtensionProperty property, String value, ExtensionDescription[] children) {
		this.elementName = name;
		this.properties = new ExtensionProperty[] {property};
		this.value = value;
		this.children = children;
	}

	/**
	 * @return - children of the extension 
	 */
	public ExtensionDescription[] getChildren() {
		return children;
	}

	/**
	 * @return - extension name
	 */
	public String getElementName() {
		return elementName;
	}

	/**
	 * @return - properties of the extension description
	 */
	public ExtensionProperty[] getProperties() {
		return properties;
	}

	/**
	 * @return true - extension description has some properties associated with it; false - no properties 
	 * are associated with the extension description
	 */
	public boolean hasProperties() {
		return (properties != null) ? properties.length != 0 : false;
	}

	/**
	 * @return - String value to be stored in the element
	 */
	public String getValue() {
		return value;
	}

}
