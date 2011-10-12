/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.baseadaptor;

import java.util.*;
import org.eclipse.osgi.service.resolver.extras.Sortable;

/**
 * Simple map when dealing with small amounts of entries.
 * This class also provides a Collections view of the keys
 *
 * @param <K> The key type
 * @param <V> the value type
 */
public class ArrayMap<K, V> implements Collection<K>, Sortable<K> {
	final List<K> keys;
	final List<V> values;

	public ArrayMap(int initialCapacity) {
		keys = new ArrayList<K>(initialCapacity);
		values = new ArrayList<V>(initialCapacity);
	}

	/**
	 * Note that the keys and values are not copied.  Changes to this ArrayMap
	 * will change the values of the keys and values Lists.
	 * @param keys
	 * @param values
	 */
	public ArrayMap(List<K> keys, List<V> values) {
		if (keys.size() != values.size())
			throw new IllegalArgumentException("Keys and values size must be equal."); //$NON-NLS-1$
		this.keys = keys;
		this.values = values;
	}

	public V get(K key) {
		int index = keys.indexOf(key);
		if (index < 0)
			return null;
		return values.get(index);
	}

	public void put(K key, V value) {
		int index = keys.indexOf(key);
		if (index > 0) {
			values.set(index, value);
		} else {
			keys.add(key);
			values.add(value);
		}
	}

	public boolean remove(Object key) {
		int index = keys.indexOf(key);
		if (index < 0)
			return false;
		keys.remove(index);
		values.remove(index);
		return true;
	}

	public void clear() {
		keys.clear();
		values.clear();
	}

	public List<K> getKeys() {
		return new ArrayList<K>(keys);
	}

	public List<V> getValues() {
		return new ArrayList<V>(values);
	}

	public int size() {
		return keys.size();
	}

	public boolean isEmpty() {
		return keys.isEmpty();
	}

	public boolean contains(Object o) {
		return keys.contains(o);
	}

	public Iterator<K> iterator() {
		final Iterator<K> keyIter = keys.iterator();
		final Iterator<V> valueIter = values.iterator();

		return new Iterator<K>() {
			public boolean hasNext() {
				return keyIter.hasNext();
			}

			public K next() {
				valueIter.next();
				return keyIter.next();
			}

			public void remove() {
				valueIter.remove();
				keyIter.remove();
			}
		};
	}

	public Object[] toArray() {
		return keys.toArray();
	}

	public <T> T[] toArray(T[] a) {
		return keys.toArray(a);
	}

	public boolean add(K o) {
		throw new UnsupportedOperationException();
	}

	public boolean containsAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	public boolean addAll(Collection<? extends K> c) {
		throw new UnsupportedOperationException();
	}

	public boolean removeAll(Collection<?> c) {
		boolean result = false;
		for (Object key : c)
			result |= remove(key);
		return result;
	}

	public boolean retainAll(Collection<?> c) {
		boolean result = false;
		Object[] keyArray = keys.toArray();
		for (Object key : keyArray) {
			if (!c.contains(key))
				result |= remove(key);
		}
		return result;
	}

	public K getKey(int index) {
		return keys.get(index);
	}

	public V getValue(int index) {
		return values.get(index);
	}

	public void sort(Comparator<K> comparator) {
		List<K> sortedKeys = new ArrayList<K>(keys);
		Collections.sort(sortedKeys, comparator);
		List<V> sortedValues = new ArrayList<V>(sortedKeys.size());
		for (K key : sortedKeys) {
			sortedValues.add(getByIdentity(key));
		}
		clear();
		for (int i = 0; i < sortedKeys.size(); i++) {
			put(sortedKeys.get(i), sortedValues.get(i));
		}
	}

	private V getByIdentity(K key) {
		int index = 0;
		for (K existing : keys) {
			if (existing == key)
				return getValue(index);
			index++;
		}
		return null;
	}
}
