/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.util;

import java.util.*;
import org.eclipse.osgi.internal.messages.Msg;
import org.eclipse.osgi.util.NLS;

/**
 * CaseInsensitiveDictionaryMap classes. This class implements Dictionary and Map with
 * the following behavior:
 * <ul>
 * <li>String keys are case-preserved,
 * but the lookup operations are case-insensitive.</li>
 * <li>Keys and values must not be null.</li>
 * </ul>
 * @since 3.13
 */
public class CaseInsensitiveDictionaryMap<K, V> extends Dictionary<K, V> implements Map<K, V> {
	private final Map<K, V> map;

	/**
	 * Create an empty CaseInsensitiveDictionaryMap.
	 */
	public CaseInsensitiveDictionaryMap() {
		map = new HashMap<>();
	}

	/**
	 * Create an empty CaseInsensitiveDictionaryMap.
	 *
	 * @param initialCapacity The initial capacity.
	 */
	public CaseInsensitiveDictionaryMap(int initialCapacity) {
		map = new HashMap<>(initialCapacity);
	}

	/**
	 * Create a CaseInsensitiveDictionaryMap dictionary from a Dictionary.
	 *
	 * @param dictionary The initial dictionary for this CaseInsensitiveDictionaryMap object.
	 * @throws IllegalArgumentException If a case-variants of a key are
	 * in the dictionary parameter.
	 */
	public CaseInsensitiveDictionaryMap(Dictionary<? extends K, ? extends V> dictionary) {
		this(initialCapacity(dictionary.size()));
		/* initialize the keys and values */
		Enumeration<? extends K> keys = dictionary.keys();
		while (keys.hasMoreElements()) {
			K key = keys.nextElement();
			if (putIfAbsent(key, dictionary.get(key)) != null) {
				throw new IllegalArgumentException(NLS.bind(Msg.HEADER_DUPLICATE_KEY_EXCEPTION, key));
			}
		}
	}

	/**
	 * Create a CaseInsensitiveDictionaryMap dictionary from a Map.
	 *
	 * @param map The initial map for this CaseInsensitiveDictionaryMap object.
	 * @throws IllegalArgumentException If a case-variants of a key are
	 * in the map parameter.
	 */
	public CaseInsensitiveDictionaryMap(Map<? extends K, ? extends V> map) {
		this(initialCapacity(map.size()));
		/* initialize the keys and values */
		for (Entry<? extends K, ? extends V> e : map.entrySet()) {
			if (putIfAbsent(e.getKey(), e.getValue()) != null) {
				throw new IllegalArgumentException(NLS.bind(Msg.HEADER_DUPLICATE_KEY_EXCEPTION, e.getKey()));
			}
		}
	}

	/**
	 * Compute the initial capacity of a map for the specified number of entries
	 * based upon the load factor of 0.75f.
	 *
	 * @param size The desired number of entries.
	 * @return The initial capacity of a map.
	 */
	protected static int initialCapacity(int size) {
		return Math.max((int) (size / 0.75f) + 1, 16);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Enumeration<K> keys() {
		return Collections.enumeration(keySet());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Enumeration<V> elements() {
		return Collections.enumeration(values());
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The key must be non-null.
	 * <p>
	 * If the key is a String, the key is located in a case-insensitive manner.
	 */
	@Override
	public V get(Object key) {
		Objects.requireNonNull(key);
		return get0(mappedKey(key));
	}

	private V get0(Object key) {
		return map.get(key);
	}

	/**
	 * Returns the specified key or, if the key is a String and the map
	 * contains a case-variant of the key, returns the case-variant of
	 * the key in the map.
	 *
	 * @param key
	 * @return The specified key or the case-variant of the key in the map.
	 */
	private Object mappedKey(Object key) {
		if ((key instanceof String) && !map.containsKey(key)) {
			String stringKey = (String) key;
			for (K k : map.keySet()) {
				if ((k instanceof String) && stringKey.equalsIgnoreCase((String) k)) {
					return k;
				}
			}
		}
		return key;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int size() {
		return map.size();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The key and value must be non-null.
	 * <p>
	 * If the key is a String, any case-variant will be replaced.
	 */
	@Override
	public V put(K key, V value) {
		Objects.requireNonNull(key);
		Objects.requireNonNull(value);
		return put0(key, value, mappedKey(key));
	}

	private V put0(K key, V value, Object mappedKey) {
		if (key instanceof String) {
			@SuppressWarnings("unchecked")
			K k = (K) ((String) key).intern();
			key = k;
		}
		V removeResult = (mappedKey != key) ? map.remove(mappedKey) : null;
		V putResult = map.put(key, value);
		return (putResult != null) ? putResult : removeResult;
	}

	/**
	 * If the specified key is not already associated with a value,
	 * associates it with the specified value and return
	 * {@code null}. Otherwise returns the current value of the
	 * specified key.
	 * <p>
	 * The key and value must be non-null.
	 * <p>
	 * If the key is a String, any case-variant will be replaced.
	 */
	public V putIfAbsent(K key, V value) {
		Objects.requireNonNull(key);
		Objects.requireNonNull(value);
		Object mappedKey = mappedKey(key);
		V v = get0(mappedKey);
		if (v == null) {
			v = put0(key, value, mappedKey);
		}
		return v;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The key must be non-null.
	 * <p>
	 * If the key is a String, the key is removed in a case-insensitive manner.
	 */
	@Override
	public V remove(Object key) {
		Objects.requireNonNull(key);
		return map.remove(mappedKey(key));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return map.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clear() {
		map.clear();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The key must be non-null.
	 * <p>
	 * If the key is a String, the key is located in a case-insensitive manner.
	 */
	@Override
	public boolean containsKey(Object key) {
		Objects.requireNonNull(key);
		if (map.containsKey(key)) {
			return true;
		}
		return map.containsKey(mappedKey(key));
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The value must be non-null.
	 */
	@Override
	public boolean containsValue(Object value) {
		Objects.requireNonNull(value);
		return map.containsValue(value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<Entry<K, V>> entrySet() {
		return map.entrySet();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<K> keySet() {
		return map.keySet();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Collection<V> values() {
		return map.values();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * If the specified map has case-variants of a String key, only the last case-variant
	 * found while iterating over the entrySet will be present in this object.
	 */
	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		for (Entry<? extends K, ? extends V> e : m.entrySet()) {
			put(e.getKey(), e.getValue());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		return map.hashCode();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		return map.equals(obj);
	}

	/**
	 * Return an unmodifiable map wrapping this CaseInsensitiveDictionaryMap.
	 *
	 * @return An unmodifiable map wrapping this CaseInsensitiveDictionaryMap.
	 */
	public Map<K, V> asUnmodifiableMap() {
		return Collections.unmodifiableMap(this);
	}

	/**
	 * Return an unmodifiable dictionary wrapping this CaseInsensitiveDictionaryMap.
	 *
	 * @return An unmodifiable dictionary wrapping this CaseInsensitiveDictionaryMap.
	 */
	public Dictionary<K, V> asUnmodifiableDictionary() {
		return unmodifiableDictionary(this);
	}

	/**
	 * Return an unmodifiable dictionary wrapping the specified dictionary.
	 *
	 * @return An unmodifiable dictionary wrapping the specified dictionary.
	 */
	public static <K, V> Dictionary<K, V> unmodifiableDictionary(Dictionary<? extends K, ? extends V> d) {
		return new UnmodifiableDictionary<>(d);
	}

	private static class UnmodifiableDictionary<K, V> extends Dictionary<K, V> {
		private final Dictionary<? extends K, ? extends V> d;

		UnmodifiableDictionary(Dictionary<? extends K, ? extends V> d) {
			this.d = Objects.requireNonNull(d);
		}

		@Override
		public int size() {
			return d.size();
		}

		@Override
		public boolean isEmpty() {
			return d.isEmpty();
		}

		@SuppressWarnings("unchecked")
		@Override
		public Enumeration<K> keys() {
			return (Enumeration<K>) d.keys();
		}

		@SuppressWarnings("unchecked")
		@Override
		public Enumeration<V> elements() {
			return (Enumeration<V>) d.elements();
		}

		@Override
		public V get(Object key) {
			return d.get(key);
		}

		@Override
		public V put(K key, V value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public V remove(Object key) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String toString() {
			return d.toString();
		}

		@Override
		public int hashCode() {
			return d.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			return d.equals(obj);
		}
	}
}
