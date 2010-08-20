/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.module;

import java.lang.reflect.Array;
import java.util.*;

/*
 * A MappedList maps values into keyed list arrays.  All values with the same key are stored
 * into the same array.  Extending classes may override the sort method to sort the individual
 * arrays in the MappedList.  By default the MappedList appends new values to the end of the array.
 */
public class MappedList<K, V> {
	// the mapping with key -> Object[] mapping
	protected final HashMap<K, Object> internal = new HashMap();
	protected final Class<V> valueClass;
	protected final V[] empty;

	public MappedList(Class<V> valueClass) {
		this.valueClass = valueClass;
		empty = (V[]) Array.newInstance(valueClass, 0);
	}

	public void put(K key, V value) {
		Object existing = internal.get(key);
		if (existing == null) {
			internal.put(key, value);
		} else {
			V[] existingValues;
			if (existing.getClass().isArray()) {
				existingValues = (V[]) existing;
			} else {
				existingValues = (V[]) Array.newInstance(valueClass, 1);
				existingValues[0] = (V) existing;
			}
			// insert the new value
			int index = insertionIndex(existingValues, value);
			V[] newValues = (V[]) Array.newInstance(valueClass, existingValues.length + 1);
			System.arraycopy(existingValues, 0, newValues, 0, index);
			newValues[index] = value;
			System.arraycopy(existingValues, index, newValues, index + 1, existingValues.length - index);
			internal.put(key, newValues); // overwrite the old values in the map
		}
	}

	protected int insertionIndex(V[] existing, V value) {
		// a MappedList is by default not sorted so just insert at the end
		// extending classes may override this method to provide an index that retains sorted order
		return existing.length;
	}

	// removes all values with the specified key
	public V[] remove(K key) {
		return get(key, true);
	}

	// gets all values with the specified key
	public V[] get(K key) {
		return get(key, false);
	}

	// gets all values with the specified and optionally removes them
	private V[] get(K key, boolean remove) {
		Object result = remove ? internal.remove(key) : internal.get(key);
		if (result != null && result.getClass().isArray())
			return (V[]) result;
		if (result == null)
			return empty;
		V[] singleValue = (V[]) Array.newInstance(valueClass, 1);
		singleValue[0] = (V) result;
		return singleValue;
	}

	// returns the number of keyed lists
	public int getSize() {
		return internal.size();
	}

	// returns all values of all keys
	public V[] getAllValues() {
		if (getSize() == 0)
			return empty;
		ArrayList<V> results = new ArrayList(getSize());
		Iterator iter = internal.values().iterator();
		while (iter.hasNext()) {
			Object value = iter.next();
			if (value.getClass().isArray()) {
				Object[] values = (Object[]) value;
				for (int i = 0; i < values.length; i++)
					results.add((V) values[i]);
			} else
				results.add((V) value);
		}
		return results.toArray(empty);
	}

	// removes all keys from the map
	public void clear() {
		internal.clear();
	}

}
