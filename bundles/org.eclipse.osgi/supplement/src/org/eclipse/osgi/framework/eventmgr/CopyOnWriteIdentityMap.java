/*******************************************************************************
 * Copyright (c) 2003, 2020 IBM Corporation and others.
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

package org.eclipse.osgi.framework.eventmgr;

import static java.util.Objects.requireNonNull;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * A copy-on-write identity map. Write operations result in copying the underlying data so that
 * simultaneous read operations are not affected.
 * This allows for safe, unsynchronized traversal.
 *
 * <p>
 * Note: This class uses identity for key and value comparison, not equals.
 *
 * @since 3.5
 */
public class CopyOnWriteIdentityMap<K, V> implements Map<K, V> {
	/**
	 * The empty array singleton instance.
	 */
	@SuppressWarnings("rawtypes")
	private static final Entry[] emptyArray = new Entry[0];

	/**
	 * The array of entries. This field is volatile so it can be
	 * accessed from unsynchronized reader methods.
	 */
	private volatile Entry<K, V>[] entries;

	/**
	 * Creates an empty map.
	 */
	public CopyOnWriteIdentityMap() {
		entries = empty();
	}

	/**
	 * Copy constructor.
	 *
	 * @param source The CopyOnWriteMap to copy.
	 */
	public CopyOnWriteIdentityMap(CopyOnWriteIdentityMap<? extends K, ? extends V> source) {
		@SuppressWarnings("unchecked")
		Entry<K, V>[] toCopy = (Entry<K, V>[]) source.entries();
		this.entries = toCopy;
	}

	/* These methods modify the map and are synchronized. */

	/**
	 * Add a key, value pair to the map. If the key object is already in the map,
	 * then its value is replaced with the new value. Keys are compared using
	 * identity.
	 *
	 * @param key   The key object to be added to the list.
	 * @param value The value object to be associated with the key. This may be
	 *              null.
	 * @return <code>null</code> if the specified key was newly added to the map.
	 *         Otherwise the previous value of the key.
	 * @throws NullPointerException If key is null.
	 */
	@Override
	public synchronized V put(K key, V value) {
		requireNonNull(key);

		int size = entries.length;
		for (int i = 0; i < size; i++) {
			if (entries[i].key == key) {
				V v = entries[i].value;
				if (v == value) {
					return v;
				}
				@SuppressWarnings("unchecked")
				Entry<K, V>[] newEntries = new Entry[size];
				System.arraycopy(entries, 0, newEntries, 0, size);
				newEntries[i] = new Entry<>(key, value);
				entries = newEntries;
				return v;
			}
		}

		@SuppressWarnings("unchecked")
		Entry<K, V>[] newEntries = new Entry[size + 1];
		if (size > 0) {
			System.arraycopy(entries, 0, newEntries, 0, size);
		}
		newEntries[size] = new Entry<>(key, value);
		entries = newEntries;
		return null;
	}

	/**
	 * Add all the entries from the specified map to this map.
	 *
	 * @param source The map whose entries are to be added to this map.
	 */
	@Override
	public void putAll(Map<? extends K, ? extends V> source) {
		int sourceSize = source.size();
		if (sourceSize == 0) {
			return;
		}
		if (source instanceof CopyOnWriteIdentityMap<?, ?>) {
			putAll(((CopyOnWriteIdentityMap<? extends K, ? extends V>) source).entries());
			return;
		}

		@SuppressWarnings("unchecked")
		Entry<K, V>[] toCopy = new Entry[sourceSize];
		Iterator<? extends Map.Entry<? extends K, ? extends V>> iter = source.entrySet().iterator();
		for (int i = 0; i < sourceSize; i++) {
			Map.Entry<? extends K, ? extends V> mapEntry = iter.next();
			toCopy[i] = new Entry<>(mapEntry.getKey(), mapEntry.getValue());
		}
		putAll(toCopy);
	}

	/**
	 * Add all the keys from the specified array to this map with the value
	 * <code>null</code>.
	 *
	 * @param keys The array of keys to be added to this map.
	 */
	public <L extends K> void putAll(L[] keys) {
		int sourceSize = keys.length;
		if (sourceSize == 0) {
			return;
		}
		@SuppressWarnings("unchecked")
		Entry<K, V>[] toCopy = new Entry[sourceSize];
		for (int i = 0; i < sourceSize; i++) {
			toCopy[i] = new Entry<>(keys[i], null);
		}
		putAll(toCopy);
	}

	/**
	 * Add all the entries to this map.
	 *
	 * @param toCopy Array of entries to add to this map.
	 */
	private synchronized void putAll(Entry<? extends K, ? extends V>[] toCopy) {
		int sourceSize = toCopy.length;
		int size = entries.length;
		@SuppressWarnings("unchecked")
		Entry<K, V>[] newEntries = new Entry[size + sourceSize];
		System.arraycopy(entries, 0, newEntries, 0, size);
		copy: for (int n = 0; n < sourceSize; n++) {
			@SuppressWarnings("unchecked")
			Entry<K, V> copy = (Entry<K, V>) toCopy[n];
			for (int i = 0; i < size; i++) {
				if (newEntries[i].key == copy.key) {
					newEntries[i] = copy;
					continue copy;
				}
			}
			newEntries[size] = copy;
			size++;
		}
		if (size == newEntries.length) {
			entries = newEntries;
			return;
		}
		@SuppressWarnings("unchecked")
		Entry<K, V>[] e = new Entry[size];
		System.arraycopy(newEntries, 0, e, 0, size);
		entries = e;
	}

	/**
	 * Remove a key from the map and returns the value associated with the key. Key
	 * objects are compared using identity.
	 *
	 * @param key The key object to be removed from the map.
	 * @return <code>null</code> if the key was not in the list. Otherwise, the
	 *         value associated with the key.
	 * @throws NullPointerException If key is null.
	 */
	@Override
	public synchronized V remove(Object key) {
		requireNonNull(key);

		int size = entries.length;
		for (int i = 0; i < size; i++) {
			if (entries[i].key == key) {
				V v = entries[i].value;
				entries = removeEntry(entries, i);
				return v;
			}
		}
		return null;
	}

	/**
	 * Static method used to return an Entry array with the ith entry removed.
	 */
	static <K, V> Entry<K, V>[] removeEntry(final Entry<K, V>[] entries, final int i) {
		int size = entries.length;
		if (size == 1) {
			return empty();
		}
		@SuppressWarnings("unchecked")
		Entry<K, V>[] newEntries = new Entry[size - 1];
		if (i > 0) {
			System.arraycopy(entries, 0, newEntries, 0, i);
		}
		int next = size - 1 - i;
		if (next > 0) {
			System.arraycopy(entries, i + 1, newEntries, i, next);
		}
		return newEntries;
	}

	/**
	 * Remove all entries from the map.
	 */
	@Override
	public synchronized void clear() {
		entries = empty();
	}

	/* These methods only read the map and are not synchronized. */

	/**
	 * Accessor for methods which only read the entries.
	 * @return The array of entries. Callers to this method MUST NOT
	 * modify the returned array.
	 */
	private Entry<K, V>[] entries() {
		return entries;
	}

	/**
	 * Return the static empty array generically type safe.
	 * @return The empty array of entries.
	 */
	@SuppressWarnings("unchecked")
	static <K, V> Entry<K, V>[] empty() {
		return emptyArray;
	}

	/**
	 * Is the map empty?
	 *
	 * @return <code>true</code> if the list is empty.
	 */
	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	/**
	 * Return the number of entries in the map.
	 *
	 * @return The number of entries in the map.
	 */
	@Override
	public int size() {
		return entries().length;
	}

	/**
	 * Return the value object for the specified key. Keys are compared using
	 * identity.
	 *
	 * @param key The key object.
	 * @return The value object for the specified key.
	 * @throws NullPointerException If key is null.
	 */
	@Override
	public V get(Object key) {
		requireNonNull(key);

		for (Entry<K, V> entry : entries()) {
			if (entry.key == key) {
				return entry.value;
			}
		}
		return null;
	}

	/**
	 * Check if the map contains the specified key. Keys are compared using
	 * identity.
	 *
	 * @param key The key object.
	 * @return <code>true</code> if the specified key is in the map.
	 * @throws NullPointerException If key is null.
	 */
	@Override
	public boolean containsKey(Object key) {
		requireNonNull(key);

		for (Entry<K, V> entry : entries()) {
			if (entry.key == key) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check if the map contains the specified value.
	 * Values are compared using identity.
	 *
	 * @param value The value object.
	 * @return <code>true</code> if the specified value is in the map.
	 */
	@Override
	public boolean containsValue(Object value) {
		for (Entry<K, V> entry : entries()) {
			if (entry.value == value) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns a snapshot of the entries in this map.
	 * Changes to the returned set or this map will not affect each other.
	 *
	 * @return A Set of Map.Entry for each entry in this map.
	 * The entries returned by the set cannot be modified.
	 */
	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		return new Snapshot<>(entries()).entrySet();
	}

	/**
	 * Returns a snapshot of the keys in this map.
	 * Changes to the returned set or this map will not affect each other.
	 *
	 * @return A Set of the key objects in this map
	 */
	@Override
	public Set<K> keySet() {
		return new Snapshot<>(entries()).keySet();
	}

	/**
	 * Returns a snapshot of the values in this map.
	 * Changes to the returned set or this map will not affect each other.
	 *
	 * @return A Collection of the value objects in this map.
	 */
	@Override
	public Collection<V> values() {
		return new Snapshot<>(entries()).values();
	}

	/**
	 * This class represents the entry in this identity map.
	 * Entry is immutable.
	 */
	static private final class Entry<K, V> implements Map.Entry<K, V> {
		/**
		 * Key object.
		 */
		final K key;

		/**
		 * Value object.
		 */
		final V value;

		/**
		 * Constructor for map entry.
		 * 
		 * @param key   Key object in entry. Used for uniqueness.
		 * @param value Value object stored with key object.
		 * @throws NullPointerException If key is null.
		 */
		Entry(final K key, final V value) {
			this.key = requireNonNull(key);
			this.value = value;
		}

		@Override
		public K getKey() {
			return key;
		}

		@Override
		public V getValue() {
			return value;
		}

		@Override
		public V setValue(V value) {
			throw new UnsupportedOperationException(); // entries cannot be modified.
		}

		@Override
		public String toString() {
			return key + "=" + value; //$NON-NLS-1$
		}

		@Override
		public int hashCode() {
			return System.identityHashCode(key) ^ System.identityHashCode(value);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}

			if (!(obj instanceof Map.Entry)) {
				return false;
			}

			Map.Entry<?, ?> e = (Map.Entry<?, ?>) obj;
			return (key == e.getKey()) && (value == e.getValue());
		}
	}

	/**
	 * A snapshot of the entries in the map. This snapshot used by
	 * the map collection views. Changes made by the collection
	 * views only mutate the snapshot and not the map. The collection
	 * views only allow removal not addition.
	 */
	static private final class Snapshot<K, V> {
		volatile Entry<K, V>[] entries;

		Snapshot(Entry<K, V>[] e) {
			entries = e;
		}

		Entry<K, V>[] entries() {
			return entries;
		}

		synchronized void removeEntry(int i) {
			entries = CopyOnWriteIdentityMap.removeEntry(entries, i);
		}

		synchronized void clearEntries() {
			entries = CopyOnWriteIdentityMap.empty();
		}

		Set<Map.Entry<K, V>> entrySet() {
			return new EntrySet();
		}

		Set<K> keySet() {
			return new KeySet();
		}

		Collection<V> values() {
			return new ValueCollection();
		}

		/**
		 * Entry set view over the snapshot.
		 */
		private final class EntrySet extends AbstractSet<Map.Entry<K, V>> {
			EntrySet() {
				super();
			}

			@Override
			public Iterator<Map.Entry<K, V>> iterator() {
				return new EntryIterator();
			}

			@Override
			public int size() {
				return entries().length;
			}

			@Override
			public boolean remove(Object o) {
				requireNonNull(o);

				synchronized (Snapshot.this) {
					int size = entries.length;
					for (int i = 0; i < size; i++) {
						if (entries[i].equals(o)) {
							removeEntry(i);
							return true;
						}
					}
				}
				return false;
			}

			@Override
			public void clear() {
				clearEntries();
			}
		}

		/**
		 * Entry set iterator over the snapshot.
		 */
		private final class EntryIterator extends SnapshotIterator<Map.Entry<K, V>> {
			EntryIterator() {
				super();
			}

			@Override
			public Map.Entry<K, V> next() {
				return nextEntry();
			}
		}

		/**
		 * Key set view over the snapshot.
		 */
		private final class KeySet extends AbstractSet<K> {
			KeySet() {
				super();
			}

			@Override
			public Iterator<K> iterator() {
				return new KeyIterator();
			}

			@Override
			public int size() {
				return entries().length;
			}

			@Override
			public boolean remove(Object o) {
				requireNonNull(o);

				synchronized (Snapshot.this) {
					int size = entries.length;
					for (int i = 0; i < size; i++) {
						if (entries[i].key == o) {
							removeEntry(i);
							return true;
						}
					}
				}
				return false;
			}

			@Override
			public void clear() {
				clearEntries();
			}
		}

		/**
		 * Key set iterator over the snapshot.
		 */
		private final class KeyIterator extends SnapshotIterator<K> {
			KeyIterator() {
				super();
			}

			@Override
			public K next() {
				return nextEntry().key;
			}
		}

		/**
		 * Value collection view over the snapshot.
		 */
		private final class ValueCollection extends AbstractCollection<V> {
			ValueCollection() {
				super();
			}

			@Override
			public Iterator<V> iterator() {
				return new ValueIterator();
			}

			@Override
			public int size() {
				return entries().length;
			}

			@Override
			public boolean remove(Object o) {
				requireNonNull(o);

				synchronized (Snapshot.this) {
					int size = entries.length;
					for (int i = 0; i < size; i++) {
						if (entries[i].value == o) {
							removeEntry(i);
							return true;
						}
					}
				}
				return false;
			}

			@Override
			public void clear() {
				clearEntries();
			}
		}

		/**
		 * Value collection iterator over the snapshot.
		 */
		private final class ValueIterator extends SnapshotIterator<V> {
			ValueIterator() {
				super();
			}

			@Override
			public V next() {
				return nextEntry().value;
			}
		}

		/**
		 * Base iterator class handling removal and concurrent modifications.
		 */
		private abstract class SnapshotIterator<E> implements Iterator<E> {
			private int length;
			private int cursor;

			SnapshotIterator() {
				length = entries().length;
				cursor = 0;
			}

			@Override
			public final boolean hasNext() {
				return cursor < length;
			}

			protected final Entry<K, V> nextEntry() {
				Entry<K, V>[] e = entries();
				if (length != e.length) {
					throw new ConcurrentModificationException();
				}
				if (cursor == length) {
					throw new NoSuchElementException();
				}
				return e[cursor++];
			}

			@Override
			public final void remove() {
				if (length != entries().length) {
					throw new ConcurrentModificationException();
				}
				if (cursor == 0) {
					throw new IllegalStateException();
				}
				cursor--;
				removeEntry(cursor);
				length = entries().length;
			}
		}
	}
}
