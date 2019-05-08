/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.metatype.impl;

import java.io.IOException;
import java.util.Set;
import org.eclipse.equinox.metatype.impl.Persistence.Reader;
import org.eclipse.equinox.metatype.impl.Persistence.Writer;
import org.osgi.framework.Bundle;

/**
 * Represents an Icon with a name and a size
 */
class Icon implements Cloneable {

	private final String _fileName;
	private final int _size;
	private final Bundle _bundle;

	/**
	 * Constructor of class Icon.
	 */
	public Icon(String fileName, int size, Bundle bundle) {

		this._fileName = fileName;
		this._size = size;
		this._bundle = bundle;
	}

	@Override
	public Object clone() {
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
	Integer getIconSize() {
		return _size;
	}

	/**
	 * Method to get the bundle having this Icon.
	 */
	Bundle getIconBundle() {
		return _bundle;
	}

	void getStrings(Set<String> strings) {
		strings.add(_fileName);
	}

	public static Icon load(Reader reader, Bundle b) throws IOException {
		int size = reader.readInt();
		String fileName = reader.readString();
		return new Icon(fileName, size, b);
	}

	public void write(Writer writer) throws IOException {
		writer.writeInt(_size);
		writer.writeString(_fileName);
	}
}
