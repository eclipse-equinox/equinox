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

import java.util.*;

/*
 * A MappedList maps values into keyed list arrays.  All values with the same key are stored
 * into the same array.  Extending classes may override the sort method to sort the individual
 * arrays in the MappedList.  By default the MappedList appends new values to the end of the array.
 */
public class MappedList {
	// the mapping with key -> Object[] mapping
	protected HashMap internal = new HashMap();

	public void put(Object key, Object value) {
		Object existing = internal.get(key);
		if (existing == null) {
			internal.put(key, value);
		} else {
			Object[] existingValues = existing.getClass().isArray() ? (Object[]) existing : new Object[] {existing};
			// insert the new value
			int index = insertionIndex(existingValues, value);
			Object[] newValues = new Object[existingValues.length + 1];
			System.arraycopy(existingValues, 0, newValues, 0, index);
			newValues[index] = value;
			System.arraycopy(existingValues, index, newValues, index + 1, existingValues.length - index);
			internal.put(key, newValues); // overwrite the old values in the map
		}
	}

	protected int insertionIndex(Object[] existing, Object value) {
		// a MappedList is by default not sorted so just insert at the end
		// extending classes may override this method to provide an index that retains sorted order
		return existing.length;
	}

	// removes all values with the specified key
	public Object[] remove(Object key) {
		return get(key, true);
	}

	// gets all values with the specified key
	public Object[] get(Object key) {
		return get(key, false);
	}

	// gets all values with the specified and optionally removes them
	private Object[] get(Object key, boolean remove) {
		Object result = remove ? internal.remove(key) : internal.get(key);
		if (result != null && result.getClass().isArray())
			return (Object[]) result;
		return result == null ? new Object[0] : new Object[] {result};
	}

	// returns the number of keyed lists
	public int getSize() {
		return internal.size();
	}

	// returns all values of all keys
	public Object[] getAllValues() {
		if (getSize() == 0)
			return new Object[0];
		ArrayList results = new ArrayList(getSize());
		Iterator iter = internal.values().iterator();
		while (iter.hasNext()) {
			Object value = iter.next();
			if (value.getClass().isArray()) {
				Object[] values = (Object[]) iter.next();
				for (int i = 0; i < values.length; i++)
					results.add(values[i]);
			} else
				results.add(value);
		}
		return results.toArray();
	}

	// removes all keys from the map
	public void clear() {
		internal.clear();
	}

}
