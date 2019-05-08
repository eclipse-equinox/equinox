/*******************************************************************************
 * Copyright (c) 2010, 2016 IBM Corporation and others.
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
package org.eclipse.osgi.framework.util;

import java.util.*;

/**
 * Simple map when dealing with small amounts of entries.
 * This class also provides a Collections view of the keys
 *
 * @param <K> The key type
 * @param <V> the value type
 */
public class ArrayMap<K, V> implements Collection<K> {
	final List<K> keys;
	final List<V> values;

	public ArrayMap(int initialCapacity) {
		keys = new ArrayList<>(initialCapacity);
		values = new ArrayList<>(initialCapacity);
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

	@Override
	public boolean remove(Object key) {
		int index = keys.indexOf(key);
		if (index < 0)
			return false;
		keys.remove(index);
		values.remove(index);
		return true;
	}

	@Override
	public void clear() {
		keys.clear();
		values.clear();
	}

	public List<K> getKeys() {
		return new ArrayList<>(keys);
	}

	public List<V> getValues() {
		return new ArrayList<>(values);
	}

	@Override
	public int size() {
		return keys.size();
	}

	@Override
	public boolean isEmpty() {
		return keys.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return keys.contains(o);
	}

	@Override
	public Iterator<K> iterator() {
		final Iterator<K> keyIter = keys.iterator();
		final Iterator<V> valueIter = values.iterator();

		return new Iterator<K>() {
		@Override
			public boolean hasNext() {
				return keyIter.hasNext();
			}

		@Override
			public K next() {
				valueIter.next();
				return keyIter.next();
			}

		@Override
			public void remove() {
				valueIter.remove();
				keyIter.remove();
			}
		};
	}

	@Override
	public Object[] toArray() {
		return keys.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return keys.toArray(a);
	}

	@Override
	public boolean add(K o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(Collection<? extends K> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		boolean result = false;
		for (Object key : c)
			result |= remove(key);
		return result;
	}

	@Override
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
		List<K> sortedKeys = new ArrayList<>(keys);
		Collections.sort(sortedKeys, comparator);
		List<V> sortedValues = new ArrayList<>(sortedKeys.size());
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
