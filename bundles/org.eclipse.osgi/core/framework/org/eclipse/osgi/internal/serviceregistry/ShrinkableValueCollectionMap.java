/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

	public void clear() {
		map.clear();
		if (values != null) {
			values.clear();
		}
	}

	public boolean containsKey(Object key) {
		return map.containsKey(key);
	}

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

	public Set<Map.Entry<K, Collection<V>>> entrySet() {
		return new EntrySet();
	}

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
			value = new ShrinkableEntrySetValueCollection<V>(entrySet);
			if (values == null) {
				values = new HashMap<Object, Collection<V>>(map.size());
			}
			values.put(key, value);
		}
		return value;
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

	public Collection<V> remove(Object key) {
		Set<? extends Map.Entry<?, ? extends V>> entrySet = map.remove(key);
		Collection<V> value = null;
		if (values != null) {
			value = values.remove(key);
		}
		if ((value == null) && (entrySet != null)) {
			value = new ShrinkableEntrySetValueCollection<V>(entrySet);
		}
		return value;
	}

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

		public Iterator<Map.Entry<K, Collection<V>>> iterator() {
			return new EntryIterator();
		}

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

		public boolean hasNext() {
			return iter.hasNext();
		}

		public Map.Entry<K, Collection<V>> next() {
			last = iter.next();
			return new Entry(last);
		}

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

		public K getKey() {
			return key;
		}

		public Collection<V> getValue() {
			if (value == null) {
				value = ShrinkableValueCollectionMap.this.get(key);
			}
			return value;
		}

		public Collection<V> setValue(Collection<V> value) {
			throw new UnsupportedOperationException(); // entries cannot be modified.
		}

		public String toString() {
			return getKey() + "=" + getValue(); //$NON-NLS-1$
		}

		public int hashCode() {
			return hash(getKey()) ^ hash(getValue());
		}

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
