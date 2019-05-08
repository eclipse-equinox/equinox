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

package org.eclipse.osgi.internal.serviceregistry;

import java.util.*;

public class ShrinkableValueCollectionMap<K, V> extends AbstractMap<K, Collection<V>> implements Map<K, Collection<V>> {
	final Map<? extends K, ? extends Set<? extends Map.Entry<?, ? extends V>>> map;
	Map<Object, Collection<V>> values;

	public ShrinkableValueCollectionMap(Map<? extends K, ? extends Set<? extends Map.Entry<?, ? extends V>>> m) {
		if (m == null) {
			throw new NullPointerException();
		}
		map = m;
		values = null;
	}

	@Override
	public void clear() {
		map.clear();
		if (values != null) {
			values.clear();
		}
	}

	@Override
	public boolean containsKey(Object key) {
		return map.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		/* Since values are collections and the collection has identity equality,
		 * there is no way any value could be contained in this map unless that
		 * value has already been gotten from this map.
		 */
		if (values == null) {
			return false;
		}
		return values.containsValue(value);
	}

	@Override
	public Set<Map.Entry<K, Collection<V>>> entrySet() {
		return new EntrySet();
	}

	@Override
	public Collection<V> get(Object key) {
		Collection<V> value = null;
		if (values != null) {
			value = values.get(key);
		}
		if (value == null) {
			Set<? extends Map.Entry<?, ? extends V>> entrySet = map.get(key);
			if (entrySet == null) {
				return null;
			}
			value = new ShrinkableEntrySetValueCollection<>(entrySet);
			if (values == null) {
				values = new HashMap<>(map.size());
			}
			values.put(key, value);
		}
		return value;
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public Collection<V> remove(Object key) {
		Set<? extends Map.Entry<?, ? extends V>> entrySet = map.remove(key);
		Collection<V> value = null;
		if (values != null) {
			value = values.remove(key);
		}
		if ((value == null) && (entrySet != null)) {
			value = new ShrinkableEntrySetValueCollection<>(entrySet);
		}
		return value;
	}

	@Override
	public int size() {
		return map.size();
	}

	/**
	 * Set class used for entry sets.
	 */
	private final class EntrySet extends AbstractSet<Map.Entry<K, Collection<V>>> {
		EntrySet() {
			super();
		}

		@Override
		public Iterator<Map.Entry<K, Collection<V>>> iterator() {
			return new EntryIterator();
		}

		@Override
		public int size() {
			return ShrinkableValueCollectionMap.this.size();
		}
	}

	/** 
	 * Iterator class used for entry sets.
	 */
	private final class EntryIterator implements Iterator<Map.Entry<K, Collection<V>>> {
		private final Iterator<? extends K> iter;
		private K last;

		EntryIterator() {
			iter = map.keySet().iterator();
		}

		@Override
		public boolean hasNext() {
			return iter.hasNext();
		}

		@Override
		public Map.Entry<K, Collection<V>> next() {
			last = iter.next();
			return new Entry(last);
		}

		@Override
		public void remove() {
			iter.remove();
			if (values != null) {
				values.remove(last);
			}
		}
	}

	/**
	 * This class represents the entry in this map.
	 */
	private final class Entry implements Map.Entry<K, Collection<V>> {
		private final K key;
		private Collection<V> value;

		Entry(final K k) {
			key = k;
		}

		@Override
		public K getKey() {
			return key;
		}

		@Override
		public Collection<V> getValue() {
			if (value == null) {
				value = ShrinkableValueCollectionMap.this.get(key);
			}
			return value;
		}

		@Override
		public Collection<V> setValue(Collection<V> value) {
			throw new UnsupportedOperationException(); // entries cannot be modified.
		}

		@Override
		public String toString() {
			return getKey() + "=" + getValue(); //$NON-NLS-1$
		}

		@Override
		public int hashCode() {
			return hash(getKey()) ^ hash(getValue());
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}
			if (!(obj instanceof Map.Entry)) {
				return false;
			}
			Map.Entry<?, ?> other = (Map.Entry<?, ?>) obj;
			return equality(getKey(), other.getKey()) && equality(getValue(), other.getValue());
		}
	}

	static int hash(Object one) {
		return (one == null) ? 0 : one.hashCode();
	}

	static boolean equality(Object one, Object two) {
		return (one == null) ? (two == null) : one.equals(two);
	}
}
