/*******************************************************************************
 * Copyright (c) 2005, 2021 IBM Corporation and others.
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
package org.eclipse.osgi.internal.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/*
 * A MappedList maps values into keyed list arrays.  All values with the same key are stored
 * into the same array.  Extending classes may override the sort method to sort the individual
 * arrays in the MappedList.  By default the MappedList appends new values to the end of the array.
 */
public class MappedList<K, V> {
	// the mapping with key -> Object[] mapping
	protected final HashMap<K, List<V>> internal = new HashMap<>();
	protected final List<V> empty = Collections.emptyList();

	public void put(K key, V value) {
		List<V> existing = internal.get(key);
		if (existing == null) {
			existing = new ArrayList<>(1);
			existing.add(value);
			internal.put(key, existing);
			return;
		}
		// insert the new value
		int index = insertionIndex(existing, value);
		existing.add(index, value);
	}

	protected int insertionIndex(List<V> existing, V value) {
		// a MappedList is by default not sorted so just insert at the end
		// extending classes may override this method to provide an index that retains sorted order
		return existing.size();
	}

	// removes all values with the specified key
	public List<V> remove(K key) {
		return get(key, true);
	}

	// gets all values with the specified key
	public List<V> get(K key) {
		return get(key, false);
	}

	// gets all values with the specified and optionally removes them
	private List<V> get(K key, boolean remove) {
		List<V> result = remove ? internal.remove(key) : internal.get(key);
		return result == null ? empty : result;
	}

	// returns the number of keyed lists
	public int getSize() {
		return internal.size();
	}

	// returns all values of all keys
	public List<V> getAllValues() {
		if (getSize() == 0)
			return empty;
		ArrayList<V> results = new ArrayList<>(getSize());
		for (List<V> element : internal.values())
			results.addAll(element);
		return results;
	}

	// removes all keys from the map
	public void clear() {
		internal.clear();
	}

}
