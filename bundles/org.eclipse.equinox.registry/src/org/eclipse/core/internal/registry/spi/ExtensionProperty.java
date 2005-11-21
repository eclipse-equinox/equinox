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
 * Describes properties of an extension. Properties are pairs of strings: 
 * {property name; property value}.
 * 
 * It is expected that both property name and property value are not null. 
 *  
 * This class is not intended to be subclassed.
 * 
 * @since org.eclipse.equinox.registry 1.0
 */
public final class ExtensionProperty {

	/**
	 * Property name.
	 */
	private String name;

	/**
	 * Property value
	 */
	private String value;

	/**
	 * Constructor.
	 * @param name - property name
	 * @param value - property value
	 */
	public ExtensionProperty(String name, String value) {
		this.name = name;
		this.value = value;
	}

	/**
	 * @return - property name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return - property value
	 */
	public String getValue() {
		return value;
	}
}
