/*******************************************************************************
 * Copyright (c) 2002, 2005 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.wireadmin;

import java.util.*;

public class ReadOnlyDictionary extends Hashtable {

	private Dictionary dictionary;

	public ReadOnlyDictionary(Dictionary dictionary) {
		this.dictionary = dictionary;
	}

	public Object put(Object key, Object value) {
		return (null);
	}

	public void clear() {
		//??? Do I need to throw an exception
	}

	public Enumeration elements() {
		return dictionary.elements();
	}

	public boolean equals(Object object) {
		//??? - What should we really do here
		if (object instanceof ReadOnlyDictionary) {
			return super.equals(object);
		}
		return dictionary.equals(object);
	}

	public Object get(Object key) {
		return dictionary.get(key);
	}

	public int hashCode() {
		//??? is this right
		return dictionary.hashCode();
	}

	public boolean isEmpty() {
		return dictionary.isEmpty();
	}

	public Enumeration keys() {
		return dictionary.keys();
	}

	public Object remove(Object key) {
		//??? - throw exception???
		return (null);
	}

	public int size() {
		return dictionary.size();
	}

	public String toString() {
		return dictionary.toString();
	}
}
