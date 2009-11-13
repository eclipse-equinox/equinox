/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.registry;

/**
 * Essentially a map String -> String[] for small number of keys. 
 * 
 * For Maps containing a small number of objects hashing often reduces performance. 
 * This implementation uses two parallel arrays, one for keys, one for
 * values, and grows them as necessary.
 */
public class DirectMap {

	final private float growthFactor;

	private String[] keyArray;
	private String[][] valueArray;
	private int size;

	public DirectMap(int initialSize, float growthFactor) {
		if (initialSize < 1)
			throw new IllegalArgumentException();
		if (growthFactor <= 0)
			throw new IllegalArgumentException();
		this.growthFactor = growthFactor;
		keyArray = new String[initialSize];
		valueArray = new String[initialSize][];
		size = 0;
	}

	public synchronized void put(String key, String[] value) {
		if (key == null)
			throw new IllegalArgumentException();
		int id = findKey(key);
		if (id != -1)
			throw new IllegalArgumentException();

		if (size >= keyArray.length) { // need to resize
			int newSize = recalcSize(keyArray.length);
			if (newSize <= size)
				newSize = size + 1;

			String[] newKeyArray = new String[newSize];
			System.arraycopy(keyArray, 0, newKeyArray, 0, keyArray.length);
			keyArray = newKeyArray;

			String[][] newValueArray = new String[newSize][];
			System.arraycopy(valueArray, 0, newValueArray, 0, valueArray.length);
			valueArray = newValueArray;
		}
		keyArray[size] = key;
		valueArray[size] = value;
		size++;
	}

	public synchronized boolean containsKey(String key) {
		if (key == null)
			throw new IllegalArgumentException();
		return (findKey(key) != -1);
	}

	public synchronized String[] get(String key) {
		if (key == null)
			throw new IllegalArgumentException();
		int id = findKey(key);
		if (id == -1)
			return null;
		return valueArray[id];
	}

	/* package */String[] getKeys() {
		return keyArray;
	}

	/* package */String[][] getValues() {
		return valueArray;
	}

	/* package */int getSzie() {
		return size;
	}

	private int recalcSize(int currentSize) {
		return (int) (currentSize * (1.0f + growthFactor));
	}

	private int findKey(String key) {
		for (int i = 0; i < keyArray.length; i++) {
			if (keyArray[i] == null)
				continue;
			if (keyArray[i].equals(key))
				return i;
		}
		return -1;
	}
}
