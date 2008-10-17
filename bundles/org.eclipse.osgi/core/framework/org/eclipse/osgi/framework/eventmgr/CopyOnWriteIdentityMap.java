/*******************************************************************************
 * Copyright (c) 2003, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.eventmgr;

import java.util.*;

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
public class CopyOnWriteIdentityMap implements Map {
	/**
	 * The empty array singleton instance.
	 */
	private static final Entry[] emptyArray = new Entry[0];

	/**
	 * The array of entries. This field is volatile so it can be 
	 * accessed from unsynchronized reader methods.
	 */
	private volatile Entry[] entries;

	/**
	 * Creates an empty map.
	 *
	 */
	public CopyOnWriteIdentityMap() {
		entries = emptyArray;
	}

	/**
	 * Copy constructor.
	 *
	 * @param source The CopyOnWriteMap to copy.
	 */
	public CopyOnWriteIdentityMap(CopyOnWriteIdentityMap source) {
		this.entries = source.entries();
	}

	/* These methods modify the map and are synchronized. */

	/**
	 * Add a key, value pair to the map.
	 * If the key object is already in the map, then its value is replaced with the new value.
	 * Keys are compared using identity.
	 *
	 * @param key The key object to be added to the list.
	 * @param value The value object to be associated with the key.
	 * This may be null.
	 * @return <code>null</code> if the specified key was newly added to the map.
	 * Otherwise the previous value of the key.
	 * @throws IllegalArgumentException If key is null.
	 */
	public synchronized Object put(Object key, Object value) {
		if (key == null) {
			throw new IllegalArgumentException();
		}

		int size = entries.length;
		for (int i = 0; i < size; i++) {
			if (entries[i].key == key) {
				Object v = entries[i].value;
				if (v == value) {
					return v;
				}
				Entry[] newEntries = new Entry[size];
				System.arraycopy(entries, 0, newEntries, 0, size);
				newEntries[i] = new Entry(key, value);
				entries = newEntries;
				return v;
			}
		}

		Entry[] newEntries = new Entry[size + 1];
		if (size > 0) {
			System.arraycopy(entries, 0, newEntries, 0, size);
		}
		newEntries[size] = new Entry(key, value);
		entries = newEntries;
		return null;
	}

	/**
	 * Add all the entries from the specified map to this map.
	 * 
	 * @param source The map whose entries are to be added to this map.
	 */
	public void putAll(Map source) {
		int sourceSize = source.size();
		if (sourceSize == 0) {
			return;
		}
		if (source instanceof CopyOnWriteIdentityMap) {
			putAll(((CopyOnWriteIdentityMap) source).entries());
			return;
		}

		Entry[] toCopy = new Entry[sourceSize];
		Iterator iter = source.entrySet().iterator();
		for (int i = 0; i < sourceSize; i++) {
			Map.Entry mapEntry = (Map.Entry) iter.next();
			toCopy[i] = new Entry(mapEntry.getKey(), mapEntry.getValue());
		}
		putAll(toCopy);
	}

	/**
	 * Add all the keys from the specified array to this map with the value
	 * <code>null</code>.
	 * 
	 * @param keys The array of keys to be added to this map.
	 */
	public void putAll(Object[] keys) {
		int sourceSize = keys.length;
		if (sourceSize == 0) {
			return;
		}
		Entry[] toCopy = new Entry[sourceSize];
		for (int i = 0; i < sourceSize; i++) {
			toCopy[i] = new Entry(keys[i], null);
		}
		putAll(toCopy);
	}

	/**
	 * Add all the entries to this map.
	 * 
	 * @param toCopy Array of entries to add to this map.
	 */
	private synchronized void putAll(Entry[] toCopy) {
		int sourceSize = toCopy.length;
		int size = entries.length;
		Entry[] newEntries = new Entry[size + sourceSize];
		System.arraycopy(entries, 0, newEntries, 0, size);
		copy: for (int n = 0; n < sourceSize; n++) {
			Entry copy = toCopy[n];
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
		Entry[] e = new Entry[size];
		System.arraycopy(newEntries, 0, e, 0, size);
		entries = e;
	}

	/**
	 * Remove a key from the map and returns the value associated with the key.
	 * Key objects are compared using identity.
	 *
	 * @param key The key object to be removed from the map.
	 * @return <code>null</code> if the key was not in the list. 
	 * Otherwise, the value associated with the key.
	 * @throws IllegalArgumentException If key is null.
	 */
	public synchronized Object remove(Object key) {
		if (key == null) {
			throw new IllegalArgumentException();
		}

		int size = entries.length;
		for (int i = 0; i < size; i++) {
			if (entries[i].key == key) {
				Object v = entries[i].value;
				if (size == 1) {
					entries = emptyArray;
					return v;
				}
				Entry[] newEntries = new Entry[size - 1];
				if (i > 0) {
					System.arraycopy(entries, 0, newEntries, 0, i);
				}
				int next = size - 1 - i;
				if (next > 0) {
					System.arraycopy(entries, i + 1, newEntries, i, next);
				}
				entries = newEntries;
				return v;
			}
		}
		return null;
	}

	/**
	 * Remove all entries from the map.
	 * 
	 */
	public synchronized void clear() {
		entries = emptyArray;
	}

	/* These methods only read the map and are not synchronized. */

	/**
	 * Accessor for methods which only read the entries.
	 * @return The array of entries. Callers to this method MUST NOT
	 * modify the returned array.
	 */
	private Entry[] entries() {
		return entries;
	}

	/**
	 * Is the map empty?
	 * 
	 * @return <code>true</code> if the list is empty.
	 */
	public boolean isEmpty() {
		return size() == 0;
	}

	/**
	 * Return the number of entries in the map.
	 * 
	 * @return The number of entries in the map.
	 */
	public int size() {
		return entries().length;
	}

	/**
	 * Return the value object for the specified key.
	 * Keys are compared using identity.
	 * 
	 * @param key The key object.
	 * @return The value object for the specified key.
	 * @throws IllegalArgumentException If key is null.
	 */
	public Object get(Object key) {
		if (key == null) {
			throw new IllegalArgumentException();
		}

		Entry[] e = entries();
		for (int i = 0; i < e.length; i++) {
			if (e[i].key == key) {
				return e[i].value;
			}
		}
		return null;
	}

	/**
	 * Check if the map contains the specified key.
	 * Keys are compared using identity.
	 * 
	 * @param key The key object.
	 * @return <code>true</code> if the specified key is in the map.
	 * @throws IllegalArgumentException If key is null.
	 */
	public boolean containsKey(Object key) {
		if (key == null) {
			throw new IllegalArgumentException();
		}

		Entry[] e = entries();
		for (int i = 0; i < e.length; i++) {
			if (e[i].key == key) {
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
	public boolean containsValue(Object value) {
		Entry[] e = entries();
		for (int i = 0; i < e.length; i++) {
			if (e[i].value == value) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns a snapshot of the entries in this map.
	 * The returned set will NOT be changed by future changes to this map.
	 * 
	 * @return A Set of Map.Entry for each entry in this map.
	 * The set and the entries returned by the set cannot be modified.
	 */
	public Set entrySet() {
		return new EntrySet(entries(), EntrySet.ENTRY);
	}

	/**
	 * Returns a snapshot of the keys in this map.
	 * The returned set will NOT be changed by future changes to this map.
	 * 
	 * @return A Set of the key objects in this map
	 * The set cannot be modified.
	 */
	public Set keySet() {
		return new EntrySet(entries(), EntrySet.KEY);
	}

	/**
	 * Returns a snapshot of the values in this map.
	 * The returned collection will NOT be changed by future changes to this map.
	 * 
	 * @return A Collection of the value objects in this map.
	 * The collection cannot be modified.
	 */
	public Collection values() {
		return new EntrySet(entries(), EntrySet.VALUE);
	}

	/**
	 * This class represents the entry in this Map.
	 * Entry is immutable.
	 */
	private static class Entry implements Map.Entry {
		/**
		 * Key object.
		 */
		final Object key;

		/**
		 * Value object.
		 */
		final Object value;

		/**
		 * Constructor for map entry.
		 * @param key Key object in entry. Used for uniqueness.
		 * @param value Value object stored with key object.
		 */
		Entry(final Object key, final Object value) {
			this.key = key;
			this.value = value;
		}

		public Object getKey() {
			return key;
		}

		public Object getValue() {
			return value;
		}

		public Object setValue(Object o) {
			throw new UnsupportedOperationException(); // entries cannot be modified.
		}
	}

	/**
	 * Set class used for entry and key sets and values collections.
	 *
	 * This class is immutable.
	 */
	private static class EntrySet extends AbstractSet {
		private final Entry[] entries;
		private final int returnType;
		final static int ENTRY = 1;
		final static int KEY = 2;
		final static int VALUE = 3;

		EntrySet(Entry[] entries, int returnType) {
			this.entries = entries;
			this.returnType = returnType;
		}

		public Iterator iterator() {
			return new EntryIterator(entries, returnType);
		}

		public int size() {
			return entries.length;
		}
	}

	/** 
	 * Iterator class used for entry and key sets and values collections.
	 *
	 */
	private static class EntryIterator implements Iterator {
		private final Entry[] entries;
		private final int returnType;
		private int cursor = 0;

		EntryIterator(Entry[] entries, int returnType) {
			this.entries = entries;
			this.returnType = returnType;
		}

		public boolean hasNext() {
			return cursor < entries.length;
		}

		public Object next() {
			if (cursor == entries.length) {
				throw new NoSuchElementException();
			}
			switch (returnType) {
				case EntrySet.ENTRY :
					return entries[cursor++];
				case EntrySet.KEY :
					return entries[cursor++].key;
				case EntrySet.VALUE :
					return entries[cursor++].value;
			}
			throw new InternalError();
		}

		public void remove() {
			throw new UnsupportedOperationException(); // the collection cannot be modified.
		}
	}
}
