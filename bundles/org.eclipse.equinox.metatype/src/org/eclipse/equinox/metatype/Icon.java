/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.metatype;

import org.osgi.framework.Bundle;

/**
 * Represents an Icon with a name and a size
 */
class Icon implements Cloneable {

	private String _fileName;
	private int _size;
	private Bundle _bundle;

	/**
	 * Constructor of class Icon.
	 */
	public Icon(String fileName, int size, Bundle bundle) {

		this._fileName = fileName;
		this._size = size;
		this._bundle = bundle;
	}

	/**
	 * Constructor of class Icon.
	 */
	public Icon(String fileName, Bundle bundle) {

		// Integer.MIN_VALUE signifies size was not specified
		this(fileName, Integer.MIN_VALUE, bundle);
	}

	/*
	 * 
	 */
	public synchronized Object clone() {
		return new Icon(this._fileName, this._size, this._bundle);
	}

	/**
	 * Method to get the icon's file name.
	 */
	String getIconName() {
		return _fileName;
	}

	/**
	 * returns the size specified when the icon was created
	 * 
	 * @return size or Integer.MIN_VALUE if no size was specified
	 */
	int getIconSize() {
		return _size;
	}

	/**
	 * Method to get the bundle having this Icon.
	 */
	Bundle getIconBundle() {
		return _bundle;
	}
}
